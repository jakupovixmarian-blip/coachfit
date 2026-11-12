package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.RoutineExercise
import kotlin.math.max
import kotlin.math.roundToInt

data class ProgressionSuggestion(
    val exerciseId: Long,
    val currentWeightKg: Double?,
    val suggestedWeightKg: Double?,
    val suggestedReps: Int,
    val reason: String,
    val canProgress: Boolean
)

/**
 * Double progression: reps are increased inside the target range first; the load
 * increases only when the top of the range is completed at the target RIR with
 * no pain and no declared technique failure.
 */
object ProgressionEngine {

    fun suggest(
        routineExercise: RoutineExercise,
        history: List<LoggedSet>
    ): ProgressionSuggestion {
        val workingSets = history.filter { it.completed && it.setType.name != "WARMUP" }
        if (workingSets.isEmpty()) {
            return ProgressionSuggestion(
                exerciseId = routineExercise.exerciseId,
                currentWeightKg = null,
                suggestedWeightKg = null,
                suggestedReps = routineExercise.targetRepsMin,
                reason = "Nessuno storico: parti dal range basso e registra la prima seduta.",
                canProgress = false
            )
        }
        val lastWeight = workingSets.maxOfOrNull { it.weightKg ?: 0.0 } ?: 0.0
        val lastReps = workingSets.mapNotNull { it.reps }.maxOrNull() ?: routineExercise.targetRepsMin
        val minRir = workingSets.mapNotNull { it.rir }.minOrNull()
        val anyPain = workingSets.any { it.pain }

        if (anyPain) {
            return ProgressionSuggestion(
                exerciseId = routineExercise.exerciseId,
                currentWeightKg = lastWeight,
                suggestedWeightKg = lastWeight,
                suggestedReps = lastReps,
                reason = "Dolore segnalato: nessun aumento di carico. Mantieni o riduci e verifica la tecnica.",
                canProgress = false
            )
        }
        val targetRir = routineExercise.targetRir
        val rirTooHigh = minRir != null && minRir > targetRir + 1
        if (rirTooHigh) {
            return ProgressionSuggestion(
                exerciseId = routineExercise.exerciseId,
                currentWeightKg = lastWeight,
                suggestedWeightKg = lastWeight,
                suggestedReps = lastReps,
                reason = "RIR troppo alto (serie facili): non si aumenta il carico, aumenta prima le ripetizioni.",
                canProgress = false
            )
        }
        val reachedTop = lastReps >= routineExercise.targetRepsMax
        val goodRir = minRir == null || minRir <= targetRir
        return if (reachedTop && goodRir) {
            val increment = if (lastWeight >= 60) 2.5 else 1.25
            ProgressionSuggestion(
                exerciseId = routineExercise.exerciseId,
                currentWeightKg = lastWeight,
                suggestedWeightKg = lastWeight + increment,
                suggestedReps = routineExercise.targetRepsMin,
                reason = "Top del range raggiunto con RIR adeguato: aumenta il carico e riparti dal range basso.",
                canProgress = true
            )
        } else {
            val nextReps = (lastReps + 1).coerceAtMost(routineExercise.targetRepsMax)
            ProgressionSuggestion(
                exerciseId = routineExercise.exerciseId,
                currentWeightKg = lastWeight,
                suggestedWeightKg = lastWeight,
                suggestedReps = nextReps,
                reason = "Mantieni il carico e punta a $nextReps ripetizioni (progressione doppia).",
                canProgress = false
            )
        }
    }

    /** Estimated 1RM using Epley's formula. */
    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0 || reps <= 0 || reps > 15) return null
        return weightKg * (1 + reps / 30.0)
    }

    fun volume(sets: List<LoggedSet>): Double =
        sets.filter { it.completed && it.setType.name != "WARMUP" }
            .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }

    fun totalReps(sets: List<LoggedSet>): Int = sets.sumOf { it.reps ?: 0 }

    fun averageRir(sets: List<LoggedSet>): Double? {
        val values = sets.mapNotNull { it.rir }
        if (values.isEmpty()) return null
        return values.average()
    }

    fun bestSet(sets: List<LoggedSet>): LoggedSet? =
        sets.filter { it.completed }.maxByOrNull { (it.weightKg ?: 0.0) * max(1, it.reps ?: 1) }
}
