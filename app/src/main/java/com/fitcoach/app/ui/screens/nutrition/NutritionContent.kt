package com.fitcoach.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.AnimatedBar
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
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FatColor
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.HighCarbColor
import com.fitcoach.app.ui.theme.LowCarbColor
import com.fitcoach.app.ui.theme.MediumCarbColor
import com.fitcoach.app.ui.theme.ProteinColor
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/**
 * Alimentazione — schermata completa.
 *
 * Composabile stateless: riceve [NutritionUiState] e restituisce eventi.
 * Nessun pulsante decorativo: ogni azione chiama la funzione già esistente
 * (diario, piano settimanale, lista spesa, sostituzione alimento).
 */
@Composable
fun NutritionContent(
    state: NutritionUiState,
    onOpenDiary: () -> Unit,
    onOpenWeeklyPlan: () -> Unit,
    onOpenShopping: () -> Unit,
    onSubstitute: (Long) -> Unit,
    onAddFood: (Long) -> Unit,
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
        NutritionHeader(state)

        when {
            state.isLoading -> {
                LoadingBox()
                LoadingBox()
            }

            error != null -> EmptyState(
                title = "Dati non disponibili",
                message = error,
                actionLabel = "Riprova",
                onAction = onRetry,
            )

            !state.hasPlan -> EmptyState(
                title = "Nessun piano alimentare",
                message = "Completa l'anamnesi: calorie e macro vengono calcolati dal motore nutrizionale, non inseriti a mano.",
                actionLabel = "Apri anamnesi",
                onAction = onOpenDiary,
            )

            else -> {
                CalorieCard(state)
                MacroCard(state)
                if (state.week.isNotEmpty()) {
                    WeekStrip(state)
                }
                MealsSection(state, onSubstitute, onAddFood)
                NutritionActions(onOpenDiary, onOpenWeeklyPlan, onOpenShopping)
            }
        }
    }
}

@Composable
private fun NutritionHeader(state: NutritionUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Alimentazione",
                style = FitTextStyles.display,
                color = TextPrimary,
            )
            if (state.dateLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.dateLabel,
                    style = FitTextStyles.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Pill(text = carbDayLabel(state.carbDay), color = carbDayColor(state.carbDay))
    }
}

@Composable
private fun CalorieCard(state: NutritionUiState) {
    val accent = if (state.hasOvershoot) WarningAmber else Emerald

    SectionCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = state.kcalProgress,
                    size = 116.dp,
                    strokeWidth = 10.dp,
                    color = accent,
                    center = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.kcalRemaining.toString(),
                                style = FitTextStyles.numericL,
                                color = TextPrimary,
                            )
                            Text(
                                text = "RIMANENTI",
                                style = FitTextStyles.caption,
                                color = TextLow,
                            )
                        }
                    },
                )
                Spacer(modifier = Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    DataRow(label = "Obiettivo", value = "${state.targetKcal} kcal")
                    DataRow(
                        label = "Consumate",
                        value = "${state.consumedKcal} kcal",
                        valueColor = if (state.hasOvershoot) WarningAmber else TextPrimary,
                    )
                    DataRow(label = "Pasti registrati", value = "${state.loggedMeals}/${state.totalMeals}")
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedBar(fraction = state.kcalProgress, color = accent)
                }
            }
            if (state.hasOvershoot) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "+${state.overshootKcal} kcal oltre l'obiettivo di giornata",
                    style = FitTextStyles.caption,
                    color = WarningAmber,
                )
            }
        }
    }
}

@Composable
private fun MacroCard(state: NutritionUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Macronutrienti", subtitle = "Residui rispetto al target del giorno")
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroBar(
                    proteinGrams = state.consumedProtein.toDouble(),
                    carbsGrams = state.consumedCarbs.toDouble(),
                    fatGrams = state.consumedFat.toDouble(),
                    modifier = Modifier.fillMaxWidth(),
                )
                MacroLegend(
                    proteinGrams = state.targetProtein.toDouble(),
                    carbsGrams = state.targetCarbs.toDouble(),
                    fatGrams = state.targetFat.toDouble(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroStat(
                        label = "P",
                        consumed = state.consumedProtein,
                        target = state.targetProtein,
                        accent = ProteinColor,
                        modifier = Modifier.weight(1f),
                    )
                    MacroStat(
                        label = "C",
                        consumed = state.consumedCarbs,
                        target = state.targetCarbs,
                        accent = CarbsColor,
                        modifier = Modifier.weight(1f),
                    )
                    MacroStat(
                        label = "G",
                        consumed = state.consumedFat,
                        target = state.targetFat,
                        accent = FatColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroStat(
    label: String,
    consumed: Int,
    target: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(FitRadius.sm)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FitSurfaceVariant)
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(text = label, style = FitTextStyles.caption, color = accent)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$consumed",
            style = FitTextStyles.numericS,
            color = TextPrimary,
        )
        Text(
            text = "di $target g",
            style = FitTextStyles.caption,
            color = TextLow,
        )
    }
}

@Composable
private fun WeekStrip(state: NutritionUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Piano settimanale", subtitle = state.weeklyBalanceLabel)
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.week.forEach { day ->
                    val shape = RoundedCornerShape(FitRadius.sm)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (day.isToday) BrandSoft else Color.Transparent)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = day.dayLabel,
                            style = FitTextStyles.caption,
                            color = if (day.isToday) Emerald else TextLow,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.kcal.toString(),
                            style = FitTextStyles.numericS,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = carbDayLabel(day.carbDay).take(1),
                            style = FitTextStyles.caption,
                            color = carbDayColor(day.carbDay),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealsSection(
    state: NutritionUiState,
    onSubstitute: (Long) -> Unit,
    onAddFood: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Pasti del giorno", subtitle = "${state.totalMeals} pasti previsti dal piano")
        state.meals.forEach { meal ->
            MealCard(meal = meal, onSubstitute = onSubstitute, onAddFood = onAddFood)
        }
    }
}

@Composable
private fun MealCard(
    meal: MealUi,
    onSubstitute: (Long) -> Unit,
    onAddFood: (Long) -> Unit,
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = meal.name, style = FitTextStyles.bodyStrong, color = TextPrimary)
                    if (meal.timeLabel != null) {
                        Text(
                            text = meal.timeLabel,
                            style = FitTextStyles.caption,
                            color = TextLow,
                        )
                    }
                }
                Text(
                    text = "${meal.kcal} kcal",
                    style = FitTextStyles.numericS,
                    color = if (meal.isLogged) Emerald else TextSecondary,
                )
            }

            meal.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = FitTextStyles.bodySmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${item.quantityLabel} · P ${item.protein} C ${item.carbs} G ${item.fat}",
                            style = FitTextStyles.caption,
                            color = TextLow,
                        )
                    }
                    Text(
                        text = "${item.kcal}",
                        style = FitTextStyles.numericS,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sostituisci",
                        style = FitTextStyles.caption,
                        color = Emerald,
                        modifier = Modifier.clickable { onSubstitute(item.id) },
                    )
                }
            }

            GhostButton(text = "Aggiungi alimento", onClick = { onAddFood(meal.id) })
        }
    }
}

@Composable
private fun NutritionActions(
    onOpenDiary: () -> Unit,
    onOpenWeeklyPlan: () -> Unit,
    onOpenShopping: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton(text = "Diario alimentare", onClick = onOpenDiary)
        GhostButton(text = "Piano alimentare settimanale", onClick = onOpenWeeklyPlan)
        GhostButton(text = "Lista della spesa", onClick = onOpenShopping)
    }
}

private fun carbDayLabel(kind: CarbDayKind): String = when (kind) {
    CarbDayKind.HIGH -> "HIGH CARB"
    CarbDayKind.MEDIUM -> "MEDIUM CARB"
    CarbDayKind.LOW -> "LOW CARB"
}

private fun carbDayColor(kind: CarbDayKind): Color = when (kind) {
    CarbDayKind.HIGH -> HighCarbColor
    CarbDayKind.MEDIUM -> MediumCarbColor
    CarbDayKind.LOW -> LowCarbColor
}
