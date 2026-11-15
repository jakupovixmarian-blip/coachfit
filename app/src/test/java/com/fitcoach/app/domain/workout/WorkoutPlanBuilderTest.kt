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

/**
 * Verifica gli helper rimasti di [WorkoutPlanBuilder]: la costruzione delle
 * schede non e piu responsabilita di questo oggetto (la decide il Coach AI), ma
 * il calendario dei giorni e la sostituzione degli esercizi restano contratti
 * dell'app usati dall'interfaccia.
 */
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
    fun `default training days spread across the week`() {
        assertThat(WorkoutPlanBuilder.defaultTrainingDays(4)).hasSize(4)
        assertThat(WorkoutPlanBuilder.defaultTrainingDays(0)).isEmpty()
        assertThat(WorkoutPlanBuilder.defaultTrainingDays(3)).hasSize(3)
        assertThat(WorkoutPlanBuilder.defaultTrainingDays(4)).containsExactly(0, 1, 3, 4)
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

    @Test
    fun `categoryLabel describes each category`() {
        assertThat(WorkoutPlanBuilder.categoryLabel(ExerciseCategory.COMPOUND)).isEqualTo("Multiarticolare")
        assertThat(WorkoutPlanBuilder.categoryLabel(ExerciseCategory.ISOLATION)).isEqualTo("Isolamento")
        assertThat(WorkoutPlanBuilder.categoryLabel(ExerciseCategory.BODYWEIGHT)).isEqualTo("Corpo libero")
    }
}
