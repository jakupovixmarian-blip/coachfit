package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.WorkoutSession

/**
 * Gestione dell'affaticamento.
 *
 * Analizza piu sessioni per rilevare:
 *  - calo di prestazione (stagnazione/regressione);
 *  - volume settimanale;
 *  - qualita del recupero (sonno, RPE);
 *  - dolore segnalato.
 *
 * IMPORTANTE: non interpreta automaticamente ogni stagnazione come necessita
 * di aumentare il volume. Puo suggerire deload o mantenimento.
 */
object FatigueManager {

    enum class FatigueSignal { NONE, STAGNATION, REGRESSION, OVERREACHING, UNDER_STIMULUS }

    data class FatigueAssessment(
        val signal: FatigueSignal,
        val severity: Int, // 0..3
        val explanation: String,
        val recommendation: String
    )

    /**
     * Valuta l'andamento delle prestazioni su un esercizio.
     * @param performances ordinate dalla piu recente alla piu vecchia
     */
    fun assessPerformance(performances: List<ExercisePerformance>): FatigueAssessment {
        if (performances.size < 3) {
            return FatigueAssessment(
                FatigueSignal.NONE, 0,
                "Dati insufficienti per valutare l'andamento (servono almeno 3 sedute).",
                "Continua a registrare le prestazioni."
            )
        }
        val recent = performances.take(3).map { it.estimated1Rm ?: (it.topWeightKg * (1 + it.topReps / 30.0)) }
        val previous = performances.drop(3).take(3).map { it.estimated1Rm ?: (it.topWeightKg * (1 + it.topReps / 30.0)) }

        if (previous.isEmpty()) {
            return FatigueAssessment(
                FatigueSignal.NONE, 0, "Storico in costruzione.", "Continua a registrare."
            )
        }
        val recentAvg = recent.average()
        val prevAvg = previous.average()
        val delta = (recentAvg - prevAvg) / prevAvg

        return when {
            delta <= -0.05 -> FatigueAssessment(
                FatigueSignal.REGRESSION, 3,
                "Calo di prestazione del ${"%.1f".format(-delta * 100)}% nelle ultime 3 sedute.",
                "Valuta una settimana di deload: riduci il volume del 40% mantenendo l'intensita."
            )
            delta < -0.01 -> FatigueAssessment(
                FatigueSignal.STAGNATION, 2,
                "Prestazione stabile o in leggero calo (${"%.1f".format(delta * 100)}%).",
                "Mantieni il carico, verifica sonno e recupero. Non aumentare il volume automaticamente."
            )
            delta >= 0.05 -> FatigueAssessment(
                FatigueSignal.UNDER_STIMULUS, 0,
                "Progresso solido (+${"%.1f".format(delta * 100)}%).",
                "La progressione funziona: continua con il piano attuale."
            )
            else -> FatigueAssessment(
                FatigueSignal.NONE, 0,
                "Andamento regolare (+${"%.1f".format(delta * 100)}%).",
                "Prosegui con la double progression."
            )
        }
    }

    /**
     * Valuta il recupero complessivo da una serie di sessioni recenti.
     * Rileva segnali di overreaching se sonno basso e RPE alto persistono.
     */
    fun assessRecovery(sessions: List<WorkoutSession>): FatigueAssessment {
        val completed = sessions.filter { it.status == com.fitcoach.app.domain.model.SessionStatus.COMPLETED }
        if (completed.size < 3) {
            return FatigueAssessment(FatigueSignal.NONE, 0, "Dati di recupero insufficienti.", "Continua a registrare.")
        }
        val recent = completed.take(5)
        val avgSleep = recent.mapNotNull { it.sleepQuality }.takeIf { it.isNotEmpty() }?.average()
        val avgRpe = recent.mapNotNull { it.perceivedEffort }.takeIf { it.isNotEmpty() }?.average()
        val painCount = recent.count { it.painReported }

        if (painCount >= 2) {
            return FatigueAssessment(
                FatigueSignal.OVERREACHING, 3,
                "Dolore segnalato in $painCount delle ultime ${recent.size} sedute.",
                "Interrompi gli esercizi dolorosi, riduci il volume e valuta una consulenza professionale."
            )
        }
        if (avgSleep != null && avgSleep <= 2.5 && avgRpe != null && avgRpe >= 8.5) {
            return FatigueAssessment(
                FatigueSignal.OVERREACHING, 3,
                "Sonno basso (${"%.1f".format(avgSleep)}/5) e RPE elevato (${"%.1f".format(avgRpe)}).",
                "Segnali di affaticamento accumulato: pianifica un deload e migliora il recupero."
            )
        }
        if (avgSleep != null && avgSleep <= 3.0) {
            return FatigueAssessment(
                FatigueSignal.STAGNATION, 2,
                "Qualita del sonno migliorabile (${"%.1f".format(avgSleep)}/5).",
                "Priorita a sonno e recupero: non aumentare il volume."
            )
        }
        return FatigueAssessment(FatigueSignal.NONE, 0, "Recupero nella norma.", "Prosegui con il piano.")
    }

    /**
     * Verifica se il volume settimanale e ragionevole rispetto al gruppo.
     * Range raccomandato: 10-20 serie/gruppo/settimana.
     */
    fun assessWeeklyVolume(setsPerGroup: Int): FatigueAssessment = when {
        setsPerGroup < 8 -> FatigueAssessment(
            FatigueSignal.UNDER_STIMULUS, 1,
            "Volume basso ($setsPerGroup serie/settimana).",
            "Se il recupero lo consente, puoi aggiungere 1-2 serie per gruppo."
        )
        setsPerGroup in 8..22 -> FatigueAssessment(
            FatigueSignal.NONE, 0,
            "Volume in range ($setsPerGroup serie/settimana).",
            "Volume adeguato: mantieni."
        )
        else -> FatigueAssessment(
            FatigueSignal.OVERREACHING, 2,
            "Volume elevato ($setsPerGroup serie/settimana).",
            "Volume molto alto: valuta una riduzione per garantire il recupero."
        )
    }
}
