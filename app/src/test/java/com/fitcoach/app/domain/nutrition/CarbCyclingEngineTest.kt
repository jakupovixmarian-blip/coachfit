package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class CarbCyclingEngineTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 4, experience = TrainingExperience.INTERMEDIATE, goal = Goal.RECOMP
    )

    private val energy = EnergyCalculator.compute(profile)!!

    @Test
    fun `weekly total matches planned balance`() {
        val plan = CarbCyclingEngine.build(profile, energy)
        val total = plan.weeklyKcalFromTarget
        assertThat(abs(total - plan.weeklyKcal)).isLessThan(60.0)
    }

    @Test
    fun `macro coherence is within tolerance for every day type`() {
        val plan = CarbCyclingEngine.build(profile, energy)
        plan.targets.values.forEach { target ->
            assertThat(CarbCyclingEngine.validateCoherence(target)).isLessThan(CarbCyclingEngine.KCAL_TOLERANCE)
        }
    }

    @Test
    fun `high carb days have more carbs than low carb days`() {
        val plan = CarbCyclingEngine.build(profile, energy)
        val high = plan.targets[CarbDayType.HIGH]!!
        val low = plan.targets[CarbDayType.LOW]!!
        assertThat(high.carbs).isGreaterThan(low.carbs)
    }

    @Test
    fun `low carb days have more fat than high carb days`() {
        val plan = CarbCyclingEngine.build(profile, energy)
        assertThat(plan.targets[CarbDayType.LOW]!!.fat)
            .isAtLeast(plan.targets[CarbDayType.HIGH]!!.fat)
    }

    @Test
    fun `protein is constant across day types`() {
        val plan = CarbCyclingEngine.build(profile, energy)
        val proteins = plan.targets.values.map { it.protein }.distinct()
        assertThat(proteins).hasSize(1)
    }

    @Test
    fun `protein equals weight times protein per kg`() {
        val plan = CarbCyclingEngine.build(profile, energy)
        val expected = 80.0 * CarbCyclingEngine.proteinPerKg(Goal.RECOMP)
        assertThat(plan.targets[CarbDayType.HIGH]!!.protein).isWithin(1.0).of(expected)
    }

    @Test
    fun `cut goal produces weekly deficit`() {
        val cutProfile = profile.copy(goal = Goal.CUT)
        val cutEnergy = EnergyCalculator.compute(cutProfile)!!
        val plan = CarbCyclingEngine.build(cutProfile, cutEnergy)
        assertThat(plan.weeklyKcal).isLessThan(cutEnergy.tdee * 7)
    }

    @Test
    fun `hypertrophy goal produces weekly surplus`() {
        val hyperProfile = this.profile.copy(goal = Goal.HYPERTROPHY)
        val hyperEnergy = EnergyCalculator.compute(hyperProfile)!!
        val plan = CarbCyclingEngine.build(hyperProfile, hyperEnergy)
        assertThat(plan.weeklyKcal).isGreaterThan(hyperEnergy.tdee * 7)
    }

    @Test
    fun `safety floor prevents extreme deficit`() {
        val aggressive = profile.copy(goal = Goal.CUT)
        val energy = EnergyCalculator.compute(aggressive)!!
        val plan = CarbCyclingEngine.build(
            aggressive, energy, weeklyAdjustmentOverride = -0.5
        )
        // Il piano segnala il problema di sicurezza.
        assertThat(plan.warnings.any { it.contains("soglia di sicurezza") }).isTrue()
        assertThat(plan.isValid).isFalse()
    }

    @Test
    fun `day type distribution covers all seven days`() {
        val (high, medium, low) = CarbCyclingEngine.distributeDays(4)
        assertThat(high + medium + low).isEqualTo(7)
    }

    @Test
    fun `assign day types marks training days as high first`() {
        val types = CarbCyclingEngine.assignDayTypes(2, 1, 4, listOf(0, 1, 3, 4))
        assertThat(types[0]).isEqualTo(CarbDayType.HIGH)
        assertThat(types[1]).isEqualTo(CarbDayType.HIGH)
        assertThat(types[3]).isEqualTo(CarbDayType.MEDIUM)
        assertThat(types[2]).isEqualTo(CarbDayType.LOW)
    }

    @Test
    fun `fat never below safety minimum`() {
        val plan = CarbCyclingEngine.build(profile, energy)
        val minFat = 80.0 * CarbCyclingEngine.MIN_FAT_G_PER_KG
        plan.targets.values.forEach { assertThat(it.fat).isAtLeast(minFat - 1.0) }
    }
}
