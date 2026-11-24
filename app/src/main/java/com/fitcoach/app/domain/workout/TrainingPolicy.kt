package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile

/**
 * Regole di programmazione che dipendono dall'anamnesi.
 *
 * Perche esiste: prima l'esperienza entrava solo negli avvisi di volume, quindi
 * un neofita poteva ricevere la stessa scheda di un avanzato: stesse tecniche,
 * stesso carico tecnico. L'obiettivo non entrava in nessun controllo della
 * scheda. Il prompt lo "chiedeva" al modello, ma un requisito non verificato non
 * e' un requisito.
 *
 * PRINCIPIO: qui non si inventano numeri. Le serie per esercizio restano quelle
 * del catalogo ([VolumeGuidelines]), perche' il catalogo e' la prescrizione e una
 * soglia piu' bassa la contraddirebbe (le Alzate laterali sono prescritte a 4
 * serie: imporne 3 a un neofita significherebbe respingere una scheda corretta).
 * Cio' che cambia con l'esperienza e' cio' che il corpus documenta come diverso:
 * quali tecniche d'intensita' sono sostenibili, e quali esercizi sono troppo
 * esigenti dal punto di vista tecnico.
 */
object TrainingPolicy {

    /**
     * Tecniche d'intensita ammesse per livello.
     *
     * Il corpus (categoria LEVEL) documenta che il principiante lavora sulla
     * tecnica di base; le tecniche che portano al cedimento richiedono un
     * controllo motorio che a quel livello non c'e'. Per intermedio e avanzato
     * restano opzioni: il corpus le dichiara facoltative, non obbligatorie.
     */
    fun allowedTechniques(experience: TrainingExperience): Set<IntensityTechnique> = when (experience) {
        TrainingExperience.BEGINNER -> setOf(IntensityTechnique.NONE)
        TrainingExperience.INTERMEDIATE -> setOf(
            IntensityTechnique.NONE, IntensityTechnique.SUPERSET,
            IntensityTechnique.DROP_SET, IntensityTechnique.BACK_OFF_SET,
            IntensityTechnique.ECCENTRIC_EMPHASIS
        )
        TrainingExperience.ADVANCED -> IntensityTechnique.entries.toSet()
    }

    /**
     * Esercizi troppo esigenti per il livello dichiarato.
     *
     * Usa la difficolta' del catalogo (1..3), che e' un dato e non una stima. Per
     * il neofita restano fuori i movimenti di difficolta' 3: sono quelli in cui un
     * errore tecnico ha il costo piu' alto.
     */
    fun isTooDemanding(exercise: Exercise, experience: TrainingExperience): Boolean =
        when (experience) {
            TrainingExperience.BEGINNER -> exercise.difficulty > 2
            TrainingExperience.INTERMEDIATE -> exercise.difficulty > 3
            TrainingExperience.ADVANCED -> false
        }

    /**
     * Volume settimanale di riferimento per obiettivo, dal corpus.
     *
     * Il corpus (categoria GOAL_TRAIN) documenta 10-25 serie allenanti a
     * settimana per l'ipertrofia e 12-20 per la ricomposizione. E' un intervallo
     * di riferimento, non un cancello: serve a motivare, non a respingere.
     */
    fun volumeTarget(goal: Goal): IntRange = when (goal) {
        Goal.HYPERTROPHY -> 10..25
        Goal.RECOMP -> 12..20
        // In definizione il volume resta alto abbastanza da mantenere la massa;
        // il corpus non fissa un intervallo proprio per il cut, quindi si usa
        // quello dell'ipertrofia come riferimento di mantenimento.
        Goal.CUT -> 10..20
        Goal.MAINTENANCE -> 8..18
        Goal.STRENGTH -> 8..20
    }

    /**
     * Range di ripetizioni piu' comodo per l'ipertrofia secondo il corpus.
     *
     * Il corpus indica 8-20 ripetizioni come range ipertrofico, con 8-12 come
     * fascia storica di riferimento. Serve al prompt e alla diagnosi.
     */
    val hypertrophyRepRange: IntRange = 8..20
}
