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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.TextSecondary

@Composable
fun AiProvidersScreen(
    navController: NavHostController,
    /** Primo avvio: nessun "indietro", e in fondo un pulsante che porta all'anamnesi. */
    setupMode: Boolean = false,
    onSetupDone: () -> Unit = {},
    viewModel: AiProvidersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!setupMode) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = TextSecondary)
                }
            }
            Text("Provider AI", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground)
        }

        if (setupMode) {
            Text(
                "Prima di compilare l'anamnesi configura il Coach AI: il piano alimentare e la " +
                    "scheda vengono costruiti dal modello che scegli qui. Puoi cambiare provider " +
                    "in qualsiasi momento dalle Impostazioni.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SectionCard {
                    Column {
                        SectionHeader("Provider", "API compatibili OpenAI")
                        Spacer(Modifier.height(10.dp))
                        AiProvider.entries.forEach { p ->
                            FilterChip(
                                selected = state.provider == p,
                                onClick = { viewModel.selectProvider(p) },
                                label = { Text(p.label) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        SectionHeader("Configurazione")
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.baseUrl,
                            onValueChange = viewModel::updateBaseUrl,
                            label = { Text("Base URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.model,
                            onValueChange = viewModel::updateModel,
                            label = { Text("Modello") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.apiKeyInput,
                            onValueChange = viewModel::updateApiKey,
                            label = { Text(if (state.hasStoredKey) "API Key (già salvata)" else "API Key") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.hasStoredKey) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Chiave salvata in modo cifrato.",
                                    style = MaterialTheme.typography.bodySmall, color = Emerald,
                                    modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearKey() }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Rimuovi chiave",
                                        tint = ErrorRed)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.useBackendProxy,
                                onCheckedChange = viewModel::updateUseProxy)
                            Text("Usa backend proxy (chiave sul server, non sul dispositivo)",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        if (state.useBackendProxy) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.backendBaseUrl,
                                onValueChange = viewModel::updateBackendUrl,
                                label = { Text("URL backend proxy") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        SectionHeader("Sicurezza delle chiavi")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Le chiavi API sono cifrate con EncryptedSharedPreferences (AES-256) e non " +
                                "vengono mai incluse nell'APK. Per la produzione, abilita il backend proxy " +
                                "così la chiave resta sul server e non sul dispositivo.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { viewModel.save() }, modifier = Modifier.weight(1f)) {
                        Text("Salva")
                    }
                    OutlinedButton(
                        onClick = { viewModel.testConnection() },
                        enabled = !state.testing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.testing) "Test..." else "Testa connessione")
                    }
                }
            }

            state.testStatus?.let { status ->
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (state.testOk == true) "✓ " else "✕ ",
                                color = if (state.testOk == true) Emerald else ErrorRed,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            if (setupMode) {
                item {
                    SectionCard {
                        Column {
                            SectionHeader("Conferma")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Salva la configurazione e verifica la connessione. Il test non è " +
                                    "obbligatorio: puoi proseguire e provarlo più tardi.",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            var navigating by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    if (!navigating) {
                                        navigating = true
                                        viewModel.save(onSaved = onSetupDone)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Salva e continua") }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onSetupDone, modifier = Modifier.fillMaxWidth()) {
                                Text("Configura più tardi")
                            }
                        }
                    }
                }
            }
        }
    }

    SnackbarHost(snackbar)
}
