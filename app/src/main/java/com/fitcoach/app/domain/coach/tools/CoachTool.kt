package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.remote.FunctionDefinition
import com.fitcoach.app.data.remote.ToolDefinition
import kotlinx.serialization.json.JsonObject

/**
 * Uno strumento (function calling) che l'AI puo invocare per leggere dati reali,
 * eseguire calcoli o proporre modifiche ai programmi.
 *
 * L'AI e l'unica autorita decisionale: gli strumenti le danno accesso ai servizi
 * applicativi reali. I calcoli matematici sono strumenti subordinati che l'AI
 * consulta per decidere, non generatori autonomi di programmi.
 */
interface CoachTool {
    /** Nome univoco (snake_case) esposto al modello. */
    val name: String

    /** Descrizione operativa: cosa fa e quando usarlo. */
    val description: String

    /** Schema JSON dei parametri (JSON Schema). */
    val parameters: JsonObject

    /**
     * True se lo strumento modifica lo stato dell'app. Gli strumenti di scrittura
     * NON applicano direttamente: registrano una proposta che l'utente conferma.
     */
    val isWrite: Boolean get() = false

    /** Esegue lo strumento e ritorna un risultato JSON strutturato e verificabile. */
    suspend fun execute(arguments: JsonObject): String

    fun toDefinition(): ToolDefinition = ToolDefinition(
        function = FunctionDefinition(name = name, description = description, parameters = parameters)
    )
}
