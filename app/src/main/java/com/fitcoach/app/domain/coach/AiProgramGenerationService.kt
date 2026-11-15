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
import com.fitcoach.app.domain.nutrition.FoodFilter
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

    suspend fun generate(profile: UserProfile): Result {
        if (!profile.isAnamnesisReady) {
            return Result.Failure("Completa e conferma l'anamnesi prima di generare il programma.", false)
        }
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        if (foods.isEmpty()) return Result.Failure("Nessun alimento compatibile disponibile nel database.", false)
        if (exercises.isEmpty()) return Result.Failure("Database esercizi vuoto: impossibile validare la scheda.", false)

        val config = resolveConfig()
            ?: return Result.Failure(
                "Provider AI non configurato. Inserisci una chiave o configura il backend proxy nelle Impostazioni."
            )

        val contextPrompt = buildGenerationContext(profile, foods, exercises)
        val turn = orchestrator.run(
            config = config,
            systemPrompt = generationSystemPrompt,
            contextPrompt = contextPrompt,
            history = emptyList(),
            userMessage = "Genera ora il mio programma completo e consegnalo con submit_full_program. " +
                "Non fermarti prima: devi arrivare a ok=true.",
            maxIterations = GENERATION_MAX_ITERATIONS
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
            maxTokens = maxOf(settings.maxTokens, 8_000),
            usesBackendProxy = settings.useBackendProxy
        )
        return config.takeIf { it.isConfigured }
    }

    private fun buildGenerationContext(
        profile: UserProfile,
        foods: List<FoodItem>,
        exercises: List<Exercise>
    ): String = buildString {
        appendLine("Genera il programma COMPLETO per FitCoach usando SOLO gli identificativi testuali elencati.")
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
        appendLine("Il risultato deve avere esattamente 7 giorni nutrizionali e ${profile.trainingDaysPerWeek} giorni di allenamento.")
        appendLine("Consegnalo con lo strumento submit_full_program: la verifica e il salvataggio avvengono li.")
    }

    companion object {
        private const val SUBMIT_TOOL = "submit_full_program"
        private const val GENERATION_MAX_ITERATIONS = 8

        val generationSystemPrompt = """
            Sei il Coach AI di FitCoach. Sei l'unica autorita decisionale: costruisci un percorso
            PERSONALIZZATO, non un modello generico, coerente con alimentazione, allenamento, recupero
            e progressione.

            METODO
            1. Leggi i dati reali con gli strumenti se ti servono (get_anamnesis, list_foods,
               list_exercises, compute_energy_targets, compute_carb_cycling, compute_weekly_volume,
               compute_portion, validate_nutrition_day, validate_workout_plan).
            2. Costruisci 7 giorni nutrizionali con carb cycling (HIGH nei giorni piu intensi, MEDIUM,
               LOW nei giorni di riposo) e una scheda periodizzata con esattamente i giorni dichiarati.
            3. Verifica la scheda con validate_workout_plan e le giornate con validate_nutrition_day.
            4. Consegnala con submit_full_program (una sola chiamata, oggetto completo). Se ricevi
               ok=false, correggi SOLO i valori in 'violazioni' e richiama lo strumento.

            VINCOLI
            - Usa solo alimenti ed esercizi dei cataloghi. Non inventare identificativi, misure,
              alimenti o esercizi.
            - Rispetta allergie, intolleranze, alimenti esclusi, limitazioni, attrezzatura, esperienza,
              recupero e budget.
            - `dayType` deve essere HIGH, MEDIUM o LOW; `slot` un valore MealSlot; `unit` RAW, COOKED
              o AS_SOLD; `split` FULL_BODY, UPPER_LOWER, PUSH_PULL_LEGS o CUSTOM.
            - Ogni seduta: 6-24 serie allenanti, nessun esercizio vietato, giornate diverse quando lo
              split lo consente. Non assegnare una serie completa ai muscoli secondari.
            - Non sei un medico: nessuna diagnosi, farmaco, ormone o sostanza dopante. Nessun deficit
              calorico estremo o volume irragionevole.
        """.trimIndent()
    }
}
