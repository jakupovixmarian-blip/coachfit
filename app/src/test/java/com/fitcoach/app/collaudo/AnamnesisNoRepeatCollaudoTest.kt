package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.ui.onboarding.OnboardingUiState
import com.fitcoach.app.ui.onboarding.OnboardingViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Onboarding e anamnesi guidata sono due passaggi consecutivi dello stesso
 * flusso. Qui si riproduce il percorso REALE: prima le scelte dell'onboarding
 * (che passano da `select`, quindi finiscono in `touched`), poi l'intervista.
 * Obiettivo: nessuna domanda gia' risposta deve ricomparire.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnamnesisNoRepeatCollaudoTest {

    /** Stato realistico: le scelte fatte nell'onboarding sono tutte in `touched`. */
    private fun stateAfterUserChoices() = OnboardingUiState(
        name = "Test",
        age = "30",
        heightCm = "178",
        weightKg = "78",
        bodyFat = "18",
        sex = Sex.MALE,
        goal = Goal.HYPERTROPHY,
        experience = TrainingExperience.INTERMEDIATE,
        trainingDays = 4,
        budgetPersons = 3,
        touched = setOf(
            "sex", "goal", "experience", "trainingDaysPerWeek", "availableEquipment",
            "budgetPersons", "mealsPerDay", "breakfastPreference", "workType"
        )
    )

    private fun profile(answered: Set<String>) = UserProfile(
        name = "Test",
        ageYears = 30,
        sex = Sex.MALE,
        heightCm = 178.0,
        weightKg = 78.0,
        bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE,
        goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4,
        budgetPersons = 3,
        answeredQuestionIds = answered
    )

    @Test
    fun `le scelte fatte nell'onboarding non vengono richieste dall'anamnesi`() {
        val answered = OnboardingViewModel.answeredFrom(stateAfterUserChoices())
        val p = profile(answered)

        val ripetute = listOf(
            "sex", "goal", "experience", "trainingDaysPerWeek",
            "budgetPersons", "mealsPerDay", "breakfastPreference", "workType"
        ).filter { AnamnesisEngine.needsAnswer(p, it) }

        assertThat(ripetute).isEmpty()
    }

    @Test
    fun `senza dichiarazione l'anamnesi richiede i campi con default`() {
        // Controprova: dimostra che il test sopra non passa "per caso".
        val p = profile(emptySet())
        assertThat(AnamnesisEngine.needsAnswer(p, "goal")).isTrue()
        assertThat(AnamnesisEngine.needsAnswer(p, "experience")).isTrue()
    }

    @Test
    fun `il percorso completo non ripete mai la stessa domanda`() {
        // Simula l'intervista dall'inizio: nessun id deve uscire due volte.
        var p = profile(OnboardingViewModel.answeredFrom(stateAfterUserChoices()))
        val visti = mutableListOf<String>()
        var q = AnamnesisEngine.nextQuestion(p)
        var guard = 0
        while (q != null && guard++ < 200) {
            visti += q.id
            p = AnamnesisEngine.applyAnswer(p, q.id, rispostaPer(q))
            if (rispostaPer(q).isNotBlank()) {
                p = p.copy(answeredQuestionIds = p.answeredQuestionIds + q.id)
            }
            q = AnamnesisEngine.nextQuestionAfter(p, q.id)
        }
        assertThat(visti).containsNoDuplicates()
    }

    @Test
    fun `il contatore persone a carico dichiara la risposta nel ViewModel reale`() {
        // Il VM e' costruito con dipendenze reali su un database in memoria:
        // serve a verificare che il contatore + / - marchi la risposta come
        // dichiarata. Senza questo, l'anamnesi la richiede di nuovo.
        val vm = viewModel()
        vm.setBudgetPersons(4)
        val answered = OnboardingViewModel.answeredFrom(vm.state.value)
        assertThat(answered).contains("budgetPersons")
        assertThat(vm.state.value.budgetPersons).isEqualTo(4)
    }

    private fun viewModel(): OnboardingViewModel {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context, com.fitcoach.app.data.local.FitCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        return OnboardingViewModel(
            com.fitcoach.app.data.repository.UserRepository(db.userProfileDao()),
            com.fitcoach.app.data.repository.PreferencesRepository(db.userPreferencesDao()),
            com.fitcoach.app.data.repository.ProgressRepository(db.progressDao(), db.checkInDao())
        )
    }

    private fun rispostaPer(q: AnamnesisEngine.Question): String = when (q.kind) {
        AnamnesisEngine.Kind.CHOICE, AnamnesisEngine.Kind.MULTI -> q.options.firstOrNull().orEmpty()
        AnamnesisEngine.Kind.BOOL -> "Si"
        else -> "1"
    }
}