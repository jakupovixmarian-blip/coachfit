package dev.fitcoach.app.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
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

data class ManualDraftExercise(
    val exerciseId: Long,
    val sets: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 12,
    val restSeconds: Int = 90
)

data class ManualDraftDay(val label: String, val exercises: List<ManualDraftExercise>)

data class ManualProgramState(
    val programName: String = "Il mio programma",
    val days: List<ManualDraftDay> = emptyList(),
    val exercisesById: Map<Long, Exercise> = emptyMap(),
    val allExercises: List<Exercise> = emptyList(),
    val message: String? = null,
    val saved: Boolean = false
)

/**
 * Manual program builder, independent from the AI. The user chooses sessions,
 * exercises and numbers; nothing here calls the agent or applies defaults beyond
 * the fields shown in the form.
 */
@HiltViewModel
class ManualProgramViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ManualProgramState())
    val state: StateFlow<ManualProgramState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            exerciseRepository.ensureSeeded()
            val catalog = exerciseRepository.getAll()
            _state.update { it.copy(allExercises = catalog, exercisesById = catalog.associateBy { e -> e.id }) }
        }
    }

    fun setProgramName(value: String) = _state.update { it.copy(programName = value) }

    fun addDay() = _state.update {
        it.copy(days = it.days + ManualDraftDay(label = "Giorno ${it.days.size + 1}", exercises = emptyList()))
    }

    fun removeDay(index: Int) = _state.update { current ->
        current.copy(days = current.days.filterIndexed { i, _ -> i != index })
    }

    fun setDayLabel(index: Int, label: String) = _state.update { current ->
        current.copy(days = current.days.mapIndexed { i, day -> if (i == index) day.copy(label = label) else day })
    }

    fun addExercise(dayIndex: Int, exerciseId: Long) = _state.update { current ->
        current.copy(days = current.days.mapIndexed { i, day ->
            if (i != dayIndex) day
            else day.copy(exercises = day.exercises + ManualDraftExercise(exerciseId))
        })
    }

    fun updateExercise(dayIndex: Int, exerciseIndex: Int, transform: (ManualDraftExercise) -> ManualDraftExercise) =
        _state.update { current ->
            current.copy(days = current.days.mapIndexed { i, day ->
                if (i != dayIndex) day
                else day.copy(exercises = day.exercises.mapIndexed { j, item ->
                    if (j == exerciseIndex) transform(item) else item
                })
            })
        }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) = _state.update { current ->
        current.copy(days = current.days.mapIndexed { i, day ->
            if (i != dayIndex) day else day.copy(exercises = day.exercises.filterIndexed { j, _ -> j != exerciseIndex })
        })
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            if (current.days.isEmpty()) {
                _state.update { it.copy(message = "Aggiungi almeno una giornata.") }
                return@launch
            }
            if (current.days.any { it.exercises.isEmpty() }) {
                _state.update { it.copy(message = "Ogni giornata deve avere almeno un esercizio.") }
                return@launch
            }
            val routines = current.days.mapIndexed { index, day ->
                Routine(
                    name = day.label,
                    description = "Programma creato manualmente",
                    isActive = true,
                    programId = 0L,
                    dayIndex = index,
                    dayLabel = day.label
                )
            }
            val exercisesByRoutine = current.days.map { day ->
                day.exercises.mapIndexed { order, item ->
                    RoutineExercise(
                        routineId = 0L,
                        exerciseId = item.exerciseId,
                        orderIndex = order,
                        targetSets = item.sets,
                        targetRepsMin = item.repsMin,
                        targetRepsMax = item.repsMax,
                        targetRir = 2,
                        restSeconds = item.restSeconds
                    )
                }
            }
            routineRepository.saveProgram(current.programName.ifBlank { "Il mio programma" }, routines, exercisesByRoutine)
            _state.update { it.copy(message = "Programma salvato", saved = true) }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
