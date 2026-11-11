package com.fitcoach.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.4,
    @SerialName("max_tokens") val maxTokens: Int = 1200,
    val stream: Boolean = false
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
    val error: ApiError? = null
) {
    @Serializable
    data class Choice(
        val index: Int = 0,
        val message: ChatMessageDto? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0,
        @SerialName("total_tokens") val totalTokens: Int = 0
    )

    val content: String? get() = choices.firstOrNull()?.message?.content
}

@Serializable
data class ApiError(
    val message: String = "",
    val type: String? = null,
    val code: String? = null
)
