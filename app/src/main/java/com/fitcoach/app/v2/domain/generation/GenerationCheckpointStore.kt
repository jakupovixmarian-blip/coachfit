package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.data.local.dao.V2GenerationDao
import com.fitcoach.app.data.local.entity.V2GenerationJobEntity
import com.fitcoach.app.data.local.entity.V2GenerationPayloadEntity
import com.fitcoach.app.v2.domain.GenerationJob
import com.fitcoach.app.v2.domain.GenerationPhase
import com.fitcoach.app.v2.domain.GenerationPhaseState
import com.fitcoach.app.v2.domain.GenerationPhaseStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

interface GenerationCheckpointStore {
    suspend fun load(jobId: String): GenerationCheckpoint?
    suspend fun save(checkpoint: GenerationCheckpoint)
}

data class GenerationCheckpoint(
    val job: GenerationJob,
    val payloads: Map<GenerationPhase, JsonObject>
)

@Serializable
private data class JobSnapshot(
    val id: String,
    val profileVersion: Long,
    val committed: Boolean,
    val phases: List<PhaseSnapshot>
)

@Serializable
private data class PhaseSnapshot(
    val phase: String,
    val status: String,
    val attempt: Int,
    val error: String?
)

@Singleton
class RoomGenerationCheckpointStore @Inject constructor(
    private val dao: V2GenerationDao,
    private val json: Json
) : GenerationCheckpointStore {
    override suspend fun load(jobId: String): GenerationCheckpoint? {
        val row = dao.job(jobId) ?: return null
        val snapshot = runCatching { json.decodeFromString<JobSnapshot>(row.stateJson) }.getOrNull() ?: return null
        val job = snapshot.toDomain() ?: return null
        val payloads = dao.payloads(jobId).mapNotNull { payload ->
            runCatching {
                GenerationPhase.valueOf(payload.phase) to json.parseToJsonElement(payload.payloadJson) as JsonObject
            }.getOrNull()
        }.toMap()
        return GenerationCheckpoint(job, payloads)
    }

    override suspend fun save(checkpoint: GenerationCheckpoint) {
        val now = System.currentTimeMillis()
        val snapshot = JobSnapshot(
            id = checkpoint.job.id,
            profileVersion = checkpoint.job.profileVersion,
            committed = checkpoint.job.committed,
            phases = checkpoint.job.phases.map { PhaseSnapshot(it.phase.name, it.status.name, it.attempt, it.error) }
        )
        dao.replaceCheckpoint(
            V2GenerationJobEntity(
                id = checkpoint.job.id,
                profileVersion = checkpoint.job.profileVersion,
                stateJson = json.encodeToString(snapshot),
                committed = checkpoint.job.committed,
                updatedAt = now
            ),
            checkpoint.payloads.map { (phase, payload) ->
                V2GenerationPayloadEntity(checkpoint.job.id, phase.name, payload.toString(), now)
            }
        )
    }

    private fun JobSnapshot.toDomain(): GenerationJob? = runCatching {
        GenerationJob(
            id = id,
            profileVersion = profileVersion,
            committed = committed,
            phases = phases.map {
                GenerationPhaseState(
                    phase = GenerationPhase.valueOf(it.phase),
                    status = GenerationPhaseStatus.valueOf(it.status),
                    attempt = it.attempt,
                    error = it.error
                )
            }
        )
    }.getOrNull()
}
