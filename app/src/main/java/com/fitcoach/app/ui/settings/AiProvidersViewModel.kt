package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.domain.coach.CoachService
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiProvidersUiState(
    val loading: Boolean = true,
    val provider: AiProvider = AiProvider.OPENAI,
    val baseUrl: String = "",
    val model: String = "",
    val apiKeyInput: String = "",
    val hasStoredKey: Boolean = false,
    val useBackendProxy: Boolean = false,
    val backendBaseUrl: String = "",
    val temperature: Double = 0.4,
    val maxTokens: Int = 1200,
    val testing: Boolean = false,
    val testStatus: String? = null,
    val testOk: Boolean? = null,
    val message: String? = null
)

@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val secureKeyStore: SecureKeyStore,
    private val coachService: CoachService
) : ViewModel() {

    private val _state = MutableStateFlow(AiProvidersUiState())
    val state: StateFlow<AiProvidersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = chatRepository.getAiSettings()
            _state.value = AiProvidersUiState(
                loading = false,
                provider = s.provider,
                baseUrl = s.baseUrl,
                model = s.model,
                hasStoredKey = secureKeyStore.hasKey(s.provider.name),
                useBackendProxy = s.useBackendProxy,
                backendBaseUrl = s.backendBaseUrl,
                temperature = s.temperature,
                maxTokens = s.maxTokens,
                testOk = s.lastTestOk,
                testStatus = s.lastTestMessage.ifBlank { null }
            )
        }
    }

    fun selectProvider(provider: AiProvider) {
        val current = _state.value
        val keepCustomUrls = current.provider == provider
        _state.value = current.copy(
            provider = provider,
            baseUrl = if (keepCustomUrls) current.baseUrl else provider.defaultBaseUrl,
            model = if (keepCustomUrls) current.model else provider.defaultModel,
            hasStoredKey = secureKeyStore.hasKey(provider.name),
            testStatus = null,
            testOk = null
        )
    }

    fun updateBaseUrl(v: String) { _state.value = _state.value.copy(baseUrl = v) }
    fun updateModel(v: String) { _state.value = _state.value.copy(model = v) }
    fun updateApiKey(v: String) { _state.value = _state.value.copy(apiKeyInput = v) }
    fun updateUseProxy(v: Boolean) { _state.value = _state.value.copy(useBackendProxy = v) }
    fun updateBackendUrl(v: String) { _state.value = _state.value.copy(backendBaseUrl = v) }
    fun updateTemperature(v: Double) { _state.value = _state.value.copy(temperature = v) }
    fun updateMaxTokens(v: Int) { _state.value = _state.value.copy(maxTokens = v) }

    private suspend fun saveCurrent(): AiProviderSettings {
        val s = _state.value
        if (s.apiKeyInput.isNotBlank()) {
            secureKeyStore.putApiKey(s.provider.name, s.apiKeyInput.trim())
        }
        val settings = AiProviderSettings(
            provider = s.provider,
            baseUrl = s.baseUrl.trim().ifBlank { s.provider.defaultBaseUrl },
            model = s.model.trim().ifBlank { s.provider.defaultModel },
            useBackendProxy = s.useBackendProxy,
            backendBaseUrl = s.backendBaseUrl.trim(),
            temperature = s.temperature,
            maxTokens = s.maxTokens
        )
        chatRepository.saveAiSettings(settings)
        _state.value = _state.value.copy(
            apiKeyInput = "",
            hasStoredKey = secureKeyStore.hasKey(s.provider.name)
        )
        return settings
    }

    fun save() {
        viewModelScope.launch {
            saveCurrent()
            _state.value = _state.value.copy(message = "Impostazioni salvate.")
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            secureKeyStore.clear(_state.value.provider.name)
            _state.value = _state.value.copy(
                hasStoredKey = false, apiKeyInput = "", message = "Chiave rimossa."
            )
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(testing = true, testStatus = null, testOk = null)
            saveCurrent()
            val result = coachService.testConnection()
            val settings = chatRepository.getAiSettings()
            when (result) {
                is AiResult.Success -> {
                    chatRepository.saveAiSettings(
                        settings.copy(
                            lastTestOk = true,
                            lastTestMessage = "Connessione riuscita (${result.model ?: "modello ok"}).",
                            lastTestAt = System.currentTimeMillis()
                        )
                    )
                    _state.value = _state.value.copy(
                        testing = false, testOk = true,
                        testStatus = "Connessione riuscita con ${_state.value.provider.label}."
                    )
                }
                is AiResult.Failure -> {
                    chatRepository.saveAiSettings(
                        settings.copy(
                            lastTestOk = false,
                            lastTestMessage = result.message,
                            lastTestAt = System.currentTimeMillis()
                        )
                    )
                    _state.value = _state.value.copy(
                        testing = false, testOk = false, testStatus = result.message
                    )
                }
            }
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
