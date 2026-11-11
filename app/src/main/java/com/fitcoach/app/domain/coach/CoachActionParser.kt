package com.fitcoach.app.domain.coach

import kotlinx.serialization.json.Json

/**
 * Estrae una proposta di azione strutturata dalla risposta del modello.
 * Il modello puo restituire il blocco JSON dentro un code fence ```json ... ```
 * oppure come oggetto JSON isolato. Se il JSON e assente o non valido, la
 * risposta viene trattata come testo semplice.
 */
object CoachActionParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): CoachReply {
        val block = extractJsonBlock(raw) ?: return CoachReply(text = raw.trim())
        val action = runCatching { json.decodeFromString<CoachAction>(block) }.getOrNull()
        if (action == null || action.action.uppercase() !in CoachAction.VALID) {
            return CoachReply(text = cleanText(raw))
        }
        return CoachReply(
            text = cleanText(raw),
            proposedAction = action.copy(action = action.action.uppercase())
        )
    }

    private fun extractJsonBlock(raw: String): String? {
        val fence = Regex("```(?:json)?\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)
        fence.find(raw)?.let { return it.groupValues[1] }
        // Fallback: primo oggetto JSON bilanciato che contiene "action".
        val start = raw.indexOf('{')
        while (start >= 0) {
            val end = matchingBrace(raw, start)
            if (end > start) {
                val candidate = raw.substring(start, end + 1)
                if (candidate.contains("\"action\"")) return candidate
            }
            val next = raw.indexOf('{', start + 1)
            if (next <= start) break
        }
        return null
    }

    private fun matchingBrace(text: String, start: Int): Int {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    /** Rimuove il blocco JSON dalla risposta mostrata all'utente. */
    private fun cleanText(raw: String): String {
        val withoutFence = Regex("```(?:json)?\\s*\\{.*?\\}\\s*```", RegexOption.DOT_MATCHES_ALL)
            .replace(raw, "")
        return withoutFence.trim()
    }
}
