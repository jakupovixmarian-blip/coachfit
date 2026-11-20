package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.TrainingExperience

/**
 * Volume settimanale di riferimento per gruppo muscolare.
 *
 * Perche esiste: i limiti di volume erano tre numeri scollegati (24 serie per
 * seduta nella diagnostica, 10-20 nel tool di analisi, 8/22 per gli avvisi).
 * Nessuno citava una fonte, e nessuno cambiava con l'esperienza dell'utente:
 * un principiante e un avanzato venivano giudicati con la stessa soglia.
 *
 * I valori qui sotto sono il consenso sulla dose di allenamento per ipertrofia
 * (Schoenfeld, Ogborn & Krieger 2016; Baz-Valle et al. 2022 sulla relazione
 * dose-risposta; Israetel et al. per il modello MEV/MAV/MRV). Sono un intervallo
 * di riferimento, non una prescrizione individuale: l'app li usa per avvisare,
 * non per decidere al posto del coach.
 */
object VolumeGuidelines {

    /**
     * Intervallo consigliato di serie settimanali per gruppo.
     *
     * @param min soglia sotto la quale il gruppo e' sotto-allenato
     * @param max soglia oltre la quale il volume non aggiunge stimolo utile
     * @param mrv massimo recuperabile: oltre, l'affaticamento supera il beneficio
     */
    data class Range(val min: Int, val max: Int, val mrv: Int) {
        init {
            require(min <= max && max <= mrv) { "intervallo di volume incoerente: $min/$max/$mrv" }
        }
    }

    /**
     * Serie settimanali per gruppo, modulate sull'esperienza.
     *
     * I gruppi grandi tollerano piu' volume dei piccoli: applicare la stessa
     * soglia a schiena e bicipiti e' un errore comune che porta a sedute
     * squilibrate.
     */
    fun forGroup(group: MuscleGroup, experience: TrainingExperience): Range {
        val base = if (group.isLargeGroup) LARGE else SMALL
        return when (experience) {
            TrainingExperience.BEGINNER -> base.beginner
            TrainingExperience.INTERMEDIATE -> base.intermediate
            TrainingExperience.ADVANCED -> base.advanced
        }
    }

    private data class Band(val beginner: Range, val intermediate: Range, val advanced: Range)

    private val LARGE = Band(
        beginner = Range(min = 8, max = 12, mrv = 16),
        intermediate = Range(min = 10, max = 18, mrv = 22),
        advanced = Range(min = 12, max = 20, mrv = 26)
    )

    private val SMALL = Band(
        beginner = Range(min = 6, max = 10, mrv = 14),
        intermediate = Range(min = 8, max = 14, mrv = 18),
        advanced = Range(min = 10, max = 16, mrv = 22)
    )

    /** Serie massime in una singola seduta per un esercizio, per categoria. */
    fun maxSetsPerExercise(category: ExerciseCategory): Int = when (category) {
        // I multiarticolari pesano sul sistema nervoso: oltre 6 serie la qualita'
        // del lavoro crolla e il recupero si allunga senza guadagno.
        ExerciseCategory.COMPOUND -> 6
        ExerciseCategory.ISOLATION -> 5
        // Il corpo libero si limita da solo con l'affaticamento locale.
        ExerciseCategory.BODYWEIGHT -> 5
    }

    /** Serie minime perche' una seduta abbia senso. */
    const val MIN_SETS_PER_SESSION = 6

    /** Serie massime in una seduta, somma di tutti gli esercizi. */
    fun maxSetsPerSession(exercises: List<Pair<ExerciseCategory, Int>>): Int =
        exercises.sumOf { (category, _) -> maxSetsPerExercise(category) }

    /**
     * Somma dei massimi per esercizio: rappresenta il tetto di una seduta
     * composta dagli esercizi realmente scelti, non un numero universale.
     */
    fun sessionCeiling(categories: List<ExerciseCategory>): Int =
        categories.sumOf { maxSetsPerExercise(it) }
}
