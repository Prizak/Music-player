package com.example.musicplayer.viewmodel

import com.example.musicplayer.data.Track
import com.example.musicplayer.data.prefs.SortOrder

enum class LibraryEmptyReason { NONE, NO_PERMISSION, NO_TRACKS, NO_SEARCH_RESULTS, ERROR }

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val filteredTracks: List<Track> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val emptyReason: LibraryEmptyReason = LibraryEmptyReason.NONE,
    val errorMessage: String? = null,
    val sortOrder: SortOrder = SortOrder.TITLE,
    val favoritesOnly: Boolean = false   // filtro "Favoritas" (em memória), combinado com busca e ordenação
)
