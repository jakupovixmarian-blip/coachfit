package com.fitcoach.app.collaudo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.health.HealthConnectReader
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Il caso "permesso concesso ma nessun dato" e' il piu' frequente su Samsung:
 * Health Connect risponde, il permesso c'e', i numeri no, perche' Samsung Health
 * non ha ancora sincronizzato. Senza una spiegazione l'utente pensa che l'app sia
 * rotta. Questi test bloccano quel comportamento.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HealthCoverageNotesTest {

    private val reader = HealthConnectReader(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `permesso concesso e nessun dato spiega il ritardo di sincronizzazione`() {
        val notes = reader.coverageNotes(
            granted = setOf("android.permission.health.READ_STEPS"),
            hasActivityData = false,
            restingHeartRate = 55,
            heartRateVariability = 40.0
        )

        assertThat(notes).hasSize(1)
        assertThat(notes.first()).contains("Samsung Health")
        assertThat(notes.first()).contains("sincronizzare")
        // Deve dire cosa fare, non solo che manca il dato.
        assertThat(notes.first()).contains("Aggiorna report")
    }

    @Test
    fun `permesso assente non viene confuso con un ritardo di sincronizzazione`() {
        val notes = reader.coverageNotes(
            granted = emptySet(),
            hasActivityData = false,
            restingHeartRate = 55,
            heartRateVariability = 40.0
        )

        assertThat(notes).hasSize(1)
        assertThat(notes.first()).contains("Permessi non concessi")
        assertThat(notes.first()).doesNotContain("sincronizzare")
    }

    @Test
    fun `con dati presenti non compare nessun avviso di dato mancante`() {
        val notes = reader.coverageNotes(
            granted = setOf("android.permission.health.READ_STEPS"),
            hasActivityData = true,
            restingHeartRate = 55,
            heartRateVariability = 40.0
        )

        assertThat(notes).isEmpty()
    }

    @Test
    fun `senza FC a riposo e HRV viene spiegato che lo stress non e' esportabile`() {
        val notes = reader.coverageNotes(
            granted = setOf("android.permission.health.READ_STEPS"),
            hasActivityData = true,
            restingHeartRate = null,
            heartRateVariability = null
        )

        assertThat(notes).hasSize(1)
        assertThat(notes.first()).contains("stress")
        assertThat(notes.first()).contains("non e' esportabile")
    }

    @Test
    fun `i due avvisi possono coesistere`() {
        val notes = reader.coverageNotes(
            granted = setOf("android.permission.health.READ_STEPS"),
            hasActivityData = false,
            restingHeartRate = null,
            heartRateVariability = null
        )

        assertThat(notes).hasSize(2)
    }
}
