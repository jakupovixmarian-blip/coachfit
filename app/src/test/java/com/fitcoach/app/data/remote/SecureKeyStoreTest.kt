package com.fitcoach.app.data.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * L'archivio chiavi non deve mai far crashare l'app: se il Keystore non e
 * disponibile le operazioni degradano a "nessuna chiave".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecureKeyStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `no key is reported before anything is stored`() {
        val store = SecureKeyStore(context)
        assertThat(store.hasKey("OPENAI")).isFalse()
        assertThat(store.getApiKey("OPENAI")).isNull()
    }

    private fun plainStore() =
        SecureKeyStore(context, context.getSharedPreferences("test_secure", Context.MODE_PRIVATE))

    @Test
    fun `stored key round trips`() {
        val store = plainStore()
        assertThat(store.putApiKey("OPENAI", "sk-test-123")).isTrue()
        assertThat(store.getApiKey("OPENAI")).isEqualTo("sk-test-123")
        assertThat(store.hasKey("OPENAI")).isTrue()
        assertThat(store.clear("OPENAI")).isTrue()
        assertThat(store.hasKey("OPENAI")).isFalse()
    }

    @Test
    fun `keys are namespaced per provider`() {
        val store = plainStore()
        store.putApiKey("OPENAI", "a")
        store.putApiKey("GROQ", "b")
        assertThat(store.getApiKey("OPENAI")).isEqualTo("a")
        assertThat(store.getApiKey("GROQ")).isEqualTo("b")
    }

    @Test
    fun `blank values are treated as absent`() {
        val store = plainStore()
        store.putApiKey("OPENAI", "   ")
        assertThat(store.getApiKey("OPENAI")).isNull()
        assertThat(store.hasKey("OPENAI")).isFalse()
    }

    @Test
    fun `operations never throw when the encrypted store is unusable`() {
        // Simula un Keystore corrotto: nessuna di queste chiamate deve propagare
        // un'eccezione (era la causa del crash aprendo le impostazioni AI).
        val store = SecureKeyStore(context)
        runCatching {
            context.deleteSharedPreferences("fitcoach_secure_prefs")
        }
        assertThat(store.hasKey("OPENAI")).isFalse()
        assertThat(store.getApiKey("OPENAI")).isNull()
        store.clear("OPENAI")
        store.putApiKey("OPENAI", "x")
    }
}
