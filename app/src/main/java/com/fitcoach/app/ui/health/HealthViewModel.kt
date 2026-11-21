package com.fitcoach.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.health.DailyHealthSnapshot
import com.fitcoach.app.data.health.HealthConnectReader
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HealthUiState(
    val loading: Boolean = true,
    val availability: HealthConnectReader.Availability = HealthConnectReader.Availability.NOT_SUPPORTED,
    val permissionsGranted: Boolean = false,
    val snapshot: DailyHealthSnapshot? = null,
    val message: String? = null,
    /** Peso e composizione dalla bilancia BLE, se presenti per oggi. */
    val scaleWeightKg: Double? = null,
    val scaleBodyFat: Double? = null
)

/**
 * Report giornaliero: unisce le misure di Health Connect con quelle della
 * bilancia collegata. Ogni valore dichiara la sua fonte.
 */
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthReader: HealthConnectReader,
    private val progressRepository: ProgressRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    val requiredPermissions: Set<String> get() = healthReader.permissions

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val availability = healthReader.availability()
            val granted = runCatching { healthReader.grantedPermissions() }.getOrDefault(emptySet())
            val snapshot = if (availability == HealthConnectReader.Availability.AVAILABLE) {
                runCatching { healthReader.read(LocalDate.now()) }.getOrNull()
            } else null

            val latest = runCatching { progressRepository.getLatestWeight() }.getOrNull()

            _state.value = HealthUiState(
                loading = false,
                availability = availability,
                permissionsGranted = granted.containsAll(healthReader.permissions),
                snapshot = snapshot,
                scaleWeightKg = latest?.weightKg,
                scaleBodyFat = latest?.bodyFatPercent,
                message = null
            )
        }
    }

    fun onPermissionsResult() = refresh()

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
