package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.CircumferenceRecord
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeeklyNutritionSummary
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WorkoutSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifica il comportamento del motore di check-in settimanale sui casi richiesti:
 * dati insufficienti, aderenza bassa, peso stabile, perdita coerente, prestazioni
 * in miglioramento/peggioramento. Il motore NON decide: produce un report e delle
 * classificazioni di supporto che l'AI interpreta.
 */
class WeeklyCheckInAnalyzerTest {

    private val weekStart = 20_000L // lunedi arbitrario

    private val profile = UserProfile(
        id = 1L, name = "Test", ageYears = 30, weightKg = 80.0,
        goal = Goal.RECOMP, trainingDaysPerWeek = 3
    )

    // ---- Dati insufficienti: nessuna decisione, il report lo dichiara. ----

    @Test
    fun `dati insufficienti producono un report senza decisioni operative`() {
        val report = analyze(weights = emptyList(), nutritionWeeks = emptyList())

        assertThat(report.dataSufficient).isFalse()
        assertThat(report.weightTrendKgPerWeek).isNull()
        assertThat(report.adherencePercent).isNull()
        assertThat(report.avgWeightKg).isNull()
        // Nessun valore inventato: i campi non disponibili restano null.
        assertThat(report.bodyFatPercent).isNull()
        assertThat(report.advisories.any { it.classification == "INSUFFICIENT_DATA" }).isTrue()
    }

    // ---- Aderenza bassa: blocca la modifica calorica. ----

    @Test
    fun `aderenza bassa blocca la proposta di modifica calorica`() {
        val report = analyze(
            weights = stableWeights(80.0, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 50.0, count = 3)
        )

        assertThat(report.adherencePercent).isWithin(0.5).of(50.0)
        assertThat(report.advisories.any { it.classification == "ADHERENCE_INSUFFICIENT" }).isTrue()
        assertThat(report.advisories.any {
            it.action?.action == com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES
        }).isFalse()
    }

    // ---- Peso stabile in ricomposizione: proposta di riduzione calorica. ----

    @Test
    fun `peso stabile con aderenza sufficiente propone riduzione calorica`() {
        val report = analyze(
            weights = stableWeights(80.0, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3)
        )

        assertThat(report.dataSufficient).isTrue()
        assertThat(report.weightTrendKgPerWeek).isWithin(0.05).of(0.0)
        val decision = report.advisories.singleOrNull { it.classification == "STAGNATION" }
        assertThat(decision).isNotNull()
        assertThat(decision!!.action?.action)
            .isEqualTo(com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES)
    }

    // ---- Perdita di peso coerente con l'obiettivo: nessuna modifica. ----

    @Test
    fun `perdita di peso coerente non genera proposte`() {
        // RECOMP target -0.25 kg/sett: -0.3 e coerente.
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 92.0, count = 3)
        )

        assertThat(report.weightTrendKgPerWeek).isWithin(0.05).of(-0.3)
        assertThat(report.advisories.any {
            it.action?.action == com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES
        }).isFalse()
    }

    // ---- Perdita troppo rapida: proposta di aumento calorico. ----

    @Test
    fun `perdita troppo rapida propone aumento calorico`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -1.0, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3)
        )

        assertThat(report.advisories.singleOrNull { it.classification == "REGRESSION_NUTRITION" })
            .isNotNull()
        assertThat(report.advisories.single { it.classification == "REGRESSION_NUTRITION" }
            .action?.action)
            .isEqualTo(com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES)
    }

    // ---- Prestazioni in miglioramento: nessun intervento sul volume. ----

    @Test
    fun `prestazioni in miglioramento mantengono il piano`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            performances = mapOf("Panca" to e1rmHistory(100.0, perSession = +2.0))
        )

        assertThat(report.progressionCount).isEqualTo(1)
        assertThat(report.regressionCount).isEqualTo(0)
        assertThat(report.advisories.any { it.classification == "PROGRESSION" }).isTrue()
        assertThat(report.advisories.any {
            it.action?.action == com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_VOLUME
        }).isFalse()
    }

    // ---- Prestazioni in peggioramento: NON aumenta il volume. ----

    @Test
    fun `prestazioni in peggioramento non aumentano il volume`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            performances = mapOf("Squat" to e1rmHistory(150.0, perSession = -3.0))
        )

        assertThat(report.regressionCount).isEqualTo(1)
        val regressionAction = report.advisories.firstOrNull {
            it.classification == "REGRESSION" || it.classification == "REGRESSION_RECOVERY"
        }
        assertThat(regressionAction).isNotNull()
        // In regressione si RIDUCE il volume o si fa deload: mai aumento.
        assertThat(regressionAction!!.action?.action).isAnyOf(
            com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_VOLUME,
            com.fitcoach.app.domain.coach.CoachAction.ACTION_DELOAD
        )
        assertThat(report.advisories.none {
            it.classification == "STAGNATION" && it.action?.action == com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_VOLUME
        }).isTrue()
    }

    // ---- Regressione con recupero scarso: deload, non piu volume. ----

    @Test
    fun `regressione con recupero scarso propone deload`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            performances = mapOf("Stacco" to e1rmHistory(180.0, perSession = -3.0)),
            sessions = painfulSessions(count = 3)
        )

        assertThat(report.recoverySeverity).isAtLeast(3)
        assertThat(report.advisories.singleOrNull { it.classification == "REGRESSION_RECOVERY" }
            ?.action?.action).isEqualTo(com.fitcoach.app.domain.coach.CoachAction.ACTION_DELOAD)
    }

    // ---- Peso stabile ma vita in riduzione: ricomposizione, nessuna modifica. ----

    @Test
    fun `peso stabile con vita in riduzione mantiene il piano`() {
        val report = analyze(
            weights = stableWeights(80.0, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            circumferences = waist(84.0, perWeek = -0.4, weeks = 5)
        )

        assertThat(report.waistTrendCmPerWeek).isNotNull()
        assertThat(report.advisories.any { it.classification == "RECOMPOSITION_PROGRESS" }).isTrue()
        assertThat(report.advisories.any {
            it.action?.action == com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES
        }).isFalse()
    }

    // ---- Il report non inventa il recupero se non ci sono sedute. ----

    @Test
    fun `senza sedute il recupero e zero e non si inventano dati`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            sessions = emptyList()
        )

        assertThat(report.sessionsCompleted).isEqualTo(0)
        assertThat(report.recoverySeverity).isEqualTo(0)
        assertThat(report.sleepAvg).isNull()
    }

    // ---------------------------------------------------------------- helpers

    private fun analyze(
        weights: List<WeightRecord>,
        nutritionWeeks: List<NutritionWeek>,
        performances: Map<String, List<ExercisePerformance>> = emptyMap(),
        sessions: List<WorkoutSession> = emptyList(),
        circumferences: List<CircumferenceRecord> = emptyList()
    ): WeeklyCheckInAnalyzer.Report = WeeklyCheckInAnalyzer.analyze(
        WeeklyCheckInAnalyzer.Input(
            profile = profile,
            weekStartEpochDay = weekStart,
            weights = weights,
            circumferences = circumferences,
            nutritionWeeks = nutritionWeeks,
            recentSessions = sessions,
            performancesByExercise = performances,
            activeNutritionPlanId = 10L,
            activeMesocycleId = 20L,
            currentWeeklyKcal = 14_000.0
        )
    )

    private fun stableWeights(kg: Double, weeks: Int): List<WeightRecord> =
        (0 until weeks).flatMap { w ->
            listOf(
                WeightRecord(dateEpochDay = weekStart - w * 7L, weightKg = kg, bodyFatPercent = 18.0),
                WeightRecord(dateEpochDay = weekStart - w * 7L + 3, weightKg = kg)
            )
        }

    private fun linearWeights(startKg: Double, perWeek: Double, weeks: Int): List<WeightRecord> =
        (0 until weeks).flatMap { w ->
            val kg = startKg + perWeek * (weeks - 1 - w)
            listOf(
                WeightRecord(dateEpochDay = weekStart - w * 7L, weightKg = kg, bodyFatPercent = 18.0),
                WeightRecord(dateEpochDay = weekStart - w * 7L + 3, weightKg = kg)
            )
        }

    private fun waist(startCm: Double, perWeek: Double, weeks: Int): List<CircumferenceRecord> =
        (0 until weeks).map { w ->
            CircumferenceRecord(
                dateEpochDay = weekStart - w * 7L,
                site = "vita",
                valueCm = startCm + perWeek * (weeks - 1 - w)
            )
        }

    private fun nutritionWeeks(adherence: Double, count: Int): List<NutritionWeek> =
        (0 until count).map { offset ->
            NutritionWeek(weekStart - offset * 7L, summary(adherence))
        }

    private fun summary(adherence: Double) = WeeklyNutritionSummary(
        daysTracked = 7,
        avgKcal = 2_000.0,
        avgProtein = 150.0,
        avgCarbs = 200.0,
        avgFat = 70.0,
        avgPlannedKcal = 2_000.0,
        avgAdherencePercent = adherence,
        daysOnTarget = 6,
        extraEntries = 0,
        kcalDifference = 0.0
    )

    /** Storico 1RM (piu recente per primo) con variazione per seduta. */
    private fun e1rmHistory(base: Double, perSession: Double, sessions: Int = 6): List<ExercisePerformance> =
        (0 until sessions).map { i ->
            val value = base + perSession * (sessions - 1 - i)
            ExercisePerformance(
                exerciseId = 1L,
                exerciseName = "X",
                sessionId = i.toLong(),
                dateEpochDay = weekStart - i.toLong(),
                topWeightKg = value,
                topReps = 5,
                totalSets = 4,
                totalReps = 20,
                volumeKg = value * 20,
                estimated1Rm = value
            )
        }

    private fun painfulSessions(count: Int): List<WorkoutSession> =
        (0 until count).map { i ->
            WorkoutSession(
                id = i.toLong(),
                dayTitle = "Sessione $i",
                dateEpochDay = weekStart - i.toLong(),
                status = SessionStatus.COMPLETED,
                perceivedEffort = 9.0,
                sleepQuality = 2,
                painReported = true,
                painNotes = "Fastidio alla spalla"
            )
        }
}
