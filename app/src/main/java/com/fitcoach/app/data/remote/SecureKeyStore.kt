package com.fitcoach.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
 * usare un backend proxy, cosi la chiave non risiede sul dispositivo.
 *
 * Il Keystore Android puo invalidare la master key senza preavviso: accade dopo
 * un ripristino da backup, una reinstallazione, un cambio di blocco schermo o su
 * alcuni firmware Samsung. In quel caso ogni accesso sollevava un'eccezione e
 * l'app andava in crash appena si aprivano le impostazioni AI (il ViewModel legge
 * lo stato delle chiavi nell'init). Ora l'archivio cifrato viene ricreato una
 * volta; se anche questo fallisce le operazioni degradano a "nessuna chiave"
 * invece di terminare il processo.
 */
@Singleton
class SecureKeyStore private constructor(
    private val context: Context,
    private val prefsProvider: (() -> SharedPreferences?)?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    /** Solo per i test: verifica la logica senza dipendere dal Keystore Android. */
    internal constructor(context: Context, prefs: SharedPreferences) : this(context, { prefs })

    private val prefs: SharedPreferences? by lazy { prefsProvider?.invoke() ?: openEncrypted() }

    fun putApiKey(providerKey: String, value: String): Boolean =
        runCatching { prefs?.edit()?.putString(prefix(providerKey), value)?.commit() }
            .getOrNull() == true

    fun getApiKey(providerKey: String): String? =
        runCatching { prefs?.getString(prefix(providerKey), null)?.takeIf { it.isNotBlank() } }
            .getOrNull()

    fun clear(providerKey: String): Boolean =
        runCatching { prefs?.edit()?.remove(prefix(providerKey))?.commit() }.getOrNull() == true

    fun hasKey(providerKey: String): Boolean = getApiKey(providerKey) != null

    private fun openEncrypted(): SharedPreferences? {
        runCatching { create(FILE_NAME) }.getOrNull()?.let { return it }

        // Master key non piu valida: si elimina l'archivio illeggibile e si ricrea.
        // Le chiavi API vanno reinserite, ma i dati utente (Room) restano intatti.
        Log.w(TAG, "Archivio chiavi illeggibile: ricreazione dell'archivio cifrato.")
        runCatching { context.deleteSharedPreferences(FILE_NAME) }

        return runCatching { create(REBUILD_FILE_NAME) }
            .onFailure {
                Log.e(TAG, "Keystore non disponibile: le chiavi API non saranno salvabili.", it)
            }
            .getOrNull()
    }

    /**
     * Dopo la ricreazione si usa un file con nome diverso: se la master key e
     * rotta, ricreare lo stesso nome non basta a renderlo leggibile.
     */
    private fun create(fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun prefix(key: String) = "api_key_${key.lowercase()}"

    companion object {
        private const val TAG = "SecureKeyStore"
        private const val FILE_NAME = "fitcoach_secure_prefs"
        private const val REBUILD_FILE_NAME = "fitcoach_secure_prefs_r1"
    }
}
