package dev.fitcoach.app.domain.nutrition

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.MealSlot
import kotlin.math.abs
import kotlin.math.roundToInt

/** The macro targets the plan must reach. Computed elsewhere, never invented here. */
data class DietTargets(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

/** How a food is used inside a meal, derived from its own declared values. */
enum class FoodRole { PROTEIN, CARB, FAT, VEGETABLE }

data class PlannedFood(val food: Food, val grams: Double) {
    val kcal: Double get() = food.kcalPer100g * grams / 100.0
    val proteinG: Double get() = food.proteinPer100g * grams / 100.0
    val carbsG: Double get() = food.carbsPer100g * grams / 100.0
    val fatG: Double get() = food.fatPer100g * grams / 100.0
}

data class PlannedMeal(val slot: MealSlot, val foods: List<PlannedFood>) {
    val kcal: Double get() = foods.sumOf { it.kcal }
    val proteinG: Double get() = foods.sumOf { it.proteinG }
    val carbsG: Double get() = foods.sumOf { it.carbsG }
    val fatG: Double get() = foods.sumOf { it.fatG }
}

data class PlannedDay(val dayIndex: Int, val meals: List<PlannedMeal>) {
    val kcal: Double get() = meals.sumOf { it.kcal }
    val proteinG: Double get() = meals.sumOf { it.proteinG }
    val carbsG: Double get() = meals.sumOf { it.carbsG }
    val fatG: Double get() = meals.sumOf { it.fatG }

    fun deviationPct(targets: DietTargets): Double =
        if (targets.kcal <= 0) 0.0 else abs(kcal - targets.kcal) / targets.kcal * 100.0
}

data class DietPlan(
    val days: List<PlannedDay>,
    val targets: DietTargets,
    val warnings: List<String>
) {
    val averageKcal: Double get() = if (days.isEmpty()) 0.0 else days.sumOf { it.kcal } / days.size
    val averageProteinG: Double get() = if (days.isEmpty()) 0.0 else days.sumOf { it.proteinG } / days.size
}

/**
 * The plan request.
 *
 * [pool] is the food catalogue already stored in the database: the generator never
 * creates a food and never guesses a value. [excludedKeywords] covers allergies and
 * intolerances by name, [excludedFoodIds] the foods the user removed explicitly.
 */
data class DietRequest(
    val targets: DietTargets,
    val pool: List<Food>,
    val days: Int = 7,
    val mealsPerDay: Int = 4,
    val excludedFoodIds: Set<Long> = emptySet(),
    val excludedKeywords: List<String> = emptyList(),
    val maxKcalDeviationPct: Double = 7.0
)

/**
 * Builds 7-day plans from real food records.
 *
 * Every gram is computed here from the stored per-100 g values: the AI proposes which
 * foods to combine, this engine decides the quantities and checks that the resulting
 * totals match the targets. Nothing is invented and no average between conflicting
 * sources is taken.
 */
class DietGenerator {

    fun generate(request: DietRequest): DietPlan {
        val warnings = mutableListOf<String>()
        val days = (1..request.days.coerceIn(1, 7)).map { index ->
            buildDay(index, request, warnings)
        }
        return DietPlan(days = days, targets = request.targets, warnings = warnings.distinct())
    }

    private fun buildDay(dayIndex: Int, request: DietRequest, warnings: MutableList<String>): PlannedDay {
        val usable = request.pool.filter { food ->
            food.kcalPer100g > 0 &&
                food.id !in request.excludedFoodIds &&
                !hasExcludedKeyword(food, request.excludedKeywords)
        }
        if (usable.isEmpty()) {
            warnings += "Nessun alimento utilizzabile nel catalogo: la dieta non può essere generata."
            return PlannedDay(dayIndex, emptyList())
        }

        val byRole = FoodRole.entries.associateWith { role ->
            usable.filter { roleOf(it) == role }.sortedBy { it.name }
        }
        if (byRole[FoodRole.PROTEIN].isNullOrEmpty()) {
            warnings += "Nessuna fonte proteica nel catalogo: le proteine restano sotto il target."
        }
        if (byRole[FoodRole.CARB].isNullOrEmpty()) {
            warnings += "Nessuna fonte di carboidrati nel catalogo: i carboidrati restano sotto il target."
        }

        val slots = slotsFor(request.mealsPerDay)
        val shares = sharesFor(request.mealsPerDay)

        val meals = slots.mapIndexed { mealIndex, slot ->
            buildMeal(
                slot = slot,
                share = shares[mealIndex],
                targets = request.targets,
                byRole = byRole,
                rotation = dayIndex * 7 + mealIndex
            )
        }

        return correctDay(PlannedDay(dayIndex, meals), request, warnings)
    }

    /**
     * Anchors the protein first, then the carbohydrates and the fat left over, and only
     * then checks the calories. Grams are rounded to 5 g, which is what a scale shows.
     */
    private fun buildMeal(
        slot: MealSlot,
        share: Double,
        targets: DietTargets,
        byRole: Map<FoodRole, List<Food>>,
        rotation: Int
    ): PlannedMeal {
        val mealTargets = DietTargets(
            kcal = targets.kcal * share,
            proteinG = targets.proteinG * share,
            carbsG = targets.carbsG * share,
            fatG = targets.fatG * share
        )

        val foods = mutableListOf<PlannedFood>()
        var remainingProtein = mealTargets.proteinG
        var remainingCarbs = mealTargets.carbsG
        var remainingFat = mealTargets.fatG

        // Le verdure fanno parte del pasto: entrano nel conteggio invece di aggiungersi
        // alle calorie già assegnate, altrimenti il giorno supera il target.
        pick(byRole[FoodRole.VEGETABLE], rotation + 3)?.let { food ->
            if (food.kcalPer100g > 0) {
                val item = PlannedFood(food, if (share >= 0.2) 150.0 else 100.0)
                foods += item
                remainingProtein -= item.proteinG
                remainingCarbs -= item.carbsG
                remainingFat -= item.fatG
            }
        }

        pick(byRole[FoodRole.PROTEIN], rotation)?.let { food ->
            if (food.proteinPer100g > 0 && remainingProtein > 0) {
                val item = PlannedFood(food, roundTo5(remainingProtein / (food.proteinPer100g / 100.0)).coerceIn(20.0, 400.0))
                foods += item
                remainingProtein -= item.proteinG
                remainingCarbs -= item.carbsG
                remainingFat -= item.fatG
            }
        }

        pick(byRole[FoodRole.CARB], rotation + 1)?.let { food ->
            if (food.carbsPer100g > 0 && remainingCarbs > 0) {
                val item = PlannedFood(food, roundTo5(remainingCarbs / (food.carbsPer100g / 100.0)).coerceIn(0.0, 500.0))
                foods += item
                remainingProtein -= item.proteinG
                remainingCarbs -= item.carbsG
                remainingFat -= item.fatG
            }
        }

        pick(byRole[FoodRole.FAT], rotation + 2)?.let { food ->
            if (food.fatPer100g > 0 && remainingFat > 0) {
                val item = PlannedFood(food, roundTo5(remainingFat / (food.fatPer100g / 100.0)).coerceIn(0.0, 100.0))
                foods += item
                remainingFat -= item.fatG
            }
        }

        return correctMeal(PlannedMeal(slot, foods.filter { it.grams > 0 }), mealTargets)
    }

    /** Adjusts the carbohydrate food of one meal when its calories drift too far. */
    private fun correctMeal(meal: PlannedMeal, targets: DietTargets): PlannedMeal {
        if (targets.kcal <= 0) return meal
        var current = meal
        repeat(2) {
            val deviation = current.kcal - targets.kcal
            if (abs(deviation) / targets.kcal <= MEAL_TOLERANCE) return current
            val carb = current.foods
                .filter { it.food.carbsPer100g >= 10 }
                .maxByOrNull { it.grams } ?: return current
            val kcalPerGram = carb.food.kcalPer100g / 100.0
            if (kcalPerGram <= 0) return current
            val grams = roundTo5((carb.grams - deviation / kcalPerGram).coerceAtLeast(0.0))
            current = current.copy(
                foods = current.foods
                    .map { if (it.food.id == carb.food.id) it.copy(grams = grams) else it }
                    .filter { it.grams > 0 }
            )
        }
        return current
    }

    /**
     * One pass of correction on the carbohydrate food, the largest lever available.
     * Repeated a few times at most: if the targets themselves are inconsistent, the
     * plan reports it instead of silently forcing the numbers.
     */
    private fun correctDay(day: PlannedDay, request: DietRequest, warnings: MutableList<String>): PlannedDay {
        var current = day
        repeat(MAX_CORRECTIONS) {
            val deviation = current.kcal - request.targets.kcal
            val deviationPct = if (request.targets.kcal > 0) abs(deviation) / request.targets.kcal * 100.0 else 0.0
            if (deviationPct <= request.maxKcalDeviationPct) return current

            val meal = current.meals.firstOrNull { candidate ->
                candidate.foods.any { it.food.carbsPer100g >= 10 && it.grams > 0 }
            } ?: return current

            val carbItem = meal.foods
                .filter { it.food.carbsPer100g >= 10 && it.grams > 0 }
                .maxByOrNull { it.grams } ?: return current

            val kcalPerGram = carbItem.food.kcalPer100g / 100.0
            if (kcalPerGram <= 0) return current

            val deltaGrams = -deviation / kcalPerGram
            val newGrams = roundTo5((carbItem.grams + deltaGrams).coerceAtLeast(0.0))

            val updatedMeal = meal.copy(
                foods = meal.foods.map { item ->
                    if (item.food.id == carbItem.food.id) item.copy(grams = newGrams) else item
                }
            )
            current = current.copy(
                meals = current.meals.map { if (it.slot == meal.slot) updatedMeal else it }
            )
            if (updatedMeal.foods.isEmpty()) return current
        }

        val finalPct = current.deviationPct(request.targets)
        if (finalPct > request.maxKcalDeviationPct) {
            warnings += "Giorno ${current.dayIndex}: calorie a ${current.kcal.roundToInt()} kcal, " +
                "scostamento ${finalPct.roundToInt()}% dal target."
        }
        return current
    }

    private fun pick(candidates: List<Food>?, rotation: Int): Food? {
        if (candidates.isNullOrEmpty()) return null
        return candidates[rotation.mod(candidates.size)]
    }

    private fun hasExcludedKeyword(food: Food, keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return false
        val haystack = "${food.name} ${food.brand} ${food.category}".lowercase()
        return keywords.any { it.isNotBlank() && haystack.contains(it.lowercase()) }
    }

    private fun slotsFor(mealsPerDay: Int): List<MealSlot> = when (mealsPerDay.coerceIn(3, 6)) {
        3 -> listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER)
        4 -> listOf(MealSlot.BREAKFAST, MealSlot.MORNING_SNACK, MealSlot.LUNCH, MealSlot.DINNER)
        5 -> listOf(MealSlot.BREAKFAST, MealSlot.MORNING_SNACK, MealSlot.LUNCH, MealSlot.AFTERNOON_SNACK, MealSlot.DINNER)
        else -> listOf(
            MealSlot.BREAKFAST, MealSlot.MORNING_SNACK, MealSlot.LUNCH,
            MealSlot.AFTERNOON_SNACK, MealSlot.DINNER, MealSlot.EVENING_SNACK
        )
    }

    private fun sharesFor(mealsPerDay: Int): List<Double> = when (mealsPerDay.coerceIn(3, 6)) {
        3 -> listOf(0.30, 0.35, 0.35)
        4 -> listOf(0.25, 0.10, 0.35, 0.30)
        5 -> listOf(0.25, 0.10, 0.30, 0.10, 0.25)
        else -> listOf(0.20, 0.10, 0.28, 0.10, 0.22, 0.10)
    }

    /** Classifies a food by the share of energy its own macros provide. */
    private fun roleOf(food: Food): FoodRole {
        val kcal = food.kcalPer100g
        if (kcal <= 0) return FoodRole.VEGETABLE
        val proteinShare = food.proteinPer100g * 4 / kcal
        val carbShare = food.carbsPer100g * 4 / kcal
        val fatShare = food.fatPer100g * 9 / kcal
        return when {
            proteinShare >= 0.35 && food.proteinPer100g >= 8 -> FoodRole.PROTEIN
            fatShare >= 0.55 -> FoodRole.FAT
            carbShare >= 0.50 && food.carbsPer100g >= 10 -> FoodRole.CARB
            else -> FoodRole.VEGETABLE
        }
    }

    private fun roundTo5(value: Double): Double = (value / 5.0).roundToInt() * 5.0

    private companion object {
        const val MAX_CORRECTIONS = 5
        const val MEAL_TOLERANCE = 0.10
    }
}
