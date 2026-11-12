package dev.fitcoach.app.domain.model

enum class SessionStatus { PLANNED, IN_PROGRESS, COMPLETED, SKIPPED }

data class WorkoutSession(
    val id: Long = 0L,
    val routineId: Long?,
    val name: String,
    val dateEpochDay: Long,
    val startedAt: Long = 0L,
    val finishedAt: Long? = null,
    val status: SessionStatus = SessionStatus.PLANNED,
    val notes: String = ""
)

data class LoggedSet(
    val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val setIndex: Int,
    val setType: SetType = SetType.WORKING,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rir: Int? = null,
    val rpe: Double? = null,
    val restSeconds: Int? = null,
    val pain: Boolean = false,
    val completed: Boolean = false,
    val notes: String = "",
    val loggedAt: Long = 0L
)

/** Previous performance reference shown while logging the next session. */
data class ExerciseHistoryEntry(
    val exerciseId: Long,
    val lastDateEpochDay: Long,
    val sets: List<LoggedSet>
)
