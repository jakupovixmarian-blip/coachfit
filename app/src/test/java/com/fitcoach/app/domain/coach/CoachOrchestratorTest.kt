package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.ChatCompletionRequest
import com.fitcoach.app.data.remote.ChatCompletionResponse
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.Schema
import com.fitcoach.app.domain.model.AiProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Test
import retrofit2.Response

/**
 * Verifica il ciclo agentico: il modello richiede uno strumento, l'orchestratore
 * lo esegue sui servizi reali e restituisce il risultato, poi il modello produce
 * la risposta finale. Verifica anche il fallback quando i tool non sono supportati.
 */
class CoachOrchestratorTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** Strumento di lettura che ritorna un valore fisso. */
    private class EchoTool : CoachTool {
        var called = false
        override val name = "echo"
        override val description = "ritorna un valore"
        override val parameters: JsonObject = Schema.obj()
        override suspend fun execute(arguments: JsonObject): String {
            called = true
            return """{"ok":true,"valore":42}"""
        }
    }

    /** Strumento di scrittura che registra una proposta (changeId=7). */
    private class WriteTool : CoachTool {
        override val name = "propose_x"
        override val description = "propone"
        override val parameters: JsonObject = Schema.obj()
        override val isWrite = true
        override suspend fun execute(arguments: JsonObject): String =
            """{"ok":true,"changeId":7,"stato":"PROPOSTA_REGISTRATA"}"""
    }

    private class ScriptedApi(
        private val responses: MutableList<Response<ChatCompletionResponse>>
    ) : AiApi {
        val sent = mutableListOf<ChatCompletionRequest>()
        override suspend fun chatCompletions(
            url: String,
            authorization: String?,
            contentType: String,
            referer: String?,
            title: String?,
            body: ChatCompletionRequest
        ): Response<ChatCompletionResponse> {
            sent += body
            return responses.removeAt(0)
        }
    }

    private fun config() = AiProviderConfig(
        provider = AiProvider.OPENAI, baseUrl = "https://api.openai.com/v1/",
        model = "gpt-4o-mini", apiKey = "k"
    )

    private fun toolCallResponse(name: String, args: String) = Response.success(
        ChatCompletionResponse(
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(
                        content = null,
                        toolCalls = listOf(
                            ChatCompletionResponse.ToolCall(
                                id = "c1", type = "function",
                                function = ChatCompletionResponse.FunctionCall(name = name, arguments = args)
                            )
                        )
                    ),
                    finishReason = "tool_calls"
                )
            )
        )
    )

    private fun textResponse(text: String) = Response.success(
        ChatCompletionResponse(
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(content = JsonPrimitive(text)),
                    finishReason = "stop"
                )
            )
        )
    )

    @Test
    fun `read tool is executed and result fed back to the model`() = runTest {
        val echo = EchoTool()
        val api = ScriptedApi(mutableListOf(toolCallResponse("echo", "{}"), textResponse("Fatto.")))
        val registry = CoachToolRegistry(setOf(echo))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(config(), "sys", "ctx", emptyList(), "ciao")

        assertThat(echo.called).isTrue()
        assertThat(result.text).isEqualTo("Fatto.")
        assertThat(result.toolTrace).contains("echo")
        // Il secondo turno deve contenere il messaggio role=tool con il risultato.
        val second = json.encodeToString(ChatCompletionRequest.serializer(), api.sent[1])
        assertThat(second).contains("\"role\":\"tool\"")
        assertThat(second).contains("\"tool_call_id\":\"c1\"")
        assertThat(second).contains("42")
    }

    @Test
    fun `write tool proposal id is extracted`() = runTest {
        val write = WriteTool()
        val api = ScriptedApi(mutableListOf(toolCallResponse("propose_x", "{}"), textResponse("Propongo.")))
        val registry = CoachToolRegistry(setOf(write))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(config(), "sys", "ctx", emptyList(), "cambia")

        assertThat(result.pendingChangeIds).contains(7L)
        assertThat(registry.writeToolNames).contains("propose_x")
    }

    @Test
    fun `unknown tool returns structured error without crashing`() = runTest {
        val api = ScriptedApi(mutableListOf(toolCallResponse("does_not_exist", "{}"), textResponse("Ok.")))
        val registry = CoachToolRegistry(setOf(EchoTool()))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(config(), "sys", "ctx", emptyList(), "x")
        assertThat(result.text).isEqualTo("Ok.")
    }

    @Test
    fun `tool unsupported error falls back to plain completion`() = runTest {
        val errorBody = """{"error":{"message":"tools is not supported for this model"}}"""
        val api = ScriptedApi(
            mutableListOf(
                Response.error(400, errorBody.toResponseBody()),
                textResponse("Risposta senza strumenti.")
            )
        )
        val registry = CoachToolRegistry(setOf(EchoTool()))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(config(), "sys", "ctx", emptyList(), "x")
        assertThat(result.text).isEqualTo("Risposta senza strumenti.")
        assertThat(result.providerError).isNull()
    }

    private fun String.toResponseBody() =
        okhttp3.ResponseBody.create("application/json".toMediaType(), this)
}
