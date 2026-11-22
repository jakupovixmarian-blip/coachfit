package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiProgramProposalJson
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Il contratto tra solve_meal_portions e submit_full_program.
 *
 * Difetto segnalato dall'utente: submit_full_program rispondeva con tutti i pasti
 * vuoti ("giorno 0, pasto 'BREAKFAST': nessun alimento") e 0 kcal su 7 giorni,
 * pur avendo il modello chiamato il solver delle porzioni.
 *
 * Causa: il solver restituisce le voci di un pasto sotto la chiave "voci", ma il
 * parser di submit_full_program cerca "entries". Il modello copia fedelmente
 * l'output del solver, quindi ogni pasto arrivava senza voci. Con
 * ignoreUnknownKeys = true il decode non falliva: restituiva pasti vuoti in
 * silenzio, e la diagnosi accusava il modello di non aver messo alimenti.
 */
class SolverSubmitContractCollaudoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /** Esattamente la forma che solve_meal_portions stampa nella sua risposta. */
    private fun solverOutputMeal(slot: String, foodName: String, grams: Double) = """
        {
          "slot": "$slot",
          "totaleKcal": 120,
          "voci": [
            { "foodName": "$foodName", "grams": $grams, "kcal": 120,
              "proteine": 22, "carboidrati": 0, "grassi": 3 }
          ]
        }
    """.trimIndent()

    private fun proposalWith(meals: String) = """
        {
          "scope": "NUTRITION",
          "nutrition": { "days": [ { "dayIndex": 0, "dayType": "HIGH", "meals": [ $meals ] } ] },
          "workout": { "split": "CUSTOM", "days": [] }
        }
    """.trimIndent()

    @Test
    fun `le voci prodotte dal solver arrivano al parser di submit_full_program`() {
        val raw = proposalWith(solverOutputMeal("BREAKFAST", "Petto di pollo", 150.0))

        val proposal = AiProgramProposalJson.decode(json, raw)

        assertThat(proposal).isNotNull()
        val meals = proposal!!.nutrition.days.first().meals
        assertThat(meals).hasSize(1)
        // Il cuore del difetto: prima della correzione questo elenco era vuoto.
        assertThat(meals.first().entries).hasSize(1)
        assertThat(meals.first().entries.first().foodName).isEqualTo("Petto di pollo")
        assertThat(meals.first().entries.first().grams).isEqualTo(150.0)
    }

    @Test
    fun `la forma entries continua a funzionare`() {
        val raw = proposalWith(
            """
            {
              "slot": "LUNCH",
              "entries": [ { "foodName": "Riso bianco (crudo)", "grams": 80 } ]
            }
            """.trimIndent()
        )

        val meals = AiProgramProposalJson.decode(json, raw)!!.nutrition.days.first().meals

        assertThat(meals.first().entries).hasSize(1)
        assertThat(meals.first().entries.first().foodName).isEqualTo("Riso bianco (crudo)")
    }

    /**
     * Il secondo difetto: la lista mostrata al modello si fermava a "Noci".
     *
     * Il database restituisce gli alimenti CREA in ordine alfabetico (ORDER BY
     * name), e il catalogo curato ha ~174 voci. Con il troncamento a 60 il
     * modello vedeva Albicocche..Noci: restavano fuori proprio gli alimenti
     * base del piano (Petto di pollo, Riso, Pasta, Uova, Yogurt, Olio), che
     * quindi non poteva scegliere.
     */
    @Test
    fun `la lista di alimenti mostrata al modello contiene gli alimenti base`() {
        // Riproduce l'ordine reale del database (ORDER BY name).
        val all = FoodSeedData.foods.sortedBy { it.name }
        val profile = MarjanProfile.build()
        val visible = FoodCatalogSelector.forGeneration(all, profile)

        val names = visible.map { it.name }.toSet()
        // Una fonte per macro: senza queste il modello non puo comporre un pasto
        // completo e inventa nomi che la validazione rifiuta.
        assertThat(names).contains("Petto di pollo")
        assertThat(names).contains("Riso bianco (crudo)")
        assertThat(names).contains("Pasta di semola (cruda)")
        assertThat(names).contains("Olio extravergine d'oliva")
        assertThat(names).contains("Uovo intero di gallina")
        assertThat(names).contains("Yogurt greco 0% grassi")
    }

    @Test
    fun `la lista mostrata al modello non e troncata a meta alfabeto`() {
        val all = FoodSeedData.foods.sortedBy { it.name }
        val profile = MarjanProfile.build()
        val visible = FoodCatalogSelector.forGeneration(all, profile)

        // Un tetto di 60 voci su un catalogo alfabetico di 174 tagliava tutto
        // cio che viene dopo la N: la selezione deve arrivare in fondo.
        val last = visible.map { it.name }.maxOrNull()!!
        assertThat(last.first()).isAtLeast('V')
    }

    /**
     * La validazione deve usare gli stessi nomi mostrati nel prompt.
     *
     * submit_full_program validava con FoodFilter (catalogo intero) mentre il
     * prompt elencava la selezione di FoodCatalogSelector. Il messaggio di
     * errore "alimenti_validi" poteva quindi indicare nomi mai mostrati.
     */
    @Test
    fun `la selezione per la generazione e un sottoinsieme coerente del catalogo`() {
        val all = FoodSeedData.foods.sortedBy { it.name }
        val profile = MarjanProfile.build()
        val visible = FoodCatalogSelector.forGeneration(all, profile)

        assertThat(visible.size).isAtMost(all.size)
        // Ogni nome mostrato deve esistere nel catalogo: nessun nome inventato.
        val catalogNames = all.map { it.name }.toSet()
        assertThat(catalogNames).containsAtLeastElementsIn(visible.map { it.name })
    }
}
