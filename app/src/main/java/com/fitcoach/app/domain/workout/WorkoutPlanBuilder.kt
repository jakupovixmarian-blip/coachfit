package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay

/**
 * Contratti e utilita della scheda di allenamento.
 *
 * Non esiste piu un costruttore autonomo di schede: split, esercizi, serie,
 * recuperi e tecniche sono decisi dal Coach AI (vedi submit_full_program) e
 * verificati dai vincoli matematici. Qui restano il contratto di persistenza
 * ([BuiltPlan]), il calendario di default dei giorni e gli helper di
 * sostituzione degli esercizi usati dall'interfaccia.
 */
object WorkoutPlanBuilder {

    data class BuiltPlan(
        val splitType: SplitType,
        val days: List<WorkoutDay>,
        val weeklyVolumePerGroup: Map<MuscleGroup, Int>,
        val notes: List<String>
    )

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
                EquipmentFit.isAvailable(ex, equipment ?: emptySet())
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
