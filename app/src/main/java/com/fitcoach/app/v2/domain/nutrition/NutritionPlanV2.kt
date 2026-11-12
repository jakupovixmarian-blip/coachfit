package com.fitcoach.app.v2.domain.nutrition

import com.fitcoach.app.v2.domain.generation.GenerationProfile
import kotlin.math.abs
import kotlin.math.roundToInt

/** Tipo di giornata usato dal piano settimanale. */
enum class CarbDayTypeV2 { HIGH, MEDIUM, LOW }

data class EnergyEstimateV2(
    val bmr: Double,
    val tdee: Double,
    val formula: String,
    val activityFactor: Double
)

data class MacroTargetV2(
    val dayType: CarbDayTypeV2,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double
) {
    val macroCalories: Double
        get() = proteinGrams * 4 + carbohydrateGrams * 4 + fatGrams * 9
}

data class WeeklyNutritionPlanV2(
    val energy: EnergyEstimateV2,
    val targets: Map<CarbDayTypeV2, MacroTargetV2>,
    val dayTypes: List<CarbDayTypeV2>,
    val weeklyCalories: Double,
    val targetWeeklyCalories: Double,
    val warnings: List<String>,
    val isValid: Boolean
)

object NutritionPlanEngineV2 {
    fun build(profile: GenerationProfile): WeeklyNutritionPlanV2 {
        require(profile.trainingDays in 0..7) { "I giorni di allenamento devono essere tra 0 e 7." }
        require(profile.ageYears in 13..100) { "L'età è fuori dall'intervallo supportato." }
        require(profile.heightCm > 0 && profile.weightKg > 0) { "Altezza e peso devono essere positivi." }
        val energy = energy(profile)
        val (highDays, mediumDays, lowDays) = distributeDays(profile.trainingDays)
        val targetWeekly = energy.tdee * 7 * goalAdjustment(profile.primaryGoal)
        val factorTotal = highDays * 1.10 + mediumDays * 1.0 + lowDays * 0.90
        val scale = if (factorTotal == 0.0) 0.0 else targetWeekly / factorTotal
        val protein = profile.weightKg * proteinPerKg(profile.primaryGoal)
        val targets = linkedMapOf<CarbDayTypeV2, MacroTargetV2>()
        listOf(
            CarbDayTypeV2.HIGH to 1.10 to 0.60,
            CarbDayTypeV2.MEDIUM to 1.0 to 0.70,
            CarbDayTypeV2.LOW to 0.90 to 0.80
        ).forEach { (typeAndFactor, fatPerKg) ->
            val (type, factor) = typeAndFactor
            val kcal = scale * factor
            targets[type] = macroTarget(type, kcal, protein, profile.weightKg * fatPerKg)
        }
        val dayTypes = buildDayTypes(profile.trainingDays)
        val weekly = dayTypes.sumOf { targets.getValue(it).calories }
        val warnings = buildList {
            val floor = safetyFloor(profile, energy)
            targets.values.filter { it.calories < floor }.forEach {
                add("${it.dayType.name} sotto il minimo prudenziale di ${floor.roundToInt()} kcal.")
            }
            if (abs(weekly - targetWeekly) > 15.0) add("Il bilancio calorico settimanale non è coerente.")
            targets.values.filter { abs(it.calories - it.macroCalories) > 15.0 }.forEach {
                add("Macronutrienti incoerenti per la giornata ${it.dayType.name}.")
            }
        }
        return WeeklyNutritionPlanV2(
            energy = energy,
            targets = targets,
            dayTypes = dayTypes,
            weeklyCalories = weekly,
            targetWeeklyCalories = targetWeekly,
            warnings = warnings,
            isValid = warnings.isEmpty()
        )
    }

    private fun energy(profile: GenerationProfile): EnergyEstimateV2 {
        val sexTerm = when (profile.biologicalSex.trim().uppercase()) {
            "M", "MALE", "UOMO", "MASCHIO" -> 5.0
            "F", "FEMALE", "DONNA", "FEMMINA" -> -161.0
            else -> error("Sesso biologico non riconosciuto: nessun valore medio inventato.")
        }
        val bmr = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.ageYears + sexTerm
        val factor = when (profile.trainingDays) {
            0, 1 -> 1.20
            2, 3 -> 1.375
            4, 5 -> 1.55
            else -> 1.725
        }
        return EnergyEstimateV2(bmr, bmr * factor, "Mifflin-St Jeor", factor)
    }

    private fun macroTarget(
        type: CarbDayTypeV2,
        calories: Double,
        protein: Double,
        fat: Double
    ): MacroTargetV2 {
        val carbs = ((calories - protein * 4 - fat * 9) / 4).coerceAtLeast(0.0)
        return MacroTargetV2(type, calories, protein, carbs, fat)
    }

    private fun goalAdjustment(goal: String): Double = when (goal.trim().lowercase()) {
        "definizione", "cut", "dimagrimento" -> 0.80
        "ricomposizione", "recomp" -> 0.92
        "ipertrofia", "massa", "hypertrophy" -> 1.10
        "mantenimento", "maintenance" -> 1.0
        else -> error("Obiettivo non riconosciuto: impossibile scegliere un bilancio calorico.")
    }

    private fun proteinPerKg(goal: String): Double = when (goal.trim().lowercase()) {
        "definizione", "cut", "dimagrimento", "ricomposizione", "recomp" -> 2.0
        "ipertrofia", "massa", "hypertrophy" -> 1.8
        "mantenimento", "maintenance" -> 1.6
        else -> error("Obiettivo non riconosciuto: impossibile scegliere le proteine.")
    }

    private fun safetyFloor(profile: GenerationProfile, energy: EnergyEstimateV2): Double {
        val normalizedSex = profile.biologicalSex.trim().uppercase()
        val female = normalizedSex in setOf("F", "FEMALE", "DONNA", "FEMMINA")
        return maxOf(energy.bmr * 0.85, if (female) 1_200.0 else 1_500.0)
    }

    private fun distributeDays(trainingDays: Int): Triple<Int, Int, Int> = when (trainingDays) {
        0 -> Triple(0, 0, 7)
        1 -> Triple(0, 1, 6)
        2 -> Triple(1, 1, 5)
        3 -> Triple(1, 1, 5)
        4 -> Triple(2, 1, 4)
        5 -> Triple(2, 2, 3)
        6 -> Triple(3, 2, 2)
        else -> Triple(4, 2, 1)
    }

    private fun buildDayTypes(trainingDays: Int): List<CarbDayTypeV2> {
        val (high, medium, low) = distributeDays(trainingDays)
        return buildList {
            repeat(high) { add(CarbDayTypeV2.HIGH) }
            repeat(medium) { add(CarbDayTypeV2.MEDIUM) }
            repeat(low) { add(CarbDayTypeV2.LOW) }
        }
    }
}
