package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.dao.PlanVersionDao
import com.fitcoach.app.data.local.entity.PlanVersionEntity
import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.WorkoutPlan
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storico delle versioni dei piani. Ogni rigenerazione salva uno snapshot
 * testuale immutabile, cosi l'utente puo consultare e confrontare le versioni.
 */
@Singleton
class PlanVersionRepository @Inject constructor(
    private val dao: PlanVersionDao
) {
    suspend fun snapshotNutrition(plan: NutritionPlan, reason: String) {
        dao.clearActiveForDomain(ChangeDomain.NUTRITION)
        dao.insert(
            PlanVersionEntity(
                domain = ChangeDomain.NUTRITION,
                planId = plan.id,
                version = plan.version,
                label = "Nutrizione v${plan.version}",
                reason = reason,
                snapshotJson = nutritionSummary(plan),
                isActive = true
            )
        )
    }

    suspend fun snapshotWorkout(plan: WorkoutPlan, reason: String) {
        dao.clearActiveForDomain(ChangeDomain.WORKOUT)
        dao.insert(
            PlanVersionEntity(
                domain = ChangeDomain.WORKOUT,
                planId = plan.id,
                version = plan.version,
                label = "Allenamento v${plan.version}",
                reason = reason,
                snapshotJson = workoutSummary(plan),
                isActive = true
            )
        )
    }

    suspend fun getForDomain(domain: ChangeDomain): List<PlanVersionEntity> = dao.getForDomain(domain)

    suspend fun getAll(): List<PlanVersionEntity> = dao.getAll()

    private fun nutritionSummary(plan: NutritionPlan): String = buildString {
        append("Calorie settimanali: ${plan.weeklyCalories.toInt()} kcal; ")
        append("BMR ${plan.bmr.toInt()}, TDEE ${plan.tdee.toInt()}; ")
        append("carb cycling: ${if (plan.carbCyclingApplied) "si" else "no"}; ")
        append("giorni HIGH/MED/LOW: ${plan.highCarbDays}/${plan.mediumCarbDays}/${plan.lowCarbDays}; ")
        append("proteine ${"%.1f".format(plan.proteinGPerKg)} g/kg.")
    }

    private fun workoutSummary(plan: WorkoutPlan): String = buildString {
        append("Split: ${plan.splitType.label}; ")
        append("mesocicli: ${plan.mesocycles.size}; ")
        append("giorni di allenamento: ${plan.days.count { !it.isRestDay }}; ")
        append("volume/gruppo: ")
        append(plan.weeklyVolumePerGroup.entries.joinToString { "${it.key.label} ${it.value}" })
        append(".")
    }
}
