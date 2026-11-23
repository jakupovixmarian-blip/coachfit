package com.fitcoach.app.audit

import com.fitcoach.app.domain.adaptive.ExerciseTrendCalculator
import com.fitcoach.app.domain.adaptive.ExerciseTrendClass
import com.fitcoach.app.domain.adaptive.RangeStatistics
import com.fitcoach.app.domain.adaptive.StatRange
import com.fitcoach.app.domain.adaptive.StatisticsEngine
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WorkoutSession
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.domain.workout.EffectiveVolumeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Questi tre motori non avevano test diretti: erano raggiunti solo attraverso
 * i ViewModel, quindi un errore di calcolo non sarebbe stato localizzato.
 * I casi sotto sono quelli che rompono una statistica scritta in fretta:
 * liste vuote, un solo campione, divisioni per zero, dati fuori intervallo.
 */
class AdaptiveEnginesAdversarialTest {

    private val oggi = 20_000L

    private fun peso(day: Long, kg: Double) = WeightRecord(dateEpochDay = day, weightKg = kg)

    private fun pasto(day: Long, kcal: Double, attivo: Boolean = true) = DiaryEntry(
        dateEpochDay = day, mealSlot = MealSlot.LUNCH, foodItemId = 1, foodName = "Riso",
        grams = 100.0, kcal = kcal, protein = 10.0, carbs = 20.0, fat = 5.0, active = attivo
    )

    /**
     * Il volume non e' un campo: e' la somma di carico x ripetizioni delle
     * serie completate e non di riscaldamento. Costruire le serie vere rende
     * il test fedele a come il volume nasce nell'app.
     */
    private fun seduta(
        day: Long,
        volume: Double,
        stato: SessionStatus = SessionStatus.COMPLETED
    ): WorkoutSession {
        val serie = if (volume <= 0.0) emptyList() else listOf(
            WorkoutSet(
                exerciseId = 1, exerciseName = "Squat", setIndex = 0,
                plannedReps = 10, actualReps = 10, weightKg = volume / 10.0, completed = true
            )
        )
        return WorkoutSession(
            dayTitle = "Gambe", dateEpochDay = day, status = stato, sets = serie
        )
    }

    @Test
    fun `il volume di una seduta ignora riscaldamento e serie non completate`() {
        val s = stats(
            sessions = listOf(
                WorkoutSession(
                    dayTitle = "Gambe", dateEpochDay = oggi, status = SessionStatus.COMPLETED,
                    sets = listOf(
                        WorkoutSet(
                            exerciseId = 1, exerciseName = "Squat", setIndex = 0,
                            plannedReps = 10, actualReps = 10, weightKg = 100.0,
                            completed = true, isWarmup = true
                        ),
                        WorkoutSet(
                            exerciseId = 1, exerciseName = "Squat", setIndex = 1,
                            plannedReps = 10, actualReps = 10, weightKg = 100.0, completed = true
                        ),
                        WorkoutSet(
                            exerciseId = 1, exerciseName = "Squat", setIndex = 2,
                            plannedReps = 10, actualReps = 0, weightKg = 100.0, completed = false
                        )
                    )
                )
            )
        )
        assertEquals(
            "Solo la serie valida conta: 100 kg x 10 reps",
            1000.0, s.totalVolumeKg, 0.001
        )
    }

    private fun stats(
        weights: List<WeightRecord> = emptyList(),
        diary: List<DiaryEntry> = emptyList(),
        sessions: List<WorkoutSession> = emptyList(),
        performances: List<ExercisePerformance> = emptyList(),
        range: StatRange = StatRange.WEEK,
        target: (Long) -> Double = { 2000.0 }
    ): RangeStatistics = StatisticsEngine.compute(
        range = range, todayEpochDay = oggi, weights = weights, diary = diary,
        sessions = sessions, performances = performances, targetKcalFor = target
    )

    // ---------- StatisticsEngine ----------

    @Test
    fun `senza dati le statistiche sono nulle e non zero`() {
        val s = stats()
        assertNull("La media peso non esiste senza pesate", s.avgWeightKg)
        assertNull("La media calorie non esiste senza diario", s.avgKcal)
        assertNull("L'aderenza non esiste senza target e diario", s.avgAdherencePercent)
        assertNull("La variazione di peso richiede due pesate", s.weightDeltaKg)
        assertNull("Il volume medio non esiste senza sedute", s.avgVolumePerSessionKg)
        assertNull("Non c'e' un miglior sollevamento", s.bestLift)
        assertEquals(0, s.weightSamples)
        assertEquals(0, s.daysTracked)
        assertEquals(0, s.sessionsCompleted)
        assertEquals(0.0, s.totalVolumeKg, 0.0)
        assertTrue("Le serie devono essere vuote, non con un punto a zero",
            s.caloriesSeries.isEmpty() && s.volumeSeries.isEmpty() && s.weightSeries.isEmpty())
    }

    @Test
    fun `una sola pesata non produce una variazione di peso`() {
        val s = stats(weights = listOf(peso(oggi, 80.0)))
        assertEquals(1, s.weightSamples)
        assertEquals(80.0, s.avgWeightKg!!, 0.001)
        assertNull("Con un solo campione la variazione non e' calcolabile", s.weightDeltaKg)
    }

    @Test
    fun `i dati fuori intervallo vengono esclusi`() {
        val s = stats(
            weights = listOf(peso(oggi - 30, 95.0), peso(oggi, 80.0)),
            range = StatRange.WEEK
        )
        assertEquals("Solo la pesata dentro la settimana conta", 1, s.weightSamples)
        assertEquals(80.0, s.avgWeightKg!!, 0.001)
    }

    @Test
    fun `il confine dell'intervallo e' incluso`() {
        val inizio = oggi - (StatRange.WEEK.days - 1)
        val s = stats(weights = listOf(peso(inizio, 80.0), peso(oggi, 79.0)))
        assertEquals("Il primo giorno dell'intervallo deve contare", 2, s.weightSamples)
        assertEquals(-1.0, s.weightDeltaKg!!, 0.001)
    }

    @Test
    fun `il diario disattivato non conta nelle calorie`() {
        val s = stats(diary = listOf(pasto(oggi, 2000.0, attivo = false)))
        assertNull("Un pasto disattivato non e' cibo consumato", s.avgKcal)
        assertEquals(0, s.daysTracked)
    }

    @Test
    fun `un target calorico a zero non produce aderenza infinita`() {
        val s = stats(diary = listOf(pasto(oggi, 2000.0)), target = { 0.0 })
        assertNull("Senza target non si calcola aderenza, non si divide per zero", s.avgAdherencePercent)
        assertNotNull("Le calorie restano misurate", s.avgKcal)
    }

    @Test
    fun `un target negativo non produce aderenza`() {
        val s = stats(diary = listOf(pasto(oggi, 2000.0)), target = { -500.0 })
        assertNull(s.avgAdherencePercent)
    }

    @Test
    fun `l'aderenza e limitata e non esplode con un pasto enorme`() {
        val s = stats(diary = listOf(pasto(oggi, 100_000.0)), target = { 2000.0 })
        assertNotNull(s.avgAdherencePercent)
        assertTrue(
            "L'aderenza non deve superare il tetto del 150%: era ${s.avgAdherencePercent}",
            s.avgAdherencePercent!! <= 150.0
        )
    }

    @Test
    fun `le sedute non completate non contano nel volume`() {
        val s = stats(
            sessions = listOf(
                seduta(oggi, 5000.0, SessionStatus.COMPLETED),
                seduta(oggi, 3000.0, SessionStatus.PLANNED)
            )
        )
        assertEquals(1, s.sessionsCompleted)
        assertEquals("Solo la seduta completata porta volume", 5000.0, s.totalVolumeKg, 0.001)
    }

    @Test
    fun `il miglior sollevamento sceglie il 1RM piu alto`() {
        val s = stats(
            performances = listOf(
                ExercisePerformance(
                    exerciseId = 1, exerciseName = "Squat", sessionId = 1, dateEpochDay = oggi,
                    topWeightKg = 100.0, topReps = 5, totalSets = 3, totalReps = 15,
                    volumeKg = 1500.0, estimated1Rm = 116.0
                ),
                ExercisePerformance(
                    exerciseId = 2, exerciseName = "Panca", sessionId = 1, dateEpochDay = oggi,
                    topWeightKg = 80.0, topReps = 10, totalSets = 3, totalReps = 30,
                    volumeKg = 2400.0, estimated1Rm = 106.0
                )
            )
        )
        assertEquals("Squat", s.bestLift!!.exerciseName)
        assertEquals(116.0, s.bestLift.estimated1Rm!!, 0.001)
    }

    @Test
    fun `una performance senza 1RM usa il peso come criterio`() {
        val s = stats(
            performances = listOf(
                ExercisePerformance(
                    exerciseId = 1, exerciseName = "Trazioni", sessionId = 1, dateEpochDay = oggi,
                    topWeightKg = 0.0, topReps = 12, totalSets = 3, totalReps = 36,
                    volumeKg = 0.0, estimated1Rm = null
                ),
                ExercisePerformance(
                    exerciseId = 2, exerciseName = "Curl", sessionId = 1, dateEpochDay = oggi,
                    topWeightKg = 20.0, topReps = 10, totalSets = 3, totalReps = 30,
                    volumeKg = 600.0, estimated1Rm = null
                )
            )
        )
        assertEquals("Curl", s.bestLift!!.exerciseName)
    }

    @Test
    fun `i limiti dell'intervallo sono coerenti con la durata`() {
        StatRange.entries.forEach { range ->
            val s = stats(range = range)
            assertEquals(
                "${range.name}: l'intervallo deve coprire ${range.days} giorni",
                range.days.toLong(), s.toEpochDay - s.fromEpochDay + 1
            )
        }
    }

    // ---------- ExerciseTrendCalculator ----------

    /**
     * Il contratto reale richiede 6 campioni: confronta la media delle 3
     * prestazioni piu' recenti con le 3 precedenti. Sotto quella soglia il
     * motore dichiara INSUFFICIENT_DATA, che e' la risposta corretta: con
     * meno dati non si distingue la progressione dal rumore.
     */
    @Test
    fun `sotto i sei campioni la tendenza e insufficiente, mai stagnazione`() {
        listOf(1, 2, 3, 4, 5).forEach { n ->
            val serie = List(n) { 80.0 }
            val t = ExerciseTrendCalculator.classify("Panca", serie)
            assertEquals(
                "Con $n campioni non si puo' concludere nulla",
                ExerciseTrendClass.INSUFFICIENT_DATA, t.classification
            )
        }
    }

    @Test
    fun `nessun dato non produce una tendenza inventata`() {
        val t = ExerciseTrendCalculator.classify("Panca", emptyList())
        assertEquals(ExerciseTrendClass.INSUFFICIENT_DATA, t.classification)
        assertEquals(0, t.samples)
    }

    @Test
    fun `sei campioni in crescita sono progressione`() {
        // I 3 piu' recenti (85) contro i 3 precedenti (80): +6.25%.
        val serie = listOf(85.0, 85.0, 85.0, 80.0, 80.0, 80.0)
        val t = ExerciseTrendCalculator.classify("Panca", serie)
        assertEquals(ExerciseTrendClass.PROGRESSION, t.classification)
        assertEquals(6.25, t.changePercent, 0.01)
    }

    @Test
    fun `sei campioni in calo sono regressione`() {
        val serie = listOf(75.0, 75.0, 75.0, 80.0, 80.0, 80.0)
        val t = ExerciseTrendCalculator.classify("Panca", serie)
        assertEquals(ExerciseTrendClass.REGRESSION, t.classification)
    }

    @Test
    fun `valori identici sono stagnazione`() {
        val t = ExerciseTrendCalculator.classify("Panca", List(6) { 80.0 })
        assertEquals(ExerciseTrendClass.STAGNATION, t.classification)
        assertEquals(0.0, t.changePercent, 0.0001)
    }

    @Test
    fun `una variazione sotto il cinque per cento e stagnazione`() {
        // +2.5%: dentro la banda di rumore.
        val serie = listOf(82.0, 82.0, 82.0, 80.0, 80.0, 80.0)
        val t = ExerciseTrendCalculator.classify("Panca", serie)
        assertEquals(ExerciseTrendClass.STAGNATION, t.classification)
    }

    @Test
    fun `il confine del cinque per cento e incluso nella progressione`() {
        val serie = listOf(84.0, 84.0, 84.0, 80.0, 80.0, 80.0)
        val t = ExerciseTrendCalculator.classify("Panca", serie)
        assertEquals(ExerciseTrendClass.PROGRESSION, t.classification)
    }

    @Test
    fun `una serie di zeri non divide per zero`() {
        val t = ExerciseTrendCalculator.classify("Trazioni", List(6) { 0.0 })
        assertEquals(
            "Senza carico precedente non si calcola una percentuale",
            ExerciseTrendClass.INSUFFICIENT_DATA, t.classification
        )
        assertTrue("La variazione non deve essere infinita", t.changePercent.isFinite())
    }

    @Test
    fun `il nome dell'esercizio viene riportato nella tendenza`() {
        val t = ExerciseTrendCalculator.classify("Rematore", List(6) { 60.0 })
        assertEquals("Rematore", t.exerciseName)
        assertEquals(6, t.samples)
    }

    // ---------- EffectiveVolumeCalculator ----------

    private fun pianificato(
        exerciseId: Long, muscolo: MuscleGroup, serie: Int, nome: String = "Esercizio"
    ) = PlannedExercise(
        exerciseId = exerciseId, exerciseName = nome, primaryMuscle = muscolo,
        orderIndex = 0, sets = serie, minReps = 8, maxReps = 12, restSeconds = 90, targetRir = 2
    )

    private fun esercizio(
        id: Long, primario: MuscleGroup, secondari: Set<MuscleGroup> = emptySet()
    ) = Exercise(
        id = id, name = "E$id", primaryMuscle = primario, secondaryMuscles = secondari,
        category = ExerciseCategory.COMPOUND, technique = "", defaultSets = 3,
        minReps = 8, maxReps = 12, restSeconds = 90, targetRir = 2
    )

    @Test
    fun `nessun esercizio produce volume zero e nessun gruppo`() {
        val b = EffectiveVolumeCalculator.forExercises(emptyList())
        assertTrue("Senza esercizi non ci sono gruppi", b.effective.isEmpty())
    }

    @Test
    fun `il muscolo principale conta una serie intera`() {
        val b = EffectiveVolumeCalculator.forExercises(
            listOf(pianificato(1, MuscleGroup.CHEST, 4))
        )
        assertEquals(4.0, b.effective[MuscleGroup.CHEST]!!, 0.001)
    }

    @Test
    fun `il primo muscolo secondario vale mezzo e il secondo un quarto`() {
        val esercizi = mapOf(
            1L to esercizio(1, MuscleGroup.CHEST, setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS))
        )
        val b = EffectiveVolumeCalculator.forExercises(
            listOf(pianificato(1, MuscleGroup.CHEST, 4)), esercizi
        )
        assertEquals(4.0, b.effective[MuscleGroup.CHEST]!!, 0.001)
        assertEquals(2.0, b.effective[MuscleGroup.TRICEPS]!!, 0.001)
        assertEquals(1.0, b.effective[MuscleGroup.SHOULDERS]!!, 0.001)
    }

    @Test
    fun `zero serie non aggiunge volume`() {
        val b = EffectiveVolumeCalculator.forExercises(
            listOf(pianificato(1, MuscleGroup.BACK, 0))
        )
        assertEquals(0.0, b.effective[MuscleGroup.BACK] ?: 0.0, 0.001)
    }

    @Test
    fun `esercizi dello stesso gruppo si sommano`() {
        val b = EffectiveVolumeCalculator.forExercises(
            listOf(
                pianificato(1, MuscleGroup.QUADS, 3),
                pianificato(2, MuscleGroup.QUADS, 4)
            )
        )
        assertEquals(7.0, b.effective[MuscleGroup.QUADS]!!, 0.001)
    }

    @Test
    fun `un esercizio sconosciuto nel catalogo non inventa muscoli secondari`() {
        val b = EffectiveVolumeCalculator.forExercises(
            listOf(pianificato(99, MuscleGroup.BICEPS, 3)), emptyMap()
        )
        assertEquals(3.0, b.effective[MuscleGroup.BICEPS]!!, 0.001)
        assertEquals("Senza catalogo resta solo il primario", 1, b.effective.size)
    }
}
