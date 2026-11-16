package com.fitcoach.app.ui.screens.coach

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.AiAccent
import com.fitcoach.app.ui.theme.AiAccentAlt
import com.fitcoach.app.ui.theme.AiBorder
import com.fitcoach.app.ui.theme.AiSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.OfflineGray
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* =============================================================================
 * Coach AI — identità visiva distinta (accento AiAccent, superfici proprie).
 * Il messaggio normale e la scheda di proposta sono visivamente diversi:
 * la proposta ha bordo e fondo dedicati, confronto prima/dopo e due azioni
 * reali (approva / rifiuta) che chiamano la conferma già esistente.
 * ========================================================================== */

@Composable
fun CoachContent(
    state: CoachUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onOpenProviderSettings: () -> Unit,
    onOpenHistory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        CoachHeader(state = state, onOpenProviderSettings = onOpenProviderSettings)

        if (state.history.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            HistoryRow(history = state.history, onOpenHistory = onOpenHistory)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.status == CoachStatus.NO_PROVIDER -> EmptyState(
                    title = "Nessun provider AI configurato",
                    message = "La generazione e le proposte richiedono un provider attivo. Senza provider il Coach non produce piani alternativi.",
                    actionLabel = "Configura provider",
                    onAction = onOpenProviderSettings,
                )

                !state.hasMessages -> EmptyState(
                    title = "Coach pronto",
                    message = "Il Coach conosce profilo, piano alimentare, carb cycling, scheda, storico carichi e modifiche precedenti.",
                )

                else -> {
                    state.messages.forEach { message ->
                        MessageBubble(
                            message = message,
                            onApprove = onApprove,
                            onReject = onReject,
                        )
                    }
                    if (state.isSending) {
                        ThinkingIndicator()
                    }
                }
            }

            if (error != null) {
                Text(text = error, style = FitTextStyles.caption, color = ErrorRed)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        CoachInput(
            text = state.inputText,
            canSend = state.canSend,
            onInputChange = onInputChange,
            onSend = onSend,
        )
    }
}

@Composable
private fun CoachHeader(state: CoachUiState, onOpenProviderSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AiAccent, AiAccentAlt)))
                .border(width = 1.dp, color = AiBorder, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "AI", style = FitTextStyles.label, color = TextPrimary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Coach AI", style = FitTextStyles.h3, color = TextPrimary)
            Text(
                text = if (state.providerLabel.isNotBlank()) {
                    listOfNotNull(state.providerLabel, state.modelLabel.ifBlank { null }).joinToString(" · ")
                } else {
                    "Nessun provider configurato"
                },
                style = FitTextStyles.caption,
                color = TextSecondary,
                modifier = Modifier.clickable { onOpenProviderSettings() },
            )
        }
        Pill(text = statusLabel(state.status), color = statusColor(state.status))
    }
}

@Composable
private fun HistoryRow(history: List<CoachSessionUi>, onOpenHistory: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        history.take(3).forEach { session ->
            val shape = RoundedCornerShape(FitRadius.sm)
            Text(
                text = session.title,
                style = FitTextStyles.caption,
                color = if (session.isActive) AiAccent else TextLow,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (session.isActive) AiSoft else FitSurfaceVariant)
                    .border(width = 1.dp, color = if (session.isActive) AiBorder else FitOutline, shape = shape)
                    .clickable { onOpenHistory(session.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: CoachMessageUi,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
) {
    when (message.role) {
        CoachRole.SYSTEM -> Text(
            text = message.text,
            style = FitTextStyles.caption,
            color = TextLow,
            modifier = Modifier.fillMaxWidth(),
        )

        CoachRole.USER -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(FitRadius.md))
                    .background(FitSurfaceVariant)
                    .border(width = 1.dp, color = FitOutline, shape = RoundedCornerShape(FitRadius.md))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text = message.text, style = FitTextStyles.bodySmall, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message.timeLabel, style = FitTextStyles.caption, color = TextLow)
            }
        }

        CoachRole.COACH -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FitRadius.lg))
                .background(AiSoft)
                .border(width = 1.dp, color = AiBorder, shape = RoundedCornerShape(FitRadius.lg))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message.text,
                style = FitTextStyles.bodySmall,
                color = if (message.isError) ErrorRed else TextPrimary,
            )
            Text(text = message.timeLabel, style = FitTextStyles.caption, color = TextLow)
            if (message.proposal != null) {
                ProposalCard(
                    proposal = message.proposal,
                    onApprove = onApprove,
                    onReject = onReject,
                )
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: CoachProposalUi,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FitRadius.md))
            .background(FitSurfaceVariant)
            .border(width = 1.dp, color = AiBorder, shape = RoundedCornerShape(FitRadius.md))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PROPOSTA DI MODIFICA",
                style = FitTextStyles.caption,
                color = AiAccent,
                modifier = Modifier.weight(1f),
            )
            Pill(
                text = when (proposal.status) {
                    CoachProposalStatus.PROPOSED -> "Da approvare"
                    CoachProposalStatus.APPROVED -> "Approvata"
                    CoachProposalStatus.REJECTED -> "Rifiutata"
                },
                color = when (proposal.status) {
                    CoachProposalStatus.PROPOSED -> WarningAmber
                    CoachProposalStatus.APPROVED -> SuccessGreen
                    CoachProposalStatus.REJECTED -> OfflineGray
                },
            )
        }

        Text(text = proposal.title, style = FitTextStyles.bodyStrong, color = TextPrimary)

        ProposalField("Situazione attuale", proposal.situation)
        ProposalField("Problema rilevato", proposal.problem)
        ProposalField("Dati usati", proposal.dataUsed)
        ProposalField("Modifica suggerita", proposal.change)
        ProposalField("Motivazione", proposal.rationale)
        ProposalField("Risultato atteso", proposal.expected)
        if (proposal.cautions != null) {
            ProposalField("Controindicazioni", proposal.cautions)
        }

        if (proposal.comparison.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "CONFRONTO PROGRAMMA",
                style = FitTextStyles.caption,
                color = TextLow,
            )
            proposal.comparison.forEach { diff ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = diff.label, style = FitTextStyles.caption, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = diff.before,
                            style = FitTextStyles.bodySmall,
                            color = TextLow,
                            textDecoration = TextDecoration.LineThrough,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "→", style = FitTextStyles.bodySmall, color = TextLow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = diff.after,
                            style = FitTextStyles.bodyStrong,
                            color = Emerald,
                        )
                    }
                }
            }
        }

        if (proposal.status == CoachProposalStatus.PROPOSED) {
            Spacer(modifier = Modifier.height(2.dp))
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

@Composable
private fun ProposalField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), style = FitTextStyles.caption, color = TextLow)
        Text(text = value, style = FitTextStyles.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(FitRadius.md))
            .background(AiSoft)
            .border(width = 1.dp, color = AiBorder, shape = RoundedCornerShape(FitRadius.md))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AiAccent),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Analisi dei dati in corso…",
            style = FitTextStyles.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun CoachInput(
    text: String,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val shape = RoundedCornerShape(FitRadius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FitSurfaceVariant)
            .border(width = 1.dp, color = AiBorder, shape = shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (text.isBlank()) {
                Text(
                    text = "Chiedi al Coach…",
                    style = FitTextStyles.bodySmall,
                    color = TextLow,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onInputChange,
                textStyle = FitTextStyles.bodySmall.copy(color = TextPrimary),
                cursorBrush = SolidColor(AiAccent),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (canSend) {
                        Brush.linearGradient(listOf(AiAccent, AiAccentAlt))
                    } else {
                        SolidColor(FitOutline)
                    },
                )
                .clickable(enabled = canSend) { onSend() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "↑",
                style = FitTextStyles.numericS,
                color = if (canSend) TextPrimary else TextLow,
            )
        }
    }
}

private fun statusLabel(status: CoachStatus): String = when (status) {
    CoachStatus.READY -> "Pronto"
    CoachStatus.THINKING -> "Analisi"
    CoachStatus.OFFLINE -> "Offline"
    CoachStatus.NO_PROVIDER -> "Da configurare"
}

private fun statusColor(status: CoachStatus) = when (status) {
    CoachStatus.READY -> SuccessGreen
    CoachStatus.THINKING -> AiAccent
    CoachStatus.OFFLINE -> OfflineGray
    CoachStatus.NO_PROVIDER -> WarningAmber
}
