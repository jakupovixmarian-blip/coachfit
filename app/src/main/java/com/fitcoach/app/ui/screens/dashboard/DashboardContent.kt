package com.fitcoach.app.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.MacroBar
import com.fitcoach.app.ui.components.MacroLegend
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.StatTile
import com.fitcoach.app.ui.components.ButtonRow
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.AiBorder
import com.fitcoach.app.ui.theme.AiGradient
import com.fitcoach.app.ui.theme.AiSoft
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.EmeraldLight
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FatColor
import com.fitcoach.app.ui.theme.FitAnthracite
import com.fitcoach.app.ui.theme.FitMotion
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.HighCarbColor
import com.fitcoach.app.ui.theme.InfoBlue
import com.fitcoach.app.ui.theme.LowCarbColor
import com.fitcoach.app.ui.theme.MacroTrack
import com.fitcoach.app.ui.theme.MediumCarbColor
import com.fitcoach.app.ui.theme.OnAi
import com.fitcoach.app.ui.theme.ProteinColor
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber
import com.fitcoach.app.ui.theme.pressFeedback
import com.fitcoach.app.ui.theme.rememberMotionEnabled
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/* =============================================================================
 * FitCoach 2.0 — Dashboard · DashboardContent
 * -----------------------------------------------------------------------------
 * Composable STATELESS: riceve tutto dallo stato e non conosce ViewModel, Room
 * né repository. Sostituisce solo il corpo grafico della Dashboard esistente.
 *
 * AGGANCIO AL VIEWMODEL ESISTENTE (una volta sola, nel tuo DashboardScreen):
 *
 *   val uiState by viewModel.uiState.collectAsStateWithLifecycle()
 *   DashboardContent(
 *       state = uiState.toDashboardUiState(),        // mapping: vedi DashboardUiState.kt
 *       onOpenNutrition  = { navController.navigate(Routes.NUTRITION) },
 *       onOpenWorkout    = { navController.navigate(Routes.WORKOUT) },
 *       onOpenProgress   = { navController.navigate(Routes.PROGRESS) },
 *       onOpenCoach      = { navController.navigate(Routes.COACH) },
 *       onOpenAnamnesis  = { navController.navigate(Routes.ANAMNESIS) },
 *       onLogWeight      = { apriSheetPeso() },        // funzione già esistente
 *       onSkipWorkout    = { salvaOverride() },        // WorkoutScheduleOverride
 *       onGenerateProgram= { generaProgramma() },      // AiProgramGenerationService
 *       onRetry          = { viewModel.reload() },
 *   )
 *
 * Ogni callback è OBBLIGATORIA (nessun default): il compilatore impedisce di
 * lasciare un pulsante che non fa niente.
 *
 * Nessun `ChartPoint` viene costruito qui: lo sparkline del peso è disegnato
 * direttamente dai valori, così il file non dipende dalla forma del
 * costruttore di ChartPoint definito nel tuo Charts.kt.
 * ========================================================================== */

@Composable
fun DashboardContent(
    state: DashboardUiState,
    onOpenNutrition: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenAnamnesis: () -> Unit,
    onLogWeight: () -> Unit,
    onSkipWorkout: () -> Unit,
    onGenerateProgram: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberMotionEnabled()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground()),
    ) {
        when {
            state.isLoading -> DashboardSkeleton()
            state.errorMessage != null -> DashboardError(state.errorMessage, onRetry)
            !state.hasPlan -> DashboardNoPlan(onGenerateProgram)
            else -> DashboardBody(
                state = state,
                motionEnabled = motionEnabled,
                onOpenNutrition = onOpenNutrition,
                onOpenWorkout = onOpenWorkout,
                onOpenProgress = onOpenProgress,
                onOpenCoach = onOpenCoach,
                onOpenAnamnesis = onOpenAnamnesis,
                onLogWeight = onLogWeight,
                onSkipWorkout = onSkipWorkout,
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Corpo                                                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun DashboardBody(
    state: DashboardUiState,
    motionEnabled: Boolean,
    onOpenNutrition: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenAnamnesis: () -> Unit,
    onLogWeight: () -> Unit,
    onSkipWorkout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StaggeredEntry(0, motionEnabled) { DashboardHeader(state) }
        if (state.needsAnamnesis) {
            StaggeredEntry(1, motionEnabled) {
                AnamnesisCard(state.anamnesisSummary, onOpenAnamnesis)
            }
        }
        if (state.warnings.isNotEmpty()) {
            StaggeredEntry(2, motionEnabled) { WarningsCard(state.warnings) }
        }
        StaggeredEntry(3, motionEnabled) { KcalHeroCard(state, onOpenNutrition) }
        StaggeredEntry(4, motionEnabled) { MacroRailCard(state, onOpenNutrition) }
        StaggeredEntry(5, motionEnabled) { TodaySummaryCard(state) }
        StaggeredEntry(6, motionEnabled) {
            WorkoutCard(state.workout, onOpenWorkout, onSkipWorkout)
        }
        StaggeredEntry(7, motionEnabled) {
            ProgressCard(state.weight, onOpenProgress, onLogWeight)
        }
        StaggeredEntry(8, motionEnabled) {
            RecoveryRow(state.sleepHours, state.weeklyVolumeKg)
        }
        StaggeredEntry(9, motionEnabled) { CoachCard(state.lastCoachMessage, onOpenCoach) }
        val notification = state.nextNotification
        if (notification != null) {
            StaggeredEntry(10, motionEnabled) { NextNotificationRow(notification) }
        }
    }
}

/**
 * Ingresso sfalsato: ogni blocco entra con fade + 12.dp di risalita.
 * Con le animazioni di sistema disattivate il blocco è subito a posto.
 */
@Composable
private fun StaggeredEntry(
    index: Int,
    motionEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (motionEnabled) delay(60L * index)
        visible = true
    }
    val animated by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = FitMotion.enter(),
        label = "dashboardEntry$index",
    )
    val progress = if (motionEnabled) animated else 1f

    Column(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 12.dp.toPx()
        },
    ) {
        content()
    }
}

/* -------------------------------------------------------------------------- */
/* 1. Header contestuale                                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun DashboardHeader(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.greeting.ifBlank { "Dashboard" },
                style = FitTextStyles.h2,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.weekdayLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.weekdayLabel,
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Pill(text = dayTypeLabel(state.dayType), color = dayTypeColor(state.dayType))
    }
}

/* -------------------------------------------------------------------------- */
/* 2. Headline kcal                                                           */
/* -------------------------------------------------------------------------- */

@Composable
private fun KcalHeroCard(state: DashboardUiState, onOpenNutrition: () -> Unit) {
    ClickableCard(onClick = onOpenNutrition) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RIMANENTI",
                    style = FitTextStyles.label,
                    color = TextLow,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatInt(state.kcalRemaining),
                        style = FitTextStyles.numericXL,
                        color = if (state.kcalRemaining < 0) ErrorRed else TextPrimary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "kcal",
                        style = FitTextStyles.caption,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${formatInt(state.kcalConsumed)} assunte · obiettivo ${formatInt(state.kcalTarget)}",
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(14.dp))
            KcalRing(
                diameter = 78.dp,
                consumed = state.kcalConsumed,
                target = state.kcalTarget,
            )
        }
    }
}

/**
 * Anello di avanzamento: traccia su MacroTrack, arco con gradiente AI.
 * Rappresenta consumato/target; se il target è 0 resta vuoto (nessuna
 * divisione per zero).
 */
@Composable
private fun KcalRing(consumed: Int, target: Int, diameter: Dp = 78.dp) {
    val motionEnabled = rememberMotionEnabled()
    val raw = if (target > 0) (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(
        targetValue = raw,
        animationSpec = FitMotion.value(),
        label = "kcalRing",
    )
    val progress = if (motionEnabled) animated else raw

    Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(
                width = (size.width - strokeWidth).coerceAtLeast(1f),
                height = (size.height - strokeWidth).coerceAtLeast(1f),
            )
            drawArc(
                color = MacroTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (progress > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(colors = AiGradient),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "${(progress * 100f).roundToInt()}%",
            style = FitTextStyles.numericS,
            color = TextSecondary,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* 3. Rail macro                                                              */
/* -------------------------------------------------------------------------- */

@Composable
private fun MacroRailCard(state: DashboardUiState, onOpenNutrition: () -> Unit) {
    ClickableCard(onClick = onOpenNutrition) {
        Column {
            SectionHeader(title = "Macro di oggi", subtitle = "Consumati sul target")
            Spacer(Modifier.height(12.dp))
            MacroBar(
                proteinGrams = state.proteinConsumedGrams,
                carbsGrams = state.carbsConsumedGrams,
                fatGrams = state.fatConsumedGrams,
            )
            Spacer(Modifier.height(10.dp))
            MacroLegend(
                proteinGrams = state.proteinConsumedGrams,
                carbsGrams = state.carbsConsumedGrams,
                fatGrams = state.fatConsumedGrams,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MacroRemainder(
                    letter = "P",
                    remaining = state.proteinRemainingGrams,
                    accent = ProteinColor,
                    modifier = Modifier.weight(1f),
                )
                MacroRemainder(
                    letter = "C",
                    remaining = state.carbsRemainingGrams,
                    accent = CarbsColor,
                    modifier = Modifier.weight(1f),
                )
                MacroRemainder(
                    letter = "G",
                    remaining = state.fatRemainingGrams,
                    accent = FatColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MacroRemainder(
    letter: String,
    remaining: Double,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val overshoot = remaining < 0
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "$letter rimasti",
                style = FitTextStyles.label,
                color = TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatGrams(remaining),
            style = FitTextStyles.numericM,
            color = if (overshoot) ErrorRed else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* 4. Riepilogo della giornata                                                */
/* -------------------------------------------------------------------------- */

@Composable
private fun TodaySummaryCard(state: DashboardUiState) {
    SectionCard {
        Column {
            SectionHeader(title = "Oggi", subtitle = state.weekdayLabel.ifBlank { null })
            Spacer(Modifier.height(12.dp))
            if (!state.goalLabel.isNullOrBlank()) {
                SummaryRow(
                    label = "Obiettivo",
                    value = state.goalLabel,
                    accent = EmeraldLight,
                )
                Spacer(Modifier.height(10.dp))
            }
            SummaryRow(
                label = "Pasti registrati",
                value = if (state.meals.planned > 0) {
                    "${state.meals.logged} di ${state.meals.planned}"
                } else {
                    "nessun pasto previsto"
                },
                accent = if (state.meals.logged >= state.meals.planned && state.meals.planned > 0) {
                    SuccessGreen
                } else {
                    TextPrimary
                },
            )
            Spacer(Modifier.height(10.dp))
            SummaryRow(
                label = "Allenamento",
                value = workoutSummaryLabel(state.workout),
                accent = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            SummaryRow(
                label = "Idratazione",
                value = state.hydrationLiters?.let { formatLiters(it) } ?: "non registrata",
                accent = TextPrimary,
            )
        }
    }
}

private fun workoutSummaryLabel(workout: DashboardWorkout?): String = when {
    workout == null -> "riposo"
    workout.state == WorkoutState.COMPLETED -> "${workout.name} · completato"
    workout.state == WorkoutState.IN_PROGRESS -> "${workout.name} · in corso"
    else -> workout.name
}

@Composable
private fun SummaryRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = FitTextStyles.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = value,
            style = FitTextStyles.numericS,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* 5. Allenamento programmato                                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun WorkoutCard(
    workout: DashboardWorkout?,
    onOpenWorkout: () -> Unit,
    onSkipWorkout: () -> Unit,
) {
    SectionCard {
        Column {
            SectionHeader(
                title = "Allenamento di oggi",
                subtitle = workout?.focus?.takeIf { it.isNotBlank() },
            )
            Spacer(Modifier.height(12.dp))
            if (workout == null) {
                Text(
                    text = "Giorno di riposo",
                    style = FitTextStyles.bodyStrong,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Nessun allenamento programmato: recupero previsto dal mesociclo.",
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                // Anche a riposo la scheda deve restare raggiungibile.
                PrimaryButton(text = "Vedi la scheda", onClick = onOpenWorkout)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workout.name,
                            style = FitTextStyles.h3,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sett. ${workout.weekIndex} di ${workout.weekTotal} · " +
                                "${workout.exerciseCount} esercizi",
                            style = FitTextStyles.caption,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Pill(
                        text = workoutStateLabel(workout.state),
                        color = workoutStateColor(workout.state),
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (workout.canSkip) {
                    ButtonRow(
                        primaryText = "Apri allenamento",
                        onPrimary = onOpenWorkout,
                        secondaryText = "Salta",
                        onSecondary = onSkipWorkout,
                    )
                } else {
                    PrimaryButton(text = "Apri allenamento", onClick = onOpenWorkout)
                }
            }
        }
    }
}

private fun workoutStateLabel(state: WorkoutState): String = when (state) {
    WorkoutState.PLANNED -> "Pianificato"
    WorkoutState.IN_PROGRESS -> "In corso"
    WorkoutState.COMPLETED -> "Completato"
}

private fun workoutStateColor(state: WorkoutState): Color = when (state) {
    WorkoutState.PLANNED -> EmeraldLight
    WorkoutState.IN_PROGRESS -> WarningAmber
    WorkoutState.COMPLETED -> SuccessGreen
}

/* -------------------------------------------------------------------------- */
/* 6. Progressi recenti                                                       */
/* -------------------------------------------------------------------------- */

@Composable
private fun ProgressCard(
    weight: DashboardWeight?,
    onOpenProgress: () -> Unit,
    onLogWeight: () -> Unit,
) {
    SectionCard {
        Column {
            SectionHeader(title = "Progressi", subtitle = "Peso corporeo")
            Spacer(Modifier.height(12.dp))
            if (weight == null) {
                Text(
                    text = "Registra il peso per vedere l'andamento.",
                    style = FitTextStyles.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    text = "Registra peso",
                    onClick = onLogWeight,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatDecimal(weight.currentKg),
                                style = FitTextStyles.numericL,
                                color = TextPrimary,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "kg",
                                style = FitTextStyles.caption,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = deltaLabel(weight.deltaKg),
                            style = FitTextStyles.numericS,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    WeightSparkline(
                        values = weight.recentKg,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                GhostButton(
                    text = "Vedi tutti i progressi",
                    onClick = onOpenProgress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Sparkline compatta del peso: nessun asse, nessuna etichetta, solo la forma
 * della tendenza. Il colore resta neutro (Emerald): se salire sia bene o male
 * dipende dall'obiettivo dell'utente, e la UI non deve giudicarlo.
 */
@Composable
private fun WeightSparkline(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.size < 2) return

    val motionEnabled = rememberMotionEnabled()
    val minValue = values.min()
    val maxValue = values.max()
    val span = (maxValue - minValue).let { if (it <= 0.0) 1.0 else it }

    var appeared by remember(values) { mutableStateOf(false) }
    LaunchedEffect(values) { appeared = true }
    val animated by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = FitMotion.value(),
        label = "weightSparkline",
    )
    val reveal = if (motionEnabled) animated else 1f

    Canvas(modifier = modifier) {
        val paddingY = 5.dp.toPx()
        val usableHeight = (size.height - paddingY * 2f).coerceAtLeast(1f)
        val path = Path()
        for (index in values.indices) {
            val x = size.width * (index.toFloat() / (values.size - 1).toFloat())
            val normalized = ((values[index] - minValue) / span).toFloat()
            val y = paddingY + usableHeight * (1f - normalized)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        clipRect(right = size.width * reveal) {
            drawPath(
                path = path,
                color = Emerald,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

private fun deltaLabel(deltaKg: Double): String {
    if (abs(deltaKg) < 0.05) return "stabile vs registrazione precedente"
    val sign = if (deltaKg > 0) "+" else "−"
    return "$sign${formatDecimal(abs(deltaKg))} kg vs precedente"
}

/* -------------------------------------------------------------------------- */
/* 7. Recupero e attività                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun RecoveryRow(sleepHours: Double?, weeklyVolumeKg: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            label = "Sonno",
            value = sleepHours?.let { "${formatDecimal(it)} h" } ?: "—",
            accent = InfoBlue,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Volume 7 gg",
            value = weeklyVolumeKg?.let { "${formatInt(it.roundToInt())} kg" } ?: "—",
            accent = Emerald,
            modifier = Modifier.weight(1f),
        )
    }
}

/* -------------------------------------------------------------------------- */
/* 8. Accesso al Coach AI                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun CoachCard(lastMessage: String?, onOpenCoach: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(FitRadius.lg)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressFeedback(interaction)
            .clip(shape)
            .background(Brush.linearGradient(listOf(AiSoft, FitSurfaceVariant)))
            .border(width = 1.dp, color = AiBorder, shape = shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpenCoach,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(AiGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AI",
                    style = FitTextStyles.label,
                    color = OnAi,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chiedi al Coach",
                    style = FitTextStyles.bodyStrong,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = lastMessage?.takeIf { it.isNotBlank() } ?: "Nessun messaggio recente",
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "›",
                style = FitTextStyles.h3,
                color = TextLow,
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 9. Prossima notifica                                                       */
/* -------------------------------------------------------------------------- */

@Composable
private fun NextNotificationRow(notification: DashboardNotification) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(TextLow),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = notification.title,
            style = FitTextStyles.caption,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = notification.timeLabel,
            style = FitTextStyles.numericS,
            color = TextLow,
            maxLines = 1,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* 10. Anamnesi incompleta + avvisi reali                                     */
/* -------------------------------------------------------------------------- */

/**
 * Invito a completare l'anamnesi. Compare solo se il dato è davvero mancante:
 * senza anamnesi il Coach AI non può generare nulla, quindi è la prima azione.
 */
@Composable
private fun AnamnesisCard(summary: String?, onOpenAnamnesis: () -> Unit) {
    SectionCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Anamnesi da completare",
                    style = FitTextStyles.bodyStrong,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Pill(text = "Richiesta", color = WarningAmber)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = summary?.takeIf { it.isNotBlank() }
                    ?: "Senza anamnesi il Coach AI non può generare nutrizione e scheda.",
                style = FitTextStyles.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            GhostButton(
                text = "Completa l'anamnesi",
                onClick = onOpenAnamnesis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Avvisi prodotti dai controlli reali (allergie, incongruenze, limitazioni).
 * Non sono decorativi: sono gli stessi messaggi che bloccano la generazione.
 */
@Composable
private fun WarningsCard(warnings: List<String>) {
    SectionCard {
        Column {
            Text(
                text = "DA CONTROLLARE",
                style = FitTextStyles.label,
                color = WarningAmber,
            )
            Spacer(Modifier.height(8.dp))
            warnings.forEach { warning ->
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(WarningAmber),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = warning,
                        style = FitTextStyles.bodySmall,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Stati: caricamento, errore, nessun piano                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun DashboardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LoadingBox(modifier = Modifier.height(64.dp))
        LoadingBox(modifier = Modifier.height(120.dp))
        LoadingBox(modifier = Modifier.height(120.dp))
        LoadingBox(modifier = Modifier.height(96.dp))
        LoadingBox(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun DashboardError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        SectionCard {
            Column {
                Text(
                    text = "Impossibile leggere i dati",
                    style = FitTextStyles.h3,
                    color = ErrorRed,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    style = FitTextStyles.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(text = "Riprova", onClick = onRetry)
            }
        }
    }
}

@Composable
private fun DashboardNoPlan(onGenerateProgram: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            title = "Nessun programma attivo",
            message = "Completa l'anamnesi: il Coach AI genera nutrizione e " +
                "allenamento sui tuoi dati reali.",
            actionLabel = "Genera il programma",
            onAction = onGenerateProgram,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Card cliccabile + helper                                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun ClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    SectionCard(
        modifier = modifier
            .pressFeedback(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        content = content,
    )
}

private fun dayTypeLabel(dayType: CarbDayType): String = when (dayType) {
    CarbDayType.HIGH -> "High carb"
    CarbDayType.MEDIUM -> "Medium"
    CarbDayType.LOW -> "Low carb"
}

private fun dayTypeColor(dayType: CarbDayType): Color = when (dayType) {
    CarbDayType.HIGH -> HighCarbColor
    CarbDayType.MEDIUM -> MediumCarbColor
    CarbDayType.LOW -> LowCarbColor
}

/** Interi con separatore migliaia italiano: 1640 -> "1.640". */
private fun formatInt(value: Int): String = String.format(Locale.ITALY, "%,d", value)

/** Un decimale con virgola: 82.4 -> "82,4". */
private fun formatDecimal(value: Double): String = String.format(Locale.ITALY, "%.1f", value)

private fun formatLiters(value: Double): String = "${formatDecimal(value)} L"

/** Grammi residui; il segno meno è tipografico, non un trattino. */
private fun formatGrams(value: Double): String {
    val rounded = value.roundToInt()
    return when {
        rounded > 0 -> "$rounded g"
        rounded < 0 -> "−${-rounded} g"
        else -> "0 g"
    }
}
