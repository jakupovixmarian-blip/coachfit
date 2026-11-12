package com.fitcoach.workout

import kotlin.math.max
import kotlin.math.roundToInt

enum class ExerciseType { COMPOUND, ISOLATION, BODYWEIGHT }

data class ProgressionInput(
    val type: ExerciseType,
    val loadKg: Double,
    val completedReps: List<Int>,
    val minReps: Int,
    val maxReps: Int,
    val rir: Double?,
    val formWasGood: Boolean,
    val painReported: Boolean
)

data class ProgressionDecision(
    val nextLoadKg: Double,
    val increase: Boolean,
    val reason: String
)

object DoubleProgressionEngine {
    fun decide(input: ProgressionInput): ProgressionDecision {
        require(input.completedReps.isNotEmpty())
        require(input.minReps in 1..input.maxReps)
        if (input.painReported) return ProgressionDecision(input.loadKg, false, "Dolore segnalato: mantieni il carico e valuta una sostituzione")
        if (!input.formWasGood) return ProgressionDecision(input.loadKg, false, "Tecnica non validata: nessun aumento automatico")
        if (input.rir != null && input.rir < 1.0) return ProgressionDecision(input.loadKg, false, "RIR insufficiente: consolida il carico")
        val reachedTop = input.completedReps.all { it >= input.maxReps }
        if (!reachedTop) return ProgressionDecision(input.loadKg, false, "Completa il range alto prima di aumentare")
        return when (input.type) {
            ExerciseType.BODYWEIGHT -> ProgressionDecision(input.loadKg, true, "Aggiungi una ripetizione o una piccola zavorra")
            ExerciseType.COMPOUND -> adjusted(input.loadKg, 0.025, "Range completato: incremento proporzionato al multiarticolare")
            ExerciseType.ISOLATION -> adjusted(input.loadKg, 0.0125, "Range completato: incremento conservativo per isolamento")
        }
    }

    private fun adjusted(loadKg: Double, percentage: Double, reason: String): ProgressionDecision {
        val increment = max(0.5, (loadKg * percentage * 2).roundToInt() / 2.0)
        return ProgressionDecision(loadKg + increment, true, reason)
    }
}
