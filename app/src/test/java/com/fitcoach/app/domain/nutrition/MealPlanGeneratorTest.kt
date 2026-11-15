package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifica la matematica dei pasti rimasta in [MealPlanGenerator]. La generazione
 * autonoma dei piani e stata rimossa: la struttura la decide il Coach AI, qui
 * restano solo i calcoli riutilizzati dalla validazione e dalla sostituzione.
 */
class MealPlanGeneratorTest {

    @Test
    fun `gramsForMacro computes inverse of per100g`() {
        // 200 g di un alimento con 20 g proteine/100g = 40 g proteine.
        assertThat(MealPlanGenerator.gramsForMacro(40.0, 20.0)).isWithin(0.01).of(200.0)
    }

    @Test
    fun `gramsForMacro guards against zero macro`() {
        assertThat(MealPlanGenerator.gramsForMacro(40.0, 0.0)).isEqualTo(0.0)
    }

    @Test
    fun `substitute grams preserves the macro contribution`() {
        val chicken = FoodSeedData.foods.first { it.name == "Petto di pollo" }
        val turkey = FoodSeedData.foods.first { it.name == "Fesa di tacchino" }
        val grams = MealPlanGenerator.substituteGrams(chicken, 150.0, turkey)
        // 150 g di pollo = 34.95 g proteine -> tacchino (24 g/100g) ~ 145.6 g
        val expectedProtein = 150.0 * chicken.proteinPer100g / 100.0
        val actualProtein = grams * turkey.proteinPer100g / 100.0
        assertThat(actualProtein).isWithin(1.0).of(expectedProtein)
    }

    @Test
    fun `dominant macro identifies protein foods`() {
        val chicken = FoodSeedData.foods.first { it.name == "Petto di pollo" }
        assertThat(MealPlanGenerator.dominantMacro(chicken))
            .isEqualTo(MealPlanGenerator.DominantMacro.PROTEIN)
    }

    @Test
    fun `dominant macro identifies fat and carb foods`() {
        val oil = FoodItem(
            id = 1, name = "Olio test", category = "Grassi", kcalPer100g = 899.0,
            proteinPer100g = 0.0, carbsPer100g = 0.0, fatPer100g = 99.9, source = "test"
        )
        val rice = FoodItem(
            id = 2, name = "Riso test", category = "Cereali", kcalPer100g = 332.0,
            proteinPer100g = 6.7, carbsPer100g = 80.4, fatPer100g = 0.4, source = "test"
        )
        assertThat(MealPlanGenerator.dominantMacro(oil)).isEqualTo(MealPlanGenerator.DominantMacro.FAT)
        assertThat(MealPlanGenerator.dominantMacro(rice)).isEqualTo(MealPlanGenerator.DominantMacro.CARBS)
    }

    @Test
    fun `generated day and meal totals aggregate entries`() {
        val entry = com.fitcoach.app.domain.model.MealEntry(
            foodItemId = 1, foodName = "Petto di pollo", grams = 100.0,
            unit = com.fitcoach.app.domain.model.FoodUnit.RAW,
            kcal = 110.0, protein = 23.3, carbs = 0.0, fat = 1.5
        )
        val meal = MealPlanGenerator.GeneratedMeal(
            slot = com.fitcoach.app.domain.model.MealSlot.LUNCH,
            entries = listOf(entry, entry)
        )
        assertThat(meal.kcal).isWithin(0.01).of(220.0)
        assertThat(meal.protein).isWithin(0.01).of(46.6)
        assertThat(meal.totals.kcal).isWithin(0.01).of(220.0)
    }
}
