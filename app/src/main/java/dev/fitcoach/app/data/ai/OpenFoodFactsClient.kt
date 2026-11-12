package dev.fitcoach.app.data.ai

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live Open Food Facts client (public API, no key required). Values returned are
 * the real values published by the database for the scanned/queried product.
 */
@Singleton
class OpenFoodFactsClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) {
    suspend fun searchByName(query: String, limit: Int = 15): List<Food> = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/cgi/search.pl" +
            "?search_terms=${java.net.URLEncoder.encode(query, "UTF-8")}" +
            "&search_simple=1&action=process&json=1&page_size=$limit" +
            "&fields=code,product_name,brands,nutriments,categories"
        val request = Request.Builder().url(url).header("User-Agent", "FitCoach/2.0 (Android)").get().build()
        runCatching {
            client.newCall(request).execute().use { r ->
                if (!r.isSuccessful) return@use emptyList()
                val body = r.body?.string().orEmpty()
                val root = json.parseToJsonElement(body).jsonObject
                root["products"]?.jsonArray?.mapNotNull { element ->
                    val obj = element.jsonObject
                    val name = obj["product_name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val n = obj["nutriments"]?.jsonObject ?: return@mapNotNull null
                    fun value(key: String): Double? = n[key]?.jsonPrimitive?.content?.toDoubleOrNull()
                    val kcal = value("energy-kcal_100g") ?: return@mapNotNull null
                    Food(
                        name = name,
                        brand = obj["brands"]?.jsonPrimitive?.content.orEmpty(),
                        category = obj["categories"]?.jsonPrimitive?.content?.substringBefore(",").orEmpty(),
                        kcalPer100g = kcal,
                        proteinPer100g = value("proteins_100g") ?: 0.0,
                        carbsPer100g = value("carbohydrates_100g") ?: 0.0,
                        fatPer100g = value("fat_100g") ?: 0.0,
                        fiberPer100g = value("fiber_100g"),
                        source = FoodSource.OPEN_FOOD_FACTS,
                        barcode = obj["code"]?.jsonPrimitive?.content,
                        state = FoodState.COMMERCIAL,
                        isCustom = false
                    )
                }.orEmpty()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchByBarcode(barcode: String): Food? = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
        val request = Request.Builder().url(url).header("User-Agent", "FitCoach/2.0 (Android)").get().build()
        runCatching {
            client.newCall(request).execute().use { r ->
                if (!r.isSuccessful) return@use null
                val body = r.body?.string().orEmpty()
                val root = json.parseToJsonElement(body).jsonObject
                if (root["status"]?.jsonPrimitive?.content?.toIntOrNull() != 1) return@use null
                val product = root["product"]?.jsonObject ?: return@use null
                val n = product["nutriments"]?.jsonObject ?: return@use null
                fun value(key: String): Double? = n[key]?.jsonPrimitive?.content?.toDoubleOrNull()
                val kcal = value("energy-kcal_100g") ?: return@use null
                Food(
                    name = product["product_name"]?.jsonPrimitive?.content.orEmpty().ifBlank { "Prodotto $barcode" },
                    brand = product["brands"]?.jsonPrimitive?.content.orEmpty(),
                    kcalPer100g = kcal,
                    proteinPer100g = value("proteins_100g") ?: 0.0,
                    carbsPer100g = value("carbohydrates_100g") ?: 0.0,
                    fatPer100g = value("fat_100g") ?: 0.0,
                    fiberPer100g = value("fiber_100g"),
                    source = FoodSource.OPEN_FOOD_FACTS,
                    barcode = barcode,
                    state = FoodState.COMMERCIAL,
                    isCustom = false
                )
            }
        }.getOrNull()
    }
}
