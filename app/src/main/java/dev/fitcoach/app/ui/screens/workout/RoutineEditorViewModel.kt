package dev.fitcoach.app.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineEditorState(
    val name: String = "",
    val description: String = "",
    val items: List<RoutineExercise> = emptyList(),
    val exercisesById: Map<Long, Exercise> = emptyMap(),
    val saved: Boolean = false
)

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<Long>("routineId") ?: 0L
    private var loadedRoutine: Routine? = null

    private val _state = MutableStateFlow(RoutineEditorState())
    val state: StateFlow<RoutineEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Seeding runs in the background at app start: without this the editor could
            // open against a still empty catalog and show names with no exercise to add.
            exerciseRepository.ensureSeeded()
            val exercises = exerciseRepository.getAll().associateBy { it.id }
            val routine = routineRepository.getById(routineId)
            loadedRoutine = routine
            _state.value = RoutineEditorState(
                name = routine?.name ?: "Nuova scheda",
                description = routine?.description.orEmpty(),
                items = routineRepository.getExercises(routineId).mapIndexed { i, re -> re.copy(orderIndex = i) },
                exercisesById = exercises
            )
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }

    fun addExercise(exerciseId: Long) {
        _state.update { current ->
            current.copy(
                items = current.items + RoutineExercise(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    orderIndex = current.items.size
                )
            )
        }
    }

    fun updateItem(index: Int, item: RoutineExercise) {
        _state.update { current ->
            val list = current.items.toMutableList()
            if (index in list.indices) list[index] = item.copy(orderIndex = index)
            current.copy(items = list)
        }
    }

    fun removeItem(index: Int) {
        _state.update { current ->
            val list = current.items.toMutableList()
            if (index in list.indices) list.removeAt(index)
            current.copy(items = list.mapIndexed { i, re -> re.copy(orderIndex = i) })
        }
    }

    fun move(index: Int, delta: Int) {
        _state.update { current ->
            val list = current.items.toMutableList()
            val target = index + delta
            if (index in list.indices && target in list.indices) {
                val tmp = list[target]
                list[target] = list[index]
                list[index] = tmp
            }
            current.copy(items = list.mapIndexed { i, re -> re.copy(orderIndex = i) })
        }
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            val routine = (loadedRoutine ?: Routine(name = current.name)).copy(
                id = routineId,
                name = current.name,
                description = current.description
            )
            val id = routineRepository.saveRoutineWithExercises(routine, current.items)
            loadedRoutine = routine.copy(id = id)
            if (routineId == 0L) routineRepository.setActive(id)
            _state.update { it.copy(saved = true) }
        }
    }
}
