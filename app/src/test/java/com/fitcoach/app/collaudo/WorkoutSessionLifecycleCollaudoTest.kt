package com.fitcoach.app.collaudo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.notifications.NotificationScheduler
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.ui.workout.WorkoutViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Collaudo del ciclo di vita della SEDUTA dalla schermata Allenamento.
 *
 * Riproduce due difetti segnalati dall'utente:
 *  - "la pagina workout non permette di terminare la sessione se non si
 *    inseriscono gli esercizi terminati": una seduta avviata e poi abbandonata
 *    senza registrare serie deve poter essere CHIUSA, non restare bloccata
 *    per sempre IN_PROGRESS;
 *  - "non si riesce ad avviare il timer": il cronometro di recupero deve poter
 *    partire anche su richiesta esplicita dell'utente.
 *
 * NOTA: il cronometro usa tempo reale (System.currentTimeMillis) in un loop con
 * delay. Sotto runTest NON si usa advanceUntilIdle, che avanzerebbe il tempo
 * virtuale all'infinito facendo girare il loop per sempre. Si pompa invece lo
 * scheduler con runCurrent attendendo la condizione reale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkoutSessionLifecycleCollaudoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private lateinit var context: Context
    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var scheduler: NotificationScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        userRepository = UserRepository(db.userProfileDao())
        workoutRepository = WorkoutRepository(db)
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        scheduler = NotificationScheduler(context)
        kotlinx.coroutines.runBlocking {
            db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        }
    }

    @After
    fun tearDown() = db.close()

    private fun coordinator(): ProgramGenerationCoordinator {
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, FoodRepository(db.foodDao()), exerciseRepository,
            NutritionRepository(db), workoutRepository, PlanVersionRepository(db.planVersionDao()),
            db, json
        )
        val service = AiProgramGenerationService(
            CoachOrchestrator(
                AiClient(ScriptedApi(mutableListOf()), json),
                CoachToolRegistry(setOf(submitTool)),
                json
            ),
            ChatRepository(db.chatDao(), db.aiSettingsDao()),
            SecureKeyStore(context),
            json,
            FoodRepository(db.foodDao()),
            exerciseRepository
        )
        return ProgramGenerationCoordinator(service)
    }

    private fun buildViewModel() = WorkoutViewModel(
        userRepository = userRepository,
        workoutRepository = workoutRepository,
        exerciseRepository = exerciseRepository,
        generationCoordinator = coordinator(),
        notificationScheduler = scheduler,
        context = context,
    )

    /** Pompa lo scheduler e attende (reale) che la condizione sia vera. */
    private fun awaitCondition(
        scheduler: TestCoroutineScheduler,
        timeoutMs: Long = 4_000,
        condition: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            scheduler.runCurrent()
            if (condition()) return
            Thread.sleep(15)
        }
        scheduler.runCurrent()
    }

    // ---------------------------------------------------------------- Bug 2

    @Test
    fun `una sessione senza serie puo essere chiusa come abbandonata e non risulta completata`() =
        runTest {
            val epochDay = LocalDate.now().toEpochDay()
            val sessionId = workoutRepository.startFreeSession(epochDay)
            assertThat(workoutRepository.getSessionForDay(epochDay)?.status)
                .isEqualTo(SessionStatus.IN_PROGRESS)

            val closed = workoutRepository.abandonSession(sessionId)

            assertThat(closed).isTrue()
            val after = workoutRepository.getSessionForDay(epochDay)
            assertThat(after?.status).isEqualTo(SessionStatus.SKIPPED)
            assertThat(after?.status).isNotEqualTo(SessionStatus.IN_PROGRESS)
            assertThat(workoutRepository.completedSessionCount()).isEqualTo(0)
            // Una seduta abbandonata non produce volume ne performance.
            assertThat(after?.totalVolumeKg ?: 0.0).isEqualTo(0.0)
            assertThat(workoutRepository.getPerformanceByExercise()).isEmpty()
        }

    @Test
    fun `una sessione con serie valide viene completata`() = runTest {
        val epochDay = LocalDate.now().toEpochDay()
        val sessionId = workoutRepository.startFreeSession(epochDay)
        val exercise = exerciseRepository.getAll().first()
        workoutRepository.saveSet(
            sessionId,
            WorkoutSet(
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setIndex = 0,
                plannedReps = 8,
                actualReps = 8,
                weightKg = 70.0,
                completed = true,
            ),
        )

        workoutRepository.abandonSession(sessionId)

        assertThat(workoutRepository.getSessionForDay(epochDay)?.status)
            .isEqualTo(SessionStatus.COMPLETED)
    }

    @Test
    fun `il ViewModel chiude la sessione senza serie invece di bloccarla`() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val epochDay = LocalDate.now().toEpochDay()
            kotlinx.coroutines.runBlocking { workoutRepository.startFreeSession(epochDay) }
            val vm = buildViewModel()

            awaitCondition(dispatcher.scheduler) {
                vm.state.value.session?.status == SessionStatus.IN_PROGRESS
            }
            vm.openFinishDialog()
            vm.completeSession()

            awaitCondition(dispatcher.scheduler) {
                vm.state.value.session?.status != SessionStatus.IN_PROGRESS
            }

            assertThat(vm.state.value.session?.status).isNotEqualTo(SessionStatus.IN_PROGRESS)
            val persisted = kotlinx.coroutines.runBlocking {
                workoutRepository.getSessionForDay(epochDay)?.status
            }
            assertThat(persisted).isEqualTo(SessionStatus.SKIPPED)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ---------------------------------------------------------------- Bug 3

    @Test
    fun `il cronometro di recupero parte su richiesta dell'utente`() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val vm = buildViewModel()
            dispatcher.scheduler.runCurrent()

            assertThat(vm.state.value.resting).isFalse()

            vm.startManualRest()

            assertThat(vm.state.value.resting).isTrue()
            assertThat(vm.state.value.restRemainingSeconds).isGreaterThan(0)
            assertThat(vm.state.value.restReason).isNotEmpty()

            vm.skipRest()
            assertThat(vm.state.value.resting).isFalse()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
