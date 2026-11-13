package dev.fitcoach.app.domain.catalog

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource

/**
 * A food as returned by an external source, before it is stored.
 *
 * The source identifier travels with the record so an import can be repeated
 * without duplicating the food, and so the app can always say where a value came
 * from. Nothing here is inferred: if the source does not publish a field, the
 * field stays absent instead of being filled with a plausible guess.
 */
data class RemoteFoodHit(
    val food: Food,
    val sourceId: String,
    val sourceName: String,
    /** Timestamp published by the source for this record, when it exposes one. */
    val sourceUpdatedAt: Long? = null,
    /** Human readable note about what the source actually measured. */
    val sourceNote: String = ""
)

/**
 * A connector to one external food database.
 *
 * Implementations only fetch and translate. Persistence, deduplication and
 * verification belong to the catalog repository, so every connector behaves the
 * same way from the point of view of the rest of the app.
 */
interface RemoteFoodSource {

    val source: FoodSource

    /** Human readable name of the database, used in the UI and in provenance. */
    val displayName: String

    /** False when the source needs an API key that the user has not configured yet. */
    suspend fun isAvailable(): Boolean

    /** Why the source is unavailable, so the UI can tell the user what to configure. */
    suspend fun unavailableReason(): String?

    suspend fun search(query: String, limit: Int = 15): List<RemoteFoodHit>

    /** Lookup by the identifier used by the source itself. */
    suspend fun fetchById(sourceId: String): RemoteFoodHit?
}
