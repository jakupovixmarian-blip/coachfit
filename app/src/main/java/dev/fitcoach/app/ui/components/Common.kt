package dev.fitcoach.app.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fitcoach.app.ui.theme.HeroGradientEnd
import dev.fitcoach.app.ui.theme.HeroGradientStart

/**
 * Section heading: small, spaced, muted. The title should read as a label, not
 * compete with the numbers below it.
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 2.dp, bottom = 6.dp)
    )
}

/** Page title with an optional subtitle, used at the top of every screen. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Standard surface: a flat, bordered container. No shadow, so lists stay calm. */
@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                .padding(16.dp)
        ) { content() }
    }
}

/** Highlighted card, used for the single most important block of a screen. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.large)
                .padding(18.dp)
        ) { content() }
    }
}

/** Big number with a caption, the base unit of every dashboard. */
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Compact inline stat, kept for dense rows. */
@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A labelled metric row: label on the left, value on the right. */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Rounded progress track used for goals and macro splits. */
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Small pill used for tags such as muscle groups or meal slots. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val tint = color ?: MaterialTheme.colorScheme.primary
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
