package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.nutrition.CatalogNameResolver
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Il programma dell'utente veniva respinto con "alimento 'Uova intere' non
 * presente nel database" mentre "Uovo intero di gallina" era in catalogo.
 * Questi casi sono i nomi realmente rifiutati, presi dal messaggio d'errore.
 */
class CatalogNameResolverTest {

    private fun food(name: String) = FoodItem(
        id = name.hashCode().toLong(),
        name = name,
        category = "test",
        kcalPer100g = 100.0,
        proteinPer100g = 10.0,
        carbsPer100g = 10.0,
        fatPer100g = 2.0,
        source = "CREA"
    )

    private val catalog = listOf(
        "Petto di pollo", "Fesa di tacchino", "Fesa di manzo", "Uovo intero di gallina",
        "Albume d'uovo", "Merluzzo", "Salmone", "Tonno in scatola al naturale", "Branzino",
        "Yogurt greco 0% grassi", "Yogurt bianco magro", "Fiocchi di latte (cottage)",
        "Formaggio grana padano", "Riso bianco (crudo)", "Riso venere (crudo)",
        "Riso integrale (crudo)", "Quinoa (cruda)", "Fiocchi d'avena", "Pane integrale",
        "Olio extravergine d'oliva"
    ).map { food(it) }

    @Test
    fun `i nomi rifiutati dall'app ora si risolvono sul catalogo`() {
        val casi = mapOf(
            "Uova intere" to "Uovo intero di gallina",
            "Riso" to "Riso bianco (crudo)",
            "Fiocchi di latte" to "Fiocchi di latte (cottage)",
            "Quinoa" to "Quinoa (cruda)",
            "Grana padano" to "Formaggio grana padano",
            "Yogurt greco" to "Yogurt greco 0% grassi",
            "Tonno al naturale" to "Tonno in scatola al naturale",
            "Trancio di salmone" to "Salmone",
            "Riso venere" to "Riso venere (crudo)",
            "Tagliata di manzo" to "Fesa di manzo",
            "Petto di pollo" to "Petto di pollo"
        )
        casi.forEach { (scritto, atteso) ->
            val risolto = CatalogNameResolver.resolveFood(scritto, catalog)
            assertThat(risolto?.name).isEqualTo(atteso)
        }
    }

    @Test
    fun `non confonde prodotti diversi che condividono una parola`() {
        // "Latte d'avena" non e' "Fiocchi d'avena": sono alimenti diversi per
        // macro e uso. Meglio nessuna corrispondenza che una sbagliata.
        val catalogWithOats = catalog + food("Latte d'avena")
        val risolto = CatalogNameResolver.resolveFood("Latte d'avena", catalogWithOats)
        assertThat(risolto?.name).isEqualTo("Latte d'avena")
        assertThat(CatalogNameResolver.resolveFood("Latte di soia", catalogWithOats)).isNull()
    }

    @Test
    fun `la tolleranza sui nomi non reintroduce un alimento escluso`() {
        // Il risolutore cerca SOLO nella lista che riceve, gia filtrata per
        // allergie ed esclusioni: un nome simile non puo far rientrare un
        // alimento vietato. Qui la lista non contiene uova (allergia).
        val senzaUova = catalog.filterNot { it.name.contains("Uovo", ignoreCase = true) }
        assertThat(CatalogNameResolver.resolveFood("Uova intere", senzaUova)).isNull()
    }

    @Test
    fun `un alimento davvero assente resta assente`() {
        // Marmellata e mango non sono in catalogo: il risolutore non deve
        // inventare un sostituto, altrimenti le calorie del piano sarebbero false.
        listOf("Marmellata", "Mango", "Frutti di bosco").forEach { nome ->
            assertThat(CatalogNameResolver.resolveFood(nome, catalog)).isNull()
        }
    }

    @Test
    fun `il nome esatto ha la precedenza su ogni somiglianza`() {
        val risolto = CatalogNameResolver.resolveFood("Riso venere", catalog)
        assertThat(risolto?.name).isEqualTo("Riso venere (crudo)")
    }

    @Test
    fun `le suggerimenti aiutano a correggere senza applicare la correzione`() {
        val hint = CatalogNameResolver.foodSuggestions("Uova intere", catalog)
        assertThat(hint).isNotEmpty()
        assertThat(hint.first()).isEqualTo("Uovo intero di gallina")
    }
}
