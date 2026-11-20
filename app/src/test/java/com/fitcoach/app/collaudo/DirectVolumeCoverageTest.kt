package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.workout.VolumeGuidelines
import com.fitcoach.app.domain.workout.WorkoutPlanValidator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Difetto segnalato: in una scheda di ipertrofia comparivano 6 serie di petto,
 * 12 di schiena ma ZERO esercizi diretti per bicipiti e tricipiti. Il validatore
 * guardava solo il volume "effettivo" (diretto + indiretto), quindi i bicipiti
 * risultavano coperti dalle trazioni e i tricipiti dalle distensioni.
 *
 * Lo stimolo indiretto non sostituisce il lavoro specifico: un gruppo senza
 * serie dirette resta sotto-allenato e il piano va respinto.
 */
class DirectVolumeCoverageTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 2, experience = TrainingExperience.INTERMEDIATE,
        goal = Goal.HYPERTROPHY, availableEquipment = Equipment.entries.toSet()
    )

    private fun ex(
        id: Long, name: String, muscle: MuscleGroup,
        secondary: Set<MuscleGroup> = emptySet(),
        category: ExerciseCategory = ExerciseCategory.COMPOUND
    ) = Exercise(
        id = id, name = name, primaryMuscle = muscle, secondaryMuscles = secondary,
        category = category, equipment = setOf(Equipment.BARBELL), technique = "",
        defaultSets = 4, minReps = 8, maxReps = 12, restSeconds = 120, targetRir = 2,
        difficulty = 1
    )

    private fun day(index: Int, groups: Set<MuscleGroup>, exercises: List<Pair<Long, Int>>) =
        WorkoutPlanValidator.DayProposal(
            index, groups,
            exercises.map { (id, sets) -> WorkoutPlanValidator.ExerciseProposal(id, sets) }
        )

    private val catalog = listOf(
        ex(1, "Trazioni", MuscleGroup.BACK, setOf(MuscleGroup.BICEPS)),
        ex(2, "Rematore", MuscleGroup.BACK, setOf(MuscleGroup.BICEPS)),
        ex(3, "Curl bicipiti", MuscleGroup.BICEPS, category = ExerciseCategory.ISOLATION),
        ex(4, "Panca", MuscleGroup.CHEST, setOf(MuscleGroup.TRICEPS)),
        ex(5, "French press", MuscleGroup.TRICEPS, category = ExerciseCategory.ISOLATION)
    )

    @Test
    fun `bicipiti senza esercizi diretti vengono rifiutati`() {
        // Trazioni e rematore stimolano i bicipiti solo indirettamente.
        val days = listOf(
            day(0, setOf(MuscleGroup.BACK, MuscleGroup.BICEPS), listOf(1L to 5, 2L to 5)),
            day(1, setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS), listOf(4L to 5, 5L to 5))
        )
        val report = WorkoutPlanValidator.validate(profile, catalog, days)

        assertThat(report.ok).isFalse()
        assertThat(report.violations.joinToString("\n")).contains("BICEPS")
        assertThat(report.violations.joinToString("\n")).contains("serie dirette")
    }

    @Test
    fun `aggiungere il curl rende i bicipiti conformi`() {
        val days = listOf(
            day(0, setOf(MuscleGroup.BACK, MuscleGroup.BICEPS), listOf(1L to 5, 2L to 4, 3L to 4)),
            day(1, setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS), listOf(4L to 5, 5L to 4))
        )
        val report = WorkoutPlanValidator.validate(profile, catalog, days)

        // Con il curl i bicipiti hanno 4 serie dirette: la violazione sparisce.
        assertThat(report.violations.joinToString("\n")).doesNotContain("BICEPS")
    }

    @Test
    fun `le soglie seguono gruppo ed esperienza invece di un numero fisso`() {
        val small = VolumeGuidelines.forGroup(MuscleGroup.BICEPS, TrainingExperience.BEGINNER)
        val large = VolumeGuidelines.forGroup(MuscleGroup.BACK, TrainingExperience.BEGINNER)
        assertThat(small.min).isLessThan(large.min)

        val advanced = VolumeGuidelines.forGroup(MuscleGroup.BACK, TrainingExperience.ADVANCED)
        assertThat(advanced.min).isAtLeast(large.min)
    }
}
