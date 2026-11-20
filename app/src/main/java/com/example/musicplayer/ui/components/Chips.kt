package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.BrandGradient

/** Pílula de filtro do Stitch: ativa = gradiente coral com texto branco; inativa = container + borda sutil. */
@Composable
fun ResonanceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(BrandGradient)
                else Modifier.background(cs.surfaceContainerLow).border(1.dp, cs.outlineVariant.copy(alpha = 0.4f), CircleShape)
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else cs.onSurfaceVariant, maxLines = 1
        )
    }
}
