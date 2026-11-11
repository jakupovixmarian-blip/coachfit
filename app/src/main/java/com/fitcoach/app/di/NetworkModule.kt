package com.fitcoach.app.di

import com.fitcoach.app.data.remote.AiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
            }
        )
        .build()

    @Provides
    @Singleton
    fun provideAiApi(client: OkHttpClient, json: Json): AiApi =
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/") // sostituito da @Url a ogni chiamata
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AiApi::class.java)

    @Provides
    @Singleton
    fun provideFoodApi(client: OkHttpClient, json: Json): com.fitcoach.app.data.remote.FoodApi =
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/") // sostituito da @Url a ogni chiamata
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(com.fitcoach.app.data.remote.FoodApi::class.java)
}
