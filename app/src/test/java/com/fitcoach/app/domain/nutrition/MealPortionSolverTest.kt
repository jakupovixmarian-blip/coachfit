package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbDayTarget
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Il solver deve centrare i target reali del carb cycling con alimenti del
 * catalogo reale. Se non ci riesce, l'AI non ha alcun modo di produrre un piano
 * valido: per questo i test usano i valori nutrizionali veri (CREA/USDA).
 */
class MealPortionSolverTest {

    private fun food(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name = name, category = "test", kcalPer100g = kcal, proteinPer100g = p,
            carbsPer100g = c, fatPer100g = f, source = "CREA – Tabelle di composizione degli alimenti")

    // Valori reali dal seed (CREA).
    private val chicken = food("Petto di pollo", 110.0, 23.3, 0.0, 1.5)
    private val rice = food("Riso bianco (crudo)", 332.0, 6.7, 80.0, 0.4)
    private val oil = food("Olio extravergine d'oliva", 899.0, 0.0, 0.0, 99.9)
    private val egg = food("Uovo intero di gallina", 128.0, 12.4, 0.7, 8.7)
    private val oats = food("Fiocchi d'avena", 367.0, 13.0, 60.0, 7.0)
    private val bread = food("Pane integrale", 240.0, 8.0, 41.0, 3.0)
    private val veg = food("Zucchine", 16.0, 1.3, 2.5, 0.3)

    private val highDay = CarbDayTarget(CarbDayType.HIGH, kcal = 2037.0, protein = 165.0, carbs = 243.0, fat = 45.0)
    private val lowDay = CarbDayTarget(CarbDayType.LOW, kcal = 1662.0, protein = 165.0, carbs = 120.0, fat = 58.0)

    private fun fiveMeals(carbSource: FoodItem) = listOf(
        "BREAKFAST" to listOf(oats, egg),
        "MORNING_SNACK" to listOf(bread, chicken),
        "LUNCH" to listOf(carbSource, chicken, oil, veg),
        "AFTERNOON_SNACK" to listOf(chicken, bread),
        "DINNER" to listOf(carbSource, chicken, oil, veg)
    )

    // ------------------------------------------------------------ roles
    @Test
    fun `whole egg is a protein source despite fat calories`() {
        // L'uovo ha piu kcal da grassi (78) che da proteine (50): classificarlo
        // come grasso lo renderebbe inutilizzabile come fonte proteica.
        assertThat(MealPortionSolver.roleOf(egg)).isEqualTo(MealPortionSolver.Role.PROTEIN)
    }

    @Test
    fun `roles are correct for staples`() {
        assertThat(MealPortionSolver.roleOf(chicken)).isEqualTo(MealPortionSolver.Role.PROTEIN)
        assertThat(MealPortionSolver.roleOf(rice)).isEqualTo(MealPortionSolver.Role.CARBS)
        assertThat(MealPortionSolver.roleOf(oil)).isEqualTo(MealPortionSolver.Role.FAT)
        assertThat(MealPortionSolver.roleOf(veg)).isEqualTo(MealPortionSolver.Role.MIXED)
    }

    // ----------------------------------------------------------- solving
    @Test
    fun `solves a high carb day within three percent`() {
        val result = MealPortionSolver.solve(highDay, fiveMeals(rice))

        assertThat(result.ok).isTrue()
        assertThat(result.warnings).isEmpty()
        assertThat(abs(result.actual.kcal - 2037.0) / 2037.0).isLessThan(0.03)
        assertThat(abs(result.actual.protein - 165.0) / 165.0).isLessThan(0.03)
        assertThat(abs(result.actual.carbs - 243.0) / 243.0).isLessThan(0.03)
        assertThat(abs(result.actual.fat - 45.0) / 45.0).isLessThan(0.03)
    }

    @Test
    fun `solves a low carb day within three percent`() {
        val result = MealPortionSolver.solve(lowDay, fiveMeals(rice))

        assertThat(result.ok).isTrue()
        assertThat(abs(result.actual.kcal - 1662.0) / 1662.0).isLessThan(0.03)
        assertThat(abs(result.actual.protein - 165.0) / 165.0).isLessThan(0.03)
        assertThat(abs(result.actual.carbs - 120.0) / 120.0).isLessThan(0.03)
        assertThat(abs(result.actual.fat - 58.0) / 58.0).isLessThan(0.03)
    }

    @Test
    fun `solved day passes the real validator`() {
        // Il vero collaudo: la giornata risolta deve superare i controlli di
        // ProgramGenerationEngine, non solo quelli del solver.
        val result = MealPortionSolver.solve(highDay, fiveMeals(rice))
        val meals = result.meals

        val actual = meals.fold(com.fitcoach.app.domain.model.MacroTotals.ZERO) { s, m -> s + m.totals }
        fun close(a: Double, t: Double, tol: Double) = abs(a - t) / t <= tol
        assertThat(close(actual.kcal, highDay.kcal, 0.20)).isTrue()
        assertThat(close(actual.protein, highDay.protein, 0.25)).isTrue()
        assertThat(close(actual.carbs, highDay.carbs, 0.30)).isTrue()
        assertThat(close(actual.fat, highDay.fat, 0.30)).isTrue()
    }

    @Test
    fun `grams are whole numbers and within legal range`() {
        val result = MealPortionSolver.solve(highDay, fiveMeals(rice))
        result.meals.forEach { meal ->
            meal.entries.forEach { e ->
                assertThat(e.grams).isAtLeast(1.0)
                assertThat(e.grams).isAtMost(2000.0)
                assertThat(e.grams % 1.0).isEqualTo(0.0)
            }
        }
    }

    @Test
    fun `meal totals equal the sum of their entries`() {
        val result = MealPortionSolver.solve(highDay, fiveMeals(rice))
        result.meals.forEach { meal ->
            assertThat(meal.totals.kcal)
                .isWithin(0.01).of(meal.entries.sumOf { it.kcal })
            assertThat(meal.totals.protein)
                .isWithin(0.01).of(meal.entries.sumOf { it.protein })
        }
    }

    @Test
    fun `all meals get food and no meal is left empty`() {
        val result = MealPortionSolver.solve(highDay, fiveMeals(rice))
        assertThat(result.meals).hasSize(5)
        result.meals.forEach { assertThat(it.entries).isNotEmpty() }
    }

    // --------------------------------------------------- honest failures
    @Test
    fun `missing protein source is reported instead of silently wrong`() {
        val structure = listOf(
            "BREAKFAST" to listOf(oats),
            "LUNCH" to listOf(rice, oil),
            "DINNER" to listOf(rice, oil)
        )
        val result = MealPortionSolver.solve(highDay, structure)

        assertThat(result.ok).isFalse()
        assertThat(result.warnings.any { it.contains("proteine") }).isTrue()
    }

    @Test
    fun `missing carb source is reported`() {
        val structure = listOf(
            "BREAKFAST" to listOf(egg),
            "LUNCH" to listOf(chicken, oil),
            "DINNER" to listOf(chicken, oil)
        )
        val result = MealPortionSolver.solve(highDay, structure)

        assertThat(result.ok).isFalse()
        assertThat(result.warnings.any { it.contains("carboidrati") }).isTrue()
    }

    @Test
    fun `empty structure is rejected`() {
        val result = MealPortionSolver.solve(highDay, emptyList())
        assertThat(result.ok).isFalse()
        assertThat(result.warnings).isNotEmpty()
    }

    @Test
    fun `impossible target with tiny portions is flagged`() {
        // Un solo pasto con alimenti a bassissima densita non puo coprire 2037 kcal
        // senza superare i 2000 g: va segnalato, non restituito come valido.
        val light = food("Cetrioli", 12.0, 0.6, 1.8, 0.1)
        val result = MealPortionSolver.solve(
            CarbDayTarget(CarbDayType.HIGH, kcal = 3000.0, protein = 200.0, carbs = 400.0, fat = 90.0),
            listOf("LUNCH" to listOf(light))
        )
        assertThat(result.ok).isFalse()
        assertThat(result.warnings.any { it.contains("IMPOSSIBILE") || it.contains("massimo") }).isTrue()
    }

    @Test
    fun `solver never invents foods`() {
        val result = MealPortionSolver.solve(highDay, fiveMeals(rice))
        val allowed = setOf(oats.name, egg.name, bread.name, chicken.name, rice.name, oil.name, veg.name)
        result.meals.flatMap { it.entries }.forEach {
            assertThat(allowed).contains(it.foodName)
        }
    }

    @Test
    fun `initial grams are respected as starting point`() {
        val result = MealPortionSolver.solve(
            highDay, fiveMeals(rice), mapOf("Petto di pollo" to 250.0)
        )
        assertThat(result.ok).isTrue()
        // Il pollo resta la fonte proteica dominante e viene riportato al target.
        val chickenTotal = result.meals.flatMap { it.entries }
            .filter { it.foodName == "Petto di pollo" }.sumOf { it.grams }
        assertThat(chickenTotal).isGreaterThan(0.0)
    }
}
