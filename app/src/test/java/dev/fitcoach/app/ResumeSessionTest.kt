package dev.fitcoach.app

import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.usecase.ActiveSessionSource
import dev.fitcoach.app.domain.usecase.ResumeSessionUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Opening the app with a session left open must restore exactly what was recorded.
 */
class ResumeSessionTest {

    private var now = 10_000_000L

    private class FakeSource(
        private val session: WorkoutSession?,
        private val sets: List<LoggedSet> = emptyList()
    ) : ActiveSessionSource {
        override suspend fun inProgress(): WorkoutSession? = session
        override suspend fun sets(sessionId: Long): List<LoggedSet> = sets
    }

    private fun session(startedAt: Long = 10_000_000L - 3_600_000L) = WorkoutSession(
        id = 7L,
        routineId = 1L,
        name = "Upper A",
        dateEpochDay = 20_000L,
        startedAt = startedAt,
        status = SessionStatus.IN_PROGRESS
    )

    private fun set(index: Int, weight: Double?, reps: Int?, completed: Boolean, order: Int = 0) = LoggedSet(
        id = index.toLong(),
        sessionId = 7L,
        exerciseId = 1L,
        orderIndex = order,
        setIndex = index,
        weightKg = weight,
        reps = reps,
        completed = completed
    )

    private fun useCase(source: ActiveSessionSource) = ResumeSessionUseCase(source) { now }

    @Test
    fun `senza sessione aperta non c'e nulla da riprendere`() {
        val result = runBlocking { useCase(FakeSource(null)).resume() }

        assertNull(result)
    }

    @Test
    fun `ricostruisce le serie registrate e il volume`() {
        val source = FakeSource(
            session(),
            listOf(
                set(2, 80.0, 8, completed = true, order = 1),
                set(1, 80.0, 10, completed = true, order = 0),
                set(3, 80.0, 8, completed = false, order = 1)
            )
        )

        val result = runBlocking { useCase(source).resume() }!!

        assertEquals(7L, result.session.id)
        assertEquals(3, result.sets.size)
        assertEquals(listOf(1, 2, 3), result.sets.map { it.setIndex })
        assertEquals(2, result.completedSets)
        assertEquals(80.0 * 10 + 80.0 * 8, result.volumeKg, 0.001)
        assertTrue(result.hasRecordedWork)
        assertTrue(result.message.contains("2 serie"))
    }

    @Test
    fun `una serie completata senza ripetizioni non conta come lavoro`() {
        val source = FakeSource(session(), listOf(set(1, 60.0, 0, completed = true)))

        val result = runBlocking { useCase(source).resume() }!!

        assertEquals(0, result.completedSets)
        assertEquals(0.0, result.volumeKg, 0.001)
        assertFalse(result.hasRecordedWork)
        assertTrue(result.message.contains("senza serie"))
    }

    @Test
    fun `una seduta rimasta aperta troppo a lungo viene segnalata`() {
        val source = FakeSource(
            session(startedAt = now - 20 * 3_600_000L),
            listOf(set(1, 100.0, 5, completed = true))
        )

        val result = runBlocking { useCase(source).resume() }!!

        assertTrue(result.isStale)
        assertEquals(20L, result.openForHours)
        assertTrue(result.message.contains("20 ore"))
    }

    @Test
    fun `una seduta appena iniziata non viene segnalata come vecchia`() {
        val source = FakeSource(
            session(startedAt = now - 600_000L),
            listOf(set(1, 100.0, 5, completed = true))
        )

        val result = runBlocking { useCase(source).resume() }!!

        assertFalse(result.isStale)
        assertEquals(0L, result.openForHours)
    }
}
