package dev.fitcoach.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.repository.BackupRepository
import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.FoodRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val profile: UserProfile? = null,
    val message: String? = null,
    val error: String? = null,
    val busy: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val exerciseRepository: ExerciseRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                _state.value = _state.value.copy(profile = profile)
            }
        }
    }

    fun dismissMessages() { _state.value = _state.value.copy(message = null, error = null) }

    fun updateProfile(transform: (UserProfile) -> UserProfile) {
        viewModelScope.launch {
            val current = profileRepository.getProfile() ?: UserProfile()
            profileRepository.saveProfile(transform(current).copy(updatedAt = DateUtils.nowMillis()))
            _state.value = _state.value.copy(message = "Profilo aggiornato")
        }
    }

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            try {
                val payload = backupRepository.exportSnapshot()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write(payload.toByteArray())
                    } ?: throw IllegalStateException("Impossibile aprire il file di destinazione")
                }
                _state.value = _state.value.copy(busy = false, message = "Backup esportato")
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, error = "Esportazione non riuscita: ${t.message}")
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Impossibile leggere il file")
                }
                val result = backupRepository.importSnapshot(content)
                result.fold(
                    onSuccess = { restored ->
                        exerciseRepository.ensureSeeded()
                        foodRepository.ensureSeeded()
                        _state.value = _state.value.copy(busy = false, message = "Backup importato ($restored record principali)")
                    },
                    onFailure = { t ->
                        _state.value = _state.value.copy(busy = false, error = "Importazione non riuscita: ${t.message}")
                    }
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, error = "Importazione non riuscita: ${t.message}")
            }
        }
    }
}
