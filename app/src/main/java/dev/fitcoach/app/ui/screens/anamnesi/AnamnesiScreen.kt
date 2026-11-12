package dev.fitcoach.app.ui.screens.anamnesi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.ui.components.ChatBubble
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.SectionTitle

@Composable
fun AnamnesiScreen(
    onOpenAiSettings: () -> Unit,
    onFinished: () -> Unit,
    viewModel: AnamnesiViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    // Coming back from the AI settings must start the intake without a manual tap.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshConfiguration() }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(topBar = {}) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ScreenHeader("Anamnesi con l'agente", "Rispondi alle domande: costruisco il tuo profilo")
            Spacer(Modifier.height(4.dp))
            Text(
                "Il tuo coach raccoglie i dati con una conversazione. Niente moduli: rispondi e lui compila il profilo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.checking -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                !state.configured -> FitCard {
                    Column {
                        SectionTitle("Serve un provider AI")
                        Text(
                            "L'anamnesi e la generazione di schede e piani funzionano solo con un provider AI configurato.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onOpenAiSettings, modifier = Modifier.fillMaxWidth()) {
                            Text("Configura provider AI")
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { viewModel.refreshConfiguration() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ho configurato, ricontrolla")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            ChatBubble(text = message.content, fromUser = message.isUser)
                        }
                        if (state.sending) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(20.dp).fillMaxWidth(0.08f)
                                    )
                                    Text(
                                        "  Il coach sta scrivendo...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    state.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("Rispondi al coach") },
                            modifier = Modifier.weight(1f),
                            maxLines = 4
                        )
                        Button(
                            onClick = { viewModel.send(draft); draft = "" },
                            enabled = state.canSend && draft.isNotBlank(),
                            modifier = Modifier.padding(start = 8.dp)
                        ) { Text("Invia") }
                    }

                    if (state.profile.name.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.finish() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ho finito, salva l'anamnesi")
                        }
                    }
                }
            }
        }
    }
}
