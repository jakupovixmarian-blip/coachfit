package com.fitcoach.app.audit

import com.fitcoach.app.domain.nutrition.CatalogNameResolver
import com.fitcoach.app.domain.nutrition.FoodSeedData

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L'AI scrive i nomi degli alimenti in italiano corrente ("Avena", "Yogurt
 * greco", "Petto di pollo"). Se il resolver non li riconosce, la validazione
 * scarta un programma alimentare corretto e l'utente resta senza piano.
 * Questi nomi sono quelli realmente prodotti dal modello in produzione.
 */
class CatalogResolutionAuditTest {

    private val foods = FoodSeedData.foods

    @Test
    fun `i nomi correnti del modello risolvono sul catalogo`() {
        val casi = listOf(
            "Avena" to "Fiocchi d'avena",
            "Yogurt greco" to "Yogurt greco 0% grassi",
            "Petto di pollo" to "Petto di pollo",
            "Riso" to "Riso bianco (crudo)",
            "Uova" to "Uovo intero di gallina",
            "Olio d'oliva" to "Olio extravergine d'oliva",
            "Pane" to "Pane bianco",
            "Pasta" to "Pasta di semola (cruda)",
            "Salmone" to "Salmone",
            "Mandorle" to "Mandorle",
        )

        val falliti = casi.mapNotNull { (scritto, atteso) ->
            val risolto = CatalogNameResolver.resolveFood(scritto, foods)?.name
            if (risolto == null) "$scritto -> NESSUN MATCH (atteso $atteso)"
            else if (risolto != atteso) "$scritto -> $risolto (atteso $atteso)"
            else null
        }

        assertTrue("Alimenti scritti dal modello non risolti: $falliti", falliti.isEmpty())
    }

    @Test
    fun `il resolver non inventa un alimento per nomi inesistenti`() {
        val inventati = listOf("Unicorno in salmì", "Polvere di stelle", "Zzzzqqq")
        inventati.forEach { nome ->
            assertNull(
                "'$nome' non deve risolvere su un alimento reale",
                CatalogNameResolver.resolveFood(nome, foods)
            )
        }
    }
}
