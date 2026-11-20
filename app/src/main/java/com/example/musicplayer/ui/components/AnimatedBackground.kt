package com.example.musicplayer.ui.components

import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.musicplayer.ui.theme.Amber

/**
 * Fundo sutil: dois brilhos radiais (coral/âmbar) que derivam bem devagar (ciclos de 24-32 s).
 * Custo baixo: gradientes desenhados uma vez; a animação só muda translação/alpha na camada gráfica
 * (sem recomposição por frame). A animação para (fundo estático) com a tela fora de primeiro plano
 * ou com a economia de bateria ligada.
 */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var resumed by remember { mutableStateOf(true) }
    var powerSave by remember { mutableStateOf(isPowerSave(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumed = true; powerSave = isPowerSave(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { resumed = false }

    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.5f
    val a = if (dark) 0.14f else 0.10f
    Box(modifier.fillMaxSize().background(cs.background)) {
        if (resumed && !powerSave) {
            val t = rememberInfiniteTransition(label = "bg")
            val p1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse), label = "p1")
            val p2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(32_000, easing = LinearEasing), RepeatMode.Reverse), label = "p2")
            Glow(cs.primary.copy(alpha = a), Offset(0.15f, 0.12f)) { translationX = (p1 - 0.5f) * 160f; translationY = (p1 - 0.5f) * 120f }
            Glow(Amber.copy(alpha = a * 0.7f), Offset(0.9f, 0.85f)) { translationX = (0.5f - p2) * 140f; translationY = (p2 - 0.5f) * 160f }
        } else {
            Glow(cs.primary.copy(alpha = a), Offset(0.15f, 0.12f)) {}
            Glow(Amber.copy(alpha = a * 0.7f), Offset(0.9f, 0.85f)) {}
        }
        content()
    }
}

@Composable
private fun Glow(color: Color, center: Offset, layer: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().graphicsLayer(block = layer).drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(color, Color.Transparent),
                    center = Offset(size.width * center.x, size.height * center.y),
                    radius = size.maxDimension * 0.55f
                )
            )
        }
    )
}

private fun isPowerSave(context: Context): Boolean =
    (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
