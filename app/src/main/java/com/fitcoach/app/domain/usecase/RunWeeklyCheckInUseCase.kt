package com.fitcoach.app.domain.usecase

import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.AdaptiveEngine
import com.fitcoach.app.domain.adaptive.ExerciseTrendClass
import com.fitcoach.app.domain.adaptive.LongitudinalCoachEngine
import com.fitcoach.app.domain.adaptive.LongitudinalContext
import com.fitcoach.app.domain.adaptive.NutritionWeek
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.model.CoachAuthorizationMode
import com.fitcoach.app.domain.model.CheckIn
import com.fitcoach.app.domain.model.CircumferenceRecord
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.ProgramChange
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.workout.FatigueManager
import java.time.LocalDate
import javax.inject.Inject

/**
 * Esegue il check-in settimanale: aggrega i dati reali (peso, allenamenti,
 * recupero), costruisce un [CheckIn] e fa girare il motore adattivo per
 * generare proposte di modifica motivate. Le proposte NON vengono applicate:
 * restano in stato PROPOSED in attesa di conferma.
 */
class RunWeeklyCheckInUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val diaryRepository: NutritionDiaryRepository,
    private val changeRepository: ChangeRepository,
    private val actionExecutor: CoachActionExecutor,
    private val preferencesRepository: PreferencesRepository
) {
    data class Outcome(
        val checkIn: CheckIn,
        val proposals: Int,
        val notes: List<String>
    )

    suspend operator fun invoke(): Outcome? {
        val profile = userRepository.get() ?: return null
        val weights = progressRepository.getWeights()
        val trend = if (weights.isEmpty()) null else WeightAnalyzer.trend(weights)

        val today = LocalDate.now()
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekStartEpochDay = weekStart.toEpochDay()

        val sessions = workoutRepository.getRecentSessions(limit = 90)
        val weekSessions = sessions.filter { it.dateEpochDay in weekStartEpochDay..(weekStartEpochDay + 6) }
        val completed = weekSessions.count { it.status == SessionStatus.COMPLETED }
        val planned = profile.trainingDaysPerWeek

        val recovery = FatigueManager.assessRecovery(sessions)
        val sleepAvg = weekSessions.mapNotNull { it.sleepQuality }.takeIf { it.isNotEmpty() }?.average()
        val painNotes = weekSessions.filter { it.painReported }
            .map { it.painNotes }
            .filter { it.isNotBlank() }
            .joinToString("; ")

        val nutritionPlan = nutritionRepository.getActivePlan()
        val nutritionWeeks = (0..5).map { offset ->
            val start = weekStartEpochDay - offset * 7L
            NutritionWeek(start, diaryRepository.weeklySummary(start))
        }
        val currentNutrition = nutritionWeeks.first().summary
        val avgConsumedCalories = currentNutrition.avgKcal.takeIf { currentNutrition.daysTracked > 0 }
        val consumedAdherence = currentNutrition.avgAdherencePercent.takeIf { currentNutrition.daysTracked > 0 }

        val checkIn = AdaptiveEngine.buildCheckIn(
            profile = profile,
            weekStartEpochDay = weekStartEpochDay,
            avgWeight = trend?.weeklyAverages?.firstOrNull()?.avgWeightKg,
            trend = trend?.trendKgPerWeek,
            avgCalories = avgConsumedCalories,
            adherence = consumedAdherence,
            sessionsCompleted = completed,
            sessionsPlanned = planned,
            performanceTrend = recovery.explanation,
            sleepAvg = sleepAvg,
            painNotes = painNotes
        )
        if (progressRepository.getCheckIns().none { it.weekStartEpochDay == weekStartEpochDay }) {
            progressRepository.addCheckIn(checkIn)
        }

        val workoutPlan = workoutRepository.getActivePlan()
        val performanceTrends = workoutRepository.getPerformanceByExercise().map { (name, values) ->
            LongitudinalCoachEngine.classifyExercise(
                name,
                values.sortedByDescending { it.dateEpochDay }.map { it.estimated1Rm ?: it.topWeightKg * (1 + it.topReps / 30.0) }
            )
        }
        val waistTrend = circumferenceTrend(progressRepository.getCircumferences(), "vita")
        evaluateAppliedChanges(
            changes = changeRepository.getAppliedPendingOutcome(),
            trend = trend?.trendKgPerWeek?.takeIf { trend.isReliable },
            target = profile.goal,
            regressions = performanceTrends.count { it.classification == ExerciseTrendClass.REGRESSION },
            hasRecentCheckIn = progressRepository.getCheckIns().any { it.createdAt >= (System.currentTimeMillis() - 14 * 86_400_000L) }
        )
        val decisions = if (nutritionPlan != null && workoutPlan != null) {
            LongitudinalCoachEngine.evaluate(
                LongitudinalContext(
                    profile = profile,
                    planId = nutritionPlan.id,
                    mesocycleId = workoutPlan.mesocycles.firstOrNull()?.id,
                    currentWeeklyKcal = nutritionPlan.weeklyCalories,
                    weightTrendKgPerWeek = trend?.trendKgPerWeek,
                    reliableWeightTrend = trend?.isReliable == true,
                    weightWeeks = trend?.weeksOfData ?: 0,
                    waistTrendCmPerWeek = waistTrend,
                    nutritionWeeks = nutritionWeeks,
                    performances = performanceTrends,
                    recoverySeverity = recovery.severity,
                    painSessions = sessions.take(7).count { it.painReported },
                    recentSessions = sessions
                )
            )
        } else emptyList()

        val notes = mutableListOf<String>()
        var proposals = 0
        val mode = preferencesRepository.getOrCreate().coachAuthorizationMode
        decisions.forEach { decision ->
            decision.action?.let { action ->
                val changeId = actionExecutor.registerProposal(action)
                proposals++
                val ordinary = action.action == CoachAction.ACTION_ADJUST_CALORIES
                if (mode == CoachAuthorizationMode.ADAPTIVE && ordinary) {
                    when (val result = actionExecutor.apply(changeId)) {
                        is com.fitcoach.app.data.repository.ApplyResult.Applied -> notes.add("Applicata automaticamente: ${result.message}")
                        is com.fitcoach.app.data.repository.ApplyResult.Failed -> notes.add(result.message)
                        else -> Unit
                    }
                } else {
                    notes.add("Proposta ${decision.classification}: richiede approvazione.")
                }
            } ?: notes.add("${decision.classification}: ${decision.reason}")
        }
        if (nutritionPlan == null) notes.add("Nessun piano nutrizionale attivo.")
        if (workoutPlan == null) notes.add("Nessun piano di allenamento attivo.")
        if (decisions.isEmpty()) notes.add("Dati longitudinali insufficienti o nessun intervento necessario.")

        return Outcome(checkIn, proposals, notes)
    }

    private suspend fun evaluateAppliedChanges(
        changes: List<ProgramChange>,
        trend: Double?,
        target: Goal,
        regressions: Int,
        hasRecentCheckIn: Boolean
    ) {
        if (!hasRecentCheckIn) return
        val cutoff = System.currentTimeMillis() - 7 * 86_400_000L
        val targetRate = when (target) {
            Goal.CUT -> -0.5
            Goal.RECOMP -> -0.25
            Goal.HYPERTROPHY -> 0.3
            Goal.MAINTENANCE -> 0.0
            Goal.STRENGTH -> 0.15
        }
        changes.filter { it.appliedAt != null && it.appliedAt < cutoff }.forEach { change ->
            val nutritionResult = when {
                trend == null -> "INSUFFICIENT_DATA"
                kotlin.math.abs(trend - targetRate) <= 0.2 -> "SUCCESS"
                else -> "PARTIAL"
            }
            val result = when {
                change.domain.name == "NUTRITION" -> nutritionResult
                regressions == 0 -> "SUCCESS"
                else -> "NEEDS_CORRECTION"
            }
            val followUp = when (result) {
                "SUCCESS" -> "Mantenere la strategia e continuare la raccolta dati."
                "INSUFFICIENT_DATA" -> "Attendere un altro check-in con dati affidabili."
                else -> "Rivalutare la strategia nel prossimo check-in; non ripetere automaticamente la stessa modifica."
            }
            changeRepository.recordOutcome(
                id = change.id,
                status = result,
                actualOutcome = "Risultato osservato nel check-in longitudinale: $result.",
                followUpAction = followUp
            )
        }
    }

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
