package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il compositore non deve mai inventare alimenti ne grammature: riorganizza
 * il pasto esistente. Questi test bloccano le regressioni su quel contratto.
 */
class RecipeComposerTest {

    private fun entry(name: String, grams: Double, kcal: Double, p: Double, c: Double, f: Double) =
        MealEntry(
            foodItemId = name.hashCode().toLong(),
            foodName = name,
            grams = grams,
            unit = FoodUnit.RAW,
            kcal = kcal,
            protein = p,
            carbs = c,
            fat = f
        )

    private fun food(name: String, p: Double, c: Double, f: Double, category: String = "Varie") =
        FoodItem(
            id = name.hashCode().toLong(),
            name = name,
            category = category,
            kcalPer100g = p * 4 + c * 4 + f * 9,
            proteinPer100g = p,
            carbsPer100g = c,
            fatPer100g = f,
            source = "test"
        )

    private val oats = entry("Fiocchi d'avena", 60.0, 228.0, 8.0, 36.0, 4.2)
    private val yogurt = entry("Yogurt greco 0%", 170.0, 97.0, 17.0, 6.0, 0.5)
    private val berries = entry("Frutti di bosco", 100.0, 45.0, 1.0, 9.0, 0.4)
    private val almonds = entry("Mandorle", 15.0, 87.0, 3.2, 3.0, 7.5)

    private val foods = listOf(
        food("Fiocchi d'avena", 13.0, 60.0, 7.0, "Cereali"),
        food("Yogurt greco 0%", 10.0, 3.5, 0.3, "Latticini"),
        food("Frutti di bosco", 1.0, 9.0, 0.4, "Frutta"),
        food("Mandorle", 21.0, 20.0, 50.0, "Frutta secca")
    )

    @Test
    fun `colazione compone un piatto con carboidrati proteine e frutta`() {
        val recipe = RecipeComposer.create(
            entries = listOf(oats, yogurt, berries, almonds),
            slot = MealSlot.BREAKFAST,
            foods = foods
        )
        assertNotNull(recipe)
        val names = recipe!!.ingredients.map { it.foodName }
        assertTrue(names.contains("Fiocchi d'avena"))
        assertTrue(names.contains("Yogurt greco 0%"))
        assertTrue(names.contains("Frutti di bosco"))
    }

    @Test
    fun `i totali coincidono con la somma degli ingredienti usati`() {
        val recipe = RecipeComposer.create(
            entries = listOf(oats, yogurt, berries, almonds),
            slot = MealSlot.BREAKFAST,
            foods = foods
        )!!
        val expectedKcal = recipe.ingredients.sumOf { it.kcal }
        assertEquals(expectedKcal, recipe.kcal, 0.001)
        assertEquals(recipe.ingredients.sumOf { it.protein }, recipe.protein, 0.001)
        assertEquals(recipe.ingredients.sumOf { it.carbs }, recipe.carbs, 0.001)
        assertEquals(recipe.ingredients.sumOf { it.fat }, recipe.fat, 0.001)
    }

    @Test
    fun `le grammature sono quelle del piano e non vengono riscalate`() {
        val recipe = RecipeComposer.create(
            entries = listOf(oats, yogurt, berries, almonds),
            slot = MealSlot.BREAKFAST,
            foods = foods
        )!!
        val byName = recipe.ingredients.associateBy { it.foodName }
        assertEquals(60.0, byName.getValue("Fiocchi d'avena").grams, 0.001)
        assertEquals(170.0, byName.getValue("Yogurt greco 0%").grams, 0.001)
    }

    @Test
    fun `stessi input producono la stessa ricetta`() {
        val a = RecipeComposer.create(listOf(oats, yogurt, berries, almonds), MealSlot.BREAKFAST, foods)
        val b = RecipeComposer.create(listOf(oats, yogurt, berries, almonds), MealSlot.BREAKFAST, foods)
        assertEquals(a?.signature, b?.signature)
        assertEquals(a?.name, b?.name)
    }

    @Test
    fun `un solo alimento non basta per una ricetta`() {
        val recipe = RecipeComposer.create(listOf(oats), MealSlot.BREAKFAST, foods)
        assertNull(recipe)
    }

    @Test
    fun `gli alimenti esclusi dal profilo non entrano nella ricetta`() {
        val avoid = setOf(FoodItem.normalizeName("Yogurt greco 0%"))
        val recipe = RecipeComposer.create(
            entries = listOf(oats, yogurt, berries, almonds),
            slot = MealSlot.BREAKFAST,
            foods = foods,
            avoid = avoid
        )
        assertNotNull(recipe)
        assertTrue(recipe!!.ingredients.none { it.foodName == "Yogurt greco 0%" })
    }

    @Test
    fun `se tutti gli alimenti sono esclusi la ricetta non viene creata`() {
        val avoid = listOf(oats, yogurt, berries, almonds)
            .map { FoodItem.normalizeName(it.foodName) }
            .toSet()
        assertNull(
            RecipeComposer.create(listOf(oats, yogurt, berries, almonds),
                MealSlot.BREAKFAST, foods, avoid = avoid)
        )
    }

    @Test
    fun `lo stile leggero esclude i grassi aggiunti`() {
        val classic = RecipeComposer.create(
            listOf(oats, yogurt, berries, almonds), MealSlot.BREAKFAST, foods,
            style = RecipeStyle.CLASSIC
        )!!
        val light = RecipeComposer.create(
            listOf(oats, yogurt, berries, almonds), MealSlot.BREAKFAST, foods,
            style = RecipeStyle.LIGHT
        )!!
        assertTrue(classic.ingredients.any { it.foodName == "Mandorle" })
        assertTrue(light.ingredients.none { it.foodName == "Mandorle" })
    }

    @Test
    fun `lo stile ricco aggiunge un secondo latticino`() {
        val secondDairy = entry("Ricotta magra", 80.0, 114.0, 9.0, 3.0, 8.0)
        val recipe = RecipeComposer.create(
            entries = listOf(oats, yogurt, secondDairy, berries),
            slot = MealSlot.BREAKFAST,
            foods = foods,
            style = RecipeStyle.RICH
        )!!
        assertTrue(recipe.ingredients.any { it.foodName == "Ricotta magra" })
    }

    @Test
    fun `uno spuntino usa proteine e frutta`() {
        val recipe = RecipeComposer.create(
            entries = listOf(yogurt, berries, almonds),
            slot = MealSlot.MORNING_SNACK,
            foods = foods
        )
        assertNotNull(recipe)
        val names = recipe!!.ingredients.map { it.foodName }
        assertTrue(names.contains("Yogurt greco 0%"))
        assertTrue(names.contains("Frutti di bosco"))
    }

    @Test
    fun `non ci sono ingredienti duplicati`() {
        val duplicated = listOf(oats, oats, yogurt, berries)
        val recipe = RecipeComposer.create(duplicated, MealSlot.BREAKFAST, foods)!!
        val names = recipe.ingredients.map { FoodItem.normalizeName(it.foodName) }
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun `la ricetta contiene passi di preparazione`() {
        val recipe = RecipeComposer.create(
            listOf(oats, yogurt, berries, almonds), MealSlot.BREAKFAST, foods
        )!!
        assertTrue(recipe.steps.isNotEmpty())
        assertTrue(recipe.name.isNotBlank())
    }

    @Test
    fun `un alimento non presente in anagrafica viene classificato per macro`() {
        val unknown = entry("Preparato proteico", 30.0, 110.0, 24.0, 2.0, 1.0)
        val recipe = RecipeComposer.create(
            entries = listOf(unknown, oats, berries),
            slot = MealSlot.BREAKFAST,
            foods = foods
        )
        assertNotNull(recipe)
    }
}
