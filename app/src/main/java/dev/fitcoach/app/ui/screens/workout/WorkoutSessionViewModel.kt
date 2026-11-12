package dev.fitcoach.app.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExerciseHistoryEntry
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.SetType
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.RoutineRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionExerciseBlock(
    val routineExercise: RoutineExercise?,
    val exercise: Exercise,
    val sets: List<LoggedSet>,
    val lastPerformance: ExerciseHistoryEntry?,
    val suggestionReason: String?,
    val suggestedWeightKg: Double?,
    val suggestedReps: Int?
) {
    val volumeKg: Double get() = ProgressionEngine.volume(sets)
    val completedSets: Int get() = sets.count { it.completed }
    val plannedSets: Int get() = routineExercise?.targetSets ?: sets.size

    /** Rest the user chose for this exercise: the routine value, else the exercise default. */
    val restSeconds: Int get() = routineExercise?.restSeconds ?: exercise.defaultRestSeconds
}

data class SessionUiState(
    val session: WorkoutSession? = null,
    val blocks: List<SessionExerciseBlock> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val finished: Boolean = false,
    /** Set when the user asked to close a session with no recorded set. */
    val confirmEmptyFinish: Boolean = false
) {
    val totalVolumeKg: Double get() = blocks.sumOf { it.volumeKg }
    val totalSets: Int get() = blocks.sumOf { it.completedSets }

    /** Sets that carry real data: a weight and reps recorded and marked done. */
    val effectiveSets: Int get() = blocks.sumOf { block ->
        block.sets.count { it.completed && it.reps != null && it.reps!! > 0 }
    }

    val plannedTotalSets: Int get() = blocks.sumOf { it.plannedSets }
    val hasRecordedWork: Boolean get() = effectiveSets > 0
}

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val session = sessionRepository.getById(sessionId)
            if (session == null) {
                _state.update { it.copy(loading = false, message = "Seduta non trovata") }
                return@launch
            }
            rebuild(session)
        }
    }

    private suspend fun rebuild(session: WorkoutSession) {
        val logged = sessionRepository.getSets(session.id)
        val routineExercises = session.routineId?.let { routineRepository.getExercises(it) }.orEmpty()
        val exercises = exerciseRepository.getAll().associateBy { it.id }

        val blocks = mutableListOf<SessionExerciseBlock>()
        // Exercises planned by the routine
        routineExercises.sortedBy { it.orderIndex }.forEach { re ->
            val exercise = exercises[re.exerciseId] ?: return@forEach
            val sets = logged.filter { it.exerciseId == re.exerciseId }.sortedBy { it.setIndex }
            val history = sessionRepository.historyFor(re.exerciseId)
            val suggestion = ProgressionEngine.suggest(re, history)
            blocks += SessionExerciseBlock(
                routineExercise = re,
                exercise = exercise,
                sets = sets,
                lastPerformance = sessionRepository.lastPerformance(re.exerciseId),
                suggestionReason = suggestion.reason,
                suggestedWeightKg = suggestion.suggestedWeightKg,
                suggestedReps = suggestion.suggestedReps
            )
        }
        // Exercises logged but not planned (added on the fly)
        logged.map { it.exerciseId }.distinct()
            .filter { id -> routineExercises.none { it.exerciseId == id } }
            .forEach { exerciseId ->
                val exercise = exercises[exerciseId] ?: return@forEach
                val sets = logged.filter { it.exerciseId == exerciseId }.sortedBy { it.setIndex }
                blocks += SessionExerciseBlock(
                    routineExercise = null,
                    exercise = exercise,
                    sets = sets,
                    lastPerformance = sessionRepository.lastPerformance(exerciseId),
                    suggestionReason = null,
                    suggestedWeightKg = null,
                    suggestedReps = null
                )
            }
        _state.update { it.copy(session = session, blocks = blocks, loading = false) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            val existing = sessionRepository.getSets(session.id).filter { it.exerciseId == exerciseId }
            val setIndex = (existing.maxOfOrNull { it.setIndex } ?: 0) + 1
            sessionRepository.upsertSet(
                LoggedSet(
                    sessionId = session.id,
                    exerciseId = exerciseId,
                    orderIndex = 0,
                    setIndex = setIndex,
                    setType = SetType.WORKING,
                    weightKg = null,
                    reps = null,
                    rir = null,
                    completed = false
                )
            )
            rebuild(session)
        }
    }

    fun addSet(block: SessionExerciseBlock) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            val nextIndex = (block.sets.maxOfOrNull { it.setIndex } ?: 0) + 1
            val last = block.sets.lastOrNull { it.completed } ?: block.sets.lastOrNull()
            sessionRepository.upsertSet(
                LoggedSet(
                    sessionId = session.id,
                    exerciseId = block.exercise.id,
                    orderIndex = block.routineExercise?.orderIndex ?: 0,
                    setIndex = nextIndex,
                    setType = SetType.WORKING,
                    weightKg = last?.weightKg ?: block.suggestedWeightKg,
                    reps = block.suggestedReps ?: block.routineExercise?.targetRepsMin,
                    rir = null,
                    restSeconds = block.routineExercise?.restSeconds,
                    completed = false
                )
            )
            rebuild(session)
        }
    }

    fun updateSet(set: LoggedSet, weight: Double?, reps: Int?, rir: Int?, pain: Boolean) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            sessionRepository.upsertSet(
                set.copy(weightKg = weight, reps = reps, rir = rir, pain = pain)
            )
            rebuild(session)
        }
    }

    fun toggleCompleted(set: LoggedSet) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            sessionRepository.upsertSet(set.copy(completed = !set.completed))
            rebuild(session)
        }
    }

    /**
     * Changes the rest of one exercise by [delta] seconds, clamped to a sane range,
     * and persists it both on the routine (so it survives the session) and on the
     * exercise default (so a session without a routine keeps it too).
     */
    fun adjustRest(block: SessionExerciseBlock, delta: Int) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            val newRest = (block.restSeconds + delta).coerceIn(15, 600)
            exerciseRepository.setDefaultRest(block.exercise.id, newRest)
            block.routineExercise?.let { re ->
                val updated = routineRepository.getExercises(re.routineId).map { item ->
                    if (item.id == re.id) item.copy(restSeconds = newRest) else item
                }
                routineRepository.getById(re.routineId)?.let { routine ->
                    routineRepository.saveRoutineWithExercises(routine, updated)
                }
            }
            rebuild(session)
        }
    }

    fun deleteSet(set: LoggedSet) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            sessionRepository.deleteSet(set.id)
            rebuild(session)
        }
    }

    /**
     * Closes the session. A session with no effective set is never silently marked
     * as completed: the caller must pass [force] after an explicit confirmation.
     */
    fun finishSession(notes: String, force: Boolean = false) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            if (!_state.value.hasRecordedWork && !force) {
                _state.update {
                    it.copy(
                        confirmEmptyFinish = true,
                        message = "Nessuna serie registrata: conferma di voler chiudere una seduta vuota."
                    )
                }
                return@launch
            }
            val updated = session.copy(
                status = SessionStatus.COMPLETED,
                finishedAt = DateUtils.nowMillis(),
                notes = notes
            )
            sessionRepository.updateSession(updated)
            _state.update {
                it.copy(
                    session = updated,
                    message = if (_state.value.hasRecordedWork) "Seduta salvata" else "Seduta vuota salvata",
                    finished = true,
                    confirmEmptyFinish = false
                )
            }
        }
    }

    fun cancelEmptyFinish() = _state.update { it.copy(confirmEmptyFinish = false, message = null) }

    fun discardSession() {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            sessionRepository.deleteSession(session.id)
            _state.update { it.copy(message = "Seduta eliminata", finished = true) }
        }
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            sessionRepository.updateSession(session.copy(notes = notes))
            _state.update { it.copy(session = session.copy(notes = notes)) }
        }
    }

    fun allExercises(): List<Exercise> = _state.value.blocks.map { it.exercise }
}
