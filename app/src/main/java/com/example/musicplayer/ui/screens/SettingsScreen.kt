package com.example.musicplayer.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.prefs.ThemeMode
import com.example.musicplayer.ui.components.*

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    onResetRanking: () -> Unit,
    onBack: () -> Unit,
    totalListenedMs: Long,
    equalizer: EqualizerController,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    BackHandler(onBack = onBack)
    var confirmReset by remember { mutableStateOf(false) }
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val (appName, version) = remember { appInfo(context) }

    Column(modifier.fillMaxSize().topScreenInsets()) {
        ScreenHeader("Configurações", leading = { IconButton(onClick = onBack) { Icon(ResIcons.ArrowBack, "Voltar") } })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("Estatísticas")
            SectionCard {
                Text("Tempo total escutado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatListenTime(totalListenedMs), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Tema")
            val modes = listOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Escuro")
            SectionCard {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onSetThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            activeContentColor = MaterialTheme.colorScheme.primary,
                            activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    ) { Text(label) }
                }
            }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Áudio")
            SectionCard {
                Text("Ajuste as bandas de frequência e escolha presets.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = { showEqualizer = true }) {
                    Icon(ResIcons.Tune, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Equalizador")
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Dados")
            SectionCard {
                Text("Apaga a contagem de reproduções do ranking.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    onClick = { confirmReset = true },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) { Text("Zerar ranking", color = MaterialTheme.colorScheme.error) }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Sobre")
            SectionCard {
                Text(appName, style = MaterialTheme.typography.titleMedium)
                Text("Versão $version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showEqualizer) EqualizerSheet(equalizer, onDismiss = { showEqualizer = false })
    if (confirmReset) {
        ConfirmDialog(
            "Zerar ranking", "Todas as contagens de reprodução serão apagadas. Esta ação não pode ser desfeita.", "Zerar",
            onConfirm = onResetRanking, onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 0.dp)
    )

/** Cartão de seção do Stitch: rounded-2xl, surface-container, borda coral sutil. */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(if (cs.background.luminance() < 0.5f) cs.surfaceContainer else cs.surfaceContainerLow)
            .border(1.dp, cs.primary.copy(alpha = 0.2f), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content
    )
}

@Suppress("DEPRECATION")
private fun appInfo(context: Context): Pair<String, String> {
    val name = context.applicationInfo.loadLabel(context.packageManager).toString()
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (_: PackageManager.NameNotFoundException) { "?" }
    return name to version
}
