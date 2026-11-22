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
import kotlinx.serialization.json.JsonArray
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

/** Chiamata di prerequisito andata a buon fine, con i suoi argomenti. */
data class SatisfiedPrerequisite(
    val toolName: String,
    val arguments: JsonObject?,
    val output: String
)

/**
 * Condizione che deve valere PRIMA che lo strumento obbligatorio venga eseguito.
 *
 * Non basta "lo strumento e' stato chiamato": `consult_invictus` senza argomento
 * restituisce una regola per categoria e lascerebbe la scheda senza i numeri che
 * servono. [isSatisfiedBy] decide sulla base delle chiamate realmente avvenute.
 */
class Prerequisite(
    val toolName: String,
    val description: String,
    private val predicate: (List<SatisfiedPrerequisite>) -> Boolean
) {
    fun isSatisfiedBy(calls: List<SatisfiedPrerequisite>): Boolean = predicate(calls)

    companion object {
        /**
         * Almeno una consultazione per OGNI area indicata, con un argomento che
         * riguardi quell'area.
         *
         * La sola area non basta: chiedere `area=nutrizione&argomento=petto` passa
         * il controllo dell'area ma non restituisce nulla di utile sulla
         * nutrizione, e il modello costruirebbe i macro senza averli letti.
         */
        fun consultations(toolName: String, areas: List<String>, description: String): Prerequisite =
            Prerequisite(toolName, description) { calls ->
                areas.all { area ->
                    calls.any { c ->
                        c.toolName == toolName &&
                            c.arguments?.let { argArea(it) } == area &&
                            outputHasArea(c.output, area)
                    }
                }
            }

        private fun argArea(arguments: JsonObject): String? =
            (arguments["area"] as? kotlinx.serialization.json.JsonPrimitive)
                ?.content?.trim()?.lowercase()

        private fun outputHasArea(output: String, area: String): Boolean =
            output.contains("\"area\":\"$area\"") || output.contains("\"area\": \"$area\"")
    }
}

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

    /**
     * Esegue il turno e garantisce la coerenza tra testo e stato reale.
     *
     * Il modello a volte scrive "ho modificato il piano" dopo aver chiamato uno
     * strumento `propose_*`, che pero' registra solo una proposta. Il testo
     * veniva mostrato cosi' com'era e l'utente credeva alla modifica: qui la
     * frase viene corretta in modo deterministico, senza dipendere dal modello.
     */
    suspend fun run(
        config: AiProviderConfig,
        systemPrompt: String,
        contextPrompt: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        maxIterations: Int = MAX_ITERATIONS,
        requireTool: String? = null,
        requiredPrerequisites: List<Prerequisite> = emptyList(),
        deadlineMs: Long? = null,
        onProgress: ((String) -> Unit)? = null
    ): CoachTurnResult {
        val result = runInternal(
            config = config,
            systemPrompt = systemPrompt,
            contextPrompt = contextPrompt,
            history = history,
            userMessage = userMessage,
            maxIterations = maxIterations,
            requireTool = requireTool,
            requiredPrerequisites = requiredPrerequisites,
            deadlineMs = deadlineMs,
            onProgress = onProgress
        )
        return result.copy(text = clarifyPendingProposal(result.text, result.pendingChangeIds.isNotEmpty()))
    }

    /**
     * Se c'e' una proposta in attesa, il testo non puo' affermare che la modifica
     * e' gia' attiva: si aggiunge una riga esplicita di conferma richiesta.
     * Se invece non c'e' nessuna proposta ma il testo dice "ho modificato",
     * la frase viene sostituita perche' sarebbe falsa.
     */
    internal fun clarifyPendingProposal(text: String, hasPending: Boolean): String {
        val claimsApplied = APPLIED_CLAIM.containsMatchIn(text)
        if (hasPending) {
            if (text.isBlank()) {
                return "Ho preparato una proposta di modifica. Confermala per applicarla."
            }
            if (!APPLIED_CLAIM.containsMatchIn(text)) return text
            val corrected = text.replace(APPLIED_CLAIM, "ho preparato una proposta per")
            return corrected + "\n\nLa modifica non e' ancora attiva: serve la tua conferma."
        }
        if (!claimsApplied) return text
        // Nessuna proposta registrata: l'affermazione e' falsa in ogni caso.
        return "Non ho applicato nessuna modifica: non risulta alcuna proposta registrata. " +
            "Dimmi quale cambiamento vuoi e lo preparo."
    }

    private suspend fun runInternal(
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
         * Strumenti che devono essere stati chiamati con successo PRIMA che
         * [requireTool] venga eseguito, con la descrizione di cosa forniscono.
         *
         * Serve alla generazione: `submit_full_program` salva il programma quando
         * risponde ok, quindi un controllo fatto dopo l'esecuzione arriverebbe
         * troppo tardi. Se un prerequisito manca, la consegna non viene eseguita
         * e il modello riceve l'istruzione di chiamare prima il prerequisito.
         *
         * La presenza nel prompt non basta: `consult_invictus` era citato in prosa
         * e il modello lo ignorava in 3 prove live su 3, perche' uno strumento
         * obbligatorio in concorrenza vince sempre su uno suggerito.
         */
        requiredPrerequisites: List<Prerequisite> = emptyList(),
        /**
         * Limite complessivo del turno, in millisecondi. Il ciclo puo fare fino a
         * [maxIterations] richieste, ognuna con retry e readTimeout di 90s: senza
         * un tetto globale la schermata puo restare su "Generazione..." per
         * decine di minuti. Il limite e applicato sia tra un'iterazione e l'altra
         * sia alla singola chiamata ([callWithBudget]): il solo controllo tra le
         * iterazioni non intercettava un provider che non risponde mai.
         */
        deadlineMs: Long? = null,
        /**
         * Avanzamento leggibile del turno, per la UI. Senza di esso la schermata
         * resta su una barra immobile per minuti e l'utente non distingue
         * "sta lavorando" da "si e bloccata".
         */
        onProgress: ((String) -> Unit)? = null
    ): CoachTurnResult {
        val deadlineAt = deadlineMs?.let { System.currentTimeMillis() + it }
        val messages = mutableListOf<ChatMessageDto>()
        // Un solo messaggio system. Due messaggi system consecutivi sono
        // formalmente validi, ma alcuni provider dietro OpenRouter (Parasail)
        // li rifiutano con "System message must be at the beginning": la
        // richiesta falliva in base a chi serviva la chiamata.
        messages += ChatMessageDto(
            role = "system",
            content = listOf(systemPrompt, contextPrompt)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        )
        messages += history
        messages += ChatMessageDto(role = "user", content = userMessage)

        val pendingChangeIds = mutableListOf<Long>()
        val trace = mutableListOf<String>()
        val toolResults = mutableListOf<CoachToolResult>()
        var lastText = ""
        var requiredToolDelivered = false
        /** Chiamate di prerequisito andate a buon fine, con argomenti e output. */
        val satisfiedCalls = mutableListOf<SatisfiedPrerequisite>()
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
                // Prerequisito non soddisfatto: la chiamata NON viene eseguita.
                // submit_full_program salva quando risponde ok, quindi rifiutarla
                // dopo l'esecuzione sarebbe troppo tardi: il programma sarebbe
                // gia' sul database. Si devia il modello sullo strumento mancante.
                val unmet = if (call.function.name == requireTool) {
                    requiredPrerequisites.firstOrNull { !it.isSatisfiedBy(satisfiedCalls) }
                } else {
                    null
                }
                val output = if (unmet != null) {
                    """{"ok":false,"error":"Prima di consegnare devi consultare la metodologia con """ +
                        """'${unmet.toolName}': ${unmet.description} Chiama '${unmet.toolName}' adesso """ +
                        """(una o piu' volte, per ogni area che ti serve), poi richiama """ +
                        """${call.function.name} con l'oggetto completo."}"""
                } else {
                    val args = parseArgs(call.function.arguments)
                    // Argomenti non parsabili = chiamata troncata (finish_reason
                    // "length") o malformata. Eseguire lo strumento con un oggetto
                    // vuoto produrrebbe una diagnosi fuorviante ("giorno 0 kcal
                    // errata") su un payload che il modello non ha mai completato.
                    if (args == null) {
                        """{"ok":false,"error":"Argomenti della chiamata non validi o troncati. """ +
                            """Riduci la dimensione del payload e richiama lo strumento."}"""
                    } else {
                        toolRegistry.execute(call.function.name, args)
                    }
                }
                trace += call.function.name
                toolResults += CoachToolResult(call.function.name, output)
                // Un prerequisito conta come soddisfatto solo se ha restituito
                // contenuto: un ok=true con zero risultati non e' una consultazione.
                if (requiredPrerequisites.any { it.toolName == call.function.name } &&
                    outputReportsOk(output) &&
                    outputDeliversContent(output)
                ) {
                    satisfiedCalls += SatisfiedPrerequisite(
                        toolName = call.function.name,
                        arguments = parseArgs(call.function.arguments),
                        output = output
                    )
                }
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
            onProgress?.invoke(
                if (iteration == 0) "Invio la richiesta al provider AI..."
                else "Tentativo ${iteration + 1} di $maxIterations: il Coach AI sta correggendo il programma..."
            )
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
            val missingPrerequisite =
                requiredPrerequisites.firstOrNull { !it.isSatisfiedBy(satisfiedCalls) }?.toolName
            // Oltre alla risposta in prosa, anche una consegna ripetutamente
            // respinta dalla validazione indica che il modello non riesce a
            // correggersi da solo: dopo due rifiuti si forza la chiamata.
            val escalateForce = mustDeliver &&
                (!thinkingCapable || proseMisses >= 1 || rejectedDeliveries >= 2)
            // Se manca un prerequisito si forza QUELLO, non lo strumento finale:
            // forzare `submit` quando il modello non ha ancora consultato lo
            // rendeva impossibile soddisfare il prerequisito, e il turno finiva
            // in fallimento senza che il modello avesse modo di correggersi.
            val forcedChoice = when {
                escalateForce && missingPrerequisite != null ->
                    ToolChoice.force(missingPrerequisite)
                escalateForce -> ToolChoice.force(requireTool)
                else -> ToolChoice.auto
            }
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

    /**
     * Vero se lo strumento ha restituito contenuto utile, non solo `ok=true`.
     *
     * `consult_invictus` risponde ok anche quando l'argomento non produce nulla:
     * contare quella chiamata come consultazione permetterebbe al modello di
     * soddisfare il prerequisito con una ricerca vuota.
     */
    private fun outputDeliversContent(output: String): Boolean = runCatching {
        val obj = json.parseToJsonElement(output) as? JsonObject ?: return false
        val counts = listOf("regole", "tecniche", "esercizi").mapNotNull { key ->
            (obj[key] as? JsonArray)?.size
        }
        counts.isNotEmpty() && counts.any { it > 0 }
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

        /**
         * Frasi con cui il modello dichiara una modifica gia' avvenuta o in corso
         * di esecuzione. Volutamente ristrette: devono cogliere l'affermazione,
         * non ogni uso del verbo.
         *
         * Copre anche "procedo con la modifica", "vado ad aggiornare",
         * "sto applicando": erano le formule con cui il modello annunciava un
         * lavoro che non aveva modo di svolgere (nessuno strumento per le
         * tecniche d'intensita'), e restavano visibili all'utente come se la
         * modifica fosse stata fatta.
         */
        internal val APPLIED_CLAIM = Regex(
            "(?i)\\b(?:" +
                // Passato: "ho/abbiamo modificato".
                "(?:ho|abbiamo)\\s+(?:gia\\s+)?(?:modificat|aggiornat|applicat|sostituit|cambiat|" +
                "spostat|impostat|regolat|adattat|rigenerat|aggiunt|inserit|rimoss|eliminat|" +
                "aumentat|ridott|diminuit)\\w*" +
                // Presente progressivo: "sto modificando", "stiamo applicando".
                "|(?:sto|stiamo)\\s+(?:modificando|aggiornando|applicando|sostituendo|cambiando|" +
                "spostando|impostando|regolando|adattando|rigenerando|aggiungendo|inserendo|" +
                "aumentando|riducendo)" +
                // Annuncio di esecuzione: "procedo con la modifica", "vado ad aggiornare".
                "|(?:procedo|procediamo|vado|andiamo)\\s+(?:con|ad?|a)\\s+(?:la\\s+|il\\s+|le\\s+|lo\\s+)?" +
                "(?:modific|aggiorn|applic|sostitu|regol|adatt|rigener|aggiung|inser|aument|riduc|" +
                "diminu|camb)" +
                // "modifica avvenuta / applicata / completata".
                "|(?:modific|aggiornament|cambi|sostituzion|applicazion)\\w*\\s+(?:e\\s+)?(?:stat|son\\s+stat)\\w*\\s+" +
                "(?:applicat|completat|eseguit|fatt|inserit|aggiunt)\\w*" +
                ")"
        )
    }
}
