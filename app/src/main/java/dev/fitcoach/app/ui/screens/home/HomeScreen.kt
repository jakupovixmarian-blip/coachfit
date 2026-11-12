package dev.fitcoach.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.StatPill

@Composable
fun HomeScreen(
    onOpenWorkout: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenPlan: () -> Unit,
    onStartOnboarding: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ciao ${profile?.name?.ifBlank { "" } ?: ""}".trim(), style = MaterialTheme.typography.headlineSmall)
        Text(
            dev.fitcoach.app.core.util.DateUtils.format(dev.fitcoach.app.core.util.DateUtils.today()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (profile == null || !profile.onboardingCompleted) {
            FitCard {
                Column {
                    SectionTitle("Completa l'anamnesi")
                    Text(
                        "Inserisci i tuoi dati per ottenere target e programmi personalizzati. Puoi comunque usare l'app manualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onStartOnboarding) { Text("Apri anamnesi") }
                }
            }
        }

        // Oggi: calorie e macro
        FitCard {
            Column {
                SectionTitle("Oggi")
                val target = profile?.targetCalories()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Format.kcal(state.today.kcal), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        target?.let { "obiettivo ${Format.kcal(it)}" } ?: "obiettivo non calcolabile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (target != null && target > 0) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (state.today.kcal / target).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Rimanenti ${Format.kcal((target - state.today.kcal).coerceAtLeast(0.0))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatPill("Proteine", Format.grams(state.today.protein))
                    StatPill("Carboidrati", Format.grams(state.today.carbs))
                    StatPill("Grassi", Format.grams(state.today.fat))
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenDiary, modifier = Modifier.fillMaxWidth()) { Text("Apri diario") }
            }
        }

        // Allenamento
        FitCard {
            Column {
                SectionTitle("Allenamento")
                KeyValueRow("Programma attivo", state.routine?.name ?: "Nessuno")
                KeyValueRow("Sedute completate questa settimana", state.weeklySessions.toString())
                KeyValueRow("Ultima seduta", state.lastSession?.name ?: "Nessuna registrata")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenWorkout, modifier = Modifier.weight(1f)) { Text("Workout") }
                    OutlinedButton(onClick = onOpenPlan, modifier = Modifier.weight(1f)) { Text("Piano") }
                }
            }
        }

        // Piano alimentare
        FitCard {
            Column {
                SectionTitle("Piano alimentare")
                if (state.plan == null) {
                    Text(
                        "Nessun piano attivo. Puoi crearlo manualmente o con il Coach AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    KeyValueRow("Piano", state.plan!!.name)
                    KeyValueRow("Calorie target", Format.kcal(state.plan!!.targetKcal))
                    KeyValueRow("Proteine target", Format.grams(state.plan!!.targetProtein))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) { Text("Gestisci piano") }
            }
        }

        // Scorciatoie
        FitCard {
            Column {
                SectionTitle("Scorciatoie")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenProgress, modifier = Modifier.weight(1f)) { Text("Progressi") }
                    OutlinedButton(onClick = onOpenCoach, modifier = Modifier.weight(1f)) { Text("Coach") }
                }
            }
        }

        if (state.today.kcal == 0.0 && state.weeklySessions == 0 && state.plan == null && state.routine == null) {
            EmptyState(
                title = "Benvenuto in FitCoach",
                subtitle = "Inizia registrando un pasto dal Diario o una seduta dal Workout. I dati restano sul dispositivo."
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
