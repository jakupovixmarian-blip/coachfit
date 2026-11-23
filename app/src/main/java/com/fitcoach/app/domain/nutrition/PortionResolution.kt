package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.coach.AiNutritionDay
import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.model.CarbCyclingPlan
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import kotlin.math.roundToInt

/**
 * Sostituisce le grammature scelte dall'AI con quelle calcolate dal solver.
 *
 * Perche esiste: `solve_meal_portions` e' uno strumento che l'AI PUO' chiamare.
 * Nella prova reale su OpenRouter il modello non lo ha chiamato e ha calcolato a
 * mano, sbagliando 6 giorni su 7 (fino a -900 kcal): la proposta veniva respinta
 * dal validatore. Un vincolo che dipende dall'obbedienza del modello non e' un
 * vincolo.
 *
 * Qui il calcolo diventa non aggirabile: l'AI sceglie COSA si mangia (alimenti,
 * pasti, ordine), il codice decide QUANTO. Se il modello ha gia' usato il solver
 * le sue grammature restano il punto di partenza; altrimenti si parte dai
 * valori di default del solver. In entrambi i casi il risultato e' ricalcolato
 * sui valori reali del catalogo.
 */
object PortionResolution {

    /**
     * Ricalcola tutte le giornate che hanno alimenti validi.
     *
     * Le giornate senza alimenti riconoscibili restano invariate: il validatore
     * le respingera' con un messaggio preciso, che e' piu' utile all'AI di un
     * piano silenziosamente svuotato.
     */
    fun resolve(
        nutrition: AiNutritionProposal,
        foods: List<FoodItem>,
        cycling: CarbCyclingPlan
    ): Pair<AiNutritionProposal, List<String>> {
        val notes = mutableListOf<String>()
        val byName = foods.associateBy { it.normalizedName }

        val days = nutrition.days.map { day ->
            val type = runCatching { CarbDayType.valueOf(day.dayType.trim().uppercase()) }.getOrNull()
            val target = type?.let { cycling.targets[it] }
            if (target == null) {
                notes += "giorno ${day.dayIndex}: tipo '${day.dayType}' non valido, grammature non ricalcolate"
                return@map day
            }

            val structure = mutableListOf<Pair<String, List<FoodItem>>>()
            val initial = mutableMapOf<String, Double>()
            day.meals.forEach { meal ->
                val picked = meal.entries.mapNotNull { entry ->
                    // Il solver scartava in silenzio gli alimenti con nome non
                    // identico: il pasto perdeva una fonte proteica e il giorno
                    // finiva a 1463 kcal contro 1891 attese.
                    val food = byName[FoodItem.normalizeName(entry.foodName)]
                        ?: CatalogNameResolver.resolveFood(entry.foodName, foods)
                    if (food != null && entry.grams in MealPortionSolver.MIN_GRAMS..MealPortionSolver.MAX_GRAMS) {
                        // La grammatura del modello, se presente, e' solo il punto
                        // di partenza: il solver la corregge comunque.
                        initial.putIfAbsent(food.name, entry.grams)
                    }
                    food
                }
                if (picked.isNotEmpty()) structure += meal.slot.uppercase() to picked
            }

            if (structure.isEmpty()) {
                notes += "giorno ${day.dayIndex}: nessun alimento riconosciuto, grammature non ricalcolate"
                return@map day
            }

            val result = MealPortionSolver.solve(target, structure, initial)
            if (!result.ok) {
                // Senza lo scostamento la nota era "il solver non centra il
                // target ()": il modello non sapeva di quanto ne' su quale macro.
                val gaps = buildList {
                    fun gap(label: String, actual: Double, targetMacro: Double) {
                        if (targetMacro > 0) add("$label ${(actual - targetMacro).roundToInt()}")
                    }
                    gap("kcal", result.actual.kcal, result.target.kcal)
                    gap("P", result.actual.protein, result.target.protein)
                    gap("C", result.actual.carbs, result.target.carbs)
                    gap("F", result.actual.fat, result.target.fat)
                }.joinToString(", ")
                notes += "giorno ${day.dayIndex}: grammature fuori tolleranza (scarto: $gaps)" +
                    if (result.warnings.isEmpty()) "" else " - ${result.warnings.joinToString("; ")}"
            }

            // Le grammature calcolate vengono ricomposte nella stessa struttura
            // di pasti inviata dal modello, mantenendo orari e slot.
            val solvedBySlot = result.meals.associateBy { it.slot.uppercase() }
            val meals = day.meals.map { meal ->
                val solved = solvedBySlot[meal.slot.uppercase()] ?: return@map meal
                val gramsByName = solved.entries.groupBy { it.foodName }
                    .mapValues { (_, list) -> list.first().grams }
                meal.copy(
                    entries = meal.entries.map { entry ->
                        // Il solver restituisce il nome canonico del catalogo: per
                        // ritrovare la grammatura va risolto anche qui, altrimenti
                        // il nome scritto dall'AI non combacia e i grammi restano
                        // quelli originali.
                        val resolved = CatalogNameResolver.resolveFood(entry.foodName, foods)
                        val grams = gramsByName[resolved?.name] ?: gramsByName[entry.foodName]
                        if (grams == null) entry else entry.copy(grams = grams)
                    }
                )
            }
            day.copy(meals = meals)
        }

        return nutrition.copy(days = days) to notes
    }
}
