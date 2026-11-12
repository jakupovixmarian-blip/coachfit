package com.fitcoach.app.v2.ai.provider

import com.fitcoach.app.v2.ai.transport.AiTransport
import com.fitcoach.app.v2.ai.transport.TransportResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Test

class OpenAiCompatibleClientTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private class FixedCredentials : CredentialStore {
        override suspend fun read(ref: String): String? = if (ref == "openai") "secret" else null
    }

    private class RecordingTransport(
        private val responses: ArrayDeque<TransportResponse>
    ) : AiTransport {
        val postBodies = mutableListOf<String>()
        val postHeaders = mutableListOf<Map<String, String>>()

        override suspend fun post(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): TransportResponse {
            postBodies += body
            postHeaders += headers
            return responses.removeFirst()
        }

        override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Long): TransportResponse =
            responses.removeFirst()
    }

    private class ThrowingTransport(private val throwable: IOException) : AiTransport {
        override suspend fun post(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): TransportResponse =
            throw throwable

        override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Long): TransportResponse =
            throw throwable
    }


    private fun config(model: String) = ProviderConfiguration(
        provider = ProviderKind.OPENAI,
        baseUrl = "https://api.openai.com/v1",
        model = model,
        credentialRef = "openai",
        maxOutputTokens = 400,
        timeoutMs = 30_000
    )

    private fun success(content: String = "OK") = TransportResponse(
        statusCode = 200,
        body = """{"model":"gpt-test","choices":[{"message":{"role":"assistant","content":"$content"},"finish_reason":"stop"}]}"""
    )

    @Test
    fun `new OpenAI model uses completion tokens and omits unsupported temperature`() = runTest {
        val transport = RecordingTransport(ArrayDeque(listOf(success())))
        val client = OpenAiCompatibleClient(transport, json, FixedCredentials())

        val result = client.complete(config("gpt-5.6"), AiRequest(user = "test", temperature = 0.2))
        val body = json.parseToJsonElement(transport.postBodies.single()).jsonObject

        assertThat(result).isInstanceOf(AiCompletion.Success::class.java)
        assertThat(body["max_completion_tokens"]?.toString()).isEqualTo("400")
        assertThat(body).doesNotContainKey("max_tokens")
        assertThat(body).doesNotContainKey("temperature")
    }

    @Test
    fun `classic OpenAI model sends max tokens and temperature`() = runTest {
        val transport = RecordingTransport(ArrayDeque(listOf(success())))
        val client = OpenAiCompatibleClient(transport, json, FixedCredentials())

        client.complete(config("gpt-4o-mini"), AiRequest(user = "test", temperature = 0.2))
        val body = json.parseToJsonElement(transport.postBodies.single()).jsonObject

        assertThat(body["max_tokens"]?.toString()).isEqualTo("400")
        assertThat(body).doesNotContainKey("max_completion_tokens")
        assertThat(body["temperature"]?.toString()).isEqualTo("0.2")
    }

    @Test
    fun `unsupported token field gets exactly one controlled retry`() = runTest {
        val transport = RecordingTransport(
            ArrayDeque(
                listOf(
                    TransportResponse(400, """{"error":{"message":"max_completion_tokens is unsupported"}}"""),
                    success()
                )
            )
        )
        val client = OpenAiCompatibleClient(transport, json, FixedCredentials())

        val result = client.complete(config("gpt-5.6"), AiRequest(user = "test"))

        assertThat(result).isInstanceOf(AiCompletion.Success::class.java)
        assertThat(transport.postBodies).hasSize(2)
        assertThat(transport.postBodies[0]).contains("max_completion_tokens")
        assertThat(transport.postBodies[1]).contains("max_tokens")
        assertThat(transport.postHeaders[0]["Authorization"]).isEqualTo("Bearer secret")
    }

    @Test
    fun `reasoning-only response is rejected without exposing reasoning`() = runTest {
        val transport = RecordingTransport(
            ArrayDeque(
                listOf(
                    TransportResponse(
                        200,
                        """{"choices":[{"message":{"content":null,"reasoning_content":"private reasoning"},"finish_reason":"stop"}]}"""
                    )
                )
            )
        )
        val client = OpenAiCompatibleClient(transport, json, FixedCredentials())

        val result = client.complete(config("gpt-5.6"), AiRequest(user = "test"))

        assertThat(result).isInstanceOf(AiCompletion.Failure::class.java)
        assertThat((result as AiCompletion.Failure).reason).isEqualTo(FailureReason.EMPTY_RESPONSE)
        assertThat(result.message).doesNotContain("private reasoning")
    }

    @Test
    fun `connection errors are classified by HTTP status`() = runTest {
        val transport = RecordingTransport(
            ArrayDeque(listOf(TransportResponse(401, """{"error":{"message":"bad key"}}""")))
        )
        val client = OpenAiCompatibleClient(transport, json, FixedCredentials())

        val result = client.complete(config("gpt-5.6"), AiRequest(user = "test"))

        assertThat(result).isInstanceOf(AiCompletion.Failure::class.java)
        assertThat((result as AiCompletion.Failure).reason).isEqualTo(FailureReason.AUTHENTICATION)
    }

    @Test
    fun `model discovery returns provider model ids`() = runTest {
        val transport = RecordingTransport(
            ArrayDeque(listOf(TransportResponse(200, """{"data":[{"id":"gpt-5.6","owned_by":"openai"}]}""")))
        )
        val client = OpenAiCompatibleClient(transport, json, FixedCredentials())

        val result = client.discoverModels(config("gpt-5.6")).getOrThrow()

        assertThat(result.map { it.id }).containsExactly("gpt-5.6")
    }

    @Test
    fun `timeout is distinct from generic network failure`() = runTest {
        val client = OpenAiCompatibleClient(
            ThrowingTransport(SocketTimeoutException("timeout")),
            json,
            FixedCredentials()
        )

        val result = client.complete(config("gpt-5.6"), AiRequest(user = "test"))

        assertThat((result as AiCompletion.Failure).reason).isEqualTo(FailureReason.TIMEOUT)
    }

}
