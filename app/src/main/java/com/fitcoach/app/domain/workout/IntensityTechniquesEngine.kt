package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile

/**
 * Motore delle tecniche d'intensita.
 *
 * Le tecniche avanzate NON sono obbligatorie per l'ipertrofia e non vengono
 * inserite casualmente. Il motore le applica SOLO quando esiste una
 * giustificazione, valutando: esperienza, esercizio, obiettivo, fatica
 * accumulata, recupero, volume settimanale e rischi tecnici.
 *
 * Ogni decisione include la motivazione (perche usarla) e il motivo per cui NON
 * viene usata altrove.
 */
object IntensityTechniquesEngine {

    data class TechniqueDecision(
        val technique: IntensityTechnique,
        val reason: String,
        /** Spiegazione di perche NON sono state usate tecniche piu avanzate. */
        val avoidedReason: String
    )

    /**
     * @param exercise esercizio candidato
     * @param profile profilo utente
     * @param isLastExerciseOfSession se e l'ultimo esercizio della sessione
     * @param weeklySetsForGroup volume settimanale gia assegnato al gruppo
     * @param fatigueSeverity fatica accumulata 0..3
     * @param isFirstMesocycleWeeks se l'utente e nelle prime settimane del mesociclo
     */
    fun decide(
        exercise: Exercise,
        profile: UserProfile,
        isLastExerciseOfSession: Boolean,
        weeklySetsForGroup: Int,
        fatigueSeverity: Int = 0,
        isFirstMesocycleWeeks: Boolean = true
    ): TechniqueDecision {
        // Sicurezza: nessuna tecnica avanzata per principianti o con recupero scarso.
        if (profile.experience == TrainingExperience.BEGINNER) {
            return TechniqueDecision(
                IntensityTechnique.NONE,
                "Nessuna tecnica avanzata: come principiante, la priorita e la tecnica di esecuzione e la progressione pulita.",
                "Le tecniche d'intensita aggiungono fatica senza benefici aggiuntivi quando la tecnica non e ancora consolidata."
            )
        }
        if (fatigueSeverity >= 3) {
            return TechniqueDecision(
                IntensityTechnique.NONE,
                "Nessuna tecnica avanzata: recupero insufficiente.",
                "Aggiungere intensita con fatica elevata peggiora il recupero e la qualita delle serie."
            )
        }
        if (profile.sleepQuality == Level3.LOW && profile.stressLevel == Level3.HIGH) {
            return TechniqueDecision(
                IntensityTechnique.NONE,
                "Nessuna tecnica avanzata: sonno basso e stress alto riducono la capacita di recupero.",
                "In questa condizione conviene mantenere volume e intensita controllati."
            )
        }

        // Volume gia elevato: evitare tecniche che aggiungono ulteriore fatica.
        if (weeklySetsForGroup >= 20) {
            return TechniqueDecision(
                IntensityTechnique.NONE,
                "Nessuna tecnica avanzata: volume settimanale gia elevato ($weeklySetsForGroup serie).",
                "Con volume alto, aggiungere tecniche d'intensita aumenta il rischio di overreaching."
            )
        }

        val avoid = "Applicabile ma non necessaria ora: la tecnica di base e la doppia progressione " +
            "sono sufficienti; le tecniche avanzate restano facoltative."

        // Prime settimane del mesociclo: solo esercizi di isolamento con back-off leggero.
        if (isFirstMesocycleWeeks) {
            return TechniqueDecision(
                IntensityTechnique.NONE,
                "Nessuna tecnica avanzata nelle prime settimane del mesociclo: si costruisce prima la base di volume.",
                avoid
            )
        }

        // Isolamento come ultimo esercizio: drop set o myo-reps giustificati (bassa richiesta tecnica).
        if (exercise.category == ExerciseCategory.ISOLATION && isLastExerciseOfSession &&
            profile.experience == TrainingExperience.ADVANCED
        ) {
            val tech = if (exercise.isUnilateral) IntensityTechnique.MYO_REPS else IntensityTechnique.DROP_SET
            return TechniqueDecision(
                tech,
                "Tecnica applicata sull'ultimo esercizio di isolamento (${exercise.name}): " +
                    "basso rischio tecnico, fatica localizzata e obiettivo ipertrofico.",
                avoid
            )
        }

        // Multiarticolare: solo back-off set quando il volume e gestibile.
        if (exercise.category == ExerciseCategory.COMPOUND &&
            profile.goal in setOf(Goal.HYPERTROPHY, Goal.STRENGTH) &&
            weeklySetsForGroup <= 16
        ) {
            return TechniqueDecision(
                IntensityTechnique.BACK_OFF_SET,
                "Back-off set su ${exercise.name}: dopo le serie pesanti, una serie piu leggera " +
                    "aggiunge volume di qualita con fatica contenuta.",
                avoid
            )
        }

        return TechniqueDecision(IntensityTechnique.NONE, "Nessuna tecnica d'intensita: non giustificata per questo esercizio.", avoid)
    }

    /** Descrizione estesa di una tecnica (per la UI). */
    fun describe(technique: IntensityTechnique): String = when (technique) {
        IntensityTechnique.NONE -> "Serie tradizionali."
        IntensityTechnique.DROP_SET -> "Drop set: riduci il carico (~20-30%) e prosegui senza pausa dopo il cedimento tecnico."
        IntensityTechnique.REST_PAUSE -> "Rest-pause: 10-15 s di pausa all'interno della serie per recuperare qualche ripetizione."
        IntensityTechnique.MYO_REPS -> "Myo-reps: serie di attivazione a RIR 1-2 seguita da mini-serie con poche respirazioni di pausa."
        IntensityTechnique.SUPERSET -> "Superset: due esercizi in sequenza senza recupero intermedio."
        IntensityTechnique.BACK_OFF_SET -> "Back-off set: dopo le serie pesanti, una serie con ~10% di carico in meno per volume di qualita."
        IntensityTechnique.CLUSTER_SET -> "Cluster set: suddividi la serie in cluster con brevi pause (10-20 s) mantenendo piu qualita."
        IntensityTechnique.FAILURE -> "Serie a cedimento: esegui fino all'esaurimento tecnico. Usare con parsimonia."
        IntensityTechnique.ECCENTRIC_EMPHASIS -> "Enfasi eccentrica: fase di discesa lenta e controllata (3-4 s)."
    }
}
