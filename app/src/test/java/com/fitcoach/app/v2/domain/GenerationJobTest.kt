package com.fitcoach.app.v2.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GenerationJobTest {
    @Test
    fun `phases are sequential and final commit is atomic`() {
        var job = GenerationJob(profileVersion = 7L)
        assertThat(job.canCommit).isFalse()

        GenerationPhase.entries.forEach { phase ->
            job = job.startNext().markValidated(phase)
        }
        assertThat(job.canCommit).isTrue()
        assertThat(job.commit().committed).isTrue()
    }

    @Test
    fun `failed phase can be resumed without losing validated phases`() {
        var job = GenerationJob(profileVersion = 4L)
            .startNext()
            .markFailed(GenerationPhase.NUTRITION, "JSON troncato")

        assertThat(job.phases.first().attempt).isEqualTo(1)
        job = job.retry(GenerationPhase.NUTRITION).startNext()
        assertThat(job.phases.first().status).isEqualTo(GenerationPhaseStatus.RUNNING)
        assertThat(job.phases.first().attempt).isEqualTo(2)
    }

    @Test
    fun `cannot run phases in parallel or skip an earlier phase`() {
        val running = GenerationJob(profileVersion = 1L).startNext()
        assertThat(runCatching { running.startNext() }.isFailure).isTrue()
        assertThat(runCatching { GenerationJob(profileVersion = 1L).markValidated(GenerationPhase.WORKOUT) }.isFailure).isTrue()
    }
}
