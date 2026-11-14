package com.fitcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "v2_anamnesis_sessions",
    indices = [Index(value = ["updatedAt"])]
)
data class V2AnamnesisSessionEntity(
    @androidx.room.PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val confirmedAt: Long?
)

@Entity(
    tableName = "v2_anamnesis_answers",
    primaryKeys = ["sessionId", "field"],
    indices = [Index(value = ["sessionId"])]
)
data class V2AnamnesisAnswerEntity(
    val sessionId: String,
    val field: String,
    val status: String,
    val value: String?,
    val updatedAt: Long
)

@Entity(tableName = "v2_generation_jobs", indices = [Index(value = ["updatedAt"])])
data class V2GenerationJobEntity(
    @androidx.room.PrimaryKey val id: String,
    val profileVersion: Long,
    val stateJson: String,
    val committed: Boolean,
    val updatedAt: Long
)

@Entity(
    tableName = "v2_generation_payloads",
    primaryKeys = ["jobId", "phase"],
    indices = [Index(value = ["jobId"])]
)
data class V2GenerationPayloadEntity(
    val jobId: String,
    val phase: String,
    val payloadJson: String,
    val updatedAt: Long
)
