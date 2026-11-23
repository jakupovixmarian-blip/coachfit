package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.remote.FoodApi
import com.fitcoach.app.data.remote.OffProductResponse
import com.fitcoach.app.data.remote.OffSearchResponse
import com.fitcoach.app.data.remote.UsdaSearchResponse
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.FoodSearchRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.CircumferenceRecord
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.ui.diary.DiaryViewModel
import com.fitcoach.app.ui.workout.CustomWorkoutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 * I ViewModel orchestrano dati e validazioni, ma 13 su 20 non avevano test.
 * Qui si esercitano le azioni che l'utente puo' davvero premere, con database
 * reale e repository reali: nessun mock, cosi' un errore di query o di
 * validazione emerge come nell'app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ViewModelAdversarialTest {

    private lateinit var context: Context
    private lateinit var db: FitCoachDatabase
    /**
     * Il Diario osserva un Flow di Room, che emette su thread reali: un
     * dispatcher di test virtuale non lo farebbe avanzare. Unconfined lascia
     * girare il lavoro sulla stessa pila, cosi' l'attesa e' deterministica.
     */
    private val dispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    /** Attende una condizione reale: il Flow di Room emette fuori dallo scheduler. */
    private fun attendi(timeoutMs: Long = 3_000, condizione: () -> Boolean) {
        val fine = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < fine && !condizione()) {
            Thread.sleep(10)
        }
    }

    /** Come un telefono senza rete: ogni chiamata remota fallisce davvero. */
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

    private fun riso() = FoodItem(
        name = "Riso bianco", category = "Cereali", kcalPer100g = 350.0,
        proteinPer100g = 7.0, carbsPer100g = 78.0, fatPer100g = 1.0,
        defaultUnit = FoodUnit.RAW, source = "CREA"
    )

    private fun profilo(giorni: Int = 3) = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.BEGINNER, goal = Goal.RECOMP,
        trainingDaysPerWeek = giorni, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    // ---------- DiaryViewModel ----------

    private fun diaryVm() = DiaryViewModel(
        diaryRepository = NutritionDiaryRepository(db),
        foodRepository = FoodRepository(db.foodDao()),
        foodSearchRepository = FoodSearchRepository(db.foodDao(), OfflineFoodApi())
    )

    @Test
    fun `il diario parte vuoto senza inventare pasti`() = runTest(dispatcher) {
        val vm = diaryVm()
        attendi { !vm.state.value.loading }
        assertTrue("Nessuna voce senza inserimenti", vm.state.value.entries.isEmpty())
        assertFalse("Il caricamento deve essere concluso", vm.state.value.loading)
    }

    @Test
    fun `zero grammi non aggiunge una voce al diario`() = runTest(dispatcher) {
        val vm = diaryVm()
        advanceUntilIdle()
        vm.addExtraFood(riso(), 0.0, MealSlot.LUNCH)
        attendi { vm.state.value.message != null }
        assertTrue(
            "Zero grammi non e' un pasto: il diario deve restare vuoto",
            vm.state.value.entries.isEmpty()
        )
    }

    @Test
    fun `grammi negativi non aggiungono una voce`() = runTest(dispatcher) {
        val vm = diaryVm()
        advanceUntilIdle()
        vm.addExtraFood(riso(), -100.0, MealSlot.LUNCH)
        attendi { vm.state.value.message != null }
        assertTrue("Grammi negativi non devono entrare", vm.state.value.entries.isEmpty())
    }

    @Test
    fun `un alimento valido entra nel diario con le calorie calcolate`() = runTest(dispatcher) {
        val vm = diaryVm()
        advanceUntilIdle()
        vm.addExtraFood(riso(), 200.0, MealSlot.LUNCH)
        attendi { vm.state.value.entries.isNotEmpty() }

        val voci = vm.state.value.entries
        assertEquals("Una voce inserita", 1, voci.size)
        assertEquals("200 g di riso sono 700 kcal", 700.0, voci.first().kcal, 1.0)
        assertNotNull("Il bilancio del giorno deve essere calcolato", vm.state.value.balance)
    }

    // ---------- CustomWorkoutViewModel ----------

    private fun customVm() = CustomWorkoutViewModel(
        userRepository = UserRepository(db.userProfileDao()),
        workoutRepository = WorkoutRepository(db),
        exerciseRepository = ExerciseRepository(db.exerciseDao())
    )

    @Test
    fun `la scheda manuale si carica anche senza profilo`() = runTest(dispatcher) {
        val vm = customVm()
        advanceUntilIdle()
        assertNotNull("Lo stato deve esistere anche senza profilo", vm.state.value)
        assertTrue("Senza profilo non ci sono giorni", vm.state.value.days.isEmpty())
    }

    @Test
    fun `un profilo con giorni disponibili non genera una scheda da solo`() = runTest(dispatcher) {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = customVm()
        advanceUntilIdle()
        // La scheda manuale e' vuota finche' l'utente non aggiunge esercizi:
        // non deve comparire una scheda precompilata con dati inventati.
        assertTrue(
            "La scheda manuale non deve precompilarsi da sola",
            vm.state.value.days.isEmpty()
        )
    }

    // ---------- Repository: validazione dei dati ----------

    @Test
    fun `il peso registrato mantiene il valore esatto`() = runTest(dispatcher) {
        val progress = ProgressRepository(db.progressDao(), db.checkInDao())
        progress.addWeight(WeightRecord(dateEpochDay = 20_000, weightKg = 79.35, bodyFatPercent = 17.2))
        val pesi = progress.getWeights()
        assertEquals(1, pesi.size)
        assertEquals(79.35, pesi.first().weightKg, 0.0001)
        assertEquals(17.2, pesi.first().bodyFatPercent!!, 0.0001)
    }

    @Test
    fun `la circonferenza registrata mantiene il valore esatto`() = runTest(dispatcher) {
        val progress = ProgressRepository(db.progressDao(), db.checkInDao())
        progress.addCircumference(
            CircumferenceRecord(dateEpochDay = 20_000, site = "vita", valueCm = 82.5)
        )
        val misure = progress.getCircumferences()
        assertEquals(1, misure.size)
        assertEquals(82.5, misure.first().valueCm, 0.001)
        assertEquals("vita", misure.first().site)
    }

    @Test
    fun `due pesate nello stesso giorno non si perdono`() = runTest(dispatcher) {
        val progress = ProgressRepository(db.progressDao(), db.checkInDao())
        progress.addWeight(WeightRecord(dateEpochDay = 20_000, weightKg = 80.0))
        progress.addWeight(WeightRecord(dateEpochDay = 20_000, weightKg = 80.4))
        val pesi = progress.getWeights()
        assertEquals(
            "Entrambe le pesate devono essere conservate, non sovrascritte",
            2, pesi.size
        )
    }
}
