package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * La diagnostica deve indicare il controllo fallito con valore atteso e valore
 * realmente restituito, invece del generico "vincoli non rispettati".
 */
class AiProposalDiagnosticsTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 3, availableEquipment = Equipment.entries.toSet()
    )

    private val validExerciseName = ExerciseSeedData.exercises.first().name

    private fun proposalWithDays(days: List<Int>): AiProgramProposal = AiProgramProposal(
        workout = AiWorkoutProposal(
            split = "FULL_BODY",
            days = days.map { index ->
                AiWorkoutDay(
                    dayIndex = index,
                    exercises = listOf(
                        AiPlannedExercise(
                            exerciseName = validExerciseName,
                            sets = 3, minReps = 8, maxReps = 12,
                            restSeconds = 120, targetRir = 2
                        )
                    )
                )
            }
        ),
        calendar = AiCalendarProposal(
            workoutDayIndices = days,
            restDayIndices = (0..6).filterNot { it in days }
        )
    )

    @Test
    fun `reports exact training day mismatch with expected and received`() {
        val errors = AiProposalDiagnostics.analyze(
            profile, proposalWithDays(listOf(0, 2)), FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("giorni allenamento") && it.contains("attesi 3") }).isTrue()
        assertThat(errors.any { it.contains("ricevuti 2") }).isTrue()
    }

    @Test
    fun `reports unknown exercise instead of a generic message`() {
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                days = (0..2).map { index ->
                    AiWorkoutDay(
                        dayIndex = index,
                        exercises = listOf(
                            AiPlannedExercise(
                                exerciseName = "Esercizio inesistente",
                                sets = 3, minReps = 8, maxReps = 12
                            )
                        )
                    )
                }
            ),
            calendar = AiCalendarProposal(listOf(0, 1, 2), listOf(3, 4, 5, 6))
        )
        val errors = AiProposalDiagnostics.analyze(
            profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("non presente nel database") }).isTrue()
    }

    @Test
    fun `reports empty session and excessive session volume`() {
        val overloaded = AiWorkoutDay(
            dayIndex = 1,
            exercises = List(4) {
                AiPlannedExercise(exerciseName = validExerciseName, sets = 8, minReps = 8, maxReps = 12)
            }
        )
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                days = listOf(
                    AiWorkoutDay(dayIndex = 0, exercises = emptyList()),
                    overloaded,
                    AiWorkoutDay(
                        dayIndex = 2,
                        exercises = listOf(
                            AiPlannedExercise(exerciseName = validExerciseName, sets = 3, minReps = 8, maxReps = 12)
                        )
                    )
                )
            ),
            calendar = AiCalendarProposal(listOf(0, 1, 2), listOf(3, 4, 5, 6))
        )
        val errors = AiProposalDiagnostics.analyze(
            profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("nessun esercizio") }).isTrue()
        assertThat(errors.any { it.contains("massimo sicuro 24") }).isTrue()
    }

    @Test
    fun `reports exercise incompatible with equipment`() {
        val bodyweightOnly = profile.copy(availableEquipment = setOf(Equipment.BODYWEIGHT))
        val errors = AiProposalDiagnostics.analyze(
            bodyweightOnly, proposalWithDays(listOf(0, 1, 2)), FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("incompatibile") }).isTrue()
    }

    @Test
    fun `diagnostics are never empty`() {
        val errors = AiProposalDiagnostics.analyze(
            profile, proposalWithDays(listOf(0, 2)), FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors).isNotEmpty()
    }
}
