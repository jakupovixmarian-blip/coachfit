package dev.fitcoach.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fitcoach.app.ui.theme.BgDeep
import dev.fitcoach.app.ui.theme.CardNavyHigh
import dev.fitcoach.app.ui.theme.InkOnNeon
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextHi
import dev.fitcoach.app.ui.theme.TextMid

/**
 * Bottoni, chip e barra superiore coerenti con il mockup neon.
 *
 * Mantengono la stessa forma di chiamata dei componenti Material
 * (`onClick`, `modifier`, `enabled`, contenuto in lambda) così le schermate
 * possono usarli senza riscrivere la logica.
 */

private val PrimaryGradient = listOf(NeonCyan, Color(0xFF3B82F6))
private val DisabledGradient = listOf(CardNavyHigh, CardNavyHigh)

@Composable
fun FitPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(
                brush = if (enabled) Brush.horizontalGradient(PrimaryGradient) else Brush.horizontalGradient(DisabledGradient),
                shape = shape
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) InkOnNeon else TextMid) {
            ProvideTextStyle(
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun FitSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(CardNavyHigh.copy(alpha = 0.55f), shape)
            .border(1.dp, NeonCyan.copy(alpha = if (enabled) 0.38f else 0.16f), shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) NeonCyan else TextMid) {
            ProvideTextStyle(
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun FitChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) NeonCyan else TextMid
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) NeonCyan.copy(alpha = 0.16f) else CardNavyHigh.copy(alpha = 0.5f), CircleShape)
            .border(1.dp, if (selected) NeonCyan.copy(alpha = 0.55f) else StrokeNavy, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            ProvideTextStyle(
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            ) { label() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BgDeep,
            titleContentColor = TextHi,
            navigationIconContentColor = TextHi,
            actionIconContentColor = TextMid
        )
    )
}
