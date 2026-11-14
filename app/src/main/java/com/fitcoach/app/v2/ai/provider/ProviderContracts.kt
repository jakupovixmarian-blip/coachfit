package com.fitcoach.app.v2.ai.provider

/** Provider supportati dal contratto AI 2.0. */
enum class ProviderKind(val displayName: String) {
    OPENAI("OpenAI"),
    OPENROUTER("OpenRouter"),
    DEEPSEEK("DeepSeek"),
    GROQ("Groq"),
    CUSTOM("Compatibile OpenAI")
}

enum class TokenField {
    MAX_TOKENS,
    MAX_COMPLETION_TOKENS
}

data class ProviderCapabilities(
    val tokenField: TokenField,
    val supportsTemperature: Boolean,
    val supportsJsonObject: Boolean,
    val supportsJsonSchema: Boolean,
    val supportsFunctionCalling: Boolean,
    val supportsModelDiscovery: Boolean
)

data class ProviderDefinition(
    val kind: ProviderKind,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val capabilities: ProviderCapabilities
) {
    val completionsPath: String = "/chat/completions"

    fun capabilitiesFor(model: String): ProviderCapabilities {
        if (kind != ProviderKind.OPENAI) return capabilities
        val normalized = model.trim().lowercase()
        val reasoning = normalized.startsWith("o1") || normalized.startsWith("o3") ||
            normalized.startsWith("o4") || normalized.startsWith("gpt-5")
        return capabilities.copy(
            tokenField = if (reasoning) TokenField.MAX_COMPLETION_TOKENS else TokenField.MAX_TOKENS,
            supportsTemperature = !reasoning
        )
    }
}

/**
 * Built-in defaults describe endpoint capabilities, not user credentials.
 * The model remains editable and can be refreshed through model discovery.
 */
object ProviderCatalog {
    private val classic = ProviderCapabilities(
        tokenField = TokenField.MAX_TOKENS,
        supportsTemperature = true,
        supportsJsonObject = true,
        supportsJsonSchema = false,
        supportsFunctionCalling = true,
        supportsModelDiscovery = true
    )

    val definitions: List<ProviderDefinition> = listOf(
        ProviderDefinition(
            ProviderKind.OPENAI,
            "https://api.openai.com/v1",
            // Listed in the official OpenAI models documentation on 2026-11-12.
            // The connection test and model discovery remain authoritative.
            "gpt-5.6",
            ProviderCapabilities(
                tokenField = TokenField.MAX_COMPLETION_TOKENS,
                supportsTemperature = true,
                supportsJsonObject = true,
                supportsJsonSchema = true,
                supportsFunctionCalling = true,
                supportsModelDiscovery = true
            )
        ),
        ProviderDefinition(ProviderKind.OPENROUTER, "https://openrouter.ai/api/v1", "openai/gpt-5.6", classic),
        ProviderDefinition(ProviderKind.DEEPSEEK, "https://api.deepseek.com/v1", "deepseek-chat", classic),
        ProviderDefinition(ProviderKind.GROQ, "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", classic),
        ProviderDefinition(ProviderKind.CUSTOM, "", "", classic)
    )

    fun forKind(kind: ProviderKind): ProviderDefinition =
        definitions.first { it.kind == kind }
}

data class ProviderConfiguration(
    val provider: ProviderKind,
    val baseUrl: String,
    val model: String,
    /** Alias in the secure credential store. Never an API key. */
    val credentialRef: String?,
    val usesBackendProxy: Boolean = false,
    val maxOutputTokens: Int,
    val timeoutMs: Long
) {
    fun endpoint(path: String = "/chat/completions"): String =
        baseUrl.trim().trimEnd('/') + "/" + path.trim().trimStart('/')
}

interface CredentialStore {
    suspend fun read(ref: String): String?
}

data class AiRequest(
    val system: String? = null,
    val user: String,
    val temperature: Double? = null,
    val jsonObject: Boolean = false,
    val toolsJson: String? = null
)

sealed interface AiCompletion {
    data class Success(val content: String, val model: String?, val usage: TokenUsage?) : AiCompletion
    data class ToolCalls(val rawJson: String, val model: String?) : AiCompletion
    data class Truncated(val partialContent: String?, val model: String?) : AiCompletion
    data class Refused(val message: String, val model: String?) : AiCompletion
    data class Failure(val reason: FailureReason, val message: String, val httpCode: Int? = null) : AiCompletion
}

enum class FailureReason {
    AUTHENTICATION,
    AUTHORIZATION,
    MODEL_NOT_FOUND,
    INVALID_REQUEST,
    RATE_LIMIT,
    CREDITS,
    TIMEOUT,
    NETWORK,
    SERVER,
    EMPTY_RESPONSE,
    INVALID_JSON,
    CONFIGURATION,
    UNKNOWN
}

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class ModelDescriptor(
    val id: String,
    val ownedBy: String? = null,
    val supportsChat: Boolean = true
)

sealed interface ConnectionTest {
    data class Success(val model: String, val latencyMs: Long) : ConnectionTest
    data class Failure(val result: AiCompletion.Failure) : ConnectionTest
}
