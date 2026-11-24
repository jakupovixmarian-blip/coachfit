package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.knowledge.CorpusPrompt
import com.fitcoach.app.domain.knowledge.InvictusKnowledge
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Il corpus deve arrivare all'AI, non restare una libreria consultabile a
 * richiesta. Questi test misurano cosa il modello riceve davvero.
 */
class CorpusPromptCollaudoTest {

    @Test
    fun `ogni categoria dell'area entra nel prompt con almeno una regola`() {
        val aree = InvictusKnowledge.Area.entries
        val selezione = CorpusPrompt.select(aree, perCategory = 2)
        val categorie = selezione.map { it.category }.toSet()
        val attese = InvictusKnowledge.Category.entries.filter { it.area in aree }.toSet()
        assertThat(categorie).isEqualTo(attese)
    }

    @Test
    fun `le regole scelte contengono numeri e non sono rumore`() {
        CorpusPrompt.select(InvictusKnowledge.Area.entries, perCategory = 3).forEach { s ->
            s.rules.forEach { r ->
                assertThat(r.statement.any { it.isDigit() }).isTrue()
                assertThat(r.url).startsWith("https://")
                assertThat(r.articleTitle).isNotEmpty()
            }
        }
    }

    @Test
    fun `nessuna regola viene ripetuta dentro la stessa categoria`() {
        // La ripetizione TRA categorie e' voluta: il corpus assegna la stessa
        // frase a piu' parametri che informa.
        CorpusPrompt.select(InvictusKnowledge.Area.entries, perCategory = 3).forEach { s ->
            val frasi = s.rules.map { it.statement.trim().lowercase() }
            assertThat(frasi).containsNoDuplicates()
        }
    }

    @Test
    fun `il blocco resta dentro il budget che non rompe la generazione`() {
        // Il README documenta che ~45.000 caratteri di corpus nel prompt producono
        // programmi non validi su un modello piccolo. Il blocco deve restare ben
        // sotto quella soglia, lasciando spazio ad anamnesi e cataloghi.
        val blocco = CorpusPrompt.block(InvictusKnowledge.Area.entries)
        assertThat(blocco.length).isLessThan(CorpusPrompt.MAX_BLOCK_CHARS)
        assertThat(blocco.length).isLessThan(15_000)
    }

    @Test
    fun `le fonti sono citate nel blocco`() {
        val blocco = CorpusPrompt.block(listOf(InvictusKnowledge.Area.NUTRIZIONE), perCategory = 2)
        assertThat(blocco).contains("Fonte:")
        assertThat(blocco).contains("projectinvictus.it")
        assertThat(blocco).contains("non cancelli")
    }
}
