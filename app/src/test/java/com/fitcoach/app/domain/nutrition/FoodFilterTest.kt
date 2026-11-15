package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FoodFilterTest {

    private fun food(
        id: Long,
        name: String,
        category: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        subs: List<String> = emptyList()
    ) = FoodItem(
        id = id, name = name, category = category, kcalPer100g = kcal,
        proteinPer100g = protein, carbsPer100g = carbs, fatPer100g = fat,
        source = "test", substituteNames = subs
    )

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 4, experience = TrainingExperience.INTERMEDIATE,
        goal = Goal.HYPERTROPHY
    )

    private val pollo = food(1, "Petto di pollo", "Carne", 110.0, 23.3, 0.0, 1.5, listOf("Fesa di tacchino"))
    private val tacchino = food(2, "Fesa di tacchino", "Carne", 108.0, 24.0, 0.0, 0.8)
    private val manzo = food(3, "Fesa di manzo", "Carne", 130.0, 21.5, 0.0, 4.5)
    private val lonza = food(4, "Lonza di maiale", "Carne", 143.0, 21.2, 0.0, 6.4)
    private val riso = food(5, "Riso bianco (crudo)", "Cereali", 332.0, 7.0, 80.0, 0.6)

    @Test
    fun `substitutes include explicit and same-category variants`() {
        val all = listOf(pollo, tacchino, manzo, lonza, riso)
        val result = FoodFilter.findSubstitutes(pollo, all, profile)

        // L'esplicito e presente, ma NON e l'unico: l'utente vede anche le varianti.
        assertThat(result.map { it.name }).contains("Fesa di tacchino")
        assertThat(result.map { it.name }).contains("Fesa di manzo")
        assertThat(result.size).isGreaterThan(1)
        // Alimenti di altra categoria non compaiono.
        assertThat(result.map { it.name }).doesNotContain("Riso bianco (crudo)")
    }

    @Test
    fun `same-category substitutes are ranked by macro similarity`() {
        val all = listOf(pollo, tacchino, manzo, lonza)
        val result = FoodFilter.findSubstitutes(pollo, all, profile)

        // Il tacchino (proteico magro) deve precedere la lonza (piu grassa).
        val tacchinoIndex = result.indexOfFirst { it.name == "Fesa di tacchino" }
        val lonzaIndex = result.indexOfFirst { it.name == "Lonza di maiale" }
        assertThat(tacchinoIndex).isLessThan(lonzaIndex)
    }

    @Test
    fun `substitutes respect allergies`() {
        val allergic = profile.copy(allergies = listOf("tacchino"))
        val all = listOf(pollo, tacchino, manzo)
        val result = FoodFilter.findSubstitutes(pollo, all, allergic)

        assertThat(result.map { it.name }).doesNotContain("Fesa di tacchino")
        assertThat(result.map { it.name }).contains("Fesa di manzo")
    }

    @Test
    fun `vegan preference removes animal substitutes`() {
        val vegan = profile.copy(dietaryPreferences = setOf(DietaryPreference.VEGAN))
        val all = listOf(pollo, tacchino, manzo, riso)
        val result = FoodFilter.findSubstitutes(pollo, all, vegan)

        assertThat(result).isEmpty()
    }
}
