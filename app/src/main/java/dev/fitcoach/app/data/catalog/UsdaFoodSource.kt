package dev.fitcoach.app.data.catalog

import dev.fitcoach.app.domain.catalog.RemoteFoodHit
import dev.fitcoach.app.domain.catalog.RemoteFoodSource
import dev.fitcoach.app.domain.model.FoodSource

/**
 * Supplies the USDA API key. Kept as an interface so the key can come from the
 * encrypted settings store instead of being compiled into the app.
 */
fun interface UsdaApiKeyProvider {
    suspend fun apiKey(): String?
}

/**
 * USDA FoodData Central exposed through the common connector contract.
 *
 * Without a key the connector reports itself as unavailable and returns nothing:
 * it never falls back to invented data or to a different source silently.
 */
class UsdaFoodSource(
    private val client: UsdaFoodClient,
    private val keyProvider: UsdaApiKeyProvider
) : RemoteFoodSource {

    override val source: FoodSource = FoodSource.USDA

    override val displayName: String = "USDA FoodData Central"

    override suspend fun isAvailable(): Boolean = keyProvider.apiKey()?.isNotBlank() == true

    override suspend fun unavailableReason(): String? =
        if (isAvailable()) null
        else "Serve una API key USDA (gratuita) per interrogare FoodData Central."

    override suspend fun search(query: String, limit: Int): List<RemoteFoodHit> {
        if (query.isBlank()) return emptyList()
        val key = keyProvider.apiKey()?.takeIf { it.isNotBlank() } ?: return emptyList()
        return client.search(query, key, limit)
    }

    override suspend fun fetchById(sourceId: String): RemoteFoodHit? {
        val key = keyProvider.apiKey()?.takeIf { it.isNotBlank() } ?: return null
        return client.fetchById(sourceId, key)
    }
}
