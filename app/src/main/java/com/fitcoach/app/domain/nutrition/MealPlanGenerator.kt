package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbDayTarget
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MacroTotals
import kotlin.math.roundToInt

/**
 * Contratti e matematica dei pasti.
 *
 * Non esiste piu un generatore autonomo di piani: la struttura dei pasti e
 * decisa dal Coach AI (vedi submit_full_program) e solo VERIFICATA dai motori
 * matematici. Questo oggetto conserva i contratti di persistenza dei giorni
 * ([GeneratedDay]/[GeneratedMeal]) e le funzioni di calcolo riutilizzate dalla
 * validazione e dalla sostituzione alimentare.
 */
object MealPlanGenerator {

    data class GeneratedMeal(
        val slot: MealSlot,
        val entries: List<MealEntry>
    ) {
        val kcal: Double get() = entries.sumOf { it.kcal }
        val protein: Double get() = entries.sumOf { it.protein }
        val carbs: Double get() = entries.sumOf { it.carbs }
        val fat: Double get() = entries.sumOf { it.fat }
        val totals: MacroTotals get() = MacroTotals(kcal, protein, carbs, fat)
    }

    data class GeneratedDay(
        val dayIndex: Int,
        val dayType: CarbDayType,
        val target: CarbDayTarget,
        val meals: List<GeneratedMeal>,
        val actual: MacroTotals,
        val deviations: MacroTotals,
        val isCoherent: Boolean
    )

    /** Grammi necessari per raggiungere il macro bersaglio da un alimento. */
    fun gramsForMacro(targetGrams: Double, per100g: Double): Double {
        if (per100g <= 0.0) return 0.0
        return targetGrams / per100g * 100.0
    }

    /**
     * Calcola i grammi di un sostituto per mantenere lo stesso apporto del
     * macro dominante dell'alimento originale.
     */
    fun substituteGrams(
        original: FoodItem,
        originalGrams: Double,
        substitute: FoodItem
    ): Double {
        val dominant = dominantMacro(original)
        val originalMacroAmount = macroValue(original, dominant) * originalGrams / 100.0
        val per100 = macroValue(substitute, dominant)
        return if (per100 <= 0.0) originalGrams else (originalMacroAmount / per100 * 100.0)
    }

    enum class DominantMacro { PROTEIN, CARBS, FAT }

    fun dominantMacro(food: FoodItem): DominantMacro {
        val pKcal = food.proteinPer100g * 4
        val cKcal = food.carbsPer100g * 4
        val fKcal = food.fatPer100g * 9
        return when (maxOf(pKcal, cKcal, fKcal)) {
            pKcal -> DominantMacro.PROTEIN
            cKcal -> DominantMacro.CARBS
            else -> DominantMacro.FAT
        }
    }

    private fun macroValue(food: FoodItem, macro: DominantMacro): Double = when (macro) {
        DominantMacro.PROTEIN -> food.proteinPer100g
        DominantMacro.CARBS -> food.carbsPer100g
        DominantMacro.FAT -> food.fatPer100g
    }

    fun unitLabel(unit: FoodUnit): String = when (unit) {
        FoodUnit.RAW -> "crudo"
        FoodUnit.COOKED -> "cotto"
        FoodUnit.AS_SOLD -> "tal quale"
    }
}
