package dev.fitcoach.app

import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.usecase.ExerciseWithSets
import dev.fitcoach.app.domain.usecase.VolumeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Direct, indirect and weekly volume accounting. */
class VolumeCalculatorTest {

    private fun exercise(
        id: Long,
        primary: MuscleGroup,
        secondary: List<MuscleGroup> = emptyList()
    ) = Exercise(id = id, name = "Ex $id", primaryMuscle = primary, secondaryMuscles = secondary)

    @Test
    fun `a set counts fully for the primary muscle`() {
        val session = listOf(ExerciseWithSets(exercise(1, MuscleGroup.CHEST), sets = 4))
        val effective = VolumeCalculator.effectiveSetsByMuscle(session)
        assertEquals(4.0, effective[MuscleGroup.CHEST]!!, 0.001)
    }

    @Test
    fun `a set counts half for every secondary muscle`() {
        val session = listOf(
            ExerciseWithSets(
                exercise(1, MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)),
                sets = 4
            )
        )
        val effective = VolumeCalculator.effectiveSetsByMuscle(session)
        assertEquals(4.0, effective[MuscleGroup.CHEST]!!, 0.001)
        assertEquals(2.0, effective[MuscleGroup.TRICEPS]!!, 0.001)
        assertEquals(2.0, effective[MuscleGroup.SHOULDERS]!!, 0.001)
    }

    @Test
    fun `indirect volume accumulates across exercises of the same session`() {
        val session = listOf(
            ExerciseWithSets(exercise(1, MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS)), sets = 4),
            ExerciseWithSets(exercise(2, MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS)), sets = 3)
        )
        val effective = VolumeCalculator.effectiveSetsByMuscle(session)
        // 4*0.5 + 3*0.5
        assertEquals(3.5, effective[MuscleGroup.TRICEPS]!!, 0.001)
    }

    @Test
    fun `weekly direct sets are summed only on the primary muscle`() {
        val sessions = listOf(
            listOf(ExerciseWithSets(exercise(1, MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS)), sets = 4)),
            listOf(ExerciseWithSets(exercise(2, MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS)), sets = 3))
        )
        val weekly = VolumeCalculator.weeklyDirectSets(sessions)
        assertEquals(7, weekly[MuscleGroup.CHEST])
        assertEquals(null, weekly[MuscleGroup.TRICEPS])
    }

    @Test
    fun `weekly frequency counts the sessions that touch a muscle`() {
        val sessions = listOf(
            listOf(ExerciseWithSets(exercise(1, MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS)), sets = 4)),
            listOf(ExerciseWithSets(exercise(2, MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS)), sets = 3)),
            listOf(ExerciseWithSets(exercise(3, MuscleGroup.BACK), sets = 4))
        )
        val frequency = VolumeCalculator.weeklyFrequency(sessions)
        assertEquals(2, frequency[MuscleGroup.TRICEPS])
        assertEquals(1, frequency[MuscleGroup.CHEST])
    }

    @Test
    fun `ceilings grow with the experience level`() {
        assertTrue(
            VolumeCalculator.weeklyDirectCeiling(ExperienceLevel.BEGINNER) <
                VolumeCalculator.weeklyDirectCeiling(ExperienceLevel.INTERMEDIATE)
        )
        assertTrue(
            VolumeCalculator.weeklyDirectCeiling(ExperienceLevel.INTERMEDIATE) <
                VolumeCalculator.weeklyDirectCeiling(ExperienceLevel.ADVANCED)
        )
        assertTrue(
            VolumeCalculator.sessionEffectiveCeiling(ExperienceLevel.BEGINNER) <
                VolumeCalculator.sessionEffectiveCeiling(ExperienceLevel.ADVANCED)
        )
    }

    @Test
    fun `session set count sums every exercise`() {
        val session = listOf(
            ExerciseWithSets(exercise(1, MuscleGroup.CHEST), sets = 4),
            ExerciseWithSets(exercise(2, MuscleGroup.BACK), sets = 3)
        )
        assertEquals(7, VolumeCalculator.sessionSetCount(session))
    }

    @Test
    fun `rest time increases the cost of a set in minutes`() {
        val shortRest = VolumeCalculator.minutesPerSet(60)
        val longRest = VolumeCalculator.minutesPerSet(180)
        assertTrue(longRest > shortRest)
    }
}
