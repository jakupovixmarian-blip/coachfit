package com.fitcoach.app.data.remote

import com.fitcoach.app.domain.model.AiProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Verifica che il JSON inviato ai diversi provider contenga il parametro token
 * corretto e MAI entrambi (`max_tokens` / `max_completion_tokens`).
 */
class AiRequestJsonTest {

    // Stessa configurazione del modulo di rete reale (explicitNulls = false).
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val messages = listOf(
        ChatMessageDto(role = "system", content = "test"),
        ChatMessageDto(role = "user", content = "ciao")
    )

    private fun encode(request: ChatCompletionRequest) =
        json.encodeToString(ChatCompletionRequest.serializer(), request)
    private fun keysOf(request: ChatCompletionRequest) =
        json.parseToJsonElement(encode(request)).jsonObject.keys

    @Test
    fun `openai gpt-4o-mini usa max_tokens`() {
        val req = ChatCompletionRequest.forProvider(
            AiProvider.OPENAI, "gpt-4o-mini", messages, temperature = 0.4, maxTokens = 1200
        )
        val keys = keysOf(req)
        assertThat(keys).contains("max_tokens")
        assertThat(keys).doesNotContain("max_completion_tokens")
        assertThat(encode(req)).contains("\"max_tokens\":1200")
    }

    @Test
    fun `openai o1 usa max_completion_tokens`() {
        val req = ChatCompletionRequest.forProvider(
            AiProvider.OPENAI, "o1-preview", messages, temperature = 1.0, maxTokens = 1200
        )
        val keys = keysOf(req)
        assertThat(keys).contains("max_completion_tokens")
        assertThat(keys).doesNotContain("max_tokens")
        assertThat(encode(req)).contains("\"max_completion_tokens\":1200")
    }

    @Test
    fun `openai gpt-4-1 e gpt-5 usano max_completion_tokens`() {
        listOf("gpt-4.1", "gpt-4.1-mini", "gpt-5", "gpt-5-nano", "o3-mini", "o4-mini").forEach { model ->
            val keys = keysOf(
                ChatCompletionRequest.forProvider(AiProvider.OPENAI, model, messages, maxTokens = 500)
            )
            assertThat(keys).contains("max_completion_tokens")
            assertThat(keys).doesNotContain("max_tokens")
        }
    }

    @Test
    fun `openai gpt-6-luna usa max_completion_tokens`() {
        listOf("gpt-6-luna", "gpt-6-sol", "gpt-6-astra", "gpt-5.6-luna", "gpt-7-x").forEach { model ->
            val keys = keysOf(
                ChatCompletionRequest.forProvider(AiProvider.OPENAI, model, messages, maxTokens = 256)
            )
            assertThat(keys).contains("max_completion_tokens")
            assertThat(keys).doesNotContain("max_tokens")
        }
    }

    @Test
    fun `groq openrouter deepseek e custom usano sempre max_tokens`() {
        val cases = listOf(
            AiProvider.GROQ to "llama-3.3-70b-versatile",
            AiProvider.OPENROUTER to "meta-llama/llama-3.3-70b-instruct:free",
            AiProvider.DEEPSEEK to "deepseek-chat",
            AiProvider.CUSTOM to "o1-like-local-model"
        )
        cases.forEach { (provider, model) ->
            val keys = keysOf(
                ChatCompletionRequest.forProvider(provider, model, messages, maxTokens = 800)
            )
            assertThat(keys).contains("max_tokens")
            assertThat(keys).doesNotContain("max_completion_tokens")
        }
    }

    @Test
    fun `non invia mai entrambi i parametri token`() {
        val all = listOf(
            AiProvider.OPENAI to "gpt-4o-mini",
            AiProvider.OPENAI to "o1-mini",
            AiProvider.GROQ to "llama-3.3-70b-versatile",
            AiProvider.OPENROUTER to "x",
            AiProvider.DEEPSEEK to "deepseek-chat",
            AiProvider.CUSTOM to "whatever"
        )
        all.forEach { (provider, model) ->
            val keys = keysOf(ChatCompletionRequest.forProvider(provider, model, messages, maxTokens = 300))
            val both = keys.contains("max_tokens") && keys.contains("max_completion_tokens")
            assertThat(both).isFalse()
        }
    }

    @Test
    fun `senza maxTokens non viene inviato alcun parametro token`() {
        val keys = keysOf(
            ChatCompletionRequest.forProvider(AiProvider.OPENAI, "gpt-4o-mini", messages, maxTokens = null)
        )
        assertThat(keys).doesNotContain("max_tokens")
        assertThat(keys).doesNotContain("max_completion_tokens")
    }

    @Test
    fun `withAlternateTokenParam sposta il valore sull'altro campo`() {
        val original = ChatCompletionRequest.forProvider(
            AiProvider.OPENAI, "o1-mini", messages, maxTokens = 700
        )
        val swapped = ChatCompletionRequest.withAlternateTokenParam(original)
        assertThat(swapped.maxCompletionTokens).isNull()
        assertThat(swapped.maxTokens).isEqualTo(700)
        val keys = keysOf(swapped)
        assertThat(keys).contains("max_tokens")
        assertThat(keys).doesNotContain("max_completion_tokens")
    }

    @Test
    fun `response_format opzionale viene serializzato solo se richiesto`() {
        val withFormat = ChatCompletionRequest.forProvider(
            AiProvider.OPENROUTER, "x", messages, maxTokens = 100,
            responseFormat = ChatCompletionRequest.ResponseFormat()
        )
        assertThat(encode(withFormat)).contains("\"response_format\"")
        val without = ChatCompletionRequest.forProvider(AiProvider.OPENROUTER, "x", messages, maxTokens = 100)
        assertThat(encode(without)).doesNotContain("response_format")
    }

    @Test
    fun `temperature opzionale viene omessa se null`() {
        val req = ChatCompletionRequest.forProvider(
            AiProvider.OPENAI, "gpt-4o-mini", messages, temperature = null, maxTokens = 100
        )
        assertThat(keysOf(req)).doesNotContain("temperature")
    }
}
