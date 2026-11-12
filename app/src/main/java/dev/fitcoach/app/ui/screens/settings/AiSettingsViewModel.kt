package dev.fitcoach.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.data.ai.AiClient
import dev.fitcoach.app.domain.model.AiConfig
import dev.fitcoach.app.domain.model.AiProvider
import dev.fitcoach.app.domain.model.ConnectionCheck
import dev.fitcoach.app.domain.repository.AiConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSettingsUiState(
    val config: AiConfig = AiConfig(),
    val apiKeyInput: String = "",
    val hasStoredKey: Boolean = false,
    val testing: Boolean = false,
    val testResult: String? = null,
    val availableModels: List<String> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val aiConfigRepository: AiConfigRepository,
    private val aiClient: AiClient
) : ViewModel() {

    private val _state = MutableStateFlow(AiSettingsUiState())
    val state: StateFlow<AiSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val config = aiConfigRepository.getConfig()
            _state.value = _state.value.copy(
                config = config,
                hasStoredKey = aiConfigRepository.hasApiKey(config.provider)
            )
        }
    }

    fun dismissMessage() { _state.value = _state.value.copy(message = null) }

    fun selectProvider(provider: AiProvider) {
        viewModelScope.launch {
            val config = _state.value.config.copy(
                provider = provider,
                baseUrl = provider.defaultBaseUrl,
                modelId = provider.defaultModel
            )
            aiConfigRepository.saveConfig(config)
            _state.value = _state.value.copy(
                config = config,
                hasStoredKey = aiConfigRepository.hasApiKey(provider),
                testResult = null,
                availableModels = emptyList()
            )
        }
    }

    fun updateBaseUrl(value: String) = updateConfig { it.copy(baseUrl = value) }
    fun updateModel(value: String) = updateConfig { it.copy(modelId = value) }
    fun updateTemperature(value: Double) = updateConfig { it.copy(temperature = value) }
    fun updateTopP(value: Double) = updateConfig { it.copy(topP = value) }
    fun updateMaxTokens(value: Int) = updateConfig { it.copy(maxOutputTokens = value) }
    fun updateTimeout(value: Int) = updateConfig { it.copy(timeoutSeconds = value) }
    fun updateJsonMode(value: Boolean) = updateConfig { it.copy(useJsonMode = value) }
    fun updateSendPhotos(value: Boolean) = updateConfig { it.copy(sendPhotosToAi = value) }
    fun updateApiKeyInput(value: String) { _state.value = _state.value.copy(apiKeyInput = value) }

    private fun updateConfig(transform: (AiConfig) -> AiConfig) {
        viewModelScope.launch {
            val config = transform(_state.value.config)
            aiConfigRepository.saveConfig(config)
            _state.value = _state.value.copy(config = config, message = "Configurazione salvata")
        }
    }

    fun saveApiKey() {
        val key = _state.value.apiKeyInput.trim()
        if (key.isBlank()) {
            _state.value = _state.value.copy(testResult = "Inserisci una chiave valida.")
            return
        }
        val provider = _state.value.config.provider
        aiConfigRepository.saveApiKey(provider, key)
        _state.value = _state.value.copy(
            apiKeyInput = "",
            hasStoredKey = true,
            message = "Chiave salvata nel Keystore"
        )
    }

    fun clearApiKey() {
        val provider = _state.value.config.provider
        aiConfigRepository.clearApiKey(provider)
        _state.value = _state.value.copy(hasStoredKey = false, testResult = null, message = "Chiave rimossa")
    }

    fun testConnection() {
        viewModelScope.launch {
            val config = _state.value.config
            val key = aiConfigRepository.getApiKey(config.provider)
            if (key.isNullOrBlank()) {
                _state.value = _state.value.copy(testResult = "Nessuna chiave salvata per ${config.provider.displayName}.")
                return@launch
            }
            _state.value = _state.value.copy(testing = true, testResult = null, availableModels = emptyList())
            val result = aiClient.listModels(config, key)
            val suffix = when (result.check) {
                ConnectionCheck.OK -> {
                    val modelOk = config.modelId.isBlank() || result.availableModels.isEmpty() ||
                        result.availableModels.any { it == config.modelId }
                    if (modelOk) "" else " Il modello '${config.modelId}' non risulta tra quelli disponibili."
                }
                else -> ""
            }
            _state.value = _state.value.copy(
                testing = false,
                testResult = result.message + suffix,
                availableModels = result.availableModels.take(50)
            )
        }
    }
}
