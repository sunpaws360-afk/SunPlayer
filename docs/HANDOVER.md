# SunPlayer Handover

## Final intent

SunPlayer is intended to become a lightweight, offline-first Android music
player that owns a durable local library while remaining small, predictable,
and respectful of device resources.

The product should prioritize reliable local playback and library workflows
before optional features such as DSP, downloads, network providers, or AI.

## Current goals

1. Keep MediaStore scanning and Room indexing reliable for local audio.
2. Keep playback alive outside the Activity through Media3.
3. Make queue and favorite state useful across app restarts and rescans.
4. Keep CI reproducible with Java 17 and focused tests.
5. Add larger features incrementally without destabilizing playback.

## Done

- Android Kotlin/Compose project scaffold and Gradle wrapper 8.9.
- MediaStore audio scanning with permission handling.
- Room-backed track index with atomic scan replacement.
- Room migration from schema version 1 to version 2.
- Favorite state stored in Room and preserved across rescans.
- Search and sorting for title, artist, album, and duration.
- Media3 ExoPlayer moved into `PlaybackService`.
- MediaSession controller facade for background playback controls.
- Shuffle, repeat, seek, previous, next, and play/pause controls.
- Queue add-next, add-to-end, remove, reorder, and clear-upcoming actions.
- Queue bottom sheet and favorite controls in the library UI.
- Unit and Room instrumentation test coverage for core persistence behavior.
- CI configured for Java 17, unit tests, debug builds, and release builds.
- Codespaces build instructions in `docs/BUILDING.md`.

## Verification status

Static diagnostics and whitespace checks pass. Local Gradle compilation is
blocked by the container's OpenJDK 25 runtime; Kotlin 1.9.22 rejects that Java
version string before source compilation. CI is configured to use Temurin 17,
which is the required verification environment.

## Remaining work

### Immediate

- Run CI on this branch and confirm Kotlin compilation, tests, and APK output.
- Install the debug APK on a device and verify background playback, lock-screen
  controls, headset unplug behavior, queue operations, and favorites.
- Replace debug release signing with a protected release keystore in CI.

### Next product slices

- Room playlists with create, rename, delete, add, and remove track actions.
- Library tabs for albums, artists, folders, playlists, and favorites.
- Persistent queue and playback history.
- Incremental/resumable scanner with cancellation and modification tracking.
- Artwork extraction and metadata editing.
- Accessibility, localization, backup/restore, and large-library testing.
- Android Auto and widget integration after service behavior is proven.

## Non-goals for the current milestone

Do not begin AI recommendations, downloads, recording, network providers, or
advanced DSP until playback, library persistence, queue behavior, signing, and
test coverage are stable on a real device.
