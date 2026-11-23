package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.RecipeRepository
import com.fitcoach.app.data.repository.ScientificSourceRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.RecipeAiService
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.ui.nutrition.RecipesViewModel
import com.fitcoach.app.ui.settings.KnowledgeViewModel
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
 * Ricette e basi scientifiche. Il rischio qui e' proporre una ricetta senza
 * piano o far credere che esista una fonte che non c'e'.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipesAndKnowledgeAdversarialTest {

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

    private fun profilo(esclusi: List<String> = emptyList()) = UserProfile(
        id = 1L, name = "Luca", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        excludedFoods = esclusi,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    private fun recipesVm(): RecipesViewModel {
        val aiClient = AiClient(ScriptedApi(mutableListOf()), json)
        return RecipesViewModel(
            nutritionRepository = NutritionRepository(db),
            foodRepository = FoodRepository(db.foodDao()),
            userRepository = UserRepository(db.userProfileDao()),
            recipeRepository = RecipeRepository(db.savedRecipeDao()),
            recipeAiService = RecipeAiService(
                aiClient,
                com.fitcoach.app.domain.coach.AiConfigResolver(
                    com.fitcoach.app.data.repository.ChatRepository(db.chatDao(), db.aiSettingsDao()),
                    SecureKeyStore(context)
                ),
                json
            )
        )
    }

    // ---------- Ricette ----------

    @Test
    fun `senza piano le ricette non vengono inventate e il motivo e spiegato`() = runTest {
        val vm = recipesVm()
        attendi { !vm.state.value.loading }
        val s = vm.state.value
        assertTrue("Nessuna ricetta senza piano", s.recipes.isEmpty())
        assertTrue("Nessuno slot senza piano", s.availableSlots.isEmpty())
        assertNotNull("Deve spiegare perché non ci sono ricette", s.message)
        assertTrue(
            "Il messaggio deve parlare del piano: ${s.message}",
            s.message!!.contains("piano", ignoreCase = true)
        )
    }

    @Test
    fun `senza piano non si tenta nemmeno la generazione AI`() = runTest {
        val vm = recipesVm()
        attendi { !vm.state.value.loading }
        assertNull("Nessuna ricetta AI senza piano", vm.state.value.aiRecipe)
        assertFalse("Nessuna generazione in corso", vm.state.value.composing)
    }

    @Test
    fun `il messaggio consumato viene svuotato`() = runTest {
        val vm = recipesVm()
        attendi { !vm.state.value.loading }
        vm.consumeMessage()
        assertNull(vm.state.value.message)
    }

    @Test
    fun `selezionare un giorno inesistente non rompe lo stato`() = runTest {
        val vm = recipesVm()
        attendi { !vm.state.value.loading }
        vm.selectDay(99)
        Thread.sleep(300)
        assertTrue("Nessuna ricetta da un giorno che non esiste", vm.state.value.recipes.isEmpty())
    }

    @Test
    fun `scartare una bozza inesistente non lancia`() = runTest {
        val vm = recipesVm()
        attendi { !vm.state.value.loading }
        vm.discardDraft()
        Thread.sleep(200)
        assertNull("Nessuna bozza da scartare", vm.state.value.draft)
    }

    @Test
    fun `comporre senza provider configurato non lascia lo stato occupato`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val vm = recipesVm()
        attendi { !vm.state.value.loading }
        vm.compose(MealSlot.LUNCH)
        attendi(timeoutMs = 8_000) { !vm.state.value.composing }
        assertFalse("Lo stato non deve restare in composizione", vm.state.value.composing)
    }

    // ---------- Knowledge ----------

    @Test
    fun `senza fonti salvate l'elenco resta vuoto e non si inventa nulla`() = runTest {
        val vm = KnowledgeViewModel(
            ScientificSourceRepository(db.scientificSourceDao()),
            PlanVersionRepository(db.planVersionDao())
        )
        attendi { !vm.state.value.loading }
        assertTrue("Nessuna fonte senza dati", vm.state.value.sources.isEmpty())
        assertTrue("Nessuna versione senza dati", vm.state.value.versions.isEmpty())
    }

    @Test
    fun `le fonti presenti nel database vengono mostrate`() = runTest {
        db.scientificSourceDao().insertAll(
            listOf(com.fitcoach.app.data.local.entity.ScientificSourceEntity(
                id = 0,
                kind = com.fitcoach.app.domain.model.SourceKind.OFFICIAL_GUIDELINE,
                title = "Linee guida CREA",
                authors = "CREA",
                organization = "CREA",
                url = "https://www.crea.gov.it/",
                publishedDate = "2019",
                topic = "NUTRIZIONE",
                evidence = "Tabelle di composizione degli alimenti",
                derivedRule = "Valori nutrizionali di riferimento per gli alimenti"
            ))
        )
        val vm = KnowledgeViewModel(
            ScientificSourceRepository(db.scientificSourceDao()),
            PlanVersionRepository(db.planVersionDao())
        )
        attendi { !vm.state.value.loading && vm.state.value.sources.isNotEmpty() }
        assertEquals(1, vm.state.value.sources.size)
        assertEquals("Linee guida CREA", vm.state.value.sources.first().title)
    }
}
