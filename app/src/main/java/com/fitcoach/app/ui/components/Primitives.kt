package com.fitcoach.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.EmeraldLight
import com.fitcoach.app.ui.theme.FitDuration
import com.fitcoach.app.ui.theme.FitEasing
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceInteractive
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.MacroTrack
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.rememberMotionEnabled

/* =============================================================================
 * Primitives.kt — componenti condivisi delle schermate ridisegnate.
 * -----------------------------------------------------------------------------
 * Solo presentazione. Nessun accesso a ViewModel, repository o database:
 * ricevono valori già calcolati e restituiscono eventi con callback.
 * Tutti gli import puntano a simboli realmente presenti in ui/theme.
 * ========================================================================== */

/**
 * Anello di progresso (donut) con animazione di ingresso.
 * Usato da Alimentazione (calorie), Progressi (aderenza), Sessione (serie).
 *
 * @param progress 0f..1f, valori fuori range vengono limitati.
 * @param center contenuto al centro dell'anello (numeri, unità).
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    strokeWidth: Dp = 12.dp,
    color: Color = Emerald,
    trackColor: Color = MacroTrack,
    center: @Composable () -> Unit = {},
) {
    val motion = rememberMotionEnabled()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = if (motion) FitDuration.Slow else 0,
            easing = FitEasing.Standard,
        ),
        label = "progressRing",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.001f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        center()
    }
}

/** Barra orizzontale con riempimento animato. */
@Composable
fun AnimatedBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Emerald,
    height: Dp = 8.dp,
    trackColor: Color = MacroTrack,
) {
    val motion = rememberMotionEnabled()
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = if (motion) FitDuration.Medium else 0,
            easing = FitEasing.Standard,
        ),
        label = "animatedBar",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(FitRadius.pill)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(FitRadius.pill)
                .background(color),
        )
    }
}

/**
 * Controllo segmentato a selezione singola (filtri temporali di Progressi,
 * tipi di giornata in Alimentazione).
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(FitRadius.sm)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FitSurfaceVariant)
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                targetValue = if (selected) BrandSoft else Color.Transparent,
                animationSpec = tween(durationMillis = FitDuration.Fast),
                label = "segBackground",
            )
            val foreground by animateColorAsState(
                targetValue = if (selected) EmeraldLight else TextLow,
                animationSpec = tween(durationMillis = FitDuration.Fast),
                label = "segForeground",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(FitRadius.xs))
                    .background(background)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = FitTextStyles.label,
                    color = foreground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Riga etichetta/valore per elenchi densi di dati. */
@Composable
fun DataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = FitTextStyles.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = FitTextStyles.numericS, color = valueColor)
    }
}

/** Campo in sola lettura (Anamnesi, Profilo): etichetta sopra, valore sotto. */
@Composable
fun FieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(FitRadius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FitSurfaceVariant)
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = label.uppercase(), style = FitTextStyles.caption, color = TextLow)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = FitTextStyles.bodyStrong, color = TextPrimary)
    }
}

/** Indicatore di passo (Anamnesi 1 di 6, Onboarding). */
@Composable
fun StepDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index <= current
            val color by animateColorAsState(
                targetValue = if (active) Emerald else FitSurfaceInteractive,
                animationSpec = tween(durationMillis = FitDuration.Base),
                label = "stepDot",
            )
            Box(
                modifier = Modifier
                    .width(if (index == current) 22.dp else 8.dp)
                    .height(4.dp)
                    .clip(FitRadius.pill)
                    .background(color),
            )
        }
    }
}

/**
 * Grafico a barre senza dipendenze esterne (volume settimanale, aderenza,
 * calorie per giorno). `highlightIndex` evidenzia la barra selezionata.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    heightDp: Int = 120,
    color: Color = Emerald,
    highlightIndex: Int = -1,
) {
    val max = values.maxOrNull() ?: 0f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = if (max <= 0f) 0f else (value / max).coerceIn(0f, 1f)
            val barColor = if (index == highlightIndex) color else color.copy(alpha = 0.38f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(FitRadius.xs))
                            .background(barColor),
                    )
                }
                if (index < labels.size) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = labels[index],
                        style = FitTextStyles.caption,
                        color = TextLow,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Grafico a tendenza con area sfumata e rivelazione animata.
 *
 * Autonomo: non dipende da tipi esterni (nessun ChartPoint), così si integra
 * senza rischio di doppie dichiarazioni nel progetto. Riceve solo valori già
 * presenti nel database: se la serie non ha almeno 2 punti non disegna nulla.
 */
@Composable
fun TrendChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    heightDp: Int = 160,
    lineColor: Color = Emerald,
    unitSuffix: String = "",
) {
    if (values.size < 2) return

    val motion = rememberMotionEnabled()
    var target by remember(values) { mutableFloatStateOf(0f) }
    LaunchedEffect(values) { target = 1f }
    val reveal by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (motion) FitDuration.Slow else 0,
            easing = FitEasing.Decelerate,
        ),
        label = "trendReveal",
    )

    val max = values.maxOrNull() ?: 0f
    val min = values.minOrNull() ?: 0f
    val span = (max - min).takeIf { it > 0f } ?: 1f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${formatChartValue(values.first())}$unitSuffix",
                style = FitTextStyles.caption,
                color = TextLow,
            )
            Text(
                text = "${formatChartValue(values.last())}$unitSuffix",
                style = FitTextStyles.numericS,
                color = lineColor,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val stepX = width / (values.size - 1)
            val padding = height * 0.08f
            val usable = (height - padding * 2f).coerceAtLeast(1f)

            fun pointAt(index: Int): Offset {
                val ratio = ((values[index] - min) / span).coerceIn(0f, 1f)
                return Offset(stepX * index, height - padding - ratio * usable)
            }

            val linePath = Path()
            values.indices.forEach { index ->
                val point = pointAt(index)
                if (index == 0) linePath.moveTo(point.x, point.y) else linePath.lineTo(point.x, point.y)
            }

            val areaPath = Path()
            areaPath.moveTo(0f, height)
            values.indices.forEach { index ->
                val point = pointAt(index)
                areaPath.lineTo(point.x, point.y)
            }
            areaPath.lineTo(width, height)
            areaPath.close()

            clipRect(right = width * reveal) {
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                    ),
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            if (reveal > 0.98f) {
                val last = pointAt(values.lastIndex)
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = last)
                drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = last)
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = labels.first(), style = FitTextStyles.caption, color = TextLow)
                if (labels.size > 2) {
                    Text(
                        text = labels[labels.size / 2],
                        style = FitTextStyles.caption,
                        color = TextLow,
                    )
                }
                Text(text = labels.last(), style = FitTextStyles.caption, color = TextLow)
            }
        }
    }
}

private fun formatChartValue(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
