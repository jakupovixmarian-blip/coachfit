package dev.fitcoach.app.domain.ai

sealed interface OrchestratorOutcome {
    data class Answer(val text: String, val toolsUsed: List<String>) : OrchestratorOutcome
    data class NeedsApproval(
        val pending: PendingToolCall,
        val transcript: List<ChatMessage>
    ) : OrchestratorOutcome
    data class Unavailable(val reason: String) : OrchestratorOutcome
}

/**
 * Runs one conversation turn: the model proposes, the tools act, the user decides.
 *
 * The model never writes to the database itself. It asks for a tool, the
 * [ToolEngine] validates the request, and any operation that changes data stops at
 * an approval gate. Tool results, including refusals and failures, are fed back to
 * the model so it can correct itself instead of inventing an answer.
 */
class AiOrchestrator(
    private val providers: ProviderManager,
    private val gateway: ProviderGateway,
    private val tools: ToolEngine,
    private val maxSteps: Int = 4
) {

    suspend fun ask(
        userMessage: String,
        systemPrompt: String,
        history: List<ChatMessage> = emptyList(),
        approvals: Map<String, Approval> = emptyMap()
    ): OrchestratorOutcome {
        val chain = providers.orderedChain()
        if (chain.isEmpty()) return OrchestratorOutcome.Unavailable(providers.unavailableReason())

        val messages = buildList {
            add(ChatMessage(Role.SYSTEM, systemPrompt))
            addAll(history)
            add(ChatMessage(Role.USER, userMessage))
        }.toMutableList()

        val used = mutableListOf<String>()
        val failures = mutableListOf<String>()

        repeat(maxSteps) {
            val reply = callWithFallback(chain, messages.toList(), failures)
                ?: return OrchestratorOutcome.Unavailable(
                    "Nessun provider ha risposto. " + failures.joinToString(" ")
                )

            if (reply.toolCalls.isEmpty()) {
                val text = reply.text?.takeIf { it.isNotBlank() }
                    ?: return OrchestratorOutcome.Unavailable("Il provider ha risposto senza contenuto.")
                return OrchestratorOutcome.Answer(text, used)
            }

            if (!reply.text.isNullOrBlank()) {
                messages += ChatMessage(Role.ASSISTANT, reply.text)
            }

            for (call in reply.toolCalls) {
                val decision = approvals[call.id] ?: approvals[call.toolName] ?: Approval.NotRequested
                when (val result = tools.execute(call.toolName, call.arguments, decision)) {
                    is ToolResult.NeedsApproval ->
                        return OrchestratorOutcome.NeedsApproval(result.pending, messages.toList())

                    is ToolResult.Ok -> {
                        used += call.toolName
                        messages += ChatMessage(Role.TOOL, result.output, toolName = call.toolName)
                    }

                    is ToolResult.Refused ->
                        messages += ChatMessage(
                            Role.TOOL,
                            "Operazione rifiutata: ${result.reason}",
                            toolName = call.toolName
                        )

                    is ToolResult.Failed ->
                        messages += ChatMessage(
                            Role.TOOL,
                            "Errore dello strumento: ${result.reason}",
                            toolName = call.toolName
                        )
                }
            }
        }

        return OrchestratorOutcome.Answer(
            "Limite di passaggi raggiunto senza una risposta conclusiva.",
            used
        )
    }

    /** Tries the primary provider, then the fallback, and reports each failure. */
    private suspend fun callWithFallback(
        chain: List<ProviderConfig>,
        messages: List<ChatMessage>,
        failures: MutableList<String>
    ): ModelReply? {
        for (config in chain) {
            val result = gateway.complete(config, messages, tools.specs())
            result.fold(
                onSuccess = { return it },
                onFailure = { error ->
                    val detail = error.message ?: error::class.simpleName ?: "errore sconosciuto"
                    failures += "${config.displayName}: $detail"
                }
            )
        }
        return null
    }
}
