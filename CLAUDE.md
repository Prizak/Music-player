## Orquestração de Agentes (Sessão Atual)
O coordenador da sessão deve usar a tool `Agent` com `name` distinto para cada papel trabalhar em paralelo sem conflito de arquivos:

1. **`Agent(name="documentador")`**:
   - **Arquivos:** `CONTRACTS.md` e `TASKS.md`.
   - **Papel:** Definir contratos de dados (`Track`, `LibraryUiState`, `PlayerUiState`) e atualizar o checklist da Fase 1.

2. **`Agent(name="programador")`**:
   - **Arquivos:** `app/build.gradle.kts`, `AndroidManifest.xml`, `data/`, `service/`, `viewmodel/`.
   - **Proibido:** Ler `.html` em `stitch_ref/` ou editar `ui/`.
   - **Escopo Fase 1:** Media3 (`ExoPlayer` + `MediaSessionService`), permissões (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`), leitura de áudio local via `MediaStore` e `MusicViewModel`.

3. **`Agent(name="frontend")`**:
   - **Arquivos:** `ui/theme/`, `ui/components/`, `ui/screens/`, `MainActivity.kt`.
   - **Contexto:** Ler `stitch_ref/resonance_material_3_audio.md` e APENAS `music_library/code.html`, `now_playing/code.html` e `ui_kit_system_states/code.html`[cite: 1, 8].
   - **Escopo Fase 1:** Tema Resonance (Dark OLED `#121316`, Coral `#FF4D6D`, Amber `#FFAA00`, Cyan `#00E5FF` + Light), `MusicLibraryScreen`, `MiniPlayer` (respeitando `WindowInsets`) e `NowPlayingScreen` (sem slider de volume embutido)[cite: 2, 7].

4. **`Agent(name="revisor", model="opus")`**:
   - **Papel:** Invocado ao final da Fase 1 (sem escrever features novas) para rodar `./gradlew assembleDebug` e auditar vazamentos no `ExoPlayer`, permissões (API 24–34+) e `WindowInsets`/`LazyColumn` no Compose.
