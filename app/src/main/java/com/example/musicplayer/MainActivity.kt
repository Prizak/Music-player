package com.example.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.compose.material3.*
import com.example.musicplayer.data.Track
import com.example.musicplayer.ui.components.AddToPlaylistSheet
import com.example.musicplayer.ui.components.ResIcons
import com.example.musicplayer.ui.components.AnimatedBackground
import com.example.musicplayer.ui.components.FavoritesController
import com.example.musicplayer.ui.components.LocalFavorites
import com.example.musicplayer.ui.screens.PlaylistDetailScreen
import com.example.musicplayer.ui.screens.PlaylistsScreen
import com.example.musicplayer.ui.screens.RankingScreen
import com.example.musicplayer.viewmodel.UiEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.musicplayer.data.prefs.ThemeMode
import com.example.musicplayer.ui.components.resetCoverMisses
import com.example.musicplayer.ui.screens.SettingsScreen
import com.example.musicplayer.ui.screens.EqualizerController
import com.example.musicplayer.ui.components.MiniPlayer
import com.example.musicplayer.ui.screens.AudioPermission
import com.example.musicplayer.ui.screens.MusicLibraryScreen
import com.example.musicplayer.ui.screens.NowPlayingScreen
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.example.musicplayer.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MusicPlayerTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    ResonanceApp(viewModel, mode)
                }
            }
        }
    }
}

@Composable
private fun ResonanceApp(viewModel: MusicViewModel, themeMode: ThemeMode) {
    val library by viewModel.libraryState.collectAsStateWithLifecycle()
    // O playerState muda a cada 500 ms (positionMs). Para não recompor o app inteiro, o estado "estrutural"
    // vem sem a posição (distinctUntilChanged) e a posição é lida só no MiniPlayer (draw) e no SeekBar.
    val playerFlow = viewModel.playerState
    val structuralFlow = remember(playerFlow) { playerFlow.map { it.copy(positionMs = 0L) }.distinctUntilChanged() }
    val player by structuralFlow.collectAsStateWithLifecycle(initialValue = playerFlow.value.copy(positionMs = 0L))
    val positionFlow = remember(playerFlow) { playerFlow.map { it.positionMs }.distinctUntilChanged() }
    val positionState = positionFlow.collectAsStateWithLifecycle(initialValue = playerFlow.value.positionMs)
    val positionProvider = remember(positionState) { { positionState.value } }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    // Checa a permissão atual ao iniciar e reporta ao ViewModel.
    val context = LocalContext.current
    // Reavalia a permissão sempre que voltar ao primeiro plano (inclui o início).
    // Só reporta na 1ª vez ou quando o estado da permissão mudar (evita recarregar a biblioteca a cada resume).
    var permissionChecked by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(context, AudioPermission) == PackageManager.PERMISSION_GRANTED
        if (!permissionChecked || granted != viewModel.libraryState.value.hasPermission) {
            permissionChecked = true
            if (granted) resetCoverMisses() // permite nova tentativa das capas
            viewModel.onPermissionResult(granted)
        }
    }

    // POST_NOTIFICATIONS (API 33+) para a notificação de mídia; pedido uma vez ao iniciar.
    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    // Se a faixa sumir, volta para a biblioteca.
    LaunchedEffect(player.currentTrack == null) {
        if (player.currentTrack == null) showNowPlaying = false
    }

    val playlists by viewModel.playlistsState.collectAsStateWithLifecycle()
    val ranking by viewModel.rankingState.collectAsStateWithLifecycle()

    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val favorites = remember(favoriteIds) { FavoritesController(favoriteIds, viewModel::onToggleFavorite) }
    val totalListenedMs by viewModel.totalListenedMs.collectAsStateWithLifecycle()
    val eqState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val equalizer = remember(eqState) {
        EqualizerController(
            eqState, viewModel::onSetEqualizerEnabled, viewModel::onSelectEqPreset,
            viewModel::onSetEqBandLevel, viewModel::onResetEqualizer
        )
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }            // 0 Biblioteca, 1 Playlists, 2 Ranking, 3 Settings
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) } // sub-destino de Playlists
    var addTarget by remember { mutableStateOf<Track?>(null) }

    // Feedback de validação (uiEvents) -> Snackbar.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { e ->
            if (e is UiEvent.Message) {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(e.text)
            }
        }
    }

    // Altura da pilha inferior (MiniPlayer + barra de navegação, esta já inclui o inset das nav bars).
    var bottomStackPx by remember { mutableIntStateOf(0) }
    val hasMini = player.currentTrack != null
    val bottomPadding = with(LocalDensity.current) { bottomStackPx.toDp() }
    val currentId = player.currentTrack?.id

    // Voltar em Playlists/Ranking leva à Biblioteca (registrado antes das telas: os BackHandlers internos têm prioridade).
    BackHandler(enabled = tab != 0 && !showNowPlaying) { tab = 0; detailId = null }

    CompositionLocalProvider(LocalFavorites provides favorites) {
    AnimatedBackground {
    Box(Modifier.fillMaxSize()) {
        when (tab) {
            0 -> MusicLibraryScreen(
                state = library,
                currentTrackId = currentId,
                onQueryChange = viewModel::onSearchQueryChange,
                onTrackClick = viewModel::playTrack,
                onPermissionResult = viewModel::onPermissionResult,
                onRetry = viewModel::refreshLibrary,
                onAddToPlaylist = { addTarget = it },
                onShuffleAll = viewModel::onShuffleAll,
                onSortOrderChange = viewModel::onSortOrderChange,
                onToggleFavoritesFilter = viewModel::onToggleFavoritesFilter,
                bottomPadding = bottomPadding
            )
            1 -> {
                val detail = detailId?.let { id -> playlists.playlists.firstOrNull { it.id == id } }
                if (detail != null) {
                    PlaylistDetailScreen(
                        playlist = detail,
                        currentTrackId = currentId,
                        onBack = { detailId = null },
                        onPlay = { index, shuffle -> viewModel.onPlayPlaylist(detail.id, index, shuffle) },
                        onRemoveTrack = { viewModel.onRemoveTrackFromPlaylist(detail.id, it) },
                        onAddToPlaylist = { addTarget = it },
                        bottomPadding = bottomPadding
                    )
                } else {
                    // Playlist aberta foi excluída: volta à lista (a checagem só vale após o carregamento).
                    if (detailId != null && !playlists.isLoading) LaunchedEffect(detailId) { detailId = null }
                    PlaylistsScreen(
                        state = playlists,
                        onOpen = { detailId = it },
                        onCreate = viewModel::onCreatePlaylist,
                        onRename = viewModel::onRenamePlaylist,
                        onDelete = viewModel::onDeletePlaylist,
                        bottomPadding = bottomPadding
                    )
                }
            }
            3 -> SettingsScreen(
                themeMode = themeMode,
                onSetThemeMode = viewModel::onSetThemeMode,
                onResetRanking = viewModel::onResetRanking,
                onBack = { tab = 0 },
                totalListenedMs = totalListenedMs,
                equalizer = equalizer,
                bottomPadding = bottomPadding
            )
            else -> RankingScreen(
                state = ranking,
                currentTrackId = currentId,
                onPlay = viewModel::playTrack,
                onAddToPlaylist = { addTarget = it },
                bottomPadding = bottomPadding
            )
        }

        // Pilha inferior: Snackbar, MiniPlayer persistente e barra de navegação.
        Column(Modifier.align(Alignment.BottomCenter).onSizeChanged { bottomStackPx = it.height }) {
            if (hasMini) {
                MiniPlayer(
                    state = player,
                    onClick = { showNowPlaying = true },
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::skipNext,
                    applyNavigationBarInsets = false,
                    positionMs = positionProvider
                )
            }
            ResonanceNavigationBar(selected = tab, onSelect = { if (it == tab && it == 1) detailId = null; tab = it })
        }
        AnimatedVisibility(
            visible = showNowPlaying && hasMini,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Surface(Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                NowPlayingScreen(
                    state = player,
                    onBack = { showNowPlaying = false },
                    onPlayPause = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekTo,
                    onNext = viewModel::skipNext,
                    onPrevious = viewModel::skipPrevious,
                    onToggleShuffle = viewModel::onToggleShuffle,
                    onCycleRepeatMode = viewModel::onCycleRepeatMode,
                    onPlayFromQueue = viewModel::onPlayFromQueue,
                    onRemoveFromQueue = viewModel::onRemoveFromQueue,
                    equalizer = equalizer,
                    positionMs = positionProvider
                )
            }
        }
        // Camada superior: Snackbar visível também sobre a NowPlaying.
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = if (showNowPlaying && hasMini) 0.dp else bottomPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).let { if (showNowPlaying && hasMini) it else WindowInsets(0) })
                .padding(horizontal = 8.dp)
        )
    }
    }
    }

    addTarget?.let { t ->
        AddToPlaylistSheet(
            track = t,
            playlists = playlists.playlists,
            onPick = { viewModel.onAddTrackToPlaylist(it, t.id) },
            onCreateAndAdd = { viewModel.onCreatePlaylistAndAddTrack(it, t.id) },
            onDismiss = { addTarget = null }
        )
    }
}

@Composable
private fun ResonanceNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
    HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
    NavigationBar(containerColor = cs.surfaceContainer, tonalElevation = 0.dp) {
        val items = listOf(
            Triple("Biblioteca", ResIcons.MusicNote, 0),
            Triple("Playlists", ResIcons.Queue, 1),
            Triple("Ranking", ResIcons.Leaderboard, 2),
            Triple("Settings", ResIcons.Settings, 3)
        )
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = cs.primary,
                    selectedTextColor = cs.primary,
                    indicatorColor = cs.primary.copy(alpha = 0.18f),
                    unselectedIconColor = cs.onSurfaceVariant,
                    unselectedTextColor = cs.onSurfaceVariant
                )
            )
        }
    }
}
}
