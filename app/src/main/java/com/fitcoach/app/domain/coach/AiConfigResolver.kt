package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Risolve la configurazione effettiva del provider AI dalle impostazioni dell'utente,
 * risolvendo la credenziale (chiave API locale oppure backend proxy senza chiave sul
 * dispositivo). Unico punto di risoluzione, condiviso da chat, generazione e check-in.
 */
@Singleton
class AiConfigResolver @Inject constructor(
    private val chatRepository: ChatRepository,
    private val secureKeyStore: SecureKeyStore
) {
    suspend fun resolve(): AiProviderConfig? {
        val settings = chatRepository.getAiSettings()
        val provider = settings.provider
        val baseUrl = when {
            settings.useBackendProxy && settings.backendBaseUrl.isNotBlank() -> settings.backendBaseUrl
            else -> settings.baseUrl.ifBlank { provider.defaultBaseUrl }
        }
        val model = settings.model.ifBlank { provider.defaultModel }
        val key = if (settings.useBackendProxy) {
            null
        } else {
            // Un guasto del keystore non deve far crashare la chat: equivale a "nessuna chiave".
            runCatching { secureKeyStore.getApiKey(provider.name) }.getOrNull()
        }
        return AiProviderConfig(
            provider = provider,
            baseUrl = baseUrl,
            model = model,
            apiKey = key,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
            usesBackendProxy = settings.useBackendProxy
        )
    }

    suspend fun resolveConfigured(): AiProviderConfig? = resolve()?.takeIf { it.isConfigured }
}
