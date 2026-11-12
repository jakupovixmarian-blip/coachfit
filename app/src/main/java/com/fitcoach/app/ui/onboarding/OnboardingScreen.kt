package com.fitcoach.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Il tuo profilo") },
            navigationIcon = { OutlinedButton(onClick = onCancel) { Text("Indietro") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Partiamo dai dati che conosci. FitCoach non inventa le informazioni mancanti.", style = MaterialTheme.typography.bodyLarge)
            Field("Età", state.age) { value -> viewModel.update { it.copy(age = value) } }
            Field("Sesso biologico", state.biologicalSex) { value -> viewModel.update { it.copy(biologicalSex = value) } }
            Field("Altezza (cm)", state.heightCm) { value -> viewModel.update { it.copy(heightCm = value) } }
            Field("Peso (kg)", state.weightKg) { value -> viewModel.update { it.copy(weightKg = value) } }
            Field("Percentuale di grasso (facoltativa)", state.bodyFatPercent) { value -> viewModel.update { it.copy(bodyFatPercent = value) } }
            Field("Obiettivo", state.goal) { value -> viewModel.update { it.copy(goal = value) } }
            Field("Esperienza", state.trainingExperience) { value -> viewModel.update { it.copy(trainingExperience = value) } }
            Field("Giorni disponibili", state.trainingDays) { value -> viewModel.update { it.copy(trainingDays = value) } }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.save(onCompleted) }, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                if (state.saving) CircularProgressIndicator()
                else Text("Salva profilo")
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

