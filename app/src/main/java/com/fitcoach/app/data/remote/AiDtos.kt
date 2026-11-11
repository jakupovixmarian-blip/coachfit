package com.fitcoach.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.4,
    @SerialName("max_tokens") val maxTokens: Int = 1200,
    val stream: Boolean = false,
    /**
     * Alcuni provider (OpenRouter) accettano un oggetto `response_format`.
     * Non e sempre supportato: il chiamante decide se richiedere il JSON.
     */
    @SerialName("response_format") val responseFormat: ResponseFormat? = null
) {
    @Serializable
    data class ResponseFormat(val type: String = "json_object")
}

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

/**
 * Risposta Chat Completions compatibile OpenAI.
 *
 * Nota di robustezza (spec. §14): `message.content` puo essere **null** su alcuni
 * modelli (in particolare reasoning su OpenRouter), e il testo utile puo trovarsi in
 * `reasoning`/`reasoning_content`. I campi sono quindi tutti opzionali e il testo
 * viene estratto con [extractText] invece di assumere una stringa non nulla.
 */
@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
    val error: ApiError? = null
) {
    @Serializable
    data class Choice(
        val index: Int = 0,
        val message: Message? = null,
        val delta: Message? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    data class Message(
        val role: String? = null,
        /** Puo essere null (reasoning models) o un array di parti su alcuni provider. */
        val content: JsonElement? = null,
        /** Testo di ragionamento separato (DeepSeek/OpenRouter). NON va mostrato. */
        @SerialName("reasoning_content") val reasoningContent: String? = null,
        val reasoning: String? = null,
        /** Rifiuto esplicito del modello (es. policy di sicurezza). */
        val refusal: String? = null,
        @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null
    )

    @Serializable
    data class ToolCall(
        val id: String? = null,
        val type: String? = null,
        val function: FunctionCall? = null
    )

    @Serializable
    data class FunctionCall(
        val name: String? = null,
        val arguments: String? = null
    )

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0,
        @SerialName("total_tokens") val totalTokens: Int = 0
    )

    /** Testo utile (contenuto vero, senza reasoning). Null se assente. */
    val content: String? get() = choices.firstOrNull()?.message?.let { extractText(it) }

    /** Motivo di fine generazione del primo choice. */
    val finishReason: String? get() = choices.firstOrNull()?.finishReason
}

/** Estrae il testo utile da un messaggio, gestendo content null/array/reasoning. */
internal fun extractText(message: ChatCompletionResponse.Message): String? {
    val fromContent = message.content?.let { element ->
        val prim = element.asPrimitiveOrNull()
        when {
            prim != null -> prim
            element is kotlinx.serialization.json.JsonArray ->
                element.mapNotNull { it.asPrimitiveOrNull() }.joinToString("")
            else -> null
        }
    }
    if (!fromContent.isNullOrBlank()) return fromContent
    // Fallback: alcuni modelli mettono tutto il testo in reasoning_content.
    return message.reasoningContent?.takeIf { it.isNotBlank() }
        ?: message.reasoning?.takeIf { it.isNotBlank() }
}

private fun JsonElement.asPrimitiveOrNull(): String? =
    (this as? kotlinx.serialization.json.JsonPrimitive)
        ?.takeIf { it.isString || (it.content != "null") }
        ?.content

@Serializable
data class ApiError(
    val message: String = "",
    val type: String? = null,
    val code: String? = null
)

