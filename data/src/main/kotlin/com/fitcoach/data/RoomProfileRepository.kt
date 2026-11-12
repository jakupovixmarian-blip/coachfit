package com.fitcoach.data

import com.fitcoach.database.ProfileDao
import com.fitcoach.database.ProfileEntity
import com.fitcoach.domain.ProfileRepository
import com.fitcoach.domain.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProfileRepository(
    private val dao: ProfileDao
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = dao.observe().map { entity ->
        entity?.let {
            UserProfile(
                age = it.age,
                biologicalSex = it.biologicalSex,
                heightCm = it.heightCm,
                weightKg = it.weightKg,
                bodyFatPercent = it.bodyFatPercent,
                goal = it.goal,
                trainingExperience = it.trainingExperience,
                availableTrainingDays = it.availableTrainingDays
            )
        }
    }

    override suspend fun saveProfile(profile: UserProfile) {
        dao.save(
            ProfileEntity(
                age = profile.age,
                biologicalSex = profile.biologicalSex,
                heightCm = profile.heightCm,
                weightKg = profile.weightKg,
                bodyFatPercent = profile.bodyFatPercent,
                goal = profile.goal,
                trainingExperience = profile.trainingExperience,
                availableTrainingDays = profile.availableTrainingDays
            )
        )
    }

    override suspend fun clearProfile() = dao.clear()
}
