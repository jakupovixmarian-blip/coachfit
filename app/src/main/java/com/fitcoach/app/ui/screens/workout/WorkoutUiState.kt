package com.fitcoach.app.ui.screens.workout

/* =============================================================================
 * FitCoach 2.0 — Allenamento · WorkoutUiState
 * -----------------------------------------------------------------------------
 * Stato di PRESENTAZIONE. Nessun calcolo: carichi, RIR, volume e progressione
 * sono già decisi dal motore (Double Progression) e dal CoachOrchestrator.
 * Qui c'è solo la forma pronta per il disegno.
 *
 * MAPPING — da dove viene ogni campo (nessun dato inventato):
 *
 *   planName          <- WorkoutPlan.nome
 *   splitLabel        <- WorkoutPlan.split (Upper/Lower, PPL, Full Body, custom)
 *   mesocycleLabel    <- Mesocycle.nome + settimana corrente
 *   weekIndex/weekTotal <- Mesocycle.settimana / Mesocycle.durataSettimane
 *   week              <- WorkoutDay dei 7 giorni + WorkoutSession (stato)
 *   day               <- WorkoutDay selezionato + PlannedExercise + Exercise
 *   volumeByMuscle    <- somma serie per gruppo muscolare nella settimana
 *   calendar          <- WorkoutDay + WorkoutScheduleOverride della settimana
 *   library           <- Exercise filtrati per nome/gruppo muscolare
 *   history           <- WorkoutSession concluse, dalla più recente
 *   loadLog           <- WorkoutSet registrate nell'ultima sessione
 *   session           <- WorkoutSession in corso + WorkoutSet correnti
 *   sleepHours        <- CheckIn.sonno dell'ultimo check-in
 *   painNote          <- segnalazione dolore dell'utente (WorkoutSet.painFlag)
 *
 * Tutti i campi nullable significano "dato non registrato": la UI mostra uno
 * stato vuoto onesto, mai uno zero che sembrerebbe un dato reale.
 * ========================================================================== */

/** Stato di un giorno nella settimana di allenamento. */
enum class DayStatus {
    COMPLETED,
    TODAY,
    SCHEDULED,
    REST,
}

/** Dialog aperti dalla schermata Allenamento. Uno solo alla volta. */
enum class WorkoutDialog {
    NONE,
    SET_ENTRY,
    EXERCISE_EDITOR,
    EXERCISE_LIBRARY,
    SUBSTITUTION,
    SESSION_HISTORY,
    CALENDAR,
    FINISH,
}

/** Giorno della settimana nella barra orizzontale. */
data class TrainingDayUi(
    val dayLabel: String = "",
    val dateLabel: String = "",
    val status: DayStatus = DayStatus.SCHEDULED,
    val planName: String = "",
    val isExtra: Boolean = false,
    val overrideLabel: String? = null,
)

/** Esercizio programmato nella scheda del giorno. */
data class PlannedExerciseUi(
    val id: Long = 0L,
    val order: Int = 0,
    val name: String = "",
    val muscleGroup: String = "",
    val equipment: String = "",
    val sets: Int = 0,
    val repsLabel: String = "",
    val rirLabel: String = "",
    val restLabel: String = "",
    val targetLoadLabel: String? = null,
    val lastPerformanceLabel: String? = null,
    val hasAlternatives: Boolean = false,
    val techniqueNote: String? = null,
    val painFlagged: Boolean = false,
    val description: String = "",
)

/** Scheda del giorno selezionato. */
data class WorkoutDayUi(
    val name: String = "",
    val focus: String = "",
    val exerciseCount: Int = 0,
    val totalSets: Int = 0,
    val estimatedMinutes: Int = 0,
    val volumeLabel: String = "",
    val exercises: List<PlannedExerciseUi> = emptyList(),
    val note: String? = null,
    val isRestDay: Boolean = false,
)

/** Volume settimanale di un gruppo muscolare. */
data class MuscleVolumeUi(
    val muscle: String = "",
    val sets: Int = 0,
    val targetSets: Int = 0,
    val deltaLabel: String = "",
)

/** Giorno del calendario allenamenti (settimana corrente, con override). */
data class CalendarDayUi(
    val weekdayLabel: String = "",
    val dateLabel: String = "",
    val status: DayStatus = DayStatus.SCHEDULED,
    val planName: String = "",
    val isOverride: Boolean = false,
    val overrideNote: String? = null,
)

/** Esercizio nella libreria, con ricerca per nome o gruppo muscolare. */
data class ExerciseLibraryItemUi(
    val name: String = "",
    val muscle: String = "",
    val equipment: String = "",
    val inTodaySession: Boolean = false,
)

/** Alternativa proposta per un esercizio (stesso gruppo, attrezzatura simile). */
data class ExerciseAlternativeUi(
    val name: String = "",
    val muscle: String = "",
    val equipment: String = "",
    val prescription: String = "",
    val note: String? = null,
)

/** Sessione conclusa nello storico. */
data class SessionHistoryUi(
    val id: Long = 0L,
    val dateLabel: String = "",
    val dayName: String = "",
    val sets: Int = 0,
    val volumeLabel: String = "",
    val durationLabel: String = "",
    val painFlagged: Boolean = false,
)

/** Serie registrata nella sessione in corso. */
data class SetLogUi(
    val setNumber: Int = 1,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val rir: Int? = null,
    val rpe: Int? = null,
    val isCompleted: Boolean = false,
    val isPainFlagged: Boolean = false,
)

/** Esercizio della sessione in corso. */
data class SessionExerciseUi(
    val id: Long = 0L,
    val name: String = "",
    val muscleGroup: String = "",
    val targetRepsLabel: String = "",
    val targetRirLabel: String = "",
    val techniqueNote: String? = null,
    val sets: List<SetLogUi> = emptyList(),
)

/**
 * Sessione in corso.
 *
 * @param progress 0f..1f, serie completate sul totale della seduta
 * @param isResting true mentre il timer di recupero è attivo
 * @param restSecondsLeft secondi mancanti; null se non c'è recupero in corso
 * @param canLogSet true solo se carico e ripetizioni sono validi
 * @param sleepHours CheckIn.sonno dell'ultimo check-in, se registrato
 */
data class SessionUiState(
    val dayName: String = "",
    val completedSets: Int = 0,
    val totalSets: Int = 0,
    val setNumber: Int = 1,
    val progress: Float = 0f,
    val isResting: Boolean = false,
    val restSecondsLeft: Int? = null,
    val restDefaultSeconds: Int = 120,
    val currentExercise: SessionExerciseUi? = null,
    val weightInput: String = "",
    val repsInput: String = "",
    val rirInput: String = "",
    val rpeInput: String = "",
    val canLogSet: Boolean = false,
    val isSaving: Boolean = false,
    val isPainReported: Boolean = false,
    val isLastExercise: Boolean = false,
    val sleepHours: Double? = null,
    val errorMessage: String? = null,
)

/** Dati del dialog di registrazione serie. */
data class SetEntryDraftUi(
    val exerciseName: String = "",
    val setNumber: Int = 1,
    val weightInput: String = "",
    val repsInput: String = "",
    val rirInput: String = "",
    val rpeInput: String = "",
    val restInput: String = "",
    val targetLabel: String = "",
    val painFlagged: Boolean = false,
    val errorMessage: String? = null,
)

/** Bozza del dialog di modifica esercizio. */
data class ExerciseEditorDraftUi(
    val exerciseId: Long = 0L,
    val name: String = "",
    val setsInput: String = "",
    val repsInput: String = "",
    val rirInput: String = "",
    val restInput: String = "",
    val techniqueNote: String = "",
    val isNew: Boolean = false,
    val errorMessage: String? = null,
)

/** Stato completo della schermata Allenamento. */
data class WorkoutUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasPlan: Boolean = true,

    val planName: String = "",
    val splitLabel: String = "",
    val mesocycleLabel: String = "",
    val weekIndex: Int = 1,
    val weekTotal: Int = 1,

    val week: List<TrainingDayUi> = emptyList(),
    val selectedDayIndex: Int = 0,
    val completedDays: Int = 0,
    val plannedDays: Int = 0,

    val day: WorkoutDayUi? = null,
    val volumeByMuscle: List<MuscleVolumeUi> = emptyList(),
    val calendar: List<CalendarDayUi> = emptyList(),
    val library: List<ExerciseLibraryItemUi> = emptyList(),
    val libraryQuery: String = "",
    val history: List<SessionHistoryUi> = emptyList(),
    val loadLog: List<SetLogUi> = emptyList(),

    val session: SessionUiState? = null,
    val isSessionActive: Boolean = false,
    val sleepHours: Double? = null,
    val painNote: String? = null,

    val dialog: WorkoutDialog = WorkoutDialog.NONE,
    val setDraft: SetEntryDraftUi = SetEntryDraftUi(),
    val editorDraft: ExerciseEditorDraftUi = ExerciseEditorDraftUi(),
    val alternatives: List<ExerciseAlternativeUi> = emptyList(),
    val selectedAlternative: String? = null,

    val warnings: List<String> = emptyList(),
) {
    /** true se il giorno selezionato è un giorno di riposo. */
    val isRestDay: Boolean get() = day?.isRestDay == true

    /** Percentuale della settimana completata, 0f..1f. */
    val weekProgress: Float
        get() = if (plannedDays <= 0) 0f else (completedDays.toFloat() / plannedDays).coerceIn(0f, 1f)

    val canGoToPreviousWeek: Boolean get() = weekIndex > 1
    val canGoToNextWeek: Boolean get() = weekIndex < weekTotal
}
