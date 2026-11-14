package com.fitcoach.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.DatabaseBackupRepository
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
    val message: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DatabaseBackupRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun createBackup() {
        viewModelScope.launch {
            _state.value = BackupUiState(working = true)
            try {
                shareBackup(repository.createBackup())
            } catch (t: Throwable) {
                _state.value = BackupUiState(message = "Backup non riuscito: ${t.message}")
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupUiState(working = true)
            try {
                repository.restoreBackup(uri)
                restartApplication()
            } catch (t: Throwable) {
                _state.value = BackupUiState(message = "Ripristino non riuscito: ${t.message}")
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
        _state.value = BackupUiState(message = "Backup creato: ${file.name}")
    }

    private fun restartApplication() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        if (launchIntent != null) context.startActivity(launchIntent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
