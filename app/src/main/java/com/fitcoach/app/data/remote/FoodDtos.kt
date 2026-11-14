package com.fitcoach.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------- Open Food Facts ----------------

/**
 * Risposta di ricerca Open Food Facts (v1 `cgi/search.pl` con `json=1`).
 * I campi possono essere assenti o nulli: tutto e opzionale.
 */
@Serializable
data class OffSearchResponse(
    val count: Int? = null,
    val products: List<OffProduct> = emptyList()
)

@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_it") val productNameIt: String? = null,
    val brands: String? = null,
    @SerialName("nutriments") val nutriments: OffNutriments? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null
) {
    /** Nome preferendo la lingua italiana, poi il nome generico. */
    val displayName: String? get() = productNameIt?.takeIf { it.isNotBlank() }
        ?: productName?.takeIf { it.isNotBlank() }
}

@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("energy_100g") val energyKj100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null
) {
    /** kcal/100g: usa il campo kcal se presente, altrimenti converte da kJ. */
    val kcal100g: Double? get() = energyKcal100g ?: energyKj100g?.let { it / 4.184 }
}

/** Risposta del prodotto singolo Open Food Facts (v2). */
@Serializable
data class OffProductResponse(
    val status: Int? = null,
    val product: OffProduct? = null
)

// ---------------- USDA FoodData Central ----------------

@Serializable
data class UsdaSearchResponse(
    val foods: List<UsdaFood> = emptyList()
)

@Serializable
data class UsdaFood(
    @SerialName("fdcId") val fdcId: Long? = null,
    val description: String? = null,
    @SerialName("brandOwner") val brandOwner: String? = null,
    val brandName: String? = null,
    @SerialName("foodNutrients") val foodNutrients: List<UsdaNutrient> = emptyList()
) {
    fun nutrient(vararg numbers: String): Double? = foodNutrients.firstOrNull { n ->
        n.nutrientNumber in numbers || n.nutrientName in numbers
    }?.value
}

@Serializable
data class UsdaNutrient(
    @SerialName("nutrientId") val nutrientId: Int? = null,
    @SerialName("nutrientNumber") val nutrientNumber: String? = null,
    @SerialName("nutrientName") val nutrientName: String? = null,
    @SerialName("unitName") val unitName: String? = null,
    val value: Double? = null
)
