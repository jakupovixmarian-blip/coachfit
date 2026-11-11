package com.fitcoach.app.domain.usecase

import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import javax.inject.Inject

/**
 * Genera e salva i programmi nutrizionale e di allenamento completi a partire
 * dal profilo dell'utente. Usa SOLO dati reali (profilo + database alimentare
 * ed esercizi). Se i dati essenziali mancano, ritorna un errore esplicito
 * invece di inventare valori.
 */
class GenerateProgramsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
    private val planVersionRepository: PlanVersionRepository
) {
    sealed interface Result {
        data class Success(
            val nutritionPlanId: Long,
            val workoutPlanId: Long,
            val warnings: List<String>
        ) : Result
        data class MissingData(val missing: List<String>) : Result
        data class Error(val message: String) : Result
    }

    suspend operator fun invoke(
        profile: UserProfile,
        trainingDayIndices: List<Int> = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek),
        regenerateNutrition: Boolean = true,
        regenerateWorkout: Boolean = true
    ): Result {
        val missing = buildList {
            if (profile.ageYears == null) add("eta")
            if (profile.heightCm == null) add("altezza")
            if (profile.weightKg == null) add("peso")
            if (profile.trainingDaysPerWeek <= 0) add("giorni di allenamento")
        }
        if (missing.isNotEmpty()) return Result.MissingData(missing)

        val energy = EnergyCalculator.compute(profile)
            ?: return Result.Error("Impossibile calcolare il fabbisogno energetico: dati insufficienti.")

        val warnings = mutableListOf<String>()
        var nutritionPlanId = 0L
        var workoutPlanId = 0L

        if (regenerateNutrition) {
            val foods = foodRepository.getAll()
            if (foods.isEmpty()) return Result.Error("Database alimentare vuoto: impossibile generare il piano.")

            // Il carb cycling si applica SOLO quando e giustificato dal profilo.
            val carbCyclingApplied = CarbCyclingEngine.shouldApplyCarbCycling(profile)
            val cycling = if (carbCyclingApplied) {
                CarbCyclingEngine.build(profile, energy)
            } else {
                CarbCyclingEngine.buildUniform(profile, energy)
            }
            warnings += cycling.warnings
            if (!carbCyclingApplied) {
                warnings += "Carb cycling non applicato: con questo obiettivo e numero di allenamenti " +
                    "una distribuzione uniforme dei carboidrati e piu adeguata."
            }

            val dayTypes = if (carbCyclingApplied) {
                CarbCyclingEngine.assignDayTypes(
                    cycling.highDays, cycling.mediumDays, cycling.lowDays, trainingDayIndices
                )
            } else {
                List(7) { com.fitcoach.app.domain.model.CarbDayType.MEDIUM }
            }
            val days = MealPlanGenerator.generate(profile, cycling, foods, dayTypes)
            days.filter { !it.isCoherent }.forEach { d ->
                warnings += "Giorno ${d.dayIndex + 1}: scostamento dal target di " +
                    "${d.deviations.kcal.toInt()} kcal."
            }

            nutritionPlanId = nutritionRepository.savePlan(
                profile = profile,
                energy = energy,
                cycling = cycling,
                generatedDays = days,
                dayTypes = dayTypes,
                trainingDayIndices = trainingDayIndices,
                carbCyclingApplied = carbCyclingApplied
            )
            nutritionRepository.getActivePlan()?.let { plan ->
                planVersionRepository.snapshotNutrition(
                    plan,
                    if (carbCyclingApplied) "Piano generato con carb cycling."
                    else "Piano generato con distribuzione uniforme dei carboidrati."
                )
            }
        }

        if (regenerateWorkout) {
            val exercises = exerciseRepository.getAll()
            if (exercises.isEmpty()) return Result.Error("Database esercizi vuoto: impossibile generare la scheda.")

            val built = WorkoutPlanBuilder.build(
                profile = profile,
                exercises = exercises,
                trainingDayIndices = trainingDayIndices
            )
            warnings += built.notes
            workoutPlanId = workoutRepository.savePlan(profile, built)
            workoutRepository.getActivePlan()?.let { plan ->
                planVersionRepository.snapshotWorkout(plan, "Scheda generata dal motore di allenamento.")
            }
        }

        userRepository.save(profile.copy(updatedAt = System.currentTimeMillis()))
        return Result.Success(nutritionPlanId, workoutPlanId, warnings)
    }
}
