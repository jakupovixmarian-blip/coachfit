package com.fitcoach.app.ui.anamnesis

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.navigation.Routes
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Anamnesi iniziale guidata (spec. §3).
 *
 * Intervista progressiva: una domanda per volta, con avanzamento, possibilità di
 * tornare indietro, riepilogo finale e conferma esplicita. Finche non si conferma,
 * la generazione di alimentazione e allenamento resta bloccata.
 */
@Composable
fun AnamnesisScreen(
    navController: NavHostController,
    viewModel: AnamnesisViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnamnesisContent(
        state = state,
        onNavigateDashboard = { navController.navigate(Routes.DASHBOARD) },
        onOpenSummary = viewModel::openSummary,
        onCloseSummary = viewModel::closeSummary,
        onConfirm = viewModel::confirm,
        onSubmit = viewModel::submit,
        onBack = viewModel::goBack,
        onSkip = viewModel::skip,
        onToggleOption = viewModel::toggleOption,
        onUpdateInput = viewModel::updateInput
    )
}

/**
 * Contenuto della schermata, senza dipendenze da ViewModel/Navigazione: cosi e
 * renderizzabile in isolamento (test/anteprime) e verificabile visivamente.
 */
@Composable
internal fun AnamnesisContent(
    state: AnamnesisUiState,
    onNavigateDashboard: () -> Unit,
    onOpenSummary: () -> Unit,
    onCloseSummary: () -> Unit,
    onConfirm: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onToggleOption: (String, Boolean) -> Unit,
    onUpdateInput: (String) -> Unit
) {
    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Anamnesi iniziale – Il tuo profilo", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Il coach costruisce i tuoi programmi solo dopo questa intervista. " +
                "Puoi modificare tutto in seguito.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary
        )

        val total = AnamnesisEngine.questions.size
        LinearProgressIndicator(
            progress = { state.answeredCount.toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )
        Text("${state.answeredCount} / $total domande · ${state.totalRequired} obbligatorie",
            style = MaterialTheme.typography.labelSmall, color = TextSecondary)

        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
        if (state.contradictions.isNotEmpty()) {
            SectionCard {
                Column {
                    Text("Incongruenze rilevate", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error)
                    state.contradictions.forEach { c ->
                        Text("· $c", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (state.confirmed && state.current == null) {
            SectionCard {
                Column {
                    Text("Anamnesi confermata", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ora puoi generare alimentazione e allenamento dalla Dashboard o dalle " +
                            "rispettive sezioni.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNavigateDashboard,
                        modifier = Modifier.fillMaxWidth()) { Text("Vai alla Dashboard") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenSummary,
                        modifier = Modifier.fillMaxWidth()) { Text("Rivedi il riepilogo") }
                }
            }
        }

        val q = state.current
        if (q != null && !state.showSummary) {
            SectionCard {
                Column {
                    Text(q.section.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(q.prompt, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground)
                    if (q.hint.isNotBlank()) {
                        Text(q.hint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(12.dp))

                    when (q.kind) {
                        AnamnesisEngine.Kind.CHOICE -> {
                            q.options.forEach { opt ->
                                FilterChip(
                                    selected = state.selected.contains(opt),
                                    onClick = { onToggleOption(opt, false) },
                                    label = { Text(opt) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                )
                            }
                        }
                        AnamnesisEngine.Kind.MULTI -> {
                            q.options.forEach { opt ->
                                FilterChip(
                                    selected = state.selected.contains(opt),
                                    onClick = { onToggleOption(opt, true) },
                                    label = { Text(opt) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                )
                            }
                        }
                        AnamnesisEngine.Kind.BOOL -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = state.input.equals("Si", true),
                                    onClick = { onUpdateInput("Si") }, label = { Text("Sì") }
                                )
                                FilterChip(
                                    selected = state.input.equals("No", true),
                                    onClick = { onUpdateInput("No") }, label = { Text("No") }
                                )
                            }
                        }
                        else -> {
                            OutlinedTextField(
                                value = state.input,
                                onValueChange = onUpdateInput,
                                singleLine = q.kind != AnamnesisEngine.Kind.TEXT,
                                label = { Text(labelFor(q)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onBack) { Text("Indietro") }
                        if (!q.required) {
                            OutlinedButton(onClick = onSkip) { Text("Salta") }
                        }
                        Button(onClick = onSubmit, modifier = Modifier.weight(1f)) {
                            Text("Avanti")
                        }
                    }
                }
            }
        }

        if (state.showSummary || (state.current == null && !state.confirmed)) {
            SectionCard {
                Column {
                    Text("Riepilogo anamnesi", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Text(state.summary.ifBlank { AnamnesisEngine.summary(state.profile) },
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    if (state.missingRequired.isNotEmpty()) {
                        Text("Domande obbligatorie mancanti:", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error)
                        state.missingRequired.forEach {
                            Text("· $it", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = state.missingRequired.isEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Conferma anamnesi") }
                    if (state.showSummary) {
                        OutlinedButton(onClick = onCloseSummary,
                            modifier = Modifier.fillMaxWidth()) { Text("Torna alle domande") }
                    }
                }
            }
        }
    }
}

private fun labelFor(q: AnamnesisEngine.Question): String = when (q.kind) {
    AnamnesisEngine.Kind.NUMBER -> "Valore"
    AnamnesisEngine.Kind.INT -> "Numero"
    else -> "Risposta"
}
