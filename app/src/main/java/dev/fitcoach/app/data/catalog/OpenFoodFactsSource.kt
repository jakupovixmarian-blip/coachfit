package dev.fitcoach.app.data.catalog

import dev.fitcoach.app.data.ai.OpenFoodFactsClient
import dev.fitcoach.app.domain.catalog.RemoteFoodHit
import dev.fitcoach.app.domain.catalog.RemoteFoodSource
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import javax.inject.Inject

/**
 * Open Food Facts through the common connector contract.
 *
 * The public API needs no key, so the source is always available. What it returns
 * is a label value declared by the producer, not a laboratory measurement, and the
 * note attached to every record says so.
 */
class OpenFoodFactsSource @Inject constructor(
    private val client: OpenFoodFactsClient
) : RemoteFoodSource {

    override val source: FoodSource = FoodSource.OPEN_FOOD_FACTS

    override val displayName: String = "Open Food Facts"

    override suspend fun isAvailable(): Boolean = true

    override suspend fun unavailableReason(): String? = null

    override suspend fun search(query: String, limit: Int): List<RemoteFoodHit> =
        client.searchByName(query, limit).map { it.toHit() }

    override suspend fun fetchById(sourceId: String): RemoteFoodHit? =
        client.fetchByBarcode(sourceId)?.toHit()

    private fun Food.toHit(): RemoteFoodHit {
        val stableId = barcode?.takeIf { it.isNotBlank() } ?: "nome:" + slug(name)
        return RemoteFoodHit(
            food = this,
            sourceId = stableId,
            sourceName = displayName,
            sourceUpdatedAt = null,
            sourceNote = "Valori dichiarati in etichetta dal produttore, non misurati da un laboratorio."
        )
    }

    private fun slug(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
