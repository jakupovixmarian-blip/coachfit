package dev.fitcoach.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A single chat bubble, aligned left for the assistant and right for the user. */
@Composable
fun ChatBubble(
    text: String,
    fromUser: Boolean,
    modifier: Modifier = Modifier
) {
    val container = if (fromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = container,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (fromUser) "Tu" else "Coach AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
