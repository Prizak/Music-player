package com.example.musicplayer.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.MediaStoreTrackRepository
import com.example.musicplayer.data.PlayCountRepository
import com.example.musicplayer.data.PlaylistRepository
import com.example.musicplayer.data.db.ResonanceDatabase
import com.example.musicplayer.data.prefs.SettingsRepository
import com.example.musicplayer.data.prefs.SortOrder
import com.example.musicplayer.data.prefs.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.example.musicplayer.data.Track
import com.example.musicplayer.service.EqualizerManager
import com.example.musicplayer.service.PlaybackService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private const val MAX_PLAYLIST_NAME = 50

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreTrackRepository(application)

    private val settingsRepo = SettingsRepository(application)

    // Leitura inicial única (rápida, arquivo pequeno) para o tema/ordenação corretos já no primeiro
    // frame, evitando piscar o tema errado. Com timeout e fallback para os defaults (DARK/TITLE).
    private val initialSettings: Pair<ThemeMode, SortOrder> = try {
        runBlocking(Dispatchers.IO) { withTimeoutOrNull(300L) { settingsRepo.readInitial() } }
    } catch (e: Exception) {
        null
    } ?: (ThemeMode.DARK to SortOrder.TITLE)

    private val _libraryState = MutableStateFlow(LibraryUiState(sortOrder = initialSettings.second))
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var loadJob: Job? = null

    /** Faixas da fila atual por id (independe da biblioteca, evita descartar itens). */
    private val queueTrackById = HashMap<Long, Track>()

    /** Ação aguardando a conexão do MediaController (último pedido vence). */
    private var pendingAction: ((MediaController) -> Unit)? = null

    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialSettings.first)

    /** Tempo total ouvido (ms), acumulado pelo PlaybackService e persistido no DataStore. */
    val totalListenedMs: StateFlow<Long> = settingsRepo.totalListenedMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Faixas favoritas (ids), persistidas no DataStore; a UI atualiza na hora via Flow. */
    val favoriteIds: StateFlow<Set<Long>> = settingsRepo.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var favIds: Set<Long> = emptySet()
    init {
        viewModelScope.launch {
            settingsRepo.favoriteIds.collect { ids ->
                favIds = ids
                if (_libraryState.value.favoritesOnly) _libraryState.update { recompute(it) }
            }
        }
    }

    fun onToggleFavoritesFilter() {
        _libraryState.update { recompute(it.copy(favoritesOnly = !it.favoritesOnly)) }
    }

    fun onToggleFavorite(trackId: Long) {
        launchCatching("Não foi possível salvar o favorito") { settingsRepo.toggleFavorite(trackId) }
    }

    fun onSetThemeMode(mode: ThemeMode) {
        launchCatching("Não foi possível salvar o tema") { settingsRepo.setThemeMode(mode) }
    }

    // ---------------- Equalizador ----------------

    val equalizerState: StateFlow<EqualizerUiState> = EqualizerManager.uiState

    fun onSetEqualizerEnabled(enabled: Boolean) = EqualizerManager.setEnabled(enabled)
    fun onSelectEqPreset(index: Int) = EqualizerManager.selectPreset(index)
    fun onSetEqBandLevel(band: Int, levelMb: Int) = EqualizerManager.setBandLevel(band, levelMb)
    fun onResetEqualizer() = EqualizerManager.reset()

    @OptIn(FlowPreview::class)
    private fun startEqualizerPersistence() {
        viewModelScope.launch {
            try {
                EqualizerManager.restore(settingsRepo.eqSettings.first())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Mantém defaults.
            }
            // Debounce: arrastar o slider não grava a cada ms.
            EqualizerManager.settings.debounce(300L).collect { s ->
                try {
                    settingsRepo.setEqSettings(s)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emitMessage("Não foi possível salvar o equalizador")
                }
            }
        }
    }

    // ---------------- Room: playlists e ranking ----------------

    private val db = ResonanceDatabase.get(application)
    private val playlistRepo = PlaylistRepository(db.playlistDao())
    private val playCountRepo = PlayCountRepository(db.playCountDao())

    // Channel bufferizado: eventos emitidos sem coletor não se perdem.
    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<UiEvent> = _uiEvents.receiveAsFlow()

    /** Contagens por faixa, para a ordenação MOST_PLAYED (atualizado reativamente do Room). */
    @Volatile private var playCounts: Map<Long, Int> = emptyMap()

    // distinctUntilChanged: digitar na busca muda libraryState mas não tracks; evita recombinar playlists/ranking.
    private val libraryTracks = _libraryState.map { it.tracks }.distinctUntilChanged()

    val playlistsState: StateFlow<PlaylistsUiState> = combine(
        playlistRepo.observePlaylists(),
        playlistRepo.observeAllPlaylistTracks(),
        libraryTracks
    ) { playlists, links, tracks ->
        val byId = tracks.associateBy { it.id }
        val linksByPlaylist = links.groupBy { it.playlistId }
        PlaylistsUiState(
            playlists = playlists.map { p ->
                val present = linksByPlaylist[p.id].orEmpty()
                    .sortedBy { it.position }
                    .mapNotNull { byId[it.trackId] }
                PlaylistUi(p.id, p.name, present.size, present)
            },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistsUiState())

    val rankingState: StateFlow<RankingUiState> = combine(
        playCountRepo.observeAll(),
        libraryTracks
    ) { counts, tracks ->
        val byId = tracks.associateBy { it.id }
        // Órfãs são ignoradas ANTES do corte do Top 10. Já vem ordenado pelo DAO.
        val top = counts.mapNotNull { c -> byId[c.trackId]?.let { it to c.playCount } }.take(10)
        RankingUiState(top.mapIndexed { i, (t, n) -> RankingEntry(t, n, i + 1) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingUiState())

    private fun emitMessage(text: String) {
        _uiEvents.trySend(UiEvent.Message(text))
    }

    private fun launchCatching(errorMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitMessage(errorMessage)
            }
        }
    }

    /** Valida e normaliza o nome; emite mensagem e retorna null se inválido. */
    private suspend fun validateName(raw: String, ignoreId: Long?): String? {
        val name = raw.trim()
        if (name.isEmpty()) {
            emitMessage("O nome da playlist não pode ser vazio")
            return null
        }
        if (name.length > MAX_PLAYLIST_NAME) {
            emitMessage("O nome da playlist pode ter no máximo $MAX_PLAYLIST_NAME caracteres")
            return null
        }
        // Comparação em Kotlin (Unicode), não COLLATE NOCASE do SQLite (só ASCII).
        val duplicate = playlistRepo.getAll().any { it.id != ignoreId && it.name.equals(name, ignoreCase = true) }
        if (duplicate) {
            emitMessage("Já existe uma playlist com esse nome")
            return null
        }
        return name
    }

    fun onCreatePlaylist(name: String) {
        launchCatching("Não foi possível criar a playlist") {
            val valid = validateName(name, null) ?: return@launchCatching
            playlistRepo.create(valid)
        }
    }

    fun onCreatePlaylistAndAddTrack(name: String, trackId: Long) {
        launchCatching("Não foi possível criar a playlist") {
            val valid = validateName(name, null) ?: return@launchCatching
            playlistRepo.createWithTrack(valid, trackId)
            emitMessage("Playlist criada e faixa adicionada")
        }
    }

    fun onRenamePlaylist(id: Long, name: String) {
        launchCatching("Não foi possível renomear a playlist") {
            val valid = validateName(name, id) ?: return@launchCatching
            playlistRepo.rename(id, valid)
        }
    }

    fun onDeletePlaylist(id: Long) {
        launchCatching("Não foi possível excluir a playlist") { playlistRepo.delete(id) }
    }

    fun onAddTrackToPlaylist(playlistId: Long, trackId: Long) {
        launchCatching("Não foi possível adicionar a faixa") {
            if (playlistRepo.addTrack(playlistId, trackId)) {
                emitMessage("Faixa adicionada à playlist")
            } else {
                emitMessage("A faixa já está na playlist")
            }
        }
    }

    fun onRemoveTrackFromPlaylist(playlistId: Long, trackId: Long) {
        launchCatching("Não foi possível remover a faixa") {
            playlistRepo.removeTrack(playlistId, trackId)
        }
    }

    fun onPlayPlaylist(playlistId: Long, startIndex: Int = 0, shuffle: Boolean = false) {
        launchCatching("Não foi possível tocar a playlist") {
            // Lê direto do banco (não depende de haver coletor em playlistsState).
            val byId = _libraryState.value.tracks.associateBy { it.id }
            val tracks = playlistRepo.getTrackLinks(playlistId)
                .sortedBy { it.position }
                .mapNotNull { byId[it.trackId] }
            if (tracks.isEmpty()) {
                emitMessage("Nenhuma faixa disponível nesta playlist")
                return@launchCatching
            }
            if (startIndex !in tracks.indices) return@launchCatching
            playQueue(tracks, startIndex, shuffle)
        }
    }

    fun onShuffleAll() {
        val tracks = _libraryState.value.filteredTracks
        if (tracks.isEmpty()) {
            emitMessage("Nenhuma faixa para tocar")
            return
        }
        playQueue(tracks, 0, shuffle = true)
    }

    fun onResetRanking() {
        launchCatching("Não foi possível zerar o ranking") {
            playCountRepo.resetAll()
            emitMessage("Ranking zerado")
        }
    }

    /**
     * Enfileira [queue] no player. Com shuffle e startIndex == 0 usa resetPosition = true: o ExoPlayer
     * começa em timeline.getFirstWindowIndex(shuffle) (ExoPlayerImpl 1.8.0), ou seja, no INÍCIO da
     * ordem embaralhada, e toda a fila é tocada. Um índice inicial aleatório explícito caía no meio
     * da ordem embaralhada (DefaultShuffleOrder independente do índice) e, com repeat OFF, as faixas
     * anteriores a ele na ordem nunca tocavam. O índice 0 que o controller pode mostrar por um
     * instante é só visual e se corrige na resposta do serviço.
     */
    private fun playQueue(queue: List<Track>, startIndex: Int, shuffle: Boolean) {
        withController { c ->
            queueTrackById.clear()
            queue.forEach { queueTrackById[it.id] = it }
            c.shuffleModeEnabled = shuffle
            val items = queue.map { it.toMediaItem() }
            if (shuffle && startIndex == 0) c.setMediaItems(items, /* resetPosition = */ true)
            else c.setMediaItems(items, startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    fun onSortOrderChange(order: SortOrder) {
        _libraryState.update { recompute(it.copy(sortOrder = order)) }
        launchCatching("Não foi possível salvar a ordenação") { settingsRepo.setSortOrder(order) }
    }

    // A contagem de reproduções vive no PlaybackService (service/PlayCounter).

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPlayerState()
            updatePositionLoop()
        }
    }

    init {
        connectController()
        startEqualizerPersistence()
        viewModelScope.launch {
            playCountRepo.observeAll().collect { list ->
                playCounts = list.associate { it.trackId to it.playCount }
                if (_libraryState.value.sortOrder == SortOrder.MOST_PLAYED) {
                    _libraryState.update { recompute(it) }
                }
            }
        }
    }

    // ---------------- Permissão / biblioteca ----------------

    fun onPermissionResult(granted: Boolean) {
        _libraryState.update { recompute(it.copy(hasPermission = granted, isLoading = false)) }
        if (granted) refreshLibrary()
    }

    fun refreshLibrary() {
        if (!_libraryState.value.hasPermission) return
        loadJob?.cancel()
        _libraryState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            try {
                val tracks = repository.loadTracks()
                _libraryState.update {
                    recompute(it.copy(tracks = tracks, isLoading = false, errorMessage = null))
                }
                // Reconstrói playerState com o novo mapa (ex.: serviço já tocando ao recriar o ViewModel).
                syncPlayerState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _libraryState.update {
                    recompute(
                        it.copy(
                            tracks = emptyList(),
                            isLoading = false,
                            errorMessage = e.message ?: "Falha ao ler a biblioteca"
                        )
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _libraryState.update { recompute(it.copy(query = query)) }
    }

    private fun recompute(s: LibraryUiState): LibraryUiState {
        val q = s.query.trim()
        val base = if (s.favoritesOnly) s.tracks.filter { it.id in favIds } else s.tracks
        val matching = if (q.isEmpty()) base else base.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
        val filtered = sortTracks(matching, s.sortOrder)
        val reason = when {
            !s.hasPermission -> LibraryEmptyReason.NO_PERMISSION
            s.errorMessage != null -> LibraryEmptyReason.ERROR
            s.tracks.isEmpty() -> LibraryEmptyReason.NO_TRACKS
            (q.isNotEmpty() || s.favoritesOnly) && filtered.isEmpty() -> LibraryEmptyReason.NO_SEARCH_RESULTS
            else -> LibraryEmptyReason.NONE
        }
        return s.copy(filteredTracks = filtered, emptyReason = reason)
    }

    private fun sortTracks(list: List<Track>, order: SortOrder): List<Track> {
        val ci = String.CASE_INSENSITIVE_ORDER
        val byTitle = compareBy<Track, String>(ci) { it.title }
        return when (order) {
            SortOrder.TITLE -> list.sortedWith(byTitle)
            SortOrder.ARTIST -> list.sortedWith(compareBy<Track, String>(ci) { it.artist }.then(byTitle))
            SortOrder.ALBUM -> list.sortedWith(compareBy<Track, String>(ci) { it.album }.then(byTitle))
            SortOrder.RECENT -> list.sortedWith(compareByDescending<Track> { it.dateAddedSec }.then(byTitle))
            SortOrder.MOST_PLAYED -> {
                val counts = playCounts
                list.sortedWith(compareByDescending<Track> { counts[it.id] ?: 0 }.then(byTitle))
            }
        }
    }

    // ---------------- Reprodução ----------------

    fun playTrack(track: Track) {
        val lib = _libraryState.value
        // Fila = lista filtrada; se a faixa não está nela, cai para a biblioteca completa;
        // se também não está, toca só a faixa.
        val queue = listOf(lib.filteredTracks, lib.tracks)
            .firstOrNull { list -> list.any { it.id == track.id } } ?: listOf(track)
        val index = queue.indexOfFirst { it.id == track.id }
        withController { c ->
            queueTrackById.clear()
            queue.forEach { queueTrackById[it.id] = it }
            c.setMediaItems(queue.map { it.toMediaItem() }, index, 0L)
            c.prepare()
            c.play()
        }
    }

    /** Executa [action] agora, ou assim que o MediaController conectar. */
    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else pendingAction = action
    }

    fun onToggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun onCycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun onPlayFromQueue(index: Int) {
        val c = controller ?: return
        val real = displayOrder.getOrNull(index) ?: return
        if (real !in 0 until c.mediaItemCount) return
        c.seekToDefaultPosition(real)
        c.prepare()
        c.play()
    }

    fun onRemoveFromQueue(index: Int) {
        val c = controller ?: return
        val real = displayOrder.getOrNull(index) ?: return
        if (real !in 0 until c.mediaItemCount) return
        c.removeMediaItem(real)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) {
        val c = controller ?: return
        val dur = c.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        c.seekTo(positionMs.coerceIn(0L, dur))
        syncPlayerState()
    }

    fun skipNext() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) c.seekToNextMediaItem()
    }

    fun skipPrevious() {
        val c = controller ?: return
        if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem()
    }

    // ---------------- MediaController ----------------

    private fun connectController() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                syncPlayerState()
                updatePositionLoop()
                pendingAction?.let { a ->
                    pendingAction = null
                    a(c)
                }
            } catch (e: Exception) {
                // Conexão falhou/cancelada; playerState permanece no valor inicial.
            }
        }, ContextCompat.getMainExecutor(app))
    }

    /** Mapeia índice de EXIBIÇÃO (queue da UI) -> índice real do item no player. */
    private var displayOrder: List<Int> = emptyList()

    /** Ordem efetiva de reprodução (embaralhada se shuffle), repeat OFF, como índices reais. */
    private fun computeDisplayOrder(c: MediaController): List<Int> {
        val count = c.mediaItemCount
        if (count == 0) return emptyList()
        val shuffle = c.shuffleModeEnabled
        if (!shuffle) return (0 until count).toList()
        val timeline = c.currentTimeline
        if (timeline.isEmpty || timeline.windowCount != count) return (0 until count).toList()
        val order = ArrayList<Int>(count)
        var i = timeline.getFirstWindowIndex(true)
        while (i != C.INDEX_UNSET && order.size < count) {
            order += i
            i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, true)
        }
        // Salvaguarda: nunca descarta itens se o timeline vier incompleto.
        if (order.size != count) return (0 until count).toList()
        return order
    }

    private fun syncPlayerState() {
        val c = controller ?: return
        // Fila e índice vêm do mesmo timeline; itens sem Track conhecido são reconstruídos
        // a partir do MediaItem, para queue[currentIndex] == currentTrack sempre.
        val items = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
        val order = computeDisplayOrder(c)
        displayOrder = order
        val queue = order.map { items[it].toTrack() }
        val real = c.currentMediaItemIndex
        val idx = if (real in items.indices) order.indexOf(real) else -1
        // Poda faixas que saíram da fila.
        val ids = items.mapNotNull { it.mediaId.toLongOrNull() }.toSet()
        queueTrackById.keys.retainAll(ids)
        val current = if (idx >= 0) queue[idx] else null
        val dur = if (c.duration > 0) c.duration else (current?.durationMs ?: 0L)
        _playerState.value = PlayerUiState(
            currentTrack = current,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = dur,
            queue = queue,
            currentIndex = idx,
            hasNext = current != null && c.hasNextMediaItem(),
            hasPrevious = current != null && c.hasPreviousMediaItem(),
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            }
        )
    }

    private fun updatePositionLoop() {
        val c = controller
        if (c == null || !c.isPlaying) {
            positionJob?.cancel()
            positionJob = null
            return
        }
        if (positionJob?.isActive == true) return
        positionJob = viewModelScope.launch {
            while (isActive) {
                val cc = controller ?: break
                // App em segundo plano (sem coletores): não gasta CPU atualizando a posição.
                if (_playerState.subscriptionCount.value > 0) {
                    val pos = cc.currentPosition.coerceAtLeast(0L)
                    _playerState.update { it.copy(positionMs = pos) }
                }
                delay(500)
            }
        }
    }

    private fun MediaItem.toTrack(): Track {
        val id = mediaId.toLongOrNull()
        id?.let { queueTrackById[it] }?.let { return it }
        val m = mediaMetadata
        return Track(
            id = id ?: -1L,
            title = m.title?.toString() ?: "Faixa desconhecida",
            artist = m.artist?.toString() ?: "Artista desconhecido",
            album = m.albumTitle?.toString() ?: "Álbum desconhecido",
            durationMs = m.durationMs ?: 0L,
            contentUri = localConfiguration?.uri?.toString() ?: "",
            albumArtUri = m.artworkUri?.toString()
        )
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setDurationMs(durationMs)
                    // API 29+: thumbnail da própria faixa (carregado pelo ArtworkBitmapLoader do serviço);
                    // antes: URI legada de álbum.
                    .setArtworkUri((if (albumArtUri != null && android.os.Build.VERSION.SDK_INT >= 29) contentUri else albumArtUri)?.toUri())
                    .build()
            )
            .build()

    override fun onCleared() {
        positionJob?.cancel()
        pendingAction = null
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        super.onCleared()
    }
}
