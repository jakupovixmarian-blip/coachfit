package com.fitcoach.app.ui.settings

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fitcoach.app.BuildConfig
import com.fitcoach.app.diagnostics.CrashLog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.domain.model.CoachAuthorizationMode
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.navigation.Routes
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    navController: NavHostController,
    exportViewModel: ExportViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    coachAuthorizationViewModel: CoachAuthorizationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var crashLog by remember { mutableStateOf(CrashLog.read(context)) }
    val onShareCrashLog: () -> Unit = {
        crashLog?.let { text ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "FitCoach — errore registrato")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Condividi il log"))
        }
    }
    val onClearCrashLog: () -> Unit = {
        CrashLog.clear(context)
        crashLog = null
    }
    val exportState by exportViewModel.state.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val coachAuthorizationState by coachAuthorizationViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(backupViewModel::restoreBackup) }

    LaunchedEffect(exportState.message) {
        exportState.message?.let {
            snackbar.showSnackbar(it)
            exportViewModel.consumeMessage()
        }
    }
    LaunchedEffect(backupState.message) {
        backupState.message?.let {
            snackbar.showSnackbar(it)
            backupViewModel.consumeMessage()
        }
    }
    LaunchedEffect(coachAuthorizationState.message) {
        coachAuthorizationState.message?.let {
            snackbar.showSnackbar(it)
            coachAuthorizationViewModel.consumeMessage()
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
            Text("Impostazioni", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsRow(
                    icon = Icons.Filled.Person,
                    title = "Profilo",
                    subtitle = "Dati personali, obiettivo, attrezzatura",
                    onClick = { navController.navigate(Routes.PROFILE_EDIT) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Provider AI",
                    subtitle = "Configura il modello del coach",
                    onClick = { navController.navigate(Routes.AI_PROVIDERS) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifiche",
                    subtitle = "Pasti, allenamento, idratazione",
                    onClick = { navController.navigate(Routes.NOTIFICATION_SETTINGS) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.Bluetooth,
                    title = "Dispositivi",
                    subtitle = "Collega bilancia e cardiofrequenzimetro",
                    onClick = { navController.navigate(Routes.DEVICES) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.MenuBook,
                    title = "Fonti e versioni",
                    subtitle = "Basi scientifiche e storico dei piani",
                    onClick = { navController.navigate(Routes.KNOWLEDGE) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.Person,
                    title = "Anamnesi completa",
                    subtitle = "Modifica stile di vita, recupero, alimentazione, allenamento",
                    onClick = { navController.navigate(Routes.ANAMNESIS_EDIT) }
                )
            }

            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionHeader("Modalità Coach AI", coachAuthorizationState.mode.description)
                        Spacer(Modifier.height(8.dp))
                        CoachAuthorizationMode.entries.forEach { mode ->
                            OutlinedButton(
                                onClick = { coachAuthorizationViewModel.setMode(mode) },
                                enabled = mode != coachAuthorizationState.mode,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (mode == coachAuthorizationState.mode) "✓ ${mode.label}" else mode.label) }
                        }
                    }
                }
            }

            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionHeader("Esporta dati", "CSV / JSON dei tuoi risultati")
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportViewModel.exportWeights() },
                                enabled = !exportState.exporting,
                                modifier = Modifier.weight(1f)
                            ) { Text("Peso") }
                            OutlinedButton(
                                onClick = { exportViewModel.exportSessions() },
                                enabled = !exportState.exporting,
                                modifier = Modifier.weight(1f)
                            ) { Text("Sedute") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportViewModel.exportPerformance() },
                                enabled = !exportState.exporting,
                                modifier = Modifier.weight(1f)
                            ) { Text("Carichi") }
                            OutlinedButton(
                                onClick = { exportViewModel.exportCheckIns() },
                                enabled = !exportState.exporting,
                                modifier = Modifier.weight(1f)
                            ) { Text("Check-in") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { exportViewModel.exportAll() },
                            enabled = !exportState.exporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (exportState.exporting) "Esportazione..." else "Esporta tutto (JSON)")
                        }
                    }
                }
            }

            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionHeader("Diagnostica", "Ultimo errore registrato sul dispositivo")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (crashLog == null) "Nessun errore registrato."
                            else "Un errore e stato registrato. Condividi il file per l'analisi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onShareCrashLog,
                                enabled = crashLog != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("Condividi errore") }
                            OutlinedButton(
                                onClick = onClearCrashLog,
                                enabled = crashLog != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("Elimina") }
                        }
                    }
                }
            }

            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionHeader("Backup e ripristino", "Copia locale completa del database")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Il ripristino sostituisce i dati locali e riavvia l'app. Conserva il file in un luogo sicuro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { backupViewModel.createBackup() },
                                enabled = !backupState.working,
                                modifier = Modifier.weight(1f)
                            ) { Text(if (backupState.working) "Attendi..." else "Crea backup") }
                            OutlinedButton(
                                onClick = { restoreLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "*/*")) },
                                enabled = !backupState.working,
                                modifier = Modifier.weight(1f)
                            ) { Text("Ripristina") }
                        }
                    }
                }

                // Identifica la build installata: senza, due APK con lo stesso
                // nome e versioneCode diverso sono indistinguibili sull'app.
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionHeader(
                            "Versione",
                            "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Se qui leggi una versione diversa da quella che ti e' stata indicata, " +
                                "hai installato un APK vecchio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    SnackbarHost(snackbar)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Emerald)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}
