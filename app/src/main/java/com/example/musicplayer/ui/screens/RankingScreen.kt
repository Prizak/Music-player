package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.data.Track
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.ui.theme.*
import com.example.musicplayer.viewmodel.RankingUiState

@Composable
fun RankingScreen(
    state: RankingUiState,
    currentTrackId: Long?,
    onPlay: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    Column(modifier.fillMaxSize().topScreenInsets()) {
        ScreenHeader("Mais tocadas", "Baseado nas suas reproduções", badge = { TopBadge() })
        if (state.top10.isEmpty()) {
            EmptyMessage("Ainda sem reproduções", "Ouça algumas músicas e elas aparecerão aqui.", Modifier.padding(bottom = bottomPadding))
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, bottomPadding + 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.top10, key = { _, e -> e.track.id }) { index, e ->
                    if (index == 0) {
                        PodiumCard(e.track, e.playCount, e.track.id == currentTrackId, { onPlay(e.track) })
                    } else {
                        if (index == 1) SectionLabel("Ranking #2 – #${state.top10.size}")
                        val rankColor = when (e.rank) { 2 -> Coral; 3 -> Amber; 4 -> Coral; 5 -> Amber; 6 -> Cyan; else -> MaterialTheme.colorScheme.outline }
                        val strip = when (e.rank) {
                            2 -> Brush.verticalGradient(listOf(Coral, Amber))
                            3 -> Brush.verticalGradient(listOf(Amber, Cyan))
                            else -> null
                        }
                        TrackItem(
                            e.track, isCurrent = e.track.id == currentTrackId, onClick = { onPlay(e.track) },
                            menu = listOf(TrackMenuAction("Adicionar à playlist") { onAddToPlaylist(e.track) }),
                            stripBrush = strip,
                            outlineColor = when (e.rank) { 2 -> Coral.copy(alpha = 0.3f); 3 -> Amber.copy(alpha = 0.3f); else -> null },
                            leading = {
                                Text(
                                    "${e.rank}", color = rankColor,
                                    fontWeight = FontWeight.Bold,
                                    style = if (e.rank <= 3) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center, modifier = Modifier.width(32.dp)
                                )
                            },
                            trailing = { PlayCount(e.playCount, highlight = e.rank <= 3) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBadge() {
    Row(
        Modifier.clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(Coral.copy(alpha = 0.2f), Coral.copy(alpha = 0.1f), Amber.copy(alpha = 0.2f))))
            .border(1.dp, Coral.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TOP 10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Coral)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.outline)
        Text("Todos os tempos", style = MaterialTheme.typography.labelSmall, color = Coral)
    }
}

@Composable
private fun PlayCount(count: Int, highlight: Boolean) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            "$count", style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = if (highlight) Cyan else MaterialTheme.colorScheme.onSurface
        )
        Text(
            if (count == 1) "reprodução" else "reproduções", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/** Cartão do #1 (card-rank do Stitch): rounded-3xl, borda coral 2dp, capa 64dp com anel âmbar. */
@Composable
private fun PodiumCard(track: Track, playCount: Int, isCurrent: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(cs.surfaceContainerHigh, cs.surfaceContainer, cs.surfaceContainerHigh)))
            .border(2.dp, Coral.copy(alpha = if (isCurrent) 0.9f else 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(ResIcons.Leaderboard, null, tint = Coral, modifier = Modifier.size(20.dp))
            Text("1", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Amber)
        }
        Spacer(Modifier.width(12.dp))
        AlbumArt(
            track, Modifier.size(64.dp).border(2.dp, Coral.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            cornerRadius = 16.dp, iconSize = 28.dp, targetPx = CoverSize.LIST
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (playCount == 1) "1 reprodução" else "$playCount reproduções",
                    style = MaterialTheme.typography.labelSmall, color = Cyan
                )
                Text("  •  ", style = MaterialTheme.typography.labelSmall, color = cs.outline)
                Text("Campeã", style = MaterialTheme.typography.labelSmall, color = Amber)
            }
        }
    }
}
