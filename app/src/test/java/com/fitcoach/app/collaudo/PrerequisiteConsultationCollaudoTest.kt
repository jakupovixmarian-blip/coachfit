package com.fitcoach.app.collaudo

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.Prerequisite
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
 * Il corpus InVictus deve essere consultato, non solo citato nel prompt.
 *
 * Tre prove live su tre hanno mostrato `toolTrace=[submit_full_program]`: il
 * modello ignorava `consult_invictus` perche' uno strumento obbligatorio in
 * concorrenza vince su uno suggerito in prosa. Il prerequisito va quindi imposto
 * dal codice, e va imposto PRIMA dell'esecuzione: `submit_full_program` salva il
 * programma quando risponde ok, quindi un controllo successivo arriverebbe a
 * database gia' scritto.
 */
class PrerequisiteConsultationCollaudoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val config = AiProviderConfig(
        provider = AiProvider.CUSTOM,
        baseUrl = "https://provider.test/v1",
        model = "test-model",
        apiKey = "test-key"
    )

    private class StubTool(
        override val name: String,
        private val output: String
    ) : CoachTool {
        var executions = 0
        override val description = "Strumento di prova."
        override val parameters: JsonObject = Schema.obj()
        override suspend fun execute(arguments: JsonObject): String {
            executions++
            return output
        }
    }

    private fun orchestrator(vararg tools: CoachTool, api: ScriptedApi) = CoachOrchestrator(
        AiClient(api, json),
        CoachToolRegistry(tools.toSet()),
        json
    )

    private suspend fun run(orch: CoachOrchestrator) = orch.run(
        config = config,
        systemPrompt = "system",
        contextPrompt = "context",
        history = emptyList(),
        userMessage = "genera",
        maxIterations = 4,
        requireTool = "submit",
        requiredPrerequisites = listOf(
            Prerequisite.consultations(
                toolName = "consult",
                areas = listOf("allenamento"),
                description = "la metodologia"
            )
        )
    )

    @Test
    fun `la consegna non viene eseguita se il prerequisito non e stato consultato`() = runTest {
        val consult = StubTool("consult", """{"ok":true,"area":"allenamento","regole":[{"regola":"x"}]}""")
        val submit = StubTool("submit", """{"ok":true,"stato":"PROGRAMMA_SALVATO"}""")
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit", """{"giorni":[]}"""),
                toolCallResponse("consult", """{"area":"allenamento"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Fatto.")
            )
        )

        val result = run(orchestrator(consult, submit, api = api))

        // Il punto: la prima submit non deve aver scritto nulla.
        assertThat(submit.executions).isEqualTo(1)
        assertThat(result.toolTrace).containsExactly("submit", "consult", "submit")
        // Il rifiuto deve spiegare al modello cosa fare, non solo negare.
        val rejection = result.toolResults.first().output
        assertThat(rejection).contains("consult")
        assertThat(rejection).contains("Prima di consegnare")
    }

    @Test
    fun `una consultazione vuota non soddisfa il prerequisito`() = runTest {
        val consult = StubTool("consult", """{"ok":true,"area":"allenamento","regole":[]}""")
        val submit = StubTool("submit", """{"ok":true,"stato":"PROGRAMMA_SALVATO"}""")
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("consult", """{"area":"allenamento"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                toolCallResponse("consult", """{"area":"allenamento"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Fatto.")
            )
        )

        val result = run(orchestrator(consult, submit, api = api))

        // ok=true ma zero regole non e' una consultazione: la submit non viene
        // MAI eseguita, quindi non salva nulla e il turno finisce in errore
        // invece di scrivere un programma costruito su una ricerca vuota.
        assertThat(submit.executions).isEqualTo(0)
        assertThat(result.toolResults.none { it.output.contains("PROGRAMMA_SALVATO") }).isTrue()
        assertThat(result.toolTrace).containsExactly("consult", "submit", "consult", "submit")
    }

    @Test
    fun `con il prerequisito consultato la consegna passa al primo colpo`() = runTest {
        val consult = StubTool("consult", """{"ok":true,"area":"allenamento","regole":[{"regola":"x"}]}""")
        val submit = StubTool("submit", """{"ok":true,"stato":"PROGRAMMA_SALVATO"}""")
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("consult", """{"area":"allenamento"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Fatto.")
            )
        )

        val result = run(orchestrator(consult, submit, api = api))

        assertThat(consult.executions).isEqualTo(1)
        assertThat(submit.executions).isEqualTo(1)
        assertThat(result.toolTrace).containsExactly("consult", "submit")
        assertThat(result.toolResults.last().output).contains("PROGRAMMA_SALVATO")
    }

    @Test
    fun `servono entrambe le aree non una sola`() = runTest {
        // Lo stub risponde in base all'area richiesta: solo allenamento.
        val consult = AreaAwareConsult()
        val submit = StubTool("submit", """{"ok":true,"stato":"PROGRAMMA_SALVATO"}""")
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("consult", """{"area":"allenamento"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                toolCallResponse("consult", """{"area":"nutrizione"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Fatto.")
            )
        )

        val result = orchestrator(consult, submit, api = api).run(
            config = config,
            systemPrompt = "system",
            contextPrompt = "context",
            history = emptyList(),
            userMessage = "genera",
            maxIterations = 5,
            requireTool = "submit",
            requiredPrerequisites = listOf(
                Prerequisite.consultations(
                    toolName = "consult",
                    areas = listOf("allenamento", "nutrizione"),
                    description = "metodologia"
                )
            )
        )

        // La prima submit e' bloccata perche' manca la nutrizione, la seconda passa.
        assertThat(submit.executions).isEqualTo(1)
        assertThat(result.toolTrace)
            .containsExactly("consult", "submit", "consult", "submit")
    }

    @Test
    fun `una consultazione con argomento di un'altra area non conta`() = runTest {
        val consult = AreaAwareConsult()
        val submit = StubTool("submit", """{"ok":true,"stato":"PROGRAMMA_SALVATO"}""")
        val api = ScriptedApi(
            mutableListOf(
                // area corretta ma argomento che non appartiene alla nutrizione
                toolCallResponse("consult", """{"area":"nutrizione","argomento":"petto"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                toolCallResponse("consult", """{"area":"allenamento"}"""),
                toolCallResponse("consult", """{"area":"nutrizione"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Fatto.")
            )
        )

        val result = orchestrator(consult, submit, api = api).run(
            config = config,
            systemPrompt = "system",
            contextPrompt = "context",
            history = emptyList(),
            userMessage = "genera",
            maxIterations = 6,
            requireTool = "submit",
            requiredPrerequisites = listOf(
                Prerequisite.consultations(
                    toolName = "consult",
                    areas = listOf("allenamento", "nutrizione"),
                    description = "metodologia"
                )
            )
        )

        assertThat(submit.executions).isEqualTo(1)
        assertThat(result.toolTrace.last()).isEqualTo("submit")
    }

    /** Restituisce regole solo se l'argomento appartiene all'area richiesta. */
    private class AreaAwareConsult : CoachTool {
        override val name = "consult"
        override val description = "Consulta la metodologia."
        override val parameters: JsonObject = Schema.obj()
        override suspend fun execute(arguments: JsonObject): String {
            val area = (arguments["area"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            val topic = (arguments["argomento"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            val nutritionTopics = setOf("proteine", "calorie", "carboidrati")
            val hasContent = when (area) {
                "allenamento" -> topic == null
                "nutrizione" -> topic == null || topic in nutritionTopics
                else -> false
            }
            return if (hasContent) {
                """{"ok":true,"area":"$area","regole":[{"regola":"x"}]}"""
            } else {
                """{"ok":false,"area":"$area","regole":[]}"""
            }
        }
    }

    @Test
    fun `il rifiuto nomina l'area mancante invece di chiedere genericamente la metodologia`() = runTest {
        val consult = StubTool("consult", """{"ok":true,"area":"allenamento","regole":[{"regola":"x"}]}""")
        val submit = StubTool("submit", """{"ok":true,"stato":"PROGRAMMA_SALVATO"}""")
        val api = ScriptedApi(
            mutableListOf(
                // Il modello consulta una sola area e prova a consegnare: senza il
                // dettaglio sull'area mancante ripeteva all'infinito la stessa.
                toolCallResponse("consult", """{"area":"allenamento"}"""),
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Fatto.")
            )
        )

        val result = orchestrator(consult, submit, api = api).run(
            config = config,
            systemPrompt = "system",
            contextPrompt = "context",
            history = emptyList(),
            userMessage = "genera",
            maxIterations = 3,
            requireTool = "submit",
            requiredPrerequisites = listOf(
                Prerequisite.consultations(
                    toolName = "consult",
                    areas = listOf("allenamento", "nutrizione", "tecniche"),
                    description = "metodologia"
                )
            )
        )

        // La consegna resta bloccata: il prerequisito non e' soddisfatto.
        assertThat(submit.executions).isEqualTo(0)
        // Ma il modello riceve i nomi esatti delle aree che gli mancano.
        val feedback = result.toolResults.joinToString { it.output }
        assertThat(feedback).contains("nutrizione")
        assertThat(feedback).contains("tecniche")
    }

    @Test
    fun `senza prerequisiti il comportamento resta quello di prima`() = runTest {
        val submit = StubTool("submit", """{"ok":true,"stato":"PROGRAMMA_SALVATO"}""")
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit", """{"giorni":[]}"""),
                textResponse("Fatto.")
            )
        )

        val result = orchestrator(submit, api = api).run(
            config = config,
            systemPrompt = "system",
            contextPrompt = "context",
            history = emptyList(),
            userMessage = "genera",
            maxIterations = 4,
            requireTool = "submit"
        )

        assertThat(submit.executions).isEqualTo(1)
        assertThat(result.toolTrace).containsExactly("submit")
    }
}
