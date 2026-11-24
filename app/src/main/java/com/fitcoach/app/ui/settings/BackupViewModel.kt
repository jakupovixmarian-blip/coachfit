package com.fitcoach.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.DatabaseBackupRepository
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.work.AutoBackupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val working: Boolean = false,
    val message: String? = null,
    val autoBackupEnabled: Boolean = true
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DatabaseBackupRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val prefs = preferencesRepository.getOrCreate()
                _state.value = _state.value.copy(autoBackupEnabled = prefs.autoBackupEnabled)
            }
        }
    }

    /**
     * Attiva o disattiva il backup automatico. Il lavoro WorkManager viene
     * accodato o annullato subito: rimandarlo all'avvio successivo farebbe
     * credere all'utente che il backup sia attivo quando non lo e'.
     */
    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                val prefs = preferencesRepository.getOrCreate()
                preferencesRepository.save(prefs.copy(autoBackupEnabled = enabled))
                AutoBackupWorker.enqueue(context, enabled)
                _state.value = _state.value.copy(
                    autoBackupEnabled = enabled,
                    message = if (enabled) "Backup automatico attivo." else "Backup automatico disattivato."
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    message = "Impostazione non salvata: ${it.message ?: it::class.simpleName}"
                )
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true, message = null)
            try {
                shareBackup(repository.createBackup())
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    working = false,
                    message = "Backup non riuscito: ${t.message}"
                )
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true, message = null)
            try {
                repository.restoreBackup(uri)
                restartApplication()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    working = false,
                    message = "Ripristino non riuscito: ${t.message}"
                )
            }
        }
    }

    private fun shareBackup(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Salva backup FitCoach").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        _state.value = _state.value.copy(working = false, message = "Backup creato: ${file.name}")
    }

    private fun restartApplication() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        if (launchIntent != null) context.startActivity(launchIntent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
