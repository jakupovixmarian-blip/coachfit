@file:OptIn(ExperimentalSerializationApi::class)

package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.CatalogNameResolver
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import com.fitcoach.app.domain.workout.CatalogBounds
import com.fitcoach.app.domain.workout.EffectiveVolumeCalculator
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.abs

/**
 * Motore deterministico di validazione e mappatura del percorso generato dall'AI.
 *
 * L'AI e l'autorita decisionale: propone la struttura completa (nutrizione + scheda).
 * Questo motore NON genera programmi alternativi: verifica che la proposta rispetti
 * i calcoli matematici (calorie, macro, carb cycling), il database alimentare ed
 * esercizi e i vincoli del profilo. Se la proposta non e valida, la blocca e
 * restituisce la diagnosi, cosi l'AI puo correggersi.
 *
 * La stessa logica serve la generazione iniziale e le modifiche successive: un'unica
 * autorita AI, un unico insieme di regole.
 */
object ProgramGenerationEngine {

    /** Ambito di rigenerazione richiesto dall'utente. L'AI resta l'autorita. */
    enum class Scope { BOTH, NUTRITION, WORKOUT }

    /** Proposta mappata e verificata, pronta per la persistenza. */
    data class Validated(
        val nutrition: ValidatedNutrition?,
        val workout: WorkoutPlanBuilder.BuiltPlan?,
        val warnings: List<String>
    )

    data class ValidatedNutrition(
        val energy: com.fitcoach.app.domain.model.EnergyResult,
        val cycling: com.fitcoach.app.domain.model.CarbCyclingPlan,
        val days: List<MealPlanGenerator.GeneratedDay>,
        val dayTypes: List<CarbDayType>,
        val trainingDays: List<Int>,
        val carbCyclingApplied: Boolean
    )

    /**
     * Verifica la proposta dell'AI contro i vincoli reali per l'ambito richiesto.
     * Ritorna la proposta mappata se le parti INCLUSE sono valide, altrimenti null
     * (usa [diagnose] per i dettagli).
     */
    fun validate(
        profile: UserProfile,
        proposal: AiProgramProposal,
        foods: List<FoodItem>,
        exercises: List<Exercise>,
        scope: Scope = Scope.BOTH
    ): Validated? {
        val energy = EnergyCalculator.compute(profile) ?: return null
        val includeNutrition = scope != Scope.WORKOUT
        val includeWorkout = scope != Scope.NUTRITION
        val trainingDays = proposal.workout.days.map { it.dayIndex }.distinct().sorted()

        val nutrition = if (includeNutrition) {
            validateNutrition(profile, proposal, foods, energy, trainingDays) ?: return null
        } else null
        val workout = if (includeWorkout) {
            mapWorkout(profile, proposal, exercises) ?: return null
        } else null
        if (workout != null) {
            val plannedDays = workout.days.filterNot { it.isRestDay }
            if (plannedDays.size != profile.trainingDaysPerWeek || plannedDays.any { it.exercises.isEmpty() }) return null
            if (plannedDays.any { it.totalSets !in 6..24 }) return null
            if (plannedDays.flatMap { it.exercises }.groupBy { it.exerciseId }
                    .any { (_, items) -> items.sumOf { it.sets } > 24 }
            ) return null
        }
        val warnings = listOf("Strategia generata dal Coach AI; numeri e vincoli verificati offline.")
        return Validated(nutrition, workout, warnings)
    }

    private fun validateNutrition(
        profile: UserProfile,
        proposal: AiProgramProposal,
        foods: List<FoodItem>,
        energy: com.fitcoach.app.domain.model.EnergyResult,
        trainingDays: List<Int>
    ): ValidatedNutrition? {
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
                    // Stesso risolutore del validatore: se la diagnostica accetta
                    // un nome tollerante, il salvataggio non deve rifiutarlo.
                    val food = CatalogNameResolver.resolveFood(aiEntry.foodName, foods)
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

        val weeklyKcal = generatedDays.sumOf { it.actual.kcal }
        if (!close(weeklyKcal, cycling.weeklyKcal, 0.20)) return null
        return ValidatedNutrition(energy, cycling, generatedDays, dayTypes, trainingDays, cyclingApplied)
    }

    /** Diagnosi leggibile dei vincoli violati (valore atteso vs ricevuto). */
    fun diagnose(
        profile: UserProfile,
        proposal: AiProgramProposal,
        foods: List<FoodItem>,
        exercises: List<Exercise>,
        scope: Scope = Scope.BOTH
    ): List<String> = AiProposalDiagnostics.analyze(profile, proposal, foods, exercises, scope)

    private fun mapWorkout(
        profile: UserProfile,
        proposal: AiProgramProposal,
        exercises: List<Exercise>
    ): WorkoutPlanBuilder.BuiltPlan? {
        val known = exercises.associateBy { normalize(it.name) }
        val training = proposal.workout.days.associateBy { it.dayIndex }
        val days = (0..6).map { index ->
            val aiDay = training[index]
            if (aiDay == null) return@map WorkoutDay(dayIndex = index, title = "Riposo", isRestDay = true)
            if (aiDay.exercises.isEmpty()) return null
            val planned = aiDay.exercises.mapIndexed { order, aiExercise ->
                // Un esercizio senza nome o senza prescrizione non e' un esercizio:
                // salvarlo produrrebbe una scheda con righe vuote da 0 serie.
                if (aiExercise.exerciseName.isBlank()) return null
                if (aiExercise.sets < 1 || aiExercise.minReps < 1 ||
                    aiExercise.maxReps < aiExercise.minReps
                ) return null
                val exercise = CatalogNameResolver.resolveExercise(aiExercise.exerciseName, exercises)
                    ?: return null
                if (!allowedExercise(exercise, profile)) return null
                // Stessi limiti del catalogo usati dalla diagnostica: un'unica
                // fonte, altrimenti validate e analyze rifiutano cose diverse.
                if (CatalogBounds.violations(
                        exercise, aiExercise.sets, aiExercise.minReps,
                        aiExercise.maxReps, aiExercise.restSeconds, aiExercise.targetRir
                    ).isNotEmpty()
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
        val volume = EffectiveVolumeCalculator
            .forExercises(days.flatMap { it.exercises }, exercises.associateBy { it.id })
            .effective
            .mapValues { (_, value) -> kotlin.math.round(value).toInt() }
        val split = runCatching { SplitType.valueOf(proposal.workout.split.uppercase()) }.getOrDefault(SplitType.CUSTOM)
        return WorkoutPlanBuilder.BuiltPlan(
            splitType = split,
            days = days,
            weeklyVolumePerGroup = volume,
            notes = listOf(proposal.workout.rationale)
        )
    }

    private fun allowedExercise(exercise: Exercise, profile: UserProfile): Boolean {
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

    private fun parseFoodUnit(raw: String): FoodUnit? =
        runCatching { FoodUnit.valueOf(raw.trim().uppercase()) }.getOrNull()

    private fun parseMuscle(raw: String): MuscleGroup? =
        runCatching { MuscleGroup.valueOf(raw.trim().uppercase()) }.getOrNull()

    private fun parseTechnique(raw: String): IntensityTechnique = runCatching {
        IntensityTechnique.valueOf(raw.trim().uppercase().replace("-", "_"))
    }.getOrDefault(IntensityTechnique.NONE)

    private fun normalize(raw: String): String = raw.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9àèéìòù]+"), " ").trim()
}

/** Contratto JSON strutturato prodotto dall'AI (nutrizione + scheda + calendario). */
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
    @JsonNames("name", "food", "food_name", "alimento")
    val foodName: String = "",
    @JsonNames("quantity", "quantityGrams", "grammi")
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
    // Gli alias coprono le varianti di nome campo che i modelli usano di fatto.
    // Senza di essi `ignoreUnknownKeys` faceva decodificare un esercizio con
    // nome vuoto e 0 ripetizioni, e la diagnosi parlava di "esercizio ''".
    @JsonNames("name", "exercise", "exercise_name", "esercizio")
    val exerciseName: String = "",
    @JsonNames("serie")
    val sets: Int = 0,
    @JsonNames("reps", "min_reps", "repsMin", "ripetizioni_min")
    val minReps: Int = 0,
    @JsonNames("max_reps", "repsMax", "ripetizioni_max")
    val maxReps: Int = 0,
    @JsonNames("rest", "rest_seconds", "recupero")
    val restSeconds: Int = 0,
    @JsonNames("rir", "rir_target", "target_rir")
    val targetRir: Int = 0,
    val technique: String = "NONE",
    val techniqueReason: String = ""
)

@Serializable
data class AiCalendarProposal(
    val workoutDayIndices: List<Int> = emptyList(),
    val restDayIndices: List<Int> = emptyList()
)

/** Parser stricto per il contratto JSON del generatore AI. */
object AiProgramProposalJson {
    fun decode(json: Json, raw: String): AiProgramProposal? {
        val candidate = raw.trim().let {
            if (it.startsWith("``")) it.replace(Regex("^```(?:json)?\\s*|\\s*```$"), "").trim() else it
        }
        return runCatching { json.decodeFromString<AiProgramProposal>(candidate) }.getOrNull()
    }
}

/**
 * Riepilogo testuale dell'anamnesi fornito al generatore.
 *
 * Ogni campo riporta "NON_DISPONIBILE" quando l'utente non lo ha dichiarato.
 * Il profilo usa valori di default non nullable (obiettivo RECOMP, lavoro
 * SEDENTARY, 5 pasti, esperienza BEGINNER) per non propagare null: stamparli
 * come se fossero risposte farebbe decidere l'AI su dati mai forniti. Un campo
 * si considera dichiarato solo se compare tra le risposte dell'intervista.
 */
object AnamnesisSummary {

    private const val UNKNOWN = "NON_DISPONIBILE"

    /**
     * Valore dichiarato dall'utente, oppure [fallback] se la domanda [id] non e
     * tra le risposte registrate.
     *
     * Unico punto di verita per il riepilogo e per il contesto di generazione:
     * duplicare la regola nei due prompt produceva istruzioni contraddittorie
     * (riepilogo "NON_DISPONIBILE" + riga "OBIETTIVO: RECOMP").
     */
    fun declared(p: UserProfile, id: String, value: Any?, fallback: String = UNKNOWN): String =
        if (id in AnamnesisEngine.answeredIds(p) && value != null) value.toString() else fallback

    fun forProfile(p: UserProfile): String {
        val answered = AnamnesisEngine.answeredIds(p)
        fun d(id: String, value: Any?): String =
            if (id in answered && value != null) value.toString() else UNKNOWN

        // I campi nullable senza default non possono essere confusi con una
        // misura inventata: se hanno un valore, l'utente li ha inseriti. Serve
        // per il budget settimanale, che il form di onboarding salva ma che non
        // e una domanda dell'intervista.
        fun n(value: Any?): String = value?.toString() ?: UNKNOWN

        return buildString {
            appendLine(
                "eta=${d("age", p.ageYears)}; sesso=${d("sex", p.sex.takeIf { it != Sex.UNSPECIFIED })}; " +
                    "altezza=${d("heightCm", p.heightCm)}; peso=${d("weightKg", p.weightKg)}; " +
                    "massa_grassa=${d("bodyFatPercent", p.bodyFatPercent)}"
            )
            appendLine(
                "esperienza=${d("experience", p.experience)}; stato=${d("trainingStatus", p.trainingStatus)}; " +
                    "anni=${d("trainingYears", p.trainingYears)}; tecnico=${d("technicalLevel", p.technicalLevel)}"
            )
            appendLine(
                "obiettivo=${d("goal", p.goal)}; secondari=${d("secondaryGoals", p.secondaryGoals.takeIf { it.isNotEmpty() })}; " +
                    "timeline_settimane=${d("targetTimelineWeeks", p.targetTimelineWeeks)}"
            )
            appendLine(
                "preferenze=${d("dietaryPreferences", p.dietaryPreferences.takeIf { it.isNotEmpty() })}; " +
                    "esclusi=${d("dislikedFoods", p.dislikedFoods.takeIf { it.isNotEmpty() })}; " +
                    "allergie=${d("allergies", p.allergies.takeIf { it.isNotEmpty() })}"
            )
            appendLine(
                "limitazioni=${d("physicalLimitations", p.physicalLimitations.takeIf { it.isNotEmpty() })}; " +
                    "movimenti_da_evitare=${d("movementsToAvoid", p.movementsToAvoid.takeIf { it.isNotEmpty() })}"
            )
            appendLine(
                "recupero=${d("recoveryCapacity", p.recoveryCapacity)}; " +
                    "sonno=${d("sleepHours", p.sleepHours)}/${d("sleepQuality", p.sleepQuality)}; " +
                    "stress=${d("stressLevel", p.stressLevel)}; energia=${d("dailyEnergy", p.dailyEnergy)}"
            )
            appendLine(
                "lavoro=${d("workType", p.workType)}; passi=${d("dailySteps", p.dailySteps)}; " +
                    "sport_extra=${d("extraSportSessionsPerWeek", p.extraSportSessionsPerWeek)}; " +
                    "sessione_max=${d("maxSessionMinutes", p.maxSessionMinutes)}"
            )
            appendLine(
                "budget_settimanale=${n(p.weeklyFoodBudgetEur)}; " +
                    "budget_mensile=${d("monthlyFoodBudgetEur", p.monthlyFoodBudgetEur)}; " +
                    "persone=${d("budgetPersons", p.budgetPersons)}"
            )
            appendLine(
                "alimenti_graditi=${d("likedFoods", p.likedFoods.takeIf { it.isNotEmpty() })}; " +
                    "non_graditi=${d("dislikedFoods", p.dislikedFoods.takeIf { it.isNotEmpty() })}; " +
                    "pasti=${d("mealsPerDay", p.mealsPerDay)}; cucina=${d("cookingTimeAvailable", p.cookingTimeAvailable)}"
            )
            appendLine(
                "giorni_allenamento=${d("trainingDaysPerWeek", p.trainingDaysPerWeek)}; " +
                    "attrezzatura=${d("availableEquipment", p.availableEquipment.takeIf { it.isNotEmpty() })}; " +
                    "carichi_attuali=${d("currentLoads", p.currentLoads.takeIf { it.isNotEmpty() })}"
            )
        }.trimEnd()
    }
}
