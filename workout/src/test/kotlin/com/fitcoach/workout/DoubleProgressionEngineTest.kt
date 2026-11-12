package com.fitcoach.workout

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DoubleProgressionEngineTest {
    @Test
    fun `does not increase when pain is reported`() {
        val decision = DoubleProgressionEngine.decide(
            ProgressionInput(ExerciseType.COMPOUND, 70.0, listOf(10, 10, 10, 10), 8, 10, 2.0, true, true)
        )
        assertThat(decision.increase).isFalse()
        assertThat(decision.nextLoadKg).isEqualTo(70.0)
    }

    @Test
    fun `increases compound load after clean top range`() {
        val decision = DoubleProgressionEngine.decide(
            ProgressionInput(ExerciseType.COMPOUND, 70.0, listOf(10, 10, 10, 10), 8, 10, 2.0, true, false)
        )
        assertThat(decision.increase).isTrue()
        assertThat(decision.nextLoadKg).isEqualTo(72.0)
    }
}
