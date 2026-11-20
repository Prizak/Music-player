package com.example.musicplayer.viewmodel

/** Uma banda do equalizador. Níveis em milibels (100 mB = 1 dB). */
data class EqBandUi(val index: Int, val centerFreqHz: Int, val levelMb: Int)

data class EqualizerUiState(
    val isAvailable: Boolean = false,
    val enabled: Boolean = false,
    val bands: List<EqBandUi> = emptyList(),
    val minLevelMb: Int = 0,
    val maxLevelMb: Int = 0,
    val presets: List<String> = emptyList(),
    /** -1 = Personalizado. */
    val selectedPreset: Int = -1
)
