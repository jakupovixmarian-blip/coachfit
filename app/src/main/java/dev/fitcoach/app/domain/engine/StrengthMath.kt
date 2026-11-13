package dev.fitcoach.app.domain.engine

import kotlin.math.round

/**
 * Strength arithmetic used by the progression engine and the workout summary.
 *
 * Estimated 1RM is a projection from a real set, not a measurement: it is labelled
 * as an estimate everywhere it is shown, and it is only produced when the set has a
 * weight and a plausible repetition count.
 */
object StrengthMath {

    private const val MAX_REPS_FOR_ESTIMATE = 30
    private const val RELIABLE_REPS = 10

    fun epley1Rm(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps !in 1..MAX_REPS_FOR_ESTIMATE) return null
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    fun brzycki1Rm(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps !in 1 until 37) return null
        if (reps == 1) return weightKg
        return weightKg * 36.0 / (37.0 - reps)
    }

    /**
     * Average of the two formulas up to 10 reps, where both are meaningful; Epley
     * alone above, because Brzycki diverges quickly.
     */
    fun estimate1Rm(weightKg: Double, reps: Int): Double? {
        val epley = epley1Rm(weightKg, reps) ?: return null
        if (reps <= 1) return weightKg
        if (reps > RELIABLE_REPS) return epley
        val brzycki = brzycki1Rm(weightKg, reps) ?: return epley
        return (epley + brzycki) / 2.0
    }

    /** Total weight moved: sum of weight times reps for every working set. */
    fun volumeLoad(sets: List<Pair<Double, Int>>): Double = sets.sumOf { (weight, reps) ->
        if (weight <= 0.0 || reps <= 0) 0.0 else weight * reps
    }

    /** How heavy a set was relative to the estimated maximum, in percent. */
    fun intensityPercent(weightKg: Double, estimated1Rm: Double?): Double? {
        if (estimated1Rm == null || estimated1Rm <= 0.0 || weightKg <= 0.0) return null
        return weightKg / estimated1Rm * 100.0
    }

    /** Rounds a suggested load to what the user can actually put on the bar. */
    fun roundToPlate(weightKg: Double, increment: Double = 2.5): Double {
        if (increment <= 0.0) return weightKg
        return round(weightKg / increment) * increment
    }
}
