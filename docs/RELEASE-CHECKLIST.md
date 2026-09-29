# Release Setup Checklist (one-time)

Status as of 2026-09-29:
- [x] Release keystore generated (alias `sunplayer_release`, RSA-2048, valid until 2054-02-14)
      Files live ONLY in the task sandbox: the sandbox keystore export files (gitignored; see docs/DISTRIBUTION.md for storage rules). Cert SHA-256 fingerprint starts A9:E4:96:53.
- [x] GitHub environment `release` created
- [x] Secret scanning + Push Protection enabled
- [x] Branch protection on `main` (require PR, strict status checks, no force-push/delete)
- [ ] **Four secrets in the `release` environment** — blocked: current sandbox token lacks Actions-secret scope (403). Add manually:
      Settings → Secrets and variables → Actions → Environments → release → Add secret
      | Name | Value |
      |---|---|
      | SUNPLAYER_KEYSTORE_BASE64 | contents of sunplayer-release.keystore.base64 |
      | SUNPLAYER_KEYSTORE_PASSWORD | contents of .sunplayer-keystore-password.txt |
      | SUNPLAYER_KEY_ALIAS | sunplayer_release |
      | SUNPLAYER_KEY_PASSWORD | same password again |
      NOTE: names are SUNPLAYER_* (NOT RELEASE_* — the workflow reads SUNPLAYER_*).
- [x] GitHub Pages deploy workflow live (.github/workflows/pages.yml, configure-pages enablement:true).
      NOTE: first Pages run fails with "Create Pages site failed: Resource not accessible by integration" —
      the GITHUB_TOKEN cannot create a Pages site. One-time browser fix (~30 s):
      Repo Settings → Pages → Build and deployment → Source: **GitHub Actions**.
      Then re-run the Pages workflow (Actions → Pages → Run workflow) or push any docs/** change.
      Verify: https://sunpaws360-afk.github.io/SunPlayer/privacy.html returns 200 (Play Console privacy URL).
- [ ] Environment protection rule: Required reviewers → add yourself (Settings → Environments → release).
- [ ] Back up keystore + passwords to a password manager / offline encrypted drive NOW.
- [ ] Cut first release once secrets exist:
      git tag v1.3.0.1 && git push origin v1.3.0.1   (tag already exists locally — delete & re-push, or just Re-run failed jobs for run 36589284944)
      Approve the deployment wait in browser → Release appears with APK + AAB + SHA256SUMS.
- [ ] Verify: sha256sum -c SHA256SUMS.txt; apksigner verify --print-certs shows A9:E4:96:53... (not the debug cert).
- [ ] Revoke the short-lived PAT when the project wraps up; then wipe sandbox key files per user decision.
