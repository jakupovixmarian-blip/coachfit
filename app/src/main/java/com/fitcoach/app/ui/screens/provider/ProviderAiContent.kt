package com.fitcoach.app.ui.screens.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.screens.settings.ProviderId
import com.fitcoach.app.ui.screens.settings.ProviderStatus
import com.fitcoach.app.ui.theme.BrandBorder
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
 * Provider AI (design/screens.md §12) — schermata di secondo livello.
 * La chiave viene digitata qui, consegnata una volta a onSaveKey e salvata nel
 * keystore sicuro: non viene mai mostrata in chiaro né scritta nei log.
 * La generazione dei programmi fallisce con errore esplicito se il provider non
 * risponde: nessun piano alternativo deterministico.
 * ========================================================================== */

@Composable
fun ProviderAiContent(
    state: ProviderAiUiState,
    onBack: () -> Unit,
    onProviderSelect: (ProviderId) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSaveKey: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyDraft by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    val provider = state.provider

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(FitRadius.sm))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(text = "‹", style = FitTextStyles.bodyStrong, color = TextSecondary)
                }
                Text(text = "Impostazioni", style = FitTextStyles.label, color = TextLow)
            }
            Text(text = "Provider AI", style = FitTextStyles.display, color = TextPrimary)
        }

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(providerStatusColor(provider.status)),
                    )
                    Text(
                        text = provider.statusLabel,
                        style = FitTextStyles.bodyStrong,
                        color = TextPrimary,
                    )
                }
                Text(
                    text = "modello · ${provider.model}",
                    style = FitTextStyles.numericS,
                    color = TextSecondary,
                )
                provider.errorMessage?.let { message ->
                    Text(text = message, style = FitTextStyles.caption, color = ErrorRed)
                }
                provider.lastCheckedLabel?.let { label ->
                    Text(text = label, style = FitTextStyles.caption, color = TextLow)
                }
                if (state.needsKey) {
                    Text(
                        text = "Nessuna chiave salvata: la generazione non può partire.",
                        style = FitTextStyles.caption,
                        color = WarningAmber,
                    )
                }
            }
        }

        SectionHeader(title = "Provider", subtitle = "Endpoint compatibili OpenAI")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.options.forEach { option ->
                val selected = provider.id == option.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FitRadius.md))
                        .background(if (selected) BrandSoft else FitSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (selected) BrandBorder else FitOutline,
                            shape = RoundedCornerShape(FitRadius.md),
                        )
                        .clickable { onProviderSelect(option.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = option.name,
                            style = FitTextStyles.bodySmall,
                            color = TextPrimary,
                        )
                        if (option.note.isNotBlank()) {
                            Text(
                                text = option.note,
                                style = FitTextStyles.caption,
                                color = TextLow,
                            )
                        }
                    }
                    if (selected) Pill(text = "Attivo", color = Emerald)
                }
            }
        }

        SectionHeader(title = "Configurazione")

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProviderTextField(
                    label = "Base URL",
                    value = provider.baseUrl,
                    onValueChange = onBaseUrlChange,
                )
                ProviderTextField(
                    label = "Modello",
                    value = provider.model,
                    onValueChange = onModelChange,
                )
                ProviderTextField(
                    label = "API key",
                    value = keyDraft,
                    onValueChange = { keyDraft = it },
                    placeholder = if (state.needsKey) "Nessuna chiave salvata" else provider.apiKeyMasked,
                    secret = true,
                    secretVisible = keyVisible,
                    onToggleSecret = { keyVisible = !keyVisible },
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
                        text = if (state.isTesting) "Testo…" else "Testa",
                        onClick = onTestConnection,
                        enabled = state.canTest && !state.isTesting,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            text = "La chiave non viene mai scritta nei log né nel piano: resta nel keystore sicuro del dispositivo.",
            style = FitTextStyles.bodySmall,
            color = TextSecondary,
        )

        state.message?.let { message ->
            Text(text = message, style = FitTextStyles.caption, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(2.dp))
        PrimaryButton(
            text = if (state.isDirty) "Salva" else "Salvato",
            onClick = onSave,
            enabled = state.canSave,
        )
    }
}

private fun providerStatusColor(status: ProviderStatus): Color = when (status) {
    ProviderStatus.OK -> SuccessGreen
    ProviderStatus.ERROR -> ErrorRed
    ProviderStatus.CHECKING -> WarningAmber
    ProviderStatus.UNCONFIGURED -> TextLow
}

@Composable
private fun ProviderTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    secret: Boolean = false,
    secretVisible: Boolean = false,
    onToggleSecret: () -> Unit = {},
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
        visualTransformation = if (secret && !secretVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = {
            if (secret) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(FitRadius.xs))
                        .clickable { onToggleSecret() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (secretVisible) "nascondi" else "mostra",
                        style = FitTextStyles.caption,
                        color = TextLow,
                    )
                }
            }
        },
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
