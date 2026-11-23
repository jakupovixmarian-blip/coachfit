package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider

/** Configurazione effettiva di un provider AI, con credenziale risolta. */
data class AiProviderConfig(
    val provider: AiProvider,
    val baseUrl: String,
    val model: String,
    val apiKey: String?,
    val temperature: Double = 0.4,
        /**
     * Token massimi di risposta. Con i modelli "reasoning" il budget copre anche
     * il ragionamento interno: a 1200 il modello veniva troncato a meta' frase
     * mentre spiegava la richiesta, quindi non arrivava mai a chiamare lo
     * strumento e l'utente vedeva "ho analizzato..." senza alcuna modifica.
     */
    val maxTokens: Int = 4000,
    val usesBackendProxy: Boolean = false
) {
    /** URL completo dell'endpoint Chat Completions. */
    val completionsUrl: String
        get() {
            val base = baseUrl.trim().trimEnd('/')
            return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        }

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() &&
            (usesBackendProxy || provider == AiProvider.CUSTOM || !apiKey.isNullOrBlank())
}
