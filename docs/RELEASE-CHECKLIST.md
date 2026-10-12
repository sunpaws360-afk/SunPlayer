# Release Setup Checklist (one-time)

Release setup:
- [x] GitHub environment `release` created
- [x] Four release-signing secrets added to the `release` environment:
      `SUNPLAYER_KEYSTORE_BASE64`, `SUNPLAYER_KEYSTORE_PASSWORD`,
      `SUNPLAYER_KEY_ALIAS`, and `SUNPLAYER_KEY_PASSWORD`.
- [x] Secret scanning + Push Protection enabled
- [x] Branch protection on `main` (require PR, strict status checks, no force-push/delete)
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
- [ ] Confirm the `release` environment protection rules allow an authorized reviewer to approve deployment. If self-review is prevented, a different reviewer must approve; do not disable that protection merely to unblock a run.
- [ ] Back up keystore + passwords to a password manager / offline encrypted drive NOW.
- [ ] Push the matching `v<versionName>` tag after the release is ready. The tagged workflow requires all four secrets and fails rather than publishing unsigned artifacts. Approve any pending deployment in GitHub Actions with an authorized reviewer.
- [ ] Verify: sha256sum -c SHA256SUMS.txt; apksigner verify --print-certs shows A9:E4:96:53... (not the debug cert).
- [ ] Revoke the short-lived PAT when the project wraps up; then wipe sandbox key files per user decision.
