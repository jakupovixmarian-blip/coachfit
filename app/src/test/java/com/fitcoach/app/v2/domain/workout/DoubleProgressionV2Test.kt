package com.fitcoach.app.v2.domain.workout

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DoubleProgressionV2Test {
    private val prescription = ExercisePrescriptionV2(
        exerciseId = "bench_press",
        category = ExerciseCategoryV2.COMPOUND,
        sets = 4,
        minReps = 8,
        maxReps = 10,
        targetRir = 2,
        currentLoadKg = 70.0
    )

    @Test
    fun `increase only after all sets reach upper range with adequate rir`() {
        val decision = DoubleProgressionEngineV2.decide(
            prescription,
            List(4) { SetPerformanceV2(70.0, 10, 2) }
        )

        assertThat(decision.action).isEqualTo(ProgressionActionV2.INCREASE_LOAD)
        assertThat(decision.nextLoadKg).isGreaterThan(70.0)
    }

    @Test
    fun `poor technique pain and insufficient rir block automatic increase`() {
        val belowRange = DoubleProgressionEngineV2.decide(
            prescription,
            List(4) { SetPerformanceV2(70.0, 10, 1) }
        )
        val pain = DoubleProgressionEngineV2.decide(
            prescription,
            List(4) { SetPerformanceV2(70.0, 10, 2, painReported = true) }
        )
        val technique = DoubleProgressionEngineV2.decide(
            prescription,
            List(4) { SetPerformanceV2(70.0, 10, 2, techniqueValid = false) }
        )

        assertThat(belowRange.action).isEqualTo(ProgressionActionV2.HOLD_LOAD)
        assertThat(pain.action).isEqualTo(ProgressionActionV2.STOP_AND_REVIEW)
        assertThat(technique.action).isEqualTo(ProgressionActionV2.HOLD_LOAD)
        assertThat(pain.nextLoadKg).isEqualTo(70.0)
    }

    @Test
    fun `isolation and bodyweight increments are handled separately`() {
        val isolation = prescription.copy(category = ExerciseCategoryV2.ISOLATION, currentLoadKg = 10.0)
        val bodyweight = prescription.copy(category = ExerciseCategoryV2.BODYWEIGHT, currentLoadKg = 0.0)
        val isolationDecision = DoubleProgressionEngineV2.decide(isolation, List(4) { SetPerformanceV2(10.0, 10, 2) })
        val bodyweightDecision = DoubleProgressionEngineV2.decide(bodyweight, List(4) { SetPerformanceV2(0.0, 10, 2) })

        assertThat(isolationDecision.estimatedIncrementKg).isEqualTo(0.5)
        assertThat(bodyweightDecision.estimatedIncrementKg).isEqualTo(1.25)
    }
}
