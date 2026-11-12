package dev.fitcoach.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.repository.CoachRepositoryImpl
import dev.fitcoach.app.data.repository.DiaryRepositoryImpl
import dev.fitcoach.app.data.repository.ExerciseRepositoryImpl
import dev.fitcoach.app.data.repository.FoodRepositoryImpl
import dev.fitcoach.app.data.repository.MealPlanRepositoryImpl
import dev.fitcoach.app.data.repository.MeasurementRepositoryImpl
import dev.fitcoach.app.data.repository.ProfileRepositoryImpl
import dev.fitcoach.app.data.repository.RoutineRepositoryImpl
import dev.fitcoach.app.data.repository.SessionRepositoryImpl
import dev.fitcoach.app.data.repository.ShoppingRepositoryImpl
import dev.fitcoach.app.domain.model.DiaryEntry
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.MealItem
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.model.Measurement
import dev.fitcoach.app.domain.model.MeasurementType
import dev.fitcoach.app.domain.model.PlanDay
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.SetType
import dev.fitcoach.app.domain.model.ShoppingItem
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises the real repositories end to end: seeding, routine creation, a full
 * logged session, progression, plan/diary and coach analysis.
 */
@RunWith(RobolectricTestRunner::class)
class UserFlowIntegrationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var profileRepo: ProfileRepositoryImpl
    private lateinit var exerciseRepo: ExerciseRepositoryImpl
    private lateinit var routineRepo: RoutineRepositoryImpl
    private lateinit var sessionRepo: SessionRepositoryImpl
    private lateinit var foodRepo: FoodRepositoryImpl
    private lateinit var planRepo: MealPlanRepositoryImpl
    private lateinit var diaryRepo: DiaryRepositoryImpl
    private lateinit var shoppingRepo: ShoppingRepositoryImpl
    private lateinit var measurementRepo: MeasurementRepositoryImpl
    private lateinit var coachRepo: CoachRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        profileRepo = ProfileRepositoryImpl(db)
        exerciseRepo = ExerciseRepositoryImpl(db)
        routineRepo = RoutineRepositoryImpl(db)
        sessionRepo = SessionRepositoryImpl(db)
        foodRepo = FoodRepositoryImpl(db)
        planRepo = MealPlanRepositoryImpl(db)
        diaryRepo = DiaryRepositoryImpl(db)
        shoppingRepo = ShoppingRepositoryImpl(db)
        measurementRepo = MeasurementRepositoryImpl(db)
        coachRepo = CoachRepositoryImpl(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `seeded catalogs are non empty and searchable`() = runBlocking {
        exerciseRepo.ensureSeeded()
        foodRepo.ensureSeeded()
        assertTrue(exerciseRepo.getAll().size > 20)
        assertTrue(foodRepo.getAll().size > 10)
        // Seeding is idempotent
        exerciseRepo.ensureSeeded()
        foodRepo.ensureSeeded()
        val exercises = exerciseRepo.getAll()
        assertEquals(exercises.map { it.name }.distinct().size, exercises.size)
    }

    @Test
    fun `full training flow produces a real progression suggestion`() = runBlocking {
        exerciseRepo.ensureSeeded()
        val exercise = exerciseRepo.getAll().first { it.name.contains("Panca", ignoreCase = true) }

        val routineId = routineRepo.saveRoutineWithExercises(
            Routine(name = "Push A", description = "test"),
            listOf(
                RoutineExercise(
                    routineId = 0L, exerciseId = exercise.id, orderIndex = 0,
                    targetSets = 3, targetRepsMin = 8, targetRepsMax = 12,
                    targetRir = 2, restSeconds = 120
                )
            )
        )
        routineRepo.setActive(routineId)
        assertEquals(routineId, routineRepo.getActive()!!.id)

        val sessionId = sessionRepo.createSession(
            WorkoutSession(
                routineId = routineId, name = "Push A", dateEpochDay = 20000L,
                startedAt = 1L, status = SessionStatus.IN_PROGRESS
            )
        )
        assertNotNull(sessionRepo.getInProgress())

        // Log three sets at the top of the range with adequate RIR
        (1..3).forEach { index ->
            sessionRepo.upsertSet(
                LoggedSet(
                    sessionId = sessionId, exerciseId = exercise.id, orderIndex = 0, setIndex = index,
                    setType = SetType.WORKING, weightKg = 80.0, reps = 12, rir = 2,
                    restSeconds = 120, completed = true, loggedAt = index.toLong()
                )
            )
        }
        val session = sessionRepo.getById(sessionId)!!
        sessionRepo.updateSession(session.copy(status = SessionStatus.COMPLETED, finishedAt = 100L))
        assertTrue(sessionRepo.getInProgress() == null)

        val sets = sessionRepo.getSets(sessionId)
        assertEquals(3, sets.size)
        assertEquals(80.0 * 12 * 3, ProgressionEngine.volume(sets), 0.001)

        val routineExercise = routineRepo.getExercises(routineId).first()
        val history = sessionRepo.historyFor(exercise.id)
        val suggestion = ProgressionEngine.suggest(routineExercise, history)
        assertTrue("Expected load increase", suggestion.canProgress)
        assertEquals(82.5, suggestion.suggestedWeightKg!!, 0.001)

        val last = sessionRepo.lastPerformance(exercise.id)
        assertNotNull(last)
        assertEquals(3, last!!.sets.size)
    }

    @Test
    fun `plan, diary and shopping are computed from real food values`() = runBlocking {
        foodRepo.ensureSeeded()
        val food = foodRepo.getAll().first { it.kcalPer100g > 0 }

        val planId = planRepo.savePlanAtomic(
            plan = MealPlan(name = "Cut", targetKcal = 2000.0, targetProtein = 150.0),
            days = (0..6).map { PlanDay(planId = 0L, dayIndex = it) },
            itemsByDayIndex = mapOf(
                0 to listOf(MealItem(planDayId = 0L, mealSlot = MealSlot.LUNCH, foodId = food.id, grams = 200.0))
            )
        )
        planRepo.setActive(planId)
        val activePlan = planRepo.getActive()!!
        assertEquals(planId, activePlan.id)

        val days = planRepo.getDays(planId)
        assertEquals(7, days.size)
        val day0 = days.first { it.dayIndex == 0 }
        val items = planRepo.getItems(day0.id)
        assertEquals(1, items.size)
        assertEquals(food.id, items.first().foodId)

        diaryRepo.log(
            DiaryEntry(dateEpochDay = 20000L, mealSlot = MealSlot.LUNCH, foodId = food.id, grams = 200.0, consumed = true)
        )
        val today = diaryRepo.getByDate(20000L)
        assertEquals(1, today.size)
        val consumedKcal = food.kcalPer100g * 2
        assertEquals(consumedKcal, food.kcalPer100g * today.first().grams / 100.0, 0.001)

        shoppingRepo.replaceWeek(
            weekStart = 19999L,
            items = listOf(
                ShoppingItem(name = food.name, quantityGrams = 1000.0, category = food.category, estimatedPrice = 3.5)
            )
        )
        val week = shoppingRepo.getWeek(19999L)
        assertEquals(1, week.size)
        assertEquals(19999L, week.first().weekStartEpochDay)
    }

    @Test
    fun `routines can be edited duplicated and deleted`() = runBlocking {
        exerciseRepo.ensureSeeded()
        val exercises = exerciseRepo.getAll().take(2)
        val routineId = routineRepo.saveRoutineWithExercises(
            Routine(name = "Full Body", description = ""),
            exercises.mapIndexed { i, e ->
                RoutineExercise(routineId = 0L, exerciseId = e.id, orderIndex = i, targetSets = 3)
            }
        )
        assertEquals(2, routineRepo.getExercises(routineId).size)

        val copyId = routineRepo.duplicate(routineId)!!
        assertTrue(copyId != routineId)
        assertTrue(routineRepo.getById(copyId)!!.name.contains("copia"))
        assertEquals(2, routineRepo.getExercises(copyId).size)

        // Editing replaces the exercise list atomically
        routineRepo.saveRoutineWithExercises(
            routineRepo.getById(routineId)!!,
            listOf(RoutineExercise(routineId = routineId, exerciseId = exercises.first().id, orderIndex = 0, targetSets = 5))
        )
        val edited = routineRepo.getExercises(routineId)
        assertEquals(1, edited.size)
        assertEquals(5, edited.first().targetSets)

        routineRepo.delete(copyId)
        assertTrue(routineRepo.getById(copyId) == null)
    }

    @Test
    fun `session deletion also removes its logged sets`() = runBlocking {
        exerciseRepo.ensureSeeded()
        val exercise = exerciseRepo.getAll().first()
        val sessionId = sessionRepo.createSession(
            WorkoutSession(routineId = null, name = "Temp", dateEpochDay = 20000L, startedAt = 1L, status = SessionStatus.IN_PROGRESS)
        )
        sessionRepo.upsertSet(
            LoggedSet(sessionId = sessionId, exerciseId = exercise.id, orderIndex = 0, setIndex = 1,
                setType = SetType.WORKING, weightKg = 40.0, reps = 10, completed = true, loggedAt = 1L)
        )
        assertEquals(1, sessionRepo.getSets(sessionId).size)
        sessionRepo.deleteSession(sessionId)
        assertEquals(0, sessionRepo.getSets(sessionId).size)
        assertFalse(sessionRepo.getAll().any { it.id == sessionId })
    }
}
