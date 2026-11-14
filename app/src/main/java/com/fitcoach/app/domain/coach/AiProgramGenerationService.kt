package com.fitcoach.app.domain.coach

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.ChatCompletionRequest
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Generazione online controllata: l'LLM sceglie la strategia e la struttura,
 * mentre questo servizio risolve gli identificativi locali e rifiuta ogni piano
 * che non rispetta i dati nutrizionali, il database esercizi o i vincoli utente.
 */
@Singleton
class AiProgramGenerationService @Inject constructor(
    private val aiClient: AiClient,
    private val db: FitCoachDatabase,
    private val chatRepository: ChatRepository,
    private val secureKeyStore: SecureKeyStore,
    private val json: Json,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
    private val planVersionRepository: PlanVersionRepository
) {
    sealed interface Result {
        data class Success(
            val nutritionPlanId: Long,
            val workoutPlanId: Long,
            val proposal: AiProgramProposal,
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
            ?: return Result.Failure("Provider AI non configurato. Inserisci una chiave o configura il backend proxy.")
        val context = buildGenerationContext(profile, foods, exercises)
        val first = requestProposal(config, context)
        val firstText = when (first) {
            is AiResult.Failure -> return Result.Failure(first.message)
            is AiResult.Success -> first.content
        }

        val proposal = decode(firstText) ?: run {
            val repair = requestRepair(config, context, firstText)
            val repairText = when (repair) {
                is AiResult.Failure -> return Result.Failure(
                    "Il provider ha restituito JSON non valido e la correzione non è riuscita: ${repair.message}"
                )
                is AiResult.Success -> repair.content
            }
            decode(repairText) ?: return Result.Failure(
                "Il provider AI ha restituito una struttura non valida dopo due tentativi. Nessun programma è stato salvato."
            )
        }

        val validated = validateAndMap(profile, proposal, foods, exercises)
            ?: return Result.Failure(
                "Il programma AI non rispetta i vincoli del profilo o i target matematici. Nessun programma è stato salvato."
            )

        return runCatching {
            db.withTransaction {
                val nutritionId = nutritionRepository.savePlan(
                    profile = profile,
                    energy = validated.energy,
                    cycling = validated.cycling,
                    generatedDays = validated.days,
                    dayTypes = validated.dayTypes,
                    trainingDayIndices = validated.trainingDays,
                    carbCyclingApplied = validated.carbCyclingApplied
                )
                val workoutId = workoutRepository.savePlan(profile, validated.workout)
                nutritionRepository.getActivePlan()?.let {
                    planVersionRepository.snapshotNutrition(it, "Programma generato dal Coach AI e validato dai motori matematici.")
                }
                workoutRepository.getActivePlan()?.let {
                    planVersionRepository.snapshotWorkout(it, "Scheda generata dal Coach AI e validata dai vincoli di sicurezza.")
                }
                Result.Success(nutritionId, workoutId, proposal, validated.warnings)
            }
        }.getOrElse { error ->
            Result.Failure("Programma AI non salvato: ${error.message ?: "errore di persistenza"}")
        }
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

    private suspend fun requestProposal(config: AiProviderConfig, context: String): AiResult =
        aiClient.complete(
            config = config,
            messages = listOf(
                ChatMessageDto("system", generationSystemPrompt),
                ChatMessageDto("user", context)
            ),
            temperature = 0.2,
            maxTokens = maxOf(config.maxTokens, 8_000),
            responseFormat = ChatCompletionRequest.ResponseFormat()
        )

    private suspend fun requestRepair(config: AiProviderConfig, context: String, invalid: String): AiResult =
        aiClient.complete(
            config = config,
            messages = listOf(
                ChatMessageDto("system", generationSystemPrompt),
                ChatMessageDto(
                    "user",
                    "La risposta precedente non è JSON valido. Riscrivila interamente come un singolo " +
                        "oggetto JSON conforme allo schema, senza markdown e senza testo extra.\n" +
                        "CONTESTO:\n$context\nRISPOSTA DA CORREGGERE:\n$invalid"
                )
            ),
            temperature = 0.0,
            maxTokens = maxOf(config.maxTokens, 8_000),
            responseFormat = null
        )

    private fun decode(raw: String): AiProgramProposal? = AiProgramProposalJson.decode(json, raw)

    private fun buildGenerationContext(
        profile: UserProfile,
        foods: List<FoodItem>,
        exercises: List<com.fitcoach.app.domain.model.Exercise>
    ): String = buildString {
        appendLine("Genera il programma per FitCoach usando SOLO gli identificativi testuali elencati.")
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
    }

    private data class Validated(
        val energy: com.fitcoach.app.domain.model.EnergyResult,
        val cycling: com.fitcoach.app.domain.model.CarbCyclingPlan,
        val days: List<MealPlanGenerator.GeneratedDay>,
        val dayTypes: List<CarbDayType>,
        val trainingDays: List<Int>,
        val carbCyclingApplied: Boolean,
        val workout: com.fitcoach.app.domain.workout.WorkoutPlanBuilder.BuiltPlan,
        val warnings: List<String>
    )

    private suspend fun validateAndMap(
        profile: UserProfile,
        proposal: AiProgramProposal,
        foods: List<FoodItem>,
        exercises: List<com.fitcoach.app.domain.model.Exercise>
    ): Validated? {
        val energy = EnergyCalculator.compute(profile) ?: return null
        val trainingDays = proposal.workout.days.map { it.dayIndex }.distinct().sorted()
        if (trainingDays.size != profile.trainingDaysPerWeek || trainingDays.any { it !in 0..6 }) return null
        if (proposal.calendar.workoutDayIndices.distinct().sorted() != trainingDays) return null
        if (proposal.calendar.restDayIndices.distinct().sorted() != (0..6).filterNot { it in trainingDays }) return null
        if (proposal.nutrition.days.map { it.dayIndex }.toSet() != (0..6).toSet()) return null

        val cyclingApplied = CarbCyclingEngine.shouldApplyCarbCycling(profile)
        val cycling = if (cyclingApplied) CarbCyclingEngine.build(profile, energy)
        else CarbCyclingEngine.buildUniform(profile, energy)
        val dayTypes = proposal.nutrition.days.sortedBy { it.dayIndex }.map { parseDayType(it.dayType) ?: return null }
        val generatedDays = proposal.nutrition.days.map { aiDay ->
            val type = parseDayType(aiDay.dayType) ?: return null
            val target = cycling.targets[type] ?: return null
            if (aiDay.meals.isEmpty() || aiDay.meals.size !in 3..6) return null
            val meals = aiDay.meals.map { aiMeal ->
                val slot = parseMealSlot(aiMeal.slot) ?: return null
                if (aiMeal.entries.isEmpty()) return null
                val entries = aiMeal.entries.map { aiEntry ->
                    if (aiEntry.grams !in 1.0..2_000.0) return null
                    val food = foods.firstOrNull { it.normalizedName == FoodItem.normalizeName(aiEntry.foodName) }
                        ?: return null
                    val factor = aiEntry.grams / 100.0
                    MealEntry(
                        foodItemId = food.id,
                        foodName = food.name,
                        grams = aiEntry.grams,
                        unit = parseFoodUnit(aiEntry.unit) ?: food.defaultUnit,
                        kcal = food.kcalPer100g * factor,
                        protein = food.proteinPer100g * factor,
                        carbs = food.carbsPer100g * factor,
                        fat = food.fatPer100g * factor
                    )
                }
                MealPlanGenerator.GeneratedMeal(slot, entries)
            }
            val actual = meals.fold(MacroTotals.ZERO) { sum, meal -> sum + meal.totals }
            val deviations = MacroTotals(
                actual.kcal - target.kcal,
                actual.protein - target.protein,
                actual.carbs - target.carbs,
                actual.fat - target.fat
            )
            val coherent = close(actual.kcal, target.kcal, 0.20) &&
                close(actual.protein, target.protein, 0.25) &&
                close(actual.carbs, target.carbs, 0.30) &&
                close(actual.fat, target.fat, 0.30)
            if (!coherent) return null
            MealPlanGenerator.GeneratedDay(aiDay.dayIndex, type, target, meals, actual, deviations, true)
        }.sortedBy { it.dayIndex }

        val workout = mapWorkout(profile, proposal, exercises) ?: return null
        val weeklyKcal = generatedDays.sumOf { it.actual.kcal }
        if (!close(weeklyKcal, cycling.weeklyKcal, 0.20)) return null
        return Validated(
            energy, cycling, generatedDays, dayTypes, trainingDays, cyclingApplied, workout,
            listOf("Strategia generata dal Coach AI; numeri e vincoli verificati offline.")
        )
    }

    private fun mapWorkout(
        profile: UserProfile,
        proposal: AiProgramProposal,
        exercises: List<com.fitcoach.app.domain.model.Exercise>
    ): com.fitcoach.app.domain.workout.WorkoutPlanBuilder.BuiltPlan? {
        val known = exercises.associateBy { it.normalizedName() }
        val training = proposal.workout.days.associateBy { it.dayIndex }
        val days = (0..6).map { index ->
            val aiDay = training[index]
            if (aiDay == null) return@map WorkoutDay(dayIndex = index, title = "Riposo", isRestDay = true)
            if (aiDay.exercises.isEmpty()) return null
            val planned = aiDay.exercises.mapIndexed { order, aiExercise ->
                val exercise = known[normalize(aiExercise.exerciseName)] ?: return null
                if (!allowedExercise(exercise, profile)) return null
                if (aiExercise.sets !in 1..8 || aiExercise.minReps !in 1..30 ||
                    aiExercise.maxReps !in aiExercise.minReps..30 || aiExercise.restSeconds !in 15..600 ||
                    aiExercise.targetRir !in 0..5
                ) return null
                PlannedExercise(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    primaryMuscle = exercise.primaryMuscle,
                    orderIndex = order,
                    sets = aiExercise.sets,
                    minReps = aiExercise.minReps,
                    maxReps = aiExercise.maxReps,
                    restSeconds = aiExercise.restSeconds,
                    targetRir = aiExercise.targetRir,
                    technique = exercise.technique,
                    category = exercise.category,
                    selectionReason = proposal.workout.rationale,
                    restReason = "Recupero scelto dal Coach AI e limitato dal motore a 15-600 secondi.",
                    intensityTechnique = parseTechnique(aiExercise.technique),
                    techniqueReason = aiExercise.techniqueReason,
                    movementPattern = exercise.movementPattern
                )
            }
            WorkoutDay(
                dayIndex = index,
                title = aiDay.title.ifBlank { "Allenamento" },
                muscleGroups = aiDay.muscleGroups.mapNotNull { parseMuscle(it) }.toSet(),
                exercises = planned
            )
        }
        val actualDays = days
        val volume = actualDays.flatMap { it.exercises }.groupingBy { it.primaryMuscle }.fold(0) { acc, ex -> acc + ex.sets }
        val split = runCatching { SplitType.valueOf(proposal.workout.split.uppercase()) }.getOrDefault(SplitType.CUSTOM)
        return com.fitcoach.app.domain.workout.WorkoutPlanBuilder.BuiltPlan(
            splitType = split,
            days = actualDays,
            weeklyVolumePerGroup = volume,
            notes = listOf(proposal.workout.rationale)
        )
    }

    private fun allowedExercise(exercise: com.fitcoach.app.domain.model.Exercise, profile: UserProfile): Boolean {
        val equipmentOk = profile.availableEquipment.isEmpty() || exercise.bodyweightOnly ||
            exercise.equipment.isEmpty() || exercise.equipment.any { it in profile.availableEquipment }
        val avoided = profile.movementsToAvoid.any {
            exercise.name.contains(it, true) || exercise.primaryMuscle.label.contains(it, true)

        }
        return equipmentOk && !avoided && exercise.name !in profile.dislikedExercises
    }

    private fun close(actual: Double, target: Double, tolerance: Double): Boolean =
        target > 0 && abs(actual - target) / target <= tolerance

    private fun parseDayType(raw: String): CarbDayType? = runCatching {
        CarbDayType.valueOf(raw.trim().uppercase().replace(" ", "_"))
    }.getOrNull()

    private fun parseMealSlot(raw: String): MealSlot? = runCatching {
        MealSlot.valueOf(raw.trim().uppercase().replace(" ", "_"))
    }.getOrNull()

    private fun parseFoodUnit(raw: String): FoodUnit? = runCatching { FoodUnit.valueOf(raw.trim().uppercase()) }.getOrNull()

    private fun parseMuscle(raw: String): MuscleGroup? = runCatching { MuscleGroup.valueOf(raw.trim().uppercase()) }.getOrNull()

    private fun parseTechnique(raw: String): IntensityTechnique = runCatching {
        IntensityTechnique.valueOf(raw.trim().uppercase().replace("-", "_"))
    }.getOrDefault(IntensityTechnique.NONE)

    private fun normalize(raw: String): String = raw.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9àèéìòù]+"), " ").trim()

    private fun com.fitcoach.app.domain.model.Exercise.normalizedName(): String = normalize(name)

    companion object {
        private val generationSystemPrompt = """
            Sei il Coach AI di FitCoach. Genera una proposta PERSONALIZZATA, non un modello generico.
            Usa solo alimenti ed esercizi presenti nel contesto. Rispetta allergie, limitazioni,
            attrezzatura, disponibilita, esperienza, recupero e budget. Non inventare identificativi.
            Rispondi con un singolo JSON valido, senza markdown. I grammi sono quantita reali.
            `dayType` deve essere HIGH, MEDIUM o LOW; `slot` un valore MealSlot; `unit` RAW, COOKED o AS_SOLD.
            `split` deve essere FULL_BODY, UPPER_LOWER, PUSH_PULL_LEGS o CUSTOM.
            Schema obbligatorio:
            {
              "schemaVersion":1,"strategy":"...","objectives":["..."],"monitoring":["..."],
              "nutrition":{"days":[{"dayIndex":0,"dayType":"MEDIUM","meals":[{"slot":"BREAKFAST","time":"07:30","entries":[{"foodName":"nome esatto","grams":100,"unit":"RAW"}]}]}]},
              "workout":{"split":"FULL_BODY","rationale":"...","days":[{"dayIndex":0,"title":"...","muscleGroups":["CHEST"],"exercises":[{"exerciseName":"nome esatto","sets":3,"minReps":8,"maxReps":12,"restSeconds":120,"targetRir":2,"technique":"NONE","techniqueReason":"..."}]}]},
              "calendar":{"workoutDayIndices":[0,2,4],"restDayIndices":[1,3,5,6]}
            }
        """.trimIndent()
    }
}

@Serializable
data class AiProgramProposal(
    val schemaVersion: Int = 1,
    val strategy: String = "",
    val objectives: List<String> = emptyList(),
    val monitoring: List<String> = emptyList(),
    val nutrition: AiNutritionProposal = AiNutritionProposal(),
    val workout: AiWorkoutProposal = AiWorkoutProposal(),
    val calendar: AiCalendarProposal = AiCalendarProposal()
)

@Serializable
data class AiNutritionProposal(val days: List<AiNutritionDay> = emptyList())

@Serializable
data class AiNutritionDay(
    val dayIndex: Int = -1,
    val dayType: String = "MEDIUM",
    val meals: List<AiMeal> = emptyList()
)

@Serializable
data class AiMeal(
    val slot: String = "",
    val time: String = "",
    val entries: List<AiFoodEntry> = emptyList()
)

@Serializable
data class AiFoodEntry(
    val foodName: String = "",
    val grams: Double = 0.0,
    val unit: String = "RAW"
)

@Serializable
data class AiWorkoutProposal(
    val split: String = "CUSTOM",
    val rationale: String = "",
    val days: List<AiWorkoutDay> = emptyList()
)

@Serializable
data class AiWorkoutDay(
    val dayIndex: Int = -1,
    val title: String = "",
    val muscleGroups: List<String> = emptyList(),
    val exercises: List<AiPlannedExercise> = emptyList()
)

@Serializable
data class AiPlannedExercise(
    val exerciseName: String = "",
    val sets: Int = 0,
    val minReps: Int = 0,
    val maxReps: Int = 0,
    val restSeconds: Int = 0,
    val targetRir: Int = 0,
    val technique: String = "NONE",
    val techniqueReason: String = ""
)

@Serializable
data class AiCalendarProposal(
    val workoutDayIndices: List<Int> = emptyList(),
    val restDayIndices: List<Int> = emptyList()
)

private object AnamnesisSummary {
    fun forProfile(p: UserProfile): String = buildString {
        appendLine("eta=${p.ageYears}; sesso=${p.sex}; altezza=${p.heightCm}; peso=${p.weightKg}; massa_grassa=${p.bodyFatPercent}")
        appendLine("esperienza=${p.experience}; stato=${p.trainingStatus}; anni=${p.trainingYears}; tecnico=${p.technicalLevel}")
        appendLine("obiettivo=${p.goal}; secondari=${p.secondaryGoals}; timeline_settimane=${p.targetTimelineWeeks}")
        appendLine("preferenze=${p.dietaryPreferences}; esclusi=${p.excludedFoods}; allergie=${p.allergies}")
        appendLine("limitazioni=${p.physicalLimitations}; movimenti_da_evitare=${p.movementsToAvoid}")
        appendLine("recupero=${p.recoveryCapacity}; sonno=${p.sleepHours}/${p.sleepQuality}; stress=${p.stressLevel}; energia=${p.dailyEnergy}")
        appendLine("lavoro=${p.workType}; passi=${p.dailySteps}; sport_extra=${p.extraSportSessionsPerWeek}; sessione_max=${p.maxSessionMinutes}")
        appendLine("budget_settimanale=${p.weeklyFoodBudgetEur}; budget_mensile=${p.monthlyFoodBudgetEur}; persone=${p.budgetPersons}")
        appendLine("alimenti_graditi=${p.likedFoods}; non_graditi=${p.dislikedFoods}; pasti=${p.mealsPerDay}; cucina=${p.cookingTimeAvailable}")
    }
}

/** Parser stricto per il contratto JSON del generatore AI. */
object AiProgramProposalJson {
    fun decode(json: Json, raw: String): AiProgramProposal? {
        val candidate = raw.trim().let {
            if (it.startsWith("``")) it.replace(Regex("^```(?:json)?\\s*|\\s*```$"), "").trim() else it
        }
        return runCatching { json.decodeFromString<AiProgramProposal>(candidate) }.getOrNull()
    }
}

