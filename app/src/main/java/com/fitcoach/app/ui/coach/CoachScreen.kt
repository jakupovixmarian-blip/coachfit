package com.fitcoach.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.model.ProgramChange
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.navigation.Routes
import com.fitcoach.app.ui.components.OnResumeRefresh
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.TextSecondary

@Composable
fun CoachScreen(
    navController: NavHostController,
    viewModel: CoachViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnResumeRefresh { viewModel.load() }
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // La lista deve arrivare fino in fondo: la card di conferma e' registrata
    // DOPO i messaggi, quindi scorrere all'ultimo messaggio la lasciava fuori
    // dallo schermo. L'utente leggeva la risposta del coach e non vedeva il
    // pulsante "Approva e applica", concludendo che la modifica fosse gia attiva.
    LaunchedEffect(state.messages.size, state.pendingChange?.id) {
        val extra = if (state.pendingChange != null) 1 else 0
        val last = state.messages.size - 1 + extra
        if (last >= 0) listState.animateScrollToItem(last)
    }

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Coach AI", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(state.providerLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Row {
                IconButton(onClick = { navController.navigate(Routes.AI_PROVIDERS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Provider AI", tint = TextSecondary)
                }
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Cancella chat", tint = TextSecondary)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    SectionCard {
                        Column {
                            Text("Come posso aiutarti?", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Esempi: \"Non ho il pollo, cosa posso mangiare?\", " +
                                    "\"Ho migliorato la panca di due ripetizioni\", " +
                                    "\"Il peso è fermo da due settimane\", " +
                                    "\"Domani non posso allenarmi, sposta la seduta\".",
                                style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(state.messages.size) { index ->
                val msg = state.messages[index]
                if (msg.role == ChatRole.SYSTEM) return@items
                ChatBubble(
                    content = msg.content,
                    isUser = msg.role == ChatRole.USER,
                    isError = msg.isError
                )
            }

            state.pendingChange?.let { change ->
                item {
                    ProposalCard(
                        change = change,
                        applying = state.applying,
                        progress = state.applyingProgress,
                        onApprove = viewModel::approvePending,
                        onReject = viewModel::rejectPending
                    )
                }
            }

            if (state.appliedChanges.isNotEmpty()) {
                item {
                    SectionCard {
                        Column {
                            Text("Modifiche applicate", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("Puoi annullare una modifica e ripristinare il programma precedente.",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(Modifier.height(8.dp))
                            state.appliedChanges.forEach { change ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(change.title, style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text(change.proposedChange, style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary)
                                    }
                                    TextButton(onClick = { viewModel.revert(change.id) }) { Text("Annulla") }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::updateInput,
                placeholder = { Text("Scrivi al coach...") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.send() },
                enabled = !state.sending && state.input.isNotBlank()
            ) {
                if (state.sending) {
                    CircularProgressIndicator(modifier = Modifier.width(20.dp), color = Emerald)
                } else {
                    Icon(Icons.Filled.Send, contentDescription = "Invia", tint = Emerald)
                }
            }
        }

        if (state.canRetry) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.retry() },
                    enabled = !state.sending
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Riprova")
                }
            }
        }
    }

    SnackbarHost(snackbar)
}

@Composable
private fun ChatBubble(content: String, isUser: Boolean, isError: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        isError -> ErrorRed.copy(alpha = 0.15f)
                        isUser -> Emerald.copy(alpha = 0.16f)
                        else -> FitSurfaceVariant
                    }
                )
                .padding(12.dp)
        ) {
            Text(
                text = if (isUser) "Tu" else "Coach",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) Emerald else TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProposalCard(
    change: ProgramChange,
    applying: Boolean,
    progress: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    SectionCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Proposta di modifica", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Pill(change.domain.name, Emerald)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Non e' ancora attiva: confermala qui sotto per applicarla al programma.",
                style = MaterialTheme.typography.labelMedium,
                color = Emerald
            )
            Spacer(Modifier.height(8.dp))
            ProposalRow("Situazione", change.situation)
            ProposalRow("Problema", change.problem)
            ProposalRow("Dati usati", change.dataUsed)
            ProposalRow("Modifica proposta", change.proposedChange)
            ProposalRow("Motivazione", change.rationale)
            ProposalRow("Risultato atteso", change.expectedOutcome)
            ProposalRow("Controindicazioni", change.contraindications)
            Spacer(Modifier.height(12.dp))
            if (applying) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(18.dp), color = Emerald, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        progress ?: "Applico la modifica...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "La rigenerazione del programma puo richiedere fino a qualche minuto: " +
                        "puoi lasciare questa schermata, il lavoro continua.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onApprove, enabled = !applying) { Text("Approva e applica") }
                TextButton(onClick = onReject, enabled = !applying) { Text("Rifiuta") }
            }
        }
    }
}

@Composable
private fun ProposalRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
