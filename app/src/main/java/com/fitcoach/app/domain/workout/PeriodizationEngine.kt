package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Mesocycle
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile

/**
 * Periodizzazione a blocchi.
 *
 * Struttura di default:
 *  - Mesociclo di accumulo (ipertrofia) di N settimane con volume crescente.
 *  - Settimana di deload (riduzione volume ~40%) quando giustificata.
 *  - Rivalutazione e nuovo mesociclo.
 */
object PeriodizationEngine {

    data class WeekPlan(
        val weekIndex: Int,
        val isDeload: Boolean,
        val volumeMultiplier: Double,
        val intensityMultiplier: Double,
        val targetRirDelta: Int,
        val label: String
    )

    /**
     * Costruisce le settimane di un mesociclo.
     * @param mesocycleWeeks durata in settimane (default 6)
     */
    fun buildMesocycle(
        mesocycleWeeks: Int = 6,
        experience: TrainingExperience,
        goal: Goal
    ): List<WeekPlan> {
        val weeks = mesocycleWeeks.coerceIn(3, 12)
        val deloadWeek = when (experience) {
            TrainingExperience.BEGINNER -> null // i principianti spesso non necessitano deload
            TrainingExperience.INTERMEDIATE -> if (weeks >= 5) weeks - 1 else null
            TrainingExperience.ADVANCED -> weeks - 1
        }

        return (0 until weeks).map { w ->
            val isDeload = w == deloadWeek
            if (isDeload) {
                WeekPlan(
                    weekIndex = w,
                    isDeload = true,
                    volumeMultiplier = 0.6,
                    intensityMultiplier = 0.9,
                    targetRirDelta = 2,
                    label = "Deload"
                )
            } else {
                // Accumulo progressivo: +5% volume per settimana, intensita stabile.
                val accumulationWeeks = (0 until weeks).count { it != deloadWeek }
                val position = (0 until w).count { it != deloadWeek }
                val volume = 1.0 + 0.05 * position
                val intensity = when (goal) {
                    Goal.HYPERTROPHY -> 1.0 + 0.01 * position
                    Goal.CUT -> 0.98
                    else -> 1.0
                }
                WeekPlan(
                    weekIndex = w,
                    isDeload = false,
                    volumeMultiplier = volume,
                    intensityMultiplier = intensity.coerceAtMost(1.1),
                    targetRirDelta = if (position >= accumulationWeeks - 1) -1 else 0,
                    label = "Accumulo ${position + 1}"
                )
            }
        }
    }

    fun defaultMesocycle(
        planId: Long,
        profile: UserProfile,
        weeks: Int = 6
    ): Mesocycle {
        val focus = when (profile.goal) {
            Goal.HYPERTROPHY -> "Ipertrofia"
            Goal.CUT -> "Definizione (mantenimento forza)"
            Goal.RECOMP -> "Ricomposizione"
            Goal.MAINTENANCE -> "Mantenimento"
            Goal.STRENGTH -> "Forza"
        }
        return Mesocycle(
            planId = planId,
            name = "Mesociclo $focus",
            focus = focus,
            weekCount = weeks,
            startWeekIndex = 0,
            deloadWeekIndex = when (profile.experience) {
                TrainingExperience.BEGINNER -> null
                else -> weeks - 1
            },
            isDeload = false,
            progressionModel = "Double Progression"
        )
    }
}
