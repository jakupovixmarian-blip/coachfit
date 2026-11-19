package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double? = null,
    /**
     * Limite dei token generati. Il NOME del campo dipende da provider e modello:
     * `max_tokens` (Groq/OpenRouter/DeepSeek/custom e OpenAI storici) oppure
     * `max_completion_tokens` (modelli OpenAI recenti). Il payload ne contiene
     * sempre UNO SOLO: i due campi sono `null` per default e vengono omessi
     * grazie a `explicitNulls = false` nel [kotlinx.serialization.json.Json].
     * Usa [ChatCompletionRequest.forProvider] per costruirla correttamente.
     */
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int? = null,
    val stream: Boolean = false,
    /**
     * Alcuni provider (OpenRouter) accettano un oggetto `response_format`.
     * Non e sempre supportato: il chiamante decide se richiedere il JSON.
     */
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    /** Strumenti (function calling) disponibili per il modello. */
    val tools: List<ToolDefinition>? = null,
    /** Politica di scelta degli strumenti: auto/none/required oppure oggetto forzato. */
    @SerialName("tool_choice") val toolChoice: JsonElement? = null,
    /**
     * Sforzo di ragionamento per i modelli OpenAI recenti (gpt-5.x, o-series).
     * OpenAI rifiuta i function tools su `/v1/chat/completions` quando lo sforzo
     * di ragionamento e attivo: in quel caso va inviato `"none"`.
     */
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    /**
     * Interruttore della modalita Thinking di DeepSeek (`{"type":"enabled"}` /
     * `{"type":"disabled"}`). Va inviato SOLO ai provider che la supportano: gli
     * altri rifiuterebbero un campo sconosciuto.
     *
     * Regola DeepSeek: con Thinking attivo `tool_choice` puo essere solo `auto`.
     * Per forzare un singolo strumento la richiesta va inviata con Thinking
     * disabilitato (una sola richiesta, non un cambio globale).
     */
    @SerialName("thinking") val thinking: ThinkingDirective? = null
) {
    /** Direttiva Thinking di DeepSeek. */
    @Serializable
    data class ThinkingDirective(val type: String) {
        companion object {
            val enabled = ThinkingDirective("enabled")
            val disabled = ThinkingDirective("disabled")
        }
    }

    @Serializable
    data class ResponseFormat(val type: String = "json_object")

    companion object {
        /**
         * Costruisce la richiesta scegliendo il parametro token corretto per il
         * provider/modello. Non imposta mai entrambi i parametri.
         */
        fun forProvider(
            provider: AiProvider,
            model: String,
            messages: List<ChatMessageDto>,
            temperature: Double? = null,
            maxTokens: Int? = null,
            responseFormat: ResponseFormat? = null,
            tools: List<ToolDefinition>? = null,
            toolChoice: JsonElement? = null,
            thinking: ThinkingDirective? = null
        ): ChatCompletionRequest {
            val newParam = AiParameterPolicy.tokenParamFor(provider, model) ==
                TokenParam.MAX_COMPLETION_TOKENS
            val hasTools = !tools.isNullOrEmpty()
            return ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                maxTokens = if (newParam) null else maxTokens,
                maxCompletionTokens = if (newParam) maxTokens else null,
                responseFormat = responseFormat,
                tools = tools,
                toolChoice = if (hasTools) toolChoice else null,
                // OpenAI rifiuta i function tools su /v1/chat/completions quando il
                // modello applica un reasoning_effort di default (gpt-5.6:
                // "Function tools with reasoning_effort are not supported ... set
                // reasoning_effort to \"none\""). Con strumenti si forza "none";
                // senza strumenti si lascia il default del provider.
                reasoningEffort = if (hasTools && newParam) REASONING_EFFORT_NONE else null,
                thinking = thinking
            )
        }

        /**
         * Riscrive la richiesta usando l'altro parametro token (per il retry
         * controllato quando il provider rifiuta il parametro inviato).
         */
        fun withAlternateTokenParam(request: ChatCompletionRequest): ChatCompletionRequest {
            val value = request.maxTokens ?: request.maxCompletionTokens ?: return request
            val currentlyNew = request.maxCompletionTokens != null
            return request.copy(
                maxTokens = if (currentlyNew) value else null,
                maxCompletionTokens = if (currentlyNew) null else value
            )
        }

        /**
         * Rimuove la temperatura (i modelli che accettano solo il valore di default
         * la rifiutano): il payload viene inviato senza `temperature`.
         */
        fun withoutTemperature(request: ChatCompletionRequest): ChatCompletionRequest =
            request.copy(temperature = null)

        /**
         * Rimuove `reasoning_effort` (alcuni endpoint non riconoscono il campo e lo
         * rifiutano): il payload viene inviato senza, lasciando il default.
         */
        fun withoutReasoningEffort(request: ChatCompletionRequest): ChatCompletionRequest =
            request.copy(reasoningEffort = null)

        /**
         * Forza `reasoning_effort = "none"`: necessario per usare i function tools
         * su `/v1/chat/completions` con i modelli OpenAI di ragionamento (gpt-5.x).
         */
        fun withReasoningNone(request: ChatCompletionRequest): ChatCompletionRequest =
            request.copy(reasoningEffort = REASONING_EFFORT_NONE)

        /**
         * Riporta `tool_choice` su "auto": DeepSeek in Thinking mode rifiuta
         * qualsiasi scelta forzata ("Thinking mode does not support this tool_choice").
         */
        fun withAutoToolChoice(request: ChatCompletionRequest): ChatCompletionRequest =
            request.copy(toolChoice = ToolChoice.auto)

        /** Valore di reasoning_effort che abilita i function tools su Chat Completions. */
        const val REASONING_EFFORT_NONE = "none"
    }
}

@Serializable
data class ChatMessageDto(
    val role: String,
    /** Nullo per i messaggi assistant che contengono solo tool_calls. */
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDto>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    /** Nome della funzione per i messaggi role=tool. */
    val name: String? = null,
    /**
     * Catena di pensiero restituita da DeepSeek in modalita Thinking. Va
     * reinserita INTEGRALMENTE nei messaggi assistant successivi quando la
     * richiesta porta `tools`: senza di essa l'API risponde HTTP 400.
     */
    @SerialName("reasoning_content") val reasoningContent: String? = null
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

    /**
     * Testo utile con fallback sul reasoning: usato quando il modello mette tutta
     * la risposta in `reasoning_content` (alcuni reasoning model). Non va usato
     * per decidere se il turno contiene una risposta finale quando ci sono tool call.
     */
    val content: String? get() = choices.firstOrNull()?.message?.let { extractText(it) }

    /** Solo il contenuto vero (`content`), senza fallback sul reasoning. */
    val rawContent: String? get() = choices.firstOrNull()?.message?.let { extractContentOnly(it) }

    /** Motivo di fine generazione del primo choice. */
    val finishReason: String? get() = choices.firstOrNull()?.finishReason
}

/** Estrae SOLO il contenuto (`content`), senza fallback sul reasoning. */
internal fun extractContentOnly(message: ChatCompletionResponse.Message): String? =
    message.content?.let { element ->
        when {
            element.asPrimitiveOrNull() != null -> element.asPrimitiveOrNull()
            element is kotlinx.serialization.json.JsonArray ->
                element.mapNotNull { it.asPrimitiveOrNull() }.joinToString("")
            else -> null
        }
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

