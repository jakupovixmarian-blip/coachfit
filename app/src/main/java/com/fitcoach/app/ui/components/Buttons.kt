package com.fitcoach.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.EmeraldDark
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.OnBrand
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.pressFeedback

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · Buttons.kt
 * -----------------------------------------------------------------------------
 * Due soli bottoni, entrambi con un'azione obbligatoria: nessun default {},
 * così il compilatore impedisce di lasciare un pulsante decorativo.
 *
 *   PrimaryButton -> azione principale della schermata (48.dp, gradiente brand)
 *   GhostButton   -> azione secondaria (bordo, superficie trasparente)
 *
 * Non esistono nel contratto originale: sono un'aggiunta di solo design.
 * Se il tuo progetto ha già un componente equivalente, cancella questo file e
 * sostituisci le chiamate: le firme sono (text, onClick, modifier, enabled).
 * ========================================================================== */

/** Azione principale: altezza 48.dp, raggio 14.dp, gradiente del brand. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(FitRadius.md)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressFeedback(interaction, enabled)
            .clip(shape)
            .background(
                Brush.horizontalGradient(listOf(Emerald, EmeraldDark)),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = FitTextStyles.bodyStrong,
            color = OnBrand.copy(alpha = if (enabled) 1f else 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Azione secondaria: stessa altezza, bordo hairline, nessun riempimento. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(FitRadius.md)
    Box(
        modifier = modifier
            .height(48.dp)
            .pressFeedback(interaction, enabled)
            .clip(shape)
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = FitTextStyles.bodyStrong,
            color = TextSecondary.copy(alpha = if (enabled) 1f else 0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Riga di due azioni affiancate: primaria che prende lo spazio, secondaria
 * compatta. Usata quando la schermata ha un'azione principale e una di uscita.
 */
@Composable
fun ButtonRow(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            PrimaryButton(text = primaryText, onClick = onPrimary)
        }
        GhostButton(text = secondaryText, onClick = onSecondary)
    }
}
