package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.RoutineExercise

/**
 * What the approval flow needs from storage. Implemented in the data layer.
 *
 * The domain never writes anything by itself: [updateRoutineExercise] is called only
 * from [ProgressionApprovalUseCase.approve], after the user confirms the proposal.
 */
interface ProgressionPort {
    suspend fun routineExercise(id: Long): RoutineExercise?
    suspend fun history(exerciseId: Long): List<LoggedSet>
    suspend fun updateRoutineExercise(exercise: RoutineExercise)
}

/** A progression proposal. It is shown to the user and never applied on its own. */
data class ProgressionProposal(
    val routineExerciseId: Long,
    val exerciseId: Long,
    val currentWeightKg: Double?,
    val suggestedWeightKg: Double?,
    val suggestedReps: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val reason: String,
    val canProgress: Boolean
) {
    val isLoadIncrease: Boolean
        get() = currentWeightKg != null &&
            suggestedWeightKg != null &&
            suggestedWeightKg > currentWeightKg
}

/** The decision taken by the user, recorded with the values actually written. */
data class ProgressionAuditEntry(
    val routineExerciseId: Long,
    val exerciseId: Long,
    val action: String,
    val fromWeightKg: Double?,
    val toWeightKg: Double?,
    val fromReps: Int?,
    val toReps: Int,
    val note: String,
    val atMillis: Long
)

sealed interface ProgressionOutcome {
    data class Applied(
        val proposal: ProgressionProposal,
        val exercise: RoutineExercise,
        val entry: ProgressionAuditEntry
    ) : ProgressionOutcome

    data class Rejected(
        val proposal: ProgressionProposal,
        val reason: String,
        val entry: ProgressionAuditEntry
    ) : ProgressionOutcome

    data class Unavailable(val message: String) : ProgressionOutcome
}

/**
 * Turns the double progression suggested by [ProgressionEngine] into an explicit
 * decision: propose, then apply only when the user approves. A rejected or stale
 * proposal never changes the routine.
 */
class ProgressionApprovalUseCase(
    private val port: ProgressionPort,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    private val entries = mutableListOf<ProgressionAuditEntry>()

    /** Every decision taken in this session, in order. */
    val audit: List<ProgressionAuditEntry> get() = entries.toList()

    suspend fun propose(routineExerciseId: Long): ProgressionProposal? {
        val exercise = port.routineExercise(routineExerciseId) ?: return null
        val suggestion = ProgressionEngine.suggest(exercise, port.history(exercise.exerciseId))
        return ProgressionProposal(
            routineExerciseId = exercise.id,
            exerciseId = exercise.exerciseId,
            currentWeightKg = suggestion.currentWeightKg,
            suggestedWeightKg = suggestion.suggestedWeightKg,
            suggestedReps = suggestion.suggestedReps,
            targetRepsMin = exercise.targetRepsMin,
            targetRepsMax = exercise.targetRepsMax,
            reason = suggestion.reason,
            canProgress = suggestion.canProgress
        )
    }

    /**
     * Applies the proposal. The suggestion is recomputed on the current history, so a
     * proposal built on an old session cannot write a weight the data no longer support.
     */
    suspend fun approve(proposal: ProgressionProposal): ProgressionOutcome {
        val exercise = port.routineExercise(proposal.routineExerciseId)
            ?: return ProgressionOutcome.Unavailable(
                "Esercizio della scheda non trovato: nessuna modifica applicata."
            )

        val fresh = ProgressionEngine.suggest(exercise, port.history(exercise.exerciseId))
        val staleNote = if (
            fresh.suggestedWeightKg != proposal.suggestedWeightKg ||
            fresh.suggestedReps != proposal.suggestedReps
        ) {
            "Proposta ricalcolata sullo storico più recente prima di applicarla."
        } else {
            ""
        }

        val updated = exercise.copy(
            targetWeightKg = fresh.suggestedWeightKg ?: exercise.targetWeightKg,
            targetRepsMin = minOf(exercise.targetRepsMin, fresh.suggestedReps),
            targetRepsMax = maxOf(exercise.targetRepsMax, fresh.suggestedReps)
        )

        val changed = updated != exercise
        if (changed) port.updateRoutineExercise(updated)

        val note = buildString {
            if (staleNote.isNotEmpty()) append(staleNote)
            if (!changed) {
                if (isNotEmpty()) append(' ')
                append("Target già allineato: nessuna scrittura necessaria.")
            }
        }

        val entry = ProgressionAuditEntry(
            routineExerciseId = exercise.id,
            exerciseId = exercise.exerciseId,
            action = "applied",
            fromWeightKg = exercise.targetWeightKg,
            toWeightKg = updated.targetWeightKg,
            fromReps = exercise.targetRepsMax,
            toReps = updated.targetRepsMax,
            note = note.ifEmpty { fresh.reason },
            atMillis = now()
        )
        entries += entry

        return ProgressionOutcome.Applied(
            proposal = proposal.copy(
                currentWeightKg = fresh.currentWeightKg,
                suggestedWeightKg = fresh.suggestedWeightKg,
                suggestedReps = fresh.suggestedReps,
                reason = fresh.reason,
                canProgress = fresh.canProgress
            ),
            exercise = updated,
            entry = entry
        )
    }

    /** Refuses the proposal: the routine is left untouched and the reason is kept. */
    suspend fun reject(proposal: ProgressionProposal, reason: String): ProgressionOutcome.Rejected {
        val entry = ProgressionAuditEntry(
            routineExerciseId = proposal.routineExerciseId,
            exerciseId = proposal.exerciseId,
            action = "rejected",
            fromWeightKg = proposal.currentWeightKg,
            toWeightKg = proposal.suggestedWeightKg,
            fromReps = proposal.targetRepsMax,
            toReps = proposal.suggestedReps,
            note = reason,
            atMillis = now()
        )
        entries += entry
        return ProgressionOutcome.Rejected(proposal, reason, entry)
    }
}
