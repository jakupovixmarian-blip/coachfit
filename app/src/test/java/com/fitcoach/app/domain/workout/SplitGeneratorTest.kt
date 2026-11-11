package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SplitGeneratorTest {

    private val baseProfile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 4, experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        availableEquipment = Equipment.entries.toSet()
    )

    @Test
    fun `three days or fewer recommends full body`() {
        assertThat(SplitGenerator.recommendSplit(baseProfile.copy(trainingDaysPerWeek = 3)))
            .isEqualTo(SplitType.FULL_BODY)
    }

    @Test
    fun `four days recommends upper lower`() {
        assertThat(SplitGenerator.recommendSplit(baseProfile.copy(trainingDaysPerWeek = 4)))
            .isEqualTo(SplitType.UPPER_LOWER)
    }

    @Test
    fun `five or more days recommends push pull legs`() {
        assertThat(SplitGenerator.recommendSplit(baseProfile.copy(trainingDaysPerWeek = 5)))
            .isEqualTo(SplitType.PUSH_PULL_LEGS)
    }

    @Test
    fun `weekly sets scale with experience`() {
        assertThat(SplitGenerator.weeklySetsPerGroup(TrainingExperience.BEGINNER)).isLessThan(
            SplitGenerator.weeklySetsPerGroup(TrainingExperience.ADVANCED)
        )
    }

    @Test
    fun `build week marks rest days correctly`() {
        val week = SplitGenerator.buildWeek(SplitType.UPPER_LOWER, listOf(0, 1, 3, 4), TrainingExperience.INTERMEDIATE)
        assertThat(week).hasSize(7)
        assertThat(week.count { !it.isRest }).isEqualTo(4)
        assertThat(week[2].isRest).isTrue()
    }

    @Test
    fun `upper lower alternates upper and lower`() {
        val week = SplitGenerator.buildWeek(SplitType.UPPER_LOWER, listOf(0, 1, 3, 4), TrainingExperience.INTERMEDIATE)
        assertThat(week[0].title).isEqualTo("Upper")
        assertThat(week[1].title).isEqualTo("Lower")
        assertThat(week[3].title).isEqualTo("Upper")
        assertThat(week[4].title).isEqualTo("Lower")
    }

    @Test
    fun `push pull legs rotates`() {
        val week = SplitGenerator.buildWeek(SplitType.PUSH_PULL_LEGS, listOf(0, 1, 2), TrainingExperience.ADVANCED)
        assertThat(week[0].title).isEqualTo("Push")
        assertThat(week[1].title).isEqualTo("Pull")
        assertThat(week[2].title).isEqualTo("Legs")
    }

    @Test
    fun `exercise selection respects equipment`() {
        val bodyweightProfile = baseProfile.copy(availableEquipment = setOf(Equipment.BODYWEIGHT))
        val selected = SplitGenerator.selectExercises(
            setOf(com.fitcoach.app.domain.model.MuscleGroup.CHEST),
            ExerciseSeedData.exercises,
            bodyweightProfile,
            count = 5
        )
        selected.forEach { ex ->
            assertThat(ex.bodyweightOnly || ex.equipment.isEmpty() || ex.equipment.any { it == Equipment.BODYWEIGHT })
                .isTrue()
        }
    }

    @Test
    fun `exercise selection avoids movements to avoid`() {
        val profile = baseProfile.copy(movementsToAvoid = listOf("squat"))
        val selected = SplitGenerator.selectExercises(
            setOf(com.fitcoach.app.domain.model.MuscleGroup.QUADS),
            ExerciseSeedData.exercises,
            profile,
            count = 6
        )
        assertThat(selected.map { it.name.lowercase() }).doesNotContain("squat")
    }

    @Test
    fun `seed exercise database is not empty and has valid ranges`() {
        assertThat(ExerciseSeedData.exercises).isNotEmpty()
        ExerciseSeedData.exercises.forEach { ex ->
            assertThat(ex.minReps).isLessThan(ex.maxReps)
            assertThat(ex.defaultSets).isAtLeast(1)
        }
    }
}
