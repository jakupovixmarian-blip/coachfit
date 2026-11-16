package com.fitcoach.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/* ============================================================================
 * Modifica profilo — sola presentazione. I valori arrivano già calcolati dal
 * motore deterministico (calorie, macro, frequenza): qui non si calcola nulla
 * e non si scrive sul database. Ogni azione richiama una funzione esistente.
 * ========================================================================== */

data class ProfileUiState(
    val name: String = "",
    val goalLabel: String = "",
    val experienceLabel: String = "",
    val ageLabel: String = "",
    val heightLabel: String = "",
    val weightLabel: String = "",
    val daysPerWeek: Int = 0,
    val equipmentLabel: String = "",
    val kcalTargetLabel: String = "",
    val macroSummary: String = "",
    val lastUpdatedLabel: String? = null,
)

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onEditAnamnesis: () -> Unit,
    onUpdateWeight: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(text = "Profilo", style = FitTextStyles.display, color = TextPrimary)
            Text(
                text = "Dati del profilo e del piano attivo",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        SectionHeader(title = "Dati personali")
        SectionCard {
            Column {
                DataRow(label = "Nome", value = state.name)
                DataRow(label = "Età", value = state.ageLabel)
                DataRow(label = "Altezza", value = state.heightLabel)
                DataRow(label = "Peso", value = state.weightLabel)
            }
        }

        SectionHeader(
            title = "Obiettivo e programma",
            subtitle = "Decisi dal motore, non impostati a mano",
        )
        SectionCard {
            Column {
                DataRow(label = "Obiettivo", value = state.goalLabel)
                DataRow(label = "Esperienza", value = state.experienceLabel)
                DataRow(label = "Giorni a settimana", value = "${state.daysPerWeek}")
                DataRow(label = "Attrezzatura", value = state.equipmentLabel)
            }
        }

        SectionHeader(title = "Target energetico")
        SectionCard {
            Column {
                DataRow(
                    label = "Kcal target",
                    value = state.kcalTargetLabel,
                    valueColor = Emerald,
                )
                DataRow(label = "Macronutrienti", value = state.macroSummary)
                state.lastUpdatedLabel?.let { label ->
                    DataRow(label = "Ultimo aggiornamento", value = label)
                }
            }
        }

        PrimaryButton(text = "Modifica anamnesi", onClick = onEditAnamnesis)
        GhostButton(text = "Aggiorna peso", onClick = onUpdateWeight)
        GhostButton(text = "Apri progressi", onClick = onOpenProgress)
    }
}
