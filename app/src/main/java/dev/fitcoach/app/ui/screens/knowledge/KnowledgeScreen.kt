package dev.fitcoach.app.ui.screens.knowledge

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.domain.model.KnowledgeNote
import dev.fitcoach.app.domain.model.KnowledgeTopic
import dev.fitcoach.app.ui.components.ChipSelector
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.SectionTitle

/**
 * The sources the agent may use. Built-in notes carry the citation they come from;
 * user notes let the user bring in their own material.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(viewModel: KnowledgeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fonti e note") },
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
                        SectionTitle("Come le usa l'agente")
                        Text(
                            "Queste note sono l'unico riferimento scientifico che l'agente puo citare. " +
                                "Le fonti predefinite riportano la citazione esatta; puoi disattivarle o " +
                                "aggiungere le tue note (per esempio dagli studi che segui).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Aggiungi una nota")
                        }
                    }
                }
            }

            items(state.notes, key = { it.id }) { note ->
                NoteCard(note = note, onToggle = { viewModel.toggle(note) }, onDelete = { viewModel.delete(note) })
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAdd) {
        AddNoteDialog(
            onDismiss = { showAdd = false },
            onConfirm = { topic, title, content, source ->
                viewModel.addNote(topic, title, content, source)
                showAdd = false
            }
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissMessage() },
            title = { Text("Fonti") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { viewModel.dismissMessage() }) { Text("OK") } }
        )
    }
}

@Composable
private fun NoteCard(note: KnowledgeNote, onToggle: () -> Unit, onDelete: () -> Unit) {
    FitCard {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${note.topic.label}${if (note.builtIn) " - fonte verificata" else " - tua nota"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = note.enabled, onCheckedChange = { onToggle() })
            }
            Spacer(Modifier.height(6.dp))
            Text(note.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Fonte: ${note.source}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!note.builtIn) {
                TextButton(onClick = onDelete) { Text("Elimina") }
            }
        }
    }
}

@Composable
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (KnowledgeTopic, String, String, String) -> Unit
) {
    var topic by remember { mutableStateOf(KnowledgeTopic.OTHER) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova nota") },
        text = {
            Column {
                ChipSelector(
                    label = "Argomento",
                    options = KnowledgeTopic.entries.toList(),
                    selected = topic,
                    optionLabel = { it.label },
                    onSelect = { topic = it }
                )
                Spacer(Modifier.height(6.dp))
                LabeledField("Titolo", title, { title = it })
                LabeledField("Contenuto", content, { content = it }, singleLine = false)
                LabeledField("Fonte (studio, autore, link)", source, { source = it }, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(topic, title, content, source) }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
