package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Difetto segnalato: la dicitura "(crudo)" compariva su alimenti che si pesano
 * pronti al consumo, come affettati e pane. Il default del catalogo era
 * [FoodUnit.RAW] per tutte le voci, senza eccezioni.
 */
class FoodUnitLabelTest {

    private val foods = FoodSeedData.foods

    private fun unitOf(name: String) = foods.first { it.name == name }.defaultUnit

    @Test
    fun `salumi e pane sono pesati tal quale`() {
        assertThat(unitOf("Bresaola")).isEqualTo(FoodUnit.AS_SOLD)
        assertThat(unitOf("Prosciutto crudo sgrassato")).isEqualTo(FoodUnit.AS_SOLD)
        assertThat(unitOf("Pane bianco")).isEqualTo(FoodUnit.AS_SOLD)
        assertThat(unitOf("Pane integrale")).isEqualTo(FoodUnit.AS_SOLD)
        assertThat(unitOf("Fette biscottate")).isEqualTo(FoodUnit.AS_SOLD)
    }

    @Test
    fun `latticini e yogurt sono pesati tal quale`() {
        assertThat(unitOf("Yogurt greco 0% grassi")).isEqualTo(FoodUnit.AS_SOLD)
        assertThat(unitOf("Ricotta vaccina")).isEqualTo(FoodUnit.AS_SOLD)
    }

    @Test
    fun `carne e cereali da cuocere restano crudo`() {
        assertThat(unitOf("Petto di pollo")).isEqualTo(FoodUnit.RAW)
        assertThat(unitOf("Riso basmati (crudo)")).isEqualTo(FoodUnit.RAW)
        assertThat(unitOf("Pasta di semola (cruda)")).isEqualTo(FoodUnit.RAW)
    }

    @Test
    fun `nessun alimento pronto al consumo e marcato crudo`() {
        val readyToEat = listOf("Bresaola", "Prosciutto crudo sgrassato", "Pane bianco", "Pane integrale")
        readyToEat.forEach { name ->
            assertThat(unitOf(name)).isNotEqualTo(FoodUnit.RAW)
        }
    }
}
