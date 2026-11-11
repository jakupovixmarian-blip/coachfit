package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Esito di una chiamata al provider AI. */
sealed interface AiResult {
    data class Success(val content: String, val model: String?) : AiResult
    data class Failure(val message: String, val httpCode: Int? = null) : AiResult
}

/**
 * Client AI indipendente dal provider. Tutti i provider supportati espongono
 * l'API Chat Completions compatibile OpenAI, quindi cambia solo base URL,
 * modello e chiave.
 */
@Singleton
class AiClient @Inject constructor(
    private val api: AiApi,
    private val json: Json
) {
    suspend fun complete(
        config: AiProviderConfig,
        messages: List<ChatMessageDto>,
        temperature: Double = 0.4,
        maxTokens: Int = 1200
    ): AiResult {
        if (config.baseUrl.isBlank()) {
            return AiResult.Failure("Base URL non configurato. Vai in Impostazioni > Provider AI.")
        }
        if (config.model.isBlank()) {
            return AiResult.Failure("Modello non configurato. Vai in Impostazioni > Provider AI.")
        }
        if (config.provider != AiProvider.CUSTOM && config.apiKey.isNullOrBlank()) {
            return AiResult.Failure("API key mancante per ${config.provider.label}.")
        }

        val auth = config.apiKey?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val request = ChatCompletionRequest(
            model = config.model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens
        )

        return try {
            val response = api.chatCompletions(
                url = config.completionsUrl,
                authorization = auth,
                referer = if (config.provider == AiProvider.OPENROUTER) OPENROUTER_REFERER else null,
                title = if (config.provider == AiProvider.OPENROUTER) OPENROUTER_TITLE else null,
                body = request
            )
            handleResponse(response)
        } catch (io: IOException) {
            AiResult.Failure("Impossibile raggiungere il provider. Controlla la connessione Internet e il Base URL.")
        } catch (t: Throwable) {
            AiResult.Failure("Errore durante la chiamata al provider: ${t.message ?: t::class.simpleName}")
        }
    }

    private fun handleResponse(response: Response<ChatCompletionResponse>): AiResult {
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string()
            val parsed = body?.let { runCatching { json.decodeFromString<ChatCompletionResponse>(it) }.getOrNull() }
            val detail = parsed?.error?.message ?: body?.take(300)
            val hint = when (response.code()) {
                401 -> "Chiave API non valida o non autorizzata."
                402 -> "Credito esaurito sul provider."
                403 -> "Accesso negato dal provider."
                404 -> "Endpoint non trovato: verifica Base URL e modello."
                429 -> "Troppe richieste o quota superata. Riprova tra poco."
                else -> null
            }
            val message = listOfNotNull(hint, detail).joinToString(" ").ifBlank {
                "Errore HTTP ${response.code()}."
            }
            return AiResult.Failure(message, response.code())
        }
        val body = response.body()
            ?: return AiResult.Failure("Risposta vuota dal provider.", response.code())
        val content = body.content
            ?: return AiResult.Failure(body.error?.message ?: "Il provider non ha restituito contenuto.")
        return AiResult.Success(content.trim(), body.model)
    }

    /** Verifica rapida della connessione con un messaggio minimale. */
    suspend fun testConnection(config: AiProviderConfig): AiResult = complete(
        config = config,
        messages = listOf(
            ChatMessageDto(role = "system", content = "Sei un test di connessione."),
            ChatMessageDto(role = "user", content = "Rispondi esattamente con: OK")
        ),
        temperature = 0.0,
        maxTokens = 16
    )

    companion object {
        private const val OPENROUTER_REFERER = "https://fitcoach.app"
        private const val OPENROUTER_TITLE = "FitCoach"
    }
}
