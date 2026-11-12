package com.fitcoach.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import com.fitcoach.app.domain.model.CheckIn
import com.fitcoach.app.domain.model.CircumferenceRecord
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WeightTrend
import com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WeightPoint(val epochDay: Long, val kg: Double)
data class VolumePoint(val epochDay: Long, val volumeKg: Double)

data class ProgressUiState(
    val loading: Boolean = true,
    val weights: List<WeightRecord> = emptyList(),
    val circumferences: List<CircumferenceRecord> = emptyList(),
    val trend: WeightTrend? = null,
    val latestCheckIn: CheckIn? = null,
    val volumeBySession: List<VolumePoint> = emptyList(),
    val message: String? = null,
    val weightInput: String = "",
    val bfInput: String = "",
    val circumferenceSiteInput: String = "",
    val circumferenceCmInput: String = ""
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val workoutRepository: WorkoutRepository,
    private val userRepository: UserRepository,
    private val runWeeklyCheckIn: RunWeeklyCheckInUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val weights = progressRepository.getWeights()
            val circumferences = progressRepository.getCircumferences()
            val sessions = workoutRepository.getRecentSessions(limit = 60)
            val checkIn = progressRepository.getLatestCheckIn()
            _state.value = _state.value.copy(
                loading = false,
                weights = weights,
                circumferences = circumferences,
                trend = if (weights.size >= 2) WeightAnalyzer.trend(weights) else null,
                latestCheckIn = checkIn,
                volumeBySession = sessions
                    .filter { it.status == com.fitcoach.app.domain.model.SessionStatus.COMPLETED }
                    .sortedBy { it.dateEpochDay }
                    .map { VolumePoint(it.dateEpochDay, it.totalVolumeKg) }
            )
        }
    }

    fun updateWeightInput(v: String) { _state.value = _state.value.copy(weightInput = v) }
    fun updateBfInput(v: String) { _state.value = _state.value.copy(bfInput = v) }
    fun updateCircumferenceSite(v: String) {
        _state.value = _state.value.copy(circumferenceSiteInput = v)
    }
    fun updateCircumferenceCm(v: String) {
        _state.value = _state.value.copy(circumferenceCmInput = v)
    }

    fun addWeight() {
        viewModelScope.launch {
            val kg = _state.value.weightInput.replace(',', '.').toDoubleOrNull()
            if (kg == null || kg <= 20.0 || kg > 400.0) {
                _state.value = _state.value.copy(message = "Inserisci un peso valido in kg.")
                return@launch
            }
            val bf = _state.value.bfInput.replace(',', '.').toDoubleOrNull()
            progressRepository.addWeight(
                WeightRecord(
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    weightKg = kg,
                    bodyFatPercent = bf?.takeIf { it in 1.0..70.0 }
                )
            )
            _state.value = _state.value.copy(
                weightInput = "", bfInput = "", message = "Peso registrato: %.1f kg.".format(kg)
            )
            load()
        }
    }

    fun runCheckIn() {
        viewModelScope.launch {
            val outcome = runWeeklyCheckIn()
            _state.value = _state.value.copy(
                message = when {
                    outcome == null -> "Completa prima il profilo."
                    outcome.proposals > 0 ->
                        "Check-in salvato. ${outcome.proposals} proposta/e di modifica da valutare nel Coach."
                    else -> "Check-in salvato: nessuna modifica necessaria."
                }
            )
            load()
        }
    }

    fun addCircumference() {
        viewModelScope.launch {
            val site = _state.value.circumferenceSiteInput.trim()
            val cm = _state.value.circumferenceCmInput.replace(',', '.').toDoubleOrNull()
            if (site.isBlank() || cm == null || cm !in 5.0..250.0) {
                _state.value = _state.value.copy(message = "Inserisci una sede e una misura valida in cm.")
                return@launch
            }
            progressRepository.addCircumference(
                CircumferenceRecord(
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    site = site,
                    valueCm = cm
                )
            )
            _state.value = _state.value.copy(
                circumferenceSiteInput = "",
                circumferenceCmInput = "",
                message = "Misura registrata: $site %.1f cm.".format(cm)
            )
            load()
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
