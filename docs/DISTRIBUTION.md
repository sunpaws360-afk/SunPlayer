# SunPlayer Distribution Guide

How, where, and with what key SunPlayer is shipped. This complements the security
audit notes in `docs/HANDOVER.md` and the CI pipeline in
`.github/workflows/android_build.yml`.

## Current state (verified against this repo)

| Item | Status |
|---|---|
| Release signing config (`app/build.gradle.kts`) | ✅ Present — reads `SUNPLAYER_KEYSTORE_PATH`, `SUNPLAYER_KEYSTORE_PASSWORD`, `SUNPLAYER_KEY_ALIAS`, `SUNPLAYER_KEY_PASSWORD` from the environment. Falls back to an **unsigned** release build if unset, so no keystore is ever required for ordinary PR builds. |
| AAB build | ✅ CI runs `bundleRelease`; artifact uploaded as `SunPlayer-release-candidate`. |
| Minify + resource shrink | ✅ Already enabled on `release`. |
| GitHub Release publish | ✅ `publish` job creates tag + release with APK and AAB assets. |
| Target SDK | ✅ `targetSdk = 36` (Android 16), satisfying the Aug 31 2026 Play requirement. |
| SHA-256 checksums on releases | ❌ Not yet — see Open items. |
| Privacy policy hosted | ❌ Required before any store listing. |
| Actual release keystore | ❌ Must be generated **offline**; nothing committed or needed in-repo. |

> Note: the example in older planning docs used `System.getenv("RELEASE_KEYSTORE")`
> etc. The repo standardizes on the `SUNPLAYER_*` variable names above; use those
> exact names in GitHub environment secrets so CI and local builds agree.

## Signing rules (non-negotiable)

1. One real release keystore: RSA 2048, ≥25-year validity.
   ```bash
   keytool -genkeypair -v -keystore sunplayer-release.keystore \
     -alias sunplayer_release -keyalg RSA -keysize 2048 -validity 10000
   ```
   Enter passwords interactively — never put them in shell history or scripts.
2. The keystore file lives offline (encrypted backup ×2). Only a base64 copy goes
   into the GitHub `release` **environment** secrets. `.gitignore` blocks
   `*.keystore`, `*.jks`, `*.p12`, `*.pfx`, `secrets.properties`.
3. Google Play: enroll in **Play App Signing** — the keystore above becomes the
   *upload* key only; Google holds the app-signing key.
4. Sideloading / AppGallery / Amazon / Samsung: same release key signs the APK
   directly. Debug-signed APKs are never distributed outside the team.
5. If the signing key ever changes, users must uninstall/reinstall. Treat rotation
   as a last resort.

## Channel strategy (phased)

| Phase | Channel | Artifact | Notes |
|---|---|---|---|
| Now | GitHub Releases | signed APK (+AAB) | Fastest path; users sideload. Publish checksum alongside. |
| Next | Huawei AppGallery | signed APK | Primary audience (no GMS dependency in SunPlayer). Needs Huawei developer account + review. |
| Then | Google Play | AAB via internal track | $25 account, Play App Signing, privacy policy, data-safety form, IARC rating, ≥4 screenshots, feature graphic. Internal → closed → production. |
| Later | F-Droid | built from source | Requires adding an FLOSS license file first (repo currently has none) and no proprietary deps (currently satisfied). |
| Optional | Amazon / Samsung stores | signed APK/AAB | Each has its own review process. |

## Releasing today (the pipeline already does most of it)

1. Bump `versionCode`/`versionName` in `app/build.gradle.kts` and add
   `docs/releases/<version>.md` (the `publish` job uses it as release notes).
2. Ensure the `release` environment in GitHub has the four `SUNPLAYER_*` secrets
   and a **required reviewer** (manual approval gate).
3. Run workflow_dispatch on `main` with `tag=v<versionName>` → CI verifies tests,
   scans for secrets, builds signed APK+AAB, smoke-tests on an emulator, then
   (after approval) publishes the GitHub Release.
4. Attach the printed SHA-256 of both artifacts to the release body (or use the
   planned automated step) so sideloaders can verify integrity.

For Play uploads, extend `publish` later with `r0adkll/upload-google-play@v1`
using a `PLAYSTORE_JSON` service-account secret; keep that JSON out of every
agent's reach and out of the repo (already gitignored).

## Open items / next PRs

- [ ] Generate the offline release keystore; upload base64 to `release` env secrets.
- [ ] Add SHA-256SUMS generation to the `publish` job.
- [ ] Host a privacy policy (simple GitHub Pages works); link from README + store listings.
- [ ] Add an `LICENSE` file (FLOSS) if F-Droid is desired.
- [ ] Protect `main`: require PR + status checks, forbid force-push.
- [ ] User-facing permission rationale screen in-app ("why audio access, nothing leaves the device").
