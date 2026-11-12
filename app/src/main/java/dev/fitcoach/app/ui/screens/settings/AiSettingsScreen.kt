package dev.fitcoach.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.domain.model.AiProvider
import dev.fitcoach.app.ui.components.ChipSelector
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(viewModel: AiSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni AI") },
                navigationIcon = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FitCard {
                    Column {
                        SectionTitle("Provider")
                        ChipSelector(
                            label = "Provider",
                            options = AiProvider.entries.toList(),
                            selected = state.config.provider,
                            optionLabel = { it.displayName },
                            onSelect = { viewModel.selectProvider(it) }
                        )
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Base URL", state.config.baseUrl, { viewModel.updateBaseUrl(it) })
                        Spacer(Modifier.height(6.dp))
                        LabeledField("Modello", state.config.modelId, { viewModel.updateModel(it) })
                        Spacer(Modifier.height(6.dp))
                        KeyValueRow("Endpoint modelli", state.config.effectiveBaseUrl() + "/models")
                    }
                }
            }

            item {
                FitCard {
                    Column {
                        SectionTitle("Chiave API")
                        Text(
                            if (state.hasStoredKey) "Chiave presente per ${state.config.provider.displayName}."
                            else "Nessuna chiave salvata per ${state.config.provider.displayName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LabeledField(
                            label = "Nuova chiave",
                            value = state.apiKeyInput,
                            onValueChange = { viewModel.updateApiKeyInput(it) },
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.saveApiKey() }, modifier = Modifier.weight(1f)) {
                                Text("Salva chiave")
                            }
                            OutlinedButton(onClick = { viewModel.clearApiKey() }, modifier = Modifier.weight(1f)) {
                                Text("Rimuovi")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.testConnection() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.testing
                        ) { Text(if (state.testing) "Verifica in corso..." else "Verifica connessione") }
                        if (state.testing) {
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator()
                        }
                        state.testResult?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (state.availableModels.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Modelli disponibili", style = MaterialTheme.typography.titleMedium)
                            state.availableModels.take(12).forEach { model ->
                                TextButton(onClick = { viewModel.updateModel(model) }) {
                                    Text(model, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                FitCard {
                    Column {
                        SectionTitle("Parametri di generazione")
                        NumberField(
                            "Temperature",
                            state.config.temperature.toString(),
                            { it.toDoubleOrNull()?.let { v -> viewModel.updateTemperature(v) } }
                        )
                        Spacer(Modifier.height(6.dp))
                        NumberField(
                            "Top P",
                            state.config.topP.toString(),
                            { it.toDoubleOrNull()?.let { v -> viewModel.updateTopP(v) } }
                        )
                        Spacer(Modifier.height(6.dp))
                        NumberField(
                            "Max token risposta",
                            state.config.maxOutputTokens.toString(),
                            { it.toIntOrNull()?.let { v -> viewModel.updateMaxTokens(v) } },
                            allowDecimal = false
                        )
                        Spacer(Modifier.height(6.dp))
                        NumberField(
                            "Timeout (secondi)",
                            state.config.timeoutSeconds.toString(),
                            { it.toIntOrNull()?.let { v -> viewModel.updateTimeout(v) } },
                            allowDecimal = false
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Forza risposta JSON", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = state.config.useJsonMode,
                                onCheckedChange = { viewModel.updateJsonMode(it) }
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invia foto al modello", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = state.config.sendPhotosToAi,
                                onCheckedChange = { viewModel.updateSendPhotos(it) }
                            )
                        }
                        Text(
                            "Di default le foto non vengono inviate: abilitalo solo se il provider scelto le supporta.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                FitCard {
                    Column {
                        SectionTitle("Come funziona")
                        Text(
                            "1. Scegli il provider e incolla la chiave. 2. Verifica la connessione: l'app elenca i modelli realmente disponibili. 3. Genera il piano dal Coach. 4. Controlla i numeri: la proposta va approvata prima di essere applicata.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
