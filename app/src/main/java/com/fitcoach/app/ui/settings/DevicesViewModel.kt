package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.domain.device.BleDeviceManager
import com.fitcoach.app.domain.device.BodyComposition
import com.fitcoach.app.domain.device.ConnectionEvent
import com.fitcoach.app.domain.device.DeviceKind
import com.fitcoach.app.domain.device.DiscoveredDevice
import com.fitcoach.app.domain.device.Measurement
import com.fitcoach.app.domain.model.WeightRecord
import androidx.activity.ComponentActivity
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
    val message: String? = null
)

/**
 * Gestisce la sezione Dispositivi: scansione BLE reale, collegamento e lettura
 * di bilance e cardiofrequenzimetri. Il peso letto da una bilancia viene
 * registrato nei progressi solo dopo conferma esplicita dell'utente.
 */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val bleDeviceManager: BleDeviceManager,
    private val progressRepository: ProgressRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(DevicesUiState())
    val state: StateFlow<DevicesUiState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var readJob: Job? = null

    init {
        refreshCapabilities()
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

    /** Si collega al dispositivo scelto e ne legge una misurazione reale. */
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
        readJob = viewModelScope.launch {
            bleDeviceManager.connect(device.address, device.kind)
                .catch { e -> emit(ConnectionEvent.Failed(e.message ?: "Collegamento interrotto.")) }
                .collect { event ->
                    _state.value = when (event) {
                        is ConnectionEvent.Connected ->
                            _state.value.copy(connecting = false, connected = true, reading = true)
                        // Il nome arriva solo dopo la connessione: aggiorna la scheda
                        // invece di lasciare il MAC.
                        is ConnectionEvent.DeviceName -> _state.value.copy(
                            connectedDevice = _state.value.connectedDevice?.copy(name = event.name),
                            devices = _state.value.devices.map {
                                if (it.address == device.address) it.copy(name = event.name) else it
                            }
                        )
                        is ConnectionEvent.Measured -> _state.value.copy(
                            connecting = false, connected = true, reading = false,
                            lastMeasurement = event.value, message = null
                        )
                        is ConnectionEvent.Body -> _state.value.copy(
                            connecting = false, connected = true, reading = false,
                            lastComposition = event.composition,
                            lastMeasurement = Measurement(DeviceKind.SCALE, event.composition.weightKg, "kg"),
                            message = null
                        )
                        is ConnectionEvent.Failed -> _state.value.copy(
                            connecting = false, connected = false, reading = false,
                            message = event.message
                        )
                    }
                }
        }
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
