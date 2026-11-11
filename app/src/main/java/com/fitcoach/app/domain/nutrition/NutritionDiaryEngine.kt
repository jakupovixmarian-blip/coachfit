package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.DailyBalance
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.model.WeeklyNutritionSummary
import java.time.LocalDate
import kotlin.math.abs

/**
 * Motore deterministico del diario nutrizionale e del bilancio calorico.
 *
 * Distingue in modo rigoroso:
 *  - OBIETTIVO: quanto previsto dal piano approvato per la giornata (target);
 *  - CONSUMATO: quanto l'utente ha realmente registrato (voci attive);
 *  - RIMANENTE: differenza, con segnalazione esplicita degli eccessi.
 *
 * Gli alimenti extra (fuori piano) vengono sommati fedelmente: non vengono mai
 * nascosti e non modificano automaticamente gli altri pasti.
 */
object NutritionDiaryEngine {

    /** Somma le voci attive (evita il doppio conteggio delle voci deselezionate). */
    fun consumed(entries: List<DiaryEntry>): MacroTotals =
        entries.filter { it.active }.fold(MacroTotals.ZERO) { acc, e -> acc + e.macros }

    /**
     * Calcola il bilancio del giorno confrontando il target del piano con i
     * consumi reali. Se il piano non copre il giorno richiesto, il target e zero.
     */
    fun dailyBalance(
        plan: NutritionPlan?,
        dayIndex: Int,
        dateEpochDay: Long,
        entries: List<DiaryEntry>
    ): DailyBalance {
        val day: NutritionDay? = plan?.days?.firstOrNull { it.dayIndex == dayIndex }
        val consumed = consumed(entries.filter { it.dateEpochDay == dateEpochDay })
        return DailyBalance(
            dateEpochDay = dateEpochDay,
            targetKcal = day?.targetKcal ?: 0.0,
            targetProtein = day?.targetProtein ?: 0.0,
            targetCarbs = day?.targetCarbs ?: 0.0,
            targetFat = day?.targetFat ?: 0.0,
            consumedKcal = consumed.kcal,
            consumedProtein = consumed.protein,
            consumedCarbs = consumed.carbs,
            consumedFat = consumed.fat
        )
    }

    /** Raggruppa le voci attive per pasto, mantenendo l'ordine degli slot. */
    fun byMeal(entries: List<DiaryEntry>, dateEpochDay: Long): Map<MealSlot, List<DiaryEntry>> {
        val dayEntries = entries.filter { it.dateEpochDay == dateEpochDay && it.active }
        return MealSlot.entries.associateWith { slot ->
            dayEntries.filter { it.mealSlot == slot }.sortedBy { it.createdAt }
        }.filterValues { it.isNotEmpty() }
    }

    /**
     * Riepilogo settimanale basato sui consumi REALI (non sul piano).
     * L'aderenza e la percentuale di giorni in cui il consumato resta entro il
     * target calorico del giorno corrispondente.
     */
    fun weeklySummary(
        plan: NutritionPlan?,
        entries: List<DiaryEntry>,
        weekStartEpochDay: Long,
        weekLength: Int = 7
    ): WeeklyNutritionSummary {
        val days = (0 until weekLength).map { weekStartEpochDay + it }
        val perDay = days.map { epoch ->
            val idx = dayIndexFor(epoch)
            epoch to dailyBalance(plan, idx, epoch, entries)
        }
        val tracked = perDay.filter { it.second.consumedKcal > 0 }
        val n = tracked.size.coerceAtLeast(1)
        val onTarget = tracked.count { it.second.kcalWithinTarget && it.second.targetKcal > 0 }
        val avgKcal = tracked.sumOf { it.second.consumedKcal } / n
        val avgPlanned = tracked.map { it.second.targetKcal }.average().takeIf { !it.isNaN() } ?: 0.0
        val adherence = tracked.mapNotNull { (_, b) ->
            if (b.targetKcal > 0) (b.consumedKcal / b.targetKcal * 100).coerceAtMost(150.0) else null
        }
        return WeeklyNutritionSummary(
            daysTracked = tracked.size,
            avgKcal = avgKcal,
            avgProtein = tracked.sumOf { it.second.consumedProtein } / n,
            avgCarbs = tracked.sumOf { it.second.consumedCarbs } / n,
            avgFat = tracked.sumOf { it.second.consumedFat } / n,
            avgPlannedKcal = avgPlanned,
            avgAdherencePercent = if (adherence.isEmpty()) 0.0 else adherence.average(),
            daysOnTarget = onTarget,
            extraEntries = entries.count { it.source.name == "EXTRA" && it.active && it.dateEpochDay in days },
            kcalDifference = avgKcal - avgPlanned
        )
    }

    fun dayIndexFor(epochDay: Long): Int = LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1

    /** Trova la tipologia di giornata (HIGH/MEDIUM/LOW) del piano per una data. */
    fun carbDayTypeFor(plan: NutritionPlan?, epochDay: Long): CarbDayType? =
        plan?.days?.firstOrNull { it.dayIndex == dayIndexFor(epochDay) }?.dayType

    /** Verifica di coerenza: i target del piano devono corrispondere ai macro. */
    fun targetIsCoherent(target: MacroTotals, kcalTolerancePercent: Double = 5.0): Boolean {
        val macroKcal = target.protein * 4 + target.carbs * 4 + target.fat * 9
        if (target.kcal <= 0) return false
        return abs(macroKcal - target.kcal) / target.kcal * 100 <= kcalTolerancePercent
    }
}
