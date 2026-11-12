package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.data.ai.AiClient
import dev.fitcoach.app.data.ai.ChatMessage
import dev.fitcoach.app.domain.model.CoachChangeStatus
import dev.fitcoach.app.domain.model.CoachChangeType
import dev.fitcoach.app.domain.model.CoachProposal
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.MealItem
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.model.UserProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class ProposedMealItem(
    val slot: MealSlot,
    val foodId: Long,
    val grams: Double,
    val note: String
)

data class ProposedPlan(
    val items: List<ProposedMealItem>,
    val rationale: String,
    val rawResponse: String
)

/**
 * Builds prompts grounded on the user's real data and parses the model answer
 * into structured proposals. The model never returns nutrition values for foods:
 * it may only reference ids from the catalog, so numbers stay verified.
 */
@Singleton
class CoachPromptBuilder @Inject constructor(
    private val json: Json
) {

    fun planPrompt(profile: UserProfile, foods: List<Food>, targetKcal: Double?, targetProtein: Double?): String {
        val catalog = foods.take(120).joinToString("\n") { food ->
            "- id=${food.id} | ${food.name} | ${food.kcalPer100g.toInt()} kcal/100g | P ${food.proteinPer100g} C ${food.carbsPer100g} G ${food.fatPer100g} | ${food.state.label}"
        }
        return """
            Sei un nutrizionista che lavora SOLO con l'archivio alimenti fornito.
            REGOLE OBBLIGATORIE:
            - Puoi usare esclusivamente gli id presenti nell'archivio.
            - Non inventare alimenti, valori nutrizionali o grammature fuori scala.
            - Rispetta allergie, alimenti non graditi, budget e tempo di preparazione dichiarati.
            - Restituisci SOLO JSON valido, senza testo aggiuntivo.

            PROFILO:
            obiettivo=${profile.goal}, sesso=${profile.sex}, eta=${profile.age}, altezza=${profile.heightCm} cm,
            peso=${profile.weightKg} kg, pasti al giorno=${profile.mealsPerDay}, orari=${profile.mealTimes},
            preferenze=${profile.dietPreference}, allergie=${profile.allergies},
            cibi graditi=${profile.likedFoods}, cibi non graditi=${profile.dislikedFoods},
            budget settimanale=${profile.weeklyBudget}, minuti cucina=${profile.cookingMinutes}

            TARGET GIORNALIERI (calcolati dall'app):
            calorie=${targetKcal?.toInt()}, proteine=${targetProtein?.toInt()} g

            ARCHIVIO ALIMENTI:
            $catalog

            FORMATO RISPOSTA:
            {"rationale":"spiegazione breve in italiano","items":[{"slot":"BREAKFAST","foodId":12,"grams":80,"note":""}]}
        """.trimIndent()
    }

    fun parsePlan(raw: String, knownFoodIds: Set<Long>): ProposedPlan {
        val jsonText = extractJson(raw)
        val root = runCatching { json.parseToJsonElement(jsonText).jsonObject }
            .getOrElse { throw AppError.InvalidResponse("Il modello non ha restituito JSON valido") }
        val rationale = root["rationale"]?.jsonPrimitive?.content.orEmpty()
        val items = (root["items"] as? JsonArray)?.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val slotName = obj["slot"]?.jsonPrimitive?.content.orEmpty().uppercase()
            val slot = MealSlot.entries.firstOrNull { it.name == slotName } ?: return@mapNotNull null
            val foodId = obj["foodId"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
            if (foodId !in knownFoodIds) return@mapNotNull null
            val grams = obj["grams"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
            if (grams <= 0 || grams > 2000) return@mapNotNull null
            ProposedMealItem(slot, foodId, grams, obj["note"]?.jsonPrimitive?.content.orEmpty())
        }.orEmpty()

        if (items.isEmpty()) {
            throw AppError.InvalidResponse("Nessuna voce valida nel piano proposto")
        }
        return ProposedPlan(items, rationale, raw)
    }

    fun proposalFor(
        type: CoachChangeType,
        rationale: String,
        previous: String,
        new: String,
        origin: String = "Coach AI"
    ) = CoachProposal(
        createdAt = System.currentTimeMillis(),
        changeType = type,
        rationale = rationale,
        previousValue = previous,
        newValue = new,
        origin = origin,
        status = CoachChangeStatus.PROPOSED
    )

    fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start >= 0 && end > start) return raw.substring(start, end + 1)
        throw AppError.InvalidResponse("Nessun oggetto JSON nella risposta del modello")
    }
}
