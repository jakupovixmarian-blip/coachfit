package dev.fitcoach.app.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.repository.RoutineRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutHomeState(
    val routine: Routine? = null,
    val routineExercises: List<RoutineExercise> = emptyList(),
    val exercisesById: Map<Long, Exercise> = emptyMap(),
    val allExercises: List<Exercise> = emptyList(),
    val inProgressSession: WorkoutSession? = null,
    val recentSessions: List<WorkoutSession> = emptyList(),
    val suggestions: Map<Long, String> = emptyMap(),
    val message: String? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val reload = MutableStateFlow(0)
    private val _suggestions = MutableStateFlow<Map<Long, String>>(emptyMap())

    private val _state = MutableStateFlow(WorkoutHomeState())
    val state: StateFlow<WorkoutHomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(routineRepository.observeActive(), reload) { routine, _ -> routine },
                exerciseRepository.observeAll(),
                sessionRepository.observeSessions(),
                _message,
                _suggestions
            ) { routine, exercises, sessions, msg, suggestions ->
                WorkoutHomeState(
                    routine = routine,
                    exercisesById = exercises.associateBy { it.id },
                    allExercises = exercises,
                    inProgressSession = sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS },
                    recentSessions = sessions.filter { it.status == SessionStatus.COMPLETED }.take(8),
                    suggestions = suggestions,
                    message = msg
                )
            }.collect { partial ->
                val routineExercises = partial.routine?.let { routineRepository.getExercises(it.id) } ?: emptyList()
                _state.value = partial.copy(routineExercises = routineExercises)
            }
        }
        viewModelScope.launch {
            combine(routineRepository.observeActive(), reload) { routine, _ -> routine }.collect {
                refreshSuggestions()
            }
        }
    }

    fun dismissMessage() { _message.value = null }

    private suspend fun refreshSuggestions() {
        val routine = routineRepository.getActive() ?: run {
            _suggestions.value = emptyMap()
            return
        }
        val result = mutableMapOf<Long, String>()
        routineRepository.getExercises(routine.id).forEach { re ->
            val history = sessionRepository.historyFor(re.exerciseId)
            result[re.exerciseId] = ProgressionEngine.suggest(re, history).reason
        }
        _suggestions.value = result
    }

    /** Creates a routine from the profile: splits by available days, respects equipment and injuries. */
    fun generateRoutine() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            val days = (profile?.availableDays ?: 3).coerceIn(2, 6)
            val availableEquipment = profile?.equipment.orEmpty()
                .split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            val injuries = profile?.injuries.orEmpty().lowercase()
            val exercises = exerciseRepository.getAll().filter { ex ->
                !ex.isCustom
            }.filter { ex ->
                availableEquipment.isEmpty() || availableEquipment.contains(ex.equipment.name)
            }.filter { ex ->
                !isContraindicated(ex, injuries)
            }
            if (exercises.isEmpty()) {
                _message.value = "Nessun esercizio compatibile con attrezzatura e limitazioni dichiarate"
                return@launch
            }
            val split = buildSplit(days)
            val sessionMinutes = profile?.sessionMinutes ?: 60
            val level = profile?.experience ?: ExperienceLevel.BEGINNER
            val targetSets = when (level) {
                ExperienceLevel.BEGINNER -> 2
                ExperienceLevel.INTERMEDIATE -> 3
                ExperienceLevel.ADVANCED -> 4
            }
            val restSeconds = when (profile?.goal) {
                Goal.STRENGTH -> 180
                Goal.MUSCLE_GAIN -> 120
                Goal.FAT_LOSS -> 60
                else -> 90
            }
            val perSession = (sessionMinutes / 10).coerceIn(4, 8)

            val routineName = "Programma ${days} giorni"
            val routine = Routine(name = routineName, description = "Generato dai dati di anamnesi", isActive = false)
            val routineExercises = mutableListOf<RoutineExercise>()

            split.forEach { (dayIndex, muscleGroups) ->
                val dayExercises = pickForGroups(exercises, muscleGroups, perSession)
                dayExercises.forEachIndexed { index, ex ->
                    routineExercises += RoutineExercise(
                        routineId = 0L,
                        exerciseId = ex.id,
                        orderIndex = dayIndex * 100 + index,
                        targetSets = targetSets,
                        targetRepsMin = if (ex.pattern == MovementPattern.ISOLATION) 10 else 6,
                        targetRepsMax = if (ex.pattern == MovementPattern.ISOLATION) 15 else 10,
                        targetRir = if (level == ExperienceLevel.BEGINNER) 3 else 2,
                        restSeconds = restSeconds,
                        notes = "Giorno ${dayIndex + 1}"
                    )
                }
            }

            if (routineExercises.isEmpty()) {
                _message.value = "Non e stato possibile costruire la scheda con i dati attuali"
                return@launch
            }
            val id = routineRepository.saveRoutineWithExercises(routine, routineExercises)
            routineRepository.setActive(id)
            _message.value = "Programma creato: $days giorni, ${routineExercises.size} esercizi"
            reload.value = reload.value + 1
        }
    }

    private fun buildSplit(days: Int): Map<Int, List<MuscleGroup>> = when (days) {
        2 -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.CORE)
        )
        3 -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.CORE),
            2 to listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        )
        4 -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
            2 to listOf(MuscleGroup.QUADS, MuscleGroup.CALVES, MuscleGroup.CORE),
            3 to listOf(MuscleGroup.SHOULDERS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
        )
        5 -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
            2 to listOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
            3 to listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            4 to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
        )
        else -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
            2 to listOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
            3 to listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            4 to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            5 to listOf(MuscleGroup.CARDIO, MuscleGroup.CORE, MuscleGroup.FULL_BODY)
        )
    }

    private fun pickForGroups(
        exercises: List<Exercise>,
        groups: List<MuscleGroup>,
        limit: Int
    ): List<Exercise> {
        val picked = mutableListOf<Exercise>()
        val usedNames = mutableSetOf<String>()
        // Prefer one exercise per group first (balanced), then fill up.
        groups.forEach { group ->
            exercises.firstOrNull { it.primaryMuscle == group && usedNames.add(it.name) }?.let { picked += it }
        }
        exercises.filter { it.primaryMuscle in groups }
            .sortedBy { if (it.pattern == MovementPattern.ISOLATION) 1 else 0 }
            .forEach { ex ->
                if (picked.size < limit && usedNames.add(ex.name)) picked += ex
            }
        return picked.take(limit)
    }

    private fun isContraindicated(exercise: Exercise, injuries: String): Boolean {
        if (injuries.isBlank()) return false
        val rules = mapOf(
            "spalla" to setOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST),
            "ginocchio" to setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
            "schiena" to setOf(MuscleGroup.BACK),
            "lombare" to setOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            "gomito" to setOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            "caviglia" to setOf(MuscleGroup.CALVES)
        )
        return rules.any { (keyword, groups) ->
            injuries.contains(keyword) && (exercise.primaryMuscle in groups || exercise.secondaryMuscles.any { it in groups })
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val existing = sessionRepository.getInProgress()
            if (existing != null) {
                _message.value = "Esiste gia una seduta in corso"
                return@launch
            }
            val routine = routineRepository.getActive()
            val session = WorkoutSession(
                routineId = routine?.id,
                name = routine?.name ?: "Seduta libera",
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
