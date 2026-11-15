package com.fitcoach.app.domain.coach.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/** Costruttori compatti di JSON Schema per gli strumenti. */
object Schema {

    fun string(description: String): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("string"))
        put("description", JsonPrimitive(description))
    }

    fun number(description: String): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("number"))
        put("description", JsonPrimitive(description))
    }

    fun integer(description: String): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("integer"))
        put("description", JsonPrimitive(description))
    }

    fun boolean(description: String): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("boolean"))
        put("description", JsonPrimitive(description))
    }

    fun enumOf(description: String, vararg values: String): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("string"))
        put("description", JsonPrimitive(description))
        put("enum", JsonArray(values.map { JsonPrimitive(it) }))
    }

    fun array(description: String, items: JsonObject): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("array"))
        put("description", JsonPrimitive(description))
        put("items", items)
    }

    fun arrayOfStrings(description: String): JsonObject = array(description, string("valore"))

    /** Oggetto con proprieta e campi obbligatori. */
    fun obj(
        vararg properties: Pair<String, JsonObject>,
        required: List<String> = emptyList(),
        additionalProperties: Boolean = false
    ): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("properties", buildJsonObject {
            properties.forEach { (k, v) -> put(k, v) }
        })
        if (required.isNotEmpty()) {
            put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
        }
        put("additionalProperties", JsonPrimitive(additionalProperties))
    }

    /** Oggetto libero (mappa chiave/valore) senza schema rigido. */
    fun freeObject(description: String): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("description", JsonPrimitive(description))
        put("additionalProperties", JsonPrimitive(true))
    }
}
