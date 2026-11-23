package com.fitcoach.app.audit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.domain.device.BleDeviceManager
import com.fitcoach.app.domain.device.BleSessionManager
import com.fitcoach.app.domain.device.BleSessionState
import com.fitcoach.app.domain.device.BleSessionStatus
import com.fitcoach.app.domain.device.DeviceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La sessione BLE non si puo' provare con un GATT finto senza mentire sul
 * dispositivo. Si provano i percorsi reali che non richiedono hardware:
 * l'assenza di adattatore, lo stop, e la logica di stato.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BleSessionAdversarialTest {

    private lateinit var context: Context
    private lateinit var manager: BleSessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = BleSessionManager(context, BleDeviceManager(context))
    }

    @Test
    fun `lo stato iniziale non dichiara nessun dispositivo collegato`() {
        val s = manager.state.value
        assertNull("Nessun indirizzo prima del collegamento", s.address)
        assertEquals(BleSessionStatus.IDLE, s.status)
        assertFalse("Una sessione inattiva non e' attiva", s.active)
    }

    @Test
    fun `nessuna misurazione viene inventata prima del collegamento`() {
        assertNull(
            "Senza dispositivo non esiste alcuna misura",
            manager.measurements.value
        )
    }

    @Test
    fun `avviare senza Bluetooth produce un errore spiegato`() {
        // Robolectric non fornisce un adattatore: il percorso e' quello reale
        // di un telefono con Bluetooth spento.
        manager.start("AA:BB:CC:DD:EE:FF", "Bilancia", DeviceKind.SCALE)
        val s = manager.state.value
        assertEquals(BleSessionStatus.FAILED, s.status)
        assertNotNull("L'errore deve dire cosa manca", s.detail)
        assertTrue(
            "Il motivo deve essere comprensibile: ${s.detail}",
            s.detail!!.contains("Bluetooth", ignoreCase = true)
        )
        assertFalse("Una sessione fallita non e' attiva", s.active)
    }

    @Test
    fun `un avvio fallito non produce comunque misurazioni`() {
        manager.start("AA:BB:CC:DD:EE:FF", "Bilancia", DeviceKind.SCALE)
        assertNull(manager.measurements.value)
    }

    @Test
    fun `stop riporta lo stato a idle e cancella la misura pendente`() {
        manager.start("AA:BB:CC:DD:EE:FF", "Bilancia", DeviceKind.SCALE)
        manager.stop()
        val s = manager.state.value
        assertEquals(BleSessionStatus.IDLE, s.status)
        assertNull("Nessun indirizzo dopo lo stop", s.address)
        assertNull("Nessuna misura dopo lo stop", manager.measurements.value)
        assertFalse(s.active)
    }

    @Test
    fun `stop e idempotente`() {
        manager.stop()
        manager.stop()
        assertEquals(BleSessionStatus.IDLE, manager.state.value.status)
    }

    @Test
    fun `consumeMeasurement non lancia quando non c'e' nulla da consumare`() {
        manager.consumeMeasurement()
        assertNull(manager.measurements.value)
    }

    @Test
    fun `avviare due volte lo stesso indirizzo non duplica lo stato`() {
        manager.start("AA:BB:CC:DD:EE:FF", "Bilancia", DeviceKind.SCALE)
        val primo = manager.state.value
        manager.start("AA:BB:CC:DD:EE:FF", "Bilancia", DeviceKind.SCALE)
        val secondo = manager.state.value
        assertEquals(
            "Un secondo start con lo stesso indirizzo non deve cambiare stato",
            primo.status, secondo.status
        )
    }

    // ---------- BleSessionState ----------

    @Test
    fun `solo gli stati di transizione contano come attivi`() {
        assertTrue(BleSessionState(status = BleSessionStatus.CONNECTING).active)
        assertTrue(BleSessionState(status = BleSessionStatus.CONNECTED).active)
        assertTrue(BleSessionState(status = BleSessionStatus.STREAMING).active)
        assertTrue(BleSessionState(status = BleSessionStatus.RECONNECTING).active)
        assertFalse(BleSessionState(status = BleSessionStatus.IDLE).active)
        assertFalse(BleSessionState(status = BleSessionStatus.DISCONNECTED).active)
        assertFalse(
            "Una sessione fallita non deve risultare attiva",
            BleSessionState(status = BleSessionStatus.FAILED).active
        )
    }

    // ---------- BleDeviceManager ----------

    @Test
    fun `senza adattatore il supporto e dichiarato assente`() {
        val ble = BleDeviceManager(context)
        assertFalse("Robolectric non espone un adattatore", ble.isSupported())
    }

    @Test
    fun `senza permessi la scansione non e consentita`() {
        val ble = BleDeviceManager(context)
        assertFalse(ble.hasPermission())
    }

    @Test
    fun `i permessi richiesti cambiano con la versione di Android`() {
        val ble = BleDeviceManager(context)
        val richiesti = ble.requiredPermissions()
        assertTrue("Deve chiedere almeno un permesso", richiesti.isNotEmpty())
        assertTrue(
            "Su API 33 servono i permessi Bluetooth, non la posizione: $richiesti",
            richiesti.none { it.contains("LOCATION") }
        )
    }
}
