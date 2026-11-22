package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.UserProfile

/**
 * Seleziona gli alimenti da mostrare al Coach AI durante la generazione.
 *
 * Il database interno contiene ~7.800 voci USDA (FoodData Central) in inglese,
 * molte delle quali sono prodotti industriali o piatti da ristorazione
 * (es. "APPLEBEE'S ...", "ARBY'S ..."). Prendendo i primi alimenti in ordine
 * alfabetico, il modello riceveva SOLO voci in inglese di fast food: da qui i
 * piani con nomi stranieri e poco coerenti.
 *
 * Regole della selezione:
 *  1. si parte dagli alimenti CREA (tabelle italiane, nomi italiani): hanno la
 *     priorita e sono quelli che l'utente riconosce;
 *  2. si escludono le categorie non pertinenti a un piano alimentare
 *     (ristorazione, fast food, alimenti per l'infanzia, bevande);
 *  3. si completa con alimenti USDA "generici" solo se serve, evitando i nomi
 *     che sembrano prodotti di marca;
 *  4. si mantiene un tetto massimo per non eccedere il contesto del modello.
 */
object FoodCatalogSelector {

    /** Categorie escluse: non sono alimenti base di un piano nutrizionale. */
    private val excludedCategories = setOf(
        "Ristorazione",
        "Fast food",
        "Alimenti per l'infanzia",
        "Bevande",
        "Snack",
        "Dolci"
    )

    /** Un nome "di marca" contiene apostrofi, virgole o parole tipiche del confezionato. */
    private val brandPattern = Regex("""['’,]|\b(INC|LLC|KRAFT|COMPANY|BRAND|RESTAURANT|MCDONALD)\b""")

    fun forGeneration(
        all: List<FoodItem>,
        profile: UserProfile,
        limit: Int = 90
    ): List<FoodItem> {
        val allowed = FoodFilter.allowedFoods(all, profile)
        val italian = allowed.filter { isItalianSource(it) && it.category !in excludedCategories }
        val generic = allowed.filter {
            !isItalianSource(it) && it.category !in excludedCategories && !brandPattern.containsMatchIn(it.name)
        }
        val ordered = (italian + generic).distinctBy { FoodItem.normalizeName(it.name) }

        // I gusti vanno applicati PRIMA del troncamento: con ~173 voci e un tetto
        // di 90, un alimento gradito che stava oltre la posizione 90 non arrivava
        // mai al modello, che quindi componeva pasti standard ignorando le
        // preferenze dichiarate dall'utente.
        val liked = profile.likedFoods.map { it.trim() }.filter { it.isNotEmpty() }
        if (liked.isEmpty()) return ordered.take(limit)

        fun matches(food: FoodItem, term: String): Boolean =
            food.name.contains(term, ignoreCase = true) || food.category.contains(term, ignoreCase = true)

        val preferred = ordered.filter { food -> liked.any { matches(food, it) } }
        // Rispetta l'ordine in cui l'utente ha elencato i gusti.
        val ranked = liked.flatMap { term -> preferred.filter { matches(it, term) } }
            .distinctBy { FoodItem.normalizeName(it.name) }
        val rest = ordered.filterNot { food -> liked.any { matches(food, it) } }

        return (ranked + rest).take(limit)
    }

    private fun isItalianSource(food: FoodItem): Boolean =
        food.source.startsWith("CREA", ignoreCase = true)
}
