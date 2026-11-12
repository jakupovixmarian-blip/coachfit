package dev.fitcoach.app.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExerciseHistoryEntry
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    val exercises: StateFlow<List<Exercise>> =
        exerciseRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCustom(name: String, muscle: MuscleGroup, execution: String, setup: String, mistakes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            exerciseRepository.addCustom(
                Exercise(
                    name = name.trim(),
                    primaryMuscle = muscle,
                    execution = execution,
                    setup = setup,
                    commonMistakes = mistakes,
                    isCustom = true
                )
            )
        }
    }

    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch { exerciseRepository.setFavorite(exercise.id, !exercise.isFavorite) }
    }

    fun deleteCustom(exercise: Exercise) {
        viewModelScope.launch { exerciseRepository.deleteCustom(exercise.id) }
    }
}

data class ExerciseDetailState(
    val exercise: Exercise? = null,
    val history: List<LoggedSet> = emptyList(),
    val last: ExerciseHistoryEntry? = null,
    val bestWeightKg: Double? = null,
    val bestOneRm: Double? = null,
    val totalVolume: Double = 0.0,
    val personalBestReps: Int? = null
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L
    private val _state = MutableStateFlow(ExerciseDetailState())
    val state: StateFlow<ExerciseDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(exerciseId)
            val history = sessionRepository.historyFor(exerciseId)
            val working = history.filter { it.completed && it.setType.name != "WARMUP" }
            val bestWeight = working.mapNotNull { it.weightKg }.maxOrNull()
            val bestOneRm = working.mapNotNull { set ->
                set.reps?.let { reps -> set.weightKg?.let { w -> ProgressionEngine.estimatedOneRepMax(w, reps) } }
            }.maxOrNull()
            _state.value = ExerciseDetailState(
                exercise = exercise,
                history = history,
                last = sessionRepository.lastPerformance(exerciseId),
                bestWeightKg = bestWeight,
                bestOneRm = bestOneRm,
                totalVolume = ProgressionEngine.volume(history),
                personalBestReps = working.mapNotNull { it.reps }.maxOrNull()
            )
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val exercise = _state.value.exercise ?: return@launch
            exerciseRepository.setFavorite(exercise.id, !exercise.isFavorite)
            _state.update { it.copy(exercise = exercise.copy(isFavorite = !exercise.isFavorite)) }
        }
    }
}
