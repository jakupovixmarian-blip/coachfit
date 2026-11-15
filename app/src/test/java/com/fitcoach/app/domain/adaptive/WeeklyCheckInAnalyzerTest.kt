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
 * Verifica che il check-in settimanale produca SOLO MISURE affidabili sui dati
 * reali. Non esistono piu classificazioni decisionali: l'analizzatore calcola i
 * numeri (peso, aderenza, prestazioni, recupero) e dichiara se i dati sono
 * sufficienti. Se un dato manca, il campo resta null: nessun valore inventato.
 */
class WeeklyCheckInAnalyzerTest {

    private val weekStart = 20_000L // lunedi arbitrario

    private val profile = UserProfile(
        id = 1L, name = "Test", ageYears = 30, weightKg = 80.0,
        goal = Goal.RECOMP, trainingDaysPerWeek = 3
    )

    // ---- Dati insufficienti: il report lo dichiara e non inventa valori. ----

    @Test
    fun `dati insufficienti producono un report senza valori inventati`() {
        val report = analyze(weights = emptyList(), nutritionWeeks = emptyList())

        assertThat(report.dataSufficient).isFalse()
        assertThat(report.weightTrendKgPerWeek).isNull()
        assertThat(report.adherencePercent).isNull()
        assertThat(report.avgWeightKg).isNull()
        assertThat(report.bodyFatPercent).isNull()
    }

    // ---- Aderenza bassa: la misura resta, ma i dati non bastano per decidere. ----

    @Test
    fun `aderenza bassa e riportata come misura`() {
        val report = analyze(
            weights = stableWeights(80.0, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 50.0, count = 3)
        )

        assertThat(report.adherencePercent).isWithin(0.5).of(50.0)
    }

    // ---- Peso stabile: la tendenza misurata e ~0 e i dati sono sufficienti. ----

    @Test
    fun `peso stabile con dati sufficienti e misurato correttamente`() {
        val report = analyze(
            weights = stableWeights(80.0, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3)
        )

        assertThat(report.dataSufficient).isTrue()
        assertThat(report.weightTrendKgPerWeek).isWithin(0.05).of(0.0)
        assertThat(report.weightReliable).isTrue()
    }

    // ---- Perdita di peso coerente: la tendenza misurata riflette i dati. ----

    @Test
    fun `perdita di peso coerente e misurata`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 92.0, count = 3)
        )

        assertThat(report.weightTrendKgPerWeek).isWithin(0.05).of(-0.3)
        assertThat(report.dataSufficient).isTrue()
    }

    // ---- Prestazioni: progressione e regressione sono solo misurate. ----

    @Test
    fun `prestazioni in miglioramento sono classificate come progressione`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            performances = mapOf("Panca" to e1rmHistory(100.0, perSession = +2.0))
        )

        assertThat(report.progressionCount).isEqualTo(1)
        assertThat(report.regressionCount).isEqualTo(0)
    }

    @Test
    fun `prestazioni in peggioramento sono classificate come regressione`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            performances = mapOf("Squat" to e1rmHistory(150.0, perSession = -3.0))
        )

        assertThat(report.regressionCount).isEqualTo(1)
        assertThat(report.progressionCount).isEqualTo(0)
    }

    // ---- Recupero scarso: misurato dalle sedute con dolore. ----

    @Test
    fun `il recupero riflette le sedute con dolore segnalato`() {
        val report = analyze(
            weights = linearWeights(startKg = 82.0, perWeek = -0.3, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            sessions = painfulSessions(count = 3)
        )

        assertThat(report.recoverySeverity).isAtLeast(3)
        assertThat(report.painSessions).isAtLeast(3)
        assertThat(report.painNotes).contains("spalla")
    }

    // ---- Circonferenze: la tendenza vita e misurata. ----

    @Test
    fun `la tendenza della vita e misurata dalle circonferenze`() {
        val report = analyze(
            weights = stableWeights(80.0, weeks = 4),
            nutritionWeeks = nutritionWeeks(adherence = 90.0, count = 3),
            circumferences = waist(84.0, perWeek = -0.4, weeks = 5)
        )

        assertThat(report.waistTrendCmPerWeek).isNotNull()
        assertThat(report.waistTrendCmPerWeek!!).isWithin(0.1).of(-0.4)
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
