# Process and decisions (2026-11-24 run)

Goal: bring the fork `Daniel-OS01/URLCheck` up to date with everything useful around `TrianguloY/URLCheck` (issues, open PRs, work in other forks) and publish a signed `v4.0.0` APK.

## Starting state
- `origin` = Daniel-OS01/URLCheck, `upstream` added = TrianguloY/URLCheck. `git rev-list --left-right --count origin/master...upstream/master` = `0 0` (even, at `ca3bb00`).
- `.github/` (14 files) already byte-identical to upstream. Fork had Issues disabled, no repo secrets, Actions permission enabled but no workflows registered/run.
- Upstream inventory: 111 open issues (24 created since 2025-11-24, 23 excluding `wontfix`), 23 open PRs, 42 forks pushed since 2026-06-01.
- Version: `versionCode 47`, `versionName "3.5"`; `release.yml` runs on `v*` tags and needs secrets `RELEASE_STORE_FILE` (base64), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`; without them `assembleRelease` is unsigned.

## Decisions
| Id | Decision |
|---|---|
| D1(a) Signing | Generate a NEW release keystore (RSA 4096, 100-year validity), store it as the four repo secrets, keep an ACL-restricted offline backup outside the repo, replace the hard-coded upstream cert SHA-256 in `release.yml` release notes with the new one |
| D2(a) Issues | Enable Issues on the fork, clone labels, copy open upstream issues created on/after 2025-11-24 excluding `wontfix` (23), footer `Upstream: TrianguloY/URLCheck#N by <author>, <date>`, neutralise @-mentions with a zero-width space, idempotent via the marker, 3-5 s between creations |
| D3(a) PRs | Mirror all open upstream PRs: fetch `pull/N/head` -> branch `upstream-pr/<N>-<slug>` -> push to origin -> PR against master (draft if upstream is draft) with original body, attribution and marker |
| Merge policy | Merge only PRs that are non-draft, pass `Validate gradle build test`, are conflict-free and relevant; small fixes first, features later; merge with `--merge` (keeps original commits/authors); verify master CI after each wave |

## Phases executed
1. **Preflight** - gh auth ok; upstream remote added; JDK 17 obtained (portable zip, winget hung); see playbook for the Windows notes.
2. **Issues** - 23 issues created as fork issues #1-#23.
3. **PR mirror** - 23 upstream PRs mirrored as fork PRs #24-#46.
4. **Release prep** - keystore + 4 secrets, `release.yml` fingerprint, pushed to master. This push also registered the workflows.
5. **Fork scan (round 1)** - 42 forks compared on default branch (+2 named branches). One accepted: share-automation `package`/`component` arguments from `8099bb4X21` (fork PR #47). See [maps-and-fork-scan.md](maps-and-fork-scan.md).
6. **CI + merge** - 17 PRs merged in waves; conflict in the flags-search PR resolved by merging master into its branch. See [pull-request-status.md](pull-request-status.md).
7. **Release** - bump to 4.0.0 / 48, changelog `48.txt`, tag `v4.0.0`; asset and signature verified. See [release-and-signing.md](release-and-signing.md).

## Errors encountered and resolutions
| Error | Resolution |
|---|---|
| Fork Issues disabled | `gh repo edit --enable-issues` |
| winget JDK install hung on a UAC prompt | portable Temurin 17 zip, call its `keytool` directly |
| PowerShell 5 dropped double quotes in `gh issue create --title` | create via `gh api --input file.json`; fixed the title of issue #21 and created #23 this way |
| Workflows list empty / no push or pull_request runs on the fresh fork | push a workflow change, run one `workflow_dispatch`, re-PUT `actions/permissions enabled=true`, close+reopen every PR |
| `workflow_dispatch` of `alpha.yml` failed at the end (no `alpha` release) and left an `alpha` tag | tag deleted; do not use as a smoke test |
| PR conflicts appearing after earlier merges (`mergeable` goes UNKNOWN -> CONFLICTING) | merge master into the PR branch, resolve, push; skip big ones |
| `git checkout` of a non-existing branch silently stayed on master | always check `git branch --show-current`; mirror branches use the UPSTREAM PR number |

## Follow-up work (parallel threads, same day)
Issue fixer, PR fixer/merger, second fork scan, settings-backup updater and the link-interception investigation. Their outputs live in [threads/](threads/) and [link-interception.md](link-interception.md).
