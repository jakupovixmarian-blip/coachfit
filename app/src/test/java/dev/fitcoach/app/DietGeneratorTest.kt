package dev.fitcoach.app

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.nutrition.DietGenerator
import dev.fitcoach.app.domain.nutrition.DietRequest
import dev.fitcoach.app.domain.nutrition.DietTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * A generated plan must be reachable from the food records the app really stores:
 * every gram computed from the per-100 g values, no invented food, no invented number.
 */
class DietGeneratorTest {

    private fun food(
        id: Long,
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        brand: String = "",
        category: String = ""
    ) = Food(
        id = id,
        name = name,
        brand = brand,
        category = category,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        source = FoodSource.CREA
    )

    private val pool = listOf(
        food(1, "Petto di pollo", 165.0, 31.0, 0.0, 3.6, category = "carne"),
        food(2, "Riso bianco", 130.0, 2.7, 28.0, 0.3, category = "cereali"),
        food(3, "Olio di oliva", 884.0, 0.0, 0.0, 100.0, category = "condimenti"),
        food(4, "Broccoli", 34.0, 2.8, 7.0, 0.4, category = "verdure"),
        food(5, "Yogurt greco", 97.0, 9.0, 4.0, 5.0, brand = "Fage"),
        food(6, "Pane integrale", 250.0, 8.0, 50.0, 3.0, category = "cereali"),
        food(7, "Salmone", 208.0, 20.0, 0.0, 13.0, category = "pesce")
    )

    private val targets = DietTargets(kcal = 2200.0, proteinG = 150.0, carbsG = 250.0, fatG = 70.0)

    private fun request(
        pool: List<Food> = this.pool,
        days: Int = 7,
        meals: Int = 4,
        excludedIds: Set<Long> = emptySet(),
        keywords: List<String> = emptyList()
    ) = DietRequest(
        targets = targets,
        pool = pool,
        days = days,
        mealsPerDay = meals,
        excludedFoodIds = excludedIds,
        excludedKeywords = keywords
    )

    @Test
    fun `genera sette giorni con il numero di pasti richiesto`() {
        val plan = DietGenerator().generate(request())

        assertEquals(7, plan.days.size)
        assertTrue(plan.days.all { it.meals.size == 4 })
        assertTrue(plan.days.all { day -> day.meals.all { it.foods.isNotEmpty() } })
    }

    @Test
    fun `ogni alimento del piano esiste nel catalogo`() {
        val plan = DietGenerator().generate(request())
        val knownIds = pool.map { it.id }.toSet()

        val planned = plan.days.flatMap { it.meals }.flatMap { it.foods }
        assertTrue(planned.all { it.food.id in knownIds })
        assertFalse(planned.isEmpty())
    }

    @Test
    fun `le calorie giornaliere restano vicine al target`() {
        val plan = DietGenerator().generate(request())

        plan.days.forEach { day ->
            val deviation = abs(day.kcal - targets.kcal) / targets.kcal * 100.0
            assertTrue(
                "giorno ${day.dayIndex}: ${day.kcal} kcal, scostamento $deviation%",
                deviation <= 7.0
            )
        }
    }

    @Test
    fun `le proteine restano almeno al novanta per cento del target`() {
        val plan = DietGenerator().generate(request())

        plan.days.forEach { day ->
            assertTrue(
                "giorno ${day.dayIndex}: ${day.proteinG} g di proteine",
                day.proteinG >= targets.proteinG * 0.9
            )
        }
    }

    @Test
    fun `un alimento escluso dall'utente non compare`() {
        val plan = DietGenerator().generate(request(excludedIds = setOf(1L, 7L)))

        val used = plan.days.flatMap { it.meals }.flatMap { it.foods }.map { it.food.id }.toSet()
        assertFalse(used.contains(1L))
        assertFalse(used.contains(7L))
    }

    @Test
    fun `una parola chiave esclusa esclude il prodotto`() {
        val plan = DietGenerator().generate(request(keywords = listOf("yogurt")))

        val names = plan.days.flatMap { it.meals }.flatMap { it.foods }.map { it.food.name }
        assertFalse(names.any { it.contains("Yogurt", ignoreCase = true) })
    }

    @Test
    fun `le quantita sono positive e arrotondate a cinque grammi`() {
        val plan = DietGenerator().generate(request())

        val grams = plan.days.flatMap { it.meals }.flatMap { it.foods }.map { it.grams }
        assertTrue(grams.all { it > 0.0 })
        assertTrue(grams.all { it % 5.0 == 0.0 })
    }

    @Test
    fun `i totali derivano dai grammi e dai valori per cento grammi`() {
        val plan = DietGenerator().generate(request())
        val item = plan.days.first().meals.first().foods.first()

        assertEquals(item.food.kcalPer100g * item.grams / 100.0, item.kcal, 0.0001)
        assertEquals(item.food.proteinPer100g * item.grams / 100.0, item.proteinG, 0.0001)
    }

    @Test
    fun `lo stesso catalogo produce sempre lo stesso piano`() {
        val generator = DietGenerator()
        val first = generator.generate(request())
        val second = generator.generate(request())

        assertEquals(first.days.map { it.kcal }, second.days.map { it.kcal })
        assertEquals(
            first.days.flatMap { it.meals }.flatMap { it.foods }.map { it.food.id },
            second.days.flatMap { it.meals }.flatMap { it.foods }.map { it.food.id }
        )
    }

    @Test
    fun `con tre pasti gli slot sono colazione pranzo e cena`() {
        val plan = DietGenerator().generate(request(meals = 3))

        assertEquals(
            listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER),
            plan.days.first().meals.map { it.slot }
        )
    }

    @Test
    fun `un catalogo senza fonti proteiche viene segnalato senza inventare dati`() {
        val poorPool = listOf(
            food(20, "Riso bianco", 130.0, 2.7, 28.0, 0.3),
            food(21, "Olio di oliva", 884.0, 0.0, 0.0, 100.0)
        )

        val plan = DietGenerator().generate(request(pool = poorPool))

        assertTrue(plan.warnings.any { it.contains("proteic", ignoreCase = true) })
        assertTrue(plan.days.all { it.meals.isNotEmpty() })
        assertTrue(plan.days.all { it.proteinG < targets.proteinG })
    }

    @Test
    fun `un catalogo vuoto non produce un piano finto`() {
        val plan = DietGenerator().generate(request(pool = emptyList()))

        assertTrue(plan.days.all { it.meals.isEmpty() })
        assertTrue(plan.warnings.any { it.contains("Nessun alimento", ignoreCase = true) })
        assertEquals(0.0, plan.averageKcal, 0.001)
    }
}
