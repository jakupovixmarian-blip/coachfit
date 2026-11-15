package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Verifica il supporto al tool calling nel transport: i tool vengono inviati nel
 * payload e le tool_calls della risposta vengono esposte come DTO di richiesta,
 * anche quando il contenuto testuale e vuoto.
 */
class AiToolCallingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private class RecordingApi(
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
        provider = AiProvider.OPENAI,
        baseUrl = "https://api.openai.com/v1/",
        model = "gpt-4o-mini",
        apiKey = "test-key"
    )

    private fun toolDef(name: String) = ToolDefinition(
        function = FunctionDefinition(
            name = name,
            description = "test",
            parameters = buildJsonObject { put("type", JsonPrimitive("object")) }
        )
    )

    @Test
    fun `tools and tool_choice are serialized into the request`() = runTest {
        val api = RecordingApi(mutableListOf(okResponse("ok")))
        val client = AiClient(api, json)

        client.complete(
            config(),
            listOf(ChatMessageDto(role = "user", content = "ciao")),
            tools = listOf(toolDef("get_anamnesis")),
            toolChoice = ToolChoice.auto
        )

        val payload = json.encodeToString(ChatCompletionRequest.serializer(), api.sent.first())
        assertThat(payload).contains("\"tools\"")
        assertThat(payload).contains("get_anamnesis")
        assertThat(payload).contains("\"tool_choice\":\"auto\"")
    }

    @Test
    fun `tool calls are surfaced even with empty content`() = runTest {
        val api = RecordingApi(
            mutableListOf(
                Response.success(
                    ChatCompletionResponse(
                        model = "gpt-4o-mini",
                        choices = listOf(
                            ChatCompletionResponse.Choice(
                                index = 0,
                                message = ChatCompletionResponse.Message(
                                    content = null,
                                    toolCalls = listOf(
                                        ChatCompletionResponse.ToolCall(
                                            id = "call_1",
                                            type = "function",
                                            function = ChatCompletionResponse.FunctionCall(
                                                name = "list_foods",
                                                arguments = """{"limit":5}"""
                                            )
                                        )
                                    )
                                ),
                                finishReason = "tool_calls"
                            )
                        )
                    )
                )
            )
        )
        val client = AiClient(api, json)
        val result = client.complete(config(), listOf(ChatMessageDto(role = "user", content = "ciao")))

        assertThat(result).isInstanceOf(AiResult.Success::class.java)
        val success = result as AiResult.Success
        assertThat(success.toolCalls).hasSize(1)
        assertThat(success.toolCalls.first().function.name).isEqualTo("list_foods")
        assertThat(success.toolCalls.first().function.arguments).contains("limit")
    }

    @Test
    fun `tool result message carries tool_call_id and name`() {
        val msg = toolResultMessage("call_1", "list_foods", """{"ok":true}""")
        assertThat(msg.role).isEqualTo("tool")
        assertThat(msg.toolCallId).isEqualTo("call_1")
        assertThat(msg.name).isEqualTo("list_foods")
        val encoded = json.encodeToString(ChatMessageDto.serializer(), msg)
        assertThat(encoded).contains("\"tool_call_id\":\"call_1\"")
    }

    private fun okResponse(text: String) = Response.success(
        ChatCompletionResponse(
            model = "gpt-4o-mini",
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(content = JsonPrimitive(text)),
                    finishReason = "stop"
                )
            )
        )
    )
}
