package com.fitcoach.app.collaudo

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.textResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Il coach non deve dichiarare applicata una modifica che non esiste.
 *
 * Difetto segnalato: chiedendo in chat di modificare piano alimentare o scheda,
 * l'assistente rispondeva "modificato" senza che nulla cambiasse. La guardia
 * gia esistente riconosceva "ho modificato" ma non le forme piu comuni in
 * italiano: "procedo alla modifica", "modifica eseguita", "vado a modificare".
 * Con quelle frasi il testo passava intatto e l'utente credeva alla modifica.
 */
class CoachFalseApplyClaimCollaudoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val config = AiProviderConfig(
        provider = AiProvider.CUSTOM,
        baseUrl = "https://provider.test/v1",
        model = "test-model",
        apiKey = "test-key"
    )

    private fun orchestrator(api: ScriptedApi) = CoachOrchestrator(
        AiClient(api, json),
        CoachToolRegistry(emptySet()),
        json
    )

    private suspend fun replyTo(text: String): String {
        val orch = orchestrator(ScriptedApi(mutableListOf(textResponse(text))))
        return orch.run(
            config = config,
            systemPrompt = "system",
            contextPrompt = "context",
            history = emptyList(),
            userMessage = "modifica il piano alimentare"
        ).text
    }

    @Test
    fun `le forme verbali di modifica avvenuta vengono riconosciute`() = runTest {
        val claims = listOf(
            "Ho modificato il piano alimentare.",
            "Procedo alla modifica del piano.",
            "Procedo con la modifica della scheda.",
            "Vado a modificare gli esercizi.",
            "Modifica eseguita.",
            "La modifica e stata applicata.",
            "Ho aggiornato la scheda.",
            "Sto aggiornando il piano alimentare.",
            "Modifica fatta."
        )

        claims.forEach { claim ->
            val text = replyTo(claim)
            // Nessuna proposta registrata in questo turno: ogni affermazione di
            // modifica e' falsa e il testo non deve poterla contenere.
            assertThat(text).doesNotContain(claim)
            assertThat(text).contains("Non ho applicato nessuna modifica")
        }
    }

    @Test
    fun `una domanda o un annuncio al futuro non vengono riscritti`() = runTest {
        // Queste frasi sono corrette: riscriverle renderebbe la guardia un
        // ostacolo invece che una protezione.
        val legitimate = listOf(
            "Vuoi che proceda con la modifica?",
            "La modifica verra applicata dopo la tua conferma.",
            "Ecco il piano alimentare della settimana."
        )

        legitimate.forEach { phrase ->
            assertThat(replyTo(phrase)).isEqualTo(phrase)
        }
    }
}
