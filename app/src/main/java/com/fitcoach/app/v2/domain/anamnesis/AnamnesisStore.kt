package com.fitcoach.app.v2.domain.anamnesis

import com.fitcoach.app.data.local.dao.V2AnamnesisDao
import com.fitcoach.app.data.local.entity.V2AnamnesisAnswerEntity
import com.fitcoach.app.data.local.entity.V2AnamnesisSessionEntity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private val Context.anamnesisDataStore by preferencesDataStore(name = "fitcoach_v2_anamnesis")

interface AnamnesisStore {
    suspend fun load(): AnamnesisSession?
    suspend fun save(session: AnamnesisSession)
}

@Singleton
class DataStoreAnamnesisStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : AnamnesisStore {
    override suspend fun load(): AnamnesisSession? {
        val raw = context.anamnesisDataStore.data.first()[SESSION_KEY] ?: return null
        return runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val answers = root["answers"]?.jsonObject.orEmpty().mapNotNull { (name, value) ->
                val field = AnamnesisField.valueOf(name)
                val item = value.jsonObject
                val status = AnswerStatus.valueOf(item["status"]!!.jsonPrimitive.content)
                val answer = AnamnesisAnswer(
                    status = status,
                    value = item["value"]?.jsonPrimitive?.content,
                    updatedAt = item["updatedAt"]!!.jsonPrimitive.content.toLong()
                )
                field to answer
            }.toMap()
            AnamnesisSession(
                id = root["id"]!!.jsonPrimitive.content,
                answers = answers,
                confirmedAt = root["confirmedAt"]?.jsonPrimitive?.content?.toLongOrNull()
            )
        }.getOrNull()
    }

    override suspend fun save(session: AnamnesisSession) {
        val root = buildJsonObject {
            put("id", session.id)
            session.confirmedAt?.let { put("confirmedAt", it) }
            put("answers", buildJsonObject {
                session.answers.forEach { (field, answer) ->
                    put(field.name, buildJsonObject {
                        put("status", answer.status.name)
                        answer.value?.let { value -> put("value", value) }
                        put("updatedAt", answer.updatedAt)
                    })
                }
            })
        }
        context.anamnesisDataStore.edit { it[SESSION_KEY] = root.toString() }
    }

    private companion object {
        val SESSION_KEY = stringPreferencesKey("session")
    }
}

@Singleton
class RoomAnamnesisStore @Inject constructor(
    private val dao: V2AnamnesisDao
) : AnamnesisStore {
    override suspend fun load(): AnamnesisSession? {
        val entity = dao.latestSession() ?: return null
        val answers = dao.answers(entity.id).mapNotNull { row ->
            runCatching {
                AnamnesisField.valueOf(row.field) to AnamnesisAnswer(
                    status = AnswerStatus.valueOf(row.status),
                    value = row.value,
                    updatedAt = row.updatedAt
                )
            }.getOrNull()
        }.toMap()
        return AnamnesisSession(entity.id, answers, entity.confirmedAt)
    }

    override suspend fun save(session: AnamnesisSession) {
        val now = System.currentTimeMillis()
        val previous = dao.latestSession()?.takeIf { it.id == session.id }
        dao.replace(
            V2AnamnesisSessionEntity(
                id = session.id,
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
                confirmedAt = session.confirmedAt
            ),
            session.answers.map { (field, answer) ->
                V2AnamnesisAnswerEntity(
                    sessionId = session.id,
                    field = field.name,
                    status = answer.status.name,
                    value = answer.value,
                    updatedAt = answer.updatedAt
                )
            }
        )
    }
}
