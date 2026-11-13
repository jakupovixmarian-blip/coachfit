package dev.fitcoach.app.domain.catalog

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodProvenance
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.validation.FoodValidation
import dev.fitcoach.app.domain.validation.FoodValidator
import dev.fitcoach.app.domain.validation.Severity

/**
 * The storage the catalog needs. Narrow on purpose: the import logic does not
 * depend on Room, so it can be tested and reused without Android.
 */
interface FoodStore {
    /** Finds the record already imported from this source identifier, if any. */
    suspend fun bySourceRef(source: String, sourceId: String): Food?

    suspend fun insert(food: Food): Long

    suspend fun update(food: Food)

    /** Local search, used when the external sources cannot answer. */
    suspend fun search(query: String, limit: Int): List<Food>
}

/** A source that could not be used, with the reason the user can act on. */
data class SourceAvailability(
    val source: FoodSource,
    val displayName: String,
    val reason: String?
)

/**
 * Result of a catalog search. Local results are reported separately from remote
 * hits: the app can then say that it is showing stored data instead of pretending
 * the network answered.
 */
data class CatalogSearch(
    val hits: List<RemoteFoodHit>,
    val unavailable: List<SourceAvailability>,
    val localFallback: List<Food>
) {
    val usedLocalFallback: Boolean get() = hits.isEmpty() && localFallback.isNotEmpty()

    val isEmpty: Boolean get() = hits.isEmpty() && localFallback.isEmpty()

    /** True when at least one source answered, even with zero results. */
    val anySourceAnswered: Boolean get() = unavailable.size < unavailable.size + 1 && hits.isNotEmpty()
}

/**
 * Result of importing one record.
 *
 * [food] is null only when the record was rejected: an impossible record is not
 * stored with a warning, because a warning is invisible in a diary total.
 */
data class ImportResult(
    val food: Food?,
    val created: Boolean,
    val validation: FoodValidation,
    val rejection: String? = null
) {
    val stored: Boolean get() = food != null
}

/**
 * Coordinates the external connectors with the local database.
 *
 * Rules kept here, in one place, so every source behaves the same:
 * - a record is stored only if it passes validation;
 * - the same source identifier updates the existing row instead of adding one;
 * - provenance is written at import time and never guessed afterwards;
 * - when no source answers, previously stored data is returned and labelled.
 */
class FoodCatalog(
    private val sources: List<RemoteFoodSource>,
    private val store: FoodStore,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun search(query: String, limit: Int = 15): CatalogSearch {
        val unavailable = mutableListOf<SourceAvailability>()
        val hits = mutableListOf<RemoteFoodHit>()

        for (source in sources) {
            if (!source.isAvailable()) {
                unavailable += SourceAvailability(source.source, source.displayName, source.unavailableReason())
                continue
            }
            val found = runCatching { source.search(query, limit) }.getOrElse { error ->
                unavailable += SourceAvailability(
                    source.source,
                    source.displayName,
                    "Ricerca non riuscita: ${error.message ?: error::class.simpleName}"
                )
                emptyList()
            }
            hits += found
        }

        val local = if (hits.isEmpty() && query.isNotBlank()) {
            runCatching { store.search(query, limit) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        return CatalogSearch(hits = hits, unavailable = unavailable, localFallback = local)
    }

    /**
     * Stores a record from a source, or updates the one already imported from the
     * same source identifier. Returns the stored food with its provenance.
     */
    suspend fun import(hit: RemoteFoodHit): ImportResult {
        val validation = FoodValidator.validate(hit.food)
        if (!validation.isUsable) {
            val errors = validation.issues
                .filter { it.severity == Severity.ERROR }
                .joinToString(" ") { it.message }
            return ImportResult(food = null, created = false, validation = validation, rejection = errors)
        }

        val incoming = hit.food.copy(provenance = provenanceFor(hit, validation))
        val existing = store.bySourceRef(hit.food.source.name, hit.sourceId)

        return if (existing == null) {
            val id = store.insert(incoming)
            ImportResult(food = incoming.copy(id = id), created = true, validation = validation)
        } else {
            val merged = incoming.copy(id = existing.id)
            store.update(merged)
            ImportResult(food = merged, created = false, validation = validation)
        }
    }

    /**
     * Re-reads one record from its own source, for a manual refresh. Returns null
     * when the source is unavailable or no longer publishes that identifier.
     */
    suspend fun refresh(source: FoodSource, sourceId: String): ImportResult? {
        val connector = sources.firstOrNull { it.source == source } ?: return null
        if (!connector.isAvailable()) return null
        val hit = connector.fetchById(sourceId) ?: return null
        return import(hit)
    }

    /** Chooses one record among candidates; values are never averaged. */
    fun choose(candidates: List<Food>) = FoodValidator.resolve(candidates)

    private fun provenanceFor(hit: RemoteFoodHit, validation: FoodValidation): FoodProvenance {
        val missing = buildList {
            if (hit.food.fiberPer100g == null) add("fibre")
            if (hit.food.barcode.isNullOrBlank()) add("codice a barre")
            if (hit.food.brand.isBlank()) add("marca")
        }
        val limitations = listOfNotNull(
            hit.sourceNote.takeIf { it.isNotBlank() },
            missing.takeIf { it.isNotEmpty() }
                ?.let { "Non pubblicato dalla fonte: " + it.joinToString(", ") }
        ).joinToString(". ")

        return FoodProvenance(
            sourceId = hit.sourceId,
            acquiredAt = now(),
            sourceUpdatedAt = hit.sourceUpdatedAt,
            reliability = validation.reliability,
            verification = validation.status,
            limitations = limitations
        )
    }
}
