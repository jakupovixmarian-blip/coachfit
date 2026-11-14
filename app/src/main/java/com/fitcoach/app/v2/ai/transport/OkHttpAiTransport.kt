package com.fitcoach.app.v2.ai.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/** Real Android transport for the 2.0 provider client. It never logs body or credentials. */
class OkHttpAiTransport @Inject constructor(
    private val client: OkHttpClient
) : AiTransport {
    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Long
    ): TransportResponse = execute(url, headers, body, timeoutMs)

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): TransportResponse = execute(url, headers, null, timeoutMs)

    private suspend fun execute(
        url: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMs: Long
    ): TransportResponse = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }
        body?.let { requestBuilder.post(it.toRequestBody(JSON)) }
        val started = System.currentTimeMillis()
        client.newBuilder()
            .connectTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
            .newCall(requestBuilder.build())
            .execute()
            .use { response ->
                TransportResponse(
                    statusCode = response.code,
                    body = response.body?.string(),
                    elapsedMs = System.currentTimeMillis() - started
                )
            }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
