package dev.fitcoach.app.ui.screens.coach

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.ui.components.ChatBubble
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.SectionTitle

/**
 * Chat with the agent that manages the whole program. The user writes what they
 * want; the agent answers and can regenerate the program or the meal plan, change
 * rest times, add or remove exercises and update the profile.
 */
@Composable
fun CoachScreen(
    onOpenAiSettings: () -> Unit = {},
    viewModel: CoachViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Coach AI", style = MaterialTheme.typography.headlineSmall)

        FitCard {
            Column {
                SectionTitle("Stato del programma")
                KeyValueRow("Profilo", state.profile?.name?.ifBlank { "Compilato" } ?: "Da compilare")
                KeyValueRow("Programma attivo", state.program.firstOrNull()?.let { "Si, ${state.program.size} sedute" } ?: "Nessuno")
                KeyValueRow("Piano alimentare", state.plan?.name ?: "Nessuno")
                if (!state.configured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Configura un provider AI: l'agente gestisce scheda e piano tramite il modello.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = onOpenAiSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Configura provider AI")
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.regenerateProgram() },
                            modifier = Modifier.weight(1f),
                            enabled = !state.sending
                        ) { Text("Rigenera scheda") }
                        OutlinedButton(
                            onClick = { viewModel.regenerateMealPlan() },
                            modifier = Modifier.weight(1f),
                            enabled = !state.sending
                        ) { Text("Rigenera piano") }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Text(
                        "Scrivi al coach in linguaggio naturale. Esempi: \"ho solo manubri a casa, " +
                            "rifai la scheda\", \"aumenta il recupero della panca a 3 minuti\", " +
                            "\"genera un piano alimentare con 4 pasti\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.messages, key = { it.id }) { message ->
                Column {
                    ChatBubble(text = message.content, fromUser = message.isUser)
                    if (message.actionSummary.isNotBlank() && !message.isUser) {
                        Text(
                            "Azioni applicate: ${message.actionSummary}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }
            }
            if (state.sending) {
                item { CircularProgressIndicator() }
            }
        }

        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Row {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Messaggio per il coach") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            Button(
                onClick = { viewModel.send(draft); draft = "" },
                enabled = state.configured && !state.sending && draft.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Invia") }
        }
        TextButton(onClick = { viewModel.clearChat() }) { Text("Svuota conversazione") }
    }
}
