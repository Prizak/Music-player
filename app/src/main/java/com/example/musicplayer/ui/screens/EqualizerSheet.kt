package com.example.musicplayer.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.ResonanceChip
import com.example.musicplayer.ui.components.ResonanceSlider
import com.example.musicplayer.viewmodel.EqBandUi
import com.example.musicplayer.viewmodel.EqualizerUiState
import java.util.Locale

/** Agrupa estado + callbacks do equalizador para repassar às telas. */
class EqualizerController(
    val state: EqualizerUiState,
    val onSetEnabled: (Boolean) -> Unit,
    val onSelectPreset: (Int) -> Unit,
    val onSetBandLevel: (Int, Int) -> Unit,
    val onReset: () -> Unit
)

private val PtBr = Locale("pt", "BR")

internal fun formatFreq(hz: Int): String =
    if (hz < 1000) "$hz Hz" else {
        val s = String.format(PtBr, "%.1f", hz / 1000.0).removeSuffix(",0")
        "$s kHz"
    }

internal fun formatDb(mb: Int): String {
    val db = mb / 100f
    return when {
        mb == 0 -> "0 dB"
        mb % 100 == 0 -> String.format(PtBr, "%+d dB", mb / 100)
        else -> String.format(PtBr, "%+.1f dB", db)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(controller: EqualizerController, onDismiss: () -> Unit) {
    val state = controller.state
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = { WindowInsets(0) } // insets de navegação aplicados no conteúdo
    ) {
        Column(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Equalizador", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                if (state.isAvailable) {
                    Switch(
                        checked = state.enabled, onCheckedChange = controller.onSetEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            if (!state.isAvailable) {
                Text(
                    "Equalizador indisponível neste dispositivo",
                    Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.presets) { i, name ->
                    ResonanceChip(name, selected = state.selectedPreset == i, enabled = state.enabled, onClick = { controller.onSelectPreset(i) })
                }
                item {
                    ResonanceChip("Personalizado", selected = state.selectedPreset == -1, enabled = state.enabled, onClick = { controller.onSelectPreset(-1) })
                }
            }
            Spacer(Modifier.height(16.dp))
            // Sliders ficam desabilitados (não interativos e atenuados) com o equalizador desligado.
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                state.bands.forEach { band ->
                    BandColumn(band, state.minLevelMb, state.maxLevelMb, state.enabled) {
                        controller.onSetBandLevel(band.index, it)
                    }
                }
            }
            if (!state.enabled) {
                Text(
                    "Ligue o equalizador para ajustar as bandas",
                    Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = controller.onReset, enabled = state.enabled, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Resetar")
            }
        }
    }
}

@Composable
private fun BandColumn(band: EqBandUi, minMb: Int, maxMb: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    var local by remember(band.levelMb) { mutableFloatStateOf(band.levelMb.toFloat()) }
    val range = (maxMb - minMb).coerceAtLeast(1)
    val zeroFraction = ((0 - minMb).toFloat() / range).coerceIn(0f, 1f)
    val lineColor = MaterialTheme.colorScheme.outline
    val trackPad = 12.dp // aprox. raio do thumb: o trilho começa/termina após esse recuo
    val freq = formatFreq(band.centerFreqHz)
    Column(
        Modifier.width(64.dp).alpha(if (enabled) 1f else 0.5f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(freq, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.outline, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.height(200.dp).width(48.dp).drawBehind {
                val pad = trackPad.toPx()
                val y = pad + (size.height - 2 * pad) * (1f - zeroFraction) // marca de 0 dB
                drawLine(lineColor, Offset(size.width / 2 - 14.dp.toPx(), y), Offset(size.width / 2 + 14.dp.toPx(), y), 1.5.dp.toPx())
            },
            contentAlignment = Alignment.Center
        ) {
            // Slider horizontal girado -90°: mede com largura/altura trocadas e posiciona após a rotação.
            ResonanceSlider(
                value = local.coerceIn(minMb.toFloat(), maxMb.toFloat()),
                onValueChange = { local = it; onChange(it.toInt()) },
                valueRange = minMb.toFloat()..maxMb.toFloat(),
                enabled = enabled,
                modifier = Modifier
                    .layout { measurable, c ->
                        val p = measurable.measure(
                            Constraints(minWidth = c.minHeight, maxWidth = c.maxHeight, minHeight = c.minWidth, maxHeight = c.maxWidth)
                        )
                        layout(p.height, p.width) { p.place(-(p.width - p.height) / 2, (p.width - p.height) / 2) }
                    }
                    .graphicsLayer { rotationZ = -90f; transformOrigin = TransformOrigin.Center }
                    .semantics { contentDescription = "Banda $freq" }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(formatDb(local.toInt()), style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.secondary, maxLines = 1)
    }
}
