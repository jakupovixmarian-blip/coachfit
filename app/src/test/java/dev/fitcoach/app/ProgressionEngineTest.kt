package dev.fitcoach.app

import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SetType
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private val routineExercise = RoutineExercise(
        id = 1L,
        routineId = 1L,
        exerciseId = 10L,
        orderIndex = 0,
        targetSets = 3,
        targetRepsMin = 8,
        targetRepsMax = 12,
        targetRir = 2,
        restSeconds = 90
    )

    private fun set(weight: Double?, reps: Int?, rir: Int?, pain: Boolean = false, type: SetType = SetType.WORKING) =
        LoggedSet(
            id = 0L,
            sessionId = 1L,
            exerciseId = 10L,
            orderIndex = 0,
            setIndex = 1,
            setType = type,
            weightKg = weight,
            reps = reps,
            rir = rir,
            pain = pain,
            completed = true,
            loggedAt = 1_000L
        )

    @Test
    fun `no history returns a start-from-bottom suggestion`() {
        val suggestion = ProgressionEngine.suggest(routineExercise, emptyList())
        assertFalse(suggestion.canProgress)
        assertNull(suggestion.suggestedWeightKg)
        assertEquals(8, suggestion.suggestedReps)
        assertTrue(suggestion.reason.contains("Nessuno storico"))
    }

    @Test
    fun `reaching top of range with target RIR increases load`() {
        val history = listOf(set(60.0, 12, 2), set(60.0, 12, 2))
        val suggestion = ProgressionEngine.suggest(routineExercise, history)
        assertTrue(suggestion.canProgress)
        assertEquals(62.5, suggestion.suggestedWeightKg!!, 0.001)
        assertEquals(8, suggestion.suggestedReps)
    }

    @Test
    fun `below top of range keeps load and increases reps`() {
        val history = listOf(set(50.0, 9, 2))
        val suggestion = ProgressionEngine.suggest(routineExercise, history)
        assertFalse(suggestion.canProgress)
        assertEquals(50.0, suggestion.suggestedWeightKg!!, 0.001)
        assertEquals(10, suggestion.suggestedReps)
    }

    @Test
    fun `pain blocks any load increase`() {
        val history = listOf(set(80.0, 12, 1, pain = true))
        val suggestion = ProgressionEngine.suggest(routineExercise, history)
        assertFalse(suggestion.canProgress)
        assertEquals(80.0, suggestion.suggestedWeightKg!!, 0.001)
        assertTrue(suggestion.reason.contains("Dolore"))
    }

    @Test
    fun `sets that are too easy do not increase load`() {
        val history = listOf(set(60.0, 12, 5))
        val suggestion = ProgressionEngine.suggest(routineExercise, history)
        assertFalse(suggestion.canProgress)
        assertEquals(60.0, suggestion.suggestedWeightKg!!, 0.001)
        assertTrue(suggestion.reason.contains("RIR"))
    }

    @Test
    fun `volume ignores warmup and incomplete sets`() {
        val sets = listOf(
            set(100.0, 5, 2, type = SetType.WARMUP),
            set(100.0, 5, 2),
            set(100.0, 5, 2).copy(completed = false)
        )
        assertEquals(500.0, ProgressionEngine.volume(sets), 0.001)
    }

    @Test
    fun `one rep max uses epley and rejects invalid input`() {
        assertEquals(116.67, ProgressionEngine.estimatedOneRepMax(100.0, 5)!!, 0.05)
        assertNull(ProgressionEngine.estimatedOneRepMax(0.0, 5))
        assertNull(ProgressionEngine.estimatedOneRepMax(100.0, 0))
        assertNull(ProgressionEngine.estimatedOneRepMax(100.0, 20))
    }

    @Test
    fun `average RIR is null when no RIR recorded`() {
        assertNull(ProgressionEngine.averageRir(listOf(set(60.0, 8, null))))
        assertEquals(2.0, ProgressionEngine.averageRir(listOf(set(60.0, 8, 2), set(60.0, 8, 2)))!!, 0.001)
    }
}
