package dev.fitcoach.app.domain.model

/**
 * Which optional request parameters a given model/endpoint actually accepts.
 *
 * Providers are not interchangeable: OpenAI's reasoning models reject
 * `max_tokens`, `temperature` and `top_p`, while most OpenAI-compatible
 * providers only understand the classic `max_tokens`. The request must therefore
 * be built from the capability of the selected model, never from a fixed template.
 */
data class AiModelCapabilities(
    /** The model expects `max_completion_tokens` instead of `max_tokens`. */
    val usesMaxCompletionTokens: Boolean = false,
    /** The model rejects `temperature` and `top_p`. */
    val rejectsSamplingParameters: Boolean = false,
    /** The model rejects `response_format: json_object`. */
    val rejectsJsonMode: Boolean = false,
    /** The model does not support the legacy Chat Completions endpoint. */
    val requiresResponsesApi: Boolean = false,
    /** The model accepts `reasoning_effort`. */
    val supportsReasoningEffort: Boolean = false,
    /** Documented output limit, used to clamp the requested token budget. */
    val maxOutputTokensLimit: Int = 16384
) {
    /** Name of the token-budget parameter this model expects. */
    val tokenParameterName: String
        get() = if (usesMaxCompletionTokens) "max_completion_tokens" else "max_tokens"

    companion object {
        /**
         * Maps a model id and provider to its request capabilities. Matching is done
         * on the model id, because the same provider exposes models with different
         * rules under the same endpoint.
         */
        fun forModel(provider: AiProvider, modelId: String): AiModelCapabilities {
            val id = modelId.lowercase().trim()
            return when {
                isOpenAiReasoning(provider, id) -> AiModelCapabilities(
                    usesMaxCompletionTokens = true,
                    rejectsSamplingParameters = true,
                    rejectsJsonMode = true,
                    requiresResponsesApi = false,
                    supportsReasoningEffort = true,
                    maxOutputTokensLimit = 128_000
                )
                provider == AiProvider.GROQ && id.contains("reasoning") -> AiModelCapabilities(
                    usesMaxCompletionTokens = true,
                    rejectsSamplingParameters = true,
                    rejectsJsonMode = false,
                    supportsReasoningEffort = true,
                    maxOutputTokensLimit = 32_768
                )
                else -> AiModelCapabilities()
            }
        }

        private fun isOpenAiReasoning(provider: AiProvider, id: String): Boolean {
            if (provider != AiProvider.OPENAI && provider != AiProvider.CUSTOM) return false
            if (id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4")) return true
            return id.startsWith("gpt-5")
        }

        /** Clamps the requested output budget to what the model documents. */
        fun clampOutputTokens(requested: Int, capabilities: AiModelCapabilities): Int =
            requested.coerceIn(256, capabilities.maxOutputTokensLimit)
    }
}
