package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Verifica il comportamento di [AiClient]: il payload contiene un solo parametro
 * token, e in caso di errore "parametro non supportato" esegue UN solo retry
 * controllato con l'altro parametro.
 */
class AiClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private class RecordingApi(
        private val responses: MutableList<Response<ChatCompletionResponse>>,
        val sent: MutableList<ChatCompletionRequest> = mutableListOf(),
        val authorizations: MutableList<String?> = mutableListOf()
    ) : AiApi {
        override suspend fun chatCompletions(
            url: String,
            authorization: String?,
            contentType: String,
            referer: String?,
            title: String?,
            body: ChatCompletionRequest
        ): Response<ChatCompletionResponse> {
            sent += body
            authorizations += authorization
            return responses.removeAt(0)
        }
    }

    private fun okResponse() = Response.success(
        ChatCompletionResponse(
            id = "1",
            model = "test-model",
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(content = kotlinx.serialization.json.JsonPrimitive("OK")),
                    finishReason = "stop"
                )
            )
        )
    )

    private fun errorResponse(code: Int, message: String): Response<ChatCompletionResponse> {
        val body = """{"error":{"message":${'"'}$message${'"'},"type":"invalid_request_error"}}"""
        return Response.error(code, body.toResponseBody("application/json".toMediaType()))
    }

    private fun config(provider: AiProvider, model: String) = AiProviderConfig(
        provider = provider, baseUrl = "https://example.test/v1/", model = model, apiKey = "k"
    )

    @Test
    fun `openai o1 invia solo max_completion_tokens`() = runTest {
        val api = RecordingApi(mutableListOf(okResponse()))
        val client = AiClient(api, json)

        val result = client.complete(config(AiProvider.OPENAI, "o1-mini"), listOf(ChatMessageDto("user", "ciao")))

        assertThat(result).isInstanceOf(AiResult.Success::class.java)
        assertThat(api.sent).hasSize(1)
        val body = api.sent.first()
        assertThat(body.maxCompletionTokens).isEqualTo(1200)
        assertThat(body.maxTokens).isNull()
    }

    @Test
    fun `groq invia solo max_tokens`() = runTest {
        val api = RecordingApi(mutableListOf(okResponse()))
        val client = AiClient(api, json)

        client.complete(config(AiProvider.GROQ, "llama-3.3-70b-versatile"), listOf(ChatMessageDto("user", "ciao")))

        val body = api.sent.first()
        assertThat(body.maxTokens).isEqualTo(1200)
        assertThat(body.maxCompletionTokens).isNull()
    }

    @Test
    fun `retry controllato quando il provider rifiuta max_tokens`() = runTest {
        // Modello non riconosciuto come "nuovo" -> primo tentativo con max_tokens,
        // il provider risponde con errore -> retry con max_completion_tokens.
        val api = RecordingApi(
            mutableListOf(
                errorResponse(400, "Unsupported parameter: 'max_tokens' is not supported with this model. Use 'max_completion_tokens' instead."),
                okResponse()
            )
        )
        val client = AiClient(api, json)

        val result = client.complete(config(AiProvider.OPENAI, "gpt-futuro"), listOf(ChatMessageDto("user", "ciao")))

        assertThat(result).isInstanceOf(AiResult.Success::class.java)
        assertThat(api.sent).hasSize(2)
        assertThat(api.sent[0].maxTokens).isNotNull()
        assertThat(api.sent[0].maxCompletionTokens).isNull()
        assertThat(api.sent[1].maxTokens).isNull()
        assertThat(api.sent[1].maxCompletionTokens).isNotNull()
    }

    @Test
    fun `nessun retry se l'errore non riguarda il parametro token`() = runTest {
        val api = RecordingApi(mutableListOf(errorResponse(401, "Invalid API key")))
        val client = AiClient(api, json)

        val result = client.complete(config(AiProvider.OPENAI, "gpt-4o-mini"), listOf(ChatMessageDto("user", "ciao")))

        assertThat(result).isInstanceOf(AiResult.Failure::class.java)
        assertThat(api.sent).hasSize(1)
    }

    @Test
    fun `backend proxy puo funzionare senza chiave locale`() = runTest {
        val api = RecordingApi(mutableListOf(okResponse()))
        val client = AiClient(api, json)
        val cfg = AiProviderConfig(
            provider = AiProvider.OPENAI,
            baseUrl = "https://proxy.example.test/v1/",
            model = "proxy-model",
            apiKey = null,
            usesBackendProxy = true
        )

        val result = client.complete(cfg, listOf(ChatMessageDto("user", "ciao")))

        assertThat(result).isInstanceOf(AiResult.Success::class.java)
        assertThat(api.authorizations).containsExactly(null)
        assertThat(cfg.isConfigured).isTrue()
    }

    @Test
    fun `test connection usa un solo parametro token e nessuna temperatura`() = runTest {
        val api = RecordingApi(mutableListOf(okResponse()))
        val client = AiClient(api, json)

        client.testConnection(config(AiProvider.OPENAI, "o1"))

        val body = api.sent.first()
        assertThat(body.maxCompletionTokens).isEqualTo(256)
        assertThat(body.maxTokens).isNull()
        assertThat(body.temperature).isNull()
    }

    @Test
    fun `ritenta senza temperatura quando il modello la rifiuta`() = runTest {
        val api = RecordingApi(
            mutableListOf(
                errorResponse(400, "Unsupported value: 'temperature' does not support 0.4 with this model. Only the default (1) value is supported."),
                okResponse()
            )
        )
        val client = AiClient(api, json)

        val result = client.complete(
            config(AiProvider.OPENAI, "gpt-6-luna"),
            listOf(ChatMessageDto("user", "ciao")),
            temperature = 0.4
        )

        assertThat(result).isInstanceOf(AiResult.Success::class.java)
        assertThat(api.sent).hasSize(2)
        assertThat(api.sent[0].temperature).isEqualTo(0.4)
        assertThat(api.sent[1].temperature).isNull()
    }

    @Test
    fun `dopo l'apprendimento non reinvia la temperatura rifiutata`() = runTest {
        val api = RecordingApi(
            mutableListOf(
                errorResponse(400, "Unsupported value: 'temperature' does not support 0.4 with this model."),
                okResponse(),
                okResponse()
            )
        )
        val client = AiClient(api, json)
        val cfg = config(AiProvider.OPENAI, "gpt-6-luna")

        client.complete(cfg, listOf(ChatMessageDto("user", "ciao")), temperature = 0.4)
        client.complete(cfg, listOf(ChatMessageDto("user", "come stai")), temperature = 0.4)

        // 2 chiamate per la prima richiesta (retry), 1 sola per la seconda.
        assertThat(api.sent).hasSize(3)
        assertThat(api.sent[2].temperature).isNull()
    }

    @Test
    fun `con function tools su gpt-5 invia reasoning_effort none`() = runTest {
        val api = RecordingApi(mutableListOf(okResponse()))
        val client = AiClient(api, json)

        client.complete(
            config(AiProvider.OPENAI, "gpt-5.6"),
            listOf(ChatMessageDto("user", "genera")),
            tools = listOf(
                ToolDefinition(function = FunctionDefinition("echo", "eco", kotlinx.serialization.json.JsonObject(emptyMap())))
            )
        )

        val body = api.sent.first()
        assertThat(body.reasoningEffort).isEqualTo("none")
    }

    @Test
    fun `senza function tools non invia reasoning_effort`() = runTest {
        val api = RecordingApi(mutableListOf(okResponse()))
        val client = AiClient(api, json)

        client.complete(config(AiProvider.OPENAI, "gpt-5.6"), listOf(ChatMessageDto("user", "ciao")))

        assertThat(api.sent.first().reasoningEffort).isNull()
    }

    @Test
    fun `ritenta con reasoning_effort none quando il provider rifiuta i tools con reasoning`() = runTest {
        val api = RecordingApi(
            mutableListOf(
                errorResponse(
                    400,
                    "Function tools with reasoning_effort are not supported for gpt-5.6 in " +
                        "/v1/chat/completions. To use function tools, use /v1/responses or set " +
                        "reasoning_effort to \"none\""
                ),
                okResponse()
            )
        )
        val client = AiClient(api, json)

        val result = client.complete(
            config(AiProvider.OPENAI, "gpt-5.6"),
            listOf(ChatMessageDto("user", "genera")),
            tools = listOf(
                ToolDefinition(function = FunctionDefinition("echo", "eco", kotlinx.serialization.json.JsonObject(emptyMap())))
            )
        )

        assertThat(result).isInstanceOf(AiResult.Success::class.java)
        assertThat(api.sent).hasSize(2)
        assertThat(api.sent[1].reasoningEffort).isEqualTo("none")
    }

    @Test
    fun `ritenta con backoff sugli errori transitori e poi riesce`() = runTest {
        val api = RecordingApi(
            mutableListOf(
                errorResponse(503, "Service temporarily unavailable"),
                okResponse()
            )
        )
        val client = AiClient(api, json)

        val result = client.complete(config(AiProvider.GROQ, "llama-3.3-70b-versatile"), listOf(ChatMessageDto("user", "ciao")))

        assertThat(result).isInstanceOf(AiResult.Success::class.java)
        assertThat(api.sent).hasSize(2)
    }

    @Test
    fun `non ritenta sugli errori di configurazione`() = runTest {
        val api = RecordingApi(mutableListOf(errorResponse(401, "Invalid API key")))
        val client = AiClient(api, json)

        val result = client.complete(config(AiProvider.GROQ, "llama-3.3-70b-versatile"), listOf(ChatMessageDto("user", "ciao")))

        assertThat(result).isInstanceOf(AiResult.Failure::class.java)
        assertThat(api.sent).hasSize(1)
    }
}
