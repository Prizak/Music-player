package com.example.musicplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Escala sutil ao pressionar (lida só na camada gráfica: sem recomposição por frame). */
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.92f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (isPressed) pressed else 1f, spring(stiffness = 600f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** IconButton com escala ao pressionar e vibração leve ao clicar. */
@Composable
fun HapticIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    IconButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        modifier = modifier.pressScale(source),
        enabled = enabled,
        interactionSource = source,
        content = content
    )
}

/** Favoritos expostos à UI sem passar parâmetros por todas as telas (null = recurso indisponível). */
class FavoritesController(val ids: Set<Long>, val onToggle: (Long) -> Unit)

val LocalFavorites = staticCompositionLocalOf<FavoritesController?> { null }

@Composable
fun FavoriteButton(trackId: Long, modifier: Modifier = Modifier) {
    val fav = LocalFavorites.current ?: return
    val on = trackId in fav.ids
    val scale by animateFloatAsState(if (on) 1.1f else 1f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "fav")
    HapticIconButton(onClick = { fav.onToggle(trackId) }, modifier = modifier) {
        Icon(
            if (on) ResIcons.Favorite else ResIcons.FavoriteBorder,
            if (on) "Remover dos favoritos" else "Favoritar",
            Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
