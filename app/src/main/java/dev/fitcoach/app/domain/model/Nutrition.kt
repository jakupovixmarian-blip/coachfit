package dev.fitcoach.app.domain.model

data class Food(
    val id: Long = 0L,
    val name: String,
    val brand: String = "",
    val category: String = "",
    /** Per 100 g reference values. Never invented: entered by the user or imported. */
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double? = null,
    val source: FoodSource = FoodSource.MANUAL,
    val barcode: String? = null,
    val state: FoodState = FoodState.RAW,
    val pricePerKg: Double? = null,
    val isCustom: Boolean = false,
    /** Where the values come from. Foods typed by hand carry no source. */
    val provenance: FoodProvenance = FoodProvenance.NONE
)

enum class FoodSource(val label: String) {
    MANUAL("Inserito a mano"), CREA("CREA"), OPEN_FOOD_FACTS("Open Food Facts"),
    USDA("USDA FoodData Central"), AI_PLAN("Piano AI")
}

enum class FoodState(val label: String) {
    RAW("Crudo"), COOKED("Cotto"), DRAINED("Sgocciolato"), COMMERCIAL("Prodotto commerciale")
}

enum class MealSlot(val label: String) {
    BREAKFAST("Colazione"), MORNING_SNACK("Spuntino mattina"), LUNCH("Pranzo"),
    AFTERNOON_SNACK("Spuntino pomeriggio"), DINNER("Cena"), EVENING_SNACK("Spuntino sera")
}

data class MealItem(
    val id: Long = 0L,
    val planDayId: Long,
    val mealSlot: MealSlot,
    val foodId: Long,
    val grams: Double,
    val alternativeGroup: Int? = null,
    val preparation: String = "",
    val timeLabel: String = ""
)

data class PlanDay(
    val id: Long = 0L,
    val planId: Long,
    val dayIndex: Int
)

data class MealPlan(
    val id: Long = 0L,
    val name: String,
    val isActive: Boolean = false,
    val targetKcal: Double = 0.0,
    val targetProtein: Double = 0.0,
    val targetCarbs: Double = 0.0,
    val targetFat: Double = 0.0,
    val createdAt: Long = 0L,
    val source: FoodSource = FoodSource.MANUAL
)

data class DiaryEntry(
    val id: Long = 0L,
    val dateEpochDay: Long,
    val mealSlot: MealSlot,
    val foodId: Long,
    val grams: Double,
    val consumed: Boolean = true,
    val planItemId: Long? = null,
    val loggedAt: Long = 0L
)

data class ShoppingItem(
    val id: Long = 0L,
    val name: String,
    val quantityGrams: Double = 0.0,
    val category: String = "",
    val estimatedPrice: Double? = null,
    val checked: Boolean = false,
    val weekStartEpochDay: Long = 0L
)
