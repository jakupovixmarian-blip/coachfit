package com.fitcoach.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.MacroBar
import com.fitcoach.app.ui.components.MacroLegend
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.StatTile
import com.fitcoach.app.ui.navigation.Routes
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.HighCarbColor
import com.fitcoach.app.ui.theme.LowCarbColor
import com.fitcoach.app.ui.theme.MediumCarbColor
import com.fitcoach.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.dayName.ifBlank { "Oggi" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (state.profileName.isNotBlank()) "Ciao ${state.profileName}" else state.goalLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Impostazioni", tint = TextSecondary)
                }
            }
        }

        if (!state.hasProfile) {
            item {
                EmptyState(
                    title = "Completa il tuo profilo",
                    message = "Inserisci i tuoi dati per generare piano alimentare e scheda personalizzati.",
                    action = {
                        Button(onClick = { navController.navigate(Routes.ONBOARDING) }) {
                            Text("Inizia l'onboarding")
                        }
                    }
                )
            }
            return@LazyColumn
        }

        if (state.needsAnamnesis) {
            item {
                SectionCard {
                    Column {
                        SectionHeader("Anamnesi da completare", "Passaggio necessario")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Prima di generare il piano alimentare e la scheda, " +
                                "completa e conferma l'anamnesi guidata. Il coach usa questi dati " +
                                "per personalizzare tutto il programma.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navController.navigate(Routes.ANAMNESIS) }) {
                            Text("Completa l'anamnesi")
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("Oggi", "Giornata alimentare")
                        state.dayType?.let { Pill(it.label, carbColor(it)) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Calorie", "${state.targetKcal.toInt()}", Emerald, Modifier.weight(1f))
                        StatTile(
                            "Peso",
                            state.latestWeight?.let { "%.1f kg".format(it) } ?: "n/d",
                            MaterialTheme.colorScheme.onSurface,
                            Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    MacroBar(state.targetProtein, state.targetCarbs, state.targetFat)
                    Spacer(Modifier.height(8.dp))
                    MacroLegend(state.targetProtein, state.targetCarbs, state.targetFat)
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Allenamento di oggi", state.workoutTitle ?: "Giorno di riposo")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.workoutDone) Icons.Filled.CheckCircle else Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = if (state.workoutDone) Emerald else TextSecondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                state.workoutTitle == null -> "Nessuna seduta programmata per oggi."
                                state.workoutDone -> "Seduta completata. Ottimo lavoro!"
                                else -> "${state.workoutExercises} esercizi programmati."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (state.workoutTitle != null && !state.workoutDone) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navController.navigate(Routes.WORKOUT) }) {
                            Text("Vai all'allenamento")
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Pasti previsti")
                    Spacer(Modifier.height(10.dp))
                    if (state.meals.isEmpty()) {
                        Text(
                            "Nessun pasto: genera il piano in Alimentazione.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        state.meals.forEach { meal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        meal.slot.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        meal.entries.joinToString { it.foodName },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )
                                }
                                Text(
                                    "${meal.kcal.toInt()} kcal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Emerald
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Prossima notifica")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.nextNotification ?: "Nessuna notifica attiva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        state.weightTrend?.let { trend ->
            item {
                SectionCard {
                    Column {
                        SectionHeader("Andamento peso", "${trend.weeksOfData} settimane di dati")
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val rate = trend.trendKgPerWeek
                            Icon(
                                imageVector = if ((rate ?: 0.0) <= 0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if ((rate ?: 0.0) <= 0) LowCarbColor else MediumCarbColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = rate?.let { "%+.2f kg / settimana".format(it) }
                                    ?: "Dati insufficienti per la tendenza",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!trend.isReliable) {
                            Text(
                                "Servono almeno 3 settimane di misurazioni per una stima affidabile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { navController.navigate(Routes.COACH) }, modifier = Modifier.weight(1f)) {
                    Text("Chiedi al Coach")
                }
                Button(onClick = { navController.navigate(Routes.PROGRESS) }, modifier = Modifier.weight(1f)) {
                    Text("Progressi")
                }
            }
        }
    }
}

private fun carbColor(type: CarbDayType) = when (type) {
    CarbDayType.HIGH -> HighCarbColor
    CarbDayType.MEDIUM -> MediumCarbColor
    CarbDayType.LOW -> LowCarbColor
}
