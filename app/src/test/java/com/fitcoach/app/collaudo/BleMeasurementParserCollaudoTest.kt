package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.device.BleMeasurementParser
import com.fitcoach.app.domain.device.DeviceKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Collaudo della decodifica dei payload BLE standard (Bluetooth SIG).
 *
 * I byte di prova sono costruiti secondo le specifiche:
 *  - Weight Measurement (0x2A9D): flags + peso uint16 little-endian / 200;
 *  - Heart Rate Measurement (0x2A37): flags + bpm (uint8 o uint16).
 */
class BleMeasurementParserCollaudoTest {

    @Test
    fun `il peso in kg viene decodificato dai byte little-endian`() {
        // flags=0x00 (kg), peso 75.0 kg -> raw = 75 * 200 = 15000 = 0x3A98
        val bytes = byteArrayOf(0x00, 0x98.toByte(), 0x3A)
        val result = BleMeasurementParser.parseWeight(bytes)!!
        assertThat(result.kind).isEqualTo(DeviceKind.SCALE)
        assertThat(result.unit).isEqualTo("kg")
        assertThat(result.value).isWithin(0.01).of(75.0)
    }

    @Test
    fun `un peso con risoluzione 0,005 kg e gestito`() {
        // raw = 15001 -> 75.005 kg
        val bytes = byteArrayOf(0x00, 0x99.toByte(), 0x3A)
        val result = BleMeasurementParser.parseWeight(bytes)!!
        assertThat(result.value).isWithin(0.001).of(75.005)
    }

    @Test
    fun `il flag libbre converte in chilogrammi`() {
        // flags=0x01 (lb), peso 165.3467 lb -> kg ~ 75.0
        val rawLb = (75.0 / 0.45359237 * 200).toInt()
        val bytes = byteArrayOf(0x01, (rawLb and 0xFF).toByte(), ((rawLb shr 8) and 0xFF).toByte())
        val result = BleMeasurementParser.parseWeight(bytes)!!
        assertThat(result.value).isWithin(0.05).of(75.0)
    }

    @Test
    fun `un payload peso troppo corto non produce valori`() {
        assertThat(BleMeasurementParser.parseWeight(byteArrayOf(0x00, 0x01))).isNull()
    }

    @Test
    fun `la frequenza cardiaca uint8 viene letta correttamente`() {
        // flags=0x00, bpm=72
        val bytes = byteArrayOf(0x00, 72)
        val result = BleMeasurementParser.parseHeartRate(bytes)!!
        assertThat(result.kind).isEqualTo(DeviceKind.HEART_RATE)
        assertThat(result.value).isWithin(0.01).of(72.0)
        assertThat(result.unit).isEqualTo("bpm")
    }

    @Test
    fun `la frequenza cardiaca uint16 viene letta correttamente`() {
        // flags=0x01 (uint16), bpm=300 -> 0x012C
        val bytes = byteArrayOf(0x01, 0x2C, 0x01)
        val result = BleMeasurementParser.parseHeartRate(bytes)!!
        assertThat(result.value).isWithin(0.01).of(300.0)
    }

    @Test
    fun `un payload cardio vuoto non produce valori`() {
        assertThat(BleMeasurementParser.parseHeartRate(byteArrayOf())).isNull()
    }
}
