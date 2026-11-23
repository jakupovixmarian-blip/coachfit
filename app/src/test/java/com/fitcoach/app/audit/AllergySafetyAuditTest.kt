package com.fitcoach.app.audit

import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L'allergia dichiarata dall'utente deve escludere davvero gli alimenti che la
 * contengono. Il filtro confronta il termine con nome e categoria: funziona per
 * "pollo" o "pesce", ma non quando l'allergene e' una sostanza ("lattosio")
 * mentre l'alimento si chiama diversamente ("Latte scremato").
 */
class AllergySafetyAuditTest {

    private val catalog = FoodSeedData.foods

    private fun allowedNames(profile: UserProfile): List<String> =
        FoodFilter.allowedFoods(catalog, profile).map { it.name }

    @Test
    fun `allergia al lattosio esclude latte e yogurt`() {
        val profile = UserProfile(allergies = listOf("lattosio"))
        val allowed = allowedNames(profile)
        // "Latte di mandorla" e "Latte d'avena" sono bevande vegetali: non
        // contengono lattosio e devono restare disponibili.
        val violations = allowed.filter { name ->
            name.contains("Latte", true) && !name.contains("mandorla", true) && !name.contains("avena", true) ||
                name.contains("Yogurt", true) || name.contains("Ricotta", true) ||
                name.contains("cottage", true) || name.contains("capra", true)
        }
        assertTrue(
            "Con allergia al lattosio restano ${violations.size} alimenti lattiero-caseari: $violations",
            violations.isEmpty()
        )
    }

    @Test
    fun `allergia alle uova esclude uova e albume`() {
        val profile = UserProfile(allergies = listOf("uova"))
        val allowed = allowedNames(profile)
        val violations = allowed.filter { it.contains("uovo", true) || it.contains("albume", true) }
        assertTrue("Con allergia alle uova restano: $violations", violations.isEmpty())
    }

    @Test
    fun `allergia al glutine esclude pasta pane e farine`() {
        val profile = UserProfile(allergies = listOf("glutine"))
        val allowed = allowedNames(profile)
        val violations = allowed.filter {
            it.contains("Pasta", true) || it.contains("Pane", true) ||
                it.contains("farina", true) || it.contains("Cous cous", true)
        }
        assertTrue("Con allergia al glutine restano: $violations", violations.isEmpty())
    }

    @Test
    fun `allergia alla frutta a guscio esclude le mandorle`() {
        val profile = UserProfile(allergies = listOf("frutta a guscio"))
        val allowed = allowedNames(profile)
        val violations = allowed.filter {
            it.contains("mandorl", true) || it.contains("nocciol", true) ||
                it.contains("noci", true) || it.contains("anacard", true) ||
                it.contains("arachid", true)
        }
        assertTrue("Con allergia alla frutta a guscio restano: $violations", violations.isEmpty())
    }

    @Test
    fun `preferenza vegana esclude ogni alimento animale`() {
        val profile = UserProfile(dietaryPreferences = setOf(DietaryPreference.VEGAN))
        val allowed = allowedNames(profile)
        val violations = allowed.filter {
            // Le bevande vegetali contengono "latte" nel nome ma sono vegane.
            (it.contains("latte", true) && !it.contains("mandorla", true) && !it.contains("avena", true) &&
                !it.contains("soia", true)) ||
                it.contains("pollo", true) || it.contains("uovo", true) ||
                it.contains("salmone", true) || it.contains("manzo", true) || it.contains("ricotta", true)
        }
        assertTrue("Con profilo vegano restano: $violations", violations.isEmpty())
    }
}
