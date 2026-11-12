package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.v2.ai.provider.AiCompletion
import com.fitcoach.app.v2.ai.provider.AiRequest
import com.fitcoach.app.v2.ai.provider.OpenAiCompatibleClient
import com.fitcoach.app.v2.ai.provider.ProviderConfiguration
import com.fitcoach.app.v2.domain.GenerationPhase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GenerationGatewayResult {
    data class Success(val payload: JsonObject, val model: String?) : GenerationGatewayResult
    data class Failure(val message: String, val retryable: Boolean) : GenerationGatewayResult
}

interface AiGenerationGateway {
    suspend fun generate(
        configuration: ProviderConfiguration,
        phase: GenerationPhase,
        profile: GenerationProfile,
        previousPayloads: Map<GenerationPhase, JsonObject>
    ): GenerationGatewayResult
}

@Singleton
class ProviderGenerationGateway @Inject constructor(
    private val client: OpenAiCompatibleClient,
    private val json: Json
) : AiGenerationGateway {
    override suspend fun generate(
        configuration: ProviderConfiguration,
        phase: GenerationPhase,
        profile: GenerationProfile,
        previousPayloads: Map<GenerationPhase, JsonObject>
    ): GenerationGatewayResult {
        val request = AiRequest(
            system = systemPrompt(phase),
            user = buildUserPrompt(phase, profile, previousPayloads),
            temperature = 0.1,
            jsonObject = true
        )
        return when (val result = client.complete(configuration, request)) {
            is AiCompletion.Success -> parse(result.content, result.model)
            is AiCompletion.Truncated -> GenerationGatewayResult.Failure(
                "Output AI troncato prima del JSON completo.", true
            )
            is AiCompletion.Refused -> GenerationGatewayResult.Failure(
                "Il provider ha rifiutato la fase: ${result.message}", false
            )
            is AiCompletion.ToolCalls -> GenerationGatewayResult.Failure(
                "Il provider ha restituito una tool call inattesa.", true
            )
            is AiCompletion.Failure -> GenerationGatewayResult.Failure(
                "${result.reason}: ${result.message}", result.reason.retryable
            )
        }
    }

    private fun parse(content: String, model: String?): GenerationGatewayResult = runCatching {
        json.parseToJsonElement(content).let { element ->
            require(element is JsonObject) { "La fase AI deve restituire un oggetto JSON." }
            GenerationGatewayResult.Success(element, model)
        }
    }.getOrElse { GenerationGatewayResult.Failure("JSON AI non valido: ${it.message}", true) }

    private fun buildUserPrompt(
        phase: GenerationPhase,
        profile: GenerationProfile,
        previousPayloads: Map<GenerationPhase, JsonObject>
    ): String = buildString {
        appendLine(profile.promptContext())
        appendLine("FASE DA PRODURRE: ${phase.name}")
        appendLine("FASI PRECEDENTI VALIDATE:")
        previousPayloads.forEach { (previous, payload) -> appendLine("$previous: $payload") }
        appendLine("Non inventare misure, alimenti o esercizi. Se un dato manca, usa null e segnala il limite nel JSON.")
        appendLine("Restituisci un solo oggetto JSON, senza markdown, commenti o testo esterno.")
    }

    private fun systemPrompt(phase: GenerationPhase): String = """
        Sei il componente di strutturazione AI di FitCoach. Produci solo JSON per la fase $phase.
        Non sei un medico e non fare diagnosi. Non prescrivere farmaci, ormoni o sostanze dopanti.
        Non modificare database e non decidere valori matematici finali: i motori deterministici locali
        controlleranno calorie, macro, volume, carichi, sicurezza e coerenza prima del commit.
        Il JSON deve contenere anche una lista `assumptions` vuota se non ci sono assunzioni.
    """.trimIndent()

    private val com.fitcoach.app.v2.ai.provider.FailureReason.retryable: Boolean
        get() = this in setOf(
            com.fitcoach.app.v2.ai.provider.FailureReason.NETWORK,
            com.fitcoach.app.v2.ai.provider.FailureReason.TIMEOUT,
            com.fitcoach.app.v2.ai.provider.FailureReason.RATE_LIMIT,
            com.fitcoach.app.v2.ai.provider.FailureReason.SERVER,
            com.fitcoach.app.v2.ai.provider.FailureReason.INVALID_JSON
        )
}
