package dev.fitcoach.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.fitcoach.app.data.backup.BackupRepositoryImpl
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.local.entity.DiaryEntryEntity
import dev.fitcoach.app.data.local.entity.ExerciseEntity
import dev.fitcoach.app.data.local.entity.FoodEntity
import dev.fitcoach.app.data.local.entity.LoggedSetEntity
import dev.fitcoach.app.data.local.entity.MeasurementEntity
import dev.fitcoach.app.data.local.entity.ProfileEntity
import dev.fitcoach.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var repository: BackupRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BackupRepositoryImpl(db, json)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `export then import restores the same data`() = runBlocking {
        db.profileDao().upsert(
            ProfileEntity(
                name = "Luca", age = 32, sex = "MALE", heightCm = 178.0, weightKg = 82.0,
                bodyFatPercent = 18.0, goal = "MUSCLE_GAIN", experience = "INTERMEDIATE",
                trainingYears = 3.0, consistencyWeeks = 12, knownLoads = "panca 80", jobActivity = "DESK",
                dailySteps = 8000, sleepHours = 7.5, stressLevel = 3, recoveryLevel = 4, injuries = "",
                equipment = "FULL_GYM", availableDays = 4, sessionMinutes = 70, dietPreference = "OMNIVORE",
                allergies = "noci", mealsPerDay = 4, mealTimes = "8-13-17-21", weeklyBudget = 90.0,
                cookingMinutes = 30, likedFoods = "pollo", dislikedFoods = "fegato", unitSystem = "METRIC",
                onboardingCompleted = true, updatedAt = 1L
            )
        )
        val exerciseId = db.exerciseDao().insert(
            ExerciseEntity(
                name = "Panca piana", primaryMuscle = "CHEST", secondaryMuscles = "TRICEPS",
                equipment = "BARBELL", pattern = "HORIZONTAL_PUSH", execution = "spingi",
                setup = "impugna", commonMistakes = "rimbalzo", isCustom = false, isFavorite = true
            )
        )
        val foodId = db.foodDao().insert(
            FoodEntity(
                name = "Riso basmati", brand = "", category = "Cereali", kcalPer100g = 130.0,
                proteinPer100g = 2.7, carbsPer100g = 28.0, fatPer100g = 0.3, fiberPer100g = 0.4,
                source = "MANUAL", barcode = null, state = "RAW", pricePerKg = 2.5, isCustom = false
            )
        )
        db.measurementDao().insert(
            MeasurementEntity(type = "WEIGHT", customLabel = "", dateEpochDay = 20000L, value = 82.0,
                notes = "mattina", createdAt = 5L)
        )
        val sessionId = db.sessionDao().insert(
            WorkoutSessionEntity(routineId = null, name = "Push", dateEpochDay = 20000L, startedAt = 1L,
                finishedAt = 2L, status = "COMPLETED", notes = "ok")
        )
        db.loggedSetDao().insert(
            LoggedSetEntity(sessionId = sessionId, exerciseId = exerciseId, orderIndex = 0, setIndex = 1,
                setType = "WORKING", weightKg = 80.0, reps = 8, rir = 2, rpe = null, restSeconds = 120,
                pain = false, completed = true, notes = "", loggedAt = 10L)
        )
        db.diaryDao().insert(
            DiaryEntryEntity(dateEpochDay = 20000L, mealSlot = "LUNCH", foodId = foodId, grams = 150.0,
                consumed = true, planItemId = null, loggedAt = 11L)
        )

        val exported = repository.exportSnapshot()
        assertTrue(exported.contains("Panca piana"))
        assertTrue(exported.contains("Riso basmati"))

        // Wipe then re-import
        db.clearAllTables()
        assertEquals(0, db.exerciseDao().count())
        assertEquals(0, db.foodDao().count())

        val result = repository.importSnapshot(exported)
        assertTrue(result.isSuccess)

        val restoredProfile = db.profileDao().get()!!
        assertEquals("Luca", restoredProfile.name)
        assertEquals(82.0, restoredProfile.weightKg!!, 0.001)
        assertEquals("noci", restoredProfile.allergies)

        val restoredExercise = db.exerciseDao().getAll().first { it.name == "Panca piana" }
        assertEquals("CHEST", restoredExercise.primaryMuscle)
        assertTrue(restoredExercise.isFavorite)

        val restoredSets = db.loggedSetDao().getAll()
        assertEquals(1, restoredSets.size)
        assertEquals(restoredExercise.id, restoredSets.first().exerciseId)
        assertEquals(80.0, restoredSets.first().weightKg!!, 0.001)

        val restoredDiary = db.diaryDao().getAll()
        assertEquals(1, restoredDiary.size)
        val restoredFood = db.foodDao().getAll().first { it.name == "Riso basmati" }
        assertEquals(restoredFood.id, restoredDiary.first().foodId)
        assertEquals(150.0, restoredDiary.first().grams, 0.001)

        assertEquals(1, db.measurementDao().getAll().size)
        assertEquals(1, db.sessionDao().getAll().size)
    }

    @Test
    fun `import rejects malformed content without wiping data`() = runBlocking {
        db.exerciseDao().insert(
            ExerciseEntity(name = "Squat", primaryMuscle = "QUADS", secondaryMuscles = "",
                equipment = "BARBELL", pattern = "SQUAT", execution = "scendi", setup = "bilanciere",
                commonMistakes = "ginocchia in dentro", isCustom = false, isFavorite = false)
        )
        val result = repository.importSnapshot("{ questo non e json valido")
        assertTrue(result.isFailure)
        assertEquals(1, db.exerciseDao().count())
    }
}
