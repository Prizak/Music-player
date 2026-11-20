package com.example.musicplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Track
import com.example.musicplayer.viewmodel.PlaylistUi

private const val MAX_NAME = 50

/**
 * Diálogo de nome (criar/renomear) com pré-validação local: vazio, > 50 caracteres ou duplicado
 * (ignoreCase) em [existingNames]. Não fecha se inválido e preserva o texto. O ViewModel valida de novo.
 */
@Composable
fun NameDialog(
    title: String,
    confirmLabel: String,
    existingNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = ""
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    fun submit() {
        val name = text.trim()
        error = when {
            name.isEmpty() -> "Informe um nome"
            name.length > MAX_NAME -> "No máximo $MAX_NAME caracteres"
            existingNames.any { it.equals(name, ignoreCase = true) } -> "Já existe uma playlist com esse nome"
            else -> null
        }
        if (error == null) { onConfirm(name); onDismiss() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_NAME); error = null },
                label = { Text("Nome da playlist") },
                singleLine = true,
                isError = error != null,
                supportingText = { Text(error ?: "${text.length}/$MAX_NAME") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { submit() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Seletor de playlist para adicionar [track]; permite criar uma nova (ela aparece na lista em seguida). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    playlists: List<PlaylistUi>,
    onPick: (playlistId: Long) -> Unit,
    onCreateAndAdd: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, contentWindowInsets = { WindowInsets(0) }) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Adicionar à playlist", style = MaterialTheme.typography.titleLarge)
            Text(
                track.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ResIcons.Add, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text("Nova playlist", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        LazyColumn(contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
            items(playlists, key = { it.id }) { pl ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(pl.id); onDismiss() }.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(ResIcons.Queue, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${pl.trackCount} faixas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (creating) {
        NameDialog(
            "Nova playlist", "Criar", existingNames = playlists.map { it.name },
            onConfirm = { onCreateAndAdd(it); onDismiss() }, // fecha o sheet para o Snackbar (janela principal) ficar visível
            onDismiss = { creating = false }
        )
    }
}
