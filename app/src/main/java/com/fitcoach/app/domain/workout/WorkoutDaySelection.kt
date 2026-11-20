package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.model.WorkoutPlan
import java.time.LocalDate

/**
 * Scelta del giorno mostrato nella schermata Allenamento.
 *
 * Il giorno di calendario non basta: se oggi e riposo la schermata resterebbe
 * senza esercizi e sembrerebbe vuota anche con una scheda valida. La selezione
 * parte quindi da oggi, ma se oggi non si allena ripiega sul prossimo giorno di
 * allenamento della settimana (e, se non ce ne sono dopo, sul precedente).
 */
object WorkoutDaySelection {

    /**
     * Indice 0..6 del giorno da mostrare.
     *
     * @param requested indice scelto dall'utente, oppure null/-1 per "oggi".
     * @param todayIndex giorno corrente 0=Lun .. 6=Dom.
     */
    fun resolve(plan: WorkoutPlan?, requested: Int?, todayIndex: Int): Int {
        requested?.takeIf { it in 0..6 }?.let { return it }
        val today = todayIndex.coerceIn(0, 6)
        if (plan == null) return today
        val trainingDays = plan.days
            .filter { !it.isRestDay && it.exercises.isNotEmpty() }
            .map { it.dayIndex }
            .distinct()
            .sorted()
        if (trainingDays.isEmpty()) return today
        if (today in trainingDays) return today
        return trainingDays.firstOrNull { it > today } ?: trainingDays.last()
    }

    /** Data (epochDay) del giorno [dayIndex] nella settimana corrente, lunedi = 0. */
    fun epochDayFor(dayIndex: Int, today: LocalDate = LocalDate.now()): Long {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return monday.plusDays(dayIndex.coerceIn(0, 6).toLong()).toEpochDay()
    }

    /** Giorno da mostrare, con il fallback applicato da [resolve]. */
    fun dayToShow(plan: WorkoutPlan?, dayIndex: Int): WorkoutDay? =
        plan?.days?.firstOrNull { it.dayIndex == dayIndex && !it.isRestDay }
}
