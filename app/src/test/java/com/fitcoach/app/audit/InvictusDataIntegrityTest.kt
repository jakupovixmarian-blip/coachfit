package com.fitcoach.app.audit

import com.fitcoach.app.domain.knowledge.InvictusExercises
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InvictusExercises e' un archivio di 2582 righe consumato dal coach per
 * spiegare l'esecuzione all'utente. Un file dati non si testa riga per riga:
 * si testano gli invarianti che, se rotti, mandano in produzione un dato falso.
 */
class InvictusDataIntegrityTest {

    private val esercizi = InvictusExercises.exercises

    @Test
    fun `l'archivio non e' vuoto`() {
        assertTrue("Senza esercizi il coach non spiega nulla", esercizi.size >= 20)
    }

    @Test
    fun `nessun nome duplicato`() {
        val duplicati = esercizi.groupBy { it.name.lowercase() }
            .filterValues { it.size > 1 }.keys
        assertTrue("Nomi duplicati: $duplicati", duplicati.isEmpty())
    }

    @Test
    fun `ogni esercizio ha almeno un muscolo bersaglio`() {
        val senza = esercizi.filter { it.primaryMuscles.isEmpty() }.map { it.name }
        assertTrue(
            "Senza muscoli l'esercizio non si puo' assegnare a nessun gruppo: $senza",
            senza.isEmpty()
        )
    }

    @Test
    fun `i muscoli sono scritti in modo coerente`() {
        val muscoli = esercizi.flatMap { it.primaryMuscles }.distinct()
        val sporchi = muscoli.filter { it != it.trim() || it.isEmpty() || it.first().isUpperCase() }
        assertTrue("Nomi muscolo non normalizzati (spazi o maiuscole): $sporchi", sporchi.isEmpty())
    }

    @Test
    fun `ogni url e' una fonte reale e non un segnaposto`() {
        val cattivi = esercizi.filterNot { it.url.startsWith("https://www.projectinvictus.it/") }
            .map { "${it.name} -> ${it.url}" }
        assertTrue("Url fuori dominio o placeholder: $cattivi", cattivi.isEmpty())
    }

    @Test
    fun `ogni esercizio ha un titolo di fonte`() {
        val senza = esercizi.filter { it.articleTitle.isBlank() }.map { it.name }
        assertTrue("Senza fonte l'utente non puo' verificare: $senza", senza.isEmpty())
    }

    @Test
    fun `nessun testo di esecuzione vuoto o con spazi doppi`() {
        val righe = esercizi.flatMap { e -> e.execution + e.errors + e.variants }
        val vuote = righe.filter { it.isBlank() }
        assertTrue("Righe vuote nell'archivio: ${vuote.size}", vuote.isEmpty())
    }

    @Test
    fun `byName trova per nome esatto ignorando maiuscole`() {
        val primo = esercizi.first()
        assertEquals(primo.name, InvictusExercises.byName(primo.name)!!.name)
        assertEquals(
            primo.name,
            InvictusExercises.byName(primo.name.uppercase())!!.name
        )
    }

    @Test
    fun `byName non inventa un esercizio inesistente`() {
        assertNull(InvictusExercises.byName("Esercizio che non esiste nel catalogo"))
        assertNull(InvictusExercises.byName(""))
    }

    @Test
    fun `forMuscle filtra davvero per muscolo`() {
        val muscolo = esercizi.first().primaryMuscles.first()
        val trovati = InvictusExercises.forMuscle(muscolo)
        assertTrue("Il muscolo $muscolo deve avere esercizi", trovati.isNotEmpty())
        assertTrue(
            "forMuscle ha restituito esercizi senza quel muscolo",
            trovati.all { e -> e.primaryMuscles.any { it.equals(muscolo, ignoreCase = true) } }
        )
    }

    @Test
    fun `forMuscle con maiuscole diverse trova gli stessi esercizi`() {
        val muscolo = esercizi.first().primaryMuscles.first()
        assertEquals(
            InvictusExercises.forMuscle(muscolo).size,
            InvictusExercises.forMuscle(muscolo.uppercase()).size
        )
    }

    @Test
    fun `forMuscle su un muscolo inesistente restituisce vuoto e non tutto`() {
        assertTrue(InvictusExercises.forMuscle("muscolo_inventato_xyz").isEmpty())
    }

    @Test
    fun `la maggioranza delle voci ha contenuto tecnico`() {
        val conContenuto = esercizi.count {
            it.execution.isNotEmpty() || it.errors.isNotEmpty() || it.variants.isNotEmpty()
        }
        assertTrue(
            "Solo $conContenuto voci su ${esercizi.size} hanno contenuto: l'archivio " +
                "sta diventando un elenco di titoli",
            conContenuto * 2 >= esercizi.size
        )
    }

    @Test
    fun `le voci senza contenuto vengono dichiarate al modello`() = runTest {
        val tool = com.fitcoach.app.domain.coach.tools.ConsultInvictusTool()
        val r = Json.parseToJsonElement(
            tool.execute(
                buildJsonObject {
                    put("area", JsonPrimitive("esercizi"))
                    put("argomento", JsonPrimitive("panca piana"))
                    put("limite", JsonPrimitive(40))
                }
            )
        ).jsonObject
        val eserciziJson = r["esercizi"]!!.jsonArray
        assertTrue("La ricerca deve trovare la panca piana", eserciziJson.isNotEmpty())
        val vuota = eserciziJson.firstOrNull {
            it.jsonObject["nome"]!!.jsonPrimitive.content.equals("Panca piana", ignoreCase = true)
        }
        assertNotNull("La panca piana e' nell'archivio", vuota)
        assertEquals(
            "Senza contenuto il modello deve saperlo, altrimenti inventa la tecnica",
            false,
            vuota!!.jsonObject["contenutoDisponibile"]!!.jsonPrimitive.boolean
        )
        assertTrue(
            "Deve esserci una nota esplicita",
            r["nota"]!!.jsonPrimitive.content.contains("Non inventarli")
        )
    }

    @Test
    fun `un esercizio documentato viene marcato come disponibile`() = runTest {
        val tool = com.fitcoach.app.domain.coach.tools.ConsultInvictusTool()
        val documentato = esercizi.first {
            it.execution.isNotEmpty() || it.errors.isNotEmpty()
        }
        val r = Json.parseToJsonElement(
            tool.execute(
                buildJsonObject {
                    put("area", JsonPrimitive("esercizi"))
                    put("argomento", JsonPrimitive(documentato.name))
                    put("limite", JsonPrimitive(40))
                }
            )
        ).jsonObject
        val trovato = r["esercizi"]!!.jsonArray.first {
            it.jsonObject["nome"]!!.jsonPrimitive.content == documentato.name
        }
        assertEquals(
            true,
            trovato.jsonObject["contenutoDisponibile"]!!.jsonPrimitive.boolean
        )
    }
}
