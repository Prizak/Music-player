# CONTRACTS.md - Resonance (Fases 1, 2A, 2B, 3 e 4)

Contratos compartilhados entre `programador` (backend/áudio) e `frontend` (UI), para trabalho em paralelo.
Package base: `com.example.musicplayer`. Mantido pela sessão `documentador`.
Qualquer mudança de contrato deve ser pedida ao `documentador` e comunicada às duas sessões antes de ser implementada.

---

## 1. Regras de propriedade e fronteira

| Área | Dono | Observações |
|---|---|---|
| `app/build.gradle.kts`, `AndroidManifest.xml` | programador | Dependências Media3, permissões, serviço |
| `data/` (`Track`, `MediaStoreTrackRepository`) | programador | `Track` é definido aqui e consumido pela UI |
| `service/` (`PlaybackService`) | programador | ExoPlayer + `MediaSessionService` |
| `viewmodel/` (`MusicViewModel`, `LibraryUiState`, `PlayerUiState`) | programador | UI só LÊ os estados e CHAMA as funções |
| `ui/theme/`, `ui/components/`, `ui/screens/`, `MainActivity.kt` | frontend | Não acessar `data/` nem `service/` diretamente |
| `CONTRACTS.md`, `TASKS.md` | documentador | Fonte da verdade dos contratos |

Regras:
1. A UI NÃO acessa `MediaStore`, `ContentResolver`, `ExoPlayer`, `MediaController` ou `PlaybackService`. Tudo passa pelo `MusicViewModel`.
2. O backend NÃO importa nada de `ui/`.
3. A UI NÃO tem slider de volume (o volume é controlado pelo sistema/hardware).
4. A UI consome os estados com `collectAsStateWithLifecycle()` e os trata como imutáveis.
5. A solicitação da permissão em runtime é feita pela UI (`rememberLauncherForActivityResult`), que reporta o resultado via `onPermissionResult`.
6. Permissão a solicitar: `READ_MEDIA_AUDIO` (API 33+) ou `READ_EXTERNAL_STORAGE` (API 24-32).
7. Fase 1/2A: sem Room, rankings ou playlists persistentes (liberados na Fase 2B, seção 8). Equalizador DSP foi liberado na Fase 4 (seção 10). A fila de reprodução é apenas em memória.
8. O `MusicViewModel` é obtido na `MainActivity` (`viewModel()`/`AndroidViewModel`) e repassado às telas por parâmetro (as telas não criam o ViewModel).

---

## 2. Modelo de dados

Arquivo: `app/src/main/java/com/example/musicplayer/data/Track.kt`

```kotlin
package com.example.musicplayer.data

data class Track(
    val id: Long,              // MediaStore.Audio.Media._ID
    val title: String,         // nunca vazio; fallback "Faixa desconhecida"
    val artist: String,        // fallback "Artista desconhecido"
    val album: String,         // fallback "Álbum desconhecido"
    val durationMs: Long,
    val contentUri: String,    // content://media/external/audio/media/{id}
    val albumArtUri: String?   // null quando não houver capa
)
```

---

## 3. Estados de UI

Arquivo: `app/src/main/java/com/example/musicplayer/viewmodel/LibraryUiState.kt`

```kotlin
package com.example.musicplayer.viewmodel

import com.example.musicplayer.data.Track

/** Motivo pelo qual a biblioteca não tem faixas para mostrar. */
enum class LibraryEmptyReason {
    NONE,            // há faixas para exibir
    NO_PERMISSION,   // permissão negada -> UI mostra estado de permissão
    NO_TRACKS,       // permissão ok, nenhuma música no dispositivo
    NO_SEARCH_RESULTS, // há faixas, mas a busca não retornou nada
    ERROR            // falha ao ler o MediaStore (ver errorMessage)
}

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),          // todas as faixas
    val filteredTracks: List<Track> = emptyList(),  // o que a UI lista (já filtrado por query)
    val query: String = "",
    val isLoading: Boolean = false,                 // true -> UI mostra skeleton
    val hasPermission: Boolean = false,
    val emptyReason: LibraryEmptyReason = LibraryEmptyReason.NONE,
    val errorMessage: String? = null                // preenchido quando emptyReason == ERROR
)
```

Regras de derivação (responsabilidade do `MusicViewModel`):
- `filteredTracks` = `tracks` filtradas por `query` (case-insensitive, em title/artist/album); `query` vazia -> `filteredTracks == tracks`.
- `emptyReason`: `NO_PERMISSION` se `!hasPermission`; `ERROR` se houve falha; `NO_TRACKS` se `tracks` vazia; `NO_SEARCH_RESULTS` se `query` não vazia e `filteredTracks` vazia; senão `NONE`.
- Enquanto `isLoading == true`, a UI exibe skeleton independentemente de `emptyReason`.

Arquivo: `app/src/main/java/com/example/musicplayer/viewmodel/PlayerUiState.kt`

```kotlin
package com.example.musicplayer.viewmodel

import com.example.musicplayer.data.Track

enum class RepeatMode { OFF, ALL, ONE }

data class PlayerUiState(
    val currentTrack: Track? = null,       // null -> MiniPlayer oculto
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,             // atualizado ~a cada 500 ms enquanto toca
    val durationMs: Long = 0L,             // 0 se desconhecida
    val queue: List<Track> = emptyList(),  // fila atual em memória
    val currentIndex: Int = -1,            // índice de currentTrack em queue; -1 se nenhum
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)
```

Nota: na Fase 1, `shuffleEnabled`/`repeatMode` existiam no estado sem ações associadas. A partir da Fase 2A eles são OBRIGATÓRIOS e funcionais (ver seção 7). Ordem do `RepeatMode` na 2A: OFF, ONE, ALL (ciclo da ação: OFF -> ALL -> ONE -> OFF).

---

## 4. API pública do `MusicViewModel`

Arquivo: `app/src/main/java/com/example/musicplayer/viewmodel/MusicViewModel.kt`

```kotlin
package com.example.musicplayer.viewmodel

import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import com.example.musicplayer.data.Track

class MusicViewModel(application: android.app.Application) : AndroidViewModel(application) {

    // ---- Estados (somente leitura para a UI) ----
    val libraryState: StateFlow<LibraryUiState>
    val playerState: StateFlow<PlayerUiState>

    // ---- Permissão / biblioteca ----
    /** Chamado pela UI após checar/solicitar a permissão. Se granted == true, dispara refreshLibrary(). */
    fun onPermissionResult(granted: Boolean)

    /** Recarrega as faixas do MediaStore (coroutine em Dispatchers.IO). Ignorado se !hasPermission. */
    fun refreshLibrary()

    /** Atualiza query e recalcula filteredTracks. */
    fun onSearchQueryChange(query: String)

    // ---- Reprodução ----
    /**
     * Toca [track]. A fila passa a ser a lista filtrada atual (libraryState.filteredTracks)
     * e o índice inicial é o de [track] nela.
     */
    fun playTrack(track: Track)

    fun togglePlayPause()

    /** Posição em milissegundos, dentro de [0, durationMs]. */
    fun seekTo(positionMs: Long)

    fun skipNext()
    fun skipPrevious()

    // ---- Fase 1: shuffle/repeat não implementados. A partir da Fase 2A, ver seção 7. ----
    // (substituídos na 2A por onToggleShuffle())
    // (substituído na 2A por onCycleRepeatMode())
}
```

Comportamento garantido:
- `libraryState` e `playerState` nunca são `null` e sempre têm valor inicial (os `data class` acima com valores default).
- Ao iniciar, o ViewModel conecta ao `PlaybackService` via `MediaController`; `playerState` reflete o `Player` real.
- `skipNext`/`skipPrevious` não fazem nada se `hasNext`/`hasPrevious` forem `false`.
- Não existe função de volume.

---

## 5. Uso esperado pela UI (resumo)

| Elemento de UI | Lê | Chama |
|---|---|---|
| `MusicLibraryScreen` (lista, busca, skeleton, vazio/permissão) | `libraryState` | `onPermissionResult`, `refreshLibrary`, `onSearchQueryChange`, `playTrack` |
| `MiniPlayer` (persistente, visível se `currentTrack != null`) | `playerState` | `togglePlayPause`, `skipNext` (opcional); toque abre `NowPlayingScreen` |
| `NowPlayingScreen` (sem slider de volume) | `playerState` | `togglePlayPause`, `seekTo`, `skipNext`, `skipPrevious` |

Navegação entre `MusicLibraryScreen` e `NowPlayingScreen` é responsabilidade exclusiva do `frontend`.

---

## 7. Fase 2A - Polimento Visual, Capas Reais, Fila e Controles de Transporte

Nesta fase eram PROIBIDOS: Room, Playlists, Rankings, Equalizador (Room/Playlists/Rankings liberados na 2B, seção 8). Fila e tema ficam em memória, sem persistência.
Esta seção SUBSTITUI a decisão anterior de deixar `toggleShuffle`/`cycleRepeatMode` fora de escopo: eles passam a existir com os nomes abaixo.

### 7.1 `PlayerUiState` (sem mudança de campos)
- `shuffleEnabled: Boolean` e `repeatMode: RepeatMode` (`OFF`, `ONE`, `ALL`) são OBRIGATÓRIOS e FUNCIONAIS: devem refletir `ExoPlayer.shuffleModeEnabled` e `ExoPlayer.repeatMode` reais.
- `queue` e `currentIndex` estão sempre alinhados: `queue[currentIndex] == currentTrack`; `currentIndex == -1` se não há faixa.
- Com shuffle LIGADO, `queue` está na ordem EFETIVA de reprodução (timeline embaralhada do player) e `currentIndex` é a posição de exibição nela. Sem shuffle, `queue` está na ordem natural.
- `onPlayFromQueue(index)`/`onRemoveFromQueue(index)` recebem o índice de EXIBIÇÃO (posição em `queue`); o ViewModel o converte para o índice real do player.

### 7.2 Novas ações do `MusicViewModel` (nomes EXATOS)

```kotlin
// com.example.musicplayer.viewmodel.MusicViewModel

/** Tema escolhido: decisão de contrato = StateFlow separado (não faz parte de PlayerUiState/LibraryUiState). */
val isDarkTheme: StateFlow<Boolean>      // default: true (Dark OLED)

fun onToggleShuffle()                    // alterna ExoPlayer.shuffleModeEnabled
fun onCycleRepeatMode()                  // OFF -> ALL -> ONE -> OFF
fun onPlayFromQueue(index: Int)          // toca o item `index` da fila atual; índice inválido é ignorado
fun onRemoveFromQueue(index: Int)        // remove o item da fila em memória; ajusta currentIndex/hasNext/hasPrevious
fun onToggleTheme()                      // alterna isDarkTheme (em memória)
```

Comportamento:
- `onRemoveFromQueue` do item atual: segue para a próxima faixa (ou para a reprodução se a fila esvaziar, zerando `currentTrack`).
- `playTrack` chamado antes de o `MediaController` conectar NÃO pode descartar o toque: deve enfileirar a solicitação e executá-la ao conectar.
- A UI lê `isDarkTheme` via `collectAsStateWithLifecycle()` e aplica no `ResonanceTheme`.

### 7.3 Regra de fronteira: capas
- `Track.albumArtUri` continua `String?` (sem mudança em `data/`).
- O carregamento de capas fica na camada `ui/components` (via `LocalContext`), FORA da Main Thread, com cache leve em memória (ex.: `LruCache`).
- API 29+: `ContentResolver.loadThumbnail` recebendo `Track.contentUri` (não a URI legada `albumart`); API 24-28: fallback via MediaStore.
- Alvos de decodificação: 128px (lista/MiniPlayer) e 512px (NowPlaying). Cache em memória com limite `maxMemory/8`; decodificação compartilhada por chave (sem decodificar a mesma capa em paralelo).
- Placeholder em gradiente SOMENTE quando não houver capa (ou falha ao carregar).
- A UI continua sem acessar `MediaStore` para listar faixas; o acesso é restrito ao carregamento de thumbnails.

---

## 8. Fase 2B - Playlists persistentes e Ranking Top 10 (Room)

Autorização do usuário: Room, Playlists e Rankings estão liberados. O Equalizador DSP foi tratado depois, na Fase 4 (seção 10).
Fora de escopo: chips de filtro, FAB Shuffle All, Settings, Equalizador.

### 8.1 Persistência (dono: programador, pacote `com.example.musicplayer.data.db`)
Banco `ResonanceDatabase` com `version = 1`, SEM migração nesta fase. `exportSchema = true` (com `room.schemaLocation` via KSP se necessário).

```kotlin
package com.example.musicplayer.data.db

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long                 // epoch ms
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],   // a mesma faixa não repete na mesma playlist
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"], childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("playlistId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,                  // Track.id (MediaStore); sem FK, pode ficar órfão
    val position: Int,                  // 0-based, contíguo dentro da playlist
    val addedAt: Long
)

@Entity(tableName = "play_counts")
data class PlayCountEntity(
    @PrimaryKey val trackId: Long,
    val playCount: Int,
    val lastPlayedAt: Long
)
```

DAOs (interfaces, métodos de leitura reativos retornam `Flow`, escritas são `suspend`; Room executa fora da Main Thread):
- `PlaylistDao`: `observePlaylists(): Flow<List<PlaylistEntity>>`, `observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>` (ordenado por playlistId, position), `insert/rename/delete`, `insertTrack`, `deleteTrack` + reindexação de `position`, `findByName` (comparação case-insensitive).
- `PlayCountDao`: `observeAll(): Flow<List<PlayCountEntity>>` (ordenado por playCount DESC, lastPlayedAt DESC) e `increment(trackId, now)` atômico (upsert + incremento em uma transação).
- Repositórios em `data/` (ex.: `PlaylistRepository`, `PlayCountRepository`) isolam os DAOs; o ViewModel só fala com os repositórios.

Faixas órfãs (sumiram do MediaStore): as linhas permanecem no banco (sem limpeza na 2B), mas a UI só exibe faixas presentes em `libraryState.tracks`. `trackCount` conta apenas faixas presentes. No ranking, entradas de faixas ausentes são ignoradas ANTES de aplicar o limite de 10 (o Top 10 é preenchido só com faixas presentes).

### 8.2 Modelos de UI (dono: programador, pacote `com.example.musicplayer.viewmodel`)

```kotlin
package com.example.musicplayer.viewmodel

import com.example.musicplayer.data.Track

data class PlaylistUi(
    val id: Long,
    val name: String,
    val trackCount: Int,            // = tracks.size (somente faixas presentes na biblioteca)
    val tracks: List<Track>         // em ordem de position, somente faixas presentes
)

data class PlaylistsUiState(
    val playlists: List<PlaylistUi> = emptyList(),   // ordenadas por createdAt (mais antigas primeiro)
    val isLoading: Boolean = true
)

data class RankingEntry(
    val track: Track,
    val playCount: Int,
    val rank: Int                   // 1-based; desempate por lastPlayedAt (mais recente primeiro)
)

data class RankingUiState(
    val top10: List<RankingEntry> = emptyList()      // no máximo 10; vazio = sem histórico ainda
)

/** Canal simples de feedback (erros de validação e confirmações). */
sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
}
```

### 8.3 Novas APIs do `MusicViewModel` (nomes EXATOS)

```kotlin
val playlistsState: StateFlow<PlaylistsUiState>
val rankingState: StateFlow<RankingUiState>        // Top 10 reativo (Flow do Room combinado com libraryState)
val uiEvents: SharedFlow<UiEvent>                  // feedback de validação; UI coleta e mostra Snackbar

fun onCreatePlaylist(name: String)
fun onRenamePlaylist(id: Long, name: String)
fun onDeletePlaylist(id: Long)
fun onAddTrackToPlaylist(playlistId: Long, trackId: Long)
fun onRemoveTrackFromPlaylist(playlistId: Long, trackId: Long)
fun onPlayPlaylist(playlistId: Long, startIndex: Int = 0, shuffle: Boolean = false)
```

Regras de comportamento:
- Validação de nome (criar e renomear): `trim()`; vazio é rejeitado; duplicado (case-insensitive, ignorando a própria playlist ao renomear) é rejeitado; máximo 50 caracteres (excedente rejeitado). A rejeição emite `UiEvent.Message` em `uiEvents` e não altera o banco.
- `onAddTrackToPlaylist`: se a faixa já está na playlist, não duplica e emite mensagem; senão entra no fim (`position = último + 1`).
- `onRemoveTrackFromPlaylist`: remove e reindexa `position` para ficar contíguo.
- `onDeletePlaylist`: apaga a playlist e, por CASCADE, suas faixas (a UI deve confirmar antes).
- `onPlayPlaylist`: a fila passa a ser as faixas presentes da playlist; `startIndex` refere-se a essa lista (inválido é ignorado); `shuffle = true` liga o shuffle do player (`shuffleEnabled`). Playlist sem faixas presentes emite mensagem e não toca. Respeita a regra da 2A de enfileirar se o `MediaController` ainda não conectou.
- Os StateFlows usam `stateIn(viewModelScope, WhileSubscribed(5_000), valorInicial)`.

### 8.4 Contabilização de reproduções (dono: `PlaybackService`, `service/PlayCounter.kt`)
A contagem é feita por um `Player.Listener` no `ExoPlayer` do `PlaybackService`, com scope próprio. NÃO depende de Activity nem de `MusicViewModel` (funciona com o app fechado/finalizado). O `MusicViewModel` não conta mais; ele apenas define `durationMs` no `MediaMetadata` do `MediaItem`. Sem mudança em assinaturas públicas.

Regras:
- Tempo ouvido: medido por `elapsedRealtime`, acumulado SOMENTE enquanto `isPlaying == true`; não é a posição do player.
- Limiar `N = min(30_000 ms, durationMs / 2)` (30 s se a duração for desconhecida/0). Ao atingir `N` de tempo ouvido, conta 1.
- Uma contagem por execução da faixa. Pausar/retomar/seek não geram dupla contagem. Ouvir o limiar e depois arrastar até o fim conta 1 (já contado ao atingir o limiar), nunca 2.
- `RepeatMode.ONE`: cada volta é uma nova execução. Replay depois de `STATE_ENDED` também inicia nova execução.
- Pular a faixa antes do limiar = 0.
- Fim natural (transição `AUTO`/`REPEAT` ou `STATE_ENDED`): conta somente se tempo ouvido >= `min(N, 5_000 ms)` E sem "seek até o fim". Seek que termina a menos de 1 s do fim (`NEAR_END_MS`) marca "seek até o fim"; um seek posterior para longe do fim desmarca.
- Duração da execução ao fechá-la na troca de faixa (`runDurationMs()`): usa `player.duration` apenas se o `mediaId` bate com o da execução; senão usa `durationMs` do `MediaMetadata`.
- `onTaskRemoved` faz flush da execução em andamento antes do `stopSelf`; `release()` em `onDestroy` completa as escritas em andamento.
- A gravação chama `PlayCountRepository.increment` fora da Main Thread.
- Limitações menores conhecidas: `NEAR_END_MS` só bloqueia seeks que param a até 1 s do fim; com duração desconhecida o anti-seek não bloqueia (a UI desabilita o slider); janela de milissegundos em que o último `increment` pode se perder se o processo morrer logo após `onDestroy`. "Anterior" da notificação após 3 s volta ao início do mesmo item (seek, sem execução nova), coerente com "uma contagem por execução".

### 8.5 Regras de fronteira
- A UI NÃO acessa Room, DAOs nem repositórios; só o `MusicViewModel`.
- O backend não importa nada de `ui/`.
- O `MusicViewModel` mantém as faixas por `trackId`; a UI sempre recebe `Track` completo.

### 8.6 Navegação e UX (dono: frontend)
- Navegação simples preservando o `MiniPlayer` persistente: barra inferior com 3 destinos (Biblioteca, Playlists, Ranking). `PlaylistDetail` é sub-destino de Playlists. `NowPlayingScreen` continua abrindo a partir do `MiniPlayer`.
- `PlaylistsScreen`: lista, criar, renomear e excluir (com diálogo de confirmação para excluir), estado vazio.
- `PlaylistDetailScreen`: faixas em ordem, remover faixa, "Tocar" e "Aleatório" (`onPlayPlaylist(id, 0, shuffle)`), tocar a partir de uma faixa (`startIndex`).
- `RankingScreen`: Top 10 com posição, capa, título/artista e `playCount`; estado vazio ("ainda sem reproduções").
- Ação "Adicionar à playlist" por faixa (menu na faixa, na biblioteca, no detalhe e no ranking): abre diálogo/sheet com as playlists e opção de criar nova.
- Feedback: coletar `uiEvents` e mostrar `Snackbar`.
- Tela de detalhe e demais telas respeitam `WindowInsets` e `imePadding` nos diálogos de texto.

---

## 9. Fase 3 - Acabamento (Settings, Shuffle All, Ordenação, backlog de menores)

O Equalizador DSP não faz parte desta fase (liberado na Fase 4, seção 10). Mudanças desta seção são ADITIVAS em relação às seções 1-8, salvo as indicadas como "alteração".
Dois blocos independentes: backend (programador) e UI (frontend), que trabalham em paralelo a partir desta API.

### 9.1 Enums e preferências (dono: programador, pacote `com.example.musicplayer.data.prefs`)
Persistência via DataStore Preferences (nova dependência Gradle, do programador). Sem Room novo, sem migração do banco (continua `version = 1`).

```kotlin
package com.example.musicplayer.data.prefs

enum class ThemeMode { SYSTEM, LIGHT, DARK }          // default: DARK (mantém o comportamento atual)

enum class SortOrder { TITLE, ARTIST, ALBUM, RECENT, MOST_PLAYED }   // default: TITLE
```

`SettingsRepository` (mesmo pacote) expõe `themeMode: Flow<ThemeMode>` e `sortOrder: Flow<SortOrder>` e as escritas `suspend`. Leitura com valor default se ausente/inválido. A ordenação escolhida É persistida.

### 9.2 `Track` (alteração aditiva, `data/Track.kt`)

```kotlin
val dateAddedSec: Long = 0L   // MediaStore.Audio.Media.DATE_ADDED (segundos); 0 se desconhecido. Último campo, com default.
```

### 9.3 `LibraryUiState` (alteração aditiva)

```kotlin
val sortOrder: SortOrder = SortOrder.TITLE
```

- `filteredTracks` = `tracks` filtradas por `query` e DEPOIS ordenadas por `sortOrder`. A lista exibida, a fila de `playTrack` e o `onShuffleAll` usam `filteredTracks`.
- `TITLE`/`ARTIST`/`ALBUM`: ordem alfabética case-insensitive (ARTIST e ALBUM desempatam por título). `RECENT`: `dateAddedSec` decrescente. `MOST_PLAYED`: `playCount` decrescente (faixas sem contagem = 0), desempate por título; usa os dados de `PlayCountEntity` já observados.
- `tracks` (lista completa) continua na ordem do MediaStore, sem ordenar.

### 9.4 Novas APIs do `MusicViewModel` (nomes EXATOS)

```kotlin
val themeMode: StateFlow<ThemeMode>                 // default DARK até o DataStore carregar

fun onSetThemeMode(mode: ThemeMode)                 // persiste no DataStore
fun onSortOrderChange(order: SortOrder)             // atualiza libraryState.sortOrder e persiste
fun onShuffleAll()                                  // toca filteredTracks em ordem aleatória e liga o shuffle do player
fun onResetRanking()                                // apaga TODAS as linhas de play_counts; emite UiEvent.Message de confirmação
fun onCreatePlaylistAndAddTrack(name: String, trackId: Long)   // cria (mesma validação de onCreatePlaylist) e adiciona a faixa; em falha de validação não faz nada e emite UiEvent.Message
```

Comportamento:
- `onShuffleAll`: se `filteredTracks` está vazia, emite `UiEvent.Message` e não toca. Senão, a fila passa a ser `filteredTracks`, `shuffleEnabled` fica `true` e a reprodução começa pelo INÍCIO da ordem embaralhada do player; a faixa atual aparece na primeira posição da fila (o controller pode mostrar o índice 0 por um instante). Não se usa índice inicial aleatório explícito (o ExoPlayer pularia parte da fila). O mesmo vale para `onPlayPlaylist(shuffle = true)`. Respeita a regra da 2A de enfileirar se o `MediaController` ainda não conectou.
- `onResetRanking`: a UI SEMPRE pede confirmação antes de chamar. `rankingState` volta a `top10 = emptyList()`.
- Tema (comportamento final): `isDarkTheme` e `onToggleTheme()` foram REMOVIDOS (seção 7 supersedida neste ponto); `themeMode` + `onSetThemeMode` são a API. A leitura inicial do tema tem timeout de 300 ms com fallback DARK (e TITLE para a ordenação). A UI resolve o tema assim: `SYSTEM -> isSystemInDarkTheme()`, `LIGHT -> false`, `DARK -> true`.
- `uiEvents` (alteração de tipo): passa de `SharedFlow<UiEvent>` para `Flow<UiEvent>` sobre `Channel(BUFFERED).receiveAsFlow()`, com UM ÚNICO coletor na UI (cada evento é entregue a um só coletor), para não perder eventos emitidos sem coletor. A UI continua coletando com `collect`/`LaunchedEffect`; nada muda no uso.
- Validação de duplicidade de nome de playlist (alteração): feita em Kotlin com `equals(ignoreCase = true)` (cobre Unicode, "Música" = "MÚSICA"), não mais por `COLLATE NOCASE` do SQL.
- `onPlayPlaylist` lê os links da playlist direto do banco (repositório, `first()`), não de `playlistsState.value`.
- Operações de escrita (`onCreatePlaylist`, `onRenamePlaylist`, `onDeletePlaylist`, `onRemoveTrackFromPlaylist`, `onAddTrackToPlaylist`) tratam exceções (try/catch) e emitem `UiEvent.Message` de erro em vez de falhar.

### 9.5 Contagem (PlayCounter, ajuste interno, sem mudar API)
- Anti-seek (final): após qualquer seek para frente, o fim natural só conta se o tempo ouvido for >= limiar `N` completo (não basta o mínimo de 5 s) (a regra de "seek até o fim" com `NEAR_END_MS` permanece como proteção adicional).
- `toTrack` no `MusicViewModel` passa a usar `mediaMetadata.durationMs` em vez de `0L`.

### 9.6 Settings, Shuffle All e chips (dono: frontend)
- Acesso a Settings: ícone de engrenagem no cabeçalho da `MusicLibraryScreen` (mantém a barra inferior com 3 abas). `SettingsScreen` é sub-destino (voltar do sistema retorna à Biblioteca), mantendo o `MiniPlayer`.
- `SettingsScreen` contém: seletor de tema (Sistema/Claro/Escuro via `themeMode`/`onSetThemeMode`); "Zerar ranking" com diálogo de confirmação (`onResetRanking`); seção "Sobre" com nome do app e versão (lida via `PackageInfo`/`LocalContext` na UI, sem passar pelo ViewModel).
- Shuffle All: botão "Aleatório" no cabeçalho da lista da `MusicLibraryScreen` (acima dos chips), chama `onShuffleAll()`; desabilitado/oculto com biblioteca vazia ou em loading.
- Chips de ordenação (single-select) abaixo da busca: A-Z (TITLE), Artista (ARTIST), Álbum (ALBUM), Recentes (RECENT), Mais tocadas (MOST_PLAYED); refletem `libraryState.sortOrder` e chamam `onSortOrderChange`. Não há chips de agrupamento por artista/álbum nesta fase.
- Pré-validação local nos diálogos de playlist: o diálogo NÃO fecha se o nome for inválido (vazio, > 50 caracteres ou duplicado em `playlistsState`, ignorando caixa), mantém o texto e mostra o erro. O ViewModel continua sendo a fonte da verdade.
- "Nova playlist" no seletor de "Adicionar à playlist" chama `onCreatePlaylistAndAddTrack(name, trackId)`.

---

## 10. Fase 4 - Equalizador DSP

Autorizada pelo usuário em 2026-11-18. Mudanças ADITIVAS em relação às seções 1-9. Dois blocos paralelos: backend (programador) e UI (frontend), a partir desta API.

### 10.1 Engine (dono: programador, pacote `com.example.musicplayer.service`)
- Classe `service/EqualizerManager`: usa `android.media.audiofx.Equalizer` no `PlaybackService`, ligado ao `audioSessionId` REAL do ExoPlayer via `Player.Listener.onAudioSessionIdChanged`. Ignora `C.AUDIO_SESSION_ID_UNSET`; recria o efeito quando o id muda.
- Toda criação/uso do `AudioEffect` em try/catch: aparelhos/emuladores que restrinjam AudioEffect resultam em `isAvailable = false`, sem crash.
- `release()` no `onDestroy` do `PlaybackService`.
- `EqualizerManager` é singleton de processo exposto por `StateFlow`, para o `MusicViewModel` ler o estado sem vazar `AudioEffect` (o ViewModel não guarda referência ao efeito).

### 10.2 Persistência (dono: programador, pacote `com.example.musicplayer.data.prefs`)
`SettingsRepository` (DataStore) passa a guardar:
- `eqEnabled: Boolean` (default `false`)
- `eqPresetIndex: Int` (`-1` = Personalizado; default `-1`)
- `eqBandLevelsMb: List<Int>` (níveis do modo custom; default tudo 0)

Os valores são reaplicados ao criar/recriar o efeito.

### 10.3 Modelos (dono: programador, pacote `viewmodel`, arquivo `EqualizerUiState.kt`)

```kotlin
package com.example.musicplayer.viewmodel

data class EqBandUi(val index: Int, val centerFreqHz: Int, val levelMb: Int)   // levelMb em milibels (100 mB = 1 dB)

data class EqualizerUiState(
    val isAvailable: Boolean = false,       // false => UI mostra "Equalizador indisponível"
    val enabled: Boolean = false,
    val bands: List<EqBandUi> = emptyList(),   // níveis atuais
    val minLevelMb: Int = 0,                // faixa do aparelho
    val maxLevelMb: Int = 0,
    val presets: List<String> = emptyList(), // nomes nativos (Equalizer.getPresetName)
    val selectedPreset: Int = -1            // índice em presets; -1 = "Personalizado"
)
```

### 10.4 API do `MusicViewModel` (nomes EXATOS)

```kotlin
val equalizerState: StateFlow<EqualizerUiState>

fun onSetEqualizerEnabled(enabled: Boolean)
fun onSelectEqPreset(index: Int)                  // aplica preset nativo; bands passam a refletir os níveis do preset
fun onSetEqBandLevel(band: Int, levelMb: Int)     // clamp em [minLevelMb, maxLevelMb]; selectedPreset passa a -1
fun onResetEqualizer()                            // todas as bandas a 0 mB, preset -1
```

Comportamento:
- Selecionar um preset NÃO apaga os níveis custom salvos (`eqBandLevelsMb`).
- Mover um slider após um preset parte dos níveis DO PRESET e vira Personalizado (`selectedPreset = -1`), passando esses níveis a ser os custom salvos.
- `onResetEqualizer` zera as bandas e volta a Personalizado.

### 10.5 UI (dono: frontend)
- `EqualizerSheet` (`ModalBottomSheet`) aberto por um botão na `NowPlayingScreen` e por um item em `SettingsScreen`.
- Conteúdo: `Switch` Ligar/Desligar; seletor de presets (chips ou dropdown, incluindo "Personalizado"); sliders VERTICAIS por banda com rótulo de frequência (Hz se < 1000, senão kHz; ex.: "60 Hz", "3,6 kHz") e de ganho em dB (ex.: "+3 dB"); botão "Resetar" (0 dB).
- Sliders desabilitados visualmente quando `enabled = false`. Se `isAvailable = false`, mostra "Equalizador indisponível".
- Respeita `WindowInsets` e os temas claro/escuro.

### 10.6 Regras de fronteira
- A UI só usa `equalizerState` e as 4 funções acima; NÃO importa `android.media.audiofx` nem `service/`.
- Nenhum `AudioEffect` fora de `service/`.
- O backend não importa nada de `ui/`.

---

## 11. Pós-Fase 4 - Horas escutadas e navegação com 4 abas

Mudanças ADITIVAS (sem alterar assinaturas existentes).
- `SettingsRepository`: `totalListenedMs: Flow<Long>` (default 0) e `suspend addListenedMs(deltaMs)` (soma atômica no DataStore, chave `total_listened_ms`).
- `PlayCounter(player, repository, onListened: suspend (Long) -> Unit = {})`: acumula o tempo ouvido (relógio, só com `isPlaying`) e chama `onListened(delta)` fora da Main Thread em lotes (>= 10 s, ao pausar, na troca de faixa, em `flush` e `release`). O `PlaybackService` liga `onListened` ao `SettingsRepository`.
- `MusicViewModel.totalListenedMs: StateFlow<Long>`.
- UI: `SettingsScreen` recebe `totalListenedMs` e exibe "Tempo total escutado" (`formatListenTime`). Navegação passa a 4 abas (Biblioteca, Playlists, Ranking, Settings; seção 8.6/9.6 supersedidas: sem engrenagem no cabeçalho). `MusicLibraryScreen` perde `onOpenSettings`; o "Aleatório" do cabeçalho vira o FAB flutuante "Shuffle All" (mesmo `onShuffleAll`).

---

## 12. Pós-Fase 4 - Favoritos, fundo animado e feedback tátil

Mudanças ADITIVAS.
- `SettingsRepository`: `favoriteIds: Flow<Set<Long>>` e `suspend toggleFavorite(trackId)` (DataStore, `stringSet` em `favorite_track_ids`; atômico). Room continua `version = 1`.
- `MusicViewModel`: `favoriteIds: StateFlow<Set<Long>>` e `onToggleFavorite(trackId: Long)`.
- UI: a `MainActivity` provê `LocalFavorites` (`FavoritesController`); `FavoriteButton(trackId)` consome sem mudar assinaturas das telas. `HapticIconButton`, `pressScale` e `AnimatedBackground` ficam em `ui/components` (só UI, sem contrato de dados).
- Backup: favoritos seguem o DataStore (aceito, como tema/ordenação), mas os ids são do MediaStore do aparelho.

---

## 13. Pós-Fase 4 - Filtro de favoritas e capa da notificação

Mudanças ADITIVAS.
- `LibraryUiState.favoritesOnly: Boolean = false`; `MusicViewModel.onToggleFavoritesFilter()`. `filteredTracks` = tracks -> (favoritas se `favoritesOnly`) -> busca -> ordenação. Com `favoritesOnly` e lista vazia, `emptyReason = NO_SEARCH_RESULTS`. Não persistido.
- `MediaItem.mediaMetadata.artworkUri`: no API 29+ é a `Track.contentUri` (quando há capa), no API 24-28 a URI legada de álbum. O `PlaybackService` usa `ArtworkBitmapLoader` (`MediaSession.setBitmapLoader`) para carregar a capa de cada faixa sem cache.

---

## 14. Pós-Fase 4 - Otimização e release

Sem mudança de contratos de dados ou de API do `MusicViewModel`. Mudanças ADITIVAS de assinatura de UI (com default):
- `MiniPlayer(..., positionMs: () -> Long = { state.positionMs })` e `NowPlayingScreen(..., positionMs: () -> Long = { state.positionMs })`: a posição é lida de forma adiada (draw/`SeekBar`). A `MainActivity` entrega o `playerState` ESTRUTURAL (com `positionMs = 0`) e a posição real via esse lambda; as demais telas não devem ler `positionMs` do `PlayerUiState` da `MainActivity`.
- `MusicViewModel` atualiza `positionMs` só se `playerState` tem coletores; o valor de `positionMs` em `playerState` continua válido para quem coleta.
- Build: `release` com R8 + `shrinkResources` + `proguard-rules.pro`; `androidx.profileinstaller` + `src/main/baseline-prof.txt`. Qualquer nova classe acessada por reflexão (serialização, `Class.forName`) exige regra `-keep` em `app/proguard-rules.pro`.

---

## 6. Histórico de mudanças

| Data | Mudança | Autor |
|---|---|---|
| 2026-11-18 | Versão inicial dos contratos da Fase 1 | documentador |
| 2026-11-18 | `MediaStoreRepository` renomeado para `MediaStoreTrackRepository`; `toggleShuffle`/`cycleRepeatMode` fora da Fase 1 | documentador |
| 2026-11-18 | Fase 1 concluída (validada em dispositivo físico); nenhum contrato alterado | documentador |
| 2026-11-18 | Adicionada seção 7 (Fase 2A): `onToggleShuffle`, `onCycleRepeatMode`, `onPlayFromQueue`, `onRemoveFromQueue`, `isDarkTheme`/`onToggleTheme`, regra de capas na `ui/components`; revoga a exclusão de shuffle/repeat | documentador |
| 2026-11-18 | Seção 7 refinada: `queue` na ordem efetiva de reprodução com shuffle (índice de exibição convertido pelo ViewModel); regra de capas com `contentUri`, alvos 128/512px e cache `maxMemory/8` | documentador |
| 2026-11-18 | Fase 2A concluída (validada em dispositivo). Adicionada seção 8 (Fase 2B): Room (entidades/DAOs), Playlists, Ranking Top 10, ações e StateFlows do `MusicViewModel`, regra de contagem e navegação; Room/Playlists/Rankings liberados, Equalizador DSP continua proibido | documentador |
| 2026-11-18 | Seção 8.4: registrada limitação conhecida da contagem de reproduções (só com o ViewModel vivo) | documentador |
| 2026-11-18 | Fase 2B concluída (validada em celular físico); limitação da contagem (8.4) marcada como conhecida/aceita; nenhum contrato alterado | documentador |
| 2026-11-18 | Seção 8.4 reescrita: contagem migrou do `MusicViewModel` para o `PlaybackService` (`PlayCounter`), nova regra anti-seek e de fim natural; limitação anterior removida; sem mudança em assinaturas públicas | documentador |
| 2026-11-18 | Contagem de reproduções no `PlaybackService` (8.4) validada em celular físico pelo usuário; nenhum contrato alterado | documentador |
| 2026-11-18 | Adicionada seção 9 (Fase 3): `ThemeMode`/`SortOrder` (DataStore), `Track.dateAddedSec`, `LibraryUiState.sortOrder`, `onSetThemeMode`, `onSortOrderChange`, `onShuffleAll`, `onResetRanking`, `onCreatePlaylistAndAddTrack`; `uiEvents` passa a `Flow<UiEvent>`; `isDarkTheme`/`onToggleTheme` deprecados | documentador |
| 2026-11-18 | Fase 3 implementada e auditada: seção 9 atualizada com o comportamento final (`isDarkTheme`/`onToggleTheme` removidos; shuffle começa pelo início da ordem embaralhada; `uiEvents` com coletor único; anti-seek final; timeout de 300 ms do tema); sem outras mudanças de assinatura | documentador |
| 2026-11-18 | Fase 3 concluída (validada em dispositivo); Equalizador DSP continua PROIBIDO até autorização do usuário para a Fase 4; nenhum contrato alterado | documentador |
| 2026-11-18 | Fase 4 (Equalizador DSP) autorizada pelo usuário; adicionada seção 10: `EqualizerManager`, persistência no DataStore, `EqualizerUiState`/`EqBandUi`, `equalizerState` e 4 ações do `MusicViewModel`, `EqualizerSheet`; textos de "Equalizador PROIBIDO" ajustados | documentador |
| 2026-11-18 | Adicionada seção 11: `totalListenedMs` (DataStore, `PlayCounter.onListened`), Settings como 4ª aba, FAB Shuffle All | documentador |
| 2026-11-18 | Adicionada seção 12: favoritos (DataStore, `favoriteIds`/`onToggleFavorite`), `LocalFavorites`, fundo animado e feedback tátil | documentador |
| 2026-11-18 | Adicionada seção 13: `favoritesOnly`/`onToggleFavoritesFilter`, `artworkUri` e `ArtworkBitmapLoader` | documentador |
| 2026-11-18 | Adicionada seção 14: posição por lambda (`positionMs`), loop de posição só com coletores, R8/shrink, `proguard-rules.pro`, Baseline Profile; sem mudança de contratos de dados | documentador |
| 2026-11-18 | Build release passa a ser assinado via `keystore.properties` (fora do git); sem mudança de contratos | documentador |
