package com.fitcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fitcoach.app.data.local.entity.V2GenerationJobEntity
import com.fitcoach.app.data.local.entity.V2GenerationPayloadEntity

@Dao
abstract class V2GenerationDao {
    @Query("SELECT * FROM v2_generation_jobs WHERE id = :id")
    abstract suspend fun job(id: String): V2GenerationJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun saveJob(job: V2GenerationJobEntity)

    @Query("SELECT * FROM v2_generation_payloads WHERE jobId = :jobId")
    abstract suspend fun payloads(jobId: String): List<V2GenerationPayloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun savePayloads(payloads: List<V2GenerationPayloadEntity>)

    @Query("DELETE FROM v2_generation_payloads WHERE jobId = :jobId")
    protected abstract suspend fun deletePayloads(jobId: String)

    @Transaction
    open suspend fun replaceCheckpoint(
        job: V2GenerationJobEntity,
        payloads: List<V2GenerationPayloadEntity>
    ) {
        saveJob(job)
        deletePayloads(job.id)
        if (payloads.isNotEmpty()) savePayloads(payloads)
    }
}
