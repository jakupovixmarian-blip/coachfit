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

    /**
     * Un nome "di marca" contiene una parola tutta maiuscola (i prodotti
     * statunitensi sono scritti cosi: "APPLEBEE'S, french fries") oppure un
     * suffisso societario.
     *
     * L'apostrofo e la virgola NON sono indizi di marca: in italiano sono
     * normali ("Fiocchi d'avena", "Olio extravergine d'oliva"). Trattarli come
     * tali escludeva alimenti base dal catalogo mostrato al modello.
     */
    private val brandPattern = Regex("""\b[A-Z]{4,}[A-Z'&.]*\b|\b(INC|LLC|KRAFT|COMPANY|BRAND|RESTAURANT|MCDONALD)\b""")

    /**
     * Voci del database globale USDA (SR Legacy, ~7.800) importate con il nome
     * originale inglese: "Cheese, cottage, lowfat, 1% milkfat", "Egg, white,
     * dried, stabilized, glucose reduced".
     *
     * L'app e' in italiano e quei nomi non sono utilizzabili in un piano: il
     * modello o li ignora o li traduce in nomi che poi la validazione non
     * riconosce ("Banane" contro "Banana"). Restano nel database per la ricerca
     * manuale dell'utente, ma non entrano nel catalogo della generazione.
     *
     * Le voci del seed curato hanno nomi italiani anche quando la fonte e'
     * USDA, e si distinguono perche' la loro sorgente non e' quella dell'import.
     */
    private fun isImportedForeignEntry(food: FoodItem): Boolean =
        food.source == FoodAssetImporter.USDA_SOURCE

    fun forGeneration(
        all: List<FoodItem>,
        profile: UserProfile,
        limit: Int = DEFAULT_LIMIT
    ): List<FoodItem> {
        val allowed = FoodFilter.allowedFoods(all, profile)
        // Solo nomi italiani: le ~7.800 voci inglesi dell'import USDA occupavano
        // 166 posizioni su 200 e spingevano fuori verdura, frutta, cereali e olio.
        val italianNamed = allowed.filterNot { isImportedForeignEntry(it) }
        val italian = italianNamed.filter { isItalianSource(it) && it.category !in excludedCategories }
        val generic = italianNamed.filter {
            !isItalianSource(it) && it.category !in excludedCategories && !brandPattern.containsMatchIn(it.name)
        }
        val ordered = (italian + generic).distinctBy { FoodItem.normalizeName(it.name) }

        val liked = profile.likedFoods.map { it.trim() }.filter { it.isNotEmpty() }

        if (liked.isEmpty()) return balancedByCategory(ordered, limit)

        fun matchesName(food: FoodItem, term: String): Boolean = food.name.contains(term, ignoreCase = true)

        // Il nome vale piu' della categoria: "uova" come categoria trascinava
        // dentro ogni derivato, mentre l'utente intendeva le uova.
        val likedFoods = liked.flatMap { term ->
            val byName = ordered.filter { matchesName(it, term) }
            val byCategory = ordered.filter {
                !matchesName(it, term) && it.category.contains(term, ignoreCase = true)
            }
            byName + byCategory
        }.distinctBy { FoodItem.normalizeName(it.name) }

        // I gusti hanno uno slot riservato: la ripartizione per categoria si
        // applica solo al resto. Bilanciando l'intero catalogo, un alimento
        // gradito che cadeva oltre la quota della sua categoria veniva scartato
        // dal taglio e non arrivava mai al modello.
        val likedKeys = likedFoods.map { FoodItem.normalizeName(it.name) }.toSet()
        val rest = ordered.filterNot { FoodItem.normalizeName(it.name) in likedKeys }
        val budget = (limit - likedFoods.size).coerceAtLeast(0)

        return (likedFoods.take(limit) + balancedByCategory(rest, budget))
            .distinctBy { FoodItem.normalizeName(it.name) }
            .take(limit)
    }

    /**
     * Ripartisce il tetto fra le categorie invece di prendere le prime N voci.
     *
     * Con un semplice `take(limit)` l'ordine decideva il contenuto: il catalogo
     * curato elenca prima le fonti proteiche, quindi le prime posizioni
     * finivano occupate da carne, pesce, latticini e uova. Verdura, frutta,
     * cereali e grassi restavano fuori e il solver segnalava "nessuna fonte di
     * grassi nella giornata" pur avendo l'olio nel database.
     *
     * Si distribuisce una voce per categoria a turno: ogni categoria riceve la
     * sua quota e il risultato non supera mai [limit].
     */
    private fun balancedByCategory(ordered: List<FoodItem>, limit: Int): List<FoodItem> {
        if (limit <= 0) return emptyList()
        if (ordered.size <= limit) return ordered

        // Prima gli alimenti base: sono quelli su cui si costruisce ogni piano
        // (pollo, uova, riso, olio). La sola rotazione alfabetica li escludeva
        // quando il tetto era basso, lasciando fuori proprio le fonti principali.
        val staples = ordered.filter { it.name in CORE_STAPLES }
        val picked = staples.take(limit).toMutableList()
        val staplesKeys = picked.map { FoodItem.normalizeName(it.name) }.toSet()

        val byCategory = ordered
            .filterNot { FoodItem.normalizeName(it.name) in staplesKeys }
            .groupBy { it.category }
        val cursors = byCategory.keys.associateWith { 0 }.toMutableMap()

        // Un giro alla volta, una voce per categoria: nessuna categoria resta
        // privilegiata e il tetto viene rispettato esattamente.
        var progress = true
        while (picked.size < limit && progress) {
            progress = false
            for ((category, foods) in byCategory) {
                if (picked.size >= limit) break
                val cursor = cursors[category] ?: 0
                if (cursor < foods.size) {
                    picked += foods[cursor]
                    cursors[category] = cursor + 1
                    progress = true
                }
            }
        }

        // Ordine stabile: si conserva l'ordine del catalogo (CREA prima).
        val selectedKeys = picked.map { FoodItem.normalizeName(it.name) }.toSet()
        return ordered.filter { FoodItem.normalizeName(it.name) in selectedKeys }
    }

    /**
     * Tetto del catalogo mostrato al modello.
     *
     * Il catalogo curato conta ~250 voci: il tetto e' tenuto sopra quella soglia
     * perche' il modello veda TUTTO il catalogo italiano. Il taglio a 200
     * lasciava fuori 54 alimenti, e con essi intere categorie a seconda
     * dell'ordine. La ripartizione per categoria resta come rete di sicurezza
     * per l'eventuale crescita futura del catalogo.
     */
    const val DEFAULT_LIMIT = 320

    /**
     * Alimenti base presenti in ogni piano nutrizionale.
     *
     * Devono arrivare sempre al modello, anche quando il catalogo viene ridotto
     * per un contesto piccolo: senza di essi il piano non si puo comporre.
     */
    private val CORE_STAPLES = setOf(
        "Petto di pollo", "Uovo intero di gallina", "Albume d'uovo",
        "Merluzzo", "Salmone", "Tonno in scatola al naturale",
        "Yogurt greco 0% grassi", "Latte parzialmente scremato", "Ricotta vaccina",
        "Riso bianco (crudo)", "Pasta di semola (cruda)", "Fiocchi d'avena",
        "Pane integrale", "Patate", "Patate dolci",
        "Olio extravergine d'oliva", "Burro d'arachidi (100% arachidi)", "Mandorle",
        "Lenticchie cotte", "Ceci cotti", "Fagioli cannellini cotti",
        "Banana", "Mela", "Spinaci", "Broccoli", "Zucchine", "Pomodori", "Carote",
        "Mozzarella vaccina", "Parmigiano reggiano", "Bresaola", "Prosciutto crudo sgrassato"
    )

    private fun isItalianSource(food: FoodItem): Boolean =
        food.source.startsWith("CREA", ignoreCase = true)
}
