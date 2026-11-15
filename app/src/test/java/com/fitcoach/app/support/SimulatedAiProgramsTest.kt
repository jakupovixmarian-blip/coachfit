package com.fitcoach.app.support

import com.fitcoach.app.collaudo.MarjanProfile
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifica che il generatore di proposte usato dai test produca strutture che il
 * motore di validazione REALE accetta. Se questo test fallisce, i test di
 * integrazione/collaudo non stanno esercitando un percorso valido.
 */
class SimulatedAiProgramsTest {

    private val threeDay = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.BEGINNER, goal = Goal.RECOMP,
        trainingDaysPerWeek = 3, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    private fun validate(profile: UserProfile) = run {
        val foods = FoodFilter.allowedFoods(FoodSeedData.foods, profile)
        val exercises = ExerciseSeedData.exercises
        val training = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
        val proposal = SimulatedAiPrograms.proposal(profile, foods, exercises, training)
        val validated = ProgramGenerationEngine.validate(profile, proposal, foods, exercises)
        if (validated == null) {
            error("Proposta non valida: " + ProgramGenerationEngine.diagnose(profile, proposal, foods, exercises))
        }
        validated
    }

    @Test
    fun `la proposta per un profilo di tre giorni supera il validatore reale`() {
        val validated = validate(threeDay)
        assertThat(validated.nutrition).isNotNull()
        assertThat(validated.nutrition!!.days).hasSize(7)
        assertThat(validated.workout!!.days.count { !it.isRestDay }).isEqualTo(3)
    }

    @Test
    fun `la proposta per il profilo Marjan supera il validatore reale`() {
        val validated = validate(MarjanProfile.build())
        assertThat(validated.nutrition).isNotNull()
        assertThat(validated.workout!!.days.count { !it.isRestDay }).isEqualTo(4)
    }
}
