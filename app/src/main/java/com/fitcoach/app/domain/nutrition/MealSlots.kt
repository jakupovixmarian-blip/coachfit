package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.MealSlot

/**
 * Gli slot pasto che il profilo richiede, nell'ordine della giornata.
 *
 * Unica fonte di verita': la usano il prompt del Coach AI, il validatore e la
 * diagnostica. Prima ognuno aveva la propria idea (il prompt elencava tutti e
 * sei gli slot come validi, il cancello di salvataggio accettava 3..6 pasti) e il
 * modello consegnava 4 o 6 pasti su un profilo da 5.
 */
object MealSlots {

    /** Tutti gli slot, in ordine cronologico. */
    val all: List<MealSlot> = MealSlot.entries.sortedBy { it.defaultTime }

    /**
     * Gli slot per un profilo che prevede [mealsPerDay] pasti.
     *
     * Si tengono sempre i tre pasti principali e si aggiungono gli spuntini in
     * ordine, cosi 3 pasti = colazione/pranzo/cena e 6 = tutti.
     */
    fun forMealsPerDay(mealsPerDay: Int): List<MealSlot> {
        val core = listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER)
        val extras = listOf(MealSlot.MORNING_SNACK, MealSlot.AFTERNOON_SNACK, MealSlot.EVENING_SNACK)
        val chosen = core + extras.take((mealsPerDay - core.size).coerceIn(0, extras.size))
        return chosen.sortedBy { it.defaultTime }
    }

    /** Elenco leggibile per il prompt: "BREAKFAST, MORNING_SNACK, ...". */
    fun describe(mealsPerDay: Int): String =
        forMealsPerDay(mealsPerDay).joinToString(", ") { it.name }
}
