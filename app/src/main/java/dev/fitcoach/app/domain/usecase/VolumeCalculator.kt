package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.MuscleGroup

/**
 * Weekly volume accounting.
 *
 * A set contributes a *direct* set to the primary muscle and a smaller *indirect*
 * contribution to each secondary muscle. Both are summed over the whole program,
 * which is what actually decides whether a plan is balanced or overreaching.
 */
object VolumeCalculator {

    /** Indirect contribution of a set to a secondary muscle. */
    const val INDIRECT_FRACTION = 0.5

    /** Weekly direct-set budget per muscle, by experience level. */
    fun weeklyDirectTarget(level: ExperienceLevel): Int = when (level) {
        ExperienceLevel.BEGINNER -> 10
        ExperienceLevel.INTERMEDIATE -> 16
        ExperienceLevel.ADVANCED -> 20
    }

    /** Hard ceiling on weekly direct sets for one muscle, by experience level. */
    fun weeklyDirectCeiling(level: ExperienceLevel): Int = when (level) {
        ExperienceLevel.BEGINNER -> 14
        ExperienceLevel.INTERMEDIATE -> 20
        ExperienceLevel.ADVANCED -> 26
    }

    /**
     * Ceiling on the *effective* sets (direct + indirect) that a single session
     * should impose on one muscle. Going above this in one day is the main cause
     * of 30-45 set sessions that cannot be recovered from.
     */
    fun sessionEffectiveCeiling(level: ExperienceLevel): Double = when (level) {
        ExperienceLevel.BEGINNER -> 7.0
        ExperienceLevel.INTERMEDIATE -> 9.0
        ExperienceLevel.ADVANCED -> 11.0
    }

    /** Minutes a single working set costs, including rest, by goal. */
    fun minutesPerSet(restSeconds: Int): Double = (restSeconds + 45) / 60.0

    /**
     * Effective sets a session imposes on each muscle, counting the primary
     * muscle in full and every secondary muscle at [INDIRECT_FRACTION].
     */
    fun effectiveSetsByMuscle(session: List<ExerciseWithSets>): Map<MuscleGroup, Double> {
        val result = mutableMapOf<MuscleGroup, Double>()
        session.forEach { item ->
            result.merge(item.exercise.primaryMuscle, item.sets.toDouble()) { a, b -> a + b }
            item.exercise.secondaryMuscles.forEach { muscle ->
                result.merge(muscle, item.sets * INDIRECT_FRACTION) { a, b -> a + b }
            }
        }
        return result
    }

    /** Weekly direct sets per muscle across every session of a program. */
    fun weeklyDirectSets(sessions: List<List<ExerciseWithSets>>): Map<MuscleGroup, Int> {
        val result = mutableMapOf<MuscleGroup, Int>()
        sessions.forEach { session ->
            session.forEach { item ->
                result.merge(item.exercise.primaryMuscle, item.sets) { a, b -> a + b }
            }
        }
        return result
    }

    /**
     * Weekly frequency per muscle: how many distinct sessions train it, either
     * directly or indirectly.
     */
    fun weeklyFrequency(sessions: List<List<ExerciseWithSets>>): Map<MuscleGroup, Int> {
        val result = mutableMapOf<MuscleGroup, Int>()
        sessions.forEach { session ->
            val touched = mutableSetOf<MuscleGroup>()
            session.forEach { item ->
                touched += item.exercise.primaryMuscle
                touched += item.exercise.secondaryMuscles
            }
            touched.forEach { muscle -> result.merge(muscle, 1) { a, b -> a + b } }
        }
        return result
    }

    /** Total working sets of a session. */
    fun sessionSetCount(session: List<ExerciseWithSets>): Int = session.sumOf { it.sets }
}

data class ExerciseWithSets(val exercise: Exercise, val sets: Int)
