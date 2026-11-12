package com.fitcoach.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiSettingsDataStore by preferencesDataStore("ai_settings")

data class StoredAiSettings(
    val provider: String,
    val baseUrl: String,
    val model: String
)

class AiSettingsStore(private val context: Context) {
    private object Keys {
        val provider = stringPreferencesKey("provider")
        val baseUrl = stringPreferencesKey("base_url")
        val model = stringPreferencesKey("model")
    }

    val settings: Flow<StoredAiSettings?> = context.aiSettingsDataStore.data.map { preferences: Preferences ->
        val provider = preferences[Keys.provider] ?: return@map null
        StoredAiSettings(provider, preferences[Keys.baseUrl].orEmpty(), preferences[Keys.model].orEmpty())
    }

    suspend fun save(settings: StoredAiSettings) {
        context.aiSettingsDataStore.edit { preferences ->
            preferences[Keys.provider] = settings.provider
            preferences[Keys.baseUrl] = settings.baseUrl
            preferences[Keys.model] = settings.model
        }
    }

    suspend fun clear() = context.aiSettingsDataStore.edit { it.clear() }
}
