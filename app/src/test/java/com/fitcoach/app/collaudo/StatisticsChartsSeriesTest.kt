package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.adaptive.StatRange
import com.fitcoach.app.domain.adaptive.StatisticsEngine
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.DiarySource
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.MealSlot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Le serie alimentano i grafici della schermata Progressi. Un errore qui non
 * rompe il calcolo delle medie, ma mostra all'utente un grafico che dice il
 * falso: per questo vanno verificate separatamente.
 */
class StatisticsChartsSeriesTest {

    private val today = 20_000L

    private fun diary(day: Long, protein: Double, carbs: Double, fat: Double) = DiaryEntry(
        dateEpochDay = day,
        mealSlot = MealSlot.LUNCH,
        foodItemId = 1L,
        foodName = "Alimento",
        grams = 100.0,
        kcal = protein * 4 + carbs * 4 + fat * 9,
        protein = protein,
        carbs = carbs,
        fat = fat,
        source = DiarySource.EXTRA
    )

    private fun perf(
        day: Long,
        name: String,
        reps: Int,
        e1rm: Double?
    ) = ExercisePerformance(
        exerciseId = name.hashCode().toLong(),
        exerciseName = name,
        sessionId = day,
        dateEpochDay = day,
        topWeightKg = 100.0,
        topReps = reps,
        totalSets = 3,
        totalReps = reps * 3,
        volumeKg = 100.0 * reps * 3,
        estimated1Rm = e1rm
    )

    private fun compute(
        diary: List<DiaryEntry> = emptyList(),
        performances: List<ExercisePerformance> = emptyList()
    ) = StatisticsEngine.compute(
        range = StatRange.WEEK,
        todayEpochDay = today,
        weights = emptyList(),
        diary = diary,
        sessions = emptyList(),
        performances = performances,
        targetKcalFor = { 0.0 }
    )

    @Test
    fun `le serie dei macro riportano i grammi realmente registrati`() {
        val stats = compute(
            diary = listOf(
                diary(today, protein = 150.0, carbs = 200.0, fat = 60.0),
                diary(today - 1, protein = 140.0, carbs = 180.0, fat = 70.0)
            )
        )

        assertThat(stats.proteinSeries.map { it.value }).containsExactly(140.0, 150.0).inOrder()
        assertThat(stats.carbsSeries.map { it.value }).containsExactly(180.0, 200.0).inOrder()
        assertThat(stats.fatSeries.map { it.value }).containsExactly(70.0, 60.0).inOrder()
    }

    @Test
    fun `piu pasti nello stesso giorno si sommano in un solo punto`() {
        val stats = compute(
            diary = listOf(
                diary(today, protein = 40.0, carbs = 50.0, fat = 10.0),
                diary(today, protein = 35.0, carbs = 45.0, fat = 15.0)
            )
        )

        assertThat(stats.proteinSeries).hasSize(1)
        assertThat(stats.proteinSeries.single().value).isWithin(0.001).of(75.0)
        assertThat(stats.carbsSeries.single().value).isWithin(0.001).of(95.0)
        assertThat(stats.fatSeries.single().value).isWithin(0.001).of(25.0)
    }

    @Test
    fun `le ripetizioni per seduta sommano gli esercizi del giorno`() {
        val stats = compute(
            performances = listOf(
                perf(today, "Panca", reps = 8, e1rm = 100.0),
                perf(today, "Squat", reps = 5, e1rm = 120.0)
            )
        )

        assertThat(stats.repsSeries).hasSize(1)
        assertThat(stats.repsSeries.single().value).isWithin(0.001).of(39.0)
    }

    @Test
    fun `la forza prende il massimo del giorno e non la somma`() {
        val stats = compute(
            performances = listOf(
                perf(today, "Panca", reps = 8, e1rm = 100.0),
                perf(today, "Squat", reps = 5, e1rm = 120.0)
            )
        )

        assertThat(stats.strengthSeries).hasSize(1)
        assertThat(stats.strengthSeries.single().value).isWithin(0.001).of(120.0)
    }

    @Test
    fun `le prestazioni senza 1RM stimato non entrano nel grafico forza`() {
        val stats = compute(
            performances = listOf(
                perf(today, "Plank", reps = 1, e1rm = null),
                perf(today - 1, "Panca", reps = 8, e1rm = 95.0)
            )
        )

        assertThat(stats.strengthSeries).hasSize(1)
        assertThat(stats.strengthSeries.single().value).isWithin(0.001).of(95.0)
    }

    @Test
    fun `le serie sono ordinate per giorno crescente`() {
        val stats = compute(
            diary = listOf(
                diary(today, protein = 100.0, carbs = 100.0, fat = 30.0),
                diary(today - 3, protein = 90.0, carbs = 90.0, fat = 30.0),
                diary(today - 1, protein = 95.0, carbs = 95.0, fat = 30.0)
            )
        )

        val days = stats.proteinSeries.map { it.label }
        assertThat(days).isEqualTo(stats.proteinSeries.sortedBy { it.label }.map { it.label })
        assertThat(stats.proteinSeries.first().value).isWithin(0.001).of(90.0)
        assertThat(stats.proteinSeries.last().value).isWithin(0.001).of(100.0)
    }

    @Test
    fun `senza dati le serie restano vuote e i grafici non vengono disegnati`() {
        val stats = compute()

        assertThat(stats.proteinSeries).isEmpty()
        assertThat(stats.carbsSeries).isEmpty()
        assertThat(stats.fatSeries).isEmpty()
        assertThat(stats.repsSeries).isEmpty()
        assertThat(stats.strengthSeries).isEmpty()
    }
}
