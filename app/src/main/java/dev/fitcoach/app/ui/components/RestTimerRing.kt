package dev.fitcoach.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.NeonMagenta
import dev.fitcoach.app.ui.theme.NeonViolet
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow

/**
 * Countdown ring del recupero, come nel mockup: anello ciano→viola con
 * gradiente, alone luminoso e tempo grande al centro.
 *
 * Il tempo rimanente è l'unica cosa che l'utente guarda tra una serie e l'altra,
 * quindi sta al centro del ring e non in una label. Quando il totale non è noto
 * (0) l'anello resta vuoto invece che pieno, che suggerirebbe un recupero finito.
 */
@Composable
fun RestTimerRing(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp
) {
    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    } else 0f
    val ringColors = listOf(NeonCyan, NeonViolet, NeonMagenta)

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)

            // Alone esterno molto tenue.
            drawArc(
                color = NeonCyan.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                size = Size(this.size.width + 8.dp.toPx(), this.size.height + 8.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = StrokeNavy,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(ringColors + ringColors.first()),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            Text(
                "%02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "RECUPERO",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow
            )
        }
    }
}
