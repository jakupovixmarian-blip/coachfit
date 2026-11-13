package dev.fitcoach.app

import dev.fitcoach.app.domain.ai.AiClient
import dev.fitcoach.app.domain.ai.AiOrchestrator
import dev.fitcoach.app.domain.ai.Approval
import dev.fitcoach.app.domain.ai.ChatMessage
import dev.fitcoach.app.domain.ai.InMemoryToolAuditLog
import dev.fitcoach.app.domain.ai.ModelReply
import dev.fitcoach.app.domain.ai.OrchestratorOutcome
import dev.fitcoach.app.domain.ai.ProviderConfig
import dev.fitcoach.app.domain.ai.ProviderGateway
import dev.fitcoach.app.domain.ai.ProviderKind
import dev.fitcoach.app.domain.ai.ProviderManager
import dev.fitcoach.app.domain.ai.Tool
import dev.fitcoach.app.domain.ai.ToolArguments
import dev.fitcoach.app.domain.ai.ToolCallRequest
import dev.fitcoach.app.domain.ai.ToolEngine
import dev.fitcoach.app.domain.ai.ToolParameter
import dev.fitcoach.app.domain.ai.ToolParameterType
import dev.fitcoach.app.domain.ai.ToolSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The orchestrator is what makes the AI operational: it must use the tools, obey
 * the approval gate, and say clearly when no provider is usable.
 */
class AiOrchestratorTest {

    private var savedDiet: String? = null

    private class ScriptedClient(private val replies: List<ModelReply>) : AiClient {
        var calls = 0
            private set
        private var index = 0

        override suspend fun complete(
            config: ProviderConfig,
            messages: List<ChatMessage>,
            tools: List<ToolSpec>
        ): ModelReply {
            calls++
            val reply = replies[minOf(index, replies.lastIndex)]
            index++
            return reply
        }
    }

    private class FailingClient(private val message: String) : AiClient {
        override suspend fun complete(
            config: ProviderConfig,
            messages: List<ChatMessage>,
            tools: List<ToolSpec>
        ): ModelReply = throw IllegalStateException(message)
    }

    private val searchFood = object : Tool {
        override val spec = ToolSpec(
            name = "cerca_alimento",
            description = "Cerca un alimento",
            parameters = listOf(ToolParameter("query", ToolParameterType.STRING))
        )

        override suspend fun invoke(arguments: ToolArguments): String =
            "mela: 52 kcal per 100 g"
    }

    private val saveDiet = object : Tool {
        override val spec = ToolSpec(
            name = "salva_dieta",
            description = "Salva la dieta",
            parameters = listOf(ToolParameter("piano", ToolParameterType.STRING)),
            requiresApproval = true,
            mutatesData = true
        )

        override suspend fun invoke(arguments: ToolArguments): String {
            savedDiet = arguments.string("piano")
            return "dieta salvata"
        }
    }

    private fun openAiConfig() = ProviderConfig(
        id = "openai",
        kind = ProviderKind.OPENAI,
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        modelId = "gpt-4o-mini",
        apiKey = "chiave-di-test"
    )

    private fun orchestrator(
        client: AiClient,
        kind: ProviderKind = ProviderKind.OPENAI,
        config: ProviderConfig = openAiConfig()
    ): AiOrchestrator = AiOrchestrator(
        providers = ProviderManager(listOf(config), primaryId = config.id),
        gateway = ProviderGateway(mapOf(kind to client)),
        tools = ToolEngine(listOf(searchFood, saveDiet), InMemoryToolAuditLog()) { 7L }
    )

    @Test
    fun `una risposta senza strumenti viene restituita`() = runBlocking {
        val client = ScriptedClient(listOf(ModelReply(text = "Ciao, come posso aiutarti?")))
        val outcome = orchestrator(client).ask("ciao", "sei un coach")

        assertTrue(outcome is OrchestratorOutcome.Answer)
        assertEquals("Ciao, come posso aiutarti?", (outcome as OrchestratorOutcome.Answer).text)
        assertTrue(outcome.toolsUsed.isEmpty())
    }

    @Test
    fun `lo strumento richiesto dal modello viene eseguito e il risultato torna al modello`() = runBlocking {
        val client = ScriptedClient(
            listOf(
                ModelReply(toolCalls = listOf(ToolCallRequest("1", "cerca_alimento", mapOf("query" to "mela")))),
                ModelReply(text = "La mela ha 52 kcal per 100 g.")
            )
        )
        val outcome = orchestrator(client).ask("quante calorie ha una mela?", "sei un coach")

        assertTrue(outcome is OrchestratorOutcome.Answer)
        assertEquals(listOf("cerca_alimento"), (outcome as OrchestratorOutcome.Answer).toolsUsed)
        assertEquals(2, client.calls)
    }

    @Test
    fun `un'operazione che modifica dati si ferma in attesa di approvazione`() = runBlocking {
        val client = ScriptedClient(
            listOf(
                ModelReply(toolCalls = listOf(ToolCallRequest("1", "salva_dieta", mapOf("piano" to "7 giorni")))),
                ModelReply(text = "fatto")
            )
        )
        val outcome = orchestrator(client).ask("salva la dieta", "sei un coach")

        assertTrue(outcome is OrchestratorOutcome.NeedsApproval)
        assertNull(savedDiet)
        assertEquals("salva_dieta", (outcome as OrchestratorOutcome.NeedsApproval).pending.toolName)
    }

    @Test
    fun `con l'approvazione la modifica viene applicata`() = runBlocking {
        val client = ScriptedClient(
            listOf(
                ModelReply(toolCalls = listOf(ToolCallRequest("1", "salva_dieta", mapOf("piano" to "7 giorni")))),
                ModelReply(text = "fatto"),
                ModelReply(toolCalls = listOf(ToolCallRequest("1", "salva_dieta", mapOf("piano" to "7 giorni")))),
                ModelReply(text = "dieta salvata")
            )
        )
        val orchestrator = orchestrator(client)
        orchestrator.ask("salva la dieta", "sei un coach")
        val outcome = orchestrator.ask(
            "salva la dieta",
            "sei un coach",
            approvals = mapOf("salva_dieta" to Approval.Approved)
        )

        assertTrue(outcome is OrchestratorOutcome.Answer)
        assertEquals("7 giorni", savedDiet)
    }

    @Test
    fun `se il primario fallisce si usa il provider alternativo`() = runBlocking {
        val groq = ProviderConfig(
            id = "groq",
            kind = ProviderKind.GROQ,
            displayName = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            modelId = "llama-3.3-70b",
            apiKey = "chiave-di-test"
        )
        val manager = ProviderManager(listOf(groq, openAiConfig()), primaryId = "groq", fallbackId = "openai")
        val orchestrator = AiOrchestrator(
            providers = manager,
            gateway = ProviderGateway(
                mapOf(
                    ProviderKind.GROQ to FailingClient("429 rate limit"),
                    ProviderKind.OPENAI to ScriptedClient(listOf(ModelReply(text = "risposta dal fallback")))
                )
            ),
            tools = ToolEngine(listOf(searchFood), InMemoryToolAuditLog()) { 7L }
        )

        val outcome = orchestrator.ask("ciao", "sei un coach")

        assertTrue(outcome is OrchestratorOutcome.Answer)
        assertEquals("risposta dal fallback", (outcome as OrchestratorOutcome.Answer).text)
    }

    @Test
    fun `senza chiave API l'orchestrator dichiara il limite invece di inventare`() = runBlocking {
        val config = openAiConfig().copy(apiKey = null)
        val outcome = orchestrator(ScriptedClient(listOf(ModelReply(text = "x"))), config = config)
            .ask("ciao", "sei un coach")

        assertTrue(outcome is OrchestratorOutcome.Unavailable)
        assertTrue((outcome as OrchestratorOutcome.Unavailable).reason.contains("chiave API mancante"))
    }

    @Test
    fun `il provider alternativo non viene scelto se non ha la chiave`() = runBlocking {
        val broken = openAiConfig()
        val noKey = ProviderConfig(
            id = "gemini",
            kind = ProviderKind.GEMINI,
            displayName = "Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            modelId = "gemini-1.5-flash",
            apiKey = null
        )
        val manager = ProviderManager(listOf(broken, noKey), primaryId = "openai", fallbackId = "gemini")

        assertEquals(listOf("openai"), manager.orderedChain().map { it.id })
        assertTrue(manager.unavailableReason().contains("Gemini"))
    }

    @Test
    fun `la chiave API non viene mai mostrata in chiaro`() {
        val manager = ProviderManager(listOf(openAiConfig()), primaryId = "openai")

        assertEquals("••••test", manager.maskedKey(openAiConfig()))
        assertEquals("non configurata", manager.maskedKey(openAiConfig().copy(apiKey = null)))
    }
}
