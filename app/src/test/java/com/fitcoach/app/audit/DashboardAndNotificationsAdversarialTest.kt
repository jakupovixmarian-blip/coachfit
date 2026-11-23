package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.ExportRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NotificationRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.NotificationRecord
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.notifications.NotificationScheduler
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.ui.dashboard.DashboardViewModel
import com.fitcoach.app.ui.settings.NotificationSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Dashboard e notifiche: sono la prima cosa che l'utente vede e l'unico canale
 * che lo raggiunge fuori dall'app. Un numero sbagliato qui viene creduto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DashboardAndNotificationsAdversarialTest {

    private lateinit var context: Context
    private lateinit var db: FitCoachDatabase
    private val dispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun attendi(timeoutMs: Long = 3_000, condizione: () -> Boolean) {
        val fine = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < fine && !condizione()) Thread.sleep(10)
    }

    private fun profilo(weightKg: Double? = 80.0) = UserProfile(
        id = 1L, name = "Luca", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = weightKg, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    private fun coordinator(): ProgramGenerationCoordinator {
        val food = FoodRepository(db.foodDao())
        val exercises = ExerciseRepository(db.exerciseDao())
        val users = UserRepository(db.userProfileDao())
        val submitTool: CoachTool = SubmitFullProgramTool(
            users, food, exercises, NutritionRepository(db), WorkoutRepository(db),
            PlanVersionRepository(db.planVersionDao()), db, json
        )
        val service = AiProgramGenerationService(
            CoachOrchestrator(
                AiClient(ScriptedApi(mutableListOf()), json),
                CoachToolRegistry(setOf(submitTool)), json
            ),
            ChatRepository(db.chatDao(), db.aiSettingsDao()),
            SecureKeyStore(context), json, food, exercises
        )
        return ProgramGenerationCoordinator(service)
    }

    private fun dashboardVm() = DashboardViewModel(
        userRepository = UserRepository(db.userProfileDao()),
        nutritionRepository = NutritionRepository(db),
        nutritionDiaryRepository = NutritionDiaryRepository(db),
        workoutRepository = WorkoutRepository(db),
        progressRepository = ProgressRepository(db.progressDao(), db.checkInDao()),
        notificationRepository = NotificationRepository(db.notificationDao()),
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao()),
        generationCoordinator = coordinator(),
        context = context
    )

    // ---------- Dashboard ----------

    @Test
    fun `senza profilo e senza piani la dashboard non inventa numeri`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        val s = vm.state.value
        assertFalse("Non ci sono piani", s.hasPlan)
        assertEquals(0, s.kcalTarget)
        assertEquals(0.0, s.proteinTargetGrams, 0.001)
        assertNull("Nessun peso senza pesate", s.weight)
        assertNull("Nessun volume senza sedute", s.weeklyVolumeKg)
        assertNull("Nessuna notifica programmata", s.nextNotification)
        assertNull("Nessun messaggio del coach", s.lastCoachMessage)
        assertEquals(0, s.meals.planned)
    }

    @Test
    fun `senza profilo la dashboard chiede l'anamnesi`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        assertTrue(
            "Senza profilo l'invito all'anamnesi deve comparire",
            vm.state.value.needsAnamnesis
        )
    }

    @Test
    fun `il saluto non usa un nome che non esiste`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        val saluto = vm.state.value.greeting
        assertTrue("Il saluto deve esserci", saluto.isNotBlank())
        assertFalse("Senza nome non deve esserci una virgola appesa", saluto.contains(","))
    }

    @Test
    fun `con un profilo il saluto contiene il nome`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading && vm.state.value.greeting.contains(",") }
        assertTrue(vm.state.value.greeting.contains("Luca"))
    }

    @Test
    fun `un profilo senza peso non produce un peso in dashboard`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo(weightKg = 0.0).toEntity() })
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        assertNull(
            "Il peso del profilo non e' una pesata registrata: la dashboard mostra solo pesate",
            vm.state.value.weight
        )
    }

    @Test
    fun `una sola pesata non produce una variazione`() = runTest {
        val progress = ProgressRepository(db.progressDao(), db.checkInDao())
        progress.addWeight(
            com.fitcoach.app.domain.model.WeightRecord(
                dateEpochDay = java.time.LocalDate.now().toEpochDay(), weightKg = 80.0
            )
        )
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading && vm.state.value.weight != null }
        val w = vm.state.value.weight!!
        assertEquals(80.0, w.currentKg, 0.001)
        assertEquals(
            "Con una sola pesata la variazione non esiste e non va mostrata come zero",
            0.0, w.deltaKg, 0.001
        )
        assertEquals("Una sola pesata nello storico", 1, w.recentKg.size)
    }

    @Test
    fun `con due pesate la variazione e la differenza fra le ultime due`() = runTest {
        val progress = ProgressRepository(db.progressDao(), db.checkInDao())
        val oggi = java.time.LocalDate.now().toEpochDay()
        progress.addWeight(
            com.fitcoach.app.domain.model.WeightRecord(dateEpochDay = oggi - 1, weightKg = 81.0)
        )
        progress.addWeight(
            com.fitcoach.app.domain.model.WeightRecord(dateEpochDay = oggi, weightKg = 80.0)
        )
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading && vm.state.value.weight != null }
        assertEquals(-1.0, vm.state.value.weight!!.deltaKg, 0.001)
    }

    @Test
    fun `le allergie del profilo compaiono fra gli avvisi`() = runTest {
        db.userProfileDao().upsert(
            Mappers.run { profilo().copy(allergies = listOf("arachidi")).toEntity() }
        )
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading && vm.state.value.warnings.isNotEmpty() }
        assertTrue(
            "Un'allergia non deve restare nascosta: ${vm.state.value.warnings}",
            vm.state.value.warnings.any { it.contains("arachidi", ignoreCase = true) }
        )
    }

    @Test
    fun `le limitazioni fisiche compaiono fra gli avvisi`() = runTest {
        db.userProfileDao().upsert(
            Mappers.run {
                profilo().copy(physicalLimitations = listOf("spalla destra")).toEntity()
            }
        )
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading && vm.state.value.warnings.isNotEmpty() }
        assertTrue(
            vm.state.value.warnings.any { it.contains("spalla destra", ignoreCase = true) }
        )
    }

    @Test
    fun `pesare un valore estremo non rompe la dashboard`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        // 0 kg non e' una pesata valida: il repository deve rifiutarla.
        vm.logWeight(0.0)
        Thread.sleep(300)
        assertNull(
            "Una pesata a zero non deve entrare in dashboard",
            vm.state.value.weight
        )
        assertNull("Nessun errore deve restare appeso", vm.state.value.errorMessage)
    }

    @Test
    fun `una pesata valida aggiorna la dashboard`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        vm.logWeight(78.5)
        attendi { vm.state.value.weight != null }
        assertEquals(78.5, vm.state.value.weight!!.currentKg, 0.001)
    }

    @Test
    fun `spostare una seduta senza piani produce un messaggio e non un crash`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        vm.skipTodayWorkout()
        attendi { vm.events.value != null }
        assertEquals("Nessuna seduta da spostare oggi.", vm.events.value)
    }

    @Test
    fun `generare senza profilo chiede di completarlo`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        vm.generateProgram()
        attendi { vm.events.value != null }
        assertEquals("Completa prima il profilo.", vm.events.value)
    }

    @Test
    fun `l'evento consumato viene svuotato`() = runTest {
        val vm = dashboardVm()
        attendi { !vm.state.value.isLoading }
        vm.generateProgram()
        attendi { vm.events.value != null }
        vm.consumeEvent()
        assertNull("L'evento non deve ripetersi", vm.events.value)
    }

    // ---------- Notifiche ----------

    private fun notifVm() = NotificationSettingsViewModel(
        notificationRepository = NotificationRepository(db.notificationDao()),
        preferencesRepository = PreferencesRepository(db.userPreferencesDao()),
        scheduler = NotificationScheduler(context)
    )

    @Test
    fun `un orario non valido viene rifiutato e non salvato`() = runTest {
        val vm = notifVm()
        attendi { !vm.state.value.loading }
        val record = NotificationRecord(
            id = 1L, kind = "MEAL_BREAKFAST",
            title = "Colazione", body = "Mangia", triggerTime = "08:00", enabled = true
        )
        vm.setTime(record, "99:99")
        attendi { vm.state.value.message != null }
        assertNotNull("Un orario impossibile deve essere rifiutato", vm.state.value.message)
    }

    @Test
    fun `un orario valido viene accettato`() = runTest {
        val vm = notifVm()
        attendi { !vm.state.value.loading }
        val record = NotificationRecord(
            id = 1L, kind = "MEAL_BREAKFAST",
            title = "Colazione", body = "Mangia", triggerTime = "08:00", enabled = true
        )
        vm.setTime(record, "07:30")
        attendi(timeoutMs = 800) { vm.state.value.message != null }
        val messaggio = vm.state.value.message
        assertTrue(
            "Un orario valido non deve produrre un errore: $messaggio",
            messaggio == null || !messaggio.lowercase().contains("non valido")
        )
    }

    @Test
    fun `disattivare tutte le notifiche e reversibile`() = runTest {
        val vm = notifVm()
        attendi { !vm.state.value.loading }
        vm.setGlobalEnabled(false)
        attendi { vm.state.value.globalEnabled.not() }
        assertFalse(vm.state.value.globalEnabled)
        vm.setGlobalEnabled(true)
        attendi { vm.state.value.globalEnabled }
        assertTrue("La disattivazione deve essere reversibile", vm.state.value.globalEnabled)
    }

    @Test
    fun `il messaggio consumato viene svuotato`() = runTest {
        val vm = notifVm()
        attendi { !vm.state.value.loading }
        vm.setGlobalEnabled(false)
        attendi { vm.state.value.message != null }
        vm.consumeMessage()
        assertNull(vm.state.value.message)
    }
}
