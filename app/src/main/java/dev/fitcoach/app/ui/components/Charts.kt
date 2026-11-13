package dev.fitcoach.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.ui.theme.GradientCyan
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.NeonGreen
import dev.fitcoach.app.ui.theme.NeonOrange
import dev.fitcoach.app.ui.theme.NeonViolet
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow
import dev.fitcoach.app.ui.theme.TextMid
import java.time.LocalDate

data class ChartPoint(val date: LocalDate, val value: Double)

/**
 * Grafico a linea del mockup: area sfumata sotto la curva, tratto a gradiente
 * con terminazione tonda e ultimo punto evidenziato. Nessuna dipendenza esterna.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    heightDp: Int = 180,
    lineColors: List<Color> = GradientCyan
) {
    if (points.size < 2) {
        Text(
            "Servono almeno due rilevazioni per disegnare il grafico.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid,
            modifier = modifier.padding(8.dp)
        )
        return
    }
    val sorted = points.sortedBy { it.date }
    val minV = sorted.minOf { it.value }
    val maxV = sorted.maxOf { it.value }
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("min %.1f".format(minV), style = MaterialTheme.typography.labelMedium, color = TextLow)
            Text("max %.1f".format(maxV), style = MaterialTheme.typography.labelMedium, color = TextLow)
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (sorted.size - 1)
            val verticalPadding = 10.dp.toPx()

            fun pointAt(index: Int): Offset {
                val normalized = ((sorted[index].value - minV) / range).toFloat()
                return Offset(
                    x = index * stepX,
                    y = height - verticalPadding - normalized * (height - verticalPadding * 2)
                )
            }

            // Griglia orizzontale soffusa.
            val gridColor = StrokeNavy.copy(alpha = 0.55f)
            for (line in 0..3) {
                val y = height * line / 3f
                drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
            }

            val path = Path()
            val fillPath = Path()
            sorted.indices.forEach { index ->
                val p = pointAt(index)
                if (index == 0) {
                    path.moveTo(p.x, p.y)
                    fillPath.moveTo(p.x, height)
                    fillPath.lineTo(p.x, p.y)
                } else {
                    path.lineTo(p.x, p.y)
                    fillPath.lineTo(p.x, p.y)
                }
            }
            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    listOf(lineColors.first().copy(alpha = 0.35f), Color.Transparent)
                )
            )
            drawPath(
                path,
                brush = Brush.horizontalGradient(lineColors),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
            sorted.indices.forEach { index ->
                drawCircle(lineColors.first(), radius = 3.dp.toPx(), center = pointAt(index))
            }
            val last = pointAt(sorted.lastIndex)
            drawCircle(lineColors.last().copy(alpha = 0.25f), radius = 9.dp.toPx(), center = last)
            drawCircle(lineColors.last(), radius = 4.5.dp.toPx(), center = last)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(DateUtils.formatShort(sorted.first().date), style = MaterialTheme.typography.labelMedium, color = TextLow)
            Text(DateUtils.formatShort(sorted.last().date), style = MaterialTheme.typography.labelMedium, color = TextLow)
        }
    }
}

/** Barre verticali arrotondate con gradiente, usate per il volume settimanale. */
@Composable
fun BarChart(
    bars: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    heightDp: Int = 160,
    colors: List<Color> = listOf(NeonCyan, NeonViolet)
) {
    if (bars.isEmpty()) {
        Text(
            "Nessun dato disponibile.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid,
            modifier = modifier.padding(8.dp)
        )
        return
    }
    val maxV = bars.maxOf { it.second }.takeIf { it > 0.0 } ?: 1.0

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            val slot = size.width / bars.size
            val barWidth = slot * 0.5f
            bars.forEachIndexed { index, (_, value) ->
                val normalized = (value / maxV).toFloat()
                val barHeight = (normalized * (size.height - 6.dp.toPx())).coerceAtLeast(4.dp.toPx())
                val left = index * slot + (slot - barWidth) / 2
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(colors.first(), colors.last())),
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            bars.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextLow)
            }
        }
    }
}

/** Voce di legenda del donut: colore, etichetta, percentuale. */
data class DonutSlice(val label: String, val value: Float, val color: Color)

/**
 * Donut della composizione corporea: anelli a spessore pieno con stacco tra le
 * fette e valore centrale grande, come nel mockup.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 20.dp,
    centerLabel: String? = null,
    centerCaption: String? = null
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = StrokeNavy,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            var start = -90f
            slices.forEach { slice ->
                val sweep = 360f * (slice.value / total)
                drawArc(
                    color = slice.color,
                    startAngle = start + 2f,
                    sweepAngle = (sweep - 4f).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerLabel != null) {
                Text(
                    centerLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )
            }
            if (centerCaption != null) {
                Text(centerCaption, style = MaterialTheme.typography.labelSmall, color = TextLow)
            }
        }
    }
}

/** Legenda compatta per il donut e per i grafici multi-serie. */
@Composable
fun ChartLegend(items: List<DonutSlice>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .padding(0.dp)
                ) {
                    Canvas(Modifier.size(8.dp)) {
                        drawCircle(item.color)
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMid,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "%.1f%%".format(item.value),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = item.color
                )
            }
        }
    }
}

/** Colori di serie riutilizzabili dalle schermate di progresso. */
val SeriesColors: List<Color> = listOf(NeonCyan, NeonViolet, NeonGreen, NeonOrange)
