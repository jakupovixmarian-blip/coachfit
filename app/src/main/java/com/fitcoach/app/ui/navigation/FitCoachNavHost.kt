package com.fitcoach.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.fitcoach.app.ui.anamnesis.AnamnesisScreen
import com.fitcoach.app.ui.coach.CoachScreen
import com.fitcoach.app.ui.dashboard.DashboardScreen
import com.fitcoach.app.ui.diary.DiaryScreen
import com.fitcoach.app.ui.nutrition.NutritionScreen
import com.fitcoach.app.ui.onboarding.OnboardingScreen
import com.fitcoach.app.ui.progress.ProgressScreen
import com.fitcoach.app.ui.settings.AiProvidersScreen
import com.fitcoach.app.ui.settings.DevicesScreen
import com.fitcoach.app.ui.settings.KnowledgeScreen
import com.fitcoach.app.ui.settings.NotificationSettingsScreen
import com.fitcoach.app.ui.settings.ProfileEditScreen
import com.fitcoach.app.ui.settings.SettingsScreen
import com.fitcoach.app.ui.shopping.ShoppingScreen
import com.fitcoach.app.ui.workout.CustomWorkoutScreen
import com.fitcoach.app.ui.workout.WorkoutScreen
import dagger.hilt.android.EntryPointAccessors

@Composable
fun FitCoachNavHost(
    navController: NavHostController,
    pendingRoute: String? = null,
    onRouteConsumed: () -> Unit = {}
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val context = LocalContext.current
    // null = profilo non ancora letto: il NavHost non va creato prima, altrimenti
    // la destinazione iniziale sarebbe gia' stata fissata.
    var startDestination by remember { mutableStateOf<String?>(null) }
    // Deciso una volta sola insieme alla destinazione iniziale. Rileggerlo nel
    // callback di fine setup richiederebbe una coroutine, e navigare da li' ha
    // gia' fatto crashare il test ("setCurrentState must be called on the main
    // thread"): la decisione presa all'avvio e' la stessa e non costa nulla.
    var startNeedsOnboarding by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Se la lettura fallisce si ripiega sull'onboarding: un errore del
        // database non deve lasciare l'app su una schermata vuota per sempre.
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            UserProfileEntryPoint::class.java
        )
        val profile = runCatching { entryPoint.userRepository().get() }.getOrNull()
        val setupSkipped = runCatching { entryPoint.setupStateStore().isAiSetupSkipped() }
            .getOrDefault(false)
        // resolveConfigured(), non resolve(): quest'ultima restituisce sempre una
        // configurazione (con chiave vuota) e farebbe risultare "pronto" un
        // provider mai impostato.
        val providerReady = runCatching {
            entryPoint.aiConfigResolver().resolveConfigured() != null
        }.getOrDefault(false)

        startNeedsOnboarding = profile == null || !profile.isComplete
        startDestination = when {
            // Il provider viene PRIMA delle domande: senza di lui nessun piano si
            // puo' generare, e chiederlo a valle significava raccogliere eta', peso
            // e allergie per poi bloccare l'utente alla fine. Chi ha scelto
            // "Configura piu' tardi" non se lo vede richiedere a ogni avvio.
            !providerReady && !setupSkipped -> Routes.AI_SETUP
            startNeedsOnboarding -> Routes.ONBOARDING
            else -> Routes.DASHBOARD
        }
    }

    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        if (route in TopLevelDestination.entries.map { it.route }) {
            navController.navigateTopLevel(route)
            onRouteConsumed()
        }
    }

    Scaffold(
        // Trasparente: lascia vedere lo sfondo ambientale Hyperion disegnato in MainActivity.
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                FitCoachBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        val start = startDestination
        if (start == null) {
            // Attesa della lettura del profilo: dura una query, nessun contenuto da mostrare.
            return@Scaffold
        }
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(180)) }
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen(navController) }
            composable(Routes.NUTRITION) { NutritionScreen(navController) }
            composable(Routes.SHOPPING) { ShoppingScreen(navController) }
            composable(Routes.DIARY) { DiaryScreen(navController) }
            composable(Routes.WORKOUT) { WorkoutScreen(navController) }
            composable(Routes.CUSTOM_WORKOUT) { CustomWorkoutScreen(navController) }
            composable(
                route = Routes.CUSTOM_WORKOUT_EDIT,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                CustomWorkoutScreen(
                    navController = navController,
                    editPlanId = entry.arguments?.getLong("planId")
                )
            }
            composable(Routes.PROGRESS) { ProgressScreen(navController) }
            composable(Routes.COACH) { CoachScreen(navController) }
            composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
            composable(Routes.ANAMNESIS) { AnamnesisScreen(navController) }
            composable(Routes.ANAMNESIS_EDIT) { AnamnesisScreen(navController, startInEdit = true) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
            composable(Routes.AI_PROVIDERS) { AiProvidersScreen(navController) }
            composable(Routes.AI_SETUP) {
                AiProvidersScreen(
                    navController = navController,
                    setupMode = true,
                    onSetupDone = {
                        // Primo avvio: dopo il provider si raccolgono i dati, perche'
                        // senza profilo non c'e' nulla da mostrare. Utente gia'
                        // esistente (rientro dal setup): torna alla Dashboard.
                        val route = if (startNeedsOnboarding) Routes.ONBOARDING
                        else Routes.DASHBOARD
                        navController.navigate(route) {
                            popUpTo(Routes.AI_SETUP) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.NOTIFICATION_SETTINGS) { NotificationSettingsScreen(navController) }
            composable(Routes.PROFILE_EDIT) { ProfileEditScreen(navController) }
            composable(Routes.KNOWLEDGE) { KnowledgeScreen(navController) }
            composable(Routes.DEVICES) { DevicesScreen(navController) }
        }
    }
}
