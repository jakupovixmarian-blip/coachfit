package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Verifica l'integrita del database alimentare seed: nessun duplicato, valori
 * nutrizionali plausibili e calorie coerenti con i macronutrienti dichiarati.
 *
 * Le calorie teoriche sono 4*P + 4*C + 9*G. Le tabelle CREA/USDA arrotondano e
 * includono fibra/alcol, quindi si accetta una tolleranza percentuale.
 */
class FoodSeedDataTest {

    private val foods = FoodSeedData.foods

    @Test
    fun `nessun alimento duplicato per nome`() {
        val duplicates = foods
            .groupBy { FoodItem.normalizeName(it.name) }
            .filterValues { it.size > 1 }
            .keys
        assertThat(duplicates).isEmpty()
    }

    @Test
    fun `ogni alimento dichiara una fonte verificabile`() {
        val withoutSource = foods.filter { it.source.isBlank() }
        assertThat(withoutSource).isEmpty()
        val validSources = setOf(FoodSeedData.SRC_CREA, FoodSeedData.SRC_USDA)
        val unknown = foods.filter { it.source !in validSources }.map { it.name }
        assertThat(unknown).isEmpty()
    }

    @Test
    fun `nessun valore nutrizionale negativo`() {
        val invalid = foods.filter {
            it.kcalPer100g < 0 || it.proteinPer100g < 0 ||
                it.carbsPer100g < 0 || it.fatPer100g < 0 || it.fiberPer100g < 0
        }.map { it.name }
        assertThat(invalid).isEmpty()
    }

    @Test
    fun `calorie coerenti con i macronutrienti entro tolleranza`() {
        val mismatched = foods.filter { food ->
            val theoretical = 4.0 * food.proteinPer100g +
                4.0 * food.carbsPer100g +
                9.0 * food.fatPer100g
            if (theoretical <= 0.0) return@filter false
            // La fibra (2 kcal/g nelle tabelle CREA) e gli arrotondamenti rendono
            // le calorie dichiarate inferiori alla somma 4/4/9: tolleranza 25%.
            abs(theoretical - food.kcalPer100g) / theoretical > 0.25
        }.map { "${it.name}: ${it.kcalPer100g} kcal vs ${"%.0f".format(4.0 * it.proteinPer100g + 4.0 * it.carbsPer100g + 9.0 * it.fatPer100g)} teoriche" }
        assertThat(mismatched).isEmpty()
    }

    @Test
    fun `database contiene un numero significativo di alimenti`() {
        assertThat(foods.size).isAtLeast(150)
    }

    @Test
    fun `le sostituzioni proposte esistono nel database`() {
        val names = foods.map { FoodItem.normalizeName(it.name) }.toSet()
        val dangling = foods.flatMap { food ->
            food.substituteNames
                .map { FoodItem.normalizeName(it) }
                .filter { it !in names }
                .map { "${food.name} -> $it" }
        }
        assertThat(dangling).isEmpty()
    }
}
