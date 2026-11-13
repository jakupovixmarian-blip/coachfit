package dev.fitcoach.app.domain.ai

/**
 * Executes tool calls on behalf of the AI.
 *
 * The model never touches data directly: it names a tool and supplies text
 * arguments. The engine checks that the tool exists, converts and validates every
 * argument against the declared spec, refuses unknown parameters, and stops at an
 * approval gate for operations that change the user's data. Everything is written
 * to the audit log, so a refused or failed call is visible instead of silent.
 */
class ToolEngine(
    tools: List<Tool>,
    private val audit: ToolAuditLog,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private val registry: Map<String, Tool> = tools.associateBy { it.spec.name }

    fun specs(): List<ToolSpec> = registry.values.map { it.spec }.sortedBy { it.name }

    fun spec(name: String): ToolSpec? = registry[name]?.spec

    suspend fun execute(
        name: String,
        arguments: Map<String, String> = emptyMap(),
        approval: Approval = Approval.NotRequested
    ): ToolResult {
        val tool = registry[name]
        if (tool == null) {
            val reason = "Strumento sconosciuto: $name."
            record(name, arguments, "rejected", reason)
            return ToolResult.Refused(name, reason)
        }

        return when (val validated = validate(tool.spec, arguments)) {
            is Validation.Invalid -> {
                record(name, arguments, "rejected", validated.reason)
                ToolResult.Refused(name, validated.reason)
            }

            is Validation.Valid -> {
                val spec = tool.spec
                if (spec.requiresApproval && approval !is Approval.Approved) {
                    if (approval is Approval.Denied) {
                        val reason = "Operazione annullata: nessuna modifica applicata."
                        record(name, arguments, "approval_denied", reason)
                        return ToolResult.Refused(name, reason)
                    }
                    val pending = PendingToolCall(
                        toolName = name,
                        arguments = arguments,
                        summary = summarise(spec, arguments)
                    )
                    record(name, arguments, "approval_requested", pending.summary)
                    return ToolResult.NeedsApproval(pending)
                }

                val result = runCatching { tool.invoke(validated.arguments) }
                result.fold(
                    onSuccess = { output ->
                        record(name, arguments, "ok", output)
                        ToolResult.Ok(name, output)
                    },
                    onFailure = { error ->
                        val reason = error.message ?: error::class.simpleName ?: "Errore non specificato"
                        record(name, arguments, "failed", reason)
                        ToolResult.Failed(name, reason)
                    }
                )
            }
        }
    }

    private fun summarise(spec: ToolSpec, arguments: Map<String, String>): String {
        val details = arguments.entries.joinToString(", ") { (key, value) -> "$key=$value" }
        return if (details.isEmpty()) spec.description else "${spec.description} ($details)"
    }

    private suspend fun record(
        toolName: String,
        arguments: Map<String, String>,
        outcome: String,
        detail: String
    ) {
        audit.record(
            ToolAuditEntry(
                toolName = toolName,
                arguments = arguments,
                outcome = outcome,
                detail = detail,
                at = clock()
            )
        )
    }

    private sealed interface Validation {
        data class Valid(val arguments: ToolArguments) : Validation
        data class Invalid(val reason: String) : Validation
    }

    private fun validate(spec: ToolSpec, raw: Map<String, String>): Validation {
        val values = mutableMapOf<String, Any>()

        for (parameter in spec.parameters) {
            val text = raw[parameter.name]?.trim()
            if (text.isNullOrEmpty()) {
                if (parameter.required) {
                    return Validation.Invalid("Parametro obbligatorio mancante: ${parameter.name}.")
                }
                continue
            }

            val converted: Any? = when (parameter.type) {
                ToolParameterType.STRING -> text
                ToolParameterType.NUMBER -> text.replace(',', '.').toDoubleOrNull()
                ToolParameterType.INTEGER -> text.replace(',', '.').toDoubleOrNull()?.toInt()
                ToolParameterType.BOOLEAN -> when (text.lowercase()) {
                    "true", "si", "sì", "yes", "1" -> true
                    "false", "no", "0" -> false
                    else -> null
                }
            }

            if (converted == null) {
                return Validation.Invalid(
                    "Parametro ${parameter.name} non valido: '$text' (atteso ${parameter.type.name.lowercase()})."
                )
            }
            values[parameter.name] = converted
        }

        val declared = spec.parameters.map { it.name }.toSet()
        val unknown = raw.keys - declared
        if (unknown.isNotEmpty()) {
            return Validation.Invalid("Parametri non previsti: ${unknown.sorted().joinToString(", ")}.")
        }

        return Validation.Valid(ToolArguments(values))
    }
}
