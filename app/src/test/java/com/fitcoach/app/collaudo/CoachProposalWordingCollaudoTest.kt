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
 * Il coach non deve dire "ho modificato" quando ha solo proposto.
 *
 * Gli strumenti `propose_*` registrano una proposta che l'utente conferma: la
 * modifica non e' attiva. Il modello pero' scriveva comunque "ho aggiornato il
 * piano", e l'utente credeva alla modifica. Il testo viene ora corretto dal
 * codice, senza dipendere dalla disciplina del modello.
 */
class CoachProposalWordingCollaudoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val config = AiProviderConfig(
        provider = AiProvider.CUSTOM,
        baseUrl = "https://provider.test/v1",
        model = "test-model",
        apiKey = "test-key"
    )

    private class StubTool(
        override val name: String,
        private val output: String,
        /** I `propose_*` sono strumenti di scrittura: registrano una proposta. */
        override val isWrite: Boolean = false
    ) : CoachTool {
        override val description = "Strumento di prova."
        override val parameters: JsonObject = Schema.obj()
        override suspend fun execute(arguments: JsonObject): String = output
    }

    private fun orchestrator(vararg tools: CoachTool, api: ScriptedApi) = CoachOrchestrator(
        AiClient(api, json),
        CoachToolRegistry(tools.toSet()),
        json
    )

    private suspend fun run(orch: CoachOrchestrator, message: String = "togli il pollo") = orch.run(
        config = config,
        systemPrompt = "system",
        contextPrompt = "context",
        history = emptyList(),
        userMessage = message,
        maxIterations = 3
    )

    @Test
    fun `una proposta registrata non viene presentata come modifica applicata`() = runTest {
        val propose = StubTool(
            "propose_food_substitution",
            """{"ok":true,"stato":"PROPOSTA_REGISTRATA","changeId":7}""",
            isWrite = true
        )
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_food_substitution", """{"entryId":1}"""),
                textResponse("Ho modificato il piano: il pollo e' stato sostituito con il tacchino.")
            )
        )

        val result = run(orchestrator(propose, api = api))

        assertThat(result.pendingChangeIds).containsExactly(7L)
        assertThat(result.text).doesNotContain("Ho modificato")
        assertThat(result.text).contains("conferma")
    }

    @Test
    fun `senza proposta il testo non puo affermare che il piano e stato cambiato`() = runTest {
        val api = ScriptedApi(
            mutableListOf(textResponse("Ho aggiornato le calorie del piano come richiesto."))
        )

        val result = run(orchestrator(api = api))

        assertThat(result.pendingChangeIds).isEmpty()
        assertThat(result.text).doesNotContain("Ho aggiornato")
        assertThat(result.text).contains("Non ho applicato nessuna modifica")
    }

    @Test
    fun `un testo che non dichiara modifiche resta intatto`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                textResponse("Per la cena puoi usare il tacchino: stessi grammi, stessa proteina.")
            )
        )

        val result = run(orchestrator(api = api))

        assertThat(result.text)
            .isEqualTo("Per la cena puoi usare il tacchino: stessi grammi, stessa proteina.")
    }

    @Test
    fun `una proposta senza testo riceve comunque l istruzione di conferma`() {
        val orch = orchestrator(api = ScriptedApi(mutableListOf()))

        val text = orch.clarifyPendingProposal("", hasPending = true)

        assertThat(text).contains("proposta")
        assertThat(text).contains("Confermala")
    }
}
