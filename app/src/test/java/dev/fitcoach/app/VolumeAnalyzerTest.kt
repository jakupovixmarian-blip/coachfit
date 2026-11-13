package dev.fitcoach.app

import dev.fitcoach.app.domain.engine.VolumeAnalyzer
import dev.fitcoach.app.domain.engine.VolumeStatus
import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.model.RoutineExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeAnalyzerTest {

    private val bench = Exercise(
        id = 1L,
        name = "Panca piana",
        primaryMuscle = MuscleGroup.CHEST,
        secondaryMuscles = listOf(MuscleGroup.TRICEPS),
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.HORIZONTAL_PUSH
    )

    private val row = Exercise(
        id = 2L,
        name = "Rematore con bilanciere",
        primaryMuscle = MuscleGroup.BACK,
        secondaryMuscles = listOf(MuscleGroup.BICEPS),
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.HORIZONTAL_PULL
    )

    private val overheadPress = Exercise(
        id = 3L,
        name = "Military press",
        primaryMuscle = MuscleGroup.SHOULDERS,
        secondaryMuscles = listOf(MuscleGroup.TRICEPS),
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.VERTICAL_PUSH
    )

    private val squat = Exercise(
        id = 4L,
        name = "Squat",
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = listOf(MuscleGroup.GLUTES),
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.SQUAT
    )

    private val legCurl = Exercise(
        id = 5L,
        name = "Leg curl",
        primaryMuscle = MuscleGroup.HAMSTRINGS,
        equipment = Equipment.MACHINE,
        pattern = MovementPattern.ISOLATION
    )

    private val catalog = listOf(bench, row, overheadPress, squat, legCurl).associateBy { it.id }

    private fun sets(exerciseId: Long, count: Int, rest: Int = 90) = RoutineExercise(
        routineId = 1L,
        exerciseId = exerciseId,
        orderIndex = 0,
        targetSets = count,
        restSeconds = rest
    )

    @Test
    fun `direct and indirect volume are separated`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(1L, 4), sets(2L, 4))),
            exercises = catalog,
            experience = ExperienceLevel.INTERMEDIATE
        )
        val chest = report.perMuscle.first { it.muscle == MuscleGroup.CHEST }
        val triceps = report.perMuscle.first { it.muscle == MuscleGroup.TRICEPS }

        assertEquals(4.0, chest.directSets, 0.001)
        assertEquals(0.0, chest.indirectSets, 0.001)
        assertEquals(0.0, triceps.directSets, 0.001)
        assertEquals(4.0, triceps.indirectSets, 0.001)
        assertEquals(2.0, triceps.effectiveSets, 0.001)
    }

    @Test
    fun `frequency counts the sessions that train a muscle`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(2L, 3)), listOf(sets(2L, 3))),
            exercises = catalog,
            experience = ExperienceLevel.INTERMEDIATE
        )
        assertEquals(2, report.perMuscle.first { it.muscle == MuscleGroup.BACK }.frequency)
    }

    @Test
    fun `a major muscle without direct work blocks the program`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(1L, 4))),
            exercises = catalog,
            experience = ExperienceLevel.BEGINNER
        )
        assertFalse(report.isCoherent)
        assertTrue(report.issues.any { it.startsWith("!") && it.contains("Glutei") })
    }

    @Test
    fun `the same exercise repeated in a session is blocking`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(1L, 3), sets(1L, 3))),
            exercises = catalog,
            experience = ExperienceLevel.BEGINNER
        )
        assertFalse(report.isCoherent)
        assertTrue(report.issues.any { it.contains("ripetuto") })
    }

    @Test
    fun `an exercise requiring missing equipment is blocking`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(1L, 4))),
            exercises = catalog,
            experience = ExperienceLevel.BEGINNER,
            availableEquipment = setOf(Equipment.DUMBBELL)
        )
        assertTrue(report.issues.any { it.startsWith("!") && it.contains("Bilanciere") })
    }

    @Test
    fun `a program covering every muscle but with low chest volume is coherent with warnings`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(1L, 4), sets(2L, 4), sets(4L, 4), sets(5L, 3), sets(3L, 3))),
            exercises = catalog,
            experience = ExperienceLevel.BEGINNER
        )
        val chest = report.perMuscle.first { it.muscle == MuscleGroup.CHEST }
        assertEquals(VolumeStatus.BELOW_MINIMUM, chest.status)
        assertTrue(report.isCoherent)
        assertTrue(report.issues.any { it.contains("Volume sotto il minimo per Petto") })
    }

    @Test
    fun `session duration is estimated from sets and rest`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(1L, 4, rest = 90))),
            exercises = catalog,
            experience = ExperienceLevel.BEGINNER
        )
        // 4 serie * (45 s di esecuzione + 90 s di recupero) = 540 s = 9 minuti
        assertEquals(9, report.minutesPerSession.first())
    }

    @Test
    fun `status thresholds follow the experience range`() {
        val range = VolumeAnalyzer.thresholds(ExperienceLevel.BEGINNER)
        assertEquals(VolumeStatus.BELOW_MINIMUM, VolumeAnalyzer.statusFor(5.0, range))
        assertEquals(VolumeStatus.OPTIMAL, VolumeAnalyzer.statusFor(12.0, range))
        assertEquals(VolumeStatus.HIGH, VolumeAnalyzer.statusFor(19.0, range))
        assertEquals(VolumeStatus.EXCESSIVE, VolumeAnalyzer.statusFor(25.0, range))
    }

    @Test
    fun `total weekly sets sum the direct work only`() {
        val report = VolumeAnalyzer.analyze(
            sessions = listOf(listOf(sets(1L, 4), sets(2L, 4))),
            exercises = catalog,
            experience = ExperienceLevel.INTERMEDIATE
        )
        assertEquals(8.0, report.totalWeeklySets, 0.001)
    }
}
