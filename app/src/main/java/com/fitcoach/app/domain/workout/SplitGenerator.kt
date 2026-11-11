package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.Exercise
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
     * Numero di esercizi per sessione in base a esperienza e durata disponibile.
     */
    fun exercisesPerSession(experience: TrainingExperience, groupsInDay: Int): Int {
        val base = when (experience) {
            TrainingExperience.BEGINNER -> 5
            TrainingExperience.INTERMEDIATE -> 6
            TrainingExperience.ADVANCED -> 7
        }
        return base.coerceAtMost(4 + groupsInDay).coerceAtLeast(4)
    }

    /**
     * Seleziona gli esercizi per una giornata, privilegiando i multiarticolari
     * per primi, filtrando per attrezzatura e limitazioni dell'utente.
     */
    fun selectExercises(
        groups: Set<MuscleGroup>,
        all: List<Exercise>,
        profile: UserProfile,
        count: Int
    ): List<Exercise> {
        val equipment = profile.availableEquipment.ifEmpty { null }
        val excluded = profile.movementsToAvoid.map { it.lowercase() }

        val usable = all.filter { ex ->
            // Deve colpire almeno un gruppo della giornata.
            (ex.primaryMuscle in groups || ex.secondaryMuscles.any { it in groups }) &&
                // Attrezzatura disponibile (o esercizio a corpo libero).
                (equipment == null || ex.bodyweightOnly ||
                    ex.equipment.isEmpty() || ex.equipment.any { it in equipment }) &&
                // Limitazioni fisiche
                excluded.none { ex.name.lowercase().contains(it) || ex.primaryMuscle.label.lowercase().contains(it) }
        }

        // Priorita: multiarticolari che colpiscono il gruppo primario della giornata.
        val sorted = usable.sortedWith(
            compareByDescending<Exercise> { it.category == com.fitcoach.app.domain.model.ExerciseCategory.COMPOUND }
                .thenByDescending { it.primaryMuscle in groups }
        )

        // Evita duplicati dello stesso movimento e bilancia i gruppi.
        val selected = mutableListOf<Exercise>()
        val perGroup = mutableMapOf<MuscleGroup, Int>()
        for (ex in sorted) {
            if (selected.size >= count) break
            val groupCount = perGroup[ex.primaryMuscle] ?: 0
            val maxPerGroup = when {
                ex.primaryMuscle.isLargeGroup -> 3
                else -> 2
            }
            if (groupCount >= maxPerGroup) continue
            selected += ex
            perGroup[ex.primaryMuscle] = groupCount + 1
        }
        return selected
    }
}
