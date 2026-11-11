package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.WeightRecord
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightAnalyzerTest {

    private fun record(day: Long, kg: Double) = WeightRecord(dateEpochDay = day, weightKg = kg)

    @Test
    fun `weekly averages groups by week`() {
        val records = listOf(
            record(0, 80.0), record(2, 80.5), record(4, 79.5), // week 0 avg = 80.0
            record(7, 79.0), record(9, 79.5)                    // week 1 avg = 79.25
        )
        val stats = WeightAnalyzer.weeklyAverages(records)
        assertThat(stats).hasSize(2)
        val week1 = stats.first { it.weekStartEpochDay == 0L }
        assertThat(week1.avgWeightKg!!).isWithin(0.01).of(80.0)
        assertThat(week1.samples).isEqualTo(3)
    }

    @Test
    fun `trend detects weight loss`() {
        // 4 settimane, -0.5 kg/settimana
        val records = mutableListOf<WeightRecord>()
        var kg = 82.0
        for (w in 0 until 4) {
            records += record(w * 7L, kg)
            kg -= 0.5
        }
        val trend = WeightAnalyzer.trend(records)
        assertThat(trend.isReliable).isTrue()
        assertThat(trend.trendKgPerWeek!!).isWithin(0.05).of(-0.5)
    }

    @Test
    fun `trend detects weight gain`() {
        val records = (0 until 4).map { record(it * 7L, 70.0 + it * 0.3) }
        val trend = WeightAnalyzer.trend(records)
        assertThat(trend.trendKgPerWeek!!).isWithin(0.05).of(0.3)
    }

    @Test
    fun `trend is not reliable with two weeks`() {
        val records = listOf(record(0, 80.0), record(7, 79.5))
        val trend = WeightAnalyzer.trend(records)
        assertThat(trend.isReliable).isFalse()
        assertThat(trend.weeksOfData).isEqualTo(2)
    }

    @Test
    fun `insufficient data when less than two weeks`() {
        val trend = WeightAnalyzer.trend(listOf(record(0, 80.0)))
        assertThat(trend.trendKgPerWeek).isNull()
    }

    @Test
    fun `stalled weight with cut goal is flagged`() {
        val records = (0 until 4).map { record(it * 7L, 80.0) }
        val trend = WeightAnalyzer.trend(records)
        val interp = WeightAnalyzer.interpret(trend, -0.5)
        assertThat(interp.category).isEqualTo(WeightAnalyzer.TrendCategory.STALLED)
    }

    @Test
    fun `faster than expected loss is flagged too fast`() {
        val records = (0 until 4).map { record(it * 7L, 80.0 - it * 1.0) }
        val trend = WeightAnalyzer.trend(records)
        val interp = WeightAnalyzer.interpret(trend, -0.5)
        assertThat(interp.category).isEqualTo(WeightAnalyzer.TrendCategory.TOO_FAST)
    }

    @Test
    fun `on track when close to target`() {
        val records = (0 until 4).map { record(it * 7L, 80.0 - it * 0.5) }
        val trend = WeightAnalyzer.trend(records)
        val interp = WeightAnalyzer.interpret(trend, -0.5)
        assertThat(interp.category).isEqualTo(WeightAnalyzer.TrendCategory.ON_TRACK)
    }

    @Test
    fun `daily fluctuations do not change trend`() {
        // Peso stabile con oscillazioni giornaliere forti; ogni settimana ha
        // la stessa media, quindi la tendenza deve essere piatta.
        val records = listOf(
            record(0, 80.0), record(1, 81.5), record(2, 78.5), record(3, 80.5),
            record(7, 80.0), record(8, 81.5), record(9, 78.5), record(10, 80.5),
            record(14, 80.0), record(15, 81.5), record(16, 78.5), record(17, 80.5)
        )
        val trend = WeightAnalyzer.trend(records)
        assertThat(trend.trendKgPerWeek!!).isWithin(0.05).of(0.0)
    }
}
