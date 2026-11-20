package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.model.WorkoutPlan
import com.fitcoach.app.domain.workout.WorkoutDaySelection
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * Collaudo della scelta del giorno nella schermata Allenamento.
 *
 * Il difetto segnalato ("workout non si vede") aveva questa causa: aprendo la
 * sezione in un giorno di riposo, la schermata mostrava solo "Nessuna seduta
 * programmata" e nessun esercizio, pur con una scheda valida.
 */
class WorkoutDaySelectionCollaudoTest {

    private fun exercise(id: Long, name: String) = PlannedExercise(
        exerciseId = id, exerciseName = name, primaryMuscle = MuscleGroup.CHEST,
        orderIndex = 0, sets = 4, minReps = 8, maxReps = 10, restSeconds = 120, targetRir = 2
    )

    /** Scheda con sedute lunedi (0), mercoledi (2) e venerdi (4). */
    private fun plan(): WorkoutPlan = WorkoutPlan(
        id = 1L,
        name = "Scheda Push Pull Legs",
        splitType = SplitType.PUSH_PULL_LEGS,
        days = (0..6).map { index ->
            if (index in setOf(0, 2, 4)) {
                WorkoutDay(
                    id = (index + 1).toLong(),
                    dayIndex = index,
                    title = "Allenamento",
                    muscleGroups = setOf(MuscleGroup.CHEST),
                    exercises = listOf(exercise(index + 10L, "Panca piana"))
                )
            } else {
                WorkoutDay(dayIndex = index, title = "Riposo", isRestDay = true)
            }
        }
    )

    @Test
    fun `in un giorno di allenamento si mostra il giorno corrente`() {
        assertThat(WorkoutDaySelection.resolve(plan(), null, 2)).isEqualTo(2)
    }

    @Test
    fun `in un giorno di riposo si mostra il prossimo giorno di allenamento`() {
        // Martedi (1) e riposo: senza fallback la schermata restava vuota.
        assertThat(WorkoutDaySelection.resolve(plan(), null, 1)).isEqualTo(2)
        // Sabato (5) e riposo: il prossimo e lunedi? No, dopo non c'e: si usa l'ultimo.
        assertThat(WorkoutDaySelection.resolve(plan(), null, 5)).isEqualTo(4)
    }

    @Test
    fun `una scelta esplicita dell'utente non viene sovrascritta`() {
        assertThat(WorkoutDaySelection.resolve(plan(), 4, 1)).isEqualTo(4)
        assertThat(WorkoutDaySelection.resolve(plan(), 0, 3)).isEqualTo(0)
    }

    @Test
    fun `un indice fuori range ricade sul fallback`() {
        assertThat(WorkoutDaySelection.resolve(plan(), 9, 2)).isEqualTo(2)
        assertThat(WorkoutDaySelection.resolve(plan(), -1, 1)).isEqualTo(2)
    }

    @Test
    fun `senza scheda si resta sul giorno corrente`() {
        assertThat(WorkoutDaySelection.resolve(null, null, 3)).isEqualTo(3)
    }

    @Test
    fun `una scheda con soli giorni di riposo non rompe la selezione`() {
        val empty = WorkoutPlan(
            name = "Vuota", splitType = SplitType.FULL_BODY,
            days = (0..6).map { WorkoutDay(dayIndex = it, title = "Riposo", isRestDay = true) }
        )
        assertThat(WorkoutDaySelection.resolve(empty, null, 3)).isEqualTo(3)
    }

    @Test
    fun `il giorno scelto espone gli esercizi da mostrare`() {
        val day = WorkoutDaySelection.dayToShow(plan(), 2)
        assertThat(day).isNotNull()
        assertThat(day!!.exercises).hasSize(1)
        assertThat(WorkoutDaySelection.dayToShow(plan(), 1)).isNull()
    }

    @Test
    fun `la data del giorno segue la settimana che inizia di lunedi`() {
        // Mercoledi 11 novembre 2026: lunedi e il 9.
        val wednesday = LocalDate.of(2026, 11, 11)
        assertThat(WorkoutDaySelection.epochDayFor(0, wednesday))
            .isEqualTo(LocalDate.of(2026, 11, 9).toEpochDay())
        assertThat(WorkoutDaySelection.epochDayFor(2, wednesday))
            .isEqualTo(wednesday.toEpochDay())
        assertThat(WorkoutDaySelection.epochDayFor(6, wednesday))
            .isEqualTo(LocalDate.of(2026, 11, 15).toEpochDay())
    }
}
