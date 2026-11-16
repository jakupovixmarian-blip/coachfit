package com.fitcoach.app.ui.screens.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/* ============================================================================
 * Knowledge — fonti scientifiche usate dai calcoli.
 * Le voci provengono dall'entità ScientificSource: nessun contenuto inventato.
 * Il disclaimer è vincolante: FitCoach non è un professionista sanitario.
 * ========================================================================== */

data class KnowledgeUiState(
    val groups: List<KnowledgeGroupUi> = emptyList(),
    val disclaimer: String = "FitCoach non formula diagnosi e non prescrive farmaci. " +
        "In presenza di dolore interrompi l'esercizio e rivolgiti a un professionista.",
)

data class KnowledgeGroupUi(
    val title: String,
    val items: List<KnowledgeItemUi>,
)

data class KnowledgeItemUi(
    val title: String,
    val source: String,
    val summary: String,
    val tag: String,
)

@Composable
fun KnowledgeContent(
    state: KnowledgeUiState,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text(text = "Knowledge", style = FitTextStyles.display, color = TextPrimary)
            Text(
                text = "Fonti dei calcoli e dei valori nutrizionali",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        SectionCard {
            Text(
                text = state.disclaimer,
                style = FitTextStyles.bodySmall,
                color = TextSecondary,
            )
        }

        if (state.groups.isEmpty()) {
            EmptyState(
                title = "Nessuna fonte caricata",
                message = "Le fonti vengono importate con il database alimentare al primo avvio.",
            )
        } else {
            state.groups.forEach { group ->
                SectionHeader(title = group.title)
                group.items.forEach { item ->
                    SectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    style = FitTextStyles.bodyStrong,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                Pill(text = item.tag, color = Emerald)
                            }
                            Text(
                                text = item.summary,
                                style = FitTextStyles.bodySmall,
                                color = TextSecondary,
                            )
                            Text(
                                text = item.source,
                                style = FitTextStyles.caption,
                                color = TextLow,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            GhostButton(
                                text = "Apri fonte",
                                onClick = { onOpenSource(item.source) },
                            )
                        }
                    }
                }
            }
        }
    }
}
