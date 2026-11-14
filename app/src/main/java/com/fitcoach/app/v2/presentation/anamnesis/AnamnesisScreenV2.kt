package com.fitcoach.app.v2.presentation.anamnesis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnamnesisScreenV2(
    navController: NavHostController,
    viewModel: AnamnesisViewModelV2 = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Anamnesi FitCoach 2.0") },
            navigationIcon = {
                OutlinedButton(onClick = { navController.popBackStack() }) { Text("Indietro") }
            }
        )
    }) { padding ->
        if (state.loading) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            }
            return@Scaffold
        }
        val field = state.currentField
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Rispondi con i tuoi dati reali. Se non conosci un dato, puoi dichiararlo: non verrà inventato.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Risposte: ${state.session?.answers?.size ?: 0}/${com.fitcoach.app.v2.domain.anamnesis.AnamnesisField.entries.size}",
                style = MaterialTheme.typography.labelLarge
            )
            if (field != null) {
                Text(field.question, style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Risposta") },
                    minLines = 2
                )
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = viewModel::answerUnknown, modifier = Modifier.weight(1f)) {
                        Text("Non lo so")
                    }
                    Button(onClick = viewModel::submit, modifier = Modifier.weight(1f)) {
                        Text("Avanti")
                    }
                }
            } else {
                Text("Anamnesi completata", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = viewModel::openReview, modifier = Modifier.fillMaxWidth()) {
                    Text("Controlla riepilogo")
                }
            }
            if (state.showReview) {
                ReviewPanel(state, viewModel)
            }
            state.session?.confirmedAt?.let {
                Text("Anamnesi confermata. Puoi modificarla rispondendo di nuovo alle domande.")
            }
        }
    }
}

@Composable
private fun ReviewPanel(state: AnamnesisV2State, viewModel: AnamnesisViewModelV2) {
    val review = state.review ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Riepilogo", style = MaterialTheme.typography.titleLarge)
        state.session?.let { session ->
            session.answers.entries.sortedBy { it.key.ordinal }.forEach { (field, answer) ->
                Text("${field.name}: ${answer.value ?: "Non lo so"}")
            }
        }
        review.missingAnswers.takeIf { it.isNotEmpty() }?.let {
            Text("Mancano: ${it.joinToString { field -> field.name }}", color = MaterialTheme.colorScheme.error)
        }
        review.unresolvedRequired.takeIf { it.isNotEmpty() }?.let {
            Text("Dati obbligatori non noti: ${it.joinToString { field -> field.name }}", color = MaterialTheme.colorScheme.error)
        }
        review.contradictions.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = viewModel::closeReview, modifier = Modifier.weight(1f)) {
                Text("Modifica")
            }
            Button(onClick = viewModel::confirm, enabled = review.canConfirm, modifier = Modifier.weight(1f)) {
                Text("Conferma")
            }
        }
    }
}
