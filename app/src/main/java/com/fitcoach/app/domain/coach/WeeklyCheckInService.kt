package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.NutritionWeek
import com.fitcoach.app.domain.adaptive.WeeklyCheckInAnalyzer
import com.fitcoach.app.domain.model.CheckIn
import com.fitcoach.app.domain.model.ChatMessage
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.model.ProgramChange
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conduce il CHECK-IN SETTIMANALE attraverso l'unica autorita decisionale,
 * [CoachOrchestrator].
 *
 * Il servizio:
 *  1. aggrega i dati REALI con [WeeklyCheckInAnalyzer] (nessun dato inventato);
 *  2. salva il [CheckIn] (fotografia immutabile della settimana);
 *  3. apre un turno agentico sul [CoachOrchestrator] con il prompt di check-in e il
 *     contesto reale; l'AI interpreta i dati e, se serve, registra PROPOSTE con gli
 *     strumenti "propose_*" (che NON applicano nulla);
 *  4. ritorna la valutazione testuale e le proposte in attesa di conferma.
 *
 * L'AI non scrive mai sul DB: le proposte vengono applicate solo dopo la conferma
 * dell'utente tramite [com.fitcoach.app.data.repository.CoachActionExecutor].
 */
@Singleton
class WeeklyCheckInService @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository,
    private val nutritionRepository: NutritionRepository,
    private val diaryRepository: NutritionDiaryRepository,
    private val workoutRepository: WorkoutRepository,
    private val chatRepository: ChatRepository,
    private val changeRepository: ChangeRepository,
    private val aiConfigResolver: AiConfigResolver,
    private val orchestrator: CoachOrchestrator
) {
    sealed interface Outcome {
        data class Success(
            val checkIn: CheckIn,
            val report: WeeklyCheckInAnalyzer.Report,
            val evaluation: String,
            val proposals: List<ProgramChange>,
            val providerError: String? = null
        ) : Outcome

        /** Nessun profilo o dati essenziali mancanti: niente da analizzare. */
        data class MissingData(val message: String) : Outcome
    }

    suspend operator fun invoke(nutritionWeeks: Int = 6): Outcome {
        val profile = userRepository.get()
            ?: return Outcome.MissingData("Completa prima il profilo per eseguire il check-in.")

        val weekStart = NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay())
        val weeks = (0 until nutritionWeeks.coerceIn(1, 12)).map { offset ->
            val start = weekStart - offset * 7L
            NutritionWeek(start, diaryRepository.weeklySummary(start))
        }
        val nutritionPlan = nutritionRepository.getActivePlan()
        val workoutPlan = workoutRepository.getActivePlan()

        val report = WeeklyCheckInAnalyzer.analyze(
            WeeklyCheckInAnalyzer.Input(
                profile = profile,
                weekStartEpochDay = weekStart,
                weights = progressRepository.getWeights(),
                circumferences = progressRepository.getCircumferences(),
                nutritionWeeks = weeks,
                recentSessions = workoutRepository.getRecentSessions(limit = 90),
                performancesByExercise = workoutRepository.getPerformanceByExercise(),
                activeNutritionPlanId = nutritionPlan?.id,
                activeMesocycleId = workoutPlan?.mesocycles?.firstOrNull()?.id,
                currentWeeklyKcal = nutritionPlan?.weeklyCalories ?: 0.0
            )
        )

        val checkIn = report.toCheckIn(profile.id)
        if (progressRepository.getCheckIns().none { it.weekStartEpochDay == weekStart }) {
            progressRepository.addCheckIn(checkIn)
        }

        val config = aiConfigResolver.resolveConfigured()
            ?: return Outcome.Success(
                checkIn = checkIn,
                report = report,
                evaluation = "",
                proposals = emptyList(),
                providerError = "Configura un provider AI in Impostazioni per la valutazione del coach. " +
                    "I dati del check-in sono stati comunque salvati."
            )

        val contextPrompt = CoachPromptBuilder.buildCheckInContext(report)
        val turn = orchestrator.run(
            config = config,
            systemPrompt = CoachPromptBuilder.weeklyCheckInSystemPrompt,
            contextPrompt = contextPrompt,
            history = emptyList(),
            userMessage = "Esegui il check-in settimanale: analizza i miei dati reali e proponi " +
                "eventuali modifiche motivate. Non inventare dati mancanti.",
            maxIterations = CHECKIN_MAX_ITERATIONS
        )

        val proposals = turn.pendingChangeIds
            .mapNotNull { changeRepository.getById(it) }
            .filter { it.id != 0L }

        // Traccia la conversazione del check-in, cosi l'utente la ritrova nella chat.
        chatRepository.add(
            ChatMessage(
                role = ChatRole.USER,
                content = "Check-in settimanale del ${checkIn.weekStartEpochDay}."
            )
        )
        if (turn.text.isNotBlank()) {
            chatRepository.add(
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = turn.text
                )
            )
        }

        return Outcome.Success(
            checkIn = checkIn,
            report = report,
            evaluation = turn.text,
            proposals = proposals,
            providerError = turn.providerError
        )
    }

    private fun WeeklyCheckInAnalyzer.Report.toCheckIn(profileId: Long): CheckIn = CheckIn(
        profileId = profileId,
        weekStartEpochDay = weekStartEpochDay,
        avgWeightKg = avgWeightKg,
        weightTrendKgPerWeek = weightTrendKgPerWeek,
        avgCalories = avgKcalConsumed,
        dietAdherencePercent = adherencePercent,
        trainingSessionsCompleted = sessionsCompleted,
        trainingSessionsPlanned = sessionsPlanned,
        performanceTrend = performanceSummary(),
        sleepQualityAvg = sleepAvg,
        painNotes = painNotes,
        summary = summary()
    )

    private companion object {
        const val CHECKIN_MAX_ITERATIONS = 6
    }
}
