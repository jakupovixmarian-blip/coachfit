package com.fitcoach.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.model.WorkoutPlan
import com.fitcoach.app.domain.model.WorkoutSession
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.domain.usecase.GenerateProgramsUseCase
import com.fitcoach.app.domain.workout.ProgressiveOverloadEngine
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Stato del form di registrazione di una serie. */
data class SetEntryForm(
    val planned: PlannedExercise,
    val setIndex: Int,
    val weightKg: String = "",
    val reps: String = "",
    val rir: String = "",
    val suggestion: ProgressiveOverloadEngine.ProgressionDecision? = null,
    val painReported: Boolean = false,
    val poorTechnique: Boolean = false
)

data class WorkoutUiState(
    val loading: Boolean = true,
    val plan: WorkoutPlan? = null,
    val today: WorkoutDay? = null,
    val session: WorkoutSession? = null,
    val message: String? = null,
    val generating: Boolean = false,
    val form: SetEntryForm? = null,
    val finishDialog: Boolean = false,
    val sleepQuality: Int = 3,
    val perceivedEffort: String = "7",
    val painNotes: String = "",
    // Cronometro di recupero.
    val restRemainingSeconds: Int = 0,
    val restTotalSeconds: Int = 0,
    val restReason: String = "",
    val resting: Boolean = false,
    // Sostituzione esercizio.
    val substituteFor: PlannedExercise? = null,
    val editingExercise: PlannedExercise? = null,
    val substituteOptions: List<com.fitcoach.app.domain.model.Exercise> = emptyList(),
    // Promemoria della sessione precedente per esercizio (exerciseId -> performance).
    val lastPerformances: Map<Long, ExercisePerformance> = emptyMap(),
    // Storico sessioni.
    val history: List<WorkoutSession> = emptyList(),
    val showHistory: Boolean = false,
    val showCalendar: Boolean = false,
    val calendarWeekStartEpochDay: Long = LocalDate.now().toEpochDay().let { it - Math.floorMod(it + 3, 7) },
    val calendarDays: List<WorkoutDay> = emptyList(),
    val showLibrary: Boolean = false,
    val libraryQuery: String = "",
    val libraryExercises: List<com.fitcoach.app.domain.model.Exercise> = emptyList(),
    // Sezione attiva: PROGRAMMA (scheda generata) o DIARIO (carichi e ripetizioni registrati).
    val section: WorkoutSection = WorkoutSection.PROGRAM,
    val performancesByExercise: Map<String, List<ExercisePerformance>> = emptyMap(),
    // Cronometro durata sessione.
    val sessionElapsedSeconds: Int = 0
)

/** Le due sotto-sezioni della schermata Allenamento. */
enum class WorkoutSection(val label: String) {
    PROGRAM("Programma"),
    LOG("Diario carichi")
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val generatePrograms: GenerateProgramsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    private var restJob: kotlinx.coroutines.Job? = null
    private var stopwatchJob: kotlinx.coroutines.Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val plan = workoutRepository.getActivePlan()
            val todayIndex = LocalDate.now().dayOfWeek.value - 1
            val today = plan?.days?.firstOrNull { it.dayIndex == todayIndex && !it.isRestDay }
            val session = workoutRepository.getSessionForDay(LocalDate.now().toEpochDay())
            val freeExercises = if (session?.workoutDayId == null && session != null) {
                workoutRepository.getFreeExercises(session.id)
            } else emptyList()
            val effectiveToday = if (session?.workoutDayId == null && session != null) {
                WorkoutDay(id = 0L, dayIndex = todayIndex, title = "Allenamento libero", exercises = freeExercises)
            } else today
            val lastPerformances = effectiveToday?.exercises
                ?.mapNotNull { pe ->
                    workoutRepository.getLatestPerformance(pe.exerciseId)?.let { pe.exerciseId to it }
                }
                ?.toMap()
                ?: emptyMap()
            val history = workoutRepository.getRecentSessions(30)
            val performancesByExercise = workoutRepository.getPerformanceByExercise()
            _state.value = _state.value.copy(
                loading = false,
                plan = plan,
                today = effectiveToday,
                session = session,
                lastPerformances = lastPerformances,
                history = history,
                performancesByExercise = performancesByExercise
            )
            syncStopwatch(session)
        }
    }

    /** Avvia/ferma il cronometro della durata sessione in base allo stato. */
    private fun syncStopwatch(session: WorkoutSession?) {
        stopwatchJob?.cancel()
        if (session?.status == SessionStatus.IN_PROGRESS && session.startedAt != null) {
            val start = session.startedAt
            stopwatchJob = viewModelScope.launch {
                while (true) {
                    val elapsed = ((System.currentTimeMillis() - start) / 1000).toInt()
                    _state.value = _state.value.copy(sessionElapsedSeconds = elapsed)
                    kotlinx.coroutines.delay(1000)
                }
            }
        } else {
            _state.value = _state.value.copy(sessionElapsedSeconds = 0)
        }
    }

    fun showHistory(show: Boolean) {
        _state.value = _state.value.copy(showHistory = show)
    }

    fun showCalendar(show: Boolean) {
        if (!show) {
            _state.value = _state.value.copy(showCalendar = false)
            return
        }
        loadCalendar(_state.value.calendarWeekStartEpochDay)
    }

    fun changeCalendarWeek(deltaWeeks: Long) {
        loadCalendar(_state.value.calendarWeekStartEpochDay + deltaWeeks * 7L)
    }

    private fun loadCalendar(weekStart: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                showCalendar = true,
                calendarWeekStartEpochDay = weekStart,
                calendarDays = workoutRepository.getActiveDays(weekStart).sortedBy { it.dayIndex }
            )
        }
    }

    fun moveWorkout(dayId: Long, newDayIndex: Int, permanent: Boolean) {
        viewModelScope.launch {
            val weekStart = _state.value.calendarWeekStartEpochDay
            if (workoutRepository.saveScheduleOverride(dayId, newDayIndex, weekStart, permanent)) {
                _state.value = _state.value.copy(message = if (permanent) {
                    "Programmazione futura aggiornata."
                } else {
                    "Solo la settimana corrente è stata aggiornata."
                })
                showCalendar(true)
                load()
            } else {
                _state.value = _state.value.copy(message = "Spostamento non disponibile: controlla i conflitti.")
            }
        }
    }

    fun addExtraSession(epochDay: Long) {
        viewModelScope.launch {
            if (workoutRepository.getSessionForDay(epochDay) != null) {
                _state.value = _state.value.copy(message = "Esiste già una sessione in questa data.")
                return@launch
            }
            workoutRepository.startFreeSession(epochDay)
            _state.value = _state.value.copy(message = "Seduta straordinaria salvata per ${LocalDate.ofEpochDay(epochDay)}.")
            if (epochDay == LocalDate.now().toEpochDay()) load()
        }
    }

    fun showLibrary(show: Boolean) {
        if (!show) {
            _state.value = _state.value.copy(showLibrary = false, libraryQuery = "")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                showLibrary = true,
                libraryExercises = exerciseRepository.getAll().sortedBy { it.name }
            )
        }
    }

    fun updateLibraryQuery(query: String) {
        _state.value = _state.value.copy(libraryQuery = query)
    }

    fun addExerciseToToday(exercise: com.fitcoach.app.domain.model.Exercise) {
        viewModelScope.launch {
            val day = _state.value.today
            if (day?.id == 0L && _state.value.session != null) {
                workoutRepository.addExerciseToFreeSession(_state.value.session!!.id, exercise)
                _state.value = _state.value.copy(message = "${exercise.name} aggiunto alla sessione libera.", showLibrary = false)
                load()
                return@launch
            }
            if (day == null) {
                _state.value = _state.value.copy(message = "Avvia prima una sessione libera o una seduta programmata.")
                return@launch
            }
            if (workoutRepository.addExerciseToDay(day.id, exercise)) {
                _state.value = _state.value.copy(message = "${exercise.name} aggiunto alla seduta.", showLibrary = false)
                load()
            }
        }
    }

    fun removeExercise(planned: PlannedExercise) {
        viewModelScope.launch {
            workoutRepository.removeExercise(planned.id)
            _state.value = _state.value.copy(message = "${planned.exerciseName} rimosso dalla seduta.")
            load()
        }
    }

    fun updateExerciseTemplate(
        planned: PlannedExercise,
        sets: Int,
        minReps: Int,
        maxReps: Int,
        restSeconds: Int,
        targetRir: Int,
        technique: com.fitcoach.app.domain.model.IntensityTechnique
    ) {
        viewModelScope.launch {
            if (workoutRepository.updateExerciseTemplate(planned.id, sets, minReps, maxReps, restSeconds, targetRir, technique)) {
                _state.value = _state.value.copy(message = "Parametri di ${planned.exerciseName} aggiornati.")
                load()
            } else {
                _state.value = _state.value.copy(message = "Parametri non validi: controlla serie, ripetizioni, recupero e RIR.")
            }
        }
    }

    fun openEdit(planned: PlannedExercise) { _state.value = _state.value.copy(editingExercise = planned) }

    fun selectSection(section: WorkoutSection) { _state.value = _state.value.copy(section = section) }
    fun closeEdit() { _state.value = _state.value.copy(editingExercise = null) }

    fun generateWorkout() {
        viewModelScope.launch {
            val profile = userRepository.get()
            if (profile == null || !profile.isComplete) {
                _state.value = _state.value.copy(message = "Completa prima il profilo.")
                return@launch
            }
            _state.value = _state.value.copy(generating = true, message = null)
            val result = generatePrograms(
                profile = profile,
                trainingDayIndices = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek),
                regenerateNutrition = false,
                regenerateWorkout = true,
                requireAnamnesis = false
            )
            val message = when (result) {
                is GenerateProgramsUseCase.Result.Success -> "Scheda generata."
                is GenerateProgramsUseCase.Result.MissingData -> "Dati mancanti: ${result.missing.joinToString()}."
                is GenerateProgramsUseCase.Result.AnamnesisNotConfirmed ->
                    "Completa l'anamnesi guidata prima di generare."
                is GenerateProgramsUseCase.Result.Error -> result.message
            }
            _state.value = _state.value.copy(generating = false, message = message)
            load()
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val day = _state.value.today ?: return@launch
            if (day.id == 0L) workoutRepository.startFreeSession(LocalDate.now().toEpochDay())
            else workoutRepository.startSession(day, LocalDate.now().toEpochDay())
            load()
        }
    }

    fun startFreeSession() {
        viewModelScope.launch {
            workoutRepository.startFreeSession(LocalDate.now().toEpochDay())
            load()
        }
    }

    /**
     * Apre il form serie per l'esercizio, avviando o riaprendo automaticamente la sessione
     * odierna se necessario: l'utente non deve premere un pulsante separato per iniziare.
     */
    fun logSetFor(planned: PlannedExercise) {
        viewModelScope.launch {
            if (!isSessionActive()) {
                val day = _state.value.today
                if (day == null) {
                    _state.value = _state.value.copy(message = "Nessuna seduta programmata per oggi.")
                    return@launch
                }
                if (day.id == 0L) workoutRepository.startFreeSession(LocalDate.now().toEpochDay())
                else workoutRepository.startSession(day, LocalDate.now().toEpochDay())
                load()
            }
            openSetForm(planned, sessionSetsFor(planned.exerciseId).size)
        }
    }

    fun openSetForm(planned: PlannedExercise, setIndex: Int) {
        viewModelScope.launch {
            val lastPerf = workoutRepository.getLatestPerformance(planned.exerciseId)
            val lastSets = lastPerf?.let { performance ->
                workoutRepository.getSessionById(performance.sessionId)?.sets
                    ?.filter { it.exerciseId == planned.exerciseId }
            }.orEmpty()
            val lastWeight = lastPerf?.topWeightKg ?: planned.targetWeightKg ?: 0.0
            val decision = if (lastPerf != null) {
                ProgressiveOverloadEngine.decide(
                    lastWeightKg = lastPerf.topWeightKg,
                    lastSets = lastSets.ifEmpty {
                        // ricostruzione minima dal riepilogo storico
                        List(lastPerf.totalSets) {
                            WorkoutSet(
                                exerciseId = planned.exerciseId,
                                exerciseName = planned.exerciseName,
                                setIndex = it,
                                plannedReps = planned.maxReps,
                                actualReps = lastPerf.topReps,
                                weightKg = lastPerf.topWeightKg,
                                rir = lastPerf.avgRir?.toInt()
                            )
                        }
                    },
                    minReps = planned.minReps,
                    maxReps = planned.maxReps,
                    category = planned.category
                )
            } else {
                ProgressiveOverloadEngine.decide(
                    lastWeightKg = lastWeight,
                    lastSets = emptyList(),
                    minReps = planned.minReps,
                    maxReps = planned.maxReps,
                    category = planned.category
                )
            }
            _state.value = _state.value.copy(
                form = SetEntryForm(
                    planned = planned,
                    setIndex = setIndex,
                    weightKg = decision.recommendedWeightKg.takeIf { it > 0 }?.let { "%.1f".format(it) } ?: "",
                    reps = decision.recommendedReps.toString(),
                    suggestion = decision
                )
            )
        }
    }

    fun updateForm(weight: String? = null, reps: String? = null, rir: String? = null,
                   pain: Boolean? = null, technique: Boolean? = null) {
        val form = _state.value.form ?: return
        _state.value = _state.value.copy(
            form = form.copy(
                weightKg = weight ?: form.weightKg,
                reps = reps ?: form.reps,
                rir = rir ?: form.rir,
                painReported = pain ?: form.painReported,
                poorTechnique = technique ?: form.poorTechnique
            )
        )
    }

    fun closeForm() {
        _state.value = _state.value.copy(form = null)
    }

    fun saveSet() {
        viewModelScope.launch {
            val form = _state.value.form ?: return@launch
            val session = _state.value.session
            if (session == null) {
                _state.value = _state.value.copy(message = "Avvia la sessione prima di registrare.")
                return@launch
            }
            val weight = form.weightKg.replace(',', '.').toDoubleOrNull() ?: 0.0
            val reps = form.reps.toIntOrNull() ?: form.planned.minReps
            val rir = form.rir.toIntOrNull()

            val savedId = workoutRepository.saveSet(
                sessionId = session.id,
                set = WorkoutSet(
                    exerciseId = form.planned.exerciseId,
                    exerciseName = form.planned.exerciseName,
                    setIndex = form.setIndex,
                    plannedReps = form.planned.maxReps,
                    actualReps = reps,
                    weightKg = weight,
                    rir = rir,
                    completed = true
                )
            )
            if (savedId == 0L) {
                _state.value = _state.value.copy(
                    message = "Serie non registrata: la sessione non è attiva o i dati non sono validi.",
                    form = null
                )
                return@launch
            }

            // Se l'utente segnala dolore/tecnica scorretta, salviamo la nota nella sessione.
            if (form.painReported) {
                _state.value = _state.value.copy(
                    message = "Serie registrata. Dolore segnalato: carico NON verra aumentato."
                )
            } else {
                _state.value = _state.value.copy(message = "Serie registrata.")
            }
            _state.value = _state.value.copy(form = null)
            // Avvia il cronometro di recupero consigliato per l'esercizio.
            startRest(form.planned.restSeconds, form.planned.restReason)
            load()
        }
    }

    /** Avvia il cronometro di recupero con la durata consigliata dal motore. */
    fun startRest(seconds: Int, reason: String) {
        if (seconds <= 0) return
        restJob?.cancel()
        _state.value = _state.value.copy(
            restRemainingSeconds = seconds,
            restTotalSeconds = seconds,
            restReason = reason,
            resting = true
        )
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining -= 1
                _state.value = _state.value.copy(restRemainingSeconds = remaining)
            }
            _state.value = _state.value.copy(resting = false, restRemainingSeconds = 0)
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _state.value = _state.value.copy(resting = false, restRemainingSeconds = 0)
    }

    fun addRestTime(seconds: Int) {
        if (!_state.value.resting) return
        val newRemaining = _state.value.restRemainingSeconds + seconds
        restJob?.cancel()
        _state.value = _state.value.copy(
            restRemainingSeconds = newRemaining,
            restTotalSeconds = maxOf(_state.value.restTotalSeconds, newRemaining)
        )
        restJob = viewModelScope.launch {
            var remaining = newRemaining
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining -= 1
                _state.value = _state.value.copy(restRemainingSeconds = remaining)
            }
            _state.value = _state.value.copy(resting = false, restRemainingSeconds = 0)
        }
    }

    fun openFinishDialog() {
        _state.value = _state.value.copy(finishDialog = true)
    }

    fun closeFinishDialog() {
        _state.value = _state.value.copy(finishDialog = false)
    }

    fun updateFinish(sleep: Int? = null, rpe: String? = null, painNotes: String? = null) {
        _state.value = _state.value.copy(
            sleepQuality = sleep ?: _state.value.sleepQuality,
            perceivedEffort = rpe ?: _state.value.perceivedEffort,
            painNotes = painNotes ?: _state.value.painNotes
        )
    }

    fun completeSession() {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            val duration = session.startedAt?.let { ((System.currentTimeMillis() - it) / 60000).toInt() }
            val completed = workoutRepository.completeSession(
                sessionId = session.id,
                durationMinutes = duration,
                perceivedEffort = _state.value.perceivedEffort.replace(',', '.').toDoubleOrNull(),
                sleepQuality = _state.value.sleepQuality,
                painReported = _state.value.painNotes.isNotBlank() ||
                    (_state.value.session?.painReported ?: false),
                painNotes = _state.value.painNotes
            )
            _state.value = _state.value.copy(
                finishDialog = !completed,
                message = if (completed) "Allenamento completato. Ottimo lavoro!"
                else "Sessione non completata: registra almeno una serie valida."
            )
            load()
        }
    }

    fun convertFreeSessionToRoutine() {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            val converted = workoutRepository.convertFreeSessionToRoutine(session.id)
            _state.value = _state.value.copy(
                message = if (converted) "Sessione convertita in una routine." else "Impossibile convertire: serve una giornata di riposo disponibile e almeno un esercizio."
            )
            if (converted) load()
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    /** Apre la scelta di un esercizio sostitutivo, calcolando le alternative. */
    fun openSubstitution(planned: PlannedExercise) {
        viewModelScope.launch {
            val all = exerciseRepository.getAll()
            val current = all.firstOrNull { it.id == planned.exerciseId }
            val profile = userRepository.get()
            val options = if (current != null && profile != null) {
                WorkoutPlanBuilder.findSubstitutes(current, all, profile)
            } else {
                emptyList()
            }
            _state.value = _state.value.copy(substituteFor = planned, substituteOptions = options)
        }
    }

    fun closeSubstitution() {
        _state.value = _state.value.copy(substituteFor = null, substituteOptions = emptyList())
    }

    /** Applica la sostituzione mantenendo lo stimolo previsto dall'esercizio originale. */
    fun applySubstitution(replacement: com.fitcoach.app.domain.model.Exercise) {
        viewModelScope.launch {
            val planned = _state.value.substituteFor ?: return@launch
            workoutRepository.substituteExercisePreservingStimulus(planned.id, replacement)
            _state.value = _state.value.copy(
                substituteFor = null,
                substituteOptions = emptyList(),
                message = "${planned.exerciseName} sostituito con ${replacement.name}."
            )
            load()
        }
    }

    fun sessionSetsFor(exerciseId: Long): List<WorkoutSet> =
        _state.value.session?.sets?.filter { it.exerciseId == exerciseId } ?: emptyList()

    fun isSessionActive(): Boolean = _state.value.session?.status == SessionStatus.IN_PROGRESS
}
