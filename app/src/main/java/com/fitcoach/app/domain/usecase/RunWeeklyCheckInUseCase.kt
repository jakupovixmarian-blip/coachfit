package com.fitcoach.app.domain.usecase

import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.AdaptiveEngine
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import com.fitcoach.app.domain.model.CheckIn
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
    private val changeRepository: ChangeRepository
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

        val sessions = workoutRepository.getRecentSessions(limit = 30)
        val weekSessions = sessions.filter { it.dateEpochDay >= weekStartEpochDay }
        val completed = weekSessions.count { it.status == SessionStatus.COMPLETED }
        val planned = profile.trainingDaysPerWeek
        val adherence = if (planned > 0) (completed.toDouble() / planned * 100).coerceIn(0.0, 100.0) else null

        val recovery = FatigueManager.assessRecovery(sessions)
        val sleepAvg = sessions.mapNotNull { it.sleepQuality }.takeIf { it.isNotEmpty() }?.average()
        val painNotes = sessions.filter { it.painReported }
            .flatMap { listOf(it.painNotes) }
            .filter { it.isNotBlank() }
            .joinToString("; ")

        val nutritionPlan = nutritionRepository.getActivePlan()
        val diarySummary = diaryRepository.weeklySummary(weekStartEpochDay)
        val avgConsumedCalories = diarySummary.avgKcal.takeIf { diarySummary.daysTracked > 0 }
        val consumedAdherence = diarySummary.avgAdherencePercent.takeIf { diarySummary.daysTracked > 0 }

        val checkIn = AdaptiveEngine.buildCheckIn(
            profile = profile,
            weekStartEpochDay = weekStartEpochDay,
            avgWeight = trend?.weeklyAverages?.lastOrNull()?.avgWeightKg,
            trend = trend?.trendKgPerWeek,
            avgCalories = avgConsumedCalories,
            adherence = consumedAdherence,
            sessionsCompleted = completed,
            sessionsPlanned = planned,
            performanceTrend = recovery.explanation,
            sleepAvg = sleepAvg,
            painNotes = painNotes
        )
        progressRepository.addCheckIn(checkIn)

        val notes = mutableListOf<String>()
        var proposals = 0

        // 1) Valutazione nutrizionale (solo con dati sufficienti e reali).
        if (nutritionPlan != null) {
            val weekly = nutritionPlan.weeklyCalories
            val nutritionChanges = AdaptiveEngine.evaluateNutrition(
                AdaptiveEngine.NutritionContext(
                    profile = profile,
                    currentWeeklyKcal = weekly,
                    currentDailyKcal = weekly / 7.0,
                    weightTrendKgPerWeek = trend?.trendKgPerWeek?.takeIf { trend.isReliable },
                    weeksOfData = trend?.weeksOfData ?: 0,
                    avgAdherencePercent = consumedAdherence,
                    avgCaloriesConsumed = avgConsumedCalories
                )
            )
            nutritionChanges.forEach { changeRepository.add(it) }
            proposals += nutritionChanges.size
            if (nutritionChanges.isEmpty()) notes += "Nutrizione: nessuna modifica necessaria questa settimana."
        } else {
            notes += "Nessun piano nutrizionale attivo: genera un piano per ricevere valutazioni."
        }

        // 2) Valutazione allenamento (recupero, dolore, fine mesociclo).
        val mesocycleWeeks = workoutRepository.completedSessionCount() / profile.trainingDaysPerWeek.coerceAtLeast(1)
        val painSessions = sessions.take(7).count { it.painReported }
        val trainingChanges = AdaptiveEngine.evaluateTraining(
            profile = profile,
            recoverySeverity = recovery.severity,
            painSessions = painSessions,
            weeksInMesocycle = mesocycleWeeks
        )
        trainingChanges.forEach { changeRepository.add(it) }
        proposals += trainingChanges.size
        if (trainingChanges.isEmpty()) notes += "Allenamento: nessun intervento necessario (${recovery.explanation})"

        // 3) Analisi dell'andamento REALE delle prestazioni per esercizio.
        val trendChanges = AdaptiveEngine.evaluateExerciseTrends(
            workoutRepository.getPerformanceByExercise()
        )
        trendChanges.forEach { changeRepository.add(it) }
        proposals += trendChanges.size

        return Outcome(checkIn, proposals, notes)
    }
}
