package com.fitcoach.app.support

import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.ChatCompletionRequest
import com.fitcoach.app.data.remote.ChatCompletionResponse
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Provider AI "scriptato" per i test: restituisce, nell'ordine, le risposte
 * predisposte (tool call o testo). Quando le risposte finiscono simula un
 * provider non raggiungibile. Nessun mock dei motori: viene sostituito solo il
 * provider LLM, che non esiste in un test unitario.
 */
class ScriptedApi(
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
        if (responses.isEmpty()) {
            return Response.error(503, "unavailable".toResponseBody("text/plain".toMediaType()))
        }
        return responses.removeAt(0)
    }
}

fun toolCallResponse(name: String, args: String): Response<ChatCompletionResponse> = Response.success(
    ChatCompletionResponse(
        choices = listOf(
            ChatCompletionResponse.Choice(
                index = 0,
                message = ChatCompletionResponse.Message(
                    content = null,
                    toolCalls = listOf(
                        ChatCompletionResponse.ToolCall(
                            id = "c1",
                            type = "function",
                            function = ChatCompletionResponse.FunctionCall(name = name, arguments = args)
                        )
                    )
                ),
                finishReason = "tool_calls"
            )
        )
    )
)

fun textResponse(text: String): Response<ChatCompletionResponse> = Response.success(
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
