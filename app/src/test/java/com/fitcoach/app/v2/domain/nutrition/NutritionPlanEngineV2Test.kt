package com.fitcoach.app.v2.domain.nutrition

import com.fitcoach.app.v2.domain.generation.GenerationProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class NutritionPlanEngineV2Test {
    @Test
    fun `weekly carb cycling preserves target balance and macro arithmetic`() {
        val plan = NutritionPlanEngineV2.build(profile(trainingDays = 4))

        assertThat(plan.dayTypes).hasSize(7)
        assertThat(plan.dayTypes.count { it == CarbDayTypeV2.HIGH }).isEqualTo(2)
        assertThat(plan.dayTypes.count { it == CarbDayTypeV2.MEDIUM }).isEqualTo(1)
        assertThat(plan.dayTypes.count { it == CarbDayTypeV2.LOW }).isEqualTo(4)
        assertThat(abs(plan.weeklyCalories - plan.targetWeeklyCalories)).isLessThan(15.0)
        plan.targets.values.forEach { assertThat(abs(it.calories - it.macroCalories)).isLessThan(15.0) }
        assertThat(plan.targets.getValue(CarbDayTypeV2.HIGH).carbohydrateGrams)
            .isGreaterThan(plan.targets.getValue(CarbDayTypeV2.LOW).carbohydrateGrams)
    }

    @Test
    fun `unknown goal or sex is rejected instead of using an invented default`() {
        assertThat(runCatching { NutritionPlanEngineV2.build(profile(primaryGoal = "altro")) }.isFailure).isTrue()
        assertThat(runCatching { NutritionPlanEngineV2.build(profile(sex = "non dichiarato")) }.isFailure).isTrue()
    }

    private fun profile(
        trainingDays: Int = 4,
        primaryGoal: String = "ricomposizione",
        sex: String = "F"
    ) = GenerationProfile(
        ageYears = 30,
        biologicalSex = sex,
        heightCm = 170.0,
        weightKg = 65.0,
        bodyFatPercent = null,
        primaryGoal = primaryGoal,
        trainingExperience = "intermedio",
        equipment = "palestra",
        trainingDays = trainingDays,
        answers = emptyMap()
    )
}
