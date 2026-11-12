package com.fitcoach.nutrition

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnergyCalculatorTest {
    @Test
    fun `mifflin estimate uses sex and goal`() {
        val male = EnergyCalculator.estimate(30, BiologicalSex.MALE, 180.0, 80.0, 1.5, NutritionGoal.MAINTENANCE)
        val female = EnergyCalculator.estimate(30, BiologicalSex.FEMALE, 180.0, 80.0, 1.5, NutritionGoal.MAINTENANCE)
        assertThat(male.bmrKcal).isEqualTo(1780)
        assertThat(male.targetKcal).isGreaterThan(female.targetKcal)
    }

    @Test
    fun `carb cycling rejects inconsistent weekly balance`() {
        val result = runCatching {
            CarbCyclingCalculator.create(3, 2, 2, 14_000, 160, 350, 250, 100, 60)
        }
        assertThat(result.isFailure).isTrue()
    }
}
