package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile

/**
 * Generatore di split settimanali.
 *
 * La scelta dipende da esperienza e giorni disponibili, secondo criteri di
 * frequenza muscolare e recupero:
 *  - 2-3 giorni  -> Full Body (frequenza 2-3x per gruppo)
 *  - 4 giorni    -> Upper / Lower (frequenza 2x)
 *  - 5-6 giorni  -> Push / Pull / Legs (frequenza 2x, piu volume)
 *  - Advanced 6+ -> PPL con enfasi
 */
object SplitGenerator {

    data class SplitDay(
        val dayIndex: Int,
        val title: String,
        val muscleGroups: Set<MuscleGroup>,
        val isRest: Boolean = false
    )

    fun recommendSplit(profile: UserProfile): SplitType {
        return when {
            profile.trainingDaysPerWeek <= 3 -> SplitType.FULL_BODY
            profile.trainingDaysPerWeek == 4 -> SplitType.UPPER_LOWER
            else -> SplitType.PUSH_PULL_LEGS
        }
    }

    /**
     * Distribuisce i gruppi muscolari sui giorni di allenamento della settimana.
     * @param trainingDayIndices indici 0..6 (0=Lunedi) dei giorni in cui ci si allena
     */
    fun buildWeek(
        splitType: SplitType,
        trainingDayIndices: List<Int>,
        experience: TrainingExperience
    ): List<SplitDay> {
        val days = MutableList(7) { SplitDay(it, "Riposo", emptySet(), isRest = true) }
        val training = trainingDayIndices.sorted()

        when (splitType) {
            SplitType.FULL_BODY -> {
                val groups = setOf(
                    MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
                    MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
                    MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.CORE
                )
                training.forEach { idx ->
                    if (idx in 0..6) days[idx] = SplitDay(idx, "Full Body", groups)
                }
            }
            SplitType.UPPER_LOWER -> {
                val upper = setOf(
                    MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
                    MuscleGroup.BICEPS, MuscleGroup.TRICEPS
                )
                val lower = setOf(
                    MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
                    MuscleGroup.CALVES, MuscleGroup.CORE
                )
                training.forEachIndexed { i, idx ->
                    if (idx !in 0..6) return@forEachIndexed
                    val isUpper = i % 2 == 0
                    days[idx] = SplitDay(
                        idx,
                        if (isUpper) "Upper" else "Lower",
                        if (isUpper) upper else lower
                    )
                }
            }
            SplitType.PUSH_PULL_LEGS -> {
                val push = setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
                val pull = setOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.CORE)
                val legs = setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
                val rotation = listOf("Push" to push, "Pull" to pull, "Legs" to legs)
                training.forEachIndexed { i, idx ->
                    if (idx !in 0..6) return@forEachIndexed
                    val (title, groups) = rotation[i % rotation.size]
                    days[idx] = SplitDay(idx, title, groups)
                }
            }
            SplitType.CUSTOM -> {
                // Fallback: upper/lower alternato, personalizzabile dall'utente.
                val upper = setOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS)
                val lower = setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
                training.forEachIndexed { i, idx ->
                    if (idx !in 0..6) return@forEachIndexed
                    val isUpper = i % 2 == 0
                    days[idx] = SplitDay(idx, if (isUpper) "Upper" else "Lower", if (isUpper) upper else lower)
                }
            }
        }
        return days
    }

    /**
     * Serie settimanali per gruppo muscolare, in funzione dell'esperienza.
     * Valori entro i range raccomandati (Schoenfeld: 10-20 serie/gruppo/settimana).
     */
    fun weeklySetsPerGroup(experience: TrainingExperience): Int = when (experience) {
        TrainingExperience.BEGINNER -> 10
        TrainingExperience.INTERMEDIATE -> 14
        TrainingExperience.ADVANCED -> 18
    }

    /**
     * Numero di esercizi per sessione in base a esperienza, durata disponibile e
     * numero di gruppi muscolari della giornata.
     *
     * Se l'utente indica una durata massima, il numero di esercizi viene limitato
     * in modo che la sessione (serie x (esecuzione + recupero)) resti entro il tempo.
     */
    fun exercisesPerSession(
        experience: TrainingExperience,
        groupsInDay: Int,
        maxSessionMinutes: Int? = null
    ): Int {
        val base = when (experience) {
            TrainingExperience.BEGINNER -> 5
            TrainingExperience.INTERMEDIATE -> 6
            TrainingExperience.ADVANCED -> 7
        }
        val byGroups = base.coerceAtMost(4 + groupsInDay).coerceAtLeast(4)
        if (maxSessionMinutes == null) return byGroups
        // Stima prudente: ~5 min per esercizio (serie + recuperi + setup).
        val byTime = (maxSessionMinutes / 5).coerceIn(3, 9)
        return minOf(byGroups, byTime).coerceAtLeast(3)
    }

    /**
     * Seleziona gli esercizi per una giornata con logica biomeccanica:
     *  - rispetta attrezzatura, limitazioni, esercizi non conosciuti/non graditi;
     *  - privilegia gli esercizi preferiti e i gruppi muscolari prioritari;
     *  - bilancia i pattern di movimento (evita 3 spinte orizzontali identiche);
     *  - adatta la difficolta al livello tecnico;
     *  - assegna piu esercizi ai gruppi prioritari.
     *
     * Ritorna anche la motivazione della scelta per ogni esercizio.
     */
    fun selectExercisesDetailed(
        groups: Set<MuscleGroup>,
        all: List<Exercise>,
        profile: UserProfile,
        count: Int,
        rotationSeed: Int = 0
    ): List<SelectedExercise> {
        val equipment = profile.availableEquipment.ifEmpty { null }
        val excluded = profile.movementsToAvoid.map { it.lowercase() }
        val disliked = profile.dislikedExercises.map { it.lowercase() }
        val unknown = profile.unknownExercises.map { it.lowercase() }
        val preferred = profile.preferredExercises.map { it.lowercase() }
        val maxDifficulty = when (profile.technicalLevel) {
            com.fitcoach.app.domain.model.TechnicalLevel.NOVICE -> 2
            com.fitcoach.app.domain.model.TechnicalLevel.COMPETENT -> 3
            com.fitcoach.app.domain.model.TechnicalLevel.PROFICIENT -> 3
        }

        val usable = all.filter { ex ->
            (ex.primaryMuscle in groups || ex.secondaryMuscles.any { it in groups }) &&
                (equipment == null || ex.bodyweightOnly || ex.equipment.isEmpty() ||
                    ex.equipment.any { it in equipment }) &&
                excluded.none { ex.name.lowercase().contains(it) || ex.primaryMuscle.label.lowercase().contains(it) } &&
                disliked.none { ex.name.lowercase().contains(it) } &&
                unknown.none { ex.name.lowercase().contains(it) } &&
                ex.difficulty <= maxDifficulty
        }

        val priorities = profile.priorityMuscles
        // Prima gli esercizi il cui muscolo PRINCIPALE appartiene alla giornata:
        // un muscolo secondario non deve "portare" un esercizio in una seduta dove
        // non e previsto (es. Military press in una giornata gambe).
        val primaryMatches = usable.filter { it.primaryMuscle in groups }
        val secondaryMatches = usable.filter { it.primaryMuscle !in groups }
        val bestFirst = compareByDescending<Exercise> { preferred.any { p -> it.name.lowercase().contains(p) } }
            .thenByDescending { it.primaryMuscle in priorities }
            .thenByDescending { it.category == ExerciseCategory.COMPOUND }
            .thenByDescending { it.primaryMuscle in groups }
            .thenBy { it.difficulty }

        val rotationSeedSafe = if (primaryMatches.isEmpty()) 0 else Math.floorMod(rotationSeed, primaryMatches.size)
        val rotatedPrimary = (primaryMatches.sortedWith(bestFirst).let { it.drop(rotationSeedSafe) + it.take(rotationSeedSafe) })
        val rotatedSecondary = secondaryMatches.sortedWith(bestFirst)

        val selected = mutableListOf<SelectedExercise>()
        val perGroup = mutableMapOf<MuscleGroup, Int>()
        val perPattern = mutableMapOf<com.fitcoach.app.domain.model.MovementPattern, Int>()
        val chosenIds = mutableSetOf<Long>()

        fun maxPerGroup(g: MuscleGroup): Int = when {
            g in priorities -> 3
            g.isLargeGroup -> 3
            else -> 2
        }

        fun tryAdd(ex: Exercise): Boolean {
            if (selected.size >= count) return false
            if (ex.id in chosenIds) return false
            if ((perGroup[ex.primaryMuscle] ?: 0) >= maxPerGroup(ex.primaryMuscle)) return false
            if (ex.movementPattern != com.fitcoach.app.domain.model.MovementPattern.OTHER &&
                (perPattern[ex.movementPattern] ?: 0) >= 2
            ) return false
            selected += SelectedExercise(ex, reasonFor(ex, profile, groups))
            chosenIds += ex.id
            perGroup[ex.primaryMuscle] = (perGroup[ex.primaryMuscle] ?: 0) + 1
            perPattern[ex.movementPattern] = (perPattern[ex.movementPattern] ?: 0) + 1
            return true
        }

        // 1) COPERTURA: almeno un esercizio per ogni gruppo previsto dalla giornata,
        //    privilegiando i gruppi prioritari e i gruppi grandi.
        val groupOrder = groups.sortedWith(
            compareByDescending<MuscleGroup> { it in priorities }.thenByDescending { it.isLargeGroup }
        )
        for (g in groupOrder) {
            if (selected.size >= count) break
            if (perGroup[g] != null) continue
            rotatedPrimary.firstOrNull { it.primaryMuscle == g && it.id !in chosenIds && tryAdd(it) }
        }

        // 2) RIEMPIMENTO: completa fino al numero previsto con i migliori esercizi
        //    principali, rispettando i limiti per gruppo e per pattern.
        for (ex in rotatedPrimary) {
            if (selected.size >= count) break
            tryAdd(ex)
        }

        // 3) ULTIMA RISORSA: esercizi con muscolo principale fuori giornata ma con
        //    un muscolo secondario utile (solo se la copertura non basta).
        for (ex in rotatedSecondary) {
            if (selected.size >= count) break
            tryAdd(ex)
        }
        return selected
    }

    /** Costruisce la motivazione della scelta di un esercizio. */
    private fun reasonFor(
        ex: Exercise,
        profile: UserProfile,
        groups: Set<MuscleGroup>
    ): String {
        val parts = mutableListOf<String>()
        parts += when (ex.category) {
            ExerciseCategory.COMPOUND -> "multiarticolare: massimo stimolo complessivo e transfer"
            ExerciseCategory.ISOLATION -> "isolamento: enfasi mirata su ${ex.primaryMuscle.label.lowercase()}"
            ExerciseCategory.BODYWEIGHT -> "a corpo libero: adatto all'attrezzatura disponibile"
        }
        if (profile.preferredExercises.any { ex.name.lowercase().contains(it.lowercase()) }) {
            parts += "esercizio che preferisci"
        }
        if (ex.primaryMuscle in profile.priorityMuscles) {
            parts += "gruppo muscolare prioritario"
        }
        parts += "pattern: ${ex.movementPattern.label.lowercase()}"
        if (ex.difficulty >= 3) parts += "richiede tecnica consolidata"
        return parts.joinToString("; ")
    }

    data class SelectedExercise(val exercise: Exercise, val reason: String)

    /**
     * Seleziona gli esercizi per una giornata (versione semplificata, usata dai
     * test e dai casi che non richiedono la motivazione).
     */
    fun selectExercises(
        groups: Set<MuscleGroup>,
        all: List<Exercise>,
        profile: UserProfile,
        count: Int
    ): List<Exercise> = selectExercisesDetailed(groups, all, profile, count).map { it.exercise }
}
