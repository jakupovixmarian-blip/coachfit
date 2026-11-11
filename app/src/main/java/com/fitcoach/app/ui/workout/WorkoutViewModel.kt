package com.fitcoach.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
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
    val painNotes: String = ""
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val generatePrograms: GenerateProgramsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val plan = workoutRepository.getActivePlan()
            val todayIndex = LocalDate.now().dayOfWeek.value - 1
            val today = plan?.days?.firstOrNull { it.dayIndex == todayIndex && !it.isRestDay }
            val session = workoutRepository.getSessionForDay(LocalDate.now().toEpochDay())
            _state.value = _state.value.copy(
                loading = false,
                plan = plan,
                today = today,
                session = session
            )
        }
    }

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
                regenerateWorkout = true
            )
            val message = when (result) {
                is GenerateProgramsUseCase.Result.Success -> "Scheda generata."
                is GenerateProgramsUseCase.Result.MissingData -> "Dati mancanti: ${result.missing.joinToString()}."
                is GenerateProgramsUseCase.Result.Error -> result.message
            }
            _state.value = _state.value.copy(generating = false, message = message)
            load()
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val day = _state.value.today ?: return@launch
            workoutRepository.startSession(day, LocalDate.now().toEpochDay())
            load()
        }
    }

    fun openSetForm(planned: PlannedExercise, setIndex: Int) {
        viewModelScope.launch {
            val lastPerf = workoutRepository.getLatestPerformance(planned.exerciseId)
            val lastSets = _state.value.session?.sets
                ?.filter { it.exerciseId == planned.exerciseId }
                ?: emptyList()
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

            workoutRepository.saveSet(
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

            // Se l'utente segnala dolore/tecnica scorretta, salviamo la nota nella sessione.
            if (form.painReported) {
                _state.value = _state.value.copy(
                    message = "Serie registrata. Dolore segnalato: carico NON verra aumentato."
                )
            } else {
                _state.value = _state.value.copy(message = "Serie registrata.")
            }
            _state.value = _state.value.copy(form = null)
            load()
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
            workoutRepository.completeSession(
                sessionId = session.id,
                durationMinutes = duration,
                perceivedEffort = _state.value.perceivedEffort.replace(',', '.').toDoubleOrNull(),
                sleepQuality = _state.value.sleepQuality,
                painReported = _state.value.painNotes.isNotBlank() ||
                    (_state.value.session?.painReported ?: false),
                painNotes = _state.value.painNotes
            )
            _state.value = _state.value.copy(
                finishDialog = false,
                message = "Allenamento completato. Ottimo lavoro!"
            )
            load()
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun sessionSetsFor(exerciseId: Long): List<WorkoutSet> =
        _state.value.session?.sets?.filter { it.exerciseId == exerciseId } ?: emptyList()

    fun isSessionActive(): Boolean = _state.value.session?.status == SessionStatus.IN_PROGRESS
}
