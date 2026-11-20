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
        // readTimeout vale per singola lettura: un provider che apre la
        // connessione e invia un byte ogni 80s non lo fa mai scattare, e la
        // schermata resta su "Generazione..." a tempo indefinito. callTimeout
        // limita la durata complessiva della richiesta, inclusi i retry del
        // client. Il valore supera i 90s di read per non uccidere le risposte
        // lente ma sane, e resta sotto la deadline del turno (6 min).
        .callTimeout(150, TimeUnit.SECONDS)
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
