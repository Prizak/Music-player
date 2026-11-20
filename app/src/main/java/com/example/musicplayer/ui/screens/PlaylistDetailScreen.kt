package com.example.musicplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Track
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.viewmodel.PlaylistUi

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistUi,
    currentTrackId: Long?,
    onBack: () -> Unit,
    onPlay: (startIndex: Int, shuffle: Boolean) -> Unit,
    onRemoveTrack: (trackId: Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    BackHandler(onBack = onBack)
    Column(modifier.fillMaxSize().topScreenInsets()) {
        ScreenHeader(
            playlist.name, if (playlist.trackCount == 1) "1 faixa" else "${playlist.trackCount} faixas",
            leading = { IconButton(onClick = onBack) { Icon(ResIcons.ArrowBack, "Voltar") } }
        )
        if (playlist.tracks.isEmpty()) {
            EmptyMessage(
                "Playlist vazia", "Use \"Adicionar à playlist\" no menu de uma faixa para incluí-la aqui.",
                Modifier.padding(bottom = bottomPadding)
            )
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPlay(0, false) }, modifier = Modifier.weight(1f)) {
                    Icon(ResIcons.Play, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Tocar")
                }
                FilledTonalButton(onClick = { onPlay(0, true) }, modifier = Modifier.weight(1f)) {
                    Icon(ResIcons.Shuffle, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Aleatório")
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, bottomPadding + 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlist.tracks, key = { _, t -> t.id }) { index, track ->
                    TrackItem(
                        track, isCurrent = track.id == currentTrackId, onClick = { onPlay(index, false) },
                        menu = listOf(
                            TrackMenuAction("Adicionar a outra playlist") { onAddToPlaylist(track) },
                            TrackMenuAction("Remover desta playlist") { onRemoveTrack(track.id) }
                        )
                    )
                }
            }
        }
    }
}
