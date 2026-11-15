package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.AdaptiveDecision
import com.fitcoach.app.domain.adaptive.ExerciseTrend
import com.fitcoach.app.domain.adaptive.NutritionWeek
import com.fitcoach.app.domain.adaptive.WeeklyCheckInAnalyzer
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strumento di ANALISI del check-in settimanale.
 *
 * Aggrega i dati REALI degli ultimi 7 giorni e li confronta con le settimane
 * precedenti, restituendo un report strutturato con le classificazioni di
 * supporto (tendenza peso, aderenza, prestazioni, recupero). E un SUBORDINATO
 * all'AI centrale: calcola e classifica, ma NON decide e non modifica nulla.
 * L'AI interpreta il report e, se serve, propone con gli strumenti "propose_*".
 */
@Singleton
class AnalyzeWeeklyCheckInTool @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository,
    private val nutritionRepository: NutritionRepository,
    private val diaryRepository: NutritionDiaryRepository,
    private val workoutRepository: WorkoutRepository
) : CoachTool {
    override val name = "analyze_weekly_checkin"
    override val description =
        "Restituisce il report del check-in settimanale sui dati REALI: tendenza del peso (medie " +
            "settimanali e affidabilita), composizione, aderenza nutrizionale, calorie e macro " +
            "consumati, allenamenti completati/pianificati, andamento delle prestazioni, recupero, " +
            "sonno e dolore. Include classificazioni di supporto (advisory) ma NON decide: sei tu a " +
            "interpretare i dati e a proporre eventuali modifiche con gli strumenti propose_*. " +
            "Se un campo e null il dato NON e disponibile: non inventarlo."

    override val parameters: JsonObject = Schema.obj(
        "settimaneNutrizione" to Schema.integer("Quante settimane di diario considerare (default 6)."),
        "settimanePeso" to Schema.integer("Quante settimane di medie peso considerare (default 8).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val args = JsonArgs(arguments)
        val nutritionWeeks = (args.int("settimaneNutrizione") ?: 6).coerceIn(1, 12)
        val weightWeeks = (args.int("settimanePeso") ?: 8).coerceIn(1, 26)

        val profile = userRepository.get()
            ?: return JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "error" to JsonOut.str("Profilo non configurato.")))

        val weekStart = NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay())
        val weights = progressRepository.getWeights()
        val nutritionRepoWeeks = (0 until nutritionWeeks).map { offset ->
            val start = weekStart - offset * 7L
            NutritionWeek(start, diaryRepository.weeklySummary(start))
        }
        val sessions = workoutRepository.getRecentSessions(limit = 90)
        val nutritionPlan = nutritionRepository.getActivePlan()
        val workoutPlan = workoutRepository.getActivePlan()

        val report = WeeklyCheckInAnalyzer.analyze(
            WeeklyCheckInAnalyzer.Input(
                profile = profile,
                weekStartEpochDay = weekStart,
                weights = weights,
                circumferences = progressRepository.getCircumferences(),
                nutritionWeeks = nutritionRepoWeeks,
                recentSessions = sessions,
                performancesByExercise = workoutRepository.getPerformanceByExercise(),
                activeNutritionPlanId = nutritionPlan?.id,
                activeMesocycleId = workoutPlan?.mesocycles?.firstOrNull()?.id,
                currentWeeklyKcal = nutritionPlan?.weeklyCalories ?: 0.0,
                weightWeeks = weightWeeks
            )
        )

        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "settimanaInizioEpochDay" to JsonOut.num(report.weekStartEpochDay),
                "datiSufficienti" to JsonOut.bool(report.dataSufficient),
                "peso" to JsonOut.obj(
                    "settimaneDati" to JsonOut.num(report.weeksOfWeightData),
                    "affidabile" to JsonOut.bool(report.weightReliable),
                    "trendKgSettimana" to JsonOut.num(report.weightTrendKgPerWeek),
                    "mediaKg" to JsonOut.num(report.avgWeightKg),
                    "massaGrassaPerc" to JsonOut.num(report.bodyFatPercent),
                    "vitaTrendCmSettimana" to JsonOut.num(report.waistTrendCmPerWeek)
                ),
                "nutrizione" to JsonOut.obj(
                    "settimaneConDati" to JsonOut.num(report.nutritionWeeksTracked),
                    "aderenzaPerc" to JsonOut.num(report.adherencePercent),
                    "kcalMedieConsumate" to JsonOut.num(report.avgKcalConsumed),
                    "kcalMediePreviste" to JsonOut.num(report.avgPlannedKcal),
                    "proteineMedieG" to JsonOut.num(report.avgProtein),
                    "carboidratiMediG" to JsonOut.num(report.avgCarbs),
                    "grassiMediG" to JsonOut.num(report.avgFat)
                ),
                "allenamento" to JsonOut.obj(
                    "sedutePianificate" to JsonOut.num(report.sessionsPlanned),
                    "seduteCompletate" to JsonOut.num(report.sessionsCompleted),
                    "recuperoSeverita" to JsonOut.num(report.recoverySeverity),
                    "recuperoSpiegazione" to JsonOut.str(report.recoveryExplanation),
                    "sonnoMedio" to JsonOut.num(report.sleepAvg),
                    "seduteConDolore" to JsonOut.num(report.painSessions),
                    "noteDolore" to JsonOut.str(report.painNotes.ifBlank { null })
                ),
                "prestazioni" to JsonOut.arr(report.performanceTrends.map { it.toJson() }),
                "advisory" to JsonOut.arr(report.advisories.map { it.toJson() }),
                "nota" to JsonOut.str(
                    "Le classificazioni 'advisory' sono un supporto, non una decisione. Valuta i dati, " +
                        "confronta con le settimane precedenti e proponi solo se giustificato. Non " +
                        "ridurre le calorie per una singola settimana senza valutare trend e aderenza."
                )
            )
        )
    }

    private fun ExerciseTrend.toJson(): JsonObject = JsonOut.obj(
        "esercizio" to JsonOut.str(exerciseName),
        "classificazione" to JsonOut.str(classification.name),
        "variazionePerc" to JsonOut.num(changePercent),
        "campioni" to JsonOut.num(samples)
    )

    private fun AdaptiveDecision.toJson(): JsonObject = JsonOut.obj(
        "classificazione" to JsonOut.str(classification),
        "motivo" to JsonOut.str(reason),
        "azioneSuggerita" to JsonOut.str(action?.action)
    )
}
