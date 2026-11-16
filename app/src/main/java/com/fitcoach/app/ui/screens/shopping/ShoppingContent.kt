package com.fitcoach.app.ui.screens.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.ProgressRing
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.OnBrand
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/* =============================================================================
 * Lista della spesa — derivata dal piano alimentare settimanale.
 * Il flag "preso" è persistito su ShoppingItem: qui si legge e si aggiorna.
 * ========================================================================== */

@Composable
fun ShoppingContent(
    state: ShoppingUiState,
    onToggleItem: (Long) -> Unit,
    onClearChecked: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(text = "Lista della spesa", style = FitTextStyles.display, color = TextPrimary)
            Text(
                text = state.weekLabel,
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        when {
            state.isLoading -> {
                LoadingBox()
                LoadingBox()
            }

            error != null -> EmptyState(
                title = "Lista non disponibile",
                message = error,
                actionLabel = "Riprova",
                onAction = onRetry,
            )

            !state.hasItems -> EmptyState(
                title = "Lista vuota",
                message = "La lista si genera dagli alimenti del piano settimanale attivo. Genera il piano per popolarla.",
                actionLabel = "Genera dal piano",
                onAction = onRegenerate,
            )

            else -> {
                ProgressCard(state)

                state.groups.forEach { group ->
                    SectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = group.category.uppercase(),
                                style = FitTextStyles.caption,
                                color = TextLow,
                            )
                            group.items.forEach { item ->
                                ItemRow(item = item, onToggle = onToggleItem)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(text = "Rigenera dalla settimana", onClick = onRegenerate)
                    GhostButton(
                        text = "Deseleziona tutto",
                        onClick = onClearChecked,
                        enabled = state.checkedItems > 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(state: ShoppingUiState) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = state.progress,
                size = 92.dp,
                strokeWidth = 9.dp,
                center = {
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        style = FitTextStyles.numericS,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                },
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${state.checkedItems} / ${state.totalItems} articoli",
                    style = FitTextStyles.bodyStrong,
                    color = TextPrimary,
                )
                Text(
                    text = "Quantità calcolate sui grammi del piano",
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ItemRow(item: ShoppingItemUi, onToggle: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(item.id) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (item.checked) Emerald else androidx.compose.ui.graphics.Color.Transparent)
                .border(width = 1.dp, color = if (item.checked) Emerald else FitOutline, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (item.checked) {
                Text(text = "✓", style = FitTextStyles.caption, color = OnBrand)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.name,
            style = FitTextStyles.bodySmall,
            color = if (item.checked) TextLow else TextPrimary,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = item.quantityLabel,
            style = FitTextStyles.caption,
            color = TextSecondary,
        )
    }
}
