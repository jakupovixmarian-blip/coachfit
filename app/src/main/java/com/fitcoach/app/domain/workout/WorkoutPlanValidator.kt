package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.roundToInt

/**
 * Validatore deterministico di una scheda proposta.
 *
 * E il VINCOLO che l'AI deve soddisfare, non un generatore: giudica una bozza di
 * scheda (giornate -> gruppi + esercizi) verificando copertura dei gruppi previsti,
 * volume per gruppo, compatibilita con attrezzatura/limitazioni/livello, uso di
 * esercizi realmente esistenti e sicurezza (serie per seduta).
 */
object WorkoutPlanValidator {

    /** Una giornata candidata: indice, gruppi previsti ed esercizi scelti con le serie. */
    data class DayProposal(
        val dayIndex: Int,
        val groups: Set<MuscleGroup>,
        val exercises: List<ExerciseProposal>
    )

    data class ExerciseProposal(val exerciseId: Long, val sets: Int)

    data class Report(
        val violations: List<String>,
        val effectiveVolume: Map<MuscleGroup, Int>,
        val coveredGroups: Set<MuscleGroup>,
        /**
         * Osservazioni non bloccanti.
         *
         * Il volume settimanale e' una convenzione, non un requisito di
         * sicurezza: 10 serie di polpacci per un avanzato non sono "giuste" in
         * assoluto. Un piano che se ne discosta viene accettato e l'AI deve
         * motivare la scelta citando le fonti, non essere respinta.
         */
        val warnings: List<String> = emptyList()
    ) {
        /** Blocchi: violazioni verificabili e non negoziabili. */
        val ok: Boolean get() = violations.isEmpty()
    }

    private const val MAX_SETS_PER_SESSION = 30

    /**
     * Serie dirette minime perche' un gruppo sia davvero allenato.
     *
     * Il solo volume effettivo non bastava: conta anche lo stimolo indiretto,
     * quindi bicipiti e tricipiti passavano il controllo con ZERO serie dirette
     * grazie al lavoro di trazioni e distensioni. Un piano di ipertrofia senza
     * esercizi diretti per un gruppo lo lascia sotto-allenato, e nessun numero
     * di serie indirette compensa la mancanza di lavoro specifico.
     */
    private const val MIN_DIRECT_SETS = 3

    fun validate(
        profile: UserProfile,
        catalog: List<Exercise>,
        days: List<DayProposal>
    ): Report {
        val catalogById = catalog.associateBy { it.id }
        val allowedIds = catalog.filter { isCompatible(it, profile) }.map { it.id }.toSet()

        val violations = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val plannedForVolume = mutableListOf<PlannedExercise>()
        val coveredGroups = mutableSetOf<MuscleGroup>()

        for (day in days) {
            val dayGroupsCovered = mutableSetOf<MuscleGroup>()
            var daySets = 0
            for (p in day.exercises) {
                val exercise = catalogById[p.exerciseId]
                if (exercise == null) {
                    violations += "giorno ${day.dayIndex}: exerciseId ${p.exerciseId} inesistente nel catalogo"
                    continue
                }
                if (p.exerciseId !in allowedIds) {
                    violations += "giorno ${day.dayIndex}: '${exercise.name}' non compatibile con " +
                        "attrezzatura/limitazioni/livello del soggetto"
                }
                if (p.sets !in 1..6) {
                    violations += "giorno ${day.dayIndex}: '${exercise.name}' con ${p.sets} serie (consentite 1..6)"
                }
                daySets += p.sets
                if (exercise.primaryMuscle in day.groups) dayGroupsCovered += exercise.primaryMuscle
                plannedForVolume += PlannedExercise(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    primaryMuscle = exercise.primaryMuscle,
                    orderIndex = 0,
                    sets = p.sets,
                    minReps = exercise.minReps,
                    maxReps = exercise.maxReps,
                    restSeconds = exercise.restSeconds,
                    targetRir = exercise.targetRir
                )
            }
            coveredGroups += dayGroupsCovered
            if (daySets > MAX_SETS_PER_SESSION) {
                violations += "giorno ${day.dayIndex}: $daySets serie totali (troppe per una seduta)"
            }
            val missing = day.groups - dayGroupsCovered
            if (missing.isNotEmpty()) {
                violations += "giorno ${day.dayIndex}: gruppi previsti senza esercizi -> " +
                    missing.joinToString { it.name }
            }
        }

        if (days.size != profile.trainingDaysPerWeek) {
            violations += "giornate allenamento: attese ${profile.trainingDaysPerWeek}, ricevute ${days.size}"
        }

        val effective = EffectiveVolumeCalculator
            .forExercises(plannedForVolume, catalogById)
            .effective
            .mapValues { (_, v) -> v.roundToInt() }

        // Le soglie non sono piu' due numeri fissi per tutti: dipendono dal
        // gruppo (grande o piccolo) e dall'esperienza dichiarata.
        val directVolume = EffectiveVolumeCalculator
            .forExercises(plannedForVolume, catalogById)
            .direct
            .mapValues { (_, v) -> v.roundToInt() }

        // Il volume e' materia di programmazione, non di sicurezza: fuori range
        // produce un avviso motivato, non il rifiuto del piano. La decisione
        // spetta al Coach AI, che la argomenta con le fonti del corpus.
        effective.forEach { (group, sets) ->
            val range = VolumeGuidelines.forGroup(group, profile.experience)
            if (sets < range.min) {
                warnings += "volume ${group.name}: $sets serie effettive, sotto l'intervallo di " +
                    "riferimento ${range.min}-${range.max} per ${profile.experience.name.lowercase()}: " +
                    "motivare la scelta o valutare un gruppo gia' coperto indirettamente"
            }
            if (sets > range.mrv) {
                warnings += "volume ${group.name}: $sets serie effettive, oltre il massimo " +
                    "recuperabile ${range.mrv}: verificare il recupero"
            }
        }

        // Un gruppo stimolato solo di riflesso resta un segnale da valutare.
        (directVolume.keys + effective.keys + coveredGroups).forEach { group ->
            val direct = directVolume[group] ?: 0
            if (direct < MIN_DIRECT_SETS) {
                warnings += "volume ${group.name}: $direct serie dirette (sotto $MIN_DIRECT_SETS): " +
                    "lo stimolo indiretto potrebbe non bastare per il lavoro specifico"
            }
        }

        return Report(violations, effective, coveredGroups, warnings)
    }

    /** Compatibilita con attrezzatura, limitazioni, esclusioni e livello tecnico del soggetto. */
    fun isCompatible(ex: Exercise, profile: UserProfile): Boolean {
        val equipment = EquipmentFit.equipmentFor(profile)
        val excluded = profile.movementsToAvoid.map { it.lowercase() }
        val disliked = profile.dislikedExercises.map { it.lowercase() }
        val unknown = profile.unknownExercises.map { it.lowercase() }
        val maxDifficulty = when (profile.technicalLevel) {
            com.fitcoach.app.domain.model.TechnicalLevel.NOVICE -> 2
            com.fitcoach.app.domain.model.TechnicalLevel.COMPETENT -> 3
            com.fitcoach.app.domain.model.TechnicalLevel.PROFICIENT -> 3
        }
        return EquipmentFit.isAvailable(ex, equipment) &&
            excluded.none { ex.name.lowercase().contains(it) || ex.primaryMuscle.label.lowercase().contains(it) } &&
            disliked.none { ex.name.lowercase().contains(it) } &&
            unknown.none { ex.name.lowercase().contains(it) } &&
            ex.difficulty <= maxDifficulty
    }
}
