package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.WorkoutSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FatigueManagerTest {

    private fun session(
        id: Long,
        day: Long,
        sleep: Int? = 4,
        rpe: Double? = 7.0,
        pain: Boolean = false
    ) = WorkoutSession(
        id = id, dayTitle = "Push", dateEpochDay = day, status = SessionStatus.COMPLETED,
        sleepQuality = sleep, perceivedEffort = rpe, painReported = pain
    )

    private fun perf(id: Long, day: Long, weight: Double, reps: Int) = ExercisePerformance(
        id = id, exerciseId = 1, exerciseName = "Panca", sessionId = id, dateEpochDay = day,
        topWeightKg = weight, topReps = reps, totalSets = 4, totalReps = reps * 4,
        volumeKg = weight * reps * 4, estimated1Rm = weight * (1 + reps / 30.0)
    )

    @Test
    fun `insufficient sessions returns no signal`() {
        val a = FatigueManager.assessRecovery(listOf(session(1, 1), session(2, 2)))
        assertThat(a.signal).isEqualTo(FatigueManager.FatigueSignal.NONE)
    }

    @Test
    fun `repeated pain triggers overreaching`() {
        val sessions = (1L..5L).map { session(it, it, pain = it <= 2) }
        val a = FatigueManager.assessRecovery(sessions)
        assertThat(a.signal).isEqualTo(FatigueManager.FatigueSignal.OVERREACHING)
        assertThat(a.severity).isAtLeast(3)
    }

    @Test
    fun `low sleep and high rpe triggers overreaching`() {
        val sessions = (1L..5L).map { session(it, it, sleep = 2, rpe = 9.0) }
        val a = FatigueManager.assessRecovery(sessions)
        assertThat(a.signal).isEqualTo(FatigueManager.FatigueSignal.OVERREACHING)
    }

    @Test
    fun `normal recovery returns none`() {
        val sessions = (1L..5L).map { session(it, it, sleep = 4, rpe = 7.0) }
        assertThat(FatigueManager.assessRecovery(sessions).signal)
            .isEqualTo(FatigueManager.FatigueSignal.NONE)
    }

    @Test
    fun `performance regression is detected`() {
        val performances = listOf(
            perf(4, 4, 70.0, 6), perf(3, 3, 70.0, 7),
            perf(2, 2, 80.0, 10), perf(1, 1, 80.0, 10), perf(0, 0, 80.0, 10)
        )
        val a = FatigueManager.assessPerformance(performances)
        assertThat(a.signal).isEqualTo(FatigueManager.FatigueSignal.REGRESSION)
    }

    @Test
    fun `under stimulus is not interpreted as need for more volume blindly`() {
        // Volume basso ma senza stagnazione non genera overreaching.
        val low = FatigueManager.assessWeeklyVolume(5)
        assertThat(low.signal).isEqualTo(FatigueManager.FatigueSignal.UNDER_STIMULUS)
        assertThat(low.severity).isAtMost(1)
    }

    @Test
    fun `very high volume is flagged`() {
        assertThat(FatigueManager.assessWeeklyVolume(30).signal)
            .isEqualTo(FatigueManager.FatigueSignal.OVERREACHING)
    }

    @Test
    fun `in range volume is fine`() {
        assertThat(FatigueManager.assessWeeklyVolume(14).signal)
            .isEqualTo(FatigueManager.FatigueSignal.NONE)
    }
}
