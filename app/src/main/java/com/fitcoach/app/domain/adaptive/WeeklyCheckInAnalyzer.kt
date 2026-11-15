package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.CircumferenceRecord
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WorkoutSession
import com.fitcoach.app.domain.workout.FatigueManager

/**
 * Aggrega i dati REALI degli ultimi 7 giorni e li confronta con le settimane
 * precedenti per il check-in settimanale.
 *
 * Questo componente e PURO e DETERMINISTICO: calcola i numeri e produce delle
 * CLASSIFICAZIONI DI SUPPORTO (advisory). NON decide e non applica nulla: le
 * decisioni restano all'AI centrale (CoachOrchestrator), che interpreta il report
 * e propone le modifiche con gli strumenti "propose_*". Se un dato manca, il campo
 * resta null e viene dichiarato "non disponibile": nessun valore inventato.
 */
object WeeklyCheckInAnalyzer {

    data class Input(
        val profile: UserProfile,
        val weekStartEpochDay: Long,
        val weights: List<WeightRecord> = emptyList(),
        val circumferences: List<CircumferenceRecord> = emptyList(),
        val nutritionWeeks: List<NutritionWeek> = emptyList(),
        val recentSessions: List<WorkoutSession> = emptyList(),
        val performancesByExercise: Map<String, List<ExercisePerformance>> = emptyMap(),
        val activeNutritionPlanId: Long? = null,
        val activeMesocycleId: Long? = null,
        val currentWeeklyKcal: Double = 0.0,
        val weightWeeks: Int = 6
    )

    data class Report(
        val weekStartEpochDay: Long,
        // Peso e composizione
        val weeksOfWeightData: Int,
        val weightReliable: Boolean,
        val weightTrendKgPerWeek: Double?,
        val avgWeightKg: Double?,
        val bodyFatPercent: Double?,
        val waistTrendCmPerWeek: Double?,
        // Nutrizione
        val nutritionWeeksTracked: Int,
        val adherencePercent: Double?,
        val avgKcalConsumed: Double?,
        val avgPlannedKcal: Double?,
        val avgProtein: Double?,
        val avgCarbs: Double?,
        val avgFat: Double?,
        // Allenamento e recupero
        val sessionsPlanned: Int,
        val sessionsCompleted: Int,
        val recoverySeverity: Int,
        val recoveryExplanation: String,
        val sleepAvg: Double?,
        val painSessions: Int,
        val painNotes: String,
        val performanceTrends: List<ExerciseTrend>,
        // Valutazione di supporto
        val dataSufficient: Boolean,
        val advisories: List<AdaptiveDecision>
    ) {
        val progressionCount: Int get() = performanceTrends.count { it.classification == ExerciseTrendClass.PROGRESSION }
        val stagnationCount: Int get() = performanceTrends.count { it.classification == ExerciseTrendClass.STAGNATION }
        val regressionCount: Int get() = performanceTrends.count { it.classification == ExerciseTrendClass.REGRESSION }

        /** Riepilogo testuale deterministico (nessuna decisione, solo i fatti osservati). */
        fun summary(): String = buildString {
            append("Peso medio: ${avgWeightKg?.let { "%.1f".format(it) } ?: "n/d"} kg. ")
            append("Trend: ${weightTrendKgPerWeek?.let { "%+.2f".format(it) } ?: "n/d"} kg/sett")
            append(if (weightReliable) "" else " (dati insufficienti)")
            append(". ")
            append("Aderenza: ${adherencePercent?.let { "%.0f".format(it) } ?: "n/d"}%. ")
            append("Allenamenti: $sessionsCompleted/$sessionsPlanned. ")
            append("Prestazioni: $progressionCount in progressione, $stagnationCount stabili, $regressionCount in calo.")
        }

        /** Riepilogo compatto del trend di performance per il contesto AI. */
        fun performanceSummary(): String =
            "Prestazioni: $progressionCount in progressione, $stagnationCount in stazionamento, $regressionCount in calo."
    }

    fun analyze(input: Input): Report {
        val profile = input.profile
        val trend = if (input.weights.isEmpty()) null else WeightAnalyzer.trend(input.weights, weeks = input.weightWeeks)

        val weekStart = input.weekStartEpochDay
        val weekSessions = input.recentSessions.filter { it.dateEpochDay in weekStart..(weekStart + 6) }
        val completed = weekSessions.count { it.status == SessionStatus.COMPLETED }

        val recovery = FatigueManager.assessRecovery(input.recentSessions)
        val sleepAvg = weekSessions.mapNotNull { it.sleepQuality }.takeIf { it.isNotEmpty() }?.average()
        val painNotes = weekSessions.filter { it.painReported }
            .map { it.painNotes }
            .filter { it.isNotBlank() }
            .joinToString("; ")

        val nutritionWithData = input.nutritionWeeks
            .filter { it.summary.daysTracked > 0 }
            .sortedByDescending { it.weekStartEpochDay }
        val current = input.nutritionWeeks.firstOrNull()?.summary
        val adherence = current?.avgAdherencePercent?.takeIf { (current.daysTracked) > 0 }
        val avgKcal = current?.avgKcal?.takeIf { (current.daysTracked) > 0 }

        val performanceTrends = input.performancesByExercise.map { (name, values) ->
            LongitudinalCoachEngine.classifyExercise(
                name,
                values.sortedByDescending { it.dateEpochDay }
                    .map { it.estimated1Rm ?: it.topWeightKg * (1 + it.topReps / 30.0) }
            )
        }.sortedBy { it.exerciseName }

        val waistTrend = circumferenceTrend(input.circumferences, "vita")
        val latestBodyFat = input.weights.sortedByDescending { it.dateEpochDay }
            .firstNotNullOfOrNull { it.bodyFatPercent } ?: profile.bodyFatPercent

        val advisories = LongitudinalCoachEngine.evaluate(
            LongitudinalContext(
                profile = profile,
                planId = input.activeNutritionPlanId ?: 0L,
                mesocycleId = input.activeMesocycleId,
                currentWeeklyKcal = input.currentWeeklyKcal,
                weightTrendKgPerWeek = trend?.trendKgPerWeek,
                reliableWeightTrend = trend?.isReliable == true,
                weightWeeks = trend?.weeksOfData ?: 0,
                waistTrendCmPerWeek = waistTrend,
                nutritionWeeks = input.nutritionWeeks,
                performances = performanceTrends,
                recoverySeverity = recovery.severity,
                painSessions = input.recentSessions.take(7).count { it.painReported },
                recentSessions = input.recentSessions
            )
        )

        val dataSufficient = nutritionWithData.size >= LongitudinalCoachEngine.MIN_NUTRITION_WEEKS &&
            trend?.isReliable == true && (trend?.weeksOfData ?: 0) >= LongitudinalCoachEngine.MIN_WEIGHT_WEEKS

        return Report(
            weekStartEpochDay = weekStart,
            weeksOfWeightData = trend?.weeksOfData ?: 0,
            weightReliable = trend?.isReliable == true,
            weightTrendKgPerWeek = trend?.trendKgPerWeek,
            avgWeightKg = trend?.weeklyAverages?.firstOrNull()?.avgWeightKg,
            bodyFatPercent = latestBodyFat,
            waistTrendCmPerWeek = waistTrend,
            nutritionWeeksTracked = nutritionWithData.size,
            adherencePercent = adherence,
            avgKcalConsumed = avgKcal,
            avgPlannedKcal = current?.avgPlannedKcal?.takeIf { current.daysTracked > 0 },
            avgProtein = current?.avgProtein?.takeIf { current.daysTracked > 0 },
            avgCarbs = current?.avgCarbs?.takeIf { current.daysTracked > 0 },
            avgFat = current?.avgFat?.takeIf { current.daysTracked > 0 },
            sessionsPlanned = profile.trainingDaysPerWeek,
            sessionsCompleted = completed,
            recoverySeverity = recovery.severity,
            recoveryExplanation = recovery.explanation,
            sleepAvg = sleepAvg,
            painSessions = input.recentSessions.take(7).count { it.painReported },
            painNotes = painNotes,
            performanceTrends = performanceTrends,
            dataSufficient = dataSufficient,
            advisories = advisories
        )
    }

    /** Tendenza settimanale (cm/settimana) di una circonferenza; null se dati insufficienti. */
    private fun circumferenceTrend(records: List<CircumferenceRecord>, site: String): Double? {
        val weekly = records.filter { it.site.equals(site, ignoreCase = true) }
            .groupBy { it.dateEpochDay / 7L }
            .toList()
            .sortedBy { it.first }
            .takeLast(6)
            .map { (_, values) -> values.map { it.valueCm }.average() }
        if (weekly.size < 3) return null
        val meanX = (weekly.indices).average()
        val meanY = weekly.average()
        val denominator = weekly.indices.sumOf { (it - meanX) * (it - meanX) }
        if (denominator == 0.0) return null
        return weekly.indices.sumOf { (it - meanX) * (weekly[it] - meanY) } / denominator
    }
}
