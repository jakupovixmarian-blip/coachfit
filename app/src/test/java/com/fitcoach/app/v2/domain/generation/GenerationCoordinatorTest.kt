package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.v2.ai.provider.ProviderConfiguration
import com.fitcoach.app.v2.ai.provider.ProviderKind
import com.fitcoach.app.v2.domain.GenerationPhase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class GenerationCoordinatorTest {
    private val profile = GenerationProfile(
        ageYears = 30,
        biologicalSex = "F",
        heightCm = 170.0,
        weightKg = 65.0,
        bodyFatPercent = null,
        primaryGoal = "ipertrofia",
        trainingExperience = "intermedio",
        equipment = "palestra",
        trainingDays = 2,
        answers = emptyMap()
    )
    private val configuration = ProviderConfiguration(
        provider = ProviderKind.CUSTOM,
        baseUrl = "https://example.invalid/v1",
        model = "test",
        credentialRef = null,
        maxOutputTokens = 100,
        timeoutMs = 1_000
    )

    @Test
    fun `commit callback runs only after all phases validate`() = runTest {
        val gateway = FakeGateway()
        val coordinator = GenerationCoordinator(gateway, GenerationPayloadValidator(), NoOpCheckpointStore())
        var committed = false

        val result = coordinator.runAndCommit(
            profile,
            configuration,
            4L,
            object : ProgramArtifactCommitter {
                override suspend fun commit(profile: GenerationProfile, payloads: Map<GenerationPhase, kotlinx.serialization.json.JsonObject>) {
                    committed = true
                }
            }
        )

        assertThat(result).isInstanceOf(GenerationRunResult.Ready::class.java)
        assertThat(committed).isTrue()
        assertThat(gateway.calls).containsExactlyElementsIn(GenerationPhase.entries).inOrder()
    }

    @Test
    fun `failed phase does not commit and can resume without repeating validated phases`() = runTest {
        val gateway = FakeGateway(failOn = GenerationPhase.WORKOUT)
        val coordinator = GenerationCoordinator(gateway, GenerationPayloadValidator(), NoOpCheckpointStore())
        val first = coordinator.run(profile, configuration, 5L)
        assertThat(first).isInstanceOf(GenerationRunResult.Failed::class.java)
        assertThat(gateway.calls).containsExactly(GenerationPhase.NUTRITION, GenerationPhase.WORKOUT).inOrder()

        gateway.failOn = null
        val failed = first as GenerationRunResult.Failed
        val resumed = coordinator.run(
            profile,
            configuration,
            5L,
            failed.job.retry(GenerationPhase.WORKOUT),
            failed.payloads
        )

        assertThat(resumed).isInstanceOf(GenerationRunResult.Ready::class.java)
        assertThat(gateway.calls).containsExactly(
            GenerationPhase.NUTRITION,
            GenerationPhase.WORKOUT,
            GenerationPhase.WORKOUT,
            GenerationPhase.CALENDAR,
            GenerationPhase.INDICATORS
        ).inOrder()
    }

    private class NoOpCheckpointStore : GenerationCheckpointStore {
        override suspend fun load(jobId: String): GenerationCheckpoint? = null
        override suspend fun save(checkpoint: GenerationCheckpoint) = Unit
    }

    private class FakeGateway(var failOn: GenerationPhase? = null) : AiGenerationGateway {
        val calls = mutableListOf<GenerationPhase>()

        override suspend fun generate(
            configuration: ProviderConfiguration,
            phase: GenerationPhase,
            profile: GenerationProfile,
            previousPayloads: Map<GenerationPhase, kotlinx.serialization.json.JsonObject>
        ): GenerationGatewayResult {
            calls += phase
            if (phase == failOn) return GenerationGatewayResult.Failure("provider offline", true)
            val payload = buildJsonObject {
                put("assumptions", JsonArray(emptyList()))
                when (phase) {
                    GenerationPhase.NUTRITION -> put("days", JsonArray(emptyList()))
                    GenerationPhase.WORKOUT -> put("days", JsonArray(List(profile.trainingDays) { buildJsonObject {} }))
                    GenerationPhase.CALENDAR -> put("workoutDayIndices", JsonArray(emptyList()))
                    GenerationPhase.INDICATORS -> put("metrics", JsonArray(emptyList()))
                }
            }
            return GenerationGatewayResult.Success(payload, "test")
        }
    }
}
