package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.viewmodel.PlaylistUi
import com.example.musicplayer.viewmodel.PlaylistsUiState

@Composable
fun PlaylistsScreen(
    state: PlaylistsUiState,
    onOpen: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PlaylistUi?>(null) }
    var deleting by remember { mutableStateOf<PlaylistUi?>(null) }

    Column(modifier.fillMaxSize().topScreenInsets()) {
        ScreenHeader("Playlists", "${state.playlists.size} playlists") {
            FilledTonalButton(onClick = { showCreate = true }) {
                Icon(ResIcons.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nova")
            }
        }
        when {
            state.isLoading -> LazyColumn(
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, bottomPadding + 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = false
            ) { items(6) { TrackSkeletonItem() } }
            state.playlists.isEmpty() -> EmptyMessage(
                "Nenhuma playlist", "Crie sua primeira playlist para organizar suas músicas.",
                Modifier.padding(bottom = bottomPadding)
            ) { Button(onClick = { showCreate = true }) { Text("Criar playlist") } }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, bottomPadding + 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.playlists, key = { it.id }) { pl ->
                    PlaylistRow(pl, onClick = { onOpen(pl.id) }, onRename = { renaming = pl }, onDelete = { deleting = pl })
                }
            }
        }
    }

    if (showCreate) NameDialog("Nova playlist", "Criar", state.playlists.map { it.name }, onConfirm = onCreate, onDismiss = { showCreate = false })
    renaming?.let { pl ->
        NameDialog(
            "Renomear playlist", "Salvar", state.playlists.filter { it.id != pl.id }.map { it.name },
            onConfirm = { onRename(pl.id, it) }, onDismiss = { renaming = null }, initial = pl.name
        )
    }
    deleting?.let { pl ->
        ConfirmDialog(
            "Excluir playlist", "\"${pl.name}\" será excluída. As músicas continuam no dispositivo.", "Excluir",
            onConfirm = { onDelete(pl.id) }, onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun PlaylistRow(pl: PlaylistUi, onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.5f
    Surface(
        onClick = onClick, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = if (dark) cs.surfaceContainer else cs.surfaceContainerLow,
        border = if (dark) null else androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                AlbumArt(pl.tracks.firstOrNull(), Modifier.fillMaxSize(), cornerRadius = 12.dp, iconSize = 22.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pl.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (pl.trackCount == 1) "1 faixa" else "${pl.trackCount} faixas",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                )
            }
            var open by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { open = true }) { Icon(ResIcons.MoreVert, "Opções da playlist", tint = cs.onSurfaceVariant) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text("Renomear") }, onClick = { open = false; onRename() })
                    DropdownMenuItem(text = { Text("Excluir") }, onClick = { open = false; onDelete() })
                }
            }
        }
    }
}
