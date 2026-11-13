package dev.fitcoach.app

import dev.fitcoach.app.domain.engine.NutrientTotals
import dev.fitcoach.app.domain.engine.NutrientsPer100g
import dev.fitcoach.app.domain.engine.NutritionMath
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionMathTest {

    private val chicken = Food(
        id = 1L,
        name = "Petto di pollo, crudo",
        kcalPer100g = 110.0,
        proteinPer100g = 23.0,
        carbsPer100g = 0.0,
        fatPer100g = 1.5,
        fiberPer100g = 0.0,
        source = FoodSource.CREA
    )

    private val rice = Food(
        id = 2L,
        name = "Riso bianco, cotto",
        kcalPer100g = 130.0,
        proteinPer100g = 2.7,
        carbsPer100g = 28.0,
        fatPer100g = 0.3,
        fiberPer100g = 0.4,
        source = FoodSource.CREA
    )

    @Test
    fun `scales per hundred grams to the portion`() {
        val totals = NutritionMath.foodTotals(chicken, 150.0)
        assertEquals(165.0, totals.kcal, 0.001)
        assertEquals(34.5, totals.protein, 0.001)
        assertEquals(2.25, totals.fat, 0.001)
    }

    @Test
    fun `non positive portion yields zero`() {
        assertEquals(NutrientTotals(), NutritionMath.foodTotals(chicken, 0.0))
        assertEquals(NutrientTotals(), NutritionMath.foodTotals(chicken, -20.0))
    }

    @Test
    fun `sums a meal`() {
        val totals = NutritionMath.totals(listOf(chicken to 150.0, rice to 200.0))
        assertEquals(165.0 + 260.0, totals.kcal, 0.001)
        assertEquals(34.5 + 5.4, totals.protein, 0.001)
        assertEquals(56.0, totals.carbs, 0.001)
    }

    @Test
    fun `energy deviation compares declared calories with macros`() {
        val deviation = NutritionMath.energyDeviationPercent(
            declaredKcal = 100.0,
            protein = 10.0,
            carbs = 10.0,
            fat = 2.0
        )!!
        assertEquals(2.0, deviation, 0.001)
        assertNull(NutritionMath.energyDeviationPercent(0.0, 10.0, 10.0, 2.0))
    }

    @Test
    fun `macro energy uses four four nine`() {
        assertEquals(98.0, NutritionMath.macroEnergyKcal(10.0, 10.0, 2.0), 0.001)
    }

    @Test
    fun `portions are rounded to a weighable step`() {
        assertEquals(145.0, NutritionMath.roundGrams(147.3), 0.001)
        assertEquals(150.0, NutritionMath.roundGrams(148.0), 0.001)
    }

    @Test
    fun `totals are additive`() {
        val a = NutritionMath.foodTotals(chicken, 100.0)
        val b = NutritionMath.foodTotals(rice, 100.0)
        val sum = a + b
        assertEquals(a.kcal + b.kcal, sum.kcal, 0.001)
        assertEquals(110.0 + 130.0, sum.kcal, 0.001)
    }

    @Test
    fun `share percent is null without a target`() {
        assertNull(NutritionMath.sharePercent(500.0, 0.0))
        assertEquals(50.0, NutritionMath.sharePercent(500.0, 1000.0)!!, 0.001)
    }

    @Test
    fun `macro energy per hundred grams is derived from the record`() {
        val nutrients = NutrientsPer100g(kcal = 110.0, protein = 23.0, carbs = 0.0, fat = 1.5)
        assertEquals(23.0 * 4 + 1.5 * 9, NutritionMath.macroEnergyPer100g(nutrients), 0.001)
    }

    @Test
    fun `fiber is summed only when present`() {
        val withoutFiber = Food(
            name = "Alimento senza fibre",
            kcalPer100g = 50.0,
            proteinPer100g = 1.0,
            carbsPer100g = 10.0,
            fatPer100g = 0.5
        )
        val totals = NutritionMath.foodTotals(withoutFiber, 200.0)
        assertEquals(0.0, totals.fiber, 0.001)
        assertTrue(totals.kcal > 0.0)
    }
}
