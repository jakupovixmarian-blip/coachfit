package com.fitcoach.app.domain.device

/**
 * Decodifica i payload delle caratteristiche BLE standard Bluetooth SIG.
 *
 * Estratto da [BleDeviceManager] per essere verificabile con test reali sui byte:
 * nessuna dipendenza da Android.
 */
object BleMeasurementParser {

    /**
     * Weight Measurement (0x2A9D).
     * Byte 0: flags. Bit 0 = unita (0 = kg, 1 = lb). Bit 1 = presenza timestamp.
     * Byte 1-2: peso a 16 bit little-endian, con risoluzione 0.005 kg (valore / 200).
     */
    fun parseWeight(value: ByteArray): Measurement? {
        if (value.size < 3) return null
        val flags = value[0].toInt() and 0xFF
        val imperial = flags and 0x01 == 0x01
        val raw = ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
        val weight = raw / 200.0
        val kg = if (imperial) weight * 0.45359237 else weight
        return Measurement(kind = DeviceKind.SCALE, value = kg, unit = "kg")
    }

    /**
     * Heart Rate Measurement (0x2A37).
     * Byte 0: flags. Bit 0 = formato del valore (0 = uint8, 1 = uint16).
     */
    fun parseHeartRate(value: ByteArray): Measurement? {
        if (value.isEmpty()) return null
        val flags = value[0].toInt() and 0xFF
        val bpm = if (flags and 0x01 == 0x01) {
            if (value.size < 3) return null
            ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
        } else {
            if (value.size < 2) return null
            value[1].toInt() and 0xFF
        }
        return Measurement(kind = DeviceKind.HEART_RATE, value = bpm.toDouble(), unit = "bpm")
    }
}
