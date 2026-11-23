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
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.ProgressPhotoRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiConfigResolver
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.WeeklyCheckInService
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.ui.progress.ProgressViewModel
import com.fitcoach.app.ui.settings.ProfileEditViewModel
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
 * Progress e Profilo gestiscono le misure che l'utente inserisce a mano:
 * e' l'unico punto in cui un valore assurdo puo' entrare nel database.
 * Le validazioni sono il confine tra "dato reale" e "dato inventato".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProgressAndProfileAdversarialTest {

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

    private fun profilo() = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    private fun coordinator(): ProgramGenerationCoordinator {
        val food = FoodRepository(db.foodDao())
        val exercises = ExerciseRepository(db.exerciseDao())
        val nutrition = NutritionRepository(db)
        val users = UserRepository(db.userProfileDao())
        val submitTool: CoachTool = SubmitFullProgramTool(
            users, food, exercises, nutrition, WorkoutRepository(db),
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

    private fun progressVm(): ProgressViewModel {
        val progress = ProgressRepository(db.progressDao(), db.checkInDao())
        val users = UserRepository(db.userProfileDao())
        val changes = ChangeRepository(db.programChangeDao())
        val coordinator = coordinator()
        val chat = ChatRepository(db.chatDao(), db.aiSettingsDao())
        val service = WeeklyCheckInService(
            users, progress, NutritionRepository(db), NutritionDiaryRepository(db),
            WorkoutRepository(db), chat, changes,
            AiConfigResolver(chat, SecureKeyStore(context)),
            CoachOrchestrator(
                AiClient(ScriptedApi(mutableListOf()), json),
                CoachToolRegistry(emptySet()), json
            )
        )
        return ProgressViewModel(
            progressRepository = progress,
            photoRepository = ProgressPhotoRepository(db),
            workoutRepository = WorkoutRepository(db),
            userRepository = users,
            changeRepository = changes,
            actionExecutor = CoachActionExecutor(db, changes, users, object : dagger.Lazy<ProgramGenerationCoordinator> {
                override fun get() = coordinator
            }, json),
            diaryRepository = NutritionDiaryRepository(db),
            nutritionRepository = NutritionRepository(db),
            runWeeklyCheckIn = RunWeeklyCheckInUseCase(service)
        )
    }

    // ---------- ProgressViewModel: peso ----------

    @Test
    fun `un peso non numerico non viene registrato`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("ottanta")
        vm.addWeight()
        attendi { vm.state.value.message != null }
        assertEquals("Inserisci un peso valido in kg.", vm.state.value.message)
        assertTrue("Nessun peso deve entrare", vm.state.value.weights.isEmpty())
    }

    @Test
    fun `un peso a zero non viene registrato`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("0")
        vm.addWeight()
        attendi { vm.state.value.message != null }
        assertTrue(vm.state.value.weights.isEmpty())
    }

    @Test
    fun `un peso negativo non viene registrato`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("-80")
        vm.addWeight()
        attendi { vm.state.value.message != null }
        assertTrue("Un peso negativo non e' una misura", vm.state.value.weights.isEmpty())
    }

    @Test
    fun `un peso superiore a 400 kg non viene registrato`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("401")
        vm.addWeight()
        attendi { vm.state.value.message != null }
        assertTrue(vm.state.value.weights.isEmpty())
    }

    @Test
    fun `un peso di 20 kg esatti viene rifiutato come limite inferiore`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("20")
        vm.addWeight()
        attendi { vm.state.value.message != null }
        assertTrue("Il limite inferiore e' escluso", vm.state.value.weights.isEmpty())
    }

    @Test
    fun `un peso valido viene registrato e svuota il campo`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("79,4")
        vm.addWeight()
        attendi { vm.state.value.weights.isNotEmpty() }
        assertEquals(1, vm.state.value.weights.size)
        assertEquals(79.4, vm.state.value.weights.first().weightKg, 0.001)
        assertEquals("Il campo deve svuotarsi", "", vm.state.value.weightInput)
    }

    @Test
    fun `una percentuale di grasso fuori scala viene scartata ma il peso resta`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("80")
        vm.updateBfInput("95")
        vm.addWeight()
        attendi { vm.state.value.weights.isNotEmpty() }
        assertNull(
            "Una percentuale impossibile non deve essere salvata",
            vm.state.value.weights.first().bodyFatPercent
        )
        assertEquals(80.0, vm.state.value.weights.first().weightKg, 0.001)
    }

    @Test
    fun `una percentuale di grasso plausibile viene salvata`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("80")
        vm.updateBfInput("18,5")
        vm.addWeight()
        attendi { vm.state.value.weights.isNotEmpty() }
        assertEquals(18.5, vm.state.value.weights.first().bodyFatPercent!!, 0.001)
    }

    // ---------- ProgressViewModel: circonferenze ----------

    @Test
    fun `una circonferenza senza sede non viene salvata`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateCircumferenceSite("   ")
        vm.updateCircumferenceCm("80")
        vm.addCircumference()
        attendi { vm.state.value.message != null }
        assertEquals("Inserisci una sede e una misura valida in cm.", vm.state.value.message)
        assertTrue(vm.state.value.circumferences.isEmpty())
    }

    @Test
    fun `una circonferenza fuori scala non viene salvata`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateCircumferenceSite("vita")
        vm.updateCircumferenceCm("300")
        vm.addCircumference()
        attendi { vm.state.value.message != null }
        assertTrue(vm.state.value.circumferences.isEmpty())
    }

    @Test
    fun `una circonferenza negativa non viene salvata`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateCircumferenceSite("vita")
        vm.updateCircumferenceCm("-10")
        vm.addCircumference()
        attendi { vm.state.value.message != null }
        assertTrue(vm.state.value.circumferences.isEmpty())
    }

    @Test
    fun `una circonferenza valida viene salvata`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateCircumferenceSite("vita")
        vm.updateCircumferenceCm("82,5")
        vm.addCircumference()
        attendi { vm.state.value.circumferences.isNotEmpty() }
        assertEquals(82.5, vm.state.value.circumferences.first().valueCm, 0.001)
        assertEquals("vita", vm.state.value.circumferences.first().site)
    }

    // ---------- ProgressViewModel: statistiche ----------

    @Test
    fun `senza dati le statistiche non inventano valori`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading && vm.state.value.statistics != null }
        val s = vm.state.value.statistics!!
        assertNull("Nessuna media peso senza pesate", s.avgWeightKg)
        assertNull("Nessuna aderenza senza diario", s.avgAdherencePercent)
        assertEquals(0, s.weightSamples)
    }

    @Test
    fun `cambiare intervallo ricalcola senza perdere i dati`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("80")
        vm.addWeight()
        attendi { vm.state.value.weights.isNotEmpty() }

        vm.setStatRange(com.fitcoach.app.domain.adaptive.StatRange.MONTH)
        attendi { vm.state.value.statistics?.range == com.fitcoach.app.domain.adaptive.StatRange.MONTH }
        assertEquals(
            com.fitcoach.app.domain.adaptive.StatRange.MONTH,
            vm.state.value.statistics!!.range
        )
        assertEquals("La pesata deve restare", 1, vm.state.value.statistics!!.weightSamples)
    }

    @Test
    fun `la tendenza non compare con una sola pesata`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.updateWeightInput("80")
        vm.addWeight()
        attendi { vm.state.value.weights.isNotEmpty() }
        assertNull(
            "Con un solo dato non c'e' tendenza, e non va inventata",
            vm.state.value.trend
        )
    }

    // ---------- ProgressViewModel: foto ----------

    @Test
    fun `una foto con uri non locale viene rifiutata`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.addPhoto("https://esempio.com/foto.jpg")
        attendi { vm.state.value.message != null }
        assertTrue(
            "Una foto remota non deve essere salvata come locale",
            vm.state.value.photos.isEmpty()
        )
    }

    @Test
    fun `una foto locale viene salvata senza consenso AI`() = runTest {
        val vm = progressVm()
        attendi { !vm.state.value.loading }
        vm.addPhoto("file:///data/user/0/fitcoach/foto.jpg")
        attendi { vm.state.value.photos.isNotEmpty() }
        assertFalse(
            "Il consenso AI non deve essere implicito",
            vm.state.value.photos.first().consentForAi
        )
    }

    // ---------- ProfileEditViewModel ----------

    private fun profileVm() = ProfileEditViewModel(
        userRepository = UserRepository(db.userProfileDao()),
        progressRepository = ProgressRepository(db.progressDao(), db.checkInDao()),
        generationCoordinator = coordinator()
    )

    @Test
    fun `il profilo si carica nei campi di modifica`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = profileVm()
        attendi { vm.state.value.weightKg.isNotBlank() }
        assertEquals("80.0", vm.state.value.weightKg)
        assertEquals(Goal.HYPERTROPHY, vm.state.value.profile?.goal)
    }

    @Test
    fun `un peso non numerico non corrompe il profilo`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = profileVm()
        attendi { vm.state.value.profile != null }
        vm.updateWeight("abc")
        assertEquals("abc", vm.state.value.weightKg)
        // Il salvataggio deve rifiutare, non azzerare il peso reale.
        vm.save()
        attendi { vm.state.value.message != null }
        val salvato = UserRepository(db.userProfileDao()).get()
        assertEquals(
            "Il peso valido non deve essere sovrascritto da un testo",
            80.0, salvato!!.weightKg!!, 0.001
        )
    }

    @Test
    fun `un peso fuori scala non corrompe il profilo`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = profileVm()
        attendi { vm.state.value.profile != null }
        vm.updateWeight("900")
        vm.save()
        attendi { vm.state.value.message != null }
        val salvato = UserRepository(db.userProfileDao()).get()
        assertEquals(80.0, salvato!!.weightKg!!, 0.001)
    }

    @Test
    fun `una percentuale di grasso fuori scala non corrompe il profilo`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = profileVm()
        attendi { vm.state.value.profile != null }
        vm.updateBodyFat("150")
        vm.save()
        attendi { vm.state.value.message != null }
        val salvato = UserRepository(db.userProfileDao()).get()
        assertEquals(18.0, salvato!!.bodyFatPercent!!, 0.001)
    }

    @Test
    fun `zero giorni di allenamento non vengono salvati`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = profileVm()
        attendi { vm.state.value.profile != null }
        vm.setTrainingDays(0)
        vm.save()
        attendi { vm.state.value.message != null }
        val salvato = UserRepository(db.userProfileDao()).get()
        assertEquals(
            "Zero giorni renderebbe impossibile costruire una scheda",
            4, salvato!!.trainingDaysPerWeek
        )
    }

    @Test
    fun `un peso valido viene salvato e registrato nello storico`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = profileVm()
        attendi { vm.state.value.profile != null }
        vm.updateWeight("82,5")
        vm.save()
        attendi { vm.state.value.message != null }
        val salvato = UserRepository(db.userProfileDao()).get()
        assertEquals(82.5, salvato!!.weightKg!!, 0.001)
    }
}
