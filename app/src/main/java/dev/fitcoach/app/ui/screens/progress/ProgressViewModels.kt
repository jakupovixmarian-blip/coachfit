package dev.fitcoach.app.ui.screens.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.Measurement
import dev.fitcoach.app.domain.model.MeasurementType
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.MeasurementRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProgressUiState(
    val measurements: List<Measurement> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val weeklyVolume: List<Pair<String, Double>> = emptyList(),
    val weeklySets: Int = 0,
    val adherencePercent: Double? = null,
    val currentWeight: Double? = null,
    val message: String? = null
) {
    fun byType(type: MeasurementType): List<Measurement> =
        measurements.filter { it.type == type }.sortedBy { it.dateEpochDay }
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
    sessionRepository: SessionRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _volume = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    private val _sets = MutableStateFlow(0)
    private val _adherence = MutableStateFlow<Double?>(null)

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                measurementRepository.observeAll(),
                sessionRepository.observeSessions(),
                profileRepository.observeProfile()
            ) { measurements, sessions, profile ->
                ProgressUiState(
                    measurements = measurements,
                    sessions = sessions.filter { it.status == SessionStatus.COMPLETED },
                    weeklyVolume = _volume.value,
                    weeklySets = _sets.value,
                    adherencePercent = _adherence.value,
                    currentWeight = profile?.weightKg,
                    message = _message.value
                )
            }.collect { _state.value = it }
        }
        viewModelScope.launch {
            sessionRepository.observeSessions().collect { sessions ->
                computeVolume(sessionRepository, sessions.filter { it.status == SessionStatus.COMPLETED })
            }
        }
    }

    private suspend fun computeVolume(repository: SessionRepository, sessions: List<WorkoutSession>) {
        val weeks = mutableListOf<Pair<String, Double>>()
        var totalSets = 0
        val startOfWeek = DateUtils.startOfWeek()
        for (offset in 3 downTo 0) {
            val weekStart = startOfWeek.minusWeeks(offset.toLong())
            val weekEnd = weekStart.plusDays(6)
            val inWeek = sessions.filter {
                val d = DateUtils.fromEpochDay(it.dateEpochDay)
                !d.isBefore(weekStart) && !d.isAfter(weekEnd)
            }
            var volume = 0.0
            inWeek.forEach { session ->
                val sets = repository.getSets(session.id)
                volume += ProgressionEngine.volume(sets)
                totalSets += sets.count { it.completed }
            }
            weeks += "S${weekStart.dayOfMonth}" to volume
        }
        _volume.value = weeks
        _sets.value = totalSets
    }

    fun dismissMessage() { _message.value = null; _state.value = _state.value.copy(message = null) }

    fun addMeasurement(type: MeasurementType, value: Double, date: LocalDate, customLabel: String, notes: String) {
        viewModelScope.launch {
            measurementRepository.add(
                Measurement(
                    type = type,
                    customLabel = customLabel,
                    dateEpochDay = date.toEpochDay(),
                    value = value,
                    notes = notes
                )
            )
            _message.value = "Misurazione salvata"
        }
    }

    fun deleteMeasurement(measurement: Measurement) {
        viewModelScope.launch { measurementRepository.delete(measurement.id) }
    }
}

data class MeasurementDetailState(
    val type: MeasurementType = MeasurementType.WEIGHT,
    val entries: List<Measurement> = emptyList(),
    val delta: Double? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val average: Double? = null
)

@HiltViewModel
class MeasurementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val typeName: String = savedStateHandle.get<String>("type") ?: MeasurementType.WEIGHT.name
    private val type: MeasurementType = MeasurementType.entries.firstOrNull { it.name == typeName } ?: MeasurementType.WEIGHT

    private val _state = MutableStateFlow(MeasurementDetailState(type = type))
    val state: StateFlow<MeasurementDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            measurementRepository.observeByType(type).collect { list ->
                val sorted = list.sortedBy { it.dateEpochDay }
                val delta = if (sorted.size >= 2) sorted.last().value - sorted.first().value else null
                _state.value = MeasurementDetailState(
                    type = type,
                    entries = sorted,
                    delta = delta,
                    minValue = sorted.minOfOrNull { it.value },
                    maxValue = sorted.maxOfOrNull { it.value },
                    average = sorted.takeIf { it.isNotEmpty() }?.map { it.value }?.average()
                )
            }
        }
    }

    fun delete(measurement: Measurement) {
        viewModelScope.launch { measurementRepository.delete(measurement.id) }
    }
}
