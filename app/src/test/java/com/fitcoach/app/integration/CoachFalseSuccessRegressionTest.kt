package com.fitcoach.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.NutritionDayEntity
import com.fitcoach.app.data.local.entity.NutritionPlanEntity
import com.fitcoach.app.data.local.entity.PlannedExerciseEntity
import com.fitcoach.app.data.local.entity.WorkoutDayEntity
import com.fitcoach.app.data.local.entity.MesocycleEntity
import com.fitcoach.app.data.local.entity.WorkoutPlanEntity
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.ApplyResult
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regressione sui due difetti segnalati dall'utente.
 *
 * 1. Il coach rifiutava una riduzione calorica del 10% dichiarandola "non
 *    consentita": il piano ha calorie con decimali, quindi un -10% arrotondato
 *    cadeva appena sotto il rapporto 0.90 e veniva respinto.
 * 2. Il coach dichiarava di aver modificato la scheda senza che nulla cambiasse:
 *    l'UPDATE su un id inesistente tocca zero righe ma non falliva.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachFalseSuccessRegressionTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var executor: CoachActionExecutor

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val userRepository = UserRepository(db.userProfileDao())
        userRepository.save(
            UserProfile(
                id = 1L, name = "Test", ageYears = 34, sex = Sex.MALE,
                heightCm = 178.0, weightKg = 82.0, goal = Goal.RECOMP
            )
        )
        executor = CoachActionExecutor(
            db = db,
            changeRepository = ChangeRepository(db.programChangeDao()),
            userRepository = userRepository,
            generationCoordinator = Lazy { error("non usato in questo test") },
            json = json
        )
    }

    @After
    fun tearDown() = db.close()

    /** Il piano reale ha calorie con decimali: il caso che faceva fallire il -10%. */
    @Test
    fun `una riduzione del 10 per cento arrotondata viene accettata`() = runBlocking {
        val weekly = 17_637.769604
        seedNutritionPlan(weekly)
        val proposta = (weekly * 0.90).toInt().toDouble() // 15873, cioe' 0.899944 del piano

        val id = executor.registerProposal(calorieAction(proposta))
        val esito = executor.apply(id)

        assertTrue(
            "Una riduzione del 10% deve essere applicata, non rifiutata: $esito",
            esito is ApplyResult.Applied
        )
        val plan = db.nutritionPlanDao().getById(1L)!!
        assertEquals(proposta, plan.weeklyCalories, 0.5)
    }

    /** Oltre il limite la protezione deve restare attiva. */
    @Test
    fun `una riduzione eccessiva resta bloccata`() = runBlocking {
        val weekly = 17_637.769604
        seedNutritionPlan(weekly)
        val id = executor.registerProposal(calorieAction(weekly * 0.80))

        val esito = executor.apply(id)

        assertTrue(
            "Un -20% deve essere respinto: $esito",
            esito is ApplyResult.Failed
        )
        assertEquals(weekly, db.nutritionPlanDao().getById(1L)!!.weeklyCalories, 0.001)
    }

    /** Un id inventato dal modello non deve produrre un falso "modifica applicata". */
    @Test
    fun `sostituire un esercizio inesistente fallisce invece di dichiarare successo`() = runBlocking {
        seedWorkoutPlan()
        val id = executor.registerProposal(
            CoachAction(
                action = CoachAction.ACTION_SUBSTITUTE_EXERCISE,
                domain = ChangeDomain.WORKOUT.name,
                title = "Sostituzione",
                payload = mapOf(
                    "plannedId" to "9999",
                    "exerciseId" to "1",
                    "exerciseName" to "Panca piana con bilanciere"
                )
            )
        )

        val esito = executor.apply(id)

        assertTrue("Deve essere respinto, non applicato: $esito", esito is ApplyResult.Failed)
        val dopo = db.plannedExerciseDao().getForDay(1L)
        assertEquals("La scheda non deve cambiare", "Lat machine", dopo.first().exerciseName)
    }

    /** Con un id valido la sostituzione deve cambiare davvero la scheda. */
    @Test
    fun `sostituire un esercizio esistente aggiorna la scheda`() = runBlocking {
        seedWorkoutPlan()
        val plannedId = db.plannedExerciseDao().getForDay(1L).first().id
        val id = executor.registerProposal(
            CoachAction(
                action = CoachAction.ACTION_SUBSTITUTE_EXERCISE,
                domain = ChangeDomain.WORKOUT.name,
                title = "Sostituzione",
                payload = mapOf(
                    "plannedId" to plannedId.toString(),
                    "exerciseId" to "1",
                    "exerciseName" to "Panca piana con bilanciere"
                )
            )
        )

        val esito = executor.apply(id)

        assertTrue("Deve essere applicato: $esito", esito is ApplyResult.Applied)
        val dopo = db.plannedExerciseDao().getForDay(1L)
        assertEquals("Panca piana con bilanciere", dopo.first().exerciseName)
    }

    /** La tecnica d'intensita' su un id inesistente deve fallire, non fingere. */
    @Test
    fun `tecnica su esercizio inesistente viene respinta`() = runBlocking {
        seedWorkoutPlan()
        val id = executor.registerProposal(
            CoachAction(
                action = CoachAction.ACTION_SET_INTENSITY,
                domain = ChangeDomain.WORKOUT.name,
                title = "Tecnica",
                payload = mapOf(
                    "plannedId" to "8888",
                    "intensityTechnique" to IntensityTechnique.DROP_SET.name,
                    "techniqueReason" to "Aumentare lo stimolo"
                )
            )
        )

        val esito = executor.apply(id)

        assertTrue("Deve essere respinto: $esito", esito is ApplyResult.Failed)
    }

    private fun calorieAction(weekly: Double) = CoachAction(
        action = CoachAction.ACTION_ADJUST_CALORIES,
        domain = ChangeDomain.NUTRITION.name,
        title = "Aggiustamento calorie",
        payload = mapOf("weeklyKcal" to weekly.toString())
    )

    private suspend fun seedNutritionPlan(weekly: Double) {
        db.nutritionPlanDao().insert(
            NutritionPlanEntity(
                id = 1L,
                profileId = 1L,
                name = "Piano",
                weeklyCalories = weekly,
                bmr = 1795.924,
                tdee = 2738.7841,
                weeklyKcalBalance = 0.0,
                bmrFormula = "Mifflin-St Jeor",
                tdeeFormula = "Mifflin x PAL",
                highCarbDays = 3,
                mediumCarbDays = 2,
                lowCarbDays = 2,
                proteinGPerKg = 2.0
            )
        )
        db.nutritionDayDao().insertAll(
            listOf(
                NutritionDayEntity(
                    id = 1L, planId = 1L, dayIndex = 0, dayType = CarbDayType.HIGH,
                    targetKcal = weekly / 7.0,
                    targetProtein = 160.0, targetCarbs = 300.0, targetFat = 70.0
                )
            )
        )
    }

    private suspend fun seedWorkoutPlan() {
        db.workoutPlanDao().insert(
            WorkoutPlanEntity(id = 1L, profileId = 1L, name = "Scheda", splitType = SplitType.UPPER_LOWER)
        )
        db.mesocycleDao().insert(
            MesocycleEntity(id = 1L, planId = 1L, name = "Ipertrofia", focus = "Ipertrofia", weekCount = 5)
        )
        db.workoutDayDao().insertAll(
            listOf(
                WorkoutDayEntity(id = 1L, mesocycleId = 1L, dayIndex = 0, title = "Upper")
            )
        )
        db.plannedExerciseDao().insertAll(
            listOf(
                PlannedExerciseEntity(
                    id = 1L, workoutDayId = 1L, exerciseId = 1L, exerciseName = "Lat machine",
                    primaryMuscle = MuscleGroup.BACK, orderIndex = 0,
                    sets = 4, minReps = 8, maxReps = 12, restSeconds = 90, targetRir = 2
                )
            )
        )
    }
}
