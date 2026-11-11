package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class MealPlanGeneratorTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 4, experience = TrainingExperience.INTERMEDIATE, goal = Goal.RECOMP
    )
    private val energy = EnergyCalculator.compute(profile)!!
    private val cycling = CarbCyclingEngine.build(profile, energy)
    private val dayTypes = CarbCyclingEngine.assignDayTypes(2, 1, 4, listOf(0, 1, 3, 4))

    @Test
    fun `generates seven days`() {
        val days = MealPlanGenerator.generate(profile, cycling, FoodSeedData.foods, dayTypes)
        assertThat(days).hasSize(7)
    }

    @Test
    fun `every day has at least four meals`() {
        val days = MealPlanGenerator.generate(profile, cycling, FoodSeedData.foods, dayTypes)
        days.forEach { assertThat(it.meals.size).isAtLeast(4) }
    }

    @Test
    fun `meal calories are within tolerance of the day target`() {
        val days = MealPlanGenerator.generate(profile, cycling, FoodSeedData.foods, dayTypes)
        days.forEach { day ->
            // Il generatore dichiara coerenza entro 120 kcal.
            assertThat(abs(day.deviations.kcal)).isAtMost(160.0)
        }
    }

    @Test
    fun `gram quantities are positive`() {
        val days = MealPlanGenerator.generate(profile, cycling, FoodSeedData.foods, dayTypes)
        days.flatMap { it.meals }.flatMap { it.entries }.forEach {
            assertThat(it.grams).isGreaterThan(0.0)
        }
    }

    @Test
    fun `gramsForMacro computes inverse of per100g`() {
        // 200 g di un alimento con 20 g proteine/100g = 40 g proteine.
        val grams = MealPlanGenerator.gramsForMacro(40.0, 20.0)
        assertThat(grams).isWithin(0.01).of(200.0)
    }

    @Test
    fun `gramsForMacro guards against zero macro`() {
        assertThat(MealPlanGenerator.gramsForMacro(40.0, 0.0)).isEqualTo(0.0)
    }

    @Test
    fun `excluded foods are never used`() {
        val excluded = profile.copy(excludedFoods = listOf("Petto di pollo"))
        val days = MealPlanGenerator.generate(excluded, cycling, FoodSeedData.foods, dayTypes)
        val names = days.flatMap { it.meals }.flatMap { it.entries }.map { it.foodName }
        assertThat(names).doesNotContain("Petto di pollo")
    }

    @Test
    fun `substitute grams preserves the macro contribution`() {
        val chicken = FoodSeedData.foods.first { it.name == "Petto di pollo" }
        val turkey = FoodSeedData.foods.first { it.name == "Fesa di tacchino" }
        val grams = MealPlanGenerator.substituteGrams(chicken, 150.0, turkey)
        // 150 g di pollo = 34.95 g proteine -> tacchino (24 g/100g) ~ 145.6 g
        val expectedProtein = 150.0 * chicken.proteinPer100g / 100.0
        val actualProtein = grams * turkey.proteinPer100g / 100.0
        assertThat(actualProtein).isWithin(1.0).of(expectedProtein)
    }

    @Test
    fun `dominant macro identifies protein foods`() {
        val chicken = FoodSeedData.foods.first { it.name == "Petto di pollo" }
        assertThat(MealPlanGenerator.dominantMacro(chicken))
            .isEqualTo(MealPlanGenerator.DominantMacro.PROTEIN)
    }

    @Test
    fun `custom food database produces valid plan`() {
        val custom = listOf(
            FoodItem(id = 1, name = "Pollo test", category = "Carne", kcalPer100g = 110.0,
                proteinPer100g = 23.0, carbsPer100g = 0.0, fatPer100g = 1.5, source = "test"),
            FoodItem(id = 2, name = "Riso test", category = "Cereali", kcalPer100g = 350.0,
                proteinPer100g = 7.0, carbsPer100g = 78.0, fatPer100g = 0.6, source = "test"),
            FoodItem(id = 3, name = "Olio test", category = "Grassi", kcalPer100g = 900.0,
                proteinPer100g = 0.0, carbsPer100g = 0.0, fatPer100g = 100.0, source = "test"),
            FoodItem(id = 4, name = "Verdura test", category = "Verdura", kcalPer100g = 25.0,
                proteinPer100g = 2.0, carbsPer100g = 4.0, fatPer100g = 0.2, source = "test")
        )
        val days = MealPlanGenerator.generate(profile, cycling, custom, dayTypes)
        assertThat(days).hasSize(7)
    }
}
