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
        temperature: Double? = config.temperature,
        maxTokens: Int = config.maxTokens,
        responseFormat: ChatCompletionRequest.ResponseFormat? = null
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

        // Adatta il payload a quanto appreso su questo provider/modello (es. modelli
        // che accettano solo la temperatura di default), per non ripetere ogni volta
        // una chiamata destinata a fallire.
        val learned = learnedParams[config.provider to config.model]
        val effectiveTemperature = if (learned?.dropTemperature == true) null else temperature

        // Costruzione del payload con il parametro token corretto per provider/modello
        // (max_tokens oppure max_completion_tokens, mai entrambi).
        var request = ChatCompletionRequest.forProvider(
            provider = config.provider,
            model = config.model,
            messages = messages,
            temperature = effectiveTemperature,
            maxTokens = maxTokens,
            responseFormat = responseFormat
        )
        if (learned?.alternateTokenParam == true) {
            request = ChatCompletionRequest.withAlternateTokenParam(request)
        }

        // Fallback controllato e limitato: ad ogni passo si corregge UN solo
        // parametro rifiutato (token, poi temperatura). Mai entrambi i parametri
        // token insieme, mai loop infiniti.
        var result = sendOnce(config, request)
        if (result is AiResult.Failure && AiParameterPolicy.isUnsupportedTokenParamError(result.message)) {
            request = ChatCompletionRequest.withAlternateTokenParam(request)
            result = sendOnce(config, request)
            if (result is AiResult.Success) {
                learnedParams[config.provider to config.model] = Learned(dropTemperature = false, alternateTokenParam = true)
            }
        }
        if (result is AiResult.Failure && AiParameterPolicy.isUnsupportedTemperatureError(result.message)) {
            request = ChatCompletionRequest.withoutTemperature(request)
            result = sendOnce(config, request)
            if (result is AiResult.Success) {
                learnedParams[config.provider to config.model] =
                    Learned(dropTemperature = true, alternateTokenParam = learned?.alternateTokenParam == true)
            }
        }
        return result
    }

    /** Parametri corretti appresi per un provider/modello durante la sessione. */
    private data class Learned(val dropTemperature: Boolean, val alternateTokenParam: Boolean)

    private val learnedParams = java.util.concurrent.ConcurrentHashMap<Pair<AiProvider, String>, Learned>()

    private suspend fun sendOnce(
        config: AiProviderConfig,
        request: ChatCompletionRequest
    ): AiResult {
        val auth = config.apiKey?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
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
                else -> when {
                    AiParameterPolicy.isUnsupportedTokenParamError(detail ?: "") ->
                        "Il modello ha rifiutato il parametro dei token anche dopo l'adattamento " +
                            "automatico (max_tokens/max_completion_tokens): verifica il modello."
                    AiParameterPolicy.isUnsupportedTemperatureError(detail ?: "") ->
                        "Il modello non supporta la temperatura impostata (accetta solo il valore " +
                            "di default): rimuovi o reimposta la temperatura nelle impostazioni."
                    else -> null
                }
            }
            val message = listOfNotNull(hint, detail).joinToString(" ").ifBlank {
                "Errore HTTP ${response.code()}."
            }
            return AiResult.Failure(message, response.code())
        }
        val body = response.body()
            ?: return AiResult.Failure("Risposta vuota dal provider.", response.code())
        body.error?.message?.let { return AiResult.Failure(it, response.code()) }

        val message = body.choices.firstOrNull()?.message
        // Rifiuto esplicito del modello (policy di sicurezza del provider).
        message?.refusal?.takeIf { it.isNotBlank() }?.let {
            return AiResult.Failure("Il modello ha rifiutato la richiesta: $it", response.code())
        }
        val content = body.content
        if (content.isNullOrBlank()) {
            val reason = when (body.finishReason) {
                "length" -> "Risposta troncata: aumenta i token massimi o accorcia la richiesta."
                "content_filter" -> "Contenuto filtrato dal provider."
                null -> "Il provider non ha restituito contenuto."
                else -> "Il provider ha restituito una risposta vuota (${body.finishReason})."
            }
            return AiResult.Failure(reason, response.code())
        }
        return AiResult.Success(content.trim(), body.model)
    }

    /** Verifica rapida della connessione con un messaggio minimale. */
    suspend fun testConnection(config: AiProviderConfig): AiResult = complete(
        config = config,
        messages = listOf(
            ChatMessageDto(role = "system", content = "Sei un test di connessione."),
            ChatMessageDto(role = "user", content = "Rispondi esattamente con: OK")
        ),
        // Nessuna temperatura: alcuni modelli (es. gpt-6-luna) accettano solo il
        // valore di default e rifiuterebbero la richiesta.
        temperature = null,
        // Budget sufficiente anche per i modelli di reasoning (che consumano token
        // prima di produrre il testo) e per una risposta breve.
        maxTokens = CONNECTION_TEST_MAX_TOKENS
    )

    companion object {
        private const val OPENROUTER_REFERER = "https://fitcoach.app"
        private const val OPENROUTER_TITLE = "FitCoach"
        private const val CONNECTION_TEST_MAX_TOKENS = 256
    }
}
