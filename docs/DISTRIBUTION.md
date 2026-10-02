# SunPlayer Distribution Guide

How, where, and with what key SunPlayer is shipped. This complements the security
audit notes in `docs/HANDOVER.md` and the CI pipeline in
`.github/workflows/android_build.yml`.

## Current state (verified against this repo)

| Item | Status |
|---|---|
| Release signing config (`app/build.gradle.kts`) | ✅ Present — reads `SUNPLAYER_KEYSTORE_PATH`, `SUNPLAYER_KEYSTORE_PASSWORD`, `SUNPLAYER_KEY_ALIAS`, and `SUNPLAYER_KEY_PASSWORD` from the environment. Local Gradle builds may be unsigned when these are unset; the tagged CI release workflow fails if any signing secret is missing or the decoded keystore is empty. |
| Release environment secrets | ✅ The four `SUNPLAYER_*` signing secrets were visible in the GitHub `release` environment settings on 2026-10-02. This confirms they are configured, not that the keystore/password pair has successfully signed a published artifact. |
| AAB build | ✅ CI runs `bundleRelease`; the `SunPlayer-release-candidate` artifact is uploaded after a successful tagged build. |
| Minify + resource shrink | ✅ Already enabled on `release`. |
| GitHub Release publish | ✅ The `publish` job creates a GitHub Release with APK, AAB, and checksums after the tagged build succeeds and any required environment approval is granted. |
| Target SDK | ✅ `targetSdk = 36` (Android 16), satisfying the Aug 31 2026 Play requirement. |
| SHA-256 checksums on releases | ✅ The workflow generates `SHA256SUMS.txt` for the APK and AAB and attaches it to the GitHub Release. |
| Privacy policy | ✅ Source is in `docs/privacy.html`, and a GitHub Pages workflow exists. Confirm the published URL returns successfully before submitting to an app store. |
| Release keystore | ✅ Keystore material is not tracked in Git; the Base64-encoded keystore is configured as an environment secret. The signature of a successfully published release still needs verification. |
| Playlists | ✅ The app supports local playlist creation, rename/delete, adding/removing tracks, reordering, and play/shuffle. Playlist persistence uses the local Room database. |

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

## Releasing (fully tag-driven automation)

1. On a branch: bump `versionCode`/`versionName` in `app/build.gradle.kts` and
   add `docs/releases/<version>.md` (used verbatim as GitHub release notes).
2. Merge to `main`. Ensure the `release` environment in GitHub has the four
   `SUNPLAYER_*` secrets and a **required reviewer** (manual approval gate).
3. `git tag v<versionName> && git push origin v<versionName>` — that single
   command triggers the whole pipeline: secret scan + verify run first;
   `release-candidate` validates tag↔version match, builds the signed
   APK + AAB, generates `SHA256SUMS.txt`, runs the emulator smoke test;
   `publish` (after environment approval) creates the GitHub Release with
   APK, AAB, and SHA256SUMS attached and marks it `--latest`.
4. Sidenotes: `workflow_dispatch` now only runs verification (no release);
   pushing a tag whose name ≠ `v<versionName>` fails immediately, and a
   missing changelog file also fails before anything is published.

For Play uploads, extend `publish` later with `r0adkll/upload-google-play@v1`
using a `PLAYSTORE_JSON` service-account secret; keep that JSON out of every
agent's reach and out of the repo (already gitignored).

## Remaining release checks / future work

- [ ] Run the updated secret scan and verification workflow on current `main`.
- [ ] Confirm a tagged release produces an APK signed with the expected release certificate; verify the attached checksums.
- [ ] Confirm the privacy policy URL is live and link it from applicable store listings.
- [ ] Add an `LICENSE` file (FLOSS) if F-Droid is desired.
- [ ] User-facing permission rationale screen in-app ("why audio access, nothing leaves the device").
