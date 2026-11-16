package com.fitcoach.app.ui.screens.diary

/**
 * Stato di presentazione del Diario alimentare.
 *
 * Solo dati già calcolati: i totali arrivano dal motore deterministico e dalle
 * entità DiaryEntry/MealEntry. Nessun import Compose, così resta verificabile
 * con test unitari puri.
 *
 * Mapping entità -> campo (dettaglio in android/README.md, sezione 7):
 *   kcalTarget / macroTarget  <- NutritionDay (giornata HIGH/MEDIUM/LOW attiva)
 *   meals                     <- Meal + MealEntry + FoodItem
 *   waterGlasses              <- DiaryEntry di tipo acqua / UserPreferences
 */
data class DiaryUiState(
    val dateLabel: String = "",
    val dayType: String = "MEDIUM",
    val kcalTarget: Int = 0,
    val kcalConsumed: Int = 0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val proteinTarget: Double = 0.0,
    val carbsTarget: Double = 0.0,
    val fatTarget: Double = 0.0,
    val meals: List<DiaryMealUi> = emptyList(),
    val waterGlasses: Int = 0,
    val waterTarget: Int = 8,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Calorie ancora disponibili nella giornata. */
    val kcalRemaining: Int get() = (kcalTarget - kcalConsumed).coerceAtLeast(0)

    /** Frazione consumata, usata dall'anello. */
    val kcalProgress: Float
        get() = if (kcalTarget > 0) {
            (kcalConsumed.toFloat() / kcalTarget.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val hasEntries: Boolean get() = meals.any { it.entries.isNotEmpty() }
}

/** Un pasto della giornata con le voci registrate. */
data class DiaryMealUi(
    val name: String,
    val timeLabel: String,
    val kcal: Int,
    val entries: List<DiaryEntryUi> = emptyList(),
)

/** Una voce del diario: alimento, quantità e valori nutrizionali reali. */
data class DiaryEntryUi(
    val id: Long,
    val foodName: String,
    val quantityLabel: String,
    val kcal: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
