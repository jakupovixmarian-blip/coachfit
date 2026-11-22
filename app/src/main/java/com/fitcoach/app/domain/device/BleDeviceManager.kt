package com.fitcoach.app.domain.device

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accesso reale al Bluetooth Low Energy per collegare dispositivi fitness:
 * bilance connesse (Weight Scale Service 0x181D) e fasce/watch cardio
 * (Heart Rate Service 0x180D).
 *
 * Non inventa dati: se l'adattatore non e disponibile o i permessi mancano,
 * [isSupported] e [hasPermission] lo segnalano e la UI spiega cosa manca.
 */
@Singleton
class BleDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** Weight Scale Service e caratteristica Weight Measurement (Bluetooth SIG). */
        val WEIGHT_SCALE_SERVICE: java.util.UUID =
            java.util.UUID.fromString("0000181d-0000-1000-8000-00805f9b34fb")
        val WEIGHT_MEASUREMENT_CHAR: java.util.UUID =
            java.util.UUID.fromString("00002a9d-0000-1000-8000-00805f9b34fb")

        /** Body Composition Service: bilance BIA che riportano massa grassa e muscolare. */
        val BODY_COMPOSITION_SERVICE: java.util.UUID =
            java.util.UUID.fromString("0000181b-0000-1000-8000-00805f9b34fb")
        val BODY_COMPOSITION_CHAR: java.util.UUID =
            java.util.UUID.fromString("00002a9c-0000-1000-8000-00805f9b34fb")

        /** Heart Rate Service e caratteristica Heart Rate Measurement. */
        val HEART_RATE_SERVICE: java.util.UUID =
            java.util.UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_CHAR: java.util.UUID =
            java.util.UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

        /** Generic Access: nome del dispositivo. Spesso leggibile solo dopo la connessione. */
        val GENERIC_ACCESS_SERVICE: java.util.UUID =
            java.util.UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        val GAP_DEVICE_NAME_CHAR: java.util.UUID =
            java.util.UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")

        /**
         * Nome da mostrare. Molte bilance non mettono il nome nell'adv packet
         * (e `device.name` resta null finche' non si connette): in quel caso si
         * mostra il MAC con l'indicazione che il nome arrivera' dopo il
         * collegamento, invece di un generico "Dispositivo sconosciuto".
         */
        internal fun displayName(address: String, advertised: String?): String =
            advertised?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Dispositivo senza nome (${address.takeLast(5)})"

        const val CONNECT_TIMEOUT_MS = 20_000L

        /** Client Characteristic Configuration Descriptor: abilita notifiche/indications. */
        val CLIENT_CONFIG_DESCRIPTOR: java.util.UUID =
            java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /** Vero se il dispositivo ha un adattatore Bluetooth Low Energy utilizzabile. */
    fun isSupported(): Boolean = adapter?.isEnabled == true

    /** Permessi runtime necessari per la scansione, secondo la versione di Android. */
    fun requiredPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasPermission(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Avvia la scansione BLE e restituisce i dispositivi trovati come flusso. */
    @SuppressLint("MissingPermission")
    fun scan(): Flow<List<DiscoveredDevice>> = callbackFlow {
        if (!isSupported() || !hasPermission()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val found = linkedMapOf<String, DiscoveredDevice>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val advertised = result.scanRecord?.deviceName ?: result.device.name
                val services = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
                val device = DiscoveredDevice(
                    address = result.device.address,
                    name = displayName(result.device.address, advertised),
                    rssi = result.rssi,
                    kind = classify(services, advertised),
                    hasWeightService = services.contains(WEIGHT_SCALE_SERVICE),
                    hasBodyCompositionService = services.contains(BODY_COMPOSITION_SERVICE),
                    hasHeartRateService = services.contains(HEART_RATE_SERVICE)
                )
                found[result.device.address] = device
                trySend(found.values.sortedByDescending { it.rssi })
            }

            override fun onScanFailed(errorCode: Int) {
                close(ScanFailure(errorCode))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, callback)

        awaitClose {
            runCatching { scanner.stopScan(callback) }
        }
    }

    private fun classify(services: List<java.util.UUID>, name: String?): DeviceKind = when {
        services.contains(BODY_COMPOSITION_SERVICE) -> DeviceKind.SCALE
        services.contains(WEIGHT_SCALE_SERVICE) -> DeviceKind.SCALE
        services.contains(HEART_RATE_SERVICE) -> DeviceKind.HEART_RATE
        name == null -> DeviceKind.UNKNOWN
        name.contains("scale", true) || name.contains("bilancia", true) -> DeviceKind.SCALE
        name.contains("hr", true) || name.contains("polar", true) ||
            name.contains("garmin", true) || name.contains("watch", true) -> DeviceKind.HEART_RATE
        else -> DeviceKind.UNKNOWN
    }


}

enum class DeviceKind(val label: String) {
    SCALE("Bilancia"),
    HEART_RATE("Cardiofrequenzimetro"),
    UNKNOWN("Dispositivo")
}

data class DiscoveredDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val kind: DeviceKind,
    val hasWeightService: Boolean,
    val hasBodyCompositionService: Boolean = false,
    val hasHeartRateService: Boolean
)

/**
 * Stato del collegamento, emesso mentre la connessione procede: la UI puo cosi
 * distinguere "connesso", "in attesa del dato" ed errore, invece di restare
 * ferma su un indicatore indefinito.
 */
sealed interface ConnectionEvent {
    data object Connected : ConnectionEvent
    /** Nome letto dal dispositivo dopo la connessione (GAP 0x2A00). */
    data class DeviceName(val name: String) : ConnectionEvent
    data class Measured(val value: Measurement) : ConnectionEvent
    data class Body(val composition: BodyComposition) : ConnectionEvent
    data class Failed(val message: String) : ConnectionEvent
}

/** Errore di scansione, propagato alla UI invece di chiudere in silenzio. */
class ScanFailure(val errorCode: Int) : Exception("Scansione BLE fallita (codice $errorCode)")

data class Measurement(
    val kind: DeviceKind,
    val value: Double,
    val unit: String
)

/**
 * Misurazione completa di composizione corporea (Body Composition Measurement
 * 0x2A9C). Solo i campi realmente trasmessi dalla bilancia sono valorizzati:
 * una bilancia che non misura la massa ossea lascia `boneMassKg` a null, e
 * l'app non lo stima.
 */
data class BodyComposition(
    val weightKg: Double,
    val bmi: Double? = null,
    val heightM: Double? = null,
    val fatPercent: Double? = null,
    val leanMassKg: Double? = null,
    val muscleMassKg: Double? = null,
    val boneMassKg: Double? = null,
    val waterMassKg: Double? = null,
    val impedanceOhm: Int? = null,
    val measuredAt: java.time.LocalDateTime? = null
)
