package dev.fitcoach.app

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.usecase.CoachPromptBuilder
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachPromptBuilderTest {

    private val builder = CoachPromptBuilder(Json { ignoreUnknownKeys = true; isLenient = true })
    private val food = Food(
        id = 7L, name = "Yogurt greco", kcalPer100g = 59.0, proteinPer100g = 10.0,
        carbsPer100g = 3.6, fatPer100g = 0.4, source = FoodSource.OPEN_FOOD_FACTS
    )

    @Test
    fun `prompt embeds real catalog ids and targets`() {
        val profile = UserProfile(
            name = "A", age = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
            experience = ExperienceLevel.INTERMEDIATE
        )
        val prompt = builder.planPrompt(profile, listOf(food), 2500.0, 160.0)
        assertTrue(prompt.contains("id=7"))
        assertTrue(prompt.contains("Yogurt greco"))
        assertTrue(prompt.contains("2500"))
        assertTrue(prompt.contains("160"))
    }

    @Test
    fun `parses a valid model answer`() {
        val raw = """{"rationale":"Piano bilanciato","items":[{"slot":"BREAKFAST","foodId":7,"grams":150,"note":"con frutta"}]}"""
        val parsed = builder.parsePlan(raw, setOf(7L))
        assertEquals("Piano bilanciato", parsed.rationale)
        assertEquals(1, parsed.items.size)
        assertEquals(MealSlot.BREAKFAST, parsed.items.first().slot)
        assertEquals(7L, parsed.items.first().foodId)
        assertEquals(150.0, parsed.items.first().grams, 0.001)
    }

    @Test
    fun `drops items referencing unknown foods`() {
        val raw = """{"rationale":"x","items":[{"slot":"LUNCH","foodId":999,"grams":100},{"slot":"DINNER","foodId":7,"grams":120}]}"""
        val parsed = builder.parsePlan(raw, setOf(7L))
        assertEquals(1, parsed.items.size)
        assertEquals(7L, parsed.items.first().foodId)
    }

    @Test
    fun `rejects answers with no usable item`() {
        val raw = """{"rationale":"x","items":[{"slot":"LUNCH","foodId":999,"grams":100}]}"""
        assertThrows(AppError.InvalidResponse::class.java) {
            builder.parsePlan(raw, setOf(7L))
        }
    }

    @Test
    fun `extracts json surrounded by prose`() {
        val raw = "Ecco il piano:\n{\"a\":1}\nfine"
        assertEquals("{\"a\":1}", builder.extractJson(raw))
    }

    @Test
    fun `rejects text without json`() {
        assertThrows(AppError.InvalidResponse::class.java) { builder.extractJson("nessun json qui") }
    }
}
