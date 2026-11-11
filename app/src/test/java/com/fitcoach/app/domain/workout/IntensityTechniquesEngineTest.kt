package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntensityTechniquesEngineTest {

    private val isolation = Exercise(
        name = "Curl manubri", primaryMuscle = MuscleGroup.BICEPS,
        category = ExerciseCategory.ISOLATION, technique = "",
        defaultSets = 3, minReps = 8, maxReps = 12, restSeconds = 90, targetRir = 2,
        equipment = setOf(Equipment.DUMBBELL)
    )

    private val compound = Exercise(
        name = "Panca piana", primaryMuscle = MuscleGroup.CHEST,
        category = ExerciseCategory.COMPOUND, technique = "",
        defaultSets = 4, minReps = 6, maxReps = 10, restSeconds = 180, targetRir = 2,
        equipment = setOf(Equipment.BARBELL)
    )

    private val advanced = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 85.0,
        trainingDaysPerWeek = 5, experience = TrainingExperience.ADVANCED, goal = Goal.HYPERTROPHY
    )

    @Test
    fun `beginners get no intensity technique`() {
        val beginner = advanced.copy(experience = TrainingExperience.BEGINNER)
        val decision = IntensityTechniquesEngine.decide(
            isolation, beginner, isLastExerciseOfSession = true, weeklySetsForGroup = 12
        )
        assertThat(decision.technique).isEqualTo(IntensityTechnique.NONE)
        assertThat(decision.reason).contains("principiante")
    }

    @Test
    fun `high fatigue disables techniques`() {
        val decision = IntensityTechniquesEngine.decide(
            isolation, advanced, isLastExerciseOfSession = true,
            weeklySetsForGroup = 12, fatigueSeverity = 3
        )
        assertThat(decision.technique).isEqualTo(IntensityTechnique.NONE)
    }

    @Test
    fun `poor sleep and high stress disable techniques`() {
        val tired = advanced.copy(sleepQuality = Level3.LOW, stressLevel = Level3.HIGH)
        val decision = IntensityTechniquesEngine.decide(
            isolation, tired, isLastExerciseOfSession = true, weeklySetsForGroup = 12
        )
        assertThat(decision.technique).isEqualTo(IntensityTechnique.NONE)
    }

    @Test
    fun `high volume disables techniques`() {
        val decision = IntensityTechniquesEngine.decide(
            isolation, advanced, isLastExerciseOfSession = true, weeklySetsForGroup = 22
        )
        assertThat(decision.technique).isEqualTo(IntensityTechnique.NONE)
    }

    @Test
    fun `first weeks of mesocycle disable techniques`() {
        val decision = IntensityTechniquesEngine.decide(
            isolation, advanced, isLastExerciseOfSession = true,
            weeklySetsForGroup = 12, isFirstMesocycleWeeks = true
        )
        assertThat(decision.technique).isEqualTo(IntensityTechnique.NONE)
    }

    @Test
    fun `advanced isolation last exercise gets a technique`() {
        val decision = IntensityTechniquesEngine.decide(
            isolation, advanced, isLastExerciseOfSession = true,
            weeklySetsForGroup = 12, isFirstMesocycleWeeks = false
        )
        assertThat(decision.technique).isAnyOf(IntensityTechnique.DROP_SET, IntensityTechnique.MYO_REPS)
        assertThat(decision.reason).isNotEmpty()
    }

    @Test
    fun `every decision explains why advanced techniques are avoided`() {
        val decision = IntensityTechniquesEngine.decide(
            compound, advanced, isLastExerciseOfSession = false,
            weeklySetsForGroup = 12, isFirstMesocycleWeeks = false
        )
        assertThat(decision.avoidedReason).isNotEmpty()
    }

    @Test
    fun `describe returns a non-empty explanation for every technique`() {
        IntensityTechnique.entries.forEach {
            assertThat(IntensityTechniquesEngine.describe(it)).isNotEmpty()
        }
    }
}
