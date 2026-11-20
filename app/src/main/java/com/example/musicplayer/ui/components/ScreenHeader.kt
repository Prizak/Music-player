package com.example.musicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Insets de status bar/recorte/laterais para telas de aba (a barra inferior cuida do resto). */
@Composable
fun Modifier.topScreenInsets(): Modifier = windowInsetsPadding(
    WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
)

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(start = if (leading == null) 20.dp else 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                if (badge != null) { Spacer(Modifier.width(8.dp)); badge() }
            }
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions?.invoke(this)
    }
}

@Composable
fun EmptyMessage(title: String, message: String, modifier: Modifier = Modifier, content: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Icon(ResIcons.MusicNote, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (content != null) { Spacer(Modifier.height(24.dp)); content() }
    }
}
