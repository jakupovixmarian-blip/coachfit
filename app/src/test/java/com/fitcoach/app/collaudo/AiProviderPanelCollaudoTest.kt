package com.fitcoach.app.collaudo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.SetupStateStore
import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.domain.coach.AiCapabilityProbe
import com.fitcoach.app.domain.coach.AiConfigResolver
import com.fitcoach.app.ui.settings.AiProvidersScreen
import com.fitcoach.app.ui.settings.AiProvidersViewModel
import com.fitcoach.app.ui.theme.FitCoachTheme
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * Pannello provider pilotato a TOCCHI, con "Testa connessione" premuto davvero.
 *
 * Il test di `AiClient` verifica la richiesta HTTP, ma non il pannello: che i
 * campi Base URL e Modello finiscano nella configurazione salvata, e che
 * l'esito del pulsante compaia a schermo, non era coperto da nulla.
 *
 * Il provider e' un server HTTP locale che risponde nel formato OpenAI
 * Chat Completions con una tool call: e' esattamente cio' che
 * `AiCapabilityProbe` pretende per dichiarare `Ready`. Un server che
 * rispondesse solo con testo produrrebbe `TextOnly`, cioe' "connessione
 * riuscita" e poi generazioni che non arrivano mai: qui si verifica anche
 * che quel caso NON venga spacciato per riuscito.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
class AiProviderPanelCollaudoTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: FitCoachDatabase
    private lateinit var socket: ServerSocket
    private lateinit var serverThread: Thread
    private lateinit var viewModel: AiProvidersViewModel
    private var rispondeConToolCall = true
    private val richieste = mutableListOf<String>()

    @Before
    fun setUp() {
        // Server HTTP minimale su ServerSocket: com.sun.net.httpserver non e'
        // nel classpath Android, e qui serve un vero endpoint HTTP (il pannello
        // parla via OkHttp/Retrofit, non con un mock dell'interfaccia).
        socket = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
        val porta = socket.localPort
        serverThread = Thread {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                client.use { c ->
                    val reader = c.getInputStream().bufferedReader()
                    val requestLine = reader.readLine() ?: return@use
                    var contentLength = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        if (line.lowercase().startsWith("content-length:")) {
                            contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                        }
                    }
                    val body = if (contentLength > 0) {
                        val buf = CharArray(contentLength)
                        var read = 0
                        while (read < contentLength) {
                            val n = reader.read(buf, read, contentLength - read)
                            if (n <= 0) break
                            read += n
                        }
                        String(buf, 0, read)
                    } else ""
                    synchronized(richieste) { richieste.add(body) }
                    val risposta = if (rispondeConToolCall) {
                        """{"id":"x","model":"test-model","choices":[{"index":0,"message":{"role":"assistant","content":null,"tool_calls":[{"id":"call_1","type":"function","function":{"name":"ping","arguments":"{\"value\":1}"}}]},"finish_reason":"tool_calls"}]}"""
                    } else {
                        """{"id":"x","model":"test-model","choices":[{"index":0,"message":{"role":"assistant","content":"OK"},"finish_reason":"stop"}]}"""
                    }
                    val bytes = risposta.toByteArray()
                    val out = c.getOutputStream()
                    out.write(
                        ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n" +
                            "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray()
                    )
                    out.write(bytes)
                    out.flush()
                    // La richiesta HTTP non serve a instradare: una sola rotta.
                    requestLine.length
                }
            }
        }.apply { isDaemon = true; start() }

        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()

        val json = Json {
            ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false
        }
        val api = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:${socket.localPort}/")
            .client(OkHttpClient.Builder().readTimeout(10, TimeUnit.SECONDS).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AiApi::class.java)

        val chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())
        val keys = SecureKeyStore(
            context,
            context.getSharedPreferences("panel_test_keys", Context.MODE_PRIVATE)
        )
        viewModel = AiProvidersViewModel(
            chatRepository = chatRepository,
            secureKeyStore = keys,
            configResolver = AiConfigResolver(chatRepository, keys),
            capabilityProbe = AiCapabilityProbe(AiClient(api, json)),
            setupStateStore = SetupStateStore(context),
        )

        composeRule.setContent {
            FitCoachTheme {
                AiProvidersScreen(navController = rememberNavController(), viewModel = viewModel)
            }
        }
        // Il pannello parte in caricamento e si popola in una coroutine.
        // Il pannello parte in caricamento e si popola in una coroutine.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Provider AI", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Il pannello e' una LazyColumn: cio' che sta sotto la piega non viene
     * composto, quindi il nodo non esiste finche' non si scorre. Senza questo
     * "Testa connessione" e "Salva" risultano assenti anche se sono nella
     * schermata.
     */
    private fun scorriA(testo: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(testo))
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        runCatching { socket.close() }
        db.close()
    }

    /**
     * Il percorso dell'utente: sceglie il provider, scrive chiave e modello,
     * preme "Testa connessione" e legge l'esito a schermo.
     */
    @Test
    fun `il pulsante testa connessione mostra esito positivo con un provider che usa gli strumenti`() {
        scorriA("Modello")
        composeRule.onNodeWithText("Modello").performTextReplacement("test-model")
        composeRule.waitForIdle()
        scorriA("API Key")
        composeRule.onNodeWithText("API Key").performTextReplacement("chiave-di-prova")
        composeRule.waitForIdle()

        // Base URL verso il server locale: senza questo il pannello chiamerebbe
        // OpenRouter e il test dipenderebbe dalla rete.
        scorriA("Base URL")
        composeRule.onNodeWithText("Base URL")
            .performTextReplacement("http://127.0.0.1:${socket.localPort}/v1")
        composeRule.waitForIdle()

        scorriA("Testa connessione")
        composeRule.onNodeWithText("Testa connessione").performClick()
        // L'app deve portare da sola l'esito in vista: qui non si scorre, si
        // verifica che l'utente lo veda senza cercarlo.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Connessione riuscita", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("✕", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        val esito = composeRule.onAllNodesWithText("Connessione riuscita", substring = true)
            .fetchSemanticsNodes()
        assertWithMessage(
            "il pannello deve mostrare la connessione riuscita; schermo: " +
                composeRule.onAllNodesWithText("✕", substring = true)
                    .fetchSemanticsNodes().size + " esiti negativi"
        ).that(esito).isNotEmpty()

        // La richiesta deve essere partita davvero, con la chiave e il modello scritti.
        assertWithMessage("nessuna richiesta HTTP inviata dal pulsante")
            .that(richieste).isNotEmpty()
        assertWithMessage("il modello scritto nel campo deve essere usato nella richiesta")
            .that(richieste.last()).contains("test-model")
    }

    /**
     * Un provider che risponde ma non produce tool call NON deve risultare
     * pronto: la chat funzionerebbe, la generazione dei programmi no. E' il
     * caso che l'utente vedrebbe come "Connessione riuscita" seguito dal nulla.
     */
    @Test
    fun `un provider senza tool calling non viene dichiarato pronto`() {
        rispondeConToolCall = false

        scorriA("Modello")
        composeRule.onNodeWithText("Modello").performTextReplacement("test-model")
        composeRule.waitForIdle()
        scorriA("API Key")
        composeRule.onNodeWithText("API Key").performTextReplacement("chiave-di-prova")
        composeRule.waitForIdle()
        scorriA("Base URL")
        composeRule.onNodeWithText("Base URL")
            .performTextReplacement("http://127.0.0.1:${socket.localPort}/v1")
        composeRule.waitForIdle()

        scorriA("Testa connessione")
        composeRule.onNodeWithText("Testa connessione").performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("✕", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        val negativi = composeRule.onAllNodesWithText("✕", substring = true)
            .fetchSemanticsNodes()
        assertWithMessage("un provider senza strumenti deve mostrare esito NEGATIVO")
            .that(negativi).isNotEmpty()
        assertWithMessage("non deve comparire 'Connessione riuscita'")
            .that(
                composeRule.onAllNodesWithText("Connessione riuscita", substring = true)
                    .fetchSemanticsNodes()
            ).isEmpty()
        assertWithMessage("il pannello deve spiegare che mancano gli strumenti")
            .that(
                composeRule.onAllNodesWithText("non usa gli strumenti", substring = true)
                    .fetchSemanticsNodes()
            ).isNotEmpty()
    }

    /** Salvare dal pannello deve persistere modello e base URL, non solo la chiave. */
    @Test
    fun `salvare dal pannello persiste modello e base url`() {
        composeRule.onNodeWithText("Modello").performTextReplacement("modello-salvato")
        composeRule.waitForIdle()
        scorriA("API Key")
        composeRule.onNodeWithText("API Key").performTextReplacement("chiave-di-prova")
        composeRule.waitForIdle()
        scorriA("Base URL")
        composeRule.onNodeWithText("Base URL")
            .performTextReplacement("http://127.0.0.1:${socket.localPort}/v1")
        composeRule.waitForIdle()
        scorriA("Salva")
        composeRule.onNodeWithText("Salva").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runBlocking { db.aiSettingsDao().get()?.model == "modello-salvato" }
        }
        val salvato = runBlocking { db.aiSettingsDao().get()!! }
        assertWithMessage("il modello scritto deve essere persistito")
            .that(salvato.model).isEqualTo("modello-salvato")
        assertWithMessage("il base url scritto deve essere persistito")
            .that(salvato.baseUrl).contains("127.0.0.1")
    }
}
