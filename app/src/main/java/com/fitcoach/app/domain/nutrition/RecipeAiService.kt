package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.ChatCompletionRequest
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.domain.coach.AiConfigResolver
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** Esito della generazione di una ricetta scritta dall'AI. */
sealed interface RecipeAiResult {
    data class Success(val recipe: ComposedRecipe, val model: String?) : RecipeAiResult

    /**
     * L'AI non ha prodotto una ricetta valida. Non esiste un percorso
     * deterministico alternativo: l'utente deve sapere che la ricetta non c'e'
     * e perche', invece di ricevere un testo fisso presentato come ricetta.
     */
    data class Failed(val message: String, val detail: String? = null) : RecipeAiResult
}

/**
 * Scrive la ricetta con l'AI a partire dagli alimenti e dalle grammature che il
 * piano ha gia' assegnato a quel pasto.
 *
 * Regole non negoziabili, applicate a valle della risposta:
 * - gli ingredienti sono SOLO quelli del pasto, con le grammature del piano;
 * - kcal e macro non vengono presi dal modello ma ricalcolati dagli ingredienti;
 * - se il modello propone un ingrediente estraneo, la risposta viene rifiutata
 *   e si chiede al modello di correggerla.
 *
 * Non esiste fallback deterministico: se il provider non funziona o risponde in
 * modo inutilizzabile, il risultato e' [RecipeAiResult.Failed] con un messaggio
 * che dice cosa e' successo.
 */
@Singleton
class RecipeAiService @Inject constructor(
    private val aiClient: AiClient,
    private val configResolver: AiConfigResolver,
    private val json: Json
) {

    suspend fun generate(
        entries: List<MealEntry>,
        slot: MealSlot,
        style: RecipeStyle,
        profile: UserProfile?
    ): RecipeAiResult {
        val usable = entries.filter { it.grams > 0 }
        if (usable.isEmpty()) {
            return RecipeAiResult.Failed(
                "Il pasto non contiene alimenti utilizzabili: nessuna ricetta da generare."
            )
        }

        val config = configResolver.resolveConfigured()
            ?: return RecipeAiResult.Failed(
                "Nessun provider AI configurato. Vai in Impostazioni > Coach AI per " +
                    "inserire provider e chiave: la ricetta viene scritta dall'AI."
            )

        val messages = mutableListOf(
            ChatMessageDto(role = "system", content = SYSTEM_PROMPT),
            ChatMessageDto(role = "user", content = userPrompt(usable, slot, style, profile))
        )

        var lastProblem: String? = null
        // Primo tentativo piu' due correzioni: se il modello sbaglia formato o
        // inserisce alimenti estranei gli si dice cosa correggere, invece di
        // mostrare all'utente un errore di parsing.
        for (attempt in 0..MAX_CORRECTIONS) {
            val result = aiClient.complete(
                config = config,
                messages = messages,
                temperature = RECIPE_TEMPERATURE,
                maxTokens = RECIPE_MAX_TOKENS,
                responseFormat = ChatCompletionRequest.ResponseFormat()
            )
            when (result) {
                is AiResult.Failure -> {
                    // Un errore di configurazione non migliora ritentando.
                    return RecipeAiResult.Failed(result.message)
                }
                is AiResult.Success -> {
                    when (val parsed = parse(result.content, usable, slot, style)) {
                        is ParseOutcome.Ok ->
                            return RecipeAiResult.Success(parsed.recipe, result.model ?: config.model)
                        is ParseOutcome.Invalid -> {
                            lastProblem = parsed.reason
                            if (attempt == MAX_CORRECTIONS) break
                            // Il modello e' gia' in conversazione: si aggiunge la
                            // sua risposta e la correzione richiesta.
                            messages += ChatMessageDto(role = "assistant", content = result.content)
                            messages += ChatMessageDto(
                                role = "user",
                                content = correctionPrompt(parsed.reason, usable)
                            )
                        }
                    }
                }
            }
        }

        return RecipeAiResult.Failed(
            "L'AI non ha prodotto una ricetta valida dopo ${MAX_CORRECTIONS + 1} tentativi.",
            lastProblem
        )
    }

    private fun userPrompt(
        entries: List<MealEntry>,
        slot: MealSlot,
        style: RecipeStyle,
        profile: UserProfile?
    ): String = buildString {
        appendLine("Pasto: ${slot.label}")
        appendLine("Stile richiesto: ${style.label}")
        appendLine("Ingredienti del piano, con le grammature da usare (non modificabili):")
        entries.forEach { e ->
            appendLine(
                "- ${e.foodName}: ${fmt(e.grams)} g " +
                    "(${e.kcal.roundToInt()} kcal, P ${fmt(e.protein)} g, " +
                    "C ${fmt(e.carbs)} g, G ${fmt(e.fat)} g)"
            )
        }
        val avoid = profile?.excludedFoods?.filter { it.isNotBlank() }.orEmpty()
        if (avoid.isNotEmpty()) {
            appendLine("Alimenti da NON usare: ${avoid.joinToString(", ")}")
        }
        val allergies = profile?.allergies?.filter { it.isNotBlank() }.orEmpty()
        if (allergies.isNotEmpty()) {
            appendLine("Allergie e intolleranze: ${allergies.joinToString(", ")}")
        }
    }

    /** Istruzione di correzione: dice cosa non andava, senza ripetere tutto il pasto. */
    private fun correctionPrompt(problem: String, entries: List<MealEntry>): String = buildString {
        appendLine("La risposta precedente non e' utilizzabile: $problem")
        appendLine("Rispondi di nuovo usando SOLO questo JSON, senza testo prima o dopo:")
        appendLine(
            """{"nome":"...","ingredienti":[""" +
                entries.joinToString(",") { "\"${it.foodName}\"" } +
                """],"passi":["...","..."]}"""
        )
        appendLine("Gli ingredienti devono essere esattamente questi, con questi nomi:")
        appendLine(entries.joinToString(", ") { it.foodName })
    }

    private sealed interface ParseOutcome {
        data class Ok(val recipe: ComposedRecipe) : ParseOutcome
        data class Invalid(val reason: String) : ParseOutcome
    }

    /**
     * Estrae nome e passi dalla risposta. Il modello non decide gli ingredienti:
     * quelli sono del piano. Si controlla solo che non abbia introdotto alimenti
     * estranei, e in quel caso si spiega cosa correggere.
     */
    private fun parse(
        content: String,
        entries: List<MealEntry>,
        slot: MealSlot,
        style: RecipeStyle
    ): ParseOutcome {
        val obj = extractJson(content)
            ?: return ParseOutcome.Invalid("la risposta non contiene un oggetto JSON valido")

        val name = obj["nome"].asText()
            ?: return ParseOutcome.Invalid("manca il campo \"nome\"")
        val steps = obj["passi"].asTextList()
        if (steps.size < MIN_STEPS) {
            return ParseOutcome.Invalid(
                "il campo \"passi\" deve contenere almeno $MIN_STEPS passi (trovati ${steps.size})"
            )
        }

        val allowed = entries.map { it.foodName }
        val mentioned = obj["ingredienti"].asTextList()
        val foreign = mentioned.filter { !refersToKnownFood(it, allowed) }
        if (foreign.isNotEmpty()) {
            return ParseOutcome.Invalid(
                "sono stati aggiunti alimenti non previsti dal pasto: ${foreign.joinToString(", ")}"
            )
        }

        return ParseOutcome.Ok(
            ComposedRecipe(
                slot = slot,
                name = name,
                ingredients = entries,
                style = style,
                steps = steps
            )
        )
    }

    /**
     * Il JSON puo arrivare dentro un blocco markdown o preceduto da una frase:
     * si isola il primo oggetto bilanciato invece di fidarsi della prima graffa.
     * Le graffe dentro le stringhe non contano.
     */
    private fun extractJson(raw: String): JsonObject? {
        val text = raw.trim()
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> depth++
                c == '}' -> {
                    depth--
                    if (depth == 0) {
                        return runCatching {
                            json.parseToJsonElement(text.substring(start, i + 1)).jsonObject
                        }.getOrNull()
                    }
                }
            }
        }
        return null
    }

    private fun normalize(s: String) = s.lowercase().trim()

    /**
     * True se [mentioned] indica uno degli alimenti del pasto.
     *
     * Il confronto non puo' essere testuale esatto: il piano scrive "Pollo, petto"
     * e il modello risponde "petto di pollo". Si confrontano quindi le parole
     * significative, ignorando articoli, qualificatori di stato e le parole
     * troppo corte per identificare un alimento.
     */
    private fun refersToKnownFood(mentioned: String, allowed: List<String>): Boolean {
        val mentionedTokens = significantTokens(mentioned)
        if (mentionedTokens.isEmpty()) return false
        return allowed.any { name ->
            val allowedTokens = significantTokens(name)
            if (allowedTokens.isEmpty()) return false
            val overlap = mentionedTokens.count { it in allowedTokens }
            // Almeno una parola chiave in comune, oppure il nome intero contenuto
            // nell'altro (per i nomi composti da una sola parola chiave).
            overlap > 0 ||
                normalize(name).contains(normalize(mentioned)) ||
                normalize(mentioned).contains(normalize(name))
        }
    }

    private fun significantTokens(s: String): Set<String> =
        normalize(s)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= MIN_TOKEN_LENGTH && it !in IGNORED_TOKENS }
            .toSet()

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

    private fun JsonElement?.asText(): String? =
        (this as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }

    private fun JsonElement?.asTextList(): List<String> =
        (this as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private companion object {
        /** Tentativi di correzione dopo il primo: 1 iniziale + 2 correzioni. */
        const val MAX_CORRECTIONS = 2
        const val MIN_STEPS = 2
        const val RECIPE_TEMPERATURE = 0.7

        /**
         * Una ricetta sono poche centinaia di token. Il budget deve pero'
         * coprire i token di ragionamento dei modelli che li consumano prima di
         * scrivere: con un limite stretto la risposta arriverebbe troncata
         * (`finish_reason=length`) e verrebbe scartata come non interpretabile.
         */
        const val RECIPE_MAX_TOKENS = 2_000

        /** Sotto questa lunghezza una parola non identifica un alimento. */
        const val MIN_TOKEN_LENGTH = 3

        /**
         * Parole che ricorrono in quasi tutti i nomi di alimento o che indicano
         * uno stato di preparazione: non servono a riconoscere l'alimento.
         */
        val IGNORED_TOKENS = setOf(
            "di", "del", "della", "dei", "delle", "con", "senza", "alla", "allo",
            "al", "il", "lo", "la", "le", "gli", "un", "una", "uno", "e", "ed",
            "crudo", "cruda", "cotto", "cotta", "fresco", "fresca", "freschi",
            "fresche", "lesso", "lessa", "intero", "intera", "parzialmente",
            "scremato", "scremata", "magro", "magra", "biologico", "biologica"
        )

        val SYSTEM_PROMPT = """
            Sei un cuoco che scrive ricette per un piano alimentare gia' calcolato.

            Regole obbligatorie:
            1. Usa SOLO gli ingredienti elencati. Non aggiungere altri alimenti,
               nemmeno condimenti, sale, spezie, olio o liquidi non elencati.
            2. Non cambiare le grammature. Le quantita' sono gia' quelle del piano.
            3. Scrivi una preparazione REALE e specifica per questi ingredienti:
               temperature, tempi, ordine delle operazioni, tipo di cottura.
            4. Se un ingrediente si mangia crudo, dillo esplicitamente.
            5. 3-6 passi brevi, ognuno un'azione concreta. Vietate le frasi
               generiche tipo "cuoci la fonte proteica" o "componi il piatto".
            6. Non inventare valori nutrizionali e non citare kcal o macro: sono
               calcolati dall'app a partire dalle grammature.
            7. Se il pasto contiene pochi ingredienti, scrivi una ricetta semplice
               ma comunque reale (es. come tagliare, condire, impiattare).

            Formato della risposta: SOLO un oggetto JSON valido, senza testo prima
            o dopo e senza blocchi di codice markdown.
            {"nome":"Nome del piatto","ingredienti":["nome alimento"],"passi":["passo 1","passo 2"]}
            Nel campo "ingredienti" ripeti i nomi degli alimenti ricevuti, senza aggiungerne.
        """.trimIndent()
    }
}
