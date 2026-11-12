package com.fitcoach.ai

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class OpenAiCompatibleGateway(
    private val client: OkHttpClient = OkHttpClient()
) : AiChatGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(
        config: AiProviderConfig,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> = runCatching {
        require(config.baseUrl.startsWith("https://")) { "L'endpoint AI deve usare HTTPS" }
        require(apiKey.isNotBlank()) { "API key mancante" }
        val api = Retrofit.Builder()
            .baseUrl(config.baseUrl.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiCompatibleApi::class.java)
        val response = api.complete(
            authorization = "Bearer $apiKey",
            request = ChatCompletionRequest(
                model = config.model,
                messages = messages.map { ChatCompletionMessage(it.role, it.content) }
            )
        )
        response.choices.firstOrNull()?.message?.content
            ?: error("Il provider non ha restituito una risposta")
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
}

class MemoryCredentialStore : AiCredentialStore {
    private val values = mutableMapOf<AiProviderConfig, String>()

    override suspend fun read(config: AiProviderConfig): String? = values[config]

    override suspend fun write(config: AiProviderConfig, apiKey: String) {
        require(apiKey.isNotBlank())
        values[config] = apiKey
    }

    override suspend fun clear(config: AiProviderConfig) {
        values.remove(config)
    }
}
