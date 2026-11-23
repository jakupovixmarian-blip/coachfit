package com.fitcoach.app.audit

import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.coach.CoachActionParser
import com.fitcoach.app.domain.coach.tools.JsonArgs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il parser delle azioni legge JSON prodotto da un LLM, non da un umano:
 * l'input e' scritto da una macchina e puo' essere troncato, annidato,
 * con numeri al posto delle stringhe o con testo attorno. Un errore qui
 * significa una proposta persa o, peggio, JSON grezzo mostrato in chat.
 */
class CoachActionParserAdversarialTest {

    // ---------- Estrazione del blocco ----------

    @Test
    fun `un blocco json in code fence viene estratto`() {
        val raw = """
            Ecco la proposta.

            ```json
            {"action": "DELOAD", "title": "Scarico"}
            ```
        """.trimIndent()
        val r = CoachActionParser.parse(raw)
        assertNotNull("Il blocco deve essere riconosciuto", r.proposedAction)
        assertEquals("DELOAD", r.proposedAction!!.action)
        assertEquals("Scarico", r.proposedAction!!.title)
    }

    @Test
    fun `un json senza code fence viene estratto`() {
        val r = CoachActionParser.parse("""Ok: {"action": "DELOAD"} fatto.""")
        assertNotNull("Il JSON isolato deve essere riconosciuto", r.proposedAction)
        assertEquals("DELOAD", r.proposedAction!!.action)
    }

    @Test
    fun `il testo mostrato non deve contenere il json grezzo`() {
        val r = CoachActionParser.parse("""Propongo questo: {"action": "DELOAD"} per recuperare.""")
        assertNotNull(r.proposedAction)
        assertTrue(
            "L'utente non deve vedere il JSON in chat: era '${r.text}'",
            !r.text.contains("{") && !r.text.contains("action")
        )
    }

    @Test
    fun `il testo attorno al blocco viene conservato`() {
        val raw = "Prima parte.\n```json\n{\"action\": \"DELOAD\"}\n```\nDopo."
        val r = CoachActionParser.parse(raw)
        assertTrue("Il testo prima deve restare", r.text.contains("Prima parte"))
        assertTrue("Il testo dopo deve restare", r.text.contains("Dopo"))
    }

    @Test
    fun `un json annidato non viene troncato`() {
        val raw = """```json
            {"action": "ADJUST_CALORIES", "payload": {"kcal": "2200", "days": "7"}}
            ```"""
        val r = CoachActionParser.parse(raw)
        assertNotNull("Le graffe annidate non devono rompere l'estrazione", r.proposedAction)
        assertEquals("2200", r.proposedAction!!.payload["kcal"])
    }

    @Test
    fun `le graffe dentro una stringa non chiudono il blocco`() {
        val raw = """```json
            {"action": "DELOAD", "rationale": "carico {alto} e {basso}"}
            ```"""
        val r = CoachActionParser.parse(raw)
        assertNotNull(r.proposedAction)
        assertEquals("carico {alto} e {basso}", r.proposedAction!!.rationale)
    }

    @Test
    fun `le virgolette escapate non rompono il conteggio`() {
        val raw = """```json
            {"action": "DELOAD", "rationale": "l'atleta ha detto \"basta\" ieri"}
            ```"""
        val r = CoachActionParser.parse(raw)
        assertNotNull(r.proposedAction)
        assertTrue(r.proposedAction!!.rationale.contains("basta"))
    }

    @Test
    fun `un json troncato non produce un'azione parziale`() {
        val r = CoachActionParser.parse("""```json
            {"action": "DELOAD", "title": "Scar
            ```""")
        assertNull("Un JSON incompleto non deve diventare un'azione", r.proposedAction)
    }

    @Test
    fun `un'azione sconosciuta non viene accettata`() {
        val r = CoachActionParser.parse("""{"action": "DELETE_ALL_DATA"}""")
        assertNull(
            "Solo le azioni previste possono essere proposte",
            r.proposedAction
        )
    }

    @Test
    fun `un'azione in minuscolo viene normalizzata`() {
        val r = CoachActionParser.parse("""{"action": "deload"}""")
        assertNotNull(r.proposedAction)
        assertEquals("DELOAD", r.proposedAction!!.action)
    }

    @Test
    fun `un'azione con spazi non viene accettata`() {
        val r = CoachActionParser.parse("""{"action": " DELOAD "}""")
        assertNull(
            "Uno spazio rende l'azione non riconoscibile: meglio rifiutare che indovinare",
            r.proposedAction
        )
    }

    @Test
    fun `una risposta senza json resta testo semplice`() {
        val r = CoachActionParser.parse("Hai bisogno di piu' proteine.")
        assertNull(r.proposedAction)
        assertEquals("Hai bisogno di piu' proteine.", r.text)
    }

    @Test
    fun `una risposta vuota non produce un'azione`() {
        val r = CoachActionParser.parse("")
        assertNull(r.proposedAction)
        assertEquals("", r.text)
    }

    @Test
    fun `un payload con numeri al posto delle stringhe non perde la proposta`() {
        // I modelli scrivono spesso 2200 invece di "2200": il payload e'
        // Map<String, String>, quindi il decode fallisce e la proposta sparisce.
        val raw = """{"action": "ADJUST_CALORIES", "payload": {"kcal": 2200}}"""
        val r = CoachActionParser.parse(raw)
        assertNotNull(
            "Un numero nel payload non deve far perdere tutta la proposta",
            r.proposedAction
        )
        assertEquals("2200", r.proposedAction!!.payload["kcal"])
    }

    @Test
    fun `un campo testuale nullo non fa perdere la proposta`() {
        val raw = """{"action": "DELOAD", "title": null}"""
        val r = CoachActionParser.parse(raw)
        assertNotNull("Un campo nullo non deve invalidare l'azione", r.proposedAction)
    }

    @Test
    fun `un dominio sconosciuto non fa crashare`() {
        val r = CoachActionParser.parse("""{"action": "DELOAD", "domain": "SPAZIO"}""")
        assertNotNull(r.proposedAction)
        assertEquals(
            "Un dominio ignoto ricade su nutrizione invece di lanciare",
            com.fitcoach.app.domain.model.ChangeDomain.NUTRITION,
            r.proposedAction!!.changeDomain
        )
    }

    @Test
    fun `due blocchi json usano il primo valido`() {
        val raw = """
            ```json
            {"action": "DELOAD"}
            ```
            ```json
            {"action": "MOVE_WORKOUT"}
            ```
        """.trimIndent()
        val r = CoachActionParser.parse(raw)
        assertEquals("DELOAD", r.proposedAction!!.action)
    }

    @Test
    fun `una graffa isolata prima del blocco non confonde il parser`() {
        val raw = """Ecco {non e' json} e poi ```json
            {"action": "DELOAD"}
            ```"""
        val r = CoachActionParser.parse(raw)
        assertNotNull(r.proposedAction)
        assertEquals("DELOAD", r.proposedAction!!.action)
    }

    // ---------- JsonArgs ----------

    private fun args(json: String) =
        JsonArgs(Json.parseToJsonElement(json) as JsonObject)

    @Test
    fun `un campo assente restituisce null e non stringa vuota`() {
        val a = args("{}")
        assertNull(a.string("manca"))
        assertNull(a.int("manca"))
        assertNull(a.double("manca"))
        assertNull(a.boolean("manca"))
    }

    @Test
    fun `un campo null restituisce null`() {
        val a = args("""{"x": null}""")
        assertNull(a.string("x"))
        assertNull(a.int("x"))
        assertNull(a.boolean("x"))
    }

    @Test
    fun `un numero letto come stringa funziona`() {
        assertEquals("150", args("""{"g": 150}""").string("g"))
    }

    @Test
    fun `un intero scritto come stringa viene letto`() {
        assertEquals(150, args("""{"g": "150"}""").int("g"))
    }

    @Test
    fun `un testo al posto di un numero restituisce null`() {
        assertNull(args("""{"g": "abbastanza"}""").int("g"))
    }

    @Test
    fun `un decimale letto come intero restituisce null invece di troncare`() {
        assertNull(
            "Troncare 3,7 a 3 cambierebbe il dato senza dirlo",
            args("""{"g": 3.7}""").int("g")
        )
    }

    @Test
    fun `una lista mista scarta gli elementi non testuali`() {
        val a = args("""{"l": ["a", 1, null, {"x": 1}, "b"]}""")
        assertEquals(listOf("a", "1", "b"), a.strings("l"))
    }

    @Test
    fun `una lista di interi scarta i valori non numerici`() {
        val a = args("""{"l": [1, "2", "tre", null, 4]}""")
        assertEquals(listOf(1, 2, 4), a.ints("l"))
    }

    @Test
    fun `una lista vuota restituisce lista vuota e non null`() {
        assertEquals(emptyList<String>(), args("""{"l": []}""").strings("l"))
    }

    @Test
    fun `un oggetto al posto di una lista non lancia`() {
        assertEquals(emptyList<String>(), args("""{"l": {"a": 1}}""").strings("l"))
    }

    @Test
    fun `objectAt ignora un valore che non e' oggetto`() {
        assertNull(args("""{"o": [1,2]}""").objectAt("o"))
        assertNull(args("""{"o": "testo"}""").objectAt("o"))
        assertNotNull(args("""{"o": {"a": 1}}""").objectAt("o"))
    }

    @Test
    fun `un numero negativo molto grande resta leggibile`() {
        assertEquals(-2_147_483_648, args("""{"n": -2147483648}""").int("n"))
    }

    @Test
    fun `un numero oltre il limite intero non viene troncato`() {
        assertNull(
            "Oltre Int.MAX_VALUE non e' un intero valido",
            args("""{"n": 99999999999}""").int("n")
        )
    }

    @Test
    fun `un booleano scritto come stringa viene letto`() {
        assertEquals(true, args("""{"b": "true"}""").boolean("b"))
        assertNull(args("""{"b": "si"}""").boolean("b"))
    }

    @Test
    fun `raw restituisce l'oggetto originale`() {
        val a = args("""{"a": 1, "b": 2}""")
        assertEquals(2, a.raw().size)
    }
}
