package com.fitcoach.app.v2.ai.transport

/** HTTP transport isolato per consentire test contract senza rete reale. */
interface AiTransport {
    suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Long
    ): TransportResponse

    suspend fun get(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): TransportResponse
}

data class TransportResponse(
    val statusCode: Int,
    val body: String?,
    val elapsedMs: Long = 0L
)
