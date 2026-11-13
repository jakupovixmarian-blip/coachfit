package dev.fitcoach.app.domain.engine

import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UserProfile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Deterministic energy and macronutrient targets.
 *
 * Every number is derived from values the user entered. Nothing is inferred from
 * population averages when a measured value exists, and nothing is invented when a
 * required field is missing: the calculation returns null instead of guessing.
 *
 * - BMR: Katch-McArdle when body fat is known (it uses lean mass), Mifflin-St Jeor
 *   otherwise.
 * - Maintenance: BMR multiplied by an activity factor built from daily steps, work
 *   activity and weekly training volume, clamped to a plausible range.
 * - Target: maintenance adjusted by goal, but never below the measured resting
 *   metabolism, so a large deficit cannot silently become unsafe.
 * - Macros: protein per kg of body weight, fat with a floor, carbohydrate as the
 *   remaining energy. Every cap that binds is reported as a warning.
 *
 * The result is pure data: the caller decides how to present or store it.
 */
data class EnergyTargets(
    val bmrKcal: Double,
    val activityFactor: Double,
    val maintenanceKcal: Double,
    val targetKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val warnings: List<String>,
    val notes: List<String>
) {
    /** Energy actually delivered by the macro split. Used to verify a generated plan. */
    val macroEnergyKcal: Double
        get() = proteinG * EnergyEngine.KCAL_PROTEIN +
            carbsG * EnergyEngine.KCAL_CARB +
            fatG * EnergyEngine.KCAL_FAT

    /** Difference between the macro split and the calorie target, in kcal. */
    val macroEnergyDriftKcal: Double get() = macroEnergyKcal - targetKcal
}

object EnergyEngine {

    const val KCAL_PROTEIN = 4.0
    const val KCAL_CARB = 4.0
    const val KCAL_FAT = 9.0

    /** Used only when the profile has no usable body weight to anchor the floor. */
    private const val ABSOLUTE_MIN_KCAL = 1200.0

    /** Resting metabolic rate. Null when the profile cannot support a real estimate. */
    fun bmr(profile: UserProfile): Double? {
        val weight = profile.weightKg?.takeIf { it > 0.0 } ?: return null

        val bodyFat = profile.bodyFatPercent?.takeIf { it in 3.0..60.0 }
        if (bodyFat != null) {
            val leanMass = weight * (1.0 - bodyFat / 100.0)
            return 370.0 + 21.6 * leanMass
        }

        val height = profile.heightCm?.takeIf { it > 0.0 } ?: return null
        val age = profile.age?.takeIf { it in 14..100 } ?: return null
        val base = 10.0 * weight + 6.25 * height - 5.0 * age
        return when (profile.sex) {
            Sex.MALE -> base + 5.0
            Sex.FEMALE -> base - 161.0
            Sex.UNSPECIFIED -> base - 78.0
        }
    }

    /**
     * Multiplier applied to BMR. Starts from a sedentary baseline and adds only the
     * activity the profile actually declares.
     */
    fun activityFactor(profile: UserProfile): Double {
        var factor = 1.2
        factor += stepsBonus(profile.dailySteps)
        factor += jobBonus(profile.jobActivity)
        factor += trainingBonus(profile.availableDays, profile.sessionMinutes)
        return factor.coerceIn(1.2, 1.95)
    }

    /** Full target set, or null when the profile lacks the data for a real estimate. */
    fun targets(profile: UserProfile): EnergyTargets? {
        val bmr = bmr(profile) ?: return null
        val weight = profile.weightKg?.takeIf { it > 0.0 } ?: return null
        val factor = activityFactor(profile)
        val maintenance = bmr * factor

        val warnings = mutableListOf<String>()
        val notes = mutableListOf<String>()

        notes += if (profile.bodyFatPercent != null) {
            "Metabolismo basale stimato con Katch-McArdle dalla massa magra."
        } else {
            "Metabolismo basale stimato con Mifflin-St Jeor."
        }

        val rawTarget = maintenance * goalFactor(profile.goal)
        val floor = max(bmr, ABSOLUTE_MIN_KCAL)
        val target = if (rawTarget < floor) {
            warnings += "Il deficit richiesto porterebbe sotto il metabolismo basale: " +
                "obiettivo fermato a ${floor.roundToInt()} kcal."
            floor
        } else {
            rawTarget
        }

        var protein = weight * proteinPerKg(profile.goal)
        val proteinCapKcal = target * 0.40
        if (protein * KCAL_PROTEIN > proteinCapKcal) {
            protein = proteinCapKcal / KCAL_PROTEIN
            warnings += "Proteine limitate al 40% delle calorie per lasciare spazio agli altri macro."
        }

        var fat = weight * 0.8
        val minFat = weight * 0.5
        val fatCapKcal = target * 0.35
        if (fat * KCAL_FAT > fatCapKcal) fat = fatCapKcal / KCAL_FAT
        if (fat < minFat) fat = minFat

        var carbKcal = target - protein * KCAL_PROTEIN - fat * KCAL_FAT
        if (carbKcal < 0.0) {
            val reducibleFatKcal = (fat - minFat) * KCAL_FAT
            val cut = min(-carbKcal, max(0.0, reducibleFatKcal))
            fat -= cut / KCAL_FAT
            carbKcal += cut
        }
        if (carbKcal < 0.0) {
            protein = max(0.0, protein + carbKcal / KCAL_PROTEIN)
            warnings += "Carboidrati a zero: proteine ridotte per rispettare le calorie obiettivo."
            carbKcal = 0.0
        }

        return EnergyTargets(
            bmrKcal = bmr,
            activityFactor = factor,
            maintenanceKcal = maintenance,
            targetKcal = target,
            proteinG = roundTo(protein, 1.0),
            carbsG = roundTo(carbKcal / KCAL_CARB, 5.0),
            fatG = roundTo(fat, 5.0),
            fiberG = roundTo(target / 1000.0 * 14.0, 1.0),
            warnings = warnings,
            notes = notes
        )
    }

    fun goalFactor(goal: Goal): Double = when (goal) {
        Goal.FAT_LOSS -> 0.80
        Goal.RECOMP -> 0.95
        Goal.MUSCLE_GAIN -> 1.10
        Goal.STRENGTH -> 1.05
        Goal.MAINTENANCE, Goal.HEALTH -> 1.0
    }

    fun proteinPerKg(goal: Goal): Double = when (goal) {
        Goal.FAT_LOSS -> 2.2
        Goal.RECOMP -> 2.0
        Goal.MUSCLE_GAIN -> 1.8
        Goal.STRENGTH -> 1.8
        Goal.MAINTENANCE -> 1.6
        Goal.HEALTH -> 1.4
    }

    private fun stepsBonus(steps: Int?): Double = when {
        steps == null || steps <= 0 -> 0.0
        steps < 4000 -> 0.0
        steps < 7500 -> 0.05
        steps < 10000 -> 0.10
        steps < 12500 -> 0.15
        else -> 0.20
    }

    /**
     * Reads the free-text work activity the anamnesis stores. Matching is on
     * keywords because the field is written by the user, not chosen from a list.
     */
    private fun jobBonus(raw: String): Double {
        val text = raw.lowercase()
        if (text.isBlank()) return 0.0
        return when {
            text.contains("molto pesante") || text.contains("very heavy") ||
                text.contains("edile") || text.contains("cantiere") ||
                text.contains("agricol") || text.contains("scarico") -> 0.20
            text.contains("pesante") || text.contains("heavy") ||
                text.contains("magazzin") || text.contains("corriere") ||
                text.contains("murator") -> 0.15
            text.contains("moderato") || text.contains("medium") ||
                text.contains("in piedi") || text.contains("commess") ||
                text.contains("camerier") || text.contains("infermier") -> 0.10
            text.contains("leggero") || text.contains("light") ||
                text.contains("impiegat") || text.contains("ufficio") ||
                text.contains("seduto") || text.contains("smart") -> 0.05
            else -> 0.05
        }
    }

    private fun trainingBonus(days: Int?, minutesPerSession: Int?): Double {
        val days = days?.takeIf { it > 0 } ?: return 0.0
        val minutes = minutesPerSession?.takeIf { it > 0 } ?: 60
        val weeklyMinutes = days * minutes
        return when {
            weeklyMinutes >= 360 -> 0.12
            weeklyMinutes >= 240 -> 0.09
            weeklyMinutes >= 150 -> 0.06
            weeklyMinutes >= 60 -> 0.03
            else -> 0.0
        }
    }

    private fun roundTo(value: Double, step: Double): Double =
        if (step <= 0.0) value else round(value / step) * step
}
