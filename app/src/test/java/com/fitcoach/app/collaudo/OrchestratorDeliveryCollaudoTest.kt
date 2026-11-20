package com.fitcoach.app.collaudo

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.Schema
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.textResponse
import com.fitcoach.app.support.toolCallResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test

/**
 * Robustezza del ciclo agentico quando la consegna dello strumento obbligatorio
 * non va a buon fine: e la causa del blocco "Generazione..." senza esito.
 */
class OrchestratorDeliveryCollaudoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val config = AiProviderConfig(
        provider = AiProvider.CUSTOM,
        baseUrl = "https://provider.test/v1",
        model = "test-model",
        apiKey = "test-key"
    )

    /** Strumento obbligatorio che risponde con l'esito deciso dal test. */
    private class StubTool(
        private val output: String,
        private val seen: MutableList<JsonObject> = mutableListOf()
    ) : CoachTool {
        override val name = "submit"
        override val description = "Consegna il programma."
        override val parameters: JsonObject = Schema.obj()
        override suspend fun execute(arguments: JsonObject): String {
            seen += arguments
            return output
        }
    }

    private fun orchestrator(tool: CoachTool, api: ScriptedApi) = CoachOrchestrator(
        AiClient(api, json),
        CoachToolRegistry(setOf(tool)),
        json
    )

    private suspend fun run(orch: CoachOrchestrator) = orch.run(
        config = config,
        systemPrompt = "system",
        contextPrompt = "context",
        history = emptyList(),
        userMessage = "genera",
        maxIterations = 4,
        requireTool = "submit"
    )

    @Test
    fun `una consegna respinta non chiude il turno come riuscita`() = runTest {
        val tool = StubTool("""{"ok":false,"error":"giorno 3 senza pasti"}""")
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Non riesco a completare.")
            )
        )

        val result = run(orchestrator(tool, api))

        // Lo strumento e stato eseguito ma la proposta e invalida: il turno deve
        // riportare l'errore, non un successo silenzioso.
        assertThat(result.toolResults.map { it.name }).contains("submit")
        assertThat(result.providerError).isNotNull()
    }

    @Test
    fun `gli argomenti troncati non vengono eseguiti come oggetto vuoto`() = runTest {
        val seen = mutableListOf<JsonObject>()
        val tool = StubTool("""{"ok":true}""", seen)
        val api = ScriptedApi(
            mutableListOf(
                // JSON interrotto a meta: tipico di finish_reason=length.
                toolCallResponse("submit", """{"giorni":[{"n":1,"pasti":"""),
                textResponse("Fine.")
            )
        )

        val result = run(orchestrator(tool, api))

        // Eseguire lo strumento con {} produrrebbe una diagnosi fuorviante su un
        // payload che il modello non ha mai completato.
        assertThat(seen).isEmpty()
        assertThat(result.toolResults.single().output).contains("troncati")
    }

    @Test
    fun `una consegna valida chiude il turno con successo`() = runTest {
        val tool = StubTool("""{"ok":true}""")
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Programma consegnato e verificato.")
            )
        )

        val result = run(orchestrator(tool, api))

        assertThat(result.providerError).isNull()
        assertThat(result.toolResults.map { it.name }).containsExactly("submit")
    }

    @Test
    fun `la deadline interrompe un turno che non consegna mai`() = runTest {
        val tool = StubTool("""{"ok":true}""")
        // Il modello continua a rispondere in prosa senza mai chiamare lo strumento.
        val api = ScriptedApi(
            MutableList(8) { textResponse("Ci sto lavorando.") }
        )

        val result = orchestrator(tool, api).run(
            config = config,
            systemPrompt = "system",
            contextPrompt = "context",
            history = emptyList(),
            userMessage = "genera",
            maxIterations = 8,
            requireTool = "submit",
            // 0 = deadline gia scaduta: il turno deve fermarsi prima di consumare
            // altre richieste, senza dipendere dal tempo reale del test.
            deadlineMs = 0
        )

        assertThat(result.providerError).contains("non ha completato")
        assertThat(result.toolResults).isEmpty()
    }

    @Test
    fun `un provider che non risponde mai chiude il turno invece di restare appeso`() = runTest {
        val tool = StubTool("""{"ok":true}""")
        // L'API non risponde mai: simula un endpoint che accetta la connessione e
        // non invia nulla. Prima il deadline era controllato solo TRA le
        // iterazioni, quindi questa chiamata non scadeva mai e la schermata
        // restava su "Generazione..." per sempre.
        val api = ScriptedApi(mutableListOf()).apply { hangForever = true }

        val result = orchestrator(tool, api).run(
            config = config,
            systemPrompt = "system",
            contextPrompt = "context",
            history = emptyList(),
            userMessage = "genera",
            maxIterations = 8,
            requireTool = "submit",
            deadlineMs = 5_000
        )

        // Il turno deve chiudersi con un errore leggibile entro il budget, non
        // consumare tutte le iterazioni ne attendere all'infinito.
        assertThat(result.providerError).contains("non ha completato")
        assertThat(result.toolResults).isEmpty()
    }
}
