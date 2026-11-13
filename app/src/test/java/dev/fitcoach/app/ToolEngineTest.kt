package dev.fitcoach.app

import dev.fitcoach.app.domain.ai.Approval
import dev.fitcoach.app.domain.ai.InMemoryToolAuditLog
import dev.fitcoach.app.domain.ai.Tool
import dev.fitcoach.app.domain.ai.ToolArguments
import dev.fitcoach.app.domain.ai.ToolEngine
import dev.fitcoach.app.domain.ai.ToolParameter
import dev.fitcoach.app.domain.ai.ToolParameterType
import dev.fitcoach.app.domain.ai.ToolResult
import dev.fitcoach.app.domain.ai.ToolSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The engine is the boundary between the model and the user's data: it must refuse
 * invented tools and arguments, and it must never apply a change without approval.
 */
class ToolEngineTest {

    private var savedDiet: String? = null

    private val searchFood = object : Tool {
        override val spec = ToolSpec(
            name = "cerca_alimento",
            description = "Cerca un alimento nel catalogo",
            parameters = listOf(
                ToolParameter("query", ToolParameterType.STRING, description = "Testo da cercare"),
                ToolParameter("limite", ToolParameterType.INTEGER, required = false)
            )
        )

        override suspend fun invoke(arguments: ToolArguments): String =
            "trovati per ${arguments.string("query")} limite ${arguments.int("limite") ?: 10}"
    }

    private val saveDiet = object : Tool {
        override val spec = ToolSpec(
            name = "salva_dieta",
            description = "Salva il piano alimentare settimanale",
            parameters = listOf(ToolParameter("piano", ToolParameterType.STRING)),
            requiresApproval = true,
            mutatesData = true
        )

        override suspend fun invoke(arguments: ToolArguments): String {
            savedDiet = arguments.string("piano")
            return "piano salvato"
        }
    }

    private val broken = object : Tool {
        override val spec = ToolSpec(
            name = "strumento_rotto",
            description = "Fallisce sempre",
            parameters = emptyList()
        )

        override suspend fun invoke(arguments: ToolArguments): String =
            throw IllegalStateException("database non disponibile")
    }

    private fun engine(audit: InMemoryToolAuditLog = InMemoryToolAuditLog()) =
        ToolEngine(listOf(searchFood, saveDiet, broken), audit) { 42L }

    @Test
    fun `uno strumento in sola lettura viene eseguito`() = runBlocking {
        val audit = InMemoryToolAuditLog()
        val result = engine(audit).execute("cerca_alimento", mapOf("query" to "mela", "limite" to "5"))

        assertTrue(result is ToolResult.Ok)
        assertEquals("trovati per mela limite 5", (result as ToolResult.Ok).output)
        assertEquals("ok", audit.all().single().outcome)
    }

    @Test
    fun `un numero con la virgola viene accettato`() = runBlocking {
        val result = engine().execute("cerca_alimento", mapOf("query" to "riso", "limite" to "7"))
        assertTrue((result as ToolResult.Ok).output.contains("limite 7"))
    }

    @Test
    fun `un'operazione che modifica dati non viene applicata senza approvazione`() = runBlocking {
        val audit = InMemoryToolAuditLog()
        val result = engine(audit).execute("salva_dieta", mapOf("piano" to "7 giorni"))

        assertTrue(result is ToolResult.NeedsApproval)
        assertEquals(null, savedDiet)
        assertEquals("approval_requested", audit.all().single().outcome)
        assertTrue((result as ToolResult.NeedsApproval).pending.summary.contains("piano=7 giorni"))
    }

    @Test
    fun `l'approvazione applica la modifica`() = runBlocking {
        val result = engine().execute(
            "salva_dieta",
            mapOf("piano" to "7 giorni"),
            Approval.Approved
        )

        assertTrue(result is ToolResult.Ok)
        assertEquals("7 giorni", savedDiet)
    }

    @Test
    fun `il rifiuto dell'utente lascia i dati invariati`() = runBlocking {
        val audit = InMemoryToolAuditLog()
        val result = engine(audit).execute(
            "salva_dieta",
            mapOf("piano" to "7 giorni"),
            Approval.Denied
        )

        assertTrue(result is ToolResult.Refused)
        assertEquals(null, savedDiet)
        assertEquals("approval_denied", audit.all().single().outcome)
    }

    @Test
    fun `uno strumento inventato viene respinto`() = runBlocking {
        val audit = InMemoryToolAuditLog()
        val result = engine(audit).execute("cancella_tutto", emptyMap())

        assertTrue(result is ToolResult.Refused)
        assertTrue((result as ToolResult.Refused).reason.contains("sconosciuto"))
        assertEquals("rejected", audit.all().single().outcome)
    }

    @Test
    fun `un parametro obbligatorio mancante viene respinto`() = runBlocking {
        val result = engine().execute("cerca_alimento", emptyMap())

        assertTrue(result is ToolResult.Refused)
        assertTrue((result as ToolResult.Refused).reason.contains("query"))
    }

    @Test
    fun `un parametro di tipo sbagliato viene respinto`() = runBlocking {
        val result = engine().execute("cerca_alimento", mapOf("query" to "mela", "limite" to "molti"))

        assertTrue(result is ToolResult.Refused)
        assertTrue((result as ToolResult.Refused).reason.contains("limite"))
    }

    @Test
    fun `un parametro non previsto viene respinto`() = runBlocking {
        val result = engine().execute("cerca_alimento", mapOf("query" to "mela", "sql" to "drop table"))

        assertTrue(result is ToolResult.Refused)
        assertTrue((result as ToolResult.Refused).reason.contains("non previsti"))
    }

    @Test
    fun `un'eccezione dello strumento diventa un fallimento registrato`() = runBlocking {
        val audit = InMemoryToolAuditLog()
        val result = engine(audit).execute("strumento_rotto", emptyMap())

        assertTrue(result is ToolResult.Failed)
        assertTrue((result as ToolResult.Failed).reason.contains("database non disponibile"))
        assertEquals("failed", audit.all().single().outcome)
        assertFalse(audit.all().single().detail.isBlank())
    }

    @Test
    fun `le specifiche espongono quali strumenti richiedono approvazione`() = runBlocking {
        val specs = engine().specs()

        assertEquals(3, specs.size)
        assertEquals(listOf("cerca_alimento", "salva_dieta", "strumento_rotto"), specs.map { it.name })
        assertTrue(specs.first { it.name == "salva_dieta" }.requiresApproval)
        assertFalse(specs.first { it.name == "cerca_alimento" }.requiresApproval)
    }
}
