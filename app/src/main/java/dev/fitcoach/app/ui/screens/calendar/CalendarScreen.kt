package dev.fitcoach.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.SectionTitle
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var moving by remember { mutableStateOf<WorkoutSession?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Calendario", style = MaterialTheme.typography.headlineSmall) }

        item {
            FitCard {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { viewModel.previousMonth() }) { Text("<") }
                        Text(
                            state.month.month.getDisplayName(TextStyle.FULL, Locale.ITALY)
                                .replaceFirstChar { it.uppercase() } + " " + state.month.year,
                            style = MaterialTheme.typography.titleLarge
                        )
                        OutlinedButton(onClick = { viewModel.nextMonth() }) { Text(">") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom").forEach { label ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val days = state.gridDays()
                    days.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                DayCell(
                                    date = date,
                                    inMonth = date.month == state.month.month,
                                    selected = date == state.selectedDate,
                                    sessions = state.sessionsOn(date),
                                    onClick = { viewModel.selectDate(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            FitCard {
                Column {
                    SectionTitle("${DateUtils.dayName(state.selectedDate)} ${DateUtils.format(state.selectedDate)}")
                    if (state.selectedSessions.isEmpty()) {
                        Text(
                            "Nessuna seduta in questa data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.selectedSessions.forEach { session ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(session.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        session.status.label(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (session.status != SessionStatus.COMPLETED) {
                                    Row {
                                        TextButton(onClick = { moving = session }) { Text("Sposta") }
                                        if (session.status == SessionStatus.SKIPPED) {
                                            TextButton(onClick = { viewModel.restoreSession(session) }) { Text("Ripristina") }
                                        } else {
                                            TextButton(onClick = { viewModel.skipSession(session) }) { Text("Salta") }
                                        }
                                        TextButton(onClick = { viewModel.deleteSession(session) }) { Text("X") }
                                    }
                                } else {
                                    Text("Completata", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (state.program.isEmpty()) {
                        Button(onClick = { viewModel.addSession() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Aggiungi seduta in questa data")
                        }
                    } else {
                        Text(
                            "Aggiungi una seduta del programma in questa data:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.program.forEach { routine ->
                            OutlinedButton(
                                onClick = { viewModel.addSession(routine.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(routine.dayLabel.ifBlank { routine.name })
                            }
                        }
                    }
                }
            }
        }

        state.message?.let { msg ->
            item {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    moving?.let { session ->
        AlertDialog(
            onDismissRequest = { moving = null },
            title = { Text("Sposta seduta") },
            text = {
                Column {
                    Text("Scegli la nuova data per \"${session.name}\".", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(-1, 0, 1, 2, 7).forEach { offset ->
                            val target = state.selectedDate.plusDays(offset.toLong())
                            OutlinedButton(onClick = {
                                viewModel.moveSession(session, target)
                                moving = null
                            }) {
                                Text(if (offset == 0) "Oggi" else "+${offset}g")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Seleziona un altro giorno sul calendario per spostare la seduta in una data specifica.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { moving = null }) { Text("Chiudi") } }
        )
    }
}

private fun SessionStatus.label(): String = when (this) {
    SessionStatus.PLANNED -> "Pianificata"
    SessionStatus.IN_PROGRESS -> "In corso"
    SessionStatus.COMPLETED -> "Completata"
    SessionStatus.SKIPPED -> "Saltata"
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    sessions: List<WorkoutSession>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (inMonth) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            if (sessions.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                sessions.any { it.status == SessionStatus.COMPLETED } -> MaterialTheme.colorScheme.primary
                                sessions.any { it.status == SessionStatus.IN_PROGRESS } -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        .height(6.dp)
                        .fillMaxWidth(0.25f)
                )
            }
        }
    }
}
