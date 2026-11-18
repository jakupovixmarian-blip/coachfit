package com.fitcoach.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Bozza di esercizio nella scheda personalizzata, in fase di modifica. */
data class DraftExercise(
    val exerciseId: Long,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val sets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int,
    val targetRir: Int
)

/** Bozza di giorno della scheda personalizzata. */
data class DraftDay(
    val dayIndex: Int,
    val title: String,
    val isRestDay: Boolean,
    val exercises: List<DraftExercise> = emptyList()
) {
    val totalSets: Int get() = exercises.sumOf { it.sets }
}

data class CustomWorkoutUiState(
    val loading: Boolean = true,
    val days: List<DraftDay> = emptyList(),
    val library: List<Exercise> = emptyList(),
    val libraryQuery: String = "",
    val message: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    /** Giorno attualmente in modifica (-1 = nessuno). */
    val editingDayIndex: Int = -1
) {
    val libraryFiltered: List<Exercise>
        get() = if (libraryQuery.isBlank()) library
        else library.filter { it.name.contains(libraryQuery, ignoreCase = true) }
}

/**
 * Crea una scheda di allenamento completamente personalizzata: l'utente sceglie
 * i giorni, gli esercizi e i parametri (serie, ripetizioni, recupero, RIR).
 *
 * Rispetto alla scheda generata dal Coach AI (che resta il percorso principale),
 * qui l'utente ha il controllo manuale totale: e l'opzione richiesta per chi
 * vuole costruirsi la programmazione da zero. La scheda salvata diventa quella
 * attiva e passa comunque dal repository, quindi la progressione dei carichi
 * continua a funzionare.
 */
@HiltViewModel
class CustomWorkoutViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomWorkoutUiState())
    val state: StateFlow<CustomWorkoutUiState> = _state.asStateFlow()
    private var editingPlanId: Long? = null

    init {
        load()
    }

    fun load(planId: Long? = null) {
        if (planId != null) editingPlanId = planId
        viewModelScope.launch {
            val library = exerciseRepository.getAll()
            val existing = if (editingPlanId != null) {
                workoutRepository.getAllPlans().firstOrNull { it.id == editingPlanId }
            } else {
                workoutRepository.getActivePlan()
            }
            val days = if (existing != null && existing.days.isNotEmpty()) {
                existing.days.sortedBy { it.dayIndex }.map { day ->
                    DraftDay(
                        dayIndex = day.dayIndex,
                        title = day.title,
                        isRestDay = day.isRestDay,
                        exercises = day.exercises.sortedBy { it.orderIndex }.map { pe ->
                            DraftExercise(
                                exerciseId = pe.exerciseId,
                                name = pe.exerciseName,
                                primaryMuscle = pe.primaryMuscle,
                                sets = pe.sets,
                                minReps = pe.minReps,
                                maxReps = pe.maxReps,
                                restSeconds = pe.restSeconds,
                                targetRir = pe.targetRir
                            )
                        }
                    )
                }
            } else {
                (0..6).map { DraftDay(it, "Giorno ${it + 1}", isRestDay = true) }
            }
            _state.value = _state.value.copy(loading = false, days = days, library = library)
        }
    }

    fun updateLibraryQuery(query: String) {
        _state.value = _state.value.copy(libraryQuery = query)
    }

    fun startEditingDay(dayIndex: Int) {
        _state.value = _state.value.copy(editingDayIndex = dayIndex)
    }

    fun stopEditingDay() {
        _state.value = _state.value.copy(editingDayIndex = -1)
    }

    fun toggleRestDay(dayIndex: Int) {
        val days = _state.value.days.map { day ->
            if (day.dayIndex != dayIndex) day
            else day.copy(
                isRestDay = !day.isRestDay,
                exercises = if (!day.isRestDay) emptyList() else day.exercises
            )
        }
        _state.value = _state.value.copy(days = days)
    }

    fun updateDayTitle(dayIndex: Int, title: String) {
        _state.value = _state.value.copy(
            days = _state.value.days.map { if (it.dayIndex == dayIndex) it.copy(title = title) else it }
        )
    }

    fun addExerciseToDay(dayIndex: Int, exercise: Exercise) {
        val days = _state.value.days.map { day ->
            if (day.dayIndex != dayIndex) day
            else day.copy(
                isRestDay = false,
                exercises = day.exercises + DraftExercise(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    primaryMuscle = exercise.primaryMuscle,
                    sets = exercise.defaultSets.coerceIn(1, 10),
                    minReps = exercise.minReps.coerceIn(1, 50),
                    maxReps = exercise.maxReps.coerceIn(exercise.minReps, 60),
                    restSeconds = exercise.restSeconds.coerceIn(15, 600),
                    targetRir = exercise.targetRir.coerceIn(0, 5)
                )
            )
        }
        _state.value = _state.value.copy(days = days)
    }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) {
        val days = _state.value.days.map { day ->
            if (day.dayIndex != dayIndex) day
            else day.copy(exercises = day.exercises.filterIndexed { i, _ -> i != exerciseIndex })
        }
        _state.value = _state.value.copy(days = days)
    }

    fun moveExercise(dayIndex: Int, exerciseIndex: Int, delta: Int) {
        val days = _state.value.days.map { day ->
            if (day.dayIndex != dayIndex) day
            else {
                val list = day.exercises.toMutableList()
                val target = (exerciseIndex + delta).coerceIn(0, list.size - 1)
                if (target != exerciseIndex) {
                    val item = list.removeAt(exerciseIndex)
                    list.add(target, item)
                }
                day.copy(exercises = list)
            }
        }
        _state.value = _state.value.copy(days = days)
    }

    fun updateExercise(
        dayIndex: Int,
        exerciseIndex: Int,
        sets: Int? = null,
        minReps: Int? = null,
        maxReps: Int? = null,
        restSeconds: Int? = null,
        targetRir: Int? = null
    ) {
        val days = _state.value.days.map { day ->
            if (day.dayIndex != dayIndex) day
            else day.copy(
                exercises = day.exercises.mapIndexed { i, ex ->
                    if (i != exerciseIndex) ex
                    else ex.copy(
                        sets = sets?.coerceIn(1, 10) ?: ex.sets,
                        minReps = minReps?.coerceIn(1, 50) ?: ex.minReps,
                        maxReps = maxReps?.coerceIn(1, 60) ?: ex.maxReps,
                        restSeconds = restSeconds?.coerceIn(15, 600) ?: ex.restSeconds,
                        targetRir = targetRir?.coerceIn(0, 5) ?: ex.targetRir
                    ).let { updated ->
                        // La ripetizione minima non puo superare la massima.
                        if (updated.minReps > updated.maxReps) {
                            updated.copy(maxReps = updated.minReps)
                        } else updated
                    }
                }
            )
        }
        _state.value = _state.value.copy(days = days)
    }

    fun save() {
        viewModelScope.launch {
            val days = _state.value.days
            if (days.none { !it.isRestDay && it.exercises.isNotEmpty() }) {
                _state.value = _state.value.copy(
                    message = "Aggiungi almeno un esercizio prima di salvare la scheda."
                )
                return@launch
            }
            val invalid = days.firstOrNull { day ->
                !day.isRestDay && day.exercises.any { it.minReps > it.maxReps }
            }
            if (invalid != null) {
                _state.value = _state.value.copy(
                    message = "Controlla le ripetizioni del giorno ${invalid.dayIndex + 1}: il minimo supera il massimo."
                )
                return@launch
            }

            _state.value = _state.value.copy(saving = true, message = null)
            val profile = userRepository.get()
            if (profile == null) {
                _state.value = _state.value.copy(
                    saving = false,
                    message = "Profilo non disponibile: completa l'anamnesi prima di creare una scheda."
                )
                return@launch
            }

            val domainDays = days.map { draft ->
                WorkoutDay(
                    dayIndex = draft.dayIndex,
                    title = draft.title.ifBlank { if (draft.isRestDay) "Riposo" else "Allenamento" },
                    muscleGroups = draft.exercises.map { it.primaryMuscle }.toSet(),
                    isRestDay = draft.isRestDay || draft.exercises.isEmpty(),
                    exercises = draft.exercises.mapIndexed { index, ex ->
                        PlannedExercise(
                            exerciseId = ex.exerciseId,
                            exerciseName = ex.name,
                            primaryMuscle = ex.primaryMuscle,
                            orderIndex = index,
                            sets = ex.sets,
                            minReps = ex.minReps,
                            maxReps = ex.maxReps,
                            restSeconds = ex.restSeconds,
                            targetRir = ex.targetRir,
                            selectionReason = "Scelto manualmente dall'utente.",
                            restReason = "Recupero impostato manualmente."
                        )
                    }
                )
            }

            val weeklyVolume = domainDays
                .flatMap { it.exercises }
                .groupingBy { it.primaryMuscle }
                .fold(0) { acc, ex -> acc + ex.sets }

            val built = WorkoutPlanBuilder.BuiltPlan(
                splitType = SplitType.CUSTOM,
                days = domainDays,
                weeklyVolumePerGroup = weeklyVolume,
                notes = listOf("Scheda personalizzata creata manualmente.")
            )
            runCatching {
                if (editingPlanId != null) {
                    check(workoutRepository.updatePlan(editingPlanId!!, profile, built)) {
                        "La scheda da modificare non esiste piu."
                    }
                } else {
                    workoutRepository.savePlan(profile, built)
                    true
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    saving = false,
                    saved = true,
                    message = "Scheda personalizzata salvata e impostata come attiva."
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    saving = false,
                    message = "Salvataggio non riuscito: ${e.message ?: "errore sconosciuto"}"
                )
            }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
