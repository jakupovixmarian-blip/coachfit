package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider

/**
 * Nome del parametro che limita i token generati, per la Chat Completions API.
 *
 * - I provider compatibili OpenAI "storici" (OpenAI stessa per i modelli non
 *   di reasoning, Groq, OpenRouter, DeepSeek, endpoint custom) usano `max_tokens`.
 * - I modelli OpenAI piu recenti (serie o1/o3/o4, gpt-4.1, gpt-5 e relative
 *   varianti) rifiutano `max_tokens` e richiedono `max_completion_tokens`.
 *
 * Mai inviare entrambi: il payload ne contiene uno solo (vedi [ChatCompletionRequest]).
 */
enum class TokenParam(val wireName: String) {
    MAX_TOKENS("max_tokens"),
    MAX_COMPLETION_TOKENS("max_completion_tokens")
}

/**
 * Determina il parametro token corretto per provider e modello.
 *
 * La logica e volutamente basata su regole esplicite e verificabili, non su una
 * sostituzione globale: ogni provider mantiene il proprio comportamento.
 */
object AiParameterPolicy {

    /** Modelli OpenAI che richiedono `max_completion_tokens` (match per prefisso). */
    private val OPENAI_NEW_TOKEN_MODELS = listOf(
        "o1", "o3", "o4", "gpt-4.1", "gpt-5", "gpt-6", "gpt-7"
    )

    fun tokenParamFor(provider: AiProvider, model: String): TokenParam {
        if (provider != AiProvider.OPENAI) return TokenParam.MAX_TOKENS
        return if (requiresNewTokenParam(model)) {
            TokenParam.MAX_COMPLETION_TOKENS
        } else {
            TokenParam.MAX_TOKENS
        }
    }

    /** True se il modello OpenAI richiede `max_completion_tokens`. */
    fun requiresNewTokenParam(model: String): Boolean {
        val m = model.trim().lowercase()
        if (m.isEmpty()) return false
        // Famiglie gpt-N con N >= 5 richiedono il nuovo parametro.
        val gpt = Regex("^gpt-(\\d+)").find(m)
        if (gpt != null && (gpt.groupValues[1].toIntOrNull() ?: 0) >= 5) return true
        return OPENAI_NEW_TOKEN_MODELS.any { m == it || m.startsWith("$it-") || m.startsWith("$it.") }
    }

    /**
     * Rileva l'errore specifico restituito dai modelli OpenAI quando si usa il
     * parametro sbagliato, cosi il client puo ritentare con l'altro parametro.
     */
    fun isUnsupportedTokenParamError(message: String): Boolean {
        val m = message.lowercase()
        if (!m.contains("max_tokens") && !m.contains("max_completion_tokens")) return false
        return m.contains("unsupported") ||
            m.contains("not supported") ||
            m.contains("unknown") ||
            m.contains("unrecognized") ||
            m.contains("invalid")
    }

    /**
     * Rileva l'errore restituito dai modelli che accettano SOLO la temperatura di
     * default (es. gpt-6-luna): "Unsupported value: 'temperature' does not support
     * 0.0 with this model. Only the default (1) value is supported.".
     */
    fun isUnsupportedTemperatureError(message: String): Boolean {
        val m = message.lowercase()
        if (!m.contains("temperature")) return false
        return m.contains("does not support") ||
            m.contains("unsupported") ||
            m.contains("only the default")
    }

    /**
     * Rileva il rifiuto dei function tools perche il modello applica un
     * reasoning_effort non nullo (OpenAI gpt-5.x su /v1/chat/completions):
     * "Function tools with reasoning_effort are not supported for gpt-5.6 in
     * /v1/chat/completions. To use function tools, use /v1/responses or set
     * reasoning_effort to \"none\"".
     */
    fun isFunctionToolsWithReasoningError(message: String): Boolean {
        val m = message.lowercase()
        if (!m.contains("reasoning_effort") && !m.contains("reasoning effort")) return false
        return m.contains("function tools") || m.contains("tools with") ||
            m.contains("not supported") || m.contains("tool")
    }

    /**
     * True se il provider supporta la modalita Thinking con il campo
     * `thinking: {"type": ...}` (DeepSeek). Gli altri provider non devono
     * ricevere il campo: lo rifiuterebbero come sconosciuto.
     */
    fun supportsThinkingMode(provider: AiProvider): Boolean = provider == AiProvider.DEEPSEEK

    /**
     * Con Thinking attivo DeepSeek accetta solo `tool_choice = auto`: qualunque
     * scelta forzata (`required` oppure un nome specifico) produce
     * "Thinking mode does not support this tool_choice".
     */
    fun isThinkingToolChoiceError(message: String): Boolean {
        val m = message.lowercase()
        if (!m.contains("thinking")) return false
        return m.contains("tool_choice") || m.contains("tool choice")
    }

    /** Rileva il rifiuto del campo `reasoning_effort` (endpoint che non lo conosce). */
    fun isUnknownReasoningEffortError(message: String): Boolean {
        val m = message.lowercase()
        if (!m.contains("reasoning_effort") && !m.contains("reasoning effort")) return false
        return m.contains("unknown") || m.contains("unrecognized") ||
            m.contains("unsupported") || m.contains("not supported") ||
            m.contains("invalid") || m.contains("unexpected")
    }
}
