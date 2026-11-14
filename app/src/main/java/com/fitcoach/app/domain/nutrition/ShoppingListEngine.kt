package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.ShoppingCategory
import com.fitcoach.app.domain.model.ShoppingItem
import kotlin.math.roundToInt

/**
 * Genera la lista della spesa settimanale a partire dal piano alimentare
 * EFFETTIVO: legge tutti gli alimenti dei 7 giorni, somma le quantita ripetute
 * (es. pollo lunedi 150 g + martedi 200 g = 350 g) e produce una voce unica per
 * alimento, classificata per categoria merceologica.
 *
 * Non crea moduli indipendenti: usa lo stesso database alimentare e gli stessi
 * identificativi del piano, cosi una sostituzione si riflette nella spesa.
 */
object ShoppingListEngine {

    /** Aggrega le quantita per alimento (foodItemId) su tutti i pasti del piano. */
    fun aggregate(
        plan: NutritionPlan,
        weekStartEpochDay: Long,
        foodsById: Map<Long, FoodItem>,
        previous: List<ShoppingItem> = emptyList()
    ): List<ShoppingItem> {
        data class Acc(val name: String, val unit: FoodUnit, val grams: Double)

        val totals = linkedMapOf<Long, Acc>()
        plan.days.forEach { day ->
            day.meals.forEach { meal ->
                meal.entries.forEach { e ->
                    val key = e.foodItemId
                    val unit = foodsById[key]?.defaultUnit ?: e.unit
                    val existing = totals[key]
                    totals[key] = if (existing == null) {
                        Acc(e.foodName, unit, e.grams)
                    } else {
                        existing.copy(grams = existing.grams + e.grams)
                    }
                }
            }
        }

        val prevByFood = previous.associateBy { it.foodItemId }
        return totals.map { (foodId, acc) ->
            val food = foodsById[foodId]
            val category = food?.let { ShoppingCategory.fromFoodCategory(it.category) }
                ?: ShoppingCategory.OTHER
            val prev = prevByFood[foodId]
            ShoppingItem(
                id = prev?.id ?: 0,
                weekStartEpochDay = weekStartEpochDay,
                foodItemId = foodId,
                foodName = acc.name,
                category = category,
                totalGrams = acc.grams.roundToInt().toDouble(),
                unit = acc.unit,
                // Conserva spunta e "gia in casa" per lo stesso alimento, senza perdere stato.
                checked = prev?.checked ?: false,
                alreadyHave = prev?.alreadyHave ?: false
            )
        }.sortedWith(compareBy({ it.category.ordinal }, { it.foodName }))
    }

    /**
     * Rileva le voci di spesa il cui stato "spuntato/gia in casa" non e piu
     * coerente perche l'alimento e cambiato (sostituzione). Restituisce gli id da
     * segnalare all'utente prima di applicare l'aggiornamento.
     */
    fun invalidatedStates(previous: List<ShoppingItem>, updated: List<ShoppingItem>): List<String> {
        val updatedByFood = updated.associateBy { it.foodItemId }
        return previous.filter { prev ->
            val now = updatedByFood[prev.foodItemId]
            prev.checked && now == null
        }.map { it.foodName }
    }

    /** Riepilogo testuale per l'esportazione/condivisione. */
    fun toShareText(items: List<ShoppingItem>): String {
        if (items.isEmpty()) return "Lista della spesa vuota."
        val sb = StringBuilder("Lista della spesa FitCoach\n")
        items.groupBy { it.category }.forEach { (cat, list) ->
            sb.append("\n").append(cat.label).append("\n")
            list.forEach { item ->
                val check = if (item.checked || item.alreadyHave) "[x]" else "[ ]"
                sb.append("$check ${item.foodName}: ${item.totalGrams.roundToInt()} g")
                    .append(if (item.alreadyHave) " (già in casa)" else "")
                    .append("\n")
            }
        }
        return sb.toString().trim()
    }

    fun totalItems(items: List<ShoppingItem>): Int = items.size
    fun purchasedCount(items: List<ShoppingItem>): Int = items.count { it.checked || it.alreadyHave }
    fun progress(items: List<ShoppingItem>): Float =
        if (items.isEmpty()) 0f else purchasedCount(items).toFloat() / items.size
}
