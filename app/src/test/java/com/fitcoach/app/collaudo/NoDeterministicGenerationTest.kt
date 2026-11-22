package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.coach.AiProgramProposal
import com.fitcoach.app.domain.coach.AiWorkoutProposal
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * La generazione dei programmi spetta all'AI: i motori matematici calcolano e
 * verificano, non inventano. Questi test bloccano il ritorno di un generatore
 * deterministico, che produrrebbe piani verosimili ma non decisi dal coach.
 */
class NoDeterministicGenerationTest {

    private val profile = UserProfile(
        name = "Test",
        sex = Sex.MALE,
        ageYears = 30,
        heightCm = 180.0,
        weightKg = 80.0,
        experience = TrainingExperience.INTERMEDIATE,
        goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4
    )

    @Test
    fun `una proposta senza giorni non produce un piano nutrizionale`() {
        val proposal = AiProgramProposal(
            nutrition = AiNutritionProposal(days = emptyList()),
            workout = AiWorkoutProposal(days = emptyList())
        )

        val result = ProgramGenerationEngine.validate(
            profile = profile,
            proposal = proposal,
            foods = emptyList(),
            exercises = emptyList()
        )

        // Nessun ripiego: senza giorni decisi dall'AI non esiste un piano.
        assertThat(result).isNull()
    }
}
