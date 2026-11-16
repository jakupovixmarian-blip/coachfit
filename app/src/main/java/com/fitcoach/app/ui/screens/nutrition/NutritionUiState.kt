package com.fitcoach.app.ui.screens.nutrition

/**
 * Stato di presentazione della schermata Alimentazione.
 *
 * Nessuna logica di dominio qui: i valori arrivano già calcolati dal motore
 * deterministico (domain/) e dalle entità Room. Questo file non importa
 * Compose, così resta verificabile con test unitari puri.
 *
 * Mapping entità -> campo (dettaglio in android/README.md, sezione 7):
 *   dateLabel          <- NutritionDay.date
 *   carbDay            <- NutritionDay.type (HIGH / MEDIUM / LOW)
 *   targetKcal         <- NutritionDay.targetKcal
 *   targetProtein      <- NutritionDay.targetProtein
 *   targetCarbs        <- NutritionDay.targetCarbs
 *   targetFat          <- NutritionDay.targetFat
 *   consumedKcal       <- somma DiaryEntry.kcal del giorno
 *   consumedProtein   <- somma DiaryEntry.protein
 *   meals              <- Meal + MealEntry + FoodItem (grammi, crudo/cotto)
 *   week               <- NutritionDay dei 7 giorni del piano attivo
 *   weeklyBalanceLabel <- bilancio energetico settimanale (domain/)
 */
data class NutritionUiState(
    val dateLabel: String = "",
    val carbDay: CarbDayKind = CarbDayKind.MEDIUM,
    val targetKcal: Int = 0,
    val consumedKcal: Int = 0,
    val targetProtein: Int = 0,
    val consumedProtein: Int = 0,
    val targetCarbs: Int = 0,
    val consumedCarbs: Int = 0,
    val targetFat: Int = 0,
    val consumedFat: Int = 0,
    val meals: List<MealUi> = emptyList(),
    val week: List<NutritionDayUi> = emptyList(),
    val weeklyBalanceLabel: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Esiste un piano attivo con un target calorico valido. */
    val hasPlan: Boolean get() = targetKcal > 0

    val kcalRemaining: Int get() = (targetKcal - consumedKcal).coerceAtLeast(0)
    val kcalProgress: Float
        get() = if (targetKcal <= 0) 0f else (consumedKcal.toFloat() / targetKcal).coerceIn(0f, 1f)

    /** Sforamento reale: mostrato in rosso, mai nascosto. */
    val hasOvershoot: Boolean get() = consumedKcal > targetKcal
    val overshootKcal: Int get() = (consumedKcal - targetKcal).coerceAtLeast(0)

    val proteinRemaining: Int get() = (targetProtein - consumedProtein).coerceAtLeast(0)
    val carbsRemaining: Int get() = (targetCarbs - consumedCarbs).coerceAtLeast(0)
    val fatRemaining: Int get() = (targetFat - consumedFat).coerceAtLeast(0)

    val loggedMeals: Int get() = meals.count { it.isLogged }
    val totalMeals: Int get() = meals.size
}

/** Tipo di giornata del carb cycling. */
enum class CarbDayKind { HIGH, MEDIUM, LOW }

/** Un pasto del piano con i suoi alimenti. */
data class MealUi(
    val id: Long,
    val name: String,
    val timeLabel: String?,
    val kcal: Int,
    val items: List<MealItemUi> = emptyList(),
    val isLogged: Boolean = false,
)

/** Un alimento con grammi e macro reali da FoodItem. */
data class MealItemUi(
    val id: Long,
    val name: String,
    val quantityLabel: String,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

/** Giornata del piano settimanale mostrata nella striscia. */
data class NutritionDayUi(
    val dayLabel: String,
    val carbDay: CarbDayKind,
    val kcal: Int,
    val isToday: Boolean = false,
)
