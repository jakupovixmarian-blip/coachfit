package com.fitcoach.app.ui.nutrition

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.MacroBar
import com.fitcoach.app.ui.components.MacroLegend
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.HighCarbColor
import com.fitcoach.app.ui.theme.LowCarbColor
import com.fitcoach.app.ui.theme.MediumCarbColor
import com.fitcoach.app.ui.theme.TextSecondary

private val dayNames = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

@Composable
fun NutritionScreen(
    navController: NavHostController,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    if (state.plan == null) {
        Column(Modifier.fillMaxSize()) {
            EmptyState(
                title = "Nessun piano alimentare",
                message = "Genera un piano completo a 7 giorni con ciclicita dei carboidrati, " +
                    "in base al tuo profilo e ai tuoi obiettivi.",
                action = {
                    Button(onClick = { viewModel.generatePlan() }, enabled = !state.generating) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.generating) "Generazione..." else "Genera piano")
                    }
                }
            )
            SnackbarHost(snackbar)
        }
        return
    }

    val plan = state.plan!!
    val day = state.selectedDay

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Alimentazione", plan.name)
                IconButton(onClick = { viewModel.generatePlan() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Rigenera", tint = Emerald)
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Ciclicita dei carboidrati", "Bilancio settimanale")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill("HIGH ${plan.highCarbDays}", HighCarbColor)
                        Pill("MEDIUM ${plan.mediumCarbDays}", MediumCarbColor)
                        Pill("LOW ${plan.lowCarbDays}", LowCarbColor)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "BMR ${plan.bmr.toInt()} kcal · TDEE ${plan.tdee.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Text(
                        "Bilancio settimanale: ${"%+.0f".format(plan.weeklyKcalBalance)} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (plan.weeklyKcalBalance < 0) CarbsColor else Emerald
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                plan.days.forEach { d ->
                    FilterChip(
                        selected = d.dayIndex == state.selectedDayIndex,
                        onClick = { viewModel.selectDay(d.dayIndex) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dayNames.getOrElse(d.dayIndex) { "?" })
                                Text(d.dayType.label.take(3), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }
            }
        }

        if (day != null) {
            item {
                SectionCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                "Giorno ${day.dayIndex + 1}",
                                "Target: ${day.targetKcal.toInt()} kcal"
                            )
                            Pill(day.dayType.label, carbColor(day.dayType))
                        }
                        Spacer(Modifier.height(12.dp))
                        MacroBar(day.actualProtein, day.actualCarbs, day.actualFat)
                        Spacer(Modifier.height(8.dp))
                        MacroLegend(day.actualProtein, day.actualCarbs, day.actualFat)
                        Spacer(Modifier.height(6.dp))
                        val delta = day.actualKcal - day.targetKcal
                        Text(
                            "Totale: ${day.actualKcal.toInt()} kcal (scostamento ${"%+d".format(delta.toInt())} kcal)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (kotlin.math.abs(delta) <= 120) Emerald else CarbsColor
                        )
                    }
                }
            }

            day.meals.forEach { meal ->
                item {
                    MealCard(meal = meal, onSubstitute = viewModel::startSubstitution)
                }
            }
        }
    }

    state.substitutionEntry?.let { entry ->
        SubstitutionDialog(
            entryName = entry.foodName,
            substitutes = state.substitutes,
            onSelect = viewModel::applySubstitution,
            onDismiss = viewModel::cancelSubstitution
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun MealCard(meal: Meal, onSubstitute: (MealEntry) -> Unit) {
    SectionCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        meal.slot.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(meal.time, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text("${meal.kcal.toInt()} kcal", style = MaterialTheme.typography.labelLarge, color = Emerald)
            }
            Spacer(Modifier.height(8.dp))
            meal.entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${entry.foodName} · ${entry.grams.toInt()} g (${unitLabel(entry.unit)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${entry.kcal.toInt()} kcal · P ${entry.protein.toInt()} · C ${entry.carbs.toInt()} · G ${entry.fat.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = { onSubstitute(entry) }) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Sostituisci", tint = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Totale pasto: P ${meal.protein.toInt()} g · C ${meal.carbs.toInt()} g · G ${meal.fat.toInt()} g",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SubstitutionDialog(
    entryName: String,
    substitutes: List<FoodItem>,
    onSelect: (FoodItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sostituisci \"$entryName\"") },
        text = {
            Column {
                Text(
                    "Le quantita sono ricalcolate per mantenere l'apporto del macro dominante.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                if (substitutes.isEmpty()) {
                    Text(
                        "Nessun sostituto compatibile con le tue restrizioni.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    substitutes.forEach { food ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    food.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${food.kcalPer100g.toInt()} kcal/100g · P ${food.proteinPer100g.toInt()} · C ${food.carbsPer100g.toInt()} · G ${food.fatPer100g.toInt()}",
                                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                                )
                            }
                            TextButton(onClick = { onSelect(food) }) { Text("Scegli") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

private fun unitLabel(unit: FoodUnit) = when (unit) {
    FoodUnit.RAW -> "crudo"
    FoodUnit.COOKED -> "cotto"
    FoodUnit.AS_SOLD -> "tal quale"
}

private fun carbColor(type: CarbDayType) = when (type) {
    CarbDayType.HIGH -> HighCarbColor
    CarbDayType.MEDIUM -> MediumCarbColor
    CarbDayType.LOW -> LowCarbColor
}
