package com.fitcoach.app.domain.knowledge

/**
 * Cita la regola del corpus pertinente in un messaggio di violazione.
 *
 * Perche esiste: un validatore che dice solo "massimo 4 serie" insegna al modello
 * a obbedire a un numero, non a capire la regola. Citando la fonte, il modello
 * puo' correggere nel modo giusto e l'utente vede da dove viene il limite.
 */
object CorpusCitation {

    /** Lunghezza massima della citazione: un messaggio d'errore deve restare leggibile. */
    private const val MAX_CHARS = 190

    /**
     * Prima regola della categoria che parla del parametro cercato.
     *
     * @param keyword restringe alle regole che citano il parametro (es. "serie").
     */
    fun cite(category: InvictusKnowledge.Category, keyword: String? = null): String? {
        val pool = InvictusKnowledge.forCategory(category)
        val rule = (keyword?.let { k ->
            pool.firstOrNull { it.statement.contains(k, ignoreCase = true) }
        } ?: pool.firstOrNull()) ?: return null
        val text = rule.statement.trim().let {
            if (it.length <= MAX_CHARS) it else it.take(MAX_CHARS).trimEnd() + "..."
        }
        return "$text (fonte: ${rule.articleTitle}, ${rule.url})"
    }

    /** Citazione da aggiungere a una violazione, vuota se la categoria non ha regole. */
    fun suffix(category: InvictusKnowledge.Category, keyword: String? = null): String =
        cite(category, keyword)?.let { " Riferimento dal corpus: $it" } ?: ""
}
