package com.fitcoach.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.TextSecondary

/** Punto di una serie per il grafico a linee. */
data class ChartPoint(val label: String, val value: Double)

/**
 * Grafico a linee leggero basato su Compose Canvas: nessuna dipendenza esterna,
 * funziona offline e rende i dati reali dell'utente. Disegna griglia, asse
 * etichettato e la linea con i punti.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Emerald,
    heightDp: Int = 180,
    unitSuffix: String = ""
) {
    if (points.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(heightDp.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                "Servono almeno 2 misurazioni per il grafico.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        return
    }

    val minV = points.minOf { it.value }
    val maxV = points.maxOf { it.value }
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    val padMin = minV - range * 0.1
    val padMax = maxV + range * 0.1
    val padRange = padMax - padMin

    val labelColor = TextSecondary
    val gridColor = FitOutline

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(heightDp.dp).padding(vertical = 8.dp)) {
            val left = 44.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 22.dp.toPx()
            val width = (right - left).coerceAtLeast(1f)
            val chartHeight = (bottom - top).coerceAtLeast(1f)

            // Griglia orizzontale + etichette valori
            val steps = 4
            for (i in 0..steps) {
                val y = top + chartHeight * i / steps
                drawLine(gridColor.copy(alpha = 0.25f), Offset(left, y), Offset(right, y), 1f)
                val value = padMax - padRange * i / steps
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(value),
                    2f,
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = labelColor.toArgbSafe()
                        textSize = 10.dp.toPx()
                        isAntiAlias = true
                    }
                )
            }

            val n = points.size
            fun xAt(i: Int) = left + width * i / (n - 1)
            fun yAt(v: Double) = bottom - chartHeight * ((v - padMin) / padRange).toFloat()

            val path = Path()
            points.forEachIndexed { i, p ->
                val x = xAt(i)
                val y = yAt(p.value)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx()))

            points.forEachIndexed { i, p ->
                drawCircle(lineColor, radius = 3.5.dp.toPx(), center = Offset(xAt(i), yAt(p.value)))
            }

            // Etichette asse X (prime, centrale, ultima)
            val labelPaint = android.graphics.Paint().apply {
                color = labelColor.toArgbSafe()
                textSize = 10.dp.toPx()
                isAntiAlias = true
            }
            listOf(0, n / 2, n - 1).distinct().forEach { i ->
                drawContext.canvas.nativeCanvas.drawText(
                    points[i].label,
                    xAt(i) - labelPaint.measureText(points[i].label) / 2,
                    size.height - 4.dp.toPx(),
                    labelPaint
                )
            }
        }
        Text(
            text = "Min ${"%.1f".format(minV)}$unitSuffix · Max ${"%.1f".format(maxV)}$unitSuffix",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 44.dp)
        )
    }
}

private fun Color.toArgbSafe(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)
