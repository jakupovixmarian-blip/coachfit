package dev.fitcoach.app

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import dev.fitcoach.app.domain.validation.FoodValidator
import dev.fitcoach.app.domain.validation.Severity
import dev.fitcoach.app.domain.validation.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodValidatorTest {

    private fun food(
        name: String = "Petto di pollo, crudo",
        kcal: Double = 110.0,
        protein: Double = 23.0,
        carbs: Double = 0.0,
        fat: Double = 1.5,
        fiber: Double? = 0.0,
        source: FoodSource = FoodSource.CREA,
        state: FoodState = FoodState.RAW,
        brand: String = "",
        barcode: String? = null
    ) = Food(
        name = name,
        brand = brand,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        fiberPer100g = fiber,
        source = source,
        state = state,
        barcode = barcode
    )

    @Test
    fun `a coherent official record is usable and verified`() {
        val validation = FoodValidator.validate(food())
        assertTrue(validation.isUsable)
        assertEquals(VerificationStatus.VERIFIED, validation.status)
        assertEquals(0.95, validation.reliability, 0.001)
    }

    @Test
    fun `negative values are an error`() {
        val validation = FoodValidator.validate(food(protein = -3.0))
        assertFalse(validation.isUsable)
        assertTrue(validation.issues.any { it.code == "NEGATIVE_VALUE" && it.severity == Severity.ERROR })
    }

    @Test
    fun `macros cannot exceed one hundred grams per hundred grams`() {
        val validation = FoodValidator.validate(food(kcal = 600.0, protein = 50.0, carbs = 50.0, fat = 30.0))
        assertFalse(validation.isUsable)
        assertTrue(validation.issues.any { it.code == "MACRO_SUM_OVER_100" })
    }

    @Test
    fun `more energy than pure fat is an error`() {
        val validation = FoodValidator.validate(food(kcal = 1200.0))
        assertFalse(validation.isUsable)
        assertTrue(validation.issues.any { it.code == "IMPLAUSIBLE_ENERGY" })
    }

    @Test
    fun `declared calories inconsistent with macros produce a warning`() {
        val validation = FoodValidator.validate(food(kcal = 200.0, protein = 5.0, carbs = 5.0, fat = 5.0))
        assertTrue(validation.isUsable)
        assertTrue(validation.hasWarnings)
        assertTrue(validation.issues.any { it.code == "ENERGY_MISMATCH" })
    }

    @Test
    fun `missing fiber is reported without blocking`() {
        val validation = FoodValidator.validate(food(fiber = null))
        assertTrue(validation.isUsable)
        assertTrue(validation.issues.any { it.code == "FIBER_MISSING" && it.severity == Severity.INFO })
    }

    @Test
    fun `commercial product without brand is flagged`() {
        val validation = FoodValidator.validate(food(state = FoodState.COMMERCIAL, source = FoodSource.OPEN_FOOD_FACTS))
        assertTrue(validation.issues.any { it.code == "BRAND_MISSING" })
    }

    @Test
    fun `manual data is marked as such`() {
        val validation = FoodValidator.validate(food(source = FoodSource.MANUAL))
        assertEquals(VerificationStatus.MANUAL, validation.status)
        assertTrue(validation.issues.any { it.code == "MANUAL_DATA" })
    }

    @Test
    fun `conflicting sources pick the most reliable and never average`() {
        val official = food()
        val crowdsourced = food(source = FoodSource.OPEN_FOOD_FACTS, kcal = 150.0)
        val conflict = FoodValidator.resolve(listOf(crowdsourced, official))!!
        assertEquals(FoodSource.CREA, conflict.chosen.source)
        assertEquals(1, conflict.rejected.size)
        assertTrue(conflict.reason.contains("non vengono mediati"))
    }

    @Test
    fun `an invalid record never wins over a valid one`() {
        val invalidButOfficial = food(kcal = 1500.0)
        val validManual = food(source = FoodSource.MANUAL)
        val conflict = FoodValidator.resolve(listOf(invalidButOfficial, validManual))!!
        assertEquals(FoodSource.MANUAL, conflict.chosen.source)
    }

    @Test
    fun `resolve without candidates returns null`() {
        assertNotNull(FoodValidator.resolve(listOf(food())))
        assertTrue(FoodValidator.resolve(emptyList()) == null)
    }

    @Test
    fun `blank name is an error`() {
        val validation = FoodValidator.validate(food(name = "  "))
        assertFalse(validation.isUsable)
    }
}
