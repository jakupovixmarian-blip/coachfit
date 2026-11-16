package com.fitcoach.app.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.MacroBar
import com.fitcoach.app.ui.components.MacroLegend
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.ProgressRing
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.HighCarbColor
import com.fitcoach.app.ui.theme.InfoBlue
import com.fitcoach.app.ui.theme.LowCarbColor
import com.fitcoach.app.ui.theme.MediumCarbColor
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/* =============================================================================
 * Diario alimentare — calorie, macro, acqua e voci registrate per pasto.
 * Ogni azione è collegata a funzioni esistenti: aggiunta voce, rimozione,
 * aggiornamento acqua. Nessun dato fittizio: se non ci sono voci, stato vuoto.
 * ========================================================================== */

@Composable
fun DiaryContent(
    state: DiaryUiState,
    onAddFood: () -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onWaterChange: (Int) -> Unit,
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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Diario", style = FitTextStyles.display, color = TextPrimary)
                Text(
                    text = state.dateLabel,
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                )
            }
            Pill(text = state.dayType, color = dayTypeColor(state.dayType))
        }

        when {
            state.isLoading -> {
                LoadingBox()
                LoadingBox()
            }

            error != null -> EmptyState(
                title = "Diario non disponibile",
                message = error,
                actionLabel = "Riprova",
                onAction = onRetry,
            )

            state.kcalTarget == 0 && !state.hasEntries -> EmptyState(
                title = "Nessun piano attivo",
                message = "Il diario si aggancia al piano alimentare attivo. Completa l'anamnesi per generare il piano e iniziare a registrare.",
                actionLabel = "Torna all'anamnesi",
                onAction = onRetry,
            )

            else -> {
                CalorieCard(state)
                MacroCard(state)
                WaterCard(state, onWaterChange)

                if (state.hasEntries) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "Pasti di oggi",
                            subtitle = "Tocca una voce per rimuoverla",
                        )
                        state.meals.filter { it.entries.isNotEmpty() }.forEach { meal ->
                            MealCard(meal = meal, onRemoveEntry = onRemoveEntry)
                        }
                    }
                } else {
                    EmptyState(
                        title = "Diario vuoto",
                        message = "Registra il primo alimento per far partire il conteggio di calorie e macro.",
                        actionLabel = "Aggiungi alimento",
                        onAction = onAddFood,
                    )
                }

                PrimaryButton(text = "Aggiungi alimento", onClick = onAddFood)
            }
        }
    }
}

@Composable
private fun CalorieCard(state: DiaryUiState) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProgressRing(
                    progress = state.kcalProgress,
                    size = 132.dp,
                    strokeWidth = 11.dp,
                    color = if (state.kcalConsumed > state.kcalTarget) ErrorRed else Emerald,
                    center = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.kcalRemaining}",
                                style = FitTextStyles.numericL,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "kcal rimaste",
                                style = FitTextStyles.caption,
                                color = TextLow,
                                textAlign = TextAlign.Center,
                            )
                        }
                    },
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DataRow(label = "Obiettivo", value = "${state.kcalTarget} kcal")
                    DataRow(label = "Consumate", value = "${state.kcalConsumed} kcal")
                    DataRow(
                        label = "Giornata",
                        value = state.dayType,
                        valueColor = dayTypeColor(state.dayType),
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroCard(state: DiaryUiState) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Macronutrienti", style = FitTextStyles.bodyStrong, color = TextPrimary)
            MacroBar(
                proteinGrams = state.proteinGrams,
                carbsGrams = state.carbsGrams,
                fatGrams = state.fatGrams,
            )
            MacroLegend(
                proteinGrams = state.proteinGrams,
                carbsGrams = state.carbsGrams,
                fatGrams = state.fatGrams,
            )
            Text(
                text = "Target: P ${state.proteinTarget.toInt()} g · C ${state.carbsTarget.toInt()} g · G ${state.fatTarget.toInt()} g",
                style = FitTextStyles.caption,
                color = TextLow,
            )
        }
    }
}

@Composable
private fun WaterCard(state: DiaryUiState, onWaterChange: (Int) -> Unit) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Idratazione", style = FitTextStyles.bodyStrong, color = TextPrimary)
                    Text(
                        text = "${state.waterGlasses} / ${state.waterTarget} bicchieri",
                        style = FitTextStyles.caption,
                        color = TextSecondary,
                    )
                }
                GhostButton(
                    text = "−",
                    onClick = { onWaterChange((state.waterGlasses - 1).coerceAtLeast(0)) },
                    enabled = state.waterGlasses > 0,
                )
                Spacer(modifier = Modifier.width(8.dp))
                GhostButton(
                    text = "+",
                    onClick = { onWaterChange(state.waterGlasses + 1) },
                    enabled = state.waterGlasses < state.waterTarget,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(state.waterTarget.coerceAtMost(12)) { index ->
                    val filled = index < state.waterGlasses
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                color = if (filled) InfoBlue else com.fitcoach.app.ui.theme.MacroTrack,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: DiaryMealUi, onRemoveEntry: (Long) -> Unit) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = meal.name, style = FitTextStyles.bodyStrong, color = TextPrimary)
                    Text(text = meal.timeLabel, style = FitTextStyles.caption, color = TextLow)
                }
                Text(
                    text = "${meal.kcal} kcal",
                    style = FitTextStyles.numericS,
                    color = TextPrimary,
                )
            }
            meal.entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRemoveEntry(entry.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.foodName,
                            style = FitTextStyles.bodySmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${entry.quantityLabel} · P ${entry.proteinGrams.toInt()} C ${entry.carbsGrams.toInt()} G ${entry.fatGrams.toInt()}",
                            style = FitTextStyles.caption,
                            color = TextLow,
                        )
                    }
                    Text(
                        text = "${entry.kcal} kcal",
                        style = FitTextStyles.caption,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

private fun dayTypeColor(dayType: String) = when (dayType.uppercase()) {
    "HIGH" -> HighCarbColor
    "LOW" -> LowCarbColor
    else -> MediumCarbColor
}
