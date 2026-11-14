package com.fitcoach.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ExportUiState(
    val exporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun exportWeights() = export("peso") { exportRepository.exportWeightsCsv() }
    fun exportSessions() = export("allenamenti") { exportRepository.exportSessionsCsv() }
    fun exportPerformance() = export("carichi") { exportRepository.exportPerformanceCsv() }
    fun exportCheckIns() = export("checkin") { exportRepository.exportCheckInsCsv() }
    fun exportAll() = export("fitcoach_dati") { exportRepository.exportAllJson() }

    private fun export(name: String, producer: suspend () -> String) {
        viewModelScope.launch {
            _state.value = ExportUiState(exporting = true)
            try {
                val content = producer()
                val ext = if (name == "fitcoach_dati") "json" else "csv"
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                    File(dir, "${name}_${System.currentTimeMillis()}.$ext").apply { writeText(content) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = if (ext == "json") "application/json" else "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "Esporta $name").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                _state.value = ExportUiState(message = "Esportazione pronta: ${file.name}")
            } catch (t: Throwable) {
                _state.value = ExportUiState(message = "Errore esportazione: ${t.message}")
            }
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
