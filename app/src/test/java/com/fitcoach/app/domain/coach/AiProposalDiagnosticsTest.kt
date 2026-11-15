package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.support.SimulatedAiPrograms
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * La diagnostica deve indicare il controllo fallito con valore atteso e valore
 * realmente restituito, invece del generico "vincoli non rispettati".
 */
class AiProposalDiagnosticsTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 3, availableEquipment = Equipment.entries.toSet()
    )

    private val validExerciseName = ExerciseSeedData.exercises.first().name

    private fun proposalWithDays(days: List<Int>): AiProgramProposal = AiProgramProposal(
        workout = AiWorkoutProposal(
            split = "FULL_BODY",
            days = days.map { index ->
                AiWorkoutDay(
                    dayIndex = index,
                    exercises = listOf(
                        AiPlannedExercise(
                            exerciseName = validExerciseName,
                            sets = 3, minReps = 8, maxReps = 12,
                            restSeconds = 120, targetRir = 2
                        )
                    )
                )
            }
        ),
        calendar = AiCalendarProposal(
            workoutDayIndices = days,
            restDayIndices = (0..6).filterNot { it in days }
        )
    )

    @Test
    fun `reports exact training day mismatch with expected and received`() {
        val errors = AiProposalDiagnostics.analyze(
            profile, proposalWithDays(listOf(0, 2)), FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("giorni allenamento") && it.contains("attesi 3") }).isTrue()
        assertThat(errors.any { it.contains("ricevuti 2") }).isTrue()
    }

    @Test
    fun `reports unknown exercise instead of a generic message`() {
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                days = (0..2).map { index ->
                    AiWorkoutDay(
                        dayIndex = index,
                        exercises = listOf(
                            AiPlannedExercise(
                                exerciseName = "Esercizio inesistente",
                                sets = 3, minReps = 8, maxReps = 12
                            )
                        )
                    )
                }
            ),
            calendar = AiCalendarProposal(listOf(0, 1, 2), listOf(3, 4, 5, 6))
        )
        val errors = AiProposalDiagnostics.analyze(
            profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("non presente nel database") }).isTrue()
    }

    @Test
    fun `reports empty session and excessive session volume`() {
        val overloaded = AiWorkoutDay(
            dayIndex = 1,
            exercises = List(4) {
                AiPlannedExercise(exerciseName = validExerciseName, sets = 8, minReps = 8, maxReps = 12)
            }
        )
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                days = listOf(
                    AiWorkoutDay(dayIndex = 0, exercises = emptyList()),
                    overloaded,
                    AiWorkoutDay(
                        dayIndex = 2,
                        exercises = listOf(
                            AiPlannedExercise(exerciseName = validExerciseName, sets = 3, minReps = 8, maxReps = 12)
                        )
                    )
                )
            ),
            calendar = AiCalendarProposal(listOf(0, 1, 2), listOf(3, 4, 5, 6))
        )
        val errors = AiProposalDiagnostics.analyze(
            profile, proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("nessun esercizio") }).isTrue()
        assertThat(errors.any { it.contains("massimo sicuro 24") }).isTrue()
    }

    @Test
    fun `reports exercise incompatible with equipment`() {
        val bodyweightOnly = profile.copy(availableEquipment = setOf(Equipment.BODYWEIGHT))
        val errors = AiProposalDiagnostics.analyze(
            bodyweightOnly, proposalWithDays(listOf(0, 1, 2)), FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("incompatibile") }).isTrue()
    }

    @Test
    fun `diagnostics are never empty`() {
        val errors = AiProposalDiagnostics.analyze(
            profile, proposalWithDays(listOf(0, 2)), FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors).isNotEmpty()
    }

    @Test
    fun `reports out-of-range rest and rir instead of a generic message`() {
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                days = listOf(
                    AiWorkoutDay(
                        dayIndex = 0,
                        exercises = listOf(
                            AiPlannedExercise(
                                exerciseName = validExerciseName, sets = 6, minReps = 8, maxReps = 12,
                                restSeconds = 5, targetRir = 9
                            )
                        )
                    )
                )
            ),
            calendar = AiCalendarProposal(listOf(0), (1..6).toList())
        )
        val errors = AiProposalDiagnostics.analyze(
            profile.copy(trainingDaysPerWeek = 1), proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("recupero") && it.contains("600") }).isTrue()
        assertThat(errors.any { it.contains("RIR") && it.contains("da 0 a 5") }).isTrue()
        assertThat(errors).doesNotContain(AiProposalDiagnostics.GENERIC_FALLBACK)
    }

    @Test
    fun `reports too few sets per session instead of a generic message`() {
        val proposal = AiProgramProposal(
            workout = AiWorkoutProposal(
                days = listOf(
                    AiWorkoutDay(
                        dayIndex = 0,
                        exercises = listOf(
                            AiPlannedExercise(
                                exerciseName = validExerciseName, sets = 2, minReps = 8, maxReps = 12,
                                restSeconds = 120, targetRir = 2
                            )
                        )
                    )
                )
            ),
            calendar = AiCalendarProposal(listOf(0), (1..6).toList())
        )
        val errors = AiProposalDiagnostics.analyze(
            profile.copy(trainingDaysPerWeek = 1), proposal, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("minimo 6 per seduta") }).isTrue()
        assertThat(errors).doesNotContain(AiProposalDiagnostics.GENERIC_FALLBACK)
    }

    @Test
    fun `reports invalid meal slot instead of a generic message`() {
        val proposal = SimulatedAiPrograms.proposal(
            profile = fullProfile(), foods = FoodSeedData.foods, exercises = ExerciseSeedData.exercises
        )
        val broken = proposal.copy(
            nutrition = proposal.nutrition.copy(
                days = proposal.nutrition.days.map { day ->
                    day.copy(meals = day.meals.mapIndexed { index, meal ->
                        if (index == 0) meal.copy(slot = "SPUNTINO") else meal
                    })
                }
            )
        )
        val errors = AiProposalDiagnostics.analyze(
            fullProfile(), broken, FoodSeedData.foods, ExerciseSeedData.exercises
        )
        assertThat(errors.any { it.contains("slot pasto 'SPUNTINO' non valido") }).isTrue()
        assertThat(errors).doesNotContain(AiProposalDiagnostics.GENERIC_FALLBACK)
    }

    /**
     * INVARIANTE: se il validatore boccia una proposta, la diagnostica deve
     * spiegarne il motivo. Non deve mai restituire il messaggio generico.
     */
    @Test
    fun `diagnose_spiega_ogni_bocciatura_di_validate`() {
        val p = fullProfile()
        val foods = FoodFilter.allowedFoods(FoodSeedData.foods, p)
        val exercises = ExerciseSeedData.exercises
        val base = SimulatedAiPrograms.proposal(p, foods, exercises)
        assertThat(ProgramGenerationEngine.validate(p, base, foods, exercises)).isNotNull()

        val broken: List<Pair<String, AiProgramProposal>> = listOf(
            "slot pasto errato" to base.copy(
                nutrition = base.nutrition.copy(days = base.nutrition.days.map { d ->
                    d.copy(meals = d.meals.mapIndexed { i, m -> if (i == 0) m.copy(slot = "PIPPO") else m })
                })
            ),
            "grammi fuori scala" to base.copy(
                nutrition = base.nutrition.copy(days = base.nutrition.days.map { d ->
                    d.copy(meals = d.meals.mapIndexed { i, m ->
                        if (i == 0) m.copy(entries = m.entries.mapIndexed { j, e ->
                            if (j == 0) e.copy(grams = 5_000.0) else e
                        }) else m
                    })
                })
            ),
            "recupero fuori range" to base.copy(
                workout = base.workout.copy(days = base.workout.days.map { d ->
                    d.copy(exercises = d.exercises.map { it.copy(restSeconds = 5) })
                })
            ),
            "RIR fuori range" to base.copy(
                workout = base.workout.copy(days = base.workout.days.map { d ->
                    d.copy(exercises = d.exercises.map { it.copy(targetRir = 9) })
                })
            ),
            "serie per seduta sotto il minimo" to base.copy(
                workout = base.workout.copy(days = base.workout.days.mapIndexed { i, d ->
                    if (i == 0) d.copy(exercises = listOf(d.exercises.first().copy(sets = 2))) else d
                })
            ),
            "tipo giornata non valido" to base.copy(
                nutrition = base.nutrition.copy(days = base.nutrition.days.map { it.copy(dayType = "EXTREME") })
            ),
            "alimento inesistente" to base.copy(
                nutrition = base.nutrition.copy(days = base.nutrition.days.map { d ->
                    d.copy(meals = d.meals.mapIndexed { i, m ->
                        if (i == 0) m.copy(entries = m.entries.mapIndexed { j, e ->
                            if (j == 0) e.copy(foodName = "Alimento Inesistente") else e
                        }) else m
                    })
                })
            )
        )

        broken.forEach { (label, candidate) ->
            val rejected = ProgramGenerationEngine.validate(p, candidate, foods, exercises) == null
            if (rejected) {
                val diagnosis = ProgramGenerationEngine.diagnose(p, candidate, foods, exercises)
                assertThat(diagnosis).isNotEmpty()
                assertThat("$label -> $diagnosis").doesNotContain(AiProposalDiagnostics.GENERIC_FALLBACK)
            }
        }
    }

    private fun fullProfile() = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.BEGINNER, goal = Goal.RECOMP,
        trainingDaysPerWeek = 3, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )
}
