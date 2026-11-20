package com.example.musicplayer.data

import com.example.musicplayer.data.db.PlayCountDao
import com.example.musicplayer.data.db.PlayCountEntity
import kotlinx.coroutines.flow.Flow

class PlayCountRepository(private val dao: PlayCountDao) {
    fun observeAll(): Flow<List<PlayCountEntity>> = dao.observeAll()
    suspend fun resetAll() = dao.deleteAll()
    suspend fun increment(trackId: Long) = dao.increment(trackId, System.currentTimeMillis())
}
