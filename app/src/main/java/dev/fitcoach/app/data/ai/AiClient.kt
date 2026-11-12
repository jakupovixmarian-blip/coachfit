package dev.fitcoach.app.data.ai

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.domain.model.AiConfig
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
        jsonMode: Boolean = false
    ): ChatResult = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("model", config.modelId)
            put("temperature", config.temperature)
            put("top_p", config.topP)
            put("max_tokens", config.maxOutputTokens)
            if (jsonMode && config.useJsonMode) {
                put("response_format", buildJsonObject { put("type", "json_object") })
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
            when (r.code) {
                200 -> parseChat(body)
                401, 403 -> throw AppError.Auth("Chiave non valida (HTTP ${r.code})")
                402 -> throw AppError.InsufficientCredits("Crediti insufficienti (HTTP 402)")
                429 -> throw AppError.RateLimit("Limite di richieste raggiunto (HTTP 429)")
                else -> throw AppError.InvalidResponse("HTTP ${r.code}: ${body.take(300)}")
            }
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
