package com.example.musicplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC, id ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId ASC, position ASC")
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlists")
    suspend fun getPlaylists(): List<PlaylistEntity>

    /** Cria a playlist e adiciona a faixa (posição 0) na mesma transação. */
    @Transaction
    suspend fun createWithTrack(name: String, trackId: Long, now: Long): Long {
        val id = insert(PlaylistEntity(name = name, createdAt = now))
        insertTrackRaw(PlaylistTrackEntity(id, trackId, 0, now))
        return id
    }

    @Insert
    suspend fun insertTrackRaw(track: PlaylistTrackEntity)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun countTrack(playlistId: Long, trackId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    /** Insere no fim; retorna false se a faixa já existe na playlist. */
    @Transaction
    suspend fun insertTrack(playlistId: Long, trackId: Long, now: Long): Boolean {
        if (countTrack(playlistId, trackId) > 0) return false
        insertTrackRaw(PlaylistTrackEntity(playlistId, trackId, maxPosition(playlistId) + 1, now))
        return true
    }

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deleteTrackRaw(playlistId: Long, trackId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTracks(playlistId: Long): List<PlaylistTrackEntity>

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun setPosition(playlistId: Long, trackId: Long, position: Int)

    /** Remove a faixa e reindexa position para ficar contíguo (0-based). */
    @Transaction
    suspend fun deleteTrack(playlistId: Long, trackId: Long) {
        deleteTrackRaw(playlistId, trackId)
        getTracks(playlistId).forEachIndexed { i, t ->
            if (t.position != i) setPosition(playlistId, t.trackId, i)
        }
    }
}

@Dao
interface PlayCountDao {
    @Query("SELECT * FROM play_counts ORDER BY playCount DESC, lastPlayedAt DESC")
    fun observeAll(): Flow<List<PlayCountEntity>>

    @Query("UPDATE play_counts SET playCount = playCount + 1, lastPlayedAt = :now WHERE trackId = :trackId")
    suspend fun bump(trackId: Long, now: Long): Int

    @Query("DELETE FROM play_counts")
    suspend fun deleteAll()

    @Insert
    suspend fun insertNew(entity: PlayCountEntity)

    /** Upsert + incremento atômico em uma transação. */
    @Transaction
    suspend fun increment(trackId: Long, now: Long) {
        if (bump(trackId, now) == 0) insertNew(PlayCountEntity(trackId, 1, now))
    }
}
