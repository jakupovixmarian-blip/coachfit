package com.fitcoach.nutrition

import kotlin.math.roundToInt

enum class BiologicalSex { MALE, FEMALE }
enum class NutritionGoal { RECOMPOSITION, HYPERTROPHY, DEFINITION, MAINTENANCE }

data class EnergyEstimate(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val targetKcal: Int,
    val isEstimated: Boolean = true
)

object EnergyCalculator {
    fun estimate(
        age: Int,
        sex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        activityFactor: Double,
        goal: NutritionGoal
    ): EnergyEstimate {
        require(age in 13..100)
        require(heightCm in 100.0..250.0)
        require(weightKg in 30.0..300.0)
        require(activityFactor in 1.0..2.5)
        val bmr = (10 * weightKg + 6.25 * heightCm - 5 * age + if (sex == BiologicalSex.MALE) 5 else -161)
        val tdee = bmr * activityFactor
        val target = when (goal) {
            NutritionGoal.DEFINITION -> tdee * 0.90
            NutritionGoal.HYPERTROPHY -> tdee * 1.05
            NutritionGoal.RECOMPOSITION, NutritionGoal.MAINTENANCE -> tdee
        }
        return EnergyEstimate(bmr.roundToInt(), tdee.roundToInt(), target.roundToInt())
    }
}

data class MacroTarget(val proteinG: Int, val carbsG: Int, val fatG: Int) {
    val calories: Int get() = proteinG * 4 + carbsG * 4 + fatG * 9
}

data class CarbCycleDay(val label: String, val target: MacroTarget)

data class CarbCyclePlan(
    val high: CarbCycleDay,
    val medium: CarbCycleDay,
    val low: CarbCycleDay,
    val weeklyAverageKcal: Int
)

object CarbCyclingCalculator {
    fun create(
        highDays: Int,
        mediumDays: Int,
        lowDays: Int,
        weeklyTargetKcal: Int,
        proteinG: Int,
        highCarbsG: Int,
        mediumCarbsG: Int,
        lowCarbsG: Int,
        fatG: Int
    ): CarbCyclePlan {
        require(highDays >= 0 && mediumDays >= 0 && lowDays >= 0)
        require(highDays + mediumDays + lowDays == 7)
        require(weeklyTargetKcal > 0 && proteinG > 0 && highCarbsG >= 0 && mediumCarbsG >= 0 && lowCarbsG >= 0 && fatG > 0)
        val high = CarbCycleDay("HIGH", MacroTarget(proteinG, highCarbsG, fatG))
        val medium = CarbCycleDay("MEDIUM", MacroTarget(proteinG, mediumCarbsG, fatG))
        val low = CarbCycleDay("LOW", MacroTarget(proteinG, lowCarbsG, fatG))
        val weekly = highDays * high.target.calories + mediumDays * medium.target.calories + lowDays * low.target.calories
        require(kotlin.math.abs(weekly - weeklyTargetKcal) <= weeklyTargetKcal * 0.05) {
            "Il bilancio calorico settimanale non è coerente con i target inseriti"
        }
        return CarbCyclePlan(high, medium, low, weekly / 7)
    }
}
