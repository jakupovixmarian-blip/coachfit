package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.WeeklyNutritionSummary

/**
 * Misura la tendenza di un esercizio su piu sessioni: confronta la media delle
 * tre prestazioni piu recenti con le tre precedenti (1RM stimato o carico per
 * ripetizione). E solo una MISURA, non una decisione: la classificazione
 * (progressione/stagnazione/regressione) e usata come dato di supporto e
 * interpretata dal Coach AI centrale.
 */
object ExerciseTrendCalculator {
    const val MIN_SAMPLES = 6

    fun classify(name: String, valuesNewestFirst: List<Double>): ExerciseTrend {
        if (valuesNewestFirst.size < MIN_SAMPLES) {
            return ExerciseTrend(name, ExerciseTrendClass.INSUFFICIENT_DATA, 0.0, valuesNewestFirst.size)
        }
        val recent = valuesNewestFirst.take(3).average()
        val previous = valuesNewestFirst.drop(3).take(3).average()
        if (previous <= 0.0) {
            return ExerciseTrend(name, ExerciseTrendClass.INSUFFICIENT_DATA, 0.0, valuesNewestFirst.size)
        }
        val change = (recent - previous) / previous * 100.0
        val classification = when {
            change >= 5.0 -> ExerciseTrendClass.PROGRESSION
            change <= -5.0 -> ExerciseTrendClass.REGRESSION
            else -> ExerciseTrendClass.STAGNATION
        }
        return ExerciseTrend(name, classification, change, valuesNewestFirst.size)
    }
}

/** Tendenza misurata di un esercizio (nessuna decisione). */
data class ExerciseTrend(
    val exerciseName: String,
    val classification: ExerciseTrendClass,
    val changePercent: Double,
    val samples: Int
)

enum class ExerciseTrendClass { PROGRESSION, STAGNATION, REGRESSION, INSUFFICIENT_DATA }

/** Una settimana nutrizionale con il suo riepilogo reale (nessun dato inventato). */
data class NutritionWeek(
    val weekStartEpochDay: Long,
    val summary: WeeklyNutritionSummary
)
