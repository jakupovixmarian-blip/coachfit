package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider

/** Configurazione effettiva di un provider AI, con credenziale risolta. */
data class AiProviderConfig(
    val provider: AiProvider,
    val baseUrl: String,
    val model: String,
    val apiKey: String?
) {
    /** URL completo dell'endpoint Chat Completions. */
    val completionsUrl: String
        get() {
            val base = baseUrl.trim().trimEnd('/')
            return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        }

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() &&
            (provider == AiProvider.CUSTOM || !apiKey.isNullOrBlank())
}
