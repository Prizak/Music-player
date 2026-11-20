package com.example.musicplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Track
import com.example.musicplayer.ui.theme.Amber
import com.example.musicplayer.ui.theme.Cyan

data class TrackMenuAction(val label: String, val onClick: () -> Unit)

/** Cartão de faixa (music_library_light): rounded-2xl, surface-container-low, borda sutil; atual = borda coral + faixa lateral. */
@Composable
fun TrackItem(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: List<TrackMenuAction> = emptyList(),
    trailingText: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    stripBrush: Brush? = null,
    outlineColor: androidx.compose.ui.graphics.Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val dark = cs.background.luminance() < 0.5f
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().let {
            if (isCurrent) it.shadow(6.dp, shape, ambientColor = cs.primary, spotColor = cs.primary) else it
        },
        shape = shape,
        // Escuro: surface-container sem borda; claro: surface-container-low + borda sutil (como no Stitch).
        color = if (dark) cs.surfaceContainer else cs.surfaceContainerLow,
        border = when {
            outlineColor != null -> BorderStroke(1.dp, outlineColor)
            isCurrent -> BorderStroke(1.5.dp, Brush.linearGradient(listOf(cs.primary, Amber.copy(alpha = 0.8f))))
            dark -> null
            else -> BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f))
        },
        shadowElevation = 0.dp
    ) {
        Box {
            if (isCurrent || stripBrush != null) {
                Box(
                    Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight()
                        .background(stripBrush ?: Brush.verticalGradient(listOf(cs.primary, Amber, Cyan)))
                )
            }
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                leading?.invoke()
                val artMod = Modifier.size(48.dp).let {
                    if (isCurrent) it.border(2.dp, cs.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)) else it
                }
                AlbumArt(track, artMod, cornerRadius = 12.dp, iconSize = 22.dp, targetPx = CoverSize.LIST)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            track.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isCurrent) {
                            Spacer(Modifier.width(6.dp))
                            PulseDot(cs.primary)
                        }
                    }
                    Text(
                        track.artist, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (trailing != null) trailing() else Text(
                    trailingText ?: formatTime(track.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isCurrent) cs.onSurface else cs.onSurfaceVariant
                )
                FavoriteButton(track.id, Modifier.size(32.dp))
                if (menu.isNotEmpty()) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        HapticIconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) {
                            Icon(ResIcons.MoreVert, "Mais opções", tint = cs.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            menu.forEach { a ->
                                DropdownMenuItem(text = { Text(a.label) }, onClick = { open = false; a.onClick() })
                            }
                        }
                    }
                } else Spacer(Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun PulseDot(color: androidx.compose.ui.graphics.Color) {
    val a by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a"
    )
    Box(
        Modifier.size(8.dp).alpha(a).clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(color, Amber)))
    )
}
