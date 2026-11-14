package com.fitcoach.app.ui.settings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.TextSecondary

@Composable
fun ProfileEditScreen(
    navController: NavHostController,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    val profile = state.profile
    if (profile == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Nessun profilo. Completa l'onboarding.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = TextSecondary)
            }
            Text("Modifica profilo", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SectionCard {
                    Column {
                        SectionHeader("Peso e composizione", "Registra i valori attuali")
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.weightKg, onValueChange = viewModel::updateWeight,
                                label = { Text("Peso (kg)") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state.bodyFat, onValueChange = viewModel::updateBodyFat,
                                label = { Text("Massa grassa %") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        SectionHeader("Obiettivo")
                        Spacer(Modifier.height(8.dp))
                        Goal.entries.forEach { g ->
                            FilterChip(
                                selected = profile.goal == g,
                                onClick = { viewModel.setGoal(g) },
                                label = { Text(g.label) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        SectionHeader("Esperienza e frequenza")
                        Spacer(Modifier.height(8.dp))
                        TrainingExperience.entries.forEach { e ->
                            FilterChip(
                                selected = profile.experience == e,
                                onClick = { viewModel.setExperience(e) },
                                label = { Text(e.label) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Giorni a settimana", style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (2..6).forEach { d ->
                                FilterChip(
                                    selected = profile.trainingDaysPerWeek == d,
                                    onClick = { viewModel.setTrainingDays(d) },
                                    label = { Text("$d") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        SectionHeader("Sesso biologico")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Sex.entries.forEach { s ->
                                FilterChip(
                                    selected = profile.sex == s,
                                    onClick = { viewModel.setSex(s) },
                                    label = { Text(sexLabel(s)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column {
                        SectionHeader("Attrezzatura disponibile")
                        Spacer(Modifier.height(8.dp))
                        Equipment.entries.forEach { eq ->
                            FilterChip(
                                selected = eq in profile.availableEquipment,
                                onClick = { viewModel.toggleEquipment(eq) },
                                label = { Text(eq.label) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.regenerate,
                            onCheckedChange = { viewModel.setRegenerate(it) })
                        Text("Rigenera piano e scheda con i nuovi dati",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.save() },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.saving) "Salvataggio..." else "Salva modifiche")
                }
            }
        }
    }

    SnackbarHost(snackbar)
}

private fun sexLabel(sex: Sex) = when (sex) {
    Sex.MALE -> "Uomo"
    Sex.FEMALE -> "Donna"
    Sex.UNSPECIFIED -> "Non spec."
}
