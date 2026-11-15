package com.fitcoach.app.domain.coach.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Costruzione di risultati JSON compatti e leggibili per il modello.
 * I numeri vengono arrotondati per non sprecare token; i valori nulli omessi.
 */
object JsonOut {
    private val compact = Json { encodeDefaults = false; explicitNulls = false }

    fun obj(vararg pairs: Pair<String, JsonElement?>): JsonObject = buildJsonObject {
        pairs.forEach { (k, v) -> if (v != null) put(k, v) }
    }

    fun str(v: String?): JsonElement? = v?.let { JsonPrimitive(it) }

    fun num(v: Number?): JsonElement? = v?.let { JsonPrimitive(round(it)) }

    fun bool(v: Boolean?): JsonElement? = v?.let { JsonPrimitive(it) }

    fun arr(items: List<JsonElement>): JsonArray = buildJsonArray { items.forEach { add(it) } }

    fun strings(values: Iterable<String>): JsonElement = buildJsonArray { values.forEach { add(JsonPrimitive(it)) } }

    fun encode(element: JsonElement): String = compact.encodeToString(JsonElement.serializer(), element)

    private fun round(v: Number): Number = when (v) {
        is Double -> if (v.isNaN() || v.isInfinite()) 0.0 else Math.round(v * 10.0) / 10.0
        is Float -> Math.round(v.toDouble() * 10.0) / 10.0
        else -> v
    }
}
