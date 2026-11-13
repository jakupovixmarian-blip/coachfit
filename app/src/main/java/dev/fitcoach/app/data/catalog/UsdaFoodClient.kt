package dev.fitcoach.app.data.catalog

import dev.fitcoach.app.domain.catalog.RemoteFoodHit
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USDA FoodData Central client.
 *
 * The API is queried by nutrient identifier, never by nutrient name and never by
 * position in the array: the same nutrient appears with different names across
 * data types, and an array index is not a contract.
 *
 * A record without energy, protein, carbohydrate or fat values is dropped instead
 * of being stored with zeros, because a zero is a claim the source never made.
 */
@Singleton
class UsdaFoodClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) {

    suspend fun search(query: String, apiKey: String, limit: Int = 15): List<RemoteFoodHit> =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject {
                put("query", query)
                put("pageSize", limit.coerceIn(1, 50))
                put(
                    "dataType",
                    buildJsonArray {
                        add(JsonPrimitive("Foundation"))
                        add(JsonPrimitive("SR Legacy"))
                        add(JsonPrimitive("Branded"))
                    }
                )
            }
            val request = Request.Builder()
                .url("$BASE_URL/foods/search?apiKey=$apiKey")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .header("User-Agent", USER_AGENT)
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val body = response.body?.string().orEmpty()
                    val root = json.parseToJsonElement(body).jsonObject
                    root["foods"]?.jsonArray?.mapNotNull { toHit(it.jsonObject) }.orEmpty()
                }
            }.getOrDefault(emptyList())
        }

    suspend fun fetchById(sourceId: String, apiKey: String): RemoteFoodHit? =
        withContext(Dispatchers.IO) {
            if (sourceId.isBlank()) return@withContext null
            val request = Request.Builder()
                .url("$BASE_URL/food/$sourceId?apiKey=$apiKey")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string().orEmpty()
                    toHit(json.parseToJsonElement(body).jsonObject)
                }
            }.getOrNull()
        }

    private fun toHit(obj: JsonObject): RemoteFoodHit? {
        val fdcId = obj["fdcId"]?.jsonPrimitive?.content?.trim().orEmpty()
        val description = obj["description"]?.jsonPrimitive?.content?.trim().orEmpty()
        if (fdcId.isBlank() || description.isBlank()) return null

        val kcal = nutrient(obj, ENERGY_IDS) ?: return null
        val protein = nutrient(obj, PROTEIN_IDS) ?: return null
        val carbs = nutrient(obj, CARB_IDS) ?: return null
        val fat = nutrient(obj, FAT_IDS) ?: return null
        val fiber = nutrient(obj, FIBER_IDS)

        val dataType = obj["dataType"]?.jsonPrimitive?.content.orEmpty()
        val branded = dataType.contains("Branded", ignoreCase = true)
        val brand = obj["brandName"]?.jsonPrimitive?.content?.trim()
            ?: obj["brandOwner"]?.jsonPrimitive?.content?.trim()
        val category = categoryOf(obj)

        val food = Food(
            id = 0L,
            name = description,
            brand = brand.orEmpty(),
            category = category,
            kcalPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            fiberPer100g = fiber,
            source = FoodSource.USDA,
            barcode = obj["gtinUpc"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() },
            state = if (branded) FoodState.COMMERCIAL else FoodState.RAW,
            pricePerKg = null,
            isCustom = false
        )

        val missing = buildList {
            if (fiber == null) add("fibre")
            add("zuccheri")
            add("sale")
        }
        return RemoteFoodHit(
            food = food,
            sourceId = fdcId,
            sourceName = "USDA FoodData Central",
            sourceUpdatedAt = null,
            sourceNote = "Valori per 100 g da USDA (${dataType.ifBlank { "food" }}); " +
                "non riportati dalla fonte: ${missing.joinToString(", ")}."
        )
    }

    private fun categoryOf(obj: JsonObject): String {
        val raw = obj["foodCategory"] ?: return ""
        val direct = runCatching { raw.jsonPrimitive.content }.getOrNull()
        if (!direct.isNullOrBlank()) return direct
        return runCatching { raw.jsonObject["description"]?.jsonPrimitive?.content.orEmpty() }
            .getOrDefault("")
    }

    /**
     * Reads a nutrient by identifier from either response shape: search returns
     * nutrientId/value, the detail endpoint returns a nested nutrient object with
     * an amount. Unit is not checked by name, only that a number exists.
     */
    private fun nutrient(obj: JsonObject, ids: Set<Int>): Double? {
        val nutrients = obj["foodNutrients"]?.jsonArray ?: return null
        for (element in nutrients) {
            val node = runCatching { element.jsonObject }.getOrNull() ?: continue
            val id = node["nutrientId"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: runCatching {
                    node["nutrient"]?.jsonObject?.get("id")?.jsonPrimitive?.content?.toIntOrNull()
                }.getOrNull()
            if (id == null || id !in ids) continue
            val value = node["value"]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: node["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
            if (value != null) return value
        }
        return null
    }

    private companion object {
        const val BASE_URL = "https://api.nal.usda.gov/fdc/v1"
        const val USER_AGENT = "FitCoach/2.0 (Android)"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        /** 1008 = Energy kcal, 2047/2048 = Energy (Atwater) used by some records. */
        val ENERGY_IDS = setOf(1008, 2047, 2048)
        val PROTEIN_IDS = setOf(1003)
        val CARB_IDS = setOf(1005)
        val FAT_IDS = setOf(1004)
        val FIBER_IDS = setOf(1079)
    }
}
