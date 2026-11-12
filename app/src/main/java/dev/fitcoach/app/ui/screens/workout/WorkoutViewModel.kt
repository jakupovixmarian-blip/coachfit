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
import dev.fitcoach.app.domain.usecase.GeneratedProgram
import dev.fitcoach.app.domain.usecase.ProgramGenerator
import dev.fitcoach.app.domain.usecase.ProgramValidator
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
    val generationWarnings: List<String> = emptyList()
) {
    val dayCount: Int get() = program.size
    val isSingleRoutine: Boolean get() = program.size == 1
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository
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
            generationWarnings = _warnings.value
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
     * Builds a program from the onboarding data: exactly one session per declared
     * training day, each with its own split and its own volume budget, then plans
     * those sessions on distinct days of the coming week.
     */
    fun generateProgram() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            if (profile == null) {
                _message.value = "Completa prima l'anamnesi per generare il programma."
                return@launch
            }
            val catalog = exerciseRepository.getAll()
            if (catalog.isEmpty()) {
                _message.value = "Archivio esercizi vuoto: riapri l'app per completare il caricamento."
                return@launch
            }
            val generated: GeneratedProgram = ProgramGenerator.generate(profile, catalog)
            if (generated.days.isEmpty()) {
                _message.value = "Nessun esercizio compatibile con attrezzatura e limitazioni dichiarate."
                return@launch
            }
            val requested = (profile.availableDays ?: 3).coerceIn(1, 6)
            if (generated.days.size < requested) {
                _message.value =
                    "Generati ${generated.days.size} giorni su $requested: l'attrezzatura dichiarata non copre tutti i gruppi."
            }

            val programName = "Programma ${generated.days.size} giorni"
            val (routines, allExercises) = ProgramGenerator.toRoutines(
                programId = 0L,
                programName = programName,
                program = generated,
                createdAt = DateUtils.nowMillis()
            )
            val perRoutine = generated.days.map { day -> day.exercises.size }
            val exercisesByRoutine = mutableListOf<List<RoutineExercise>>()
            var cursor = 0
            perRoutine.forEach { count ->
                exercisesByRoutine += allExercises.subList(cursor, cursor + count)
                cursor += count
            }

            val ids = routineRepository.saveProgram(programName, routines, exercisesByRoutine)
            planWeek(ids, generated)

            val blocking = ProgramValidator.validate(generated.days).blockingIssues
            _warnings.value = generated.warnings
            _message.value = buildString {
                append("Programma creato: ${generated.days.size} sedute, ")
                append("${generated.exerciseCount} esercizi, ${generated.totalSets} serie totali.")
                if (blocking.isNotEmpty()) append(" Controlla gli avvisi.")
            }
            reload.value = reload.value + 1
        }
    }

    /**
     * Assigns every session of the program to a distinct day of the coming week,
     * so the calendar shows A, B, C and D instead of a single generic entry.
     */
    private suspend fun planWeek(routineIds: List<Long>, generated: GeneratedProgram) {
        val start = DateUtils.startOfWeek()
        val days = generated.days
        if (days.isEmpty()) return
        val spacing = (7 / days.size).coerceAtLeast(1)
        days.forEachIndexed { index, day ->
            val routineId = routineIds.getOrNull(index) ?: return@forEachIndexed
            val offset = (index * spacing).coerceAtMost(6)
            val date = start.plusDays(offset.toLong())
            sessionRepository.createSession(
                WorkoutSession(
                    routineId = routineId,
                    name = day.label,
                    dateEpochDay = date.toEpochDay(),
                    startedAt = DateUtils.nowMillis(),
                    status = SessionStatus.PLANNED
                )
            )
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
