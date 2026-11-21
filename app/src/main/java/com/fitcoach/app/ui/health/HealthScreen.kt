package com.fitcoach.app.ui.health

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.data.health.DailyHealthSnapshot
import com.fitcoach.app.data.health.HealthConnectReader
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.navigation.Routes
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Sezione Salute: punto d'ingresso per Dispositivi, Report giornaliero,
 * Ricette e Crea ricetta. Prima queste funzioni erano raggiungibili solo da
 * percorsi nascosti (Impostazioni, icona dentro Alimentazione).
 */
@Composable
fun HealthScreen(
    navController: NavHostController,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // I permessi di Health Connect NON sono permessi runtime Android: vanno
    // chiesti con il contratto della libreria, che apre la sua schermata.
    // Con RequestMultiplePermissions la richiesta torna vuota e il report
    // resta per sempre senza dati.
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { viewModel.onPermissionsResult() }

    /**
     * Se la schermata dei permessi non si apre (provider assente o non
     * risolvibile) non deve succedere nulla di silenzioso: si apre Health
     * Connect dalle impostazioni di sistema, dove l'accesso si concede a mano.
     */
    fun requestPermissions() {
        val launched = runCatching { permissionLauncher.launch(viewModel.requiredPermissions) }
        if (launched.isFailure) {
            val opened = runCatching {
                context.startActivity(
                    Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                )
            }
            if (opened.isFailure) {
                val fallback = runCatching {
                    context.startActivity(
                        Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
                if (fallback.isFailure) viewModel.onPermissionsResult()
            }
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
            Column {
                Text("Salute", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Misure, dispositivi e ricette", style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SectionCard {
                    Column {
                        Text("Collegamenti", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(10.dp))
                        HealthEntry(
                            icon = Icons.Filled.Bluetooth,
                            title = "Dispositivi",
                            subtitle = "Bilancia, cardiofrequenzimetro, Health Connect"
                        ) { navController.navigate(Routes.DEVICES) }
                        HealthEntry(
                            icon = Icons.Filled.RestaurantMenu,
                            title = "Ricette",
                            subtitle = "Dal piano alimentare"
                        ) { navController.navigate(Routes.RECIPES) }
                        HealthEntry(
                            icon = Icons.Filled.MenuBook,
                            title = "Crea ricetta",
                            subtitle = "Componi un piatto dagli alimenti del pasto"
                        ) { navController.navigate(Routes.RECIPES_CREATE) }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        Text("Report di oggi", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text("Fonti: Health Connect e bilancia collegata.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.height(10.dp))

                        if (state.loading) {
                            LoadingBox(Modifier.fillMaxWidth().height(80.dp))
                        } else {
                            when (state.availability) {
                                HealthConnectReader.Availability.NOT_SUPPORTED -> {
                                    Text(
                                        "Health Connect non e' disponibile su questo dispositivo. " +
                                            "Su Android 14 e successivi e' integrato nel sistema; " +
                                            "sulle versioni precedenti va installato dal Play Store.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                HealthConnectReader.Availability.UPDATE_REQUIRED -> {
                                    Text(
                                        "Health Connect richiede un aggiornamento dall'app " +
                                            "Play Store per funzionare.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                HealthConnectReader.Availability.AVAILABLE -> {
                                    if (!state.permissionsGranted) {
                                        Text(
                                            "Per leggere passi, sonno e frequenza cardiaca serve " +
                                                "il tuo permesso. Senza permesso il report resta vuoto: " +
                                                "non mostriamo numeri inventati.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Button(onClick = { requestPermissions() }) {
                                            Text("Concedi accesso")
                                        }
                                    } else {
                                        HealthMetrics(state.snapshot, state.scaleWeightKg,
                                            state.scaleBodyFat)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.permissionsGranted) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Aggiorna report") }
                }
            }

            state.snapshot?.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
                item {
                    SectionCard {
                        Column {
                            Text("Note sui dati", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            notes.forEach { n ->
                                Text("• $n", style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary, modifier = Modifier.padding(vertical = 1.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Emerald)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        OutlinedButton(onClick = onClick) { Text("Apri") }
    }
}

@Composable
private fun HealthMetrics(
    snapshot: DailyHealthSnapshot?,
    scaleWeightKg: Double?,
    scaleBodyFat: Double?
) {
    Column {
        MetricRow(Icons.Filled.DirectionsWalk, "Passi",
            snapshot?.steps?.toString() ?: "non disponibili", snapshot?.steps != null)
        MetricRow(Icons.Filled.LocalFireDepartment, "Calorie attive",
            snapshot?.activeKcal?.let { "%.0f kcal".format(it) } ?: "non disponibili",
            snapshot?.activeKcal != null)
        MetricRow(Icons.Filled.Bedtime, "Sonno",
            snapshot?.sleepMinutes?.let { formatMinutes(it) } ?: "non disponibili",
            snapshot?.sleepMinutes != null)
        MetricRow(Icons.Filled.FavoriteBorder, "FC a riposo",
            snapshot?.restingHeartRate?.let { "$it bpm" } ?: "non disponibili",
            snapshot?.restingHeartRate != null)
        MetricRow(Icons.Filled.MonitorHeart, "HRV",
            snapshot?.hrvMs?.let { "%.0f ms".format(it) } ?: "non disponibili",
            snapshot?.hrvMs != null)
        MetricRow(Icons.Filled.Scale, "Peso (bilancia)",
            scaleWeightKg?.let { "%.1f kg".format(it) } ?: "nessuna misura",
            scaleWeightKg != null)
        MetricRow(Icons.Filled.Scale, "Massa grassa (bilancia)",
            scaleBodyFat?.let { "%.1f %%".format(it) } ?: "nessuna misura",
            scaleBodyFat != null)
    }
}

@Composable
private fun MetricRow(icon: ImageVector, label: String, value: String, available: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null,
            tint = if (available) Emerald else TextSecondary)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = if (available) MaterialTheme.colorScheme.onSurface else TextSecondary)
    }
}

private fun formatMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
