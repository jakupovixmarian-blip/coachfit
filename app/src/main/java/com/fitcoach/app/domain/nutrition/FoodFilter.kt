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

    /**
     * L'utente scrive l'allergene, non l'alimento: "lattosio", "glutine",
     * "frutta a guscio". Il confronto per sottostringa non trova nulla, perche'
     * nessun alimento si chiama cosi', e l'allergia veniva ignorata: un utente
     * allergico al lattosio riceveva comunque latte e yogurt nel piano.
     * Ogni voce elenca cio' che quell'allergene rende vietato.
     */
    private val allergenRules: List<Pair<List<String>, (FoodItem) -> Boolean>> = listOf(
        listOf("lattosio", "latte", "latticini", "dairy") to { food ->
            food.category == "Latticini" ||
                listOf("latte", "yogurt", "ricotta", "cottage", "formaggio", "parmigian", "grana")
                    .any { food.name.contains(it, true) }
        },
        listOf("glutine", "celiac", "grano", "frumento") to { food ->
            glutenFoods.contains(food.name) ||
                listOf("pasta", "pane", "farina", "cous cous", "seitan", "orzo", "farro",
                    "crackers", "fette biscottate", "piadina", "gnocchi", "polenta", "muesli",
                    "cornflakes", "crusca", "germe di grano", "biscott")
                    .any { food.name.contains(it, true) }
        },
        listOf("uovo", "uova", "ovoalbumin") to { food ->
            food.category == "Uova" || food.name.contains("uovo", true) || food.name.contains("albume", true)
        },
        listOf("frutta a guscio", "frutta secca", "nuts", "nocciole") to { food ->
            listOf("mandorl", "nocciol", "noci", "anacardi", "arachidi", "pistacch", "pecan", "macadamia")
                .any { food.name.contains(it, true) }
        },
        listOf("pesce", "ittico") to { food -> food.category == "Pesce" },
        listOf("crostacei", "molluschi", "frutti di mare") to { food ->
            food.category in setOf("Crostacei", "Molluschi") ||
                listOf("gamber", "calamar", "polpo", "cozze", "vongole", "granchio")
                    .any { food.name.contains(it, true) }
        },
        listOf("soia", "soia e derivati") to { food ->
            food.name.contains("soia", true) || food.name.contains("tofu", true) ||
                food.name.contains("edamame", true)
        },
        listOf("arachidi") to { food -> food.name.contains("arachid", true) }
    )

    private fun allergenHit(term: String, food: FoodItem): Boolean {
        val t = term.trim().lowercase()
        if (t.isEmpty()) return false
        allergenRules.forEach { (keys, rule) ->
            if (keys.any { t == it || t.contains(it) }) return rule(food)
        }
        return food.name.contains(t, true) || food.category.contains(t, true)
    }

    /**
     * Un'esclusione dichiarata dall'utente vale se compare nel nome o nella
     * categoria dell'alimento. Il confronto e' per sottostringa perche' nessuno
     * scrive "Petto di pollo": scrive "pollo". Il confronto esatto lascerebbe
     * passare quasi tutte le esclusioni reali.
     */
    private fun matches(excluded: String, food: FoodItem): Boolean {
        val term = excluded.trim()
        if (term.isEmpty()) return false
        return food.name.contains(term, ignoreCase = true) ||
            food.category.contains(term, ignoreCase = true)
    }

    fun isAllowed(food: FoodItem, profile: UserProfile): Boolean {
        if (profile.excludedFoods.any { matches(it, food) }) return false

        // Allergie / intolleranze: l'allergene scritto dall'utente va tradotto
        // negli alimenti che lo contengono.
        if (profile.allergies.any { allergenHit(it, food) }) return false

        val prefs = profile.dietaryPreferences

        if (DietaryPreference.VEGAN in prefs) {
            // La categoria non basta: "Pasta all'uovo" sta in Cereali e passava.
            if (food.category in animalCategories) return false
            if (animalIngredient(food)) return false
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

    /**
     * Ingredienti animali nascosti in alimenti classificati altrove: la pasta
     * all'uovo sta nei Cereali, il pesto contiene formaggio, i gnocchi possono
     * contenere uovo. Il solo controllo di categoria li lasciava passare.
     */
    private fun animalIngredient(food: FoodItem): Boolean {
        val hidden = listOf("uovo", "albume", "tuorlo", "latte", "burro", "formaggio",
            "parmigian", "grana", "ricotta", "panna", "miele", "pesce", "tonno",
            "acciugh", "carne", "pollo", "prosciutto", "salame")
        return hidden.any { food.name.contains(it, true) }
    }

    fun allowedFoods(all: List<FoodItem>, profile: UserProfile): List<FoodItem> =
        all.filter { isAllowed(it, profile) }

    /**
     * Sostituti compatibili con le restrizioni dell'utente, ordinati per
     * somiglianza nutrizionale al macro dominante dell'originale.
     *
     * Unisce i sostituti espliciti dichiarati nella banca dati con le varianti
     * della stessa categoria, cosi l'utente ha SEMPRE piu alternative tra cui
     * scegliere (non solo una) e non resta bloccato se la prima non e compatibile.
     */
    fun findSubstitutes(
        food: FoodItem,
        all: List<FoodItem>,
        profile: UserProfile
    ): List<FoodItem> {
        val allowed = allowedFoods(all, profile)
        val byName = allowed.associateBy { it.name }
        val explicit = food.substituteNames.mapNotNull { byName[it] }

        val sameCategory = allowed.filter {
            it.id != food.id && it.category == food.category &&
                !it.name.equals(food.name, ignoreCase = true)
        }

        return (explicit + sameCategory)
            .distinctBy { it.id }
            .sortedBy { nutritionalDistance(food, it) }
            .take(MAX_SUBSTITUTES)
    }

    /** Distanza nutrizionale: privilegia alimenti con ruolo macro simile. */
    private fun nutritionalDistance(a: FoodItem, b: FoodItem): Double {
        fun share(value: Double, total: Double) = if (total <= 0.0) 0.0 else value / total
        val aKcal = a.kcalPer100g.coerceAtLeast(1.0)
        val bKcal = b.kcalPer100g.coerceAtLeast(1.0)
        val dProtein = kotlin.math.abs(share(a.proteinPer100g, aKcal) - share(b.proteinPer100g, bKcal))
        val dCarbs = kotlin.math.abs(share(a.carbsPer100g, aKcal) - share(b.carbsPer100g, bKcal))
        val dFat = kotlin.math.abs(share(a.fatPer100g, aKcal) - share(b.fatPer100g, bKcal))
        val dEnergy = kotlin.math.abs(a.kcalPer100g - b.kcalPer100g) / 300.0
        return dProtein + dCarbs + dFat + dEnergy
    }

    private const val MAX_SUBSTITUTES = 12
}
