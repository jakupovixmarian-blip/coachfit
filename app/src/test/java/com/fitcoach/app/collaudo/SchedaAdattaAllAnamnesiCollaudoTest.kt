package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiPlannedExercise
import com.fitcoach.app.domain.coach.AiWorkoutDay
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.TrainingPolicy
import com.fitcoach.app.domain.workout.TrainingPolicyChecks
import com.fitcoach.app.domain.workout.VolumeGuidelines
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * La scheda deve dipendere dall'anamnesi. Questi test usano lo stesso controllo
 * che decide il salvataggio: se un neofita puo' ricevere la scheda di un
 * avanzato, il controllo non sta facendo il suo lavoro.
 */
class SchedaAdattaAllAnamnesiCollaudoTest {

    private val catalogo = ExerciseSeedData.exercises

    private fun profilo(exp: TrainingExperience, goal: Goal = Goal.HYPERTROPHY) = UserProfile(
        name = "Test", ageYears = 30, heightCm = 178.0, weightKg = 78.0,
        experience = exp, goal = goal, trainingDaysPerWeek = 4,
        availableEquipment = setOf(
            Equipment.BARBELL, Equipment.DUMBBELL, Equipment.BENCH,
            Equipment.CABLE, Equipment.MACHINE, Equipment.BODYWEIGHT
        )
    )

    /** Giornata "da avanzato": 6 serie di multiarticolare, tecniche spinte. */
    private fun giornataAvanzata() = AiWorkoutDay(
        dayIndex = 0,
        title = "Spinta",
        muscleGroups = listOf(MuscleGroup.CHEST.name),
        exercises = listOf(
            AiPlannedExercise(
                exerciseName = "Panca piana con bilanciere", sets = 6,
                minReps = 4, maxReps = 6, restSeconds = 180, targetRir = 1,
                technique = "REST_PAUSE", techniqueReason = "Volume alto"
            ),
            AiPlannedExercise(
                exerciseName = "Croci ai cavi", sets = 5,
                minReps = 8, maxReps = 10, restSeconds = 90, targetRir = 0,
                technique = "DROP_SET", techniqueReason = "Finitura"
            )
        )
    )

    private fun violazioni(exp: TrainingExperience, goal: Goal = Goal.HYPERTROPHY): List<String> =
        TrainingPolicyChecks.dayViolations(0, giornataAvanzata(), profilo(exp, goal)) { nome ->
            catalogo.firstOrNull { it.name == nome }
        }

    @Test
    fun `un neofita non puo' ricevere la scheda di un avanzato`() {
        val neofita = violazioni(TrainingExperience.BEGINNER)
        assertThat(neofita).isNotEmpty()
        // Il vincolo e' la tecnica: il corpus la riserva a chi ha gia' il
        // controllo motorio. Il numero di serie resta quello del catalogo.
        assertThat(neofita.joinToString(" ")).contains("tecnica")
    }

    @Test
    fun `un avanzato puo' eseguire la stessa scheda`() {
        assertThat(violazioni(TrainingExperience.ADVANCED)).isEmpty()
    }

    @Test
    fun `gli esercizi esigenti sono esclusi per i neofiti e ammessi per gli avanzati`() {
        val difficile = catalogo.first { it.difficulty >= 3 }
        assertThat(TrainingPolicy.isTooDemanding(difficile, TrainingExperience.BEGINNER)).isTrue()
        assertThat(TrainingPolicy.isTooDemanding(difficile, TrainingExperience.ADVANCED)).isFalse()
    }

    @Test
    fun `il catalogo non viene contraddetto sulle serie`() {
        // Una soglia di livello piu' bassa del catalogo respingeva schede
        // corrette (Alzate laterali: 4 serie prescritte).
        val alzate = catalogo.first { it.name == "Alzate laterali" }
        assertThat(VolumeGuidelines.maxSetsPerExercise(alzate.category))
            .isAtLeast(alzate.defaultSets)
    }

    @Test
    fun `le tecniche ammesse crescono con l'esperienza`() {
        assertThat(TrainingPolicy.allowedTechniques(TrainingExperience.BEGINNER))
            .hasSize(1)
        assertThat(TrainingPolicy.allowedTechniques(TrainingExperience.ADVANCED).size)
            .isGreaterThan(TrainingPolicy.allowedTechniques(TrainingExperience.INTERMEDIATE).size)
    }

    @Test
    fun `l'obiettivo fissa un volume di riferimento dal corpus`() {
        // Il corpus documenta 10-25 serie per ipertrofia e 12-20 per ricomposizione.
        assertThat(TrainingPolicy.volumeTarget(Goal.HYPERTROPHY)).isEqualTo(10..25)
        assertThat(TrainingPolicy.volumeTarget(Goal.RECOMP)).isEqualTo(12..20)
    }

    @Test
    fun `una scheda senza tecniche non viene respinta per questo motivo`() {
        // Il corpus dichiara le tecniche facoltative: obbligarle avrebbe inventato
        // un requisito che nessuna fonte sostiene.
        val senzaTecniche = giornataAvanzata().copy(
            exercises = giornataAvanzata().exercises.map {
                it.copy(technique = "NONE", techniqueReason = "")
            }
        )
        val v = TrainingPolicyChecks.dayViolations(
            0, senzaTecniche, profilo(TrainingExperience.ADVANCED)
        ) { nome -> catalogo.firstOrNull { it.name == nome } }
        assertThat(v).isEmpty()
    }

    @Test
    fun `una tecnica senza motivazione viene respinta`() {
        val senzaMotivo = giornataAvanzata().copy(
            exercises = giornataAvanzata().exercises.map { it.copy(techniqueReason = "") }
        )
        val v = TrainingPolicyChecks.dayViolations(
            0, senzaMotivo, profilo(TrainingExperience.ADVANCED)
        ) { nome -> catalogo.firstOrNull { it.name == nome } }
        assertThat(v.joinToString(" ")).contains("senza motivazione")
    }

    @Test
    fun `ogni gruppo allenabile ha almeno un esercizio di isolamento nel catalogo`() {
        val gruppi = setOf(
            MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
            MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.QUADS,
            MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES
        )
        val scoperti = gruppi.filter { g ->
            catalogo.none { it.primaryMuscle == g && it.category == ExerciseCategory.ISOLATION }
        }
        assertThat(scoperti).isEmpty()
    }
}
