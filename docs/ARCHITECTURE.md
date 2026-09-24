# SunPlayer — Architecture Specification

## 1. Architectural Style
SunPlayer follows an offline-first, clean, unidirectional data-flow architecture layered for clarity, maintainability, and testability.

```
UI (Jetpack Compose)
  ↓ [Events / Intents]
ViewModel / Presentation (Kotlin Coroutines & StateFlow)
  ↓ [Calls]
Domain Use Cases & Controllers (AudioPlaybackController, ScannerUseCase)
  ↓ [Calls]
Repositories (TrackRepository, PlaylistRepository)
  ↓
Data Sources:
  - Local Storage / Android MediaStore
  - Room SQLite Database (Indexing, Metadata, Playlists)
  - Android Media3 (ExoPlayer & MediaSessionService)
```

## 2. Layer Definitions & Boundaries
- **UI Layer (`com.rebecca.sunplayer.ui`)**:
  - Pure declarative Jetpack Compose UI.
  - No database manipulation or direct player manipulation inside Composables.
  - Recomposition-optimized using immutable UI state objects.
- **Playback Layer (`com.rebecca.sunplayer.playback`)**:
  - Decoupled from UI components.
  - Implements Android `Media3` for seamless background playback, media notification, lock-screen controls, and headset unhook/Bluetooth events.
- **Data & Storage Layer (`com.rebecca.sunplayer.data`)**:
  - `AudioScanner`: MediaStore scanner returning explicit success/failure results; incremental scanning is a later phase.
  - Room Database (Phase 1): High-performance cached index for instant querying, filtering, and sorting.
- **Domain Models (`com.rebecca.sunplayer.model`)**:
  - Plain Kotlin data classes representing Tracks, Albums, Artists, and Playlists.

## 3. Dependency Policy
- Keep external libraries strictly minimal.
- Never place machine learning or heavy dependencies in real-time playback or basic browsing paths.
- All dependencies must be justified by core player requirements.
