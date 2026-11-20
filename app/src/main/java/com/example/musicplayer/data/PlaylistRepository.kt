package com.example.musicplayer.data

import com.example.musicplayer.data.db.PlaylistDao
import com.example.musicplayer.data.db.PlaylistEntity
import com.example.musicplayer.data.db.PlaylistTrackEntity
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val dao: PlaylistDao) {
    fun observePlaylists(): Flow<List<PlaylistEntity>> = dao.observePlaylists()
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>> = dao.observeAllPlaylistTracks()

    suspend fun create(name: String): Long =
        dao.insert(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    suspend fun rename(id: Long, name: String) = dao.rename(id, name)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun getAll(): List<PlaylistEntity> = dao.getPlaylists()
    suspend fun getTrackLinks(playlistId: Long): List<PlaylistTrackEntity> = dao.getTracks(playlistId)

    suspend fun createWithTrack(name: String, trackId: Long): Long =
        dao.createWithTrack(name, trackId, System.currentTimeMillis())

    /** @return false se a faixa já estava na playlist. */
    suspend fun addTrack(playlistId: Long, trackId: Long): Boolean =
        dao.insertTrack(playlistId, trackId, System.currentTimeMillis())

    suspend fun removeTrack(playlistId: Long, trackId: Long) = dao.deleteTrack(playlistId, trackId)
}
