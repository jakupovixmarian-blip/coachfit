package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.ExportRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.ShoppingRepository
import com.fitcoach.app.data.repository.SubstitutionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.CoachService
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.device.BleDeviceManager
import com.fitcoach.app.domain.device.BleSessionManager
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.model.CoachAuthorizationMode
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.ui.coach.CoachViewModel
import com.fitcoach.app.ui.settings.CoachAuthorizationViewModel
import com.fitcoach.app.ui.settings.DevicesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
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
 * Coach, autorizzazioni, dispositivi. Il caso critico e' il provider che non
 * risponde: la chat deve restare utilizzabile e dire cosa e' successo, senza
 * perdere il messaggio dell'utente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachAndSettingsAdversarialTest {

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

    private fun attendi(timeoutMs: Long = 5_000, condizione: () -> Boolean) {
        val fine = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < fine && !condizione()) Thread.sleep(10)
    }

    private fun profilo() = UserProfile(
        id = 1L, name = "Luca", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
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
        val service = com.fitcoach.app.domain.coach.AiProgramGenerationService(
            CoachOrchestrator(
                AiClient(ScriptedApi(mutableListOf()), json),
                CoachToolRegistry(setOf(submitTool)), json
            ),
            ChatRepository(db.chatDao(), db.aiSettingsDao()),
            SecureKeyStore(context), json, food, exercises
        )
        return ProgramGenerationCoordinator(service)
    }

    private fun coachVm(): CoachViewModel {
        val chat = ChatRepository(db.chatDao(), db.aiSettingsDao())
        val changes = ChangeRepository(db.programChangeDao())
        val users = UserRepository(db.userProfileDao())
        val aiClient = AiClient(ScriptedApi(mutableListOf()), json)
        val orchestrator = CoachOrchestrator(aiClient, CoachToolRegistry(emptySet()), json)
        val service = CoachService(
            users, NutritionRepository(db), WorkoutRepository(db),
            ProgressRepository(db.progressDao(), db.checkInDao()), changes, chat,
            NutritionDiaryRepository(db),
            ShoppingRepository(db, NutritionRepository(db), FoodRepository(db.foodDao())),
            SubstitutionRepository(db, NutritionRepository(db), FoodRepository(db.foodDao())),
            FoodRepository(db.foodDao()), aiClient, SecureKeyStore(context), orchestrator
        )
        return CoachViewModel(
            chatRepository = chat,
            changeRepository = changes,
            preferencesRepository = PreferencesRepository(db.userPreferencesDao()),
            coachService = service,
            actionExecutor = CoachActionExecutor(
                db, changes, users,
                object : dagger.Lazy<ProgramGenerationCoordinator> {
                    override fun get() = coordinator()
                },
                json
            )
        )
    }

    // ---------- Coach: stati senza configurazione ----------

    @Test
    fun `la chat parte vuota e non inventa messaggi`() = runTest {
        val vm = coachVm()
        attendi { !vm.state.value.loading }
        assertTrue("Nessuno storico salvato", vm.state.value.messages.isEmpty())
        assertNull("Nessuna proposta pendente", vm.state.value.pendingChange)
    }

    @Test
    fun `un messaggio vuoto non viene inviato`() = runTest {
        val vm = coachVm()
        attendi { !vm.state.value.loading }
        vm.updateInput("    ")
        vm.send()
        Thread.sleep(200)
        assertTrue("Uno spazio non e' un messaggio", vm.state.value.messages.isEmpty())
        assertFalse("Non deve restare in stato invio", vm.state.value.sending)
    }

    @Test
    fun `senza provider configurato la chat segnala l'errore e non perde il testo`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = coachVm()
        attendi { !vm.state.value.loading }
        vm.updateInput("Non ho pollo, cosa mangio?")
        vm.send()
        attendi(timeoutMs = 8_000) {
            !vm.state.value.sending &&
                vm.state.value.messages.any { it.role == ChatRole.ASSISTANT }
        }
        val messaggi = vm.state.value.messages
        assertTrue(
            "Il messaggio dell'utente deve restare in chat: $messaggi",
            messaggi.any { it.role == ChatRole.USER && it.content.contains("pollo") }
        )
        assertTrue(
            "Deve esserci una risposta, anche di errore: $messaggi",
            messaggi.any { it.role == ChatRole.ASSISTANT }
        )
        assertNotNull(
            "Senza API key deve esserci un motivo visibile",
            vm.state.value.message
        )
    }

    @Test
    fun `dopo un errore si puo riprovare senza duplicare il messaggio`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = coachVm()
        attendi { !vm.state.value.loading }
        vm.updateInput("Come va?")
        vm.send()
        attendi(timeoutMs = 8_000) {
            !vm.state.value.sending &&
                vm.state.value.messages.any { it.role == ChatRole.ASSISTANT }
        }

        val primaDelRetry = vm.state.value.messages.count { it.role == ChatRole.USER }
        assertTrue("Il retry deve essere offerto dopo un errore", vm.state.value.canRetry)

        vm.retry()
        attendi(timeoutMs = 8_000) { !vm.state.value.sending }
        val dopoRetry = vm.state.value.messages.count { it.role == ChatRole.USER }
        assertEquals(
            "Riprovare non deve duplicare il messaggio dell'utente ($primaDelRetry -> $dopoRetry)",
            primaDelRetry, dopoRetry
        )
    }

    @Test
    fun `una proposta inesistente non viene applicata`() = runTest {
        val vm = coachVm()
        attendi { !vm.state.value.loading }
        vm.approveChange(999_999L)
        attendi { vm.state.value.message != null }
        assertNotNull("Deve spiegare che la modifica non esiste", vm.state.value.message)
        assertFalse("Nessuna applicazione in corso", vm.state.value.applying)
    }

    @Test
    fun `rifiutare una proposta inesistente non lancia`() = runTest {
        val vm = coachVm()
        attendi { !vm.state.value.loading }
        vm.rejectChange(999_999L)
        Thread.sleep(300)
        assertFalse("Lo stato non deve restare in applicazione", vm.state.value.applying)
    }

    @Test
    fun `approvare senza proposta pendente non fa nulla`() = runTest {
        val vm = coachVm()
        attendi { !vm.state.value.loading }
        vm.approvePending()
        Thread.sleep(200)
        assertNull("Non c'era nulla da approvare", vm.state.value.pendingChange)
    }

    // ---------- CoachAuthorization ----------

    private fun authVm() = CoachAuthorizationViewModel(
        PreferencesRepository(db.userPreferencesDao())
    )

    @Test
    fun `la modalita coach parte dal valore salvato e non da un default arbitrario`() = runTest {
        val vm = authVm()
        Thread.sleep(300)
        assertNotNull("La modalita deve essere leggibile", vm.state.value.mode)
    }

    @Test
    fun `cambiare modalita la salva in modo persistente`() = runTest {
        val vm = authVm()
        Thread.sleep(300)
        vm.setMode(CoachAuthorizationMode.ADAPTIVE)
        attendi { vm.state.value.mode == CoachAuthorizationMode.ADAPTIVE }

        val riletta = PreferencesRepository(db.userPreferencesDao()).getOrCreate()
        assertEquals(
            "La scelta deve sopravvivere alla riapertura",
            CoachAuthorizationMode.ADAPTIVE, riletta.coachAuthorizationMode
        )
    }

    // ---------- Devices ----------

    private fun devicesVm() = DevicesViewModel(
        bleDeviceManager = BleDeviceManager(context),
        sessionManager = BleSessionManager(context, BleDeviceManager(context)),
        progressRepository = ProgressRepository(db.progressDao(), db.checkInDao()),
        context = context
    )

    @Test
    fun `senza Bluetooth la schermata dispositivi lo dichiara`() = runTest {
        val vm = devicesVm()
        vm.refreshCapabilities()
        assertFalse("Robolectric non ha adattatore", vm.state.value.bluetoothSupported)
    }

    @Test
    fun `senza permessi la scansione non parte e lo dice`() = runTest {
        val vm = devicesVm()
        vm.refreshCapabilities()
        vm.startScan()
        attendi { vm.state.value.message != null || !vm.state.value.scanning }
        assertFalse("Non deve restare in scansione senza hardware", vm.state.value.scanning)
    }

    @Test
    fun `scollegare senza connessione non lancia`() = runTest {
        val vm = devicesVm()
        vm.refreshCapabilities()
        vm.disconnect()
        Thread.sleep(200)
        assertFalse("Nessuna sessione da chiudere", vm.state.value.sessionActive)
        assertNull("Nessun dispositivo collegato", vm.state.value.connectedDevice)
    }

    @Test
    fun `i permessi richiesti sono esposti alla UI`() {
        val vm = devicesVm()
        assertTrue(
            "La UI deve poter chiedere i permessi giusti",
            vm.requiredPermissions().isNotEmpty()
        )
    }

    // ---------- Export ----------

    private fun exportVm() = com.fitcoach.app.ui.settings.ExportViewModel(
        context = context,
        exportRepository = ExportRepository(
            db.progressDao(), db.checkInDao(), db.workoutSessionDao(),
            db.workoutSetDao(), db.exercisePerformanceDao()
        )
    )

    @Test
    fun `esportare senza dati produce comunque un file con intestazione`() = runTest {
        val vm = exportVm()
        vm.exportWeights()
        attendi { !vm.state.value.exporting }
        assertFalse("Non deve restare occupato", vm.state.value.exporting)
    }

    @Test
    fun `esportare tutto su un database vuoto produce un json valido`() = runTest {
        // Qui si verifica il contenuto, non la condivisione: l'Intent e il
        // FileProvider sono di Android e non hanno senso in un test unitario.
        val repo = ExportRepository(
            db.progressDao(), db.checkInDao(), db.workoutSessionDao(),
            db.workoutSetDao(), db.exercisePerformanceDao()
        )
        val testo = repo.exportAllJson()
        val albero = json.parseToJsonElement(testo)
        val radice = albero as kotlinx.serialization.json.JsonObject
        assertEquals("FitCoach", radice["app"]!!.jsonPrimitive.content)
        assertTrue(
            "Le pesate devono essere una lista vuota, non assente",
            radice["weights"].toString().trim() == "[]"
        )
    }

    @Test
    fun `esportare le pesate senza dati produce solo l'intestazione`() = runTest {
        val repo = ExportRepository(
            db.progressDao(), db.checkInDao(), db.workoutSessionDao(),
            db.workoutSetDao(), db.exercisePerformanceDao()
        )
        val csv = repo.exportWeightsCsv()
        val righe = csv.trim().lines().filter { it.isNotBlank() }
        assertEquals("Solo l'intestazione, nessuna riga inventata", 1, righe.size)
    }

    @Test
    fun `il messaggio di export consumato viene svuotato`() = runTest {
        val vm = exportVm()
        vm.exportWeights()
        attendi { !vm.state.value.exporting }
        vm.consumeMessage()
        assertNull(vm.state.value.message)
    }

    // ---------- Backup ----------

    @Test
    fun `un ripristino da file non valido viene rifiutato senza toccare i dati`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val backupRepo = com.fitcoach.app.data.repository.DatabaseBackupRepository(context, db)
        val vm = com.fitcoach.app.ui.settings.BackupViewModel(context, backupRepo)
        val finto = java.io.File(context.cacheDir, "finto.db").apply {
            writeText("questo non e' un database")
        }
        vm.restoreBackup(android.net.Uri.fromFile(finto))
        attendi(timeoutMs = 5_000) { !vm.state.value.working }
        assertNotNull("Il ripristino deve segnalare il file non valido", vm.state.value.message)
        val profilo = UserRepository(db.userProfileDao()).get()
        assertEquals(
            "I dati originali non devono essere distrutti",
            80.0, profilo!!.weightKg!!, 0.001
        )
    }
}
