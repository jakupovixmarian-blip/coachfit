package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Lo strumento di consultazione deve dare al modello la metodologia quando la
 * chiede, citando sempre la fonte. Il corpus non sta nel prompt: sta qui.
 */
class ConsultInvictusToolTest {

    private val tool = ConsultInvictusTool()

    private fun call(area: String, argomento: String? = null, limite: Int? = null) = runBlocking {
        val args: JsonObject = buildJsonObject {
            put("area", area)
            argomento?.let { put("argomento", it) }
            limite?.let { put("limite", it) }
        }
        Json.parseToJsonElement(tool.execute(args)).jsonObject
    }

    @Test
    fun `senza argomento restituisce il quadro di ogni tema`() {
        val result = call("allenamento")
        assertThat(result["ok"]!!.jsonPrimitive.content).isEqualTo("true")
        val regole = result["regole"]!!.jsonArray
        assertThat(regole.size).isAtLeast(5)
        // Un tema per voce: serve a orientarsi, non a leggere tutto.
        val temi = regole.map { it.jsonObject["tema"]!!.jsonPrimitive.content }
        assertThat(temi.toSet().size).isEqualTo(temi.size)
    }

    @Test
    fun `ogni regola restituita cita la fonte con url`() {
        val result = call("allenamento", "volume")
        val regole = result["regole"]!!.jsonArray
        assertThat(regole.size).isAtLeast(1)
        regole.forEach { r ->
            val obj = r.jsonObject
            assertThat(obj["regola"]!!.jsonPrimitive.content).isNotEmpty()
            assertThat(obj["fonte"]!!.jsonPrimitive.content).isNotEmpty()
            assertThat(obj["url"]!!.jsonPrimitive.content)
                .startsWith("https://www.projectinvictus.it/")
        }
    }

    @Test
    fun `la nutrizione risponde su un macro specifico`() {
        val result = call("nutrizione", "proteine")
        assertThat(result["ok"]!!.jsonPrimitive.content).isEqualTo("true")
        val regole = result["regole"]!!.jsonArray
        assertThat(regole.size).isAtLeast(3)
        // Il filtro deve restituire attinenza, non il corpus intero.
        val totale = result["totaleTrovate"]!!.jsonPrimitive.int
        assertThat(regole.size).isAtMost(totale)
    }

    @Test
    fun `le tecniche riportano come si eseguono`() {
        val result = call("tecniche", "drop")
        val tecniche = result["tecniche"]!!.jsonArray
        assertThat(tecniche.size).isAtLeast(1)
        val drop = tecniche.first().jsonObject
        assertThat(drop["tecnica"]!!.jsonPrimitive.content.lowercase()).contains("drop")
        assertThat(drop["come"]!!.jsonArray.size).isAtLeast(1)
    }

    @Test
    fun `gli esercizi si cercano per muscolo`() {
        val result = call("esercizi", "pettorali")
        assertThat(result["ok"]!!.jsonPrimitive.content).isEqualTo("true")
        val esercizi = result["esercizi"]!!.jsonArray
        assertThat(esercizi.size).isAtLeast(3)
        esercizi.forEach { e ->
            val muscoli = e.jsonObject["muscoli"]!!.jsonArray.map { it.jsonPrimitive.content }
            assertThat(muscoli).contains("pettorali")
        }
    }

    @Test
    fun `il limite viene rispettato`() {
        val result = call("esercizi", "pettorali", limite = 2)
        assertThat(result["esercizi"]!!.jsonArray.size).isEqualTo(2)
    }

    @Test
    fun `un area sconosciuta produce errore leggibile non un crash`() {
        val result = call("astrologia")
        assertThat(result["ok"]!!.jsonPrimitive.content).isEqualTo("false")
        assertThat(result["errore"]!!.jsonPrimitive.content).contains("astrologia")
    }

    @Test
    fun `un argomento senza risultati lo dice invece di inventare`() {
        val result = call("allenamento", "termogenesi quantistica")
        assertThat(result["ok"]!!.jsonPrimitive.content).isEqualTo("false")
        assertThat(result["errore"]!!.jsonPrimitive.content).contains("termogenesi quantistica")
    }
}
