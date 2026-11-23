package com.fitcoach.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ApplyResult
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.ProgressPhotoRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.StatisticsEngine
import com.fitcoach.app.domain.adaptive.StatRange
import com.fitcoach.app.domain.adaptive.WeeklyCheckInAnalyzer
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import com.fitcoach.app.domain.model.CheckIn
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.CircumferenceRecord
import com.fitcoach.app.domain.model.ProgramChange
import com.fitcoach.app.domain.model.ProgressPhoto
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WeightTrend
import com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val checkInRunning: Boolean = false,
    val checkInEvaluation: String = "",
    val checkInReport: WeeklyCheckInAnalyzer.Report? = null,
    val checkInProposals: List<ProgramChange> = emptyList(),
    val volumeBySession: List<VolumePoint> = emptyList(),
    val statRange: StatRange = StatRange.WEEK,
    val statistics: com.fitcoach.app.domain.adaptive.RangeStatistics? = null,
    val photos: List<ProgressPhoto> = emptyList(),
    val message: String? = null,
    val weightInput: String = "",
    val bfInput: String = "",
    val circumferenceSiteInput: String = "",
    val circumferenceCmInput: String = ""
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val photoRepository: ProgressPhotoRepository,
    private val workoutRepository: WorkoutRepository,
    private val userRepository: UserRepository,
    private val changeRepository: ChangeRepository,
    private val actionExecutor: CoachActionExecutor,
    private val diaryRepository: NutritionDiaryRepository,
    private val nutritionRepository: com.fitcoach.app.data.repository.NutritionRepository,
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
            val photos = photoRepository.photos.first()
            val sessions = workoutRepository.getRecentSessions(limit = 400)
            val checkIn = progressRepository.getLatestCheckIn()
            _state.value = _state.value.copy(
                loading = false,
                weights = weights,
                circumferences = circumferences,
                trend = if (weights.size >= 2) WeightAnalyzer.trend(weights) else null,
                photos = photos,
                latestCheckIn = checkIn,
                volumeBySession = sessions
                    .filter { it.status == com.fitcoach.app.domain.model.SessionStatus.COMPLETED }
                    .sortedBy { it.dateEpochDay }
                    .map { VolumePoint(it.dateEpochDay, it.totalVolumeKg) }
            )
            loadStatistics()
        }
    }

    fun setStatRange(range: StatRange) {
        _state.value = _state.value.copy(statRange = range)
        viewModelScope.launch { loadStatistics() }
    }

    private suspend fun loadStatistics() {
        val today = LocalDate.now().toEpochDay()
        val plan = nutritionRepository.getActivePlan()
        val statistics = StatisticsEngine.compute(
            range = _state.value.statRange,
            todayEpochDay = today,
            weights = progressRepository.getWeights(),
            diary = diaryRepository.allEntries(),
            sessions = workoutRepository.getSessionsBetween(today - 120, today),
            performances = workoutRepository.getPerformanceByExercise().values.flatten(),
            targetKcalFor = { epochDay ->
                val dayIndex = com.fitcoach.app.domain.nutrition.NutritionDiaryEngine.dayIndexFor(epochDay)
                plan?.days?.firstOrNull { it.dayIndex == dayIndex }?.targetKcal ?: 0.0
            }
        )
        _state.value = _state.value.copy(statistics = statistics)
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
            _state.value = _state.value.copy(checkInRunning = true, message = null)
            val outcome = runWeeklyCheckIn()
            _state.value = _state.value.copy(
                checkInRunning = false,
                checkInEvaluation = outcome?.evaluation ?: "",
                checkInReport = outcome?.report,
                checkInProposals = outcome?.proposals ?: emptyList(),
                message = when {
                    outcome == null -> "Completa prima il profilo."
                    else -> outcome.notes.firstOrNull()
                }
            )
            load()
        }
    }

    fun approveCheckInProposal(changeId: Long) {
        viewModelScope.launch {
            when (val result = actionExecutor.apply(changeId)) {
                is ApplyResult.Applied -> {
                    _state.value = _state.value.copy(message = "Modifica applicata: ${result.message}")
                    refreshProposals()
                }
                is ApplyResult.Failed -> _state.value = _state.value.copy(message = result.message)
                else -> Unit
            }
        }
    }

    fun rejectCheckInProposal(changeId: Long) {
        viewModelScope.launch {
            changeRepository.updateStatus(changeId, ChangeStatus.REJECTED)
            _state.value = _state.value.copy(message = "Proposta rifiutata.")
            refreshProposals()
        }
    }

    private suspend fun refreshProposals() {
        val proposals = _state.value.checkInProposals.filter { change ->
            changeRepository.getById(change.id)?.status == ChangeStatus.PROPOSED
        }
        _state.value = _state.value.copy(checkInProposals = proposals)
        load()
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


    fun addPhoto(uri: String) {
        // Un URI remoto farebbe fallire il require() del repository dentro la
        // coroutine: l'eccezione chiuderebbe l'app invece di mostrare un errore.
        if (!uri.startsWith("content://") && !uri.startsWith("file://")) {
            _state.value = _state.value.copy(
                message = "La foto deve essere un file locale del dispositivo."
            )
            return
        }
        viewModelScope.launch {
            photoRepository.add(uri, System.currentTimeMillis(), pose = "", note = "", consentForAi = false)
            _state.value = _state.value.copy(message = "Foto salvata solo sul dispositivo.")
            load()
        }
    }

    fun deletePhoto(photo: ProgressPhoto) {
        viewModelScope.launch {
            photoRepository.delete(photo)
            _state.value = _state.value.copy(message = "Foto eliminata.")
            load()
        }
    }

    fun setPhotoAiConsent(photo: ProgressPhoto, enabled: Boolean) {
        viewModelScope.launch {
            photoRepository.setAiConsent(photo, enabled)
            _state.value = _state.value.copy(message = if (enabled) {
                "Consenso AI attivato solo per questa foto."
            } else "Consenso AI revocato.")
            load()
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
