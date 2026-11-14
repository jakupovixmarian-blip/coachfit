package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.WeeklyWeightStats
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WeightTrend
import kotlin.math.abs

/**
 * Analisi del peso corporeo.
 *
 * Regola chiave: NON si modificano i piani sulla base delle oscillazioni
 * giornaliere. Si usano le MEDIE SETTIMANALI e la loro tendenza.
 */
object WeightAnalyzer {

    private const val EPOCH_DAYS_PER_WEEK = 7L

    /** Raggruppa le pesate in medie settimanali (settimana ISO-like da epochDay/7). */
    fun weeklyAverages(records: List<WeightRecord>, weeks: Int = 8): List<WeeklyWeightStats> {
        if (records.isEmpty()) return emptyList()
        val grouped = records.groupBy { it.dateEpochDay / EPOCH_DAYS_PER_WEEK }
        return grouped.entries
            .sortedByDescending { it.key }
            .take(weeks)
            .map { (weekIndex, list) ->
                val weights = list.map { it.weightKg }
                WeeklyWeightStats(
                    weekStartEpochDay = weekIndex * EPOCH_DAYS_PER_WEEK,
                    avgWeightKg = weights.average(),
                    samples = weights.size,
                    minWeightKg = weights.minOrNull(),
                    maxWeightKg = weights.maxOrNull()
                )
            }
    }

    /**
     * Tendenza lineare del peso (kg/settimana) calcolata sulle medie settimanali.
     * Richiede almeno 2 settimane con dati; e "affidabile" con >= 3 settimane.
     */
    fun trend(records: List<WeightRecord>, weeks: Int = 6): WeightTrend {
        val weekly = weeklyAverages(records, weeks)
            .filter { it.avgWeightKg != null }
            .sortedBy { it.weekStartEpochDay }

        if (weekly.size < 2) {
            return WeightTrend(weekly, null, weekly.size, false)
        }

        // Regressione lineare sui punti (settimana, media).
        val n = weekly.size
        val xs = weekly.mapIndexed { i, _ -> i.toDouble() }
        val ys = weekly.map { it.avgWeightKg!! }
        val meanX = xs.average()
        val meanY = ys.average()
        val num = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) }
        val den = xs.indices.sumOf { (xs[it] - meanX) * (xs[it] - meanX) }
        val slope = if (den == 0.0) 0.0 else num / den

        return WeightTrend(
            weeklyAverages = weekly.sortedByDescending { it.weekStartEpochDay },
            trendKgPerWeek = slope,
            weeksOfData = weekly.size,
            isReliable = weekly.size >= 3
        )
    }

    /** Media delle ultime N settimane (per confronto con l'obiettivo). */
    fun recentAverage(records: List<WeightRecord>, weeks: Int = 2): Double? {
        val stats = weeklyAverages(records, weeks)
        val valid = stats.mapNotNull { it.avgWeightKg }
        return if (valid.isEmpty()) null else valid.average()
    }

    /**
     * Interpreta la tendenza rispetto all'obiettivo.
     * Ritorna (categoria, spiegazione).
     */
    fun interpret(trend: WeightTrend, targetKgPerWeek: Double): TrendInterpretation {
        val slope = trend.trendKgPerWeek
        if (slope == null || !trend.isReliable) {
            return TrendInterpretation(
                TrendCategory.INSUFFICIENT_DATA,
                "Servono almeno 3 settimane di pesate per una valutazione affidabile."
            )
        }
        val diff = slope - targetKgPerWeek
        return when {
            abs(slope) < 0.1 && targetKgPerWeek < -0.2 -> TrendInterpretation(
                TrendCategory.STALLED,
                "Il peso e sostanzialmente stabile (${fmt(slope)} kg/sett) ma l'obiettivo richiede una " +
                    "riduzione. Il piano puo essere rivisto."
            )
            abs(slope) < 0.1 && targetKgPerWeek > 0.2 -> TrendInterpretation(
                TrendCategory.STALLED,
                "Il peso e stabile (${fmt(slope)} kg/sett) mentre l'obiettivo e la crescita: valuta un aumento calorico."
            )
            diff < -0.15 -> TrendInterpretation(
                TrendCategory.TOO_FAST,
                "Il peso varia piu rapidamente del previsto (${fmt(slope)} vs ${fmt(targetKgPerWeek)} kg/sett). " +
                    "Possibile perdita di massa magra: rallenta il deficit."
            )
            diff > 0.15 -> TrendInterpretation(
                TrendCategory.TOO_SLOW,
                "La variazione e piu lenta del previsto (${fmt(slope)} vs ${fmt(targetKgPerWeek)} kg/sett). " +
                    "Possibile deficit insufficiente."
            )
            else -> TrendInterpretation(
                TrendCategory.ON_TRACK,
                "L'andamento (${fmt(slope)} kg/sett) e in linea con l'obiettivo."
            )
        }
    }

    enum class TrendCategory { INSUFFICIENT_DATA, ON_TRACK, STALLED, TOO_FAST, TOO_SLOW }

    data class TrendInterpretation(val category: TrendCategory, val explanation: String)

    private fun fmt(v: Double): String = "%+.2f".format(v)
}
