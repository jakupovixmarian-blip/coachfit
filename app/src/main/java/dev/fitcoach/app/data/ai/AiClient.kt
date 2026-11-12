package dev.fitcoach.app.data.ai

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.domain.model.AiConfig
import dev.fitcoach.app.domain.model.AiModelCapabilities
import dev.fitcoach.app.domain.model.ConnectionCheck
import dev.fitcoach.app.domain.model.ConnectionTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(val role: String, val content: String)

data class ChatResult(val content: String, val finishReason: String?)

/**
 * OpenAI-compatible chat client. Works with OpenAI, OpenRouter, DeepSeek, Groq
 * and any provider exposing the /chat/completions contract.
 *
 * API keys are provided by the caller and never persisted here.
 */
@Singleton
class AiClient @Inject constructor(
    private val baseClient: OkHttpClient,
    private val json: Json
) {

    private fun clientFor(config: AiConfig): OkHttpClient =
        baseClient.newBuilder()
            .callTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

    suspend fun listModels(config: AiConfig, apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val url = config.effectiveBaseUrl() + "/models"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        try {
            clientFor(config).newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when (response.code) {
                    200 -> {
                        val models = runCatching {
                            json.parseToJsonElement(body).jsonObject["data"]?.jsonArray
                                ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                                .orEmpty()
                        }.getOrDefault(emptyList())
                        if (models.isEmpty()) {
                            ConnectionTestResult(ConnectionCheck.UNKNOWN, "Autenticazione OK ma nessun modello elencato.")
                        } else {
                            ConnectionTestResult(ConnectionCheck.OK, "Autenticazione OK. ${models.size} modelli disponibili.", models)
                        }
                    }
                    401, 403 -> ConnectionTestResult(ConnectionCheck.AUTH_ERROR, "Chiave non valida o non autorizzata (HTTP ${response.code}).")
                    429 -> ConnectionTestResult(ConnectionCheck.RATE_LIMIT, "Limite di richieste raggiunto (HTTP 429).")
                    else -> ConnectionTestResult(ConnectionCheck.UNKNOWN, "Risposta inattesa HTTP ${response.code}: ${body.take(200)}")
                }
            }
        } catch (io: IOException) {
            ConnectionTestResult(ConnectionCheck.NETWORK_ERROR, "Connessione non riuscita: ${io.message}")
        } catch (t: Throwable) {
            ConnectionTestResult(ConnectionCheck.UNKNOWN, t.message ?: "Errore sconosciuto")
        }
    }

    suspend fun chat(
        config: AiConfig,
        apiKey: String,
        messages: List<ChatMessage>,
        jsonMode: Boolean = false,
        reasoningEffort: String? = null
    ): ChatResult = withContext(Dispatchers.IO) {
        val capabilities = AiModelCapabilities.forModel(config.provider, config.modelId)
        executeChat(config, apiKey, messages, jsonMode, reasoningEffort, capabilities)
    }

    private fun executeChat(
        config: AiConfig,
        apiKey: String,
        messages: List<ChatMessage>,
        jsonMode: Boolean,
        reasoningEffort: String?,
        capabilities: AiModelCapabilities
    ): ChatResult {
        val payload = buildChatPayload(config, messages, jsonMode, reasoningEffort, capabilities)
        val request = Request.Builder()
            .url(config.effectiveBaseUrl() + "/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = try {
            clientFor(config).newCall(request).execute()
        } catch (io: java.net.SocketTimeoutException) {
            throw AppError.Timeout("Timeout dopo ${config.timeoutSeconds}s", io)
        } catch (io: IOException) {
            throw AppError.Network("Connessione non riuscita: ${io.message}", io)
        }

        response.use { r ->
            val body = r.body?.string().orEmpty()
            return when (r.code) {
                200 -> parseChat(body)
                400 -> throw mapBadRequest(body, capabilities)
                401, 403 -> throw AppError.Auth("Chiave non valida (HTTP ${r.code})")
                402 -> throw AppError.InsufficientCredits("Crediti insufficienti (HTTP 402)")
                404 -> throw AppError.ModelMissing("Modello o endpoint non trovato (HTTP 404): ${body.take(200)}")
                429 -> throw AppError.RateLimit("Limite di richieste raggiunto (HTTP 429)")
                else -> throw AppError.InvalidResponse("HTTP ${r.code}: ${body.take(300)}")
            }
        }
    }

    /**
     * Builds the request body from the capabilities of the selected model.
     *
     * The token budget is sent under the parameter the model actually accepts, and
     * sampling/JSON parameters are omitted for models that reject them, so a
     * request is never rejected for an unsupported parameter.
     */
    fun buildChatPayload(
        config: AiConfig,
        messages: List<ChatMessage>,
        jsonMode: Boolean,
        reasoningEffort: String?,
        capabilities: AiModelCapabilities
    ): JsonObject = buildJsonObject {
        put("model", config.modelId)
        if (!capabilities.rejectsSamplingParameters) {
            put("temperature", config.temperature)
            put("top_p", config.topP)
        }
        val budget = AiModelCapabilities.clampOutputTokens(config.maxOutputTokens, capabilities)
        put(capabilities.tokenParameterName, budget)
        if (jsonMode && config.useJsonMode && !capabilities.rejectsJsonMode) {
            put("response_format", buildJsonObject { put("type", "json_object") })
        }
        if (reasoningEffort != null && capabilities.supportsReasoningEffort) {
            put("reasoning_effort", reasoningEffort)
        }
        put("messages", buildJsonArray {
            messages.forEach { m ->
                add(buildJsonObject {
                    put("role", m.role)
                    put("content", m.content)
                })
            }
        })
    }

    /**
     * Turns a 400 into a precise error. When the provider names a parameter the
     * model does not support, the message says which one, instead of a generic
     * "HTTP 400".
     */
    private fun mapBadRequest(body: String, capabilities: AiModelCapabilities): AppError {
        val message = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull().orEmpty()
        val lower = message.lowercase()
        return when {
            lower.contains("max_tokens") || lower.contains("max_completion_tokens") ->
                AppError.InvalidResponse(
                    "Il modello non accetta '${capabilities.tokenParameterName}': $message"
                )
            lower.contains("temperature") || lower.contains("top_p") ->
                AppError.InvalidResponse("Il modello non accetta i parametri di campionamento: $message")
            lower.contains("response_format") ->
                AppError.InvalidResponse("Il modello non accetta il formato JSON richiesto: $message")
            else -> AppError.InvalidResponse(message.ifBlank { "Richiesta rifiutata (HTTP 400): ${body.take(300)}" })
        }
    }

    private fun parseChat(body: String): ChatResult {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }
            .getOrElse { throw AppError.InvalidResponse("JSON non valido") }
        root["error"]?.let {
            val message = runCatching { it.jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
            throw AppError.InvalidResponse(message ?: "Errore dal provider")
        }
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw AppError.InvalidResponse("Nessuna scelta nella risposta")
        val content = choice["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw AppError.InvalidResponse("Contenuto mancante nella risposta")
        val finish = choice["finish_reason"]?.jsonPrimitive?.content
        if (finish == "length") {
            throw AppError.Truncated(
                "Il modello ha esaurito lo spazio di output: riduci il piano o alza il limite di token."
            )
        }
        return ChatResult(content, finish)
    }

    /** Extracts the first JSON object found in a possibly chatty model response. */
    fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start >= 0 && end > start) return raw.substring(start, end + 1)
        throw AppError.InvalidResponse("Nessun oggetto JSON nella risposta")
    }
}
