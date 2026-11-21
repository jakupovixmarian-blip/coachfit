package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiProgramProposal
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.coach.ProposalChecker
import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.support.SimulatedAiPrograms
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Il cancello e la diagnostica sono la stessa funzione.
 *
 * Storicamente erano due implementazioni della stessa regola, e sono divergite:
 * il vincolo dolce/salato della colazione esisteva solo nella diagnostica, quindi
 * il piano veniva respinto con un messaggio chiaro e poi **salvato se il modello
 * insisteva**. La prova live ha prodotto una colazione con albume su un profilo
 * "dolce".
 *
 * Ora [ProposalChecker.check] produce l'esito, e `validate`/`diagnose` ne sono due
 * viste. Questi test bloccano il ritorno a due verita'.
 */
class GateDiagnosticsAgreementTest {

    private val foods = FoodSeedData.foods
    private val exercises = ExerciseSeedData.exercises

    private fun profile(preference: BreakfastPreference) = UserProfile(
        id = 1L, name = "Prova", ageYears = 32, sex = Sex.MALE,
        heightCm = 180.0, weightKg = 82.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.RECOMP,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        breakfastPreference = preference,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L,
        anamnesisSummary = "Anamnesi confermata per la prova."
    )

    /** Mette [foodName] nella colazione del giorno 0, lasciando il resto intatto. */
    private fun withBreakfastFood(base: AiProgramProposal, foodName: String): AiProgramProposal {
        val entry = base.nutrition.days.first().meals.first().entries.first()
            .copy(foodName = foodName)
        return base.copy(
            nutrition = base.nutrition.copy(
                days = base.nutrition.days.mapIndexed { index, day ->
                    if (index != 0) return@mapIndexed day
                    day.copy(
                        meals = day.meals.mapIndexed { mealIndex, meal ->
                            if (mealIndex != 0) return@mapIndexed meal
                            meal.copy(entries = listOf(entry))
                        }
                    )
                }
            )
        )
    }

    @Test
    fun `alimento salato a colazione dolce viene bloccato dal cancello`() {
        val p = profile(BreakfastPreference.SWEET)
        val proposal = withBreakfastFood(SimulatedAiPrograms.proposal(p, foods, exercises), "Albume d'uovo")

        assertThat(ProgramGenerationEngine.validate(p, proposal, foods, exercises)).isNull()
        assertThat(
            ProgramGenerationEngine.diagnose(p, proposal, foods, exercises).any { it.contains("colazione") }
        ).isTrue()
    }

    @Test
    fun `la proposta valida passa il cancello e non produce violazioni`() {
        val p = profile(BreakfastPreference.SWEET)
        val proposal = SimulatedAiPrograms.proposal(p, foods, exercises)

        assertThat(ProgramGenerationEngine.validate(p, proposal, foods, exercises)).isNotNull()
        assertThat(ProposalChecker.check(p, proposal, foods, exercises).violations).isEmpty()
    }

    /**
     * Il contratto che rende impossibile la divergenza: `validate` accetta
     * esattamente quando il checker non trova violazioni. Se un giorno qualcuno
     * reintroduce un controllo dentro `validate`, questo test cade.
     */
    @Test
    fun `validate accetta se e solo se il checker non trova violazioni`() {
        val p = profile(BreakfastPreference.SWEET)
        val valid = SimulatedAiPrograms.proposal(p, foods, exercises)
        val candidates = listOf(
            "valida" to valid,
            "colazione salata" to withBreakfastFood(valid, "Albume d'uovo"),
            "alimento inesistente" to withBreakfastFood(valid, "Alimento Inesistente"),
            "giorni allenamento sbagliati" to valid.copy(
                workout = valid.workout.copy(days = valid.workout.days.take(2))
            ),
            "tipo giornata non valido" to valid.copy(
                nutrition = valid.nutrition.copy(
                    days = valid.nutrition.days.map { it.copy(dayType = "EXTREME") }
                )
            )
        )

        candidates.forEach { (label, proposal) ->
            val report = ProposalChecker.check(p, proposal, foods, exercises)
            val accepted = ProgramGenerationEngine.validate(p, proposal, foods, exercises) != null
            assertWithMessage("$label: accettato=$accepted, violazioni=${report.violations}")
                .that(accepted)
                .isEqualTo(report.violations.isEmpty())
        }
    }

    /**
     * Una bocciatura deve sempre avere una spiegazione: mai il messaggio
     * generico, che lascerebbe il modello senza nulla da correggere.
     */
    @Test
    fun `ogni bocciatura ha una motivazione specifica`() {
        val p = profile(BreakfastPreference.SWEET)
        val valid = SimulatedAiPrograms.proposal(p, foods, exercises)
        val broken = listOf(
            withBreakfastFood(valid, "Albume d'uovo"),
            withBreakfastFood(valid, "Alimento Inesistente"),
            valid.copy(workout = valid.workout.copy(days = valid.workout.days.take(2)))
        )

        broken.forEach { proposal ->
            assertThat(ProgramGenerationEngine.validate(p, proposal, foods, exercises)).isNull()
            assertThat(ProgramGenerationEngine.diagnose(p, proposal, foods, exercises))
                .doesNotContain(ProposalChecker.GENERIC_FALLBACK)
        }
    }
}
