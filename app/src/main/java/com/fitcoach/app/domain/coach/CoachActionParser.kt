package com.fitcoach.app.domain.coach

import kotlinx.serialization.json.Json

/**
 * Estrae una proposta di azione strutturata dalla risposta del modello.
 * Il modello puo restituire il blocco JSON dentro un code fence ```json ... ```
 * oppure come oggetto JSON isolato. Se il JSON e assente o non valido, la
 * risposta viene trattata come testo semplice.
 */
object CoachActionParser {

    /**
     * `coerceInputValues` trasforma un `null` in un campo non nullable nel
     * valore di default. Senza, un solo `"title": null` scritto dal modello
     * faceva fallire il decode dell'intero oggetto: la proposta spariva e
     * l'utente vedeva solo il testo, senza poter confermare nulla.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

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

    private val FENCED =
        Regex("```(?:json)?\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)

    private fun extractJsonBlock(raw: String): String? {
        FENCED.find(raw)?.let { return it.groupValues[1] }
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

    /**
     * Rimuove dalla risposta mostrata all'utente sia i blocchi in code fence sia
     * gli oggetti JSON isolati. Prima venivano tolti solo i blocchi con fence:
     * quando il modello scriveva il JSON senza backtick, l'utente si trovava
     * `{"action": "DELOAD"}` in mezzo alla frase.
     */
    private fun cleanText(raw: String): String {
        var out = FENCED.replace(raw, "")
        var start = out.indexOf('{')
        while (start >= 0) {
            val end = matchingBrace(out, start)
            if (end > start && out.substring(start, end + 1).contains("\"action\"")) {
                out = out.removeRange(start, end + 1)
                start = out.indexOf('{')
            } else {
                val next = out.indexOf('{', start + 1)
                if (next <= start) break
                start = next
            }
        }
        return out
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([.,;:!?])"), "$1")
            .trim()
    }
}
