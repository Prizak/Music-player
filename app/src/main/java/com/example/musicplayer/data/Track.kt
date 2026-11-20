package com.example.musicplayer.data

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val dateAddedSec: Long = 0L
)
