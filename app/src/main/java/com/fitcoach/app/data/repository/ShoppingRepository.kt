package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.ShoppingItem
import com.fitcoach.app.domain.nutrition.ShoppingListEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lista della spesa settimanale, derivata dal piano alimentare EFFETTIVO.
 *
 * La lista non e mai un modulo separato: viene rigenerata dal piano attivo, cosi
 * una sostituzione (pollo -> tonno) si riflette automaticamente. Lo stato di
 * spunta e "gia in casa" viene conservato per alimento quando la lista viene
 * rigenerata.
 */
@Singleton
class ShoppingRepository @Inject constructor(
    private val db: FitCoachDatabase,
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository
) {
    private val shoppingDao = db.shoppingItemDao()

    fun observeWeek(weekStart: Long): Flow<List<ShoppingItem>> =
        shoppingDao.observeForWeek(weekStart).map { list -> list.map { it.toDomain() } }

    suspend fun itemsForWeek(weekStart: Long): List<ShoppingItem> =
        shoppingDao.getForWeek(weekStart).map { it.toDomain() }

    suspend fun weeks(): List<Long> = shoppingDao.getWeeks()

    /**
     * Rigenera la lista per la settimana indicata dal piano attivo, conservando
     * spunta e "gia in casa" per gli alimenti ancora presenti. Ritorna gli
     * alimenti il cui stato spuntato e stato perso (sostituzione), da segnalare.
     */
    suspend fun regenerate(weekStart: Long): List<String> = db.withTransaction {
        val plan = nutritionRepository.getActivePlan()
            ?: return@withTransaction emptyList()
        val foodsById: Map<Long, FoodItem> = foodRepository.getAll().associateBy { it.id }
        val previous = shoppingDao.getForWeek(weekStart).map { it.toDomain() }

        val updated = ShoppingListEngine.aggregate(plan, weekStart, foodsById, previous)
        val invalidated = ShoppingListEngine.invalidatedStates(previous, updated)

        // Conserva le voci manuali (isCustom) aggiunte dall'utente.
        val custom = previous.filter { it.isCustom }

        shoppingDao.deleteGeneratedForWeek(weekStart)
        shoppingDao.insertAll((updated + custom).map { it.toEntity() })
        invalidated
    }

    suspend fun addManual(weekStart: Long, name: String, grams: Double, categoryName: String?): Long {
        val food = foodRepository.getByName(name)
        val item = ShoppingItem(
            weekStartEpochDay = weekStart,
            foodItemId = food?.id ?: 0L,
            foodName = food?.name ?: name,
            category = categoryName
                ?.let { runCatching { com.fitcoach.app.domain.model.ShoppingCategory.valueOf(it) }.getOrNull() }
                ?: food?.let { com.fitcoach.app.domain.model.ShoppingCategory.fromFoodCategory(it.category) }
                ?: com.fitcoach.app.domain.model.ShoppingCategory.OTHER,
            totalGrams = grams,
            isCustom = true
        )
        return shoppingDao.insert(item.toEntity())
    }

    suspend fun setChecked(id: Long, checked: Boolean) = shoppingDao.setChecked(id, checked)

    suspend fun setAlreadyHave(id: Long, alreadyHave: Boolean) = shoppingDao.setAlreadyHave(id, alreadyHave)

    suspend fun setGrams(id: Long, grams: Double) = shoppingDao.setGrams(id, grams)

    suspend fun delete(id: Long) {
        shoppingDao.getById(id)?.let { shoppingDao.delete(it) }
    }

    suspend fun clearWeek(weekStart: Long) = shoppingDao.deleteForWeek(weekStart)
}
