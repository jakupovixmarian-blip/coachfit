package dev.fitcoach.app

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodState
import dev.fitcoach.app.domain.search.FoodMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodMatcherTest {

    private fun food(
        id: Long,
        name: String,
        brand: String = "",
        barcode: String? = null,
        state: FoodState = FoodState.RAW
    ) = Food(
        id = id,
        name = name,
        brand = brand,
        kcalPer100g = 100.0,
        proteinPer100g = 10.0,
        carbsPer100g = 10.0,
        fatPer100g = 2.0,
        state = state,
        barcode = barcode
    )

    @Test
    fun `normalization removes accents and punctuation`() {
        assertEquals("caffe d orzo in polvere", FoodMatcher.normalize("Caffè d'orzo, in polvere"))
        assertEquals("petto di pollo", FoodMatcher.normalize("  Petto   di pollo  "))
    }

    @Test
    fun `exact name scores one`() {
        assertEquals(1.0, FoodMatcher.score("petto di pollo", "Petto di pollo"), 0.001)
    }

    @Test
    fun `ranking prefers the closest name`() {
        val foods = listOf(
            food(1L, "Pollo arrosto"),
            food(2L, "Petto di pollo, crudo")
        )
        val ranked = FoodMatcher.rank("petto pollo", foods)
        assertTrue(ranked.isNotEmpty())
        assertEquals("Petto di pollo, crudo", ranked.first().food.name)
    }

    @Test
    fun `a typo still finds the food`() {
        val score = FoodMatcher.score("ptto di pollo", "Petto di pollo, crudo")
        assertTrue("punteggio troppo basso: $score", score >= 0.6)
    }

    @Test
    fun `articles and prepositions do not change the result`() {
        val withStopwords = FoodMatcher.score("petto di pollo", "Petto pollo")
        assertTrue(withStopwords >= 0.8)
    }

    @Test
    fun `raw and cooked are different records`() {
        val raw = food(1L, "Petto di pollo, crudo")
        val cooked = food(2L, "Petto di pollo, cotto", state = FoodState.COOKED)
        assertNotEquals(FoodMatcher.dedupeKey(raw), FoodMatcher.dedupeKey(cooked))
    }

    @Test
    fun `the same barcode identifies the same product`() {
        val a = food(1L, "Yogurt greco 0%", brand = "Fage", barcode = "5201050110014")
        val b = food(2L, "Yogurt greco", brand = "Fage", barcode = "5201050110014")
        assertEquals(FoodMatcher.dedupeKey(a), FoodMatcher.dedupeKey(b))
    }

    @Test
    fun `duplicate groups are reported but not merged`() {
        val a = food(1L, "Petto di pollo, crudo")
        val b = food(2L, "petto di pollo crudo")
        val groups = FoodMatcher.duplicateGroups(listOf(a, b))
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().size)
    }

    @Test
    fun `edit distance is correct`() {
        assertEquals(0, FoodMatcher.levenshtein("pollo", "pollo"))
        assertEquals(1, FoodMatcher.levenshtein("pollo", "polo"))
        assertEquals(3, FoodMatcher.levenshtein("", "abc"))
    }

    @Test
    fun `an unrelated query returns nothing`() {
        val foods = listOf(food(1L, "Petto di pollo, crudo"), food(2L, "Riso bianco, cotto"))
        assertTrue(FoodMatcher.rank("xyzqw", foods).isEmpty())
    }

    @Test
    fun `ranking respects the limit`() {
        val foods = (1..10).map { food(it.toLong(), "Petto di pollo variante $it") }
        assertEquals(3, FoodMatcher.rank("petto di pollo", foods, limit = 3).size)
    }
}
