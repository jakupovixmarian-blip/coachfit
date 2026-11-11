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
        "o1", "o3", "o4", "gpt-4.1", "gpt-5"
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
}
