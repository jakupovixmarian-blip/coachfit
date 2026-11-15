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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        onRetryGeneration = viewModel::retryGeneration,
        onUseOfflineEngine = viewModel::useOfflineEngine,
        onStartEdit = viewModel::startEdit,
        onEditSection = viewModel::editSection,
        onStopEdit = viewModel::stopEdit,
        onResetAnamnesis = viewModel::resetAnamnesis,
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
    onRetryGeneration: () -> Unit = {},
    onUseOfflineEngine: () -> Unit = {},
    onStartEdit: () -> Unit = {},
    onEditSection: (String) -> Unit = {},
    onStopEdit: () -> Unit = {},
    onResetAnamnesis: () -> Unit = {},
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

    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (state.editing) "Modifica anamnesi" else "Anamnesi iniziale – Il tuo profilo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            if (state.editing) {
                "Stai modificando i dati esistenti. Le sezioni sono precompilate: cambia ciò che serve " +
                    "e conferma per aggiornare il programma con il Coach AI."
            } else {
                "Il coach costruisce i tuoi programmi solo dopo questa intervista. " +
                    "Puoi modificare tutto in seguito."
            },
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

        if (state.confirmed && state.current == null && !state.editing) {
            SectionCard {
                Column {
                    Text("Anamnesi confermata", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.generationMessage
                            ?: "Il percorso verra generato automaticamente usando i dati confermati.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    if (state.generatingPrograms) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Il Coach AI costruisce il JSON; i motori verificano calorie, macro, volume e vincoli...",
                            style = MaterialTheme.typography.labelSmall, color = TextSecondary
                        )
                    }
                    if (state.error != null && !state.generatingPrograms) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRetryGeneration,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Riprova generazione AI") }
                        if (state.offlineFallbackAvailable) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "In alternativa puoi generare il percorso con il motore offline " +
                                    "(calcoli deterministici sui tuoi dati reali). Non sostituisce il Coach AI: " +
                                    "e una scelta esplicita.",
                                style = MaterialTheme.typography.labelSmall, color = TextSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = onUseOfflineEngine,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Usa il motore offline") }
                        }
                    }
                    state.generationWarnings.forEach { warning ->
                        Spacer(Modifier.height(4.dp))
                        Text("• $warning", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateDashboard,
                        enabled = !state.generatingPrograms,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Vai alla Dashboard") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenSummary,
                        modifier = Modifier.fillMaxWidth()) { Text("Rivedi il riepilogo") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStartEdit,
                        enabled = !state.generatingPrograms,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Modifica anamnesi") }
                    Spacer(Modifier.height(8.dp))
                    if (!confirmReset) {
                        OutlinedButton(
                            onClick = { confirmReset = true },
                            enabled = !state.generatingPrograms,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Azzera e ricompila da zero") }
                    } else {
                        Text(
                            "Azzera tutti i dati dell'anamnesi e ricompila da zero. Account, chiavi API " +
                                "e storico allenamenti completati NON vengono toccati.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { confirmReset = false },
                                modifier = Modifier.weight(1f)
                            ) { Text("Annulla") }
                            Button(
                                onClick = { confirmReset = false; onResetAnamnesis() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Conferma reset") }
                        }
                    }
                }
            }
        }

        // Modifica a sezioni: salto rapido a una sezione specifica.
        if (state.editing) {
            SectionCard {
                Column {
                    Text("Sezioni", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    AnamnesisEngine.sections.forEach { section ->
                        FilterChip(
                            selected = state.current?.section == section,
                            onClick = { onEditSection(section) },
                            label = { Text(section) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onStopEdit, modifier = Modifier.fillMaxWidth()) {
                        Text("Fine modifica (senza rigenerare)")
                    }
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
