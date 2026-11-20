package com.fitcoach.app.domain.workout

import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.TrainingExperience
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * I limiti di programmazione devono venire dal catalogo, non da soglie fisse.
 *
 * La regressione che questi test difendono: con un tetto fisso a 30 ripetizioni
 * OGNI scheda contenente Plank (prescritto a 30-60 secondi dal catalogo) veniva
 * respinta, quindi nessun modello poteva completare la generazione.
 */
class CatalogBoundsTest {

    private val plank = ExerciseSeedData.exercises.first { it.name == "Plank" }
    private val bench = ExerciseSeedData.exercises.first { it.name.contains("Panca", ignoreCase = true) }

    @Test
    fun `Plank viene accettato con il range che il catalogo prescrive`() {
        val violations = CatalogBounds.violations(
            plank,
            sets = plank.defaultSets,
            minReps = plank.minReps,
            maxReps = plank.maxReps,
            rest = plank.restSeconds,
            rir = plank.targetRir
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `il range di ripetizioni e' derivato dal record, non da una costante`() {
        val range = CatalogBounds.reps(plank)

        // Il catalogo prescrive 30-60: un tetto fisso a 30 avrebbe escluso 60.
        assertThat(range.last).isAtLeast(plank.maxReps)
        assertThat(range.first).isAtMost(plank.minReps)
        assertThat(range.last).isGreaterThan(30)
    }

    @Test
    fun `serie fuori dal record dell'esercizio vengono segnalate`() {
        val violations = CatalogBounds.violations(
            plank,
            sets = plank.defaultSets + 5,
            minReps = plank.minReps,
            maxReps = plank.maxReps,
            rest = plank.restSeconds,
            rir = plank.targetRir
        )

        assertThat(violations.joinToString()).contains("serie")
    }

    @Test
    fun `ogni esercizio del catalogo e' programmabile con i propri valori`() {
        // Invariante: nessun esercizio pubblicato deve essere impossibile da
        // inserire in una scheda. Un solo record incoerente blocca la generazione.
        val blocked = ExerciseSeedData.exercises.mapNotNull { exercise ->
            CatalogBounds.violations(
                exercise,
                sets = exercise.defaultSets,
                minReps = exercise.minReps,
                maxReps = exercise.maxReps,
                rest = exercise.restSeconds,
                rir = exercise.targetRir
            ).takeIf { it.isNotEmpty() }?.let { "${exercise.name}: $it" }
        }

        assertThat(blocked).isEmpty()
    }

    @Test
    fun `il volume di riferimento cresce con l'esperienza`() {
        val beginner = VolumeGuidelines.forGroup(MuscleGroup.CHEST, TrainingExperience.BEGINNER)
        val advanced = VolumeGuidelines.forGroup(MuscleGroup.CHEST, TrainingExperience.ADVANCED)

        assertThat(advanced.mrv).isGreaterThan(beginner.mrv)
        assertThat(advanced.min).isAtLeast(beginner.min)
    }

    @Test
    fun `i gruppi grandi tollerano piu' volume dei piccoli`() {
        val chest = VolumeGuidelines.forGroup(MuscleGroup.CHEST, TrainingExperience.INTERMEDIATE)
        val biceps = VolumeGuidelines.forGroup(MuscleGroup.BICEPS, TrainingExperience.INTERMEDIATE)

        assertThat(chest.mrv).isGreaterThan(biceps.mrv)
    }

    @Test
    fun `il tetto di seduta dipende dagli esercizi scelti`() {
        val compoundOnly = VolumeGuidelines.sessionCeiling(
            listOf(ExerciseCategory.COMPOUND, ExerciseCategory.COMPOUND)
        )
        val mixed = VolumeGuidelines.sessionCeiling(
            listOf(ExerciseCategory.COMPOUND, ExerciseCategory.ISOLATION)
        )

        assertThat(compoundOnly).isGreaterThan(mixed)
    }
}
