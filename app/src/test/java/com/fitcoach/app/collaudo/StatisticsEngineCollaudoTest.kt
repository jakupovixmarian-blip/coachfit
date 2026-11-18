package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.adaptive.StatRange
import com.fitcoach.app.domain.adaptive.StatisticsEngine
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.DiarySource
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WorkoutSession
import com.fitcoach.app.domain.model.WorkoutSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Collaudo del motore statistiche: aggregazioni reali per intervallo. */
class StatisticsEngineCollaudoTest {

    private val today = 20_000L

    private fun diary(day: Long, kcal: Double, protein: Double) = DiaryEntry(
        dateEpochDay = day,
        mealSlot = MealSlot.LUNCH,
        foodItemId = 1L,
        foodName = "Alimento",
        grams = 100.0,
        kcal = kcal,
        protein = protein,
        carbs = 50.0,
        fat = 10.0,
        source = DiarySource.EXTRA
    )

    private fun session(day: Long, weight: Double, reps: Int) = WorkoutSession(
        id = day,
        dayTitle = "Push",
        dateEpochDay = day,
        status = SessionStatus.COMPLETED,
        sets = listOf(
            WorkoutSet(exerciseId = 1L, exerciseName = "Panca", setIndex = 0,
                plannedReps = 8, actualReps = reps, weightKg = weight)
        )
    )

    @Test
    fun `l'intervallo seleziona solo i dati dentro la finestra`() {
        val weights = listOf(
            WeightRecord(dateEpochDay = today, weightKg = 80.0),
            WeightRecord(dateEpochDay = today - 3, weightKg = 81.0),
            WeightRecord(dateEpochDay = today - 40, weightKg = 90.0)
        )
        val week = StatisticsEngine.compute(
            range = StatRange.WEEK, todayEpochDay = today,
            weights = weights, diary = emptyList(), sessions = emptyList(),
            performances = emptyList(), targetKcalFor = { 0.0 }
        )
        assertThat(week.weightSamples).isEqualTo(2)
        assertThat(week.weightDeltaKg).isWithin(0.001).of(-1.0)

        val quarter = StatisticsEngine.compute(
            range = StatRange.QUARTER, todayEpochDay = today,
            weights = weights, diary = emptyList(), sessions = emptyList(),
            performances = emptyList(), targetKcalFor = { 0.0 }
        )
        assertThat(quarter.weightSamples).isEqualTo(3)
    }

    @Test
    fun `calorie e aderenza sono calcolate sui consumi reali del periodo`() {
        val diary = listOf(
            diary(today, 2000.0, 150.0),
            diary(today - 1, 2500.0, 160.0)
        )
        val stats = StatisticsEngine.compute(
            range = StatRange.WEEK, todayEpochDay = today,
            weights = emptyList(), diary = diary, sessions = emptyList(),
            performances = emptyList(), targetKcalFor = { 2500.0 }
        )
        assertThat(stats.daysTracked).isEqualTo(2)
        assertThat(stats.avgKcal).isWithin(0.001).of(2250.0)
        assertThat(stats.avgProtein).isWithin(0.001).of(155.0)
        assertThat(stats.caloriesSeries).hasSize(2)
        // (2000/2500*100 + 2500/2500*100)/2 = 90%
        assertThat(stats.avgAdherencePercent).isWithin(0.001).of(90.0)
    }

    @Test
    fun `volume e miglior prestazione derivano dalle sedute completate`() {
        val sessions = listOf(
            session(today, 100.0, 8),   // 800 kg
            session(today - 2, 110.0, 6) // 660 kg
        )
        val performances = listOf(
            ExercisePerformance(exerciseId = 1L, exerciseName = "Panca", sessionId = 1L,
                dateEpochDay = today, topWeightKg = 110.0, topReps = 6,
                totalSets = 3, totalReps = 18, volumeKg = 660.0, estimated1Rm = 132.0)
        )
        val stats = StatisticsEngine.compute(
            range = StatRange.WEEK, todayEpochDay = today,
            weights = emptyList(), diary = emptyList(), sessions = sessions,
            performances = performances, targetKcalFor = { 0.0 }
        )
        assertThat(stats.sessionsCompleted).isEqualTo(2)
        assertThat(stats.totalVolumeKg).isWithin(0.001).of(1460.0)
        assertThat(stats.bestLift!!.exerciseName).isEqualTo("Panca")
        assertThat(stats.bestLift!!.topWeightKg).isWithin(0.001).of(110.0)
    }

    @Test
    fun `senza dati i campi restano nulli e non inventati`() {
        val stats = StatisticsEngine.compute(
            range = StatRange.MONTH, todayEpochDay = today,
            weights = emptyList(), diary = emptyList(), sessions = emptyList(),
            performances = emptyList(), targetKcalFor = { 2000.0 }
        )
        assertThat(stats.avgKcal).isNull()
        assertThat(stats.avgWeightKg).isNull()
        assertThat(stats.weightDeltaKg).isNull()
        assertThat(stats.avgAdherencePercent).isNull()
        assertThat(stats.bestLift).isNull()
        assertThat(stats.daysTracked).isEqualTo(0)
    }
}
