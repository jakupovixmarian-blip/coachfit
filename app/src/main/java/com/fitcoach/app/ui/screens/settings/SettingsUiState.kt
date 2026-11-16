package com.fitcoach.app.ui.screens.settings

/**
 * Stato di Impostazioni, compreso il pannello Provider AI.
 *
 * Le chiavi API non transitano mai in chiaro nella UI oltre il campo di input:
 * `apiKeyMasked` è solo la stringa da mostrare (••••), mentre il valore digitato
 * viene consegnato una volta sola a onSaveKey e salvato nel keystore sicuro.
 */
data class SettingsUiState(
    val provider: ProviderUi = ProviderUi(),
    val notificationsEnabled: Boolean = true,
    val exactAlarms: Boolean = true,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val versionLabel: String = "",
    val schemaLabel: String = "",
    val isBackingUp: Boolean = false,
    val message: String? = null,
) {
    val canTestConnection: Boolean get() = provider.isConfigured
}

data class ProviderUi(
    val id: ProviderId = ProviderId.OPENAI,
    val baseUrl: String = "",
    val model: String = "",
    val apiKeyMasked: String = "",
    val status: ProviderStatus = ProviderStatus.UNCONFIGURED,
    val lastCheckedLabel: String? = null,
    val errorMessage: String? = null,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKeyMasked.isNotBlank()

    val statusLabel: String
        get() = when (status) {
            ProviderStatus.UNCONFIGURED -> "Non configurato"
            ProviderStatus.CHECKING -> "Verifica…"
            ProviderStatus.OK -> "Connesso"
            ProviderStatus.ERROR -> "Errore"
        }
}

enum class ProviderId(val label: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", ""),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    CUSTOM("Compatibile OpenAI", "", ""),
}

enum class ProviderStatus { UNCONFIGURED, CHECKING, OK, ERROR }

enum class UnitSystem(val label: String) {
    METRIC("Metrico (kg, cm)"),
    IMPERIAL("Imperiale (lb, in)"),
}
