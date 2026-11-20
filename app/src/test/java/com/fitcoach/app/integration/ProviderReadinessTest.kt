package com.fitcoach.app.integration

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
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.support.ScriptedApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Il provider AI va verificato PRIMA di lanciare un turno.
 *
 * Difetto osservato: l'utente premeva "Genera" senza provider configurato e la
 * schermata restava su "Generazione..." perche il turno non poteva riuscire ma
 * nessuno lo diceva. [AiProgramGenerationService.isProviderReady] permette alle
 * schermate di fermarsi prima, con un messaggio utile.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProviderReadinessTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private lateinit var db: FitCoachDatabase
    private lateinit var chatRepository: ChatRepository
    private lateinit var secureKeyStore: SecureKeyStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())
        // Preferenze in memoria: il test verifica la logica di prontezza, non il
        // Keystore Android (non disponibile in una JVM).
        val prefs = context.getSharedPreferences("provider-readiness", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        secureKeyStore = SecureKeyStore(context, prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun service(): AiProgramGenerationService {
        val submitTool: CoachTool = SubmitFullProgramTool(
            UserRepository(db.userProfileDao()),
            FoodRepository(db.foodDao()),
            ExerciseRepository(db.exerciseDao()),
            NutritionRepository(db),
            WorkoutRepository(db),
            PlanVersionRepository(db.planVersionDao()),
            db,
            json
        )
        val orchestrator = CoachOrchestrator(
            AiClient(ScriptedApi(mutableListOf()), json),
            CoachToolRegistry(setOf(submitTool)),
            json
        )
        return AiProgramGenerationService(
            orchestrator, chatRepository, secureKeyStore, json,
            FoodRepository(db.foodDao()), ExerciseRepository(db.exerciseDao())
        )
    }

    @Test
    fun `senza chiave ne proxy il provider non e pronto`() = runTest {
        chatRepository.saveAiSettings(
            AiProviderSettings(
                provider = AiProvider.OPENAI,
                model = "gpt-4o-mini",
                useBackendProxy = false
            )
        )

        // Nessuna chiave salvata e nessun proxy: il turno fallirebbe comunque,
        // quindi la schermata deve poterlo sapere in anticipo.
        assertThat(service().isProviderReady()).isFalse()
    }

    @Test
    fun `con la chiave salvata il provider e pronto`() = runTest {
        chatRepository.saveAiSettings(
            AiProviderSettings(
                provider = AiProvider.OPENAI,
                model = "gpt-4o-mini",
                useBackendProxy = false
            )
        )
        secureKeyStore.putApiKey(AiProvider.OPENAI.name, "sk-test-key")

        assertThat(service().isProviderReady()).isTrue()
    }

    @Test
    fun `con il backend proxy il provider e pronto senza chiave locale`() = runTest {
        chatRepository.saveAiSettings(
            AiProviderSettings(
                provider = AiProvider.OPENAI,
                model = "gpt-4o-mini",
                useBackendProxy = true,
                backendBaseUrl = "https://proxy.test/v1"
            )
        )

        // La chiave resta sul server: sul dispositivo non serve.
        assertThat(service().isProviderReady()).isTrue()
    }

    @Test
    fun `un modello vuoto ricade sul default del provider`() = runTest {
        chatRepository.saveAiSettings(
            AiProviderSettings(
                provider = AiProvider.OPENAI,
                model = "",
                useBackendProxy = true,
                backendBaseUrl = "https://proxy.test/v1"
            )
        )
        secureKeyStore.putApiKey(AiProvider.OPENAI.name, "sk-test-key")

        // Un modello vuoto non blocca: si usa il default del provider. Il test
        // documenta il contratto, cosi nessuno lo tratta come errore.
        assertThat(AiProvider.OPENAI.defaultModel).isNotEmpty()
        assertThat(service().isProviderReady()).isTrue()
    }
}
