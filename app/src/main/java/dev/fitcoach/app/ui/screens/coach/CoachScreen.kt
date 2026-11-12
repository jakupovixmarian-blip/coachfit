package dev.fitcoach.app.ui.screens.coach

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.CoachChangeStatus
import dev.fitcoach.app.domain.model.InsightKind
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.SectionTitle

@Composable
fun CoachScreen(viewModel: CoachViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Coach", style = MaterialTheme.typography.headlineSmall) }

        item {
            FitCard {
                Column {
                    SectionTitle("Analisi basata sui tuoi dati")
                    if (state.insights.isEmpty()) {
                        Text(
                            "Registra allenamenti e misurazioni: le analisi compaiono qui, calcolate solo su dati reali.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.insights.forEach { insight ->
                            Spacer(Modifier.height(6.dp))
                            Text(insight.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                insight.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (insight.kind) {
                                    InsightKind.REGRESSION, InsightKind.STAGNATION -> MaterialTheme.colorScheme.error
                                    InsightKind.PROGRESS -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { viewModel.refreshInsights() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ricalcola analisi")
                    }
                }
            }
        }

        item {
            FitCard {
                Column {
                    SectionTitle("Generazione con AI")
                    Text(
                        "Il modello propone solo alimenti presenti nel tuo archivio. I valori restano quelli verificati e la proposta richiede approvazione.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    KeyValueRow("Chiave API configurata", if (state.hasApiKey) "Si" else "No")
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.generatePlanWithAi() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.generating
                    ) {
                        Text(if (state.generating) "Generazione in corso..." else "Genera proposta piano")
                    }
                    if (state.generating) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator()
                    }
                    state.message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item { SectionTitle("Proposte di modifica") }

        if (state.proposals.isEmpty()) {
            item {
                EmptyState(
                    title = "Nessuna proposta",
                    subtitle = "Le modifiche proposte dal coach compaiono qui e richiedono la tua approvazione esplicita."
                )
            }
        } else {
            items(state.proposals, key = { it.id }) { proposal ->
                FitCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(proposal.changeType.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                proposal.status.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = when (proposal.status) {
                                    CoachChangeStatus.APPLIED -> MaterialTheme.colorScheme.primary
                                    CoachChangeStatus.REJECTED -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Text(
                            DateUtils.format(DateUtils.fromEpochDay(proposal.createdAt / 86_400_000L)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(proposal.rationale, style = MaterialTheme.typography.bodyMedium)
                        KeyValueRow("Prima", proposal.previousValue)
                        KeyValueRow("Dopo", proposal.newValue)
                        Spacer(Modifier.height(6.dp))
                        if (proposal.status == CoachChangeStatus.PROPOSED) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.approveProposal(proposal) }) { Text("Approva") }
                                OutlinedButton(onClick = { viewModel.rejectProposal(proposal) }) { Text("Rifiuta") }
                                TextButton(onClick = { viewModel.deleteProposal(proposal) }) { Text("Elimina") }
                            }
                        } else {
                            TextButton(onClick = { viewModel.deleteProposal(proposal) }) { Text("Elimina") }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
