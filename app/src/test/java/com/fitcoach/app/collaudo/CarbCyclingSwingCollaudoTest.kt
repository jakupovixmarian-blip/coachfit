package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.engine.EngineParameters
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * L'escursione dei carboidrati deve restare nei 50-100 g che il corpus documenta,
 * su qualunque profilo. Prima era una percentuale del totale: su un target alto
 * produceva escursioni molto maggiori, senza che nessuno se ne accorgesse.
 */
class CarbCyclingSwingCollaudoTest {

    private fun profilo(peso: Double, goal: Goal) = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 178.0, weightKg = peso, bodyFatPercent = 15.0,
        experience = TrainingExperience.INTERMEDIATE, goal = goal,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    private fun escursione(peso: Double, goal: Goal): Double {
        val p = profilo(peso, goal)
        val e = EnergyCalculator.compute(p)!!
        val piano = CarbCyclingEngine.build(p, e)
        return piano.targets[CarbDayType.HIGH]!!.carbs - piano.targets[CarbDayType.LOW]!!.carbs
    }

    @Test
    fun `l'escursione resta nei 50-100 grammi su tutti i profili`() {
        listOf(
            Triple(60.0, Goal.CUT, "cut leggero"),
            Triple(78.0, Goal.RECOMP, "ricomposizione"),
            Triple(95.0, Goal.HYPERTROPHY, "ipertrofia pesante")
        ).forEach { (peso, goal, nome) ->
            val delta = escursione(peso, goal)
            assertThat(delta).isAtLeast(50.0)
            assertThat(delta).isAtMost(100.0)
        }
    }

    @Test
    fun `nei giorni OFF i grassi pesano piu' che nei giorni ON`() {
        // La guida: "giorno ON ... limitando le fonti lipidiche (senza
        // eliminarle)", "giorno OFF: viene dato piu' spazio agli alimenti
        // lipidici".
        //
        // Il confronto va fatto sulla QUOTA, non sui grammi: i giorni OFF hanno
        // meno calorie, quindi il 30% di meno kcal sono anche meno grammi.
        //
        // HIGH e MEDIUM possono coincidere: entrambi stanno al pavimento del 20%
        // perche' lo scarto e' limitato dal vincolo che i giorni ON restino i
        // piu' calorici. Cio' che deve valere sempre e' HIGH < LOW.
        val p = profilo(78.0, Goal.RECOMP)
        val e = EnergyCalculator.compute(p)!!
        val piano = CarbCyclingEngine.build(p, e)
        fun quota(type: CarbDayType): Double {
            val t = piano.targets[type]!!
            return t.fat * 9.0 / t.kcal
        }
        assertThat(quota(CarbDayType.HIGH)).isLessThan(quota(CarbDayType.LOW))
        assertThat(piano.targets[CarbDayType.HIGH]!!.fat)
            .isLessThan(piano.targets[CarbDayType.LOW]!!.fat)
        // Il MEDIUM sta nella fascia tra i due, senza scendere sotto il pavimento.
        assertThat(quota(CarbDayType.MEDIUM)).isAtLeast(quota(CarbDayType.HIGH) - 0.01)
        assertThat(quota(CarbDayType.MEDIUM)).isAtMost(quota(CarbDayType.LOW) + 0.01)
    }

    @Test
    fun `i grassi restano dentro i limiti documentati`() {
        // Mai sotto il 15% (Helms) e mai sopra il 30% delle calorie del giorno:
        // "senza eliminarle" non vuol dire zero, e una dieta al 50% di grassi
        // non e' quella che la guida descrive.
        val p = profilo(78.0, Goal.RECOMP)
        val e = EnergyCalculator.compute(p)!!
        val piano = CarbCyclingEngine.build(p, e)
        piano.targets.forEach { (tipo, t) ->
            val quota = t.fat * 9.0 / t.kcal
            assertThat(quota).isAtLeast(0.14)
            assertThat(quota).isAtMost(0.31)
        }
    }

    @Test
    fun `il bilancio settimanale resta invariato`() {
        val p = profilo(78.0, Goal.RECOMP)
        val e = EnergyCalculator.compute(p)!!
        val piano = CarbCyclingEngine.build(p, e)
        val somma = piano.targets[CarbDayType.HIGH]!!.kcal * piano.highDays +
            piano.targets[CarbDayType.MEDIUM]!!.kcal * piano.mediumDays +
            piano.targets[CarbDayType.LOW]!!.kcal * piano.lowDays
        // Grammi interi: lo scarto e' arrotondamento, non errore di bilancio.
        assertThat(somma).isWithin(30.0).of(piano.weeklyKcal)
    }

    @Test
    fun `il parametro dell'escursione e' documentato e nei limiti`() {
        assertThat(EngineParameters.carbCyclingSwingGrams.value).isEqualTo(75.0)
        assertThat(EngineParameters.carbCyclingSwingGrams.origin)
            .isEqualTo(EngineParameters.Origin.PRIMARY)
        assertThat(EngineParameters.carbCyclingSwingGrams.study?.pmid).isEqualTo("37229479")
    }
}
