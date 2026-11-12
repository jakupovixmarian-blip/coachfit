package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.ProgramChangeDao
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.ProgramChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangeRepository @Inject constructor(
    private val dao: ProgramChangeDao
) {
    val allChangesFlow: Flow<List<ProgramChange>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    val proposedFlow: Flow<List<ProgramChange>> = dao.observeProposed().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<ProgramChange> = dao.getAll().map { it.toDomain() }
    suspend fun getById(id: Long): ProgramChange? = dao.getById(id)?.toDomain()
    suspend fun getAppliedPendingOutcome(): List<ProgramChange> = dao.getAppliedPendingOutcome().map { it.toDomain() }

    suspend fun add(change: ProgramChange): Long = dao.insert(change.toEntity())

    suspend fun updateStatus(id: Long, status: ChangeStatus) =
        dao.updateStatus(id, status, if (status == ChangeStatus.APPLIED) System.currentTimeMillis() else null)


    suspend fun recordOutcome(id: Long, status: String, actualOutcome: String, followUpAction: String) =
        dao.updateOutcome(id, status, System.currentTimeMillis(), actualOutcome, followUpAction)

    suspend fun update(change: ProgramChange) = dao.update(change.toEntity())
}
