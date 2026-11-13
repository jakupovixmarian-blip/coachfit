package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.SessionRepository

/**
 * The only thing the resume flow needs from storage. Narrow on purpose, so the flow can
 * be tested without a database and stays independent of the Room schema.
 */
interface ActiveSessionSource {
    suspend fun inProgress(): WorkoutSession?
    suspend fun sets(sessionId: Long): List<LoggedSet>
}

/** Adapter over the existing repository: no new queries, no duplicated SQL. */
class SessionRepositorySource(
    private val repository: SessionRepository
) : ActiveSessionSource {
    override suspend fun inProgress(): WorkoutSession? = repository.getInProgress()
    override suspend fun sets(sessionId: Long): List<LoggedSet> = repository.getSets(sessionId)
}

/** What the UI shows when the app opens and a session was left open. */
data class ResumableSession(
    val session: WorkoutSession,
    val sets: List<LoggedSet>,
    val completedSets: Int,
    val volumeKg: Double,
    val openForHours: Long,
    val message: String
) {
    /** A session with no effective set has nothing to resume, only to start or close. */
    val hasRecordedWork: Boolean get() = completedSets > 0

    /** Left open long enough that the user should be told, not silently dropped into it. */
    val isStale: Boolean get() = openForHours >= ResumeSessionUseCase.STALE_HOURS
}

/**
 * Finds a session that was left in progress and rebuilds what the user had recorded.
 *
 * Nothing is deleted or completed here: closing the app must never lose a workout, so
 * the flow only reports the state and lets the user decide whether to resume or close it.
 */
class ResumeSessionUseCase(
    private val source: ActiveSessionSource,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun resume(): ResumableSession? {
        val session = source.inProgress() ?: return null
        val sets = source.sets(session.id)
            .sortedWith(compareBy({ it.orderIndex }, { it.setIndex }))

        val effective = sets.filter { it.completed && (it.reps ?: 0) > 0 }
        val volume = effective.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0).toDouble() }
        val openForHours = if (session.startedAt > 0L) {
            ((clock() - session.startedAt) / 3_600_000L).coerceAtLeast(0L)
        } else {
            0L
        }

        val message = when {
            effective.isEmpty() -> "Seduta aperta senza serie registrate: puoi iniziare o chiuderla."
            openForHours >= STALE_HOURS ->
                "Seduta rimasta aperta da $openForHours ore: riprendi o chiudi."
            else -> "Riprendi la seduta: ${effective.size} serie già registrate."
        }

        return ResumableSession(
            session = session,
            sets = sets,
            completedSets = effective.size,
            volumeKg = volume,
            openForHours = openForHours,
            message = message
        )
    }

    companion object {
        /** Hours after which an open session is reported as stale instead of resumed silently. */
        const val STALE_HOURS = 12L
    }
}
