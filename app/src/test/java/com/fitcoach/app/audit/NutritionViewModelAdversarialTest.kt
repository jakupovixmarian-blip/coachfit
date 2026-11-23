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
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.FoodSearchRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.SubstitutionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.data.remote.FoodApi
import com.fitcoach.app.data.remote.OffProductResponse
import com.fitcoach.app.data.remote.OffSearchResponse
import com.fitcoach.app.data.remote.UsdaSearchResponse
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.ui.nutrition.NutritionViewModel
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
import retrofit2.Response

/**
 * Il ViewModel piu' grande dell'app (655 righe). Modifica il piano nutrizionale,
 * quindi un errore di validazione non produce un messaggio sbagliato: produce
 * un piano sbagliato che l'utente segue per una settimana.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NutritionViewModelAdversarialTest {

    private lateinit var context: Context
    private lateinit var db: FitCoachDatabase
    private lateinit var nutrition: NutritionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var vm: NutritionViewModel
    /**
     * Dispatcher NON legato allo scheduler di `runTest`: le query Room
     * riprendono da thread propri e devono poter aggiornare lo stato mentre il
     * test attende. Con un dispatcher legato allo scheduler, il polling
     * bloccherebbe proprio la coda che deve avanzare.
     */
    private val dispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class OfflineFoodApi : FoodApi {
        override suspend fun offSearch(
            url: String, terms: String, simple: Int, action: String, json: Int,
            pageSize: Int, fields: String
        ): Response<OffSearchResponse> = throw java.io.IOException("offline")

        override suspend fun offByBarcode(url: String, fields: String): Response<OffProductResponse> =
            throw java.io.IOException("offline")

        override suspend fun usdaSearch(
            url: String, query: String, apiKey: String, pageSize: Int, dataType: String
        ): Response<UsdaSearchResponse> = throw java.io.IOException("offline")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        nutrition = NutritionRepository(db)
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        vm = build()
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

    private fun build(): NutritionViewModel {
        val exercises = ExerciseRepository(db.exerciseDao())
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exercises, nutrition, WorkoutRepository(db),
            PlanVersionRepository(db.planVersionDao()), db, json
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
            foodRepository,
            exercises
        )
        return NutritionViewModel(
            userRepository = userRepository,
            nutritionRepository = nutrition,
            foodRepository = foodRepository,
            diaryRepository = NutritionDiaryRepository(db),
            substitutionRepository = SubstitutionRepository(db, nutrition, foodRepository),
            foodSearchRepository = FoodSearchRepository(db.foodDao(), OfflineFoodApi()),
            generationCoordinator = ProgramGenerationCoordinator(service),
            context = context
        )
    }

    private fun riso() = FoodItem(
        name = "Riso bianco", category = "Cereali", kcalPer100g = 350.0,
        proteinPer100g = 7.0, carbsPer100g = 78.0, fatPer100g = 1.0,
        defaultUnit = FoodUnit.RAW, source = "CREA"
    )

    private suspend fun creaPianoManuale(): Long {
        db.userProfileDao().upsert(
            Mappers.run {
                UserProfile(
                    id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
                    heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
                    experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
                    trainingDaysPerWeek = 4, mealsPerDay = 5,
                    anamnesisCompleted = true, anamnesisConfirmedAt = 1L,
                    anamnesisSummary = "confermata"
                ).toEntity()
            }
        )
        val profile = userRepository.get()!!
        val energy = com.fitcoach.app.domain.nutrition.EnergyCalculator.compute(profile)!!
        val cycling = com.fitcoach.app.domain.nutrition.CarbCyclingEngine.build(profile, energy)
        return nutrition.createManualPlan(
            profile = profile,
            energy = energy,
            cycling = cycling,
            dayTypes = List(7) { com.fitcoach.app.domain.model.CarbDayType.MEDIUM },
            trainingDayIndices = listOf(0, 2, 4)
        )
    }

    // ---------- Stato iniziale ----------

    @Test
    fun `senza piano lo stato e' vuoto e non caricante`() = runTest {
        vm.load()
        attendi { !vm.state.value.loading }
        assertNull("Nessun piano senza dati", vm.state.value.plan)
        assertNull("Nessun giorno selezionabile", vm.state.value.selectedDay)
        assertTrue("Nessun piano salvato", vm.state.value.plans.isEmpty())
    }

    @Test
    fun `il giorno selezionato di default e oggi e non lunedi`() = runTest {
        val atteso = com.fitcoach.app.domain.nutrition.NutritionDiaryEngine
            .dayIndexFor(NutritionDiaryRepository.todayEpochDay())
        assertEquals(
            "Aprire sempre su lunedi' farebbe credere che i target di oggi siano quelli di lunedi'",
            atteso, vm.state.value.selectedDayIndex
        )
    }

    // ---------- Modifica grammature ----------

    @Test
    fun `una grammatura non numerica non salva e avvisa`() = runTest {
        vm.openEditEntry(entryDiProva(grams = 100.0))
        vm.updateEditEntryGrams("molto")
        vm.saveEditEntry()
        attendi { vm.state.value.message != null }
        assertNotNull("Deve avvisare invece di salvare", vm.state.value.message)
        assertNotNull("La finestra resta aperta", vm.state.value.editingEntry)
    }

    @Test
    fun `zero grammi non vengono accettati`() = runTest {
        vm.openEditEntry(entryDiProva(grams = 100.0))
        vm.updateEditEntryGrams("0")
        vm.saveEditEntry()
        attendi { vm.state.value.message != null }
        assertNotNull(vm.state.value.editingEntry)
    }

    @Test
    fun `grammi negativi non vengono accettati`() = runTest {
        vm.openEditEntry(entryDiProva(grams = 100.0))
        vm.updateEditEntryGrams("-50")
        vm.saveEditEntry()
        attendi { vm.state.value.message != null }
        assertNotNull("Una quantita' negativa non deve passare", vm.state.value.editingEntry)
    }

    @Test
    fun `oltre dieci chili non vengono accettati`() = runTest {
        vm.openEditEntry(entryDiProva(grams = 100.0))
        vm.updateEditEntryGrams("10001")
        vm.saveEditEntry()
        attendi { vm.state.value.message != null }
        assertNotNull(vm.state.value.editingEntry)
    }

    @Test
    fun `la virgola decimale viene accettata come separatore`() = runTest {
        vm.openEditEntry(entryDiProva(grams = 100.0))
        vm.updateEditEntryGrams("150,5")
        // Il campo accetta la virgola: la conversione avviene al salvataggio.
        assertEquals("150,5", vm.state.value.editEntryGrams)
    }

    @Test
    fun `la finestra di modifica si apre con i grammi attuali`() = runTest {
        vm.openEditEntry(entryDiProva(grams = 150.0))
        assertEquals("150", vm.state.value.editEntryGrams)
    }

    @Test
    fun `un decimale nei grammi non viene arrotondato all'apertura`() = runTest {
        vm.openEditEntry(entryDiProva(grams = 82.5))
        assertEquals("82.5", vm.state.value.editEntryGrams)
    }

    // ---------- Aggiunta manuale ----------

    @Test
    fun `aggiungere un alimento senza selezionarlo avvisa`() = runTest {
        vm.openAddFood(mealDiProva())
        vm.updateAddFoodGrams("100")
        vm.confirmAddFood()
        attendi { vm.state.value.message != null }
        assertEquals(
            "Deve chiedere di selezionare un alimento",
            "Seleziona un alimento da aggiungere.", vm.state.value.message
        )
    }

    @Test
    fun `una quantita non valida non aggiunge l'alimento`() = runTest {
        vm.openAddFood(mealDiProva())
        vm.selectFoodToAdd(riso())
        vm.updateAddFoodGrams("0")
        vm.confirmAddFood()
        attendi { vm.state.value.message != null }
        assertEquals("Inserisci una quantita valida in grammi.", vm.state.value.message)
    }

    @Test
    fun `oltre tre chili non vengono aggiunti`() = runTest {
        vm.openAddFood(mealDiProva())
        vm.selectFoodToAdd(riso())
        vm.updateAddFoodGrams("3001")
        vm.confirmAddFood()
        attendi { vm.state.value.message != null }
        assertEquals("Inserisci una quantita valida in grammi.", vm.state.value.message)
    }

    @Test
    fun `il campo grammi scarta i caratteri non numerici`() = runTest {
        vm.openAddFood(mealDiProva())
        vm.updateAddFoodGrams("12a3b,4")
        assertEquals("123,4", vm.state.value.addFoodGrams)
    }

    // ---------- Target della giornata ----------

    @Test
    fun `target con un campo vuoto non salvano`() = runTest {
        creaPianoManuale()
        vm.load()
        attendi { vm.state.value.plan != null }
        vm.openEditDay(vm.state.value.plan!!.days.first())
        vm.updateDayTargetFields("2000", "", "200", "60")
        vm.saveEditDay()
        attendi { vm.state.value.message != null }
        assertEquals("Inserisci tutti i target numerici.", vm.state.value.message)
    }

    @Test
    fun `target non numerici non salvano`() = runTest {
        creaPianoManuale()
        vm.load()
        attendi { vm.state.value.plan != null }
        vm.openEditDay(vm.state.value.plan!!.days.first())
        vm.updateDayTargetFields("duemila", "150", "200", "60")
        vm.saveEditDay()
        attendi { vm.state.value.message != null }
        assertEquals("Inserisci tutti i target numerici.", vm.state.value.message)
    }

    // ---------- Orario del pasto ----------

    @Test
    fun `un orario non valido viene rifiutato dal repository`() = runTest {
        creaPianoManuale()
        vm.load()
        attendi { vm.state.value.plan != null }
        val meal = vm.state.value.plan!!.days.first().meals.first()
        vm.openEditMeal(meal)
        vm.updateEditMealTime("25:99")
        vm.saveEditMeal()
        attendi { vm.state.value.message != null }
        assertEquals(
            "Un orario impossibile non deve essere salvato",
            "Orario non valido: usa HH:mm.", vm.state.value.message
        )
    }

    @Test
    fun `la finestra orario si apre con l'orario attuale`() = runTest {
        val meal = mealDiProva(time = "13:30")
        vm.openEditMeal(meal)
        assertEquals("13:30", vm.state.value.editMealTime)
    }

    @Test
    fun `chiudere la finestra orario pulisce il campo`() = runTest {
        vm.openEditMeal(mealDiProva(time = "13:30"))
        vm.closeEditMeal()
        assertNull(vm.state.value.editingMeal)
        assertEquals("", vm.state.value.editMealTime)
    }

    // ---------- Messaggi ----------

    @Test
    fun `il messaggio consumato viene svuotato`() = runTest {
        vm.openAddFood(mealDiProva())
        vm.confirmAddFood()
        attendi { vm.state.value.message != null }
        vm.consumeMessage()
        assertNull("Il messaggio non deve ripetersi al ricomponimento", vm.state.value.message)
    }

    // ---------- Helper ----------

    private fun entryDiProva(grams: Double) = MealEntry(
        id = 1L, mealId = 1L, foodItemId = 1L, foodName = "Riso bianco",
        grams = grams, unit = FoodUnit.RAW, kcal = 350.0, protein = 7.0,
        carbs = 78.0, fat = 1.0
    )

    private fun mealDiProva(time: String = "13:00") = Meal(
        id = 1L, nutritionDayId = 1L,
        slot = com.fitcoach.app.domain.model.MealSlot.LUNCH,
        time = time, entries = emptyList()
    )
}
