package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.WorkoutSet
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Motore di Progressive Overload (Double Progression).
 *
 * Regola:
 *  1. Si mantiene il carico finche l'utente non raggiunge il limite SUPERIORE
 *     del range di ripetizioni su TUTTE le serie allenanti.
 *  2. Raggiunto il top del range, si aumenta il carico e si torna al limite inferiore.
 *  3. L'incremento dipende dal tipo di esercizio e dal carico attuale.
 *  4. NON si aumenta se: RIR insufficiente (< 1), tecnica scorretta segnalata,
 *     dolore riportato, o performance in calo.
 */
object ProgressiveOverloadEngine {

    /** Incremento in kg per esercizi multiarticolari (bilanciere). */
    const val COMPOUND_INCREASE_KG = 2.5

    /** Incremento per esercizi di isolamento. */
    const val ISOLATION_INCREASE_KG = 1.0

    /** Soglia di RIR minima per permettere l'aumento. */
    const val MIN_RIR_FOR_INCREASE = 1

    data class ProgressionDecision(
        val recommendedWeightKg: Double,
        val recommendedReps: Int,
        val increaseApplied: Boolean,
        val increaseKg: Double,
        val reason: String,
        val holdBecauseOfPainOrTechnique: Boolean = false
    )

    /**
     * Calcola la progressione per un esercizio.
     *
     * @param lastWeightKg carico usato nella seduta precedente
     * @param lastSets serie allenanti completate con relative reps e RIR
     * @param minReps limite inferiore del range
     * @param maxReps limite superiore del range
     * @param category categoria dell'esercizio
     * @param painReported dolore segnalato
     * @param poorTechnique tecnica scorretta segnalata
     */
    fun decide(
        lastWeightKg: Double,
        lastSets: List<WorkoutSet>,
        minReps: Int,
        maxReps: Int,
        category: ExerciseCategory,
        painReported: Boolean = false,
        poorTechnique: Boolean = false
    ): ProgressionDecision {
        val working = lastSets.filter { it.completed && !it.isWarmup }
        if (working.isEmpty()) {
            return ProgressionDecision(
                recommendedWeightKg = lastWeightKg,
                recommendedReps = minReps,
                increaseApplied = false,
                increaseKg = 0.0,
                reason = "Nessuna serie registrata: mantieni il carico e registra le serie."
            )
        }

        if (painReported) {
            return ProgressionDecision(
                recommendedWeightKg = lastWeightKg,
                recommendedReps = minReps,
                increaseApplied = false,
                increaseKg = 0.0,
                reason = "Dolore segnalato: carico NON aumentato. Valuta la sostituzione dell'esercizio " +
                    "o consulta un professionista se il dolore persiste.",
                holdBecauseOfPainOrTechnique = true
            )
        }
        if (poorTechnique) {
            return ProgressionDecision(
                recommendedWeightKg = lastWeightKg,
                recommendedReps = minReps,
                increaseApplied = false,
                increaseKg = 0.0,
                reason = "Tecnica scorretta segnalata: carico NON aumentato finche la tecnica non e solida.",
                holdBecauseOfPainOrTechnique = true
            )
        }

        // Tutte le serie devono raggiungere almeno il top del range con RIR sufficiente.
        val allAtTop = working.all { it.actualReps >= maxReps }
        val minRir = working.mapNotNull { it.rir }.minOrNull()
        val rirOk = minRir == null || minRir >= MIN_RIR_FOR_INCREASE

        if (allAtTop && rirOk) {
            val increase = increaseFor(category, lastWeightKg)
            return ProgressionDecision(
                recommendedWeightKg = lastWeightKg + increase,
                recommendedReps = minReps,
                increaseApplied = true,
                increaseKg = increase,
                reason = "Hai raggiunto $maxReps ripetizioni su tutte le serie con RIR adeguato: " +
                    "aumenta di ${formatKg(increase)} kg e riparti da $minReps ripetizioni."
            )
        }

        if (allAtTop && !rirOk) {
            return ProgressionDecision(
                recommendedWeightKg = lastWeightKg,
                recommendedReps = maxReps,
                increaseApplied = false,
                increaseKg = 0.0,
                reason = "Hai raggiunto il top del range ma con RIR troppo basso (< $MIN_RIR_FOR_INCREASE): " +
                    "consolida la qualita prima di aumentare il carico."
            )
        }

        // Doppia progressione: aggiungi una ripetizione fino al top del range.
        val bestReps = working.maxOf { it.actualReps }
        val nextReps = (bestReps + 1).coerceAtMost(maxReps)
        return ProgressionDecision(
            recommendedWeightKg = lastWeightKg,
            recommendedReps = nextReps,
            increaseApplied = false,
            increaseKg = 0.0,
            reason = "Mantieni ${formatKg(lastWeightKg)} kg e prova a fare $nextReps ripetizioni " +
                "su tutte le serie (double progression)."
        )
    }

    /**
     * Incremento proporzionale: per carichi alti l'incremento base cresce
     * leggermente, ma resta prudente.
     */
    fun increaseFor(category: ExerciseCategory, currentWeightKg: Double): Double {
        val base = when (category) {
            ExerciseCategory.COMPOUND -> COMPOUND_INCREASE_KG
            ExerciseCategory.ISOLATION -> ISOLATION_INCREASE_KG
            ExerciseCategory.BODYWEIGHT -> 0.0
        }
        // Per carichi > 100 kg, aggiungi un ulteriore 2.5% arrotondato a 0.5 kg.
        val extra = if (currentWeightKg > 100) {
            ((currentWeightKg * 0.025) / 0.5).roundToInt() * 0.5
        } else 0.0
        return base + extra
    }

    /** Stima 1RM con formula di Epley. */
    fun estimated1Rm(weightKg: Double, reps: Int): Double {
        if (reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1 + reps / 30.0)
    }

    private fun formatKg(kg: Double): String =
        if (abs(kg - kg.roundToInt()) < 0.05) kg.roundToInt().toString()
        else "%.1f".format(kg)
}
