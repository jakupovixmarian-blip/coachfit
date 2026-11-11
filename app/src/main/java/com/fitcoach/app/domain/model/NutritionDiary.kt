package com.fitcoach.app.domain.model

/** Origine di una voce del diario: alimento del piano oppure extra fuori piano. */
enum class DiarySource { PLAN, EXTRA }

/** Voce del diario nutrizionale: cosa l'utente ha realmente consumato. */
data class DiaryEntry(
    val id: Long = 0,
    val dateEpochDay: Long,
    val mealSlot: MealSlot,
    val time: String = "",
    val foodItemId: Long,
    val foodName: String,
    val grams: Double,
    val unit: FoodUnit = FoodUnit.RAW,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val source: DiarySource = DiarySource.EXTRA,
    /** Voce del piano collegata (null per gli extra). Evita il doppio conteggio. */
    val planEntryId: Long? = null,
    /** False se la voce e stata deselezionata: resta nello storico ma non conta. */
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val macros: MacroTotals get() = MacroTotals(kcal, protein, carbs, fat)
}

/** Categoria merceologica per la lista della spesa. */
enum class ShoppingCategory(val label: String) {
    MEAT_FISH("Carne e pesce"),
    EGGS_DAIRY("Uova e latticini"),
    GRAINS("Cereali e derivati"),
    FRUIT("Frutta"),
    VEGETABLES("Verdura"),
    LEGUMES("Legumi"),
    NUTS_SEEDS("Frutta secca e semi"),
    CONDIMENTS_OILS("Condimenti e oli"),
    OTHER("Altri alimenti");

    companion object {
        /** Mappa una categoria del database alimentare nella categoria di spesa. */
        fun fromFoodCategory(category: String): ShoppingCategory = when (category) {
            "Carne", "Pesce", "Salumi" -> MEAT_FISH
            "Uova", "Latticini" -> EGGS_DAIRY
            "Cereali", "Tuberi" -> GRAINS
            "Frutta" -> FRUIT
            "Verdura" -> VEGETABLES
            "Legumi" -> LEGUMES
            "Frutta secca" -> NUTS_SEEDS
            "Grassi" -> CONDIMENTS_OILS
            else -> OTHER
        }
    }
}

/** Voce della lista della spesa, derivata dal piano alimentare effettivo. */
data class ShoppingItem(
    val id: Long = 0,
    val weekStartEpochDay: Long,
    val foodItemId: Long,
    val foodName: String,
    val category: ShoppingCategory,
    val totalGrams: Double,
    val unit: FoodUnit = FoodUnit.RAW,
    val checked: Boolean = false,
    /** Gia presente in casa: non va acquistato. */
    val alreadyHave: Boolean = false,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Registro immutabile di una sostituzione applicata al piano. */
data class SubstitutionLog(
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val dateEpochDay: Long,
    val mealEntryId: Long,
    val mealSlot: MealSlot,
    val originalFoodId: Long,
    val originalName: String,
    val originalGrams: Double,
    val newFoodId: Long,
    val newName: String,
    val newGrams: Double,
    val kcalBefore: Double,
    val kcalAfter: Double,
    val proteinBefore: Double,
    val proteinAfter: Double,
    val carbsBefore: Double,
    val carbsAfter: Double,
    val fatBefore: Double,
    val fatAfter: Double,
    val status: String,
    val note: String = ""
)

/** Bilancio nutrizionale di una giornata: obiettivo, consumato e rimanente. */
data class DailyBalance(
    val dateEpochDay: Long,
    val targetKcal: Double,
    val targetProtein: Double,
    val targetCarbs: Double,
    val targetFat: Double,
    val consumedKcal: Double,
    val consumedProtein: Double,
    val consumedCarbs: Double,
    val consumedFat: Double
) {
    val remainingKcal: Double get() = targetKcal - consumedKcal
    val remainingProtein: Double get() = targetProtein - consumedProtein
    val remainingCarbs: Double get() = targetCarbs - consumedCarbs
    val remainingFat: Double get() = targetFat - consumedFat

    val kcalExceeded: Double get() = (consumedKcal - targetKcal).coerceAtLeast(0.0)
    val proteinExceeded: Double get() = (consumedProtein - targetProtein).coerceAtLeast(0.0)
    val carbsExceeded: Double get() = (consumedCarbs - targetCarbs).coerceAtLeast(0.0)
    val fatExceeded: Double get() = (consumedFat - targetFat).coerceAtLeast(0.0)

    val kcalWithinTarget: Boolean get() = consumedKcal <= targetKcal
}

/** Riepilogo settimanale basato sui consumi reali. */
data class WeeklyNutritionSummary(
    val daysTracked: Int,
    val avgKcal: Double,
    val avgProtein: Double,
    val avgCarbs: Double,
    val avgFat: Double,
    val avgPlannedKcal: Double,
    val avgAdherencePercent: Double,
    val daysOnTarget: Int,
    val extraEntries: Int,
    val kcalDifference: Double
)
