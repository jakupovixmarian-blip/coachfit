package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise

/**
 * Calcola le serie allenanti senza contare una serie secondaria come una serie
 * completa. Il muscolo principale vale 1.0; i contributi indiretti valgono 0.5
 * per il primo secondario e 0.25 per gli eventuali successivi.
 */
object EffectiveVolumeCalculator {
    data class Breakdown(
        val direct: Map<MuscleGroup, Double>,
        val indirect: Map<MuscleGroup, Double>
    ) {
        val effective: Map<MuscleGroup, Double>
            get() = (direct.keys + indirect.keys).associateWith { muscle ->
                (direct[muscle] ?: 0.0) + (indirect[muscle] ?: 0.0)
            }
    }

    fun forExercises(
        planned: Iterable<PlannedExercise>,
        exercisesById: Map<Long, Exercise> = emptyMap()
    ): Breakdown {
        val direct = mutableMapOf<MuscleGroup, Double>()
        val indirect = mutableMapOf<MuscleGroup, Double>()
        planned.forEach { item ->
            direct[item.primaryMuscle] = (direct[item.primaryMuscle] ?: 0.0) + item.sets
            val source = exercisesById[item.exerciseId]
            source?.secondaryMuscles?.toList()?.take(2)?.forEachIndexed { index, muscle ->
                val coefficient = if (index == 0) 0.5 else 0.25
                indirect[muscle] = (indirect[muscle] ?: 0.0) + item.sets * coefficient
            }
        }
        return Breakdown(direct, indirect)
    }

}
