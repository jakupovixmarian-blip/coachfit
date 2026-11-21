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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.Recipe
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Ricette della giornata, filtrate per pasto.
 *
 * Le ricette non sono un ricettario generico: ognuna usa gli alimenti che il
 * piano assegna davvero a quel pasto, con le grammature calcolate dal motore
 * nutrizionale. Se un pasto non ha alimenti compatibili, non compare.
 */
@Composable
fun RecipesScreen(
    navController: NavHostController,
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
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
                                    "Se il pasto usa alimenti non coperti da una ricetta, li trovi " +
                                    "comunque nel piano alimentare.",
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

    SnackbarHost(snackbar)
}

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
