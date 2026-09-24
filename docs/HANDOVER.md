# SunPlayer Handover

## FINAL INTENT

SunPlayer is a lightweight, private, offline-first Android music player and
personal music library. It should remain useful without an account,
subscription, cloud service, AI, or Internet connection. The database is an
index and stores user-owned application state; it is not a proprietary music
vault.

## CURRENT PRODUCT STATE

The app scans local MediaStore audio, persists a Room-backed library, plays
through a Media3 `MediaSessionService`, supports search/sort, shuffle/repeat,
queue operations, and persistent favorites. It now has normal user-created
playlists with durable membership and ordering.

## CURRENT PHASE

Playlists and durable user collections.

## OBJECTIVES

1. Add normalized playlist storage without mixing user state into scanner metadata.
2. Preserve favorites and playlists across rescans and database upgrades.
3. Provide playlist CRUD, membership, ordering, and playback through existing APIs.
4. Keep missing files from destroying playlist structure or crashing access.
5. Add focused persistence tests and document exact verification limits.

## COMPLETED

- Added Room schema version 3.
- Added `PlaylistEntity` for playlist metadata.
- Added normalized `PlaylistTrackEntity` with stable explicit positions.
- Added indexes for playlist ordering and track lookup.
- Added non-destructive migrations from schema 1 to 2 and 2 to 3.
- Added playlist create, rename, delete, and observable list operations.
- Added add/remove track operations with duplicate membership rejection.
- Added persistent playlist reordering.
- Added playlist track counts.
- Added playlist playback and shuffle playback through the existing player facade.
- Added playlist list and detail UI.
- Added create, rename, delete, add tracks, remove tracks, and reorder controls.
- Added reusable Add to playlist flow from library rows.
- Added create-and-add flow from the track picker.
- Kept missing track membership rows when scanner-owned track rows disappear.
- Added explicit `ScanResult.Success`/`ScanResult.Failure` semantics so a failed
  MediaStore query cannot clear the Room library.
- Preserved coroutine cancellation during scanning instead of converting it to
  a partial successful result.
- Added a repository regression test proving failed scans retain indexed tracks.
- Prepared v1.3.0 version metadata, tagged-release CI artifacts, and release notes.
- Preserved existing Favorites behavior and schema migration.
- Added playlist Room instrumentation coverage for CRUD, ordering, duplicate
  membership, and missing-track handling.
- Updated this handover with verified and blocked status.

## LEFT

- Run the full CI build on Java 17.
- Run instrumentation tests on an Android emulator or physical device.
- Add explicit migration upgrade tests from a persisted version 2 database.
- Add scanner tests for empty-success versus failure and permission/query errors.
- Show missing playlist entries in the detail UI as unavailable placeholders.
- Add playlist-to-queue without immediately starting playback.
- Add library destinations/tabs for playlists and favorites.
- Add persistent queue and playback history.
- Replace debug release signing with protected release signing.
- Push a `v1.3.0` tag after CI approval to publish the development release.
- Improve incremental/resumable scanning, artwork, metadata editing,
  accessibility, localization, backup/restore, Android Auto, and widgets.

## TESTED

- Static diagnostics: passed for changed Kotlin, Room, UI, and test files.
- `git diff --check`: passed.
- Source-level review: completed for schema, repository, UI, and playback paths.

## BLOCKED

- `./gradlew :app:testDebugUnitTest` did not reach compilation. The container
  runs OpenJDK `25.0.4.1`; Kotlin `1.9.22` fails while parsing that Java
  version. CI is configured for Temurin Java 17 and is the required executable
  verification environment.
- Instrumentation tests were not run because no emulator/device is attached.

## KNOWN LIMITATIONS

- Playlist detail currently displays available tracks through an inner join;
  membership for a deleted file remains stored but is not yet shown as an
  unavailable placeholder.
- Playlist membership is intentionally not foreign-keyed to `tracks`, so a
  MediaStore rescan cannot destroy user playlist structure.
- Release builds still use the existing debug signing configuration.
- The v1.3.0 GitHub release is development-only until protected signing is configured.
- No smart playlists, AI, downloads, recording, network providers, DSP, or
  advanced Android integrations are part of this phase.

## NEXT PHASE

Run CI and device validation first. Then add explicit unavailable-track UI,
playlist-to-queue support, and library navigation tabs before starting playback
history or ratings.

## DO NOT REDO

- Do not recreate the Android project or Gradle wrapper.
- Do not replace the existing MediaSession playback architecture.
- Do not remove Room schema migrations.
- Do not move Favorites into scanner-owned metadata or reset favorites on scan.
- Do not replace normalized playlist membership with a serialized list.
- Do not begin AI, downloads, recording, network, or advanced DSP work yet.

## IMPORTANT FILES

- `app/src/main/java/com/rebecca/sunplayer/data/SunPlayerDatabase.kt`
- `app/src/main/java/com/rebecca/sunplayer/data/PlaylistDao.kt`
- `app/src/main/java/com/rebecca/sunplayer/data/AudioLibraryRepository.kt`
- `app/src/main/java/com/rebecca/sunplayer/PlaylistUi.kt`
- `app/src/main/java/com/rebecca/sunplayer/MainActivity.kt`
- `app/src/androidTest/java/com/rebecca/sunplayer/data/PlaylistDaoTest.kt`
- `.github/workflows/android_build.yml`

## IMPORTANT ARCHITECTURAL DECISIONS

- Scanner-owned track metadata and user-owned state remain separate concerns.
- Playlist membership uses a normalized join table with explicit positions.
- Playlist membership does not cascade from track deletion; missing files remain
  representable and can be surfaced or cleaned by a later user action.
- The existing playback facade is reused for playlist playback rather than
  introducing a second queue or player implementation.

PROJECT INTENT
---------------

FINAL PRODUCT GOAL:
A lightweight, private, offline-first Android music player and personal library.

CURRENT PRODUCT STATE:
Local scanning, Room persistence, MediaSession playback, queue, favorites, and
normal durable playlists are implemented.

CURRENT PHASE:
Playlists and durable user collections.

OBJECTIVES:
1. Normalize playlist state.
2. Preserve user data across scans and upgrades.
3. Integrate playlist management with existing playback.

COMPLETED:
- Playlist schema, migration, repository, UI, playback, and focused tests added.

LEFT:
- Java 17 CI verification, device testing, unavailable placeholders, tabs,
  history, signing, and later product phases.

TESTED:
- Static diagnostics and whitespace checks passed.

BLOCKED:
- Local Gradle execution stops on OpenJDK 25 before Kotlin compilation.

KNOWN LIMITATIONS:
- Missing playlist tracks remain stored but are currently hidden from the detail
  list because available tracks are resolved through an inner join.

NEXT PHASE:
Verify CI/device behavior, then improve unavailable-track UI and navigation tabs.

DO NOT REDO:
- Preserve Room, Favorites, MediaSession playback, queue, and normalized
  playlist membership.

IMPORTANT FILES:
- `SunPlayerDatabase.kt`, `PlaylistDao.kt`, `AudioLibraryRepository.kt`,
  `PlaylistUi.kt`, `MainActivity.kt`, and `PlaylistDaoTest.kt`.

IMPORTANT ARCHITECTURAL DECISIONS:
- Scanner-owned metadata stays separate from user-owned Favorites and playlist
  membership; missing files do not cascade-delete playlist structure.
