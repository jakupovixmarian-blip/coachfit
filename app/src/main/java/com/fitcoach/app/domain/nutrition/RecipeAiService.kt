package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.domain.coach.AiConfigResolver
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Esito della generazione: la ricetta e il motivo per cui e' stata scelta quella strada. */
sealed interface RecipeAiResult {
    data class Success(val recipe: ComposedRecipe, val model: String?) : RecipeAiResult

    /** AI non configurata o non raggiungibile: si usa il compositore deterministico. */
    data class Fallback(val recipe: ComposedRecipe, val reason: String) : RecipeAiResult

    /** Nessun percorso ha prodotto una ricetta utilizzabile. */
    data class Failed(val message: String) : RecipeAiResult
}

/**
 * Scrive la ricetta vera e propria con l'AI, partendo dagli alimenti e dalle
 * grammature che il piano ha gia' assegnato a quel pasto.
 *
 * Perche' serve l'AI: il [RecipeComposer] deterministico produce passi fissi
 * ("cuoci la fonte proteica alla piastra"), uguali per ogni piatto. Il risultato
 * e' corretto ma non e' una ricetta. Qui il modello scrive nome del piatto e
 * preparazione reale, mentre i numeri restano quelli del piano.
 *
 * Regole non negoziabili, applicate a valle della risposta:
 * - gli ingredienti sono SOLO quelli del pasto, con le grammature del piano;
 * - kcal e macro non vengono presi dal modello ma ricalcolati dagli ingredienti;
 * - se il modello propone un ingrediente diverso, la risposta viene scartata.
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
        profile: UserProfile?,
        fallback: () -> ComposedRecipe?
    ): RecipeAiResult {
        val usable = entries.filter { it.grams > 0 }
        if (usable.size < 2) {
            val recipe = fallback()
                ?: return RecipeAiResult.Failed("Servono almeno due alimenti per creare una ricetta.")
            return RecipeAiResult.Fallback(recipe, "Troppo pochi alimenti: ricetta composta senza AI.")
        }

        val config = configResolver.resolveConfigured()
            ?: return fallback(usable, slot, style, profile, fallback, "Provider AI non configurato")

        val result = aiClient.complete(
            config = config,
            messages = listOf(
                ChatMessageDto(role = "system", content = SYSTEM_PROMPT),
                ChatMessageDto(role = "user", content = userPrompt(usable, slot, style, profile))
            ),
            temperature = 0.7
        )

        if (result is AiResult.Failure) {
            return fallback(usable, slot, style, profile, fallback, result.message)
        }
        val content = (result as AiResult.Success).content
        val parsed = parse(content, usable, slot, style)
            ?: return fallback(usable, slot, style, profile, fallback, "Risposta AI non interpretabile")

        return RecipeAiResult.Success(parsed, result.model ?: config.model)
    }

    private fun fallback(
        entries: List<MealEntry>,
        slot: MealSlot,
        style: RecipeStyle,
        profile: UserProfile?,
        composer: () -> ComposedRecipe?,
        reason: String
    ): RecipeAiResult {
        val recipe = composer()
            ?: return RecipeAiResult.Failed("Impossibile comporre la ricetta: $reason")
        val short = reason.take(120)
        return RecipeAiResult.Fallback(recipe, short)
    }

    private fun userPrompt(
        entries: List<MealEntry>,
        slot: MealSlot,
        style: RecipeStyle,
        profile: UserProfile?
    ): String = buildString {
        appendLine("Pasto: ${slot.label}")
        appendLine("Stile richiesto: ${style.label}")
        appendLine("Ingredienti disponibili, con le grammature del piano (non modificabili):")
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

    /**
     * Estrae nome e passi dalla risposta. Qualsiasi ingrediente non previsto dal
     * pasto invalida la ricetta: meglio il fallback che una ricetta con alimenti
     * che l'utente non ha.
     */
    private fun parse(
        content: String,
        entries: List<MealEntry>,
        slot: MealSlot,
        style: RecipeStyle
    ): ComposedRecipe? {
        val obj = extractJson(content) ?: return null
        val name = obj["nome"].asText().orEmpty()
        val steps = obj["passi"].asTextList()

        if (name.isBlank() || steps.size < 2) return null

        // Il modello non decide gli ingredienti: quelli sono del piano. Si
        // controlla solo che non abbia introdotto alimenti estranei.
        val allowed = entries.map { normalize(it.foodName) }.toSet()
        val mentioned = obj["ingredienti"].asTextList()
        val foreign = mentioned.filter { m ->
            val n = normalize(m)
            allowed.none { a -> a.contains(n) || n.contains(a) }
        }
        if (foreign.isNotEmpty()) return null

        return ComposedRecipe(
            slot = slot,
            name = name,
            ingredients = entries,
            style = style,
            steps = steps
        )
    }

    private fun extractJson(raw: String): JsonObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject }.getOrNull()
    }

    private fun normalize(s: String) = s.lowercase().trim()

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

    /** Testo di un campo, o null se il campo manca o non e' una stringa. */
    private fun kotlinx.serialization.json.JsonElement?.asText(): String? =
        (this as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }

    /** Lista di stringhe, ignorando le voci vuote o di tipo diverso. */
    private fun kotlinx.serialization.json.JsonElement?.asTextList(): List<String> =
        (this as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private companion object {
        val SYSTEM_PROMPT = """
            Sei un cuoco che scrive ricette per un piano alimentare gia' calcolato.

            Regole obbligatorie:
            1. Usa SOLO gli ingredienti elencati. Non aggiungere altri alimenti,
               nemmeno condimenti, sale, spezie o liquidi non elencati.
            2. Non cambiare le grammature. Le quantita' sono gia' quelle del piano.
            3. Scrivi una preparazione REALE e specifica per questi ingredienti:
               temperature, tempi, ordine delle operazioni, tipo di cottura.
            4. Se un ingrediente si mangia crudo, dillo esplicitamente.
            5. 3-6 passi brevi, ognuno un'azione concreta. Vietate le frasi
               generiche tipo "cuoci la fonte proteica" o "componi il piatto".
            6. Non inventare valori nutrizionali e non citare kcal o macro.
            7. Se il pasto contiene pochi ingredienti, scrivi una ricetta semplice
               ma comunque reale (es. come tagliare, condire, impiattare).

            Rispondi SOLO con JSON valido, senza testo prima o dopo:
            {"nome":"...","ingredienti":["..."],"passi":["...","..."]}
        """.trimIndent()
    }
}
