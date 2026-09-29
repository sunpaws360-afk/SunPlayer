# Release Setup Checklist (one-time)

Status as of 2026-09-29:
- [x] Release keystore generated (alias `sunplayer_release`, RSA-2048, valid until 2054-02-14)
      Files live ONLY in the task sandbox: the sandbox keystore export files (gitignored; see docs/DISTRIBUTION.md for storage rules). Cert SHA-256 fingerprint starts A9:E4:96:53.
- [x] GitHub environment `release` created
- [x] Secret scanning + Push Protection enabled
- [x] Branch protection on `main` (require PR, strict status checks, no force-push/delete)
- [ ] **Four secrets in the `release` environment** — blocked: current sandbox token lacks Actions-secret scope (403 on list AND write, even though it has repo admin + branch protection worked). Add manually:
      Settings → Secrets and variables → Actions → Environments → release → Add secret
      | Name | Value |
      |---|---|
      | SUNPLAYER_KEYSTORE_BASE64 | contents of sunplayer-release.keystore.base64 |
      | SUNPLAYER_KEYSTORE_PASSWORD | contents of .sunplayer-keystore-password.txt |
      | SUNPLAYER_KEY_ALIAS | sunplayer_release |
      | SUNPLAYER_KEY_PASSWORD | same password again |
      NOTE: names are SUNPLAYER_* (NOT RELEASE_* as some drafts say — the workflow at
      .github/workflows/android_build.yml lines 102-129 reads SUNPLAYER_*. If you already
      created RELEASE_KEYSTORE_* secrets per an earlier draft, rename them; they will not be picked up.)
      These four secrets are the ONLY remaining blocker for v1.3.0.1: the tag is pushed,
      CI correctly failed with "All release-signing secrets must be defined" (run 36589284944),
      so after adding them just click Re-run failed jobs on that run — no new tag needed.
- [x] Privacy policy content DONE: docs/privacy.html committed & merged to main (PR #6/#7); accurate to the app
      (no INTERNET permission, local-only data, backup rules, deletion). Content verified serving-ready.
- [x] GitHub Pages deploy workflow live (.github/workflows/pages.yml, configure-pages enablement:true).
      NOTE (re-verified 2026-09-29): Pages site does NOT exist yet (API 404). Both paths to create it are
      blocked for this token: (a) GITHUB_TOKEN inside Actions cannot create sites ("Resource not accessible
      by integration" — runs 36594165235, 36596119556 both failed there), and (b) the fine-grained PAT gets
      403 on POST /pages too. This is a known fine-grained-PAT limitation — Pages administration requires a
      classic token with `repo` scope or the browser. ONE-TIME BROWSER FIX (~30 s, Play Console needs this URL):
      Repo Settings → Pages → Build and deployment → Source: **GitHub Actions** (save).
      Then: Actions → Pages workflow → Run workflow (branch main).
      Verify: https://sunpaws360-afk.github.io/SunPlayer/privacy.html returns 200 (Play Console privacy URL).
- [ ] Environment protection rule: Required reviewers → add yourself (Settings → Environments → release).
- [ ] Back up keystore + passwords to a password manager / offline encrypted drive NOW.
- [ ] Cut first release once secrets exist:
      git tag v1.3.0.1 && git push origin v1.3.0.1   (tag already exists locally — delete & re-push, or just Re-run failed jobs for run 36589284944)
      Approve the deployment wait in browser → Release appears with APK + AAB + SHA256SUMS.
- [ ] Verify: sha256sum -c SHA256SUMS.txt; apksigner verify --print-certs shows A9:E4:96:53... (not the debug cert).
- [ ] Revoke the short-lived PAT when the project wraps up; then wipe sandbox key files per user decision.
