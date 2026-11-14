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
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mostra la base di conoscenza: fonti scientifiche (con evidenza, regola
 * derivata e limiti di applicabilita) e storico delle versioni dei piani.
 */
@Composable
fun KnowledgeScreen(
    navController: NavHostController,
    viewModel: KnowledgeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = TextSecondary)
            }
            Text("Fonti e versioni", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground)
        }

        if (state.loading) {
            LoadingBox(Modifier.fillMaxSize())
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                SectionCard {
                    Column {
                        SectionHeader("Storico versioni dei piani", "Ogni rigenerazione crea una versione")
                        Spacer(Modifier.height(8.dp))
                        if (state.versions.isEmpty()) {
                            Text("Nessuna versione registrata.", style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary)
                        } else {
                            state.versions.take(20).forEach { v ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Pill(if (v.isActive) "Attiva" else v.domain.name, Emerald)
                                        Spacer(Modifier.width(6.dp))
                                        Text(v.label, style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(formatDate(v.createdAt), style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary)
                                    Text(v.snapshotJson, style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary)
                                    if (v.reason.isNotBlank()) {
                                        Text("Motivo: ${v.reason}",
                                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("Fonti scientifiche", "Base di conoscenza dei motori")
            }

            if (state.sources.isEmpty()) {
                item {
                    SectionCard {
                        Text("Nessuna fonte disponibile.", style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                }
            } else {
                state.sources.forEach { source ->
                    item {
                        SectionCard {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Pill(source.kind.label, Emerald)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(source.title, style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface)
                                if (source.organization.isNotBlank() || source.authors.isNotBlank()) {
                                    Text(
                                        listOf(source.organization, source.authors)
                                            .filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                                    )
                                }
                                if (source.publishedDate.isNotBlank()) {
                                    Text("Pubblicazione: ${source.publishedDate}",
                                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(source.evidence, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(6.dp))
                                Text("Regola applicata: ${source.derivedRule}",
                                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                if (source.applicabilityLimits.isNotBlank()) {
                                    Text("Limiti di applicabilita: ${source.applicabilityLimits}",
                                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(source.url, style = MaterialTheme.typography.bodySmall, color = Emerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(epochMillis))
