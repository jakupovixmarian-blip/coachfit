package com.fitcoach.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitSurfaceInteractive
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.OnBrand
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Notifiche programmate. Ogni voce corrisponde a un promemoria reale gestito da
 * WorkManager/AlarmManager; qui si cambia solo lo stato, l'orario arriva dal piano.
 */
data class NotificationsUiState(
    val permissionGranted: Boolean = true,
    val exactAlarmsAvailable: Boolean = true,
    val items: List<NotificationToggleUi> = emptyList(),
)

data class NotificationToggleUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val enabled: Boolean,
)

@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    onToggle: (String, Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text(text = "Notifiche", style = FitTextStyles.display, color = TextPrimary)
            Text(
                text = "Promemoria generati dal piano attivo",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        if (!state.permissionGranted) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Permesso notifiche non concesso",
                        style = FitTextStyles.bodyStrong,
                        color = ErrorRed,
                    )
                    Text(
                        text = "Senza permesso i promemoria non vengono mostrati dal sistema.",
                        style = FitTextStyles.caption,
                        color = TextSecondary,
                    )
                    GhostButton(text = "Apri impostazioni di sistema", onClick = onOpenSystemSettings)
                }
            }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sveglia esatta",
                    style = FitTextStyles.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Pill(
                    text = if (state.exactAlarmsAvailable) "Disponibile" else "Limitata",
                    color = if (state.exactAlarmsAvailable) SuccessGreen else ErrorRed,
                )
            }
        }

        SectionHeader(title = "Promemoria", subtitle = "Orari presi dal piano alimentare e dalla scheda")

        state.items.forEach { item ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.title,
                                style = FitTextStyles.bodyStrong,
                                color = TextPrimary,
                            )
                            Text(
                                text = "  ${item.time}",
                                style = FitTextStyles.numericS,
                                color = Emerald,
                            )
                        }
                        Text(
                            text = item.subtitle,
                            style = FitTextStyles.caption,
                            color = TextLow,
                        )
                    }
                    Switch(
                        checked = item.enabled,
                        onCheckedChange = { onToggle(item.id, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnBrand,
                            checkedTrackColor = Emerald,
                            uncheckedThumbColor = TextLow,
                            uncheckedTrackColor = FitSurfaceInteractive,
                            uncheckedBorderColor = FitOutline,
                        ),
                    )
                }
            }
        }
    }
}
