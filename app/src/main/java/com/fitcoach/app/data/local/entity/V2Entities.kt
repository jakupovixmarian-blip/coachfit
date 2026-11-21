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

/**
 * Ricetta creata dall'utente a partire dagli alimenti di un pasto del piano.
 *
 * Salva la composizione (nomi e grammature) come testo, perche una ricetta e'
 * uno snapshot: se il piano cambia, la ricetta creata resta quella che l'utente
 * ha approvato invece di mutare sotto i suoi occhi.
 */
@Entity(tableName = "saved_recipes", indices = [Index(value = ["createdAt"])])
data class SavedRecipeEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val slot: String,
    val dayIndex: Int,
    val style: String,
    /** Ingredienti serializzati: "nome\u001Fgrammi\u001Fkcal\u001Fproteine\u001Fcarbo\u001Fgrassi" per riga. */
    val ingredients: String,
    val steps: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val createdAt: Long
)
