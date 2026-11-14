package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FoodSubstitutionEngineTest {

    private fun food(
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        id: Long = 0
    ) = FoodItem(
        id = id, name = name, category = "Test",
        kcalPer100g = kcal, proteinPer100g = protein,
        carbsPer100g = carbs, fatPer100g = fat, source = "test"
    )

    // Valori realistici (CREA).
    private val chicken = food("Petto di pollo", 110.0, 23.3, 0.0, 1.5, id = 1)
    private val tuna = food("Tonno al naturale", 116.0, 25.5, 0.0, 1.0, id = 2)
    private val oil = food("Olio EVO", 899.0, 0.0, 0.0, 99.9, id = 3)
    private val eggWhite = food("Albume d'uovo", 43.0, 10.7, 0.7, 0.2, id = 4)
    private val beef = food("Carne macinata di manzo (magra)", 152.0, 20.0, 0.0, 8.0, id = 5)
    private val rice = food("Riso bianco (crudo)", 332.0, 6.7, 80.0, 0.4, id = 6)

    private fun macros(food: FoodItem, grams: Double) = FoodSubstitutionEngine.macrosFor(food, grams)

    /** Giornata realistica: il resto del piano copre la maggior parte del target. */
    private val dayTarget = MacroTotals(kcal = 2200.0, protein = 160.0, carbs = 240.0, fat = 65.0)
    private val dayPlannedOther = MacroTotals(kcal = 2035.0, protein = 125.0, carbs = 240.0, fat = 62.75)

    @Test
    fun `chicken to tuna keeps comparable protein and calories`() {
        val mealOther = macros(rice, 200.0) // 664 kcal, P 13.4, C 160, F 0.8
        val request = FoodSubstitutionEngine.Request(
            original = chicken,
            originalGrams = 150.0,
            substitute = tuna,
            mealOther = mealOther,
            dayPlannedOther = dayPlannedOther,
            dayTarget = dayTarget
        )
        val result = FoodSubstitutionEngine.evaluate(request)

        assertThat(result.status).isEqualTo(FoodSubstitutionEngine.Status.COMPATIBLE)
        // La quantita NON e 150 g "a parita di grammi": e ricalcolata sulle proteine.
        assertThat(result.substituteGrams).isNotEqualTo(150.0)
        val originalProtein = macros(chicken, 150.0).protein
        val delta = kotlin.math.abs(result.substituteMacros.protein - originalProtein)
        assertThat(delta).isLessThan(originalProtein * 0.10 + 0.01)
    }

    @Test
    fun `incompatible substitution is rejected when it exceeds the daily kcal limit`() {
        // Sostituire 100 g di pollo con 100 g di olio: supererebbe il limite giornaliero.
        val request = FoodSubstitutionEngine.Request(
            original = chicken,
            originalGrams = 100.0,
            substitute = oil,
            mealOther = MacroTotals(800.0, 60.0, 80.0, 20.0),
            dayPlannedOther = MacroTotals(2000.0, 150.0, 200.0, 60.0),
            dayTarget = dayTarget
        )
        val result = FoodSubstitutionEngine.evaluate(request)
        assertThat(result.status).isEqualTo(FoodSubstitutionEngine.Status.INCOMPATIBLE)
        assertThat(result.isEquivalent).isFalse()
        assertThat(result.violations).isNotEmpty()
    }

    @Test
    fun `combined substitution proposes a balancing food when fat is missing`() {
        // Origine proteica grassa (carne macinata) -> albume magrissimo: manca grasso.
        val mealOther = macros(rice, 150.0) // 498 kcal, P 10.05, C 120, F 0.6
        val request = FoodSubstitutionEngine.Request(
            original = beef,
            originalGrams = 150.0,
            substitute = eggWhite,
            mealOther = mealOther,
            dayPlannedOther = MacroTotals(1700.0, 120.0, 120.0, 50.0),
            dayTarget = dayTarget,
            fatBalancers = listOf(oil)
        )
        val result = FoodSubstitutionEngine.evaluate(request)
        assertThat(result.status).isEqualTo(FoodSubstitutionEngine.Status.COMPATIBLE_WITH_ADJUSTMENT)
        assertThat(result.addition).isNotNull()
        assertThat(result.addition!!.food.name).isEqualTo("Olio EVO")
    }

    @Test
    fun `tolerance values are configurable and honoured`() {
        val request = FoodSubstitutionEngine.Request(
            original = chicken, originalGrams = 150.0, substitute = tuna,
            mealOther = macros(rice, 200.0),
            dayPlannedOther = dayPlannedOther, dayTarget = dayTarget,
            tolerance = FoodSubstitutionEngine.Tolerance(kcalPercent = 20.0, proteinPercent = 20.0)
        )
        assertThat(FoodSubstitutionEngine.evaluate(request).isEquivalent).isTrue()
        // I valori di default sono quelli documentati.
        val defaults = FoodSubstitutionEngine.Tolerance()
        assertThat(defaults.kcalPercent).isEqualTo(5.0)
        assertThat(defaults.proteinPercent).isEqualTo(10.0)
    }

    @Test
    fun `grams must be positive`() {
        val request = FoodSubstitutionEngine.Request(
            original = chicken, originalGrams = 0.0, substitute = tuna,
            mealOther = MacroTotals.ZERO, dayPlannedOther = MacroTotals.ZERO, dayTarget = dayTarget
        )
        var threw = false
        try {
            FoodSubstitutionEngine.evaluate(request)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }
}
