package dev.fitcoach.app

import dev.fitcoach.app.data.ai.AiClient
import dev.fitcoach.app.data.ai.ChatMessage
import dev.fitcoach.app.domain.model.AiConfig
import dev.fitcoach.app.domain.model.AiModelCapabilities
import dev.fitcoach.app.domain.model.AiProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The request body must follow the capability of the selected model: the token
 * budget under the parameter that model accepts, and no parameter it rejects.
 */
class AiRequestPayloadTest {

    private val client = AiClient(OkHttpClient(), Json { ignoreUnknownKeys = true })

    private fun payload(
        model: String,
        provider: AiProvider = AiProvider.OPENAI,
        jsonMode: Boolean = true,
        maxTokens: Int = 4096,
        reasoningEffort: String? = null
    ) = client.buildChatPayload(
        config = AiConfig(
            provider = provider,
            modelId = model,
            maxOutputTokens = maxTokens,
            useJsonMode = true
        ),
        messages = listOf(ChatMessage("user", "ciao")),
        jsonMode = jsonMode,
        reasoningEffort = reasoningEffort,
        capabilities = AiModelCapabilities.forModel(provider, model)
    )

    @Test
    fun `classic model uses max_tokens`() {
        val body = payload("gpt-4o-mini")
        assertEquals("max_tokens", body.keys.first { it.contains("tokens") })
        assertEquals(4096, body["max_tokens"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `reasoning model uses max_completion_tokens and never max_tokens`() {
        listOf("o1-mini", "o3-mini", "gpt-5", "gpt-5-mini").forEach { model ->
            val body = payload(model)
            assertTrue("$model should send max_completion_tokens", body.containsKey("max_completion_tokens"))
            assertFalse("$model must not send max_tokens", body.containsKey("max_tokens"))
        }
    }

    @Test
    fun `reasoning model omits temperature and top_p`() {
        val body = payload("gpt-5")
        assertFalse(body.containsKey("temperature"))
        assertFalse(body.containsKey("top_p"))
    }

    @Test
    fun `classic model keeps temperature and top_p`() {
        val body = payload("gpt-4o-mini")
        assertTrue(body.containsKey("temperature"))
        assertTrue(body.containsKey("top_p"))
    }

    @Test
    fun `reasoning model omits response_format`() {
        val body = payload("o3-mini", jsonMode = true)
        assertFalse(body.containsKey("response_format"))
    }

    @Test
    fun `classic model keeps response_format when json mode is requested`() {
        val body = payload("gpt-4o-mini", jsonMode = true)
        assertTrue(body.containsKey("response_format"))
    }

    @Test
    fun `reasoning effort is sent only to models that support it`() {
        val supported = payload("gpt-5", reasoningEffort = "medium")
        assertEquals("medium", supported["reasoning_effort"]?.jsonPrimitive?.content)

        val unsupported = payload("gpt-4o-mini", reasoningEffort = "medium")
        assertFalse(unsupported.containsKey("reasoning_effort"))
    }

    @Test
    fun `the token budget is clamped to the model limit`() {
        val body = payload("gpt-4o-mini", maxTokens = 10_000_000)
        val sent = body["max_tokens"]!!.jsonPrimitive.content.toInt()
        val limit = AiModelCapabilities.forModel(AiProvider.OPENAI, "gpt-4o-mini").maxOutputTokensLimit
        assertEquals(limit, sent)
    }

    @Test
    fun `a non OpenAI provider keeps the classic parameter names`() {
        listOf(AiProvider.OPENROUTER, AiProvider.DEEPSEEK, AiProvider.GROQ).forEach { provider ->
            val body = payload("llama-3.3-70b-versatile", provider = provider)
            assertTrue("$provider should send max_tokens", body.containsKey("max_tokens"))
            assertFalse(body.containsKey("max_completion_tokens"))
        }
    }

    @Test
    fun `an o prefixed model on a router is not treated as an OpenAI reasoning model`() {
        val body = payload("openai/gpt-4o-mini", provider = AiProvider.OPENROUTER)
        assertTrue(body.containsKey("max_tokens"))
        assertFalse(body.containsKey("max_completion_tokens"))
    }

    @Test
    fun `every payload keeps the model and the messages`() {
        val body = payload("gpt-4o-mini")
        assertEquals("gpt-4o-mini", body["model"]!!.jsonPrimitive.content)
        assertTrue(body.containsKey("messages"))
    }

    @Test
    fun `the capability mapping is the single source of truth`() {
        val classic = AiModelCapabilities.forModel(AiProvider.OPENAI, "gpt-4o-mini")
        assertEquals("max_tokens", classic.tokenParameterName)
        val reasoning = AiModelCapabilities.forModel(AiProvider.OPENAI, "gpt-5")
        assertEquals("max_completion_tokens", reasoning.tokenParameterName)
        assertTrue(reasoning.supportsReasoningEffort)
    }
}
