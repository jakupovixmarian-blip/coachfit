package dev.fitcoach.app.data.catalog

import dev.fitcoach.app.data.local.dao.FoodDao
import dev.fitcoach.app.data.local.mapper.toDomain
import dev.fitcoach.app.data.local.mapper.toEntity
import dev.fitcoach.app.domain.catalog.FoodStore
import dev.fitcoach.app.domain.model.Food
import javax.inject.Inject

/**
 * [FoodStore] on top of Room.
 *
 * Updates are written through the entity so the new provenance columns are
 * persisted together with the values, never separately.
 */
class RoomFoodStore @Inject constructor(
    private val dao: FoodDao
) : FoodStore {

    override suspend fun bySourceRef(source: String, sourceId: String): Food? =
        dao.bySourceRef(source, sourceId)?.toDomain()

    override suspend fun insert(food: Food): Long = dao.insert(food.toEntity())

    override suspend fun update(food: Food) {
        dao.update(food.toEntity())
    }

    override suspend fun search(query: String, limit: Int): List<Food> =
        dao.searchByName(query, limit).map { it.toDomain() }
}
