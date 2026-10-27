# SunPlayer

An **offline-first** local music player for Android (Jetpack Compose + Media3).
No account, no cloud, no ads, no trackers, no telemetry. The only network access
is the optional in-app update check against GitHub Releases — nothing you listen
to ever leaves your device.

Current version: **1.4.0** (targets Android 16 / API 36, minSdk 26).

## Features

- Room-backed local library built from Android MediaStore (fast, survives rescans)
- Background playback via Media3 `MediaSessionService` — lock-screen, Bluetooth,
  headphone-unplug pause, and Android Auto compatible
- Queue management: play next, add to end, reorder, remove, clear upcoming
- Normal playlists (create / rename / delete / ordered membership) and Favorites
- Library tabs for Tracks, Albums, Artists, Favorites, and Playlists
- 0–5 star ratings, persisted per track and editable while a song plays
- In-app update check: detects a newer signed GitHub release and hands the
  downloaded APK to Android's installer (never silent, never automatic)
- Search, sort (title / artist / album / duration), shuffle, repeat modes
- Persistent play state; failed scans never wipe your existing library

## Privacy & security at a glance

| Question | Answer |
|---|---|
| What can it access? | Only your audio files, via Android MediaStore (`READ_MEDIA_AUDIO`; `READ_EXTERNAL_STORAGE` ≤ API 32 only). No documents, photos, or messages. |
| Does it phone home? | Only for the update check: a plain HTTPS request to GitHub's public Releases API for the latest version number. No identifiers, no telemetry, no analytics. Everything else is offline. |
| Where is my data? | Library index, playlists, favorites live in a local Room database on your device. |
| Can I delete it? | Uninstalling removes the app database. Your music files are untouched. |
| How are releases signed? | Release-signed with a protected key in CI (never committed). Debug builds are clearly not for distribution. |

Full rationale: [docs/DISTRIBUTION.md](docs/DISTRIBUTION.md) · security
practices enforced in CI (gitleaks secret scanning, read-only workflow tokens,
manual approval gate on the `release` environment).

## Download & install

Grab the latest release-signed build from
[GitHub Releases](../../releases). Each release ships:

- `app-release.apk` — direct sideload (Huawei/EMUI devices included)
- `app-release.aab` — Android App Bundle for store uploads
- `SHA256SUMS.txt` — verify integrity before installing:
  `sha256sum -c SHA256SUMS.txt`

On Huawei/sideloaded installs you must allow "Install from unknown sources";
always check the checksum against the one published on the release page.

## Building from source

```bash
./gradlew assembleDebug          # debug APK (no signing secrets needed)
./gradlew testDebugUnitTest      # unit tests
```

Release builds fall back to **unsigned** unless the `SUNPLAYER_KEYSTORE_*`
environment variables are set — see [docs/BUILDING.md](docs/BUILDING.md).

## Releasing (automation)

Releases are automatic. Merge a PR to `main` that bumps `versionCode`/`versionName`
in `app/build.gradle.kts` and adds `docs/releases/<version>.md`; CI runs the
secret scan, tests, signed APK+AAB build, SHA-256 checksums and emulator smoke
test, then creates the matching `v<version>` tag and GitHub Release (behind a
manual approval gate). Approve the deployment to publish — installed release
builds then offer the update in-app.

Manual tagging still works:

```bash
git tag v1.4.0 && git push origin v1.4.0
```

Details and channel strategy (Play / AppGallery / F-Droid):
[docs/DISTRIBUTION.md](docs/DISTRIBUTION.md).

## Documentation

- [docs/PRODUCT.md](docs/PRODUCT.md) — what the app is and who it is for
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — modules, data flow, playback stack
- [docs/BUILDING.md](docs/BUILDING.md) — local build setup
- [docs/DECISIONS.md](docs/DECISIONS.md) — architecture decision records
- [docs/STATUS.md](docs/STATUS.md) — current state & roadmap
- [docs/releases/](docs/releases/) — changelogs per version

## Project status & license

SunPlayer is under active personal development. A project license has not been
chosen yet (required before an F-Droid listing); see
[docs/DISTRIBUTION.md](docs/DISTRIBUTION.md) for open items.
