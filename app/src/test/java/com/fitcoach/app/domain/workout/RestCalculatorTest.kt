package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.TrainingExperience
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RestCalculatorTest {

    @Test
    fun `compound rest is longer than isolation rest`() {
        val compound = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.HYPERTROPHY, targetRir = 2)
        val isolation = RestCalculator.decide(ExerciseCategory.ISOLATION, Goal.HYPERTROPHY, targetRir = 2)
        assertThat(compound.seconds).isGreaterThan(isolation.seconds)
    }

    @Test
    fun `strength goal extends rest versus cut`() {
        val strength = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.STRENGTH, targetRir = 2)
        val cut = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.CUT, targetRir = 2)
        assertThat(strength.seconds).isGreaterThan(cut.seconds)
    }

    @Test
    fun `low rir increases rest`() {
        val hard = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.HYPERTROPHY, targetRir = 0)
        val easy = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.HYPERTROPHY, targetRir = 4)
        assertThat(hard.seconds).isGreaterThan(easy.seconds)
    }

    @Test
    fun `fatigue extends rest`() {
        val fresh = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.HYPERTROPHY, 2, fatigueSeverity = 0)
        val tired = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.HYPERTROPHY, 2, fatigueSeverity = 3)
        assertThat(tired.seconds).isGreaterThan(fresh.seconds)
    }

    @Test
    fun `rest is always within safety bounds`() {
        val extreme = RestCalculator.decide(
            ExerciseCategory.BODYWEIGHT, Goal.CUT, targetRir = 5,
            loadRatio = 0.5, experience = TrainingExperience.BEGINNER, fatigueSeverity = 0,
            technique = IntensityTechnique.SUPERSET
        )
        assertThat(extreme.seconds).isAtLeast(45)
        assertThat(extreme.seconds).isAtMost(300)
    }

    @Test
    fun `superset shortens rest`() {
        val normal = RestCalculator.decide(ExerciseCategory.ISOLATION, Goal.HYPERTROPHY, 2)
        val super_ = RestCalculator.decide(
            ExerciseCategory.ISOLATION, Goal.HYPERTROPHY, 2, technique = IntensityTechnique.SUPERSET
        )
        assertThat(super_.seconds).isLessThan(normal.seconds)
    }

    @Test
    fun `decision includes a human reason`() {
        val d = RestCalculator.decide(ExerciseCategory.COMPOUND, Goal.HYPERTROPHY, 2)
        assertThat(d.reason).isNotEmpty()
    }

    @Test
    fun `format renders minutes and seconds`() {
        assertThat(RestCalculator.format(90)).isEqualTo("1:30")
        assertThat(RestCalculator.format(45)).isEqualTo("45s")
    }
}
