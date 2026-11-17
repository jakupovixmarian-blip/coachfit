package com.fitcoach.app.domain.device

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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

        /** Heart Rate Service e caratteristica Heart Rate Measurement. */
        val HEART_RATE_SERVICE: java.util.UUID =
            java.util.UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_CHAR: java.util.UUID =
            java.util.UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
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
                val name = result.device.name ?: result.scanRecord?.deviceName ?: "Dispositivo sconosciuto"
                val services = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
                val device = DiscoveredDevice(
                    address = result.device.address,
                    name = name,
                    rssi = result.rssi,
                    kind = classify(services, name),
                    hasWeightService = services.contains(WEIGHT_SCALE_SERVICE),
                    hasHeartRateService = services.contains(HEART_RATE_SERVICE)
                )
                found[result.device.address] = device
                trySend(found.values.sortedByDescending { it.rssi })
            }

            override fun onScanFailed(errorCode: Int) {
                close()
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

    private fun classify(services: List<java.util.UUID>, name: String): DeviceKind = when {
        services.contains(WEIGHT_SCALE_SERVICE) -> DeviceKind.SCALE
        services.contains(HEART_RATE_SERVICE) -> DeviceKind.HEART_RATE
        name.contains("scale", true) || name.contains("bilancia", true) -> DeviceKind.SCALE
        name.contains("hr", true) || name.contains("polar", true) ||
            name.contains("garmin", true) || name.contains("watch", true) -> DeviceKind.HEART_RATE
        else -> DeviceKind.UNKNOWN
    }

    /**
     * Si collega al dispositivo e legge una misurazione reale.
     * Restituisce un [Measurement] con il valore letto dal GATT, oppure null se il
     * servizio non e disponibile.
     */
    @SuppressLint("MissingPermission")
    fun readMeasurement(address: String, kind: DeviceKind): Flow<Measurement?> = callbackFlow {
        if (!isSupported() || !hasPermission()) {
            trySend(null); close(); return@callbackFlow
        }
        val device = adapter?.getRemoteDevice(address)
        if (device == null) {
            trySend(null); close(); return@callbackFlow
        }

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val serviceUuid = if (kind == DeviceKind.SCALE) WEIGHT_SCALE_SERVICE else HEART_RATE_SERVICE
                val charUuid = if (kind == DeviceKind.SCALE) WEIGHT_MEASUREMENT_CHAR else HEART_RATE_MEASUREMENT_CHAR
                val characteristic = gatt.getService(serviceUuid)?.getCharacteristic(charUuid)
                if (characteristic == null) {
                    close(); return
                }
                gatt.readCharacteristic(characteristic)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                val parsed = parse(characteristic, kind)
                trySend(parsed)
                gatt.disconnect()
                close()
            }
        }

        val gatt = device.connectGatt(context, false, callback)
        awaitClose { runCatching { gatt.close() } }
    }

    private fun parse(characteristic: BluetoothGattCharacteristic, kind: DeviceKind): Measurement? {
        val value = characteristic.value ?: return null
        if (value.isEmpty()) return null
        return when (kind) {
            DeviceKind.SCALE -> BleMeasurementParser.parseWeight(value)
            DeviceKind.HEART_RATE -> BleMeasurementParser.parseHeartRate(value)
            DeviceKind.UNKNOWN -> null
        }
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
    val hasHeartRateService: Boolean
)

data class Measurement(
    val kind: DeviceKind,
    val value: Double,
    val unit: String
)
