package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.ui.dashboard.needsAnamnesis
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Primo avvio: l'invito a completare l'anamnesi deve comparire anche quando il
 * profilo non esiste. Prima la condizione era `profile?.isComplete == true`,
 * che con profilo assente vale false: la card non appariva proprio all'utente
 * che ne aveva bisogno e la Dashboard restava un vicolo cieco.
 */
class DashboardFirstRunCollaudoTest {

    @Test
    fun `profilo assente richiede l'anamnesi`() {
        assertThat(needsAnamnesis(null)).isTrue()
    }

    @Test
    fun `profilo incompleto richiede l'anamnesi`() {
        assertThat(needsAnamnesis(UserProfile())).isTrue()
    }

    @Test
    fun `profilo con dati essenziali ma anamnesi non confermata richiede l'anamnesi`() {
        val p = UserProfile(ageYears = 30, heightCm = 180.0, weightKg = 80.0)
        assertThat(needsAnamnesis(p)).isTrue()
    }

    @Test
    fun `profilo con anamnesi confermata non richiede l'anamnesi`() {
        val p = UserProfile(
            ageYears = 30,
            sex = com.fitcoach.app.domain.model.Sex.MALE,
            heightCm = 180.0,
            weightKg = 80.0,
            sleepHours = 7.0,
            trainingStatus = com.fitcoach.app.domain.model.TrainingStatus.CURRENTLY_TRAINING,
            anamnesisCompleted = true
        )
        assertThat(p.isAnamnesisReady).isTrue()
        assertThat(needsAnamnesis(p)).isFalse()
    }
}
