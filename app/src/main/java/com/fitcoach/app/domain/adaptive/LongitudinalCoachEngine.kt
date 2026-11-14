package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeeklyNutritionSummary
import com.fitcoach.app.domain.model.WorkoutSession
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Snapshot aggregato usato dal check-in. Contiene solo dati osservati su piu
 * settimane: il motore non prende decisioni da una singola giornata.
 */
data class LongitudinalContext(
    val profile: UserProfile,
    val planId: Long,
    val mesocycleId: Long?,
    val currentWeeklyKcal: Double,
    val weightTrendKgPerWeek: Double?,
    val reliableWeightTrend: Boolean,
    val weightWeeks: Int = 0,
    val waistTrendCmPerWeek: Double?,
    val nutritionWeeks: List<NutritionWeek>,
    val performances: List<ExerciseTrend>,
    val recoverySeverity: Int,
    val painSessions: Int,
    val recentSessions: List<WorkoutSession>
)

data class NutritionWeek(
    val weekStartEpochDay: Long,
    val summary: WeeklyNutritionSummary
)

enum class ExerciseTrendClass { PROGRESSION, STAGNATION, REGRESSION, INSUFFICIENT_DATA }

data class ExerciseTrend(
    val exerciseName: String,
    val classification: ExerciseTrendClass,
    val changePercent: Double,
    val samples: Int
)

data class AdaptiveDecision(
    val action: CoachAction?,
    val classification: String,
    val reason: String
)

/** Motore deterministico longitudinale. Non chiama provider e non scrive su Room. */
object LongitudinalCoachEngine {
    const val MIN_NUTRITION_WEEKS = 2
    const val MIN_WEIGHT_WEEKS = 3
    const val MIN_ADHERENCE = 75.0

    fun classifyExercise(name: String, valuesNewestFirst: List<Double>): ExerciseTrend {
        if (valuesNewestFirst.size < 6) {
            return ExerciseTrend(name, ExerciseTrendClass.INSUFFICIENT_DATA, 0.0, valuesNewestFirst.size)
        }
        val recent = valuesNewestFirst.take(3).average()
        val previous = valuesNewestFirst.drop(3).take(3).average()
        if (previous <= 0.0) return ExerciseTrend(name, ExerciseTrendClass.INSUFFICIENT_DATA, 0.0, valuesNewestFirst.size)
        val change = (recent - previous) / previous * 100.0
        val classification = when {
            change >= 5.0 -> ExerciseTrendClass.PROGRESSION
            change <= -5.0 -> ExerciseTrendClass.REGRESSION
            else -> ExerciseTrendClass.STAGNATION
        }
        return ExerciseTrend(name, classification, change, valuesNewestFirst.size)
    }

    fun evaluate(context: LongitudinalContext): List<AdaptiveDecision> {
        val decisions = mutableListOf<AdaptiveDecision>()
        val nutrition = context.nutritionWeeks
            .filter { it.summary.daysTracked > 0 }
            .sortedByDescending { it.weekStartEpochDay }
        val adherence = nutrition.take(3).map { it.summary.avgAdherencePercent }.average()
        val target = targetRate(context.profile.goal)
        val trend = context.weightTrendKgPerWeek

        if (nutrition.size >= MIN_NUTRITION_WEEKS && trend != null &&
            context.reliableWeightTrend && context.weightWeeks >= MIN_WEIGHT_WEEKS
        ) {
            if (adherence < MIN_ADHERENCE) {
                decisions += AdaptiveDecision(
                    action = null,
                    classification = "ADHERENCE_INSUFFICIENT",
                    reason = "Aderenza media ${adherence.roundToInt()}% su ${nutrition.size} settimane: non modificare calorie finché il piano non viene seguito."
                )
            } else {
                val waistImproving = context.waistTrendCmPerWeek != null && context.waistTrendCmPerWeek <= -0.1
                when {
                    waistImproving && abs(trend) < 0.1 -> decisions += AdaptiveDecision(
                        action = null,
                        classification = "RECOMPOSITION_PROGRESS",
                        reason = "Peso stabile ma vita in riduzione (${fmt(context.waistTrendCmPerWeek)} cm/settimana): mantenere il piano."
                    )
                    (context.profile.goal == Goal.CUT || context.profile.goal == Goal.RECOMP) && abs(trend) < 0.1 ->
                        decisions += calorieDecision(context, -0.07, "STAGNATION")
                    (context.profile.goal == Goal.CUT || context.profile.goal == Goal.RECOMP) && trend < target - 0.2 ->
                        decisions += calorieDecision(context, 0.06, "REGRESSION_NUTRITION")
                    context.profile.goal == Goal.HYPERTROPHY && trend < 0.1 ->
                        decisions += calorieDecision(context, 0.08, "UNDER_TARGET_GAIN")
                    context.profile.goal == Goal.HYPERTROPHY && trend > target + 0.25 ->
                        decisions += calorieDecision(context, -0.06, "EXCESS_GAIN")
                }
            }
        }

        val regressions = context.performances.count { it.classification == ExerciseTrendClass.REGRESSION }
        val stagnations = context.performances.count { it.classification == ExerciseTrendClass.STAGNATION }
        val progression = context.performances.count { it.classification == ExerciseTrendClass.PROGRESSION }
        when {
            context.painSessions >= 2 || (regressions > 0 && context.recoverySeverity >= 3) -> {
                decisions += workoutDecision(
                    context,
                    CoachAction.ACTION_DELOAD,
                    "REGRESSION_RECOVERY",
                    "Regressione su $regressions esercizi con recupero insufficiente o dolore: ridurre il volume per recuperare."
                )
            }
            regressions > 0 -> decisions += workoutDecision(
                context,
                CoachAction.ACTION_ADJUST_VOLUME,
                "REGRESSION",
                "Regressione su $regressions esercizi con recupero non gravemente compromesso: ridurre il volume del 20%."
            )
            stagnations >= 2 && context.recoverySeverity <= 1 -> decisions += workoutDecision(
                context,
                CoachAction.ACTION_ADJUST_VOLUME,
                "STAGNATION",
                "Stagnazione su $stagnations esercizi con recupero sufficiente: aumentare il volume solo del 10% e rivalutare in due settimane."
            )
            progression > 0 -> decisions += AdaptiveDecision(
                action = null,
                classification = "PROGRESSION",
                reason = "Progressione osservata su $progression esercizi: mantenere esercizi, carichi e volume attuali."
            )
        }

        if (nutrition.size < MIN_NUTRITION_WEEKS || !context.reliableWeightTrend || context.weightWeeks < MIN_WEIGHT_WEEKS) {
            decisions += AdaptiveDecision(
                null,
                "INSUFFICIENT_DATA",
                "Servono almeno due settimane di consumi reali e tre settimane di peso medio prima di modificare il piano."
            )
        }
        return decisions
    }

    private fun calorieDecision(context: LongitudinalContext, fraction: Double, classification: String): AdaptiveDecision {
        val newWeekly = (context.currentWeeklyKcal * (1.0 + fraction)).coerceAtLeast(1.0)
        val sign = if (fraction >= 0) "+" else ""
        val action = CoachAction(
            action = CoachAction.ACTION_ADJUST_CALORIES,
            domain = "NUTRITION",
            title = "Aggiustamento calorico longitudinale",
            situation = "Trend peso ${fmt(context.weightTrendKgPerWeek)} kg/settimana su almeno ${MIN_WEIGHT_WEEKS} settimane.",
            problem = "La variazione osservata non è coerente con l'obiettivo ${context.profile.goal.label}.",
            dataUsed = "Trend peso, media macro/calorie e aderenza di ${context.nutritionWeeks.size} settimane; vita ${fmt(context.waistTrendCmPerWeek)} cm/settimana.",
            proposedChange = "Variare il target settimanale del ${sign}${(fraction * 100).roundToInt()}% e ricalcolare le grammature dei pasti.",
            rationale = "La variazione è limitata e viene valutata solo dopo dati longitudinali affidabili e aderenza sufficiente.",
            expectedOutcome = "Avvicinarsi al trend obiettivo ${fmt(targetRate(context.profile.goal))} kg/settimana nelle prossime due settimane.",
            contraindications = "Non applicare con aderenza inferiore al ${MIN_ADHERENCE.roundToInt()}% o sotto la soglia calorica di sicurezza.",
            payload = mapOf(
                "planId" to context.planId.toString(),
                "weeklyKcal" to newWeekly.toString(),
                "classification" to classification,
                "scaleMeals" to "true"
            )
        )
        return AdaptiveDecision(action, classification, action.proposedChange)
    }

    private fun workoutDecision(
        context: LongitudinalContext,
        actionName: String,
        classification: String,
        reason: String
    ): AdaptiveDecision {
        val action = if (context.mesocycleId != null) CoachAction(
            action = actionName,
            domain = "WORKOUT",
            title = if (actionName == CoachAction.ACTION_DELOAD) "Deload longitudinale" else "Aggiustamento volume longitudinale",
            situation = reason,
            problem = "L'andamento su piu settimane richiede un intervento sull'allenamento.",
            dataUsed = "${context.performances.size} esercizi analizzati, recupero ${context.recoverySeverity}/3, dolore in ${context.painSessions} sedute.",
            proposedChange = if (actionName == CoachAction.ACTION_DELOAD) "Ridurre il volume del 40% e l'intensità del 10% per una settimana." else "Variare il volume del 20% e rivalutare dopo due settimane.",
            rationale = reason,
            expectedOutcome = "Ridurre i segnali di fatica e migliorare la tendenza delle prestazioni.",
            contraindications = "Interrompere gli esercizi dolorosi; il dolore persistente richiede valutazione professionale.",
            payload = mapOf(
                "mesocycleId" to context.mesocycleId.toString(),
                "planId" to context.planId.toString(),
                "volumeReductionPercent" to if (actionName == CoachAction.ACTION_DELOAD) "40" else "20"
            )
        ) else null
        return AdaptiveDecision(action, classification, reason)
    }

    private fun targetRate(goal: Goal): Double = when (goal) {
        Goal.CUT -> -0.5
        Goal.RECOMP -> -0.25
        Goal.HYPERTROPHY -> 0.3
        Goal.MAINTENANCE -> 0.0
        Goal.STRENGTH -> 0.15
    }

    private fun fmt(value: Double?): String = value?.let { "%+.2f".format(it) } ?: "n/d"
}
