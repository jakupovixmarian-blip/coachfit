package com.fitcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.WorkoutSet
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkoutSessionPersistenceTest {
    private lateinit var db: FitCoachDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), FitCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = WorkoutRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `empty session cannot be completed and valid session is completed once`() = runBlocking {
        val day = 1_700_000_000L
        val sessionId = repository.startFreeSession(day)
        assertThat(
            repository.completeSession(sessionId, 10, 7.0, 3, false, "")
        ).isFalse()
        assertThat(repository.getSessionForDay(day)?.status?.name).isEqualTo("IN_PROGRESS")

        assertThat(repository.saveSet(sessionId, WorkoutSet(
            sessionId = sessionId,
            exerciseId = 1L,
            exerciseName = "Test",
            setIndex = 0,
            plannedReps = 10,
            actualReps = 8,
            weightKg = 40.0
        ))).isGreaterThan(0L)
        assertThat(
            repository.completeSession(sessionId, 10, 7.0, 3, false, "")
        ).isTrue()
        assertThat(
            repository.completeSession(sessionId, 10, 7.0, 3, false, "")
        ).isFalse()
        val completed = repository.getSessionForDay(day)
        assertThat(completed?.status?.name).isEqualTo("COMPLETED")
        assertThat(completed?.completedSets).isEqualTo(1)
        assertThat(completed?.totalVolumeKg).isEqualTo(320.0)
    }

    @Test
    fun `legacy zero-set completed session is repaired without deletion`() = runBlocking {
        val day = 1_700_000_001L
        val sessionId = repository.startFreeSession(day)
        val entity = db.workoutSessionDao().getById(sessionId)!!
        db.workoutSessionDao().update(entity.copy(status = com.fitcoach.app.domain.model.SessionStatus.COMPLETED))

        assertThat(repository.repairInvalidCompletedSessions()).isEqualTo(1)
        val repaired = repository.getSessionForDay(day)
        // Riaperta: la sessione torna registrabile invece di restare bloccata.
        assertThat(repaired?.status?.name).isEqualTo("IN_PROGRESS")
        assertThat(db.workoutSessionDao().getById(sessionId)).isNotNull()
        assertThat(repository.saveSet(sessionId, WorkoutSet(
            sessionId = sessionId,
            exerciseId = 1L,
            exerciseName = "Test",
            setIndex = 0,
            plannedReps = 10,
            actualReps = 8,
            weightKg = 40.0
        ))).isGreaterThan(0L)
        assertThat(repository.completeSession(sessionId, 10, 7.0, 3, false, "")).isTrue()
    }

    @Test
    fun `a completed session with valid sets is never reopened`() = runBlocking {
        val day = 1_700_000_002L
        val sessionId = repository.startFreeSession(day)
        repository.saveSet(sessionId, WorkoutSet(
            sessionId = sessionId, exerciseId = 1L, exerciseName = "Test",
            setIndex = 0, plannedReps = 10, actualReps = 9, weightKg = 50.0
        ))
        assertThat(repository.completeSession(sessionId, 10, 7.0, 3, false, "")).isTrue()

        assertThat(repository.repairInvalidCompletedSessions()).isEqualTo(0)
        assertThat(repository.getSessionForDay(day)?.status?.name).isEqualTo("COMPLETED")
    }
}
