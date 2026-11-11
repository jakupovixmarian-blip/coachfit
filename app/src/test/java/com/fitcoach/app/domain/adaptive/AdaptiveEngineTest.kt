package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdaptiveEngineTest {

    private fun profile(goal: Goal) = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0, goal = goal
    )

    @Test
    fun `no changes with insufficient data`() {
        val ctx = AdaptiveEngine.NutritionContext(
            profile = profile(Goal.CUT),
            currentWeeklyKcal = 15000.0,
            currentDailyKcal = 2140.0,
            weightTrendKgPerWeek = -0.5,
            weeksOfData = 1,
            avgAdherencePercent = 95.0,
            avgCaloriesConsumed = 2100.0
        )
        assertThat(AdaptiveEngine.evaluateNutrition(ctx)).isEmpty()
    }

    @Test
    fun `low adherence blocks calorie changes`() {
        val ctx = AdaptiveEngine.NutritionContext(
            profile = profile(Goal.CUT),
            currentWeeklyKcal = 15000.0,
            currentDailyKcal = 2140.0,
            weightTrendKgPerWeek = 0.0,
            weeksOfData = 4,
            avgAdherencePercent = 50.0,
            avgCaloriesConsumed = 2600.0
        )
        val changes = AdaptiveEngine.evaluateNutrition(ctx)
        assertThat(changes).hasSize(1)
        assertThat(changes.first().proposedChange).contains("Nessuna modifica calorica")
    }

    @Test
    fun `stalled cut proposes calorie reduction`() {
        val ctx = AdaptiveEngine.NutritionContext(
            profile = profile(Goal.CUT),
            currentWeeklyKcal = 15000.0,
            currentDailyKcal = 2140.0,
            weightTrendKgPerWeek = 0.0,
            weeksOfData = 3,
            avgAdherencePercent = 90.0,
            avgCaloriesConsumed = 2100.0
        )
        val changes = AdaptiveEngine.evaluateNutrition(ctx)
        assertThat(changes).isNotEmpty()
        val change = changes.first()
        assertThat(change.domain).isEqualTo(ChangeDomain.NUTRITION)
        assertThat(change.proposedChange).contains("Ridurre")
        assertThat(change.rationale).isNotEmpty()
        assertThat(change.contraindications).isNotEmpty()
        assertThat(change.expectedOutcome).isNotEmpty()
    }

    @Test
    fun `too fast loss proposes calorie increase`() {
        val ctx = AdaptiveEngine.NutritionContext(
            profile = profile(Goal.CUT),
            currentWeeklyKcal = 15000.0,
            currentDailyKcal = 2140.0,
            weightTrendKgPerWeek = -1.2,
            weeksOfData = 3,
            avgAdherencePercent = 92.0,
            avgCaloriesConsumed = 2100.0
        )
        val changes = AdaptiveEngine.evaluateNutrition(ctx)
        assertThat(changes.any { it.proposedChange.contains("Aumentare") }).isTrue()
    }

    @Test
    fun `hypertrophy with no gain proposes surplus increase`() {
        val ctx = AdaptiveEngine.NutritionContext(
            profile = profile(Goal.HYPERTROPHY),
            currentWeeklyKcal = 20000.0,
            currentDailyKcal = 2857.0,
            weightTrendKgPerWeek = 0.0,
            weeksOfData = 3,
            avgAdherencePercent = 95.0,
            avgCaloriesConsumed = 2850.0
        )
        val changes = AdaptiveEngine.evaluateNutrition(ctx)
        assertThat(changes.any { it.proposedChange.contains("Aumentare") }).isTrue()
    }

    @Test
    fun `every proposal carries full justification fields`() {
        val ctx = AdaptiveEngine.NutritionContext(
            profile = profile(Goal.CUT),
            currentWeeklyKcal = 15000.0,
            currentDailyKcal = 2140.0,
            weightTrendKgPerWeek = 0.0,
            weeksOfData = 3,
            avgAdherencePercent = 90.0,
            avgCaloriesConsumed = 2100.0
        )
        AdaptiveEngine.evaluateNutrition(ctx).forEach { change ->
            assertThat(change.situation).isNotEmpty()
            assertThat(change.problem).isNotEmpty()
            assertThat(change.dataUsed).isNotEmpty()
            assertThat(change.proposedChange).isNotEmpty()
            assertThat(change.rationale).isNotEmpty()
            assertThat(change.expectedOutcome).isNotEmpty()
            assertThat(change.contraindications).isNotEmpty()
        }
    }

    @Test
    fun `training deload proposed on high recovery severity`() {
        val changes = AdaptiveEngine.evaluateTraining(profile(Goal.HYPERTROPHY), 3, 0, 2)
        assertThat(changes).hasSize(1)
        assertThat(changes.first().domain).isEqualTo(ChangeDomain.WORKOUT)
        assertThat(changes.first().title).contains("Deload")
    }

    @Test
    fun `training deload proposed on repeated pain`() {
        val changes = AdaptiveEngine.evaluateTraining(profile(Goal.HYPERTROPHY), 0, 2, 2)
        assertThat(changes.first().title).contains("Deload")
    }

    @Test
    fun `mesocycle end proposes reevaluation`() {
        val changes = AdaptiveEngine.evaluateTraining(profile(Goal.HYPERTROPHY), 0, 0, 7)
        assertThat(changes.first().title).contains("Rivalutazione")
    }
}
