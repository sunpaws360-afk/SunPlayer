# SunPlayer — Product Specification

## 1. Vision & Core Philosophy
**"A lightweight personal music library with professional playback capabilities."**
SunPlayer is a fast, lightweight, offline-first, private, and modern Android local audio player combining the best concepts of classic & audiophile tools (Winamp, AIMP, Musicolet, foobar2000, Poweramp, GoneMAD) without bloat, subscriptions, accounts, or mandatory cloud requirements.

## 2. Target Users & Environment
- **Platform**: Android 8.0+ (API 26+) up to Android 14+ (API 34+).
- **Target Hardware**: Standard Android smartphones, tablets, and Huawei EMUI/HarmonyOS devices.
- **Operating Context**: 100% offline, zero internet requirement for core playback and library management.

## 3. Product Principles
1. **User File Ownership**: The user's audio collection remains intact in the file system. SunPlayer is a fast index and player, never an isolating proprietary vault.
2. **Privacy First**: Zero trackers, no analytics SDKs, no accounts, no telemetry.
3. **Instant Responsiveness**: Smooth 60/120fps scrolling even on large libraries (10,000+ to 100,000+ tracks).
4. **Reliability & Graceful Degradation**: Corrupt audio files or invalid tags must never crash the player or scan process.
5. **Lightweight Footprint**: Optimized R8 shrinking, low battery/CPU consumption during playback.

## 4. Key Functional Areas
- **Storage & Scanner**: Incremental MediaStore and folder scanning (detecting added, modified, moved, deleted files).
- **Audio Engine**: Android Media3 ExoPlayer with gapless playback, audio focus handling, and headset/Bluetooth response.
- **Library Views**: Folders, Songs, Albums, Artists, Genres, Playlists, Favorites, and Search.
- **Queue Management**: Dynamic playback queue (play next, add to end, drag-and-drop reorder, save to playlist).
- **Audio Pipeline & DSP (Future Milestone)**: Preamp, graphic/parametric equalizer, bass boost, balance, and volume leveling.
- **Optional Extensions**: Offline lyrics (LRC/embedded), metadata editor, smart playlists.
