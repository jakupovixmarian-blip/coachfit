package com.fitcoach.app.live

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.MIGRATION_1_2
import com.fitcoach.app.data.local.MIGRATION_2_3
import com.fitcoach.app.data.local.MIGRATION_3_4
import com.fitcoach.app.data.local.MIGRATION_4_5
import com.fitcoach.app.data.local.MIGRATION_5_6
import com.fitcoach.app.data.local.MIGRATION_6_7
import com.fitcoach.app.data.local.MIGRATION_7_8
import com.fitcoach.app.data.local.MIGRATION_8_9
import com.fitcoach.app.data.local.MIGRATION_9_10
import com.fitcoach.app.data.local.MIGRATION_10_11
import com.fitcoach.app.data.local.MIGRATION_11_12
import com.fitcoach.app.data.local.MIGRATION_12_13
import com.fitcoach.app.data.local.MIGRATION_13_14
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.domain.coach.AiCapabilityProbe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import com.google.common.truth.Truth.assertWithMessage

/**
 * Prova live del PULSANTE "Test connessione", non della sola generazione.
 *
 * `OpenRouterLiveProbe` salta il pannello impostazioni: scrive le impostazioni
 * via repository e chiama direttamente il servizio di generazione. Restava
 * quindi non verificato proprio il percorso che l'utente tocca per primo, cioe'
 * `AiProvidersViewModel.testConnection()` -> `AiCapabilityProbe.probe()`.
 *
 * La differenza non e' cosmetica: il probe esige una TOOL CALL, mentre un
 * "rispondi OK" passa anche su modelli che poi non producono strumenti, e
 * l'utente vedrebbe "Connessione riuscita" seguito da una generazione che non
 * arriva mai. Qui si verifica che il provider scelto sia davvero capace.
 *
 *   FITCOACH_LIVE_KEY=sk-or-... ./gradlew testDebugUnitTest \
 *     --tests "com.fitcoach.app.live.LiveProviderConnectionProbe"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LiveProviderConnectionProbe {

    private val model = System.getenv("FITCOACH_LIVE_MODEL") ?: "qwen/qwen3.5-35b-a3b"
    private val key = System.getenv("FITCOACH_LIVE_KEY") ?: ""

    @Test
    fun `il test connessione del pannello provider riesce sul provider reale`() = runBlocking {
        assumeTrue(
            "Serve FITCOACH_LIVE_KEY: prova manuale, non parte nella suite automatica.",
            key.isNotBlank()
        )
        val context = ApplicationProvider.getApplicationContext<Context>()

        val json = Json {
            ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AiApi::class.java)

        val prefs = context.getSharedPreferences("live_probe_keys", Context.MODE_PRIVATE)
        val keys = SecureKeyStore(context, prefs)
        keys.putApiKey(AiProvider.OPENROUTER.name, key)

        val config = AiProviderConfig(
            provider = AiProvider.OPENROUTER,
            baseUrl = "https://openrouter.ai/api/v1",
            model = model,
            apiKey = key,
            usesBackendProxy = false
        )

        val outcome = AiCapabilityProbe(AiClient(api, json)).probe(config)
        println("=== ESITO TEST CONNESSIONE: $outcome")

        // TextOnly e' un fallimento per la generazione, anche se il provider
        // risponde: la chat funzionerebbe, i programmi no.
        assertWithMessage("il provider deve essere pronto con tool calling attivo: $outcome")
            .that(outcome)
            .isInstanceOf(AiCapabilityProbe.Outcome.Ready::class.java)

        val ready = outcome as AiCapabilityProbe.Outcome.Ready
        println("=== MODELLO: ${ready.model} · LATENZA: ${ready.latencyMs} ms")
    }

    /**
     * Il DB su DISCO, non in memoria: `OpenRouterLiveProbe` usa
     * `inMemoryDatabaseBuilder`, quindi non tocca ne' le migrazioni ne' il file
     * reale. Qui si apre il database vero, si scrive e si rilegge dopo la
     * chiusura, che e' cio' che accade tra un avvio e l'altro dell'app.
     */
    @Test
    fun `il database su disco conserva il profilo tra due aperture`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(FitCoachDatabase.DB_NAME)

        val first = buildDatabase(context)
        val saved = com.fitcoach.app.domain.model.UserProfile(
            name = "Persistenza",
            ageYears = 30,
            heightCm = 175.0,
            weightKg = 78.0,
            anamnesisCompleted = true,
        )
        first.userProfileDao().upsert(saved.toEntity())
        first.close()

        val second = buildDatabase(context)
        val read = second.userProfileDao().get()?.toDomain()
        second.close()

        println("=== RILETTO DOPO RIAVVIO: ${read?.name} / ${read?.weightKg} kg")
        assertWithMessage("il profilo deve sopravvivere alla chiusura del database")
            .that(read?.name)
            .isEqualTo("Persistenza")
        assertWithMessage("il peso deve sopravvivere alla chiusura del database")
            .that(read?.weightKg)
            .isEqualTo(78.0)

        context.deleteDatabase(FitCoachDatabase.DB_NAME)
    }

    /**
     * Stesso builder della produzione (migrazioni incluse): un builder senza
     * migrazioni aprirebbe un DB che l'app non aprirebbe, e il test non
     * direbbe nulla sulla persistenza reale.
     */
    private fun buildDatabase(context: Context): FitCoachDatabase =
        Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14
            )
            .allowMainThreadQueries()
            .build()
}
