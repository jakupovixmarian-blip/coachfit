package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.coach.AiWorkoutDay
import com.fitcoach.app.domain.knowledge.CorpusCitation
import com.fitcoach.app.domain.knowledge.InvictusKnowledge
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.UserProfile

/**
 * Applica [TrainingPolicy] a una giornata proposta dall'AI.
 *
 * Perche esiste: le regole di esperienza erano solo testo nel prompt, e un prompt
 * e' una richiesta, non un vincolo. Qui diventano violazioni bloccanti, cosi' il
 * modello riceve un errore concreto e puo' correggere.
 *
 * COSA NON CONTROLLA, E PERCHE': non impone un numero di serie diverso dal
 * catalogo, non impone un minimo di ripetizioni e non obbliga le tecniche
 * d'intensita'. Il catalogo e' la prescrizione (contraddirla respingeva schede
 * corrette) e il corpus dichiara le tecniche facoltative: renderle obbligatorie
 * avrebbe inventato un requisito che nessuna fonte sostiene.
 */
object TrainingPolicyChecks {

    /**
     * Violazioni della giornata rispetto al livello dichiarato.
     *
     * @param resolveExercises risolve il nome scritto dal modello nel record del
     *   catalogo; null se il nome non esiste (quello e' un altro controllo).
     */
    fun dayViolations(
        dayIndex: Int,
        day: AiWorkoutDay,
        profile: UserProfile,
        resolveExercises: (String) -> Exercise?
    ): List<String> {
        val out = mutableListOf<String>()
        val experience = profile.experience
        val allowed = TrainingPolicy.allowedTechniques(experience)

        day.exercises.forEach { item ->
            val exercise = resolveExercises(item.exerciseName) ?: return@forEach

            val technique = IntensityTechnique.parse(item.technique)
            if (technique != null && technique != IntensityTechnique.NONE) {
                if (technique !in allowed) {
                    out += "giorno workout $dayIndex: tecnica ${technique.name} non indicata per " +
                        "${experience.name.lowercase()} su '${exercise.name}'. Ammesse: " +
                        allowed.joinToString { it.name } +
                        CorpusCitation.suffix(InvictusKnowledge.Category.LEVEL, "principiante")
                }
                // Una tecnica senza motivazione e' un'etichetta: la scheda mostra la
                // ragione all'utente, quindi il campo non puo' restare vuoto.
                if (item.techniqueReason.isBlank()) {
                    out += "giorno workout $dayIndex: '${exercise.name}' usa ${technique.name} " +
                        "senza motivazione (campo 'techniqueReason')"
                }
            }

            if (TrainingPolicy.isTooDemanding(exercise, experience)) {
                out += "giorno workout $dayIndex: '${exercise.name}' richiede un livello tecnico " +
                    "superiore a quello dichiarato (${experience.name.lowercase()})" +
                    CorpusCitation.suffix(InvictusKnowledge.Category.LEVEL, "principiante")
            }
        }
        return out
    }
}
