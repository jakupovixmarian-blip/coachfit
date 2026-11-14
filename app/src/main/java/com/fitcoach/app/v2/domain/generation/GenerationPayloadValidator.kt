package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.v2.domain.GenerationPhase
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerationPayloadValidator @Inject constructor() {
    fun validate(phase: GenerationPhase, payload: JsonObject, profile: GenerationProfile): List<String> {
        val errors = mutableListOf<String>()
        if (payload["assumptions"] !is JsonArray) errors += "assumptions deve essere un array JSON."
        val required = when (phase) {
            GenerationPhase.NUTRITION -> "days"
            GenerationPhase.WORKOUT -> "days"
            GenerationPhase.CALENDAR -> "workoutDayIndices"
            GenerationPhase.INDICATORS -> "metrics"
        }
        if (payload[required] == null) errors += "Manca il campo $required."
        when (phase) {
            GenerationPhase.NUTRITION -> validateNutrition(payload, errors)
            GenerationPhase.WORKOUT -> validateWorkout(payload, profile, errors)
            GenerationPhase.CALENDAR -> validateCalendar(payload, errors)
            GenerationPhase.INDICATORS -> {
                if ((payload["metrics"] as? JsonArray).isNullOrEmpty()) errors += "metrics non può essere vuoto."
            }
        }
        return errors
    }

    private fun validateNutrition(payload: JsonObject, errors: MutableList<String>) {
        val days = payload["days"] as? JsonArray ?: return
        if (days.size != 7) errors += "La nutrizione deve contenere esattamente sette giorni."
        val indices = days.mapNotNull { (it as? JsonObject)?.int("dayIndex") }
        if (indices.toSet() != (0..6).toSet()) errors += "Gli indici nutrizionali devono coprire i giorni da 0 a 6."
        days.forEach { item ->
            val day = item as? JsonObject ?: return@forEach
            if (day.string("dayType") !in setOf("HIGH", "MEDIUM", "LOW")) errors += "Tipo di giornata nutrizionale non valido."
            val meals = day["meals"] as? JsonArray
            if (meals == null || meals.size !in 3..6) errors += "Ogni giorno deve contenere da 3 a 6 pasti."
            meals?.forEach { mealItem ->
                val entries = (mealItem as? JsonObject)?.get("entries") as? JsonArray
                if (entries.isNullOrEmpty()) errors += "Ogni pasto deve avere almeno un alimento."
                entries?.forEach { entryItem ->
                    val entry = entryItem as? JsonObject
                    if (entry?.string("foodName").isNullOrBlank() || (entry?.double("grams") ?: 0.0) <= 0.0) {
                        errors += "Ogni alimento deve avere nome e grammi positivi."
                    }
                }
            }
        }
    }

    private fun validateWorkout(payload: JsonObject, profile: GenerationProfile, errors: MutableList<String>) {
        val days = payload["days"] as? JsonArray ?: return
        if (days.size != profile.trainingDays) errors += "Il numero di giorni workout non coincide con quello dichiarato."
        days.forEach { item ->
            val exercises = (item as? JsonObject)?.get("exercises") as? JsonArray
            if (exercises.isNullOrEmpty()) errors += "Ogni giorno workout deve avere esercizi."
            exercises?.forEach { exerciseItem ->
                val exercise = exerciseItem as? JsonObject
                if (exercise?.string("exerciseId").isNullOrBlank()) errors += "Ogni esercizio deve avere un identificativo."
                if ((exercise?.int("sets") ?: 0) <= 0 || (exercise?.int("minReps") ?: 0) <= 0 ||
                    (exercise?.int("maxReps") ?: 0) < (exercise?.int("minReps") ?: 0)
                ) errors += "Serie e ripetizioni workout non valide."
            }
        }
    }

    private fun validateCalendar(payload: JsonObject, errors: MutableList<String>) {
        val workout = payload["workoutDayIndices"] as? JsonArray
        val rest = payload["restDayIndices"] as? JsonArray
        val workoutIndices = workout?.mapNotNull { it.jsonPrimitive.content.toIntOrNull() }?.toSet()
        val restIndices = rest?.mapNotNull { it.jsonPrimitive.content.toIntOrNull() }?.toSet()
        val covered = workoutIndices.orEmpty() + restIndices.orEmpty()
        if (workoutIndices == null || restIndices == null || workoutIndices.orEmpty().intersect(restIndices.orEmpty()).isNotEmpty() ||
            covered != (0..6).toSet()
        ) errors += "Il calendario deve coprire i sette giorni senza collisioni."
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
    private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()
    private fun JsonObject.double(key: String): Double? = string(key)?.replace(',', '.')?.toDoubleOrNull()
}

