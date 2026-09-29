# SunPlayer

An **offline-first** local music player for Android (Jetpack Compose + Media3).
No account, no cloud, no ads, no trackers, no telemetry — the core app does not
even declare the `INTERNET` permission. Nothing you listen to ever leaves your
device.

Current version: **1.3.0.1** (targets Android 16 / API 36, minSdk 26).

## Features

- Room-backed local library built from Android MediaStore (fast, survives rescans)
- Background playback via Media3 `MediaSessionService` — lock-screen, Bluetooth,
  headphone-unplug pause, and Android Auto compatible
- Queue management: play next, add to end, reorder, remove, clear upcoming
- Normal playlists (create / rename / delete / ordered membership) and Favorites
- Search, sort (title / artist / album / duration), shuffle, repeat modes
- Persistent play state; failed scans never wipe your existing library

## Privacy & security at a glance

| Question | Answer |
|---|---|
| What can it access? | Only your audio files, via Android MediaStore (`READ_MEDIA_AUDIO`; `READ_EXTERNAL_STORAGE` ≤ API 32 only). No documents, photos, or messages. |
| Does it phone home? | No. The APK contains no `INTERNET` permission — verify with `aapt dump permissions`. |
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

Push a version tag and GitHub Actions does the rest — secret scan → tests →
signed APK+AAB → SHA-256 checksums → emulator smoke test → GitHub Release
(behind a manual approval gate):

```bash
git tag v1.3.0.2 && git push origin v1.3.0.2
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
