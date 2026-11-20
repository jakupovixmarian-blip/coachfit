package com.fitcoach.app.collaudo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.ui.anamnesis.AnamnesisViewModel
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

/**
 * Collaudo del percorso REALE dell'utente dopo l'anamnesi.
 *
 * Difetto segnalato: "quando clicchi su genera piano alimentare e scheda non fa
 * nulla come se non premesse il bottone". Il sospetto e' la guardia silenziosa
 * in `generateNow()`, che esce senza messaggi se il profilo in stato non risulta
 * pronto. Qui si riproduce la sequenza esatta: rispondi a tutte le domande,
 * confermi, poi premi Genera.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GenerateAfterConfirmCollaudoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private lateinit var context: Context
    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository

    // Lo stesso scheduler per il Main dei ViewModel e per il corpo del test:
    // con due scheduler distinti advanceUntilIdle non tocca le coroutine del
    // ViewModel e lo stato iniziale restava vuoto.
    private val scheduler = TestCoroutineScheduler()
    private val mainDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        userRepository = UserRepository(db.userProfileDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun service(): AiProgramGenerationService {
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, FoodRepository(db.foodDao()), ExerciseRepository(db.exerciseDao()),
            NutritionRepository(db), WorkoutRepository(db), PlanVersionRepository(db.planVersionDao()),
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
            ExerciseRepository(db.exerciseDao())
        )
        return service
    }

    /** Pompa lo scheduler e attende (tempo reale) che la condizione sia vera. */
    private fun awaitCondition(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            scheduler.runCurrent()
            if (condition()) return
            Thread.sleep(15)
        }
        scheduler.runCurrent()
    }

    private fun viewModel() = AnamnesisViewModel(
        userRepository = userRepository,
        aiProgramGeneration = service(),
        context = context
    )

    /** Risposte plausibili per le domande a testo/numero, come le digiterebbe l'utente. */
    private val VALUES = mapOf(
        "age" to "30",
        "heightCm" to "180",
        "weightKg" to "80",
        "bodyFatPercent" to "18",
        "targetTimelineWeeks" to "16",
        "workHoursPerDay" to "8",
        "dailySteps" to "7000",
        "extraSportSessionsPerWeek" to "0",
        "maxSessionMinutes" to "60",
        "sleepHours" to "7",
        "mealsPerDay" to "5",
        "monthlyFoodBudgetEur" to "300",
        "budgetPersons" to "1",
        "trainingYears" to "3",
        "trainingDaysPerWeek" to "4",
        "currentLoads" to "panca 70 kg",
    )

    /**
     * Risponde come l'utente: per le domande a scelta si tocca un'opzione, per
     * quelle libere si scrive un valore plausibile. Scrivere "1" a una CHOICE
     * non seleziona nulla, ed e' esattamente il modo in cui il primo tentativo
     * di questo test si e' ingannato da solo.
     */
    private fun answerAll(vm: AnamnesisViewModel) {
        var guard = 0
        while (guard++ < 300) {
            val q = vm.state.value.current ?: break
            val text = VALUES[q.id].orEmpty()
            when (q.kind) {
                AnamnesisEngine.Kind.CHOICE, AnamnesisEngine.Kind.MULTI -> {
                    val option = q.options.firstOrNull { it != "UNSPECIFIED" }
                        ?: q.options.firstOrNull()
                    if (option == null) {
                        vm.skip()
                    } else {
                        vm.toggleOption(option, q.kind == AnamnesisEngine.Kind.MULTI)
                        vm.submit()
                    }
                }
                AnamnesisEngine.Kind.BOOL -> {
                    vm.toggleOption("SI", false)
                    vm.submit()
                }
                else -> {
                    if (text.isNotBlank()) {
                        vm.updateInput(text)
                        vm.submit()
                    } else {
                        vm.skip()
                    }
                }
            }
        }
    }

    @Test
    fun `dopo la conferma il pulsante Genera deve produrre un effetto visibile`() =
        runTest(mainDispatcher) {
        val vm = viewModel()
        // L'init del ViewModel legge il profilo: senza questo giro lo stato e'
        // ancora vuoto e il ciclo di risposte uscirebbe subito.
        // L'init legge il profilo da Room, che completa su un thread reale:
        // advanceUntilIdle non basta, serve attendere davvero.
        awaitCondition { vm.state.value.current != null }
        answerAll(vm)

        assertThat(AnamnesisEngine.missingRequired(vm.state.value.profile)).isEmpty()

        vm.confirm()
        awaitCondition { vm.state.value.confirmed }

        assertThat(vm.state.value.confirmed).isTrue()

        // Il pulsante deve essere premibile: se il profilo in stato non e' pronto,
        // il click non fa NULLA e l'utente resta davanti a uno schermo fermo.
        vm.generateNow()
        awaitCondition {
            val st = vm.state.value
            st.generatingPrograms || st.error != null || st.generationFinished
        }

        val state = vm.state.value
        assertThat(state.generatingPrograms || state.error != null || state.generationFinished)
            .isTrue()
    }

    @Test
    fun `premere Genera senza anamnesi confermata spiega il motivo invece di tacere`() =
        runTest(mainDispatcher) {
        val vm = viewModel()
        awaitCondition { vm.state.value.current != null }

        // Nessuna risposta, nessuna conferma: il pulsante non puo' generare, ma
        // deve dirlo. Prima usciva in silenzio e sembrava rotto.
        vm.generateNow()
        scheduler.runCurrent()

        assertThat(vm.state.value.error).isNotNull()
        assertThat(vm.state.value.generatingPrograms).isFalse()
    }
}
