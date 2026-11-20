package com.example.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreTrackRepository(private val context: Context) {

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val result = ArrayList<Track>()
        context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val albumId = c.getLong(albumIdCol)
                val artist = c.getString(artistCol)
                    ?.takeUnless { it.isBlank() || it == "<unknown>" }
                val album = c.getString(albumCol)?.takeUnless { it.isBlank() }
                result += Track(
                    id = id,
                    title = c.getString(titleCol)?.takeUnless { it.isBlank() } ?: "Faixa desconhecida",
                    artist = artist ?: "Artista desconhecido",
                    album = album ?: "Álbum desconhecido",
                    durationMs = c.getLong(durCol).coerceAtLeast(0L),
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    // Uri de capa por álbum; Coil/loader da UI trata ausência. Válido em API 24-34.
                    albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), albumId
                        ).toString()
                    } else null,
                    dateAddedSec = c.getLong(addedCol).coerceAtLeast(0L)
                )
            }
        }
        result
    }
}
