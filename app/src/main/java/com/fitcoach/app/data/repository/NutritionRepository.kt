package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.entity.MealEntity
import com.fitcoach.app.data.local.entity.MealEntryEntity
import com.fitcoach.app.data.local.entity.NutritionDayEntity
import com.fitcoach.app.data.local.entity.NutritionPlanEntity
import com.fitcoach.app.domain.model.CarbCyclingPlan
import com.fitcoach.app.domain.model.EnergyResult
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    private val db: FitCoachDatabase
) {
    private val planDao = db.nutritionPlanDao()
    private val dayDao = db.nutritionDayDao()
    private val mealDao = db.mealDao()
    private val entryDao = db.mealEntryDao()

    val activePlanFlow: Flow<NutritionPlan?> = planDao.observeActive().map { entity ->
        entity?.let { loadFullPlan(it.id) }
    }

    suspend fun getActivePlan(): NutritionPlan? {
        normalizeActivePlan()
        val entity = planDao.getActive() ?: return null
        return loadFullPlan(entity.id)
    }

    /** Tutti i piani salvati (AI e manuali), dal piu recente. */
    suspend fun getAllPlans(): List<NutritionPlan> {
        normalizeActivePlan()
        return planDao.getAll().mapNotNull { loadFullPlan(it.id) }
    }

    /** Rende attivo un piano esistente (avvio del piano scelto dall'utente). */
    suspend fun setActivePlan(planId: Long) = db.withTransaction {
        if (planDao.getById(planId) == null) return@withTransaction
        planDao.deactivateAll()
        planDao.setActive(planId, true)
    }

    /** Ripara database creati da versioni precedenti che lasciavano piu piani attivi. */
    private suspend fun normalizeActivePlan() = db.withTransaction {
        val all = planDao.getAll()
        val active = all.filter { it.isActive }
        val keep = when {
            active.size == 1 -> null
            active.isNotEmpty() -> active.maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
            else -> all.maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
        }
        if (keep != null) {
            planDao.deactivateAll()
            planDao.setActive(keep.id, true)
        }
    }

    suspend fun renamePlan(planId: Long, name: String): Boolean {
        val plan = planDao.getById(planId) ?: return false
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        planDao.update(plan.copy(name = normalized))
        return true
    }

    suspend fun deletePlan(planId: Long): Boolean = db.withTransaction {
        val plan = planDao.getById(planId) ?: return@withTransaction false
        planDao.delete(planId)
        if (plan.isActive) {
            planDao.getAll().maxWithOrNull(compareBy({ it.createdAt }, { it.id }))?.let {
                planDao.deactivateAll()
                planDao.setActive(it.id, true)
            }
        }
        true
    }

    /**
     * Crea un piano alimentare MANUALE: 7 giorni con i pasti vuoti e i target
     * calcolati dai motori deterministici (formule documentate, non valori
     * inventati). L'utente compila poi i pasti a mano dalla schermata
     * Alimentazione. I target restano modificabili giorno per giorno.
     */
    suspend fun createManualPlan(
        profile: UserProfile,
        energy: EnergyResult,
        cycling: CarbCyclingPlan,
        dayTypes: List<com.fitcoach.app.domain.model.CarbDayType>,
        trainingDayIndices: List<Int>
    ): Long = db.withTransaction {
        val previousVersion = planDao.getActive()?.version ?: 0
        planDao.deactivateAll()
        val planId = planDao.insert(
            NutritionPlanEntity(
                profileId = profile.id,
                name = "Piano manuale – ${java.time.LocalDate.now()}",
                isActive = true,
                bmr = energy.bmr,
                tdee = energy.tdee,
                weeklyCalories = cycling.weeklyKcal,
                weeklyKcalBalance = cycling.weeklyKcal - energy.tdee * 7,
                bmrFormula = energy.bmrFormula,
                tdeeFormula = energy.tdeeFormula,
                highCarbDays = cycling.highDays,
                mediumCarbDays = cycling.mediumDays,
                lowCarbDays = cycling.lowDays,
                proteinGPerKg = if (profile.weightKg != null && profile.weightKg > 0) {
                    cycling.targets.values.first().protein / profile.weightKg
                } else 0.0,
                carbCyclingApplied = true,
                version = previousVersion + 1
            )
        )
        dayTypes.forEachIndexed { dayIndex, dayType ->
            val target = cycling.targets[dayType]
                ?: com.fitcoach.app.domain.model.CarbDayTarget(
                    type = dayType, kcal = energy.tdee, protein = 0.0, carbs = 0.0, fat = 0.0
                )
            val dayId = dayDao.insertAll(
                listOf(
                    NutritionDayEntity(
                        planId = planId,
                        dayIndex = dayIndex,
                        dayType = dayType,
                        targetKcal = target.kcal,
                        targetProtein = target.protein,
                        targetCarbs = target.carbs,
                        targetFat = target.fat,
                        workoutDayIndex = trainingDayIndices.getOrNull(dayIndex)
                            ?.takeIf { it == dayIndex }
                    )
                )
            ).first()
            // Pasti vuoti pronti da riempire manualmente: nessun alimento inventato.
            val slots = com.fitcoach.app.domain.model.MealSlot.entries.take(profile.mealsPerDay.coerceIn(3, 6))
            mealDao.insertAll(slots.map { MealEntity(nutritionDayId = dayId, slot = it, time = it.defaultTime) })
        }
        planId
    }

    suspend fun savePlan(
        profile: UserProfile,
        energy: EnergyResult,
        cycling: CarbCyclingPlan,
        generatedDays: List<MealPlanGenerator.GeneratedDay>,
        dayTypes: List<com.fitcoach.app.domain.model.CarbDayType>,
        trainingDayIndices: List<Int>,
        carbCyclingApplied: Boolean = true
    ): Long = db.withTransaction {
        val previousVersion = planDao.getActive()?.version ?: 0
        planDao.deactivateAll()
        val planId = planDao.insert(
            NutritionPlanEntity(
                profileId = profile.id,
                name = "Piano ${profile.goal.label} – ${java.time.LocalDate.now()}",
                isActive = true,
                bmr = energy.bmr,
                tdee = energy.tdee,
                weeklyCalories = cycling.weeklyKcal,
                weeklyKcalBalance = cycling.weeklyKcal - energy.tdee * 7,
                bmrFormula = energy.bmrFormula,
                tdeeFormula = energy.tdeeFormula,
                highCarbDays = cycling.highDays,
                mediumCarbDays = cycling.mediumDays,
                lowCarbDays = cycling.lowDays,
                proteinGPerKg = if (profile.weightKg != null && profile.weightKg > 0) {
                    cycling.targets.values.first().protein / profile.weightKg
                } else 0.0,
                carbCyclingApplied = carbCyclingApplied,
                version = previousVersion + 1
            )
        )

        generatedDays.forEach { day ->
            val target = day.target
            val dayId = dayDao.insertAll(
                listOf(
                    NutritionDayEntity(
                        planId = planId,
                        dayIndex = day.dayIndex,
                        dayType = day.dayType,
                        targetKcal = target.kcal,
                        targetProtein = target.protein,
                        targetCarbs = target.carbs,
                        targetFat = target.fat,
                        workoutDayIndex = trainingDayIndices.getOrNull(day.dayIndex)
                            ?.takeIf { it == day.dayIndex }
                    )
                )
            ).first()

            day.meals.forEach { meal ->
                val mealId = mealDao.insertAll(
                    listOf(MealEntity(nutritionDayId = dayId, slot = meal.slot, time = meal.slot.defaultTime))
                ).first()
                entryDao.insertAll(
                    meal.entries.map { e ->
                        MealEntryEntity(
                            mealId = mealId,
                            foodItemId = e.foodItemId,
                            foodName = e.foodName,
                            grams = e.grams,
                            unit = e.unit,
                            kcal = e.kcal,
                            protein = e.protein,
                            carbs = e.carbs,
                            fat = e.fat
                        )
                    }
                )
            }
        }
        planId
    }

    suspend fun updatePlanMacros(planId: Long, weeklyCalories: Double, weeklyBalance: Double) {
        val existing = planDao.getById(planId) ?: return
        planDao.update(existing.copy(weeklyCalories = weeklyCalories, weeklyKcalBalance = weeklyBalance))
    }

    suspend fun setDayTargets(dayId: Long, kcal: Double, protein: Double, carbs: Double, fat: Double) =
        dayDao.updateTargets(dayId, kcal, protein, carbs, fat)

    suspend fun confirmMeal(mealId: Long, confirmed: Boolean) = mealDao.setConfirmed(mealId, confirmed)

    suspend fun replaceMealEntry(entryId: Long, grams: Double, kcal: Double, protein: Double, carbs: Double, fat: Double) =
        entryDao.updateEntry(entryId, grams, kcal, protein, carbs, fat)

    /** Crea una nuova versione del piano senza mutare la precedente. */
    suspend fun updateEntryAsNewPlanVersion(
        planId: Long,
        entryId: Long,
        grams: Double,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        diaryEpochDay: Long
    ): Boolean = db.withTransaction {
        val currentPlan = planDao.getById(planId) ?: return@withTransaction false
        val oldDays = dayDao.getForPlan(planId)
        val oldMeals = oldDays.flatMap { mealDao.getForDay(it.id) }
        val entriesByMeal = oldMeals.associateWith { meal -> entryDao.getForMeal(meal.id) }
        if (entriesByMeal.values.flatten().none { it.id == entryId }) return@withTransaction false

        planDao.deactivateAll()
        val newPlanId = planDao.insert(
            currentPlan.copy(
                id = 0L,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                version = currentPlan.version + 1
            )
        )
        var newEntryId: Long? = null
        oldDays.forEach { oldDay ->
            val newDayId = dayDao.insertAll(listOf(oldDay.copy(id = 0L, planId = newPlanId))).first()
            mealDao.getForDay(oldDay.id).forEach { oldMeal ->
                val newMealId = mealDao.insertAll(
                    listOf(oldMeal.copy(id = 0L, nutritionDayId = newDayId))
                ).first()
                val oldEntries = entriesByMeal[oldMeal].orEmpty()
                if (oldEntries.isNotEmpty()) {
                    val newEntries = oldEntries.map { oldEntry ->
                        if (oldEntry.id == entryId) {
                            oldEntry.copy(
                                id = 0L,
                                mealId = newMealId,
                                grams = grams,
                                kcal = kcal,
                                protein = protein,
                                carbs = carbs,
                                fat = fat
                            )
                        } else oldEntry.copy(id = 0L, mealId = newMealId)
                    }
                    val newIds = entryDao.insertAll(newEntries)
                    oldEntries.zip(newIds).firstOrNull { it.first.id == entryId }?.let {
                        newEntryId = it.second
                    }
                }
            }
        }
        val copiedEntryId = newEntryId ?: return@withTransaction false
        db.diaryEntryDao().relinkPlanEntryForDate(entryId, copiedEntryId, newPlanId, diaryEpochDay)
        true
    }

    private suspend fun clonePlanVersion(
        planId: Long,
        dayTransform: (NutritionDayEntity) -> NutritionDayEntity = { it },
        mealTransform: (MealEntity) -> MealEntity = { it },
        entryTransform: (MealEntryEntity) -> MealEntryEntity = { it }
    ): Pair<Long, Map<Long, Long>>? = db.withTransaction {
        val currentPlan = planDao.getById(planId) ?: return@withTransaction null
        val oldDays = dayDao.getForPlan(planId)
        planDao.deactivateAll()
        val newPlanId = planDao.insert(
            currentPlan.copy(
                id = 0L,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                version = currentPlan.version + 1
            )
        )
        val entryMap = mutableMapOf<Long, Long>()
        oldDays.forEach { oldDay ->
            val newDayId = dayDao.insertAll(
                listOf(dayTransform(oldDay.copy(id = 0L, planId = newPlanId)))
            ).first()
            mealDao.getForDay(oldDay.id).forEach { oldMeal ->
                val newMealId = mealDao.insertAll(
                    listOf(mealTransform(oldMeal.copy(id = 0L, nutritionDayId = newDayId)))
                ).first()
                val oldEntries = entryDao.getForMeal(oldMeal.id)
                if (oldEntries.isNotEmpty()) {
                    val newEntries = oldEntries.map { oldEntry ->
                        entryTransform(oldEntry.copy(id = 0L, mealId = newMealId))
                    }
                    val newIds = entryDao.insertAll(newEntries)
                    oldEntries.zip(newIds).forEach { (oldEntry, newEntryId) ->
                        entryMap[oldEntry.id] = newEntryId
                    }
                }
            }
        }
        newPlanId to entryMap
    }

    private suspend fun relinkCurrentDiary(
        oldToNewEntries: Map<Long, Long>,
        newPlanId: Long,
        epochDay: Long
    ) {
        val diaryDao = db.diaryEntryDao()
        oldToNewEntries.forEach { (oldId, newId) ->
            diaryDao.relinkPlanEntryForDate(oldId, newId, newPlanId, epochDay)
        }
    }

    suspend fun updateDayTargetsAsNewPlanVersion(
        planId: Long,
        dayId: Long,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        diaryEpochDay: Long
    ): Boolean {
        if (kcal <= 0 || protein < 0 || carbs < 0 || fat < 0) return false
        val result = clonePlanVersion(
            planId = planId,
            dayTransform = { day ->
                if (day.id == dayId) day.copy(
                    targetKcal = kcal,
                    targetProtein = protein,
                    targetCarbs = carbs,
                    targetFat = fat
                ) else day
            }
        ) ?: return false
        relinkCurrentDiary(result.second, result.first, diaryEpochDay)
        return true
    }

    suspend fun updateDayTypeAsNewPlanVersion(
        planId: Long,
        dayId: Long,
        dayType: com.fitcoach.app.domain.model.CarbDayType,
        diaryEpochDay: Long
    ): Boolean {
        val result = clonePlanVersion(
            planId = planId,
            dayTransform = { day -> if (day.id == dayId) day.copy(dayType = dayType) else day }
        ) ?: return false
        relinkCurrentDiary(result.second, result.first, diaryEpochDay)
        return true
    }

    suspend fun updateMealScheduleAsNewPlanVersion(
        planId: Long,
        mealId: Long,
        slot: com.fitcoach.app.domain.model.MealSlot,
        time: String,
        diaryEpochDay: Long
    ): Boolean {
        if (!Regex("^([01]\\\\d|2[0-3]):[0-5]\\\\d$").matches(time)) return false
        val result = clonePlanVersion(
            planId = planId,
            mealTransform = { meal ->
                if (meal.id == mealId) meal.copy(slot = slot, time = time) else meal
            }
        ) ?: return false
        relinkCurrentDiary(result.second, result.first, diaryEpochDay)
        return true
    }



    /** Duplica un piano creando una nuova versione attiva e lasciando intatto l'originale. */
    suspend fun duplicatePlan(planId: Long): Long? = clonePlanVersion(planId)?.first


    suspend fun replaceEntryWithFood(
        entryId: Long,
        newFoodId: Long,
        newFoodName: String,
        grams: Double,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double
    ) = entryDao.replaceEntry(entryId, newFoodId, newFoodName, grams, kcal, protein, carbs, fat)

    /** Aggiunge manualmente un alimento a un pasto, calcolando i macro dai grammi. */
    suspend fun addEntryToMeal(
        mealId: Long,
        food: com.fitcoach.app.domain.model.FoodItem,
        grams: Double,
        unit: com.fitcoach.app.domain.model.FoodUnit
    ): Long {
        val factor = grams / 100.0
        return entryDao.insertAll(
            listOf(
                MealEntryEntity(
                    mealId = mealId,
                    foodItemId = food.id,
                    foodName = food.name,
                    grams = grams,
                    unit = unit,
                    kcal = food.kcalPer100g * factor,
                    protein = food.proteinPer100g * factor,
                    carbs = food.carbsPer100g * factor,
                    fat = food.fatPer100g * factor
                )
            )
        ).firstOrNull() ?: 0L
    }

    /** Rimuove manualmente un alimento dal pasto. */
    suspend fun removeEntry(entry: com.fitcoach.app.domain.model.MealEntry) {
        entryDao.delete(
            MealEntryEntity(
                id = entry.id,
                mealId = entry.mealId,
                foodItemId = entry.foodItemId,
                foodName = entry.foodName,
                grams = entry.grams,
                unit = entry.unit,
                kcal = entry.kcal,
                protein = entry.protein,
                carbs = entry.carbs,
                fat = entry.fat
            )
        )
    }

    suspend fun getDayWithMeals(dayId: Long): NutritionDay? {
        val day = dayDao.getById(dayId) ?: return null
        return day.toDomainWithMeals()
    }

    private suspend fun NutritionDayEntity.toDomainWithMeals(): NutritionDay {
        val mealEntities = mealDao.getForDay(id)
        val meals = mealEntities.map { m ->
            Meal(
                id = m.id,
                nutritionDayId = m.nutritionDayId,
                slot = m.slot,
                time = m.time,
                entries = entryDao.getForMeal(m.id).map { it.toDomain() }
            )
        }
        return NutritionDay(
            id = id,
            planId = planId,
            dayIndex = dayIndex,
            dayType = dayType,
            targetKcal = targetKcal,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFat = targetFat,
            meals = meals,
            workoutDayIndex = workoutDayIndex
        )
    }

    private suspend fun loadFullPlan(planId: Long): NutritionPlan? {
        val entity = planDao.getById(planId) ?: return null
        val days = dayDao.getForPlan(planId).map { it.toDomainWithMeals() }
        return NutritionPlan(
            id = entity.id,
            profileId = entity.profileId,
            name = entity.name,
            createdAt = entity.createdAt,
            isActive = entity.isActive,
            bmr = entity.bmr,
            tdee = entity.tdee,
            weeklyCalories = entity.weeklyCalories,
            weeklyKcalBalance = entity.weeklyKcalBalance,
            bmrFormula = entity.bmrFormula,
            tdeeFormula = entity.tdeeFormula,
            highCarbDays = entity.highCarbDays,
            mediumCarbDays = entity.mediumCarbDays,
            lowCarbDays = entity.lowCarbDays,
            proteinGPerKg = entity.proteinGPerKg,
            carbCyclingApplied = entity.carbCyclingApplied,
            version = entity.version,
            days = days
        )
    }

    private fun com.fitcoach.app.data.local.entity.MealEntryEntity.toDomain() =
        com.fitcoach.app.domain.model.MealEntry(
            id = id, mealId = mealId, foodItemId = foodItemId, foodName = foodName,
            grams = grams, unit = unit, kcal = kcal, protein = protein, carbs = carbs, fat = fat
        )
}
