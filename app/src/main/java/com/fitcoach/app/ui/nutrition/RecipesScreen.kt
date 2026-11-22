package com.fitcoach.app.ui.nutrition

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.data.local.entity.SavedRecipeEntity
import com.fitcoach.app.data.repository.SavedIngredient
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.ComposedRecipe
import com.fitcoach.app.domain.nutrition.Recipe
import com.fitcoach.app.domain.nutrition.RecipeStyle
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.OnResumeRefresh
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Ricette in tre schede.
 *
 * "Dal piano" elenca le ricette i cui ingredienti sono presenti nel pasto del
 * piano. "Crea ricetta" compone una ricetta dagli alimenti reali del pasto
 * scelto, con le grammature del piano, e la mostra prima di salvarla.
 * "Salvate" raccoglie le ricette create dall'utente.
 */
@Composable
fun RecipesScreen(
    navController: NavHostController,
    startOnCreate: Boolean = false,
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Piano e permessi cambiano fuori da questa schermata.
    OnResumeRefresh {
        viewModel.load()
    }
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(if (startOnCreate) 1 else 0) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.composeMessage) {
        state.composeMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeComposeMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = TextSecondary)
            }
            Column {
                Text("Ricette", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Dagli alimenti del tuo piano", style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
            }
        }

        if (state.loading) {
            LoadingBox(Modifier.fillMaxSize())
            return@Column
        }

        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Emerald
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Dal piano") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Crea ricetta") })
            Tab(selected = tab == 2, onClick = { tab = 2 },
                text = { Text("Salvate (${state.saved.size})") })
        }

        when (tab) {
            0 -> PlanRecipesTab(state, viewModel)
            1 -> CreateRecipeTab(state, viewModel)
            else -> SavedRecipesTab(state, viewModel)
        }
    }

    SnackbarHost(snackbar)
}

@Composable
private fun DaySelector(state: RecipesUiState, viewModel: RecipesViewModel) {
    if (state.dayCount <= 1) return
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items((0 until state.dayCount).toList()) { index ->
            FilterChip(
                selected = index == state.dayIndex,
                onClick = { viewModel.selectDay(index) },
                label = { Text("Giorno ${index + 1}") }
            )
        }
    }
}

@Composable
private fun PlanRecipesTab(state: RecipesUiState, viewModel: RecipesViewModel) {
    Column {
        DaySelector(state, viewModel)
        if (state.availableSlots.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(state.availableSlots) { slot ->
                    FilterChip(
                        selected = slot == state.selectedSlot,
                        onClick = { viewModel.selectSlot(slot) },
                        label = { Text(slot.label) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // La ricetta scritta dall'AI va per prima: i template sotto sono
            // un'alternativa, non il risultato principale.
            if (state.aiGenerating) {
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                color = Emerald,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Scrivo la ricetta per ${state.selectedSlot.label.lowercase()}...",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
            state.aiRecipe?.let { ai ->
                item {
                    Column {
                        Text("Ricetta del coach", style = MaterialTheme.typography.titleSmall,
                            color = Emerald)
                        Spacer(Modifier.height(6.dp))
                        RecipeCard(ai)
                    }
                }
            }
            state.aiNote?.let { note ->
                item {
                    Text(note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            if (state.recipes.isNotEmpty()) {
                item {
                    Text(
                        "Altre combinazioni dagli alimenti del piano",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                }
            }

            if (state.recipes.isEmpty()) {
                item {
                    SectionCard {
                        Column {
                            Text(
                                "Nessuna ricetta per ${state.selectedSlot.label.lowercase()}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Le ricette si costruiscono sugli alimenti previsti dal piano. " +
                                    "Puoi crearne una tu dagli alimenti di questo pasto nella " +
                                    "scheda \"Crea ricetta\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            items(state.recipes, key = { it.id }) { recipe ->
                RecipeCard(recipe)
            }
        }
    }
}

/**
 * Composizione di una ricetta dagli alimenti del pasto.
 *
 * Gli slot proposti sono quelli che il profilo prevede davvero: con cinque
 * pasti compaiono anche gli spuntini, con tre no.
 */
@Composable
private fun CreateRecipeTab(state: RecipesUiState, viewModel: RecipesViewModel) {
    val slots = state.profileSlots.ifEmpty { state.availableSlots }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            SectionCard {
                Column {
                    Text("Da quale pasto parti?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "La ricetta usa solo gli alimenti che il piano assegna a quel pasto, " +
                            "con le grammature gia calcolate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    if (slots.isEmpty()) {
                        Text("Nessun pasto disponibile nel piano attivo.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(slots) { slot ->
                                FilterChip(
                                    selected = state.draftSlot == slot,
                                    onClick = { viewModel.compose(slot) },
                                    label = { Text(slot.label) }
                                )
                            }
                        }
                    }
                }
            }
        }

        val draft = state.draft
        if (draft != null) {
            item {
                SectionCard {
                    Column {
                        Text("Stile", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(RecipeStyle.entries.toList()) { style ->
                                FilterChip(
                                    selected = state.draftStyle == style,
                                    onClick = { viewModel.setDraftStyle(style) },
                                    label = { Text(style.label) }
                                )
                            }
                        }
                    }
                }
            }
            item { DraftCard(draft) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = { viewModel.saveDraft() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Salva ricetta")
                    }
                    OutlinedButton(onClick = { viewModel.discardDraft() },
                        modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Scarta")
                    }
                }
            }
        } else if (state.draftSlot == null) {
            item {
                SectionCard {
                    Text(
                        "Scegli un pasto qui sopra: l'app compone la ricetta con gli alimenti " +
                            "di quel pasto e te la mostra prima di salvarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftCard(draft: ComposedRecipe) {
    SectionCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.RestaurantMenu, contentDescription = null, tint = Emerald)
                Spacer(Modifier.width(8.dp))
                Text(draft.name, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "%d kcal · P %.0f g · C %.0f g · G %.0f g".format(
                    draft.kcal.toInt(), draft.protein, draft.carbs, draft.fat
                ),
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            draft.ingredients.forEach { e ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(e.foodName, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("%.0f g".format(e.grams),
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            if (draft.steps.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Preparazione", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                draft.steps.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary, modifier = Modifier.padding(vertical = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun SavedRecipesTab(state: RecipesUiState, viewModel: RecipesViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (state.saved.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        "Nessuna ricetta salvata. Creane una nella scheda \"Crea ricetta\": " +
                            "restera qui anche quando il piano cambia.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
            }
        }
        items(state.saved, key = { it.id }) { entity ->
            SavedRecipeCard(
                entity = entity,
                ingredients = viewModel.ingredientsOf(entity),
                steps = viewModel.stepsOf(entity),
                onDelete = { viewModel.deleteSaved(entity.id) }
            )
        }
    }
}

@Composable
private fun SavedRecipeCard(
    entity: SavedRecipeEntity,
    ingredients: List<SavedIngredient>,
    steps: List<String>,
    onDelete: () -> Unit
) {
    SectionCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entity.name, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("${slotLabel(entity.slot)} · giorno ${entity.dayIndex + 1}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Elimina ricetta",
                        tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "%d kcal · P %.0f g · C %.0f g · G %.0f g".format(
                    entity.kcal.toInt(), entity.protein, entity.carbs, entity.fat
                ),
                style = MaterialTheme.typography.bodySmall, color = Emerald
            )
            Spacer(Modifier.height(8.dp))
            ingredients.forEach { i ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(i.name, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("%.0f g".format(i.grams),
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                steps.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary, modifier = Modifier.padding(vertical = 1.dp))
                }
            }
        }
    }
}

private fun slotLabel(raw: String): String =
    MealSlot.entries.firstOrNull { it.name == raw }?.label ?: raw

@Composable
private fun RecipeCard(recipe: Recipe) {
    SectionCard {
        Column {
            Text(recipe.name, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "%d kcal · P %.0f g · C %.0f g · G %.0f g".format(
                    recipe.kcal.toInt(), recipe.protein, recipe.carbs, recipe.fat
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            recipe.ingredients.forEach { ingredient ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        ingredient.foodName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%.0f g".format(ingredient.grams),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            if (recipe.steps.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Preparazione", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                recipe.steps.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
            recipe.notes?.let { note ->
                Spacer(Modifier.height(8.dp))
                Text(note, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}
