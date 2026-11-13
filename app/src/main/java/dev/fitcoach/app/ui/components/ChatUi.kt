package dev.fitcoach.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fitcoach.app.ui.theme.CardNavy
import dev.fitcoach.app.ui.theme.GradientViolet
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.NeonGreen
import dev.fitcoach.app.ui.theme.NeonOrange
import dev.fitcoach.app.ui.theme.NeonViolet
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow
import dev.fitcoach.app.ui.theme.TextMid

/**
 * Bolla di chat: a sinistra scura per l'assistente, a destra con gradiente
 * viola per l'utente, come nel mockup dell'AI Coach.
 */
@Composable
fun ChatBubble(
    text: String,
    fromUser: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = if (fromUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .clip(shape)
                .then(
                    if (fromUser) {
                        Modifier.background(Brush.linearGradient(GradientViolet))
                    } else {
                        Modifier
                            .background(CardNavy)
                            .border(1.dp, StrokeNavy, shape)
                    }
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (fromUser) "TU" else "COACH AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fromUser) Color.White.copy(alpha = 0.75f) else NeonViolet
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (fromUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Risposta rapida suggerita dall'assistente: pillola scura con icona neon. */
@Composable
fun QuickReplyChip(
    text: String,
    icon: ImageVector,
    tint: Color = NeonCyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(CardNavy)
            .border(1.dp, StrokeNavy, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Card pasto restituita dall'AI: bordo viola luminoso, titolo, calorie e riga
 * macro, come nel mockup.
 */
@Composable
fun ChatMacroCard(
    title: String,
    kcal: String,
    protein: String,
    carbs: String,
    fat: String,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardNavy)
            .border(1.dp, NeonViolet.copy(alpha = 0.55f), shape)
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(kcal, style = MaterialTheme.typography.titleSmall, color = NeonOrange)
        }
        Spacer(Modifier.size(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MacroChip("Proteine", protein, NeonCyan)
            MacroChip("Carboidrati", carbs, NeonGreen)
            MacroChip("Grassi", fat, NeonViolet)
        }
    }
}

@Composable
private fun MacroChip(label: String, value: String, tint: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextLow)
    }
}

/** Indicatore "sta scrivendo" con tre puntini. */
@Composable
fun TypingDots(modifier: Modifier = Modifier, tint: Color = NeonViolet) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(CardNavy)
            .border(1.dp, StrokeNavy, CircleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (it == 0) 0.45f else 0.9f))
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("sto scrivendo", style = MaterialTheme.typography.labelSmall, color = TextMid)
    }
}
