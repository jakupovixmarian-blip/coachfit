package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.NotificationRepository
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.domain.model.NotificationRecord
import com.fitcoach.app.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val loading: Boolean = true,
    val records: List<NotificationRecord> = emptyList(),
    val globalEnabled: Boolean = true,
    /** Avviso sonoro a fine recupero. */
    val restAlarmSound: Boolean = true,
    /** Vibrazione a fine recupero. */
    val restAlarmVibration: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val scheduler: NotificationScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsUiState())
    val state: StateFlow<NotificationSettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val records = notificationRepository.getAll()
            val prefs = preferencesRepository.getOrCreate()
            _state.value = NotificationSettingsUiState(
                loading = false,
                records = records,
                globalEnabled = prefs.notificationsEnabled,
                restAlarmSound = prefs.restAlarmSound,
                restAlarmVibration = prefs.restAlarmVibration
            )
        }
    }

    fun setEnabled(record: NotificationRecord, enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.setEnabled(record.id, enabled)
            reschedule()
            load()
        }
    }

    fun setTime(record: NotificationRecord, time: String) {
        if (!Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(time)) {
            _state.value = _state.value.copy(message = "Formato orario non valido (usa HH:mm).")
            return
        }
        viewModelScope.launch {
            notificationRepository.setTime(record.id, time)
            reschedule()
            load()
        }
    }

    fun setGlobalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = preferencesRepository.getOrCreate()
            preferencesRepository.save(prefs.copy(notificationsEnabled = enabled))
            reschedule()
            load()
        }
    }

    /** Suono e vibrazione dell'avviso di fine recupero. Non toccano gli allarmi. */
    fun setRestAlarmSound(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = preferencesRepository.getOrCreate()
            preferencesRepository.save(prefs.copy(restAlarmSound = enabled))
            load()
        }
    }

    fun setRestAlarmVibration(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = preferencesRepository.getOrCreate()
            preferencesRepository.save(prefs.copy(restAlarmVibration = enabled))
            load()
        }
    }

    private suspend fun reschedule() {
        val prefs = preferencesRepository.getOrCreate()
        val records = notificationRepository.getAll()
        scheduler.scheduleAll(prefs, records)
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
