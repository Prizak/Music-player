package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Track
import com.example.musicplayer.viewmodel.PlayerUiState

/**
 * MiniPlayer persistente. Só renderiza se houver faixa atual.
 * Se [applyNavigationBarInsets] for true, adiciona padding das navigation bars abaixo.
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    applyNavigationBarInsets: Boolean = true,
    /** Posição lida só na fase de desenho (sem recompor o MiniPlayer a cada tick). */
    positionMs: () -> Long = { state.positionMs }
) {
    val track = state.currentTrack ?: return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp
    ) {
        Column {
        Column(
            Modifier.windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                    if (applyNavigationBarInsets) WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal else WindowInsetsSides.Horizontal
                )
            )
        ) {
            // Barra própria (3 dp). O progresso é lido dentro do drawBehind: só a fase de desenho roda a cada tick.
            val duration = state.durationMs
            Box(
                Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .drawBehind {
                        val f = if (duration > 0) (positionMs().toFloat() / duration).coerceIn(0f, 1f) else 0f
                        clipRect(right = size.width * f) { drawRect(com.example.musicplayer.ui.theme.BrandGradient) }
                    }
            )
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(track, Modifier.size(44.dp), cornerRadius = 12.dp, iconSize = 20.dp, targetPx = CoverSize.LIST)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HapticIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(40.dp).background(
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(com.example.musicplayer.ui.theme.Coral, com.example.musicplayer.ui.theme.CoralHot, com.example.musicplayer.ui.theme.Amber)),
                        androidx.compose.foundation.shape.CircleShape
                    )
                ) {
                    Icon(
                        if (state.isPlaying) ResIcons.Pause else ResIcons.Play,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproduzir",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                FavoriteButton(track.id)
                HapticIconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(ResIcons.SkipNext, contentDescription = "Próxima")
                }
            }
        }
        }
    }
}

private val previewTrack = Track(1, "Título da faixa", "Artista", "Álbum", 215_000, "content://x", null)

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MiniPlayerPreview() {
    com.example.musicplayer.ui.theme.MusicPlayerTheme(darkTheme = true) {
        MiniPlayer(PlayerUiState(currentTrack = previewTrack, isPlaying = true, positionMs = 60_000, durationMs = 215_000, hasNext = true), {}, {}, {})
    }
}
