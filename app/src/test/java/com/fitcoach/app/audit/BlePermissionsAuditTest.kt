package com.fitcoach.app.audit

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.domain.device.BleDeviceManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * I permessi Bluetooth cambiano nome e semantica tra Android 11 e 12. Chiedere
 * i permessi sbagliati per la versione in uso e' il modo piu' comune in cui lo
 * scan "non trova nulla" senza mostrare errori.
 */
@RunWith(RobolectricTestRunner::class)
class BlePermissionsAuditTest {

    private fun manager(): BleDeviceManager =
        BleDeviceManager(ApplicationProvider.getApplicationContext<Context>())

    @Test
    @Config(sdk = [33])
    fun `su Android 13 chiede SCAN e CONNECT, non la posizione`() {
        val required = manager().requiredPermissions()
        assertThat(required).containsExactly(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        assertThat(required).doesNotContain(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    @Config(sdk = [30])
    fun `su Android 11 chiede la posizione, che li' serve per lo scan`() {
        val required = manager().requiredPermissions()
        assertThat(required).containsExactly(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    @Config(sdk = [26])
    fun `sul minimo supportato chiede la posizione`() {
        val required = manager().requiredPermissions()
        assertThat(required).containsExactly(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    @Config(sdk = [33])
    fun `senza permessi concessi hasPermission e' falso`() {
        assertThat(manager().hasPermission()).isFalse()
    }
}
