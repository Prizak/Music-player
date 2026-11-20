package com.example.musicplayer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"], childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("playlistId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAt: Long
)

@Entity(tableName = "play_counts")
data class PlayCountEntity(
    @PrimaryKey val trackId: Long,
    val playCount: Int,
    val lastPlayedAt: Long
)
