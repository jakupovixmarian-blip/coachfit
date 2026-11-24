package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.engine.EngineParameters
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.TransportMode
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkType
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

    @Test
    fun `neat adjustment reflects lifestyle work type`() {
        val sedentary = maleProfile.copy(workType = WorkType.SEDENTARY)
        val heavy = maleProfile.copy(workType = WorkType.HEAVY)
        assertThat(EnergyCalculator.neatAdjustment(heavy))
            .isGreaterThan(EnergyCalculator.neatAdjustment(sedentary))
    }

    @Test
    fun `more daily steps increase activity factor`() {
        val few = maleProfile.copy(dailySteps = 2000)
        val many = maleProfile.copy(dailySteps = 15000)
        assertThat(EnergyCalculator.activityFactor(many))
            .isGreaterThan(EnergyCalculator.activityFactor(few))
    }

    @Test
    fun `walking commute increases activity factor versus car`() {
        val car = maleProfile.copy(transportMode = TransportMode.CAR)
        val walk = maleProfile.copy(transportMode = TransportMode.WALK)
        assertThat(EnergyCalculator.activityFactor(walk))
            .isGreaterThan(EnergyCalculator.activityFactor(car))
    }

    @Test
    fun `extra sport sessions increase activity factor`() {
        val none = maleProfile.copy(extraSportSessionsPerWeek = 0)
        val some = maleProfile.copy(extraSportSessionsPerWeek = 3)
        assertThat(EnergyCalculator.activityFactor(some))
            .isGreaterThan(EnergyCalculator.activityFactor(none))
    }

    @Test
    fun `manual activity override wins over lifestyle`() {
        val overridden = maleProfile.copy(
            activityFactorOverride = 1.9, workType = WorkType.SEDENTARY, dailySteps = 1000
        )
        assertThat(EnergyCalculator.activityFactor(overridden)).isWithin(0.0001).of(1.9)
    }

    /**
     * Regressione del caso segnalato dall'utente: intermedio, 5 allenamenti,
     * lavoro d'ufficio, ~0 passi. La banda 4-5 era [1,99, 2,00], larga 0,01:
     * la correzione per lo stile sedentario veniva schiacciata e il TDEE
     * usciva a 3.348 kcal con BMR 1.682, cioe' PAL 1,990 di un atleta molto
     * attivo. Il coach a quel punto "correggeva" a 2.150 kcal (PAL 1,275),
     * sotto il pavimento FAO di 1,40: due errori che si annullavano a vicenda.
     */
    @Test
    fun `sedentary office worker who trains does not get an athlete activity factor`() {
        val officeWorker = maleProfile.copy(
            trainingDaysPerWeek = 5,
            workType = WorkType.SEDENTARY,
            dailySteps = 0,
            transportMode = TransportMode.CAR
        )
        val factor = EnergyCalculator.activityFactor(officeWorker)

        // Il lavoro d'ufficio deve contare: il valore non puo' restare al centro
        // della banda come se lo stile di vita non fosse stato dichiarato.
        assertThat(factor).isLessThan(1.85)
        // ...ma restare dentro la banda FAO, non scendere al pavimento fisiologico.
        assertThat(factor).isAtLeast(EngineParameters.palModerateMin.value)
        assertThat(factor).isAtMost(EngineParameters.palVigorousMin.value)

        // Lo scenario reale: BMR 1682 -> il TDEE non e' piu' 3.348 kcal.
        val bmr = 1682.0
        assertThat(bmr * factor).isLessThan(3000.0)
    }

    @Test
    fun `lifestyle shifts activity factor even with many training days`() {
        val base = maleProfile.copy(trainingDaysPerWeek = 5, dailySteps = 5000)
        val sedentary = base.copy(workType = WorkType.SEDENTARY, dailySteps = 0)
        val manual = base.copy(workType = WorkType.MANUAL, dailySteps = 15000)

        // Se le bande collassano, questa differenza diventa zero.
        assertThat(EnergyCalculator.activityFactor(manual))
            .isGreaterThan(EnergyCalculator.activityFactor(sedentary) + 0.10)
    }

    @Test
    fun `activity factor stays inside the band for every training frequency`() {
        (0..7).forEach { days ->
            val factor = EnergyCalculator.activityFactor(
                maleProfile.copy(trainingDaysPerWeek = days, workType = WorkType.HEAVY, dailySteps = 20000)
            )
            assertThat(factor).isAtLeast(EngineParameters.palPhysiologicalFloor.value)
            assertThat(factor).isAtMost(EngineParameters.palSustainableCeiling.value)
        }
    }

    @Test
    fun `computed energy exposes the activity factor it used`() {
        val result = EnergyCalculator.compute(maleProfile.copy(trainingDaysPerWeek = 5))!!
        assertThat(result.activityFactor)
            .isWithin(0.0001).of(EnergyCalculator.activityFactor(maleProfile.copy(trainingDaysPerWeek = 5)))
    }

    @Test
    fun `calorie target override sets weekly calories`() {
        val overridden = maleProfile.copy(calorieTargetOverride = 2500.0)
        val result = EnergyCalculator.compute(overridden)!!
        assertThat(result.weeklyCalories).isWithin(0.001).of(17500.0)
    }
}
