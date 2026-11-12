package com.fitcoach.app.v2.domain.workout

import kotlin.math.max
import kotlin.math.round

enum class ExerciseCategoryV2 { COMPOUND, ISOLATION, BODYWEIGHT }

data class ExercisePrescriptionV2(
    val exerciseId: String,
    val category: ExerciseCategoryV2,
    val sets: Int,
    val minReps: Int,
    val maxReps: Int,
    val targetRir: Int,
    val currentLoadKg: Double
)

data class SetPerformanceV2(
    val loadKg: Double,
    val repetitions: Int,
    val rir: Int?,
    val techniqueValid: Boolean = true,
    val painReported: Boolean = false
)

enum class ProgressionActionV2 { INCREASE_LOAD, HOLD_LOAD, REDUCE_LOAD, STOP_AND_REVIEW }

data class ProgressionDecisionV2(
    val action: ProgressionActionV2,
    val nextLoadKg: Double,
    val reason: String,
    val estimatedIncrementKg: Double
)

object DoubleProgressionEngineV2 {
    fun decide(
        prescription: ExercisePrescriptionV2,
        performance: List<SetPerformanceV2>
    ): ProgressionDecisionV2 {
        require(prescription.sets > 0) { "Una prescrizione deve avere almeno una serie." }
        require(prescription.minReps > 0 && prescription.maxReps >= prescription.minReps) {
            "Il range di ripetizioni non è valido."
        }
        require(prescription.targetRir in 0..5) { "Il RIR target deve essere tra 0 e 5." }
        require(performance.size == prescription.sets) { "La seduta non contiene tutte le serie previste." }
        if (performance.any { it.painReported }) {
            return decision(ProgressionActionV2.STOP_AND_REVIEW, prescription.currentLoadKg, "Dolore segnalato: interrompere e rivalutare.")
        }
        if (performance.any { !it.techniqueValid }) {
            return decision(ProgressionActionV2.HOLD_LOAD, prescription.currentLoadKg, "Tecnica non valida: nessun aumento automatico.")
        }
        val averageRir = performance.mapNotNull { it.rir }.averageOrNull()
        if (averageRir == null || averageRir < prescription.targetRir) {
            return decision(ProgressionActionV2.HOLD_LOAD, prescription.currentLoadKg, "RIR insufficiente o non registrato: consolidare il carico.")
        }
        if (performance.any { it.repetitions < prescription.minReps }) {
            return decision(ProgressionActionV2.HOLD_LOAD, prescription.currentLoadKg, "Il minimo di ripetizioni non è stato raggiunto.")
        }
        if (performance.all { it.repetitions >= prescription.maxReps }) {
            val increment = increment(prescription)
            return decision(
                ProgressionActionV2.INCREASE_LOAD,
                roundToStep(prescription.currentLoadKg + increment, increment),
                "Tutte le serie hanno raggiunto il limite alto con RIR adeguato.",
                increment
            )
        }
        return decision(ProgressionActionV2.HOLD_LOAD, prescription.currentLoadKg, "Continuare la doppia progressione nel range previsto.")
    }

    private fun increment(prescription: ExercisePrescriptionV2): Double = when (prescription.category) {
        ExerciseCategoryV2.COMPOUND -> max(1.25, prescription.currentLoadKg * 0.025)
        ExerciseCategoryV2.ISOLATION -> max(0.5, prescription.currentLoadKg * 0.02)
        ExerciseCategoryV2.BODYWEIGHT -> 1.25
    }

    private fun roundToStep(value: Double, step: Double): Double = round(value / step) * step

    private fun decision(action: ProgressionActionV2, load: Double, reason: String, increment: Double = 0.0) =
        ProgressionDecisionV2(action, load, reason, increment)

    private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
