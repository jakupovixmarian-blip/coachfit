package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.support.SimulatedAiPrograms
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `validate` e `diagnose` devono rifiutare le stesse cose.
 *
 * Sono due implementazioni della stessa regola: `validate` decide il
 * salvataggio, `diagnose` spiega al modello cosa correggere. Erano divergite,
 * e il vincolo sulla colazione esisteva solo nella diagnostica: il modello
 * veniva richiamato con l'errore, ma se insisteva il piano veniva salvato.
 * La prova live ha prodotto una colazione con albume su un profilo "dolce".
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
    private fun withBreakfastFood(
        base: com.fitcoach.app.domain.coach.AiProgramProposal,
        foodName: String
    ): com.fitcoach.app.domain.coach.AiProgramProposal {
        val breakfast = base.nutrition.days.first().meals.first()
        val entry = breakfast.entries.first().copy(foodName = foodName)
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
        val proposal = withBreakfastFood(
            SimulatedAiPrograms.proposal(p, foods, exercises), "Albume d'uovo"
        )

        assertThat(ProgramGenerationEngine.diagnose(p, proposal, foods, exercises))
            .isNotEmpty()
        assertThat(ProgramGenerationEngine.validate(p, proposal, foods, exercises)).isNull()
    }

    @Test
    fun `ogni violazione diagnosticata impedisce il salvataggio`() {
        // Il contratto generale: se la diagnostica trova qualcosa, il cancello
        // non puo' accettare. Protegge da future divergenze tra i due.
        val p = profile(BreakfastPreference.SWEET)
        val proposal = withBreakfastFood(
            SimulatedAiPrograms.proposal(p, foods, exercises), "Albume d'uovo"
        )
        val reasons = ProgramGenerationEngine.diagnose(p, proposal, foods, exercises)
        val validated = ProgramGenerationEngine.validate(p, proposal, foods, exercises)

        if (reasons.isNotEmpty()) {
            assertThat(validated).isNull()
        }
    }
}
