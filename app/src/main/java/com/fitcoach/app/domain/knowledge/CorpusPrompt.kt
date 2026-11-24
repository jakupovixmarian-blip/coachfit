package com.fitcoach.app.domain.knowledge

/**
 * Porta il corpus Project InVictus dentro il prompt di generazione.
 *
 * Perche esiste: il corpus (1046 regole, 35 categorie) era raggiungibile solo se
 * il modello decideva di chiamare `consult_invictus`. Nel prompt finivano invece
 * le 12 regole scritte a mano di [KnowledgeCorpus]: il modello generava schede e
 * diete senza vedere la fonte che l'app stessa dichiara di usare, e i validatori
 * controllavano numeri che nessuna regola collegava al corpus.
 *
 * Perche non si passa tutto: 1046 regole sono ~47.000 token. Si selezionano le
 * regole piu' informative per ogni categoria dell'ambito richiesto, con criteri
 * deterministici: contengono numeri e unita' di misura, non ripetono una regola
 * gia' scelta, non sono troppo lunghe per essere lette.
 */
object CorpusPrompt {

    /** Regole scelte per una categoria, nell'ordine in cui vengono mostrate. */
    data class Selection(val category: InvictusKnowledge.Category, val rules: List<InvictusKnowledge.Rule>)

    /**
     * Unita' che rendono una regola operativa: senza, la frase e' un'opinione.
     * Il peso maggiore va ai parametri che il generatore deve decidere.
     */
    private val units = listOf(
        "g/kg" to 4, "kcal" to 4, "serie" to 3, "ripetizion" to 3, "%" to 2,
        "settiman" to 2, "secondi" to 2, "minuti" to 2, "ora" to 1, "giorni" to 1, "grammi" to 2
    )

    /** Oltre questa lunghezza la regola e' prosa: costa contesto e dice poco. */
    private const val MAX_RULE_CHARS = 320

    /** Frammenti che indicano un titolo o una ricetta, non una regola. */
    private val noise = listOf(
        "ricetta", "ingredienti", "clicca qui", "iscriviti", "copyright",
        "tutti i diritti", "leggi anche", "potrebbe interessarti"
    )

    fun score(rule: InvictusKnowledge.Rule): Int {
        val text = rule.statement.lowercase()
        if (text.length > MAX_RULE_CHARS) return -1
        if (noise.any { text.contains(it) }) return -1
        val digits = text.count { it.isDigit() }
        // Una regola senza alcun numero non aiuta a decidere un parametro.
        if (digits == 0) return -1
        val unitScore = units.sumOf { (u, w) -> if (text.contains(u)) w else 0 }
        val lengthPenalty = text.length / 60
        return digits.coerceAtMost(12) + unitScore - lengthPenalty
    }

    /**
     * Seleziona le regole per le categorie dell'ambito.
     *
     * @param areas macro-aree da includere (allenamento e/o nutrizione)
     * @param perCategory quante regole per categoria
     */
    fun select(
        areas: List<InvictusKnowledge.Area>,
        perCategory: Int = 3
    ): List<Selection> {
        return InvictusKnowledge.Category.entries
            .filter { it.area in areas }
            .mapNotNull { category ->
                // La deduplica e' per categoria, non globale: il corpus assegna la
                // stessa frase a piu' categorie ("12-20 serie in ricomposizione"
                // riguarda volume e ciclizzazione). Scartarla globalmente la
                // toglieva dalla seconda categoria, e una categoria la cui unica
                // frase era gia' uscita restava fuori dal prompt.
                val seen = mutableSetOf<String>()
                val rules = InvictusKnowledge.forCategory(category)
                    .map { it to score(it) }
                    .filter { (_, s) -> s >= 0 }
                    .sortedWith(compareByDescending<Pair<InvictusKnowledge.Rule, Int>> { it.second }
                        .thenBy { it.first.statement.length })
                    .map { it.first }
                    .filter { seen.add(it.statement.trim().lowercase()) }
                    .take(perCategory)
                    // Una categoria povera (SUBSTITUTION ha una sola regola, senza
                    // cifre) resterebbe fuori dal prompt: il modello non saprebbe
                    // che le sostituzioni sono un parametro previsto. Meglio la
                    // regola disponibile senza numero che il silenzio.
                    .ifEmpty {
                        InvictusKnowledge.forCategory(category)
                            .filter { r -> noise.none { r.statement.lowercase().contains(it) } }
                            .filter { r -> seen.add(r.statement.trim().lowercase()) }
                            .take(1)
                    }
                if (rules.isEmpty()) null else Selection(category, rules)
            }
    }

    /**
     * Blocco di prompt con le regole del corpus e la fonte di ognuna.
     *
     * Le regole sono indicazioni di metodo, non cancelli: la stessa dicitura di
     * [KnowledgeCorpus] evita che il modello le tratti come vincoli assoluti.
     */
    /**
     * Budget in caratteri del blocco.
     *
     * Non e' una precauzione teorica: il README documenta che ~45.000 caratteri
     * di corpus appesi al prompt producono programmi non validi su un modello
     * piccolo (slot dei pasti vuoti, struttura fuori schema), mentre lo stesso
     * contesto di base passa. Misurato qui: 3 regole per categoria su entrambe le
     * aree fanno 23.922 caratteri, 2 ne fanno 23.743, 1 ne fa 12.797.
     *
     * Il default e' quindi 1 regola per categoria: il modello vede che la fonte
     * esiste e da dove viene ogni parametro, senza il contesto gonfiato che
     * rompeva la generazione. Chi vuole piu' profondita' usa consult_invictus.
     */
    const val MAX_BLOCK_CHARS = 16_000

    fun block(areas: List<InvictusKnowledge.Area>, perCategory: Int = 1): String = buildString {
        var omitted = 0
        appendLine("CORPUS PROJECT INVICTUS (estratto dalle fonti pubblicate, con categoria e fonte).")
        appendLine("Sono indicazioni di metodo, non cancelli: se te ne discosti, spiega perche' citando la fonte.")
        select(areas, perCategory).forEach { selection ->
            val block = buildString {
                appendLine("[${selection.category.label}]")
                selection.rules.forEach { r ->
                    appendLine("- ${r.statement}")
                    appendLine("  Fonte: ${r.articleTitle} (${r.url}) - ${r.publishedDate}, ${r.kind.label}")
                }
            }
            // Le categorie sono in ordine di enum, non di importanza: fermarsi qui
            // taglierebbe sempre le ultime (nutrizione). Si tiene la categoria se
            // c'e' spazio, e si annota quante sono state omesse.
            if (length + block.length > MAX_BLOCK_CHARS) {
                omitted++
            } else {
                append(block)
            }
        }
        if (omitted > 0) {
            appendLine()
            appendLine("($omitted categorie omesse per limite di contesto: chiedile con consult_invictus se servono.)")
        }
    }.trimEnd()
}
