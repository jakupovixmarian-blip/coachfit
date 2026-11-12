package dev.fitcoach.app.domain.model

/** Supported AI providers. OpenAI is the default. */
enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val modelsUrlSuffix: String
) {
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        modelsUrlSuffix = "/models"
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "openai/gpt-4o-mini",
        modelsUrlSuffix = "/models"
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        modelsUrlSuffix = "/models"
    ),
    GROQ(
        id = "groq",
        displayName = "Groq",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        modelsUrlSuffix = "/models"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Provider personalizzato (OpenAI-compatible)",
        defaultBaseUrl = "https://api.example.com/v1",
        defaultModel = "",
        modelsUrlSuffix = "/models"
    );

    companion object {
        fun fromId(id: String): AiProvider = entries.firstOrNull { it.id == id } ?: OPENAI
    }
}

data class AiConfig(
    val provider: AiProvider = AiProvider.OPENAI,
    val baseUrl: String = AiProvider.OPENAI.defaultBaseUrl,
    val modelId: String = AiProvider.OPENAI.defaultModel,
    val maxOutputTokens: Int = 4096,
    val timeoutSeconds: Int = 120,
    val temperature: Double = 0.4,
    val topP: Double = 1.0,
    val useJsonMode: Boolean = true,
    val sendPhotosToAi: Boolean = false
) {
    fun effectiveBaseUrl(): String = baseUrl.trimEnd('/')
}

enum class ConnectionCheck { OK, AUTH_ERROR, MODEL_MISSING, RATE_LIMIT, INSUFFICIENT_CREDITS, TIMEOUT, NETWORK_ERROR, UNKNOWN }

data class ConnectionTestResult(
    val check: ConnectionCheck,
    val message: String,
    val availableModels: List<String> = emptyList()
)
