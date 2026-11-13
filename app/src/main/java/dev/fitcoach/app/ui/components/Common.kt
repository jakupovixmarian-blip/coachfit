package dev.fitcoach.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fitcoach.app.ui.theme.CardNavy
import dev.fitcoach.app.ui.theme.GradientCyan
import dev.fitcoach.app.ui.theme.HeroGradientEnd
import dev.fitcoach.app.ui.theme.HeroGradientStart
import dev.fitcoach.app.ui.theme.InkOnNeon
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow
import dev.fitcoach.app.ui.theme.TextMid

/**
 * Etichetta di sezione: piccola, maiuscola e spaziata, come nei titoli del
 * mockup. Il titolo deve leggersi come label e non competere con i numeri.
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextMid,
        modifier = modifier.padding(top = 2.dp, bottom = 8.dp)
    )
}

/** Titolo di pagina con sottotitolo opzionale, usato in cima a ogni schermata. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMid)
        }
    }
}

/** Card "glass" del mockup: superficie slate, bordo sottile, angoli larghi. */
@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(CardNavy, Color(0xFF141924)))
            )
            .border(1.dp, StrokeNavy, shape)
            .padding(16.dp)
    ) { content() }
}

/** Card evidenziata: gradiente blu-notte con alone ciano sul bordo. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
            .border(
                1.dp,
                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.55f), Color.Transparent)),
                shape
            )
            .padding(18.dp)
    ) { content() }
}

/** Badge quadrato arrotondato con icona neon: unità base di griglie e liste. */
@Composable
fun NeonBadge(
    icon: ImageVector,
    tint: Color = NeonCyan,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.2f))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.32f), RoundedCornerShape(size / 3.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Bottone principale: riempimento a gradiente, testo scuro, alone luminoso. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: List<Color> = GradientCyan,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = if (enabled) 18.dp else 0.dp,
                shape = shape,
                ambientColor = gradient.first().copy(alpha = 0.6f),
                spotColor = gradient.first().copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(gradient)
                } else {
                    Brush.horizontalGradient(listOf(StrokeNavy, StrokeNavy))
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = InkOnNeon, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) InkOnNeon else TextMid
            )
        }
    }
}

/** Bottone secondario: solo bordo e testo neon. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NeonCyan,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = CircleShape
    val contentAlpha = if (enabled) 1f else 0.4f
    Row(
        modifier = modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.08f * contentAlpha))
            .border(1.dp, tint.copy(alpha = 0.45f * contentAlpha), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint.copy(alpha = contentAlpha), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint.copy(alpha = contentAlpha))
    }
}

/** Numero grande con didascalia: unità base di ogni dashboard. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextLow
        )
    }
}

/** Stat compatta centrata, per righe dense (macro, serie, timer). */
@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextLow)
    }
}

/** Riga metrica: label a sinistra, valore a destra. */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMid)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Barra di progresso a gradiente con alone, come nelle card "Obiettivi". */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(StrokeNavy)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.85f), color)
                    )
                )
        )
    }
}

/** Pillola per tag (gruppi muscolari, pasti, stati). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val tint = color ?: MaterialTheme.colorScheme.primary
    Box(
        modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.13f))
            .border(1.dp, tint.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RowSpaced(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/**
 * Tile della griglia 2x2 della Home: badge neon, titolo e sottotitolo, con
 * bordo che si illumina nel colore dell'azione.
 */
@Composable
fun ActionTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(CardNavy, Color(0xFF121722))))
            .border(1.dp, tint.copy(alpha = 0.28f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        NeonBadge(icon = icon, tint = tint, size = 38.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextLow)
        }
    }
}

/** Card metrica con valore, delta colorato e mini sparkline a gradiente. */
@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    deltaColor: Color = NeonCyan,
    accent: Color = NeonCyan,
    series: List<Float> = emptyList(),
    onClick: (() -> Unit)? = null
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(CardNavy, Color(0xFF121722))))
            .border(1.dp, StrokeNavy, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMid)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (delta != null) {
            Spacer(Modifier.height(2.dp))
            Text(delta, style = MaterialTheme.typography.labelSmall, color = deltaColor)
        }
        if (series.size >= 2) {
            Spacer(Modifier.height(8.dp))
            Sparkline(series = series, color = accent, modifier = Modifier.fillMaxWidth().height(28.dp))
        }
    }
}

/** Linea compatta senza assi, usata dentro le card metrica. */
@Composable
fun Sparkline(
    series: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (series.size < 2) return
    val minV = series.min()
    val maxV = series.max()
    val range = (maxV - minV).takeIf { it > 0f } ?: 1f
    Canvas(modifier) {
        val stepX = size.width / (series.size - 1)
        fun pointAt(index: Int): Offset {
            val normalized = (series[index] - minV) / range
            return Offset(index * stepX, size.height - normalized * size.height * 0.85f - size.height * 0.05f)
        }
        val line = Path()
        val fill = Path()
        series.indices.forEach { index ->
            val p = pointAt(index)
            if (index == 0) {
                line.moveTo(p.x, p.y)
                fill.moveTo(p.x, size.height)
                fill.lineTo(p.x, p.y)
            } else {
                line.lineTo(p.x, p.y)
                fill.lineTo(p.x, p.y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(
            fill,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent))
        )
        drawPath(
            line,
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(color = color, radius = 3.dp.toPx(), center = pointAt(series.lastIndex))
    }
}

/** Ring circolare con gradiente: progresso di seduta, aderenza, obiettivi. */
@Composable
fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    colors: List<Color> = GradientCyan,
    content: (@Composable () -> Unit)? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = StrokeNavy,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (clamped > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(colors + colors.first()),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content?.invoke()
    }
}

/** Tab a pillola con fondo neon sulla voce attiva (Panoramica/Esercizi/Misure). */
@Composable
fun PillTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF121722))
            .border(1.dp, StrokeNavy, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent)
                    .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else Color.Transparent, CircleShape)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) accent else TextMid,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/** Piccolo pallino di stato, usato negli header e nelle righe di elenco. */
@Composable
fun GlowDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .shadow(6.dp, CircleShape, ambientColor = color, spotColor = color)
    )
}

/** Riga icona + testi + freccia: voce di elenco ricorrente nelle schermate. */
@Composable
fun IconActionRow(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeonBadge(icon = icon, tint = tint, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextLow)
            }
        }
        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = TextLow, modifier = Modifier.size(16.dp))
    }
}
