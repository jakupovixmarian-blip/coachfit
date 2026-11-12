package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.domain.model.CoachAuthorizationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachAuthorizationUiState(
    val mode: CoachAuthorizationMode = CoachAuthorizationMode.CONFIRMATION,
    val message: String? = null
)

@HiltViewModel
class CoachAuthorizationViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CoachAuthorizationUiState())
    val state: StateFlow<CoachAuthorizationUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(mode = preferencesRepository.getOrCreate().coachAuthorizationMode)
        }
    }

    fun setMode(mode: CoachAuthorizationMode) {
        viewModelScope.launch {
            preferencesRepository.save(preferencesRepository.getOrCreate().copy(coachAuthorizationMode = mode))
            _state.value = _state.value.copy(mode = mode, message = "Modalità Coach aggiornata.")
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
