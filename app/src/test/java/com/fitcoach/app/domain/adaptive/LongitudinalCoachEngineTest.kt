package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeeklyNutritionSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LongitudinalCoachEngineTest {
    private val profile = UserProfile(id = 1L, name = "Test", weightKg = 80.0, goal = Goal.CUT)

    @Test
    fun `progressione su piu settimane non cambia un piano che funziona`() {
        val result = engine(
            weightTrend = -0.5,
            performances = listOf(ExerciseTrend("Panca", ExerciseTrendClass.PROGRESSION, 8.0, 6))
        )

        assertThat(result.any { it.action != null }).isFalse()
        assertThat(result.any { it.classification == "PROGRESSION" }).isTrue()
    }

    @Test
    fun `stagnazione con aderenza sufficiente propone riduzione calorica`() {
        val result = engine(weightTrend = 0.0)

        assertThat(result.any { it.classification == "STAGNATION" }).isTrue()
        assertThat(result.single { it.classification == "STAGNATION" }.action?.action)
            .isEqualTo(com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES)
    }

    @Test
    fun `regressione con recupero scarso propone deload e non piu volume`() {
        val result = engine(
            weightTrend = -0.5,
            recoverySeverity = 3,
            performances = listOf(ExerciseTrend("Squat", ExerciseTrendClass.REGRESSION, -8.0, 6))
        )

        assertThat(result.single { it.classification == "REGRESSION_RECOVERY" }.action?.action)
            .isEqualTo(com.fitcoach.app.domain.coach.CoachAction.ACTION_DELOAD)
    }

    @Test
    fun `peso stabile e vita in calo mantiene il piano`() {
        val result = engine(weightTrend = 0.0, waistTrend = -0.3)

        assertThat(result.any { it.classification == "RECOMPOSITION_PROGRESS" }).isTrue()
        assertThat(result.any { it.action?.action == com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES }).isFalse()
    }

    @Test
    fun `aderenza insufficiente blocca la modifica calorica`() {
        val result = engine(weightTrend = 0.0, adherence = 55.0)

        assertThat(result.single { it.classification == "ADHERENCE_INSUFFICIENT" }.action).isNull()
        assertThat(result.any { it.action?.action == com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES }).isFalse()
    }

    @Test
    fun `la classificazione distingue stagnazione e progressione`() {
        val stagnant = LongitudinalCoachEngine.classifyExercise("Panca", listOf(100.0, 101.0, 99.0, 100.0, 100.0, 99.0))
        val progressing = LongitudinalCoachEngine.classifyExercise("Panca", listOf(110.0, 109.0, 111.0, 100.0, 101.0, 99.0))

        assertThat(stagnant.classification).isEqualTo(ExerciseTrendClass.STAGNATION)
        assertThat(progressing.classification).isEqualTo(ExerciseTrendClass.PROGRESSION)
    }

    private fun engine(
        weightTrend: Double,
        waistTrend: Double? = null,
        adherence: Double = 90.0,
        recoverySeverity: Int = 0,
        performances: List<ExerciseTrend> = emptyList()
    ): List<AdaptiveDecision> = LongitudinalCoachEngine.evaluate(
        LongitudinalContext(
            profile = profile,
            planId = 10L,
            mesocycleId = 20L,
            currentWeeklyKcal = 14_000.0,
            weightTrendKgPerWeek = weightTrend,
            reliableWeightTrend = true,
            weightWeeks = 3,
            waistTrendCmPerWeek = waistTrend,
            nutritionWeeks = listOf(
                NutritionWeek(14L, summary(adherence)),
                NutritionWeek(7L, summary(adherence))
            ),
            performances = performances,
            recoverySeverity = recoverySeverity,
            painSessions = 0,
            recentSessions = emptyList()
        )
    )

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
}
