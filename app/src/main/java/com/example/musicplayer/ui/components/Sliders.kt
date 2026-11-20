package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.BrandGradient
import com.example.musicplayer.ui.theme.Coral

/** Slider do Stitch: trilho 6dp com preenchimento em gradiente e thumb branco 16dp com borda coral. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonanceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    fill: Brush = BrandGradient
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                Modifier.size(16.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape)
                    .border(2.dp, Coral, CircleShape)
            )
        },
        track = { state ->
            val span = (state.valueRange.endInclusive - state.valueRange.start).coerceAtLeast(1e-6f)
            val frac = ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(fill, CircleShape))
            }
        }
    )
}
