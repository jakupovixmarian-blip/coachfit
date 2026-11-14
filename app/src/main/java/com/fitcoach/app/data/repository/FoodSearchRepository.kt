package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.FoodDao
import com.fitcoach.app.data.remote.FoodApi
import com.fitcoach.app.data.remote.OffProduct
import com.fitcoach.app.data.remote.UsdaFood
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ricerca alimentare progressiva (spec. §6).
 *
 * Ordine di risoluzione:
 *  1. Database locale Room (ricerca per nome normalizzato).
 *  2. Se assente, Open Food Facts (prodotti commerciali / marche).
 *  3. Se ancora assente, USDA FoodData Central (alimenti generici).
 *  4. Il risultato trovato viene salvato in Room e riutilizzato in futuro:
 *     una ricerca gia effettuata NON viene ripetuta inutilmente.
 *
 * La deduplicazione avviene su [FoodItem.normalizedName] e su `sourceId`.
 * Nessun valore nutrizionale viene inventato: se una fonte non fornisce i
 * macronutrienti, il risultato viene scartato.
 */
@Singleton
class FoodSearchRepository @Inject constructor(
    private val dao: FoodDao,
    private val api: FoodApi
) {
    /** Esito di una ricerca con indicazione della provenienza. */
    data class SearchOutcome(
        val results: List<FoodItem>,
        val fromLocal: Boolean,
        val queriedRemote: Boolean,
        val note: String? = null
    )

    /**
     * Ricerca un alimento. Se [allowRemote] e true e la ricerca locale non ha
     * risultati, interroga le fonti esterne e salva cio che trova.
     */
    suspend fun search(query: String, allowRemote: Boolean = true): SearchOutcome =
        withContext(Dispatchers.IO) {
            val normalized = FoodItem.normalizeName(query)
            if (normalized.isBlank()) return@withContext SearchOutcome(emptyList(), true, false)

            val local = dao.searchNormalized(normalized).map { it.toDomain() }
            if (local.isNotEmpty()) {
                return@withContext SearchOutcome(local, fromLocal = true, queriedRemote = false)
            }
            if (!allowRemote) {
                return@withContext SearchOutcome(emptyList(), fromLocal = true, queriedRemote = false)
            }

            val remote = fetchRemote(query)
            val saved = remote.mapNotNull { persistIfNew(it) }
            val note = when {
                saved.isNotEmpty() -> null
                else -> "Nessun risultato trovato per \"$query\" nelle fonti esterne."
            }
            SearchOutcome(saved, fromLocal = false, queriedRemote = true, note = note)
        }

    /** Recupera un prodotto dal codice a barre (Open Food Facts). */
    suspend fun searchByBarcode(barcode: String): SearchOutcome = withContext(Dispatchers.IO) {
        val existing = dao.getBySourceId(barcode)
        if (existing != null) {
            return@withContext SearchOutcome(listOf(existing.toDomain()), true, false)
        }
        val product = runCatching {
            val resp = api.offByBarcode(FoodApi.OFF_BARCODE_URL + barcode + ".json")
            if (resp.isSuccessful) resp.body()?.product else null
        }.getOrNull() ?: return@withContext SearchOutcome(
            emptyList(), false, true, "Prodotto non trovato per il codice $barcode."
        )
        val item = product.toFoodItem() ?: return@withContext SearchOutcome(
            emptyList(), false, true, "Il prodotto non riporta i valori nutrizionali."
        )
        val saved = persistIfNew(item)
        SearchOutcome(listOfNotNull(saved), false, true)
    }

    private suspend fun fetchRemote(query: String): List<FoodItem> {
        val off = fetchOff(query)
        if (off.isNotEmpty()) return off
        return fetchUsda(query)
    }

    private suspend fun fetchOff(query: String): List<FoodItem> = runCatching {
        val resp = api.offSearch(FoodApi.OFF_SEARCH_URL, terms = query)
        if (!resp.isSuccessful) return emptyList()
        resp.body()?.products.orEmpty()
            .mapNotNull { it.toFoodItem() }
            .distinctBy { it.normalizedName }
    }.getOrDefault(emptyList())

    private suspend fun fetchUsda(query: String): List<FoodItem> = runCatching {
        val resp = api.usdaSearch(FoodApi.USDA_SEARCH_URL, query = query)
        if (!resp.isSuccessful) return emptyList()
        resp.body()?.foods.orEmpty().mapNotNull { it.toFoodItem() }
    }.getOrDefault(emptyList())

    /**
     * Salva un alimento se non e un duplicato. Dedup per sourceId, poi per
     * nome normalizzato. Ritorna l'alimento salvato (o quello esistente).
     */
    private suspend fun persistIfNew(item: FoodItem): FoodItem? {
        if (item.kcalPer100g <= 0.0 && item.proteinPer100g <= 0.0 &&
            item.carbsPer100g <= 0.0 && item.fatPer100g <= 0.0
        ) {
            return null // dati nutrizionali assenti: non salviamo voci vuote
        }
        item.sourceId?.let { sid ->
            dao.getBySourceId(sid)?.let { return it.toDomain() }
        }
        dao.getByNormalizedName(item.normalizedName)?.let { return it.toDomain() }
        val id = dao.insert(item.toEntity())
        return item.copy(id = id)
    }

    /** Salva un alimento creato manualmente dall'utente. */
    suspend fun addCustom(item: FoodItem): Long = dao.insert(item.copy(isCustom = true).toEntity())
}

private const val SRC_OFF = "Open Food Facts"
private const val SRC_USDA = "USDA FoodData Central"

internal fun OffProduct.toFoodItem(): FoodItem? {
    val name = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: return null
    val n = nutriments ?: return null
    val kcal = n.kcal100g ?: return null
    return FoodItem(
        name = name,
        category = "Prodotto confezionato",
        kcalPer100g = kcal,
        proteinPer100g = n.proteins100g ?: 0.0,
        carbsPer100g = n.carbohydrates100g ?: 0.0,
        fatPer100g = n.fat100g ?: 0.0,
        fiberPer100g = n.fiber100g ?: 0.0,
        defaultUnit = FoodUnit.AS_SOLD,
        source = SRC_OFF,
        sourceId = code,
        brand = brands?.substringBefore(',')?.trim(),
        tags = listOf("off")
    )
}

internal fun UsdaFood.toFoodItem(): FoodItem? {
    val name = description?.trim().takeUnless { it.isNullOrBlank() } ?: return null
    val kcal = nutrient("208", "Energy") ?: return null
    return FoodItem(
        name = name.lowercase().replaceFirstChar { it.uppercase() },
        category = "Alimento generico",
        kcalPer100g = kcal,
        proteinPer100g = nutrient("203", "Protein") ?: 0.0,
        carbsPer100g = nutrient("205", "Carbohydrate, by difference") ?: 0.0,
        fatPer100g = nutrient("204", "Total lipid (fat)") ?: 0.0,
        fiberPer100g = nutrient("291", "Fiber, total dietary") ?: 0.0,
        defaultUnit = FoodUnit.RAW,
        source = SRC_USDA,
        sourceId = fdcId?.toString(),
        brand = brandName ?: brandOwner,
        tags = listOf("usda")
    )
}
