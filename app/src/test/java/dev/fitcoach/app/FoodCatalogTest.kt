package dev.fitcoach.app

import dev.fitcoach.app.domain.catalog.CatalogSearch
import dev.fitcoach.app.domain.catalog.FoodCatalog
import dev.fitcoach.app.domain.catalog.FoodStore
import dev.fitcoach.app.domain.catalog.RemoteFoodHit
import dev.fitcoach.app.domain.catalog.RemoteFoodSource
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.VerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the import rules that keep the food database honest: no duplicates, no
 * impossible records, provenance always written, and stored data labelled as such
 * when the network cannot answer.
 */
class FoodCatalogTest {

    private class FakeStore(initial: List<Food> = emptyList()) : FoodStore {
        val items = initial.toMutableList()
        private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1L

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

    private class FakeSource(
        override val source: FoodSource,
        override val displayName: String,
        private val available: Boolean,
        private val hits: List<RemoteFoodHit> = emptyList(),
        private val failure: String? = null
    ) : RemoteFoodSource {

        override suspend fun isAvailable(): Boolean = available

        override suspend fun unavailableReason(): String? = if (available) null else failure

        override suspend fun search(query: String, limit: Int): List<RemoteFoodHit> = hits

        override suspend fun fetchById(sourceId: String): RemoteFoodHit? =
            hits.firstOrNull { it.sourceId == sourceId }
    }

    private fun usdaHit(id: String = "171688", kcal: Double = 165.0) = RemoteFoodHit(
        food = Food(
            name = "Yogurt greco",
            brand = "",
            category = "Latticini",
            kcalPer100g = kcal,
            proteinPer100g = 10.0,
            carbsPer100g = 3.6,
            fatPer100g = 0.4,
            fiberPer100g = null,
            source = FoodSource.USDA,
            state = dev.fitcoach.app.domain.model.FoodState.RAW
        ),
        sourceId = id,
        sourceName = "USDA FoodData Central",
        sourceUpdatedAt = 1_700_000_000_000L,
        sourceNote = "Valori per 100 g"
    )

    private fun catalog(store: FakeStore, vararg sources: RemoteFoodSource) =
        FoodCatalog(sources = sources.toList(), store = store, now = { 1_800_000_000_000L })

    @Test
    fun `import scrive la provenienza e conserva i valori`() = runBlocking {
        val store = FakeStore()
        val catalog = catalog(store, FakeSource(FoodSource.USDA, "USDA", available = true))

        val result = catalog.import(usdaHit())

        assertTrue(result.stored)
        assertTrue(result.created)
        val stored = store.items.single()
        assertEquals(165.0, stored.kcalPer100g, 0.001)
        assertEquals("171688", stored.provenance.sourceId)
        assertEquals(VerificationStatus.VERIFIED, stored.provenance.verification)
        assertEquals(0.90, stored.provenance.reliability, 0.0001)
        assertEquals(1_800_000_000_000L, stored.provenance.acquiredAt)
        assertEquals(1_700_000_000_000L, stored.provenance.sourceUpdatedAt)
        assertTrue(stored.provenance.limitations.contains("fibre"))
    }

    @Test
    fun `reimportare lo stesso identificativo aggiorna invece di duplicare`() = runBlocking {
        val store = FakeStore()
        val catalog = catalog(store, FakeSource(FoodSource.USDA, "USDA", available = true))

        val first = catalog.import(usdaHit(kcal = 165.0))
        val second = catalog.import(usdaHit(kcal = 170.0))

        assertEquals(1, store.items.size)
        assertFalse(second.created)
        assertEquals(first.food?.id, second.food?.id)
        assertEquals(170.0, store.items.single().kcalPer100g, 0.001)
    }

    @Test
    fun `un record impossibile viene rifiutato e non salvato`() = runBlocking {
        val store = FakeStore()
        val catalog = catalog(store, FakeSource(FoodSource.USDA, "USDA", available = true))

        val broken = usdaHit().let {
            it.copy(
                food = it.food.copy(
                    proteinPer100g = 60.0,
                    carbsPer100g = 60.0,
                    fatPer100g = 60.0
                )
            )
        }
        val result = catalog.import(broken)

        assertFalse(result.stored)
        assertNull(result.food)
        assertTrue(store.items.isEmpty())
        assertNotNull(result.rejection)
        assertTrue(result.rejection!!.contains("100"))
    }

    @Test
    fun `fonte senza chiave viene dichiarata e si usa il dato locale`() = runBlocking {
        val local = Food(
            id = 7L,
            name = "Yogurt greco",
            kcalPer100g = 160.0,
            proteinPer100g = 9.5,
            carbsPer100g = 4.0,
            fatPer100g = 0.5,
            source = FoodSource.MANUAL
        )
        val store = FakeStore(listOf(local))
        val catalog = catalog(
            store,
            FakeSource(
                FoodSource.USDA,
                "USDA FoodData Central",
                available = false,
                failure = "Serve una API key USDA (gratuita) per interrogare FoodData Central."
            )
        )

        val search: CatalogSearch = catalog.search("yogurt")

        assertTrue(search.hits.isEmpty())
        assertTrue(search.usedLocalFallback)
        assertEquals(1, search.localFallback.size)
        assertEquals(7L, search.localFallback.single().id)
        assertEquals(1, search.unavailable.size)
        assertTrue(search.unavailable.single().reason!!.contains("API key"))
    }

    @Test
    fun `tra due fonti vince la piu affidabile e i valori non vengono mediati`() = runBlocking {
        val store = FakeStore()
        val catalog = catalog(store)

        val usda = usdaHit(kcal = 165.0).food.copy(id = 1L)
        val off = Food(
            id = 2L,
            name = "Yogurt greco 0%",
            kcalPer100g = 57.0,
            proteinPer100g = 10.0,
            carbsPer100g = 3.6,
            fatPer100g = 0.2,
            source = FoodSource.OPEN_FOOD_FACTS,
            brand = "Marca"
        )

        val conflict = catalog.choose(listOf(off, usda))

        assertNotNull(conflict)
        assertEquals(FoodSource.USDA, conflict!!.chosen.source)
        assertEquals(165.0, conflict.chosen.kcalPer100g, 0.001)
        assertEquals(1, conflict.rejected.size)
        assertEquals(FoodSource.OPEN_FOOD_FACTS, conflict.rejected.single().source)
    }
}
