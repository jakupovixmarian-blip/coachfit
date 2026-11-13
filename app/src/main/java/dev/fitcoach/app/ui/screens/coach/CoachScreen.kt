package dev.fitcoach.app.ui.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.ui.components.ChatBubble
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.GhostButton
import dev.fitcoach.app.ui.components.GradientButton
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.NeonBadge
import dev.fitcoach.app.ui.components.QuickReplyChip
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.TypingDots
import dev.fitcoach.app.ui.theme.BgDeep
import dev.fitcoach.app.ui.theme.CardNavy
import dev.fitcoach.app.ui.theme.GradientViolet
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.NeonGreen
import dev.fitcoach.app.ui.theme.NeonMagenta
import dev.fitcoach.app.ui.theme.NeonOrange
import dev.fitcoach.app.ui.theme.NeonViolet
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow
import dev.fitcoach.app.ui.theme.TextMid

/**
 * AI Coach nel layout del mockup: intestazione con badge luminoso, card di stato
 * del profilo/programma, azioni rapide e conversazione con bolle neon.
 */
@Composable
fun CoachScreen(
    onOpenSettings: () -> Unit,
    viewModel: CoachViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        ScreenHeader("AI Coach", "Il tuo allenatore personale")
                    }
                    NeonBadge(
                        icon = Icons.Filled.AutoAwesome,
                        tint = NeonViolet,
                        size = 46.dp
                    )
                }
            }

            item {
                FitCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Stato attuale", Modifier.weight(1f))
                            if (!state.configured) {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(NeonOrange.copy(alpha = 0.14f))
                                        .border(1.dp, NeonOrange.copy(alpha = 0.4f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Da configurare",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        KeyValueRow(
                            "Profilo",
                            state.profile?.name?.ifBlank { "Compilato" } ?: "Da compilare",
                            valueColor = if (state.profile == null) NeonOrange else NeonCyan
                        )
                        KeyValueRow(
                            "Programma attivo",
                            state.program.firstOrNull()?.let { "Si, ${state.program.size} sedute" } ?: "Nessuno",
                            valueColor = if (state.program.isEmpty()) NeonOrange else NeonGreen
                        )
                        KeyValueRow(
                            "Piano alimentare",
                            state.plan?.name ?: "Nessuno",
                            valueColor = if (state.plan == null) NeonOrange else NeonGreen
                        )
                        if (!state.configured) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Aggiungi la chiave del provider AI nelle impostazioni per usare il coach.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid
                            )
                            Spacer(Modifier.height(8.dp))
                            GradientButton(
                                text = "Apri impostazioni AI",
                                onClick = onOpenSettings,
                                gradient = GradientViolet
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GhostButton(
                                text = "Nuovo programma",
                                icon = Icons.Filled.FitnessCenter,
                                tint = NeonCyan,
                                enabled = !state.sending,
                                onClick = { viewModel.regenerateProgram() },
                                modifier = Modifier.weight(1f)
                            )
                            GhostButton(
                                text = "Nuovo piano",
                                icon = Icons.Filled.Restaurant,
                                tint = NeonGreen,
                                onClick = { viewModel.regenerateMealPlan() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (state.messages.isEmpty()) {
                item {
                    FitCard {
                        Column {
                            SectionTitle("Da dove partiamo?")
                            QuickReplyChip(
                                text = "Costruisci una scheda per 4 giorni",
                                icon = Icons.Filled.FitnessCenter,
                                tint = NeonCyan
                            ) { viewModel.send("Costruisci una scheda per 4 giorni") }
                            Spacer(Modifier.height(8.dp))
                            QuickReplyChip(
                                text = "Controlla i miei progressi",
                                icon = Icons.Filled.TrendingUp,
                                tint = NeonGreen
                            ) { viewModel.send("Controlla i miei progressi") }
                            Spacer(Modifier.height(8.dp))
                            QuickReplyChip(
                                text = "Rivedi il piano alimentare",
                                icon = Icons.Filled.Restaurant,
                                tint = NeonOrange
                            ) { viewModel.send("Rivedi il piano alimentare") }
                        }
                    }
                }
            }

            items(state.messages, key = { it.id }) { message ->
                Column(Modifier.fillMaxWidth()) {
                    ChatBubble(text = message.content, fromUser = message.isUser)
                    if (message.actionSummary.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                message.actionSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }

            if (state.sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypingDots()
                        state.progress?.let {
                            Spacer(Modifier.width(10.dp))
                            Text(it, style = MaterialTheme.typography.labelSmall, color = TextLow)
                        }
                    }
                }
            }

            state.error?.let { error ->
                item {
                    FitCard {
                        Column {
                            Text(
                                "Errore",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonMagenta
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(error, style = MaterialTheme.typography.bodyMedium, color = TextMid)
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = { viewModel.dismissError() }) { Text("Chiudi") }
                        }
                    }
                }
            }

            if (state.appliedActions.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Azioni applicate")
                        state.appliedActions.forEach { action ->
                            Row(
                                Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    action,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMid
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(BgDeep)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Scrivi al coach...", color = TextLow) },
                shape = CircleShape,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardNavy,
                    unfocusedContainerColor = CardNavy,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = StrokeNavy,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NeonCyan
                )
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.configured && !state.sending && draft.isNotBlank()) {
                            androidx.compose.ui.graphics.Brush.linearGradient(GradientViolet)
                        } else {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(CardNavy, CardNavy)
                            )
                        }
                    )
                    .border(1.dp, StrokeNavy, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (state.sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = NeonViolet
                    )
                } else {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                viewModel.send(draft)
                                draft = ""
                            }
                        },
                        enabled = state.configured && draft.isNotBlank()
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Send,
                            contentDescription = "Invia",
                            tint = if (state.configured && draft.isNotBlank()) BgDeep else TextLow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (state.messages.isNotEmpty()) {
            TextButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            ) {
                Text("Svuota conversazione", style = MaterialTheme.typography.labelSmall, color = TextLow)
            }
        }
    }
}
