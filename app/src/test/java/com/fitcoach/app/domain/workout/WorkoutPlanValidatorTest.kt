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

class WorkoutPlanValidatorTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 3, experience = TrainingExperience.INTERMEDIATE,
        goal = Goal.HYPERTROPHY, availableEquipment = Equipment.entries.toSet()
    )

    private fun exercise(
        id: Long, name: String, muscle: MuscleGroup,
        secondary: Set<MuscleGroup> = emptySet(),
        equipment: Set<Equipment> = setOf(Equipment.BARBELL),
        difficulty: Int = 1
    ) = Exercise(
        id = id, name = name, primaryMuscle = muscle, secondaryMuscles = secondary,
        category = ExerciseCategory.COMPOUND, equipment = equipment, technique = "",
        defaultSets = 4, minReps = 8, maxReps = 12, restSeconds = 120, targetRir = 2,
        difficulty = difficulty
    )

    @Test
    fun `valid plan with full coverage has no violations`() {
        val catalog = listOf(
            exercise(1, "Panca", MuscleGroup.CHEST, setOf(MuscleGroup.TRICEPS)),
            exercise(2, "Squat", MuscleGroup.QUADS, setOf(MuscleGroup.GLUTES)),
            exercise(3, "Stacco", MuscleGroup.HAMSTRINGS, setOf(MuscleGroup.BACK))
        )
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS),
                listOf(
                    WorkoutPlanValidator.ExerciseProposal(1, 4),
                    WorkoutPlanValidator.ExerciseProposal(2, 4),
                    WorkoutPlanValidator.ExerciseProposal(3, 4)
                )
            )
        )
        // Il profilo ha 3 giorni: allineiamo per isolare la copertura.
        val p = profile.copy(trainingDaysPerWeek = 1)
        val report = WorkoutPlanValidator.validate(p, catalog, days)
        assertThat(report.violations.filter { it.contains("gruppi previsti") }).isEmpty()
    }

    @Test
    fun `planned group without exercises is a violation`() {
        val catalog = listOf(exercise(1, "Panca", MuscleGroup.CHEST))
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CHEST, MuscleGroup.CALVES),
                listOf(WorkoutPlanValidator.ExerciseProposal(1, 4))
            )
        )
        val report = WorkoutPlanValidator.validate(profile.copy(trainingDaysPerWeek = 1), catalog, days)
        assertThat(report.violations.any { it.contains("CALVES") && it.contains("senza esercizi") }).isTrue()
    }

    @Test
    fun `unknown exercise id is a violation`() {
        val catalog = listOf(exercise(1, "Panca", MuscleGroup.CHEST))
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CHEST),
                listOf(WorkoutPlanValidator.ExerciseProposal(999, 4))
            )
        )
        val report = WorkoutPlanValidator.validate(profile.copy(trainingDaysPerWeek = 1), catalog, days)
        assertThat(report.violations.any { it.contains("inesistente") }).isTrue()
    }

    @Test
    fun `exercise incompatible with equipment is a violation`() {
        val catalog = listOf(exercise(1, "Panca", MuscleGroup.CHEST, equipment = setOf(Equipment.BARBELL)))
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CHEST),
                listOf(WorkoutPlanValidator.ExerciseProposal(1, 4))
            )
        )
        val p = profile.copy(trainingDaysPerWeek = 1, availableEquipment = setOf(Equipment.BODYWEIGHT))
        val report = WorkoutPlanValidator.validate(p, catalog, days)
        assertThat(report.violations.any { it.contains("non compatibile") }).isTrue()
    }

    @Test
    fun `avoided movement is incompatible`() {
        val catalog = listOf(exercise(1, "Squat con bilanciere", MuscleGroup.QUADS))
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.QUADS),
                listOf(WorkoutPlanValidator.ExerciseProposal(1, 4))
            )
        )
        val p = profile.copy(trainingDaysPerWeek = 1, movementsToAvoid = listOf("squat"))
        val report = WorkoutPlanValidator.validate(p, catalog, days)
        assertThat(report.violations.any { it.contains("non compatibile") }).isTrue()
    }

    @Test
    fun `low weekly volume for a covered group is a violation`() {
        val catalog = listOf(exercise(1, "Panca", MuscleGroup.CHEST))
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CHEST),
                listOf(WorkoutPlanValidator.ExerciseProposal(1, 2))
            )
        )
        val report = WorkoutPlanValidator.validate(profile.copy(trainingDaysPerWeek = 1), catalog, days)
        assertThat(report.violations.any { it.contains("minimo 8") }).isTrue()
    }

    @Test
    fun `too many sets per session is a violation`() {
        val catalog = listOf(exercise(1, "Panca", MuscleGroup.CHEST))
        val many = (0 until 10).map { WorkoutPlanValidator.ExerciseProposal(1, 6) }
        val days = listOf(WorkoutPlanValidator.DayProposal(0, setOf(MuscleGroup.CHEST), many))
        val report = WorkoutPlanValidator.validate(profile.copy(trainingDaysPerWeek = 1), catalog, days)
        assertThat(report.violations.any { it.contains("troppe per una seduta") }).isTrue()
    }

    @Test
    fun `wrong number of training days is a violation`() {
        val catalog = listOf(exercise(1, "Panca", MuscleGroup.CHEST))
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CHEST),
                listOf(WorkoutPlanValidator.ExerciseProposal(1, 4))
            )
        )
        val report = WorkoutPlanValidator.validate(profile.copy(trainingDaysPerWeek = 3), catalog, days)
        assertThat(report.violations.any { it.contains("giornate allenamento") }).isTrue()
    }
}
