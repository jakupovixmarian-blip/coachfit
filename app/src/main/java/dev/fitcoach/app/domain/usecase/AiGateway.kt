package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.CoachChatMessage

/**
 * Everything the agent needs from the AI provider, behind one seam. Keeping the
 * provider call behind this interface lets the agent logic be tested with a fake
 * gateway, without touching the network.
 */
interface AiGateway {
    /** True when a provider and an API key are configured. */
    suspend fun isConfigured(): Boolean

    /** Single-shot completion. Throws AppError on failure. */
    suspend fun complete(systemPrompt: String, userPrompt: String, jsonMode: Boolean = true): String

    /** Completion that also receives the previous conversation turns. */
    suspend fun completeWithHistory(
        systemPrompt: String,
        history: List<CoachChatMessage>,
        userPrompt: String,
        jsonMode: Boolean = true
    ): String
}
