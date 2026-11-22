package com.fitcoach.app.domain.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collegamento BLE che resta aperto, anche a schermo spento.
 *
 * [BleDeviceManager.connect] legge un solo dato e chiude: e' corretto per una
 * pesata, ma una fascia cardio che si scollega appena si spegne lo schermo non
 * serve a nulla. Qui il GATT resta vivo, le notifiche arrivano in streaming e
 * se il dispositivo esce e rientra dalla portata la connessione si riprende da
 * sola.
 *
 * Non inventa dati: se il dispositivo non risponde lo stato dice
 * [BleSessionState.Disconnected] con il motivo, e nessuna misurazione viene
 * prodotta finche' non ne arriva una reale.
 */
@Singleton
class BleSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleDeviceManager: BleDeviceManager
) {

    private companion object {
        const val TAG = "BleSession"
        /** Attesa prima di un nuovo tentativo dopo una caduta. */
        const val RECONNECT_DELAY_MS = 5_000L
        const val MAX_RECONNECT_ATTEMPTS = 20
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(BleSessionState())
    val state: StateFlow<BleSessionState> = _state.asStateFlow()

    /** Ultima misurazione ricevuta in streaming, consumata dalla UI. */
    private val _measurements = MutableStateFlow<SessionMeasurement?>(null)
    val measurements: StateFlow<SessionMeasurement?> = _measurements.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var reconnectJob: Job? = null
    private var attempts = 0
    private var stopping = false

    @Volatile
    private var pendingTarget: BluetoothGattCharacteristic? = null

    /**
     * Apre (o riapre) la connessione e la mantiene. Chiamare piu' volte con lo
     * stesso indirizzo non crea doppioni: se e' gia' attiva non fa nulla.
     */
    @SuppressLint("MissingPermission")
    fun start(address: String, name: String, kind: DeviceKind) {
        if (_state.value.address == address && gatt != null) return
        stopInternal(notify = false)
        stopping = false
        attempts = 0
        _state.value = BleSessionState(
            address = address,
            name = name,
            kind = kind,
            status = BleSessionStatus.CONNECTING
        )
        open(address)
    }

    private fun open(address: String) {
        if (stopping) return
        if (!bleDeviceManager.isSupported() || !bleDeviceManager.hasPermission()) {
            _state.value = _state.value.copy(
                status = BleSessionStatus.FAILED,
                detail = "Bluetooth spento o permessi mancanti."
            )
            return
        }
        val device = runCatching {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager)
                .adapter?.getRemoteDevice(address)
        }.getOrNull()
        if (device == null) {
            _state.value = _state.value.copy(
                status = BleSessionStatus.FAILED,
                detail = "Indirizzo Bluetooth non valido."
            )
            return
        }
        _state.value = _state.value.copy(status = BleSessionStatus.CONNECTING, detail = null)
        // autoConnect = true: e' il sistema a riprovare quando il dispositivo
        // torna in portata, senza consumare batteria con un loop nostro.
        gatt = device.connectGatt(context, true, callback, BluetoothDevice.TRANSPORT_LE)
    }

    /** Chiude la connessione e ferma i tentativi. */
    fun stop() = stopInternal(notify = true)

    private fun stopInternal(notify: Boolean) {
        stopping = true
        reconnectJob?.cancel()
        reconnectJob = null
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        pendingTarget = null
        attempts = 0
        if (notify) {
            _state.value = BleSessionState()
            _measurements.value = null
        }
    }

    private fun scheduleReconnect() {
        if (stopping) return
        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            _state.value = _state.value.copy(
                status = BleSessionStatus.FAILED,
                detail = "Dispositivo non raggiungibile. Riavvia il collegamento."
            )
            return
        }
        attempts++
        _state.value = _state.value.copy(status = BleSessionStatus.RECONNECTING)
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (stopping) return@launch
            runCatching { gatt?.close() }
            gatt = null
            _state.value.address?.let { open(it) }
        }
    }

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        scheduleReconnect(); return
                    }
                    attempts = 0
                    _state.value = _state.value.copy(status = BleSessionStatus.CONNECTED, detail = null)
                    runCatching { gatt.discoverServices() }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    pendingTarget = null
                    runCatching { gatt.close() }
                    if (stopping) {
                        _state.value = _state.value.copy(status = BleSessionStatus.DISCONNECTED)
                    } else {
                        // Una caduta non e' un errore da mostrare come guasto:
                        // il sistema sta gia' riprovando.
                        scheduleReconnect()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = _state.value.copy(
                    status = BleSessionStatus.FAILED,
                    detail = "Servizi non leggibili (codice $status)."
                )
                return
            }
            val kind = _state.value.kind
            val serviceUuid = if (kind == DeviceKind.HEART_RATE) {
                BleDeviceManager.HEART_RATE_SERVICE
            } else {
                BleDeviceManager.WEIGHT_SCALE_SERVICE
            }
            val charUuid = if (kind == DeviceKind.HEART_RATE) {
                BleDeviceManager.HEART_RATE_MEASUREMENT_CHAR
            } else {
                BleDeviceManager.WEIGHT_MEASUREMENT_CHAR
            }
            val target = gatt.getService(serviceUuid)?.getCharacteristic(charUuid)
                ?: gatt.getService(BleDeviceManager.BODY_COMPOSITION_SERVICE)
                    ?.getCharacteristic(BleDeviceManager.BODY_COMPOSITION_CHAR)
            if (target == null) {
                _state.value = _state.value.copy(
                    status = BleSessionStatus.FAILED,
                    detail = "Il dispositivo non espone il servizio atteso."
                )
                return
            }
            pendingTarget = target
            // Il nome va letto per primo: le operazioni GATT sono seriali e una
            // seconda richiesta sovrapposta fallisce. Solo alla risposta si
            // abilitano le notifiche.
            val nameChar = gatt.getService(BleDeviceManager.GENERIC_ACCESS_SERVICE)
                ?.getCharacteristic(BleDeviceManager.GAP_DEVICE_NAME_CHAR)
            val nameStarted = nameChar != null &&
                runCatching { gatt.readCharacteristic(nameChar) }.getOrDefault(false)
            if (!nameStarted) enableNotifications(gatt, target)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (characteristic.uuid == BleDeviceManager.GAP_DEVICE_NAME_CHAR) {
                val name = runCatching { String(value, Charsets.UTF_8).trim() }.getOrNull()
                if (!name.isNullOrBlank()) {
                    _state.value = _state.value.copy(name = name)
                }
                pendingTarget?.let { enableNotifications(gatt, it) }
                return
            }
            handle(characteristic, value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            characteristic.value?.let { onCharacteristicRead(gatt, characteristic, it, status) }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _state.value = _state.value.copy(status = BleSessionStatus.STREAMING)
            } else {
                _state.value = _state.value.copy(
                    status = BleSessionStatus.FAILED,
                    detail = "Il dispositivo ha rifiutato l'invio dei dati."
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handle(characteristic, value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic.value?.let { handle(characteristic, it) }
        }

        private fun enableNotifications(
            gatt: BluetoothGatt,
            target: BluetoothGattCharacteristic
        ) {
            gatt.setCharacteristicNotification(target, true)
            val descriptor = target.getDescriptor(BleDeviceManager.CLIENT_CONFIG_DESCRIPTOR)
            if (descriptor == null) {
                _state.value = _state.value.copy(
                    status = BleSessionStatus.CONNECTED,
                    detail = "Il dispositivo non supporta l'invio continuo dei dati."
                )
                return
            }
            val enable = if (target.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            descriptor.value = enable
            val started = runCatching { gatt.writeDescriptor(descriptor) }.getOrDefault(false)
            if (!started) {
                _state.value = _state.value.copy(
                    status = BleSessionStatus.FAILED,
                    detail = "Impossibile attivare lo streaming dei dati."
                )
            }
        }

        private fun handle(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == BleDeviceManager.GAP_DEVICE_NAME_CHAR) return
            val kind = _state.value.kind
            when (characteristic.uuid) {
                BleDeviceManager.BODY_COMPOSITION_CHAR ->
                    BleMeasurementParser.parseBodyComposition(value)?.let {
                        _measurements.value = SessionMeasurement(
                            kind = DeviceKind.SCALE,
                            value = it.weightKg,
                            unit = "kg",
                            composition = it
                        )
                    }
                BleDeviceManager.WEIGHT_MEASUREMENT_CHAR ->
                    BleMeasurementParser.parseWeight(value)?.let {
                        _measurements.value = SessionMeasurement(kind, it.value, it.unit)
                    }
                BleDeviceManager.HEART_RATE_MEASUREMENT_CHAR ->
                    BleMeasurementParser.parseHeartRate(value)?.let {
                        _measurements.value = SessionMeasurement(kind, it.value, it.unit)
                    }
            }
            Log.d(TAG, "Misurazione in streaming da ${_state.value.name}")
        }
    }

    /** Consuma l'ultima misurazione, per non ripeterla alla riapertura della UI. */
    fun consumeMeasurement() {
        _measurements.value = null
    }
}

enum class BleSessionStatus {
    /** Nessuna sessione attiva. */
    IDLE,
    CONNECTING,
    CONNECTED,
    /** Notifiche attive: il dispositivo sta inviando i dati. */
    STREAMING,
    RECONNECTING,
    DISCONNECTED,
    FAILED
}

data class BleSessionState(
    val address: String? = null,
    val name: String? = null,
    val kind: DeviceKind = DeviceKind.UNKNOWN,
    val status: BleSessionStatus = BleSessionStatus.IDLE,
    val detail: String? = null
) {
    val active: Boolean
        get() = status == BleSessionStatus.CONNECTING ||
            status == BleSessionStatus.CONNECTED ||
            status == BleSessionStatus.STREAMING ||
            status == BleSessionStatus.RECONNECTING
}

data class SessionMeasurement(
    val kind: DeviceKind,
    val value: Double,
    val unit: String,
    val composition: BodyComposition? = null
)
