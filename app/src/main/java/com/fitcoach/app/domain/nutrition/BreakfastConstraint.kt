package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MealSlot

/**
 * La preferenza "colazione dolce/salata" dichiarata nell'anamnesi era scritta
 * nel prompt della chat ma NON in quello di generazione, e non era verificata
 * da nessun controllo: il piano usciva con petto di pollo e riso a colazione
 * pur avendo l'utente scelto "dolce".
 *
 * Qui la preferenza diventa un vincolo verificabile. La classificazione si
 * basa sulla categoria del catalogo e su un elenco esplicito di alimenti, non
 * su euristiche sul nome: un alimento non classificato non viene mai bocciato
 * (un falso rifiuto bloccherebbe la generazione per un dubbio).
 */
object BreakfastConstraint {

    /** Alimenti che rendono salata una colazione, per categoria di catalogo. */
    private val SAVORY_CATEGORIES = setOf("Carne", "Pesce", "Salumi", "Uova", "Legumi")

    /**
     * Alimenti salati che appartengono a categorie ambigue: il pane e i formaggi
     * compaiono anche in colazioni dolci, quindi non bastano da soli.
     */
    private val SAVORY_NAMES = setOf(
        "Pane bianco", "Pane integrale", "Prosciutto crudo sgrassato", "Bresaola",
        "Salmone affumicato", "Tonno in scatola al naturale", "Fiocchi di latte (cottage)"
    )

    /** Alimenti che rendono dolce una colazione. */
    private val SWEET_CATEGORIES = setOf("Frutta")

    private val SWEET_NAMES = setOf(
        "Miele", "Marmellata", "Confettura", "Zucchero", "Cacao amaro in polvere",
        "Fiocchi d'avena", "Fette biscottate", "Yogurt greco 0% grassi",
        "Yogurt bianco magro", "Latte parzialmente scremato", "Latte scremato",
        "Bevanda di soia non zuccherata", "Ricotta vaccina"
    )

    enum class Flavor { SWEET, SAVORY, NEUTRAL }

    fun flavorOf(food: FoodItem): Flavor {
        if (food.category in SWEET_CATEGORIES || food.name in SWEET_NAMES) return Flavor.SWEET
        if (food.category in SAVORY_CATEGORIES || food.name in SAVORY_NAMES) return Flavor.SAVORY
        return Flavor.NEUTRAL
    }

    /**
     * Alimenti che violano la preferenza per il pasto [slot].
     *
     * Solo la colazione ha un vincolo di sapore: pranzo e cena sono salati per
     * convenzione e uno spuntino puo' essere entrambi.
     */
    fun violations(
        preference: BreakfastPreference,
        slot: MealSlot,
        foods: List<FoodItem>
    ): List<FoodItem> {
        if (slot != MealSlot.BREAKFAST) return emptyList()
        val wanted = when (preference) {
            BreakfastPreference.SWEET -> Flavor.SWEET
            BreakfastPreference.SAVORY -> Flavor.SAVORY
            // "Entrambe" e "niente" non impongono nulla.
            else -> return emptyList()
        }
        val opposite = if (wanted == Flavor.SWEET) Flavor.SAVORY else Flavor.SWEET
        return foods.filter { flavorOf(it) == opposite }
    }

    /**
     * Alimenti del catalogo adatti alla preferenza, per il messaggio di errore.
     * Dire "usa alimenti adatti" senza nominarli lascia il modello a indovinare.
     */
    fun suitable(preference: BreakfastPreference, foods: List<FoodItem>, limit: Int = 8): List<String> {
        val wanted = when (preference) {
            BreakfastPreference.SWEET -> Flavor.SWEET
            BreakfastPreference.SAVORY -> Flavor.SAVORY
            else -> return emptyList()
        }
        return foods.filter { flavorOf(it) == wanted }.map { it.name }.take(limit)
    }

    /** Messaggio per il modello: cosa ha sbagliato e come rimediare. */
    fun hint(preference: BreakfastPreference): String = when (preference) {
        BreakfastPreference.SWEET ->
            "La colazione deve essere DOLCE: usa avena, latte o yogurt, frutta, fette biscottate, " +
                "miele. Vietati carne, pesce, salumi e uova a colazione."
        BreakfastPreference.SAVORY ->
            "La colazione deve essere SALATA: usa uova, affettati magri, pane, formaggi. " +
                "Vietati miele, marmellata, frutta dolce e cereali da colazione."
        BreakfastPreference.BOTH ->
            "La colazione puo' essere dolce o salata, a tua scelta."
        BreakfastPreference.NONE ->
            "L'utente non fa colazione: non inserire il pasto colazione."
    }
}
