package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Gli alimenti graditi devono arrivare al modello che genera il piano.
 *
 * Difetto segnalato: la struttura del programma alimentare standard non
 * considerava gli alimenti preferiti. Causa: il selettore ordinava per fonte
 * (CREA prima, USDA poi) e troncava a 90 voci. Con un catalogo ben piu ampio,
 * un alimento gradito che stava oltre la posizione 90 non entrava mai nel
 * prompt: il modello non poteva usarlo, anche se l'utente lo aveva dichiarato.
 */
class LikedFoodsInGenerationCollaudoTest {

    private val base = MarjanProfile.build()

    private fun food(name: String, source: String = FoodSeedData.SRC_CREA, category: String = "Carne") =
        FoodItem(
            id = 0, name = name, category = category,
            kcalPer100g = 120.0, proteinPer100g = 22.0, carbsPer100g = 0.0,
            fatPer100g = 2.0, source = source
        )

    @Test
    fun `un alimento gradito entra nel prompt anche oltre il tetto`() {
        // 120 alimenti "di riempimento" prima del gradito: senza il nuovo
        // ordinamento il gradito sarebbe stato troncato fuori dall'elenco.
        val filler = (1..120).map { food("Alimento riempimento $it") }
        val liked = food("Salmone fresco")
        val all = filler + liked

        val selected = FoodCatalogSelector.forGeneration(
            all = all,
            profile = base.copy(likedFoods = listOf("salmone")),
            limit = 90
        )

        assertThat(selected.map { it.name }).contains("Salmone fresco")
    }

    @Test
    fun `senza preferenze l'elenco resta quello di prima`() {
        val filler = (1..120).map { food("Alimento riempimento $it") }

        val selected = FoodCatalogSelector.forGeneration(
            all = filler,
            profile = base.copy(likedFoods = emptyList()),
            limit = 90
        )

        assertThat(selected).hasSize(90)
        assertThat(selected.first().name).isEqualTo("Alimento riempimento 1")
    }

    @Test
    fun `l'ordine dei gusti dichiarati viene rispettato`() {
        val all = listOf(food("Merluzzo"), food("Uova intere"), food("Petto di pollo"))

        val selected = FoodCatalogSelector.forGeneration(
            all = all,
            profile = base.copy(likedFoods = listOf("uova", "merluzzo")),
            limit = 90
        )

        // "uova" e' il primo gusto dichiarato, quindi va davanti a "merluzzo".
        assertThat(selected.map { it.name }).containsExactly("Uova intere", "Merluzzo", "Petto di pollo").inOrder()
    }

    @Test
    fun `un alimento gradito ma escluso per allergia non rientra`() {
        // La preferenza non puo scavalcare un vincolo di sicurezza.
        val all = listOf(food("Salmone fresco"), food("Petto di pollo"))

        val selected = FoodCatalogSelector.forGeneration(
            all = all,
            profile = base.copy(likedFoods = listOf("salmone"), allergies = listOf("salmone")),
            limit = 90
        )

        assertThat(selected.map { it.name }).doesNotContain("Salmone fresco")
    }
}
