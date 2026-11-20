**FASE 1 CONCLUÍDA — 2026-11-18**

# TASKS.md - Resonance (Fase 1 / MVP Básico)

Regra: itens só viram `[x]` após aprovação do `@revisor` (`./gradlew assembleDebug` + auditoria). Quem atualiza o checklist é o `documentador`.
Fora de escopo na Fase 1: Room, Rankings, Playlists persistentes, Equalizador DSP.
Contratos: ver `CONTRACTS.md`.

---

## Programador (Backend/Áudio)
Arquivos: `app/build.gradle.kts`, `AndroidManifest.xml`, `data/`, `service/`, `viewmodel/`.

- [x] Gradle: dependências Media3 (`media3-exoplayer`, `media3-session`, `media3-ui` se necessário) e `lifecycle-viewmodel`/`lifecycle-runtime-compose`
- [x] Manifest: permissão `READ_MEDIA_AUDIO` (API 33+)
- [x] Manifest: permissão `READ_EXTERNAL_STORAGE` com `android:maxSdkVersion="32"`
- [x] Manifest: `FOREGROUND_SERVICE` e `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (+ `POST_NOTIFICATIONS` se aplicável)
- [x] Manifest: declarar `PlaybackService` com `foregroundServiceType="mediaPlayback"` e intent-filter `MediaSessionService`
- [x] `data/Track.kt` conforme `CONTRACTS.md`
- [x] `data/` repositório MediaStore: leitura de faixas locais (somente música, `IS_MUSIC`), com fallbacks de título/artista/álbum e `albumArtUri`
- [x] `service/PlaybackService`: `ExoPlayer` + `MediaSessionService` (+ `MediaSession`, notificação de mídia, audio focus, `handleAudioBecomingNoisy`)
- [x] `viewmodel/LibraryUiState.kt`, `PlayerUiState.kt` (e `RepeatMode`) conforme `CONTRACTS.md`
- [x] `viewmodel/MusicViewModel`: `libraryState`, `playerState`, conexão via `MediaController`, atualização de posição
- [x] `MusicViewModel`: `onPermissionResult`, `refreshLibrary`, `onSearchQueryChange`
- [x] `MusicViewModel`: `playTrack`, `togglePlayPause`, `seekTo`, `skipNext`, `skipPrevious`
- [x] Decisão do coordenador: `toggleShuffle`/`cycleRepeatMode` ficam FORA da Fase 1 (não implementados; a UI não mostra botões de shuffle/repeat)

## Frontend (UI Jetpack Compose)
Arquivos: `ui/theme/`, `ui/components/`, `ui/screens/`, `MainActivity.kt`.

- [x] Tema Resonance Dark (OLED `#121316`, Coral `#FF4D6D`, Amber `#FFAA00`, Cyan `#00E5FF`) em `ui/theme/`
- [x] Tema Light básico + tipografia/formas Material 3
- [x] `MusicLibraryScreen`: lista de faixas a partir de `libraryState.filteredTracks` (ressalva: a capa do álbum é só placeholder, sem carregar `albumArtUri`)
- [x] `MusicLibraryScreen`: campo de busca (`onSearchQueryChange`)
- [x] `MusicLibraryScreen`: estado vazio (`NO_TRACKS`, `NO_SEARCH_RESULTS`) e estado de erro
- [x] `MusicLibraryScreen`: estado de permissão negada (`NO_PERMISSION`) com botão de solicitar permissão
- [x] `MusicLibraryScreen`: skeleton de carregamento (`isLoading`)
- [x] `MiniPlayer` persistente (visível quando `currentTrack != null`), respeitando `WindowInsets`
- [x] `NowPlayingScreen`: capa, título/artista, progresso com `seekTo`, play/pause, anterior/próxima, respeitando `WindowInsets`; SEM slider de volume (ressalva: a capa do álbum é só placeholder, sem carregar `albumArtUri`)
- [x] Navegação Biblioteca <-> NowPlaying (a partir do MiniPlayer)
- [x] `MainActivity.kt`: edge-to-edge, tema, obtenção do `MusicViewModel`, launcher de permissão (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`)

## Documentador
- [x] `CONTRACTS.md` criado (v1, 2026-11-18)
- [x] `TASKS.md` criado
- [x] Atualizar `[x]` nos itens acima após cada aprovação do `@revisor`

## Revisor (@revisor, Opus)
Invocado ao final de cada bloco (backend, frontend, integração).

- [x] Bloco backend: auditoria de ciclo de vida (Service/MediaController/ViewModel), permissões e Manifest + `./gradlew assembleDebug`
- [x] Bloco frontend: auditoria de Compose (recomposição, `WindowInsets`, estados, ausência de slider de volume, UI sem acesso direto a MediaStore/Player) + `./gradlew assembleDebug`
- [x] Bloco integração: fluxo completo (permissão -> lista -> busca -> tocar -> MiniPlayer -> NowPlaying -> segundo plano) + `./gradlew assembleDebug` (origem da aprovação: validação MANUAL pelo usuário em celular físico, 2026-11-18; o @revisor não rodou este teste)
- [x] Aprovação final da Fase 1 (origem: validação manual do usuário em dispositivo físico, 2026-11-18, após build `assembleDebug` aprovado pelo @revisor)

---

## Pendências e débitos técnicos (backlog pós-Fase 1, apontados pelo @revisor, não resolvidos)
- [x] `MusicViewModel.kt:115`: `playTrack` descarta o toque se o `MediaController` ainda não conectou (resolvido na 2A)
- [x] URI `albumart` legada e pouco confiável no API 29+ (o caminho correto seria `loadThumbnail`) (resolvido: `loadThumbnail` agora recebe `Track.contentUri` no API 29+)
- [x] `MainActivity.kt:56-60`: permissão de áudio não é reavaliada em `ON_RESUME` (resolvido na 2A)
- [x] `MusicLibraryScreen`: faltam insets horizontais e `imePadding` (resolvido na 2A)
- [x] `PlaybackService.onTaskRemoved` não chama `super` (resolvido na 2A)
- [x] Teste de integração em dispositivo (feito manualmente pelo usuário em celular físico, 2026-11-18)

## Decisões
- 2026-11-18: `toggleShuffle`/`cycleRepeatMode` fora da Fase 1 (decisão do coordenador).
- 2026-11-18: Decisão anterior revogada: `toggleShuffle`/`cycleRepeatMode` entram na Fase 2A como `onToggleShuffle`/`onCycleRepeatMode` (ver `CONTRACTS.md` seção 7).

---

# FASE 2A - Polimento Visual, Capas Reais, Fila e Controles de Transporte
**FASE 2A CONCLUÍDA — 2026-11-18** (correções confirmadas pelo @revisor; `assembleDebug` passou; integração validada manualmente pelo usuário em celular).
Verificação da Fase 1: tudo [x]; os 5 débitos do backlog da Fase 1 foram resolvidos na 2A (ver seção de pendências). Menores ainda abertos seguem como backlog.
Proibido: Room, Playlists, Rankings, Equalizador. Contratos: `CONTRACTS.md` seção 7.

## 2A - Programador
- [x] `playTrack` enfileira/aguarda se o `MediaController` ainda estiver conectando (débito `MusicViewModel.kt:115`)
- [x] Alinhar `queue` com `currentIndex` em todos os cenários (play, skip, remoção, fim da fila) (ressalva: com shuffle ligado a fila aparece na ordem original, mas Próxima/Anterior seguem a ordem embaralhada; ver débito importante 1)
- [x] `PlaybackService.onTaskRemoved`: chamar `super.onTaskRemoved(rootIntent)`
- [x] `onToggleShuffle()` integrado ao ExoPlayer (`shuffleModeEnabled`) e refletido em `PlayerUiState.shuffleEnabled`
- [x] `onCycleRepeatMode()` (OFF -> ALL -> ONE -> OFF) integrado ao ExoPlayer (`repeatMode`) e refletido em `PlayerUiState.repeatMode`
- [x] `onPlayFromQueue(index)` e `onRemoveFromQueue(index)` em memória
- [x] `isDarkTheme: StateFlow<Boolean>` + `onToggleTheme()` (em memória)

## 2A - Frontend
- [x] Light Theme completo + alternância de tema (referência `music_library_light` e `now_playing`), usando `isDarkTheme`/`onToggleTheme` (ressalva: Coral fixo no `TrackItem`, não segue `colorScheme.primary` no tema claro)
- [x] Capas reais: `loadThumbnail` (API 29+), fallback MediaStore (API 24-28), cache em memória, carregamento fora da Main Thread, em `ui/components`
- [x] Capas na lista, no MiniPlayer e na NowPlaying; placeholder em gradiente só quando não houver capa (ressalva: capa de 512dp da NowPlaying vs cache de 12 MB; ver débito importante 2)
- [x] Botões Shuffle e Repeat na `NowPlayingScreen` (refletindo `shuffleEnabled`/`repeatMode`)
- [x] `ModalBottomSheet` de Fila (tocar item via `onPlayFromQueue`, remover item via `onRemoveFromQueue`)
- [x] Reavaliar permissão de áudio em `ON_RESUME` (débito `MainActivity.kt:56-60`)
- [x] Insets horizontais e `imePadding` na busca da `MusicLibraryScreen`

## 2A - Revisor (@revisor)
- [x] Auditoria: bitmaps assíncronos sem travar a Main Thread, ciclo de vida, permissões, insets + `./gradlew assembleDebug`
- [x] Bloco de integração em dispositivo: validação MANUAL do usuário no celular, 2026-11-18 (sugestão de testes: capa ao pular faixa no MiniPlayer e na NowPlaying, fila com shuffle ligado/desligado, remover a faixa atual, teclado na busca e sheet da fila em gesture nav)
- [x] Aprovação final da Fase 2A (2026-11-18)

---

## Pendências e débitos técnicos da Fase 2A (apontados pelo @revisor, backlog)

### Importantes
- [x] 1. Shuffle x fila: `queue` deveria seguir a ordem do timeline com shuffle (ou a UI indicar "ordem aleatória"), com mapeamento do índice de exibição para o real em `onPlayFromQueue`/`onRemoveFromQueue` (`MusicViewModel.kt:221`, `NowPlayingScreen.kt:140`)
- [x] 2. Tamanho das capas: limitar `sizePx` (~768-1024px) e/ou calcular o limite do `LruCache` pelo `maxMemory`; corrigir o comentário "1/8 de 16 MB" (`AlbumArt.kt:37`, `NowPlayingScreen.kt:65`)
- [x] 3. `loadThumbnail` no API 29+ deveria receber `Track.contentUri` em vez da URI legada `albumart` (`AlbumArt.kt:54`)

### Menores
- [ ] Set de "misses" do cache de capas é permanente e sem limite
- [x] `produceState` começa em `null` (flash de placeholder); inicializar com `CoverCache.get` (resolvido)
- [x] Decodificação duplicada da mesma capa em paralelo (resolvido: decodificação compartilhada por chave)
- [x] Key `"${id}#$index"` da fila deveria ser só `id` (resolvido: key composta `{id}#{n}`)
- [ ] Possível duplicação de `contentPadding` de `navigationBars` no `ModalBottomSheet` (conferir em dispositivo)
- [x] `queueTrackById` nunca é podado (resolvido: agora é podado)
- [ ] `stopSelf` redundante em `PlaybackService.onTaskRemoved`
- [ ] Reload da biblioteca ao girar a tela (`permissionChecked` com `remember`)
- [ ] Custo O(n) a cada evento do player no recálculo da fila (cachear por "timeline mudou" se a biblioteca chegar a dezenas de milhares de faixas)
- [ ] Set de "misses" permanente: falha transitória só é retentada após reiniciar o app

### Correções feitas pelo @revisor durante a auditoria
- `AlbumArt`: capa antiga presa ao trocar de faixa (MiniPlayer/NowPlaying) corrigida.
- `AlbumArt`: entrada presa no `inFlight` por corrida corrigida (o `async` passou a ser LAZY).

## Decisões (Fase 2B)
- 2026-11-18: Decisão de adiar a migração da contagem revertida: a migração foi feita e auditada (ver abaixo).
- 2026-11-18: Migração da contagem de reproduções para o `PlaybackService` ADIADA por decisão do usuário; fica como débito técnico aceito (ver débito importante 1 da 2B).
- 2026-11-18: Fase 2B autorizada pelo usuário: Room, Playlists e Rankings liberados; Equalizador DSP continua PROIBIDO.
- 2026-11-18 (2B): faixa não repete na mesma playlist (chave primária composta); `PlaylistUi` traz `tracks` completas (só faixas presentes); órfãs ficam no banco e são ignoradas pela UI; sem limpeza nem migração na 2B (`version = 1`).
- 2026-11-18 (2B): limite do nome da playlist = 50 caracteres, comparação de duplicados case-insensitive; feedback via `SharedFlow<UiEvent>` (`uiEvents`).
- 2026-11-18 (2B): contagem de reprodução = tempo OUVIDO >= min(30 s, 50% da duração) ou fim natural, uma vez por execução (detalhes em `CONTRACTS.md` 8.4).
- 2026-11-18 (2B): navegação por barra inferior (Biblioteca, Playlists, Ranking), com `PlaylistDetail` como sub-destino.

---

# FASE 2B - Playlists persistentes e Ranking Top 10 (Room)
**FASE 2B CONCLUÍDA — 2026-11-18** (auditada pelo @revisor: análise estática + `assembleDebug`; integração validada manualmente pelo usuário em celular físico).
Contratos: `CONTRACTS.md` seção 8. Proibido: Equalizador DSP. Fora de escopo: chips de filtro, FAB Shuffle All, Settings.

## 2B - Programador
- [x] Gradle: dependências Room (`room-runtime`, `room-ktx`, `room-compiler`) + plugin KSP via version catalog; `room.schemaLocation` se necessário
- [x] `data/db`: `PlaylistEntity`, `PlaylistTrackEntity` (FK CASCADE, PK composta), `PlayCountEntity`
- [x] `data/db`: `PlaylistDao` e `PlayCountDao` com `Flow`, `increment` atômico, reindexação de `position`
- [x] `data/db`: `ResonanceDatabase` (`version = 1`, `exportSchema` configurado, sem migração)
- [x] Repositórios (`PlaylistRepository`, `PlayCountRepository`) isolando os DAOs
- [x] `viewmodel`: `PlaylistUi`, `PlaylistsUiState`, `RankingEntry`, `RankingUiState`, `UiEvent`
- [x] `MusicViewModel`: `playlistsState` e `rankingState` reativos (Top 10 só com faixas presentes; órfãs ignoradas)
- [x] `MusicViewModel`: `onCreatePlaylist`, `onRenamePlaylist`, `onDeletePlaylist` com validação (vazio/duplicado/50 chars) e `uiEvents`
- [x] `MusicViewModel`: `onAddTrackToPlaylist` (sem duplicar) e `onRemoveTrackFromPlaylist` (reindexa)
- [x] `MusicViewModel`: `onPlayPlaylist(playlistId, startIndex, shuffle)` enfileirando no player (respeitando conexão do `MediaController`)
- [x] Contagem de reproduções: tempo ouvido >= min(30 s, 50%) ou fim natural, uma vez por execução, sem dupla contagem em pausa/retomada/seek (ressalva: ver débito importante 1 da 2B: a contagem só funciona com o `MusicViewModel` vivo)

## 2B - Frontend
- [x] Navegação: barra inferior (Biblioteca, Playlists, Ranking) preservando o `MiniPlayer`; `PlaylistDetail` como sub-destino
- [x] `PlaylistsScreen`: lista, criar, renomear, excluir (com confirmação), estado vazio
- [x] `PlaylistDetailScreen`: faixas, remover faixa, Tocar/Aleatório, tocar a partir de uma faixa
- [x] `RankingScreen`: Top 10 (posição, capa, título/artista, `playCount`) e estado vazio
- [x] Ação "Adicionar à playlist" por faixa (menu na biblioteca, no detalhe e no ranking) com opção de criar nova
- [x] Coleta de `uiEvents` com `Snackbar`; diálogos de texto com `imePadding`; `WindowInsets` respeitados

## 2B - Revisor (@revisor)
- [x] Auditoria: threads do Room, Flow/lifecycle, contagem sem dupla, SQL/integridade (FK/CASCADE, reindexação), órfãs, insets + `./gradlew assembleDebug`
- [x] Bloco de integração em dispositivo: validação MANUAL do usuário em celular físico, 2026-11-18, seguindo o roteiro de 9 passos (o @revisor só fez análise estática e build)
- [x] Aprovação final da Fase 2B (2026-11-18; origem: validação manual do usuário em celular físico + auditoria estática e build do @revisor)

---

## Pendências e débitos técnicos da Fase 2B (apontados pelo @revisor, backlog)

### Importantes
- [x] 1. RESOLVIDO em 2026-11-18 (auditado pelo revisor; validado em dispositivo): contagem migrada para o `PlaybackService` (`service/PlayCounter.kt`). Histórico: era débito técnico aceito, adiado em 2026-11-18. Contagem de reproduções vive no `MusicViewModel` (`MusicViewModel.kt` ~195-270, 540): se a Activity é finalizada (voltar), `onCleared` remove o listener e cancela o job, e a música que segue tocando no `PlaybackService` não é contada; o `registerPlay` pode ser cancelado antes do `increment` no Room. Solução: mover a contagem para um `Player.Listener` no `PlaybackService` com scope próprio. Limitação conhecida: com a Activity finalizada a música segue tocando e NÃO é contada. (Texto original do débito; a migração foi concluída.)
- [x] 2. Backup: o banco `resonance.db` ia para o backup na nuvem/transferência, com `trackId` (MediaStore `_ID`) válidos só no aparelho; excluir o banco de `backup_rules.xml` e `data_extraction_rules.xml` (confirmado: programador excluiu `resonance.db`, `-wal`, `-shm` e `-journal` em `backup_rules.xml` e `data_extraction_rules.xml` (cloud-backup e device-transfer) e o `assembleDebug` passou depois da mudança; restauração real em dispositivo não testada. Ressalva: exclusão por nome, um banco futuro precisaria ser excluído também)

### Menores (backlog pós-2B)
- [x] Seek até o fim conta como fim natural sem ter sido ouvido (resolvido pela regra anti-seek do `PlayCounter`)
- [x] `onPlayFromQueue` após `STATE_ENDED` não reinicia a execução (resolvido: replay inicia nova execução)
- [ ] `onPlayPlaylist` lê `playlistsState.value` (ler do repositório com `first()`)
- [ ] `uiEvents` sem replay perde eventos sem coletor
- [ ] `COLLATE NOCASE` só ASCII ("Música" x "MÚSICA" não é detectado como duplicado)
- [ ] create/rename/delete/removeTrack sem try/catch
- [ ] Shuffle com `startIndex = 0` pode mostrar índice 0 por um instante (confirmar em dispositivo)
- [ ] `AlertDialog` fecha mesmo com validação falha e perde o texto; `imePadding` inócuo
- [ ] "Nova playlist" no seletor não adiciona a faixa
- [ ] Snackbar fica atrás da NowPlaying/sheets
- [ ] Voltar do sistema em Playlists/Ranking fecha o app em vez de ir à Biblioteca

### Correção feita pelo @revisor durante a auditoria
- `libraryTracks` com `distinctUntilChanged()` no `MusicViewModel` (evita recombinar playlists/ranking a cada letra da busca).

### Roteiro de teste em dispositivo da 2B (responsável: usuário)
1. Nomes inválidos: vazio, só espaços, 51 caracteres, duplicado com outra caixa -> Snackbar sem mudar o banco; renomear mudando só a caixa é aceito.
2. Adicionar a mesma faixa duas vezes -> mensagem "já está".
3. Remover faixa do meio, conferir a ordem, fechar e reabrir o app.
4. Detalhe + girar a tela + excluir a playlist aberta -> volta à lista.
5. Tocar, Aleatório e tocar a partir da 3ª faixa.
6. Contagem: 35 s com pausa/retomada = 1; pular aos 10 s = 0; faixa curta até o fim = 1; repeat ONE com 2 voltas = 2; arrastar o slider até o fim hoje conta 1 (débito menor).
7. Com busca ativa, tocar no Ranking uma faixa fora do filtro.
8. Sair pelo voltar com a música tocando e esperar 40 s: hoje NÃO conta (débito importante 1).
9. Apagar um arquivo de música e reabrir: some da playlist e do ranking, e o Top 10 completa.

---

## Pós-2B - Migração da contagem de reproduções para o PlaybackService
Status: concluída e auditada pelo @revisor (`assembleDebug` passou, sem bloqueantes nem importantes); validado em dispositivo pelo usuário (2026-11-18). Contrato: `CONTRACTS.md` 8.4.

- [x] Contagem migrada do `MusicViewModel` para `service/PlayCounter.kt` (`Player.Listener` no `ExoPlayer` do serviço) com regra anti-seek (2026-11-18)
- [x] `MusicViewModel` deixa de contar e define `durationMs` no `MediaMetadata`
- [x] Teste em dispositivo da migração da contagem: validação MANUAL do usuário em celular físico, 2026-11-18, roteiro de 6 passos (Activity encerrada, anti-seek sem ouvir, ouvir 40 s e arrastar até o fim, replay após o fim, repeat ONE e pular aos 10 s)

### Correção feita pelo @revisor
- `runDurationMs()`: ao fechar a execução na troca de faixa, a duração usada era a da faixa seguinte; agora usa `player.duration` só se o `mediaId` bate com o da execução, senão o `durationMs` do `MediaMetadata`.

### Novos menores (backlog)
- [ ] 1. `NEAR_END_MS = 1 s` só bloqueia seeks que param a até 1 s do fim (ouvir 5 s e arrastar até 2 s antes do fim ainda conta; alternativa: exigir tempo ouvido >= limiar após qualquer seek para frente)
- [ ] 2. Com duração desconhecida o anti-seek não bloqueia (a UI desabilita o slider; seek externo cai na regra dos 5 s)
- [ ] 3. `toTrack` no `MusicViewModel` (~449) ainda usa `durationMs = 0L` (poderia usar `mediaMetadata.durationMs`)
- [ ] 4. Janela de milissegundos em que o último `increment` pode se perder se o processo morre logo após `onDestroy`
- Observação: "Anterior" da notificação depois de 3 s volta ao início do mesmo item (seek no mesmo item) e não abre execução nova; coerente com "uma contagem por execução".

### Roteiro: Teste em dispositivo da migração da contagem (usuário)
1. Tocar faixa de 2 min+, sair pelo voltar com a música tocando, esperar 40 s, reabrir o Ranking: +1; repetir tirando o app dos recentes após 35 s: +1.
2. Dar play e arrastar o slider até o final em menos de 2 s: +0.
3. Ouvir 40 s e arrastar até o fim: +1, não +2.
4. Repeat OFF, deixar a última faixa da fila terminar (+1), tocá-la de novo até o fim (+1); abrir o app depois do fim sem dar play: +0.
5. Repeat ONE com faixa curta (< 1 min), 2 voltas completas: +2.
6. Pular a faixa aos 10 s: +0.

---

# FASE 3 - Acabamento (Settings, Shuffle All, Ordenação, backlog de menores)
**FASE 3 CONCLUÍDA — 2026-11-18** (auditada pelo @revisor: análise estática + `assembleDebug`; integração validada manualmente pelo usuário em celular físico).
Autorizada pelo usuário em 2026-11-18. Equalizador DSP ficou para a Fase 4. Contratos: `CONTRACTS.md` seção 9.

## Decisões (Fase 3)
- 2026-11-18: Próxima fase planejada: Equalizador DSP (Fase 4). Autorizada pelo usuário em 2026-11-18 (ver FASE 4 abaixo).
- Acesso a Settings: ícone de engrenagem no cabeçalho da Biblioteca (mais simples que uma 4ª aba); a barra inferior continua com 3 abas.
- Tema: `ThemeMode` (SYSTEM/LIGHT/DARK), default DARK (mantém o comportamento atual), persistido em DataStore Preferences. `isDarkTheme`/`onToggleTheme` deprecados e removidos no fim da fase.
- Chips: só ORDENAÇÃO (A-Z, Artista, Álbum, Recentes, Mais tocadas); agrupamento por artista/álbum NÃO entra nesta fase (mais complexo, sem demanda). Ordenação persistida em DataStore. "Recentes" exige `Track.dateAddedSec` (`DATE_ADDED` do MediaStore).
- Shuffle All: botão "Aleatório" no cabeçalho da lista (não FAB), usa `filteredTracks` (respeita busca e ordenação).
- "Zerar ranking": apaga todas as linhas de `play_counts`, sempre com confirmação.
- `uiEvents` vira `Flow` com buffer (sem perda de eventos); duplicidade de nome de playlist validada em Kotlin (`ignoreCase`) em vez de `COLLATE NOCASE`.
- Diálogos de playlist: pré-validação local na UI; o ViewModel segue como fonte da verdade (sem mudar a assinatura de `onCreatePlaylist`/`onRenamePlaylist`).
- Itens ADIADOS do backlog (continuam [ ] no backlog): custo O(n) do recálculo da fila (só relevante com dezenas de milhares de faixas, sem evidência de problema); anti-seek com duração desconhecida (a UI já desabilita o slider; caso de borda); janela de milissegundos do último `increment` se o processo morre após `onDestroy` (inerente ao encerramento do processo).
- Não incluído por não estar no backlog: "Adicionar à playlist" na NowPlaying e skeleton extra.

## Fase 3 - Programador (backend)
Arquivos: `app/build.gradle.kts`/version catalog, `data/`, `service/`, `viewmodel/`.
Novas funcionalidades:
- [x] Gradle: dependência DataStore Preferences (version catalog)
- [x] `data/prefs`: `ThemeMode`, `SortOrder` e `SettingsRepository` (DataStore) com defaults DARK/TITLE
- [x] `Track.dateAddedSec` (`DATE_ADDED`) preenchido pelo `MediaStoreTrackRepository`
- [x] `LibraryUiState.sortOrder` e ordenação em `filteredTracks` (TITLE, ARTIST, ALBUM, RECENT, MOST_PLAYED; MOST_PLAYED usa os play counts)
- [x] `MusicViewModel`: `themeMode` + `onSetThemeMode` (persistido); `isDarkTheme`/`onToggleTheme` como `@Deprecated` até a UI migrar
- [x] `MusicViewModel`: `onSortOrderChange` (persistido)
- [x] `MusicViewModel`: `onShuffleAll()` (lista vazia -> `UiEvent.Message`)
- [x] `MusicViewModel`: `onResetRanking()` (apaga `play_counts`; `rankingState` esvazia)
- [x] `MusicViewModel`: `onCreatePlaylistAndAddTrack(name, trackId)`
- [x] Remover `isDarkTheme`/`onToggleTheme` após a migração da UI e aprovação do @revisor (removidos do `MusicViewModel`; nada em `ui/` nem `MainActivity` os usava)
Itens menores do backlog atribuídos ao programador:
- [x] `PlaybackService.onTaskRemoved`: remover `stopSelf` redundante
- [x] `onPlayPlaylist` ler a playlist do repositório com `first()` em vez de `playlistsState.value` (resolvido: links lidos direto do banco)
- [x] `uiEvents` sem perda de eventos sem coletor (`Flow` com `Channel` bufferizado) (resolvido: `Channel(BUFFERED)`)
- [x] Duplicidade de nome de playlist case-insensitive Unicode (`equals(ignoreCase = true)`) no lugar de `COLLATE NOCASE`
- [x] try/catch em create/rename/delete/removeTrack (e add) emitindo `UiEvent.Message` de erro (resolvido: sem engolir `CancellationException`)
- [x] Shuffle com `startIndex = 0` mostrando índice 0 por um instante: conferir e corrigir se confirmado (aprovado COM a correção do revisor; ver correção (a))
- [x] `toTrack` usar `mediaMetadata.durationMs` em vez de `0L`
- [x] `PlayCounter`: após seek para frente, exigir tempo ouvido >= limiar para contar (substitui/complementa `NEAR_END_MS`) (resolvido: após seek para frente, o fim natural exige o limiar completo)
- [ ] ADIADO: custo O(n) a cada evento do player no recálculo da fila (motivo na seção Decisões)
- [ ] ADIADO: anti-seek com duração desconhecida (motivo na seção Decisões)
- [ ] ADIADO: janela de ms em que o último `increment` pode se perder após `onDestroy` (motivo na seção Decisões)

## Fase 3 - Frontend (UI)
Arquivos: `ui/theme/`, `ui/components/`, `ui/screens/`, `MainActivity.kt`.
Novas funcionalidades:
- [x] Migrar o tema para `themeMode` (SYSTEM/LIGHT/DARK) no `ResonanceTheme`/`MainActivity` e remover o uso de `isDarkTheme`/`onToggleTheme`
- [x] `SettingsScreen`: seletor de tema, "Zerar ranking" com diálogo de confirmação, seção "Sobre" com versão (`PackageInfo`)
- [x] Ícone de engrenagem no cabeçalho da `MusicLibraryScreen` e navegação para Settings (voltar do sistema -> Biblioteca), com `MiniPlayer` e `WindowInsets`
- [x] Botão "Aleatório" (`onShuffleAll`) no cabeçalho da lista da Biblioteca
- [x] Chips de ordenação (A-Z, Artista, Álbum, Recentes, Mais tocadas) abaixo da busca, ligados a `sortOrder`/`onSortOrderChange`
Itens menores do backlog atribuídos ao frontend:
- [x] `AlertDialog` de playlist fecha com validação falha e perde o texto: pré-validar localmente e manter aberto com erro; `imePadding` efetivo no diálogo
- [x] "Nova playlist" no seletor deve adicionar a faixa (`onCreatePlaylistAndAddTrack`)
- [x] Snackbar atrás da NowPlaying/sheets: exibir acima de todas as camadas (ressalva: o Snackbar continua atrás dos sheets, que são outra janela; ver backlog)
- [x] Voltar do sistema em Playlists/Ranking deve ir à Biblioteca em vez de fechar o app (`BackHandler`)
- [x] Coral fixo no `TrackItem`: usar `colorScheme.primary` (tema claro)
- [x] Possível `contentPadding` de `navigationBars` duplicado no `ModalBottomSheet` (conferir e corrigir) (ressalva: margem de cima dos `ModalBottomSheet`, conferir em dispositivo)
- [x] Set de "misses" das capas permanente e sem limite: limitar tamanho/TTL para permitir nova tentativa
- [x] Reload da biblioteca ao girar a tela: `permissionChecked` com `rememberSaveable` (ou equivalente)

## Fase 3 - Revisor (@revisor)
- [x] Auditoria backend: DataStore (threads/defaults), ordenação e `MOST_PLAYED`, `onShuffleAll`, `onResetRanking`, `Flow` de eventos, try/catch, remoção de `isDarkTheme` + `./gradlew assembleDebug`
- [x] Auditoria UI: Settings, chips, botão Aleatório, diálogos, `BackHandler`, Snackbar, insets/tema claro e escuro + `./gradlew assembleDebug`
- [x] Aprovação do @revisor antes de marcar qualquer item acima como [x]

## Fase 3 - Integração em dispositivo (usuário)
- [x] Tema Sistema/Claro/Escuro persiste após fechar o app; "Zerar ranking" pede confirmação e esvazia o Top 10; versão correta em "Sobre"
- [x] Chips reordenam a lista e a ordem persiste; "Aleatório" respeita busca e ordenação
- [x] Diálogos de playlist (nome inválido mantém o texto), "Nova playlist" no seletor adiciona a faixa, Snackbar visível sobre NowPlaying/sheets, voltar do sistema leva à Biblioteca
- [x] Aprovação final da Fase 3 (2026-11-18; origem: validação MANUAL do usuário em celular físico, roteiro de 9 passos: tema/persistência, Aleatório tocando a fila inteira, chips, diálogos, Snackbar, voltar do sistema, rotação, zerar ranking e fila longa em tela cheia; o @revisor só fez análise estática e build)

---

## Fase 3 - Registro da auditoria e pendências

### Correções feitas pelo @revisor
- (a) IMPORTANTE: o índice inicial aleatório explícito em `onShuffleAll`/`onPlayPlaylist(shuffle)` fazia o ExoPlayer pular ~metade da fila (a ordem embaralhada é sorteada sem considerar o índice inicial; as faixas antes da escolhida só tocam com repeat ALL). Corrigido voltando a `setMediaItems(items, resetPosition = true)`. Custo: o controller pode mostrar o índice 0 por um instante (visual).
- (b) `MusicLibraryScreen`: o scroll voltava ao topo ao reentrar/girar; agora só volta quando a ordenação muda (`lastSortOrder` com `rememberSaveable`).

### Limpeza final do programador (build passou)
- Timeout da leitura inicial do tema de 1 s para 300 ms (fallback DARK/TITLE).
- `findByName` removido do `PlaylistDao`.
- Schema `app/schemas` `1.json` inalterado.

### Novos menores (backlog pós-Fase 3)
- [ ] Leitura bloqueante do tema no `init` (alternativa: splash com `setKeepOnScreenCondition`)
- [ ] No shuffle, tocar uma faixa específica deixa as anteriores na ordem embaralhada só com repeat ALL (padrão do ExoPlayer, desde a 2A)
- [ ] "Mais tocadas" reordena sozinha quando uma contagem muda (a lista pode pular)
- [x] Inset de CIMA zerado nos `ModalBottomSheet`: verificado em dispositivo (passo 9, fila longa em tela cheia), o título não ficou sob a status bar; sem necessidade de correção
- [ ] Snackbar embaixo dos sheets (são outra janela)
- [ ] Evento já recebido com `showSnackbar` em andamento se perde numa rotação

### Backup
- O DataStore (`files/datastore/settings.preferences_pb`) entra no backup/transferência: ACEITO por decisão (tema e ordenação não dependem do aparelho). `resonance.db` continua excluído.

### Roteiro de teste em dispositivo da Fase 3 (usuário)
1. Tema: em Settings, Claro/Escuro/Sistema; tirar dos recentes e reabrir persiste sem piscar; Sistema acompanha o modo escuro do Android; versão correta em "Sobre".
2. Aleatório: biblioteca inteira, abrir a Fila, shuffle aceso, pular até o fim e TODAS devem tocar; repetir com busca ativa (só as filtradas) e no detalhe de uma playlist.
3. Chips A-Z/Artista/Álbum/Recentes/Mais tocadas: a lista reordena e volta ao topo; a rolagem se mantém ao abrir Settings/girar; a ordenação persiste ao reabrir.
4. Diálogos: nomes inválidos (vazio, duplicado com outra caixa ou acento "Música"/"MÚSICA") mantêm o diálogo aberto com texto e erro; "Nova playlist" no seletor cria já com a faixa e mostra o Snackbar.
5. Snackbar acima da NowPlaying.
6. Voltar do sistema: Playlists/Ranking -> Biblioteca, detalhe -> lista, Settings -> Biblioteca, NowPlaying fecha, Biblioteca sai.
7. Rotação com detalhe/Settings/diálogo com texto: estado mantido e sem recarregar a biblioteca.
8. Zerar ranking: confirmação, Cancelar não apaga, confirmar esvazia o Top 10 com "Ranking zerado".
9. Fila longa em tela cheia: o título do sheet não fica sob a status bar.

---

# FASE 4 - Equalizador DSP
**FASE 4 CONCLUÍDA — 2026-11-18** (auditada pelo @revisor: estático + `assembleDebug`; validada e aprovada em dispositivo físico pelo usuário). Autorizada pelo usuário em 2026-11-18. Contratos: `CONTRACTS.md` seção 10.

## Decisões (Fase 4)
- 2026-11-18: Fase 4 (Equalizador DSP) autorizada pelo usuário; a proibição anterior do Equalizador está encerrada.
- Engine: `android.media.audiofx.Equalizer` no `PlaybackService`, ligado ao `audioSessionId` real do ExoPlayer; `isAvailable = false` (sem crash) em aparelhos que restrinjam AudioEffect.
- Persistência em DataStore (`eqEnabled`, `eqPresetIndex`, `eqBandLevelsMb`); preset não apaga os níveis custom.

## Fase 4 - Programador
Arquivos: `service/`, `data/prefs/`, `viewmodel/`.
- [x] `service/EqualizerManager`: `Equalizer` ligado ao `audioSessionId` via `onAudioSessionIdChanged` (ignora `AUDIO_SESSION_ID_UNSET`), recria ao mudar o id
- [x] try/catch em toda criação/uso do efeito; `isAvailable = false` sem crash; `release()` no `onDestroy`
- [x] `EqualizerManager` singleton de processo exposto por `StateFlow` (sem vazar `AudioEffect` para o ViewModel)
- [x] `SettingsRepository`: `eqEnabled`, `eqPresetIndex`, `eqBandLevelsMb` com defaults (false, -1, zeros) e reaplicação ao (re)criar o efeito
- [x] `viewmodel/EqualizerUiState.kt`: `EqBandUi` e `EqualizerUiState` conforme contrato
- [x] `MusicViewModel`: `equalizerState`, `onSetEqualizerEnabled`, `onSelectEqPreset`, `onSetEqBandLevel` (clamp), `onResetEqualizer`
- [x] Regra: preset não apaga níveis custom; slider após preset parte dos níveis do preset e vira Personalizado

## Fase 4 - Frontend
Arquivos: `ui/theme/`, `ui/components/`, `ui/screens/`, `MainActivity.kt`.
- [x] `EqualizerSheet` (`ModalBottomSheet`): Switch, presets (incluindo "Personalizado"), sliders verticais por banda, botão "Resetar"
- [x] Rótulos de frequência (Hz < 1000, senão kHz, ex. "60 Hz", "3,6 kHz") e de ganho (ex. "+3 dB")
- [x] Sliders desabilitados visualmente com `enabled = false`; estado "Equalizador indisponível" com `isAvailable = false`
- [x] Botão de abertura na `NowPlayingScreen` e item em `SettingsScreen`
- [x] `WindowInsets` e temas claro/escuro respeitados; sem import de `android.media.audiofx` nem `service/`

## Fase 4 - Revisor (@revisor)
- [x] Auditoria backend: ciclo de vida do `Equalizer` (recriação, `release`), vazamentos, try/catch, DataStore/defaults + `./gradlew assembleDebug`
- [x] Auditoria UI: sheet, sliders verticais, insets/temas, fronteira (sem `audiofx`/`service/` na UI) + `./gradlew assembleDebug`
- [x] Aprovação do @revisor antes de marcar qualquer item acima como [x]

## Fase 4 - Integração em dispositivo (usuário)
- [x] Equalizador audível no celular físico (ligar/desligar, presets, bandas)
- [x] Persistência após fechar o app e reabrir
- [x] Aprovação final da Fase 4 (validação MANUAL do usuário em celular físico)

### Registro da auditoria (Fase 4)
- Correção do @revisor: chip "Personalizado" agora chama `onSelectEqPreset(-1)` e `EqualizerManager.selectPreset(-1)` reaplica os níveis custom salvos.
- Menores (backlog): debounce de 300 ms perdido se o ViewModel for limpo logo após o último ajuste; efeito nasce com defaults se o serviço subir sem o app aberto; `applyLevels`/`publish` regravam/relêem todas as bandas a cada arrasto; atenuação dupla com EQ desligado; `stateDescription` do slider em dB; `EqualizerController` sem `remember` no MainActivity; `service/` importa modelos de `viewmodel/`.
- Recomendação: estado "aguardando sessão" vs "falhou" para mostrar "Inicie uma música" em vez de "indisponível" (exige mudança de contrato).

### Roteiro de teste no celular (Fase 4, usuário)
1. Tocar uma faixa, abrir o Equalizador pela NowPlaying e por Settings: o mesmo estado nos dois.
2. Ligar o Switch e escolher presets (ex. Rock, Bass): o som muda e os sliders refletem o preset; desligar volta ao som original.
3. Mover um slider após um preset: vira "Personalizado" e o som acompanha.
4. Trocar para um preset e voltar a "Personalizado": os níveis custom salvos permanecem.
5. "Resetar": todas as bandas a 0 dB, preset Personalizado.
6. Fechar o app (tirar dos recentes), reabrir e tocar: ligado/desligado, preset e níveis persistem e são reaplicados.
7. Pular faixas, pausar/retomar e trocar fones/Bluetooth: o efeito continua aplicado, sem crash.
8. Tema claro e escuro, rotação e gesture nav: o sheet respeita insets; sliders desabilitados quando desligado.

---

# Pós-Fase 4 - Polimento visual fiel ao Stitch (2026-11-18)
Status: implementado pelo `frontend`, `./gradlew assembleDebug` OK. Validação visual em celular físico PENDENTE (usuário). Nenhum contrato, ViewModel, Room ou Media3 alterado; só `ui/theme`, `ui/components`, `ui/screens`, `MainActivity.kt`. Sem auditoria do @revisor nesta rodada.

- [x] Tokens: `tertiaryContainer`, `surfaceDim/Bright`, cores de ranking (ouro/prata/bronze), `BrandGradient` (coral -> #FF3366 -> âmbar), `Shapes` (8/12/16/20/24dp), escala tipográfica do Stitch (pesos, line-height, letterSpacing, algarismos tabulares)
- [x] Biblioteca: chips de ordenação em pílula (ativo com gradiente coral), cartão da faixa por tema, faixa atual com borda/gradiente lateral/ponto pulsante
- [x] Playlists/Detalhe: linha com raio 16, capa 12dp, cabeçalho headline-sm
- [x] Ranking: selo "TOP 10", #1 como cartão de pódio, #2/#3 com faixa e borda coloridas, contagem à direita
- [x] MiniPlayer: `surfaceContainer`, play com gradiente, progresso coral
- [x] NowPlaying: capa 1:1 raio 24 com borda/brilho coral, novo `ResonanceSlider` (`components/Sliders.kt`), play 80dp, cartão inferior com Equalizador/Fila; sem slider de volume
- [x] Settings: seções em cartões, títulos coral em caixa alta, "Zerar ranking" com borda vermelha
- [x] EqualizerSheet: `ResonanceChip`, switch coral, sliders verticais com `ResonanceSlider`, dB em ciano
- [x] Barra de navegação: `surfaceContainer`, divisor sutil, indicador coral
- Limitação: fontes Jost/Instrument Sans NÃO embarcadas (mantido `FontFamily.Default`, escala reproduzida)
- [ ] Validação visual do usuário em celular físico (claro, escuro e Sistema)

---

# Pós-Fase 4 - Layout de referência (Music Library) (2026-11-18)
Status: implementado, `./gradlew assembleDebug` OK. Validação em celular físico PENDENTE (usuário). Sem auditoria do @revisor nesta rodada.

- [x] Horas escutadas: `PlayCounter` acumula tempo ouvido (só com `isPlaying`) e persiste em lotes (10 s, pausa, troca de faixa, `flush`, `release`) em `SettingsRepository.totalListenedMs` (DataStore); `MusicViewModel.totalListenedMs`; exibido em Settings > "Estatísticas" (`formatListenTime`)
- [x] Barra inferior com 4 abas (Biblioteca, Playlists, Ranking, Settings); engrenagem do topo removida; Voltar em qualquer aba != Biblioteca leva à Biblioteca; MiniPlayer acima da barra
- [x] "Shuffle All" flutuante (pílula com `BrandGradient`, canto inferior direito, acima do MiniPlayer); botão "Aleatório" do cabeçalho removido; `bottom` da lista +88 dp
- [x] Faixa atual: borda em gradiente coral->âmbar, sombra coral e ponto pulsante ao lado do título
- Limitação: o total ouvido não retroage (começa a contar a partir desta versão); até ~10 s de escuta podem se perder se o processo morrer.
- [ ] Validação em celular físico: ouvir ~1 min, abrir Settings (total sobe), fechar o app pelo voltar tocando e reabrir (continua somando); Shuffle All sobre o MiniPlayer sem cobrir a última faixa; Voltar nas 4 abas.

---

# Pós-Fase 4 - MiniPlayer, Favoritos, Fundo animado e Feedback tátil (2026-11-18)
Status: implementado, `./gradlew assembleDebug` OK. Validação em celular físico PENDENTE (usuário). Sem auditoria do @revisor nesta rodada.

- [x] MiniPlayer: barra de progresso própria (3 dp, gradiente da marca, sem pontas/indicador do M3 que ficavam cortados), logo abaixo da borda superior do card; divisor removido
- [x] Favoritos: `SettingsRepository.favoriteIds`/`toggleFavorite` (DataStore, chave `favorite_track_ids`, sem migração do Room), `MusicViewModel.favoriteIds` e `onToggleFavorite`; coração (cheio coral / contorno) na `NowPlayingScreen`, no `MiniPlayer` e em todo `TrackItem` (via `LocalFavorites`)
- [x] Fundo animado (`ui/components/AnimatedBackground`): 2 brilhos radiais com deriva de 24-32 s via `graphicsLayer` (sem recomposição por frame); estático com tela fora de primeiro plano (`ON_PAUSE`) ou economia de bateria
- [x] Feedback tátil: `HapticIconButton` (escala ao pressionar + `HapticFeedbackType.TextHandleMove`) no MiniPlayer, NowPlaying, menu da faixa e favorito; FAB Shuffle All com escala e vibração
- Limitações: favoritos são só marcação (sem tela/filtro "Favoritos"); favoritos entram no backup junto do DataStore; ids de faixas removidas ficam no conjunto; a imagem do bug do MiniPlayer não foi visível para mim (corrigi a causa provável: barra M3 de 2 dp cortada).
- [ ] Validação em celular físico (ver resumo)

---

# Pós-Fase 4 - Capa da notificação e filtro "Favoritas" (2026-11-18)
Status: implementado, `./gradlew assembleDebug` OK. Validação em celular físico PENDENTE (usuário). Sem auditoria do @revisor.

- [x] Capa da notificação/tela de bloqueio: causa provável = a notificação usava a URI legada `content://media/external/audio/albumart/{albumId}` (pouco confiável no API 29+, pode devolver capa de outro álbum/defasada). Correção: `service/ArtworkBitmapLoader` (sem cache, `loadThumbnail` na API 29+) ligado ao `MediaSession` via `setBitmapLoader`; `MusicViewModel.toMediaItem` passa a usar a `contentUri` da faixa como `artworkUri` no API 29+ (se a faixa tem capa). Causa NÃO reproduzida em dispositivo.
- [x] Chip "♥ Favoritas" no início da linha de chips da Biblioteca: `LibraryUiState.favoritesOnly`, `MusicViewModel.onToggleFavoritesFilter()`; combina com busca e ordenação; estado vazio próprio; em memória (não persiste)
- [x] Validação em celular físico: correção da capa na notificação e filtro "Favoritas" validados e APROVADOS pelo usuário em dispositivo físico (2026-11-18)

---

# Pós-Fase 4 - Otimização e preparação do build release (2026-11-18)
Status: implementado; `./gradlew assembleDebug` e `./gradlew assembleRelease` OK. Validação do release em celular físico PENDENTE (usuário). Sem auditoria do @revisor nesta rodada.

- [x] R8 + shrink de recursos no `release` (`isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt` + `app/proguard-rules.pro` novo). APK release ~2,7 MB contra ~17,6 MB do debug
- [x] `proguard-rules.pro`: atributos para stack trace (`SourceFile`/`LineNumberTable`), Room (`RoomDatabase`/`@Entity`/`@Dao`), `-dontwarn` de Media3/Guava/anotações, remoção de `Log.v/d`; sem regras amplas de Compose
- [x] Verificado no `mapping.txt`: `ResonanceDatabase_Impl`, `PlaylistDao_Impl` e `PlaybackService` mantidos
- [x] Performance Compose: `positionMs` (muda a cada 500 ms) deixou de recompor o app inteiro. `MainActivity` coleta o `playerState` sem a posição (`distinctUntilChanged`) e passa `positionMs: () -> Long` ao `MiniPlayer` (barra desenhada em `drawBehind`, zero recomposição por tick) e ao `SeekBar` da NowPlaying
- [x] `EqualizerController` memoizado com `remember(eqState)` na `MainActivity`
- [x] `MusicViewModel`: o loop de posição só atualiza o estado se há coletores (`subscriptionCount > 0`), poupando CPU com o app em segundo plano
- [x] Capas: decodificação em `Dispatchers.IO.limitedParallelism(3)` (rolagem rápida não dispara dezenas de decodificações); `loadThumbnail`/`inSampleSize` já limitam o bitmap a 128/512 px; cache LRU e dedupe por chave mantidos
- [x] `PlaybackService`: ciclo de vida auditado (sem mudança necessária): `onDestroy` libera EqualizerManager, PlayCounter, MediaSession, ExoPlayer e o executor do `ArtworkBitmapLoader`; `MusicViewModel.onCleared` libera o `MediaController`
- [x] Baseline Profile: dependência `androidx.profileinstaller` + `app/src/main/baseline-prof.txt` (regras manuais por pacote). O AGP empacota `assets/dexopt/baseline.prof` no release (confirmado no APK)
- Limitações: (1) o perfil é MANUAL (sem dispositivo para medir); para um perfil medido, criar módulo `baselineprofile` (plugin `androidx.baselineprofile` + Macrobenchmark) e gerar em aparelho/emulador. (2) o `release` usa a assinatura de DEBUG só para validação local: trocar por keystore própria antes de publicar. (3) `TrackItem` ainda recebe `menu = listOf(...)` novo por item (só recompõe quando a lista/faixa atual muda; custo baixo).
- [x] Validação do release em celular físico: concluída pelo usuário SEM regressões (2026-11-18)

## Assinatura de release (2026-11-18)
- [x] `signingConfigs.release` em `app/build.gradle.kts` lê `storeFile`, `storePassword`, `keyAlias`, `keyPassword` de `keystore.properties` (raiz). Se o arquivo ou a keystore não existir, o build NÃO falha: cai no keystore de debug (só teste local, não publicar)
- [x] `.gitignore`: `keystore.properties`, `*.jks`, `*.keystore`
- [x] `./gradlew assembleRelease` OK; APK assinado com a keystore própria (verificado com `apksigner`): `app/build/outputs/apk/release/app-release.apk`
- Cuidado: faça backup da `.jks` e das senhas fora do projeto; perdê-la impede atualizar o app já publicado. O APK agora tem assinatura diferente da debug: instalar sobre um debug exige desinstalar antes.
