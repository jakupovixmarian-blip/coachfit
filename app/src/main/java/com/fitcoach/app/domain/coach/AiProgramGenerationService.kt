package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.knowledge.CorpusPrompt
import com.fitcoach.app.domain.knowledge.InvictusKnowledge
import com.fitcoach.app.domain.knowledge.KnowledgeCorpus
import com.fitcoach.app.domain.nutrition.BreakfastConstraint
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
import com.fitcoach.app.domain.nutrition.MealSlots
import com.fitcoach.app.domain.workout.EquipmentFit
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generazione del percorso iniziale tramite il Coach AI.
 *
 * Non esiste piu un percorso di generazione separato: questo servizio apre un
 * turno agentico su [CoachOrchestrator] (la stessa autorita decisionale della chat)
 * e chiede all'AI di consegnare il programma completo con lo strumento
 * [SubmitFullProgramTool]. La verifica matematica e di sicurezza resta affidata
 * a [ProgramGenerationEngine], che puo bloccare la proposta ma non ne genera una
 * alternativa. Gli stessi dati, strumenti, memoria e regole valgono per la
 * generazione iniziale e per le modifiche successive.
 */
@Singleton
class AiProgramGenerationService @Inject constructor(
    private val orchestrator: CoachOrchestrator,
    private val chatRepository: ChatRepository,
    private val secureKeyStore: SecureKeyStore,
    private val json: Json,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository
) {
    sealed interface Result {
        data class Success(
            val nutritionPlanId: Long,
            val workoutPlanId: Long,
            val warnings: List<String> = emptyList(),
            /**
             * Strumenti effettivamente chiamati dal modello, in ordine.
             *
             * Serve a distinguere "il piano e buono" da "il piano e buono perche
             * il modello ha consultato il corpus": senza questa traccia non c'e
             * modo di sapere se una fonte e stata letta o solo citata.
             */
            val toolTrace: List<String> = emptyList(),
            /**
             * Output di ogni strumento, nell'ordine. Rende leggibile il PERCHE'
             * di un secondo submit: senza, `[submit, submit]` e' indistinguibile
             * da un doppio salvataggio.
             */
            val toolResults: List<CoachToolResult> = emptyList(),
            val lastFinishReason: String? = null
        ) : Result

        /**
         * [rawResponse] e la risposta integrale del modello quando la consegna e
         * stata rifiutata. Serve a capire se l'errore nasce dal modello, dal
         * prompt o dal validatore: senza il dato grezzo ogni correzione e un
         * tentativo alla cieca.
         */
        data class Failure(
            val message: String,
            val canRetry: Boolean = true,
            val rawResponse: String? = null,
            /**
             * Strumenti chiamati prima del fallimento. Senza, "il modello non ha
             * consegnato" non distingue un modello che non ha chiamato nulla da
             * uno che ha consultato e poi si e' fermato: la correzione e'
             * opposta nei due casi.
             */
            val toolTrace: List<String> = emptyList(),
            val toolResults: List<CoachToolResult> = emptyList(),
            val lastFinishReason: String? = null
        ) : Result
    }

    suspend fun generate(
        profile: UserProfile,
        scope: ProgramGenerationEngine.Scope = ProgramGenerationEngine.Scope.BOTH,
        /**
         * Richiesta esplicita dell'utente (es. "aggiungi tecniche d'intensita' e
         * isolamento, riduci le serie"). Senza questo canale la rigenerazione
         * ripartiva dagli stessi dati di anamnesi e produceva lo stesso piano:
         * l'utente confermava una modifica e ritrovava la scheda identica.
         */
        directive: String? = null,
        /** Avanzamento leggibile, mostrato all'utente durante l'attesa. */
        onProgress: ((String) -> Unit)? = null
    ): Result {
        if (!profile.isAnamnesisReady) {
            return Result.Failure("Completa e conferma l'anamnesi prima di generare il programma.", false)
        }
        val foods = FoodCatalogSelector.forGeneration(foodRepository.getAll(), profile)
        val allExercises = exerciseRepository.getAll()
        if (foods.isEmpty()) return Result.Failure("Nessun alimento compatibile disponibile nel database.", false)
        if (allExercises.isEmpty()) return Result.Failure("Database esercizi vuoto: impossibile validare la scheda.", false)

        // Il modello riceve SOLO gli esercizi prescrivibili per questo profilo:
        // passargli la lista intera con l'etichetta "ESERCIZI CONSENTITI" lo
        // induceva a prescrivere esercizi incompatibili con la sua attrezzatura.
        // La validazione usa la stessa lista, cosi' cio' che vede coincide con
        // cio' che viene accettato.
        val exercises = EquipmentFit.allowedFor(profile, allExercises)
        if (exercises.isEmpty()) {
            return Result.Failure(
                "Nessun esercizio compatibile con l'attrezzatura e le limitazioni dichiarate. " +
                    "Aggiungi l'attrezzatura disponibile nel profilo.",
                false
            )
        }

        val config = resolveConfig()
            ?: return Result.Failure(
                "Provider AI non configurato. Inserisci una chiave o configura il backend proxy nelle Impostazioni."
            )

        val contextPrompt = buildGenerationContext(profile, foods, exercises, scope, directive)
        // Il tetto di tempo vive nell'orchestratore: deadline tra le iterazioni e
        // budget per la singola chiamata. Qui non serve un timeout aggiuntivo.
        val turn = orchestrator.run(
            config = config,
            systemPrompt = generationSystemPrompt,
            contextPrompt = contextPrompt,
            history = emptyList(),
            userMessage = "Genera ora la parte richiesta del mio programma e consegnala con " +
                "submit_full_program (scope=${scope.name}). Non fermarti prima: devi arrivare a ok=true." +
                (directive?.takeIf { it.isNotBlank() }
                    ?.let { " RISPETTA QUESTA RICHIESTA DELL'UTENTE, che prevale sulle scelte " +
                        "precedenti a parita' di vincoli di sicurezza: $it" }
                    ?: ""),
            maxIterations = GENERATION_MAX_ITERATIONS,
            requireTool = SUBMIT_TOOL,
            requiredPrerequisites = listOf(
                Prerequisite.consultations(
                    toolName = CONSULT_TOOL,
                    // Le aree seguono lo scope: chiedere le regole di nutrizione per
                    // generare solo la scheda bruciava iterazioni su un vincolo che
                    // non serviva al risultato richiesto.
                    areas = when (scope) {
                        ProgramGenerationEngine.Scope.NUTRITION -> listOf("nutrizione")
                        ProgramGenerationEngine.Scope.WORKOUT -> listOf("allenamento", "tecniche")
                        ProgramGenerationEngine.Scope.BOTH ->
                            listOf("allenamento", "nutrizione", "tecniche")
                    },
                    description = "split, volume, RIR e recuperi per la scheda; calorie, " +
                        "proteine e carboidrati per il piano alimentare; tecniche d'intensita " +
                        "(drop set, rest-pause, myo-reps) e quando applicarle."
                )
            ),
            deadlineMs = GENERATION_TIMEOUT_MS,
            onProgress = onProgress
        )

        val submission = turn.toolResults.lastOrNull { it.name == SUBMIT_TOOL }
            ?: return Result.Failure(
                turn.providerError
                    ?: "Il Coach AI non ha consegnato il programma. Riprova: se il problema persiste, " +
                    "verifica il provider AI o prova un modello con il supporto agli strumenti.",
                rawResponse = turn.text.takeIf { it.isNotBlank() },
                toolTrace = turn.toolTrace,
                toolResults = turn.toolResults,
                lastFinishReason = turn.lastFinishReason
            )

        return interpretSubmission(
            submission.output,
            turn.toolTrace,
            turn.toolResults,
            turn.lastFinishReason
        )
    }

    private fun interpretSubmission(
        output: String,
        toolTrace: List<String> = emptyList(),
        toolResults: List<CoachToolResult> = emptyList(),
        lastFinishReason: String? = null
    ): Result {
        val obj = runCatching { json.parseToJsonElement(output) as? JsonObject }.getOrNull()
            ?: return Result.Failure("Risposta dello strumento non leggibile. Riprova.")
        val ok = (obj["ok"] as? JsonPrimitive)?.booleanOrNull ?: false
        if (!ok) {
            val violations = (obj["violazioni"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.content }
                ?: emptyList()
            val error = (obj["error"] as? JsonPrimitive)?.content
            val message = when {
                error != null -> error
                violations.isNotEmpty() -> "Il programma AI non e valido: " +
                    violations.joinToString(separator = " ", prefix = "Controlli falliti: ")
                else -> "Il programma AI non e valido e non e stato salvato."
            }
            // La traccia va propagata anche qui: senza, un programma respinto
            // dalla validazione arrivava alla diagnosi con `strumenti=[]` e non
            // si poteva distinguere "il modello non ha consultato" da "ha
            // consultato e ha sbagliato i numeri".
            return Result.Failure(
                message,
                rawResponse = output,
                toolTrace = toolTrace,
                toolResults = toolResults,
                lastFinishReason = lastFinishReason
            )
        }
        val nutritionId = (obj["nutritionPlanId"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
        val workoutId = (obj["workoutPlanId"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
        val warnings = (obj["avvisi"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            ?: emptyList()
        return Result.Success(nutritionId, workoutId, warnings, toolTrace, toolResults)
    }

    /**
     * True se il provider AI e utilizzabile adesso (base URL, modello e chiave
     * o proxy). Serve alle schermate per avvisare PRIMA di lanciare un turno:
     * un turno senza provider non puo riuscire, e l'attesa non ha senso.
     */
    suspend fun isProviderReady(): Boolean = resolveConfig() != null

    private suspend fun resolveConfig(): AiProviderConfig? {
        val settings = chatRepository.getAiSettings()
        val baseUrl = if (settings.useBackendProxy && settings.backendBaseUrl.isNotBlank()) {
            settings.backendBaseUrl
        } else settings.baseUrl.ifBlank { settings.provider.defaultBaseUrl }
        val key = if (settings.useBackendProxy) null else secureKeyStore.getApiKey(settings.provider.name)
        val config = AiProviderConfig(
            provider = settings.provider,
            baseUrl = baseUrl,
            model = settings.model.ifBlank { settings.provider.defaultModel },
            apiKey = key,
            temperature = settings.temperature,
            // Il payload di submit_full_program (7 giorni + scheda) e molto grande.
            // Sui modelli di ragionamento (gpt-5.x, o-series) il budget include i
            // token di ragionamento: con un tetto basso la chiamata viene troncata
            // e il programma non arriva. Serve un budget ampio.
            maxTokens = maxOf(settings.maxTokens, GENERATION_MAX_TOKENS),
            usesBackendProxy = settings.useBackendProxy
        )
        return config.takeIf { it.isConfigured }
    }

    internal fun buildGenerationContext(
        profile: UserProfile,
        foods: List<FoodItem>,
        exercises: List<Exercise>,
        scope: ProgramGenerationEngine.Scope,
        directive: String? = null
    ): String = buildString {
        appendLine("Genera la parte richiesta del programma COMPLETO per FitCoach usando SOLO gli identificativi testuali elencati.")
        appendLine("AMBITO RICHIESTO: ${scope.name} " + when (scope) {
            ProgramGenerationEngine.Scope.BOTH -> "(nutrizione + allenamento)"
            ProgramGenerationEngine.Scope.NUTRITION -> "(SOLO nutrizione: compila comunque i giorni di allenamento e il calendario, ma verranno usati solo per i controlli)"
            ProgramGenerationEngine.Scope.WORKOUT -> "(SOLO allenamento: compila comunque la nutrizione, ma verrà usata solo per i controlli)"
        })
        directive?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("RICHIESTA ESPLICITA DELL'UTENTE (vincolante, entro i limiti di sicurezza):")
            appendLine("  $it")
            appendLine("  Il piano deve differire dal precedente proprio su questo punto. Se un vincolo")
            appendLine("  di sicurezza impedisce di soddisfarla, dichiaralo in warnings invece di ignorarla.")
            appendLine()
        }
        appendLine("PASTI OBBLIGATORI (esattamente questi, in quest'ordine, ogni giorno):")
        appendLine("  " + MealSlots.describe(profile.mealsPerDay))
        appendLine("DATI ANAMNESI CONFERMATI:")
        appendLine(AnamnesisSummary.forProfile(profile))
        append(declaredHeader(profile))
        appendLine("ALIMENTI CONSENTITI (nome | kcal P C F per 100g):")
        // Nessun secondo troncamento: `foods` e' gia' selezionato e limitato da
        // FoodCatalogSelector. Un take(90) qui tagliava di nuovo la lista in
        // ordine alfabetico, lasciando fuori proprio gli alimenti base che
        // servono a comporre un piano (Petto di pollo, Riso, Pasta, Uova,
        // Yogurt, Olio stavano oltre la posizione 90 del catalogo).
        foods.forEach { f ->
            appendLine("${f.name} | ${f.kcalPer100g} ${f.proteinPer100g} ${f.carbsPer100g} ${f.fatPer100g} | unità ${f.defaultUnit.name}")
        }
        // "CONSENTITI" era falso: la lista non era filtrata. Ora lo e', quindi
        // l'etichetta descrive davvero il contenuto e il modello non ha motivi
        // per cercare alternative fuori elenco.
        appendLine(
            "ESERCIZI CONSENTITI (nome | muscolo | attrezzatura | range | recupero | RIR). " +
                "Questa e' la lista COMPLETA degli esercizi prescrivibili per questo utente: " +
                "sono gia' filtrati per attrezzatura, limitazioni, esercizi esclusi e livello " +
                "tecnico. Usa SOLO nomi presenti qui: qualunque altro nome viene rifiutato."
        )
        exercises.take(120).forEach { e ->
            appendLine("${e.name} | ${e.primaryMuscle.name} | ${e.equipment.joinToString { it.name }} | ${e.minReps}-${e.maxReps} | ${e.restSeconds}s | ${e.targetRir}")
        }
        // Senza queste tre righe il modello non sapeva di poter prescrivere
        // tecniche d'intensita (il campo esisteva nello schema ma nessun prompt
        // lo citava), ne che la seduta dovesse contenere lavoro di isolamento:
        // produceva schede di soli multiarticolari con "technique: NONE".
        appendLine(
            "TECNICHE D'INTENSITA (campo 'technique' di ogni esercizio). Valori ammessi: " +
                "NONE, DROP_SET, REST_PAUSE, MYO_REPS, SUPERSET, BACK_OFF_SET, CLUSTER_SET, " +
                "FAILURE, ECCENTRIC_EMPHASIS. Usale dove hanno senso metodologico e dichiara " +
                "sempre in 'techniqueReason' perche' su QUEL gruppo muscolare e QUEL livello: " +
                "la ragione viene mostrata nella scheda. Non metterle su tutti gli esercizi."
        )
        appendLine(
            "COMPOSIZIONE DI OGNI SEDUTA (verificata al salvataggio): ogni giorno di allenamento " +
                "deve contenere almeno un esercizio ISOLATION oltre ai multiarticolari. Una scheda " +
                "di soli multiarticolari non copre i distretti che restano indietro."
        )
        appendLine(
            "ISOLAMENTO PER OBIETTIVO: i multiarticolari fanno il carico, l'isolamento completa il " +
                "lavoro sui capi muscolari e sui gruppi che il multiarticolare non stimola a " +
                "sufficienza (deltoide laterale e posteriore, bicipiti, tricipiti, femorali, polpacci). " +
                "Non sostituire un multiarticolare con un isolamento: aggiungilo."
        )
        val energy = EnergyCalculator.compute(profile)
        if (energy != null) {
            val cycling = if (CarbCyclingEngine.shouldApplyCarbCycling(profile)) {
                CarbCyclingEngine.build(profile, energy)
            } else {
                CarbCyclingEngine.buildUniform(profile, energy)
            }
            appendLine("TARGET NUTRIZIONALI VINCOLANTI (verificati al salvataggio, non negoziabili):")
            cycling.targets.forEach { (type, t) ->
                appendLine(
                    "  ${type.name}: ${t.kcal.roundToInt()} kcal | ${t.protein.roundToInt()} g proteine | " +
                        "${t.carbs.roundToInt()} g carboidrati | ${t.fat.roundToInt()} g grassi"
                )
            }
            appendLine("  Settimanale: ${cycling.weeklyKcal.roundToInt()} kcal; " +
                "giornate HIGH=${cycling.highDays}, MEDIUM=${cycling.mediumDays}, LOW=${cycling.lowDays}")
            appendLine(
                "PROCEDURA OBBLIGATORIA per la nutrizione: per OGNI giorno chiama solve_meal_portions " +
                    "scegliendo gli alimenti (deve esserci almeno una fonte proteica, una di carboidrati " +
                    "e una di grassi) e usa le grammature che restituisce. Non calcolare i grammi a mano: " +
                    "il solver usa i valori reali del catalogo. Se restituisce ok=false, cambia gli alimenti " +
                    "e ripeti."
            )
        }
        appendLine("Il risultato deve avere esattamente 7 giorni nutrizionali e ${profile.trainingDaysPerWeek} giorni di allenamento.")
        appendLine(
            "PRIMA di consegnare consulta la metodologia con consult_invictus: almeno una chiamata " +
                "con area=allenamento e una con area=nutrizione, sugli argomenti che stai decidendo " +
                "(split, volume, RIR, recuperi per la scheda; calorie, proteine, carboidrati per il piano). " +
                "Non e' un consiglio: submit_full_program viene BLOCCATO dal codice finche' entrambe " +
                "le aree non hanno restituito regole. Una chiamata con area giusta ma argomento di " +
                "un'altra area non conta. I numeri del corpus non sono nel prompt: se non li leggi li stai inventando."
        )
        appendLine("Consegnalo con lo strumento submit_full_program: la verifica e il salvataggio avvengono li.")
        appendLine()
        appendLine(KnowledgeCorpus.promptBlock())
        appendLine()
        // Il corpus Invictus non e' piu' solo una libreria consultabile: le regole
        // delle categorie pertinenti allo scope arrivano nel prompt, cosi' il
        // modello non puo' generare "senza aver visto la fonte". Se una categoria
        // resta fuori per budget, il blocco lo dichiara.
        appendLine(
            CorpusPrompt.block(
                when (scope) {
                    ProgramGenerationEngine.Scope.NUTRITION -> listOf(InvictusKnowledge.Area.NUTRIZIONE)
                    ProgramGenerationEngine.Scope.WORKOUT -> listOf(InvictusKnowledge.Area.ALLENAMENTO)
                    ProgramGenerationEngine.Scope.BOTH -> InvictusKnowledge.Area.entries.toList()
                }
            )
        )
    }

    companion object {
        /**
         * Riga con obiettivo, giorni, pasti, carichi e attrezzatura.
         *
         * Usa [AnamnesisSummary.declared]: ripetere qui i valori del profilo come
         * certi (obiettivo RECOMP, 5 pasti) contraddiceva il riepilogo, che li
         * dichiara non disponibili quando l'utente non li ha confermati, e
         * lasciava all'AI la scelta tra due istruzioni opposte.
         */
        internal fun declaredHeader(profile: UserProfile): String = buildString {
            appendLine(
                "OBIETTIVO: ${AnamnesisSummary.declared(profile, "goal", profile.goal, "NON_DICHIARATO")}"
            )
            // I giorni di allenamento NON sono una preferenza: il validatore
            // rifiuta il programma se i giorni pianificati non sono esattamente
            // trainingDaysPerWeek. Dichiararli "non disponibili" mentre il
            // conteggio resta obbligatorio renderebbe il vincolo incomprensibile.
            appendLine(
                "GIORNI DI ALLENAMENTO (vincolo: esattamente ${profile.trainingDaysPerWeek} " +
                    "sedute non di riposo, altrimenti il programma e respinto)"
            )
            appendLine(
                "PASTI AL GIORNO: " +
                    AnamnesisSummary.declared(profile, "mealsPerDay", profile.mealsPerDay, "NON_DICHIARATI (usa 5)")
            )
            // Il modello non riceveva affatto questa preferenza: generava pollo
            // e riso a colazione pur avendo l'utente scelto "dolce".
            appendLine(
                "COLORE DELLA COLAZIONE (vincolo verificato): " +
                    BreakfastConstraint.hint(profile.breakfastPreference)
            )
            appendLine(
                "CARICHI DICHIARATI: " + if (profile.currentLoads.isEmpty()) "NON_DICHIARATI"
                else profile.currentLoads.entries.joinToString { "${it.key}=${it.value} kg" }
            )
            // Senza questa riga l'elenco dei graditi era solo informativo: il
            // modello componeva pasti standard (pollo, riso, uova) anche quando
            // l'utente aveva dichiarato cosa mangia davvero.
            if (profile.likedFoods.isNotEmpty()) {
                appendLine(
                    "ALIMENTI GRADITI (usali come base delle fonti principali, non ignorarli): " +
                        profile.likedFoods.joinToString()
                )
            }
            if (profile.dislikedFoods.isNotEmpty()) {
                appendLine(
                    "ALIMENTI NON GRADITI (non usarli se esiste un'alternativa compatibile): " +
                        profile.dislikedFoods.joinToString()
                )
            }
            appendLine(
                "ATTREZZATURA (categorie): " + profile.availableEquipment.joinToString { it.name }
                    .ifBlank { "NON_DICHIARATA" }
            )
            appendLine(
                "INVENTARIO ATTREZZATURA REALE: " +
                    profile.gearInventory.summary().ifBlank { "NON_COMPILATO" }
            )
            if (!profile.gearInventory.isEmpty) {
                appendLine(
                    "VINCOLO: prescrivi solo esercizi eseguibili con l'inventario qui sopra. " +
                        "Se un esercizio richiede un attrezzo assente, scegli un'alternativa " +
                        "compatibile con lo stesso gruppo muscolare e dichiarala."
                )
            }
        }

        private const val SUBMIT_TOOL = "submit_full_program"
        private const val GENERATION_MAX_ITERATIONS = 8
        private const val CONSULT_TOOL = "consult_invictus"

        /** Tetto complessivo del turno di generazione (include retry e piu iterazioni). */
        private const val GENERATION_TIMEOUT_MS = 6 * 60 * 1000L

        /**
         * Budget token per la consegna del programma. Deve coprire sia i token di
         * ragionamento (modelli gpt-5.x/o-series) sia l'oggetto JSON completo di
         * 7 giorni + scheda, altrimenti la chiamata viene troncata.
         */
        private const val GENERATION_MAX_TOKENS = 32_000

        val generationSystemPrompt = """
            Sei il Coach AI di FitCoach. Sei l'unica autorita decisionale: costruisci un percorso
            PERSONALIZZATO, non un modello generico, coerente con alimentazione, allenamento, recupero
            e progressione.

            METODO
            1. Leggi i dati reali con gli strumenti se ti servono (get_anamnesis, list_foods,
               list_exercises, compute_energy_targets, compute_carb_cycling, compute_weekly_volume,
               compute_portion, validate_nutrition_day, validate_workout_plan,
               get_nutrition_targets, solve_meal_portions).

            1b. La metodologia di Project InVictus sta in consult_invictus, non nel tuo contesto.
               Interrogalo PRIMA di decidere i parametri strutturali, non dopo: area
               "allenamento" per split, frequenza, volume settimanale, RIR, recuperi,
               progressione, periodizzazione, deload, tecniche; area "nutrizione" per
               proteine, carboidrati, grassi, fibre, timing, ricariche, numero di pasti.
               Ogni regola torna con un numero e la fonte: usa quei numeri invece di
               stimarli. Cerca per parametro (argomento="volume", "proteine", "deload"),
               non per parola generica.
            2. Costruisci 7 giorni nutrizionali con carb cycling (HIGH nei giorni piu intensi, MEDIUM,
               LOW nei giorni di riposo) e una scheda periodizzata con esattamente i giorni dichiarati.
            3. Per la nutrizione: consulta get_nutrition_targets, poi per OGNI giorno chiama
               solve_meal_portions con gli alimenti scelti. Il solver calcola le grammature sui
               target reali. Non stimare i grammi a mano e non inventare i target.
            4. Verifica la scheda con validate_workout_plan e le giornate con validate_nutrition_day.
            5. Consegnala con submit_full_program (una sola chiamata, oggetto completo). Se ricevi
               ok=false, correggi SOLO i valori in 'violazioni' e richiama lo strumento.

            VINCOLI
            - Usa solo alimenti ed esercizi dei cataloghi. Non inventare identificativi, misure,
              alimenti o esercizi.
            - COPIA I NOMI ESATTAMENTE come compaiono nel catalogo, carattere per carattere.
              Vietato abbreviare, tradurre, declinare o riassumere: "Insalata mista" non esiste,
              il catalogo dice "Insalata lattuga"; "Trancio di tonno" non esiste, il catalogo dice
              "Tonno fresco". Se un alimento che vorresti non c'e', scegline un altro dalla lista.
              Un nome inventato fa rifiutare l'intero programma.
            - Rispetta allergie, intolleranze, alimenti esclusi, limitazioni, attrezzatura, esperienza,
              recupero e budget.
            - `dayType` deve essere HIGH, MEDIUM o LOW; `slot` un valore MealSlot tra quelli
              indicati nel contesto; `unit` RAW, COOKED o AS_SOLD; `split` FULL_BODY,
              UPPER_LOWER, PUSH_PULL_LEGS o CUSTOM.
            - Ogni giorno nutrizionale ha ESATTAMENTE gli slot indicati nel contesto: ne' uno
              in piu' ne' uno in meno. Un pasto mancante o aggiunto fa rifiutare il programma.
            - Ogni pasto con almeno un alimento, ogni alimento da 1 a 2000 grammi.
              Esattamente 7 giorni con dayIndex 0..6.
            - Ogni esercizio: `sets` da 1 a 8, `minReps`/`maxReps` da 1 a 30 (con minReps<=maxReps),
              `restSeconds` da 15 a 600, `targetRir` da 0 a 5. Sono tutti obbligatori: non ometterli.
            - Ogni seduta: 6-24 serie allenanti totali; lo stesso esercizio non oltre 24 serie
              settimanali; nessun esercizio vietato. Giornate diverse quando lo split lo consente.
              Non assegnare una serie completa ai muscoli secondari.
            - Ogni seduta deve contenere ALMENO UN esercizio ISOLATION oltre ai multiarticolari.
              Una seduta di soli multiarticolari viene respinta: aggiungi l'isolamento per i
              gruppi che il multiarticolare non stimola (deltoide laterale e posteriore, bicipiti,
              tricipiti, femorali, polpacci), non sostituire un multiarticolare.
            - Le tecniche d'intensita' sono un campo della scheda, non un suggerimento a parole:
              compila `technique` per gli esercizi a cui le prescrivi, con `techniqueReason`.
              Valori ammessi: NONE, DROP_SET, REST_PAUSE, MYO_REPS, SUPERSET, BACK_OFF_SET,
              CLUSTER_SET, FAILURE, ECCENTRIC_EMPHASIS. Non metterle su tutti gli esercizi:
              abbinale dove la metodologia consultata le giustifica.
            - Ogni esercizio prescritto deve essere eseguibile con l'attrezzatura dichiarata.
              Se un esercizio richiede un attrezzo assente dall'inventario, scegli un'alternativa
              compatibile con lo stesso gruppo muscolare: non prescriverlo comunque.
            - Non sei un medico: nessuna diagnosi, farmaco, ormone o sostanza dopante. Nessun deficit
              calorico estremo o volume irragionevole.
        """.trimIndent()
    }
}
