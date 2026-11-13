package dev.fitcoach.app.domain.search

import dev.fitcoach.app.domain.model.Food
import kotlin.math.max
import kotlin.math.min

data class FoodMatch(val food: Food, val score: Double)

/**
 * Italian-aware matching for the food catalog.
 *
 * The catalog mixes official tables ("Petto di pollo, crudo"), packaged products
 * ("Yogurt greco 0% - Fage") and foods typed by the user ("petto pollo"). A plain
 * equality search finds almost none of them, so queries are normalised (accents,
 * punctuation, articles and prepositions removed), compared on whole words first
 * and only then by edit distance for typos.
 *
 * The score is a ranking aid, never proof that two records are the same food:
 * crudo and cotto are different foods and stay different records.
 */
object FoodMatcher {

    private val ACCENTS = mapOf(
        'à' to 'a', 'á' to 'a', 'â' to 'a', 'ä' to 'a',
        'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
        'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
        'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'ö' to 'o',
        'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c', 'ñ' to 'n'
    )

    private val STOPWORDS = setOf(
        "di", "del", "dello", "della", "dei", "degli", "delle",
        "con", "al", "allo", "alla", "ai", "agli", "alle",
        "il", "lo", "la", "i", "gli", "le", "un", "uno", "una",
        "per", "senza", "e", "ed", "da", "dal", "dalla", "in", "su"
    )

    /** Lowercase, accent-free, punctuation-free, single-spaced. */
    fun normalize(text: String): String {
        val lowered = text.lowercase()
        val builder = StringBuilder(lowered.length)
        for (char in lowered) {
            val mapped = ACCENTS[char] ?: char
            if (mapped.isLetterOrDigit()) builder.append(mapped) else builder.append(' ')
        }
        return builder.toString().trim().replace(WHITESPACE, " ")
    }

    /** Meaningful words of a name, without articles and prepositions. */
    fun tokens(text: String): List<String> = normalize(text)
        .split(' ')
        .filter { it.length > 1 && it !in STOPWORDS }

    fun score(query: String, candidate: String): Double {
        val q = normalize(query)
        val c = normalize(candidate)
        if (q.isEmpty() || c.isEmpty()) return 0.0
        if (q == c) return 1.0

        var best = 0.0
        best = max(best, when {
            c.startsWith("$q ") -> 0.92
            c.contains(" $q ") -> 0.88
            c.endsWith(" $q") -> 0.84
            c.contains(q) -> 0.78
            else -> 0.0
        })

        val queryTokens = tokens(query)
        if (queryTokens.isNotEmpty()) {
            val candidateTokens = tokens(candidate)
            val matched = queryTokens.count { qt ->
                candidateTokens.any { ct -> ct == qt || ct.startsWith(qt) || similarity(ct, qt) >= 0.85 }
            }
            val overlap = matched.toDouble() / queryTokens.size
            if (overlap > 0.0) best = max(best, 0.55 + 0.30 * overlap)
        }

        if (best == 0.0) best = similarity(q, c) * 0.6
        return best
    }

    fun rank(query: String, foods: List<Food>, limit: Int = 20, minimumScore: Double = 0.5): List<FoodMatch> =
        foods.asSequence()
            .map { FoodMatch(it, score(query, it.name)) }
            .filter { it.score >= minimumScore }
            .sortedWith(compareByDescending<FoodMatch> { it.score }.thenBy { it.food.name.length })
            .take(limit)
            .toList()

    /**
     * Identity of a record for deduplication. A barcode identifies a packaged
     * product exactly; otherwise name, brand and state are used, because cooked and
     * raw versions of the same food are different records.
     */
    fun dedupeKey(food: Food): String {
        val barcode = food.barcode?.trim()
        if (!barcode.isNullOrBlank()) return "barcode:$barcode"
        return "${normalize(food.name)}|${normalize(food.brand)}|${food.state.name}"
    }

    /** Groups of records that describe the same food. Never merges them silently. */
    fun duplicateGroups(foods: List<Food>): List<List<Food>> = foods
        .groupBy { dedupeKey(it) }
        .values
        .filter { it.size > 1 }

    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val longest = max(a.length, b.length)
        if (longest == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / longest
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = min(
                    min(current[j - 1] + 1, previous[j] + 1),
                    previous[j - 1] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    private val WHITESPACE = Regex("\\s+")
}
