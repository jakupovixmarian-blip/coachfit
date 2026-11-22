package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.TechnicalLevel
import com.fitcoach.app.domain.model.UserProfile

/**
 * Decide se un esercizio e eseguibile con l'attrezzatura dichiarata.
 *
 * La regola stava ripetuta in tre punti (builder, validator, diagnostica) e
 * tutti e tre trattavano BODYWEIGHT come un'attrezzatura da possedere: chi non
 * spuntava "Solo corpo libero" nell'anamnesi si vedeva vietare plank e ponte
 * glutei. BODYWEIGHT e l'assenza di attrezzatura, non un oggetto da avere.
 */
object EquipmentFit {

    fun isAvailable(exercise: Exercise, available: Set<Equipment>): Boolean {
        if (available.isEmpty()) return true
        if (exercise.bodyweightOnly) return true
        if (exercise.equipment.isEmpty()) return true
        if (Equipment.BODYWEIGHT in exercise.equipment) return true
        return exercise.equipment.any { it in available }
    }

    fun isAvailable(exercise: Exercise, profile: UserProfile): Boolean =
        isAvailable(exercise, equipmentFor(profile))

    /**
     * Unico punto di verita' su "questo esercizio e prescrivibile a questo utente".
     *
     * La regola era duplicata in [WorkoutPlanValidator] e in `ProposalChecker`,
     * e il prompt di generazione non la applicava affatto: al modello veniva
     * passata l'etichetta "ESERCIZI CONSENTITI" su una lista NON filtrata, cosi'
     * leggeva come leciti anche gli esercizi impossibili per il suo profilo e li
     * prescriveva. Filtrare qui, e usare la stessa funzione per validare, fa
     * coincidere per costruzione cio' che il modello vede con cio' che l'app
     * accetta: non e' piu' una raccomandazione nel prompt ma un vincolo.
     */
    fun isAllowed(exercise: Exercise, profile: UserProfile): Boolean {
        if (!isAvailable(exercise, profile)) return false
        val name = exercise.name.lowercase()
        val muscle = exercise.primaryMuscle.label.lowercase()
        if (profile.movementsToAvoid.any { name.contains(it.lowercase()) || muscle.contains(it.lowercase()) }) {
            return false
        }
        if (profile.dislikedExercises.any { name.contains(it.lowercase()) }) return false
        if (profile.unknownExercises.any { name.contains(it.lowercase()) }) return false
        val maxDifficulty = when (profile.technicalLevel) {
            TechnicalLevel.NOVICE -> 2
            TechnicalLevel.COMPETENT -> 3
            TechnicalLevel.PROFICIENT -> 3
        }
        return exercise.difficulty <= maxDifficulty
    }

    /** Esercizi realmente prescrivibili: e' questa la lista da mostrare al modello. */
    fun allowedFor(profile: UserProfile, exercises: List<Exercise>): List<Exercise> =
        exercises.filter { isAllowed(it, profile) }

    /**
     * Attrezzatura effettiva del profilo: le categorie spuntate PIU' quelle
     * deducibili dall'inventario dettagliato.
     *
     * Serve perche i due campi si compilano in passi diversi dell'onboarding e
     * non erano collegati: chi compilava solo l'inventario (o solo le chip)
     * lasciava l'altro vuoto, e con l'insieme vuoto [isAvailable] permette ogni
     * esercizio. Il risultato era una scheda indipendente dall'attrezzatura
     * dichiarata, senza alcun errore visibile.
     *
     * Se ENTRAMBI sono vuoti l'insieme resta vuoto: significa "non dichiarato",
     * e in quel caso non si inventa un vincolo. A quel punto pero' la scheda e
     * gia' bloccata da [UserProfile.isAnamnesisReady].
     */
    fun equipmentFor(profile: UserProfile): Set<Equipment> {
        val declared = profile.availableEquipment.toMutableSet()
        profile.gearInventory.usable.forEach { item ->
            fromGearName(item.name)?.let { declared += it }
        }
        return declared
    }

    /**
     * Deducce la categoria [Equipment] dal nome dell'attrezzo.
     *
     * Il nome e' libero (l'utente puo' scrivere qualsiasi cosa), quindi il
     * confronto e' per parola chiave e non per uguaglianza: "Panca regolabile"
     * e "Panca inclinabile" devono valere entrambe [Equipment.BENCH].
     * L'ordine dei controlli conta: "Sbarra per trazioni" e' una struttura ma
     * vale come [Equipment.PULLUP_BAR], e "Barra tricipiti" e' un bilanciere
     * anche se contiene "barra".
     */
    fun fromGearName(rawName: String): Equipment? {
        val n = rawName.lowercase()
        return when {
            n.contains("trazion") || n.contains("pull-up") || n.contains("pullup") ->
                Equipment.PULLUP_BAR
            n.contains("parallele") || n.contains("dip station") || n.contains("anelli") ->
                Equipment.DIP_STATION
            n.contains("panca") -> Equipment.BENCH
            n.contains("kettlebell") -> Equipment.KETTLEBELL
            n.contains("elastic") || n.contains("mini band") || n.contains("band") ->
                Equipment.RESISTANCE_BAND
            n.contains("cavo") || n.contains("cavi") || n.contains("crossover") ||
                n.contains("pulley") || n.contains("carrucola") || n.contains("lat machine") ||
                n.contains("cable") || n.contains("maniglia") || n.contains("corda") ||
                n.contains("cavigliera per cavi") -> Equipment.CABLE
            n.contains("manubri") || n.contains("manubrio") -> Equipment.DUMBBELL
            n.contains("bilanciere") || n.contains("barr") || n.contains("trap bar") ||
                n.contains("squat bar") || n.contains("swiss bar") ||
                n.contains("dischi") -> Equipment.BARBELL
            // Macchine e stazioni: dopo i cavi, perche "lat machine" e "pulley"
            // sono gia' stati assegnati a CABLE.
            n.contains("machine") || n.contains("multipower") || n.contains("smith") ||
                n.contains("press") || n.contains("rack") || n.contains("squat stand") ||
                n.contains("leg extension") || n.contains("leg curl") ||
                n.contains("pec deck") || n.contains("calf") || n.contains("hack") ||
                n.contains("stazione") || n.contains("back extension") -> Equipment.MACHINE
            n.contains("corpo libero") || n.contains("bodyweight") -> Equipment.BODYWEIGHT
            else -> null
        }
    }
}
