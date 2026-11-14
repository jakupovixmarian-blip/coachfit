package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.NotificationDao
import com.fitcoach.app.domain.model.NotificationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val dao: NotificationDao
) {
    val notificationsFlow: Flow<List<NotificationRecord>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<NotificationRecord> = dao.getAll().map { it.toDomain() }
    suspend fun getEnabled(): List<NotificationRecord> = dao.getEnabled().map { it.toDomain() }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun setTime(id: Long, time: String) = dao.setTime(id, time)
    suspend fun upsert(record: NotificationRecord) = dao.upsert(record.toEntity())
}
