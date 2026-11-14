package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutScenarioRegressionTest {
    private fun profile(days: Int, experience: TrainingExperience = TrainingExperience.INTERMEDIATE) = UserProfile(
        ageYears = 30,
        sex = Sex.MALE,
        heightCm = 180.0,
        weightKg = 80.0,
        trainingDaysPerWeek = days,
        experience = experience,
        goal = Goal.HYPERTROPHY,
        availableEquipment = Equipment.entries.toSet(),
        maxSessionMinutes = 90
    )

    @Test
    fun `two through five day profiles produce exact bounded sessions`() {
        listOf(2, 3, 4, 5).forEach { days ->
            val plan = WorkoutPlanBuilder.build(
                profile(days), ExerciseSeedData.exercises,
                WorkoutPlanBuilder.defaultTrainingDays(days)
            )
            val sessions = plan.days.filterNot { it.isRestDay }
            assertThat(sessions).hasSize(days)
            assertThat(sessions.all { it.exercises.isNotEmpty() }).isTrue()
            assertThat(sessions.all { it.totalSets in 6..24 }).isTrue()
            assertThat(plan.weeklyVolumePerGroup.values.all { it > 0 }).isTrue()
        }
    }

    @Test
    fun `three full body days rotate exercise selection`() {
        val plan = WorkoutPlanBuilder.build(
            profile(3), ExerciseSeedData.exercises,
            WorkoutPlanBuilder.defaultTrainingDays(3)
        )
        val names = plan.days.filterNot { it.isRestDay }
            .map { it.exercises.map { exercise -> exercise.exerciseName }.toSet() }
        assertThat(names.distinct()).hasSize(3)
    }

    @Test
    fun `limited equipment and avoided movements are never scheduled`() {
        val limited = profile(3).copy(
            availableEquipment = setOf(Equipment.BODYWEIGHT),
            movementsToAvoid = listOf("squat")
        )
        val plan = WorkoutPlanBuilder.build(
            limited, ExerciseSeedData.exercises,
            WorkoutPlanBuilder.defaultTrainingDays(3)
        )
        plan.days.flatMap { it.exercises }.forEach { planned ->
            val exercise = ExerciseSeedData.exercises.first { it.id == planned.exerciseId }
            val compatible = exercise.bodyweightOnly || exercise.equipment.isEmpty() ||
                exercise.equipment.any { it in limited.availableEquipment }
            if (!compatible) error("incompatibile: ${exercise.name} equipment=${exercise.equipment}")
            assertThat(exercise.name.lowercase()).doesNotContain("squat")
        }
    }

    @Test
    fun `secondary muscles receive weighted indirect volume`() {
        val exercise = Exercise(
            id = 100,
            name = "Press test",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            category = ExerciseCategory.COMPOUND,
            defaultSets = 4,
            minReps = 8,
            maxReps = 12,
            restSeconds = 120,
            targetRir = 2,
            technique = ""
        )
        val planned = WorkoutPlanBuilder.build(
            profile(2), listOf(exercise), listOf(0, 3)
        )
        val chest = planned.weeklyVolumePerGroup[MuscleGroup.CHEST] ?: 0
        val triceps = planned.weeklyVolumePerGroup[MuscleGroup.TRICEPS] ?: 0
        assertThat(chest).isGreaterThan(triceps)
        assertThat(triceps).isLessThan(chest)
    }
}
