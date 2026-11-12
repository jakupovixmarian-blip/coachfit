package com.fitcoach.app.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NutritionScreen(viewModel: NutritionViewModel = hiltViewModel()) {
    val estimate by viewModel.estimate.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Alimentazione", style = MaterialTheme.typography.headlineLarge)
        Text("La stima è separata dai dati realmente registrati.")
        Button(onClick = viewModel::calculate, modifier = Modifier.fillMaxWidth()) { Text("Calcola stima dal profilo") }
        estimate?.let {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stima energetica", style = MaterialTheme.typography.titleLarge)
                    Text("Metabolismo basale: ${it.bmrKcal} kcal")
                    Text("Dispendio totale stimato: ${it.tdeeKcal} kcal")
                    Text("Target iniziale: ${it.targetKcal} kcal")
                    Text("Valori stimati: verifica l'andamento medio settimanale prima di adattare il piano.")
                }
            }
        }
    }
}
