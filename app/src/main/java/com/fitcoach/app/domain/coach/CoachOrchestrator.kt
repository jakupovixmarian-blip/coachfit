package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.data.remote.ToolChoice
import com.fitcoach.app.data.remote.assistantToolCallMessage
import com.fitcoach.app.data.remote.toolResultMessage
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** Esito di un turno agentico: testo finale e proposte registrate (se presenti). */
data class CoachTurnResult(
    val text: String,
    val pendingChangeIds: List<Long> = emptyList(),
    val providerError: String? = null,
    val toolTrace: List<String> = emptyList(),
    /** Output strutturato di ogni strumento eseguito (nome -> JSON), nell'ordine di chiamata. */
    val toolResults: List<CoachToolResult> = emptyList()
)

/** Risultato grezzo dell'esecuzione di uno strumento durante il turno agentico. */
data class CoachToolResult(val name: String, val output: String)

/**
 * Orchestratore agentico del coach: l'AI e l'unica autorita decisionale.
 *
 * Ciclo di ragionamento:
 *  1. Invia i messaggi e le definizioni degli strumenti al modello.
 *  2. Se il modello richiede strumenti, li esegue sui servizi REALI dell'app e
 *     restituisce i risultati strutturati come messaggi `tool`.
 *  3. Ripete finche il modello produce una risposta testuale finale o fino a un
 *     numero massimo di iterazioni.
 *
 * Gli strumenti di scrittura NON applicano le modifiche: registrano una proposta
 * che l'utente conferma (vedi CoachActionExecutor). L'AI non scrive mai sul DB.
 */
@Singleton
class CoachOrchestrator @Inject constructor(
    private val aiClient: AiClient,
    private val toolRegistry: CoachToolRegistry,
    private val json: Json
) {

    suspend fun run(
        config: AiProviderConfig,
        systemPrompt: String,
        contextPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        maxIterations: Int = MAX_ITERATIONS
    ): CoachTurnResult {
        val messages = mutableListOf<ChatMessageDto>()
        messages += ChatMessageDto(role = "system", content = systemPrompt)
        messages += ChatMessageDto(role = "system", content = contextPrompt)
        messages += history
        messages += ChatMessageDto(role = "user", content = userMessage)

        val pendingChangeIds = mutableListOf<Long>()
        val trace = mutableListOf<String>()
        val toolResults = mutableListOf<CoachToolResult>()
        var lastText = ""

        repeat(maxIterations) { iteration ->
            val result = aiClient.complete(
                config = config,
                messages = messages,
                tools = toolRegistry.definitions,
                toolChoice = ToolChoice.auto
            )
            when (result) {
                is AiResult.Failure -> {
                    // Fallback: alcuni provider/modelli non supportano i tool. Se e il
                    // primo giro, riprova senza strumenti (il modello puo comunque
                    // rispondere in testo, incluso il blocco azione JSON legacy).
                    if (iteration == 0 && isToolUnsupported(result.message)) {
                        val plain = aiClient.complete(config, messages)
                        return when (plain) {
                            is AiResult.Success -> CoachTurnResult(text = plain.content, toolTrace = trace)
                            is AiResult.Failure -> CoachTurnResult(text = "", providerError = plain.message, toolTrace = trace)
                        }
                    }
                    return CoachTurnResult(text = lastText, providerError = result.message, toolTrace = trace, toolResults = toolResults)
                }

                is AiResult.Success -> {
                    if (result.content.isNotBlank()) lastText = result.content
                    if (result.toolCalls.isEmpty()) {
                        return CoachTurnResult(text = result.content, pendingChangeIds = pendingChangeIds, toolTrace = trace, toolResults = toolResults)
                    }
                    // Registra la richiesta di tool dell'assistant, poi esegue.
                    messages += assistantToolCallMessage(result.toolCalls)
                    result.toolCalls.forEach { call ->
                        val args = parseArgs(call.function.arguments)
                        val output = toolRegistry.execute(call.function.name, args)
                        trace += call.function.name
                        toolResults += CoachToolResult(call.function.name, output)
                        if (call.function.name in toolRegistry.writeToolNames) {
                            extractChangeId(output)?.let { pendingChangeIds += it }
                        }
                        messages += toolResultMessage(call.id, call.function.name, output)
                    }
                }
            }
        }
        // Limite iterazioni: chiedi una sintesi finale senza strumenti.
        val finalMessages = messages + ChatMessageDto(
            role = "user",
            content = "Fornisci ora la risposta finale all'utente in italiano, in modo conciso, " +
                "senza richiedere altri strumenti."
        )
        return when (val final = aiClient.complete(config, finalMessages)) {
            is AiResult.Success -> CoachTurnResult(text = final.content, pendingChangeIds = pendingChangeIds, toolTrace = trace, toolResults = toolResults)
            is AiResult.Failure -> CoachTurnResult(
                text = lastText.ifBlank { "Ho raccolto i dati necessari." },
                pendingChangeIds = pendingChangeIds,
                providerError = if (lastText.isBlank()) final.message else null,
                toolTrace = trace,
                toolResults = toolResults
            )
        }
    }

    private fun parseArgs(raw: String?): JsonObject =
        raw?.let { runCatching { json.parseToJsonElement(it) as? JsonObject }.getOrNull() } ?: JsonObject(emptyMap())

    private fun extractChangeId(output: String): Long? =
        runCatching {
            val obj = json.parseToJsonElement(output) as? JsonObject ?: return null
            (obj["changeId"] as? kotlinx.serialization.json.JsonPrimitive)
                ?.content?.toLongOrNull()
        }.getOrNull()

    private fun isToolUnsupported(message: String): Boolean {
        val m = message.lowercase()
        return m.contains("tool") && (
            m.contains("not supported") || m.contains("unsupported") ||
                m.contains("does not support") || m.contains("unknown parameter") ||
                m.contains("invalid") || m.contains("400")
            )
    }

    companion object {
        const val MAX_ITERATIONS = 6
    }
}
