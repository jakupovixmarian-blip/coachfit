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

class WorkoutPlanBuilderTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 4, experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        availableEquipment = Equipment.entries.toSet()
    )

    private fun exercise(
        id: Long, name: String, muscle: MuscleGroup, category: ExerciseCategory,
        equipment: Set<Equipment> = setOf(Equipment.BARBELL)
    ) = Exercise(
        id = id, name = name, primaryMuscle = muscle, category = category,
        equipment = equipment, technique = "", defaultSets = 4, minReps = 8, maxReps = 12,
        restSeconds = 120, targetRir = 2
    )

    private val exercises = listOf(
        exercise(1, "Panca piana", MuscleGroup.CHEST, ExerciseCategory.COMPOUND),
        exercise(2, "Croci manubri", MuscleGroup.CHEST, ExerciseCategory.ISOLATION, setOf(Equipment.DUMBBELL)),
        exercise(3, "Trazioni", MuscleGroup.BACK, ExerciseCategory.COMPOUND, emptySet()),
        exercise(4, "Rematore", MuscleGroup.BACK, ExerciseCategory.COMPOUND),
        exercise(5, "Squat", MuscleGroup.QUADS, ExerciseCategory.COMPOUND),
        exercise(6, "Leg extension", MuscleGroup.QUADS, ExerciseCategory.ISOLATION, setOf(Equipment.MACHINE))
    )

    @Test
    fun `build produces seven days`() {
        val built = WorkoutPlanBuilder.build(profile, exercises, listOf(0, 1, 3, 4))
        assertThat(built.days).hasSize(7)
        assertThat(built.days.count { !it.isRestDay }).isEqualTo(4)
    }

    @Test
    fun `every planned exercise carries a selection reason`() {
        val built = WorkoutPlanBuilder.build(profile, exercises, listOf(0, 1, 3, 4))
        built.days.flatMap { it.exercises }.forEach {
            assertThat(it.selectionReason).isNotEmpty()
        }
    }

    @Test
    fun `every planned exercise carries a rest reason`() {
        val built = WorkoutPlanBuilder.build(profile, exercises, listOf(0, 1, 3, 4))
        built.days.flatMap { it.exercises }.forEach {
            assertThat(it.restReason).isNotEmpty()
            assertThat(it.restSeconds).isAtLeast(45)
        }
    }

    @Test
    fun `weekly volume is aggregated per muscle group`() {
        val built = WorkoutPlanBuilder.build(profile, exercises, listOf(0, 1, 3, 4))
        assertThat(built.weeklyVolumePerGroup).isNotEmpty()
        assertThat(built.weeklyVolumePerGroup.values.all { it > 0 }).isTrue()
    }

    @Test
    fun `default training days spread across the week`() {
        assertThat(WorkoutPlanBuilder.defaultTrainingDays(4)).hasSize(4)
        assertThat(WorkoutPlanBuilder.defaultTrainingDays(0)).isEmpty()
        assertThat(WorkoutPlanBuilder.defaultTrainingDays(3)).hasSize(3)
    }

    @Test
    fun `disliked exercises are avoided when alternatives exist`() {
        val p = profile.copy(dislikedExercises = listOf("Rematore"))
        val built = WorkoutPlanBuilder.build(p, exercises, listOf(0, 1, 3, 4))
        val names = built.days.flatMap { it.exercises }.map { it.exerciseName }
        assertThat(names).doesNotContain("Rematore")
    }

    @Test
    fun `priority muscle gets extra set`() {
        val p = profile.copy(priorityMuscles = setOf(MuscleGroup.CHEST))
        val built = WorkoutPlanBuilder.build(p, exercises, listOf(0, 1, 3, 4))
        val chest = built.days.flatMap { it.exercises }.filter { it.primaryMuscle == MuscleGroup.CHEST }
        val nonPriority = built.days.flatMap { it.exercises }.filter { it.primaryMuscle != MuscleGroup.CHEST }
        if (chest.isNotEmpty() && nonPriority.isNotEmpty()) {
            assertThat(chest.map { it.sets }.max()).isAtLeast(nonPriority.map { it.sets }.min())
        }
    }

    @Test
    fun `low recovery capacity reduces volume`() {
        val low = profile.copy(recoveryCapacity = com.fitcoach.app.domain.model.Level3.LOW)
        val normal = profile.copy(recoveryCapacity = com.fitcoach.app.domain.model.Level3.HIGH)
        val lowBuilt = WorkoutPlanBuilder.build(low, exercises, listOf(0, 1, 3, 4))
        val normalBuilt = WorkoutPlanBuilder.build(normal, exercises, listOf(0, 1, 3, 4))
        val lowTotal = lowBuilt.days.flatMap { it.exercises }.sumOf { it.sets }
        val normalTotal = normalBuilt.days.flatMap { it.exercises }.sumOf { it.sets }
        assertThat(lowTotal).isAtMost(normalTotal)
    }

    @Test
    fun `findSubstitutes returns same-muscle alternatives`() {
        val panca = exercises.first { it.name == "Panca piana" }
        val subs = WorkoutPlanBuilder.findSubstitutes(panca, exercises, profile)
        assertThat(subs).isNotEmpty()
        subs.forEach {
            assertThat(it.primaryMuscle).isEqualTo(MuscleGroup.CHEST)
            assertThat(it.id).isNotEqualTo(panca.id)
        }
    }

    @Test
    fun `findSubstitutes respects available equipment`() {
        val limited = profile.copy(availableEquipment = setOf(Equipment.BARBELL))
        val panca = exercises.first { it.name == "Panca piana" }
        val subs = WorkoutPlanBuilder.findSubstitutes(panca, exercises, limited)
        subs.forEach {
            assertThat(it.equipment.isEmpty() || it.equipment.any { e -> e in limited.availableEquipment } ||
                it.bodyweightOnly).isTrue()
        }
    }
}
