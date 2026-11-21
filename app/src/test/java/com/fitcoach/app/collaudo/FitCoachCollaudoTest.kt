package com.fitcoach.app.collaudo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.ShoppingRepository
import com.fitcoach.app.data.repository.SubstitutionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.EquipmentFit
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.domain.workout.WorkoutPlanValidator
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.TestProgramGenerator
import com.fitcoach.app.support.textResponse
import com.fitcoach.app.support.toolCallResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.LocalDate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * COLLAUDO END-TO-END del percorso reale dell'utente Marjan.
 *
 * Nessun mock dei motori: usa database Room reale, alimenti/esercizi reali
 * (FoodSeedData / ExerciseSeedData) e i motori deterministici reali. Solo il
 * provider AI e "scriptato" (perche non esiste un LLM nel collaudo) e la chat
 * esterna e simulata.
 *
 * Copre: anamnesi -> generazione (offline reale) -> coerenza nutrizionale ->
 * carb cycling -> scheda -> registrazione allenamento -> progressive overload ->
 * diario -> check-in settimanale -> rifiuto/approvazione/rollback.
 *
 * Le asserzioni sono ESPLICITE sui difetti reali trovati: se un controllo fallisce,
 * il difetto e reale e va corretto nel codice di produzione, non nel test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FitCoachCollaudoTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var planVersionRepository: PlanVersionRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var diaryRepository: NutritionDiaryRepository
    private lateinit var shoppingRepository: ShoppingRepository
    private lateinit var substitutionRepository: SubstitutionRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var changeRepository: ChangeRepository

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val profile = MarjanProfile.build()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        nutritionRepository = NutritionRepository(db)
        workoutRepository = WorkoutRepository(db)
        planVersionRepository = PlanVersionRepository(db.planVersionDao())
        progressRepository = ProgressRepository(db.progressDao(), db.checkInDao())
        diaryRepository = NutritionDiaryRepository(db)
        shoppingRepository = ShoppingRepository(db, nutritionRepository, foodRepository)
        substitutionRepository = SubstitutionRepository(db, nutritionRepository, foodRepository)
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())
        changeRepository = ChangeRepository(db.programChangeDao())
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        userRepository.save(profile)
        chatRepository.saveAiSettings(
            AiProviderSettings(
                useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model"
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun generate() {
        val result = TestProgramGenerator.generate(
            json = json,
            userRepository = userRepository,
            foodRepository = foodRepository,
            exerciseRepository = exerciseRepository,
            nutritionRepository = nutritionRepository,
            workoutRepository = workoutRepository,
            planVersionRepository = planVersionRepository,
            chatRepository = chatRepository,
            db = db,
            profile = profile
        )
        assertThat(result).isInstanceOf(com.fitcoach.app.domain.coach.AiProgramGenerationService.Result.Success::class.java)
    }

    // ============================================================ FASE 1

    @Test
    fun `FASE 1 - il profilo Marjan genera un piano attivo persistito`() = runTest {
        generate()
        val nutrition = nutritionRepository.getActivePlan()
        val workout = workoutRepository.getActivePlan()
        assertThat(nutrition).isNotNull()
        assertThat(workout).isNotNull()
        assertThat(nutrition!!.days).hasSize(7)
        assertThat(workout!!.days).hasSize(7)
    }

    @Test
    fun `FASE 1 - i calcoli energetici usano formule documentate e distinguono stima da override`() = runTest {
        val energy = EnergyCalculator.compute(profile)!!
        assertThat(energy.bmrFormula).isEqualTo("Katch-McArdle")
        assertThat(energy.bmr).isWithin(1.0).of(370.0 + 21.6 * (75.0 * (1 - 0.19)))
        assertThat(energy.tdee).isWithin(1.0).of(energy.bmr * 1.574)
        assertThat(energy.notes.any { it.contains("stima") || it.contains("stimato") }).isTrue()
    }

    // ============================================================ FASE 2 - nutrizione

    @Test
    fun `FASE 2 - il piano nutrizionale copre 5 pasti al giorno con alimenti reali`() = runTest {
        generate()
        val plan = nutritionRepository.getActivePlan()!!
        plan.days.forEach { day ->
            assertThat(day.meals.map { it.slot }).containsAtLeast(
                MealSlot.BREAKFAST, MealSlot.MORNING_SNACK, MealSlot.LUNCH,
                MealSlot.AFTERNOON_SNACK, MealSlot.DINNER
            )
            day.meals.flatMap { it.entries }.forEach { e ->
                assertThat(e.foodName).isNotEmpty()
                assertThat(e.grams).isGreaterThan(0.0)
            }
        }
    }

    @Test
    fun `FASE 2 - ogni giorno nutrizionale e coerente con il proprio target entro tolleranza`() = runTest {
        generate()
        val plan = nutritionRepository.getActivePlan()!!
        plan.days.forEach { day ->
            val kcalDev = kotlin.math.abs(day.actualKcal - day.targetKcal)
            assertThat(kcalDev).isAtMost(150.0)
        }
    }

    @Test
    fun `FASE 2 - il bilancio settimanale dei giorni coincide con il bilancio pianificato`() = runTest {
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        assertThat(cycling.isValid).isTrue()
        assertThat(cycling.warnings).isEmpty()
        assertThat(kotlin.math.abs(cycling.weeklyKcalFromTarget - cycling.weeklyKcal)).isAtMost(50.0)
    }

    @Test
    fun `FASE 2 - il carb cycling assegna HIGH MEDIUM LOW e rispetta il numero di giorni`() = runTest {
        generate()
        val plan = nutritionRepository.getActivePlan()!!
        val counts = plan.days.groupingBy { it.dayType }.eachCount()
        assertThat(counts[com.fitcoach.app.domain.model.CarbDayType.HIGH] ?: 0).isEqualTo(plan.highCarbDays)
        assertThat(counts[com.fitcoach.app.domain.model.CarbDayType.MEDIUM] ?: 0).isEqualTo(plan.mediumCarbDays)
        assertThat(counts[com.fitcoach.app.domain.model.CarbDayType.LOW] ?: 0).isEqualTo(plan.lowCarbDays)
        assertThat(plan.highCarbDays + plan.mediumCarbDays + plan.lowCarbDays).isEqualTo(7)
    }

    @Test
    fun `FASE 2 - i giorni di allenamento non sono LOW carb`() = runTest {
        generate()
        val plan = nutritionRepository.getActivePlan()!!
        val trainingDays = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
        val wrong = plan.days.filter {
            it.dayIndex in trainingDays && it.dayType == com.fitcoach.app.domain.model.CarbDayType.LOW
        }
        assertThat(wrong.map { it.dayIndex }).isEmpty()
    }

    @Test
    fun `FASE 2 - le calorie LOW non scendono sotto la soglia di sicurezza`() = runTest {
        val energy = EnergyCalculator.compute(profile)!!
        val floor = CarbCyclingEngine.safetyFloor(profile, energy)
        val cycling = CarbCyclingEngine.build(profile, energy)
        cycling.targets.values.forEach { t ->
            assertThat(t.kcal).isAtLeast(floor - 1.0)
        }
    }

    @Test
    fun `FASE 2 - ogni target di giorno e coerente tra kcal e macro`() = runTest {
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        cycling.targets.values.forEach { t ->
            assertThat(CarbCyclingEngine.validateCoherence(t)).isAtMost(CarbCyclingEngine.KCAL_TOLERANCE)
        }
    }

    @Test
    fun `FASE 2 - la lista della spesa non e vuota con un piano attivo`() = runTest {
        generate()
        val weekStart = NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay())
        val regenerated = shoppingRepository.regenerateAndRead(weekStart)
        assertThat(regenerated.items).isNotEmpty()
        assertThat(shoppingRepository.itemsForWeek(weekStart)).isNotEmpty()
        // Ogni voce di spesa aggrega grammature reali del piano.
        assertThat(regenerated.items.all { it.totalGrams > 0.0 }).isTrue()
    }

    @Test
    fun `FASE 2 - la sostituzione alimentare mantiene i macro comparabili`() = runTest {
        generate()
        val plan = nutritionRepository.getActivePlan()!!
        // Quale fonte proteica capiti nel piano dipende dal fixture: cercare
        // "Petto di pollo" legava il test a un dettaglio irrilevante. Serve una
        // qualsiasi fonte proteica, e un'altra con lo stesso ruolo.
        val entry = plan.days.flatMap { it.meals }.flatMap { it.entries }
            .first { it.protein >= 15.0 }
        val original = foodRepository.getById(entry.foodItemId)!!
        val candidate = FoodSeedData.foods.first {
            it.id != original.id &&
                it.category == original.category &&
                it.proteinPer100g >= 15.0
        }
        val day = plan.days.first { d -> d.meals.any { m -> m.entries.any { it.id == entry.id } } }
        val preview = substitutionRepository.preview(entry.id, day.dayIndex, candidate)
        assertThat(preview).isNotNull()
        assertThat(preview!!.substituteGrams).isGreaterThan(0.0)
        // La sostituzione proposta deve restare vicina al macro dominante (proteine).
        assertThat(kotlin.math.abs(preview.substituteMacros.protein - entry.protein)).isAtMost(
            kotlin.math.max(6.0, entry.protein * 0.15)
        )
    }

    // ============================================================ FASE 3 - allenamento

    @Test
    fun `FASE 3 - la scheda rispetta i giorni disponibili e non ha sedute vuote`() = runTest {
        generate()
        val workout = workoutRepository.getActivePlan()!!
        val sessions = workout.days.filterNot { it.isRestDay }
        assertThat(sessions).hasSize(profile.trainingDaysPerWeek)
        assertThat(sessions.all { it.exercises.isNotEmpty() }).isTrue()
    }

    @Test
    fun `FASE 3 - ogni esercizio pianificato rispetta serie ripetizioni recupero e RIR`() = runTest {
        generate()
        workoutRepository.getActivePlan()!!.days.flatMap { it.exercises }.forEach { e ->
            assertThat(e.sets).isIn(1..6)
            assertThat(e.minReps).isAtLeast(1)
            assertThat(e.maxReps).isAtLeast(e.minReps)
            assertThat(e.restSeconds).isIn(15..600)
            assertThat(e.targetRir).isIn(0..5)
            assertThat(e.exerciseName).isNotEmpty()
        }
    }

    @Test
    fun `FASE 3 - nessun esercizio incompatibile con attrezzatura o limitazioni`() = runTest {
        generate()
        val catalog = exerciseRepository.getAll().associateBy { it.id }
        workoutRepository.getActivePlan()!!.days.flatMap { it.exercises }.forEach { planned ->
            val ex = catalog[planned.exerciseId]!!
            val compatible = EquipmentFit.isAvailable(ex, profile)
            assertThat(compatible).isTrue()
        }
    }

    @Test
    fun `FASE 3 - la scheda generata supera il validatore interno dell'app`() = runTest {
        generate()
        val workout = workoutRepository.getActivePlan()!!
        val catalog = exerciseRepository.getAll()
        val days = workout.days.filterNot { it.isRestDay }.map { d ->
            WorkoutPlanValidator.DayProposal(
                dayIndex = d.dayIndex,
                groups = d.muscleGroups,
                exercises = d.exercises.map { WorkoutPlanValidator.ExerciseProposal(it.exerciseId, it.sets) }
            )
        }
        val report = WorkoutPlanValidator.validate(profile, catalog, days)
        assertThat(report.violations).isEmpty()
    }

    @Test
    fun `FASE 3 - ogni seduta resta entro il numero massimo di serie (durate sessioni)`() = runTest {
        generate()
        workoutRepository.getActivePlan()!!.days.filterNot { it.isRestDay }.forEach { d ->
            assertThat(d.totalSets).isAtMost(30)
        }
    }

    // ============================================================ FASE 4 - check-in settimanale

    private suspend fun seedWeights(weeks: Int, startKg: Double, deltaPerWeek: Double) {
        val today = LocalDate.now().toEpochDay()
        for (w in 0 until weeks) {
            for (d in 0 until 3) {
                progressRepository.addWeight(
                    WeightRecord(dateEpochDay = today - w * 7L - d, weightKg = startKg + w * deltaPerWeek)
                )
            }
        }
    }

    private suspend fun seedDiary(weeks: Int) {
        val today = LocalDate.now().toEpochDay()
        val food = foodRepository.getAll().first { it.proteinPer100g >= 20.0 }
        for (w in 0 until weeks) {
            for (d in 0 until 7) {
                diaryRepository.addExtraFood(food, 150.0, MealSlot.LUNCH, "13:00", today - w * 7L - d)
            }
        }
    }

    @Test
    fun `FASE 4 - con pochi dati il check-in dichiara insufficienza e non propone modifiche`() = runTest {
        generate()
        val service = CollaudoCheckInWiring.checkInService(
            db, userRepository, progressRepository, nutritionRepository, diaryRepository,
            workoutRepository, chatRepository, changeRepository,
            ScriptedApi(mutableListOf(textResponse("Servono piu dati.")))
        )
        val outcome = com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase(service)()
        assertThat(outcome).isNotNull()
        assertThat(outcome!!.report.dataSufficient).isFalse()
        assertThat(outcome.proposals).isEmpty()
        assertThat(progressRepository.getCheckIns()).hasSize(1)
    }

    @Test
    fun `FASE 4 - una proposta di aggiustamento calorico resta in attesa finche non confermata`() = runTest {
        generate()
        seedWeights(4, 75.0, 0.2)
        seedDiary(2)
        val planId = nutritionRepository.getActivePlan()!!.id
        val originalWeekly = nutritionRepository.getActivePlan()!!.weeklyCalories
        val target = originalWeekly * 0.95

        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse(
                    "propose_calorie_adjustment",
                    CollaudoCheckInWiring.calorieArgs(planId, target)
                ),
                textResponse("Proposta pronta.")
            )
        )
        val service = CollaudoCheckInWiring.checkInService(
            db, userRepository, progressRepository, nutritionRepository, diaryRepository,
            workoutRepository, chatRepository, changeRepository, api
        )
        val executor = CollaudoCheckInWiring.actionExecutor(
            db, changeRepository, userRepository, chatRepository, foodRepository,
            exerciseRepository, nutritionRepository, workoutRepository, planVersionRepository
        )
        val outcome = com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase(service)()
        val change = outcome!!.proposals.single()

        assertThat(change.status).isEqualTo(com.fitcoach.app.domain.model.ChangeStatus.PROPOSED)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(0.5).of(originalWeekly)

        val applied = executor.apply(change.id)
        assertThat(applied).isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Applied::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(1.0).of(target)
    }

    @Test
    fun `FASE 4 - il rollback ripristina il piano precedente`() = runTest {
        generate()
        seedWeights(4, 75.0, 0.2)
        seedDiary(2)
        val planId = nutritionRepository.getActivePlan()!!.id
        val originalWeekly = nutritionRepository.getActivePlan()!!.weeklyCalories

        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse(
                    "propose_calorie_adjustment",
                    CollaudoCheckInWiring.calorieArgs(planId, originalWeekly * 0.95)
                ),
                textResponse("Proposta pronta.")
            )
        )
        val service = CollaudoCheckInWiring.checkInService(
            db, userRepository, progressRepository, nutritionRepository, diaryRepository,
            workoutRepository, chatRepository, changeRepository, api
        )
        val executor = CollaudoCheckInWiring.actionExecutor(
            db, changeRepository, userRepository, chatRepository, foodRepository,
            exerciseRepository, nutritionRepository, workoutRepository, planVersionRepository
        )
        val change = com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase(service)()!!.proposals.single()
        executor.apply(change.id)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isNotWithin(0.5).of(originalWeekly)

        val revert = executor.revert(change.id)
        assertThat(revert).isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Reverted::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(0.5).of(originalWeekly)
    }

    @Test
    fun `FASE 4 - una proposta rifiutata non e applicabile e non modifica il piano`() = runTest {
        generate()
        seedWeights(4, 75.0, 0.2)
        seedDiary(2)
        val planId = nutritionRepository.getActivePlan()!!.id
        val originalWeekly = nutritionRepository.getActivePlan()!!.weeklyCalories

        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse(
                    "propose_calorie_adjustment",
                    CollaudoCheckInWiring.calorieArgs(planId, originalWeekly * 0.95)
                ),
                textResponse("Proposta pronta.")
            )
        )
        val service = CollaudoCheckInWiring.checkInService(
            db, userRepository, progressRepository, nutritionRepository, diaryRepository,
            workoutRepository, chatRepository, changeRepository, api
        )
        val executor = CollaudoCheckInWiring.actionExecutor(
            db, changeRepository, userRepository, chatRepository, foodRepository,
            exerciseRepository, nutritionRepository, workoutRepository, planVersionRepository
        )
        val change = com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase(service)()!!.proposals.single()
        changeRepository.updateStatus(change.id, com.fitcoach.app.domain.model.ChangeStatus.REJECTED)

        assertThat(executor.apply(change.id))
            .isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Failed::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(0.5).of(originalWeekly)
    }

    // ============================================================ helper provider

    private fun textResponse(text: String) = com.fitcoach.app.support.textResponse(text)

    private fun toolCallResponse(name: String, args: String) = com.fitcoach.app.support.toolCallResponse(name, args)
}
