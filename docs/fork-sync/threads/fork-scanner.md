# Second, deeper fork scan (`fork-scanner` thread)

Scope: comprehensive audit of **all 142 forks** of `TrianguloY/URLCheck` and **every branch** on each fork (**394 branches total**, including `265` branches with `ahead_by > 0`), updating and expanding [round 1](../maps-and-fork-scan.md) (which only checked default branches of 42 forks pushed on/after `2026-06-01`, plus 2 named branches). Also executed a top-priority sweep for `[link-interception]` commits; detailed findings live in [link-interception-findings.md](link-interception-findings.md).

---

## Method

### 1. Fork & branch discovery (API calls)
- Enumerated the full fork list via `gh api --paginate "repos/TrianguloY/URLCheck/forks?per_page=100"` -> **142 forks** (`43` pushed `>= 2026-06-01` or flagged in round 1 + `99` older forks, including `8` deleted/404 or non-ancestor empty forks).
- Enumerated **all** `refs/heads/*` branches per fork via batched GraphQL `repository(owner, name) { refs(refPrefix: "refs/heads/", first: 100) { nodes { name target { oid } } } }` queries (chunked into batches of ~15 repositories), discovering **394 branches** total without hitting REST per-repo branch-list overhead.
- Compared every branch against upstream `TrianguloY/URLCheck@master` with `gh api "repos/TrianguloY/URLCheck/compare/master...OWNER:BRANCH"`, collecting `ahead_by`, `behind_by`, changed `files` (for active forks and all non-mirror older ahead branches), and commit metadata (`sha`, `author`, `message`).

### 2. Checking branches, commit comments & PR discussions
- **Commit comments & fork-local PRs/issues**: queried `repos/OWNER/REPO/commits/{sha}/comments`, `repos/OWNER/REPO/pulls?state=all`, and `repos/OWNER/REPO/issues?state=all` on key forks (`oooppet/URLCheck`, `jprapstyle/URLCheck`, `skynet-05/URLCheck`, `8099bb4X21/URLCheck`, `ottnorml/TrianguloY_URLCheck`) to ensure no follow-up warnings or review feedback were missed (only `ottnorml` had a fork PR: Copilot PR `#1` with 10 comments).
- **Upstream PR & issue discussions**: queried `repos/TrianguloY/URLCheck/issues/{N}` and `issues/{N}/comments` / `pulls/{N}` for every link-interception issue/PR (`#18`, `#47`, `#68`, `#192`, `#301`, `#320`, `#353`, `#405`, `#419`, `#494`, `#510`, `#519`, `#527`, `#532`, `#571`, `#583`) and for candidate-matching upstream PRs (`#507`, `#523`, `#543`, `#550`, `#567`, `#587`, `#604`, `#606`, `#609`, `#613`, `#619`, `#628`, `#629`).
- **Local inspection**: fetched cherry-pick candidates by full 40-char SHA (`git fetch https://github.com/oooppet/URLCheck.git <sha>`) to inspect full diffs before cherry-picking.

### 3. Detecting already-upstream commits & matching fork PRs
- **Upstream mirror branches**: matched identical tip SHAs / branch names against upstream `TrianguloY/URLCheck` branches (`shelf/PR68`, `shelf/incognito-clipboard`, `tests/no-internet-variant`, `wip/triggers`, `wip/useragent`, `wip/unshorten_token`, `wip/virustotal_v3`, `wip/json_editor_activity`, `tmp/automation-args`, `actions/assets/data.minify.json`), which account for the majority of `ahead_by > 0` branches in multi-branch forks.
- **Squashed / rebased upstream merges**: forks with small `ahead_by` counts whose commits were squashed or tweaked before upstream merge were verified against `TrianguloY/URLCheck` merged PRs and `master` files (`mvanhorn` -> `#587`, `snigdhasjg` -> `#604`, `utafrali` -> `#567`, `ez-me` -> `#550`, `kidhanis` -> `#507`, `NicolaeFericitu` -> `#543`, `lrq3000` -> 2024 backup/restore, `dperruso` -> 2022 Italian locale).
- **Matching open/merged PRs on `Daniel-OS01/URLCheck`**: listed all fork PRs (`gh pr list -R Daniel-OS01/URLCheck --state all`) and matched branch tips / `Upstream: TrianguloY/URLCheck#N` markers against fork heads (`upstream-pr/*`, `fork-pick/*`).

---

## Link interception (priority sweep)

Detailed analysis, upstream issue cross-references, and code-category audit across all 142 forks are recorded in [link-interception-findings.md](link-interception-findings.md). Summary:
- **Accepted (`[link-interception]`)**: [`oooppet/URLCheck` (`development`)](https://github.com/oooppet/URLCheck/tree/development) -> **[PR #52](https://github.com/Daniel-OS01/URLCheck/pull/52)**.
- **Real `CustomTabsService` implementation, explicit-package intent handling, extra `<intent-filter>` schemes/MIME/`ACTION_WEB_SEARCH`, Android 12+ verified-link handling, incoming clipboard/accessibility link capture**: **none found** across all 142 forks.

---

## Accepted findings

| PR on `Daniel-OS01/URLCheck` | Source fork & branch | Original author | Source SHAs (`git cherry-pick -x`) | Notes |
|---|---|---|---|---|
| **[#52](https://github.com/Daniel-OS01/URLCheck/pull/52)** `[link-interception] Add URLBlocklist and Direct Open browser forwarding (oooppet/URLCheck)` *(merged as `a2dc6e7b`)* | [`oooppet/URLCheck` (`development`)](https://github.com/oooppet/URLCheck/tree/development) | Tero Kallio | `ff7c85f9b2607615390c3e11f4869690934c39fa` -> `a7c5a965`<br>`33fcca4d813923dc1eb9e8a598463bac5f357994` -> `4f9e01bf`<br>`34275143f0dfcb300271d097c65e4e2713f0d433` -> `7807dd25` | Excluded `c6a899b541` (custom `applicationId`). Reconciled `MainDialog.java` with merged PRs `#26`/`#40`; added `d350a005` removing personal default domain `"iltalehti.fi"`; added `09abf815` replacing Java 24 APIs (`List#sort`, `Comparator.comparing`, `CharSequence#codePoints`, `IntStream#anyMatch`) with `minSdk 19`-compatible code. `Validate gradle build test` (`build`) passed on `09abf815` (`CodeQL` flagged pre-existing regex `AndroidUtils.java:293` on new `getOpenUrl(intent)` flow). |
| **[#47](https://github.com/Daniel-OS01/URLCheck/pull/47)** *(round 1, merged)* | [`8099bb4X21/URLCheck` (`patched`)](https://github.com/8099bb4X21/URLCheck/tree/patched) | `8099bb4X21` | `af9dbf06`, `c2c08ce6` | Share automation `package`/`component` intent args (remaining 11 commits in `patched` rejected). |

---

## Findings touching open PRs on `Daniel-OS01/URLCheck`

At the end of this scan, PRs `#25`-`#35`, `#37`-`#45`, and `#47`-`#54` (including our `PR #52` `a2dc6e7b`, `PR #53` `1ba32003`, and `PR #54` `1346700e`) are already merged into `origin/master` (`1346700e`). Only three mirrored upstream PRs remain open (`#24`, `#36`, `#46`):

| Open fork PR | Source / matching fork branch | Notes |
|---|---|---|
| [#46](https://github.com/Daniel-OS01/URLCheck/pull/46) (`upstream-pr/397`, draft) | `PabloOQ/UrlChecker [resource-files-update]` (`846988a3`) | Exact same snapshot as upstream `#397`; no newer fork commits or fixes exist in any fork. |
| [#36](https://github.com/Daniel-OS01/URLCheck/pull/36) (`upstream-pr/613`, draft) | `MiMoHo/TrianguloY-URLCheck [feat/remove-module-titles]` (`63f464f0`) | Exact source branch for upstream `#613`; no extra fixes elsewhere. |
| [#24](https://github.com/Daniel-OS01/URLCheck/pull/24) (`upstream-pr/629`) | `jivandabeast/URLCheck [feat/custom-modules]` & `[master]` (`ba06d23a`) | Exact source branch for upstream `#629` (custom modules with HTTP actions; intentionally left open pending explicit review/hardening). |

No external fork contains unmerged patches that fix `#24`, `#36`, or `#46`.

---

## Notable-but-not-copied ideas worth revisiting

| Idea | Source link | Why not copied directly | What would be needed for a clean implementation |
|---|---|---|---|
| Structured HTTP status & JSON error parsing in `UnshortenUtility` | [`skynet-05/URLCheck`](https://github.com/skynet-05/URLCheck/commits/feature/tor-sandbox-preview) (`3a477364`..`175b3933`) | Mixed into `Link Guard` whitelabel branch introducing `androidx` deps, committed keystore, and OTA updater. | Re-read error-message formatting/status-code handling and port cleanly onto existing `HttpUtils` / `UnshortenUtility` with zero new dependencies. |
| Android 15 edge-to-edge inset padding | [`skynet-05/URLCheck`](https://github.com/skynet-05/URLCheck/commits/rrv-c/linkguard-4.0.5-settings-scroll-top-75f7) (`75b65325`..`79953574`) | Uses `androidx.core` `ViewCompat` / `WindowInsetsCompat`, violating upstream's zero-dependency design. | Use framework `View.setOnApplyWindowInsetsListener` (guarded `SDK_INT >= 20` / `30`) on root layouts without adding `androidx`. |
| Site-specific handler prioritization over pinned fallback browser (`BrowserPreference` + `IntentApp.isUrlSpecificHandler()`) | [`jprapstyle/URLCheck`](https://github.com/jprapstyle/URLCheck/commit/b300282312) (`b3002823`..`83279a5a`) | Bundled inside a single monolithic Enlion squash alongside orientation locks, file `LogStore`, and local Korean/English docs. | Extract `BrowserPreference` and `isUrlSpecificHandler()` as a standalone, clean feature PR and test against `LastOpened` + `DirectOpen`. |
| Warm-launch `LastOpened.sort()` single-pass domain parse & ClearURL/icon LRU caches | [`jprapstyle/URLCheck`](https://github.com/jprapstyle/URLCheck/blob/master/docs/LOCAL_CHANGES_EN.md) (`b3002823`) | Bundled in the same monolithic Enlion commit. | Port `LastOpened.sort()` pre-computation separately (zero dependency risk, small diff) after benchmarking on `master`. |
| Explicit `decodeMethod` option in `PatternModule` (Base64 URL fragment decoding) | [`ottnorml/TrianguloY_URLCheck`](https://github.com/ottnorml/TrianguloY_URLCheck/tree/copilot/fix-null-pointer-exception) (`ef071a30`..`5b9386a5`) | 16 unreviewed Copilot commits under a misleading branch name; adds 260-line `UrlUtils.isValidText()` heuristic auto-decoder causing false positives on short ASCII query tokens, hardcoded `"  ↳ (base64)"` string, and overlaps with merged `SkipRedirectModule` (`#42`). | Only consider explicit per-rule opt-in `decodeMethod` with unit tests (`minSdk 19` Base64 flags) and localized UI labels — never heuristic auto-decoding. |
| Option to hide "Last opened" entry in `OpenModule` | [`8099bb4X21/URLCheck` (`patched`)](https://github.com/8099bb4X21/URLCheck/tree/patched) (`8ad0c9b3` history) | Left out of PR `#47` to keep `#47` scoped strictly to share intent args; `patched` also adds personal `minLength < 7` and CI churn. | If requested, extract only the single `hideLastUsed` toggle with localized strings. |

---

## Fork-specific licenses & attribution notes

- **Upstream (`TrianguloY/URLCheck`)**: Licensed under **Creative Commons Attribution 4.0 International (`CC-BY-4.0`)** (`LICENSE`), with explicit attribution rules in `README.md` (credit `TrianguloY` in an about/acknowledgments section and link to the repository).
- **Inspected forks (`oooppet`, `jprapstyle`, `skynet-05`, `8099bb4X21`, `ShiroiKuma0`, `3ekodomo`, `vimal-gandhi-dev`, `ottnorml`, `PabloOQ`)**: All verified to retain the upstream `CC-BY-4.0` `LICENSE` file (exact SHA `5f19d884aadeb183acfdb42e748a1bceefab6b70`) and upstream `README.md` credit blocks.
- **Rebrands & extra notices**:
  - `oooppet/URLCheck [development]`: Retains upstream `CC-BY-4.0`; only commit `c6a899b541` changed `applicationId` (excluded). Cherry-pick commits preserve original author `Tero Kallio` via `git cherry-pick -x` and credit `oooppet/URLCheck` in PR `#52`.
  - `jprapstyle/URLCheck [master]`: Documents derivative changes in `docs/LOCAL_CHANGES_EN.md` and `docs/LOCAL_CHANGES_KR.md` (`versionName "3.5-enlion.1"`).
  - `skynet-05/URLCheck`: Rebrands to `Link Guard` and commits a `signing/` folder (never copy keystore artifacts).
  - `vimal-gandhi-dev/URLCheck` (`Serva LinkCheck`, `com.servalabs.linkcheck`), `ShiroiKuma0/shiroikuma-renketsujoka` (`ShiroiURL`), and `3ekodomo/URLCheck-fork` (`com.ekodomo.urlchecker`) apply personal package/app rebrands while leaving `LICENSE` intact.

---

## Complete fork & branch scan (all 142 forks, 394 branches)

#### Ahead branches (265 across all forks)

| Fork | Branch | `pushed_at` | Ahead/Behind | Files | Verdict | Reason |
|---|---|---|---|---|---|---|
| `Daniel-OS01/URLCheck` | `actions/assets/data.minify.json` | 2026-11-24 | `+1/-0` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `Daniel-OS01/URLCheck` | `fork-pick/8099bb4X21-share-package-component` | 2026-11-24 | `+3/-0` | 3 (`release.yml`, `ShareUtility.java`, `OpenModule.java`) | **Already merged** | Fork PR #47 (`8099bb4X21` share intent package/component args) |
| `Daniel-OS01/URLCheck` | `master` | 2026-11-24 | `+45/-0` | 180 (`release.yml`, `build.gradle`, `AndroidManifest.xml`, ...) | **Target repo** | Synced fork `master` (contains upstream + merged PRs #25-#35, #37-#45, #47-#51) |
| `Daniel-OS01/URLCheck` | `shelf/PR68` | 2026-11-24 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Daniel-OS01/URLCheck` | `shelf/incognito-clipboard` | 2026-11-24 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `Daniel-OS01/URLCheck` | `tests/no-internet-variant` | 2026-11-24 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `Daniel-OS01/URLCheck` | `upstream-pr/397-added-new-resource-file-system` | 2026-11-24 | `+1/-184` | 12 (`AutoUpdateableResource.java`, `BuiltInResource.java`, `BuiltInResourceFile.java`, ...) | **Target mirror branch** | Mirrored upstream PR #397 |
| `Daniel-OS01/URLCheck` | `upstream-pr/523-add-deny-browser-to-open-share-functiona` | 2026-11-24 | `+1/-60` | 5 (`DenylistOption.java`, `ShareUtility.java`, `OpenModule.java`, ...) | **Target mirror branch** | Mirrored upstream PR #523 |
| `Daniel-OS01/URLCheck` | `upstream-pr/568-translations-update-from-hosted-weblate` | 2026-11-24 | `+1/-0` | 131 (`strings.xml`, `strings.xml`, `strings.xml`, ...) | **Target mirror branch** | Mirrored upstream PR #568 |
| `Daniel-OS01/URLCheck` | `upstream-pr/581-enhance-redirect-detection-add-support-f` | 2026-11-24 | `+1/-20` | 1 (`StatusModule.java`) | **Target mirror branch** | Mirrored upstream PR #581 |
| `Daniel-OS01/URLCheck` | `upstream-pr/606-skip-redirect-module-and-french-translat` | 2026-11-24 | `+53/-0` | 184 (`release.yml`, `build.gradle`, `AndroidManifest.xml`, ...) | **Target mirror branch** | Mirrored upstream PR #606 |
| `Daniel-OS01/URLCheck` | `upstream-pr/608-detect-ipv6-links-in-shared-and-clipboar` | 2026-11-24 | `+1/-8` | 1 (`AndroidUtils.java`) | **Target mirror branch** | Mirrored upstream PR #608 |
| `Daniel-OS01/URLCheck` | `upstream-pr/609-accept-text-html-shares-and-read-styled` | 2026-11-24 | `+1/-8` | 2 (`AndroidManifest.xml`, `MainDialog.java`) | **Target mirror branch** | Mirrored upstream PR #609 |
| `Daniel-OS01/URLCheck` | `upstream-pr/610-add-missing-content-descriptions-to-butt` | 2026-11-24 | `+1/-8` | 12 (`activity_about.xml`, `activity_automation.xml`, `activity_main.xml`, ...) | **Target mirror branch** | Mirrored upstream PR #610 |
| `Daniel-OS01/URLCheck` | `upstream-pr/611-size-the-browser-icon-in-dp-so-it-looks` | 2026-11-24 | `+1/-8` | 1 (`IntentApp.java`) | **Target mirror branch** | Mirrored upstream PR #611 |
| `Daniel-OS01/URLCheck` | `upstream-pr/612-allow-pinning-modules-so-they-stay-visib` | 2026-11-24 | `+3/-8` | 9 (`ModulesActivity.java`, `MainDialog.java`, `ModuleManager.java`, ...) | **Target mirror branch** | Mirrored upstream PR #612 |
| `Daniel-OS01/URLCheck` | `upstream-pr/613-replace-the-module-titles-with-self-expl` | 2026-11-24 | `+5/-8` | 52 (`ModulesActivity.java`, `MainDialog.java`, `ModuleManager.java`, ...) | **Target mirror branch** | Mirrored upstream PR #613 |
| `Daniel-OS01/URLCheck` | `upstream-pr/614-keep-label-text-readable-on-its-colored` | 2026-11-24 | `+1/-8` | 2 (`AndroidUtils.java`, `ids.xml`) | **Target mirror branch** | Mirrored upstream PR #614 |
| `Daniel-OS01/URLCheck` | `upstream-pr/615-show-the-flags-search-as-a-collapsible-t` | 2026-11-24 | `+32/-0` | 34 (`release.yml`, `AndroidManifest.xml`, `data.minify.json`, ...) | **Target mirror branch** | Mirrored upstream PR #615 |
| `Daniel-OS01/URLCheck` | `upstream-pr/616-dim-the-history-list-button-when-there-i` | 2026-11-24 | `+1/-8` | 1 (`HistoryModule.java`) | **Target mirror branch** | Mirrored upstream PR #616 |
| `Daniel-OS01/URLCheck` | `upstream-pr/617-break-module-labels-at-hyphens-instead-o` | 2026-11-24 | `+1/-8` | 1 (`config_module.xml`) | **Target mirror branch** | Mirrored upstream PR #617 |
| `Daniel-OS01/URLCheck` | `upstream-pr/618-only-offer-a-query-value-as-the-new-url` | 2026-11-24 | `+1/-8` | 1 (`RemoveQueriesModule.java`) | **Target mirror branch** | Mirrored upstream PR #618 |
| `Daniel-OS01/URLCheck` | `upstream-pr/619-keep-the-dialog-in-recents-until-it-is-c` | 2026-11-24 | `+1/-8` | 4 (`AndroidManifest.xml`, `MainDialog.java`, `bools.xml`, ...) | **Target mirror branch** | Mirrored upstream PR #619 |
| `Daniel-OS01/URLCheck` | `upstream-pr/620-name-the-action-buttons-after-what-they` | 2026-11-24 | `+3/-8` | 3 (`PatternCatalog.java`, `PatternModule.java`, `strings.xml`) | **Target mirror branch** | Mirrored upstream PR #620 |
| `Daniel-OS01/URLCheck` | `upstream-pr/621-only-follow-clearurls-redirections-that` | 2026-11-24 | `+1/-8` | 2 (`ClearUrlModule.java`, `strings.xml`) | **Target mirror branch** | Mirrored upstream PR #621 |
| `Daniel-OS01/URLCheck` | `upstream-pr/622-make-the-uri-parts-names-translatable` | 2026-11-24 | `+1/-8` | 2 (`UriPartsModule.java`, `strings.xml`) | **Target mirror branch** | Mirrored upstream PR #622 |
| `Daniel-OS01/URLCheck` | `upstream-pr/623-keep-the-dialog-state-when-the-theme-lan` | 2026-11-24 | `+2/-8` | 14 (`MainDialog.java`, `AModuleDialog.java`, `CTabs.java`, ...) | **Target mirror branch** | Mirrored upstream PR #623 |
| `Daniel-OS01/URLCheck` | `upstream-pr/627-updated-data-minify-json` | 2026-11-24 | `+1/-0` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Target mirror branch** | Mirrored upstream PR #627 |
| `Daniel-OS01/URLCheck` | `upstream-pr/629-add-user-defined-custom-modules` | 2026-11-24 | `+3/-0` | 12 (`ModulesActivity.java`, `MainDialog.java`, `AModuleData.java`, ...) | **Target mirror branch** | Mirrored upstream PR #629 |
| `Daniel-OS01/URLCheck` | `wip/triggers` | 2026-11-24 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `Daniel-OS01/URLCheck` | `wip/useragent` | 2026-11-24 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `jivandabeast/URLCheck` | `feat/custom-modules` | 2026-11-24 | `+3/-0` | 12 (`ModulesActivity.java`, `MainDialog.java`, `AModuleData.java`, ...) | **Open PR (#24)** | Exact source branch for upstream #629 / fork PR #24 (custom modules) |
| `jivandabeast/URLCheck` | `master` | 2026-11-24 | `+3/-0` | 12 (`ModulesActivity.java`, `MainDialog.java`, `AModuleData.java`, ...) | **Open PR (#24)** | Identical commit stack to `feat/custom-modules` (upstream #629 / fork PR #24) |
| `ozmo998/URLCheck` | `actions/assets/data.minify.json` | 2026-11-19 | `+1/-0` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **No new code** | Bot `data.minify.json` commit (already merged via #25) |
| `comilucifer-alt/URLCheck` | `shelf/PR68` | 2026-10-25 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `comilucifer-alt/URLCheck` | `shelf/incognito-clipboard` | 2026-10-25 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `comilucifer-alt/URLCheck` | `tests/no-internet-variant` | 2026-10-25 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `comilucifer-alt/URLCheck` | `wip/triggers` | 2026-10-25 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `comilucifer-alt/URLCheck` | `wip/useragent` | 2026-10-25 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `jprapstyle/URLCheck` | `master` | 2026-10-30 | `+4/-1` | 24 (`build.gradle`, `BrowserChecks.java`, `PerformanceChecks.java`, ...) | **Rejected (noted)** | Monolithic Enlion `3.5-enlion.1` squash (`BrowserPreference`, `OrientationSettings`, `LogStore`, local docs) |
| `YoussefHeshamAlsoly/URLCheck` | `shelf/PR68` | 2026-10-18 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `YoussefHeshamAlsoly/URLCheck` | `shelf/incognito-clipboard` | 2026-10-18 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `YoussefHeshamAlsoly/URLCheck` | `tests/no-internet-variant` | 2026-10-18 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `YoussefHeshamAlsoly/URLCheck` | `wip/redirect-module` | 2026-10-18 | `+4/-7` | 11 (`ModuleManager.java`, `ClearUrlCatalog.java`, `RedirectCatalog.java`, ...) | **Already merged** | Older `SkipRedirectModule` WIP (superseded by #42) |
| `YoussefHeshamAlsoly/URLCheck` | `wip/triggers` | 2026-10-18 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `YoussefHeshamAlsoly/URLCheck` | `wip/useragent` | 2026-10-18 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `oooppet/URLCheck` | `development` | 2026-10-22 | `+4/-7` | 15 (`build.gradle`, `AndroidManifest.xml`, `DirectOpenActivity.java`, ...) | **ACCEPTED -> PR #52** | Link-interception: `URLBlocklist` + `DirectOpen` forwarding (3 commits cherry-picked, excluded `c6a899b5`) |
| `MiMoHo/TrianguloY-URLCheck` | `feat/remove-module-titles` | 2026-10-15 | `+5/-8` | 52 (`ModulesActivity.java`, `MainDialog.java`, `ModuleManager.java`, ...) | **Open PR (#36)** | Exact source branch for upstream #613 / fork PR #36 (draft) |
| `MiMoHo/TrianguloY-URLCheck` | `feat/234-pin-modules` | 2026-10-15 | `+3/-8` | 9 (`ModulesActivity.java`, `MainDialog.java`, `ModuleManager.java`, ...) | **Already merged** | Upstream PR branch (`feat/234-pin-modules` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/browser-icon-dp` | 2026-10-15 | `+1/-8` | 1 (`IntentApp.java`) | **Already merged** | Upstream PR branch (`fix/browser-icon-dp` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/clearer-action-labels` | 2026-10-15 | `+3/-8` | 3 (`PatternCatalog.java`, `PatternModule.java`, `strings.xml`) | **Already merged** | Upstream PR branch (`fix/clearer-action-labels` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/clearurl-redirect-target` | 2026-10-15 | `+1/-8` | 2 (`ClearUrlModule.java`, `strings.xml`) | **Already merged** | Upstream PR branch (`fix/clearurl-redirect-target` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/dialog-in-recents` | 2026-10-15 | `+1/-8` | 4 (`AndroidManifest.xml`, `MainDialog.java`, `bools.xml`, ...) | **Already merged** | Upstream PR branch (`fix/dialog-in-recents` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/flags-search-title` | 2026-10-15 | `+1/-8` | 3 (`FlagsModule.java`, `dialog_flags.xml`, `strings.xml`) | **Already merged** | Upstream PR branch (`fix/flags-search-title` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/history-list-button` | 2026-10-15 | `+1/-8` | 1 (`HistoryModule.java`) | **Already merged** | Upstream PR branch (`fix/history-list-button` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/keep-dialog-state` | 2026-10-15 | `+2/-8` | 14 (`MainDialog.java`, `AModuleDialog.java`, `CTabs.java`, ...) | **Already merged** | Upstream PR branch (`fix/keep-dialog-state` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/label-contrast` | 2026-10-15 | `+1/-8` | 2 (`AndroidUtils.java`, `ids.xml`) | **Already merged** | Upstream PR branch (`fix/label-contrast` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/module-label-hyphen-break` | 2026-10-15 | `+1/-8` | 1 (`config_module.xml`) | **Already merged** | Upstream PR branch (`fix/module-label-hyphen-break` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/query-value-link-only` | 2026-10-15 | `+1/-8` | 1 (`RemoveQueriesModule.java`) | **Already merged** | Upstream PR branch (`fix/query-value-link-only` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/uri-parts-translatable` | 2026-10-15 | `+1/-8` | 2 (`UriPartsModule.java`, `strings.xml`) | **Already merged** | Upstream PR branch (`fix/uri-parts-translatable` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/337-ipv6-links` | 2026-10-15 | `+1/-8` | 1 (`AndroidUtils.java`) | **Already merged** | Upstream PR branch (`fix/337-ipv6-links` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/494-send-mimetypes` | 2026-10-15 | `+1/-8` | 2 (`AndroidManifest.xml`, `MainDialog.java`) | **Already merged** | Upstream PR branch (`fix/494-send-mimetypes` merged in #26-#35, #37-#41, #49) |
| `MiMoHo/TrianguloY-URLCheck` | `fix/556-button-labels` | 2026-10-15 | `+1/-8` | 12 (`activity_about.xml`, `activity_automation.xml`, `activity_main.xml`, ...) | **Already merged** | Upstream PR branch (`fix/556-button-labels` merged in #26-#35, #37-#41, #49) |
| `LeDimigit/URLCheck` | `master` | 2026-10-11 | `+7/-12` | 6 (`ModuleManager.java`, `SkipRedirectUtility.java`, `SkipRedirectModule.java`, ...) | **Already merged** | Source of upstream #606 / fork PR #42 (`SkipRedirectModule`) |
| `BDHACK638/URLCheck` | `master` | 2026-10-10 | `+1/-13` | 1 (`README.md`) | **Rejected (junk)** | Deletes `README.md` (`1700e561`) |
| `snigdhasjg/URLCheck` | `master` | 2026-09-20 | `+1/-13` | 2 (`AndroidManifest.xml`, `network_security_config.xml`) | **Already upstream** | User CA trust (`network_security_config.xml`, upstream #604) already on `master` |
| `skynet-05/URLCheck` | `feature/tor-sandbox-preview` | 2026-11-08 | `+28/-13` | 102 (`linkguard-release.yml`, `.gitignore`, `build.gradle`, ...) | **Rejected (noted)** | Whitelabel `Link Guard` (`androidx`, committed keystore, OTA, `#571 wontfix`); see Notable Ideas |
| `skynet-05/URLCheck` | `rrv-c/linkguard-4.0.3-ui-errors-75f7` | 2026-11-08 | `+17/-13` | 89 (`linkguard-release.yml`, `.gitignore`, `build.gradle`, ...) | **Rejected (rebrand)** | Intermediate `Link Guard` ancestor branch of `feature/tor-sandbox-preview` |
| `skynet-05/URLCheck` | `rrv-c/linkguard-4.0.4-settings-nav-75f7` | 2026-11-08 | `+19/-13` | 91 (`linkguard-release.yml`, `.gitignore`, `build.gradle`, ...) | **Rejected (rebrand)** | Intermediate `Link Guard` ancestor branch of `feature/tor-sandbox-preview` |
| `skynet-05/URLCheck` | `rrv-c/linkguard-4.0.5-settings-scroll-top-75f7` | 2026-11-08 | `+21/-13` | 95 (`linkguard-release.yml`, `.gitignore`, `build.gradle`, ...) | **Rejected (rebrand)** | Intermediate `Link Guard` ancestor branch of `feature/tor-sandbox-preview` |
| `skynet-05/URLCheck` | `rrv-c/linkguard-4.0.6-settings-linkguard-entry-75f7` | 2026-11-08 | `+22/-13` | 96 (`linkguard-release.yml`, `.gitignore`, `build.gradle`, ...) | **Rejected (rebrand)** | Intermediate `Link Guard` ancestor branch of `feature/tor-sandbox-preview` |
| `3ekodomo/URLCheck-fork` | `actions/assets/data.minify.json` | 2026-11-08 | `+50/-16` | 115 (`FUNDING.yml`, `alpha.yml`, `codacy.yml`, ...) | **Rejected (rebrand)** | Bot `data.minify.json` commit on top of `com.ekodomo.urlchecker` rebrand |
| `3ekodomo/URLCheck-fork` | `backup` | 2026-11-08 | `+26/-16` | 108 (`FUNDING.yml`, `codacy.yml`, `codeql-analysis.yml`, ...) | **Rejected (rebrand)** | Earlier `com.ekodomo.urlchecker` backup snapshot |
| `3ekodomo/URLCheck-fork` | `fix-gradlew-permissions-9733027769332480225` | 2026-11-08 | `+41/-16` | 113 (`FUNDING.yml`, `alpha.yml`, `codacy.yml`, ...) | **Rejected (rebrand)** | Personal `com.ekodomo.urlchecker` + Gradle permission workflow experiments |
| `3ekodomo/URLCheck-fork` | `master` | 2026-11-08 | `+49/-16` | 114 (`FUNDING.yml`, `alpha.yml`, `codacy.yml`, ...) | **Rejected (rebrand)** | Personal `com.ekodomo.urlchecker` rebrand + workflow/plan churn |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `custom` | 2026-09-04 | `+25/-18` | 256 (`SKILL.md`, `SKILL.md`, `FUNDING.yml`, ...) | **Rejected (rebrand)** | Personal `ShiroiURL` whitelabel (yellow/black theme, `SkToast`, `FOREGROUND_SERVICE` backup) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `dependabot/github_actions/actions/setup-java-5.6.0` | 2026-09-04 | `+1/-18` | 4 (`alpha.yml`, `codeql-analysis.yml`, `release.yml`, ...) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/setup-java-5.6.0`) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `dependabot/github_actions/github/codeql-action-4.37.4` | 2026-09-04 | `+1/-18` | 2 (`codacy.yml`, `codeql-analysis.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/github/codeql-action-4.37.4`) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `dependabot/github_actions/gradle/actions-6.2.0` | 2026-09-04 | `+1/-18` | 3 (`alpha.yml`, `release.yml`, `validate-gradle-build-test.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/gradle/actions-6.2.0`) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `shelf/PR68` | 2026-09-04 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `shelf/incognito-clipboard` | 2026-09-04 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `tests/no-internet-variant` | 2026-09-04 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `wip/triggers` | 2026-09-04 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `wip/useragent` | 2026-09-04 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `dkguptadk8722-dev/URLCheck` | `dependabot/github_actions/actions/setup-java-5.6.0` | 2026-08-05 | `+1/-18` | 4 (`alpha.yml`, `codeql-analysis.yml`, `release.yml`, ...) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/setup-java-5.6.0`) |
| `dkguptadk8722-dev/URLCheck` | `dependabot/github_actions/github/codeql-action-4.37.4` | 2026-08-05 | `+1/-18` | 2 (`codacy.yml`, `codeql-analysis.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/github/codeql-action-4.37.4`) |
| `dkguptadk8722-dev/URLCheck` | `dependabot/github_actions/gradle/actions-6.2.0` | 2026-08-05 | `+1/-18` | 3 (`alpha.yml`, `release.yml`, `validate-gradle-build-test.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/gradle/actions-6.2.0`) |
| `dkguptadk8722-dev/URLCheck` | `patch-1` | 2026-08-05 | `+1/-18` | 1 (`Satyam`) | **Rejected (junk)** | Renames `README.md` to `Satyam` (`6a6de947`) |
| `dkguptadk8722-dev/URLCheck` | `shelf/PR68` | 2026-08-05 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `dkguptadk8722-dev/URLCheck` | `shelf/incognito-clipboard` | 2026-08-05 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `dkguptadk8722-dev/URLCheck` | `tests/no-internet-variant` | 2026-08-05 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `dkguptadk8722-dev/URLCheck` | `wip/triggers` | 2026-08-05 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `dkguptadk8722-dev/URLCheck` | `wip/useragent` | 2026-08-05 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `8099bb4X21/URLCheck` | `patched` | 2026-09-26 | `+13/-13` | 14 (`codeql-config.yml`, `alpha.yml`, `assets-updater.yml`, ...) | **Partial -> PR #47 / rejected rest** | 2 share-arg commits merged in #47; remaining 11 commits (`minLength<7`, `hideLastUsed`, CI/version) rejected |
| `mvanhorn/URLCheck` | `fix/560-urlcheck-dynamic-colors` | 2026-09-09 | `+2/-19` | 3 (`colors.xml`, `colors.xml`, `styles.xml`) | **Already upstream** | Material You dynamic colors merged upstream in #587 (ahead count is pre-squash history) |
| `mvanhorn/URLCheck` | `shelf/PR68` | 2026-09-09 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `mvanhorn/URLCheck` | `shelf/incognito-clipboard` | 2026-09-09 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `mvanhorn/URLCheck` | `tests/no-internet-variant` | 2026-09-09 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `mvanhorn/URLCheck` | `wip/triggers` | 2026-09-09 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `mvanhorn/URLCheck` | `wip/useragent` | 2026-09-09 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `qpst4/URLCheck-0` | `enhance-redirect-detection` | 2026-06-16 | `+1/-20` | 1 (`StatusModule.java`) | **Already merged** | Source of upstream #581 / fork PR #43 (meta refresh / JS redirects) |
| `vimal-gandhi-dev/URLCheck` | `feature/updated-code` | 2026-09-10 | `+2/-20` | 155 (`README.md`, `.DS_Store`, `build.gradle`, ...) | **Rejected (rebrand)** | Earlier `Serva LinkCheck` (`com.servalabs.linkcheck`) rebrand snapshot |
| `vimal-gandhi-dev/URLCheck` | `master` | 2026-09-10 | `+4/-20` | 155 (`README.md`, `.DS_Store`, `build.gradle`, ...) | **Rejected (rebrand)** | Rebranded `Serva LinkCheck` (`com.servalabs.linkcheck`) + `.DS_Store`/APK artifacts |
| `morocoeltopo/URLCheck_Personal` | `actions/assets/data.minify.json` | 2026-11-23 | `+3/-30` | 3 (`alpha.yml`, `data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `morocoeltopo/URLCheck_Personal` | `master` | 2026-11-23 | `+4/-0` | 1 (`alpha.yml`) | **Rejected (personal CI)** | Removes zip password in `.github/workflows/alpha.yml` |
| `morocoeltopo/URLCheck_Personal` | `shelf/PR68` | 2026-11-23 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `morocoeltopo/URLCheck_Personal` | `shelf/incognito-clipboard` | 2026-11-23 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `morocoeltopo/URLCheck_Personal` | `tests/no-internet-variant` | 2026-11-23 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `morocoeltopo/URLCheck_Personal` | `wip/triggers` | 2026-11-23 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `morocoeltopo/URLCheck_Personal` | `wip/unshorten_token` | 2026-11-23 | `+1/-35` | 6 (`BackupActivity.java`, `UnshortenUtility.java`, `UnshortenModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/unshorten_token`) |
| `VVytai/URLCheck` | `actions/assets/data.minify.json` | 2026-11-22 | `+4/-65` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `VVytai/URLCheck` | `shelf/PR68` | 2026-11-22 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `VVytai/URLCheck` | `tmp/automation-args` | 2026-11-22 | `+1/-150` | 4 (`MainDialog.java`, `AutomationRules.java`, `WebhookModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`tmp/automation-args`, superseded by #47) |
| `VVytai/URLCheck` | `wip/virustotal_v3` | 2026-11-22 | `+4/-210` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `A-Archives-and-Forks/UrlChecker` | `feature/rejection-detector` | 2026-09-14 | `+3/-321` | 56 (`AboutActivity.java`, `MainActivity.java`, `ModulesActivity.java`, ...) | **Rejected (stale WIP)** | Old incomplete `RejectionDetector` branch (`-321` behind upstream) |
| `A-Archives-and-Forks/UrlChecker` | `shelf/PR68` | 2026-09-14 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `weblate/UrlChecker` | `shelf/PR68` | 2026-11-14 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `weblate/UrlChecker` | `trianguloy/old_shortcut` | 2026-11-14 | `+1/-477` | 2 (`AndroidManifest.xml`, `ShortcutsActivity.java`) | **Rejected (obsolete)** | Historical upstream `old_shortcut` branch (`-477` behind) |
| `weblate/UrlChecker` | `weblate-urlcheck-app-strings` | 2026-11-14 | `+1/-0` | 131 (`strings.xml`, `strings.xml`, `strings.xml`, ...) | **Already merged** | Hosted Weblate sync (merged via upstream #568 / fork PR #44) |
| `PabloOQ/UrlChecker` | `2025-fastlane-migration` | 2026-11-22 | `+21/-53` | 239 (`labeler.yml`, `release.yml`, `validate-store-files.yml`, ...) | **Already upstream / stale** | Fastlane/workflow migration branch (`-53` behind) |
| `PabloOQ/UrlChecker` | `actions/assets/data.minify.json` | 2026-11-22 | `+12/-567` | 1 (`data.minify.json`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `PabloOQ/UrlChecker` | `dependabot/github_actions/actions/checkout-4` | 2026-11-22 | `+1/-353` | 7 (`alpha.yml`, `assets-updater.yml`, `codacy.yml`, ...) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/checkout-4`) |
| `PabloOQ/UrlChecker` | `dependabot/github_actions/actions/setup-java-4` | 2026-11-22 | `+1/-353` | 2 (`alpha.yml`, `validate-gradle-build-test.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/setup-java-4`) |
| `PabloOQ/UrlChecker` | `incognito-clipboard` | 2026-11-22 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Rejected (closed upstream)** | Chrome UI-automation accessibility service (upstream `#301`, closed) |
| `PabloOQ/UrlChecker` | `incognito-clipboard-copy` | 2026-11-22 | `+16/-312` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Rejected (closed upstream)** | Chrome UI-automation accessibility service (upstream `#301`, closed) |
| `PabloOQ/UrlChecker` | `incognito-module` | 2026-11-22 | `+31/-312` | 58 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Rejected (closed upstream)** | Chrome UI-automation accessibility service (upstream `#301`, closed) |
| `PabloOQ/UrlChecker` | `incognito-service` | 2026-11-22 | `+8/-312` | 30 (`AndroidManifest.xml`, `ShortcutsActivity.java`, `Incognito.java`, ...) | **Rejected (closed upstream)** | Chrome UI-automation accessibility service (upstream `#301`, closed) |
| `PabloOQ/UrlChecker` | `open-and-share` | 2026-11-22 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Already upstream / stale** | Old `shelf/PR68` preset-flags snapshot (`-734` behind) |
| `PabloOQ/UrlChecker` | `open-share` | 2026-11-22 | `+14/-736` | 8 (`OpenModule.java`, `GenericPref.java`, `mopen_auto.xml`, ...) | **Rejected (obsolete)** | Stale 2024 open/share experiment (`-736` behind) |
| `PabloOQ/UrlChecker` | `resource-files-update` | 2026-11-22 | `+1/-184` | 12 (`AutoUpdateableResource.java`, `BuiltInResource.java`, `BuiltInResourceFile.java`, ...) | **Open PR (#46)** | Exact snapshot of upstream #397 / fork PR #46 (draft) |
| `PabloOQ/UrlChecker` | `separator-mod-id` | 2026-11-22 | `+2/-355` | 17 (`MainDialog.java`, `AModuleDialog.java`, `ClearUrlModule.java`, ...) | **Rejected (obsolete)** | Stale module-separator refactor (`-355` behind) |
| `PabloOQ/UrlChecker` | `test-assets-action` | 2026-11-22 | `+8/-643` | 4 (`file-updater.sh`, `assets-updater.yml`, `data.minify.json`, ...) | **Rejected (stale CI)** | 2024 `assets-updater` CI workflow test branch |
| `PabloOQ/UrlChecker` | `test-update-action-2` | 2026-11-22 | `+6/-641` | 2 (`file-updater.sh`, `assets-updater.yml`) | **Rejected (stale CI)** | 2024 `assets-updater` CI workflow test branch |
| `PabloOQ/UrlChecker` | `test-update-action-3` | 2026-11-22 | `+4/-609` | 1 (`assets-updater.yml`) | **Rejected (stale CI)** | 2024 `assets-updater` CI workflow test branch |
| `NicholasThyssen/URLCheck` | `Open-source-Contribution` | 2025-09-30 | `+1/-60` | 5 (`DenylistOption.java`, `ShareUtility.java`, `OpenModule.java`, ...) | **Already merged** | Source of upstream #523 / fork PR #45 (deny browser) |
| `NicholasThyssen/URLCheck` | `shelf/PR68` | 2025-09-30 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `NicholasThyssen/URLCheck` | `tests/no-internet-variant` | 2025-09-30 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `mdmustakim124com-boop/URLCheck` | `shelf/PR68` | 2026-04-07 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `mdmustakim124com-boop/URLCheck` | `shelf/incognito-clipboard` | 2026-04-07 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `mdmustakim124com-boop/URLCheck` | `tests/no-internet-variant` | 2026-04-07 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `mdmustakim124com-boop/URLCheck` | `wip/triggers` | 2026-04-07 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `mdmustakim124com-boop/URLCheck` | `wip/useragent` | 2026-04-07 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `utafrali/URLCheck` | `fix/issue-566-songlink-replacement-on-every-url` | 2026-04-13 | `+2/-30` | 1 (`PatternCatalog.java`) | **Already upstream** | Song.link pattern fix merged upstream in #567 |
| `utafrali/URLCheck` | `shelf/PR68` | 2026-04-13 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `utafrali/URLCheck` | `shelf/incognito-clipboard` | 2026-04-13 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `utafrali/URLCheck` | `tests/no-internet-variant` | 2026-04-13 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `utafrali/URLCheck` | `wip/triggers` | 2026-04-13 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `utafrali/URLCheck` | `wip/useragent` | 2026-04-13 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `coderj001/URLCheck` | `shelf/PR68` | 2026-03-30 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `coderj001/URLCheck` | `shelf/incognito-clipboard` | 2026-03-30 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `coderj001/URLCheck` | `tests/no-internet-variant` | 2026-03-30 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `coderj001/URLCheck` | `wip/triggers` | 2026-03-30 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `coderj001/URLCheck` | `wip/useragent` | 2026-03-30 | `+3/-30` | 10 (`StatusModule.java`, `AndroidUtils.java`, `list.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/useragent`) |
| `NicolaeFericitu/URLCheck` | `master` | 2025-12-29 | `+3/-45` | 1 (`strings.xml`) | **Already upstream** | Earlier Romanian translation commits merged upstream in #543 |
| `NicolaeFericitu/URLCheck` | `patch-1` | 2025-12-29 | `+1/-45` | 1 (`strings.xml`) | **Already upstream** | Romanian translation merged upstream in #543 |
| `NicolaeFericitu/URLCheck` | `shelf/PR68` | 2025-12-29 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `NicolaeFericitu/URLCheck` | `tests/no-internet-variant` | 2025-12-29 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `ez-me/URLCheck` | `patch-1` | 2026-01-08 | `+2/-39` | 1 (`PatternCatalog.java`) | **Already upstream** | PatternCatalog fix merged upstream in #550 |
| `ez-me/URLCheck` | `shelf/PR68` | 2026-01-08 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `ez-me/URLCheck` | `shelf/incognito-clipboard` | 2026-01-08 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `ez-me/URLCheck` | `tests/no-internet-variant` | 2026-01-08 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `ottnorml/TrianguloY_URLCheck` | `copilot/fix-null-pointer-exception` | 2026-04-11 | `+16/-45` | 4 (`ClearUrlModule.java`, `PatternModule.java`, `UriPartsModule.java`, ...) | **Rejected (noted)** | Misleading name (no NPE fix); 16 Copilot commits adding heuristic Base64 decoder |
| `tofansadat5-cmyk/URLCheck` | `shelf/PR68` | 2026-01-19 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `tofansadat5-cmyk/URLCheck` | `shelf/incognito-clipboard` | 2026-01-19 | `+24/-72` | 59 (`.gitignore`, `build.gradle`, `proguard-rules.pro`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/incognito-clipboard` / `ForceURL`) |
| `tofansadat5-cmyk/URLCheck` | `tests/no-internet-variant` | 2026-01-19 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `tofansadat5-cmyk/URLCheck` | `wip/triggers` | 2026-01-19 | `+1/-35` | 5 (`MainDialog.java`, `AModuleData.java`, `AutomationRules.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/triggers`) |
| `tofansadat5-cmyk/URLCheck` | `wip/unshorten_token` | 2026-01-19 | `+1/-35` | 6 (`BackupActivity.java`, `UnshortenUtility.java`, `UnshortenModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/unshorten_token`) |
| `adesso636-spec/URLCheck` | `shelf/PR68` | 2025-12-22 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `adesso636-spec/URLCheck` | `tests/no-internet-variant` | 2025-12-22 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `safan77260-png/URLCheck` | `shelf/PR68` | 2025-11-21 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `safan77260-png/URLCheck` | `tests/no-internet-variant` | 2025-11-21 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `sfukada77-png/URLCheck` | `shelf/PR68` | 2025-10-24 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `sfukada77-png/URLCheck` | `tests/no-internet-variant` | 2025-10-24 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `nonomal/URLCheck` | `shelf/PR68` | 2025-10-24 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `nonomal/URLCheck` | `tests/no-internet-variant` | 2025-10-24 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `minhdient49-tech/minhdient49-gmail.comm` | `master` | 2025-10-10 | `+2/-63` | 1 (`README.md`) | **Rejected (junk)** | Appends personal reference to `README.md` |
| `kidhanis/URLCheck` | `connection-http-casting` | 2025-07-23 | `+5/-68` | 1 (`Connection.java`) | **Already upstream** | Connection HTTP casting fix merged upstream in #507 |
| `kidhanis/URLCheck` | `shelf/PR68` | 2025-07-23 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `S3n3scH/URLCheck` | `actions/assets/data.minify.json` | 2025-06-29 | `+3/-77` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `S3n3scH/URLCheck` | `shelf/PR68` | 2025-06-29 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `miniBill/URLCheck` | `actions/assets/data.minify.json` | 2025-07-06 | `+2/-77` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `miniBill/URLCheck` | `shelf/PR68` | 2025-07-06 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `dabe71064/URLCheck` | `actions/assets/data.minify.json` | 2025-09-04 | `+1/-65` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `dabe71064/URLCheck` | `dependabot/github_actions/actions/checkout-5` | 2025-09-04 | `+1/-63` | 7 (`alpha.yml`, `assets-updater.yml`, `codacy.yml`, ...) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/checkout-5`) |
| `dabe71064/URLCheck` | `dependabot/github_actions/actions/labeler-6` | 2025-09-04 | `+1/-63` | 1 (`labeler.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/labeler-6`) |
| `dabe71064/URLCheck` | `shelf/PR68` | 2025-09-04 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `dabe71064/URLCheck` | `tests/no-internet-variant` | 2025-09-04 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `hmdanjkr-cmyk/URLCheck` | `actions/assets/data.minify.json` | 2025-09-04 | `+1/-65` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `hmdanjkr-cmyk/URLCheck` | `dependabot/github_actions/actions/checkout-5` | 2025-09-04 | `+1/-63` | 7 (`alpha.yml`, `assets-updater.yml`, `codacy.yml`, ...) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/checkout-5`) |
| `hmdanjkr-cmyk/URLCheck` | `dependabot/github_actions/actions/labeler-6` | 2025-09-04 | `+1/-63` | 1 (`labeler.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/labeler-6`) |
| `hmdanjkr-cmyk/URLCheck` | `shelf/PR68` | 2025-09-04 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `hmdanjkr-cmyk/URLCheck` | `tests/no-internet-variant` | 2025-09-04 | `+1/-66` | 8 (`build.gradle`, `AndroidManifest.xml`, `AndroidManifest.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`tests/no-internet-variant`) |
| `evg58/URLCheck` | `actions/assets/data.minify.json` | 2025-06-08 | `+1/-77` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `evg58/URLCheck` | `shelf/PR68` | 2025-06-08 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Quinta-5/URLCheck` | `shelf/PR68` | 2025-05-13 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Quinta-5/URLCheck` | `wip/virustotal_v3` | 2025-05-13 | `+4/-97` | 36 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `WebhookModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `Kishanjvaghela/URLCheck` | `imgbot` | 2025-02-27 | `+1/-147` | 15 (`featured.png`, `ic_launcher-playstore.png`, `trianguloy.png`, ...) | **Rejected (stale bot)** | 2025 ImgBot PNG compression (`fbee2568`) |
| `bitilk/URLCheck` | `shelf/PR68` | 2025-03-22 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `bitilk/URLCheck` | `tmp/automation-args` | 2025-03-22 | `+1/-150` | 4 (`MainDialog.java`, `AutomationRules.java`, `WebhookModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`tmp/automation-args`, superseded by #47) |
| `bitilk/URLCheck` | `wip/virustotal_v3` | 2025-03-22 | `+4/-210` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `yahyan7666/URLCheck` | `actions/assets/data.minify.json` | 2025-04-10 | `+1/-114` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `yahyan7666/URLCheck` | `patch-1` | 2025-04-10 | `+1/-114` | 1 (`From.me`) | **Rejected (junk)** | Creates spam file `app/src/main/play/release-notes/From.me` |
| `yahyan7666/URLCheck` | `shelf/PR68` | 2025-04-10 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `yahyan7666/URLCheck` | `tmp/automation-args` | 2025-04-10 | `+1/-150` | 4 (`MainDialog.java`, `AutomationRules.java`, `WebhookModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`tmp/automation-args`, superseded by #47) |
| `yahyan7666/URLCheck` | `wip/virustotal_v3` | 2025-04-10 | `+4/-210` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `Dinesh281198/URLCheck` | `shelf/PR68` | 2025-03-13 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Dinesh281198/URLCheck` | `tmp/automation-args` | 2025-03-13 | `+1/-150` | 4 (`MainDialog.java`, `AutomationRules.java`, `WebhookModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`tmp/automation-args`, superseded by #47) |
| `Dinesh281198/URLCheck` | `wip/virustotal_v3` | 2025-03-13 | `+4/-210` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `Dinesh2811/URLCheck` | `actions/assets/data.minify.json` | 2025-03-02 | `+1/-145` | 2 (`data.minify.json`, `ClearUrlCatalog.java`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `Dinesh2811/URLCheck` | `shelf/PR68` | 2025-03-02 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Dinesh2811/URLCheck` | `tmp/automation-args` | 2025-03-02 | `+1/-150` | 4 (`MainDialog.java`, `AutomationRules.java`, `WebhookModule.java`, ...) | **Upstream mirror** | Upstream mirror branch (`tmp/automation-args`, superseded by #47) |
| `Dinesh2811/URLCheck` | `wip/virustotal_v3` | 2025-03-02 | `+4/-210` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `pokegamer5547/UrlChecker` | `master` | 2024-11-27 | `+6/-170` | 1 (`android.yml`) | **Rejected (personal CI)** | Personal `.github/workflows/android.yml` workflow |
| `ninadnaik10/UrlChecker` | `custom-ui-changes` | 2024-09-05 | `+1/-313` | 254 (`FUNDING.yml`, `bug_report.yml`, `config.yml`, ...) | **Rejected (personal UI)** | Stale personal spacing/color UI tweaks (`-200+` behind) |
| `vecnathewhisperd0/UrlChecker` | `shelf/PR68` | 2024-11-03 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `vecnathewhisperd0/UrlChecker` | `wip/json_editor_activity` | 2024-11-03 | `+1/-210` | 38 (`AndroidManifest.xml`, `JsonEditorActivity.java`, `JsonEditorInterface.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/json_editor_activity`, already upstream) |
| `vecnathewhisperd0/UrlChecker` | `wip/virustotal_v3` | 2024-11-03 | `+4/-210` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `2447776119A/UrlChecker` | `shelf/PR68` | 2024-08-22 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `2447776119A/UrlChecker` | `wip/json_editor_activity` | 2024-08-22 | `+1/-238` | 38 (`AndroidManifest.xml`, `JsonEditorActivity.java`, `JsonEditorInterface.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/json_editor_activity`, already upstream) |
| `2447776119A/UrlChecker` | `wip/virustotal_v3` | 2024-08-22 | `+4/-238` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `anoop-b/UrlChecker` | `shelf/PR68` | 2024-11-18 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `anoop-b/UrlChecker` | `wip/json_editor_activity` | 2024-11-18 | `+1/-210` | 38 (`AndroidManifest.xml`, `JsonEditorActivity.java`, `JsonEditorInterface.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/json_editor_activity`, already upstream) |
| `anoop-b/UrlChecker` | `wip/virustotal_v3` | 2024-11-18 | `+4/-210` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `mariachrisochoou/UrlChecker` | `master` | 2024-06-10 | `+7/-247` | 2 (`TextInputModule.java`, `dialog_text.xml`) | **Rejected (stale exp.)** | 2024 `TextInputModule` TextView toggle experiment |
| `shishka-b0b/UrlChecker` | `shelf/PR68` | 2024-06-17 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Bellabas23/UrlChecker` | `shelf/PR68` | 2024-07-08 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Bellabas23/UrlChecker` | `wip/json_editor_activity` | 2024-07-08 | `+1/-238` | 38 (`AndroidManifest.xml`, `JsonEditorActivity.java`, `JsonEditorInterface.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/json_editor_activity`, already upstream) |
| `Bellabas23/UrlChecker` | `wip/virustotal_v3` | 2024-07-08 | `+4/-238` | 27 (`VirusTotalUtility.java`, `VirusTotalModule.java`, `HttpUtils.java`, ...) | **Upstream mirror** | Upstream mirror branch (`wip/virustotal_v3`, already upstream) |
| `Suhatec/UrlChecker` | `shelf/PR68` | 2024-04-07 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `ko9ma7/UrlChecker` | `master` | 2023-12-17 | `+1/-304` | 2 (`codacy.yml`, `codeql-analysis.yml`) | **Rejected (stale CI)** | 2024 Dependabot CodeQL workflow bump |
| `lrq3000/UrlChecker` | `backupRestore` | 2024-02-10 | `+14/-290` | 32 (`build.gradle`, `AndroidManifest.xml`, `backup_readme.txt`, ...) | **Already upstream** | Backup/restore feature merged upstream in 2024 |
| `lrq3000/UrlChecker` | `shelf/PR68` | 2024-02-10 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `TeaqariaWTF/UrlChecker` | `actions/assets/data.minify.json` | 2024-08-08 | `+2/-264` | 1 (`data.minify.json`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `vaimalaviya1233/UrlChecker` | `actions/assets/data.minify.json` | 2023-11-19 | `+1/-313` | 1 (`data.minify.json`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `vaimalaviya1233/UrlChecker` | `shelf/PR68` | 2023-11-19 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Martinligabue/UrlChecker` | `master` | 2023-07-14 | `+1/-351` | 1 (`VirusTotalModule copy.java`) | **Rejected (junk)** | Accidental duplicate file `VirusTotalModule copy.java` |
| `Martinligabue/UrlChecker` | `shelf/PR68` | 2023-07-14 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `54m4d/UrlChecker` | `shelf/PR68` | 2023-04-19 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `vaginessa/UrlChecker-1` | `shelf/PR68` | 2023-04-17 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Swipdaggoe/UrlChecker` | `shelf/PR68` | 2023-01-19 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `the-blank-x/UrlChecker` | `actions/assets/data.minify.json` | 2023-06-18 | `+1/-369` | 1 (`data.minify.json`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `Bigstud209/UrlChecker` | `shelf/PR68` | 2022-10-30 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Bigstud209/UrlChecker` | `tmp/settings-screen` | 2022-10-30 | `+1/-663` | 20 (`AndroidManifest.xml`, `MainActivity.java`, `ModulesActivity.java`, ...) | **Already upstream** | 2022 `SettingsActivity` draft already implemented upstream |
| `Azure-Helper/UrlChecker` | `master` | 2022-09-13 | `+2/-678` | 3 (`alpha.yml`, `ic_about.xml`, `activity_main.xml`) | **Rejected (stale)** | 2022 icon edit + `alpha.yml` zip password removal |
| `dperruso/UrlChecker` | `Ilithy-patch-1` | 2022-12-18 | `+5/-529` | 3 (`full-description.txt`, `full-description.txt`, `strings.xml`) | **Already upstream** | 2022 fastlane/string edits merged upstream |
| `dperruso/UrlChecker` | `it-locale` | 2022-12-18 | `+7/-529` | 4 (`full-description.txt`, `short-description.txt`, `title.txt`, ...) | **Already upstream** | 2022 Italian translation merged upstream |
| `dperruso/UrlChecker` | `shelf/PR68` | 2022-12-18 | `+1/-734` | 8 (`OpenModule.java`, `mopen_auto.xml`, `mopen_exclude_recents.xml`, ...) | **Upstream mirror** | Upstream mirror branch (`shelf/PR68`, preset flags) |
| `Rtizer-9/UrlChecker` | `master` | 2022-06-26 | `+2/-960` | 0 | **Rejected (empty diff)** | Auto-apply commit immediately reverted (`0` net files) |
| `Huhni/UrlChecker` | `master` | 2022-04-05 | `+5/-985` | 5 (`README.md`, `build.gradle`, `AndroidManifest.xml`, ...) | **Rejected (obsolete)** | 2022 `exported=true` (upstream) + hardcoded `t.co`/Twitter clicker |
| `Raj9039852537/UrlChecker` | `actions/assets/data.minify.json` | 2025-05-07 | `+5/-536` | 1 (`data.minify.json`) | **Upstream mirror** | Upstream/bot asset update (`data.minify.json`, merged via #25) |
| `Raj9039852537/UrlChecker` | `dependabot/github_actions/actions/checkout-4` | 2025-05-07 | `+1/-536` | 7 (`alpha.yml`, `assets-updater.yml`, `codacy.yml`, ...) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/checkout-4`) |
| `Raj9039852537/UrlChecker` | `dependabot/github_actions/actions/setup-java-4` | 2025-05-07 | `+1/-536` | 2 (`alpha.yml`, `validate-gradle-build-test.yml`) | **Rejected (bot PR)** | Unmerged Dependabot branch (`dependabot/github_actions/actions/setup-java-4`) |
| `Ilithy/UrlChecker` | `Add-Obtainium-badge` | 2026-03-29 | `+1/-169` | 1 (`README.md`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`Add-Obtainium-badge`) |
| `Ilithy/UrlChecker` | `Github-apk]-release` | 2026-03-29 | `+13/-526` | 4 (`release.yml`, `.gitignore`, `build.gradle`, ...) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`Github-apk]-release`) |
| `Ilithy/UrlChecker` | `Ilithy-patch` | 2026-03-29 | `+2/-597` | 1 (`strings.xml`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`Ilithy-patch`) |
| `Ilithy/UrlChecker` | `Pull-request-template` | 2026-03-29 | `+3/-641` | 1 (`pull_request_template.md`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`Pull-request-template`) |
| `Ilithy/UrlChecker` | `Template-test` | 2026-03-29 | `+13/-497` | 7 (`bug_report.md`, `bug_report.yml`, `config.yml`, ...) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`Template-test`) |
| `Ilithy/UrlChecker` | `actions/assets/data.minify.json` | 2026-03-29 | `+24/-556` | 1 (`data.minify.json`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`actions/assets/data.minify.json`) |
| `Ilithy/UrlChecker` | `badge` | 2026-03-29 | `+41/-641` | 3 (`README.md`, `featured.png`, `test.txt`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`badge`) |
| `Ilithy/UrlChecker` | `dependabot/github_actions/actions/checkout-5` | 2026-03-29 | `+1/-169` | 8 (`alpha.yml`, `assets-updater.yml`, `codacy.yml`, ...) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`dependabot/github_actions/actions/checkout-5`) |
| `Ilithy/UrlChecker` | `dependabot/github_actions/actions/labeler-6` | 2026-03-29 | `+1/-169` | 1 (`labeler.yml`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`dependabot/github_actions/actions/labeler-6`) |
| `Ilithy/UrlChecker` | `dependabot/github_actions/actions/setup-java-5` | 2026-03-29 | `+1/-169` | 4 (`alpha.yml`, `codeql-analysis.yml`, `release.yml`, ...) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`dependabot/github_actions/actions/setup-java-5`) |
| `Ilithy/UrlChecker` | `dependabot/github_actions/github/codeql-action-4` | 2026-03-29 | `+1/-169` | 2 (`codacy.yml`, `codeql-analysis.yml`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`dependabot/github_actions/github/codeql-action-4`) |
| `Ilithy/UrlChecker` | `dependabot/github_actions/gradle/actions-5` | 2026-03-29 | `+1/-169` | 3 (`alpha.yml`, `release.yml`, `validate-gradle-build-test.yml`) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`dependabot/github_actions/gradle/actions-5`) |
| `Ilithy/UrlChecker` | `update-issue-template` | 2026-03-29 | `+7/-497` | 6 (`bug_report.md`, `bug_report.yml`, `config.yml`, ...) | **Rejected (repo meta)** | Old template/badge/release-script experiment (`update-issue-template`) |

#### Zero-ahead & deleted/empty forks (129 zero-ahead branches + 8 deleted/empty)

| Fork | Branch(es) | `pushed_at` | Ahead/Behind | Files | Verdict | Reason |
|---|---|---|---|---|---|---|
| `bonchhunter/URLCheck` | `master` | 2026-11-22 | `0/-0` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `sovallep/URLCheck` | `master` | 2026-11-08 | `0/-0` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `ozmo998/URLCheck` | `master` | 2026-11-19 | `0/-0` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `changfengpolang6/URLCheck` | `master` | 2026-11-08 | `0/-0` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `soydeboca/URLCheck` | `master` | 2026-11-08 | `0/-0` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `zwjy99/URLCheck` | `master` | 2026-10-25 | `0/-1` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `comilucifer-alt/URLCheck` | `master` | 2026-10-25 | `0/-1` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `YoussefHeshamAlsoly/URLCheck` | `master` | 2026-10-18 | `0/-7` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `oooppet/URLCheck` | `master` | 2026-10-22 | `0/-7` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `MD-Muzahidul-Islam-Sabbir/URLCheck` | `master` | 2026-10-18 | `0/-7` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `AnonymousDevTeam/URLCheck` | `master` | 2026-10-18 | `0/-7` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `namixm/URLCheck` | `master` | 2026-10-11 | `0/-8` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `MiMoHo/TrianguloY-URLCheck` | `master` | 2026-10-15 | `0/-8` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `huggingkind/Revurldev` | `master` | 2026-10-11 | `0/-8` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `fuyuu575/URLCheck` | `master` | 2026-09-14 | `0/-13` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `FEL-33/URLCheck` | `master` | 2026-09-14 | `0/-13` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `jimsug/URLCheck` | `master` | 2026-09-14 | `0/-13` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `skynet-05/URLCheck` | `master` | 2026-11-08 | `0/-13` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `sr3050440-code/URLCheck` | `master` | 2026-09-12 | `0/-15` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `hizbu996-art/URLCheck-barq` | `master` | 2026-08-13 | `0/-17` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `mohamedmagdysayed2040-prog/URLCheck` | `master` | 2026-08-13 | `0/-17` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `PingouinFerreux/URLCheck` | - | 2026-08-13 | - | - | Empty (no branches / non-ancestor) | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `ShiroiKuma0/shiroikuma-renketsujoka` | `master` | 2026-09-04 | `0/-18` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `dvbavel/URLCheck` | `master` | 2026-08-03 | `0/-18` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `dkguptadk8722-dev/URLCheck` | `master` | 2026-08-05 | `0/-18` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `8099bb4X21/URLCheck` | `master` | 2026-09-26 | `0/-18` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `mvanhorn/URLCheck` | `master` | 2026-09-09 | `0/-19` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `qpst4/URLCheck-0` | `master` | 2026-06-16 | `0/-20` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `S4tyendra/URLCheck` | `master` | 2026-09-01 | `0/-16` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `VVytai/URLCheck` | `master` | 2026-11-22 | `0/-13` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `A-Archives-and-Forks/UrlChecker` | `master` | 2026-09-14 | `0/-13` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `weblate/UrlChecker` | `master` | 2026-11-14 | `0/-477` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `PabloOQ/UrlChecker` | `assets-updater-action`, `dummy`, `master`, `regex-array`, `test` | 2026-11-22 | `0/-609` (+4 more) | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `NicholasThyssen/URLCheck` | `master` | 2025-09-30 | `0/-60` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `zerserob/URLCheck` | `master` | 2026-04-16 | `0/-20` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `baddyallen/URLCheck` | `master` | 2026-04-16 | `0/-20` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `joshuawright85-commits/URLCheck` | `master` | 2026-04-02 | `0/-23` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `PyNAABO/URLCheck` | `master` | 2026-02-06 | `0/-30` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `alhazred64/URLCheck` | `master` | 2026-02-17 | `0/-30` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `NotExist/URLCheck` | `master` | 2026-01-19 | `0/-35` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `mdmustakim124com-boop/URLCheck` | `master` | 2026-04-07 | `0/-23` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `utafrali/URLCheck` | `master` | 2026-04-13 | `0/-30` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `coderj001/URLCheck` | `custom_redirect_url`, `feat/redirect-module`, `master` | 2026-03-30 | `0/-30` (+2 more) | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `ez-me/URLCheck` | `master` | 2026-01-08 | `0/-39` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `ayanarshad/URLCheck` | `master` | 2025-12-22 | `0/-45` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `snajperek123/URLCheck` | `master` | 2025-11-21 | `0/-55` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `ottnorml/TrianguloY_URLCheck` | `master` | 2026-04-11 | `0/-23` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `tofansadat5-cmyk/URLCheck` | `master` | 2026-01-19 | `0/-35` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `adesso636-spec/URLCheck` | `master` | 2025-12-22 | `0/-45` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `OctoberNicole/URLCheck_Android` | `master` | 2025-10-24 | `0/-56` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `hendrik1337/URLCheck` | `master` | 2025-10-24 | `0/-56` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `safan77260-png/URLCheck` | `master` | 2025-11-21 | `0/-55` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `31cotysmith31/URLCheck` | `master` | 2025-10-16 | `0/-57` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `windlow/URLCheck` | `master` | 2025-10-16 | `0/-57` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `sfukada77-png/URLCheck` | `master` | 2025-10-24 | `0/-56` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `nonomal/URLCheck` | `master` | 2025-10-24 | `0/-56` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `AladdinGPT/URLCheck` | `master` | 2025-08-21 | `0/-63` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Wan-Ian/URLCheck` | `master` | 2025-08-03 | `0/-65` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `sasarax/URLCheck` | `master` | 2025-06-29 | `0/-77` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `kidhanis/URLCheck` | `master` | 2025-07-23 | `0/-72` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `S3n3scH/URLCheck` | `master` | 2025-06-29 | `0/-77` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `d-subrahmanyam/URLCheck` | `master` | 2025-05-13 | `0/-97` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `miniBill/URLCheck` | `master` | 2025-07-06 | `0/-77` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `dabe71064/URLCheck` | `master` | 2025-09-04 | `0/-63` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `hmdanjkr-cmyk/URLCheck` | `master` | 2025-09-04 | `0/-63` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `cnbdhr62/TrianguloY-URLCheck-449` | - | 2025-03-28 | - | - | Deleted / 404 | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `evg58/URLCheck` | `master` | 2025-06-08 | `0/-77` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `uno-ghost/URLCheck` | `master` | 2025-04-03 | `0/-114` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Quinta-5/URLCheck` | `master` | 2025-05-13 | `0/-97` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `omeos-fork/TrianguloY.URLCheck` | - | 2025-03-02 | - | - | Deleted / 404 | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `Naruto6249/URLCheck` | `master` | 2025-02-02 | `0/-167` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Kishanjvaghela/URLCheck` | `master` | 2025-02-27 | `0/-147` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `bitilk/URLCheck` | `master` | 2025-03-22 | `0/-123` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `necimye/URLCheck` | `master` | 2025-01-18 | `0/-167` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `yahyan7666/URLCheck` | `master` | 2025-04-10 | `0/-114` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `Dinesh281198/URLCheck` | `master` | 2025-03-13 | `0/-139` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `Monday1908/URLCheck` | `master` | 2024-11-28 | `0/-169` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `illuminated-shade/URLCheck` | `master` | 2024-11-28 | `0/-169` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `PmPPolska/UrlChecker` | - | 2024-09-30 | - | - | Deleted / 404 | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `Dinesh2811/URLCheck` | `master` | 2025-03-02 | `0/-143` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `ninadnaik10/UrlChecker` | `master` | 2024-09-05 | `0/-212` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `vecnathewhisperd0/UrlChecker` | `master` | 2024-11-03 | `0/-187` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `Laiiev/UrlChecker` | - | 2024-04-06 | - | - | Deleted / 404 | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `2447776119A/UrlChecker` | `master` | 2024-08-22 | `0/-221` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `anoop-b/UrlChecker` | `master`, `webhook` | 2024-11-18 | `0/-187`, `0/-185` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `shishka-b0b/UrlChecker` | `master` | 2024-06-17 | `0/-242` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `Bellabas23/UrlChecker` | `master` | 2024-07-08 | `0/-238` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `snake-git/UrlChecker` | `master` | 2024-03-03 | `0/-282` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `hjude404/UrlCheckerFORK` | `master` | 2024-02-15 | `0/-289` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Suhatec/UrlChecker` | `master` | 2024-04-07 | `0/-264` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `chomsin/UrlChecker` | - | 2024-05-14 | - | - | Deleted / 404 | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `rsohlot/UrlChecker` | `master` | 2024-01-14 | `0/-299` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Amr9990/UrlChecker` | - | 2023-12-14 | - | - | Deleted / 404 | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `jpluimers/TrianguloY.UrlChecker` | `master` | 2024-02-10 | `0/-289` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `No1illuminati/UrlChecker` | - | 2025-06-13 | - | - | Deleted / 404 | Fork deleted/404 or non-ancestor (`PingouinFerreux` `gh-pages`) |
| `agusku003/UrlChecker` | `master` | 2023-08-11 | `0/-339` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `lrq3000/UrlChecker` | `master` | 2024-02-10 | `0/-289` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `TeaqariaWTF/UrlChecker` | `master` | 2024-08-08 | `0/-231` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `vaimalaviya1233/UrlChecker` | `master` | 2023-11-19 | `0/-313` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `Whiteknightai2/UrlChecker` | `master` | 2023-06-29 | `0/-351` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `royt93/4_UrlChecker` | `master` | 2024-05-03 | `0/-259` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `bjbjorn/UrlChecker` | `New_button_+_issue98`, `master` | 2023-05-27 | `0/-375`, `0/-385` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `gzqx/UrlChecker` | `master` | 2023-04-16 | `0/-417` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `54m4d/UrlChecker` | `master` | 2023-04-19 | `0/-415` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `2002suraj456/UrlChecker` | `master` | 2024-01-10 | `0/-299` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `xxcavemanxx/UrlChecker` | `master` | 2024-07-15 | `0/-238` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Dau-Tuan-216390064/UrlChecker` | `master` | 2023-01-23 | `0/-517` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `vaginessa/UrlChecker-1` | `master` | 2023-04-17 | `0/-416` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `mikjori/UrlChecker` | `master` | 2022-12-19 | `0/-523` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Swipdaggoe/UrlChecker` | `master` | 2023-01-19 | `0/-517` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `the-blank-x/UrlChecker` | `excluderegex`, `master`, `optional-groups`, `optional-groups-dev` | 2023-06-18 | `0/-446` (+3 more) | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `LaneGL/UrlChecker` | `master` | 2022-12-18 | `0/-529` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `tosunkaya/UrlChecker` | `master` | 2022-11-10 | `0/-578` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Bigstud209/UrlChecker` | `master` | 2022-10-30 | `0/-579` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `krishnapandey24/UrlChecker` | `master` | 2022-08-17 | `0/-793` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `metezd/UrlChecker` | `master` | 2022-08-07 | `0/-923` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `dperruso/UrlChecker` | `master` | 2022-12-18 | `0/-529` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `guillaumeboehm/UrlChecker` | `master` | 2022-07-16 | `0/-925` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `ClintonKildepstein/UrlChecker` | `master` | 2022-02-13 | `0/-985` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `m8e/UrlChecker` | `master` | 2022-01-29 | `0/-991` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `sniten/UrlChecker` | `master` | 2021-08-26 | `0/-1013` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Loww5241/UrlChecker` | `gh-pages`, `mastermaster` | 2022-03-05 | `0/-0`, `0/-991` | - | Skipped (`0` ahead) | No commits ahead of upstream `master` |
| `Raj9039852537/UrlChecker` | `assets-updater-action`, `master` | 2025-05-07 | `0/-626`, `0/-105` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |
| `Ilithy/UrlChecker` | `master` | 2026-03-29 | `0/-169` | - | Skipped (`0` ahead) | Branch(es) on same fork with `0` commits ahead |

---

## Lessons & API rate-limit notes

- **GraphQL batching avoids rate limits**: Fetching branch heads for all 142 forks via GraphQL (`112 / 5,000` GraphQL points used) cut ~140+ separate REST `/branches` calls to ~10 batched queries; combined with `compare` and `issues`/`commits` inspections, total REST usage across the deep scan stayed at **`957 / 5,000` core calls** (`4,043` remaining).
- **Never rely on `pushed_at` or default branches alone**: `oooppet/URLCheck` (`development` vs `0`-ahead `master`), `8099bb4X21` (`patched`), `ShiroiKuma0` (`custom`), and `ottnorml` (`copilot/...`) all lived on non-default branches, while older forks held active PR/feature branches pushed prior to `2026-06-01`.
- **De-duplicate upstream mirror branches**: Over 25 forks clicked GitHub's "Copy all branches" option, mirroring upstream `shelf/*`, `wip/*`, `tests/*`, and `actions/*` heads (`265` ahead branches overall collapse to ~`35` unique non-mirror code branches).
- **Check upstream PR history even when `ahead_by > 0`**: Squash/tweak merges leave fork branches showing `ahead_by > 0` (`mvanhorn` `+2/-19` merged as `#587`; `snigdhasjg` `+1/-13` merged as `#604`; `utafrali`, `ez-me`, `kidhanis`, `NicolaeFericitu` merged as `#567`, `#550`, `#507`, `#543`). Always check upstream PRs before assuming code is unmerged.
- **JDK 17 compilation != Android `minSdk 19` lint pass**: `oooppet`'s `DirectOpen` and `URLBlocklist` compiled cleanly under JDK 17 + `compileSdk 36`, yet CI `:app:lintDebug` failed with 4 `NewApi` errors (`ArrayList#sort`, `Comparator.comparing`, `CharSequence#codePoints`, `IntStream#anyMatch`, all API 24+). Local builds without an Android SDK miss `lintDebug` — inspect new Java 8+ standard library calls against `minSdkVersion 19` before pushing, and verify `Validate gradle build test` passes on GitHub Actions.
- **Sanitize personal defaults & check CodeQL data-flow paths**: Always inspect defaults in cherry-picked classes (`URLBlocklist` shipped with `"iltalehti.fi"` -> stripped in `d350a005`) and never cherry-pick fork `signing/` folders or app-ID changes. Also note CodeQL tracks new call paths into pre-existing code (`AndroidUtils.java:293` polynomial regex flagged on PR `#52` because `getOpenUrl(intent)` introduced a tracked intent path).
