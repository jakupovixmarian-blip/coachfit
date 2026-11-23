package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.FoodItem

/**
 * Riconoscimento tollerante dei nomi del catalogo.
 *
 * L'AI scrive nomi plausibili in italiano corrente ("Uova intere", "Riso",
 * "Yogurt greco") mentre il catalogo usa la dicitura della fonte ("Uovo intero di
 * gallina", "Riso bianco (crudo)", "Yogurt greco 0% grassi"). Il confronto era
 * esatto: ogni sinonimo, plurale o qualificatore mancante faceva fallire la
 * validazione di un programma altrimenti corretto.
 *
 * I passi sono ordinati dal più stretto al più largo e ognuno è deterministico:
 * lo stesso nome produce sempre lo stesso risultato. Non viene mai scelto un
 * alimento arbitrario, perché un falso positivo falserebbe le calorie del piano.
 */
object CatalogNameResolver {

    /** Similarità minima per accettare un candidato nel passo fuzzy. */
    private const val MIN_SCORE = 0.70

    /** Sotto questa lunghezza il confronto fuzzy è troppo rischioso. */
    private const val MIN_FUZZY_LENGTH = 6

    /** Parole grammaticali che non distinguono un alimento da un altro. */
    private val STOPWORDS = setOf(
        "di", "da", "del", "della", "dei", "degli", "delle", "al", "allo", "alla",
        "con", "in", "a", "e", "ed", "il", "lo", "la", "i", "gli", "le", "un", "uno", "una"
    )

    fun resolveFood(query: String, foods: List<FoodItem>): FoodItem? =
        resolve(query, foods) { it.name }

    fun resolveExercise(query: String, exercises: List<Exercise>): Exercise? =
        resolve(query, exercises) { it.name }

    fun foodSuggestions(query: String, foods: List<FoodItem>, limit: Int = 3): List<String> =
        suggestions(query, foods) { it.name }.take(limit)

    fun exerciseSuggestions(query: String, exercises: List<Exercise>, limit: Int = 3): List<String> =
        suggestions(query, exercises) { it.name }.take(limit)

    private fun <T> resolve(query: String, items: List<T>, name: (T) -> String): T? {
        val q = FoodItem.normalizeName(query)
        if (q.isBlank()) return null

        // 1. Nome identico: il caso normale, nessuna ambiguità.
        items.firstOrNull { FoodItem.normalizeName(name(it)) == q }?.let { return it }

        // 1-bis. Sinonimo corrente ("spigola" -> "branzino", "cottage cheese" ->
        //    "fiocchi di latte"): si sostituisce e si ripete la ricerca, perche il
        //    catalogo puo aggiungere qualificatori ("Fiocchi di latte (cottage)").
        SYNONYMS[q]?.let { canonico ->
            val sostituito = FoodItem.normalizeName(canonico)
            if (sostituito != q) {
                items.firstOrNull { FoodItem.normalizeName(name(it)) == sostituito }?.let { return it }
                val candidato = items
                    .map { it to significantWords(singularize(FoodItem.normalizeName(name(it)))) }
                    .filter { (_, cWords) -> cWords.containsAll(significantWords(singularize(sostituito))) }
                    .minByOrNull { it.second.size }
                if (candidato != null) return candidato.first
            }
        }

        // 2. Stesso nome a meno di singolare/plurale ("uova" -> "uovo").
        val singular = singularize(q)
        if (singular != q) {
            items.firstOrNull { singularize(FoodItem.normalizeName(name(it))) == singular }
                ?.let { return it }
        }

        val qWords = significantWords(singular)
        if (qWords.isEmpty()) return null

        // 3. Le parole scritte compaiono tutte nel nome del catalogo, o viceversa:
        //    "uova intere" dentro "uovo intero di gallina", "riso" dentro "riso
        //    bianco (crudo)".
        //
        //    A parita' di corrispondenza vince il nome la cui PRIMA parola
        //    significativa coincide con la prima parola cercata: senza questo
        //    criterio vinceva il nome piu' corto, e "Riso" finiva su "Gallette di
        //    riso" (2 parole) invece che su "Riso bianco (crudo)" (3), "Uova" su
        //    "Albume d'uovo" invece che su "Uovo intero di gallina". Sono
        //    alimenti diversi: sbagliare cambia calorie e macro del piano.
        val qHead3 = qWords.first()
        items
            .map {
                Triple(
                    it,
                    significantWords(singularize(FoodItem.normalizeName(name(it)))),
                    FoodItem.normalizeName(name(it)).length
                )
            }
            .filter { (_, cWords, _) ->
                cWords.isNotEmpty() &&
                    (cWords.containsAll(qWords) || qWords.containsAll(cWords))
            }
            .minWithOrNull(
                compareBy(
                    { (_, cWords, _) -> if (cWords.first() == qHead3) 0 else 1 },
                    { (_, cWords, _) -> cWords.size },
                    { (_, _, len) -> len }
                )
            )
            ?.let { return it.first }

        // 4. Stessa testa (ultima parola significativa): "tagliata di manzo" e
        //    "fesa di manzo" sono lo stesso alimento in tagli diversi. Il
        //    candidato non può essere più dettagliato della richiesta, altrimenti
        //    "latte d'avena" finirebbe su "fiocchi d'avena", che è altro.
        val qHead = qWords.last()
        items
            .map { it to significantWords(singularize(FoodItem.normalizeName(name(it)))) }
            .filter { (_, cWords) ->
                cWords.size >= 2 && cWords.size <= qWords.size + 1 && cWords.last() == qHead
            }
            .minWithOrNull(compareBy({ it.second.size }, { FoodItem.normalizeName(name(it.first)).length }))
            ?.let { return it.first }

        // 5. Somiglianza lessicale, solo per nomi abbastanza lunghi.
        if (singular.length < MIN_FUZZY_LENGTH) return null
        return items
            .map { it to score(qWords, significantWords(singularize(FoodItem.normalizeName(name(it))))) }
            .filter { it.second >= MIN_SCORE }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun <T> suggestions(query: String, items: List<T>, name: (T) -> String): List<String> {
        val qWords = significantWords(singularize(FoodItem.normalizeName(query)))
        if (qWords.isEmpty()) return emptyList()
        return items
            .map { name(it) to score(qWords, significantWords(singularize(FoodItem.normalizeName(name(it))))) }
            .sortedByDescending { it.second }
            .filter { it.second > 0.25 }
            .map { it.first }
            .distinct()
    }

    /**
     * Riduce le parole al singolare italiano. Copre i casi che ricorrono nei nomi
     * alimentari, comprese le forme irregolari come "uova" -> "uovo".
     */
    internal fun singularize(normalized: String): String =
        normalized.split(' ').joinToString(" ") { word ->
            when {
                word.length <= 3 -> word
                word in IRREGULAR -> IRREGULAR[word]!!
                word.endsWith("che") || word.endsWith("ghe") -> word
                word.endsWith("i") -> word.dropLast(1) + "o"
                word.endsWith("e") -> word.dropLast(1) + "a"
                else -> word
            }
        }

    /**
     * Sinonimi correnti che il catalogo non riporta: l'utente e i modelli usano
     * "spigola" e "branzino" per lo stesso pesce, "cottage" per i fiocchi di
     * latte. Sono equivalenze reali, non somiglianze: senza di esse il programma
     * veniva respinto per un nome diverso ma corretto.
     */
    private val SYNONYMS = mapOf(
        "spigola" to "branzino",
        "orate" to "orata",
        "cottage" to "fiocchi di latte",
        "cottage cheese" to "fiocchi di latte",
        "greek yogurt" to "yogurt greco",
        "brown rice" to "riso integrale",
        "white rice" to "riso bianco",
        "chicken breast" to "petto di pollo",
        "salmon" to "salmone",
        "tuna" to "tonno in scatola al naturale",
        "uova" to "uovo intero di gallina",
        "uovo" to "uovo intero di gallina",
        "whole eggs" to "uovo intero di gallina",
        "eggs" to "uovo intero di gallina",
        "egg whites" to "albume d uovo",
        "olive oil" to "olio extravergine d oliva",
        "oats" to "fiocchi d avena",
        "oatmeal" to "fiocchi d avena",
        "ricotta" to "ricotta vaccina",
        "grana" to "grana padano",
        "parmigiano" to "grana padano",
        "yogurt" to "yogurt greco",
        "riso" to "riso bianco",
        "pasta" to "pasta di semola",
        "pane" to "pane bianco",
        "farina" to "farina di frumento",
        "olio" to "olio extravergine d oliva",
        "fagioli" to "fagioli borlotti",
        "lenticchie" to "lenticchie secche",
    )

    private val IRREGULAR = mapOf(
        "uova" to "uovo",
        "tuorli" to "tuorlo",
        "carni" to "carne",
        "pesci" to "pesce",
    )

    /**
     * Parole significative in ordine, ridotte alla radice per far combaciare le
     * concordanze ("intere"/"intero" -> "inte", "greci"/"greco" -> "grec").
     * L'ordine è conservato perché l'ultima parola è la testa del nome composto.
     */
    internal fun significantWords(normalized: String): List<String> =
        normalized.split(' ')
            .filter { it.isNotBlank() && it !in STOPWORDS }
            .map { word ->
                when {
                    word.length <= 4 -> word
                    else -> word.dropLast(1).let { if (it.length > 4 && it.last() in "aeo") it.dropLast(1) else it }
                }
            }

    /** Similarità fra due nomi, 0..1 (Jaccard sulle radici). */
    private fun score(a: List<String>, b: List<String>): Double {
        val sa = a.toSet()
        val sb = b.toSet()
        if (sa.isEmpty() || sb.isEmpty()) return 0.0
        if (sa == sb) return 1.0
        return sa.intersect(sb).size.toDouble() / sa.union(sb).size
    }
}
