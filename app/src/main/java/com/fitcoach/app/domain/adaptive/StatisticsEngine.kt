package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WorkoutSession
import java.time.LocalDate

/** Intervallo di analisi dello storico: settimana, mese, trimestre. */
enum class StatRange(val label: String, val days: Int) {
    WEEK("Settimana", 7),
    MONTH("Mese", 30),
    QUARTER("Trimestre", 90)
}

/** Punto di una serie giornaliera (calorie o volume) con etichetta leggibile. */
data class DailyStatPoint(
    val epochDay: Long,
    val label: String,
    val value: Double
)

/** Migliore prestazione del periodo su un esercizio (nessun valore stimato inventato). */
data class BestLift(
    val exerciseName: String,
    val topWeightKg: Double,
    val reps: Int,
    val estimated1Rm: Double?
)

/**
 * Statistiche di un intervallo, calcolate solo da dati REALI registrati
 * dall'utente. I campi sono nulli quando non ci sono misurazioni sufficienti:
 * l'assenza di dati non viene mai mascherata con zeri o stime.
 */
data class RangeStatistics(
    val range: StatRange,
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val weightSamples: Int,
    val avgWeightKg: Double?,
    val weightDeltaKg: Double?,
    val daysTracked: Int,
    val avgKcal: Double?,
    val avgProtein: Double?,
    val avgCarbs: Double?,
    val avgFat: Double?,
    val avgAdherencePercent: Double?,
    val sessionsCompleted: Int,
    val totalVolumeKg: Double,
    val avgVolumePerSessionKg: Double?,
    val bestLift: BestLift?,
    val caloriesSeries: List<DailyStatPoint>,
    val volumeSeries: List<DailyStatPoint>,
    val weightSeries: List<DailyStatPoint>
)

/**
 * Motore deterministico delle statistiche. Aggrega pesate, diario reale e
 * sedute completate su un intervallo. Non prende decisioni: misura e basta,
 * lasciando l'interpretazione al Coach.
 */
object StatisticsEngine {

    fun compute(
        range: StatRange,
        todayEpochDay: Long,
        weights: List<WeightRecord>,
        diary: List<DiaryEntry>,
        sessions: List<WorkoutSession>,
        performances: List<ExercisePerformance>,
        targetKcalFor: (Long) -> Double
    ): RangeStatistics {
        val from = todayEpochDay - (range.days - 1)
        val inRange = { day: Long -> day in from..todayEpochDay }

        val rangeWeights = weights.filter { inRange(it.dateEpochDay) }.sortedBy { it.dateEpochDay }
        val rangeDiary = diary.filter { inRange(it.dateEpochDay) && it.active }
        val rangeSessions = sessions.filter {
            inRange(it.dateEpochDay) && it.status == com.fitcoach.app.domain.model.SessionStatus.COMPLETED
        }
        val rangePerformances = performances.filter { inRange(it.dateEpochDay) }

        val byDay = rangeDiary.groupBy { it.dateEpochDay }
        val trackedDays = byDay.keys.sorted()

        val avgKcal = trackedDays.map { byDay.getValue(it).sumOf { e -> e.kcal } }.averageOrNull()
        val avgProtein = trackedDays.map { byDay.getValue(it).sumOf { e -> e.protein } }.averageOrNull()
        val avgCarbs = trackedDays.map { byDay.getValue(it).sumOf { e -> e.carbs } }.averageOrNull()
        val avgFat = trackedDays.map { byDay.getValue(it).sumOf { e -> e.fat } }.averageOrNull()

        val adherence = trackedDays.mapNotNull { day ->
            val target = targetKcalFor(day)
            if (target <= 0.0) null
            else (byDay.getValue(day).sumOf { e -> e.kcal } / target * 100.0).coerceAtMost(150.0)
        }

        val bestLift = rangePerformances.maxByOrNull { it.estimated1Rm ?: it.topWeightKg }?.let {
            BestLift(it.exerciseName, it.topWeightKg, it.topReps, it.estimated1Rm)
        }

        val weightDelta = if (rangeWeights.size >= 2) {
            rangeWeights.last().weightKg - rangeWeights.first().weightKg
        } else null

        return RangeStatistics(
            range = range,
            fromEpochDay = from,
            toEpochDay = todayEpochDay,
            weightSamples = rangeWeights.size,
            avgWeightKg = rangeWeights.map { it.weightKg }.averageOrNull(),
            weightDeltaKg = weightDelta,
            daysTracked = trackedDays.size,
            avgKcal = avgKcal,
            avgProtein = avgProtein,
            avgCarbs = avgCarbs,
            avgFat = avgFat,
            avgAdherencePercent = adherence.averageOrNull(),
            sessionsCompleted = rangeSessions.size,
            totalVolumeKg = rangeSessions.sumOf { it.totalVolumeKg },
            avgVolumePerSessionKg = rangeSessions.map { it.totalVolumeKg }.averageOrNull(),
            bestLift = bestLift,
            caloriesSeries = trackedDays.map { day ->
                DailyStatPoint(day, dayLabel(day), byDay.getValue(day).sumOf { e -> e.kcal })
            },
            volumeSeries = rangeSessions.sortedBy { it.dateEpochDay }.map { session ->
                DailyStatPoint(session.dateEpochDay, dayLabel(session.dateEpochDay), session.totalVolumeKg)
            },
            weightSeries = rangeWeights.map { record ->
                DailyStatPoint(record.dateEpochDay, dayLabel(record.dateEpochDay), record.weightKg)
            }
        )
    }

    private fun dayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return "%02d/%02d".format(date.dayOfMonth, date.monthValue)
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
