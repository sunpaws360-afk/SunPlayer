# SunPlayer — Android Build & Release CI/CD Handover Document

**Date:** Monday, October 12, 2026
**Repository:** https://github.com/sunpaws360-afk/SunPlayer (branch `main`)
**App version under release:** `versionName = "1.3.0.1"`, `versionCode = 5` (`app/build.gradle.kts`)
**Local repo state:** `main` @ **HEAD = see `git rev-parse --short main`** (includes this document; the exact hash changes with each edit so it is referenced symbolically here) on top of `01e6624` — **ahead of `origin/main` (`a97efdc`) by 7 commits, NOT yet pushed** (sandbox has no GitHub credentials).

---

## 1. Executive Summary

Two separate blockers were diagnosed across recent workflow runs:

1. **`secret-scan` (gitleaks) job failing** — false-positive detections in documentation/config files on older commits still present on the remote. Remediated locally via `.gitleaks.toml` allowlist + config restoration; verified clean with the exact gitleaks version CI uses. **Fix exists only locally; must be pushed.**
2. **`release-candidate` job failing at "Configure release signing"** — caused by an *old* commit snapshot of the workflow (strict empty-secret check) running *before* the environment secrets were created. Secrets now exist and resolve correctly (`***` in logs). The current workflow on `main` handles missing secrets gracefully and releases are gated to **tag pushes only**.

**The single most important outstanding action:** run `git push origin main` from a machine with GitHub credentials (fast-forward `a97efdc → local main HEAD`, safe, no force needed). Until then, GitHub keeps building the stale remote tip whose gitleaks config differs and can still flag false positives.

---

## 2. Workflow Run History (what each failure actually was)

| Run | Trigger | Job | Failure cause | Status |
|---|---|---|---|---|
| **#45** (36589284944, commit `3f37599`) | PR #4 merge ("sunplayer-security-vulnerab…") | `release-candidate` | Old workflow snapshot had strict validation: any empty signing secret ⇒ `exit 1`. Run executed before/independent of the environment secrets being finalized. Debug log shows all four secrets evaluating to `'***'` (non-empty) at expression level, but the step's shell check failed on that stale code path. Also note: PR events historically could reach this job under the old `on:` triggers. | Superseded — do not re-run old runs; trigger fresh. |
| **#61** (36979146305) | — | `secret-scan` | gitleaks 8.24.3 reported leaks: `.gitleaks.toml#L1-45`, `RELEASE-CHECKLIST.md#L1-41`, `RELEASE-CHECKLIST.md#L1-25`, `DISTRIBUTION.md#L1-60`. All reviewed line-by-line: **documentation false positives, no real credentials anywhere in repo or history for those lines.** | Fixed locally (allowlist + config restore); needs push. |
| **#68** (a97efdc) | `workflow_dispatch` on `main` | — | Showed **"Queued"** — normal, not a hang. `concurrency: group sunplayer-${{ github.ref }}` + `cancel-in-progress: true` serializes runs per ref; queued runs show no jobs/duration/artifacts until a runner picks them up. Also: dispatch runs execute only `secret-scan` + `verify`; release jobs require a `v*` tag. | Informational. |

---

## 3. Git State & Divergence (critical)

- Local `main` contains 4 CI/gitleaks-fix commits: `858c090` (allowlist historical docs FP commit), `f391e43` (merge allowlists, restore full ruleset), `466c616` (restore complete `.gitleaks.toml`: `[extend] useDefault=true`, both custom rules, single global commits list).
- Remote `origin/main` has 3 different commits pushed directly from GitHub: `347e00c` (modify gitleaks.toml), `bdc5d8c` (.gitignore local secrets), `a97efdc` (refactor release-signing secrets validation). Branches had **diverged**; any push would have been rejected.
- Resolution performed in `/workspace`: merged `origin/main` into local `main` (`cb6d9be`), then deduplicated the `keystore-*.txt` block the merge accidentally repeated in `.gitignore` (`01e6624`). Result keeps the complete gitleaks config **and** the remote's new graceful signing-secrets validation.
- Push test from sandbox: `git push` hangs at username prompt — credential store file configured but absent. **No push was possible from this environment.**

### Windows PowerShell notes from the user's session
- Multi-line command with `\` continuations failed — **`\` is not a line-continuation character in PowerShell** (that's bash). Use backtick `` ` `` or put the whole command on one line. The one-line retry worked.
- `keytool` warning *"Different store and key passwords not supported for PKCS12 KeyStores"* ⇒ for `.keystore` (PKCS12) files, **store password == key password**. Set `SUNPLAYER_KEY_PASSWORD` identical to `SUNPLAYER_KEYSTORE_PASSWORD` in GitHub.
- Second `keytool -genkeypair` errored `alias <sunplayer> already exists` — expected; the first invocation already created it. Don't regenerate unless you delete the keystore (and if you regenerate, the previously copied Base64 is stale).
- Keystore artifact confirmed: `C:\Users\rebecca.arecksamy\sunplayer-release.keystore`, 2,718 bytes, dated 10/3/2026. Base64 copied to clipboard via `[Convert]::ToBase64String([IO.File]::ReadAllBytes(...)) | Set-Clipboard` (single continuous line — correct format).
- `git push origin main` failed with `fatal: not a git repository` because it ran in `%USERPROFILE%`, and a recursive search found **no `.git` directory on that machine** — there is no clone there. Must clone first (see §8).

---

## 4. Gitleaks Remediation (details)

**Final `.gitleaks.toml` (local `main`, verified valid TOML):**
- `[extend] useDefault = true` — restores built-in ruleset (ghp_, github_pat_, private keys, etc.). Earlier broken edits had dropped this.
- Custom rules retained: `generic-secret-assignment` and `android-keystore-reference` (the latter allowlisted for `docs/*.md` and `README.md`).
- Global `[allowlist]`:
  - `paths`: `gitleaks-report.json`
  - `commits`: `5ef4d0fdd558c6b120449ba078d3618b55188a31` — historical pre-config commit where prose in `docs/STATUS.md` matched the built-in generic-api-key rule via substring "CI/in". Not a secret; file rewritten later.
  - `regexes`: env-var indirection patterns (`providers.environmentVariable("SUNPLAYER_...")`, `${{ secrets.SUNPLAYER_... }}`) and doc placeholders (`your-(password|alias|keystore)`).

**Why earlier advice was wrong / corrected:**
- A `.gitleaksignore` JSON-style file as proposed in some analyses is **not the reliable mechanism here**; commit-SHA allowlisting inside `.gitleaks.toml`'s global `[allowlist].commits` is what works for history scans and survives gitleaks version bumps differently than fingerprint-based ignore files.
- Newer gitleaks versions hash file *blobs*; the remote's `.gitleaks.toml` differed from local by a trailing newline, changing fingerprints and re-triggering detections on the config file itself. The merge resolved this.

**Verification performed:**
- Ran gitleaks locally against full history (47 commits at the time) with the merged config: **"no leaks found."**
- Re-checked with gitleaks **8.24.3** (exact version used by `gitleaks/gitleaks-action@v2` in the failing run): local tip `01e6624` and remote tip `a97efdc` both clean.
- No secret-like files are git-tracked (`git ls-files` grep for keystore/.jks/secret/password/.env ⇒ none).

---

## 5. GitHub Environment `release` — Configuration & Protection Rules

User-confirmed state (from Settings screenshot):
- **Environment secrets present** (all "Updated ~12 minutes ago"): `SUNPLAYER_KEYSTORE_BASE64`, `SUNPLAYER_KEYSTORE_PASSWORD`, `SUNPLAYER_KEY_ALIAS`, `SUNPLAYER_KEY_PASSWORD`. Input boxes clearing after save is **by design** — values are stored.
- **Deployment protection rules:** `Required reviewers: sunpaws360-afk` (repo owner). ⚠️ If **"Prevent self-review"** is checked, the owner cannot approve their own deployment and the job stays blocked indefinitely. Either uncheck it, or remove/add a second reviewer. There is **no wait timer** mentioned — nothing to "wait out"; approval is the only gate.
- **Workflow binding verified:** `release-candidate` and `publish` both declare `environment: release` — required so environment secrets are injected instead of repository secrets.

**Behavioral caveat introduced by remote commit `a97efdc` (now merged):** missing signing secrets produce a `::notice::` + empty `keystore_path` output and `exit 0` (skip) instead of a hard error. On a **tag release** this means Gradle builds an **unsigned** release and the pipeline fails later at the SHA-256 step. Recommendation: fail fast on tag builds when secrets are absent (i.e., keep skip behavior only for non-release contexts), or simply ensure secrets remain configured.

---

## 6. Signing Secrets — Exact Values To Store

| Secret name | Value | Notes |
|---|---|---|
| `SUNPLAYER_KEYSTORE_BASE64` | Output of `[Convert]::ToBase64String([IO.File]::ReadAllBytes("sunplayer-release.keystore"))` | Single continuous line, no CRLF. Verified pattern matches prior 3,624-char single-line encoding (0 `\r`, 0 `\n`, decodes cleanly, validated with `keytool -list` against alias `sunplayer_release`/`sunplayer`). Decode test: `echo "<string>" | base64 --decode > test.jks && keytool -list -keystore test.jks -storepass '<pw>'`. |
| `SUNPLAYER_KEYSTORE_PASSWORD` | The `-storepass` used at generation (e.g. `YourKeystorePassword123!` — replace with your real strong password) | |
| `SUNPLAYER_KEY_PASSWORD` | **Must equal the store password** | PKCS12 ignores separate keypass; keeping them equal avoids mismatch failures. |
| `SUNPLAYER_KEY_ALIAS` | `sunplayer` | Matches `-alias` used in `keytool -genkeypair`. |

Key parameters baked into the keystore: RSA 2048, SHA256withRSA, validity 10,000 days, dname `CN=SunPlayer, OU=Android, O=Rebecca, L=, S=, C=US`.

⚠️ Security hygiene: the placeholder passwords shown in chat (`YourKeystorePassword123!` / `YourKeyPassword123!`) should **not** be the final production values if they were ever shared publicly in transcripts/screenshots; consider regenerating with unique strong secrets and updating GitHub accordingly (regeneration requires deleting the existing keystore file first due to alias-exists error). Keep the keystore backed up offline — losing it means losing the ability to update the app on Google Play under the same signing key.

---

## 7. Workflow File Facts (`.github/workflows/android_build.yml`, current `main`)

- Triggers: `push` (branches/tags incl. `v*`) and `workflow_dispatch`. `permissions: contents: read`.
- Concurrency: `group: sunplayer-${{ github.ref }}`, `cancel-in-progress: true` (explains queued/serial runs).
- Jobs:
  1. `secret-scan` — gitleaks (`gitleaks/gitleaks-action@v2`, binary 8.24.3). Gates everything.
  2. `verify` — build/test.
  3. `release-candidate` — `needs: [verify, secret-scan]`, `if: github.event_name == 'push' && startsWith(github.ref, 'refs/tags/v')`, `environment: release`. Steps: derive tag from `versionName` & validate match → configure signing (graceful skip if secrets empty) → `./gradlew testDebugUnitTest assembleRelease bundleRelease` → **delete decoded keystore from `$RUNNER_TEMP` immediately** (`if: always()`) → SHA-256 checksums of APK/AAB → emulator smoke test.
  4. `publish` — `needs: [release-candidate, secret-scan]`, tag-gated, `environment: release`.
- Known non-blocking warnings: Node.js 20 deprecation notices (`actions/checkout@v4`, `setup-java@v4`, `setup-gradle@v3`, `gitleaks-action@v2` forced onto Node 24); `setup-java v4` deprecated ⇒ migrate to `@v5`; gradle cache restore 400 (transient, harmless); ubuntu-latest → Ubuntu 26 migration Oct 19, 2026.
- Tag `v1.3.0.1` already exists locally; verify whether it exists on remote (`git ls-remote --tags origin`) before pushing — if it does, either delete+recreate it deliberately or bump the version.

---

## 8. Remaining Action Checklist (ordered)

**On the Windows machine (or wherever credentials live):**
1. Clone the repo (none exists on that PC):
   ```powershell
   git clone https://github.com/sunpaws360-afk/SunPlayer.git "$env:USERPROFILE\SunPlayer"
   cd "$env:USERPROFILE\SunPlayer"
   ```
2. Bring in the 7 fix commits. Options: (a) copy/export from the environment holding them (`git bundle create fixes.bundle a97efdc..main` → transfer → `git fetch fixes.bundle main:main`), or (b) cherry-pick/recreate the `.gitleaks.toml` + `.gitignore` changes. Target tip: local `main` HEAD (fixes through `01e6624`, merge `cb6d9be`, plus this handover doc).
3. Confirm: `git status -sb` shows `main...origin/main [ahead 7]`, working tree clean.
4. `git push origin main` — fast-forward `a97efdc → local main HEAD (currently `43c7cb0`)`, **no force needed**.
5. In GitHub Settings → Environments → `release`: ensure **"Prevent self-review" is UNCHECKED** (or add another reviewer). Verify all 4 secrets listed (§6 values).
6. Sanity-check the scan passes: Actions → "Android Build & Release" → Run branch (push event on `main`) → `secret-scan` green.
7. Cut the release:
   ```powershell
   git tag v1.3.0.1        # skip if tag already on remote; then decide delete/recreate vs. version bump
   git push origin v1.3.0.1
   ```
8. In the new run, `release-candidate` enters **Waiting** → click **Review deployments → Approve and deploy**. It then executes immediately (no timers involved).
9. Download artifacts (signed APK/AAB + `SHA256SUMS.txt`), verify checksums before sideloading/publishing.

**If gitleaks still flags anything after the push:** open the SARIF artifact/job summary, take the offending **commit SHA**, and append it to `[allowlist].commits` in `.gitleaks.toml` with an explanatory comment (same pattern as `5ef4d0fd…`). Only do this after confirming the hit is genuinely a documentation false positive — never allowlist a real credential; rotate it instead.

**Optional hardening backlog:** migrate `setup-java@v4→v5`, `checkout@v4→v5`, `setup-gradle@v3→v4`; make empty signing secrets a hard failure on tag builds; revisit `.gitleaksignore` only if you prefer fingerprint-based ignores over commit allowlists.

---

## 9. Things Explicitly Debunked During This Investigation

- ❌ "You need to wait for a cooldown/timer" — No. Only gates are the concurrency queue and the manual environment approval.
- ❌ "Create a `.gitleaksignore` with Pattern/Reason entries" — wrong format/mechanism for this history-scan setup; use `.gitleaks.toml` `[allowlist].commits`.
- ❌ "Real secrets leaked; scrub history with BFG" — every flagged line was inspected; all false positives; no history rewrite needed.
- ❌ "Environment binding missing" — both release jobs already declare `environment: release`.
- ❌ "Base64 malformed" — verified single-line, decodes, keytool-valid.
- ❌ "Fork/PR secret redaction applies" — release jobs are tag-push-gated on `main`; not fork-PR driven.
- ✅ "GitHub clears the secret textarea after saving" — correct and by design.

---

## 10. Current Repo Artifacts In `/workspace` (ignored, untracked — do not commit)

`sunplayer-release.keystore` (2,718 B), `sunplayer-release.keystore.base64` / `keystore-base64.txt` (3,624 chars, single line), `keystore-password.txt`, `.sunplayer-keystore-password.txt`. `.gitignore` covers these via `*.keystore`, `*.base64`, `keystore-*.txt` (deduped in `01e6624`; verified with `git check-ignore`). None are tracked by git.
