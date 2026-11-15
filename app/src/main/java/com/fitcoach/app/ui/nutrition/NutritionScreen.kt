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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.fitcoach.app.domain.nutrition.FoodSubstitutionEngine
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { viewModel.generatePlan() }, enabled = !state.generating) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.generating) "Generazione..." else "Genera piano")
                        }
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
                    MealCard(
                        meal = meal,
                        checks = state.checks,
                        onToggle = viewModel::toggleEntry,
                        onSubstitute = viewModel::startSubstitution
                    )
                }
            }
        }
    }

    state.substitutionEntry?.let { entry ->
        SubstitutionDialog(
            entryName = entry.foodName,
            substitutes = state.substitutes,
            substituteStatuses = state.substituteStatuses,
            substituteEvaluating = state.substituteEvaluating,
            preview = state.preview,
            previewFood = state.previewFood,
            searchQuery = state.foodSearchQuery,
            searchResults = state.foodSearchResults,
            searchLoading = state.foodSearchLoading,
            searchNote = state.foodSearchNote,
            onSearchQueryChange = viewModel::updateFoodSearch,
            onSearch = viewModel::searchFoods,
            onPreview = viewModel::previewSubstitution,
            onApply = viewModel::applySubstitution,
            onDismiss = viewModel::cancelSubstitution
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun MealCard(
    meal: Meal,
    checks: Map<Long, Long>,
    onToggle: (MealEntry) -> Unit,
    onSubstitute: (MealEntry) -> Unit
) {
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
                val checked = checks.containsKey(entry.id)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggle(entry) })
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${entry.foodName} · ${entry.grams.toInt()} g (${unitLabel(entry.unit)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (checked) TextSecondary else MaterialTheme.colorScheme.onSurface
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
    substituteStatuses: Map<Long, FoodSubstitutionEngine.Status>,
    substituteEvaluating: Boolean,
    preview: FoodSubstitutionEngine.Result?,
    previewFood: FoodItem?,
    searchQuery: String,
    searchResults: List<FoodItem>,
    searchLoading: Boolean,
    searchNote: String?,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPreview: (FoodItem) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sostituisci \"$entryName\"") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (preview == null || previewFood == null) {
                    Text(
                        "Le quantita sono ricalcolate per mantenere calorie e macronutrienti del pasto " +
                            "e della giornata. Scegli un sostituto per il controllo preventivo.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    if (substituteEvaluating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Valuto la compatibilita delle alternative…",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    } else if (substitutes.isEmpty()) {
                        Text(
                            "Nessun sostituto compatibile con le tue restrizioni.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val valid = substitutes.count {
                            substituteStatuses[it.id] != FoodSubstitutionEngine.Status.INCOMPATIBLE
                        }
                        Text(
                            if (valid > 0) "$valid varianti applicabili · le altre richiedono un alimento di riequilibrio."
                            else "Nessuna variante diretta: prova a cercare un alimento specifico qui sotto.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        substitutes.forEach { food ->
                            SubstituteRow(food, substituteStatuses[food.id], onPreview)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Cerca un alimento",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Es. yogurt greco, bresaola…") }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onSearch, enabled = !searchLoading) {
                            if (searchLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Cerca")
                        }
                    }
                    searchNote?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (searchResults.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        searchResults.forEach { food ->
                            Column {
                                Text(
                                    food.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${food.source}${food.brand?.let { " · $it" } ?: ""}",
                                    style = MaterialTheme.typography.labelSmall, color = TextSecondary
                                )
                                SubstituteRow(food, null, onPreview)
                            }
                        }
                    }
                } else {
                    SubstitutionPreview(previewFood, preview)
                }
            }
        },
        confirmButton = {
            if (preview != null && previewFood != null) {
                val enabled = preview.status != FoodSubstitutionEngine.Status.INCOMPATIBLE
                TextButton(onClick = onApply, enabled = enabled) {
                    Text(if (enabled) "Applica" else "Non compatibile")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@Composable
private fun SubstituteRow(
    food: FoodItem,
    status: FoodSubstitutionEngine.Status?,
    onPreview: (FoodItem) -> Unit
) {
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
            status?.let {
                val (label, color) = when (it) {
                    FoodSubstitutionEngine.Status.COMPATIBLE -> "Compatibile" to Emerald
                    FoodSubstitutionEngine.Status.COMPATIBLE_WITH_ADJUSTMENT ->
                        "Compatibile con aggiunta" to CarbsColor
                    FoodSubstitutionEngine.Status.INCOMPATIBLE -> "Non compatibile" to TextSecondary
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
        TextButton(onClick = { onPreview(food) }) { Text("Verifica") }
    }
}

@Composable
private fun SubstitutionPreview(food: FoodItem, preview: FoodSubstitutionEngine.Result) {
    val statusText = when (preview.status) {
        FoodSubstitutionEngine.Status.COMPATIBLE -> "Compatibile"
        FoodSubstitutionEngine.Status.COMPATIBLE_WITH_ADJUSTMENT -> "Compatibile con modifica aggiuntiva"
        FoodSubstitutionEngine.Status.INCOMPATIBLE -> "Non compatibile con i vincoli del piano"
    }
    val statusColor = when (preview.status) {
        FoodSubstitutionEngine.Status.COMPATIBLE -> Emerald
        FoodSubstitutionEngine.Status.COMPATIBLE_WITH_ADJUSTMENT -> CarbsColor
        FoodSubstitutionEngine.Status.INCOMPATIBLE -> CarbsColor
    }
    Column {
        Text(food.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Pill(statusText, statusColor)
        Spacer(Modifier.height(10.dp))
        Text("Quantità sostitutiva: ${preview.substituteGrams.toInt()} g", style = MaterialTheme.typography.bodyMedium, color = Emerald)
        preview.addition?.let {
            Text(
                "Aggiungi ${it.grams.toInt()} g di ${it.food.name}",
                style = MaterialTheme.typography.bodyMedium, color = CarbsColor
            )
        }
        Spacer(Modifier.height(10.dp))
        PreviewRow("Calorie pasto", preview.mealBefore.kcal, preview.mealAfter.kcal)
        PreviewRow("Proteine pasto", preview.mealBefore.protein, preview.mealAfter.protein)
        PreviewRow("Carboidrati pasto", preview.mealBefore.carbs, preview.mealAfter.carbs)
        PreviewRow("Grassi pasto", preview.mealBefore.fat, preview.mealAfter.fat)
        Spacer(Modifier.height(6.dp))
        PreviewRow("Calorie giornata", preview.dayBefore.kcal, preview.dayAfter.kcal)
        if (preview.violations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            preview.violations.forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = CarbsColor)
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, before: Double, after: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            "${before.toInt()} → ${after.toInt()}",
            style = MaterialTheme.typography.bodySmall,
            color = if (kotlin.math.abs(after - before) <= kotlin.math.abs(before) * 0.05 + 1) Emerald else CarbsColor
        )
    }
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
