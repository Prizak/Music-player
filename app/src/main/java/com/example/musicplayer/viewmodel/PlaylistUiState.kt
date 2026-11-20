package com.example.musicplayer.viewmodel

import com.example.musicplayer.data.Track

data class PlaylistUi(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val tracks: List<Track>
)

data class PlaylistsUiState(
    val playlists: List<PlaylistUi> = emptyList(),
    val isLoading: Boolean = true
)

data class RankingEntry(
    val track: Track,
    val playCount: Int,
    val rank: Int
)

data class RankingUiState(
    val top10: List<RankingEntry> = emptyList()
)

/** Canal simples de feedback (erros de validação e confirmações). */
sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
}
