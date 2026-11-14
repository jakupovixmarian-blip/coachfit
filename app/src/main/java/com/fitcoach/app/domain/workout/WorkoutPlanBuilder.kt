package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay

/**
 * Costruisce la scheda settimanale completa a partire dal profilo.
 *
 * Pipeline:
 *  1. SplitGenerator decide split e giorni.
 *  2. Per ogni giorno di allenamento seleziona gli esercizi compatibili.
 *  3. Assegna serie, ripetizioni, recuperi e RIR target.
 *  4. Calcola il volume settimanale per gruppo muscolare.
 */
object WorkoutPlanBuilder {

    data class BuiltPlan(
        val splitType: SplitType,
        val days: List<WorkoutDay>,
        val weeklyVolumePerGroup: Map<MuscleGroup, Int>,
        val notes: List<String>
    )

    fun build(
        profile: UserProfile,
        exercises: List<Exercise>,
        trainingDayIndices: List<Int> = defaultTrainingDays(profile.trainingDaysPerWeek),
        splitTypeOverride: SplitType? = null,
        fatigueSeverity: Int = 0
    ): BuiltPlan {
        val splitType = splitTypeOverride ?: SplitGenerator.recommendSplit(profile)
        val notes = mutableListOf<String>()

        val splitWeek = SplitGenerator.buildWeek(splitType, trainingDayIndices, profile.experience)
        val volume = mutableMapOf<MuscleGroup, Int>()
        // Distribuiamo prima il volume per giorno per sapere quanti set per gruppo.
        val trainingDayCount = splitWeek.count { !it.isRest }

        val days = splitWeek.map { day ->
            if (day.isRest) {
                WorkoutDay(
                    dayIndex = day.dayIndex,
                    title = "Riposo",
                    isRestDay = true
                )
            } else {
                val count = SplitGenerator.exercisesPerSession(
                    profile.experience, day.muscleGroups.size, profile.maxSessionMinutes
                )
                val compatibleExercises = exercises.filter { exercise ->
                    profile.availableEquipment.isEmpty() || exercise.bodyweightOnly ||
                        exercise.equipment.isEmpty() ||
                        exercise.equipment.any { it in profile.availableEquipment }
                }
                val selected = SplitGenerator.selectExercisesDetailed(
                    day.muscleGroups, compatibleExercises, profile, count, rotationSeed = day.dayIndex
                )
                if (selected.isEmpty()) {
                    notes += "Nessun esercizio compatibile per ${day.title}: verifica l'attrezzatura disponibile."
                }
                val planned = selected.mapIndexed { i, sel ->
                    val ex = sel.exercise
                    val sets = setsForExercise(profile, ex, trainingDayCount, day.muscleGroups.size)
                    val isLast = i == selected.lastIndex
                    val fatigue = fatigueSeverity
                    val technique = IntensityTechniquesEngine.decide(
                        exercise = ex,
                        profile = profile,
                        isLastExerciseOfSession = isLast,
                        weeklySetsForGroup = (volume[ex.primaryMuscle] ?: 0) + sets,
                        fatigueSeverity = fatigue
                    )
                    val rest = RestCalculator.decide(
                        category = ex.category,
                        goal = profile.goal,
                        targetRir = ex.targetRir,
                        loadRatio = loadRatioFor(profile, ex),
                        experience = profile.experience,
                        fatigueSeverity = fatigue,
                        technique = technique.technique
                    )
                    PlannedExercise(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        primaryMuscle = ex.primaryMuscle,
                        orderIndex = i,
                        sets = sets,
                        minReps = ex.minReps,
                        maxReps = ex.maxReps,
                        restSeconds = rest.seconds,
                        targetRir = ex.targetRir,
                        technique = ex.technique,
                        category = ex.category,
                        selectionReason = sel.reason,
                        restReason = rest.reason,
                        intensityTechnique = technique.technique,
                        techniqueReason = if (technique.technique != IntensityTechnique.NONE)
                            technique.reason else "",
                        movementPattern = ex.movementPattern
                    )
                }
                planned.forEach { pe ->
                    volume[pe.primaryMuscle] = (volume[pe.primaryMuscle] ?: 0) + pe.sets
                }
                WorkoutDay(
                    dayIndex = day.dayIndex,
                    title = day.title,
                    muscleGroups = day.muscleGroups,
                    exercises = planned
                )
            }
        }

        if (profile.availableEquipment.isEmpty()) {
            notes += "Nessuna attrezzatura selezionata: sono stati privilegiati esercizi a corpo libero."
        }
        // Nota sul volume per i gruppi prioritari.
        if (profile.priorityMuscles.isNotEmpty()) {
            val prio = profile.priorityMuscles.joinToString { it.label }
            notes += "Volume aumentato per i gruppi prioritari: $prio."
        }

        val allPlanned = days.asSequence().flatMap { it.exercises.asSequence() }.toList()
        val effectiveVolume = EffectiveVolumeCalculator
            .forExercises(allPlanned, exercises.associateBy { it.id })
            .effective
            .mapValues { (_, value) -> kotlin.math.round(value).toInt() }
        return BuiltPlan(splitType, days, effectiveVolume, notes)
    }

    /** Rapporto tra carico attuale dichiarato e carico tipico (per stimare l'intensita). */
    private fun loadRatioFor(profile: UserProfile, ex: Exercise): Double {
        val declared = profile.currentLoads.entries
            .firstOrNull { ex.name.lowercase().contains(it.key.lowercase()) }?.value ?: return 1.0
        // Nessun riferimento assoluto: usiamo il carico per ripetizione come proxy.
        return when {
            declared <= 0 -> 1.0
            declared > 100 -> 1.1
            declared < 20 -> 0.85
            else -> 1.0
        }
    }

    /**
     * Serie per esercizio, individualizzate su esperienza, volume target del gruppo
     * e numero di esercizi nella sessione. Il volume non e fisso per tutti.
     */
    private fun setsForExercise(
        profile: UserProfile,
        ex: Exercise,
        trainingDayCount: Int,
        groupsInDay: Int
    ): Int {
        val base = ex.defaultSets
        val priority = ex.primaryMuscle in profile.priorityMuscles
        // Esperienza e capacita di recupero modulano il volume.
        val experienceAdj = when (profile.experience) {
            TrainingExperience.BEGINNER -> -1
            TrainingExperience.INTERMEDIATE -> 0
            TrainingExperience.ADVANCED -> if (ex.category == ExerciseCategory.COMPOUND) 1 else 0
        }
        val recoveryAdj = when (profile.recoveryCapacity) {
            Level3.LOW -> -1
            Level3.MEDIUM -> 0
            Level3.HIGH -> 0
        }
        val priorityAdj = if (priority) 1 else 0
        return (base + experienceAdj + recoveryAdj + priorityAdj).coerceIn(2, 5)
    }

    fun defaultTrainingDays(daysPerWeek: Int): List<Int> {
        return when (daysPerWeek.coerceIn(0, 7)) {
            0 -> emptyList()
            1 -> listOf(2)
            2 -> listOf(1, 4)
            3 -> listOf(0, 2, 4)
            4 -> listOf(0, 1, 3, 4)
            5 -> listOf(0, 1, 2, 4, 5)
            6 -> listOf(0, 1, 2, 3, 4, 5)
            else -> listOf(0, 1, 2, 3, 4, 5, 6)
        }
    }

    /**
     * Sostituisce un esercizio mantenendo lo stimolo muscolare.
     * Preferisce alternative esplicite, poi stessa categoria/gruppo.
     */
    fun findSubstitutes(
        exercise: Exercise,
        all: List<Exercise>,
        profile: UserProfile
    ): List<Exercise> {
        val equipment = profile.availableEquipment.ifEmpty { null }
        val usable = all.filter { ex ->
            ex.id != exercise.id &&
                ex.primaryMuscle == exercise.primaryMuscle &&
                (equipment == null || ex.bodyweightOnly || ex.equipment.isEmpty() || ex.equipment.any { it in equipment })
        }
        val byName = usable.associateBy { it.name }
        val explicit = exercise.substituteNames.mapNotNull { byName[it] }
        val sameCategory = usable.filter { it.category == exercise.category }
        // Fallback: se non c'e nulla con la stessa categoria (es. multiarticolare
        // senza alternative multiarticolari), usa qualsiasi esercizio dello stesso
        // gruppo muscolare per mantenere lo stimolo.
        val sameMuscleFallback = if (explicit.isEmpty() && sameCategory.isEmpty()) usable else emptyList()
        return (explicit + sameCategory + sameMuscleFallback).distinctBy { it.id }.take(4)
    }

    fun categoryLabel(category: ExerciseCategory): String = when (category) {
        ExerciseCategory.COMPOUND -> "Multiarticolare"
        ExerciseCategory.ISOLATION -> "Isolamento"
        ExerciseCategory.BODYWEIGHT -> "Corpo libero"
    }
}
