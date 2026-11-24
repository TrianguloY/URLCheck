# Fork Sync Playbook — recommendations for future clones

Living file. Updated during the URLCheck run (2026-11-24). Reuse for other repos: replace OWNER/REPO.

## 0. Pre-flight (do first, ~2 min)
- `gh repo view --json isFork,parent` → confirm the fork/parent relationship.
- `git remote add upstream <parent>`; `git fetch upstream`; `git rev-list --left-right --count origin/master...upstream/master` → sync master first if behind.
- Check the fork's **Issues enabled** (forks default to disabled) → `gh repo edit --enable-issues`.
- Check **Actions enabled** on the fork (forks often need a manual enable) → `gh api repos/OWNER/REPO/actions/permissions`.
- Check **secrets** needed by release workflows (`grep secrets. .github/workflows/*`) → forks never inherit secrets. Decide on signing before promising a release.
- Check hardcoded upstream identity in workflows/notes (cert hashes, package names, bot names, FUNDING.yml, codacy/codeql tokens).

## 1. `.github/` copy
- `git diff HEAD upstream/master -- .github` first; a synced fork already has it.
- Otherwise: `git checkout upstream/master -- .github`, then review FUNDING.yml / scheduled workflows (asset-updater crons will open PRs on your fork weekly).

## 2. Issues
- Define "new and relevant" explicitly (date window + label exclusions such as `wontfix`, `duplicate`, `question`).
- Recreate labels first (`gh label clone UPSTREAM/REPO -R OWNER/REPO`).
- Body template: original body + footer `Upstream: UPSTREAM#N by <author>, <date>`; avoid live @-mentions (zero-width space) to avoid pinging people.
- Idempotent: search the fork for the `Upstream: UPSTREAM#N` marker before creating.
- Secondary rate limit: sleep ~3–5 s between creations.

## 3. Mirroring open PRs
- `git fetch upstream pull/N/head:upstream-pr/N` works for any PR, including from deleted forks.
- Push the branch to the fork and open a PR against the fork's default branch; keep the draft state; credit the author in the body.
- Note: CI on PRs created from the same repo runs with the repo's secrets — review workflow-changing PRs before opening.

## 4. Fork scan
- `pushed_at` is noisy (sync pushes). Use `gh api repos/UPSTREAM/compare/master...FORKOWNER:BRANCH` → `ahead_by`, `files`.
- Skip forks whose commits already exist as upstream PRs (match SHAs/branch names).
- Parallelize read-only triage across subagents; one owner applies results.

## 5. Merge policy
- Only merge: non-draft + CI green + conflict-free + relevant.
- Merge small fixes first, then features; re-run CI after each merge (overlapping UI changes).

## 6. Release
- Bump versionCode/versionName and add a fastlane changelog for the new versionCode.
- Tag push triggers the release; verify the asset and `apksigner verify --print-certs`.

## Lessons from runs
- URLCheck: fork was already even with upstream and `.github` identical → step 1 was a no-op; Issues were disabled; no signing secrets.

### URLCheck run, 2026-11-24 (Windows PC12700K) - additional lessons
**Windows / PowerShell 5**
- `winget install ... JDK` can hang with `msiexec` waiting on a UAC prompt in a non-interactive session. Fallback: download the portable zip `https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse`, `Expand-Archive` to a tools dir and call its `bin\keytool.exe` directly (no install, no admin).
- Native-arg quoting: PS 5 mangles embedded double quotes in args (`gh issue create --title 'a "b" c'` drops or splits them). For titles/bodies with quotes use `gh api ... --input file.json` (build JSON with `ConvertTo-Json`, write UTF-8 without BOM). `--body-file` is fine for bodies.
- `gh ... --search "\"marker\" in:body"` breaks the same way; for idempotency list all fork issues/PRs once with `--json number,url,body` and match the marker locally with `-like`.
- `git fetch <remote> <short-sha>` fails; use the full 40-char SHA. Commits reachable only from other forks CAN be fetched by full SHA from the parent remote (shared object store), no extra remote needed.
- `git checkout` of a branch name that does not exist silently leaves you on the previous branch, so a following `git merge` hits the wrong branch. Check `git branch --show-current` before merging; local mirror branches are named by the UPSTREAM PR number, not the fork PR number.
- `keytool ... -storepass:env VAR` keeps passwords out of the process list; generate passwords with `RandomNumberGenerator`, pipe to `gh secret set` via stdin, store the backup under `~\Backups\<name>` and lock it with `icacls <dir> /inheritance:r /grant:r "$env:USERDOMAIN\$env:USERNAME:(OI)(CI)F"`.
- With `core.autocrlf=true`, writing files with LF is fine (git warns and normalizes). Use `[IO.File]::WriteAllText(path, text, New-Object Text.UTF8Encoding($false))` to avoid a BOM.

**GitHub Actions on a fresh fork**
- A fork's workflows are not registered until a push touches them; `gh api repos/O/R/actions/workflows` returned `[]` at first. `push`/`pull_request` events from before that moment never run. After registration I still saw zero PR/push runs until I (a) ran one `workflow_dispatch` workflow and (b) re-PUT `actions/permissions enabled=true`; after that, new pushes triggered runs. So: push a workflow change to master, dispatch one workflow, re-PUT permissions, then **close+reopen** every mirrored PR (or push a commit) to start its CI.
- Mirroring 23 PRs queues 23 x (Validate + CodeQL + Codacy) runs; GitHub drains about 1 PR/min. Merge in waves as results arrive instead of waiting for all.
- `gh workflow run alpha.yml` on a fork fails at the end (`release not found` for the `alpha` release) and creates an `alpha` tag; delete it (`gh api -X DELETE repos/O/R/git/refs/tags/alpha`) and do not use it as a smoke test.
- Merge with `gh pr merge N --merge` to keep original commits and authors (squash re-attributes). After each merge `mergeable` flips to UNKNOWN for ~20 s, then shows new CONFLICTING PRs: resolve small ones locally (merge master into the PR branch, push), skip big ones. Verify master CI after each wave (concurrency cancels older master runs; only the last matters).

**Mirroring content**
- Neutralise `@` with a zero-width space AND `#N` (insert U+200B after `#`) in mirrored PR bodies, otherwise `Fixes #N` closes/links unrelated fork issues. The mandatory `Upstream: OWNER/REPO#N by author, date` footer is a real cross-reference and shows up on the upstream thread as a backlink; keep it only if that is acceptable.
- Use `gh api repos/O/R/pulls --input json` with `draft=true` to keep drafts.

**Triage heuristics that worked**
- A fork 'ahead' by 1 commit can already be in upstream under a different SHA (snigdhasjg: user-CA trust config). Check the file exists on the parent master before cherry-picking; an empty cherry-pick means that.
- Skip: forks that ZIP-replace the root, rebrand (new package/icons/colours), personal CI/signing churn, README-only edits, deleted forks (404).
- Do not merge unreviewed large features that add outbound requests or unbounded body reads (custom-modules PR with HTTP actions, JS-redirect scraper reading the full body without limit); leave them open for upstream review.
- Merge-order that worked: bot asset PR, one-line fixes, label/a11y/UI fixes, features, translation PR (Weblate, 131 files) last-ish, then flag-search layout PR after resolving its conflict with the content-description PR.
