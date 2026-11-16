package com.fitcoach.app.ui.screens.dashboard

/* =============================================================================
 * FitCoach 2.0 — Dashboard · DashboardUiState
 * -----------------------------------------------------------------------------
 * Stato di PRESENTAZIONE della Dashboard. Non è un modello di dominio: è la
 * forma già pronta per il disegno, che il ViewModel esistente riempie leggendo
 * le entità Room. La UI quindi non conosce Room, e il restyling non tocca
 * data/domain.
 *
 * MAPPING — da dove viene ogni campo (nessun dato inventato):
 *
 *   greeting          <- UserProfile.nome + ora corrente (il ViewModel compone
 *                        "Buonasera, Marco"; la UI non calcola il saluto)
 *   weekdayLabel      <- giorno corrente, formattato (es. "Mercoledì 12")
 *   dayType           <- NutritionDay.tipo  (HIGH / MEDIUM / LOW del carb cycling)
 *   kcalTarget        <- NutritionDay.kcalTarget
 *   kcalConsumed      <- somma di DiaryEntry.kcal del giorno
 *   protein/carbs/fat <- NutritionDay (target) e somma DiaryEntry (consumati)
 *   meals             <- Meal previsti vs MealEntry registrati nel giorno
 *   hydrationLiters   <- registrazione idratazione del giorno (null = assente)
 *   workout           <- WorkoutDay + Mesocycle.settimana + PlannedExercise +
 *                        WorkoutSession del giorno; canSkip = esiste un
 *                        WorkoutScheduleOverride per oggi
 *   weight            <- ultimi WeightRecord (current = più recente,
 *                        delta = differenza col precedente, recentKg = serie
 *                        per lo sparkline, dal più vecchio al più recente)
 *   sleepHours        <- CheckIn.sonno dell'ultimo check-in
 *   weeklyVolumeKg    <- volume totale (kg) delle WorkoutSession degli ultimi 7 giorni
 *   lastCoachMessage  <- ultimo ChatMessage della conversazione corrente
 *   nextNotification  <- NotificationRecord programmata più vicina
 *   hasPlan           <- false dopo l'anamnesi ma senza NutritionPlan/WorkoutPlan
 *
 * Tutti i campi nullable significano "dato non registrato": la UI mostra uno
 * stato vuoto onesto, non uno zero che sembrerebbe un dato reale.
 * ========================================================================== */

/** Tipo di giornata alimentare del carb cycling. */
enum class CarbDayType {
    HIGH,
    MEDIUM,
    LOW,
}

/** Stato dell'allenamento di oggi, derivato dalla WorkoutSession del giorno. */
enum class WorkoutState {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
}

/** Pasti registrati sul totale previsto per oggi. */
data class DashboardMeals(
    val logged: Int = 0,
    val planned: Int = 0,
)

/**
 * Allenamento programmato per oggi.
 *
 * @param name nome del giorno/scheda (es. "Upper A")
 * @param focus gruppi muscolari del giorno (es. "Petto · Spalle · Tricipiti")
 * @param weekIndex settimana corrente del mesociclo, 1-based
 * @param weekTotal durata del mesociclo in settimane
 * @param exerciseCount numero di PlannedExercise del giorno
 * @param state stato della sessione di oggi
 * @param canSkip true solo se esiste una WorkoutScheduleOverride per oggi
 */
data class DashboardWorkout(
    val name: String,
    val focus: String,
    val weekIndex: Int,
    val weekTotal: Int,
    val exerciseCount: Int,
    val state: WorkoutState = WorkoutState.PLANNED,
    val canSkip: Boolean = false,
)

/**
 * Andamento del peso.
 *
 * @param currentKg ultimo WeightRecord
 * @param deltaKg differenza con il record precedente (può essere negativa)
 * @param recentKg serie recente per lo sparkline, dal più vecchio al più recente
 */
data class DashboardWeight(
    val currentKg: Double,
    val deltaKg: Double,
    val recentKg: List<Double> = emptyList(),
)

/** Prossima notifica programmata. */
data class DashboardNotification(
    val timeLabel: String,
    val title: String,
)

/**
 * Stato completo della Dashboard.
 *
 * Le property calcolate (kcalRemaining, proteinRemainingGrams, ...) possono
 * essere negative: un superamento del target è un dato reale e la UI lo colora
 * in ErrorRed invece di azzerarlo.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasPlan: Boolean = true,

    val greeting: String = "",
    val weekdayLabel: String = "",
    val dayType: CarbDayType = CarbDayType.MEDIUM,

    val kcalTarget: Int = 0,
    val kcalConsumed: Int = 0,

    val proteinTargetGrams: Double = 0.0,
    val proteinConsumedGrams: Double = 0.0,
    val carbsTargetGrams: Double = 0.0,
    val carbsConsumedGrams: Double = 0.0,
    val fatTargetGrams: Double = 0.0,
    val fatConsumedGrams: Double = 0.0,

    val meals: DashboardMeals = DashboardMeals(),
    val hydrationLiters: Double? = null,
    val workout: DashboardWorkout? = null,
    val weight: DashboardWeight? = null,
    val sleepHours: Double? = null,
    val weeklyVolumeKg: Double? = null,
    val lastCoachMessage: String? = null,
    val nextNotification: DashboardNotification? = null,
) {
    /** Calorie ancora disponibili oggi; negativo se il target è superato. */
    val kcalRemaining: Int get() = kcalTarget - kcalConsumed

    val proteinRemainingGrams: Double get() = proteinTargetGrams - proteinConsumedGrams
    val carbsRemainingGrams: Double get() = carbsTargetGrams - carbsConsumedGrams
    val fatRemainingGrams: Double get() = fatTargetGrams - fatConsumedGrams

    /** true se almeno un macro è oltre il target. */
    val hasMacroOvershoot: Boolean
        get() = proteinRemainingGrams < 0 || carbsRemainingGrams < 0 || fatRemainingGrams < 0
}
