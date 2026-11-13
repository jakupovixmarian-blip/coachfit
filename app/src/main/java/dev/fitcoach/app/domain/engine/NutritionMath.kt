package dev.fitcoach.app.domain.engine

import dev.fitcoach.app.domain.model.Food
import kotlin.math.abs
import kotlin.math.round

/**
 * Pure nutrition arithmetic: per-100 g values scaled to the grams actually eaten.
 *
 * The app never trusts a total produced by the model. The model proposes which
 * food and how many grams; these functions compute the energy and the macros from
 * the values stored in the local database, so the same input always produces the
 * same output and the result can be audited.
 */
data class NutrientsPer100g(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null
)

data class NutrientTotals(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0
) {
    operator fun plus(other: NutrientTotals): NutrientTotals = NutrientTotals(
        kcal = kcal + other.kcal,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
        fiber = fiber + other.fiber
    )

    val macroEnergyKcal: Double
        get() = protein * NutritionMath.KCAL_PROTEIN +
            carbs * NutritionMath.KCAL_CARB +
            fat * NutritionMath.KCAL_FAT
}

object NutritionMath {

    const val KCAL_PROTEIN = 4.0
    const val KCAL_CARB = 4.0
    const val KCAL_FAT = 9.0

    fun nutrientsOf(food: Food): NutrientsPer100g = NutrientsPer100g(
        kcal = food.kcalPer100g,
        protein = food.proteinPer100g,
        carbs = food.carbsPer100g,
        fat = food.fatPer100g,
        fiber = food.fiberPer100g
    )

    /** Scales stored per-100 g values to [grams]. Non-positive weights yield zero. */
    fun forGrams(nutrients: NutrientsPer100g, grams: Double): NutrientTotals {
        if (grams <= 0.0) return NutrientTotals()
        val factor = grams / 100.0
        return NutrientTotals(
            kcal = nutrients.kcal * factor,
            protein = nutrients.protein * factor,
            carbs = nutrients.carbs * factor,
            fat = nutrients.fat * factor,
            fiber = (nutrients.fiber ?: 0.0) * factor
        )
    }

    fun foodTotals(food: Food, grams: Double): NutrientTotals = forGrams(nutrientsOf(food), grams)

    /** Sums a list of (food, grams) pairs, for example one meal or one day. */
    fun totals(items: List<Pair<Food, Double>>): NutrientTotals =
        items.fold(NutrientTotals()) { acc, (food, grams) -> acc + foodTotals(food, grams) }

    fun macroEnergyKcal(protein: Double, carbs: Double, fat: Double): Double =
        protein * KCAL_PROTEIN + carbs * KCAL_CARB + fat * KCAL_FAT

    /**
     * Distance between the declared energy and the energy of the macros, as a
     * percentage of the declared value. Null when there is no declared energy to
     * compare against.
     */
    fun energyDeviationPercent(
        declaredKcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double
    ): Double? {
        if (declaredKcal <= 0.0) return null
        val fromMacros = macroEnergyKcal(protein, carbs, fat)
        return abs(fromMacros - declaredKcal) / declaredKcal * 100.0
    }

    /** Rounds a portion to a weighable step (5 g by default). */
    fun roundGrams(grams: Double, step: Double = 5.0): Double =
        if (step <= 0.0) grams else round(grams / step) * step

    fun remainingKcal(target: Double, consumed: Double): Double = target - consumed

    fun sharePercent(part: Double, total: Double): Double? =
        if (total <= 0.0) null else part / total * 100.0

    /**
     * Energy density of the macro sum per 100 g. Used to detect records where the
     * declared calories and the macros cannot both be true.
     */
    fun macroEnergyPer100g(nutrients: NutrientsPer100g): Double =
        macroEnergyKcal(nutrients.protein, nutrients.carbs, nutrients.fat)
}
