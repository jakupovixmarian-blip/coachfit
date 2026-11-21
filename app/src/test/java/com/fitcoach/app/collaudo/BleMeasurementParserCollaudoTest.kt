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

    // ---------------- Body Composition (BIA) ----------------

    /** Costruisce un payload 0x2A9C con i soli campi richiesti. */
    private fun bodyComposition(
        flags: Int,
        weightRaw: Int,
        optional: List<Int> = emptyList()
    ): ByteArray {
        val bytes = mutableListOf<Byte>()
        bytes.add(flags.toByte()); bytes.add((flags shr 8).toByte())
        bytes.add((weightRaw and 0xFF).toByte())
        bytes.add(((weightRaw shr 8) and 0xFF).toByte())
        // 2026-03-14 07:30:00
        bytes.add(0xEA.toByte()); bytes.add(0x07)
        bytes.add(3); bytes.add(14); bytes.add(7); bytes.add(30); bytes.add(0)
        optional.forEach {
            bytes.add((it and 0xFF).toByte())
            bytes.add(((it shr 8) and 0xFF).toByte())
        }
        return bytes.toByteArray()
    }

    @Test
    fun `la composizione corporea legge peso e massa grassa`() {
        // flags: peso + BMI(0x01) + grassa(0x08)
        val payload = bodyComposition(
            flags = 0x09,
            weightRaw = 15000, // 75.0 kg
            optional = listOf(232, 185) // BMI 23.2, grassa 18.5%
        )
        val result = BleMeasurementParser.parseBodyComposition(payload)!!
        assertThat(result.weightKg).isWithin(0.01).of(75.0)
        assertThat(result.bmi!!).isWithin(0.01).of(23.2)
        assertThat(result.fatPercent!!).isWithin(0.01).of(18.5)
        assertThat(result.measuredAt!!.year).isEqualTo(2026)
        assertThat(result.measuredAt!!.monthValue).isEqualTo(3)
        assertThat(result.measuredAt!!.dayOfMonth).isEqualTo(14)
    }

    @Test
    fun `i campi non trasmessi restano null e non vengono inventati`() {
        // Solo peso e BMI: nessun dato su grassa, muscolo o osso.
        val payload = bodyComposition(flags = 0x01, weightRaw = 16000, optional = listOf(240))
        val result = BleMeasurementParser.parseBodyComposition(payload)!!
        assertThat(result.weightKg).isWithin(0.01).of(80.0)
        assertThat(result.bmi!!).isWithin(0.01).of(24.0)
        assertThat(result.fatPercent).isNull()
        assertThat(result.muscleMassKg).isNull()
        assertThat(result.boneMassKg).isNull()
        assertThat(result.waterMassKg).isNull()
    }

    @Test
    fun `la massa muscolare viene letta solo se il flag e impostato`() {
        // BMI(1) + grassa(8) + magra(0x10) + muscolo(0x20)
        val payload = bodyComposition(
            flags = 0x39,
            weightRaw = 15000,
            optional = listOf(232, 185, 12000, 9000)
        )
        val result = BleMeasurementParser.parseBodyComposition(payload)!!
        assertThat(result.fatPercent!!).isWithin(0.01).of(18.5)
        assertThat(result.leanMassKg!!).isWithin(0.01).of(60.0)
        assertThat(result.muscleMassKg!!).isWithin(0.01).of(45.0)
        assertThat(result.boneMassKg).isNull()
    }

    @Test
    fun `un payload di composizione troppo corto non produce valori`() {
        assertThat(BleMeasurementParser.parseBodyComposition(byteArrayOf(0x01, 0x00, 0x01))).isNull()
    }

    @Test
    fun `il nome da mostrare usa quello annunciato quando presente`() {
        assertThat(
            com.fitcoach.app.domain.device.BleDeviceManager.displayName("AA:BB:CC:DD:EE:FF", "Mi Scale")
        ).isEqualTo("Mi Scale")
    }

    @Test
    fun `senza nome annunciato si mostra il MAC e non un testo generico`() {
        val label = com.fitcoach.app.domain.device.BleDeviceManager.displayName("AA:BB:CC:DD:EE:FF", null)
        assertThat(label).contains("EE:FF")
        assertThat(label).doesNotContain("sconosciuto")
    }
}
