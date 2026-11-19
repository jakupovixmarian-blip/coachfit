package com.fitcoach.app.collaudo

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.fitcoach.app.notifications.BootReceiver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Il ripristino delle notifiche gira senza UI (dopo un riavvio o un
 * aggiornamento dell'app). Un errore li non deve chiudere il processo:
 * l'utente lo percepisce come "l'app va in crash".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReceiverCrashTest {

    @Before
    fun setUp() {
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        ShadowLog.clear()
    }

    @Test
    fun `reschedule logs instead of propagating a scheduler failure`() = runBlocking {
        val receiver = BootReceiver()
        // Le dipendenze Hilt non sono iniettate: l'accesso solleva. Il receiver
        // deve registrare l'errore e terminare, senza rilanciare l'eccezione.
        receiver.reschedule()

        val handled = ShadowLog.getLogs().any {
            it.tag == "BootReceiver" && it.throwable != null
        }
        assertThat(handled).isTrue()
    }

    @Test
    fun `boot receiver ignores unrelated broadcasts`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        BootReceiver().onReceive(context, Intent("com.example.UNRELATED"))
        val touched = ShadowLog.getLogs().any { it.tag == "BootReceiver" }
        assertThat(touched).isFalse()
    }
}
