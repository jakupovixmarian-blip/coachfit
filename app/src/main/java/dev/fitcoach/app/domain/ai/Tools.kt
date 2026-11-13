package dev.fitcoach.app.domain.ai

/** Type declared by a tool parameter, so arguments are converted and checked, never guessed. */
enum class ToolParameterType { STRING, NUMBER, INTEGER, BOOLEAN }

data class ToolParameter(
    val name: String,
    val type: ToolParameterType,
    val required: Boolean = true,
    val description: String = ""
)

/**
 * What the model is allowed to call.
 *
 * [requiresApproval] marks the operations that change the user's data: the engine
 * stops and asks, it never applies them on its own. [mutatesData] documents the
 * intent for the caller and the audit trail.
 */
data class ToolSpec(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList(),
    val requiresApproval: Boolean = false,
    val mutatesData: Boolean = false
)

/** Arguments already converted to the types declared by the spec. */
class ToolArguments(private val values: Map<String, Any>) {

    fun string(name: String): String? = values[name] as? String

    fun number(name: String): Double? = when (val value = values[name]) {
        is Double -> value
        is Int -> value.toDouble()
        else -> null
    }

    fun int(name: String): Int? = when (val value = values[name]) {
        is Int -> value
        is Double -> value.toInt()
        else -> null
    }

    fun boolean(name: String): Boolean? = values[name] as? Boolean

    fun raw(): Map<String, Any> = values
}

/** One concrete operation the AI can perform on the app. */
interface Tool {
    val spec: ToolSpec
    suspend fun invoke(arguments: ToolArguments): String
}

/** Every attempt is recorded, including refusals and failures. */
data class ToolAuditEntry(
    val toolName: String,
    val arguments: Map<String, String>,
    val outcome: String,
    val detail: String,
    val at: Long
)

interface ToolAuditLog {
    suspend fun record(entry: ToolAuditEntry)
}

/** Keeps the trail in memory; the persistent implementation lives in the data layer. */
class InMemoryToolAuditLog : ToolAuditLog {
    private val entries = mutableListOf<ToolAuditEntry>()

    override suspend fun record(entry: ToolAuditEntry) {
        entries += entry
    }

    fun all(): List<ToolAuditEntry> = entries.toList()
}

/** The user's answer about an operation that changes data. */
sealed interface Approval {
    data object NotRequested : Approval
    data object Approved : Approval
    data object Denied : Approval
}

/** A call waiting for the user's confirmation. */
data class PendingToolCall(
    val toolName: String,
    val arguments: Map<String, String>,
    val summary: String
)

sealed interface ToolResult {
    data class Ok(val toolName: String, val output: String) : ToolResult
    data class NeedsApproval(val pending: PendingToolCall) : ToolResult
    data class Refused(val toolName: String, val reason: String) : ToolResult
    data class Failed(val toolName: String, val reason: String) : ToolResult
}
