package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.ui.onboarding.OnboardingUiState
import com.fitcoach.app.ui.onboarding.OnboardingViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Collaudo della provenienza dei dati raccolti dall'onboarding.
 *
 * Il form ha default visibili (obiettivo RECOMP, esperienza BEGINNER, 3 giorni,
 * 5 pasti): registrarli come risposte faceva arrivare all'AI valori che
 * l'utente non aveva mai dichiarato. All'opposto, non registrarli affatto
 * faceva risultare "NON_DISPONIBILE" anche eta, altezza e peso inseriti a mano.
 */
class OnboardingProvenanceCollaudoTest {

    @Test
    fun `i campi obbligatori compilati risultano dichiarati`() {
        val ids = OnboardingViewModel.answeredFrom(OnboardingUiState())
        assertThat(ids).containsAtLeast("age", "heightCm", "weightKg")
    }

    @Test
    fun `i default non toccati non diventano risposte`() {
        val ids = OnboardingViewModel.answeredFrom(OnboardingUiState())
        // Presenti nello stato con un default, mai scelti dall'utente.
        assertThat(ids).doesNotContain("goal")
        assertThat(ids).doesNotContain("experience")
        assertThat(ids).doesNotContain("trainingDaysPerWeek")
        assertThat(ids).doesNotContain("mealsPerDay")
        assertThat(ids).doesNotContain("workType")
    }

    @Test
    fun `una selezione toccata diventa una risposta`() {
        val state = OnboardingUiState(touched = setOf("goal", "experience"))
        val ids = OnboardingViewModel.answeredFrom(state)
        assertThat(ids).containsAtLeast("goal", "experience")
    }

    @Test
    fun `un campo testuale vuoto non e una risposta`() {
        val ids = OnboardingViewModel.answeredFrom(OnboardingUiState())
        assertThat(ids).doesNotContain("bodyFatPercent")
        assertThat(ids).doesNotContain("allergies")
        assertThat(ids).doesNotContain("monthlyFoodBudgetEur")
    }

    @Test
    fun `un campo testuale compilato e una risposta`() {
        val state = OnboardingUiState(
            bodyFat = "18.5",
            allergies = "lattosio",
            monthlyBudgetEur = "250"
        )
        val ids = OnboardingViewModel.answeredFrom(state)
        assertThat(ids).containsAtLeast("bodyFatPercent", "allergies", "monthlyFoodBudgetEur")
    }

    @Test
    fun `salvare di nuovo non cancella le risposte gia dichiarate`() {
        // Regressione: riaprire l'onboarding con un profilo esistente azzerava
        // `touched` e le risposte dell'anamnesi guidata sparivano.
        val ids = OnboardingViewModel.answeredFrom(
            OnboardingUiState(),
            alreadyDeclared = setOf("goal", "trainingDaysPerWeek", "availableEquipment")
        )
        assertThat(ids).containsAtLeast("goal", "trainingDaysPerWeek", "availableEquipment")
    }

    @Test
    fun `ogni id prodotto esiste tra le domande dell'anamnesi`() {
        val known = AnamnesisEngine.questions.map { it.id }.toSet()
        val state = OnboardingUiState(
            touched = setOf("goal", "sex", "availableEquipment", "dietaryPreferences", "priorityMuscles"),
            bodyFat = "20",
            dailySteps = "8000",
            sleepHours = "7",
            trainingYears = "3",
            maxSessionMinutes = "70",
            allergies = "noci",
            likedFoods = "pollo",
            dislikedFoods = "pesce",
            monthlyBudgetEur = "300",
            preferredExercises = "panca",
            dislikedExercises = "stacco",
            physicalLimitations = "spalla",
            movementsToAvoid = "lento avanti"
        )
        val ids = OnboardingViewModel.answeredFrom(state)
        val unknown = ids - known
        assertThat(unknown).isEmpty()
    }

    @Test
    fun `l'esclusione alimentare ora ha una domanda e viene registrata`() {
        // `excludedFoods` alimenta FoodFilter (esclusione dura dal piano), quindi
        // deve essere dichiarabile: ha una domanda nell'anamnesi e il form ne
        // registra la provenienza come per le altre risposte testuali.
        val known = AnamnesisEngine.questions.map { it.id }.toSet()
        assertThat(known).contains("excludedFoods")

        val state = OnboardingUiState(excludedFoods = "glutine")
        val ids = OnboardingViewModel.answeredFrom(state)
        assertThat(ids).contains("excludedFoods")
        assertThat(ids - known).isEmpty()
    }
}
