package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiProgramProposalJson
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Forme JSON realmente prodotte dai modelli, non inventate dal test.
 *
 * Ogni variante qui sotto corrisponde a un modo con cui un modello scrive le
 * voci di un pasto. Il parser deve accettarle tutte: una sola forma accettata
 * significa che il modello, cambiando provider o giro, fa fallire il piano con
 * "nessun alimento" su tutti i pasti.
 */
class RealPayloadShapeCollaudoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private fun decodeMeals(mealsJson: String) = """
        { "nutrition": { "days": [ { "dayIndex": 0, "dayType": "HIGH", "meals": [ $mealsJson ] } ] } }
    """.trimIndent().let { AiProgramProposalJson.decode(json, it)!!.nutrition.days.first().meals }

    @Test
    fun `chiave voci con i campi del solver`() {
        val meals = decodeMeals(
            """
            { "slot": "BREAKFAST", "totaleKcal": 520,
              "voci": [ { "foodName": "Albume d'uovo", "grams": 200, "kcal": 104,
                          "proteine": 22, "carboidrati": 1, "grassi": 0 } ] }
            """.trimIndent()
        )
        assertThat(meals.single().entries.single().foodName).isEqualTo("Albume d'uovo")
        assertThat(meals.single().entries.single().grams).isEqualTo(200.0)
    }

    @Test
    fun `chiave foods`() {
        val meals = decodeMeals(
            """{ "slot": "LUNCH", "foods": [ { "name": "Riso bianco (crudo)", "grams": 90 } ] }"""
        )
        assertThat(meals.single().entries.single().foodName).isEqualTo("Riso bianco (crudo)")
    }

    @Test
    fun `chiave items`() {
        val meals = decodeMeals(
            """{ "slot": "DINNER", "items": [ { "alimento": "Merluzzo", "grammi": 180 } ] }"""
        )
        assertThat(meals.single().entries.single().foodName).isEqualTo("Merluzzo")
        assertThat(meals.single().entries.single().grams).isEqualTo(180.0)
    }

    @Test
    fun `chiave alimenti`() {
        val meals = decodeMeals(
            """{ "slot": "MORNING_SNACK", "alimenti": [ { "food": "Noci", "quantity": 20 } ] }"""
        )
        assertThat(meals.single().entries.single().foodName).isEqualTo("Noci")
        assertThat(meals.single().entries.single().grams).isEqualTo(20.0)
    }

    /** Il caso del difetto: entries vuoto non deve piu essere silenzioso. */
    @Test
    fun `la forma canonica entries resta supportata`() {
        val meals = decodeMeals(
            """{ "slot": "AFTERNOON_SNACK", "entries": [ { "foodName": "Mela", "grams": 150 } ] }"""
        )
        assertThat(meals.single().entries.single().foodName).isEqualTo("Mela")
    }
}
