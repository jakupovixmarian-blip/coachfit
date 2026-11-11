package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.BudgetScope
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BudgetEngineTest {

    private fun food(id: Long, name: String, price: Double?, protein: Double = 20.0) = FoodItem(
        id = id, name = name, category = "Carne", kcalPer100g = 150.0,
        proteinPer100g = protein, carbsPer100g = 0.0, fatPer100g = 5.0,
        source = "test", pricePerKgEur = price
    )

    private fun planWith(vararg grams: Pair<Long, Double>): NutritionPlan {
        val entries = grams.map { (foodId, g) ->
            MealEntry(foodItemId = foodId, foodName = "f$foodId", grams = g, unit = FoodUnit.RAW,
                kcal = g * 1.5, protein = g * 0.2, carbs = 0.0, fat = g * 0.05)
        }
        return NutritionPlan(
            name = "test", bmr = 1800.0, tdee = 2600.0, weeklyCalories = 18200.0,
            weeklyKcalBalance = 0.0, bmrFormula = "", tdeeFormula = "",
            highCarbDays = 2, mediumCarbDays = 3, lowCarbDays = 2, proteinGPerKg = 2.0,
            days = (0 until 7).map { d ->
                NutritionDay(
                    dayIndex = d, dayType = CarbDayType.MEDIUM, targetKcal = 2600.0,
                    targetProtein = 160.0, targetCarbs = 300.0, targetFat = 70.0,
                    meals = listOf(Meal(slot = MealSlot.LUNCH, time = "13:00", entries = entries))
                )
            }
        )
    }

    @Test
    fun `cost uses real gram weights and prices`() {
        // 200 g al giorno x 7 = 1.4 kg x 10 €/kg = 14 €
        val plan = planWith(1L to 200.0)
        val report = BudgetEngine.report(plan, mapOf(1L to food(1, "Pollo", 10.0)), UserProfile())
        assertThat(report.weeklyCostEur).isWithin(0.01).of(14.0)
        assertThat(report.partialEstimate).isFalse()
    }

    @Test
    fun `missing price yields partial estimate and no invented cost`() {
        val plan = planWith(1L to 200.0)
        val report = BudgetEngine.report(plan, mapOf(1L to food(1, "Pollo", null)), UserProfile())
        assertThat(report.weeklyCostEur).isEqualTo(0.0)
        assertThat(report.partialEstimate).isTrue()
        assertThat(report.unpricedFoods).containsExactly("f1")
    }

    @Test
    fun `personal budget compares monthly cost`() {
        val plan = planWith(1L to 200.0)
        val profile = UserProfile(monthlyFoodBudgetEur = 20.0, budgetScope = BudgetScope.PERSONAL_ONLY)
        val report = BudgetEngine.report(plan, mapOf(1L to food(1, "Pollo", 10.0)), profile)
        assertThat(report.monthlyBudgetEur).isWithin(0.01).of(20.0)
        assertThat(report.withinBudget).isFalse()
        assertThat(report.overBudgetEur).isGreaterThan(0.0)
    }

    @Test
    fun `shared budget attributes per capita share`() {
        val profile = UserProfile(
            monthlyFoodBudgetEur = 400.0, budgetScope = BudgetScope.SHARED_FOOD, budgetPersons = 4
        )
        assertThat(BudgetEngine.attributableBudget(profile)!!).isWithin(0.01).of(100.0)
    }

    @Test
    fun `whole shopping budget estimates food share`() {
        val profile = UserProfile(monthlyFoodBudgetEur = 500.0, budgetScope = BudgetScope.WHOLE_SHOPPING)
        assertThat(BudgetEngine.attributableBudget(profile)!!).isWithin(0.01).of(350.0)
    }

    @Test
    fun `no budget means not binding`() {
        val plan = planWith(1L to 200.0)
        val report = BudgetEngine.report(plan, mapOf(1L to food(1, "Pollo", 10.0)), UserProfile())
        assertThat(report.monthlyBudgetEur).isNull()
        assertThat(report.withinBudget).isTrue()
    }

    @Test
    fun `cost per protein gram computed only with price and protein`() {
        val withPrice = food(1, "Pollo", 10.0, protein = 25.0)
        assertThat(BudgetEngine.costPerProteinGram(withPrice)!!).isWithin(0.001).of(0.04)
        assertThat(BudgetEngine.costPerProteinGram(food(2, "X", null))).isNull()
        assertThat(BudgetEngine.costPerProteinGram(food(3, "Y", 10.0, protein = 0.0))).isNull()
    }
}
