package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.remote.ToolDefinition
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro di tutti gli strumenti disponibili per l'AI.
 *
 * Il modello vede solo nome, descrizione e schema; l'esecuzione avviene sui
 * servizi reali dell'app. Un nome sconosciuto produce un errore strutturato che
 * il modello puo leggere e correggere, senza mai bloccare la conversazione.
 */
@Singleton
class CoachToolRegistry @Inject constructor(
    tools: Set<@JvmSuppressWildcards CoachTool>
) {
    private val byName: Map<String, CoachTool> = tools.associateBy { it.name }

    val definitions: List<ToolDefinition> = tools.map { it.toDefinition() }

    fun names(): Set<String> = byName.keys

    /** Nomi degli strumenti che modificano lo stato (richiedono conferma). */
    val writeToolNames: Set<String> = tools.filter { it.isWrite }.map { it.name }.toSet()

    suspend fun execute(name: String, arguments: JsonObject): String {
        val tool = byName[name]
            ?: return errorJson("Strumento sconosciuto: $name. Strumenti disponibili: ${byName.keys.sorted()}")
        return try {
            tool.execute(arguments)
        } catch (t: Throwable) {
            errorJson("Errore durante l'esecuzione di $name: ${t.message ?: t::class.simpleName}")
        }
    }

    private fun errorJson(message: String): String =
        """{"ok":false,"error":${quote(message)}}"""

    private fun quote(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""
}
