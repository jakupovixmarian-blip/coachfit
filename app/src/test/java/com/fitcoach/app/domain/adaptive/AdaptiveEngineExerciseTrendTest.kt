package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.ExercisePerformance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdaptiveEngineExerciseTrendTest {

    private fun perf(day: Long, oneRm: Double, name: String = "Panca piana") = ExercisePerformance(
        exerciseId = 1L, exerciseName = name, sessionId = day, dateEpochDay = day,
        topWeightKg = 80.0, topReps = 8, totalSets = 4, totalReps = 32,
        volumeKg = 2560.0, estimated1Rm = oneRm
    )

    @Test
    fun `insufficient history produces no proposals`() {
        val map = mapOf("Panca piana" to listOf(perf(5, 100.0), perf(4, 100.0), perf(3, 100.0)))
        assertThat(AdaptiveEngine.evaluateExerciseTrends(map)).isEmpty()
    }

    @Test
    fun `regression triggers a volume reduction proposal`() {
        // recent 3 average lower than previous 3
        val map = mapOf(
            "Panca piana" to listOf(
                perf(6, 95.0), perf(5, 95.0), perf(4, 96.0), // recent
                perf(3, 105.0), perf(2, 105.0), perf(1, 105.0) // previous
            )
        )
        val changes = AdaptiveEngine.evaluateExerciseTrends(map)
        assertThat(changes).hasSize(1)
        assertThat(changes.first().domain).isEqualTo(ChangeDomain.WORKOUT)
        assertThat(changes.first().proposedChange).contains("riduci il volume")
    }

    @Test
    fun `solid progression does not change the exercise`() {
        val map = mapOf(
            "Squat" to listOf(
                perf(6, 120.0, "Squat"), perf(5, 120.0, "Squat"), perf(4, 121.0, "Squat"),
                perf(3, 100.0, "Squat"), perf(2, 100.0, "Squat"), perf(1, 100.0, "Squat")
            )
        )
        val changes = AdaptiveEngine.evaluateExerciseTrends(map)
        assertThat(changes).hasSize(1)
        assertThat(changes.first().proposedChange).contains("Prosegui con la double progression")
    }

    @Test
    fun `stable performance produces no proposal`() {
        val map = mapOf(
            "Rematore" to listOf(
                perf(6, 100.0, "Rematore"), perf(5, 100.5, "Rematore"), perf(4, 100.0, "Rematore"),
                perf(3, 100.0, "Rematore"), perf(2, 99.5, "Rematore"), perf(1, 100.0, "Rematore")
            )
        )
        assertThat(AdaptiveEngine.evaluateExerciseTrends(map)).isEmpty()
    }
}
