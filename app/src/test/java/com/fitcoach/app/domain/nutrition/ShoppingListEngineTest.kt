package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.ShoppingItem
import com.fitcoach.app.domain.model.CarbDayType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShoppingListEngineTest {

    private val chicken = FoodItem(
        id = 1, name = "Petto di pollo", category = "Carne",
        kcalPer100g = 110.0, proteinPer100g = 23.3, carbsPer100g = 0.0, fatPer100g = 1.5, source = "CREA"
    )
    private val rice = FoodItem(
        id = 2, name = "Riso bianco (crudo)", category = "Cereali",
        kcalPer100g = 332.0, proteinPer100g = 6.7, carbsPer100g = 80.0, fatPer100g = 0.4, source = "CREA"
    )
    private val foods = mapOf(1L to chicken, 2L to rice)

    private fun entry(food: FoodItem, grams: Double) = MealEntry(
        foodItemId = food.id, foodName = food.name, grams = grams,
        unit = FoodUnit.RAW, kcal = food.kcalPer100g * grams / 100.0,
        protein = food.proteinPer100g * grams / 100.0,
        carbs = food.carbsPer100g * grams / 100.0,
        fat = food.fatPer100g * grams / 100.0
    )

    private fun planWith(gramsPerDay: List<Pair<FoodItem, Double>>): NutritionPlan {
        val days = (0 until 7).map { i ->
            val (food, grams) = gramsPerDay[i % gramsPerDay.size]
            NutritionDay(
                id = (i + 1).toLong(), planId = 1, dayIndex = i, dayType = CarbDayType.MEDIUM,
                targetKcal = 2200.0, targetProtein = 160.0, targetCarbs = 240.0, targetFat = 65.0,
                meals = listOf(
                    Meal(
                        id = (i + 1).toLong(), nutritionDayId = (i + 1).toLong(),
                        slot = MealSlot.LUNCH, time = "13:00",
                        entries = listOf(entry(food, grams))
                    )
                )
            )
        }
        return NutritionPlan(
            id = 1, name = "Test", bmr = 1700.0, tdee = 2600.0,
            weeklyCalories = 15400.0, weeklyKcalBalance = -400.0,
            bmrFormula = "test", tdeeFormula = "test",
            highCarbDays = 2, mediumCarbDays = 2, lowCarbDays = 3, proteinGPerKg = 2.0, days = days
        )
    }

    @Test
    fun `repeated foods are summed into a single item`() {
        // 7 giorni di pollo 150 g = 1050 g in una sola voce.
        val plan = planWith(listOf(chicken to 150.0))
        val items = ShoppingListEngine.aggregate(plan, weekStartEpochDay = 0L, foodsById = foods)
        val chickenItem = items.first { it.foodItemId == 1L }
        assertThat(items).hasSize(1)
        assertThat(chickenItem.totalGrams).isEqualTo(1050.0)
    }

    @Test
    fun `different foods produce separate categorised items`() {
        val plan = planWith(listOf(chicken to 100.0, rice to 50.0))
        val items = ShoppingListEngine.aggregate(plan, 0L, foods)
        assertThat(items.map { it.foodItemId }.toSet()).containsExactly(1L, 2L)
        val riceItem = items.first { it.foodItemId == 2L }
        assertThat(riceItem.category.label).isEqualTo("Cereali e derivati")
    }

    @Test
    fun `checked state is preserved across regeneration`() {
        val plan = planWith(listOf(chicken to 150.0))
        val first = ShoppingListEngine.aggregate(plan, 0L, foods)
            .map { if (it.foodItemId == 1L) it.copy(id = 10, checked = true) else it }
        val regenerated = ShoppingListEngine.aggregate(plan, 0L, foods, previous = first)
        assertThat(regenerated.first { it.foodItemId == 1L }.checked).isTrue()
    }

    @Test
    fun `substitution invalidates checked state of removed food`() {
        val planChicken = planWith(listOf(chicken to 150.0))
        val previous = ShoppingListEngine.aggregate(planChicken, 0L, foods)
            .map { it.copy(checked = true) }
        // Nuovo piano senza pollo (sostituito): la voce spuntata non esiste piu.
        val planRice = planWith(listOf(rice to 50.0))
        val updated = ShoppingListEngine.aggregate(planRice, 0L, foods, previous = previous)
        val invalidated = ShoppingListEngine.invalidatedStates(previous, updated)
        assertThat(invalidated).contains("Petto di pollo")
    }

    @Test
    fun `progress reflects checked and already-have items`() {
        val items = listOf(
            ShoppingItem(weekStartEpochDay = 0, foodItemId = 1, foodName = "A", category = com.fitcoach.app.domain.model.ShoppingCategory.MEAT_FISH, totalGrams = 100.0, checked = true),
            ShoppingItem(weekStartEpochDay = 0, foodItemId = 2, foodName = "B", category = com.fitcoach.app.domain.model.ShoppingCategory.GRAINS, totalGrams = 100.0, alreadyHave = true),
            ShoppingItem(weekStartEpochDay = 0, foodItemId = 3, foodName = "C", category = com.fitcoach.app.domain.model.ShoppingCategory.FRUIT, totalGrams = 100.0)
        )
        assertThat(ShoppingListEngine.purchasedCount(items)).isEqualTo(2)
        assertThat(ShoppingListEngine.progress(items)).isWithin(0.01f).of(2f / 3f)
    }

    @Test
    fun `share text lists aggregated quantities`() {
        val plan = planWith(listOf(chicken to 150.0))
        val items = ShoppingListEngine.aggregate(plan, 0L, foods)
        val text = ShoppingListEngine.toShareText(items)
        assertThat(text).contains("Petto di pollo")
        assertThat(text).contains("1050 g")
    }
}
