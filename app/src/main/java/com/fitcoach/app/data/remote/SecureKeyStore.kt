package com.fitcoach.app.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Archivio sicuro per le chiavi API.
 *
 * Le chiavi NON vengono mai salvate nel database in chiaro ne incluse nell'APK:
 * sono cifrate con EncryptedSharedPreferences (AES-256, master key nel Keystore
 * hardware quando disponibile). Per le build di produzione si consiglia comunque
 * di usare un backend proxy, cosi la chiave non risiede sul dispositivo.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putApiKey(providerKey: String, value: String) {
        prefs.edit().putString(prefix(providerKey), value).apply()
    }

    fun getApiKey(providerKey: String): String? =
        prefs.getString(prefix(providerKey), null)?.takeIf { it.isNotBlank() }

    fun clear(providerKey: String) {
        prefs.edit().remove(prefix(providerKey)).apply()
    }

    fun hasKey(providerKey: String): Boolean = getApiKey(providerKey) != null

    private fun prefix(key: String) = "api_key_${key.lowercase()}"

    companion object {
        private const val FILE_NAME = "fitcoach_secure_prefs"
    }
}
