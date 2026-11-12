package dev.fitcoach.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.fitcoach.app.core.util.DateUtils
import java.time.LocalDate

data class ChartPoint(val date: LocalDate, val value: Double)

/** Native Compose line chart: no external charting dependency. */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    heightDp: Int = 180
) {
    if (points.size < 2) {
        Text(
            "Servono almeno due rilevazioni per disegnare il grafico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(8.dp)
        )
        return
    }
    val sorted = points.sortedBy { it.date }
    val minV = sorted.minOf { it.value }
    val maxV = sorted.maxOf { it.value }
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("min %.1f".format(minV), style = MaterialTheme.typography.labelMedium)
            Text("max %.1f".format(maxV), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (sorted.size - 1)

            fun pointAt(index: Int): Offset {
                val normalized = ((sorted[index].value - minV) / range).toFloat()
                return Offset(
                    x = index * stepX,
                    y = height - normalized * height
                )
            }

            drawLine(axisColor, Offset(0f, height), Offset(width, height), strokeWidth = 2f)

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
            drawPath(fillPath, color = fillColor)
            drawPath(path, color = lineColor, style = Stroke(width = 5f))
            sorted.indices.forEach { drawCircle(lineColor, radius = 8f, center = pointAt(it)) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(DateUtils.formatShort(sorted.first().date), style = MaterialTheme.typography.labelMedium)
            Text(DateUtils.formatShort(sorted.last().date), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Simple horizontal bar chart used for weekly volume. */
@Composable
fun BarChart(
    bars: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    heightDp: Int = 160
) {
    if (bars.isEmpty()) {
        Text(
            "Nessun dato disponibile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(8.dp)
        )
        return
    }
    val maxV = bars.maxOf { it.second }.takeIf { it > 0.0 } ?: 1.0
    val barColor = MaterialTheme.colorScheme.primary

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            val slot = size.width / bars.size
            val barWidth = slot * 0.55f
            bars.forEachIndexed { index, (_, value) ->
                val normalized = (value / maxV).toFloat()
                val barHeight = normalized * size.height
                val left = index * slot + (slot - barWidth) / 2
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            bars.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
