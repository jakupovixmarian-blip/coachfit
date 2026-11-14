package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.UserProfileDao
import com.fitcoach.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    val profileFlow: Flow<UserProfile?> = dao.observe().map { it?.toDomain() }

    suspend fun get(): UserProfile? = dao.get()?.toDomain()

    suspend fun save(profile: UserProfile) = dao.upsert(profile.toEntity())

    suspend fun exists(): Boolean = dao.count() > 0
}
