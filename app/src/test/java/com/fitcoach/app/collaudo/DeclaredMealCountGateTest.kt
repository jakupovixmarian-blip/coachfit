package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.support.SimulatedAiPrograms
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Il numero di pasti dichiarato nel profilo e un vincolo, non un suggerimento.
 *
 * `ProgramGenerationEngine.validate` e l'unico cancello che decide il salvataggio.
 * Accettava 3..6 pasti ignorando il profilo: un piano con un pasto in meno veniva
 * salvato. La prova live su OpenRouter ha prodotto 4 pasti su un profilo da 5 e
 * l'app lo ha accettato.
 */
class DeclaredMealCountGateTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val profile = UserProfile(
        id = 1L, name = "Prova", ageYears = 32, sex = Sex.MALE,
        heightCm = 180.0, weightKg = 82.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.RECOMP,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L,
        anamnesisSummary = "Anamnesi confermata per la prova."
    )

    private val foods = FoodFilter.allowedFoods(FoodSeedData.foods, profile)
    private val exercises = ExerciseSeedData.exercises

    private fun proposal(mealsPerDay: Int) =
        SimulatedAiPrograms.proposal(profile, foods, exercises).let { base ->
            base.copy(
                nutrition = base.nutrition.copy(
                    days = base.nutrition.days.map { day ->
                        day.copy(meals = day.meals.take(mealsPerDay))
                    }
                )
            )
        }

    @Test
    fun `piano con un pasto in meno del dichiarato viene respinto`() {
        val validated = ProgramGenerationEngine.validate(profile, proposal(4), foods, exercises)
        assertThat(validated).isNull()
    }

    @Test
    fun `la diagnosi spiega quanti pasti sono attesi`() {
        val reasons = ProgramGenerationEngine.diagnose(profile, proposal(4), foods, exercises)
        assertThat(reasons.joinToString("\n")).contains("il profilo ne prevede 5")
    }

    @Test
    fun `piano con il numero di pasti dichiarato viene accettato`() {
        val validated = ProgramGenerationEngine.validate(profile, proposal(5), foods, exercises)
        assertThat(validated).isNotNull()
        assertThat(validated!!.nutrition!!.days.map { it.meals.size }.toSet()).isEqualTo(setOf(5))
    }

    @Test
    fun `il vincolo segue il profilo e non un valore fisso`() {
        // Il generatore simulato produce sempre 5 pasti: lo stesso piano viene
        // respinto da un profilo da 4 e accettato da un profilo da 5. Se il
        // cancello usasse una costante, uno dei due casi passerebbe.
        val fiveMeals = SimulatedAiPrograms.proposal(profile, foods, exercises)
        val fourMealsProfile = profile.copy(mealsPerDay = 4)
        val fourMealsFoods = FoodFilter.allowedFoods(FoodSeedData.foods, fourMealsProfile)

        assertThat(ProgramGenerationEngine.validate(fourMealsProfile, fiveMeals, fourMealsFoods, exercises))
            .isNull()
        assertThat(ProgramGenerationEngine.validate(profile, fiveMeals, foods, exercises)).isNotNull()
    }
}
