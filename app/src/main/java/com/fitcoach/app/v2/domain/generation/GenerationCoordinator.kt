package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.v2.ai.provider.ProviderConfiguration
import com.fitcoach.app.v2.domain.GenerationJob
import com.fitcoach.app.v2.domain.GenerationPhase
import com.fitcoach.app.v2.domain.GenerationPhaseStatus
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GenerationRunResult {
    data class Ready(
        val job: GenerationJob,
        val payloads: Map<GenerationPhase, JsonObject>
    ) : GenerationRunResult

    data class Failed(
        val job: GenerationJob,
        val payloads: Map<GenerationPhase, JsonObject>,
        val message: String,
        val retryable: Boolean
    ) : GenerationRunResult
}

interface ProgramArtifactCommitter {
    /** Scrive tutti gli artefatti in una transazione, solo dopo la validazione completa. */
    suspend fun commit(profile: GenerationProfile, payloads: Map<GenerationPhase, JsonObject>)
}

@Singleton
class GenerationCoordinator @Inject constructor(
    private val gateway: AiGenerationGateway,
    private val validator: GenerationPayloadValidator,
    private val checkpointStore: GenerationCheckpointStore
) {
    suspend fun run(
        profile: GenerationProfile,
        configuration: ProviderConfiguration,
        profileVersion: Long,
        initialJob: GenerationJob = GenerationJob(profileVersion = profileVersion),
        existingPayloads: Map<GenerationPhase, JsonObject> = emptyMap()
    ): GenerationRunResult {
        require(initialJob.profileVersion == profileVersion) { "Il job non appartiene alla versione profilo corrente." }
        require(!initialJob.committed) { "Il job è già stato committato." }
        var job = initialJob
        val payloads = existingPayloads.toMutableMap()
        while (job.phases.any { it.status == GenerationPhaseStatus.PENDING }) {
            val phase = job.currentPhase?.phase ?: error("Job senza fase corrente")
            job = job.startNext()
            when (val result = gateway.generate(configuration, phase, profile, payloads)) {
                is GenerationGatewayResult.Success -> {
                    val validationErrors = validator.validate(phase, result.payload, profile)
                    if (validationErrors.isNotEmpty()) {
                        val message = "Payload ${phase.name} non valido: ${validationErrors.joinToString()}"
                        job = job.markFailed(phase, message)
                        checkpointStore.save(GenerationCheckpoint(job, payloads.toMap()))
                        return GenerationRunResult.Failed(job, payloads.toMap(), message, false)
                    }
                    payloads[phase] = result.payload
                    job = job.markValidated(phase)
                    checkpointStore.save(GenerationCheckpoint(job, payloads.toMap()))
                }
                is GenerationGatewayResult.Failure -> {
                    job = job.markFailed(phase, result.message)
                    checkpointStore.save(GenerationCheckpoint(job, payloads.toMap()))
                    return GenerationRunResult.Failed(job, payloads.toMap(), result.message, result.retryable)
                }
            }
        }
        val committedJob = job.commit()
        checkpointStore.save(GenerationCheckpoint(committedJob, payloads.toMap()))
        return GenerationRunResult.Ready(committedJob, payloads.toMap())
    }

    suspend fun runAndCommit(
        profile: GenerationProfile,
        configuration: ProviderConfiguration,
        profileVersion: Long,
        committer: ProgramArtifactCommitter,
        initialJob: GenerationJob = GenerationJob(profileVersion = profileVersion),
        existingPayloads: Map<GenerationPhase, JsonObject> = emptyMap()
    ): GenerationRunResult {
        return when (val result = run(profile, configuration, profileVersion, initialJob, existingPayloads)) {
            is GenerationRunResult.Failed -> result
            is GenerationRunResult.Ready -> {
                committer.commit(profile, result.payloads)
                result
            }
        }
    }
}
