package com.fitcoach.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Endpoint compatibile con l'API OpenAI Chat Completions.
 * Supporta OpenAI, Groq, OpenRouter, DeepSeek e endpoint custom.
 */
interface AiApi {
    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("HTTP-Referer") referer: String? = null,
        @Header("X-Title") title: String? = null,
        @Body body: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
