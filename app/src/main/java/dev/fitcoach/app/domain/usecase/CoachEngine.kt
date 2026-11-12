package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.CoachInsight
import dev.fitcoach.app.domain.model.DiaryEntry
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.InsightKind
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.Measurement
import dev.fitcoach.app.domain.model.MeasurementType
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.model.WorkoutSession
import java.time.LocalDate
import kotlin.math.abs

data class PlanValidation(
    val valid: Boolean,
    val totalKcal: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val issues: List<String>
)

/**
 * Rule-based analysis over the user's real data. Everything here is computed from
 * stored values: nothing is estimated or invented.
 */
object CoachEngine {

    fun insights(
        profile: UserProfile?,
        sessions: List<WorkoutSession>,
        measurements: List<Measurement>,
        diary: List<DiaryEntry>,
        foods: List<Food>,
        setsBySession: Map<Long, List<LoggedSet>>
    ): List<CoachInsight> {
        val insights = mutableListOf<CoachInsight>()

        // 1. Training adherence over the last 4 weeks
        val fourWeeksAgo = LocalDate.now().minusWeeks(4).toEpochDay()
        val recentSessions = sessions.filter { it.dateEpochDay >= fourWeeksAgo }
        val expected = (profile?.availableDays ?: 3) * 4
        val adherence = if (expected > 0) recentSessions.size.toDouble() / expected * 100.0 else 0.0
        insights += CoachInsight(
            title = "Aderenza allenamento",
            detail = "${recentSessions.size} sedute su $expected attese nelle ultime 4 settimane (%.0f%%).".format(adherence),
            kind = when {
                adherence >= 80 -> InsightKind.ADHERENCE
                adherence >= 50 -> InsightKind.INFO
                else -> InsightKind.REGRESSION
            }
        )

        // 2. Volume trend: last week vs previous week
        val weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toEpochDay()
        val thisWeek = sessions.filter { it.dateEpochDay in weekStart..(weekStart + 6) }
        val lastWeek = sessions.filter { it.dateEpochDay in (weekStart - 7)..(weekStart - 1) }
        val thisVolume = thisWeek.sumOf { session -> setsBySession[session.id]?.let { ProgressionEngine.volume(it) } ?: 0.0 }
        val lastVolume = lastWeek.sumOf { session -> setsBySession[session.id]?.let { ProgressionEngine.volume(it) } ?: 0.0 }
        if (lastVolume > 0) {
            val change = (thisVolume - lastVolume) / lastVolume * 100.0
            insights += CoachInsight(
                title = "Volume settimanale",
                detail = "Questa settimana %.0f kg contro %.0f kg della precedente (%+.0f%%).".format(thisVolume, lastVolume, change),
                kind = when {
                    change > 5 -> InsightKind.PROGRESS
                    change < -15 -> InsightKind.REGRESSION
                    else -> InsightKind.INFO
                }
            )
        }

        // 3. Stagnation detection on the most trained exercises
        val exerciseFrequency = setsBySession.values.flatten()
            .filter { it.completed && it.weightKg != null }
            .groupBy { it.exerciseId }
            .filter { it.value.size >= 6 }
        exerciseFrequency.entries.take(3).forEach { (exerciseId, sets) ->
            val ordered = sets.sortedBy { it.loggedAt }
            val firstHalf = ordered.take(ordered.size / 2).maxOfOrNull { (it.weightKg ?: 0.0) } ?: 0.0
            val secondHalf = ordered.drop(ordered.size / 2).maxOfOrNull { (it.weightKg ?: 0.0) } ?: 0.0
            if (secondHalf <= firstHalf && firstHalf > 0) {
                insights += CoachInsight(
                    title = "Possibile stallo (esercizio $exerciseId)",
                    detail = "Il carico massimo non e aumentato (%.1f kg -> %.1f kg) sulle ultime serie registrate.".format(firstHalf, secondHalf),
                    kind = InsightKind.STAGNATION
                )
            }
        }

        // 4. Weight trend
        val weights = measurements.filter { it.type == MeasurementType.WEIGHT }.sortedBy { it.dateEpochDay }
        if (weights.size >= 2) {
            val delta = weights.last().value - weights.first().value
            val days = (weights.last().dateEpochDay - weights.first().dateEpochDay).coerceAtLeast(1)
            val perWeek = delta / days * 7
            insights += CoachInsight(
                title = "Andamento peso",
                detail = "Variazione %.1f kg in $days giorni (circa %+.2f kg/settimana).".format(delta, perWeek),
                kind = InsightKind.BODY_COMP
            )
        }

        // 5. Nutrition adherence for today
        val today = LocalDate.now().toEpochDay()
        val todayEntries = diary.filter { it.dateEpochDay == today && it.consumed }
        val target = profile?.targetCalories()
        if (target != null && target > 0) {
            val consumed = todayEntries.sumOf { entry ->
                val food = foods.firstOrNull { it.id == entry.foodId } ?: return@sumOf 0.0
                food.kcalPer100g * entry.grams / 100.0
            }
            val diff = consumed - target
            insights += CoachInsight(
                title = "Bilancio calorico di oggi",
                detail = "%.0f kcal registrate su un obiettivo di %.0f kcal (%+.0f kcal).".format(consumed, target, diff),
                kind = if (abs(diff) <= target * 0.1) InsightKind.ADHERENCE else InsightKind.INFO
            )
        }

        return insights
    }

    /** Validates an AI-generated plan against the real food catalog and the targets. */
    fun validatePlan(
        items: List<Pair<Long, Double>>,
        foodById: Map<Long, Food>,
        targetKcal: Double?,
        targetProtein: Double?,
        targetCarbs: Double?,
        targetFat: Double?
    ): PlanValidation {
        val issues = mutableListOf<String>()
        var kcal = 0.0; var protein = 0.0; var carbs = 0.0; var fat = 0.0
        items.forEach { (foodId, grams) ->
            val food = foodById[foodId]
            if (food == null) {
                issues.add("Alimento $foodId non presente nell'archivio: voce rifiutata.")
                return@forEach
            }
            if (grams <= 0 || grams > 3000) {
                issues.add("Grammatura non plausibile per ${food.name}: ${grams.toInt()} g.")
                return@forEach
            }
            val factor = grams / 100.0
            kcal += food.kcalPer100g * factor
            protein += food.proteinPer100g * factor
            carbs += food.carbsPer100g * factor
            fat += food.fatPer100g * factor
        }
        fun check(label: String, actual: Double, target: Double?, tolerance: Double) {
            if (target == null || target <= 0) return
            val deviation = abs(actual - target) / target
            if (deviation > tolerance) {
                issues.add(
                    "%s: %.0f contro un obiettivo di %.0f (scostamento %.0f%%).".format(
                        label, actual, target, deviation * 100
                    )
                )
            }
        }
        check("Calorie", kcal, targetKcal, 0.10)
        check("Proteine", protein, targetProtein, 0.15)
        check("Carboidrati", carbs, targetCarbs, 0.20)
        check("Grassi", fat, targetFat, 0.20)

        return PlanValidation(
            valid = issues.isEmpty(),
            totalKcal = kcal,
            totalProtein = protein,
            totalCarbs = carbs,
            totalFat = fat,
            issues = issues
        )
    }

    /** Suggests a gram adjustment for a single food to close a calorie gap. */
    fun adjustGramsForCalories(food: Food, currentGrams: Double, deltaKcal: Double): Double? {
        if (food.kcalPer100g <= 0) return null
        val deltaGrams = deltaKcal / food.kcalPer100g * 100.0
        val updated = currentGrams + deltaGrams
        if (updated <= 0 || updated > 2000) return null
        return updated
    }
}
