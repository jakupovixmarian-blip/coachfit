package dev.fitcoach.app

import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.usecase.ExerciseWithSets
import dev.fitcoach.app.domain.usecase.PlannedExercise
import dev.fitcoach.app.domain.usecase.ProgramDay
import dev.fitcoach.app.domain.usecase.ProgramIssueType
import dev.fitcoach.app.domain.usecase.ProgramValidator
import dev.fitcoach.app.domain.usecase.VolumeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The validator must report duplicates, pattern overuse, imbalance and overload. */
class ProgramValidatorTest {

    private fun exercise(
        id: Long,
        name: String,
        primary: MuscleGroup,
        pattern: MovementPattern = MovementPattern.ISOLATION,
        secondary: List<MuscleGroup> = emptyList()
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        pattern = pattern
    )

    private fun day(index: Int, vararg planned: PlannedExercise) =
        ProgramDay(dayIndex = index, label = "Giorno $index", focus = emptyList(), exercises = planned.toList())

    private fun planned(
        id: Long,
        name: String,
        primary: MuscleGroup,
        sets: Int = 3,
        pattern: MovementPattern = MovementPattern.ISOLATION,
        secondary: List<MuscleGroup> = emptyList()
    ) = PlannedExercise(
        exercise = exercise(id, name, primary, pattern, secondary),
        sets = sets,
        repsMin = 8,
        repsMax = 12,
        rir = 2,
        restSeconds = 90
    )

    @Test
    fun `identical duplicate inside the same session is detected`() {
        val days = listOf(
            day(
                0,
                planned(1L, "Pulley basso", MuscleGroup.BACK),
                planned(1L, "Pulley basso", MuscleGroup.BACK)
            )
        )
        val validation = ProgramValidator.validate(days)
        assertTrue(validation.issues.any { it.type == ProgramIssueType.DUPLICATE_IN_SESSION })
        assertTrue(validation.blockingIssues.isNotEmpty())
    }

    @Test
    fun `a repeated exercise across different days is detected`() {
        val days = listOf(
            day(0, planned(1L, "Panca piana", MuscleGroup.CHEST, pattern = MovementPattern.HORIZONTAL_PUSH)),
            day(1, planned(1L, "Panca piana", MuscleGroup.CHEST, pattern = MovementPattern.HORIZONTAL_PUSH))
        )
        val validation = ProgramValidator.validate(days)
        assertTrue(validation.issues.any { it.type == ProgramIssueType.DUPLICATE_ACROSS_SESSIONS })
    }

    @Test
    fun `distinct exercises on different days raise no duplicate`() {
        val days = listOf(
            day(0, planned(1L, "Panca piana", MuscleGroup.CHEST, pattern = MovementPattern.HORIZONTAL_PUSH)),
            day(1, planned(2L, "Trazioni", MuscleGroup.BACK, pattern = MovementPattern.VERTICAL_PULL))
        )
        val validation = ProgramValidator.validate(days)
        assertFalse(validation.issues.any { it.type == ProgramIssueType.DUPLICATE_ACROSS_SESSIONS })
    }

    @Test
    fun `too many exercises with the same movement pattern are flagged`() {
        val days = listOf(
            day(
                0,
                planned(1L, "Spinta 1", MuscleGroup.CHEST, pattern = MovementPattern.HORIZONTAL_PUSH),
                planned(2L, "Spinta 2", MuscleGroup.SHOULDERS, pattern = MovementPattern.HORIZONTAL_PUSH),
                planned(3L, "Spinta 3", MuscleGroup.TRICEPS, pattern = MovementPattern.HORIZONTAL_PUSH)
            )
        )
        val validation = ProgramValidator.validate(days)
        assertTrue(validation.issues.any { it.type == ProgramIssueType.PATTERN_OVERUSE })
    }

    @Test
    fun `core exercises may legitimately repeat without being flagged`() {
        val days = listOf(
            day(
                0,
                planned(1L, "Plank", MuscleGroup.CORE, pattern = MovementPattern.CORE),
                planned(2L, "Crunch inverso", MuscleGroup.CORE, pattern = MovementPattern.CORE),
                planned(3L, "Ab wheel", MuscleGroup.CORE, pattern = MovementPattern.CORE)
            )
        )
        val validation = ProgramValidator.validate(days)
        assertFalse(validation.issues.any { it.type == ProgramIssueType.PATTERN_OVERUSE })
    }

    @Test
    fun `a chest and back imbalance is detected`() {
        val days = listOf(
            day(
                0,
                planned(1L, "Panca piana", MuscleGroup.CHEST, sets = 4, pattern = MovementPattern.HORIZONTAL_PUSH),
                planned(2L, "Croci", MuscleGroup.CHEST, sets = 4, pattern = MovementPattern.ISOLATION),
                planned(3L, "Rematore", MuscleGroup.BACK, sets = 2, pattern = MovementPattern.HORIZONTAL_PULL)
            )
        )
        val validation = ProgramValidator.validate(days)
        assertTrue(validation.issues.any { it.type == ProgramIssueType.MUSCLE_IMBALANCE })
    }

    @Test
    fun `a balanced program raises no imbalance issue`() {
        val days = listOf(
            day(
                0,
                planned(1L, "Panca piana", MuscleGroup.CHEST, sets = 4, pattern = MovementPattern.HORIZONTAL_PUSH),
                planned(2L, "Rematore", MuscleGroup.BACK, sets = 4, pattern = MovementPattern.HORIZONTAL_PULL)
            )
        )
        val validation = ProgramValidator.validate(days)
        assertFalse(validation.issues.any { it.type == ProgramIssueType.MUSCLE_IMBALANCE })
    }

    @Test
    fun `an overloaded session is flagged as blocking`() {
        val exercises = (1..12).map { index ->
            planned(index.toLong(), "Esercizio $index", MuscleGroup.CHEST, sets = 3)
        }
        val days = listOf(ProgramDay(0, "Giorno A", emptyList(), exercises))
        val validation = ProgramValidator.validate(days)
        assertTrue(validation.issues.any { it.type == ProgramIssueType.SESSION_OVERLOAD })
        assertTrue(validation.blockingIssues.isNotEmpty())
    }

    @Test
    fun `validator never removes exercises from the input`() {
        val days = listOf(
            day(
                0,
                planned(1L, "Pulley basso", MuscleGroup.BACK),
                planned(1L, "Pulley basso", MuscleGroup.BACK)
            )
        )
        ProgramValidator.validate(days)
        assertEquals(2, days[0].exercises.size)
    }
}
