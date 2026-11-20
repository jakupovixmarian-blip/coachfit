package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiParameterPolicy
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.ChatCompletionRequest
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.data.remote.ToolChoice
import com.fitcoach.app.data.remote.assistantToolCallMessage
import com.fitcoach.app.data.remote.toolResultMessage
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import kotlinx.coroutines.withTimeoutOrNull
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
         * strumento: viene sollecitato nel prompt e, se il modello risponde in
         * prosa senza chiamarlo, viene richiamato. Usato dalla generazione del
         * programma (submit_full_program), dove una risposta senza consegna non
         * ha valore.
         *
         * Il forzamento via `tool_choice` avviene SOLO se il provider lo accetta:
         * con Thinking attivo DeepSeek ammette unicamente `auto`, quindi la
         * richiesta forzata viene inviata con Thinking disabilitato (per quella
         * singola richiesta) e mai disattivando il ragionamento in modo globale.
         */
        requireTool: String? = null,
        /**
         * Limite complessivo del turno, in millisecondi. Il ciclo puo fare fino a
         * [maxIterations] richieste, ognuna con retry e readTimeout di 90s: senza
         * un tetto globale la schermata puo restare su "Generazione..." per
         * decine di minuti. Il limite e applicato sia tra un'iterazione e l'altra
         * sia alla singola chiamata ([callWithBudget]): il solo controllo tra le
         * iterazioni non intercettava un provider che non risponde mai.
         */
        deadlineMs: Long? = null
    ): CoachTurnResult {
        val deadlineAt = deadlineMs?.let { System.currentTimeMillis() + it }
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
        /** Quante volte il modello ha risposto in prosa senza consegnare lo strumento obbligatorio. */
        var proseMisses = 0
        /** Quante volte lo strumento obbligatorio e stato chiamato ma respinto dalla validazione. */
        var rejectedDeliveries = 0

        /** Applica una risposta di successo del modello ai buffer del turno. */
        suspend fun consumeSuccess(result: AiResult.Success): Step {
            if (result.content.isNotBlank()) lastText = result.content
            if (result.toolCalls.isEmpty()) {
                // Se uno strumento era obbligatorio e il modello ha risposto in
                // prosa (o e stato troncato), sollecitalo: una risposta senza
                // consegna non e accettabile.
                if (requireTool != null && !requiredToolDelivered) {
                    proseMisses++
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
            // Il reasoning_content va conservato: con `tools` presenti DeepSeek lo
            // richiede integralmente nei turni successivi (altrimenti HTTP 400).
            messages += assistantToolCallMessage(
                toolCalls = result.toolCalls,
                reasoningContent = result.reasoningContent,
                content = result.content.takeIf { it.isNotBlank() }
            )
            result.toolCalls.forEach { call ->
                val args = parseArgs(call.function.arguments)
                // Argomenti non parsabili = chiamata troncata (finish_reason
                // "length") o malformata. Eseguire lo strumento con un oggetto
                // vuoto produrrebbe una diagnosi fuorviante ("giorno 0 kcal
                // errata") su un payload che il modello non ha mai completato.
                val output = if (args == null) {
                    """{"ok":false,"error":"Argomenti della chiamata non validi o troncati. """ +
                        """Riduci la dimensione del payload e richiama lo strumento."}"""
                } else {
                    toolRegistry.execute(call.function.name, args)
                }
                trace += call.function.name
                toolResults += CoachToolResult(call.function.name, output)
                // Conta come consegnato SOLO se lo strumento obbligatorio ha accettato
                // (ok=true). Se la proposta viene respinta (ok=false) la scelta forzata
                // resta attiva e il modello riceve la diagnosi per correggersi.
                if (call.function.name == requireTool) {
                    if (outputReportsOk(output)) {
                        requiredToolDelivered = true
                    } else {
                        rejectedDeliveries++
                    }
                }
                if (call.function.name in toolRegistry.writeToolNames) {
                    extractChangeId(output)?.let { pendingChangeIds += it }
                }
                messages += toolResultMessage(call.id, call.function.name, output)
            }
            return Step.Continue
        }

        repeat(maxIterations) { iteration ->
            if (deadlineAt != null && System.currentTimeMillis() >= deadlineAt) {
                return CoachTurnResult(
                    text = lastText,
                    pendingChangeIds = pendingChangeIds,
                    providerError = deadlineMessage(deadlineMs),
                    toolTrace = trace,
                    toolResults = toolResults
                )
            }
            // Con Thinking attivo DeepSeek accetta SOLO `tool_choice = auto`
            // ("Thinking mode does not support this tool_choice"). Il ragionamento
            // resta quindi attivo e la consegna obbligatoria viene perseguita con
            // le istruzioni e il sollecito. Solo se il modello continua a rispondere
            // in prosa senza consegnare, UNA singola richiesta viene inviata in
            // modalita non-thinking con lo strumento forzato: e l'unico modo
            // ammesso per imporre un tool specifico, senza disattivare il
            // ragionamento in modo globale.
            val thinkingCapable = AiParameterPolicy.supportsThinkingMode(config.provider)
            val mustDeliver = requireTool != null && !requiredToolDelivered
            // Oltre alla risposta in prosa, anche una consegna ripetutamente
            // respinta dalla validazione indica che il modello non riesce a
            // correggersi da solo: dopo due rifiuti si forza la chiamata.
            val escalateForce = mustDeliver &&
                (!thinkingCapable || proseMisses >= 1 || rejectedDeliveries >= 2)
            val forcedChoice = if (escalateForce) ToolChoice.force(requireTool) else ToolChoice.auto
            // Il budget residuo limita la SINGOLA chiamata, non solo il ciclo:
            // senza questo un provider che non risponde mai lascia la schermata
            // su "Generazione..." per sempre, perche il controllo del deadline
            // avviene solo tra un'iterazione e l'altra.
            val result = callWithBudget(deadlineAt) {
                aiClient.complete(
                    config = config,
                    messages = messages,
                    tools = toolRegistry.definitions,
                    toolChoice = forcedChoice,
                    thinking = when {
                        !thinkingCapable -> null
                        escalateForce -> ChatCompletionRequest.ThinkingDirective.disabled
                        else -> ChatCompletionRequest.ThinkingDirective.enabled
                    }
                )
            } ?: return CoachTurnResult(
                text = lastText,
                pendingChangeIds = pendingChangeIds,
                providerError = deadlineMessage(deadlineMs),
                toolTrace = trace,
                toolResults = toolResults
            )
            when (result) {
                is AiResult.Failure -> {
                    // Se il provider rifiuta la scelta forzata dello strumento, non
                    // perdere il tool calling: riprova lo stesso giro con "auto".
                    if (forcedChoice != ToolChoice.auto &&
                        (isToolChoiceUnsupported(result.message) ||
                            AiParameterPolicy.isThinkingToolChoiceError(result.message))
                    ) {
                        val auto = aiClient.complete(
                            config = config,
                            messages = messages,
                            tools = toolRegistry.definitions,
                            toolChoice = ToolChoice.auto,
                            // Il provider ha rifiutato la scelta forzata: si torna
                            // al ragionamento attivo con la scelta automatica.
                            thinking = if (thinkingCapable) {
                                ChatCompletionRequest.ThinkingDirective.enabled
                            } else {
                                null
                            }
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

    /**
     * Esegue la chiamata entro il budget residuo del turno.
     *
     * `withTimeoutOrNull` invece di `withTimeout` perche un superamento del
     * budget e un esito previsto (il provider e lento), non un errore da
     * propagare. Il timeout qui e ammesso anche se il commento su [deadlineMs]
     * lo escludeva: la' il problema era cancellare una chiamata a meta quando
     * il turno doveva ancora consegnare, qui il budget e gia finito e non c'e
     * nulla da salvare.
     */
    private suspend fun <T> callWithBudget(deadlineAt: Long?, block: suspend () -> T): T? {
        if (deadlineAt == null) return block()
        val remaining = deadlineAt - System.currentTimeMillis()
        if (remaining <= 0) return null
        return withTimeoutOrNull(remaining) { block() }
    }

    private fun deadlineMessage(deadlineMs: Long?): String =
        "Il Coach AI non ha completato la generazione entro " +
            "${(deadlineMs ?: 0) / 60_000} minuti. Il modello potrebbe essere lento o non " +
            "supportare bene gli strumenti: riprova o scegli un altro modello nelle Impostazioni."

    /** Null se gli argomenti non sono un oggetto JSON valido (chiamata troncata o malformata). */
    private fun parseArgs(raw: String?): JsonObject? =
        raw?.let { runCatching { json.parseToJsonElement(it) as? JsonObject }.getOrNull() }

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
