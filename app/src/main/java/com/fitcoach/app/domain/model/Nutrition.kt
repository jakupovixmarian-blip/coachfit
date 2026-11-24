package com.fitcoach.app.domain.model

/** Voce del database alimentare con valori nutrizionali per 100 g (fonte dichiarata). */
data class FoodItem(
    val id: Long = 0,
    val name: String,
    val category: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double = 0.0,
    val defaultUnit: FoodUnit = FoodUnit.RAW,
    val source: String,
    val isCustom: Boolean = false,
    val tags: List<String> = emptyList(),
    /** Sostituti suggeriti per nome (mappati a FoodItem a runtime). */
    val substituteNames: List<String> = emptyList(),
    /** Identificativo nella fonte esterna (es. codice Open Food Facts, fdcId USDA). */
    val sourceId: String? = null,
    /** Marca commerciale, se presente (prodotti Open Food Facts). */
    val brand: String? = null,
    /**
     * Prezzo indicativo in EUR per kg. Puo essere null quando non disponibile:
     * in quel caso il costo non viene inventato ma segnalato come non stimabile.
     */
    val pricePerKgEur: Double? = null
) {
    /** Nome normalizzato per il riconoscimento duplicati (minuscolo, senza accenti/spazi doppi). */
    val normalizedName: String get() = normalizeName(name)

    companion object {
        fun normalizeName(raw: String): String = raw.lowercase()
            .replace(Regex("[àáâãä]"), "a").replace(Regex("[èéêë]"), "e")
            .replace(Regex("[ìíîï]"), "i").replace(Regex("[òóôõö]"), "o")
            .replace(Regex("[ùúûü]"), "u")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }
}


/** Alimento quantificato all'interno di un pasto. */
data class MealEntry(
    val id: Long = 0,
    val mealId: Long = 0,
    val foodItemId: Long,
    val foodName: String,
    val grams: Double,
    val unit: FoodUnit,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class Meal(
    val id: Long = 0,
    val nutritionDayId: Long = 0,
    val slot: MealSlot,
    val time: String,
    val entries: List<MealEntry> = emptyList()
) {
    val kcal: Double get() = entries.sumOf { it.kcal }
    val protein: Double get() = entries.sumOf { it.protein }
    val carbs: Double get() = entries.sumOf { it.carbs }
    val fat: Double get() = entries.sumOf { it.fat }
}

data class NutritionDay(
    val id: Long = 0,
    val planId: Long = 0,
    val dayIndex: Int, // 0..6
    val dayType: CarbDayType,
    val targetKcal: Double,
    val targetProtein: Double,
    val targetCarbs: Double,
    val targetFat: Double,
    val meals: List<Meal> = emptyList(),
    val workoutDayIndex: Int? = null
) {
    val actualKcal: Double get() = meals.sumOf { it.kcal }
    val actualProtein: Double get() = meals.sumOf { it.protein }
    val actualCarbs: Double get() = meals.sumOf { it.carbs }
    val actualFat: Double get() = meals.sumOf { it.fat }
}

data class NutritionPlan(
    val id: Long = 0,
    val profileId: Long = 1L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val bmr: Double,
    val tdee: Double,
    val weeklyCalories: Double,
    val weeklyKcalBalance: Double,
    val bmrFormula: String,
    val tdeeFormula: String,
    val highCarbDays: Int,
    val mediumCarbDays: Int,
    val lowCarbDays: Int,
    val proteinGPerKg: Double,
    val carbCyclingApplied: Boolean = true,
    val version: Int = 1,
    val days: List<NutritionDay> = emptyList()
)

/** Risultato dei calcoli energetici (valori stimati, non misurati). */
data class EnergyResult(
    val bmr: Double,
    val tdee: Double,
    val bmrFormula: String,
    val tdeeFormula: String,
    val leanMassKg: Double?,
    val weeklyCalories: Double,
    /**
     * PAL applicato. Il TDEE non basta a spiegare la stima: senza il fattore,
     * chi legge non puo' distinguere "BMR alto" da "fattore alto" e finisce per
     * attribuire il numero alla causa sbagliata.
     */
    val activityFactor: Double = 0.0,
    val notes: List<String> = emptyList()
)

/** Target per una tipologia di giornata. */
data class CarbDayTarget(
    val type: CarbDayType,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class CarbCyclingPlan(
    val targets: Map<CarbDayType, CarbDayTarget>,
    val highDays: Int,
    val mediumDays: Int,
    val lowDays: Int,
    val weeklyKcal: Double,
    val weeklyKcalFromTarget: Double,
    val weeklyProtein: Double,
    val weeklyCarbs: Double,
    val weeklyFat: Double,
    val warnings: List<String> = emptyList(),
    val isValid: Boolean = true,
    /** True se il carb cycling e stato applicato, false se si usa distribuzione uniforme. */
    val applied: Boolean = true
)

data class MacroTotals(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
) {
    operator fun plus(other: MacroTotals) = MacroTotals(
        kcal = kcal + other.kcal,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat
    )

    companion object {
        val ZERO = MacroTotals(0.0, 0.0, 0.0, 0.0)

        fun of(entries: List<MealEntry>) = MacroTotals(
            kcal = entries.sumOf { it.kcal },
            protein = entries.sumOf { it.protein },
            carbs = entries.sumOf { it.carbs },
            fat = entries.sumOf { it.fat }
        )
    }
}
