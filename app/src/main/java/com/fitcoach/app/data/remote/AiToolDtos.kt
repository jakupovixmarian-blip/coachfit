package com.fitcoach.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Definizione di uno strumento (function calling) inviata al provider, nel
 * formato Chat Completions compatibile OpenAI.
 *
 * L'AI e l'unica autorita decisionale: questi strumenti le danno accesso ai
 * servizi reali dell'app (lettura anamnesi, programmi, storico, calcoli e
 * scrittura controllata). I parametri seguono lo JSON Schema.
 */
@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

/** Messaggio assistant che richiede l'esecuzione di uno o piu strumenti. */
@Serializable
data class ToolCallDto(
    val id: String,
    val type: String = "function",
    val function: FunctionCallDto
)

@Serializable
data class FunctionCallDto(
    val name: String,
    /** Argomenti JSON serializzati come stringa (formato OpenAI). */
    val arguments: String = "{}"
)

/** Helper per costruire un tool_choice. */
object ToolChoice {
    val auto = kotlinx.serialization.json.JsonPrimitive("auto")
    val none = kotlinx.serialization.json.JsonPrimitive("none")
    val required = kotlinx.serialization.json.JsonPrimitive("required")
    fun force(name: String): JsonObject = kotlinx.serialization.json.buildJsonObject {
        put("type", kotlinx.serialization.json.JsonPrimitive("function"))
        put("function", kotlinx.serialization.json.buildJsonObject {
            put("name", kotlinx.serialization.json.JsonPrimitive(name))
        })
    }
}

/** Costruisce un messaggio assistant con tool calls. */
fun assistantToolCallMessage(toolCalls: List<ToolCallDto>): ChatMessageDto =
    ChatMessageDto(role = "assistant", content = null, toolCalls = toolCalls)

/** Costruisce un messaggio di risultato di uno strumento. */
fun toolResultMessage(toolCallId: String, name: String, content: String): ChatMessageDto =
    ChatMessageDto(role = "tool", content = content, toolCallId = toolCallId, name = name)

/** Converte i tool calls di risposta in DTO di richiesta (per il turno successivo). */
fun ChatCompletionResponse.ToolCall.toRequestDto(): ToolCallDto = ToolCallDto(
    id = id ?: "call",
    type = type ?: "function",
    function = FunctionCallDto(
        name = function?.name ?: "",
        arguments = function?.arguments ?: "{}"
    )
)
