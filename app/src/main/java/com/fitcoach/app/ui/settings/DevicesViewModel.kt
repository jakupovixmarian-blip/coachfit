package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.domain.device.BleDeviceManager
import com.fitcoach.app.domain.device.BleSessionManager
import com.fitcoach.app.domain.device.BleSessionService
import com.fitcoach.app.domain.device.BleSessionStatus
import com.fitcoach.app.domain.device.BodyComposition
import com.fitcoach.app.domain.device.DeviceKind
import com.fitcoach.app.domain.device.DiscoveredDevice
import com.fitcoach.app.domain.device.Measurement
import com.fitcoach.app.domain.model.WeightRecord
import androidx.core.app.ActivityCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DevicesUiState(
    val bluetoothSupported: Boolean = false,
    val permissionGranted: Boolean = false,
    val scanning: Boolean = false,
    val devices: List<DiscoveredDevice> = emptyList(),
    val connectedDevice: DiscoveredDevice? = null,
    /** Fase del collegamento, per non lasciare la UI su un indicatore indefinito. */
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val reading: Boolean = false,
    val lastMeasurement: Measurement? = null,
    /** Composizione corporea completa, quando la bilancia la trasmette (BIA). */
    val lastComposition: BodyComposition? = null,
    val message: String? = null,
    /** Collegamento persistente attivo (foreground service), non solo la lettura. */
    val sessionActive: Boolean = false,
    val sessionStatus: BleSessionStatus = BleSessionStatus.IDLE,
    val sessionDetail: String? = null,
    val sessionName: String? = null
)

/**
 * Gestisce la sezione Dispositivi: scansione BLE reale, collegamento e lettura
 * di bilance e cardiofrequenzimetri. Il peso letto da una bilancia viene
 * registrato nei progressi solo dopo conferma esplicita dell'utente.
 */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val bleDeviceManager: BleDeviceManager,
    private val sessionManager: BleSessionManager,
    private val progressRepository: ProgressRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(DevicesUiState())
    val state: StateFlow<DevicesUiState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var readJob: Job? = null

    init {
        refreshCapabilities()
        // Lo stato della sessione vive nel servizio: la UI lo segue invece di
        // dedurlo dal proprio ciclo di vita, altrimenti sembra cadere quando
        // si cambia schermata.
        viewModelScope.launch {
            sessionManager.state.collect { session ->
                _state.value = _state.value.copy(
                    sessionActive = session.active,
                    sessionStatus = session.status,
                    sessionDetail = session.detail,
                    sessionName = session.name,
                    connected = session.active && session.status != BleSessionStatus.CONNECTING,
                    connecting = session.status == BleSessionStatus.CONNECTING,
                    reading = session.status == BleSessionStatus.STREAMING
                )
            }
        }
        viewModelScope.launch {
            sessionManager.measurements.collect { m ->
                if (m == null) return@collect
                _state.value = _state.value.copy(
                    lastMeasurement = Measurement(m.kind, m.value, m.unit),
                    lastComposition = m.composition ?: _state.value.lastComposition
                )
                sessionManager.consumeMeasurement()
            }
        }
    }

    fun refreshCapabilities() {
        _state.value = _state.value.copy(
            bluetoothSupported = bleDeviceManager.isSupported(),
            permissionGranted = bleDeviceManager.hasPermission()
        )
    }

    fun requiredPermissions(): List<String> = bleDeviceManager.requiredPermissions()

    /**
     * Un rifiuto definitivo impedisce al sistema di mostrare ancora il dialogo.
     * Va distinto da "non ancora concesso", perche' richiede un percorso diverso.
     */
    fun permissionsPermanentlyDenied(): Boolean {
        val activity = context.findActivity() ?: return false
        return bleDeviceManager.requiredPermissions().any { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun onPermissionsBlocked() {
        _state.value = _state.value.copy(
            message = "Permesso Bluetooth negato in modo definitivo. Attivalo dalle " +
                "impostazioni dell'app, sezione Autorizzazioni."
        )
    }

    /** Avvia la scansione BLE reale e aggiorna la lista man mano che arrivano i dispositivi. */
    fun startScan() {
        refreshCapabilities()
        if (!_state.value.bluetoothSupported) {
            _state.value = _state.value.copy(
                message = "Bluetooth non disponibile o disattivato. Attivalo per cercare dispositivi."
            )
            return
        }
        if (!_state.value.permissionGranted) {
            _state.value = _state.value.copy(
                message = "Concedi i permessi Bluetooth per cercare dispositivi."
            )
            return
        }
        scanJob?.cancel()
        _state.value = _state.value.copy(scanning = true, devices = emptyList(), message = null)
        scanJob = viewModelScope.launch {
            bleDeviceManager.scan()
                .catch { _state.value = _state.value.copy(scanning = false, message = "Scansione interrotta.") }
                .collect { devices ->
                    _state.value = _state.value.copy(devices = devices)
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(scanning = false)
    }

    /**
     * Avvia il collegamento persistente: il servizio in foreground tiene vivo
     * il GATT anche a schermo spento, e le misurazioni arrivano in streaming.
     * Una singola pesata non basta piu': una fascia cardio deve restare
     * collegata durante tutto l'allenamento.
     */
    fun connect(device: DiscoveredDevice) {
        stopScan()
        readJob?.cancel()
        _state.value = _state.value.copy(
            connectedDevice = device,
            connecting = true,
            connected = false,
            reading = false,
            lastMeasurement = null,
            lastComposition = null,
            message = null
        )
        val started = BleSessionService.start(context, device.address, device.name, device.kind)
        if (!started) {
            _state.value = _state.value.copy(
                connecting = false,
                connected = false,
                message = "Android non consente di avviare il collegamento in background " +
                    "in questo momento. Apri l'app e riprova."
            )
        }
    }

    /** Chiude il collegamento e ferma il servizio. */
    fun disconnect() {
        BleSessionService.stop(context)
        sessionManager.stop()
        _state.value = _state.value.copy(
            connectedDevice = null,
            connected = false,
            connecting = false,
            reading = false,
            sessionActive = false,
            sessionStatus = BleSessionStatus.IDLE,
            sessionDetail = null
        )
    }

    /** Salva il peso letto dalla bilancia nei progressi, alla data odierna. */
    fun saveWeight(weightKg: Double) {
        viewModelScope.launch {
            if (weightKg !in 20.0..400.0) {
                _state.value = _state.value.copy(message = "Peso fuori intervallo plausibile (20-400 kg).")
                return@launch
            }
            progressRepository.addWeight(
                WeightRecord(
                    profileId = 1L,
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    weightKg = weightKg,
                    note = "Letto da ${_state.value.connectedDevice?.name ?: "dispositivo BLE"}"
                )
            )
            _state.value = _state.value.copy(
                message = "Peso ${"%.1f".format(weightKg)} kg registrato nei progressi."
            )
        }
    }

    fun clearMeasurement() {
        _state.value = _state.value.copy(lastMeasurement = null)
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
