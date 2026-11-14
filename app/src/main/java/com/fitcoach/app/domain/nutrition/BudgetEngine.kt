package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.BudgetScope
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.roundToInt

/**
 * Motore deterministico del budget alimentare mensile (spec. §5.1).
 *
 * Il budget e un vincolo EFFETTIVO: i costi sono calcolati dagli alimenti del
 * piano (grammature reali x prezzo/kg). I prezzi provengono da [FoodItem.pricePerKgEur];
 * quando mancano NON vengono inventati: l'alimento e segnalato come "costo non
 * stimabile" e il totale viene marcato come parziale.
 *
 * Il costo del piano e la spesa per la sola persona: se il budget riguarda piu
 * persone o l'intera spesa familiare, la quota attribuibile all'utente viene
 * derivata in modo trasparente (vedi [attributableBudget]).
 */
object BudgetEngine {

    /** Esito del calcolo di costo di un piano settimanale. */
    data class CostReport(
        val dailyCostEur: Double,
        val weeklyCostEur: Double,
        val monthlyCostEur: Double,
        /** Budget mensile effettivamente disponibile per l'alimentazione dell'utente. */
        val monthlyBudgetEur: Double?,
        /** Budget residuo (negativo se sforato). Null se budget non impostato. */
        val remainingMonthlyEur: Double?,
        val withinBudget: Boolean,
        /** True se almeno un alimento non ha prezzo: il totale e una stima parziale. */
        val partialEstimate: Boolean,
        val unpricedFoods: List<String>,
        val notes: List<String>
    ) {
        val overBudgetEur: Double get() = remainingMonthlyEur?.let { if (it < 0) -it else 0.0 } ?: 0.0
    }

    /**
     * Budget mensile attribuibile all'alimentazione della persona.
     *
     * - PERSONAL_ONLY: l'intero budget e della persona.
     * - SHARED_FOOD: il budget copre la spesa alimentare di [persons] persone:
     *   si attribuisce la quota proporzionale (budget / persone), perche il piano
     *   riguarda solo l'utente.
     * - WHOLE_SHOPPING: il budget include anche la spesa non alimentare; si stima
     *   che la parte alimentare sia circa il 70% (stima dichiarata, non un dato reale).
     */
    fun attributableBudget(profile: UserProfile): Double? {
        val monthly = profile.monthlyFoodBudgetEur ?: return null
        val persons = profile.budgetPersons.coerceAtLeast(1)
        return when (profile.budgetScope) {
            BudgetScope.PERSONAL_ONLY -> monthly
            BudgetScope.SHARED_FOOD -> monthly / persons
            BudgetScope.WHOLE_SHOPPING -> monthly * FOOD_SHARE_OF_WHOLE
        }
    }

    private const val FOOD_SHARE_OF_WHOLE = 0.70
    private const val WEEKS_PER_MONTH = 4.345

    /** Costo settimanale del piano (somma di tutte le voci dei 7 giorni). */
    fun weeklyCost(plan: NutritionPlan, foodsById: Map<Long, FoodItem>): Pair<Double, List<String>> {
        var total = 0.0
        val unpriced = linkedSetOf<String>()
        plan.days.forEach { day ->
            day.meals.forEach { meal ->
                meal.entries.forEach { e ->
                    val price = foodsById[e.foodItemId]?.pricePerKgEur
                    if (price == null) {
                        unpriced += e.foodName
                    } else {
                        total += price * e.grams / 1000.0
                    }
                }
            }
        }
        return total to unpriced.toList()
    }

    fun report(
        plan: NutritionPlan,
        foodsById: Map<Long, FoodItem>,
        profile: UserProfile
    ): CostReport {
        val (weekly, unpriced) = weeklyCost(plan, foodsById)
        val daily = weekly / 7.0
        val monthly = weekly * WEEKS_PER_MONTH
        val budget = attributableBudget(profile)
        val remaining = budget?.let { it - monthly }
        val notes = mutableListOf<String>()
        if (budget == null) {
            notes += "Nessun budget mensile impostato: costo calcolato ma non vincolante."
        } else {
            when (profile.budgetScope) {
                BudgetScope.PERSONAL_ONLY -> Unit
                BudgetScope.SHARED_FOOD -> notes += "Budget condiviso tra ${profile.budgetPersons} " +
                    "persone: attribuita la quota pro capite."
                BudgetScope.WHOLE_SHOPPING -> notes += "Budget su tutta la spesa: stimata la quota " +
                    "alimentare (70%, stima dichiarata)."
            }
        }
        if (unpriced.isNotEmpty()) {
            notes += "Costo parziale: ${unpriced.size} alimenti senza prezzo noto " +
                "(il totale e una stima per difetto)."
        }
        return CostReport(
            dailyCostEur = daily,
            weeklyCostEur = weekly,
            monthlyCostEur = monthly,
            monthlyBudgetEur = budget,
            remainingMonthlyEur = remaining,
            withinBudget = budget == null || monthly <= budget + 0.01,
            partialEstimate = unpriced.isNotEmpty(),
            unpricedFoods = unpriced,
            notes = notes
        )
    }

    /** Formattazione leggibile del costo. */
    fun formatEur(value: Double): String = "%.2f €".format(value)

    /**
     * Indice di convenienza di un alimento: costo per grammo di proteine.
     * Null se manca il prezzo o le proteine sono nulle (non inventare).
     */
    fun costPerProteinGram(food: FoodItem): Double? {
        val price = food.pricePerKgEur ?: return null
        if (food.proteinPer100g <= 0.0) return null
        // costo di 100 g / grammi di proteine in 100 g
        return (price * 0.1) / food.proteinPer100g
    }

    /** Arrotonda un costo in centesimi per la visualizzazione. */
    fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0
}
