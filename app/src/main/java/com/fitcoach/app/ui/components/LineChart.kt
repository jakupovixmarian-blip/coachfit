package com.fitcoach.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.theme.ChartAxisLabel
import com.fitcoach.app.ui.theme.ChartGrid
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitAnthracite
import com.fitcoach.app.ui.theme.FitMotion
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · LineChart
 * -----------------------------------------------------------------------------
 * ATTENZIONE ALL'INSTALLAZIONE — leggi prima di copiare:
 *   `ChartPoint` è definito nel tuo ui/components/Charts.kt e NON viene
 *   ridefinito qui (il contratto non ne riporta i tipi esatti: x è String o
 *   Long, y è Float). Questo file contiene SOLO la funzione LineChart, con la
 *   firma esatta del contratto.
 *
 *   Passi:
 *     1. In Charts.kt cancella la vecchia `fun LineChart(...)` e lascia
 *        `data class ChartPoint(...)` e gli eventuali altri grafici.
 *     2. Aggiungi questo file nel pacchetto com.fitcoach.app.ui.components.
 *
 *   L'asse X usa `point.x.toString()`, così funziona sia con String sia con
 *   Long: nessuna assunzione sul tipo.
 * ========================================================================== */

/**
 * Grafico a linea con area sfumata, griglia orizzontale e rivelazione animata.
 * Disegnato con Canvas nativo: nessuna libreria esterna, coerente con
 * l'architettura offline-first.
 *
 * @param points serie da mostrare; se vuota viene mostrato lo stato "nessun dato".
 * @param lineColor colore della linea e dell'area.
 * @param heightDp altezza dell'area di disegno (default 180).
 * @param unitSuffix suffisso delle etichette di valore (es. " kg", " kcal").
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Emerald,
    heightDp: Int = 180,
    unitSuffix: String = "",
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(RoundedCornerShape(FitRadius.lg))
                .background(FitAnthracite)
                .border(width = 1.dp, color = FitOutline, shape = RoundedCornerShape(FitRadius.lg)),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(
                text = "Nessun dato nel periodo",
                style = FitTextStyles.caption,
                color = TextLow,
            )
        }
        return
    }

    val minY = points.minOf { it.value }.toFloat()
    val maxY = points.maxOf { it.value }.toFloat()
    val span = (maxY - minY).let { if (it <= 0f) 1f else it }

    var appeared by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = FitMotion.value(),
        label = "lineChartReveal",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Valori estremi: contesto immediato senza dover leggere l'asse.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatChartValue(maxY) + unitSuffix,
                style = FitTextStyles.numericS,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatChartValue(minY) + unitSuffix,
                style = FitTextStyles.numericS,
                color = ChartAxisLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
            val width = size.width
            val height = size.height
            val paddingY = 8.dp.toPx()
            val usableHeight = (height - paddingY * 2f).coerceAtLeast(1f)
            val gridLines = 4

            // Griglia orizzontale: riferimento di lettura, non decorazione.
            for (i in 0..gridLines) {
                val y = paddingY + usableHeight * (i.toFloat() / gridLines)
                drawLine(
                    color = ChartGrid,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            if (points.size == 1) {
                val y = paddingY + usableHeight * (1f - (points[0].value.toFloat() - minY) / span)
                drawCircle(
                    color = lineColor.copy(alpha = 0.25f * reveal),
                    radius = 7.dp.toPx(),
                    center = Offset(width / 2f, y),
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(width / 2f, y),
                )
                return@Canvas
            }

            val offsets = ArrayList<Offset>(points.size)
            for (i in points.indices) {
                val x = width * (i.toFloat() / (points.size - 1).toFloat())
                val normalized = (points[i].value.toFloat() - minY) / span
                val y = paddingY + usableHeight * (1f - normalized)
                offsets.add(Offset(x, y))
            }

            // Curva morbida: controllo ai punti medi, nessun overshoot evidente.
            val linePath = Path().apply {
                moveTo(offsets[0].x, offsets[0].y)
                for (i in 1 until offsets.size) {
                    val previous = offsets[i - 1]
                    val current = offsets[i]
                    val midX = (previous.x + current.x) / 2f
                    cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
                }
            }

            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(offsets.last().x, height)
                lineTo(offsets.first().x, height)
                close()
            }

            clipRect(right = width * reveal) {
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                        startY = paddingY,
                        endY = height,
                    ),
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            // Punto finale: dove sei adesso.
            if (reveal > 0.02f) {
                val last = offsets.last()
                drawCircle(
                    color = lineColor.copy(alpha = 0.22f),
                    radius = 7.dp.toPx(),
                    center = last,
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.5.dp.toPx(),
                    center = last,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = points.first().label,
                style = FitTextStyles.numericS,
                color = ChartAxisLabel,
                maxLines = 1,
            )
            if (points.size > 2) {
                Text(
                    text = points[points.size / 2].label,
                    style = FitTextStyles.numericS,
                    color = ChartAxisLabel,
                    maxLines = 1,
                )
            }
            Text(
                text = points.last().label,
                style = FitTextStyles.numericS,
                color = ChartAxisLabel,
                maxLines = 1,
            )
        }
    }
}

/** Formattazione compatta: interi senza decimali, altrimenti un decimale. */
private fun formatChartValue(value: Float): String {
    val rounded = value.roundToInt()
    return if (abs(value - rounded) < 0.05f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
