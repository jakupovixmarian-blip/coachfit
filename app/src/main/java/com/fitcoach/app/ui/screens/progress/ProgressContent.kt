package com.fitcoach.app.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.BarChart
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.SegmentedControl
import com.fitcoach.app.ui.components.StatTile
import com.fitcoach.app.ui.components.TrendChart
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.AiBorder
import com.fitcoach.app.ui.theme.AiSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FatColor
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.InfoBlue
import com.fitcoach.app.ui.theme.OfflineGray
import com.fitcoach.app.ui.theme.ProteinColor
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* =============================================================================
 * Progressi — grafici, filtri temporali, confronto periodi e proposte.
 * Nessun dato inventato: ogni sezione compare solo se esistono record reali.
 * ========================================================================== */

@Composable
fun ProgressContent(
    state: ProgressUiState,
    onRangeChange: (RangeFilter) -> Unit,
    onApproveProposal: (Long) -> Unit,
    onRejectProposal: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.errorMessage
    val ranges = RangeFilter.entries

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Progressi", style = FitTextStyles.display, color = TextPrimary)
                Text(
                    text = "Confronto sul periodo selezionato",
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                )
            }
            if (state.adherencePercent != null) {
                Pill(text = "Aderenza ${state.adherencePercent}%", color = adherenceColor(state.adherencePercent))
            }
        }

        SegmentedControl(
            options = ranges.map { it.label },
            selectedIndex = ranges.indexOf(state.range).coerceAtLeast(0),
            onSelect = { index -> onRangeChange(ranges[index]) },
        )

        when {
            state.isLoading -> {
                LoadingBox()
                LoadingBox()
                LoadingBox()
            }

            error != null -> EmptyState(
                title = "Dati non disponibili",
                message = error,
                actionLabel = "Riprova",
                onAction = onRetry,
            )

            !state.hasAnything && state.pendingProposals.isEmpty() -> EmptyState(
                title = "Ancora nessun dato",
                message = "I grafici compaiono solo con record reali: peso, circonferenze, serie registrate e check-in settimanali.",
                actionLabel = "Registra un check-in",
                onAction = onRetry,
            )

            else -> {
                if (state.hasWeight) {
                    WeightSection(state)
                }
                if (state.hasComposition) {
                    CompositionSection(state)
                }
                if (state.hasStrength) {
                    StrengthSection(state)
                }
                if (state.hasVolume) {
                    VolumeSection(state)
                }
                if (state.hasCircumferences) {
                    CircumferenceSection(state)
                }
                state.checkIn?.let { CheckInSection(it) }
                if (state.proposals.isNotEmpty()) {
                    ProposalsSection(
                        proposals = state.proposals,
                        onApprove = onApproveProposal,
                        onReject = onRejectProposal,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightSection(state: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Peso corporeo",
            subtitle = "Media settimanale, non la singola pesata",
        )
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TrendChart(
                    values = state.weightSeries.map { it.value },
                    labels = state.weightSeries.map { it.label },
                    unitSuffix = " ${state.weightUnit}",
                    lineColor = Emerald,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.weightDeltaLabel != null) {
                        Text(
                            text = state.weightDeltaLabel,
                            style = FitTextStyles.numericS,
                            color = trendColor(state.weightTrendDown),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "vs periodo precedente",
                        style = FitTextStyles.caption,
                        color = TextLow,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompositionSection(state: ProgressUiState) {
    val composition = state.composition ?: return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Composizione corporea", subtitle = "Massa grassa e massa magra registrate")
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        label = "Massa grassa",
                        value = "${"%.1f".format(composition.fatMassKg)} kg",
                        accent = FatColor,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Massa magra",
                        value = "${"%.1f".format(composition.leanMassKg)} kg",
                        accent = ProteinColor,
                        modifier = Modifier.weight(1f),
                    )
                }
                DataRow(
                    label = "Percentuale grasso",
                    value = "${"%.1f".format(composition.fatPercent)} %",
                )
                DataRow(
                    label = "Variazione grasso",
                    value = signed(composition.fatDeltaKg, "kg"),
                    valueColor = trendColor(composition.fatDeltaKg < 0),
                )
                DataRow(
                    label = "Variazione massa magra",
                    value = signed(composition.leanDeltaKg, "kg"),
                    valueColor = trendColor(composition.leanDeltaKg > 0),
                )
            }
        }
    }
}

@Composable
private fun StrengthSection(state: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Progressione carichi", subtitle = "Miglior serie per esercizio")
        state.strength.forEach { strength ->
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strength.exerciseName,
                            style = FitTextStyles.bodyStrong,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = strength.topSetLabel,
                            style = FitTextStyles.numericS,
                            color = TextPrimary,
                        )
                    }
                    if (strength.series.size >= 2) {
                        TrendChart(
                            values = strength.series.map { it.value },
                            labels = strength.series.map { it.label },
                            heightDp = 110,
                            lineColor = SuccessGreen,
                        )
                    }
                    if (strength.deltaLabel != null) {
                        Text(
                            text = strength.deltaLabel,
                            style = FitTextStyles.caption,
                            color = SuccessGreen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeSection(state: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Volume allenante", subtitle = state.volumeTotalLabel)
        SectionCard {
            BarChart(
                values = state.volumeSeries.map { it.value },
                labels = state.volumeSeries.map { it.label },
                heightDp = 130,
                color = InfoBlue,
                highlightIndex = state.volumeSeries.lastIndex,
            )
        }
    }
}

@Composable
private fun CircumferenceSection(state: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Misure corporee", subtitle = "Ultimo valore registrato")
        SectionCard {
            Column {
                state.circumferences.forEach { circumference ->
                    DataRow(
                        label = circumference.name,
                        value = "${"%.1f".format(circumference.valueCm)} cm · ${signed(circumference.deltaCm, "cm")}",
                        valueColor = if (circumference.deltaCm == 0.0) TextPrimary else TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInSection(checkIn: CheckInUi) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Check-in settimanale", subtitle = checkIn.weekLabel)
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DataRow(label = "Peso medio", value = checkIn.weightAvgLabel)
                DataRow(label = "Aderenza", value = checkIn.adherenceLabel)
                DataRow(label = "Performance", value = checkIn.performanceLabel)
                if (checkIn.sleepLabel != null) {
                    DataRow(label = "Sonno", value = checkIn.sleepLabel)
                }
                if (checkIn.note != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = checkIn.note,
                        style = FitTextStyles.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProposalsSection(
    proposals: List<ProposalUi>,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Proposte di modifica",
            subtitle = "Restano in attesa finché non decidi",
        )
        proposals.forEach { proposal ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FitRadius.lg))
                    .background(AiSoft)
                    .border(width = 1.dp, color = AiBorder, shape = RoundedCornerShape(FitRadius.lg))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = proposal.title,
                        style = FitTextStyles.bodyStrong,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Pill(
                        text = when (proposal.status) {
                            ProposalStatus.PROPOSED -> "Da approvare"
                            ProposalStatus.APPROVED -> "Approvata"
                            ProposalStatus.REJECTED -> "Rifiutata"
                        },
                        color = when (proposal.status) {
                            ProposalStatus.PROPOSED -> WarningAmber
                            ProposalStatus.APPROVED -> SuccessGreen
                            ProposalStatus.REJECTED -> OfflineGray
                        },
                    )
                }

                ProposalField("Situazione attuale", proposal.situation)
                ProposalField("Problema", proposal.problem)
                ProposalField("Dati usati", proposal.dataUsed)
                ProposalField("Modifica suggerita", proposal.change)
                ProposalField("Motivazione", proposal.rationale)
                ProposalField("Risultato atteso", proposal.expected)
                if (proposal.cautions != null) {
                    ProposalField("Controindicazioni", proposal.cautions)
                }

                if (proposal.status == ProposalStatus.PROPOSED) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Rifiuta",
                            onClick = { onReject(proposal.id) },
                            enabled = !proposal.isApplying,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = "Approva",
                            onClick = { onApprove(proposal.id) },
                            enabled = !proposal.isApplying,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), style = FitTextStyles.caption, color = TextLow)
        Text(
            text = value,
            style = FitTextStyles.bodySmall,
            color = TextSecondary,
            textDecoration = TextDecoration.None,
        )
    }
}

private fun adherenceColor(percent: Int) = when {
    percent >= 85 -> SuccessGreen
    percent >= 65 -> WarningAmber
    else -> OfflineGray
}

private fun trendColor(isPositive: Boolean?): androidx.compose.ui.graphics.Color = when (isPositive) {
    true -> SuccessGreen
    false -> WarningAmber
    null -> TextPrimary
}

private fun signed(value: Double, unit: String): String {
    val rounded = "%.1f".format(value)
    return if (value > 0) "+$rounded $unit" else "$rounded $unit"
}
