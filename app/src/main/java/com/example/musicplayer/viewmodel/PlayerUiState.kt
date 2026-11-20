package com.example.musicplayer.viewmodel

import com.example.musicplayer.data.Track

enum class RepeatMode { OFF, ALL, ONE }

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)
