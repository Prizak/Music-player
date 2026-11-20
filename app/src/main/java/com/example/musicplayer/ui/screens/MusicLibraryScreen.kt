package com.example.musicplayer.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.example.musicplayer.ui.theme.BrandGradient
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.musicplayer.data.prefs.SortOrder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Track
import com.example.musicplayer.ui.components.ResIcons
import com.example.musicplayer.ui.components.pressScale
import com.example.musicplayer.ui.components.ResonanceChip
import com.example.musicplayer.ui.components.TrackItem
import com.example.musicplayer.ui.components.TrackMenuAction
import com.example.musicplayer.ui.components.TrackSkeletonItem
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.example.musicplayer.viewmodel.LibraryEmptyReason
import com.example.musicplayer.viewmodel.LibraryUiState

private val SortChips = listOf(
    SortOrder.TITLE to "A-Z", SortOrder.ARTIST to "Artista", SortOrder.ALBUM to "Álbum",
    SortOrder.RECENT to "Recentes", SortOrder.MOST_PLAYED to "Mais tocadas"
)

val AudioPermission: String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

/**
 * @param bottomPadding espaço no fim da lista: o chamador passa a altura do MiniPlayer
 * (quando visível) ou o inset das navigation bars (quando não há MiniPlayer).
 */
@Composable
fun MusicLibraryScreen(
    state: LibraryUiState,
    currentTrackId: Long?,
    onQueryChange: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShuffleAll: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onPermissionResult)

    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)).imePadding()) {
        // Barra de busca no topo (como music_library_light).
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.hasPermission) {
                Box(Modifier.weight(1f)) { SearchField(state.query, onQueryChange) }
            } else {
                Text("Resonance", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            }
        }
        if (state.hasPermission && state.emptyReason != LibraryEmptyReason.ERROR) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Faixas", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    if (!state.isLoading) Text(
                        "${state.tracks.size} arquivos locais",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "fav") {
                    ResonanceChip("♥ Favoritas", selected = state.favoritesOnly, onClick = onToggleFavoritesFilter)
                }
                items(SortChips, key = { it.first }) { (order, label) ->
                    ResonanceChip(label, selected = state.sortOrder == order, onClick = { onSortOrderChange(order) })
                }
            }
        }
        val listState = rememberLazyListState()
        // Ao trocar a ordenação, volta ao topo (a posição antiga não faria sentido na nova ordem).
        // Só quando a ordenação MUDA (não ao reentrar na tela/voltar de Settings/girar, o que perderia a rolagem).
        var lastSortOrder by rememberSaveable { mutableStateOf(state.sortOrder) }
        LaunchedEffect(state.sortOrder) {
            if (state.sortOrder != lastSortOrder) {
                lastSortOrder = state.sortOrder
                listState.scrollToItem(0)
            }
        }
        val listPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 88.dp)

        when {
            state.isLoading -> LazyColumn(contentPadding = listPadding, userScrollEnabled = false, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(10) { TrackSkeletonItem() }
            }
            state.emptyReason == LibraryEmptyReason.NO_PERMISSION -> MessageState(
                title = "Permissão necessária",
                message = "Permita o acesso aos arquivos de áudio para listar suas músicas.",
                actionLabel = "Conceder permissão",
                onAction = { launcher.launch(AudioPermission) }
            )
            state.emptyReason == LibraryEmptyReason.ERROR -> MessageState(
                title = "Algo deu errado",
                message = state.errorMessage ?: "Não foi possível ler suas músicas.",
                actionLabel = "Tentar novamente",
                onAction = onRetry
            )
            state.emptyReason == LibraryEmptyReason.NO_TRACKS -> MessageState(
                title = "Nenhuma música encontrada",
                message = "Não há músicas neste dispositivo.",
                actionLabel = "Atualizar",
                onAction = onRetry
            )
            state.emptyReason == LibraryEmptyReason.NO_SEARCH_RESULTS -> MessageState(
                title = "Sem resultados",
                message = if (state.query.isBlank()) "Nenhuma música favorita ainda. Toque no coração de uma faixa." else "Nada encontrado para \"${state.query}\".",
                actionLabel = null,
                onAction = {}
            )
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filteredTracks, key = { it.id }) { track ->
                    TrackItem(
                        track, isCurrent = track.id == currentTrackId, onClick = { onTrackClick(track) },
                        menu = listOf(TrackMenuAction("Adicionar à playlist") { onAddToPlaylist(track) })
                    )
                }
            }
        }
    }
    if (state.hasPermission && !state.isLoading && state.filteredTracks.isNotEmpty()) {
        ShuffleAllFab(
            onClick = onShuffleAll,
            modifier = Modifier.align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.End))
                .padding(end = 16.dp, bottom = bottomPadding + 16.dp)
        )
    }
    }
}

/** Botão flutuante "Shuffle All": pílula com gradiente da marca, acima do MiniPlayer. */
@Composable
private fun ShuffleAllFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(
        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onClick() },
        interactionSource = source,
        modifier = modifier.pressScale(source, 0.95f), shape = RoundedCornerShape(50),
        color = androidx.compose.ui.graphics.Color.Transparent, shadowElevation = 8.dp
    ) {
        Row(
            Modifier.background(BrandGradient).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ResIcons.Shuffle, null, Modifier.size(20.dp), tint = androidx.compose.ui.graphics.Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Shuffle All", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.White)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar músicas, artistas, álbuns", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(ResIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                Icon(ResIcons.Close, contentDescription = "Limpar busca")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceContainerLow,
            unfocusedContainerColor = cs.surfaceContainerLow,
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = cs.outlineVariant.copy(alpha = 0.6f),
            focusedPlaceholderColor = cs.onSurfaceVariant,
            unfocusedPlaceholderColor = cs.onSurfaceVariant
        )
    )
}

@Composable
private fun MessageState(title: String, message: String, actionLabel: String?, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(ResIcons.MusicNote, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LibraryPermissionPreview() {
    MusicPlayerTheme(darkTheme = true) {
        MusicLibraryScreen(
            LibraryUiState(emptyReason = LibraryEmptyReason.NO_PERMISSION), null, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
