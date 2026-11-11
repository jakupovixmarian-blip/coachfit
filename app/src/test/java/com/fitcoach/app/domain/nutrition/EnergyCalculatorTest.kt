package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnergyCalculatorTest {

    private val maleProfile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 4, experience = TrainingExperience.INTERMEDIATE
    )

    @Test
    fun `mifflin bmr for male matches formula`() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        val bmr = EnergyCalculator.bmrMifflin(maleProfile)
        assertThat(bmr).isWithin(0.001).of(1780.0)
    }

    @Test
    fun `mifflin bmr for female uses -161 term`() {
        val female = maleProfile.copy(sex = Sex.FEMALE)
        // 800 + 1125 - 150 - 161 = 1614
        assertThat(EnergyCalculator.bmrMifflin(female)).isWithin(0.001).of(1614.0)
    }

    @Test
    fun `katch mcardle uses lean mass`() {
        val profile = maleProfile.copy(bodyFatPercent = 20.0)
        // lean = 80 * 0.8 = 64; 370 + 21.6*64 = 370 + 1382.4 = 1752.4
        assertThat(EnergyCalculator.bmrKatchMcArdle(profile)).isWithin(0.001).of(1752.4)
    }

    @Test
    fun `katch preferred when body fat present`() {
        val profile = maleProfile.copy(bodyFatPercent = 20.0)
        val result = EnergyCalculator.compute(profile)!!
        assertThat(result.bmrFormula).isEqualTo("Katch-McArdle")
        assertThat(result.leanMassKg).isWithin(0.001).of(64.0)
    }

    @Test
    fun `compute returns null without weight or height`() {
        assertThat(EnergyCalculator.compute(UserProfile(ageYears = 30, sex = Sex.MALE))).isNull()
    }

    @Test
    fun `activity factor increases with training days`() {
        val low = EnergyCalculator.activityFactor(maleProfile.copy(trainingDaysPerWeek = 2))
        val high = EnergyCalculator.activityFactor(maleProfile.copy(trainingDaysPerWeek = 5))
        assertThat(high).isGreaterThan(low)
    }

    @Test
    fun `tdee equals bmr times activity factor`() {
        val result = EnergyCalculator.compute(maleProfile)!!
        val factor = EnergyCalculator.activityFactor(maleProfile)
        assertThat(result.tdee).isWithin(0.001).of(result.bmr * factor)
    }
}
