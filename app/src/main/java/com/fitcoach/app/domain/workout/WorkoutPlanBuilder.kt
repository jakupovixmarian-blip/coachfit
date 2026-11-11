package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SplitType
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
        splitTypeOverride: SplitType? = null
    ): BuiltPlan {
        val splitType = splitTypeOverride ?: SplitGenerator.recommendSplit(profile)
        val notes = mutableListOf<String>()

        val splitWeek = SplitGenerator.buildWeek(splitType, trainingDayIndices, profile.experience)
        val weeklySetsTarget = SplitGenerator.weeklySetsPerGroup(profile.experience)
        val volume = mutableMapOf<MuscleGroup, Int>()

        val days = splitWeek.map { day ->
            if (day.isRest) {
                WorkoutDay(
                    dayIndex = day.dayIndex,
                    title = "Riposo",
                    isRestDay = true
                )
            } else {
                val count = SplitGenerator.exercisesPerSession(profile.experience, day.muscleGroups.size)
                val selected = SplitGenerator.selectExercises(
                    day.muscleGroups, exercises, profile, count
                )
                if (selected.isEmpty()) {
                    notes += "Nessun esercizio compatibile per ${day.title}: verifica l'attrezzatura disponibile."
                }
                val planned = selected.mapIndexed { i, ex ->
                    PlannedExercise(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        primaryMuscle = ex.primaryMuscle,
                        orderIndex = i,
                        sets = setsForExercise(ex, weeklySetsTarget, day.muscleGroups.size, selected.size),
                        minReps = ex.minReps,
                        maxReps = ex.maxReps,
                        restSeconds = ex.restSeconds,
                        targetRir = ex.targetRir,
                        technique = ex.technique,
                        category = ex.category
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

        return BuiltPlan(splitType, days, volume, notes)
    }

    private fun setsForExercise(
        ex: Exercise,
        weeklyTarget: Int,
        groupsInDay: Int,
        exercisesInDay: Int
    ): Int {
        val base = ex.defaultSets
        // Se il volume target e alto e ci sono pochi esercizi, aggiungi una serie.
        val adjusted = if (weeklyTarget >= 16 && exercisesInDay <= 5) base + 1 else base
        // Non eccedere 5 serie per esercizio.
        return adjusted.coerceIn(2, 5)
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
        return (explicit + sameCategory).distinctBy { it.id }.take(4)
    }

    fun categoryLabel(category: ExerciseCategory): String = when (category) {
        ExerciseCategory.COMPOUND -> "Multiarticolare"
        ExerciseCategory.ISOLATION -> "Isolamento"
        ExerciseCategory.BODYWEIGHT -> "Corpo libero"
    }
}
