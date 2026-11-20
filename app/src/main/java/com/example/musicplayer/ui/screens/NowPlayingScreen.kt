package com.example.musicplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.musicplayer.viewmodel.RepeatMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.musicplayer.ui.components.ResonanceSlider
import com.example.musicplayer.ui.theme.Amber
import com.example.musicplayer.ui.theme.Coral
import com.example.musicplayer.ui.theme.CoralHot
import com.example.musicplayer.data.Track
import com.example.musicplayer.ui.components.AlbumArt
import com.example.musicplayer.ui.components.CoverSize
import com.example.musicplayer.ui.components.ResIcons
import com.example.musicplayer.ui.components.HapticIconButton
import com.example.musicplayer.ui.components.FavoriteButton
import com.example.musicplayer.ui.components.formatTime
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.example.musicplayer.viewmodel.EqualizerUiState
import com.example.musicplayer.viewmodel.PlayerUiState

/** Sem slider de volume (controle do sistema). */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onPlayFromQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    equalizer: EqualizerController,
    modifier: Modifier = Modifier,
    /** Posição lida só no SeekBar (o resto da tela não recompõe a cada tick). */
    positionMs: () -> Long = { state.positionMs }
) {
    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = if (showQueue) ({ showQueue = false }) else onBack)
    val track = state.currentTrack
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cs = MaterialTheme.colorScheme
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HapticIconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).background(cs.surfaceContainer, CircleShape)
            ) { Icon(ResIcons.ExpandMore, contentDescription = "Voltar", tint = cs.onSurfaceVariant) }
            Text(
                "TOCANDO DA BIBLIOTECA", Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), fontWeight = FontWeight.SemiBold,
                color = cs.primary
            )
            if (track != null) FavoriteButton(track.id, Modifier.size(40.dp)) else Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.weight(0.4f))
        // Capa 1:1, rounded-3xl, borda coral 2dp/30% e sombra com brilho coral.
        AlbumArt(
            track,
            Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = cs.primary, spotColor = cs.primary)
                .border(2.dp, cs.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            cornerRadius = 24.dp, iconSize = 96.dp, targetPx = CoverSize.NOW_PLAYING
        )
        Spacer(Modifier.weight(0.4f))
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(
                track?.title ?: "Nenhuma faixa", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track?.artist ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val album = track?.album.orEmpty()
            if (album.isNotBlank()) Text(
                "Álbum: $album", style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(24.dp))
        SeekBar(state.durationMs, positionMs, onSeekTo)
        Spacer(Modifier.height(8.dp))
        val active = cs.primary
        val inactive = cs.onSurfaceVariant
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            HapticIconButton(onClick = onToggleShuffle) {
                Icon(ResIcons.Shuffle, if (state.shuffleEnabled) "Aleatório ligado" else "Aleatório desligado",
                    tint = if (state.shuffleEnabled) active else inactive)
            }
            HapticIconButton(
                onClick = onPrevious, enabled = state.hasPrevious,
                modifier = Modifier.size(48.dp).background(cs.surfaceContainer, CircleShape)
            ) { Icon(ResIcons.SkipPrevious, "Anterior", Modifier.size(28.dp)) }
            HapticIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(80.dp).shadow(12.dp, CircleShape, ambientColor = cs.primary, spotColor = cs.primary)
                    .background(Brush.linearGradient(listOf(Coral, CoralHot, Amber)), CircleShape)
            ) {
                Icon(
                    if (state.isPlaying) ResIcons.Pause else ResIcons.Play,
                    if (state.isPlaying) "Pausar" else "Reproduzir", Modifier.size(40.dp), tint = Color.White
                )
            }
            HapticIconButton(
                onClick = onNext, enabled = state.hasNext,
                modifier = Modifier.size(48.dp).background(cs.surfaceContainer, CircleShape)
            ) { Icon(ResIcons.SkipNext, "Próxima", Modifier.size(28.dp)) }
            HapticIconButton(onClick = onCycleRepeatMode) {
                Icon(
                    if (state.repeatMode == RepeatMode.ONE) ResIcons.RepeatOne else ResIcons.Repeat,
                    when (state.repeatMode) {
                        RepeatMode.OFF -> "Repetir desligado"
                        RepeatMode.ALL -> "Repetir todas"
                        RepeatMode.ONE -> "Repetir faixa"
                    },
                    tint = if (state.repeatMode == RepeatMode.OFF) inactive else active
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        // Cartão de ações secundárias (sem volume: controle do sistema).
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainer)
                .border(1.dp, cs.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionPill("Equalizador", ResIcons.Tune, if (equalizer.state.enabled) active else inactive) { showEqualizer = true }
            ActionPill("Fila (${state.queue.size})", ResIcons.Queue, inactive) { showQueue = true }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (showEqualizer) EqualizerSheet(equalizer, onDismiss = { showEqualizer = false })
    if (showQueue) {
        QueueSheet(state, onDismiss = { showQueue = false }, onPlayFromQueue, onRemoveFromQueue)
    }
}

@Composable
private fun ActionPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.clip(CircleShape).background(cs.surfaceContainerHigh).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0) } // o padding de navigationBars é aplicado só na lista (sem duplicar)
    ) {
        Text("Fila (${state.queue.size})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        // Key estável sem depender do índice: id + n-ésima ocorrência do mesmo id (a fila pode repetir faixas).
        val queueKeys = remember(state.queue) {
            val seen = HashMap<Long, Int>()
            state.queue.map { t -> val n = seen.merge(t.id, 1, Int::plus)!!; "${t.id}#$n" }
        }
        val listState = rememberLazyListState()
        LaunchedEffect(Unit) { if (state.currentIndex > 0) listState.scrollToItem(state.currentIndex) }
        LazyColumn(state = listState, contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
            itemsIndexed(state.queue, key = { index, _ -> queueKeys[index] }) { index, t ->
                val current = index == state.currentIndex
                Row(
                    Modifier.fillMaxWidth().clickable { onPlay(index) }.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArt(t, Modifier.size(44.dp), iconSize = 20.dp, targetPx = CoverSize.LIST)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(t.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (current) Icon(ResIcons.Play, "Tocando", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    HapticIconButton(onClick = { onRemove(index) }) { Icon(ResIcons.Close, "Remover da fila") }
                }
            }
        }
    }
}

@Composable
private fun SeekBar(duration: Long, positionMs: () -> Long, onSeekTo: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val fraction = if (duration > 0) (positionMs().toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) dragValue else fraction
    Column {
        ResonanceSlider(
            value = shown,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                onSeekTo((dragValue * duration).toLong())
                dragging = false
            },
            enabled = duration > 0
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((shown * duration).toLong()), style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.primary)
            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.outline)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showSystemUi = true)
@Composable
private fun NowPlayingPreview() {
    MusicPlayerTheme(darkTheme = true) {
        Surface {
            NowPlayingScreen(
                PlayerUiState(Track(1, "Título da faixa", "Artista", "Álbum", 215_000, "content://x", null), true, 60_000, 215_000, hasNext = true),
                {}, {}, {}, {}, {}, {}, {}, {}, {},
                EqualizerController(EqualizerUiState(), {}, {}, { _, _ -> }, {})
            )
        }
    }
}
