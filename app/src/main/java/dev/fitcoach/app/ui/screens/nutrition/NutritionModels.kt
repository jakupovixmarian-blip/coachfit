package dev.fitcoach.app.ui.screens.nutrition

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.MealItem
import dev.fitcoach.app.domain.model.MealSlot

data class MacroSet(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(other: MacroSet) = MacroSet(
        kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat
    )

    companion object {
        val ZERO = MacroSet(0.0, 0.0, 0.0, 0.0)
    }
}

fun Food.macrosFor(grams: Double): MacroSet {
    val f = grams / 100.0
    return MacroSet(kcalPer100g * f, proteinPer100g * f, carbsPer100g * f, fatPer100g * f)
}

fun Food.priceFor(grams: Double): Double? = pricePerKg?.let { it * grams / 1000.0 }

fun List<MealItem>.totals(foodById: Map<Long, Food>): MacroSet =
    fold(MacroSet.ZERO) { acc, item ->
        val food = foodById[item.foodId] ?: return@fold acc
        acc + food.macrosFor(item.grams)
    }

fun mealSlotOrder(slot: MealSlot): Int = when (slot) {
    MealSlot.BREAKFAST -> 0
    MealSlot.MORNING_SNACK -> 1
    MealSlot.LUNCH -> 2
    MealSlot.AFTERNOON_SNACK -> 3
    MealSlot.DINNER -> 4
    MealSlot.EVENING_SNACK -> 5
}
