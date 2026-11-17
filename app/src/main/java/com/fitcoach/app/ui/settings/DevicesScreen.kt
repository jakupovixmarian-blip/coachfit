package com.fitcoach.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.device.DeviceKind
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Sezione Dispositivi: cerca bilance e cardiofrequenzimetri via Bluetooth Low
 * Energy, si collega e legge la misurazione reale. Un peso letto da una bilancia
 * puo essere registrato nei progressi con un tocco.
 */
@Composable
fun DevicesScreen(
    navController: NavHostController,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        viewModel.refreshCapabilities()
        if (granted.values.all { it }) viewModel.startScan()
        else viewModel.startScan() // startScan gestisce l'assenza di permessi con un messaggio.
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = TextSecondary)
            }
            Text(
                "Dispositivi",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SectionCard {
                    Column {
                        SectionHeader("Collega un dispositivo", "Bilancia o cardiofrequenzimetro BLE")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "FitCoach legge direttamente i dati dal dispositivo: peso per le bilance, " +
                                "battiti per le fasce cardio. Nessun valore viene inventato.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        if (!state.bluetoothSupported) {
                            Text(
                                "Bluetooth non disponibile o disattivato su questo dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (state.permissionGranted) viewModel.startScan()
                                    else permissionLauncher.launch(viewModel.requiredPermissions().toTypedArray())
                                },
                                enabled = state.bluetoothSupported && !state.scanning
                            ) {
                                Icon(
                                    if (state.scanning) Icons.Filled.BluetoothSearching else Icons.Filled.Bluetooth,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (state.scanning) "Ricerca..." else "Cerca dispositivi")
                            }
                            if (state.scanning) {
                                OutlinedButton(onClick = viewModel::stopScan) { Text("Ferma") }
                            }
                        }
                    }
                }
            }

            if (state.scanning) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(20.dp), color = Emerald)
                        Spacer(Modifier.width(10.dp))
                        Text("Scansione in corso...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            if (state.devices.isEmpty() && !state.scanning && state.bluetoothSupported) {
                item {
                    Text(
                        "Nessun dispositivo trovato. Avvicina la bilancia o la fascia e riprova.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
            }

            items(state.devices, key = { it.address }) { device ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (device.kind == DeviceKind.SCALE) Icons.Filled.MonitorWeight
                            else Icons.Filled.Bluetooth,
                            contentDescription = null,
                            tint = Emerald
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                device.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${device.kind.label} · ${device.address} · RSSI ${device.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary
                            )
                        }
                        Button(
                            onClick = { viewModel.connect(device) },
                            enabled = !state.reading
                        ) { Text("Collega") }
                    }
                }
            }

            state.connectedDevice?.let { device ->
                item {
                    SectionCard {
                        Column {
                            SectionHeader("Dispositivo collegato", device.name)
                            Spacer(Modifier.height(8.dp))
                            if (state.reading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.height(20.dp), color = Emerald)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Lettura in corso...", style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary)
                                }
                            }
                            state.lastMeasurement?.let { m ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Emerald)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${"%.1f".format(m.value)} ${m.unit}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Emerald
                                    )
                                }
                                if (m.kind == DeviceKind.SCALE) {
                                    Spacer(Modifier.height(10.dp))
                                    Button(onClick = { viewModel.saveWeight(m.value) }) {
                                        Text("Registra nei progressi")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(snackbar)
    }
}
