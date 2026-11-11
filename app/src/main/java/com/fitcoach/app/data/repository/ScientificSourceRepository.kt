package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.dao.ScientificSourceDao
import com.fitcoach.app.data.local.entity.ScientificSourceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Accesso alla base di conoscenza delle fonti scientifiche. */
@Singleton
class ScientificSourceRepository @Inject constructor(
    private val dao: ScientificSourceDao
) {
    val flow: Flow<List<ScientificSourceEntity>> = dao.observeAll()

    suspend fun getAll(): List<ScientificSourceEntity> = dao.getAll()
}
