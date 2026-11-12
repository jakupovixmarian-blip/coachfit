package dev.fitcoach.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.HeroCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.ProgressTrack
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.StatTile
import dev.fitcoach.app.ui.theme.BlueAccent
import dev.fitcoach.app.ui.theme.VioletAccent

@Composable
fun HomeScreen(
    onOpenWorkout: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartOnboarding: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    val target = profile?.targetCalories()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Ciao${profile?.name?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""}",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            DateUtils.format(DateUtils.today()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (profile == null || !profile.onboardingCompleted) {
            HeroCard {
                Column {
                    SectionTitle("Inizia da qui")
                    Text(
                        "Completa l'anamnesi: l'agente raccoglie i tuoi dati e costruisce scheda e piano.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStartOnboarding) { Text("Apri anamnesi") }
                }
            }
        }

        // Il numero piu importante della schermata.
        HeroCard {
            Column {
                SectionTitle("Oggi")
                Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    Text(
                        state.today.kcal.toInt().toString(),
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (target != null && target > 0) {
                    Spacer(Modifier.height(10.dp))
                    ProgressTrack((state.today.kcal / target).toFloat())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "obiettivo ${Format.kcal(target)} · rimangono " +
                            Format.kcal((target - state.today.kcal).coerceAtLeast(0.0)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Proteine", Format.grams(state.today.protein), accent = MaterialTheme.colorScheme.primary)
                    StatTile("Carboidrati", Format.grams(state.today.carbs), accent = BlueAccent)
                    StatTile("Grassi", Format.grams(state.today.fat), accent = VioletAccent)
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Allenamento")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Sedute", state.weeklySessions.toString())
                    StatTile("Volume", Format.kg(state.weeklyVolumeKg))
                }
                Spacer(Modifier.height(10.dp))
                KeyValueRow("Programma attivo", state.routine?.name ?: "Nessuno")
                KeyValueRow("Ultima seduta", state.lastSession?.name ?: "Nessuna")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenWorkout, modifier = Modifier.weight(1f)) { Text("Workout") }
                    OutlinedButton(onClick = onOpenProgress, modifier = Modifier.weight(1f)) { Text("Progressi") }
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Piano alimentare")
                if (state.plan == null) {
                    Text(
                        "Nessun piano attivo. Generane uno con l'agente o crealo a mano.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    KeyValueRow("Piano", state.plan!!.name)
                    KeyValueRow("Calorie target", Format.kcal(state.plan!!.targetKcal))
                    KeyValueRow("Proteine target", Format.grams(state.plan!!.targetProtein))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenPlan, modifier = Modifier.weight(1f)) { Text("Piano") }
                    OutlinedButton(onClick = onOpenDiary, modifier = Modifier.weight(1f)) { Text("Diario") }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenCoach,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) { Text("Parla con il Coach AI") }

        FitCard {
            Column {
                SectionTitle("Altro")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenCalendar, modifier = Modifier.weight(1f)) { Text("Calendario") }
                    OutlinedButton(onClick = onOpenShopping, modifier = Modifier.weight(1f)) { Text("Spesa") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Impostazioni")
                }
            }
        }

        if (state.today.kcal == 0.0 && state.weeklySessions == 0 && state.plan == null && state.routine == null) {
            EmptyState(
                title = "Benvenuto in FitCoach",
                subtitle = "Registra un pasto dal Diario o avvia una seduta dal Workout. I dati restano sul dispositivo."
            )
        }
    }
}
