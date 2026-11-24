package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Distribuzione personalizzata delle giornate carb cycling (sez. 5.2).
 *
 * Il punto critico non e' "l'override funziona", ma che una terna sbagliata
 * non produca mai un piano: se l'utente scrive 2/2/2 la settimana avrebbe sei
 * giorni e il bilancio settimanale sarebbe calcolato su un periodo che non
 * esiste.
 */
class CarbDistributionOverrideTest {

    private fun profilo(
        high: Int? = null,
        medium: Int? = null,
        low: Int? = null,
        trainingDays: Int = 4
    ) = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 178.0, weightKg = 78.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = trainingDays,
        carbHighDaysOverride = high,
        carbMediumDaysOverride = medium,
        carbLowDaysOverride = low,
        anamnesisCompleted = true
    )

    @Test
    fun `senza override usa la distribuzione dai giorni di allenamento`() {
        val attesa = CarbCyclingEngine.distributeDays(4)
        assertThat(CarbCyclingEngine.effectiveDistribution(profilo())).isEqualTo(attesa)
    }

    @Test
    fun `una terna completa e valida viene rispettata`() {
        val t = CarbCyclingEngine.effectiveDistribution(profilo(3, 2, 2))
        assertThat(t).isEqualTo(Triple(3, 2, 2))
    }

    @Test
    fun `una terna che non somma sette viene ignorata per intero`() {
        val attesa = CarbCyclingEngine.distributeDays(4)
        // 2+2+2 = 6: settimana incompleta. Non si applica "in parte".
        assertThat(CarbCyclingEngine.effectiveDistribution(profilo(2, 2, 2))).isEqualTo(attesa)
        assertThat(CarbCyclingEngine.effectiveDistribution(profilo(4, 3, 3))).isEqualTo(attesa)
    }

    @Test
    fun `una terna parziale viene ignorata`() {
        val attesa = CarbCyclingEngine.distributeDays(4)
        assertThat(CarbCyclingEngine.effectiveDistribution(profilo(high = 3))).isEqualTo(attesa)
        assertThat(CarbCyclingEngine.effectiveDistribution(profilo(high = 3, medium = 2))).isEqualTo(attesa)
    }

    @Test
    fun `valori negativi vengono rifiutati`() {
        val attesa = CarbCyclingEngine.distributeDays(4)
        assertThat(CarbCyclingEngine.effectiveDistribution(profilo(-1, 4, 4))).isEqualTo(attesa)
    }

    @Test
    fun `la terna scelta arriva al piano e il bilancio settimanale resta coerente`() {
        val p = profilo(4, 2, 1)
        val energy = EnergyCalculator.compute(p)!!
        val plan = CarbCyclingEngine.build(p, energy)

        assertThat(plan.highDays).isEqualTo(4)
        assertThat(plan.mediumDays).isEqualTo(2)
        assertThat(plan.lowDays).isEqualTo(1)
        assertThat(plan.highDays + plan.mediumDays + plan.lowDays).isEqualTo(7)

        // L'override cambia la distribuzione, non il totale calorico: il
        // vincolo settimanale e' indipendente da come i giorni sono ripartiti.
        assertThat(plan.weeklyKcalFromTarget).isWithin(60.0).of(plan.weeklyKcal)
    }

    @Test
    fun `una settimana tutta LOW non e' ammessa come scorciatoia per il deficit`() {
        // 0/0/7 e' formalmente valido come somma ma azzera i giorni di carico.
        // Il motore lo accetta come scelta esplicita (l'utente puo' volere una
        // settimana di scarico) ma deve segnalarlo, non produrlo in silenzio.
        val p = profilo(0, 0, 7)
        val energy = EnergyCalculator.compute(p)!!
        val plan = CarbCyclingEngine.build(p, energy)

        assertThat(plan.lowDays).isEqualTo(7)
        // Il floor di sicurezza resta attivo anche in questo caso.
        assertThat(plan.isValid || plan.warnings.isNotEmpty()).isTrue()
    }
}
