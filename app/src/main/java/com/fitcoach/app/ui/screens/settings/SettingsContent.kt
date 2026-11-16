package com.fitcoach.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.SegmentedControl
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* ============================================================================
 * Impostazioni — provider AI configurabili e indipendenti.
 * La chiave viene digitata qui, consegnata una volta a onSaveKey e salvata nel
 * keystore sicuro: non viene mai mostrata in chiaro né loggata.
 * ========================================================================== */

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onProviderSelect: (ProviderId) -> Unit,
    onProviderFieldChange: (ProviderUi) -> Unit,
    onSaveKey: (String) -> Unit,
    onTestConnection: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenKnowledge: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyDraft by remember { mutableStateOf("") }
    val provider = state.provider

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(text = "Impostazioni", style = FitTextStyles.display, color = TextPrimary)
            Text(
                text = "Provider AI, notifiche e dati",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        SectionHeader(title = "Provider AI", subtitle = "La generazione fallisce se il provider non risponde")

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProviderId.entries.forEach { option ->
                    val selected = provider.id == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(FitRadius.md))
                            .background(if (selected) BrandSoft else FitSurfaceVariant)
                            .clickable { onProviderSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = FitTextStyles.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Pill(text = "Attivo", color = Emerald)
                        }
                    }
                }
            }
        }

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Stato connessione",
                        style = FitTextStyles.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Pill(text = provider.statusLabel, color = statusColor(provider.status))
                }
                provider.lastCheckedLabel?.let { label ->
                    Text(text = label, style = FitTextStyles.caption, color = TextLow)
                }
                provider.errorMessage?.let { message ->
                    Text(text = message, style = FitTextStyles.caption, color = ErrorRed)
                }

                ProviderField(
                    label = "Base URL",
                    value = provider.baseUrl,
                    onValueChange = { onProviderFieldChange(provider.copy(baseUrl = it)) },
                )
                ProviderField(
                    label = "Modello",
                    value = provider.model,
                    onValueChange = { onProviderFieldChange(provider.copy(model = it)) },
                )
                ProviderField(
                    label = "API key",
                    value = keyDraft,
                    onValueChange = { keyDraft = it },
                    placeholder = if (provider.apiKeyMasked.isBlank()) "Nessuna chiave salvata" else provider.apiKeyMasked,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        text = "Salva chiave",
                        onClick = {
                            onSaveKey(keyDraft)
                            keyDraft = ""
                        },
                        enabled = keyDraft.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = if (provider.status == ProviderStatus.CHECKING) "Verifica…" else "Test",
                        onClick = onTestConnection,
                        enabled = state.canTestConnection && provider.status != ProviderStatus.CHECKING,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SectionHeader(title = "Notifiche", subtitle = "Promemoria pasti, allenamento e check-in")
        SectionCard {
            Column {
                DataRow(
                    label = "Notifiche attive",
                    value = if (state.notificationsEnabled) "Sì" else "No",
                    valueColor = if (state.notificationsEnabled) SuccessGreen else TextLow,
                )
                DataRow(
                    label = "Sveglia esatta",
                    value = if (state.exactAlarms) "Sì" else "No",
                    valueColor = if (state.exactAlarms) SuccessGreen else TextLow,
                )
            }
        }
        GhostButton(text = "Gestisci notifiche", onClick = onOpenNotifications)

        SectionHeader(title = "Unità di misura")
        SegmentedControl(
            options = UnitSystem.entries.map { it.label },
            selectedIndex = UnitSystem.entries.indexOf(state.unitSystem),
            onSelect = { onUnitSystemChange(UnitSystem.entries[it]) },
        )

        SectionHeader(title = "Account e dati")
        GhostButton(text = "Modifica profilo", onClick = onOpenProfile)
        Spacer(modifier = Modifier.height(4.dp))
        GhostButton(text = "Fonti scientifiche", onClick = onOpenKnowledge)
        Spacer(modifier = Modifier.height(4.dp))
        GhostButton(
            text = if (state.isBackingUp) "Backup in corso…" else "Esporta backup",
            onClick = onBackup,
            enabled = !state.isBackingUp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        GhostButton(text = "Ripristina da backup", onClick = onRestore)

        state.message?.let { message ->
            Text(text = message, style = FitTextStyles.caption, color = TextSecondary)
        }

        SectionCard {
            Column {
                DataRow(label = "Versione", value = state.versionLabel)
                DataRow(label = "Schema database", value = state.schemaLabel)
                DataRow(label = "Tema", value = "Dark (fisso)")
            }
        }
    }
}

private fun statusColor(status: ProviderStatus): androidx.compose.ui.graphics.Color = when (status) {
    ProviderStatus.OK -> SuccessGreen
    ProviderStatus.ERROR -> ErrorRed
    ProviderStatus.CHECKING -> WarningAmber
    ProviderStatus.UNCONFIGURED -> TextLow
}

@Composable
private fun ProviderField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, style = FitTextStyles.caption) },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(text = placeholder, style = FitTextStyles.caption, color = TextLow)
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(FitRadius.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = FitSurfaceVariant,
            unfocusedContainerColor = FitSurfaceVariant,
            focusedBorderColor = Emerald,
            unfocusedBorderColor = FitOutline,
            cursorColor = Emerald,
            focusedLabelColor = Emerald,
            unfocusedLabelColor = TextLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
