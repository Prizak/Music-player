package com.example.musicplayer.service

import android.os.Handler
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.musicplayer.data.PlayCountRepository
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Contabiliza reproduções (CONTRACTS 8.4) acoplado ao Player do serviço, para contar mesmo
 * sem Activity/ViewModel. Todos os métodos e callbacks rodam na thread do player (Main).
 *
 * Regras: tempo OUVIDO (relógio, só com isPlaying) >= min(30 s, duração/2) (30 s se duração
 * desconhecida) conta uma vez por execução. Fim natural (STATE_ENDED / transição AUTO ou REPEAT)
 * conta só se tempoOuvido >= min(limiar, 5 s) (o limiar completo após qualquer seek para frente)
 * e o fim não veio de um seek até ~1 s do final.
 */
class PlayCounter(
    private val player: Player,
    private val repository: PlayCountRepository,
    private val onListened: suspend (Long) -> Unit = {}
) : Player.Listener {

    // Job completável: release() o completa para deixar as escritas em andamento terminarem
    // (não as cancela) e rejeitar novas.
    private val job: CompletableJob = SupervisorJob()
    private val ioScope = CoroutineScope(job + Dispatchers.IO)
    private val handler = Handler(player.applicationLooper)

    private var trackId: Long? = null
    private var metaDurationMs = 0L
    private var listenedMs = 0L
    private var playingSince = 0L       // elapsedRealtime; 0 = não está ouvindo
    private var counted = false
    private var seekedNearEnd = false
    private var seekedForward = false
    private var released = false
    private var lastState = Player.STATE_IDLE
    private var pendingTotalMs = 0L     // tempo ouvido ainda não persistido (total geral)

    private val tick = object : Runnable {
        override fun run() {
            if (released) return
            checkThreshold()
            accrue()
            if (pendingTotalMs >= PERSIST_EVERY_MS) persistTotal()
            if (playingSince != 0L) handler.postDelayed(this, TICK_MS)
        }
    }

    init {
        player.addListener(this)
        lastState = player.playbackState
        startRun(player.currentMediaItem)
    }

    // ---- núcleo ----

    private fun accrue() {
        if (playingSince != 0L) {
            val now = SystemClock.elapsedRealtime()
            val delta = now - playingSince
            listenedMs += delta
            pendingTotalMs += delta
            playingSince = now
        }
    }

    /**
     * Duração da execução atual. Em onMediaItemTransition/onPositionDiscontinuity o player já aponta
     * para o PRÓXIMO item; nesse caso player.duration não é desta execução e vale o MediaMetadata.
     */
    private fun runDurationMs(): Long {
        val sameItem = trackId != null && player.currentMediaItem?.mediaId == trackId.toString()
        val playerDur = if (sameItem) player.duration.takeIf { it > 0 } else null
        return playerDur ?: metaDurationMs
    }

    private fun thresholdMs(): Long {
        val dur = runDurationMs()
        return if (dur > 0) minOf(MAX_THRESHOLD_MS, dur / 2) else MAX_THRESHOLD_MS
    }

    private fun checkThreshold() {
        if (counted || trackId == null) return
        accrue()
        if (listenedMs >= thresholdMs()) register()
    }

    /** Fim natural da execução: exige tempo ouvido mínimo e que não tenha sido seek até o fim. */
    private fun naturalEnd() {
        if (counted || trackId == null) return
        accrue()
        // Após seek para frente, o fim natural exige o limiar completo (não só o mínimo de 5 s).
        val needed = if (seekedForward) thresholdMs() else minOf(thresholdMs(), MIN_NATURAL_MS)
        if (!seekedNearEnd && listenedMs >= needed) register()
    }

    private fun persistTotal() {
        val delta = pendingTotalMs
        if (delta <= 0) return
        pendingTotalMs = 0L
        ioScope.launch { onListened(delta) }
    }

    private fun register() {
        val id = trackId ?: return
        if (counted || id < 0) return
        counted = true
        ioScope.launch { repository.increment(id) }
    }

    private fun startRun(item: MediaItem?) {
        trackId = item?.mediaId?.toLongOrNull()
        metaDurationMs = item?.mediaMetadata?.durationMs ?: 0L
        listenedMs = 0L
        counted = false
        seekedNearEnd = false
        seekedForward = false
        playingSince = 0L
        if (trackId != null && player.isPlaying) beginListening()
    }

    private fun beginListening() {
        if (playingSince == 0L) {
            playingSince = SystemClock.elapsedRealtime()
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, TICK_MS)
        }
    }

    private fun stopListening() {
        accrue()
        persistTotal()
        playingSince = 0L
        handler.removeCallbacks(tick)
    }

    /** Grava uma execução já acima do limiar (ex.: onTaskRemoved). Não aplica regra de fim natural. */
    fun flush() {
        if (released) return
        checkThreshold()
        accrue()
        persistTotal()
    }

    fun release() {
        if (released) return
        checkThreshold()
        accrue()
        persistTotal()
        released = true
        handler.removeCallbacks(tick)
        player.removeListener(this)
        job.complete()
    }

    // ---- Player.Listener ----

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
        ) naturalEnd() else checkThreshold()
        stopListening()
        startRun(mediaItem)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK &&
            reason != Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
        ) return
        if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) return // pulo de faixa
        // Crédito de fim natural só vale se o tempo foi realmente ouvido antes do seek.
        checkThreshold()
        if (newPosition.positionMs > oldPosition.positionMs) seekedForward = true
        val dur = runDurationMs()
        seekedNearEnd = dur > 0 && dur - newPosition.positionMs < NEAR_END_MS
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (trackId != null) beginListening()
        } else {
            stopListening()
            checkThreshold()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            naturalEnd()
            stopListening()
        } else if (lastState == Player.STATE_ENDED) {
            // Retomada/replay depois do fim: nova execução do item atual.
            startRun(player.currentMediaItem)
        }
        lastState = playbackState
    }

    private companion object {
        const val TICK_MS = 500L
        const val PERSIST_EVERY_MS = 10_000L
        const val MAX_THRESHOLD_MS = 30_000L
        const val MIN_NATURAL_MS = 5_000L
        const val NEAR_END_MS = 1_000L
    }
}
