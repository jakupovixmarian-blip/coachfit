package com.fitcoach.app.ui.screens.workout

/* ============================================================================
 * Scheda personalizzata / dettaglio giorno — dati di sola presentazione.
 * Nessun calcolo qui: i valori arrivano da WorkoutDay e PlannedExercise.
 * ========================================================================== */

data class PlanMetaUi(
    val exercises: Int = 0,
    val totalSets: Int = 0,
    val volumeLabel: String = "",
    val durationLabel: String = "",
)

data class PlanExerciseUi(
    val index: Int = 0,
    val name: String = "",
    val primaryMuscle: String = "",
    val secondaryMuscles: String = "",
    val equipment: String = "",
    val prescription: String = "",
    val rirLabel: String = "",
    val restLabel: String = "",
    val loadLabel: String? = null,
    val description: String = "",
    val alternatives: List<String> = emptyList(),
    val history: List<Float> = emptyList(),
    val painNote: String? = null,
) {
    val hasHistory: Boolean get() = history.size >= 2

    val muscleLine: String
        get() = when {
            primaryMuscle.isBlank() && secondaryMuscles.isBlank() -> ""
            secondaryMuscles.isBlank() -> primaryMuscle
            primaryMuscle.isBlank() -> secondaryMuscles
            else -> "$primaryMuscle · $secondaryMuscles"
        }
}

data class CustomPlanUiState(
    val dayName: String = "",
    val weekLabel: String = "",
    val splitLabel: String = "",
    val meta: PlanMetaUi = PlanMetaUi(),
    val exercises: List<PlanExerciseUi> = emptyList(),
    val isRestDay: Boolean = false,
    val restHint: String = "",
    val mesocycleWarning: String? = null,
) {
    val isEmpty: Boolean get() = exercises.isEmpty() && !isRestDay
}
