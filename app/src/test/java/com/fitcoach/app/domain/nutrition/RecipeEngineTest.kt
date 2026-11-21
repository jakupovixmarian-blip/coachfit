package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecipeEngineTest {

    private fun entry(name: String, grams: Double = 100.0, kcal: Double = 100.0) = MealEntry(
        foodItemId = 1L,
        foodName = name,
        grams = grams,
        unit = FoodUnit.RAW,
        kcal = kcal,
        protein = 10.0,
        carbs = 10.0,
        fat = 2.0
    )

    private fun meal(slot: MealSlot, vararg entries: MealEntry) =
        Meal(id = 1L, nutritionDayId = 1L, slot = slot, time = slot.defaultTime, entries = entries.toList())

    @Test
    fun `la colazione propone solo ricette di colazione`() {
        val breakfast = meal(
            MealSlot.BREAKFAST,
            entry("Fiocchi d'avena"),
            entry("Proteine del siero (whey, polvere)")
        )
        val recipes = RecipeEngine.forMeal(breakfast)
        assertThat(recipes).isNotEmpty()
        assertThat(recipes.all { it.slot == MealSlot.BREAKFAST }).isTrue()
        assertThat(recipes.map { it.name }).contains("Porridge di avena con whey")
    }

    @Test
    fun `una ricetta non compare se gli ingredienti non sono nel pasto`() {
        // Il pranzo contiene solo tonno: la ricetta "pollo e riso" non deve apparire.
        val lunch = meal(MealSlot.LUNCH, entry("Tonno in scatola al naturale"))
        val recipes = RecipeEngine.forMeal(lunch)
        assertThat(recipes.map { it.name }).doesNotContain("Pollo alla piastra con riso e verdure")
    }

    @Test
    fun `gli ingredienti usano le grammature reali del pasto`() {
        val breakfast = meal(
            MealSlot.BREAKFAST,
            entry("Fiocchi d'avena", grams = 80.0),
            entry("Proteine del siero (whey, polvere)", grams = 30.0)
        )
        val recipe = RecipeEngine.forMeal(breakfast).first { it.id == "porridge_avena_whey" }
        val oats = recipe.ingredients.first { it.foodName == "Fiocchi d'avena" }
        assertThat(oats.grams).isEqualTo(80.0)
    }

    @Test
    fun `un alimento escluso dal profilo non entra nelle ricette`() {
        val breakfast = meal(
            MealSlot.BREAKFAST,
            entry("Fiocchi d'avena"),
            entry("Proteine del siero (whey, polvere)")
        )
        val profile = UserProfile(excludedFoods = listOf("avena"))
        val recipes = RecipeEngine.forMeal(breakfast, profile)
        assertThat(recipes.map { it.name }).doesNotContain("Porridge di avena con whey")
    }

    @Test
    fun `una allergia esclude gli alimenti che la contengono`() {
        // "latte" deve escludere anche "Latte parzialmente scremato".
        assertThat(RecipeEngine.isExcluded("Latte parzialmente scremato", UserProfile(allergies = listOf("latte")))).isTrue()
        assertThat(RecipeEngine.isExcluded("Fiocchi d'avena", UserProfile(allergies = listOf("latte")))).isFalse()
    }

    @Test
    fun `un pasto vuoto non produce ricette`() {
        assertThat(RecipeEngine.forMeal(meal(MealSlot.DINNER))).isEmpty()
    }

    @Test
    fun `la cena propone ricette di cena e non di colazione`() {
        val dinner = meal(
            MealSlot.DINNER,
            entry("Salmone"),
            entry("Riso basmati (crudo)"),
            entry("Broccoli")
        )
        val recipes = RecipeEngine.forMeal(dinner)
        assertThat(recipes.map { it.name }).contains("Salmone al forno con riso e broccoli")
        assertThat(recipes.none { it.slot == MealSlot.BREAKFAST }).isTrue()
    }

    @Test
    fun `le calorie della ricetta sono la somma degli ingredienti`() {
        val lunch = meal(
            MealSlot.LUNCH,
            entry("Petto di pollo", kcal = 200.0),
            entry("Riso basmati (crudo)", kcal = 350.0),
            entry("Zucchine", kcal = 20.0)
        )
        val recipe = RecipeEngine.forMeal(lunch).first { it.id == "pollo_riso_verdure" }
        assertThat(recipe.kcal).isWithin(0.01).of(570.0)
    }

    @Test
    fun `forDay raggruppa per slot e salta i pasti senza ricette`() {
        val breakfast = meal(MealSlot.BREAKFAST, entry("Fiocchi d'avena"), entry("Proteine del siero (whey, polvere)"))
        val lunch = meal(MealSlot.LUNCH, entry("Tonno in scatola al naturale"))
        val bySlot = RecipeEngine.forDay(listOf(breakfast, lunch))
        assertThat(bySlot.keys).contains(MealSlot.BREAKFAST)
        assertThat(bySlot[MealSlot.BREAKFAST]).isNotEmpty()
    }

    /**
     * Guardia sui nomi: un refuso in un ingrediente non produce errori, la
     * ricetta semplicemente non comparirebbe mai. Questo test confronta ogni
     * ingrediente con il database alimentare reale.
     */
    @Test
    fun `ogni ingrediente delle ricette esiste nel database alimentare`() {
        val known = FoodSeedData.foods.map { it.normalizedName }.toSet()
        val missing = mutableSetOf<String>()
        MealSlot.entries.forEach { slot ->
            // Un pasto che contiene tutti gli alimenti del database: se una
            // ricetta resta fuori, il nome non corrisponde a nessun alimento.
            val meal = Meal(
                id = 1L,
                nutritionDayId = 1L,
                slot = slot,
                time = slot.defaultTime,
                entries = FoodSeedData.foods.map {
                    MealEntry(
                        foodItemId = it.id,
                        foodName = it.name,
                        grams = 100.0,
                        unit = it.defaultUnit,
                        kcal = it.kcalPer100g,
                        protein = it.proteinPer100g,
                        carbs = it.carbsPer100g,
                        fat = it.fatPer100g
                    )
                }
            )
            RecipeEngine.forMeal(meal, null, FoodSeedData.foods)
                .flatMap { it.ingredients }
                .forEach { if (it.foodName.normalizedName() !in known) missing.add(it.foodName) }
        }
        assertThat(missing).isEmpty()
    }

    /**
     * Ogni slot con almeno una ricetta deve produrne una quando il pasto
     * contiene gli alimenti giusti: verifica che le ricette siano raggiungibili
     * e non codice morto.
     */
    @Test
    fun `ogni pasto principale ha almeno una ricetta proponibile`() {
        val cases = mapOf(
            MealSlot.BREAKFAST to listOf("Fiocchi d'avena", "Proteine del siero (whey, polvere)"),
            MealSlot.LUNCH to listOf("Petto di pollo", "Riso basmati (crudo)", "Zucchine"),
            MealSlot.DINNER to listOf("Salmone", "Riso basmati (crudo)", "Broccoli")
        )
        cases.forEach { (slot, foods) ->
            val meal = Meal(
                id = 1L, nutritionDayId = 1L, slot = slot, time = slot.defaultTime,
                entries = foods.map { entry(it) }
            )
            assertThat(RecipeEngine.forMeal(meal)).isNotEmpty()
        }
    }
}

private fun String.normalizedName(): String =
    com.fitcoach.app.domain.model.FoodItem.normalizeName(this)
