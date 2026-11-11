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
        val entity = planDao.getActive() ?: return null
        return loadFullPlan(entity.id)
    }

    suspend fun savePlan(
        profile: UserProfile,
        energy: EnergyResult,
        cycling: CarbCyclingPlan,
        generatedDays: List<MealPlanGenerator.GeneratedDay>,
        dayTypes: List<com.fitcoach.app.domain.model.CarbDayType>,
        trainingDayIndices: List<Int>
    ): Long = db.withTransaction {
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
                } else 0.0
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
            days = days
        )
    }

    private fun com.fitcoach.app.data.local.entity.MealEntryEntity.toDomain() =
        com.fitcoach.app.domain.model.MealEntry(
            id = id, mealId = mealId, foodItemId = foodItemId, foodName = foodName,
            grams = grams, unit = unit, kcal = kcal, protein = protein, carbs = carbs, fat = fat
        )
}
