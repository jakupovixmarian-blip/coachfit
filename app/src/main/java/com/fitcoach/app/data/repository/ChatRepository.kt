package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.AiSettingsDao
import com.fitcoach.app.data.local.dao.ChatDao
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val aiSettingsDao: AiSettingsDao
) {
    val messagesFlow: Flow<List<ChatMessage>> = chatDao.observeAll().map { list -> list.map { it.toDomain() } }
    val aiSettingsFlow: Flow<AiProviderSettings> = aiSettingsDao.observe().map {
        it?.toDomain() ?: AiProviderSettings()
    }

    suspend fun getRecent(limit: Int = 30): List<ChatMessage> =
        chatDao.getRecent(limit).map { it.toDomain() }.reversed()

    suspend fun add(message: ChatMessage): Long = chatDao.insert(message.toEntity())

    suspend fun clear() = chatDao.clear()

    suspend fun delete(id: Long) = chatDao.delete(id)

    suspend fun getAiSettings(): AiProviderSettings =
        aiSettingsDao.get()?.toDomain() ?: AiProviderSettings()

    suspend fun saveAiSettings(settings: AiProviderSettings) = aiSettingsDao.upsert(settings.toEntity())
}
