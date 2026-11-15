package com.fitcoach.app.domain.coach.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Accesso tollerante agli argomenti JSON inviati dal modello. */
class JsonArgs(private val obj: JsonObject) {

    fun string(key: String): String? = (obj[key] as? JsonPrimitive)?.contentOrNullSafe()

    fun int(key: String): Int? = (obj[key] as? JsonPrimitive)?.intOrNull

    fun double(key: String): Double? = (obj[key] as? JsonPrimitive)?.doubleOrNull

    fun boolean(key: String): Boolean? = (obj[key] as? JsonPrimitive)?.booleanOrNull

    fun strings(key: String): List<String> =
        (obj[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNullSafe() } ?: emptyList()

    fun ints(key: String): List<Int> =
        (obj[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.intOrNull } ?: emptyList()

    fun objectAt(key: String): JsonObject? = obj[key] as? JsonObject

    fun arrayAt(key: String): JsonArray? = obj[key] as? JsonArray

    fun raw(): JsonObject = obj

    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (this is JsonPrimitive && (isString || content != "null")) content else null
}
