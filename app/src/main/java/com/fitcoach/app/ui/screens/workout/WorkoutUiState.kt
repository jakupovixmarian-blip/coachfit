package com.fitcoach.app.ui.screens.workout

/**
 * Stato di presentazione di Allenamento.
 *
 * Solo dati già calcolati: nessun accesso a ViewModel, repository o Room.
 * Mapping entità -> campo (dettaglio in android/README.md, sezione 8):
 *   planName          <- WorkoutPlan.name
 *   mesocycleLabel    <- Mesocycle.name + settimana corrente / durata
 *   week              <- WorkoutDay dei 7 giorni (WorkoutScheduleOverride incluso)
 *   day.exercises     <- PlannedExercise ordinati per `order`
 *   day.exercises[].lastPerformance <- ExercisePerformance più recente
 *   volumeLabel       <- volume settimanale per gruppo (domain/)
 *
 * Nota: la progressione dei carichi (Double Progression) NON è calcolata qui.
 * `targetLoadLabel` e `suggestion` arrivano già decisi dal motore.
 */
data class WorkoutUiState(
    val planName: String = "",
    val mesocycleLabel: String = "",
    val focusLabel: String = "",
    val week: List<TrainingDayUi> = emptyList(),
    val selectedDayIndex: Int = 0,
    val day: WorkoutDayUi? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasPlan: Boolean get() = planName.isNotBlank() && week.isNotEmpty()
    val completedDays: Int get() = week.count { it.status == DayStatus.COMPLETED }
    val plannedDays: Int get() = week.count { it.status != DayStatus.REST }
}

/** Giornata nella striscia settimanale. */
data class TrainingDayUi(
    val dayLabel: String,
    val dateLabel: String,
    val name: String,
    val status: DayStatus,
    val isSelected: Boolean = false,
)

enum class DayStatus { COMPLETED, TODAY, SCHEDULED, REST }

/** Scheda della giornata selezionata. */
data class WorkoutDayUi(
    val dayId: Long,
    val name: String,
    val focus: String,
    val estimatedMinutes: Int,
    val volumeLabel: String,
    val exercises: List<PlannedExerciseUi> = emptyList(),
    val note: String? = null,
) {
    val exerciseCount: Int get() = exercises.size
    val totalSets: Int get() = exercises.sumOf { it.sets }
}

/** Esercizio programmato con i parametri reali della scheda. */
data class PlannedExerciseUi(
    val id: Long,
    val order: Int,
    val name: String,
    val muscleGroup: String,
    val equipment: String?,
    val sets: Int,
    val repsLabel: String,
    val rirLabel: String,
    val restLabel: String,
    val targetLoadLabel: String?,
    val lastPerformanceLabel: String?,
    val hasAlternatives: Boolean = false,
)

/* ---------------------------------------------------------------------------
 * Sessione in corso (schermata durante l'allenamento)
 * ------------------------------------------------------------------------ */

/**
 * Stato della sessione attiva. Mappato su WorkoutSession + WorkoutSet.
 * `restSecondsLeft` è il timer di recupero: la schermata mostra solo il valore,
 * il conto alla rovescia vive nel ViewModel.
 */
data class SessionUiState(
    val sessionId: Long = 0L,
    val dayName: String = "",
    val exerciseIndex: Int = 0,
    val exercises: List<SessionExerciseUi> = emptyList(),
    val setNumber: Int = 1,
    val weightInput: String = "",
    val repsInput: String = "",
    val rirInput: String = "",
    val restSecondsLeft: Int? = null,
    val isResting: Boolean = false,
    val isPainReported: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val currentExercise: SessionExerciseUi? get() = exercises.getOrNull(exerciseIndex)
    val completedSets: Int get() = exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val progress: Float
        get() = if (totalSets <= 0) 0f else (completedSets.toFloat() / totalSets).coerceIn(0f, 1f)
    val isLastExercise: Boolean get() = exerciseIndex >= exercises.lastIndex
    val canLogSet: Boolean get() = weightInput.isNotBlank() && repsInput.isNotBlank()
}

/** Esercizio nella sessione, con le serie già registrate. */
data class SessionExerciseUi(
    val plannedExerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val targetRepsLabel: String,
    val targetRirLabel: String,
    val restSeconds: Int,
    val suggestedLoadLabel: String?,
    val sets: List<SetLogUi> = emptyList(),
)

/** Singola serie registrata: carico, ripetizioni, RIR reali. */
data class SetLogUi(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int?,
    val isCompleted: Boolean = true,
)
