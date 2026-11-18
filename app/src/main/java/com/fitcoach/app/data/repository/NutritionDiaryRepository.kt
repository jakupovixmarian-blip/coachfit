package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.domain.model.DailyBalance
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.DiarySource
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.WeeklyNutritionSummary
import com.fitcoach.app.domain.nutrition.NutritionDiaryEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Diario nutrizionale: registra cosa l'utente ha realmente mangiato, sia dal
 * piano (spunta di una voce prevista) sia fuori piano (alimenti extra).
 *
 * Le voci del piano collegate (planEntryId non nullo) non vengono mai conteggiate
 * due volte: la spunta crea/riattiva una sola voce per ciascuna voce di piano.
 */
@Singleton
class NutritionDiaryRepository @Inject constructor(
    private val db: FitCoachDatabase
) {
    private val diaryDao = db.diaryEntryDao()
    private val foodDao = db.foodDao()
    private val planDao = db.nutritionPlanDao()
    private val dayDao = db.nutritionDayDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntry>> =
        planDao.observeActive().flatMapLatest { plan ->
            kotlinx.coroutines.flow.flow {
                diaryDao.observeForDay(epochDay).collect { entries ->
                    emit(entries.filterForActivePlan(plan?.id).map { it.toDomain() })
                }
            }
        }

    suspend fun entriesForDay(epochDay: Long): List<DiaryEntry> {
        val activePlanId = planDao.getActive()?.id
        return diaryDao.getForDay(epochDay)
            .filterForActivePlan(activePlanId)
            .map { it.toDomain() }
    }

    /** Mappa planEntryId -> voce del diario del SOLO piano attivo. */
    suspend fun planChecks(epochDay: Long): Map<Long, DiaryEntry> {
        val activePlanId = planDao.getActive()?.id
        return diaryDao.getForDay(epochDay)
            .filterForActivePlan(activePlanId)
            .filter { it.planEntryId != null && it.active }
            .associate { it.planEntryId!! to it.toDomain() }
    }

    /** Bilancio del giorno: obiettivo del piano vs consumato reale (voci attive). */
    suspend fun balanceForDay(epochDay: Long): DailyBalance {
        val plan = activePlanDomain()
        val dayIndex = NutritionDiaryEngine.dayIndexFor(epochDay)
        return NutritionDiaryEngine.dailyBalance(plan, dayIndex, epochDay, entriesForDay(epochDay))
    }

    suspend fun weeklySummary(weekStartEpochDay: Long): WeeklyNutritionSummary {
        val plan = activePlanDomain()
        val entries = diaryDao.getBetween(weekStartEpochDay, weekStartEpochDay + 6)
            .filterForActivePlan(plan?.id)
            .map { it.toDomain() }
        return NutritionDiaryEngine.weeklySummary(plan, entries, weekStartEpochDay)
    }

    /**
     * Spunta/despunta una voce del piano. Alla spunta crea la voce del diario con
     * i valori previsti; alla despunta la disattiva (resta nello storico).
     */
    suspend fun togglePlanEntry(entry: MealEntry, slot: MealSlot, time: String, epochDay: Long) {
        db.withTransaction {
            val existing = diaryDao.getByPlanEntry(entry.id, epochDay)
            if (existing != null) {
                diaryDao.setActive(existing.id, !existing.active)
                return@withTransaction
            }
            diaryDao.insert(
                DiaryEntry(
                    dateEpochDay = epochDay,
                    mealSlot = slot,
                    time = time,
                    foodItemId = entry.foodItemId,
                    foodName = entry.foodName,
                    grams = entry.grams,
                    unit = entry.unit,
                    kcal = entry.kcal,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat,
                    source = DiarySource.PLAN,
                    planEntryId = entry.id,
                    nutritionPlanId = planDao.getActive()?.id,
                    active = true
                ).toEntity()
            )
        }
    }

    /** Tutte le voci del diario del piano attivo (per le statistiche storiche). */
    suspend fun allEntries(): List<DiaryEntry> {
        val activePlanId = planDao.getActive()?.id
        return diaryDao.getAll().filterForActivePlan(activePlanId).map { it.toDomain() }
    }

    /** Conferma il pasto previsto: attiva (senza invertire) la voce di piano del giorno. */
    suspend fun confirmPlanEntry(entry: MealEntry, slot: MealSlot, time: String, epochDay: Long) {
        db.withTransaction {
            val existing = diaryDao.getByPlanEntry(entry.id, epochDay)
            if (existing != null) {
                if (!existing.active) diaryDao.setActive(existing.id, true)
                return@withTransaction
            }
            diaryDao.insert(
                DiaryEntry(
                    dateEpochDay = epochDay,
                    mealSlot = slot,
                    time = time,
                    foodItemId = entry.foodItemId,
                    foodName = entry.foodName,
                    grams = entry.grams,
                    unit = entry.unit,
                    kcal = entry.kcal,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat,
                    source = DiarySource.PLAN,
                    planEntryId = entry.id,
                    nutritionPlanId = planDao.getActive()?.id,
                    active = true
                ).toEntity()
            )
        }
    }

    /** Aggiunge un alimento fuori piano, calcolando i valori dalla quantita. */
    suspend fun addExtraFood(food: FoodItem, grams: Double, slot: MealSlot, time: String, epochDay: Long): Long {
        val factor = grams / 100.0
        return diaryDao.insert(
            DiaryEntry(
                dateEpochDay = epochDay,
                mealSlot = slot,
                time = time,
                foodItemId = food.id,
                foodName = food.name,
                grams = grams,
                unit = food.defaultUnit,
                kcal = food.kcalPer100g * factor,
                protein = food.proteinPer100g * factor,
                carbs = food.carbsPer100g * factor,
                fat = food.fatPer100g * factor,
                source = DiarySource.EXTRA,
                planEntryId = null,
                active = true
            ).toEntity()
        )
    }

    /** Modifica la quantita consumata di una voce, ricalcolando i valori nutrizionali. */
    suspend fun updateGrams(entryId: Long, grams: Double) {
        val entity = diaryDao.getById(entryId) ?: return
        val food = foodDao.getById(entity.foodItemId)?.toDomain()
        val factor = grams / 100.0
        val updated = if (food != null) {
            entity.copy(
                grams = grams,
                kcal = food.kcalPer100g * factor,
                protein = food.proteinPer100g * factor,
                carbs = food.carbsPer100g * factor,
                fat = food.fatPer100g * factor
            )
        } else {
            // Alimento non piu presente nel DB: riscala proporzionalmente i valori salvati.
            val ratio = if (entity.grams > 0) grams / entity.grams else 1.0
            entity.copy(
                grams = grams,
                kcal = entity.kcal * ratio,
                protein = entity.protein * ratio,
                carbs = entity.carbs * ratio,
                fat = entity.fat * ratio
            )
        }
        diaryDao.update(updated)
    }

    suspend fun setActive(entryId: Long, active: Boolean) = diaryDao.setActive(entryId, active)

    suspend fun moveToMeal(entryId: Long, slot: MealSlot) {
        val entity = diaryDao.getById(entryId) ?: return
        diaryDao.update(entity.copy(mealSlot = slot))
    }

    suspend fun delete(entryId: Long) {
        val entity = diaryDao.getById(entryId) ?: return
        diaryDao.delete(entity)
    }

    private fun List<com.fitcoach.app.data.local.entity.DiaryEntryEntity>.filterForActivePlan(
        activePlanId: Long?
    ): List<com.fitcoach.app.data.local.entity.DiaryEntryEntity> = filter {
        it.source != DiarySource.PLAN || (it.nutritionPlanId != null && it.nutritionPlanId == activePlanId)
    }

    /** Riporta al piano lo stato dei consumi: disattiva tutte le voci di piano del giorno. */
    suspend fun resetPlanChecks(epochDay: Long) = diaryDao.deletePlanEntriesForDay(epochDay)

    private suspend fun activePlanDomain(): com.fitcoach.app.domain.model.NutritionPlan? {
        val plan = planDao.getActive() ?: return null
        val days = dayDao.getForPlan(plan.id)
        return com.fitcoach.app.domain.model.NutritionPlan(
            id = plan.id,
            profileId = plan.profileId,
            name = plan.name,
            createdAt = plan.createdAt,
            isActive = plan.isActive,
            bmr = plan.bmr,
            tdee = plan.tdee,
            weeklyCalories = plan.weeklyCalories,
            weeklyKcalBalance = plan.weeklyKcalBalance,
            bmrFormula = plan.bmrFormula,
            tdeeFormula = plan.tdeeFormula,
            highCarbDays = plan.highCarbDays,
            mediumCarbDays = plan.mediumCarbDays,
            lowCarbDays = plan.lowCarbDays,
            proteinGPerKg = plan.proteinGPerKg,
            carbCyclingApplied = plan.carbCyclingApplied,
            version = plan.version,
            days = days.map { d ->
                com.fitcoach.app.domain.model.NutritionDay(
                    id = d.id,
                    planId = d.planId,
                    dayIndex = d.dayIndex,
                    dayType = d.dayType,
                    targetKcal = d.targetKcal,
                    targetProtein = d.targetProtein,
                    targetCarbs = d.targetCarbs,
                    targetFat = d.targetFat,
                    workoutDayIndex = d.workoutDayIndex
                )
            }
        )
    }

    companion object {
        fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

        /** Lunedì della settimana della data indicata. */
        fun weekStart(epochDay: Long): Long {
            val date = LocalDate.ofEpochDay(epochDay)
            return date.minusDays((date.dayOfWeek.value - 1).toLong()).toEpochDay()
        }
    }
}
