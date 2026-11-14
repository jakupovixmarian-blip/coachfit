package com.fitcoach.app.data.repository

import com.fitcoach.app.data.remote.OffNutriments
import com.fitcoach.app.data.remote.OffProduct
import com.fitcoach.app.data.remote.UsdaFood
import com.fitcoach.app.data.remote.UsdaNutrient
import com.fitcoach.app.domain.model.FoodItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FoodSearchMappingTest {

    @Test
    fun `open food facts product maps with italian name and kcal`() {
        val product = OffProduct(
            code = "8001234567890",
            productName = "Chicken breast",
            productNameIt = "Petto di pollo a fette",
            brands = "Amadori, altri",
            nutriments = OffNutriments(
                energyKcal100g = 110.0, proteins100g = 23.0,
                carbohydrates100g = 1.0, fat100g = 2.0, fiber100g = 0.0
            )
        )
        val item = product.toFoodItem()!!
        assertThat(item.name).isEqualTo("Petto di pollo a fette")
        assertThat(item.kcalPer100g).isWithin(0.01).of(110.0)
        assertThat(item.proteinPer100g).isWithin(0.01).of(23.0)
        assertThat(item.sourceId).isEqualTo("8001234567890")
        assertThat(item.brand).isEqualTo("Amadori")
        assertThat(item.normalizedName).isEqualTo("petto di pollo a fette")
    }

    @Test
    fun `open food facts converts kJ when kcal missing`() {
        val product = OffProduct(
            productName = "Snack",
            nutriments = OffNutriments(energyKj100g = 418.4, proteins100g = 5.0)
        )
        val item = product.toFoodItem()!!
        assertThat(item.kcalPer100g).isWithin(0.5).of(100.0)
    }

    @Test
    fun `product without nutriments is rejected not invented`() {
        val product = OffProduct(productName = "Ignoto", nutriments = null)
        assertThat(product.toFoodItem()).isNull()
    }

    @Test
    fun `usda food maps nutrient numbers`() {
        val food = UsdaFood(
            fdcId = 12345L,
            description = "CHICKEN BREAST, RAW",
            foodNutrients = listOf(
                UsdaNutrient(nutrientNumber = "208", value = 120.0),
                UsdaNutrient(nutrientNumber = "203", value = 22.5),
                UsdaNutrient(nutrientNumber = "205", value = 0.0),
                UsdaNutrient(nutrientNumber = "204", value = 2.6)
            )
        )
        val item = food.toFoodItem()!!
        assertThat(item.kcalPer100g).isWithin(0.01).of(120.0)
        assertThat(item.proteinPer100g).isWithin(0.01).of(22.5)
        assertThat(item.sourceId).isEqualTo("12345")
        assertThat(item.name).isEqualTo("Chicken breast, raw")
    }

    @Test
    fun `usda food without energy is rejected`() {
        val food = UsdaFood(fdcId = 1L, description = "X", foodNutrients = emptyList())
        assertThat(food.toFoodItem()).isNull()
    }

    @Test
    fun `normalization removes accents and punctuation`() {
        assertThat(FoodItem.normalizeName("Petto di pollo, à la plancha!"))
            .isEqualTo("petto di pollo a la plancha")
    }
}
