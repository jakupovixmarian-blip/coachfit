package dev.fitcoach.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.ui.components.ChipSelector
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.SectionTitle

@Composable
fun SettingsScreen(
    onOpenAi: () -> Unit,
    onOpenKnowledge: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportData(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importData(context, it) } }

    val profile = state.profile

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader("Impostazioni", "Dati, backup e configurazione AI")

        FitCard {
            Column {
                SectionTitle("Profilo")
                if (profile == null) {
                    Text(
                        "Nessun profilo salvato. Compila l'anamnesi per attivare i calcoli.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LabeledField("Nome", profile.name, { v -> viewModel.updateProfile { it.copy(name = v) } })
                    Spacer(Modifier.height(6.dp))
                    NumberField("Peso (kg)", profile.weightKg?.toString().orEmpty(), { v ->
                        viewModel.updateProfile { it.copy(weightKg = v.toDoubleOrNull()) }
                    })
                    Spacer(Modifier.height(6.dp))
                    NumberField("Altezza (cm)", profile.heightCm?.toString().orEmpty(), { v ->
                        viewModel.updateProfile { it.copy(heightCm = v.toDoubleOrNull()) }
                    })
                    Spacer(Modifier.height(6.dp))
                    NumberField("Eta", profile.age?.toString().orEmpty(), { v ->
                        viewModel.updateProfile { it.copy(age = v.toIntOrNull()) }
                    }, allowDecimal = false)
                    Spacer(Modifier.height(10.dp))
                    KeyValueRow("Metabolismo basale", profile.bmr()?.let { Format.kcal(it) } ?: "Dati insufficienti")
                    KeyValueRow("Fabbisogno stimato", profile.maintenanceCalories()?.let { Format.kcal(it) } ?: "-")
                    KeyValueRow("Calorie obiettivo", profile.targetCalories()?.let { Format.kcal(it) } ?: "-")
                    KeyValueRow("Proteine", profile.proteinTarget()?.let { Format.grams(it) } ?: "-")
                    KeyValueRow("Carboidrati", profile.carbsTarget()?.let { Format.grams(it) } ?: "-")
                    KeyValueRow("Grassi", profile.fatTarget()?.let { Format.grams(it) } ?: "-")
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Unita di misura")
                profile?.let { p ->
                    ChipSelector(
                        label = "Sistema",
                        options = dev.fitcoach.app.domain.model.UnitSystem.entries.toList(),
                        selected = p.unitSystem,
                        optionLabel = { if (it.name == "METRIC") "Metrico" else "Imperiale" },
                        onSelect = { unit -> viewModel.updateProfile { it.copy(unitSystem = unit) } }
                    )
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Intelligenza artificiale")
                Text(
                    "Configura provider, modello e chiave. La chiave e protetta con Android Keystore e non lascia il dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenAi, modifier = Modifier.fillMaxWidth()) { Text("Apri impostazioni AI") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenKnowledge, modifier = Modifier.fillMaxWidth()) {
                    Text("Fonti e note dell'agente")
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Dati")
                Text(
                    "Il backup esporta tutti i dati reali salvati: profilo, misurazioni, allenamenti, schede, alimenti, piani, diario e spesa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { exportLauncher.launch("fitcoach-backup.json") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.busy
                    ) { Text("Esporta") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.busy
                    ) { Text("Importa") }
                }
                state.message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                state.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Privacy")
                Text(
                    "Tutti i dati restano sul dispositivo. Le uniche connessioni di rete sono le chiamate al provider AI che configuri e la ricerca alimenti su Open Food Facts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
