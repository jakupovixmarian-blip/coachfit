package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.v2.domain.GenerationPhase
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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
        if (phase == GenerationPhase.WORKOUT && payload["days"] is JsonArray) {
            val days = payload["days"] as JsonArray
            if (days.size != profile.trainingDays) {
                errors += "Il numero di giorni workout non coincide con quello dichiarato."
            }
        }
        return errors
    }
}
