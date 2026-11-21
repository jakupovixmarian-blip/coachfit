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
     * Body Composition Measurement (0x2A9C), usata dalle bilance BIA.
     *
     * Struttura (Bluetooth SIG):
     *  - byte 0: flags;
     *  - byte 1-2: peso uint16 little-endian, risoluzione 0.005 kg;
     *  - byte 3-4: anno uint16, poi mese, giorno, ore, minuti, secondi;
     *  - campi opzionali, presenti solo se il flag corrispondente e impostato,
     *    NELL'ORDINE dei bit: BMI (bit 0), altezza (1), circonferenza (2),
     *    massa grassa % (3), massa magra (4), massa muscolare (5), massa ossea (6),
     *    massa d'acqua (7), impedenza (8).
     *
     * I campi non presenti restano null: non vengono stimati ne dedotti.
     */
    fun parseBodyComposition(value: ByteArray): BodyComposition? {
        if (value.size < 11) return null
        val flags = value[0].toInt() and 0xFF
        val rawWeight = ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
        val weightKg = rawWeight / 200.0

        var index = 3
        // Timestamp: anno, mese, giorno, ore, minuti, secondi.
        val year = u16(value, index) ?: return null
        index += 2
        val month = value.getOrNull(index++)?.toInt()?.and(0xFF) ?: return null
        val day = value.getOrNull(index++)?.toInt()?.and(0xFF) ?: return null
        val hour = value.getOrNull(index++)?.toInt()?.and(0xFF) ?: return null
        val minute = value.getOrNull(index++)?.toInt()?.and(0xFF) ?: return null
        val second = value.getOrNull(index++)?.toInt()?.and(0xFF) ?: return null

        fun nextRaw(): Int? = u16(value, index).also { if (it != null) index += 2 }

        val bmi = if (flags and 0x01 != 0) nextRaw()?.let { it / 10.0 } else null
        val heightM = if (flags and 0x02 != 0) nextRaw()?.let { it / 1000.0 } else null
        // Bit 2: circonferenza in mm. Non registrata: il campo viene solo consumato
        // per non sfalsare l'offset dei campi successivi.
        if (flags and 0x04 != 0) nextRaw()
        val fatPercent = if (flags and 0x08 != 0) nextRaw()?.let { it / 10.0 } else null
        val leanMassKg = if (flags and 0x10 != 0) nextRaw()?.let { it / 200.0 } else null
        val muscleMassKg = if (flags and 0x20 != 0) nextRaw()?.let { it / 200.0 } else null
        val boneMassKg = if (flags and 0x40 != 0) nextRaw()?.let { it / 200.0 } else null
        val waterMassKg = if (flags and 0x80 != 0) nextRaw()?.let { it / 200.0 } else null
        // Bit 8 (secondo byte di flags): impedenza in ohm, non usata dal diario.
        val impedanceOhm = if (flags and 0x100 != 0) nextRaw() else null

        return BodyComposition(
            weightKg = weightKg,
            bmi = bmi,
            heightM = heightM,
            fatPercent = fatPercent,
            leanMassKg = leanMassKg,
            muscleMassKg = muscleMassKg,
            boneMassKg = boneMassKg,
            waterMassKg = waterMassKg,
            impedanceOhm = impedanceOhm,
            measuredAt = runCatching {
                java.time.LocalDateTime.of(year, month, day, hour, minute, second)
            }.getOrNull()
        )
    }

    private fun u16(value: ByteArray, index: Int): Int? {
        if (index + 1 >= value.size) return null
        return ((value[index + 1].toInt() and 0xFF) shl 8) or (value[index].toInt() and 0xFF)
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
