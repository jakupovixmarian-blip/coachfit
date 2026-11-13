package dev.fitcoach.app

import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SetType
import dev.fitcoach.app.domain.usecase.ProgressionApprovalUseCase
import dev.fitcoach.app.domain.usecase.ProgressionOutcome
import dev.fitcoach.app.domain.usecase.ProgressionPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A progression must never change the routine by itself: propose, then apply only
 * after the user approves. The audit keeps what was decided and what was written.
 */
class ProgressionApprovalTest {

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

    private fun set(weight: Double, reps: Int, rir: Int?, pain: Boolean = false) = LoggedSet(
        id = 0L,
        sessionId = 1L,
        exerciseId = 10L,
        orderIndex = 0,
        setIndex = 1,
        setType = SetType.WORKING,
        weightKg = weight,
        reps = reps,
        rir = rir,
        pain = pain,
        completed = true,
        loggedAt = 1_000L
    )

    private class FakePort(
        val exercises: MutableMap<Long, RoutineExercise>,
        val history: MutableMap<Long, List<LoggedSet>>
    ) : ProgressionPort {
        var writes = 0
        override suspend fun routineExercise(id: Long): RoutineExercise? = exercises[id]
        override suspend fun history(exerciseId: Long): List<LoggedSet> = history[exerciseId].orEmpty()
        override suspend fun updateRoutineExercise(exercise: RoutineExercise) {
            writes++
            exercises[exercise.id] = exercise
        }
    }

    private fun port(history: List<LoggedSet>) = FakePort(
        exercises = mutableMapOf(1L to routineExercise),
        history = mutableMapOf(10L to history)
    )

    @Test
    fun `la proposta non scrive nulla nella scheda`() = kotlinx.coroutines.runBlocking {
        val port = port(listOf(set(60.0, 12, 2)))
        val useCase = ProgressionApprovalUseCase(port)

        val proposal = useCase.propose(1L)!!

        assertEquals(0, port.writes)
        assertTrue(proposal.canProgress)
        assertEquals(62.5, proposal.suggestedWeightKg!!, 0.001)
        assertTrue(proposal.reason.isNotBlank())
    }

    @Test
    fun `l'approvazione scrive il carico suggerito`() = kotlinx.coroutines.runBlocking {
        val port = port(listOf(set(60.0, 12, 2)))
        val useCase = ProgressionApprovalUseCase(port, now = { 5_000L })

        val proposal = useCase.propose(1L)!!
        val outcome = useCase.approve(proposal)

        assertTrue(outcome is ProgressionOutcome.Applied)
        outcome as ProgressionOutcome.Applied
        assertEquals(62.5, outcome.exercise.targetWeightKg!!, 0.001)
        assertEquals(1, port.writes)
        assertEquals(62.5, port.exercises[1L]!!.targetWeightKg!!, 0.001)
        assertEquals("applied", outcome.entry.action)
        assertEquals(5_000L, outcome.entry.atMillis)
    }

    @Test
    fun `il dolore blocca l'aumento anche dopo l'approvazione`() = kotlinx.coroutines.runBlocking {
        val port = port(listOf(set(80.0, 12, 1, pain = true)))
        val useCase = ProgressionApprovalUseCase(port)

        val proposal = useCase.propose(1L)!!
        assertFalse(proposal.canProgress)

        val outcome = useCase.approve(proposal)
        outcome as ProgressionOutcome.Applied
        assertNull(outcome.exercise.targetWeightKg)
        assertEquals(0, port.writes)
        assertTrue(outcome.entry.note.contains("Dolore") || outcome.entry.note.contains("allineato"))
    }

    @Test
    fun `il rifiuto non tocca la scheda e registra il motivo`() = kotlinx.coroutines.runBlocking {
        val port = port(listOf(set(60.0, 12, 2)))
        val useCase = ProgressionApprovalUseCase(port)

        val proposal = useCase.propose(1L)!!
        val outcome = useCase.reject(proposal, "Preferisco consolidare la tecnica")

        assertEquals(0, port.writes)
        assertEquals("rejected", outcome.entry.action)
        assertEquals("Preferisco consolidare la tecnica", outcome.entry.note)
        assertEquals(1, useCase.audit.size)
    }

    @Test
    fun `una proposta basata su storico vecchio viene ricalcolata`() = kotlinx.coroutines.runBlocking {
        val port = port(listOf(set(60.0, 12, 2)))
        val useCase = ProgressionApprovalUseCase(port)

        val proposal = useCase.propose(1L)!!
        assertEquals(62.5, proposal.suggestedWeightKg!!, 0.001)

        // A new session arrived after the proposal: 9 reps, no load increase is due.
        port.history[10L] = listOf(set(60.0, 9, 2))
        val outcome = useCase.approve(proposal)

        outcome as ProgressionOutcome.Applied
        assertEquals(60.0, outcome.exercise.targetWeightKg ?: 60.0, 0.001)
        assertTrue(outcome.entry.note.contains("ricalcolata"))
    }

    @Test
    fun `un esercizio inesistente non produce proposte ne scritture`() = kotlinx.coroutines.runBlocking {
        val port = port(emptyList())
        val useCase = ProgressionApprovalUseCase(port)

        assertNull(useCase.propose(99L))

        val missing = ProgressionApprovalUseCase(port).approve(
            dev.fitcoach.app.domain.usecase.ProgressionProposal(
                routineExerciseId = 99L,
                exerciseId = 10L,
                currentWeightKg = 60.0,
                suggestedWeightKg = 62.5,
                suggestedReps = 8,
                targetRepsMin = 8,
                targetRepsMax = 12,
                reason = "test",
                canProgress = true
            )
        )

        assertTrue(missing is ProgressionOutcome.Unavailable)
        assertEquals(0, port.writes)
    }

    @Test
    fun `il registro accumula le decisioni in ordine`() = kotlinx.coroutines.runBlocking {
        val port = port(listOf(set(60.0, 12, 2)))
        val useCase = ProgressionApprovalUseCase(port)

        val first = useCase.propose(1L)!!
        useCase.reject(first, "non ora")
        useCase.approve(first)

        assertEquals(listOf("rejected", "applied"), useCase.audit.map { it.action })
    }
}
