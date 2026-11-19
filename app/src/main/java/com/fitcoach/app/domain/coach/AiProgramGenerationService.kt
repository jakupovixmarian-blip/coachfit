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
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
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
            val warnings: List<String> = emptyList()
        ) : Result

        data class Failure(val message: String, val canRetry: Boolean = true) : Result
    }

    suspend fun generate(
        profile: UserProfile,
        scope: ProgramGenerationEngine.Scope = ProgramGenerationEngine.Scope.BOTH
    ): Result {
        if (!profile.isAnamnesisReady) {
            return Result.Failure("Completa e conferma l'anamnesi prima di generare il programma.", false)
        }
        val foods = FoodCatalogSelector.forGeneration(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        if (foods.isEmpty()) return Result.Failure("Nessun alimento compatibile disponibile nel database.", false)
        if (exercises.isEmpty()) return Result.Failure("Database esercizi vuoto: impossibile validare la scheda.", false)

        val config = resolveConfig()
            ?: return Result.Failure(
                "Provider AI non configurato. Inserisci una chiave o configura il backend proxy nelle Impostazioni."
            )

        val contextPrompt = buildGenerationContext(profile, foods, exercises, scope)
        val turn = orchestrator.run(
            config = config,
            systemPrompt = generationSystemPrompt,
            contextPrompt = contextPrompt,
            history = emptyList(),
            userMessage = "Genera ora la parte richiesta del mio programma e consegnala con " +
                "submit_full_program (scope=${scope.name}). Non fermarti prima: devi arrivare a ok=true.",
            maxIterations = GENERATION_MAX_ITERATIONS,
            requireTool = SUBMIT_TOOL
        )

        val submission = turn.toolResults.lastOrNull { it.name == SUBMIT_TOOL }
            ?: return Result.Failure(
                turn.providerError
                    ?: "Il Coach AI non ha consegnato il programma. Riprova: se il problema persiste, " +
                    "verifica il provider AI o prova un modello con il supporto agli strumenti."
            )

        return interpretSubmission(submission.output)
    }

    private fun interpretSubmission(output: String): Result {
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
            return Result.Failure(message)
        }
        val nutritionId = (obj["nutritionPlanId"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
        val workoutId = (obj["workoutPlanId"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
        val warnings = (obj["avvisi"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            ?: emptyList()
        return Result.Success(nutritionId, workoutId, warnings)
    }

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

    private fun buildGenerationContext(
        profile: UserProfile,
        foods: List<FoodItem>,
        exercises: List<Exercise>,
        scope: ProgramGenerationEngine.Scope
    ): String = buildString {
        appendLine("Genera la parte richiesta del programma COMPLETO per FitCoach usando SOLO gli identificativi testuali elencati.")
        appendLine("AMBITO RICHIESTO: ${scope.name} " + when (scope) {
            ProgramGenerationEngine.Scope.BOTH -> "(nutrizione + allenamento)"
            ProgramGenerationEngine.Scope.NUTRITION -> "(SOLO nutrizione: compila comunque i giorni di allenamento e il calendario, ma verranno usati solo per i controlli)"
            ProgramGenerationEngine.Scope.WORKOUT -> "(SOLO allenamento: compila comunque la nutrizione, ma verrà usata solo per i controlli)"
        })
        appendLine("DATI ANAMNESI CONFERMATI:")
        appendLine(AnamnesisSummary.forProfile(profile))
        appendLine("OBIETTIVO: ${profile.goal.name}; giorni disponibili: ${profile.trainingDaysPerWeek}; pasti: ${profile.mealsPerDay}")
        appendLine("CARICHI DICHIARATI: ${profile.currentLoads.ifEmpty { mapOf("nessuno" to 0.0) }}")
        appendLine("ATTREZZATURA: ${profile.availableEquipment.joinToString { it.name }.ifBlank { "nessuna" }}")
        appendLine("ALIMENTI CONSENTITI (nome | kcal P C F per 100g):")
        foods.take(90).forEach { f ->
            appendLine("${f.name} | ${f.kcalPer100g} ${f.proteinPer100g} ${f.carbsPer100g} ${f.fatPer100g} | unità ${f.defaultUnit.name}")
        }
        appendLine("ESERCIZI CONSENTITI (nome | muscolo | attrezzatura | range | recupero | RIR):")
        exercises.take(120).forEach { e ->
            appendLine("${e.name} | ${e.primaryMuscle.name} | ${e.equipment.joinToString { it.name }} | ${e.minReps}-${e.maxReps} | ${e.restSeconds}s | ${e.targetRir}")
        }
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
        appendLine("Consegnalo con lo strumento submit_full_program: la verifica e il salvataggio avvengono li.")
    }

    companion object {
        private const val SUBMIT_TOOL = "submit_full_program"
        private const val GENERATION_MAX_ITERATIONS = 8

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
            - Rispetta allergie, intolleranze, alimenti esclusi, limitazioni, attrezzatura, esperienza,
              recupero e budget.
            - `dayType` deve essere HIGH, MEDIUM o LOW; `slot` un valore MealSlot (BREAKFAST,
              MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER, EVENING_SNACK); `unit` RAW, COOKED
              o AS_SOLD; `split` FULL_BODY, UPPER_LOWER, PUSH_PULL_LEGS o CUSTOM.
            - Ogni giorno nutrizionale: da 3 a 6 pasti, ogni pasto con almeno un alimento, ogni
              alimento da 1 a 2000 grammi. Esattamente 7 giorni con dayIndex 0..6.
            - Ogni esercizio: `sets` da 1 a 8, `minReps`/`maxReps` da 1 a 30 (con minReps<=maxReps),
              `restSeconds` da 15 a 600, `targetRir` da 0 a 5. Sono tutti obbligatori: non ometterli.
            - Ogni seduta: 6-24 serie allenanti totali; lo stesso esercizio non oltre 24 serie
              settimanali; nessun esercizio vietato. Giornate diverse quando lo split lo consente.
              Non assegnare una serie completa ai muscoli secondari.
            - Non sei un medico: nessuna diagnosi, farmaco, ormone o sostanza dopante. Nessun deficit
              calorico estremo o volume irragionevole.
        """.trimIndent()
    }
}
