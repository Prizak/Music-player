package com.example.musicplayer.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import com.example.musicplayer.data.PlayCountRepository
import com.example.musicplayer.data.db.ResonanceDatabase
import com.example.musicplayer.data.prefs.SettingsRepository
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var playCounter: PlayCounter? = null
    private var artworkLoader: ArtworkBitmapLoader? = null

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exo
        EqualizerManager.attach(exo, exo.audioSessionId)
        val settings = SettingsRepository(applicationContext)
        playCounter = PlayCounter(
            exo,
            PlayCountRepository(ResonanceDatabase.get(applicationContext).playCountDao()),
            onListened = { delta ->
                try { settings.addListenedMs(delta) } catch (e: java.io.IOException) { /* perde só este lote */ }
            }
        )
        val loader = ArtworkBitmapLoader(applicationContext)
        artworkLoader = loader
        mediaSession = MediaSession.Builder(this, exo).setBitmapLoader(loader).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Grava antes de o super possivelmente encerrar o serviço (Media3 1.8 chama
        // pauseAllPlayersAndStopSelf quando não há reprodução em andamento; o stopSelf manual era redundante).
        playCounter?.flush()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        EqualizerManager.release()
        playCounter?.release()
        playCounter = null
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        artworkLoader?.shutdown()
        artworkLoader = null
        super.onDestroy()
    }
}
