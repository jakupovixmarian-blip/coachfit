package dev.fitcoach.app.domain.validation

import dev.fitcoach.app.domain.engine.NutritionMath
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import kotlin.math.roundToInt

enum class Severity { ERROR, WARNING, INFO }

data class ValidationIssue(
    val severity: Severity,
    val code: String,
    val message: String
)

/**
 * Where a value comes from and how much it can be trusted. This is stored with the
 * food so the app can always tell a verified official table from a crowdsourced
 * product or a value typed by hand.
 */
enum class VerificationStatus(val label: String) {
    VERIFIED("Verificato su fonte ufficiale"),
    IMPORTED("Importato da fonte esterna"),
    MANUAL("Inserito a mano"),
    ESTIMATED("Stimato")
}

data class FoodValidation(
    val issues: List<ValidationIssue>,
    val reliability: Double,
    val status: VerificationStatus,
    val energyDeviationPercent: Double?
) {
    /** A record with an error must not be used in a plan or in a diary total. */
    val isUsable: Boolean get() = issues.none { it.severity == Severity.ERROR }

    val hasWarnings: Boolean get() = issues.any { it.severity == Severity.WARNING }
}

/**
 * The record chosen when two sources describe the same food. Values are never
 * averaged: an average of two different measurements is a third value that no
 * source supports.
 */
data class FoodConflict(
    val chosen: Food,
    val rejected: List<Food>,
    val reason: String
)

/**
 * Validates a food record before it is stored, planned or summed.
 *
 * The checks are deliberately physical, not statistical: negative values, a macro
 * sum above 100 g per 100 g, more energy than pure fat can carry, and a declared
 * calorie value that disagrees with its own macros. Each of these means the record
 * is wrong, not merely unusual.
 */
object FoodValidator {

    private const val MAX_KCAL_PER_100G = 900.0
    private const val ENERGY_TOLERANCE_PERCENT = 25.0
    private const val MIN_KCAL_FOR_ENERGY_CHECK = 20.0

    fun reliabilityOf(source: FoodSource): Double = when (source) {
        FoodSource.CREA -> 0.95
        FoodSource.USDA -> 0.90
        FoodSource.OPEN_FOOD_FACTS -> 0.70
        FoodSource.MANUAL -> 0.55
        FoodSource.AI_PLAN -> 0.25
    }

    fun statusOf(food: Food): VerificationStatus = when (food.source) {
        FoodSource.CREA, FoodSource.USDA -> VerificationStatus.VERIFIED
        FoodSource.OPEN_FOOD_FACTS -> VerificationStatus.IMPORTED
        FoodSource.MANUAL -> VerificationStatus.MANUAL
        FoodSource.AI_PLAN -> VerificationStatus.ESTIMATED
    }

    fun validate(food: Food): FoodValidation {
        val issues = mutableListOf<ValidationIssue>()

        if (food.name.isBlank()) {
            issues += ValidationIssue(Severity.ERROR, "NAME_MISSING", "Il nome dell'alimento è vuoto.")
        }

        val negatives = buildList {
            if (food.kcalPer100g < 0) add("calorie")
            if (food.proteinPer100g < 0) add("proteine")
            if (food.carbsPer100g < 0) add("carboidrati")
            if (food.fatPer100g < 0) add("grassi")
            if ((food.fiberPer100g ?: 0.0) < 0) add("fibre")
        }
        if (negatives.isNotEmpty()) {
            issues += ValidationIssue(
                Severity.ERROR,
                "NEGATIVE_VALUE",
                "Valori negativi non ammessi: ${negatives.joinToString(", ")}."
            )
        }

        if (food.kcalPer100g > MAX_KCAL_PER_100G) {
            issues += ValidationIssue(
                Severity.ERROR,
                "IMPLAUSIBLE_ENERGY",
                "Energia non plausibile: ${food.kcalPer100g} kcal/100 g (il grasso puro è 900)."
            )
        }

        val macroSum = food.proteinPer100g + food.carbsPer100g + food.fatPer100g
        if (macroSum > 100.5) {
            issues += ValidationIssue(
                Severity.ERROR,
                "MACRO_SUM_OVER_100",
                "Proteine, carboidrati e grassi sommano ${macroSum.roundToInt()} g per 100 g: impossibile."
            )
        }

        if ((food.fiberPer100g ?: 0.0) > 100.0) {
            issues += ValidationIssue(
                Severity.ERROR,
                "FIBER_OVER_100",
                "Fibre oltre 100 g per 100 g: valore non valido."
            )
        }

        val deviation = NutritionMath.energyDeviationPercent(
            declaredKcal = food.kcalPer100g,
            protein = food.proteinPer100g,
            carbs = food.carbsPer100g,
            fat = food.fatPer100g
        )
        if (deviation != null && food.kcalPer100g >= MIN_KCAL_FOR_ENERGY_CHECK &&
            deviation > ENERGY_TOLERANCE_PERCENT
        ) {
            issues += ValidationIssue(
                Severity.WARNING,
                "ENERGY_MISMATCH",
                "Le calorie dichiarate non corrispondono ai macro (scarto ${deviation.roundToInt()}%): " +
                    "possibile errore di trascrizione o valore riferito a una porzione diversa."
            )
        }

        if (food.fiberPer100g == null) {
            issues += ValidationIssue(
                Severity.INFO,
                "FIBER_MISSING",
                "Fibre non disponibili: il totale giornaliero delle fibre sarà incompleto."
            )
        }

        if (food.source == FoodSource.MANUAL) {
            issues += ValidationIssue(
                Severity.INFO,
                "MANUAL_DATA",
                "Valore inserito a mano: non verificato su una tabella ufficiale."
            )
        }

        if (food.state == FoodState.COMMERCIAL && food.brand.isBlank()) {
            issues += ValidationIssue(
                Severity.WARNING,
                "BRAND_MISSING",
                "Prodotto commerciale senza marca: rischio di confonderlo con un prodotto simile."
            )
        }

        return FoodValidation(
            issues = issues,
            reliability = reliabilityOf(food.source),
            status = statusOf(food),
            energyDeviationPercent = deviation
        )
    }

    /**
     * Chooses one record among candidates that describe the same food. The most
     * reliable source wins, ties are broken by completeness, and the rejected
     * records are returned so the caller can keep them as evidence.
     */
    fun resolve(candidates: List<Food>): FoodConflict? {
        if (candidates.isEmpty()) return null
        val ranked = candidates.sortedByDescending { rank(it) }
        val chosen = ranked.first()
        val rejected = ranked.drop(1)
        val reason = if (rejected.isEmpty()) {
            "Unica corrispondenza disponibile."
        } else {
            "Scelto ${chosen.name} da ${chosen.source.label} " +
                "(affidabilità ${(reliabilityOf(chosen.source) * 100).roundToInt()}%): " +
                "valori diversi non vengono mediati."
        }
        return FoodConflict(chosen = chosen, rejected = rejected, reason = reason)
    }

    private fun rank(food: Food): Double {
        var score = reliabilityOf(food.source) * 100.0
        if (food.brand.isNotBlank()) score += 2.0
        if (!food.barcode.isNullOrBlank()) score += 1.0
        if (food.fiberPer100g != null) score += 1.0
        if (food.isCustom) score -= 0.5
        if (!validate(food).isUsable) score -= 50.0
        return score
    }
}
