package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.TrainingExperience

/**
 * Calcola il recupero tra le serie. L'utente NON sceglie il recupero: lo
 * assegna il motore in base a tipo di esercizio, carico, obiettivo, intensita
 * (RIR), fatica accumulata e necessita di mantenere la qualita tecnica.
 *
 * Indicazioni di partenza (non valori rigidi):
 *  - multiarticolari pesanti: 2-4 min
 *  - multiarticolari moderati: 2-3 min
 *  - isolamento: 1-2 min
 *  - tecniche d'intensita: recuperi specifici per metodo
 */
object RestCalculator {

    data class RestDecision(
        val seconds: Int,
        val reason: String
    )

    /**
     * @param category categoria dell'esercizio
     * @param goal obiettivo dell'utente
     * @param targetRir RIR target (piu basso = piu vicino al cedimento)
     * @param loadRatio carico relativo stimato (0..1.5) rispetto al tipico (1.0 = normale)
     * @param experience esperienza dell'utente
     * @param fatigueSeverity severita fatica accumulata 0..3
     * @param technique tecnica d'intensita eventualmente applicata
     */
    fun decide(
        category: ExerciseCategory,
        goal: Goal,
        targetRir: Int,
        loadRatio: Double = 1.0,
        experience: TrainingExperience = TrainingExperience.INTERMEDIATE,
        fatigueSeverity: Int = 0,
        technique: IntensityTechnique = IntensityTechnique.NONE
    ): RestDecision {
        // Base per categoria.
        var base = when (category) {
            ExerciseCategory.COMPOUND -> 150
            ExerciseCategory.ISOLATION -> 90
            ExerciseCategory.BODYWEIGHT -> 90
        }

        val reasons = mutableListOf<String>()
        reasons += when (category) {
            ExerciseCategory.COMPOUND -> "esercizio multiarticolare"
            ExerciseCategory.ISOLATION -> "esercizio di isolamento"
            ExerciseCategory.BODYWEIGHT -> "esercizio a corpo libero"
        }

        // Obiettivo: forza richiede recuperi piu lunghi, ipertrofia medi.
        when (goal) {
            Goal.STRENGTH -> { base += 45; reasons += "obiettivo forza (recuperi lunghi)" }
            Goal.HYPERTROPHY -> base += 0
            Goal.CUT, Goal.RECOMP -> { base -= 15; reasons += "obiettivo definizione/ricomposizione" }
            Goal.MAINTENANCE -> base += 0
        }

        // Intensita: RIR basso (vicino al cedimento) -> piu recupero.
        when {
            targetRir <= 1 -> { base += 30; reasons += "RIR ${targetRir} (serie molto intense)" }
            targetRir >= 3 -> { base -= 15; reasons += "RIR ${targetRir} (margine ampio)" }
        }

        // Carico relativo.
        if (loadRatio >= 1.1) { base += 20; reasons += "carico elevato" }
        else if (loadRatio <= 0.8) { base -= 10; reasons += "carico contenuto" }

        // Esperienza: i principianti recuperano con meno precisione -> recupero un filo piu lungo.
        if (experience == TrainingExperience.BEGINNER) { base += 15; reasons += "principiante" }

        // Fatica accumulata: priorita al recupero.
        if (fatigueSeverity >= 2) { base += 30; reasons += "fatica accumulata (recupero esteso)" }

        // Tecniche d'intensita: recuperi specifici.
        base += techniqueRestDelta(technique).also { if (it != 0) reasons += techniqueReason(technique) }

        // Limiti di sicurezza: 45s .. 300s.
        val clamped = base.coerceIn(45, 300)
        return RestDecision(clamped, reasons.joinToString(", "))
    }

    private fun techniqueRestDelta(technique: IntensityTechnique): Int = when (technique) {
        IntensityTechnique.NONE -> 0
        IntensityTechnique.DROP_SET -> 15
        IntensityTechnique.REST_PAUSE -> 10
        IntensityTechnique.MYO_REPS -> 10
        IntensityTechnique.SUPERSET -> -30 // recupero ridotto: si alterna un altro esercizio
        IntensityTechnique.BACK_OFF_SET -> 15
        IntensityTechnique.CLUSTER_SET -> -15 // recuperi brevi tra i cluster
        IntensityTechnique.FAILURE -> 30
        IntensityTechnique.ECCENTRIC_EMPHASIS -> 20
    }

    private fun techniqueReason(technique: IntensityTechnique): String = when (technique) {
        IntensityTechnique.NONE -> ""
        IntensityTechnique.DROP_SET -> "drop set"
        IntensityTechnique.REST_PAUSE -> "rest-pause"
        IntensityTechnique.MYO_REPS -> "myo-reps"
        IntensityTechnique.SUPERSET -> "superset"
        IntensityTechnique.BACK_OFF_SET -> "back-off set"
        IntensityTechnique.CLUSTER_SET -> "cluster set"
        IntensityTechnique.FAILURE -> "serie a cedimento"
        IntensityTechnique.ECCENTRIC_EMPHASIS -> "enfasi eccentrica"
    }

    fun format(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "%d:%02d".format(m, s) else "${s}s"
    }
}
