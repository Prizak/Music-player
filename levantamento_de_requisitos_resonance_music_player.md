# Levantamento de Requisitos — Resonance: Native Android Music Player

- **Projeto:** `Music Player` (`com.example.musicplayer`)
- **Plataforma:** Android Nativo (Android Studio)
- **Min SDK:** API 24 (Android 7.0+) | **Target SDK:** API 34+
- **Stack Tecnológica:** Kotlin, Jetpack Compose (Material 3), AndroidX Media3 (`ExoPlayer` + `MediaSessionService`), `MediaStore`, `Room Database`, `AudioEffect` (`Equalizer`, `BassBoost`, `Virtualizer`).
- **Arquitetura:** MVVM (`ui/`, `viewmodel/`, `data/`, `service/`).

---

## 1. Visão Geral e Escopo do Produto

O **Resonance** é um reprodutor de áudio local nativo para Android com estética Dark Neon de alto contraste (OLED Obsidian `#121316` com acentos Coral `#FF4D6D`, Amber `#FFAA00` e Cyan `#00E5FF`) e suporte completo a Light Mode (`#FFFFFF`). O aplicativo permite descobrir e tocar arquivos de áudio locais (`.mp3`, `.flac`, etc.), organizar playlists, acompanhar estatísticas de reprodução no dispositivo e ajustar parâmetros acústicos via DSP nativo.

---

## 2. Requisitos Funcionais (RF)

### RF-01 — Biblioteca Local de Músicas (`MusicLibraryScreen`)
- **RF-01.1:** Escanear arquivos de áudio locais armazenados no dispositivo por meio do `MediaStore` (`.mp3`, `.flac` e formatos suportados pelo `ExoPlayer`).
- **RF-01.2:** Exibir cabeçalho com contagem total de faixas encontradas, espaço total ocupado e opção de ordenação (ex.: por título, artista ou data de adição).
- **RF-01.3:** Fornecer barra de pesquisa (`SearchBar` Material 3) com filtragem em tempo real por título, artista ou álbum.
- **RF-01.4:** Disponibilizar filtros rápidos em carrossel horizontal (`All .MP3`, `Recently Added`, `Artists`, `Albums`).
- **RF-01.5:** Exibir botão flutuante **Shuffle All** que embaralha toda a biblioteca local e inicia a reprodução imediatamente.

### RF-02 — Permissões e Estados de Sistema (`UiKitStates`)
- **RF-02.1:** Solicitar permissão de leitura de áudio em runtime respeitando a versão do Android:
  - Android 13+ (API 33+): `READ_MEDIA_AUDIO`.
  - Android 7.0 a 12 (API 24–32): `READ_EXTERNAL_STORAGE`.
- **RF-02.2:** Exibir estado de carregamento com **Skeleton Loading** durante a leitura inicial do `MediaStore` para evitar saltos visuais (*layout shift*).
- **RF-02.3:** Exibir **Empty / Permission State** claro quando a permissão for negada ou nenhuma faixa local for encontrada, oferecendo botão direto para conceder permissão ou reescanear diretórios.
- **RF-02.4:** Disponibilizar **Modal Bottom Sheet** ao tocar no menu de opções (`more_vert`) de uma faixa, contendo:
  - *Add to Playback Queue*
  - *Add to Playlist*
  - *Set Audio Lab DSP Preset*
  - *Share Audio File*
  - *Inspect Audio Details* (codec, bitrate, sample rate, caminho/tamanho)

### RF-03 — Reprodução em Segundo Plano e Fila (`NowPlayingScreen` & `MiniPlayer`)
- **RF-03.1:** Executar a reprodução de áudio em segundo plano por meio de `MediaSessionService` (`Media3`) com notificação persistente e integração aos controles de mídia do Android.
- **RF-03.2:** Exibir o **Mini Player** flutuante acima da barra de navegação nas telas principais, mostrando capa, título, artista e controles rápidos (*Previous*, *Play/Pause*, *Next*).
- **RF-03.3:** Ao tocar no Mini Player, abrir a tela dedicada **Now Playing** com:
  - Capa do álbum em destaque com badges informativos de formato/qualidade (ex.: `FLAC` / `MP3`) e atalho para `DSP`.
  - Metadados da faixa (título, artista, álbum) e botão de favoritar.
  - Visualizador gráfico animado sincronizado com o estado de reprodução.
  - Barra de progresso (*Scrubber*) interativa com tempo decorrido e tempo restante.
  - Controles de transporte: *Shuffle*, *Previous*, *Hero Play/Pause*, *Next* e *Repeat* (*Off*, *All*, *One*).
  - Acesso compacto a **Lyrics** (letra local/embutida) e **Queue** (fila atual de reprodução).
- **RF-03.4 (Adaptação Android):** Não incluir slider de volume embutido na tela `Now Playing`; o controle de volume utiliza os botões físicos e o painel de volume do sistema Android.

### RF-04 — Playlists e Favoritos (`PlaylistsScreen` + `Room`)
- **RF-04.1:** Persistir playlists personalizadas e relação faixa-playlist no banco de dados local `Room`.
- **RF-04.2:** Exibir coleções em grade de 2 colunas com capa, título, quantidade de faixas e duração total.
- **RF-04.3:** Suportar filtros rápidos (`All`, `Smart Mixes`, `Favorites`), criação de nova playlist, renomeação, adição/remoção de faixas e exclusão de playlist.

### RF-05 — Telemetria Local e Rankings (`RankingsScreen` + `Room`)
- **RF-05.1:** Registrar histórico local de reproduções no `Room` contendo `trackId` e `timestamp`.
- **RF-05.2:** **Regra de contabilização:** Incrementar `+1` no contador de reproduções apenas quando a faixa atingir **30 segundos de reprodução ou 50% da duração total** (o que ocorrer primeiro), evitando contagem falsa ao pular faixas rapidamente.
- **RF-05.3:** Exibir o **Top 10 Most Played** com destaque visual de pódio para o #1 e filtro temporal (*All-Time*, *This Month*, *This Week*).

### RF-06 — Equalizador e Audio Lab (`EqualizerScreen` + `AudioEffect`)
- **RF-06.1:** Conectar instâncias de `Equalizer`, `BassBoost` e `Virtualizer` (`android.media.audiofx`) ao `audioSessionId` ativo do `ExoPlayer`.
- **RF-06.2:** Disponibilizar *Master Switch* para ativar/desativar o processamento DSP e presets rápidos (`Electronic`, `Rock`, `Jazz`, `Classical`, `Custom`).
- **RF-06.3 (Fallback de Hardware):** Consultar `equalizer.numberOfBands` e `equalizer.bandLevelRange` em tempo de execução. Caso o hardware exponha menos de 10 bandas (ex.: 5 bandas), mapear/interpolar os valores dos 10 sliders da interface para as bandas reais do dispositivo sem gerar exceções.
- **RF-06.4 (Modo Experimental):** Exibir **Bit-Perfect Direct USB** claramente identificado como recurso experimental (`BETA`), desabilitando o pipeline DSP quando ativado em hardware compatível e mantendo fallback seguro para reprodução padrão quando não houver DAC USB suportado.

### RF-07 — Configurações e Temas (`SettingsScreen`)
- **RF-07.1:** Alternar entre **Dark Theme** e **Light Theme** com persistência da preferência do usuário (`DataStore` / `SharedPreferences`).
- **RF-07.2:** Configurar **Gapless Playback & Crossfade** no `ExoPlayer`.
- **RF-07.3:** Exibir resumo de armazenamento local utilizado pelas faixas e permitir acionar manualmente uma nova varredura da biblioteca (**Scan**) ou limpar o cache de miniaturas/capas (**Clear Album Art Cache**).

---

## 3. Requisitos Não Funcionais (RNF)

- **RNF-01 — Ciclo de Vida e Gerenciamento de Recursos:** Liberar obrigatoriamente `ExoPlayer`, `MediaSession`, `Equalizer`, `BassBoost` e `Virtualizer` no `onDestroy()` do serviço para evitar *memory leaks* e travamento de sessão de áudio do sistema.
- **RNF-02 — Desempenho de Interface e Concorrência:** Executar consultas ao `MediaStore`, leitura de metadados/capas e operações no `Room` fora da *Main Thread* (`Dispatchers.IO`). Utilizar `LazyColumn` e `LazyVerticalGrid` com `key` estável no Jetpack Compose.
- **RNF-03 — Ergonomia, Áreas Seguras e Contraste:**
  - Respeitar `WindowInsets` (`statusBars`, `navigationBars`, `ime`) em layout *Edge-to-Edge*.
  - Garantir espaçamento vertical adequado entre a `BottomNavigationBar`, o `MiniPlayer` flutuante e o FAB `Shuffle All` para evitar sobreposição ou toques acidentais.
  - Manter alvos de toque mínimos de $48 \times 48\text{dp}$ e contraste mínimo de $4.5:1$ (WCAG AA) em ambos os temas (Dark e Light).
- **RNF-04 — Arquitetura Modular para Desenvolvimento com Agentes:** Isolar responsabilidades entre pacotes (`ui/`, `viewmodel/`, `data/`, `service/`) e manter contratos explícitos entre UI e backend.

---

## 4. Decisões Técnicas Consolidadas

| Tópico | Decisão Adotada |
| :--- | :--- |
| **Critério de Play no Ranking** | Contabiliza após 30s ou 50% da faixa ouvida; salva timestamp para suportar filtros por período. |
| **10 Bandas vs. Hardware Android** | UI mantém 10 bandas; camada DSP interpola para `numberOfBands` suportado pelo chip do aparelho. |
| **Bit-Perfect Direct USB** | Tratado como toggle experimental (`BETA`) com fallback seguro para áudio padrão do Android. |
| **Capas de Álbum** | Extração local via `MediaStore` / tags embutidas com cache em disco gerenciável em `Settings`. |
| **Fila de Reprodução (Queue)** | MVP suporta visualizar fila, pular para faixa, adicionar e remover itens; *drag-and-drop* fica como melhoria futura. |

---

## 5. Backlog de Implementação (Agent Team)

- [ ] **T01 (`documenter`):** Criar `CONTRACTS.md` com os modelos (`Track`, `Playlist`, `RankItem`, `EqualizerState`, `PlayerUiState`, `LibraryUiState`).
- [ ] **T02 (`programmer`):** Configurar dependências no `app/build.gradle.kts` (Media3, Room, DataStore, Icons Extended) e permissões no `AndroidManifest.xml`.
- [ ] **T03 (`frontend`):** Implementar `Theme.kt` (Dark Neon + Light Mode), tipografia e estrutura de navegação inferior com `MiniPlayer` e `WindowInsets`.
- [ ] **T04 (`programmer`):** Implementar repositório `MediaStore` e fluxo de permissão (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`).
- [ ] **T05 (`frontend`):** Construir `MusicLibraryScreen` (Dark e Light) e componentes de `UiKitStates` (`SkeletonLoading`, `EmptyState`, `TrackActionBottomSheet`).
- [ ] **T06 (`programmer`):** Implementar `PlaybackService` (`MediaSessionService` + `ExoPlayer`) e `PlayerViewModel`.
- [ ] **T07 (`frontend`):** Construir `NowPlayingScreen` (sem slider de volume embutido, com waveform, scrubber, transporte e sheets de Lyrics/Queue).
- [ ] **T08 (`programmer`):** Implementar `Room Database` para Playlists, Favoritos e Telemetria de Rankings (regra 30s / 50%).
- [ ] **T09 (`frontend`):** Construir `PlaylistsScreen` e `RankingsScreen`.
- [ ] **T10 (`programmer` + `frontend`):** Implementar `AudioEffectManager` (`Equalizer` com interpolação de bandas, `BassBoost`, `Virtualizer`), `EqualizerScreen` e `SettingsScreen`.
- [ ] **T11 (`@revisor` Opus):** Auditar ciclo de vida, permissões, performance do Compose e validar build final com `./gradlew assembleDebug`.