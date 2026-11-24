# Link-interception research & fork findings

Thread: `fork-scanner` (second, deeper scan across all 142 forks of `TrianguloY/URLCheck`, including a dedicated top-priority pass for link-interception improvements). Complements [../link-interception.md](../link-interception.md).

## Problem & baseline on `master`

Requirement: ensure links from almost any app (e.g., Google app / Google Search "Visit", Samsung "Open with" chooser, in-app links) open in URLCheck instead of jumping directly to Chrome.

Current `master` (`app/src/main/AndroidManifest.xml`, `dialogs/MainDialog.java`, `services/CustomTabs.java`) already provides:
- `MainDialog` filters for `ACTION_VIEW` (`DEFAULT`, `BROWSABLE`) on `http`/`https` plus odd-case variants (`Http`, `Https`, `HTTP`, `HTTPS`), a `VIEW` filter for MIME types `text/html`, `text/plain`, and `application/xhtml+xml`, host filters for libredirect-style redirects, `ACTION_SEND` for `text/*` & `application/xhtml+xml`, and `ACTION_MAIN` + `CATEGORY_APP_BROWSER`.
- `activity-alias` handling `ACTION_PROCESS_TEXT` (`text/plain`).
- `services.CustomTabs`: an empty stub service exporting `android.support.customtabs.action.CustomTabsService` so apps binding to the default browser's Custom Tabs provider can bind to URLCheck rather than bypassing to Chrome.
- Fork PRs merged prior to this report: [#40](https://github.com/Daniel-OS01/URLCheck/pull/40) (`text/html` shares, upstream `#609`), [#45](https://github.com/Daniel-OS01/URLCheck/pull/45) (deny browser in open/share module, upstream `#523`), [#47](https://github.com/Daniel-OS01/URLCheck/pull/47) (share automation optional `package`/`component` arguments from `8099bb4X21`), and [#51](https://github.com/Daniel-OS01/URLCheck/pull/51) (self-exclusion in `OpenModule`/`ShareUtility` so automations never loop back to URLCheck itself).

---

## Upstream issues & PRs checked

| Upstream # | Type & State | Title / Topic | Relevance to link interception |
|---|---|---|---|
| [#527](https://github.com/TrianguloY/URLCheck/issues/527) | Issue (open) | Unable to open custom tabs | Reports failure opening Custom Tabs; relates to stub `services/CustomTabs.java`. |
| [#510](https://github.com/TrianguloY/URLCheck/issues/510) | Issue (open) | Firefox custom tabs remain in recents screen after pressing back | Custom Tabs session/task behavior (`CTabs.java` / `CustomTabs.java`). |
| [#320](https://github.com/TrianguloY/URLCheck/issues/320) | Issue (open) | Allow generic URIs (include more schemas) | Non-HTTP/HTTPS scheme interception (`VIEW` filters). |
| [#494](https://github.com/TrianguloY/URLCheck/issues/494) | Issue (open) | Allow other mimetypes for other intents | `SEND` / `VIEW` MIME filter coverage (`#609` / fork `#40` addressed `text/html` `SEND`). |
| [#353](https://github.com/TrianguloY/URLCheck/issues/353) | Issue (open) | "Open with" showing only one option due to new app | Android chooser / package query filtering. |
| [#583](https://github.com/TrianguloY/URLCheck/issues/583) | Issue (open, fork [#12](https://github.com/Daniel-OS01/URLCheck/issues/12)) | Open with options not showing | Browser discovery / `<queries>` / handler visibility. |
| [#18](https://github.com/TrianguloY/URLCheck/issues/18) | Issue (open) | Allow auto-opening links in default apps | Auto-open/auto-continue to the real browser without dialog friction. |
| [#192](https://github.com/TrianguloY/URLCheck/issues/192) | Issue (open) | List of trusted/blocked domains and apps to process some links automatically | Domain-level auto-open / auto-block rules. |
| [#419](https://github.com/TrianguloY/URLCheck/issues/419) | Issue (open) | Ignore/Block Specific URLs | Silently ignore/block unwanted incoming domains. |
| [#519](https://github.com/TrianguloY/URLCheck/issues/519) | Issue (open) | Auto open links without dialogue box if not flagged as malware... | Auto-open safe links directly to chosen browser. |
| [#405](https://github.com/TrianguloY/URLCheck/issues/405) | Issue (open) | Better pop-up UI / LinkSheet-like chooser clarity | Browser chooser UX and app categorization. |
| [#47](https://github.com/TrianguloY/URLCheck/issues/47) | Issue (open) | Option to show all apps that can open a link (UI improvement) | Chooser app list completeness & ordering. |
| [#532](https://github.com/TrianguloY/URLCheck/issues/532) | Issue (open) | Browser Share Intents(?) from app list | Share-sheet / send intent integration in browser selector. |
| [#301](https://github.com/TrianguloY/URLCheck/pull/301) | PR (closed) | Incognito for chromium browsers (`PabloOQ`) | Post-open Chrome UI automation (`UrlHelperService`); closed by maintainer due to >2,500 LOC complexity. |
| [#571](https://github.com/TrianguloY/URLCheck/issues/571) | Issue (open, `wontfix`) | Incognito for Chromium/Brave etc doesn't work | Maintainer `wontfix` for proprietary Brave/Chromium private-tab hacks. |

---

## Link-interception findings across all 142 forks

Every branch (`394` total across `142` forks) was checked, with targeted inspection of any commit touching `AndroidManifest.xml`, `services/CustomTabs.java`, `dialogs/MainDialog.java`, `modules/companions/IntentApp.java`, `ShareUtility.java`, `OpenModule.java`, `CTabs.java`, `Incognito.java`, `LastOpened.java`, `ShortcutsActivity.java`, `UrlHelperService.java`, `DirectOpen`, or `URLBlocklist`.

### 1. Accepted & PR'd (`[link-interception]`)

- **Fork / Branch**: [`oooppet/URLCheck` (`development`)](https://github.com/oooppet/URLCheck/tree/development) (pushed `2026-10-22`)
- **Author**: Tero Kallio
- **Fork PR**: **[Daniel-OS01/URLCheck#52](https://github.com/Daniel-OS01/URLCheck/pull/52)** (merged into `master` as `a2dc6e7b`) — `[link-interception] Add URLBlocklist and Direct Open browser forwarding (oooppet/URLCheck)`
- **Source commits cherry-picked (`git cherry-pick -x`)**:
  - `ff7c85f9b2607615390c3e11f4869690934c39fa` -> `a7c5a965` (*Add incoming URL domain blocking*)
  - `33fcca4d813923dc1eb9e8a598463bac5f357994` -> `4f9e01bf` (*Add configurable URLBlocklist with persistent domain filtering*)
  - `34275143f0dfcb300271d097c65e4e2713f0d433` -> `7807dd25` (*Add Direct Open browser forwarding and update blocklist icon*)
  - Excluded commit: `c6a899b541` (*Set custom application ID*).
- **Local follow-ups on `fork-pick/oooppet-urlblocklist-direct-open`**:
  - Reconciled `MainDialog.java` with merged PR [#26](https://github.com/Daniel-OS01/URLCheck/pull/26) (`savedInstanceState` restoration, `ensureDialogInitialized()` called in restore path, `awaitingSelection` guard) and merged PR [#40](https://github.com/Daniel-OS01/URLCheck/pull/40) (`getCharSequenceExtra(Intent.EXTRA_TEXT)`).
  - `d350a005`: Removed fork author's personal default domain (`"iltalehti.fi"`) from `URLBlocklist.java` so the blocklist starts empty.
  - `09abf815`: Replaced Java 24 APIs (`List#sort`, `Comparator.comparing`, `CharSequence#codePoints`, `IntStream#anyMatch`) in `DirectOpen.java` and `URLBlocklist.java` with `minSdkVersion 19`-compatible code after `:app:lintDebug` flagged 4 `NewApi` errors; **`build` (`Validate gradle build test`) passed** on `09abf815` (note: CodeQL flagged a pre-existing polynomial regex at `AndroidUtils.java:293` reached via `MainDialog.getOpenUrl(intent)`).
- **What it delivers for link interception**:
  - **Direct Open (`DirectOpen`, `DirectOpenActivity`)**: Opt-in forwarding when URLCheck is set as the system default browser. Enumerates general HTTP+HTTPS `CATEGORY_BROWSABLE` + `CATEGORY_DEFAULT` browsers, explicitly excludes URLCheck (`activity.packageName.equals(context.getPackageName())`), rejects site-specific deep-link handlers and disabled/suspended apps, and adds `<queries>` entries for `http`/`https` `ACTION_VIEW` `BROWSABLE`. Launches the chosen external browser before dialog creation, falling back to `MainDialog` with an error banner and paused automations (`!directOpenFailed`) if launch fails.
  - **URLBlocklist (`URLBlocklist`, `URLBlocklistActivity`)**: Filters incoming URLs by domain/subdomain before `MainDialog` creates modules or launches browsers (relates to `#18`, `#192`, `#419`, `#519`).

### 2. Already merged / handled earlier

- **`8099bb4X21/URLCheck [patched]`** (`af9dbf06`, `c2c08ce6`): Optional `package`/`component` arguments on share automation intents (`#405`/`#47`) -> merged as [PR #47](https://github.com/Daniel-OS01/URLCheck/pull/47).
- **`NicholasThyssen/URLCheck [Open-source-Contribution]`** (`22aa911a`): Deny-browser option in open/share module (upstream `#523`) -> merged as [PR #45](https://github.com/Daniel-OS01/URLCheck/pull/45).
- **`MiMoHo/TrianguloY-URLCheck [fix/494-send-mimetypes]`** (`23804f79`) & **`[fix/dialog-in-recents]`** (`d0945f0d`): `text/html` share MIME + styled `CharSequence` extraction (`#609` -> PR [#40](https://github.com/Daniel-OS01/URLCheck/pull/40)) and dialog recents retention (`#619` -> PR [#30](https://github.com/Daniel-OS01/URLCheck/pull/30)).

### 3. Inspected but rejected (with reasons)

- **`jprapstyle/URLCheck [master]`** (`b3002823`..`83279a5a`): Adds `BrowserPreference` (long-press pin fallback browser) and `IntentApp.isUrlSpecificHandler()` to prioritize app-specific handlers over a pinned fallback browser. Explicitly does **not** auto-open or alter link interception; bundled in a monolithic Enlion squash (`3.5-enlion.1`) with orientation settings, file-based `LogStore`, LRU caches, and Korean/English `docs/LOCAL_CHANGES*`. Worth extracting cleanly later as a standalone UX enhancement if desired.
- **`PabloOQ/UrlChecker`** (`incognito-clipboard`, `incognito-clipboard-copy`, `incognito-module`, `incognito-service`, `open-share`): Implements `ForceURL` / `UrlHelperService` (`AccessibilityService` + overlay) to drive Chrome's UI into Incognito mode after opening a link (upstream [#301](https://github.com/TrianguloY/URLCheck/pull/301), closed as >2,500 LOC complexity). Does **not** perform incoming link capture.
- **`skynet-05/URLCheck [feature/tor-sandbox-preview]`** (`4595ec4f`..`61901447`): Merged `PROCESS_TEXT` alias into `MainDialog` and added `BravePrivateTabHelper` / `OPEN_PRIVATE_TAB` `<queries>` + clipboard fallback. Entangled with the `Link Guard` whitelabel (`androidx.core`/`androidx.webkit`, committed keystore, OTA updater) and hits upstream `#571` (`wontfix`).
- **`Huhni/UrlChecker [master]`** (`47a26bb9`, `50c426cc`, `a809dd8b`): 2022 `exported="true"` (already upstream) plus hardcoded auto-click for `t.co`/Twitter status checks (superseded by `AutomationRules`).

### 4. Categories searched with **none found** (across all 142 forks, all 394 branches)

| Link-interception category | Result across all 142 forks | Details |
|---|---|---|
| Real `CustomTabsService` (`warmup`, `newSession` -> `true`, `mayLaunchUrl`, `validateRelationship` in `services/CustomTabs.java`) | **None found** | Only touched by package renames (`3ekodomo`, `vimal-gandhi-dev`), cosmetic `SkToast` (`ShiroiKuma0`), and stale merge diff (`ninadnaik10`). |
| Explicit-package intent handling / bypass interception | **None found** | Android enforces explicit `intent.setPackage(...)` before any third-party app receives the intent. |
| Extra `<intent-filter>` coverage (`ACTION_WEB_SEARCH`, `ACTION_SEARCH`, `ACTION_VIEW` without `BROWSABLE`, extra schemes `#320`, host-less filters, `android:autoVerify`) | **None found** | Only `#609` (`text/html` `SEND`, already merged) and `oooppet` `<queries>` `http`/`https` `BROWSABLE` exist across all forks. |
| Clipboard / Accessibility **incoming** link capture | **None found** | `PabloOQ`'s `UrlHelperService` automates Chrome's incognito menu post-launch rather than capturing incoming links. |
| Android 12+ verified-link / `"Open by default"` dialog changes | **None found** | Only OS Settings (`Default browser app` + disabling `"Open supported links"` per app) control domain verification. |
| Share-sheet / Direct Share targets (`ChooserTargetService` / `ShortcutInfo`) | **None found** | Beyond merged PRs `#45` / `#47` and rejected `jprapstyle` / `skynet-05`, no fork adds direct-share targets. |

---

## Implications & next steps if Google Search / apps still bypass URLCheck

1. **OS settings remain decisive** (see [../link-interception.md](../link-interception.md)): set URLCheck as Android's default browser, and for apps that verify or force-open their own links (Google app, Chrome, YouTube, etc.), turn off **Open supported links** under *Settings -> Apps -> \<app\> -> Open by default* (note: in Google app settings, turning off *"Open web pages in the app"* stops in-app CCT/Chrome handoff when Google respects the system browser, though explicit `setPackage("com.android.chrome")` intents still bypass Android's chooser by OS design).
2. **Code items left open for future original work** (not found in any fork):
   - Implementing a real `CustomTabsService` (`newSession` returning `true`, `warmup`, `mayLaunchUrl`, `validateRelationship`) in `services/CustomTabs.java` to handle apps that fall back to Chrome when stub binding returns `false`/`null` ([#527](https://github.com/TrianguloY/URLCheck/issues/527), [#510](https://github.com/TrianguloY/URLCheck/issues/510)).
   - Adding generic/non-HTTP URI schemes ([#320](https://github.com/TrianguloY/URLCheck/issues/320)) or `ACTION_WEB_SEARCH` / additional MIME types ([#494](https://github.com/TrianguloY/URLCheck/issues/494)) if desired as opt-in `<activity-alias>` components.
   - Optionally extracting `jprapstyle`'s `IntentApp.isUrlSpecificHandler()` + `BrowserPreference` fallback cleanly without the rest of that fork's squash.
