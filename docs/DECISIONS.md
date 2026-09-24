# SunPlayer — Architectural Decisions Log (ADR)

## ADR 001: Playback Engine Selection — Android Media3 (ExoPlayer)
- **Status**: Accepted
- **Context**: Need a robust, reliable audio playback engine supporting MP3, AAC, FLAC, OGG, WAV, and OPUS with gapless playback, lock-screen notifications, and MediaSession integration.
- **Decision**: Use `androidx.media3:media3-exoplayer` and `androidx.media3:media3-session`.
- **Consequences**: Standardizes Android media sessions across all Android API versions (26–34+), handles audio focus changes, and simplifies future notification integration.

## ADR 002: UI Architecture — Jetpack Compose with Material 3
- **Status**: Accepted
- **Context**: Fast, modern, declarative Android UI development with smooth layout transitions and minimal boilerplate.
- **Decision**: Adopt Jetpack Compose BOM (`2024.06.00`).
- **Consequences**: Modern UI paradigm with single source of truth states. To prevent large APK overhead, R8 minification/shrinking will be activated on release builds.

## ADR 003: Storage & Local File Access Strategy
- **Status**: Accepted
- **Context**: Must discover user's audio files reliably across both older Android versions (API 26–32) and Android 13+ (API 33+), as well as Huawei devices with strict privacy controls.
- **Decision**: Implement a MediaStore content-resolver scanner as the primary source of truth, backed by a persistent index cache for instant startup.
- **Consequences**: Avoids broad intrusive file permissions; uses standard `READ_MEDIA_AUDIO` on API 33+ and fallback `READ_EXTERNAL_STORAGE` on API 26–32.

## ADR 004: Media3 Owns Runtime Queue State
- **Status**: Accepted for future queue work
- **Context**: `AudioPlayerManager` currently mirrors queue data in application memory while Media3 maintains its own media-item queue.
- **Decision**: Preserve the current facade for this stabilization pass, but treat Media3's queue as the future runtime authority. Later queue refactoring should observe/project Media3 state instead of adding more parallel mutable state.
- **Consequences**: No queue rewrite is introduced now. External MediaSession commands and Android Auto integration remain risks until that consolidation is completed.

## ADR 005: Failed Scans Never Replace Library State
- **Status**: Accepted
- **Context**: A failed MediaStore query can produce no tracks, which must not be interpreted as an empty library.
- **Decision**: `AudioScanner` returns explicit `ScanResult.Success` or `ScanResult.Failure`; only successful results reach Room replacement.
- **Consequences**: A legitimate successful empty scan can clear stale rows, while query/permission/runtime failures preserve the last known library.
