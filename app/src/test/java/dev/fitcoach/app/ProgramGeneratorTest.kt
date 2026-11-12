package dev.fitcoach.app

import dev.fitcoach.app.data.seed.ExerciseSeed
import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.usecase.ExerciseWithSets
import dev.fitcoach.app.domain.usecase.GeneratedProgram
import dev.fitcoach.app.domain.usecase.ProgramGenerator
import dev.fitcoach.app.domain.usecase.ProgramIssueType
import dev.fitcoach.app.domain.usecase.ProgramValidator
import dev.fitcoach.app.domain.usecase.VolumeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator must produce one distinct session per declared training day, keep
 * the weekly volume inside the budget for the level and never repeat an exercise
 * by accident.
 */
class ProgramGeneratorTest {

    /** The database assigns an id to every exercise; the generator relies on it. */
    private val catalog: List<Exercise> = ExerciseSeed.all()
        .mapIndexed { index, exercise -> exercise.copy(id = (index + 1).toLong()) }

    private fun profile(
        days: Int,
        level: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
        goal: Goal = Goal.MUSCLE_GAIN,
        equipment: String = "BARBELL,DUMBBELL,MACHINE,CABLE,BODYWEIGHT"
    ) = UserProfile(
        name = "Test",
        age = 30,
        heightCm = 178.0,
        weightKg = 78.0,
        goal = goal,
        experience = level,
        equipment = equipment,
        availableDays = days,
        sessionMinutes = 60
    )

    @Test
    fun `three days produce exactly three distinct sessions`() {
        val program = ProgramGenerator.generate(profile(3), catalog)

        assertEquals(3, program.days.size)
        assertEquals(listOf(0, 1, 2), program.days.map { it.dayIndex })
        val labels = program.days.map { it.label }
        assertEquals(labels.size, labels.distinct().size)
    }

    @Test
    fun `four days produce four sessions and never one full body repeated`() {
        val program = ProgramGenerator.generate(profile(4), catalog)

        assertEquals(4, program.days.size)
        // No session may be a generic full body when the split is A/B/C/D.
        assertTrue(program.days.none { it.focus.contains(MuscleGroup.FULL_BODY) })
        // Every session must have its own exercise list.
        val signatures = program.days.map { day -> day.exercises.map { it.exercise.id }.sorted() }
        assertEquals(signatures.size, signatures.distinct().size)
    }

    @Test
    fun `five days produce five distinct sessions`() {
        val program = ProgramGenerator.generate(profile(5), catalog)

        assertEquals(5, program.days.size)
        val signatures = program.days.map { day -> day.exercises.map { it.exercise.id }.sorted() }
        assertEquals(signatures.size, signatures.distinct().size)
    }

    @Test
    fun `two days are split upper and lower`() {
        val program = ProgramGenerator.generate(profile(2), catalog)

        assertEquals(2, program.days.size)
        val first = program.days[0].focus
        val second = program.days[1].focus
        assertTrue(first.any { it in setOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS) })
        assertTrue(second.any { it in setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES) })
    }

    @Test
    fun `no exercise is repeated across sessions unless the pattern is intentional`() {
        val program = ProgramGenerator.generate(profile(4), catalog)

        val all = program.days.flatMap { day -> day.exercises.map { it.exercise.id } }
        assertEquals(all.size, all.distinct().size)
    }

    @Test
    fun `no duplicate inside a single session`() {
        (2..6).forEach { days ->
            val program = ProgramGenerator.generate(profile(days), catalog)
            program.days.forEach { day ->
                val ids = day.exercises.map { it.exercise.id }
                assertEquals("day ${day.label}", ids.size, ids.distinct().size)
            }
        }
    }

    @Test
    fun `weekly volume per muscle stays inside the level ceiling`() {
        listOf(ExperienceLevel.BEGINNER, ExperienceLevel.INTERMEDIATE, ExperienceLevel.ADVANCED).forEach { level ->
            (2..5).forEach { days ->
                val program = ProgramGenerator.generate(profile(days, level = level), catalog)
                val sessions = program.days.map { day ->
                    day.exercises.map { ExerciseWithSets(it.exercise, it.sets) }
                }
                VolumeCalculator.weeklyDirectSets(sessions).forEach { (muscle, sets) ->
                    assertTrue(
                        "$level $days days: $muscle has $sets weekly sets",
                        sets <= VolumeCalculator.weeklyDirectCeiling(level)
                    )
                }
            }
        }
    }

    @Test
    fun `no session is overloaded beyond the hard cap`() {
        (2..6).forEach { days ->
            val program = ProgramGenerator.generate(profile(days), catalog)
            program.days.forEach { day ->
                val sets = day.exercises.sumOf { it.sets }
                assertTrue("${day.label} has $sets sets", sets <= ProgramValidator.MAX_SETS_PER_SESSION)
            }
        }
    }

    @Test
    fun `a beginner gets fewer weekly sets than an advanced user`() {
        val beginner = ProgramGenerator.generate(
            profile(4, level = ExperienceLevel.BEGINNER), catalog
        )
        val advanced = ProgramGenerator.generate(
            profile(4, level = ExperienceLevel.ADVANCED), catalog
        )
        assertTrue(advanced.totalSets > beginner.totalSets)
    }

    @Test
    fun `the session count never drops below the declared days when equipment allows`() {
        (3..5).forEach { days ->
            val program = ProgramGenerator.generate(profile(days), catalog)
            assertEquals(days, program.days.size)
        }
    }

    @Test
    fun `limited equipment still produces the requested number of days`() {
        val program = ProgramGenerator.generate(
            profile(3, equipment = "BODYWEIGHT"), catalog
        )
        assertEquals(3, program.days.size)
        assertTrue(program.days.all { day -> day.exercises.isNotEmpty() })
    }

    @Test
    fun `injuries remove the contraindicated exercises`() {
        val base = profile(4)
        val injured = base.copy(injuries = "spalla")
        val program = ProgramGenerator.generate(injured, catalog)

        program.days.flatMap { it.exercises }.forEach { planned ->
            assertFalse(
                planned.exercise.primaryMuscle == MuscleGroup.SHOULDERS ||
                    planned.exercise.secondaryMuscles.contains(MuscleGroup.SHOULDERS)
            )
        }
    }

    @Test
    fun `generated program passes the validator without blocking issues`() {
        (2..6).forEach { days ->
            val program = ProgramGenerator.generate(profile(days), catalog)
            val validation = ProgramValidator.validate(program.days)
            assertTrue(
                "days=$days issues=${validation.blockingIssues.map { it.message }}",
                validation.blockingIssues.isEmpty()
            )
        }
    }

    @Test
    fun `each day has its own sets reps and rest`() {
        val program = ProgramGenerator.generate(profile(4), catalog)
        program.days.forEach { day ->
            day.exercises.forEach { planned ->
                assertTrue(planned.sets >= 2)
                assertTrue(planned.repsMin >= 1)
                assertTrue(planned.repsMax >= planned.repsMin)
                assertTrue(planned.restSeconds > 0)
            }
        }
    }

    @Test
    fun `toRoutines keeps one routine per day with a distinct program id`() {
        val program = ProgramGenerator.generate(profile(4), catalog)
        val (routines, exercises) = ProgramGenerator.toRoutines(1L, "Programma", program, createdAt = 5L)

        assertEquals(4, routines.size)
        assertEquals(setOf(1L), routines.map { it.programId }.toSet())
        assertEquals(listOf(0, 1, 2, 3), routines.map { it.dayIndex })
        assertEquals(program.exerciseCount, exercises.size)
        assertTrue(routines.all { it.dayLabel.isNotBlank() })
    }

    @Test
    fun `empty catalog is reported instead of producing an empty plan`() {
        val program: GeneratedProgram = ProgramGenerator.generate(profile(4), emptyList())
        assertTrue(program.days.isEmpty())
    }

    @Test
    fun `equipment filter is applied to the catalog`() {
        val program = ProgramGenerator.generate(
            profile(3, equipment = "DUMBBELL"), catalog
        )
        program.days.flatMap { it.exercises }.forEach { planned ->
            assertEquals(Equipment.DUMBBELL, planned.exercise.equipment)
        }
    }
}
