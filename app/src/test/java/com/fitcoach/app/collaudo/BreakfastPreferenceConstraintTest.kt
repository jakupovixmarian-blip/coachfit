package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.BreakfastConstraint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Difetto segnalato: con colazione "Dolce" nell'anamnesi il piano generato
 * conteneva petto di pollo, riso basmati e olio. La preferenza era scritta nel
 * prompt della chat ma non in quello di generazione e nessun controllo la
 * verificava, quindi restava un consiglio ignorabile.
 */
class BreakfastPreferenceConstraintTest {

    private fun food(name: String, category: String) = FoodItem(
        name = name,
        category = category,
        kcalPer100g = 100.0,
        proteinPer100g = 10.0,
        carbsPer100g = 10.0,
        fatPer100g = 2.0,
        defaultUnit = FoodUnit.RAW,
        source = "test"
    )

    @Test
    fun `colazione dolce rifiuta pollo e riso`() {
        val chicken = food("Petto di pollo", "Carne")
        val violations = BreakfastConstraint.violations(
            BreakfastPreference.SWEET, MealSlot.BREAKFAST, listOf(chicken)
        )
        assertThat(violations).containsExactly(chicken)
    }

    @Test
    fun `colazione dolce accetta avena yogurt e frutta`() {
        val foods = listOf(
            food("Fiocchi d'avena", "Cereali"),
            food("Yogurt greco 0% grassi", "Latticini"),
            food("Mela", "Frutta")
        )
        assertThat(
            BreakfastConstraint.violations(BreakfastPreference.SWEET, MealSlot.BREAKFAST, foods)
        ).isEmpty()
    }

    @Test
    fun `colazione salata rifiuta miele e frutta`() {
        val foods = listOf(food("Miele", "Dolcificanti"), food("Mela", "Frutta"))
        val violations = BreakfastConstraint.violations(
            BreakfastPreference.SAVORY, MealSlot.BREAKFAST, foods
        )
        assertThat(violations).hasSize(2)
    }

    @Test
    fun `colazione salata accetta uova e affettati`() {
        val foods = listOf(food("Uovo intero di gallina", "Uova"), food("Bresaola", "Salumi"))
        assertThat(
            BreakfastConstraint.violations(BreakfastPreference.SAVORY, MealSlot.BREAKFAST, foods)
        ).isEmpty()
    }

    @Test
    fun `il vincolo vale solo per la colazione`() {
        val chicken = food("Petto di pollo", "Carne")
        // Pranzo e cena sono salati per convenzione: il pollo e' corretto li'.
        assertThat(
            BreakfastConstraint.violations(BreakfastPreference.SWEET, MealSlot.LUNCH, listOf(chicken))
        ).isEmpty()
    }

    @Test
    fun `entrambe e nessuna colazione non impongono vincoli`() {
        val chicken = food("Petto di pollo", "Carne")
        assertThat(
            BreakfastConstraint.violations(BreakfastPreference.BOTH, MealSlot.BREAKFAST, listOf(chicken))
        ).isEmpty()
        assertThat(
            BreakfastConstraint.violations(BreakfastPreference.NONE, MealSlot.BREAKFAST, listOf(chicken))
        ).isEmpty()
    }

    @Test
    fun `un alimento non classificato non viene mai bocciato`() {
        // Un falso rifiuto bloccherebbe la generazione: meglio neutrale.
        val unknown = food("Alimento sconosciuto", "Categoria ignota")
        assertThat(
            BreakfastConstraint.violations(BreakfastPreference.SWEET, MealSlot.BREAKFAST, listOf(unknown))
        ).isEmpty()
    }
}
