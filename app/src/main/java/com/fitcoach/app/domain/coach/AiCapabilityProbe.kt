package com.fitcoach.app.domain.coach

import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
import com.fitcoach.app.data.remote.AiResult
import com.fitcoach.app.data.remote.ChatMessageDto
import com.fitcoach.app.data.remote.FunctionDefinition
import com.fitcoach.app.data.remote.ToolChoice
import com.fitcoach.app.data.remote.ToolDefinition
import com.fitcoach.app.domain.coach.tools.Schema
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verifica che il provider configurato sia in grado di sostenere una generazione.
 *
 * Un semplice "rispondi OK" non basta: la generazione e un turno agentico che
 * invia ~15 definizioni di strumenti e richiede una tool call. Un provider puo
 * rispondere HTTP 200 al messaggio di testo e poi fallire (o ignorare) gli
 * strumenti: l'utente vedrebbe "Connessione riuscita" seguito da una generazione
 * che non arriva mai. Il probe riproduce quindi il percorso reale usando lo
 * stesso [AiClient] della chat e della generazione.
 */
@Singleton
class AiCapabilityProbe @Inject constructor(
    private val aiClient: AiClient
) {
    sealed interface Outcome {
        /** Provider raggiungibile e capace di produrre una tool call. */
        data class Ready(val model: String?, val latencyMs: Long) : Outcome

        /**
         * Provider raggiungibile e in grado di rispondere, ma senza tool calling
         * utilizzabile: la chat funziona, la generazione dei programmi no.
         */
        data class TextOnly(val model: String?, val detail: String) : Outcome

        data class Failed(val message: String) : Outcome
    }

    suspend fun probe(config: AiProviderConfig): Outcome {
        val started = System.currentTimeMillis()
        val result = aiClient.complete(
            config = config,
            messages = listOf(
                ChatMessageDto(
                    role = "system",
                    content = "Sei un test di integrazione. Usa lo strumento disponibile e basta."
                ),
                ChatMessageDto(
                    role = "user",
                    content = "Chiama lo strumento ping con valore 1."
                )
            ),
            tools = listOf(PING_TOOL),
            toolChoice = ToolChoice.auto,
            // Il test non deve consumare il budget di ragionamento: serve sapere
            // se gli strumenti arrivano, non la qualita del ragionamento.
            maxTokens = 512
        )
        val latency = System.currentTimeMillis() - started

        return when (result) {
            is AiResult.Failure -> Outcome.Failed(result.message)
            is AiResult.Success -> when {
                result.toolCalls.any { it.function.name == PING_TOOL.function.name } ->
                    Outcome.Ready(result.model, latency)
                result.toolCalls.isNotEmpty() ->
                    Outcome.Ready(result.model, latency)
                else -> Outcome.TextOnly(
                    model = result.model,
                    detail = if (result.truncated) {
                        "Il modello ha esaurito i token senza chiamare lo strumento."
                    } else {
                        "Il modello ha risposto in testo senza usare lo strumento."
                    }
                )
            }
        }
    }

    private companion object {
        val PING_TOOL = ToolDefinition(
            function = FunctionDefinition(
                name = "ping",
                description = "Strumento di prova: restituisce il valore ricevuto.",
                parameters = Schema.obj(
                    "valore" to Schema.integer("Valore intero da restituire."),
                    required = listOf("valore")
                )
            )
        )
    }
}
