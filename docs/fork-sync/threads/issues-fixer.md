# Thread report: Fix easy open issues (`issues-fixer`)

## Purpose and scope

Review all 23 open issues on `Daniel-OS01/URLCheck` (`#1`–`#23`, mirrored from `TrianguloY/URLCheck`), filter out issues already fixed on `origin/master` or covered by open PRs, identify small, well-defined fixes (fastlane metadata, UI copy clarity, accessibility labels), implement each on its own `fix/issue-<N>-<slug>` branch off `origin/master`, and open PRs against `master` without merging them directly.

Base commit inspected: `origin/master` at `b0213d6` (`Release 4.0.0 (versionCode 48)`).

---

## Full issue triage table

| Fork # | Upstream # | Title | Easy? | Already fixed / covered? | Action / PR | Reason / notes |
|---:|---:|---|---|---|---|---|
| [#1](https://github.com/Daniel-OS01/URLCheck/issues/1) | #542 | Make the status module perform all redirections at the same time | No | Partially overlaps open [PR #43](https://github.com/Daniel-OS01/URLCheck/pull/43) | Skipped | Feature request requiring design decision (option in Status module vs. new module). |
| [#2](https://github.com/Daniel-OS01/URLCheck/issues/2) | #548 | Add support for the .ts format | No | No | Skipped | Needs design decision on which video MIME types (`video/mp2t`, `.m3u8`, etc.) to add to `AndroidManifest.xml` without over-intercepting intents; exact intent payload not verified. |
| [#3](https://github.com/Daniel-OS01/URLCheck/issues/3) | #552 | Accessibility issues | No | Sub-items split into #4, #5, #6 | Skipped | Umbrella issue proposing Material `TextInputLayout` (app has no Material Components dependency). |
| [#4](https://github.com/Daniel-OS01/URLCheck/issues/4) | #553 | Avoid relying on EditText's hints | **Yes** | No | **Fixed** — [PR #50](https://github.com/Daniel-OS01/URLCheck/pull/50) (merged) | Added persistent `TextView` labels + `android:labelFor` to `EditText` fields. |
| [#5](https://github.com/Daniel-OS01/URLCheck/issues/5) | #555 | Proper TextView Labeling | No | Partially addressed by #4 | Skipped | Setting `contentDescription` on normal `TextView`s overrides their displayed text for TalkBack; needs clarification on which dynamic/non-text views (if any) need labels. |
| [#6](https://github.com/Daniel-OS01/URLCheck/issues/6) | #556 | Missing Labels on buttons | Yes | **Yes** — merged [PR #39](https://github.com/Daniel-OS01/URLCheck/pull/39) (`a3b83ec`, upstream #610) | Skipped | Already fixed on `master` (resolved all 24 Android lint `ContentDescription` warnings). |
| [#7](https://github.com/Daniel-OS01/URLCheck/issues/7) | #558 | Skip redirect | No | **Yes** — open [PR #42](https://github.com/Daniel-OS01/URLCheck/pull/42) (upstream #606) | Skipped | Already covered by open PR #42; also a full new module. |
| [#8](https://github.com/Daniel-OS01/URLCheck/issues/8) | #559 | Feature suggestion: Modify the list of default browsers. | No | No | Skipped | Feature request requiring new settings UI and data model. |
| [#9](https://github.com/Daniel-OS01/URLCheck/issues/9) | #565 | Custom Patterns Clarity and Update | No | No | Skipped | Refers to the upstream GitHub Wiki (`Custom-patterns`), not files in this repository (`VxTikTok` / `InstaFix` do not appear in the repo). |
| [#10](https://github.com/Daniel-OS01/URLCheck/issues/10) | #576 | Feature request: Like the LibRedirect browser plugin | No | No | Skipped | Large feature request (frontend instance rotation). |
| [#11](https://github.com/Daniel-OS01/URLCheck/issues/11) | #579 | Check status: make it immediately obvious that it will access the remote site to be checked | **Yes** | No | **Fixed** — [PR #49](https://github.com/Daniel-OS01/URLCheck/pull/49) (merged) | Updated `mStatus_check` and `mStatus_desc` in `strings.xml`. |
| [#12](https://github.com/Daniel-OS01/URLCheck/issues/12) | #583 | Open with options not showing | No | No | Skipped | Intermittent OneUI 5.1 bug report with insufficient reproduction steps. |
| [#13](https://github.com/Daniel-OS01/URLCheck/issues/13) | #584 | Route Intermediate Hops through Tor | No | No | Skipped | Large networking/proxy feature. |
| [#14](https://github.com/Daniel-OS01/URLCheck/issues/14) | #586 | Feature request: More distinguishable font or feature to change fonts | No | Partial (`mInput_monospace` exists) | Skipped | Requires font bundling/licensing and UI design; monospace toggle already exists in Input module. |
| [#15](https://github.com/Daniel-OS01/URLCheck/issues/15) | #589 | minor fastlane issue | **Yes** | No | **Fixed** — [PR #48](https://github.com/Daniel-OS01/URLCheck/pull/48) (merged) | Shortened `fr-FR` and `ta-IN` `full_description.txt` well below 4,000 chars. |
| [#16](https://github.com/Daniel-OS01/URLCheck/issues/16) | #590 | Exported BackupActivity lets any app/website trigger a one-tap settings restore | No | No | Skipped | Security-sensitive hardening/UX redesign (explicitly out of scope for quick fixes). |
| [#17](https://github.com/Daniel-OS01/URLCheck/issues/17) | #597 | Show Open Graph, Show Server-type | No | No | Skipped | Large feature request (HTML/header parsing, metadata display, app routing). |
| [#18](https://github.com/Daniel-OS01/URLCheck/issues/18) | #598 | Special handling for WMF sites | No | No | Skipped | Feature request for new multi-domain / app routing rules (Kiwix, Wikimedia wikis). |
| [#19](https://github.com/Daniel-OS01/URLCheck/issues/19) | #599 | special handling of Youtube links and links to peertube, other sites when Youtube-Video-ID is found | No | No | Skipped | Large feature request (local filesystem search + Wikimedia/Archive/PeerTube/Invidious queries). |
| [#20](https://github.com/Daniel-OS01/URLCheck/issues/20) | #607 | Alternative to ClearURL | No | No | Skipped | Product/design decision on choosing an alternative rule catalog. |
| [#21](https://github.com/Daniel-OS01/URLCheck/issues/21) | #626 | Add support for "extras" action argument in the automations JSON | No | No | Skipped | Medium feature requiring JSON schema/type design, validation, and documentation. |
| [#22](https://github.com/Daniel-OS01/URLCheck/issues/22) | #628 | Add an option to redirect to archive and remove paywal sites? | No | No | Skipped | Feature/preset request for a third-party service (already achievable via Custom Patterns or Redirector). |
| [#23](https://github.com/Daniel-OS01/URLCheck/issues/23) | #569 | To add a "just once", "always" option like android app handling | No | No | Skipped | Large feature request (per-domain/per-app remembered handler + settings UI). |

---

## Fixed issues — details

### 1. Issue #15 — Shorten `fr-FR` and `ta-IN` fastlane full descriptions

- **PR**: [PR #48](https://github.com/Daniel-OS01/URLCheck/pull/48) (branch `fix/issue-15-fastlane-description-length`, initial commit `4ce31f6`, merged as `b252012`)
- **Root cause**: `fastlane/metadata/android/fr-FR/full_description.txt` (3,992 raw UTF-8 chars) and `fastlane/metadata/android/ta-IN/full_description.txt` (3,915 raw UTF-8 chars) exceeded the 4,000-character Play Store / IzzyOnDroid limit once line endings / HTML conversion were applied (IzzySoft reported 4,079 and 4,006 chars respectively and recommended staying around ~3,700 chars).
- **Files touched**:
  - `fastlane/metadata/android/fr-FR/full_description.txt` (trimmed to 2,913 chars; also fixed minor typo "Elle est gratuit" -> "Elle est gratuite")
  - `fastlane/metadata/android/ta-IN/full_description.txt` (trimmed to 2,500 chars)
- **Verification**: Verified UTF-8 character counts locally via PowerShell; `Validate gradle build test` (`build` job) passed in GitHub Actions and PR was merged.

### 2. Issue #11 — Clarify that Check status connects directly to the remote site

- **PR**: [PR #49](https://github.com/Daniel-OS01/URLCheck/pull/49) (branch `fix/issue-11-status-check-remote-warning`, initial commit `8ac27fc`, merged as `9607955`)
- **Root cause**: Unlike other network-accessing modules updated in merged PR #29 (`Check on VirusTotal`, `Unshorten via unshorten.me`), the Status code module button still read only `Check status`, which did not immediately warn users that pressing it sends an HTTP GET request directly to the remote URL.
- **Files touched**:
  - `app/src/main/res/values/strings.xml` — updated `mStatus_check` to `Check status (connects to site)` and tightened `mStatus_desc` to explicitly mention connecting directly to the remote site (HTTP GET) and avoiding use on untrusted links.
- **Verification**: Verified `R.string.mStatus_check` call sites (`StatusModule.java` and legacy migration in `VersionManager.java`), checked XML well-formedness locally, and CI `build` passed; PR was merged.

### 3. Issue #4 — Add persistent `TextView` labels and `android:labelFor` to `EditText` fields

- **PR**: [PR #50](https://github.com/Daniel-OS01/URLCheck/pull/50) (branch `fix/issue-4-edittext-labels`, initial commit `88b5e1d`, merged as `362f79d`)
- **Root cause**: Several configuration and editor `EditText` fields relied solely on `android:hint`, which disappears once text is entered, leaving users without a persistent visual or TalkBack label.
- **Files touched**:
  - `app/src/main/res/layout/config_webhook.xml` — added `TextView` labels with `labelFor` for `webhook_url` and `webhook_body`
  - `app/src/main/res/layout/config_virustotal.xml` — added `TextView` label with `labelFor` for `api_key`
  - `app/src/main/res/layout/config_unshorten.xml` — added `TextView` label with `labelFor` for `token`
  - `app/src/main/res/layout/flags_editor.xml` — added `TextView` label with `labelFor` for `search` (reusing `@string/mFlag_search`)
  - `app/src/main/res/layout/config_redirect_row.xml` — added `TextView` labels with `labelFor` for `name`, `from`, `to` and replaced hardcoded `"Name"` hint with `@string/mRedir_ruleName`
  - `app/src/main/res/layout/config_log.xml` — added `android:labelFor="@id/limit"` to existing adjacent label
  - `app/src/main/res/layout/config_clearurls_updater.xml` — added `android:labelFor` to existing `catalog_URL` and `hash_URL` labels
  - `app/src/main/res/values/strings.xml` — added `<string name="mRedir_ruleName">Name</string>`
- **Verification**: Confirmed `RedirectVisualEditor.java` looks up views by `R.id.*` (no fragile child-index assumptions inside each row), validated all 8 modified XML files with PowerShell's XML parser, and CI `build` passed; PR was merged.

---

## Codebase structure and pitfalls

- **Module architecture**: Each module lives under `app/src/main/java/com/trianguloy/urlchecker/modules/list/` and splits into `AModuleData` (metadata/prefs), `AModuleDialog` (in-dialog UI, `dialog_*.xml`), and `AModuleConfig` (settings screen, `config_*.xml`). Companion helpers/catalogs live under `modules/companions/`.
- **Preferences binding**: `GenericPref` (`BoolPref`, `IntPref`, `StringPref`) provides `.attachToSwitch(...)` and `.attachToEditText(...)` helpers, so layout IDs (`@id/limit`, `@id/webhook_url`, etc.) are bound directly in `onInitialize(View)`.
- **Strings & translations**:
  - `app/build.gradle` disables the `MissingTranslation` lint check because translations come from Weblate and lag behind `values/strings.xml`. Adding or updating English strings in `values/strings.xml` does not break lint.
  - Check all references before changing existing string keys: `R.string.mStatus_check` is also used by `VersionManager.java` when migrating the legacy `statusCode_autoCheck` preference into an automation entry name.
- **Accessibility conventions**:
  - `minSdkVersion` is `19`, so `android:labelFor` (API 17+) is natively supported with no compat wrapper or lint warning.
  - Avoid setting `android:contentDescription` on normal `TextView`s that display dynamic text (it overrides the text read by TalkBack). Use `android:labelFor` on a label `TextView` pointing at an `EditText` instead.
  - Do not put `android:labelFor` on a clickable toggle header like `dialog_flags.xml`'s `overflowButton`.
  - Button `contentDescription` warnings were already cleaned up in merged PR #39 (`1fd6f83`).
- **Manifest intent filters**: `AndroidManifest.xml` separates scheme-only `http`/`https` `VIEW` filters from MIME-typed `VIEW` filters (`text/html`, `text/plain`, `application/xhtml+xml`). On Android, an intent carrying a MIME type will *not* match a scheme-only filter; adding new MIME types must be done carefully to avoid intercepting unrelated media/file intents.

---

## Commands that worked on Windows (PowerShell)

```powershell
# List all open fork issues with full bodies
gh issue list -R Daniel-OS01/URLCheck --state open --limit 100 --json number,title,body |
    ConvertFrom-Json | Sort-Object number

# Check character counts of all Fastlane full_description.txt files
Get-ChildItem fastlane/metadata/android/*/full_description.txt | ForEach-Object {
    $c = Get-Content -Raw -Encoding UTF8 $_.FullName
    [PSCustomObject]@{ Locale = $_.Directory.Name; Chars = $c.Length }
} | Sort-Object Chars -Descending | Format-Table -AutoSize

# Local XML well-formedness check (no Android SDK needed)
[void][xml](Get-Content -Raw -Encoding UTF8 app/src/main/res/values/strings.xml)

# Open a PR safely without PowerShell 5 quote mangling
$payload = @{ title = "..."; head = "fix/..."; base = "master"; body = "...`n`nFixes #N" } | ConvertTo-Json -Depth 3
$tmp = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($tmp, $payload, (New-Object System.Text.UTF8Encoding $false))
try {
    gh api repos/Daniel-OS01/URLCheck/pulls --input $tmp --jq '{number: .number, html_url: .html_url, sha: .head.sha}'
} finally {
    Remove-Item $tmp -Force
}
```

---

## Ideas for next fixes and issues needing a design decision

- **Good next candidates (medium effort, clear scope)**:
  - **[#21](https://github.com/Daniel-OS01/URLCheck/issues/21) (`extras` in share automation JSON)**: Builds cleanly on merged [PR #47](https://github.com/Daniel-OS01/URLCheck/pull/47) (`package`/`component` support in `Share` automation). Needs a small decision on supported value types (`string`, `int`, `boolean`, etc.) and error handling.
  - **[#16](https://github.com/Daniel-OS01/URLCheck/issues/16) (`BackupActivity` hardening)**: Well-analyzed in the issue itself; needs owner decision on whether to drop `BROWSABLE`, default categories off for external VIEW intents, or add an explicit external-source warning dialog.
- **Open questions for the owner**:
  - **[#2](https://github.com/Daniel-OS01/URLCheck/issues/2) (`.ts` / `.m3u8` support)**: Should URLCheck register video MIME types (`video/mp2t`, `application/vnd.apple.mpegurl`, `application/x-mpegURL`) in `MainDialog`'s `VIEW` filter?
  - **[#5](https://github.com/Daniel-OS01/URLCheck/issues/5) (TextView labeling)**: Should this be closed as superseded by PR #39 + PR #50, or are there specific icon/dynamic `TextView`s that still fail TalkBack checks on-device?
  - **[#6](https://github.com/Daniel-OS01/URLCheck/issues/6) (Missing button labels)**: Already fixed by merged PR #39 — can be closed on the fork.
  - **[#9](https://github.com/Daniel-OS01/URLCheck/issues/9) (Wiki custom patterns)**: Fork has no wiki mirroring; close as upstream-wiki-only?

---

## Lessons for next time

1. **Watch the target repository in MCP PR tools**: When a repo has an `upstream` remote pointing to `TrianguloY/URLCheck`, repo-less MCP PR tools (`pull_request_list`, `pull_request_create`) can target `TrianguloY/URLCheck` instead of `Daniel-OS01/URLCheck`. Always verify the target repo or pass `repository: "Daniel-OS01/URLCheck"` / use `gh api repos/Daniel-OS01/URLCheck/pulls`.
2. **Check merged and open PRs before touching an issue**: Cross-checking `gh pr list --state all` immediately avoided duplicating work on #6 (merged PR #39) and #7 (open PR #42).
3. **Validate XML locally before pushing**: Even without an Android SDK installed locally, PowerShell's `[xml]` parser catches malformed XML immediately before waiting on GitHub Actions.
