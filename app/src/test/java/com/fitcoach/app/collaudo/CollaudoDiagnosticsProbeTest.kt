package com.fitcoach.app.collaudo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.usecase.GenerateProgramsUseCase
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.domain.workout.WorkoutPlanValidator
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sonda diagnostica: verifica se il piano generato dal motore deterministico
 * offline rispetta il validatore interno dell'app ([WorkoutPlanValidator]) e se
 * la distribuzione del carb cycling assegna correttamente i giorni di allenamento.
 * Solo output: serve a documentare i difetti reali prima di correggerli.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CollaudoDiagnosticsProbeTest {

    @Test
    fun `sonda validatore e carb cycling sul piano offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        val userRepository = UserRepository(db.userProfileDao())
        val foodRepository = FoodRepository(db.foodDao())
        val exerciseRepository = ExerciseRepository(db.exerciseDao())
        val nutritionRepository = NutritionRepository(db)
        val workoutRepository = WorkoutRepository(db)
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })

        val marjan = MarjanProfile.build()
        userRepository.save(marjan)
        GenerateProgramsUseCase(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, PlanVersionRepository(db.planVersionDao())
        )(marjan, requireAnamnesis = false)

        // --- 1) Validatore sullo split generato offline ---
        val workout = workoutRepository.getActivePlan()!!
        val catalog = exerciseRepository.getAll()
        val days = workout.days.filterNot { it.isRestDay }.map { d ->
            WorkoutPlanValidator.DayProposal(
                dayIndex = d.dayIndex,
                groups = d.muscleGroups,
                exercises = d.exercises.map { WorkoutPlanValidator.ExerciseProposal(it.exerciseId, it.sets) }
            )
        }
        val report = WorkoutPlanValidator.validate(marjan, catalog, days)
        println("=== VALIDATORE OFFLINE: ok=${report.ok} ===")
        report.violations.forEach { println("VIOLAZIONE: $it") }
        println("volumeEffettivo=${report.effectiveVolume}")

        // --- 2) Carb cycling: i giorni di allenamento ricevono HIGH/MEDIUM? ---
        val plan = nutritionRepository.getActivePlan()!!
        val trainingDays = WorkoutPlanBuilder.defaultTrainingDays(marjan.trainingDaysPerWeek)
        println("=== CARB CYCLING (giorni allenamento=$trainingDays) ===")
        plan.days.forEach { d ->
            val isTraining = d.dayIndex in trainingDays
            println("giorno ${d.dayIndex} tipo=${d.dayType} allenamento=$isTraining")
        }
        val trainingLowDays = plan.days.filter { it.dayIndex in trainingDays && it.dayType == CarbDayType.LOW }
        println("GIORNI DI ALLENAMENTO CON LOW CARB: ${trainingLowDays.map { it.dayIndex }}")

        // --- 3) Coerenza bilancio settimanale ---
        val energy = EnergyCalculator.compute(marjan)!!
        val cycling = CarbCyclingEngine.build(marjan, energy)
        println("=== BILANCIO ===")
        println("weeklyKcal=${cycling.weeklyKcal} weeklyFromTarget=${cycling.weeklyKcalFromTarget} diff=${cycling.weeklyKcalFromTarget - cycling.weeklyKcal}")
        println("warnings=${cycling.warnings}")

        // --- 4) Scostamenti nutrizionali per giorno ---
        plan.days.forEach { d ->
            println("giorno ${d.dayIndex} scostamento=${d.actualKcal - d.targetKcal}")
        }
        db.close()
    }
}
