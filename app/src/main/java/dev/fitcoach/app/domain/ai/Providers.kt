package dev.fitcoach.app.domain.ai

/** The provider families the app can talk to. */
enum class ProviderKind { OPENAI, GROQ, OPENROUTER, DEEPSEEK, GEMINI, CUSTOM }

/**
 * A configured provider.
 *
 * The key is kept here only at runtime and is never printed: [ProviderManager.maskedKey]
 * is what the UI and the logs are allowed to show.
 */
data class ProviderConfig(
    val id: String,
    val kind: ProviderKind,
    val displayName: String,
    val baseUrl: String,
    val modelId: String,
    val apiKey: String? = null,
    val maxTokens: Int = 1024
)

sealed interface ProviderReadiness {
    data object Ready : ProviderReadiness
    data object MissingApiKey : ProviderReadiness
    data object MissingBaseUrl : ProviderReadiness
    data object MissingModel : ProviderReadiness
}

enum class Role { SYSTEM, USER, ASSISTANT, TOOL }

data class ChatMessage(
    val role: Role,
    val content: String,
    val toolName: String? = null
)

data class ToolCallRequest(
    val id: String,
    val toolName: String,
    val arguments: Map<String, String>
)

/** What the model returned for one turn. */
data class ModelReply(
    val text: String? = null,
    val toolCalls: List<ToolCallRequest> = emptyList()
)

/** Transport for one provider family. Implemented per API dialect in the data layer. */
interface AiClient {
    suspend fun complete(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        tools: List<ToolSpec>
    ): ModelReply
}

/**
 * Chooses which provider to use, and never pretends one is usable when it is not.
 *
 * The chain is the primary provider followed by the fallback and then any other
 * ready provider. A provider without a key, a base URL or a model is left out of
 * the chain and named in [unavailableReason], so the UI can say what is missing
 * instead of failing silently.
 */
class ProviderManager(
    private val configs: List<ProviderConfig>,
    private val primaryId: String?,
    private val fallbackId: String? = null
) {

    fun all(): List<ProviderConfig> = configs

    fun primary(): ProviderConfig? = configs.firstOrNull { it.id == primaryId }

    fun fallback(): ProviderConfig? = fallbackId?.let { id -> configs.firstOrNull { it.id == id } }

    fun readiness(config: ProviderConfig): ProviderReadiness = when {
        config.baseUrl.isBlank() -> ProviderReadiness.MissingBaseUrl
        config.modelId.isBlank() -> ProviderReadiness.MissingModel
        config.apiKey.isNullOrBlank() -> ProviderReadiness.MissingApiKey
        else -> ProviderReadiness.Ready
    }

    fun readiness(id: String): ProviderReadiness? =
        configs.firstOrNull { it.id == id }?.let { readiness(it) }

    /** Primary, then fallback, then the other ready providers, without duplicates. */
    fun orderedChain(): List<ProviderConfig> {
        val preferred = listOfNotNull(primary(), fallback()).distinctBy { it.id }
        val preferredIds = preferred.map { it.id }.toSet()
        val rest = configs.filter { it.id !in preferredIds }
        return (preferred + rest).filter { readiness(it) is ProviderReadiness.Ready }
    }

    /** Switching provider is a configuration change, not a code change. */
    fun selectPrimary(id: String): ProviderManager = ProviderManager(configs, id, fallbackId)

    fun selectFallback(id: String?): ProviderManager = ProviderManager(configs, primaryId, id)

    fun unavailableReason(): String {
        if (configs.isEmpty()) return "Nessun provider AI configurato."
        val details = configs.joinToString("; ") { config ->
            val state = when (val readiness = readiness(config)) {
                is ProviderReadiness.Ready -> "pronto"
                is ProviderReadiness.MissingApiKey -> "chiave API mancante"
                is ProviderReadiness.MissingBaseUrl -> "URL di base mancante"
                is ProviderReadiness.MissingModel -> "modello mancante"
            }
            "${config.displayName}: $state"
        }
        return "Nessun provider AI utilizzabile ($details)."
    }

    /** What may be shown: never the key itself. */
    fun maskedKey(config: ProviderConfig): String {
        val key = config.apiKey
        if (key.isNullOrBlank()) return "non configurata"
        if (key.length <= 4) return "••••"
        return "••••" + key.takeLast(4)
    }
}

/** Sends the request through the client registered for the provider family. */
class ProviderGateway(private val clients: Map<ProviderKind, AiClient>) {

    fun hasClient(kind: ProviderKind): Boolean = clients.containsKey(kind)

    suspend fun complete(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        tools: List<ToolSpec>
    ): Result<ModelReply> {
        val client = clients[config.kind]
            ?: return Result.failure(IllegalStateException("Nessun client per ${config.kind}."))
        return runCatching { client.complete(config, messages, tools) }
    }
}
