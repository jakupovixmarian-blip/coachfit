package dev.fitcoach.app.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.repository.RoutineRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import dev.fitcoach.app.domain.usecase.CoachAgent
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramDayUi(
    val routine: Routine,
    val exercises: List<RoutineExercise>,
    val totalSets: Int
)

data class WorkoutHomeState(
    val program: List<ProgramDayUi> = emptyList(),
    val exercisesById: Map<Long, Exercise> = emptyMap(),
    val allExercises: List<Exercise> = emptyList(),
    val inProgressSession: WorkoutSession? = null,
    val recentSessions: List<WorkoutSession> = emptyList(),
    val suggestions: Map<Long, String> = emptyMap(),
    val message: String? = null,
    val generationWarnings: List<String> = emptyList(),
    val generating: Boolean = false
) {
    val dayCount: Int get() = program.size
    val isSingleRoutine: Boolean get() = program.size == 1
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val coachAgent: CoachAgent
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _warnings = MutableStateFlow<List<String>>(emptyList())
    private val reload = MutableStateFlow(0)
    private val _suggestions = MutableStateFlow<Map<Long, String>>(emptyMap())

    private val _state = MutableStateFlow(WorkoutHomeState())
    val state: StateFlow<WorkoutHomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch { reload.collect { refresh() } }
    }

    private suspend fun refresh() {
        val exercises = exerciseRepository.getAll()
        val exercisesById = exercises.associateBy { it.id }
        val routines = routineRepository.getActiveProgram()
            .ifEmpty { routineRepository.getActive()?.let { listOf(it) }.orEmpty() }

        val program = routines.map { routine ->
            val items = routineRepository.getExercises(routine.id)
            ProgramDayUi(routine = routine, exercises = items, totalSets = items.sumOf { it.targetSets })
        }
        val sessions = sessionRepository.getAll()

        val suggestions = mutableMapOf<Long, String>()
        program.flatMap { it.exercises }.forEach { re ->
            val history = sessionRepository.historyFor(re.exerciseId)
            suggestions[re.exerciseId] = ProgressionEngine.suggest(re, history).reason
        }

        _state.value = WorkoutHomeState(
            program = program,
            exercisesById = exercisesById,
            allExercises = exercises,
            inProgressSession = sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS },
            recentSessions = sessions.filter { it.status == SessionStatus.COMPLETED }.take(8),
            suggestions = suggestions,
            message = _message.value,
            generationWarnings = _warnings.value,
            generating = _state.value.generating
        )
    }

    fun dismissMessage() {
        _message.value = null
        _state.value = _state.value.copy(message = null)
    }

    fun dismissWarnings() {
        _warnings.value = emptyList()
        _state.value = _state.value.copy(generationWarnings = emptyList())
    }

    /**
     * Asks the agent to build the program from the profile analysis. The app does
     * not decide split, exercises, sets or rest: it only saves what the agent
     * proposed after checking it refers to real catalog entries.
     */
    fun generateProgram() {
        viewModelScope.launch {
            _state.value = _state.value.copy(generating = true)
            try {
                if (!coachAgent.isConfigured()) {
                    _message.value = "Configura un provider AI: la scheda viene generata dall'agente."
                    return@launch
                }
                if (profileRepository.getProfile() == null) {
                    _message.value = "Completa prima l'anamnesi con l'agente."
                    return@launch
                }
                val outcome = coachAgent.generateProgram()
                _warnings.value = emptyList()
                _message.value = buildString {
                    append("Programma \"${outcome.name}\": ${outcome.sessionCount} sedute, ")
                    append("${outcome.exerciseCount} esercizi, ${outcome.totalSets} serie totali.")
                }
                reload.value = reload.value + 1
            } catch (error: dev.fitcoach.app.core.result.AppError) {
                _message.value = error.message
            } catch (t: Throwable) {
                _message.value = t.message ?: "Errore durante la generazione"
            } finally {
                _state.value = _state.value.copy(generating = false)
            }
        }
    }

    fun startSession(routineId: Long? = null) {
        viewModelScope.launch {
            val existing = sessionRepository.getInProgress()
            if (existing != null) {
                _message.value = "Esiste gia una seduta in corso: riprendila o concludila."
                return@launch
            }
            val routine = routineId?.let { routineRepository.getById(it) }
                ?: routineRepository.getActiveProgram().firstOrNull()
                ?: routineRepository.getActive()
            val session = WorkoutSession(
                routineId = routine?.id,
                name = routine?.dayLabel?.ifBlank { routine.name } ?: "Seduta libera",
                dateEpochDay = DateUtils.today().toEpochDay(),
                startedAt = DateUtils.nowMillis(),
                status = SessionStatus.IN_PROGRESS
            )
            sessionRepository.createSession(session)
            reload.value = reload.value + 1
        }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session.id)
            reload.value = reload.value + 1
        }
    }
}
