package dev.fitcoach.app.data.ai

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.domain.model.CoachChatMessage
import dev.fitcoach.app.domain.repository.AiConfigRepository
import dev.fitcoach.app.domain.usecase.AiGateway
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AiGateway] backed by the configured OpenAI-compatible provider. The API key is
 * read from the encrypted store on every call and never kept in memory longer than
 * the request needs it.
 */
@Singleton
class AiGatewayImpl @Inject constructor(
    private val aiClient: AiClient,
    private val aiConfigRepository: AiConfigRepository
) : AiGateway {

    override suspend fun isConfigured(): Boolean {
        val config = aiConfigRepository.getConfig()
        return !aiConfigRepository.getApiKey(config.provider).isNullOrBlank()
    }

    override suspend fun complete(systemPrompt: String, userPrompt: String, jsonMode: Boolean): String {
        val (config, key) = credentials()
        return aiClient.chat(
            config = config,
            apiKey = key,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            jsonMode = jsonMode
        ).content
    }

    override suspend fun completeWithHistory(
        systemPrompt: String,
        history: List<CoachChatMessage>,
        userPrompt: String,
        jsonMode: Boolean
    ): String {
        val (config, key) = credentials()
        val messages = buildList {
            add(ChatMessage("system", systemPrompt))
            history.takeLast(20).forEach { message -> add(ChatMessage(message.role.wire, message.content)) }
            add(ChatMessage("user", userPrompt))
        }
        return aiClient.chat(config = config, apiKey = key, messages = messages, jsonMode = jsonMode).content
    }

    private suspend fun credentials(): Pair<dev.fitcoach.app.domain.model.AiConfig, String> {
        val config = aiConfigRepository.getConfig()
        val key = aiConfigRepository.getApiKey(config.provider)
        if (key.isNullOrBlank()) {
            throw AppError.Auth("Configura una chiave API nelle impostazioni AI.")
        }
        return config to key
    }
}
