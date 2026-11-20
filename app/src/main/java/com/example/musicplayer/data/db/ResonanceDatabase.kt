package com.example.musicplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, PlayCountEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ResonanceDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao

    companion object {
        @Volatile private var instance: ResonanceDatabase? = null

        fun get(context: Context): ResonanceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, ResonanceDatabase::class.java, "resonance.db"
            ).build().also { instance = it }
        }
    }
}
