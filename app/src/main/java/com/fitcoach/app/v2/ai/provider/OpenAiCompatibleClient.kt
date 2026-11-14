package com.fitcoach.app.v2.ai.provider

import com.fitcoach.app.v2.ai.transport.AiTransport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Provider client 2.0. It owns no persistence and receives a transport port.
 * It never exposes reasoning content as assistant content.
 */
class OpenAiCompatibleClient @Inject constructor(
    private val transport: AiTransport,
    private val json: Json,
    private val credentials: CredentialStore
) {
    suspend fun complete(configuration: ProviderConfiguration, request: AiRequest): AiCompletion {
        val definition = ProviderCatalog.forKind(configuration.provider)
        val capabilities = definition.capabilitiesFor(configuration.model)
        if (configuration.baseUrl.isBlank() || configuration.model.isBlank()) {
            return failure(FailureReason.CONFIGURATION, "Provider o modello non configurato.")
        }
        if (configuration.maxOutputTokens !in 1..128_000 || configuration.timeoutMs !in 1_000..300_000) {
            return failure(FailureReason.CONFIGURATION, "Limiti provider non validi.")
        }
        val key = configuration.credentialRef?.let { credentials.read(it) }
        if (!configuration.usesBackendProxy && configuration.provider != ProviderKind.CUSTOM && key.isNullOrBlank()) {
            return failure(FailureReason.AUTHENTICATION, "Credenziale provider mancante.")
        }
        val headers = buildMap {
            put("Content-Type", "application/json")
            key?.let { put("Authorization", "Bearer $it") }
            if (configuration.provider == ProviderKind.OPENROUTER) {
                put("HTTP-Referer", "https://fitcoach.app")
                put("X-Title", "FitCoach")
            }
        }
        var body = encodeRequest(capabilities, configuration, request)
        var response = try {
            transport.post(configuration.endpoint(), headers, body, configuration.timeoutMs)
        } catch (_: SocketTimeoutException) {
            return failure(FailureReason.TIMEOUT, "Timeout durante la chiamata AI.")
        } catch (_: IOException) {
            return failure(FailureReason.NETWORK, "Errore di rete durante la chiamata AI.")
        }
        var result = decodeCompletion(response)
        if (result is AiCompletion.Failure && isCompatibilityParameterError(result.message)) {
            val retryCapabilities = if (isTokenParameterError(result.message)) {
                capabilities.copy(tokenField = capabilities.tokenField.other())
            } else {
                capabilities
            }
            val retryRequest = if (isTemperatureParameterError(result.message)) {
                request.copy(temperature = null)
            } else {
                request
            }
            body = encodeRequest(retryCapabilities, configuration, retryRequest)
            response = try {
                transport.post(configuration.endpoint(), headers, body, configuration.timeoutMs)
            } catch (_: SocketTimeoutException) {
                return failure(FailureReason.TIMEOUT, "Timeout durante il retry AI.")
            } catch (_: IOException) {
                return failure(FailureReason.NETWORK, "Errore di rete durante il retry AI.")
            }
            result = decodeCompletion(response)
        }
        return result
    }

    suspend fun testConnection(configuration: ProviderConfiguration): ConnectionTest {
        val started = System.currentTimeMillis()
        val result = complete(configuration, AiRequest(user = "Rispondi esattamente: OK"))
        return when (result) {
            is AiCompletion.Success -> ConnectionTest.Success(
                model = result.model ?: configuration.model,
                latencyMs = (System.currentTimeMillis() - started).coerceAtLeast(result.usage?.totalTokens?.toLong() ?: 0L)
            )
            is AiCompletion.Failure -> ConnectionTest.Failure(result)
            is AiCompletion.Truncated -> ConnectionTest.Failure(
                AiCompletion.Failure(FailureReason.EMPTY_RESPONSE, "Il test provider è stato troncato.")
            )
            is AiCompletion.Refused -> ConnectionTest.Failure(
                AiCompletion.Failure(FailureReason.UNKNOWN, "Il provider ha rifiutato il test: ${result.message}")
            )
            is AiCompletion.ToolCalls -> ConnectionTest.Failure(
                AiCompletion.Failure(FailureReason.UNKNOWN, "Il provider ha restituito tool call nel test.")
            )
        }
    }

    suspend fun discoverModels(configuration: ProviderConfiguration): Result<List<ModelDescriptor>> {
        val definition = ProviderCatalog.forKind(configuration.provider)
        if (!definition.capabilities.supportsModelDiscovery) return Result.failure(IllegalStateException("Provider senza discovery"))
        val key = configuration.credentialRef?.let { credentials.read(it) }
        if (!configuration.usesBackendProxy && configuration.provider != ProviderKind.CUSTOM && key.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Credenziale provider mancante"))
        }
        val headers = buildMap {
            key?.let { put("Authorization", "Bearer $it") }
            put("Accept", "application/json")
        }
        val response = runCatching {
            transport.get(configuration.endpoint("/models"), headers, configuration.timeoutMs)
        }.getOrElse { return Result.failure(it) }
        if (response.statusCode !in 200..299 || response.body.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Discovery modelli HTTP ${response.statusCode}"))
        }
        return runCatching {
            json.decodeFromString<ModelListResponse>(response.body!!).data
                .filter { it.id.isNotBlank() }
                .map { ModelDescriptor(it.id, it.ownedBy) }
        }
    }

    private fun encodeRequest(
        capabilities: ProviderCapabilities,
        configuration: ProviderConfiguration,
        request: AiRequest
    ): String = buildJsonObject {
        put("model", configuration.model)
        putJsonArray("messages") {
            request.system?.let { add(buildJsonObject { put("role", "system"); put("content", it) }) }
            add(buildJsonObject { put("role", "user"); put("content", request.user) })
        }
        when (capabilities.tokenField) {
            TokenField.MAX_TOKENS -> put("max_tokens", configuration.maxOutputTokens)
            TokenField.MAX_COMPLETION_TOKENS -> put("max_completion_tokens", configuration.maxOutputTokens)
        }
        if (capabilities.supportsTemperature) request.temperature?.let { put("temperature", it) }
        if (capabilities.supportsJsonObject && request.jsonObject) {
            putJsonObject("response_format") { put("type", "json_object") }
        }
        request.toolsJson?.let { raw ->
            if (capabilities.supportsFunctionCalling) {
                put("tools", json.parseToJsonElement(raw))
            }
        }
    }.toString()

    private fun isCompatibilityParameterError(message: String): Boolean =
        isTokenParameterError(message) || isTemperatureParameterError(message)

    private fun isTokenParameterError(message: String): Boolean {
        val normalized = message.lowercase()
        return (normalized.contains("max_tokens") || normalized.contains("max_completion_tokens")) &&
            listOf("unsupported", "not supported", "unknown", "invalid").any(normalized::contains)
    }

    private fun isTemperatureParameterError(message: String): Boolean {
        val normalized = message.lowercase()
        return normalized.contains("temperature") &&
            listOf("unsupported", "not supported", "unknown", "invalid").any(normalized::contains)
    }

    private fun TokenField.other(): TokenField = when (this) {
        TokenField.MAX_TOKENS -> TokenField.MAX_COMPLETION_TOKENS
        TokenField.MAX_COMPLETION_TOKENS -> TokenField.MAX_TOKENS
    }


    private fun decodeCompletion(response: com.fitcoach.app.v2.ai.transport.TransportResponse): AiCompletion {
        if (response.statusCode !in 200..299) {
            val detail = response.body?.let { parseError(it) } ?: "Risposta HTTP ${response.statusCode}."
            return failure(classifyStatus(response.statusCode, detail), detail, response.statusCode)
        }
        val raw = response.body ?: return failure(FailureReason.EMPTY_RESPONSE, "Risposta vuota dal provider.")
        val decoded = runCatching { json.decodeFromString<CompletionResponse>(raw) }
            .getOrElse { return failure(FailureReason.INVALID_JSON, "Risposta provider non valida.") }
        val choice = decoded.choices.firstOrNull()
            ?: return failure(FailureReason.EMPTY_RESPONSE, "Il provider non ha restituito scelte.")
        choice.message?.refusal?.takeIf { it.isNotBlank() }?.let {
            return AiCompletion.Refused(it, decoded.model)
        }
        if (!choice.message?.toolCalls.isNullOrEmpty()) {
            return AiCompletion.ToolCalls(
                rawJson = json.encodeToString(ListSerializerHolder.serializer, choice.message!!.toolCalls!!),
                model = decoded.model
            )
        }
        val content = choice.message?.content
        if (choice.finishReason == "length") return AiCompletion.Truncated(content, decoded.model)
        if (content.isNullOrBlank()) return failure(FailureReason.EMPTY_RESPONSE, "Il provider non ha restituito testo.")
        return AiCompletion.Success(content, decoded.model, decoded.usage?.toTokenUsage())
    }

    private fun parseError(raw: String): String = runCatching {
        json.decodeFromString<ErrorEnvelope>(raw).error?.message
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Errore del provider."

    private fun classifyStatus(status: Int, detail: String): FailureReason = when {
        status == 401 -> FailureReason.AUTHENTICATION
        status == 403 -> FailureReason.AUTHORIZATION
        status == 404 && detail.contains("model", ignoreCase = true) -> FailureReason.MODEL_NOT_FOUND
        status == 404 -> FailureReason.INVALID_REQUEST
        status == 408 -> FailureReason.TIMEOUT
        status == 402 -> FailureReason.CREDITS
        status == 429 -> FailureReason.RATE_LIMIT
        status >= 500 -> FailureReason.SERVER
        status in 400..499 -> FailureReason.INVALID_REQUEST
        else -> FailureReason.UNKNOWN
    }

    private fun failure(reason: FailureReason, message: String, code: Int? = null) =
        AiCompletion.Failure(reason, message, code)

    @Serializable
    private data class CompletionResponse(
        val model: String? = null,
        val choices: List<Choice> = emptyList(),
        val usage: Usage? = null
    )

    @Serializable
    private data class Choice(
        val message: Message? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    private data class Message(
        val content: String? = null,
        val refusal: String? = null,
        @SerialName("tool_calls") val toolCalls: List<ToolCall> = emptyList()
    )

    @Serializable
    private data class ToolCall(val id: String? = null, val type: String? = null, val function: FunctionCall? = null)

    @Serializable
    private data class FunctionCall(val name: String? = null, val arguments: String? = null)

    @Serializable
    private data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0,
        @SerialName("total_tokens") val totalTokens: Int = 0
    ) {
        fun toTokenUsage() = TokenUsage(promptTokens, completionTokens, totalTokens)
    }

    @Serializable
    private data class ModelListResponse(val data: List<ModelResponse> = emptyList())

    @Serializable
    private data class ModelResponse(val id: String = "", @SerialName("owned_by") val ownedBy: String? = null)

    @Serializable
    private data class ErrorEnvelope(val error: ErrorBody? = null)

    @Serializable
    private data class ErrorBody(val message: String? = null)

    private object ListSerializerHolder {
        val serializer = kotlinx.serialization.builtins.ListSerializer(ToolCall.serializer())
    }
}
