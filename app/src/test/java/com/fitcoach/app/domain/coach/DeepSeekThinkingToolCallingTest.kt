package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.ChatCompletionRequest
import com.fitcoach.app.data.remote.ChatCompletionResponse
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.data.remote.ToolChoice
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.Schema
import com.fitcoach.app.domain.model.AiProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Verifica il protocollo DeepSeek Thinking + Tool Calls.
 *
 * Il provider simulato applica le due regole documentate da DeepSeek:
 *  1. con Thinking attivo `tool_choice` puo essere solo `auto`, altrimenti
 *     risponde 400 "Thinking mode does not support this tool_choice";
 *  2. con `tools` presenti il `reasoning_content` di TUTTI i turni precedenti
 *     deve essere reinserito integralmente, altrimenti risponde 400.
 *
 * Se il codice viola una delle due regole il test fallisce con HTTP 400, come
 * accadeva realmente in produzione.
 */
class DeepSeekThinkingToolCallingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** Strumento di lettura. */
    private class ReadTool(private val toolName: String, private val output: String) : CoachTool {
        var calls = 0
        override val name = toolName
        override val description = "lettura"
        override val parameters: JsonObject = Schema.obj()
        override suspend fun execute(arguments: JsonObject): String {
            calls++
            return output
        }
    }

    /** Strumento di consegna: rifiuta la prima volta, accetta la seconda. */
    private class SubmitTool(private val acceptFirst: Boolean = false) : CoachTool {
        var calls = 0
        override val name = "submit_full_program"
        override val description = "consegna il programma"
        override val parameters: JsonObject = Schema.obj()
        override suspend fun execute(arguments: JsonObject): String {
            calls++
            return if (acceptFirst || calls > 1) {
                """{"ok":true,"nutritionPlanId":31,"workoutPlanId":32,"avvisi":[]}"""
            } else {
                """{"ok":false,"violazioni":["giorno 0 kcal: attese 2000, ricevute 1500"]}"""
            }
        }
    }

    /**
     * Provider DeepSeek simulato che rifiuta con 400 le richieste non conformi
     * al protocollo Thinking + Tool Calls.
     */
    private class DeepSeekSimulator(
        private val script: MutableList<() -> Response<ChatCompletionResponse>>,
        private val thinkingSupported: Boolean = true
    ) : AiApi {
        val sent = mutableListOf<ChatCompletionRequest>()
        var rejectedForToolChoice = 0
        var rejectedForMissingReasoning = 0

        override suspend fun chatCompletions(
            url: String,
            authorization: String?,
            contentType: String,
            referer: String?,
            title: String?,
            body: ChatCompletionRequest
        ): Response<ChatCompletionResponse> {
            // L'orchestratore riusa la stessa MutableList tra i turni: senza
            // snapshot le asserzioni leggerebbero lo stato finale, non la
            // richiesta realmente inviata.
            sent += body.copy(messages = body.messages.toList())
            val thinkingOn = thinkingSupported && body.thinking?.type == "enabled"
            val hasTools = !body.tools.isNullOrEmpty()

            // Regola 1: Thinking attivo accetta solo tool_choice = auto.
            if (thinkingOn && body.toolChoice != null && body.toolChoice != ToolChoice.auto) {
                rejectedForToolChoice++
                return error400("Thinking mode does not support this tool_choice")
            }
            // Regola 2: con tools, ogni messaggio assistant deve riportare il
            // reasoning_content ricevuto dal provider.
            if (hasTools) {
                val missing = body.messages.withIndex().any { (_, m) ->
                    m.role == "assistant" && m.toolCalls != null && m.reasoningContent.isNullOrBlank()
                }
                if (missing) {
                    rejectedForMissingReasoning++
                    return error400(
                        "Missing reasoning_content in assistant message: it must be passed back " +
                            "when tools are provided."
                    )
                }
            }
            return script.removeAt(0).invoke()
        }

        private fun error400(message: String): Response<ChatCompletionResponse> =
            Response.error(400, """{"error":{"message":"$message"}}""".toResponseBody())
    }

    private fun deepSeekConfig() = AiProviderConfig(
        provider = AiProvider.DEEPSEEK,
        baseUrl = "https://api.deepseek.com/v1/",
        model = "deepseek-flash",
        apiKey = "test-key"
    )

    private fun toolCallResponse(
        name: String,
        args: String = "{}",
        reasoning: String? = "Sto valutando i dati reali dell'utente.",
        content: String? = null,
        id: String = "call_1"
    ) = Response.success(
        ChatCompletionResponse(
            model = "deepseek-flash",
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(
                        content = content?.let { JsonPrimitive(it) },
                        reasoningContent = reasoning,
                        toolCalls = listOf(
                            ChatCompletionResponse.ToolCall(
                                id = id,
                                type = "function",
                                function = ChatCompletionResponse.FunctionCall(
                                    name = name,
                                    arguments = args
                                )
                            )
                        )
                    ),
                    finishReason = "tool_calls"
                )
            )
        )
    )

    private fun textResponse(
        text: String,
        reasoning: String? = "Ho completato l'analisi."
    ) = Response.success(
        ChatCompletionResponse(
            model = "deepseek-flash",
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(
                        content = JsonPrimitive(text),
                        reasoningContent = reasoning
                    ),
                    finishReason = "stop"
                )
            )
        )
    )

    private fun payloadOf(request: ChatCompletionRequest): String =
        json.encodeToString(ChatCompletionRequest.serializer(), request)

    // ---------------------------------------------------------------- test 1
    @Test
    fun `nutrition generation runs in thinking mode with auto tool choice`() = runTest {
        val foods = ReadTool("list_foods", """{"ok":true,"alimenti":["riso","pollo"]}""")
        val submit = SubmitTool(acceptFirst = true)
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("list_foods", reasoning = "Leggo il catalogo alimenti.") },
                { toolCallResponse("submit_full_program", reasoning = "Consegno i 7 giorni.") },
                { textResponse("Programma consegnato.") }
            )
        )
        val registry = CoachToolRegistry(setOf(foods, submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera nutrizione",
            requireTool = "submit_full_program"
        )

        assertThat(result.providerError).isNull()
        assertThat(api.rejectedForToolChoice).isEqualTo(0)
        assertThat(api.rejectedForMissingReasoning).isEqualTo(0)
        assertThat(foods.calls).isEqualTo(1)
        assertThat(submit.calls).isEqualTo(1)
        // La prima richiesta usa Thinking attivo e tool_choice auto.
        assertThat(api.sent.first().thinking?.type).isEqualTo("enabled")
        assertThat(api.sent.first().toolChoice).isEqualTo(ToolChoice.auto)
    }

    // ---------------------------------------------------------------- test 2
    @Test
    fun `workout generation runs in thinking mode with auto tool choice`() = runTest {
        val exercises = ReadTool("list_exercises", """{"ok":true,"esercizi":["panca","squat"]}""")
        val validate = ReadTool("validate_workout_plan", """{"ok":true}""")
        val submit = SubmitTool(acceptFirst = true)
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("list_exercises", reasoning = "Leggo gli esercizi disponibili.") },
                { toolCallResponse("validate_workout_plan", reasoning = "Verifico il volume.") },
                { toolCallResponse("submit_full_program", reasoning = "Scheda pronta.") },
                { textResponse("Scheda consegnata.") }
            )
        )
        val registry = CoachToolRegistry(setOf(exercises, validate, submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera scheda",
            requireTool = "submit_full_program"
        )

        assertThat(result.providerError).isNull()
        assertThat(api.rejectedForToolChoice).isEqualTo(0)
        assertThat(exercises.calls).isEqualTo(1)
        assertThat(validate.calls).isEqualTo(1)
        assertThat(submit.calls).isEqualTo(1)
        assertThat(api.sent.all { it.thinking?.type == "enabled" }).isTrue()
        assertThat(api.sent.all { it.toolChoice == ToolChoice.auto }).isTrue()
    }

    // ---------------------------------------------------------------- test 3
    @Test
    fun `multiple consecutive tool calls keep working`() = runTest {
        val first = ReadTool("compute_energy_targets", """{"ok":true,"kcal":2500}""")
        val second = ReadTool("compute_carb_cycling", """{"ok":true,"high":300}""")
        val third = ReadTool("compute_weekly_volume", """{"ok":true,"serie":62}""")
        val submit = SubmitTool(acceptFirst = true)
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("compute_energy_targets", reasoning = "Calcolo il TDEE.", id = "c1") },
                { toolCallResponse("compute_carb_cycling", reasoning = "Ciclicizzo i carboidrati.", id = "c2") },
                { toolCallResponse("compute_weekly_volume", reasoning = "Verifico il volume.", id = "c3") },
                { toolCallResponse("submit_full_program", reasoning = "Consegno.", id = "c4") },
                { textResponse("Fatto.") }
            )
        )
        val registry = CoachToolRegistry(setOf(first, second, third, submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera", requireTool = "submit_full_program"
        )

        assertThat(result.providerError).isNull()
        assertThat(result.toolTrace).containsExactly(
            "compute_energy_targets", "compute_carb_cycling", "compute_weekly_volume", "submit_full_program"
        ).inOrder()
        assertThat(api.rejectedForMissingReasoning).isEqualTo(0)
        assertThat(api.sent.size).isEqualTo(5)
    }

    // ---------------------------------------------------------------- test 4
    @Test
    fun `reasoning content is preserved across every subsequent request`() = runTest {
        val read = ReadTool("get_anamnesis", """{"ok":true,"eta":30}""")
        val submit = SubmitTool(acceptFirst = true)
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("get_anamnesis", reasoning = "PRIMO_RAGIONAMENTO", id = "c1") },
                { toolCallResponse("submit_full_program", reasoning = "SECONDO_RAGIONAMENTO", id = "c2") },
                { textResponse("Ok.") }
            )
        )
        val registry = CoachToolRegistry(setOf(read, submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera", requireTool = "submit_full_program"
        )

        // Il secondo invio deve contenere il reasoning del primo turno.
        val second = payloadOf(api.sent[1])
        assertThat(second).contains("PRIMO_RAGIONAMENTO")
        // Il terzo invio deve contenere entrambi i ragionamenti.
        val third = payloadOf(api.sent[2])
        assertThat(third).contains("PRIMO_RAGIONAMENTO")
        assertThat(third).contains("SECONDO_RAGIONAMENTO")
    }

    // ---------------------------------------------------------------- test 5
    @Test
    fun `tool results are matched to the correct tool_call_id`() = runTest {
        val a = ReadTool("list_foods", """{"ok":true,"quanti":3}""")
        val b = ReadTool("list_exercises", """{"ok":true,"quanti":5}""")
        val submit = SubmitTool(acceptFirst = true)
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("list_foods", reasoning = "catalogo cibo", id = "call_food") },
                { toolCallResponse("list_exercises", reasoning = "catalogo esercizi", id = "call_ex") },
                { toolCallResponse("submit_full_program", reasoning = "consegna", id = "call_submit") },
                { textResponse("Ok.") }
            )
        )
        val registry = CoachToolRegistry(setOf(a, b, submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera", requireTool = "submit_full_program"
        )

        // Dopo la prima chiamata la richiesta contiene solo il risultato di call_food.
        val afterFirst = api.sent[1].messages.filter { it.role == "tool" }
        assertThat(afterFirst.map { it.toolCallId }).containsExactly("call_food")
        assertThat(afterFirst.single().name).isEqualTo("list_foods")
        assertThat(afterFirst.single().content).contains("quanti")
        // Dopo la seconda chiamata ci sono entrambi, ciascuno col proprio id.
        val afterSecond = api.sent[2].messages.filter { it.role == "tool" }
        assertThat(afterSecond.map { it.toolCallId }).containsExactly("call_food", "call_ex").inOrder()
        assertThat(afterSecond.single { it.toolCallId == "call_ex" }.name).isEqualTo("list_exercises")
    }

    // ---------------------------------------------------------------- test 6
    @Test
    fun `submit_full_program is delivered and validated`() = runTest {
        val submit = SubmitTool(acceptFirst = true)
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("submit_full_program", reasoning = "Consegno il programma completo.") },
                { textResponse("Programma salvato.") }
            )
        )
        val registry = CoachToolRegistry(setOf(submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera", requireTool = "submit_full_program"
        )

        assertThat(submit.calls).isEqualTo(1)
        assertThat(result.providerError).isNull()
        assertThat(result.toolResults.map { it.name }).contains("submit_full_program")
        assertThat(result.toolResults.last().output).contains("\"ok\":true")
        assertThat(result.toolResults.last().output).contains("nutritionPlanId")
    }

    // ---------------------------------------------------------------- test 7
    @Test
    fun `rejected submission is corrected without any HTTP 400`() = runTest {
        // Il primo submit viene respinto dalla validazione (ok=false): il modello
        // riceve la diagnosi e corregge. Nessuna richiesta deve essere rifiutata
        // dal provider per tool_choice o reasoning mancante.
        val submit = SubmitTool(acceptFirst = false)
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("submit_full_program", reasoning = "Prima proposta.", id = "s1") },
                { toolCallResponse("submit_full_program", reasoning = "Correggo le calorie.", id = "s2") },
                { textResponse("Programma corretto.") }
            )
        )
        val registry = CoachToolRegistry(setOf(submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera", requireTool = "submit_full_program"
        )

        assertThat(submit.calls).isEqualTo(2)
        assertThat(result.providerError).isNull()
        assertThat(api.rejectedForToolChoice).isEqualTo(0)
        assertThat(api.rejectedForMissingReasoning).isEqualTo(0)
        // La diagnosi del rifiuto deve essere stata restituita al modello.
        val secondRequest = payloadOf(api.sent[1])
        assertThat(secondRequest).contains("giorno 0 kcal")
    }

    // ---------------------------------------------------------------- test 8
    @Test
    fun `provider error is surfaced clearly and never replaced by an offline plan`() = runTest {
        val api = DeepSeekSimulator(
            mutableListOf(
                {
                    Response.error(
                        401,
                        """{"error":{"message":"Authentication Fails"}}""".toResponseBody()
                    )
                }
            )
        )
        val registry = CoachToolRegistry(setOf(ReadTool("list_foods", """{"ok":true}""")))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera", requireTool = "submit_full_program"
        )

        assertThat(result.providerError).isNotNull()
        assertThat(result.providerError).contains("Authentication Fails")
        assertThat(result.toolResults).isEmpty()
        // Nessuna proposta registrata: l'errore non produce un piano.
        assertThat(result.pendingChangeIds).isEmpty()
    }

    // ---------------------------------------------------------------- test 9
    @Test
    fun `no program is applied when the model never submits`() = runTest {
        val api = DeepSeekSimulator(
            MutableList(6) { { textResponse("Ecco alcune idee, ma non consegno nulla.") } }
        )
        val submit = SubmitTool(acceptFirst = true)
        val registry = CoachToolRegistry(setOf(submit))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera",
            maxIterations = 4, requireTool = "submit_full_program"
        )

        assertThat(submit.calls).isEqualTo(0)
        assertThat(result.providerError).contains("non ha consegnato il programma")
        assertThat(result.toolResults).isEmpty()
        assertThat(result.pendingChangeIds).isEmpty()
    }

    // ------------------------------------------------------- protocol details
    @Test
    fun `forced tool choice is never sent while thinking is enabled`() = runTest {
        // Il modello ignora il primo sollecito: l'orchestratore deve degradare a
        // non-thinking SOLO per la richiesta forzata, non inviare mai una scelta
        // forzata con Thinking attivo.
        val api = DeepSeekSimulator(
            mutableListOf(
                { textResponse("Ci penso...") },
                { toolCallResponse("submit_full_program", reasoning = "Ora consegno.", id = "f1") },
                { textResponse("Ok.") }
            )
        )
        val registry = CoachToolRegistry(setOf(SubmitTool(acceptFirst = true)))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(
            deepSeekConfig(), "sys", "ctx", emptyList(), "genera", requireTool = "submit_full_program"
        )

        assertThat(api.rejectedForToolChoice).isEqualTo(0)
        assertThat(result.providerError).isNull()
        // Prima richiesta: Thinking attivo, scelta auto.
        assertThat(api.sent[0].thinking?.type).isEqualTo("enabled")
        assertThat(api.sent[0].toolChoice).isEqualTo(ToolChoice.auto)
        // Richiesta forzata: Thinking disattivato per quella singola chiamata.
        assertThat(api.sent[1].thinking?.type).isEqualTo("disabled")
        assertThat(api.sent[1].toolChoice).isNotEqualTo(ToolChoice.auto)
    }

    @Test
    fun `thinking field is not sent to providers that do not support it`() = runTest {
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("list_foods", reasoning = null, id = "o1") },
                { textResponse("Ok.", reasoning = null) }
            ),
            thinkingSupported = false
        )
        val registry = CoachToolRegistry(setOf(ReadTool("list_foods", """{"ok":true}""")))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        orchestrator.run(
            AiProviderConfig(
                provider = AiProvider.OPENAI,
                baseUrl = "https://api.openai.com/v1/",
                model = "gpt-4o-mini",
                apiKey = "k"
            ),
            "sys", "ctx", emptyList(), "x"
        )

        assertThat(api.sent.all { it.thinking == null }).isTrue()
    }

    @Test
    fun `reasoning is never used as the final answer when tool calls are present`() = runTest {
        // Il modello restituisce reasoning + tool call e content vuoto: il
        // reasoning NON deve diventare il messaggio mostrato all'utente.
        val api = DeepSeekSimulator(
            mutableListOf(
                { toolCallResponse("list_foods", reasoning = "RAGIONAMENTO_INTERNO", content = null) },
                { textResponse("Ecco il catalogo.", reasoning = "FINE") }
            )
        )
        val registry = CoachToolRegistry(setOf(ReadTool("list_foods", """{"ok":true}""")))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)

        val result = orchestrator.run(deepSeekConfig(), "sys", "ctx", emptyList(), "x")

        assertThat(result.text).isEqualTo("Ecco il catalogo.")
        assertThat(result.text).doesNotContain("RAGIONAMENTO_INTERNO")
    }

    @Test
    fun `client retries with auto when the provider rejects a forced tool choice`() = runTest {
        // Difesa in profondita: anche se una richiesta forzata arrivasse al client
        // con Thinking attivo, il client corregge e ripete con "auto".
        val api = DeepSeekSimulator(
            mutableListOf(
                { textResponse("Recuperato con auto.", reasoning = "r") }
            )
        )
        val client = AiClient(api, json)

        val result = client.complete(
            deepSeekConfig(),
            listOf(ChatMessageDto(role = "user", content = "ciao")),
            tools = listOf(
                com.fitcoach.app.data.remote.ToolDefinition(
                    function = com.fitcoach.app.data.remote.FunctionDefinition(
                        name = "list_foods",
                        description = "d",
                        parameters = Schema.obj()
                    )
                )
            ),
            toolChoice = ToolChoice.force("list_foods"),
            thinking = ChatCompletionRequest.ThinkingDirective.enabled
        )

        assertThat(result).isInstanceOf(com.fitcoach.app.data.remote.AiResult.Success::class.java)
        assertThat(api.rejectedForToolChoice).isEqualTo(1)
        assertThat(api.sent).hasSize(2)
        assertThat(api.sent[1].toolChoice).isEqualTo(ToolChoice.auto)
    }
}
