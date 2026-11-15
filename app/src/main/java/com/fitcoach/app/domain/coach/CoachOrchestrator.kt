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

    private sealed interface Step {
        /** Continua il ciclo (richiesta inviata, nessuna risposta finale). */
        data object Continue : Step
        /** Risposta finale pronta per l'utente. */
        data class Done(val result: CoachTurnResult) : Step
    }

    suspend fun run(
        config: AiProviderConfig,
        systemPrompt: String,
        contextPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        maxIterations: Int = MAX_ITERATIONS,
        /**
         * Se impostato, il turno DEVE concludersi con una chiamata a questo
         * strumento: viene forzato con `tool_choice` e, se il modello risponde
         * in prosa senza chiamarlo, viene sollecitato a chiamarlo. Usato dalla
         * generazione del programma (submit_full_program), dove una risposta
         * senza consegna non ha valore.
         */
        requireTool: String? = null
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
        var requiredToolDelivered = false

        /** Applica una risposta di successo del modello ai buffer del turno. */
        suspend fun consumeSuccess(result: AiResult.Success): Step {
            if (result.content.isNotBlank()) lastText = result.content
            if (result.toolCalls.isEmpty()) {
                // Se uno strumento era obbligatorio e il modello ha risposto in
                // prosa (o e stato troncato), sollecitalo: una risposta senza
                // consegna non e accettabile.
                if (requireTool != null && !requiredToolDelivered) {
                    val reason = if (result.truncated) {
                        "La risposta precedente e stata troncata per limite di token. "
                    } else ""
                    messages += ChatMessageDto(
                        role = "user",
                        content = reason + "Non hai ancora consegnato il programma. Devi chiamare " +
                            "ORA lo strumento $requireTool con l'oggetto completo (un'unica chiamata). " +
                            "Non rispondere a parole: emetti la chiamata allo strumento."
                    )
                    return Step.Continue
                }
                return Step.Done(
                    CoachTurnResult(
                        text = result.content,
                        pendingChangeIds = pendingChangeIds,
                        toolTrace = trace,
                        toolResults = toolResults
                    )
                )
            }
            // Registra la richiesta di tool dell'assistant, poi esegue.
            messages += assistantToolCallMessage(result.toolCalls)
            result.toolCalls.forEach { call ->
                val args = parseArgs(call.function.arguments)
                val output = toolRegistry.execute(call.function.name, args)
                trace += call.function.name
                toolResults += CoachToolResult(call.function.name, output)
                // Conta come consegnato SOLO se lo strumento obbligatorio ha accettato
                // (ok=true). Se la proposta viene respinta (ok=false) la scelta forzata
                // resta attiva e il modello riceve la diagnosi per correggersi.
                if (call.function.name == requireTool && outputReportsOk(output)) {
                    requiredToolDelivered = true
                }
                if (call.function.name in toolRegistry.writeToolNames) {
                    extractChangeId(output)?.let { pendingChangeIds += it }
                }
                messages += toolResultMessage(call.id, call.function.name, output)
            }
            return Step.Continue
        }

        repeat(maxIterations) { iteration ->
            // Forza la consegna finche lo strumento richiesto non e stato chiamato.
            val forcedChoice = if (requireTool != null && !requiredToolDelivered) {
                ToolChoice.force(requireTool)
            } else {
                ToolChoice.auto
            }
            val result = aiClient.complete(
                config = config,
                messages = messages,
                tools = toolRegistry.definitions,
                toolChoice = forcedChoice
            )
            when (result) {
                is AiResult.Failure -> {
                    // Se il provider rifiuta la scelta forzata dello strumento, non
                    // perdere il tool calling: riprova lo stesso giro con "auto".
                    if (forcedChoice != ToolChoice.auto && isToolChoiceUnsupported(result.message)) {
                        val auto = aiClient.complete(
                            config = config,
                            messages = messages,
                            tools = toolRegistry.definitions,
                            toolChoice = ToolChoice.auto
                        )
                        if (auto is AiResult.Success) {
                            when (val step = consumeSuccess(auto)) {
                                is Step.Continue -> return@repeat
                                is Step.Done -> return step.result
                            }
                        }
                    }
                    // Fallback: alcuni provider/modelli non supportano i tool. Se e il
                    // primo giro e NESSUNO strumento e obbligatorio, riprova senza
                    // strumenti (il modello puo comunque rispondere in testo).
                    if (iteration == 0 && requireTool == null && isToolUnsupported(result.message)) {
                        val plain = aiClient.complete(config, messages)
                        return when (plain) {
                            is AiResult.Success -> CoachTurnResult(text = plain.content, toolTrace = trace)
                            is AiResult.Failure -> CoachTurnResult(text = "", providerError = plain.message, toolTrace = trace)
                        }
                    }
                    return CoachTurnResult(text = lastText, providerError = result.message, toolTrace = trace, toolResults = toolResults)
                }

                is AiResult.Success -> {
                    when (val step = consumeSuccess(result)) {
                        is Step.Continue -> Unit
                        is Step.Done -> return step.result
                    }
                }
            }
        }
        // Lo strumento obbligatorio non e stato consegnato entro le iterazioni:
        // non mascherare il fallimento con una sintesi testuale.
        if (requireTool != null && !requiredToolDelivered) {
            return CoachTurnResult(
                text = lastText,
                pendingChangeIds = pendingChangeIds,
                providerError = "Il modello non ha consegnato il programma con lo strumento " +
                    "$requireTool entro $maxIterations tentativi (risposta troncata o senza chiamata " +
                    "allo strumento).",
                toolTrace = trace,
                toolResults = toolResults
            )
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

    /**
     * True se l'output di uno strumento riporta `ok=true`. Uno strumento di
     * consegna che risponde `ok=false` NON conta come consegna riuscita: la
     * proposta e stata respinta e il modello deve correggersi.
     */
    private fun outputReportsOk(output: String): Boolean = runCatching {
        val obj = json.parseToJsonElement(output) as? JsonObject ?: return false
        (obj["ok"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
    }.getOrDefault(false)

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

    /**
     * True se l'errore riguarda specificamente `tool_choice` forzato (alcuni
     * modelli/provider accettano solo "auto"): in quel caso si ritenta con "auto"
     * invece di perdere del tutto il tool calling.
     */
    private fun isToolChoiceUnsupported(message: String): Boolean {
        val m = message.lowercase()
        return m.contains("tool_choice") || (m.contains("tool") && m.contains("required"))
    }

    companion object {
        const val MAX_ITERATIONS = 6
    }
}
