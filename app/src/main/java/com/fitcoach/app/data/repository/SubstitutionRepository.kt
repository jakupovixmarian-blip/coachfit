package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.SubstitutionLog
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSubstitutionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sostituzione alimentare VINCOLATA: nessun alimento viene mai scambiato "a
 * parita di grammi". Il motore matematico ([FoodSubstitutionEngine]) calcola la
 * quantita del sostituto per mantenere i macro del pasto e verifica calorie e
 * macronutrienti dell'intero pasto e della giornata. Ogni applicazione e
 * transazionale e registrata nello storico delle sostituzioni.
 */
@Singleton
class SubstitutionRepository @Inject constructor(
    private val db: FitCoachDatabase,
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository
) {
    private val logDao = db.substitutionLogDao()
    private val entryDao = db.mealEntryDao()

    fun observeHistory(): Flow<List<SubstitutionLog>> =
        logDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun history(): List<SubstitutionLog> = logDao.getAll().map { it.toDomain() }

    /** Sostituti candidati compatibili con le restrizioni del profilo. */
    suspend fun candidates(original: FoodItem, profile: UserProfile?): List<FoodItem> {
        val all = foodRepository.getAll()
        return if (profile != null) {
            FoodFilter.findSubstitutes(original, all, profile)
        } else {
            all.filter { it.id != original.id }.take(12)
        }
    }

    /**
     * Precalcola la sostituzione per la voce [entry] all'interno della giornata
     * [dayIndex] del piano attivo, con il sostituto [substitute]. Non modifica
     * nulla: serve a mostrare il controllo preventivo (prima/dopo).
     */
    suspend fun preview(entryId: Long, dayIndex: Int, substitute: FoodItem): FoodSubstitutionEngine.Result? {
        val plan = nutritionRepository.getActivePlan() ?: return null
        val day = plan.days.firstOrNull { it.dayIndex == dayIndex } ?: return null
        val meal = day.meals.firstOrNull { m -> m.entries.any { it.id == entryId } } ?: return null
        val entry = meal.entries.firstOrNull { it.id == entryId } ?: return null
        val original = foodRepository.getById(entry.foodItemId)
            ?: foodRepository.getByName(entry.foodName)
            ?: return null

        val mealOther = sum(meal.entries.filter { it.id != entryId })
        val dayPlannedOther = sum(day.meals.flatMap { m -> m.entries }.filter { it.id != entryId })
        val dayTarget = MacroTotals(day.targetKcal, day.targetProtein, day.targetCarbs, day.targetFat)

        return FoodSubstitutionEngine.evaluate(
            FoodSubstitutionEngine.Request(
                original = original,
                originalGrams = entry.grams,
                substitute = substitute,
                mealOther = mealOther,
                dayPlannedOther = dayPlannedOther,
                dayTarget = dayTarget,
                fatBalancers = balancerFoods("Grassi"),
                carbBalancers = balancerFoods("Cereali")
            )
        )
    }

    /**
     * Applica la sostituzione approvata: aggiorna la voce del pasto con la nuova
     * quantita calcolata e registra lo storico. Ritorna l'id del log.
     */
    suspend fun apply(
        entryId: Long,
        dayIndex: Int,
        epochDay: Long,
        substitute: FoodItem,
        result: FoodSubstitutionEngine.Result
    ): Long = db.withTransaction {
        val plan = nutritionRepository.getActivePlan()
            ?: throw IllegalStateException("Nessun piano attivo.")
        val day = plan.days.firstOrNull { it.dayIndex == dayIndex }
            ?: throw IllegalStateException("Giorno non trovato.")
        val meal = day.meals.firstOrNull { m -> m.entries.any { it.id == entryId } }
            ?: throw IllegalStateException("Pasto non trovato.")
        val entry = meal.entries.firstOrNull { it.id == entryId }
            ?: throw IllegalStateException("Voce non trovata.")

        val grams = result.substituteGrams
        val macros = result.substituteMacros
        entryDao.replaceEntry(
            id = entryId,
            foodItemId = substitute.id,
            foodName = substitute.name,
            grams = grams,
            kcal = macros.kcal,
            protein = macros.protein,
            carbs = macros.carbs,
            fat = macros.fat
        )

        val before = MacroTotals(entry.kcal, entry.protein, entry.carbs, entry.fat)
        val log = SubstitutionLog(
            dateEpochDay = epochDay,
            mealEntryId = entryId,
            mealSlot = meal.slot,
            originalFoodId = entry.foodItemId,
            originalName = entry.foodName,
            originalGrams = entry.grams,
            newFoodId = substitute.id,
            newName = substitute.name,
            newGrams = grams,
            kcalBefore = before.kcal,
            kcalAfter = macros.kcal,
            proteinBefore = before.protein,
            proteinAfter = macros.protein,
            carbsBefore = before.carbs,
            carbsAfter = macros.carbs,
            fatBefore = before.fat,
            fatAfter = macros.fat,
            status = result.status.name,
            note = result.notes.joinToString(" ")
        )
        logDao.insert(log.toEntity())
    }

    /** Annulla una sostituzione ripristinando l'alimento e la quantita originali. */
    suspend fun revert(logId: Long): Boolean = db.withTransaction {
        val log = logDao.getRecent(500).firstOrNull { it.id == logId } ?: return@withTransaction false
        val entry = entryDao.getById(log.mealEntryId) ?: return@withTransaction false
        val originalFood = foodRepository.getById(log.originalFoodId)
        val originalMacros = if (originalFood != null) {
            val f = log.originalGrams / 100.0
            MacroTotals(
                kcal = originalFood.kcalPer100g * f,
                protein = originalFood.proteinPer100g * f,
                carbs = originalFood.carbsPer100g * f,
                fat = originalFood.fatPer100g * f
            )
        } else {
            MacroTotals(log.kcalBefore, log.proteinBefore, log.carbsBefore, log.fatBefore)
        }
        entryDao.replaceEntry(
            id = log.mealEntryId,
            foodItemId = log.originalFoodId,
            foodName = log.originalName,
            grams = log.originalGrams,
            kcal = originalMacros.kcal,
            protein = originalMacros.protein,
            carbs = originalMacros.carbs,
            fat = originalMacros.fat
        )
        // La voce del diario collegata (se presente) va disattivata per non conteggiare
        // un alimento che non e piu nel piano.
        true
    }

    private suspend fun balancerFoods(category: String): List<FoodItem> =
        foodRepository.getAll().filter { it.category.equals(category, ignoreCase = true) }

    private fun sum(entries: List<MealEntry>): MacroTotals = MacroTotals(
        kcal = entries.sumOf { it.kcal },
        protein = entries.sumOf { it.protein },
        carbs = entries.sumOf { it.carbs },
        fat = entries.sumOf { it.fat }
    )
}
