package com.fitcoach.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

enum class AiProvider(val displayName: String) {
    OPENAI("OpenAI"), GROQ("Groq"), OPENROUTER("OpenRouter"), DEEPSEEK("DeepSeek"), CUSTOM("Endpoint compatibile")
}

data class AiProviderConfig(
    val provider: AiProvider,
    val baseUrl: String,
    val model: String
)

interface AiCredentialStore {
    suspend fun read(config: AiProviderConfig): String?
    suspend fun write(config: AiProviderConfig, apiKey: String)
    suspend fun clear(config: AiProviderConfig)
}

interface AiChatGateway {
    suspend fun complete(config: AiProviderConfig, apiKey: String, messages: List<ChatMessage>): Result<String>
}

data class ChatMessage(val role: String, val content: String)

interface OpenAiCompatibleApi {
    @POST("chat/completions")
    suspend fun complete(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val temperature: Double = 0.2
)

@Serializable
data class ChatCompletionMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionResponse(val choices: List<Choice> = emptyList()) {
    @Serializable
    data class Choice(@SerialName("message") val message: ChatCompletionMessage)
}
