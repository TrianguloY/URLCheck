# PR fixer / merger thread

## Purpose and scope

Go through every open PR of `Daniel-OS01/URLCheck` on 2026-11-24, fix failing CI and conflicts, merge what is complete, green and relevant, and leave the rest open with a reason. A follow-up brief from the parent thread put link interception first: review PRs touching link handling before anything else, and check that URLCheck can never pick itself as the target browser (a loop when it is the default browser).

Rules followed: never force-push `master`, merge with `--merge` (keeps original authors), small fixes first, no release/tag/version changes, no secrets.

## Results per PR

| PR | What was wrong | What was changed | Result |
|---|---|---|---|
| [#26](https://github.com/Daniel-OS01/URLCheck/pull/26) keep dialog state on theme/language/rotation | Draft, but the work was complete and reviewed | Marked ready | Merged `7abeead` |
| [#43](https://github.com/Daniel-OS01/URLCheck/pull/43) meta refresh / JS redirect detection | Read the whole response body without a limit and followed any redirect target scheme | `StatusModule`: read timeout, body read only for `text/html` / `application/xhtml` and capped at 64 KB, precompiled patterns, redirect targets limited to http/https (`resolveHtmlRedirectUrl`) | Merged `bd30b6e` |
| [#42](https://github.com/Daniel-OS01/URLCheck/pull/42) skip redirect module + French update | Conflict in `strings.xml`; unbounded recursion in `SkipRedirectUtility.skip` | Kept both string blocks; added `MAX_DEPTH = 10` recursion cap | Merged `be93032` |
| [#48](https://github.com/Daniel-OS01/URLCheck/pull/48) shorten fr-FR / ta-IN fastlane descriptions | Nothing (from the issue fixer thread) | - | Merged `60cf889` |
| [#49](https://github.com/Daniel-OS01/URLCheck/pull/49) Check status connects to the remote site | Nothing (text only) | - | Merged `d81dda4` |
| [#50](https://github.com/Daniel-OS01/URLCheck/pull/50) labels for EditText fields | Behind master; its branch was checked out in another worktree, so `git checkout` of it failed | Merged master on a temporary branch and pushed with `git push origin HEAD:refs/heads/fix/issue-4-edittext-labels` | Merged `68737e7` |
| [#45](https://github.com/Daniel-OS01/URLCheck/pull/45) deny browser in open/share | Conflict in `ShareUtility` with the refactored `shareUrl(packageName, componentStr)` from #47; then CI compile errors: `GenericPref.Enumeration` does not exist, `var x = null` cannot infer a type | Took master's `ShareUtility` and added only `hideDeniedApp` (uses `EXTRA_EXCLUDE_COMPONENTS`, API 24+); `DENYLIST_PREF` now an `EnumerationPref<DenylistOption>`; explicit `String deniedPackage` | Merged `3d8c759` |
| [#51](https://github.com/Daniel-OS01/URLCheck/pull/51) never open or share to URLCheck itself | New, from the self-target check below | `OpenModule.openUrl(component, package)` and `ShareUtility.shareUrl` refuse targets that are URLCheck itself (or an implicit VIEW intent) with the `toast_noApp` toast | Merged `5bdfbc8` |
| [#24](https://github.com/Daniel-OS01/URLCheck/pull/24) user-defined custom modules | ~950 new lines that send URLs to user-configured servers; later conflicted in `strings.xml` | `CustomModule.request`: http/https only, method allowlist (GET, POST, PUT, PATCH, DELETE, HEAD), request body max 64 KB, response read max 256 KB, no automatic redirects. Merged master (kept both `mCustom_*` and `mSkip_*` string blocks) | Open, `build` green on `29d9b20`; needs Daniel's decision |
| [#36](https://github.com/Daniel-OS01/URLCheck/pull/36) replace module titles with buttons | Draft, conflicts with master, user-facing design change | - | Open |
| [#46](https://github.com/Daniel-OS01/URLCheck/pull/46) new resource file system | Draft, author says untested, large refactor, far behind master, conflicts | - | Open |
| [#52](https://github.com/Daniel-OS01/URLCheck/pull/52) URLBlocklist + Direct Open (from fork oooppet) | Lint `NewApi` errors at minSdk 19 (`ArrayList#sort`, `Comparator.comparing`, `codePoints().anyMatch`) | Fixed by the fork-scan thread in `09abf81` (same fix as found here: `Collections.sort` with a lambda, a plain loop for whitespace). Reviewed here, see below | Merged `a2dc6e7` (pinned to `09abf81`) |

Merge order: #26, #43, #42, #48, #49, #50, #45, #51, #52 (small fixes first, then features). The `Validate gradle build test` run on `master` passed on `3d8c759` (after #45, which includes all earlier merges) and on `5bdfbc8` (after #51).

## Link interception check (URLCheck must never target itself)

Read on `master` after #45 and #42:
- `IntentApp.getOtherPackages` drops URLCheck's own package, so the open button, the "open with" popup and the deny-browser filter never list it. The open button always launches an explicit component.
- Gap (fixed in #51): the `open` automation passed its `package` / `component` arguments straight through. A rule naming URLCheck itself, or a component string that cannot be parsed and no package, produced a self-targeted or implicit VIEW intent; with URLCheck as default browser this relaunches it, reruns the automations and loops. The `share` automation had the same problem with a self target.
- Not changed: URLCheck can still appear in its own manual share chooser. That is a user choice, not a loop.

Review notes for #52 (Direct Open, URLBlocklist), useful when deciding on it:
- Both features are off by default. Direct Open refuses URLCheck's own package, only accepts general browsers (http and https, no data authorities, enabled, exported) and launches an explicit component, so it cannot loop either. If launching fails, the normal dialog opens with a warning and automations paused.
- With Direct Open on, every allowed http/https link skips the dialog (no checks at all); that is the feature.
- Behaviour changes in `MainDialog`: a new intent without links now closes an open dialog (before: toast, dialog kept); a blocked URL closes silently (the PR text mentions a toast, the code has none); a missing-module `assertError` was removed.

## Decisions for Daniel

- **#24 custom modules**: do you want a feature that sends URLs to servers you configure? It is hardened (see table) and builds, but was never tested on a device. Should automations be able to trigger it without confirmation?
- **#36**: replace module titles with buttons or not; if yes it needs rebasing onto master.
- **#46**: close, or ask the author to rebase and test.
- **#45 limitation**: on Android below 7 the denied browser still shows in the share chooser; the open list hides it on all versions.
- **#52**: whether Direct Open (skip the dialog entirely for normal links) fits how you want to use URLCheck as default browser.

## Repo-specific gotchas

- `app/src/main/res/values/strings.xml` conflicts on almost every feature PR because new module strings are appended near the same place. Resolve by keeping both blocks, then check there are no duplicate names (`translators` appears twice on master because one is inside a comment; that is fine).
- The CI job that matters is `build` in workflow `Validate gradle build test` (`./gradlew build test`, which includes lint). `Codacy Security Scan`, `Analyze (java)` (CodeQL), `triage` and CodeRabbit are not required; merging before they finish is fine.
- Lint aborts on the first error and the log shows only that one (`Lint found 4 errors ... First failure:`); there is no uploaded lint report. With `minSdkVersion 19` and no core library desugaring, instance methods added in Java 8 (`List#sort`, `CharSequence#codePoints`, streams) fail `NewApi`; static factories like `List.of` / `Set.of` are fine (backported by D8).
- Pin merges to the head you checked: `gh pr merge N --merge --match-head-commit <sha>` (or `pull_request_merge` with `expectedHeadSha`). Other threads push to the same PRs; #52 got a fix from another thread while it was being reviewed here.
- After each merge `mergeable` shows `UNKNOWN` for ~20 s, then `CONFLICTING` for the newly affected PRs.
- Test-APK bot comments ("This PR builds correctly, here is the generated apk") mean `build` passed for that head.

## Commands that worked on Windows (PowerShell)

```powershell
# failing CI errors (ci_logs tail often misses them)
gh run view <run-id> -R Daniel-OS01/URLCheck --log-failed | Select-String -Pattern 'error:|Error: |What went wrong' -Context 0,2

# state of a PR in one line
gh pr view <N> -R Daniel-OS01/URLCheck --json mergeable,headRefOid,statusCheckRollup --jq '.statusCheckRollup[] | select(.name=="build") | "\(.status) \(.conclusion)"'

# merge pinned to the checked head
gh pr merge <N> -R Daniel-OS01/URLCheck --merge --match-head-commit <sha>

# update a PR branch that is checked out in another worktree
git checkout -b tmp/merge origin/<branch>; git merge origin/master; git push origin HEAD:refs/heads/<branch>

# duplicate string names after a strings.xml merge
Select-String -Path app/src/main/res/values/strings.xml -Pattern 'name="([^"]+)"' -AllMatches | % { $_.Matches } | % { $_.Groups[1].Value } | Group-Object | ? Count -gt 1
```

Pitfalls:
- `git checkout <missing-branch>` fails but the shell stays on the current branch; check `git branch --show-current` before editing.
- `git show rev:path > file` in Windows PowerShell 5 rewrites the encoding; write files with `[IO.File]::WriteAllText(path, text, (New-Object Text.UTF8Encoding $false))` instead.
- No JDK/Android SDK on this PC: compile and lint only run in CI.
