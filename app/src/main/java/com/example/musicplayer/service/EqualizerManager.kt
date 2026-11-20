package com.example.musicplayer.service

import android.media.audiofx.Equalizer
import androidx.media3.common.C
import androidx.media3.common.Player
import com.example.musicplayer.data.prefs.EqSettings
import com.example.musicplayer.viewmodel.EqBandUi
import com.example.musicplayer.viewmodel.EqualizerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Equalizador DSP (android.media.audiofx.Equalizer) ligado ao audioSessionId real do ExoPlayer.
 *
 * Singleton de processo (serviço e ViewModel rodam no mesmo processo). Concorrência: todo acesso
 * ao estado e ao efeito é protegido por [lock] (operações do Equalizer são rápidas), então comandos
 * do ViewModel e callbacks da thread principal do serviço nunca se intercalam.
 * Nenhum tipo de AudioEffect vaza para fora deste arquivo.
 */
object EqualizerManager {

    private val lock = Any()

    private var effect: Equalizer? = null
    private var player: Player? = null
    private var sessionId = C.AUDIO_SESSION_ID_UNSET

    // Estado desejado (persistido). Vale mesmo sem serviço/efeito.
    private var enabled = false
    private var presetIndex = -1
    private var customLevels: List<Int> = emptyList()
    private var restored = false

    private val _ui = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _ui.asStateFlow()

    private val _settings = MutableStateFlow(EqSettings())
    /** Estado desejado para o ViewModel persistir. */
    val settings: StateFlow<EqSettings> = _settings.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            synchronized(lock) { bind(audioSessionId) }
        }
    }

    /** Carrega o estado persistido (uma vez por processo; chamadas seguintes são ignoradas). */
    fun restore(saved: EqSettings) = synchronized(lock) {
        if (restored) return
        restored = true
        enabled = saved.enabled
        presetIndex = saved.presetIndex
        customLevels = saved.bandLevelsMb
        if (effect != null) applyDesired()
        publish()
    }

    fun attach(p: Player, currentSessionId: Int) = synchronized(lock) {
        player?.removeListener(listener)
        player = p
        p.addListener(listener)
        bind(currentSessionId)
    }

    fun release() = synchronized(lock) {
        player?.removeListener(listener)
        player = null
        sessionId = C.AUDIO_SESSION_ID_UNSET
        releaseEffect()
        publish()
    }

    fun setEnabled(value: Boolean) = synchronized(lock) {
        enabled = value
        effect?.let { safe { it.enabled = value } }
        publish()
    }

    fun selectPreset(index: Int) = synchronized(lock) {
        val eq = effect
        if (index < 0) {
            // "Personalizado": volta aos níveis custom salvos (selecionar preset não os apaga).
            presetIndex = -1
            eq?.let { applyLevels(it, customLevels) }
        } else if (eq != null) {
            val ok = safe {
                require(index < eq.numberOfPresets.toInt())
                eq.usePreset(index.toShort())
            }
            if (ok) presetIndex = index
        } else {
            presetIndex = index
        }
        publish()
    }

    fun setBandLevel(band: Int, levelMb: Int) = synchronized(lock) {
        val eq = effect ?: return
        val cur = _ui.value
        if (band !in cur.bands.indices) return
        val levels = cur.bands.map { it.levelMb }.toMutableList()
        levels[band] = levelMb.coerceIn(cur.minLevelMb, cur.maxLevelMb)
        customLevels = levels
        presetIndex = -1
        applyLevels(eq, levels)
        publish()
    }

    fun reset() = synchronized(lock) {
        presetIndex = -1
        customLevels = emptyList()
        effect?.let { applyDesired() }
        publish()
    }

    // ---- internos (sempre sob lock) ----

    private fun bind(newId: Int) {
        if (newId == C.AUDIO_SESSION_ID_UNSET) return
        if (newId == sessionId && effect != null) return
        sessionId = newId
        releaseEffect()
        effect = try {
            Equalizer(0, newId)
        } catch (e: Exception) {
            null
        }
        if (effect != null) applyDesired()
        publish()
    }

    private fun applyDesired() {
        val eq = effect ?: return
        val ok = safe {
            eq.enabled = enabled
            if (presetIndex in 0 until eq.numberOfPresets.toInt()) {
                eq.usePreset(presetIndex.toShort())
            } else {
                presetIndex = -1
                applyLevels(eq, customLevels)
            }
        }
        if (!ok) releaseEffect()
    }

    private fun applyLevels(eq: Equalizer, levels: List<Int>) {
        safe {
            val range = eq.bandLevelRange
            for (b in 0 until eq.numberOfBands.toInt()) {
                val l = (levels.getOrNull(b) ?: 0).coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(b.toShort(), l.toShort())
            }
        }
    }

    private fun releaseEffect() {
        effect?.let { safe { it.release() } }
        effect = null
    }

    private fun publish() {
        _settings.value = EqSettings(enabled, presetIndex, customLevels)
        val eq = effect
        var state = EqualizerUiState(enabled = enabled, selectedPreset = presetIndex)
        if (eq != null) {
            try {
                val range = eq.bandLevelRange
                state = EqualizerUiState(
                    isAvailable = true,
                    enabled = enabled,
                    bands = (0 until eq.numberOfBands.toInt()).map { b ->
                        EqBandUi(b, eq.getCenterFreq(b.toShort()) / 1000, eq.getBandLevel(b.toShort()).toInt())
                    },
                    minLevelMb = range[0].toInt(),
                    maxLevelMb = range[1].toInt(),
                    presets = (0 until eq.numberOfPresets.toInt()).map { eq.getPresetName(it.toShort()) },
                    selectedPreset = presetIndex
                )
            } catch (e: Exception) {
                releaseEffect()
                state = EqualizerUiState(enabled = enabled, selectedPreset = presetIndex)
            }
        }
        _ui.value = state
    }

    private inline fun safe(block: () -> Unit): Boolean = try {
        block()
        true
    } catch (e: Exception) { // RuntimeException, UnsupportedOperationException, IllegalStateException...
        false
    }
}
