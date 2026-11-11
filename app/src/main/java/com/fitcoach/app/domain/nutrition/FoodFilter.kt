package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.UserProfile

/**
 * Filtro alimentare deterministico: applica preferenze, esclusioni, allergie
 * e intolleranze dell'utente al database alimentare.
 *
 * Regola di sicurezza: un alimento che corrisponde a un allergene dichiarato
 * viene SEMPRE escluso, anche se presente in una preferenza.
 */
object FoodFilter {

    private val glutenFoods = setOf(
        "Pasta di semola (cruda)", "Pasta integrale (cruda)", "Pane bianco", "Pane integrale",
        "Fette biscottate", "Cous cous (crudo)", "Seitan", "Fiocchi d'avena"
    )

    private val highLactose = setOf(
        "Latte parzialmente scremato", "Latte scremato", "Ricotta vaccina",
        "Yogurt bianco magro", "Fiocchi di latte (cottage)", "Yogurt greco 0% grassi"
    )

    private val porkFoods = setOf("Lonza di maiale", "Prosciutto crudo sgrassato")
    private val meatCategories = setOf("Carne", "Salumi")
    private val fishCategories = setOf("Pesce")
    private val animalCategories = setOf("Carne", "Salumi", "Pesce", "Uova", "Latticini")
    private val dairyCategories = setOf("Latticini")

    fun isAllowed(food: FoodItem, profile: UserProfile): Boolean {
        if (profile.excludedFoods.any { it.equals(food.name, ignoreCase = true) }) return false

        // Allergie / intolleranze: confronto case-insensitive su nome e categoria.
        val allergyHit = profile.allergies.any { a ->
            food.name.contains(a, ignoreCase = true) || food.category.contains(a, ignoreCase = true)
        }
        if (allergyHit) return false

        val prefs = profile.dietaryPreferences

        if (DietaryPreference.VEGAN in prefs) {
            if (food.category in animalCategories) return false
        }
        if (DietaryPreference.VEGETARIAN in prefs) {
            if (food.category in meatCategories || food.category in fishCategories) return false
        }
        if (DietaryPreference.PESCATARIAN in prefs) {
            if (food.category in meatCategories) return false
        }
        if (DietaryPreference.LOW_LACTOSE in prefs) {
            if (food.name in highLactose) return false
        }
        if (DietaryPreference.GLUTEN_FREE in prefs) {
            if (food.name in glutenFoods) return false
        }
        if (DietaryPreference.HALAL in prefs) {
            if (food.name in porkFoods) return false
        }
        // Restrizione latticini generica via allergie (es. "lattosio") gia coperta sopra.
        return true
    }

    fun allowedFoods(all: List<FoodItem>, profile: UserProfile): List<FoodItem> =
        all.filter { isAllowed(it, profile) }

    /** Sostituti compatibili con le restrizioni dell'utente. */
    fun findSubstitutes(
        food: FoodItem,
        all: List<FoodItem>,
        profile: UserProfile
    ): List<FoodItem> {
        val allowed = allowedFoods(all, profile)
        val byName = allowed.associateBy { it.name }
        val explicit = food.substituteNames.mapNotNull { byName[it] }
        if (explicit.isNotEmpty()) return explicit
        // Fallback: stessa categoria, valori simili.
        return allowed.filter {
            it.id != food.id && it.category == food.category &&
                !it.name.equals(food.name, ignoreCase = true)
        }.sortedBy { kotlin.math.abs(it.proteinPer100g - food.proteinPer100g) }.take(3)
    }
}
