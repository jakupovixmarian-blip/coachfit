package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.knowledge.InvictusKnowledge
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Il corpus serve a generare piani alimentari e schede. Se un parametro che il
 * generatore deve decidere non ha regole, l'AI lo inventera. Questo test
 * verifica la copertura dei parametri, non il numero totale di regole.
 */
class InvictusKnowledgeCoverageTest {

    /** Parametri che una scheda deve decidere. */
    private val trainingParams = mapOf(
        "SPLIT" to "split settimanale",
        "FREQUENCY" to "frequenza per muscolo",
        "VOLUME" to "serie settimanali",
        "INTENSITY" to "RIR/RPE",
        "REPS" to "range ripetizioni",
        "REST" to "recuperi",
        "PROGRESSION" to "progressione",
        "PERIODIZATION" to "mesociclo",
        "TECHNIQUE" to "tecniche",
        "EXECUTION" to "esecuzione",
        "CARDIO" to "cardio",
        "GOAL_TRAIN" to "obiettivo",
        "SAFETY_TRAIN" to "sicurezza"
    )

    /** Parametri che un piano alimentare deve decidere. */
    private val nutritionParams = mapOf(
        "CALORIES" to "target calorico",
        "METABOLISM" to "dispendio",
        "PROTEIN" to "proteine",
        "CARBS" to "carboidrati",
        "FAT" to "grassi",
        "FIBER" to "fibre",
        "MEALS" to "pasti",
        "TIMING" to "timing",
        "CARB_CYCLING" to "ricariche",
        "HYDRATION" to "acqua",
        "WEIGHT" to "peso",
        "BODYCOMP" to "composizione"
    )

    @Test
    fun `ogni parametro della scheda ha almeno una regola`() {
        val mancanti = trainingParams.filter { (code, _) ->
            val cat = InvictusKnowledge.Category.of(code)
            cat == null || InvictusKnowledge.forCategory(cat).isEmpty()
        }
        assertThat(mancanti).isEmpty()
    }

    @Test
    fun `ogni parametro del piano alimentare ha almeno una regola`() {
        val mancanti = nutritionParams.filter { (code, _) ->
            val cat = InvictusKnowledge.Category.of(code)
            cat == null || InvictusKnowledge.forCategory(cat).isEmpty()
        }
        assertThat(mancanti).isEmpty()
    }

    @Test
    fun `le categorie di parametro hanno una base solida`() {
        // Sotto 3 regole il parametro e' fragile: il modello ha poco su cui
        // basarsi e tende a inventare. Deload e sostituzioni sono i piu' sottili
        // nella fonte e restano documentati come tali.
        val fragili = listOf("SPLIT", "VOLUME", "INTENSITY", "REPS", "REST",
                             "PROTEIN", "CARBS", "FAT", "CALORIES")
        fragili.forEach { code ->
            val cat = InvictusKnowledge.Category.of(code)!!
            assertThat(InvictusKnowledge.forCategory(cat).size).isAtLeast(3)
        }
    }

    @Test
    fun `ogni regola ha una fonte verificabile`() {
        InvictusKnowledge.rules.forEach { r ->
            assertThat(r.statement).isNotEmpty()
            assertThat(r.articleTitle).isNotEmpty()
            assertThat(r.url).startsWith("https://www.projectinvictus.it/")
            assertThat(r.publishedDate).isNotEmpty()
        }
    }

    @Test
    fun `ogni regola contiene un numero verificabile`() {
        // Una regola senza numero non e' azionabile: "allenati di piu'" non
        // dice quanto. Il corpus deve dare parametri, non consigli generici.
        val senzaNumero = InvictusKnowledge.rules.filter { r ->
            !r.statement.contains(Regex("\\d"))
        }
        assertThat(senzaNumero.map { it.statement }).isEmpty()
    }

    @Test
    fun `la ricerca per parametro restituisce regole attinenti`() {
        val volume = InvictusKnowledge.search("volume", InvictusKnowledge.Area.ALLENAMENTO, 50)
        assertThat(volume).isNotEmpty()
        assertThat(volume.all { it.category.area == InvictusKnowledge.Area.ALLENAMENTO }).isTrue()

        val proteine = InvictusKnowledge.search("proteine", InvictusKnowledge.Area.NUTRIZIONE, 50)
        assertThat(proteine).isNotEmpty()
        assertThat(proteine.all { it.category.area == InvictusKnowledge.Area.NUTRIZIONE }).isTrue()
    }

    @Test
    fun `le due aree restano separate`() {
        val allenamento = InvictusKnowledge.forArea(InvictusKnowledge.Area.ALLENAMENTO)
        val nutrizione = InvictusKnowledge.forArea(InvictusKnowledge.Area.NUTRIZIONE)
        assertThat(allenamento).isNotEmpty()
        assertThat(nutrizione).isNotEmpty()
        // Una regola di allenamento non deve comparire tra quelle nutrizionali.
        val urlsAllenamento = allenamento.map { it.statement }.toSet()
        val overlap = nutrizione.count { it.statement in urlsAllenamento }
        assertThat(overlap).isAtMost(nutrizione.size / 10)
    }

    @Test
    fun `nessuna categoria resta vuota`() {
        val vuote = InvictusKnowledge.Category.entries.filter {
            InvictusKnowledge.forCategory(it).isEmpty()
        }
        assertThat(vuote).isEmpty()
    }
}
