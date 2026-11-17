package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.nutrition.FoodSubstitutionEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Collaudo del motore di sostituzione alimentare.
 *
 * Difetto segnalato: una sostituzione legittima (albume d'uovo -> yogurt greco
 * 0% grassi) viene marcata "non compatibile". Entrambi sono fonti proteiche
 * magre: lo scambio deve essere ammesso, con quantita ricalcolate.
 */
class SubstitutionCompatibilityCollaudoTest {

    private fun food(name: String, cat: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(id = name.hashCode().toLong(), name = name, category = cat,
            kcalPer100g = kcal, proteinPer100g = p, carbsPer100g = c, fatPer100g = f, source = "TEST")

    private val albume = food("Albume d'uovo", "Uova", 43.0, 10.7, 0.7, 0.2)
    private val yogurtGreco = food("Yogurt greco 0% grassi", "Latticini", 59.0, 10.0, 3.6, 0.4)

    @Test
    fun `albume sostituito con yogurt greco e compatibile`() {
        // Colazione tipica: albume + avena. Il pasto ha gia carboidrati dall'avena.
        val avena = MacroTotals(kcal = 190.0, protein = 6.5, carbs = 33.0, fat = 3.5)
        val dayTarget = MacroTotals(kcal = 2000.0, protein = 160.0, carbs = 200.0, fat = 60.0)

        val result = FoodSubstitutionEngine.evaluate(
            FoodSubstitutionEngine.Request(
                original = albume,
                originalGrams = 200.0,
                substitute = yogurtGreco,
                mealOther = avena,
                dayPlannedOther = avena,
                dayTarget = dayTarget
            )
        )

        assertThat(result.status)
            .isNotEqualTo(FoodSubstitutionEngine.Status.INCOMPATIBLE)
        assertThat(result.isEquivalent).isTrue()
    }

    @Test
    fun `lo yogurt greco resta una fonte proteica equivalente`() {
        val result = FoodSubstitutionEngine.evaluate(
            FoodSubstitutionEngine.Request(
                original = albume,
                originalGrams = 200.0,
                substitute = yogurtGreco,
                mealOther = MacroTotals(0.0, 0.0, 0.0, 0.0),
                dayPlannedOther = MacroTotals(0.0, 0.0, 0.0, 0.0),
                dayTarget = MacroTotals(2000.0, 160.0, 200.0, 60.0)
            )
        )
        // Le proteine fornite devono restare vicine all'originale (21.4 g).
        assertThat(result.substituteMacros.protein).isWithin(2.0).of(21.4)
    }
}
