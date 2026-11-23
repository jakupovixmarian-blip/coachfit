package com.fitcoach.app.audit

import com.fitcoach.app.domain.adaptive.StatRange
import com.fitcoach.app.domain.adaptive.StatisticsEngine
import com.fitcoach.app.domain.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La media settimanale del peso guida la decisione "il peso e' fermo?" e quindi
 * la modifica delle calorie. Se due pesate dello stesso giorno contano doppio,
 * la media si sposta e il coach prende la decisione sbagliata.
 */
class StatisticsWeightAverageTest {

    private fun stats(pesate: List<WeightRecord>, oggi: Long = 100L) =
        StatisticsEngine.compute(
            range = StatRange.WEEK,
            todayEpochDay = oggi,
            weights = pesate,
            diary = emptyList(),
            sessions = emptyList(),
            performances = emptyList(),
            targetKcalFor = { 2_000.0 }
        )

    private fun peso(id: Long, giorno: Long, kg: Double) =
        WeightRecord(id = id, dateEpochDay = giorno, weightKg = kg)

    @Test
    fun `due pesate nello stesso giorno contano come una`() {
        // Giorno 100: 80.0 poi 82.0 (vale l'ultima). Giorno 99: 84.0.
        // Media per giorno: (82 + 84) / 2 = 83.0
        // Media ingenua su 3 record: (84 + 80 + 82) / 3 = 82.0
        // Se il giorno con due pesate contasse doppio, la media scenderebbe a 82.0.
        val r = stats(listOf(peso(1, 99, 84.0), peso(2, 100, 80.0), peso(3, 100, 82.0)))
        assertEquals(
            "Il giorno con due pesate non deve pesare il doppio nella media",
            83.0, r.avgWeightKg!!, 0.001
        )
    }

    @Test
    fun `vince l'ultima pesata del giorno`() {
        // L'utente si pesa, poi corregge. Deve contare la correzione, non la prima.
        val r = stats(listOf(peso(1, 100, 99.0), peso(2, 100, 80.0)))
        assertEquals(80.0, r.avgWeightKg!!, 0.001)
    }

    @Test
    fun `i campioni contati sono i giorni, non i record`() {
        val r = stats(listOf(peso(1, 99, 84.0), peso(2, 100, 80.0), peso(3, 100, 82.0)))
        assertEquals(
            "weightSamples alimenta la UI: deve dire quanti giorni, non quante righe",
            2, r.weightSamples
        )
    }

    @Test
    fun `una pesata al giorno resta invariata`() {
        val r = stats(listOf(peso(1, 98, 83.0), peso(2, 99, 82.0), peso(3, 100, 81.0)))
        assertEquals(82.0, r.avgWeightKg!!, 0.001)
        assertEquals(3, r.weightSamples)
    }

    @Test
    fun `senza pesate la media e' null e non zero`() {
        val r = stats(emptyList())
        assertEquals(null, r.avgWeightKg)
        assertEquals(0, r.weightSamples)
    }

    @Test
    fun `il delta usa il primo e l'ultimo giorno, non il primo e l'ultimo record`() {
        // Giorno 98: 90. Giorno 100: due pesate (85 e 80). Delta = 80 - 90 = -10
        val r = stats(
            listOf(peso(1, 98, 90.0), peso(2, 100, 85.0), peso(3, 100, 80.0))
        )
        assertEquals(-10.0, r.weightDeltaKg!!, 0.001)
    }

    @Test
    fun `la serie giornaliera ha un punto al giorno`() {
        val r = stats(listOf(peso(1, 99, 84.0), peso(2, 100, 80.0), peso(3, 100, 82.0)))
        assertEquals(
            "Il grafico deve avere un punto per giorno, non due sullo stesso",
            2, r.weightSeries.size
        )
        assertEquals(r.weightSeries.map { it.epochDay }.distinct().size, r.weightSeries.size)
    }

    @Test
    fun `le pesate fuori intervallo non entrano nella media`() {
        // StatRange.WEEK = 7 giorni: il giorno 50 e' fuori.
        val r = stats(listOf(peso(1, 50, 120.0), peso(2, 100, 80.0)))
        assertEquals(80.0, r.avgWeightKg!!, 0.001)
        assertEquals(1, r.weightSamples)
    }
}
