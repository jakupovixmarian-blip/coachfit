package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Motore deterministico di sostituzione alimentare con vincoli nutrizionali.
 *
 * Non sostituisce mai "a parita di grammi": calcola la quantita del nuovo
 * alimento per mantenere il macro dominante dell'originale, poi verifica che
 * l'intero pasto e la giornata rispettino calorie e macronutrienti entro le
 * tolleranze configurate. Se un singolo alimento non basta, propone una
 * combinazione (alimento sostitutivo + alimento di riequilibrio) calcolata
 * matematicamente, senza mai superare il budget calorico approvato.
 */
object FoodSubstitutionEngine {

    /** Tolleranze nutrizionali configurabili (percentuali). */
    data class Tolerance(
        val kcalPercent: Double = 5.0,
        val proteinPercent: Double = 10.0,
        val carbsPercent: Double = 10.0,
        val fatPercent: Double = 10.0
    )

    data class Request(
        val original: FoodItem,
        val originalGrams: Double,
        val substitute: FoodItem,
        /** Somma degli altri alimenti del pasto (esclusa la voce sostituita). */
        val mealOther: MacroTotals,
        /** Somma di tutti gli altri pasti/alimenti della giornata (esclusa la voce). */
        val dayPlannedOther: MacroTotals,
        /** Obiettivo calorico e macro della giornata approvata. */
        val dayTarget: MacroTotals,
        val tolerance: Tolerance = Tolerance(),
        /** Alimenti candidati per il riequilibrio (grassi, carboidrati, proteine). */
        val fatBalancers: List<FoodItem> = emptyList(),
        val carbBalancers: List<FoodItem> = emptyList(),
        val maxGrams: Double = 600.0
    )

    enum class Status { COMPATIBLE, COMPATIBLE_WITH_ADJUSTMENT, INCOMPATIBLE }

    data class Addition(val food: FoodItem, val grams: Double, val reason: String)

    data class Result(
        val status: Status,
        val substituteGrams: Double,
        val substituteMacros: MacroTotals,
        val addition: Addition? = null,
        val mealBefore: MacroTotals,
        val mealAfter: MacroTotals,
        val dayBefore: MacroTotals,
        val dayAfter: MacroTotals,
        val mealTarget: MacroTotals,
        val dayTarget: MacroTotals,
        val violations: List<String> = emptyList(),
        val notes: List<String> = emptyList()
    ) {
        val isEquivalent: Boolean get() = status != Status.INCOMPATIBLE
    }

    // Tolleranze minime assolute, per non rendere impossibili le sostituzioni su
    // pasti con valori molto bassi (es. grassi di un pasto magro).
    private const val KCAL_FLOOR = 30.0
    private const val PROTEIN_FLOOR = 4.0
    private const val CARBS_FLOOR = 5.0
    private const val FAT_FLOOR = 2.5
    private const val KCAL_EPSILON = 1.0

    fun evaluate(req: Request): Result {
        require(req.originalGrams > 0) { "La quantita originale deve essere positiva." }

        val originalMacros = macrosFor(req.original, req.originalGrams)
        val mealBefore = req.mealOther + originalMacros
        val dayBefore = req.dayPlannedOther + originalMacros

        val dominant = MealPlanGenerator.dominantMacro(req.original)
        val idealGrams = MealPlanGenerator.substituteGrams(req.original, req.originalGrams, req.substitute)
        val baseGrams = snap(idealGrams.coerceIn(1.0, req.maxGrams))
        val baseSubMacros = macrosFor(req.substitute, baseGrams)

        val baseMealAfter = req.mealOther + baseSubMacros
        val baseDayAfter = req.dayPlannedOther + baseSubMacros
        val baseViolations = violations(baseMealAfter, baseDayAfter, mealBefore, dayBefore, req.dayTarget, req.tolerance)

        if (baseViolations.isEmpty()) {
            return result(
                Status.COMPATIBLE, baseGrams, baseSubMacros, null,
                mealBefore, baseMealAfter, dayBefore, baseDayAfter, req,
                violations = emptyList(),
                notes = listOf("Sostituzione equivalente: quantita calcolata sul macro dominante (${dominant.label()}).")
            )
        }

        // Tentativo di riequilibrio con un secondo alimento (sostituzione combinata).
        combinedAttempt(req, baseGrams, baseSubMacros, mealBefore, dayBefore, originalMacros)?.let { return it }

        // Tentativo di aggiustamento dei grammi entro una finestra ragionevole.
        val searched = searchGrams(req, mealBefore, dayBefore)
        if (searched != null) return searched

        // Nessuna soluzione: restituiamo la piu vicina con le violazioni spiegate.
        return result(
            Status.INCOMPATIBLE, baseGrams, baseSubMacros, null,
            mealBefore, baseMealAfter, dayBefore, baseDayAfter, req,
            violations = baseViolations,
            notes = listOf("La sostituzione non rispetta i vincoli del piano. Scegli un'altra alternativa.")
        )
    }

    /** Cerca un quantitativo che soddisfi tutti i vincoli entro +/-40% del valore ideale. */
    private fun searchGrams(
        req: Request,
        mealBefore: MacroTotals,
        dayBefore: MacroTotals
    ): Result? {
        val ideal = MealPlanGenerator.substituteGrams(req.original, req.originalGrams, req.substitute)
            .coerceIn(1.0, req.maxGrams)
        val lo = (ideal * 0.6).coerceAtLeast(1.0)
        val hi = (ideal * 1.4).coerceAtMost(req.maxGrams)
        var g = lo
        var best: Pair<Double, List<String>>? = null
        while (g <= hi + 0.001) {
            val grams = snap(g)
            val sub = macrosFor(req.substitute, grams)
            val mealAfter = req.mealOther + sub
            val dayAfter = req.dayPlannedOther + sub
            val v = violations(mealAfter, dayAfter, mealBefore, dayBefore, req.dayTarget, req.tolerance)
            if (v.isEmpty()) {
                return result(
                    Status.COMPATIBLE, grams, sub, null,
                    mealBefore, mealAfter, dayBefore, dayAfter, req,
                    notes = listOf("Quantitativo ricalcolato (${grams.toInt()} g) per rispettare i vincoli del piano.")
                )
            }
            if (best == null || v.size < best!!.second.size) best = grams to v
            g += 1.0
        }
        return null
    }

    /** Sostituzione combinata: sostituto + alimento di riequilibrio. */
    private fun combinedAttempt(
        req: Request,
        baseGrams: Double,
        baseSubMacros: MacroTotals,
        mealBefore: MacroTotals,
        dayBefore: MacroTotals,
        originalMacros: MacroTotals
    ): Result? {
        val mealAfterBase = req.mealOther + baseSubMacros
        val dayAfterBase = req.dayPlannedOther + baseSubMacros

        data class Deficit(val kind: String, val amount: Double, val balancers: List<FoodItem>, val picker: (FoodItem) -> Double)
        val deficits = buildList {
            val pDef = mealBefore.protein - mealAfterBase.protein
            if (pDef > PROTEIN_FLOOR) add(Deficit("proteine", pDef, emptyList()) { 0.0 })
            val cDef = mealBefore.carbs - mealAfterBase.carbs
            if (cDef > CARBS_FLOOR) add(Deficit("carboidrati", cDef, req.carbBalancers) { it.carbsPer100g })
            val fDef = mealBefore.fat - mealAfterBase.fat
            if (fDef > FAT_FLOOR) add(Deficit("grassi", fDef, req.fatBalancers) { it.fatPer100g })
        }

        for (def in deficits) {
            if (def.balancers.isEmpty()) continue
            val kcalCapMeal = (mealBefore.kcal * (1 + req.tolerance.kcalPercent / 100.0)) - mealAfterBase.kcal
            val kcalCapDay = maxOf(req.dayTarget.kcal, dayBefore.kcal) - dayAfterBase.kcal
            val kcalHeadroom = minOf(kcalCapMeal, kcalCapDay)
            if (kcalHeadroom <= 0.0) continue

            for (balancer in def.balancers) {
                val per100 = def.picker(balancer)
                if (per100 <= 0.0) continue
                val idealBalGrams = def.amount / (per100 / 100.0)
                val maxByKcal = if (balancer.kcalPer100g > 0) kcalHeadroom / (balancer.kcalPer100g / 100.0) else idealBalGrams
                val grams = snap(idealBalGrams.coerceAtMost(maxByKcal))
                if (grams < 1.0) continue

                val addMacros = macrosFor(balancer, grams)
                val mealAfter = mealAfterBase + addMacros
                val dayAfter = dayAfterBase + addMacros
                val v = violations(mealAfter, dayAfter, mealBefore, dayBefore, req.dayTarget, req.tolerance)
                if (v.isEmpty()) {
                    val reason = "Aggiunti ${grams.toInt()} g di ${balancer.name} per riequilibrare ${def.kind}."
                    return result(
                        Status.COMPATIBLE_WITH_ADJUSTMENT, baseGrams, baseSubMacros,
                        Addition(balancer, grams, reason),
                        mealBefore, mealAfter, dayBefore, dayAfter, req,
                        notes = listOf(reason)
                    )
                }
            }
        }
        return null
    }

    private fun violations(
        mealAfter: MacroTotals,
        dayAfter: MacroTotals,
        mealRef: MacroTotals,
        dayBefore: MacroTotals,
        dayTarget: MacroTotals,
        tol: Tolerance
    ): List<String> {
        val out = mutableListOf<String>()

        fun checkMeal(name: String, after: Double, ref: Double, pct: Double, floor: Double) {
            val allowed = maxOf(ref * pct / 100.0, floor)
            if (abs(after - ref) > allowed) {
                out += "%s del pasto fuori tolleranza (%.1f vs %.1f, max %s)".format(name, after, ref, "%.1f".format(allowed))
            }
        }
        checkMeal("Calorie", mealAfter.kcal, mealRef.kcal, tol.kcalPercent, KCAL_FLOOR)
        checkMeal("Proteine", mealAfter.protein, mealRef.protein, tol.proteinPercent, PROTEIN_FLOOR)
        checkMeal("Carboidrati", mealAfter.carbs, mealRef.carbs, tol.carbsPercent, CARBS_FLOOR)
        checkMeal("Grassi", mealAfter.fat, mealRef.fat, tol.fatPercent, FAT_FLOOR)

        // Vincolo 5: non superare il limite calorico approvato della giornata.
        val kcalLimit = maxOf(dayTarget.kcal, dayBefore.kcal)
        if (dayAfter.kcal > kcalLimit + KCAL_EPSILON) {
            out += "Supererebbe il limite calorico giornaliero (%.0f > %.0f kcal)".format(dayAfter.kcal, kcalLimit)
        }

        // Vincolo 6: non allontanare i macro giornalieri dal target oltre la tolleranza.
        fun checkDay(name: String, after: Double, before: Double, target: Double, pct: Double, floor: Double) {
            val devBefore = abs(before - target)
            val devAfter = abs(after - target)
            val allowed = maxOf(devBefore, target * pct / 100.0, floor)
            if (devAfter > allowed + 0.01) {
                out += "%s giornaliere fuori target (%.0f vs obiettivo %.0f)".format(name, after, target)
            }
        }
        checkDay("Proteine", dayAfter.protein, dayBefore.protein, dayTarget.protein, tol.proteinPercent, PROTEIN_FLOOR)
        checkDay("Carboidrati", dayAfter.carbs, dayBefore.carbs, dayTarget.carbs, tol.carbsPercent, CARBS_FLOOR)
        checkDay("Grassi", dayAfter.fat, dayBefore.fat, dayTarget.fat, tol.fatPercent, FAT_FLOOR)

        return out
    }

    private fun result(
        status: Status,
        grams: Double,
        subMacros: MacroTotals,
        addition: Addition?,
        mealBefore: MacroTotals,
        mealAfter: MacroTotals,
        dayBefore: MacroTotals,
        dayAfter: MacroTotals,
        req: Request,
        violations: List<String> = emptyList(),
        notes: List<String> = emptyList()
    ) = Result(
        status = status,
        substituteGrams = grams,
        substituteMacros = subMacros,
        addition = addition,
        mealBefore = mealBefore,
        mealAfter = mealAfter,
        dayBefore = dayBefore,
        dayAfter = dayAfter,
        mealTarget = mealBefore,
        dayTarget = req.dayTarget,
        violations = violations,
        notes = notes
    )

    /** Arrotonda i grammi al grammo piu vicino (coerente con MealPlanGenerator). */
    private fun snap(grams: Double): Double = grams.roundToInt().toDouble()

    fun macrosFor(food: FoodItem, grams: Double): MacroTotals {
        val f = grams / 100.0
        return MacroTotals(
            kcal = food.kcalPer100g * f,
            protein = food.proteinPer100g * f,
            carbs = food.carbsPer100g * f,
            fat = food.fatPer100g * f
        )
    }

    private fun MealPlanGenerator.DominantMacro.label(): String = when (this) {
        MealPlanGenerator.DominantMacro.PROTEIN -> "proteine"
        MealPlanGenerator.DominantMacro.CARBS -> "carboidrati"
        MealPlanGenerator.DominantMacro.FAT -> "grassi"
    }
}
