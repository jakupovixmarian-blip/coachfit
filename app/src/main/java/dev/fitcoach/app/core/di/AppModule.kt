package dev.fitcoach.app.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.fitcoach.app.core.security.SecureKeyStore
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitCoachDatabase =
        Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.NAME)
            .addMigrations(*FitCoachDatabase.ALL)
            .build()

    @Provides
    @Singleton
    fun provideSecureKeyStore(@ApplicationContext context: Context): SecureKeyStore = SecureKeyStore(context)

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}
