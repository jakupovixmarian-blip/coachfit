package dev.fitcoach.app

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.usecase.CoachEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachEngineTest {

    private val chicken = Food(
        id = 1L, name = "Petto di pollo", kcalPer100g = 165.0, proteinPer100g = 31.0,
        carbsPer100g = 0.0, fatPer100g = 3.6, source = FoodSource.MANUAL
    )
    private val rice = Food(
        id = 2L, name = "Riso", kcalPer100g = 130.0, proteinPer100g = 2.7,
        carbsPer100g = 28.0, fatPer100g = 0.3, source = FoodSource.MANUAL
    )

    @Test
    fun `validation rejects unknown food ids`() {
        val result = CoachEngine.validatePlan(
            items = listOf(99L to 100.0),
            foodById = mapOf(1L to chicken),
            targetKcal = null, targetProtein = null, targetCarbs = null, targetFat = null
        )
        assertFalse(result.valid)
        assertEquals(0.0, result.totalKcal, 0.001)
        assertTrue(result.issues.first().contains("non presente"))
    }

    @Test
    fun `validation rejects implausible gram amounts`() {
        val result = CoachEngine.validatePlan(
            items = listOf(1L to 5000.0),
            foodById = mapOf(1L to chicken),
            targetKcal = null, targetProtein = null, targetCarbs = null, targetFat = null
        )
        assertFalse(result.valid)
        assertEquals(0.0, result.totalKcal, 0.001)
    }

    @Test
    fun `validation computes real totals from the catalog`() {
        val result = CoachEngine.validatePlan(
            items = listOf(1L to 200.0, 2L to 100.0),
            foodById = mapOf(1L to chicken, 2L to rice),
            targetKcal = null, targetProtein = null, targetCarbs = null, targetFat = null
        )
        assertTrue(result.valid)
        assertEquals(165.0 * 2 + 130.0, result.totalKcal, 0.001)
        assertEquals(31.0 * 2 + 2.7, result.totalProtein, 0.001)
        assertEquals(28.0, result.totalCarbs, 0.001)
        assertEquals(3.6 * 2 + 0.3, result.totalFat, 0.001)
    }

    @Test
    fun `validation flags deviation from targets`() {
        val result = CoachEngine.validatePlan(
            items = listOf(2L to 100.0),
            foodById = mapOf(2L to rice),
            targetKcal = 2000.0, targetProtein = 150.0, targetCarbs = 200.0, targetFat = 60.0
        )
        assertFalse(result.valid)
        assertTrue(result.issues.any { it.startsWith("Calorie") })
        assertTrue(result.issues.any { it.startsWith("Proteine") })
    }

    @Test
    fun `gram adjustment closes a calorie gap`() {
        val grams = CoachEngine.adjustGramsForCalories(chicken, 100.0, 165.0)
        assertNotNull(grams)
        assertEquals(200.0, grams!!, 0.01)
    }

    @Test
    fun `gram adjustment refuses impossible values`() {
        assertNull(CoachEngine.adjustGramsForCalories(chicken, 100.0, -500.0))
        assertNull(CoachEngine.adjustGramsForCalories(chicken, 100.0, 100_000.0))
    }
}
