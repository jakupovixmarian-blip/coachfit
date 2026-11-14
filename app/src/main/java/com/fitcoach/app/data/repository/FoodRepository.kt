package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.FoodDao
import com.fitcoach.app.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val dao: FoodDao
) {
    val foodsFlow: Flow<List<FoodItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<FoodItem> = dao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): FoodItem? = dao.getById(id)?.toDomain()

    suspend fun getByName(name: String): FoodItem? = dao.getByName(name)?.toDomain()

    suspend fun search(query: String): List<FoodItem> =
        if (query.isBlank()) getAll() else dao.search(query).map { it.toDomain() }

    suspend fun searchNormalized(query: String): List<FoodItem> =
        if (query.isBlank()) getAll() else dao.searchNormalized(query).map { it.toDomain() }

    /** Alimenti creati o importati dall'utente ("I miei alimenti"). */
    val customFoodsFlow: Flow<List<FoodItem>> =
        dao.observeCustom().map { list -> list.map { it.toDomain() } }

    suspend fun getByNormalizedName(name: String): FoodItem? =
        dao.getByNormalizedName(com.fitcoach.app.domain.model.FoodItem.normalizeName(name))?.toDomain()

    suspend fun getBySourceId(sourceId: String): FoodItem? = dao.getBySourceId(sourceId)?.toDomain()

    suspend fun addCustom(item: FoodItem): Long = dao.insert(item.toEntity())

    suspend fun count(): Int = dao.count()
}
