package dev.fitcoach.app

import dev.fitcoach.app.domain.catalog.CreaCsvImporter
import dev.fitcoach.app.domain.catalog.FoodCatalog
import dev.fitcoach.app.domain.catalog.FoodStore
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.VerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The CREA tables arrive as a file, so the importer must be strict: a malformed
 * line is reported with its number and never becomes a food with invented values.
 */
class CreaImportTest {

    private class MemoryStore : FoodStore {
        val items = mutableListOf<Food>()
        private var nextId = 1L

        override suspend fun bySourceRef(source: String, sourceId: String): Food? =
            items.firstOrNull { it.source.name == source && it.provenance.sourceId == sourceId }

        override suspend fun insert(food: Food): Long {
            val id = nextId++
            items += food.copy(id = id)
            return id
        }

        override suspend fun update(food: Food) {
            val index = items.indexOfFirst { it.id == food.id }
            if (index >= 0) items[index] = food
        }

        override suspend fun search(query: String, limit: Int): List<Food> =
            items.filter { it.name.contains(query, ignoreCase = true) }.take(limit)
    }

    private val csv = """
        nome;categoria;kcal;proteine;carboidrati;grassi;fibre
        Pasta di semola cruda;Cereali e derivati;353;10,9;71,2;1,4;2,7
        Olio di oliva extravergine;Grassi da condimento;899;0;0;99,9;
        Mela;Frutta;45;0,3;10,6;0,2;2,6
        Riga rotta;Varie;n.d.;1;2;3;4
        Solo tre colonne;Errore;100
    """.trimIndent()

    @Test
    fun `importa le righe valide con fonte CREA e provenienza`() = runBlocking {
        val store = MemoryStore()
        val catalog = FoodCatalog(sources = emptyList(), store = store, now = { 1L })
        val report = CreaCsvImporter(catalog).import(csv)

        assertEquals(5, report.total)
        assertEquals(3, report.created)
        assertEquals(0, report.updated)
        assertEquals(2, report.rejected.size)
        assertEquals(3, store.items.size)

        val pasta = store.items.first { it.name.startsWith("Pasta") }
        assertEquals(353.0, pasta.kcalPer100g, 0.001)
        assertEquals(10.9, pasta.proteinPer100g, 0.001)
        assertEquals(2.7, pasta.fiberPer100g!!, 0.001)
        assertEquals(FoodSource.CREA, pasta.source)
        assertEquals(VerificationStatus.VERIFIED, pasta.provenance.verification)
        assertEquals(0.95, pasta.provenance.reliability, 0.0001)
        assertTrue(pasta.provenance.sourceId.startsWith("crea:"))
        assertTrue(pasta.provenance.limitations.contains("CREA"))
    }

    @Test
    fun `la riga con calorie non numeriche viene respinta con il suo numero`() = runBlocking {
        val store = MemoryStore()
        val catalog = FoodCatalog(sources = emptyList(), store = store, now = { 1L })
        val report = CreaCsvImporter(catalog).import(csv)

        val badKcal = report.rejected.first { it.lineNumber == 5 }
        assertTrue(badKcal.reason.contains("Calorie"))
        assertTrue(store.items.none { it.name.startsWith("Riga rotta") })
    }

    @Test
    fun `la riga con colonne insufficienti viene respinta`() = runBlocking {
        val store = MemoryStore()
        val catalog = FoodCatalog(sources = emptyList(), store = store, now = { 1L })
        val report = CreaCsvImporter(catalog).import(csv)

        val shortRow = report.rejected.first { it.lineNumber == 6 }
        assertTrue(shortRow.reason.contains("Colonne insufficienti"))
    }

    @Test
    fun `reimportare lo stesso file aggiorna senza duplicare`() = runBlocking {
        val store = MemoryStore()
        val catalog = FoodCatalog(sources = emptyList(), store = store, now = { 1L })
        val importer = CreaCsvImporter(catalog)

        importer.import(csv)
        val second = importer.import(csv)

        assertEquals(3, store.items.size)
        assertEquals(0, second.created)
        assertEquals(3, second.updated)
    }

    @Test
    fun `il valore mancante resta assente invece di diventare zero`() = runBlocking {
        val store = MemoryStore()
        val catalog = FoodCatalog(sources = emptyList(), store = store, now = { 1L })
        CreaCsvImporter(catalog).import(csv)

        val oil = store.items.first { it.name.startsWith("Olio") }
        assertEquals(null, oil.fiberPer100g)
        assertNotNull(oil.provenance.limitations)
    }
}
