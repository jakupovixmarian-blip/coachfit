package com.fitcoach.app.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.ui.navigation.Routes
import com.fitcoach.app.ui.navigation.navigateTopLevel
import com.fitcoach.app.ui.screens.dashboard.DashboardContent

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWeightDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Il ViewModel sopravvive alla navigazione: tornando da Alimentazione dopo
    // aver spuntato alimenti, senza questo la Home restava sui valori vecchi.
    // Inoltre la data puo' essere cambiata (app rimasta aperta oltre mezzanotte).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(event) {
        event?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeEvent()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DashboardContent(
            state = state,
            onOpenNutrition = { navController.navigateTopLevel(Routes.NUTRITION) },
            onOpenWorkout = { navController.navigateTopLevel(Routes.WORKOUT) },
            onOpenProgress = { navController.navigateTopLevel(Routes.PROGRESS) },
            onOpenCoach = { navController.navigateTopLevel(Routes.COACH) },
            onOpenAnamnesis = { navController.navigate(Routes.ANAMNESIS) },
            onLogWeight = { showWeightDialog = true },
            onSkipWorkout = { viewModel.skipTodayWorkout() },
            onGenerateProgram = { viewModel.generateProgram() },
            onRetry = { viewModel.refresh() },
            onOpenRecipes = { navController.navigate(Routes.RECIPES) },
            onOpenHealth = { navController.navigate(Routes.HEALTH) },
            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
        )
    }

    if (showWeightDialog) {
        WeightDialog(
            onDismiss = { showWeightDialog = false },
            onConfirm = { kg ->
                viewModel.logWeight(kg)
                showWeightDialog = false
            }
        )
    }
}

@Composable
private fun WeightDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var input by remember { mutableStateOf("") }
    val parsed = input.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registra il peso") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Peso in kg") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null && parsed in 20.0..400.0
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
