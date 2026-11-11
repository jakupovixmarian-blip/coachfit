package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.DiarySource
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NutritionDiaryEngineTest {

    private fun day(index: Int) = NutritionDay(
        id = (index + 1).toLong(), planId = 1, dayIndex = index, dayType = CarbDayType.MEDIUM,
        targetKcal = 2200.0, targetProtein = 160.0, targetCarbs = 240.0, targetFat = 65.0
    )

    private val plan = NutritionPlan(
        id = 1, name = "Test", bmr = 1700.0, tdee = 2600.0,
        weeklyCalories = 15400.0, weeklyKcalBalance = -400.0,
        bmrFormula = "test", tdeeFormula = "test",
        highCarbDays = 2, mediumCarbDays = 2, lowCarbDays = 3, proteinGPerKg = 2.0,
        days = (0 until 7).map { day(it) }
    )

    private fun entry(
        epochDay: Long, kcal: Double, protein: Double, carbs: Double, fat: Double,
        slot: MealSlot = MealSlot.LUNCH, source: DiarySource = DiarySource.PLAN,
        active: Boolean = true, planEntryId: Long? = 1L
    ) = DiaryEntry(
        dateEpochDay = epochDay, mealSlot = slot, foodItemId = 1, foodName = "Test",
        grams = 100.0, kcal = kcal, protein = protein, carbs = carbs, fat = fat,
        source = source, planEntryId = planEntryId, active = active
    )

    @Test
    fun `balance shows objective consumed and remaining`() {
        val entries = listOf(entry(0L, 550.0, 40.0, 60.0, 15.0))
        val balance = NutritionDiaryEngine.dailyBalance(plan, 0, 0L, entries)
        assertThat(balance.targetKcal).isEqualTo(2200.0)
        assertThat(balance.consumedKcal).isEqualTo(550.0)
        assertThat(balance.remainingKcal).isEqualTo(1650.0)
        assertThat(balance.kcalExceeded).isEqualTo(0.0)
    }

    @Test
    fun `inactive entries are not counted (no double counting on deselect)`() {
        val entries = listOf(
            entry(0L, 500.0, 40.0, 50.0, 10.0, active = true),
            entry(0L, 300.0, 20.0, 30.0, 5.0, active = false)
        )
        val balance = NutritionDiaryEngine.dailyBalance(plan, 0, 0L, entries)
        assertThat(balance.consumedKcal).isEqualTo(500.0)
    }

    @Test
    fun `exceeding the target is shown as exceeded not as negative remaining`() {
        val entries = listOf(entry(0L, 2500.0, 200.0, 300.0, 80.0))
        val balance = NutritionDiaryEngine.dailyBalance(plan, 0, 0L, entries)
        assertThat(balance.kcalExceeded).isEqualTo(300.0)
        assertThat(balance.kcalWithinTarget).isFalse()
    }

    @Test
    fun `extra food is recorded faithfully and summed`() {
        val entries = listOf(
            entry(0L, 500.0, 40.0, 50.0, 10.0, source = DiarySource.PLAN),
            entry(0L, 250.0, 5.0, 30.0, 8.0, slot = MealSlot.EVENING_SNACK,
                source = DiarySource.EXTRA, planEntryId = null)
        )
        val balance = NutritionDiaryEngine.dailyBalance(plan, 0, 0L, entries)
        assertThat(balance.consumedKcal).isEqualTo(750.0)
    }

    @Test
    fun `entries of another day are not transferred`() {
        val entries = listOf(entry(0L, 500.0, 40.0, 50.0, 10.0), entry(1L, 999.0, 0.0, 0.0, 0.0))
        val balance = NutritionDiaryEngine.dailyBalance(plan, 0, 0L, entries)
        assertThat(balance.consumedKcal).isEqualTo(500.0)
    }

    @Test
    fun `weekly summary uses real consumption`() {
        val entries = (0 until 7).map { d -> entry(d.toLong(), 2000.0, 150.0, 200.0, 60.0) }
        val summary = NutritionDiaryEngine.weeklySummary(plan, entries, weekStartEpochDay = 0L)
        assertThat(summary.daysTracked).isEqualTo(7)
        assertThat(summary.avgKcal).isWithin(0.01).of(2000.0)
        assertThat(summary.daysOnTarget).isEqualTo(7)
    }

    @Test
    fun `target coherence check detects inconsistent macros`() {
        val coherent = com.fitcoach.app.domain.model.MacroTotals(2000.0, 150.0, 200.0, 60.0)
        // 150*4 + 200*4 + 60*9 = 600 + 800 + 540 = 1940 ~ 2000 (entro 5%).
        assertThat(NutritionDiaryEngine.targetIsCoherent(coherent)).isTrue()
        val incoherent = com.fitcoach.app.domain.model.MacroTotals(2000.0, 10.0, 10.0, 10.0)
        assertThat(NutritionDiaryEngine.targetIsCoherent(incoherent)).isFalse()
    }
}
