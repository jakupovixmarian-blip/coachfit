package dev.fitcoach.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.ui.components.ActionTile
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.GradientButton
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.NeonBadge
import dev.fitcoach.app.ui.components.ProgressTrack
import dev.fitcoach.app.ui.components.RingProgress
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.theme.CardNavy
import dev.fitcoach.app.ui.theme.GradientCyan
import dev.fitcoach.app.ui.theme.GradientGreen
import dev.fitcoach.app.ui.theme.GradientViolet
import dev.fitcoach.app.ui.theme.HeroGradientEnd
import dev.fitcoach.app.ui.theme.HeroGradientStart
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.NeonGreen
import dev.fitcoach.app.ui.theme.NeonMagenta
import dev.fitcoach.app.ui.theme.NeonOrange
import dev.fitcoach.app.ui.theme.NeonViolet
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow
import dev.fitcoach.app.ui.theme.TextMid

/**
 * Home dashboard del mockup "FitCoach 2.0": header con logo e azioni rapide,
 * hero con saluto e CTA principale, griglia 2x2 di scorciatoie, card "Oggi" con
 * ring delle calorie e card "Obiettivi" con le barre dei macro.
 */
@Composable
fun HomeScreen(
    onOpenWorkout: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartOnboarding: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    val target = profile?.targetCalories()
    val kcalProgress = if (target != null && target > 0) {
        (state.today.kcal / target).toFloat()
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header ---------------------------------------------------------
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeonBadge(icon = Icons.Filled.FitnessCenter, tint = NeonCyan, size = 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "FitCoach 2.0",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text("AI Fitness Coach", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            }
            CircleIconButton(Icons.Filled.NotificationsNone, NeonCyan) { onOpenCalendar() }
            Spacer(Modifier.width(8.dp))
            CircleIconButton(Icons.Filled.ChatBubbleOutline, NeonViolet) { onOpenCoach() }
        }

        // --- Hero -----------------------------------------------------------
        Box(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.5f), Color.Transparent)),
                    MaterialTheme.shapes.large
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "Ciao${profile?.name?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Pronto per l'allenamento?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    DateUtils.format(DateUtils.today()),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLow
                )

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            Format.kcal(state.today.kcal),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan
                        )
                        Text(
                            if (target != null) "di ${Format.kcal(target)} kcal oggi" else "kcal registrate oggi",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMid
                        )
                        Spacer(Modifier.height(10.dp))
                        ProgressTrack(progress = kcalProgress, color = NeonCyan)
                    }
                    Spacer(Modifier.width(16.dp))
                    RingProgress(progress = kcalProgress, size = 74.dp, strokeWidth = 7.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(kcalProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text("oggi", style = MaterialTheme.typography.labelSmall, color = TextLow)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                GradientButton(
                    text = "Inizia allenamento",
                    icon = Icons.Filled.PlayArrow,
                    onClick = onOpenWorkout,
                    gradient = GradientCyan
                )
            }
        }

        if (profile == null || !profile.onboardingCompleted) {
            FitCard {
                Column {
                    SectionTitle("Inizia da qui")
                    Text(
                        "Completa l'anamnesi: l'agente raccoglie i tuoi dati e costruisce scheda e piano.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMid
                    )
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = "Apri anamnesi",
                        onClick = onStartOnboarding,
                        gradient = GradientViolet
                    )
                }
            }
        }

        // --- Griglia 2x2 ----------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(
                title = "Allenamenti",
                subtitle = state.routine?.name ?: "Nessuna scheda",
                icon = Icons.Filled.FitnessCenter,
                tint = NeonCyan,
                onClick = onOpenWorkout,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                title = "Alimentazione",
                subtitle = state.plan?.name ?: "Nessun piano",
                icon = Icons.Filled.Restaurant,
                tint = NeonGreen,
                onClick = onOpenDiary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(
                title = "Progressi",
                subtitle = "${state.weeklySessions} sedute · 7gg",
                icon = Icons.Filled.ShowChart,
                tint = NeonOrange,
                onClick = onOpenProgress,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                title = "AI Coach",
                subtitle = "Consigli e schede",
                icon = Icons.Filled.SmartToy,
                tint = NeonViolet,
                onClick = onOpenCoach,
                modifier = Modifier.weight(1f)
            )
        }

        // --- Oggi -----------------------------------------------------------
        FitCard {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("Oggi")
                        Text(
                            state.routine?.name ?: "Nessun programma attivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            state.lastSession?.name?.let { "Ultima seduta: $it" }
                                ?: "Avvia la prima seduta della settimana",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLow
                        )
                    }
                    RingProgress(
                        progress = (state.weeklySessions / 4f).coerceIn(0f, 1f),
                        size = 58.dp,
                        strokeWidth = 6.dp,
                        colors = GradientGreen
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${state.weeklySessions}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen
                            )
                            Text("sedute", style = MaterialTheme.typography.labelSmall, color = TextLow)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("Volume 7gg", Format.kg(state.weeklyVolumeKg), NeonCyan, Modifier.weight(1f))
                    MiniStat("Proteine", Format.grams(state.today.protein), NeonGreen, Modifier.weight(1f))
                    MiniStat("Carboidrati", Format.grams(state.today.carbs), NeonOrange, Modifier.weight(1f))
                    MiniStat("Grassi", Format.grams(state.today.fat), NeonMagenta, Modifier.weight(1f))
                }
            }
        }

        // --- Obiettivi ------------------------------------------------------
        FitCard {
            Column {
                SectionTitle("Obiettivi")
                if (state.plan == null) {
                    Text(
                        "Nessun piano attivo. Generane uno con l'agente o crealo a mano.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMid
                    )
                } else {
                    val plan = state.plan!!
                    GoalRow(
                        label = "Calorie",
                        value = Format.kcal(state.today.kcal),
                        target = Format.kcal(plan.targetKcal),
                        progress = if (plan.targetKcal > 0) (state.today.kcal / plan.targetKcal).toFloat() else 0f,
                        color = NeonCyan
                    )
                    Spacer(Modifier.height(10.dp))
                    GoalRow(
                        label = "Proteine",
                        value = Format.grams(state.today.protein),
                        target = Format.grams(plan.targetProtein),
                        progress = if (plan.targetProtein > 0) (state.today.protein / plan.targetProtein).toFloat() else 0f,
                        color = NeonGreen
                    )
                    Spacer(Modifier.height(10.dp))
                    KeyValueRow("Piano alimentare", plan.name)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton(
                        text = "Piano",
                        onClick = onOpenPlan,
                        gradient = GradientGreen,
                        modifier = Modifier.weight(1f)
                    )
                    GradientButton(
                        text = "Diario",
                        onClick = onOpenDiary,
                        gradient = GradientViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- Altro ----------------------------------------------------------
        FitCard {
            Column {
                SectionTitle("Altro")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(
                        title = "Calendario",
                        icon = Icons.Filled.CalendarMonth,
                        tint = NeonCyan,
                        onClick = onOpenCalendar,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Spesa",
                        icon = Icons.Filled.ShoppingCart,
                        tint = NeonGreen,
                        onClick = onOpenShopping,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(
                        title = "Diario",
                        icon = Icons.Filled.RestaurantMenu,
                        tint = NeonOrange,
                        onClick = onOpenDiary,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Impostazioni",
                        icon = Icons.Filled.Settings,
                        tint = NeonViolet,
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (state.today.kcal == 0.0 && state.weeklySessions == 0 && state.plan == null && state.routine == null) {
            EmptyState(
                title = "Benvenuto in FitCoach",
                subtitle = "Registra un pasto dal Diario o avvia una seduta dal Workout. I dati restano sul dispositivo."
            )
        }
    }
}

/** Icona tonda su superficie scura, usata nell'header della Home. */
@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CardNavy)
            .border(1.dp, StrokeNavy, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Piccola cella numerica dentro le card, con accento per tipo di macro. */
@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color(0xFF121722))
            .border(1.dp, StrokeNavy, MaterialTheme.shapes.small)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextLow)
    }
}

/** Riga obiettivo: label, valore/target e barra colorata. */
@Composable
private fun GoalRow(
    label: String,
    value: String,
    target: String,
    progress: Float,
    color: Color
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMid)
            Text(
                "$value / $target",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressTrack(progress = progress, color = color)
    }
}
