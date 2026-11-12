package dev.fitcoach.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.repository.ExerciseRepositoryImpl
import dev.fitcoach.app.data.repository.ProfileRepositoryImpl
import dev.fitcoach.app.data.repository.RoutineRepositoryImpl
import dev.fitcoach.app.data.repository.SessionRepositoryImpl
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.SetType
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.usecase.ProgramGenerator
import dev.fitcoach.app.domain.usecase.ProgressionEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Persists a real generated program in Room and checks the contract the UI relies
 * on: distinct sessions, a calendar that matches the program, and volume computed
 * only from the sets actually recorded.
 */
@RunWith(RobolectricTestRunner::class)
class ProgramPersistenceIntegrationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var profileRepo: ProfileRepositoryImpl
    private lateinit var exerciseRepo: ExerciseRepositoryImpl
    private lateinit var routineRepo: RoutineRepositoryImpl
    private lateinit var sessionRepo: SessionRepositoryImpl

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
    }

    @After
    fun tearDown() = db.close()

    private fun profile(days: Int, level: ExperienceLevel = ExperienceLevel.INTERMEDIATE) = UserProfile(
        name = "Test",
        age = 32,
        heightCm = 180.0,
        weightKg = 80.0,
        goal = Goal.MUSCLE_GAIN,
        experience = level,
        equipment = "BARBELL,DUMBBELL,MACHINE,CABLE,BODYWEIGHT",
        availableDays = days,
        sessionMinutes = 60
    )

    private suspend fun persist(days: Int, level: ExperienceLevel = ExperienceLevel.INTERMEDIATE): List<Long> {
        exerciseRepo.ensureSeeded()
        val catalog = exerciseRepo.getAll()
        val generated = ProgramGenerator.generate(profile(days, level), catalog)
        val (routines, allExercises) = ProgramGenerator.toRoutines(0L, "Programma $days", generated, createdAt = 1L)
        val sizes = generated.days.map { it.exercises.size }
        val perRoutine = mutableListOf<List<RoutineExercise>>()
        var cursor = 0
        sizes.forEach { count ->
            perRoutine += allExercises.subList(cursor, cursor + count)
            cursor += count
        }
        return routineRepo.saveProgram("Programma $days", routines, perRoutine)
    }

    @Test
    fun `four days are persisted as four distinct routines with their own exercises`() = runBlocking {
        val ids = persist(4)
        assertEquals(4, ids.size)
        assertEquals(ids.size, ids.distinct().size)

        val program = routineRepo.getActiveProgram()
        assertEquals(4, program.size)
        assertEquals(listOf(0, 1, 2, 3), program.map { it.dayIndex })
        assertEquals(4, program.map { it.dayLabel }.distinct().size)
        assertTrue(program.all { it.programId != 0L })

        // Every session must have its own exercise list.
        val signatures = program.map { routine ->
            routineRepo.getExercises(routine.id).map { it.exerciseId }.sorted()
        }
        assertEquals(4, signatures.distinct().size)
        program.forEach { routine ->
            assertTrue(routineRepo.getExercises(routine.id).isNotEmpty())
        }
    }

    @Test
    fun `the active program observer returns every day of the program`() = runBlocking {
        persist(3)
        val observed = routineRepo.observeActiveProgram().first()
        assertEquals(3, observed.size)
        assertEquals(listOf(0, 1, 2), observed.map { it.dayIndex })
    }

    @Test
    fun `regenerating replaces the previous program instead of stacking days`() = runBlocking {
        persist(4)
        assertEquals(4, routineRepo.getActiveProgram().size)

        val ids = persist(3)
        val program = routineRepo.getActiveProgram()

        assertEquals(3, program.size)
        assertEquals(3, ids.size)
        // The old days must be gone: no routine from the previous program survives.
        assertEquals(3, routineRepo.observeRoutines().first().count { it.programId != 0L })
        assertEquals(0, ids.toSet().intersect(routineRepo.observeRoutines().first().map { it.id }.toSet() - ids.toSet()).size)
    }

    @Test
    fun `a single routine is never part of a program`() = runBlocking {
        exerciseRepo.ensureSeeded()
        val exercise = exerciseRepo.getAll().first()
        val id = routineRepo.saveRoutineWithExercises(
            dev.fitcoach.app.domain.model.Routine(name = "Scheda manuale"),
            listOf(
                RoutineExercise(routineId = 0L, exerciseId = exercise.id, orderIndex = 0)
            )
        )
        val routine = routineRepo.getById(id)!!
        assertEquals(0L, routine.programId)
        assertTrue(routineRepo.getActiveProgram().isEmpty())
    }

    @Test
    fun `calendar sessions match the generated program`() = runBlocking {
        val ids = persist(4)
        val program = routineRepo.getActiveProgram()
        val start = DateUtils.startOfWeek()

        // Mirrors the planner used by the view model.
        val spacing = (7 / program.size).coerceAtLeast(1)
        program.forEachIndexed { index, routine ->
            sessionRepo.createSession(
                WorkoutSession(
                    routineId = routine.id,
                    name = routine.dayLabel,
                    dateEpochDay = start.plusDays((index * spacing).toLong()).toEpochDay(),
                    startedAt = 1L,
                    status = SessionStatus.PLANNED
                )
            )
        }

        val planned = sessionRepo.getAll().filter { it.status == SessionStatus.PLANNED }
        assertEquals(4, planned.size)
        assertEquals(4, planned.map { it.name }.distinct().size)
        assertEquals(ids.toSet(), planned.mapNotNull { it.routineId }.toSet())
        // The calendar must not show one generic name for every entry.
        assertTrue(planned.none { it.name.contains("Full Body", ignoreCase = true) })
        assertTrue(planned.all { it.dateEpochDay in start.toEpochDay()..(start.toEpochDay() + 6) })
    }

    @Test
    fun `a completed session with recorded sets keeps a real volume`() = runBlocking {
        persist(2)
        val routine = routineRepo.getActiveProgram().first()
        val exerciseId = routineRepo.getExercises(routine.id).first().exerciseId

        val sessionId = sessionRepo.createSession(
            WorkoutSession(
                routineId = routine.id,
                name = routine.dayLabel,
                dateEpochDay = DateUtils.today().toEpochDay(),
                startedAt = 1L,
                status = SessionStatus.IN_PROGRESS
            )
        )
        (1..3).forEach { index ->
            sessionRepo.upsertSet(
                LoggedSet(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    orderIndex = 0,
                    setIndex = index,
                    setType = SetType.WORKING,
                    weightKg = 60.0,
                    reps = 10,
                    rir = 2,
                    completed = true,
                    loggedAt = index.toLong()
                )
            )
        }
        sessionRepo.updateSession(
            sessionRepo.getById(sessionId)!!.copy(status = SessionStatus.COMPLETED, finishedAt = 2L)
        )

        val sets = sessionRepo.getSets(sessionId)
        assertEquals(3, sets.count { it.completed })
        assertEquals(1800.0, ProgressionEngine.volume(sets), 0.001)
    }

    @Test
    fun `an unfinished set never contributes to the volume`() = runBlocking {
        persist(2)
        val routine = routineRepo.getActiveProgram().first()
        val exerciseId = routineRepo.getExercises(routine.id).first().exerciseId
        val sessionId = sessionRepo.createSession(
            WorkoutSession(
                routineId = routine.id,
                name = routine.dayLabel,
                dateEpochDay = DateUtils.today().toEpochDay(),
                startedAt = 1L,
                status = SessionStatus.IN_PROGRESS
            )
        )
        sessionRepo.upsertSet(
            LoggedSet(
                sessionId = sessionId, exerciseId = exerciseId, orderIndex = 0, setIndex = 1,
                setType = SetType.WORKING, weightKg = 100.0, reps = 10, completed = false
            )
        )
        assertEquals(0.0, ProgressionEngine.volume(sessionRepo.getSets(sessionId)), 0.001)
    }

    @Test
    fun `sets are attached to the exercise they belong to`() = runBlocking {
        persist(2)
        val routine = routineRepo.getActiveProgram().first()
        val items = routineRepo.getExercises(routine.id)
        val first = items[0].exerciseId
        val second = items.getOrNull(1)?.exerciseId ?: return@runBlocking

        val sessionId = sessionRepo.createSession(
            WorkoutSession(
                routineId = routine.id, name = routine.dayLabel,
                dateEpochDay = 1L, startedAt = 1L, status = SessionStatus.IN_PROGRESS
            )
        )
        sessionRepo.upsertSet(
            LoggedSet(
                sessionId = sessionId, exerciseId = first, orderIndex = 0, setIndex = 1,
                setType = SetType.WORKING, weightKg = 40.0, reps = 8, completed = true
            )
        )
        sessionRepo.upsertSet(
            LoggedSet(
                sessionId = sessionId, exerciseId = second, orderIndex = 1, setIndex = 1,
                setType = SetType.WORKING, weightKg = 80.0, reps = 5, completed = true
            )
        )

        val sets = sessionRepo.getSets(sessionId)
        assertEquals(1, sets.count { it.exerciseId == first })
        assertEquals(1, sets.count { it.exerciseId == second })
        assertEquals(320.0, ProgressionEngine.volume(sets.filter { it.exerciseId == first }), 0.001)
        assertEquals(400.0, ProgressionEngine.volume(sets.filter { it.exerciseId == second }), 0.001)
    }

    @Test
    fun `data survives closing and reopening the session view`() = runBlocking {
        persist(2)
        val routine = routineRepo.getActiveProgram().first()
        val exerciseId = routineRepo.getExercises(routine.id).first().exerciseId
        val sessionId = sessionRepo.createSession(
            WorkoutSession(
                routineId = routine.id, name = routine.dayLabel,
                dateEpochDay = 5L, startedAt = 1L, status = SessionStatus.IN_PROGRESS
            )
        )
        sessionRepo.upsertSet(
            LoggedSet(
                sessionId = sessionId, exerciseId = exerciseId, orderIndex = 0, setIndex = 1,
                setType = SetType.WORKING, weightKg = 55.0, reps = 12, completed = true
            )
        )
        // A fresh read, as the screen does when it is reopened.
        val reloaded = sessionRepo.getSets(sessionId)
        assertEquals(1, reloaded.size)
        assertEquals(660.0, ProgressionEngine.volume(reloaded), 0.001)
        assertNotNull(sessionRepo.getById(sessionId))
    }

    @Test
    fun `a planned session is not counted as a completed one`() = runBlocking {
        persist(3)
        routineRepo.getActiveProgram().forEachIndexed { index, routine ->
            sessionRepo.createSession(
                WorkoutSession(
                    routineId = routine.id, name = routine.dayLabel,
                    dateEpochDay = index.toLong(), startedAt = 1L, status = SessionStatus.PLANNED
                )
            )
        }
        val all = sessionRepo.getAll()
        assertEquals(3, all.size)
        assertTrue(all.none { it.status == SessionStatus.COMPLETED })
        assertNull(sessionRepo.getInProgress())
    }
}
