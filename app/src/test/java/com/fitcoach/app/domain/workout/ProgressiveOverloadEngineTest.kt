package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.WorkoutSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgressiveOverloadEngineTest {

    private fun set(reps: Int, weight: Double = 70.0, rir: Int? = 2) = WorkoutSet(
        exerciseId = 1, exerciseName = "Panca piana", setIndex = 0,
        plannedReps = 10, actualReps = reps, weightKg = weight, rir = rir
    )

    @Test
    fun `no sets keeps weight and asks for logs`() {
        val d = ProgressiveOverloadEngine.decide(70.0, emptyList(), 8, 10, ExerciseCategory.COMPOUND)
        assertThat(d.increaseApplied).isFalse()
        assertThat(d.recommendedWeightKg).isEqualTo(70.0)
    }

    @Test
    fun `double progression adds a rep before weight`() {
        val sets = listOf(set(8), set(8), set(8), set(8))
        val d = ProgressiveOverloadEngine.decide(70.0, sets, 8, 10, ExerciseCategory.COMPOUND)
        assertThat(d.increaseApplied).isFalse()
        assertThat(d.recommendedWeightKg).isEqualTo(70.0)
        assertThat(d.recommendedReps).isEqualTo(9)
    }

    @Test
    fun `progression climbs reps one at a time to the top`() {
        val sets = listOf(set(9), set(9), set(9), set(9))
        val d = ProgressiveOverloadEngine.decide(70.0, sets, 8, 10, ExerciseCategory.COMPOUND)
        assertThat(d.recommendedReps).isEqualTo(10)
        assertThat(d.increaseApplied).isFalse()
    }

    @Test
    fun `increase applied when all sets hit top of range with enough rir`() {
        val sets = listOf(set(10), set(10), set(10), set(10))
        val d = ProgressiveOverloadEngine.decide(70.0, sets, 8, 10, ExerciseCategory.COMPOUND)
        assertThat(d.increaseApplied).isTrue()
        assertThat(d.increaseKg).isEqualTo(ProgressiveOverloadEngine.COMPOUND_INCREASE_KG)
        assertThat(d.recommendedWeightKg).isEqualTo(72.5)
        assertThat(d.recommendedReps).isEqualTo(8)
    }

    @Test
    fun `isolation exercise gets smaller increment`() {
        val sets = listOf(set(12), set(12), set(12))
        val d = ProgressiveOverloadEngine.decide(20.0, sets, 10, 12, ExerciseCategory.ISOLATION)
        assertThat(d.increaseApplied).isTrue()
        assertThat(d.increaseKg).isEqualTo(ProgressiveOverloadEngine.ISOLATION_INCREASE_KG)
    }

    @Test
    fun `no increase when rir too low even at top of range`() {
        val sets = listOf(set(10, rir = 0), set(10, rir = 0))
        val d = ProgressiveOverloadEngine.decide(70.0, sets, 8, 10, ExerciseCategory.COMPOUND)
        assertThat(d.increaseApplied).isFalse()
        assertThat(d.recommendedWeightKg).isEqualTo(70.0)
    }

    @Test
    fun `pain blocks the increase`() {
        val sets = listOf(set(10), set(10))
        val d = ProgressiveOverloadEngine.decide(
            70.0, sets, 8, 10, ExerciseCategory.COMPOUND, painReported = true
        )
        assertThat(d.increaseApplied).isFalse()
        assertThat(d.holdBecauseOfPainOrTechnique).isTrue()
        assertThat(d.reason).contains("Dolore")
    }

    @Test
    fun `poor technique blocks the increase`() {
        val sets = listOf(set(10), set(10))
        val d = ProgressiveOverloadEngine.decide(
            70.0, sets, 8, 10, ExerciseCategory.COMPOUND, poorTechnique = true
        )
        assertThat(d.increaseApplied).isFalse()
        assertThat(d.holdBecauseOfPainOrTechnique).isTrue()
    }

    @Test
    fun `epley one rep max formula`() {
        // 100 kg x 10 reps -> 100 * (1 + 10/30) = 133.33
        assertThat(ProgressiveOverloadEngine.estimated1Rm(100.0, 10)).isWithin(0.01).of(133.33)
    }

    @Test
    fun `warmup sets are ignored`() {
        val working = listOf(set(10), set(10))
        val warmups = listOf(
            WorkoutSet(exerciseId = 1, exerciseName = "Panca piana", setIndex = 0,
                plannedReps = 10, actualReps = 5, weightKg = 40.0, isWarmup = true)
        )
        val d = ProgressiveOverloadEngine.decide(70.0, working + warmups, 8, 10, ExerciseCategory.COMPOUND)
        assertThat(d.increaseApplied).isTrue()
    }
}
