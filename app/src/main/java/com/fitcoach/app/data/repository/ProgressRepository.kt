package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.CheckInDao
import com.fitcoach.app.data.local.dao.ProgressDao
import com.fitcoach.app.domain.model.CheckIn
import com.fitcoach.app.domain.model.CircumferenceRecord
import com.fitcoach.app.domain.model.ProgressRecord
import com.fitcoach.app.domain.model.WeightRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val checkInDao: CheckInDao
) {
    val weightsFlow: Flow<List<WeightRecord>> = progressDao.observeWeights().map { list -> list.map { it.toDomain() } }
    val circumferencesFlow: Flow<List<CircumferenceRecord>> =
        progressDao.observeCircumferences().map { list -> list.map { it.toDomain() } }
    val progressFlow: Flow<List<ProgressRecord>> = progressDao.observeProgress().map { list -> list.map { it.toDomain() } }
    val checkInsFlow: Flow<List<CheckIn>> = checkInDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getWeights(): List<WeightRecord> = progressDao.getWeights().map { it.toDomain() }
    suspend fun getLatestWeight(): WeightRecord? = progressDao.getLatestWeight()?.toDomain()
    suspend fun addWeight(record: WeightRecord): Long = progressDao.insertWeight(record.toEntity())
    suspend fun deleteWeight(record: WeightRecord) = progressDao.deleteWeight(record.toEntity())

    suspend fun getCircumferences(): List<CircumferenceRecord> =
        progressDao.getCircumferences().map { it.toDomain() }
    suspend fun addCircumference(record: CircumferenceRecord): Long =
        progressDao.insertCircumference(record.toEntity())

    suspend fun getProgressRecords(): List<ProgressRecord> = progressDao.getProgress().map { it.toDomain() }
    suspend fun addProgressRecord(record: ProgressRecord): Long = progressDao.insertProgress(record.toEntity())

    suspend fun getCheckIns(): List<CheckIn> = checkInDao.getAll().map { it.toDomain() }
    suspend fun getLatestCheckIn(): CheckIn? = checkInDao.getLatest()?.toDomain()
    suspend fun addCheckIn(checkIn: CheckIn): Long = checkInDao.insert(checkIn.toEntity())
}
