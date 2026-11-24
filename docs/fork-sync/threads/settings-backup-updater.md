# Thread report: settings-backup updater

How to take a URLCheck settings backup (`.ucbckp`) exported by an older app version, and produce an updated one that imports cleanly into the current app: keep the owner's settings, migrate what changed, add new features, add recommended privacy-preserving configuration, and validate the result without running the app.

> **Privacy.** This report is general. It contains no values from any real backup: no webhook URLs, tokens, API keys, custom domains, personal patterns, rule names or learned-app history. Keys appear only by name with their code defaults. The backup itself, its original and the working scripts that touch real data stay on the owner's machine and must never be committed, uploaded or pasted. The generic tools in [tools/](../tools/) print key names and counts only.

| | |
|---|---|
| Status | done (a backup was updated, validated and delivered locally; this is the reusable write-up) |
| Source backup | exported by app 3.5 (`versionCode 47`, commit `dd85007e`, 2026-04-02) |
| Target | app `4.0.0` (`versionCode 48`) |
| Code analysed | `origin/master` at `5bdfbc88` (tag `v4.0.0` is `b0213d6e`; master is 32 commits ahead of the tag and adds the Redirect skipper module `skipRedirect` and the `open_denylist` preference, see [section 5](#5-version-to-version-migration-notes)) |
| Repo changes during the task | none (read-only); this file and `tools/` are the only writes |

Related: [link-interception.md](../link-interception.md) (why links bypass URLCheck and what is phone setting versus code), [playbook.md](../playbook.md) (Windows/PowerShell pitfalls), [release-and-signing.md](../release-and-signing.md) (the fork build cannot update over the upstream build, so backup/restore is the migration path).

## 1. The `.ucbckp` format

Source of truth: `app/src/main/java/com/trianguloy/urlchecker/activities/BackupActivity.java` (export `~l.170-215`, restore `~l.260-295`, constants `~l.419-440`), `utilities/wrappers/ZipWriter.java`, `ZipReader.java`.

**Container.** A plain ZIP written by Java `ZipOutputStream` (so: deflated entries, streamed with data descriptors, UTF-8 name flag `0x800` set). Zip comment: `<app name> (<applicationId>) <backup label>`, e.g. `URLCheck (com.trianguloy.urlchecker) Backup`. Debug builds use applicationId suffixes `.alpha` / `.test` and version suffixes `-ALPHA` / `-TEST`, so a backup from those builds has a different comment and version string; restore never reads the comment.

**Entries, in the order the app writes them:**

| Entry | Content | Notes |
|---|---|---|
| `version` | `BuildConfig.VERSION_NAME`, UTF-8, no trailing newline | Gate for restore, see [section 2](#2-how-the-app-imports-and-validates) |
| `readme.txt` | text asset `backup_readme.txt` | Never read on restore. When rewriting a backup, reuse the original entry bytes (the bundled asset may differ from an older export only in line endings) |
| `preferences` | non-secret SharedPreferences, one JSON object | only if the "preferences" box was ticked at export |
| `secrets` | secret SharedPreferences, same format | only if "secrets" was ticked; keys listed in [section 2](#2-how-the-app-imports-and-validates) |
| `files/<name>` | internal files, raw bytes | one entry per file; `files/.empty` is written as a marker when there are none |
| `cache/<name>` | cache-type internal files (names starting `hosts_`) | only if "cache" was ticked; not included by default |

**`preferences` / `secrets` payload.** Compact single-line JSON, UTF-8, no BOM, no trailing newline:

```json
{"<key>":{"value":<json value>,"type":"String|Integer|Long|Boolean"}}
```

- `type` is `value.getClass().getSimpleName()` at export and selects the SharedPreferences setter on restore. Restore handles exactly `String`, `Integer`, `Long`, `Boolean`; anything else goes to `assertError("Unknown type")`.
- **Known bug: `Float` preferences are exported but cannot be restored.** `FloatPref` (`jsonPref_size`, the JSON editor text size) exports with `"type":"Float"`, but `restorePreferencesMatching` has no `Float` case. With "delete before restoring" the key is deleted and not put back, so the setting silently resets to its default. Do not rely on a `Float` key surviving a restore; the validator warns about it. (Fix candidate: add a `case "Float" -> editor.putFloat(...)` branch in `BackupActivity`.)
- Insertion order is the app's own (not significant). Duplicate keys must not occur (their handling by Android's JSON parser is not something to rely on; the validator rejects them).
- Only **non-default** values exist. `GenericPref.set(default)` removes the key, so a key that equals its default is redundant (older versions sometimes stored explicit defaults; harmless, and the import accepts them).
- Booleans are JSON `true`/`false`, `Long` is a JSON number (keep it within 64 bits), `Integer` within 32 bits.

**`files/*` payloads.** Internal files that back the JSON "catalog" editors. All are UTF-8 JSON objects:

| File | Written by | Serialisation to imitate |
|---|---|---|
| `files/automations` | `AutomationRules` | Android `org.json` (`JSONObject.toString()`): compact, **`/` is escaped as `\/`**, insertion order kept |
| `files/clearUrlCatalog` | `ClearUrlCatalog` | `org.json` style, same `\/` escaping |
| `files/patterns` | `PatternCatalog` | plain compact JSON (`json.dumps(..., separators=(',', ':'), ensure_ascii=False)`) |
| `files/hosts` | `HostsCatalog` | plain compact JSON |
| `files/redirect` | `RedirectCatalog` (module added after 3.5) | `org.json` style |
| `files/flags_editor_settings` | `FlagsModule` | flags editor configuration (JSON) |

Using the "wrong" serialisation is not an error for the app (Android's `org.json` is lenient and re-saving normalises), but matching byte style keeps a diff against a real export quiet. The generic [orgjson.py](../tools/orgjson.py) reproduces Android's `JSONStringer` escaping (`"`, `\`, `/`, control chars, U+2028/2029) and detects duplicate keys.

**Catalogs fall back silently.** `JsonCatalog.getCatalog()` returns the built-in catalog when the file is absent or is not valid JSON, with no error. A broken catalog file therefore looks like "my rules vanished", not like an import failure. Validate catalog JSON yourself.

**Catalog schemas** (fields the code reads; unknown fields are ignored, which is how pass-through fields such as `description` survive):

| Catalog | Entry fields (key = entry name) |
|---|---|
| automations | `regex` (string or array, full match), `excludeRegex` (string or array), `referrer` (string or array), `action` (string or array), `args` (object), `stop` (bool), `enabled` (bool, default true) |
| patterns | `regex` (string or array), `excludeRegex`, `replacement` (string or array), `automatic`, `enabled` (default true), `decode`, `encode`, `button` (label, new after 3.5), `applied` (label, new after 3.5) |
| hosts | `file`, `hosts`, `color`, `replace` (default true), `enabled` (default true) |
| redirect | `from`, `to`, `auto` |
| clearUrlCatalog | two objects, `providers` and `personal`, each a map of provider objects (ClearURLs format) |

Automation semantics worth knowing when editing (`modules/AutomationRules.java`, `~l.90-120`): rules run in file order; regexes are **full-match** (`String.matches`); a `null` referrer matches any `referrer` filter; `stop:true` ends evaluation after that rule; actions are only registered for modules that are enabled (an action whose module is off is "not found" and shows an error toast when `auto_error_toast` is on). Known actions: `open`, `share`, `copy`, `ctabs`, `incognito`, `close`, `checkStatus`, `unshort`, `scan`, `drawer`, `webhook`, `toast`.

## 2. How the app imports and validates

`BackupActivity.restore()`:

1. `ZipReader` copies the chosen file to a cache file and opens it with `java.util.zip.ZipFile`; entries are looked up **by name**, so entry order, comment and compression do not matter to the import (they matter only for diff-friendliness).
2. **Version gate:** unless "ignore newer" is ticked, `VersionManager.isVersionNewer(<version entry>)` must be false. It compares dot-separated numeric parts; an unparsable version counts as newer; the same string as the app is never newer; more parts than the app on equal prefix counts as newer. Consequence: a backup labelled with an older version imports; relabelling an updated backup to the target version string is accepted and correct because the format did not change.
3. `restorePreferencesMatching`: with "delete before restoring" ticked (default), it first removes every matching key, then puts each key by `type` (`String`, `Integer`, `Long`, `Boolean`). **Any key is accepted**: there is no allow-list, so unknown, retired and future keys are simply pass-through into the SharedPreferences file. Keys that match the secret list are only read from `secrets`, others only from `preferences`.
4. `restoreFilesMatching`: with delete ticked, removes the matching internal files first, then writes each `files/*` entry (the `files/.empty` marker is skipped). Files are written verbatim, they are not parsed at import time.
5. `AndroidSettings.reload`, then the UI is restarted by the activity.

**Secret preferences** (`IS_PREF_SECRET`): `api_key` (VirusTotal), `log_data` (Log module, contains visited URLs), `webhook_url`, `unshorten_token`. They are exported/imported only with the "secrets" box. Treat the `secrets` entry and `log_data` as the most sensitive parts of any backup.

**Cache predicate:** names starting with `hosts_` (downloaded hosts lists). They are rebuilt from the Hosts module ("build database"), so leave "cache" unticked.

**Dialog defaults:** data, preferences, files, secrets and "delete before restoring" are ticked; cache and "ignore newer" are not.

**Pass-through keys.** Because import is permissive, these never cause an import error, so correctness is a matter of the *running* app:
- keys that code no longer reads (retired): ignored forever, take space only;
- keys the code reads with another type than stored: `SharedPreferences` throws `ClassCastException` on read, which is caught by the pref wrapper or crashes the screen that reads it. Always match the declared type (the validator does);
- dynamic keys (`<moduleId>_en`, `_decorate`, `_pinned`, and the `opened <componentA> <componentB>` counters) are pass-through by design.

## 3. Preference reference (derived from the code)

All keys live in the app's single SharedPreferences file (named after the applicationId). "Default" is the value the code uses when the key is absent; types are the backup `type` names. Module ids are in [section 3.2](#32-modules-ids-and-defaults). Verify against the code when updating: the validator's `EXPECT` table encodes this section.

### 3.1 App-wide

| Key | Type | Default | Meaning |
|---|---|---|---|
| `order` | String | all module ids | Module order, `;`-joined, **reverse of the displayed order** (last id is shown first). Ids missing from it are inserted where the `debug` module sits; modules after `drawer` are hidden in the drawer; unknown ids are ignored |
| `dayNight` | Integer | `0` | Theme: 0 device default, 1 dark, 2 light |
| `animations` | Boolean | `true` | UI animations |
| `width` | Integer | `-2` (`WRAP_CONTENT`) | Dialog width in px (the settings text says 0 = automatic) |
| `vertical_gravity` | Integer | `0` | Dialog vertical position: 0 default, 1 top, 2 center, 3 bottom |
| `horizontal_gravity` | Integer | `0` | Dialog horizontal position: 0 default, 1 start, 2 center, 3 end |
| `locale` | String | `""` | Language tag (`language[-country[-variant]]`), empty = system |
| `syncProcessText` | Boolean | `true` | Sync the URL with the editable text when opened via "Open in URLCheck" |
| `tutorial_done` | Boolean | `false` | First-run tutorial shown |
| `jsonPref_size` | Float | caller-supplied | Text size in the JSON catalog editors. **Exported as `Float`, which restore cannot read back** (see section 1) |
| `changelog_lastVersion` | String | `null` | Last version for which the changelog notice was shown (runtime bookkeeping) |
| `auto_enabled` | Boolean | `true` | Master switch of the automations |
| `auto_error_toast` | Boolean | `false` | Show a toast when an automation action fails |

### 3.2 Modules: ids and defaults

Per-module flags use the module id as prefix: `<id>_en` (enabled, default per table), `<id>_decorate` (default `false`, decorate the module header), `<id>_pinned` (default `false`, new after 3.5; keeps a module's block visible; the drawer module cannot be pinned).

| Module | id | Enabled by default | Notes |
|---|---|---|---|
| Log | `log` | yes | |
| Input text | `text` | yes | |
| History | `history` | yes | |
| Status code | `statusCode` | yes | |
| Unshortener | `unshorten` | yes | |
| URL Scanner (VirusTotal) | `virustotal` | **no** | third party, needs a key |
| URL Cleaner (ClearURLs) | `clearUrl` | yes | |
| Redirect skipper | `skipRedirect` | yes | new on master after 3.5 |
| Queries Remover | `removeQueries` | **no** | |
| Uri parts | `uriparts` | yes | |
| Pattern checker | `pattern` | yes | |
| Hosts labeler | `hosts` | yes | |
| Webhook | `webhook` | **no** | third party |
| Redirector | `redirect` | **no** | new after 3.5 |
| Flags editor | `flagsEditor` | **no** | beta |
| Debug/Marker | `debug` | **no** | marker for the position of newly added modules |
| Open and share | `open` | yes | |
| Changelog | `changelog` | yes | |
| Drawer | `drawer` | **no** | everything below it is hidden in a drawer |

Registration order in `ModuleManager` (`~l.40-63`) is the default order; new modules are inserted at the `debug` position when absent from a stored `order`.

### 3.3 Per-module keys

| Module | Key | Type | Default | Meaning |
|---|---|---|---|---|
| Open | `open_closeopen` | Boolean | `true` | Close the dialog after opening |
| Open | `open_closeshare` | Boolean | `true` | Close after sharing |
| Open | `open_closecopy` | Boolean | `false` | Close after copying |
| Open | `open_mergeCopy` | Boolean | `false` | Merge Copy and Share buttons |
| Open | `open_noReferrer` | Boolean | `false` | Hide the source app (referrer) from the open list |
| Open | `open_rejected` | Boolean | `true` | Hide an app that, within 5 s, asks to open the same URL it was just opened with (bounce detector). Affects the list and the no-args `open` automation, not explicit `package`/`component` targets and not sharing |
| Open | `open_iconsize` | Integer | `2` | Icon size: 0 none, 1 small, 2 normal, 3 big |
| Open | `open_denylist` | Integer | `0` | Remove one browser from the open list: 0 none, 1 Chrome, 2 Firefox, 3 Edge, 4 Opera, 5 Brave (master only, after `v4.0.0`) |
| Open | `open_ctabs` | Integer | `0` | Custom-tab option; shared `OnOffConfig` values: 0 auto, 1 default on, 2 default off, 3 always on, 4 always off, 5 hidden |
| Open | `open_incognito` | Integer | `0` | Incognito option, same values as `open_ctabs` |
| Open (companion `LastOpened`) | `lastOpen_perDomain` | Boolean | `false` | Learn the preferred app per domain instead of globally |
| Open (companion `LastOpened`) | `opened <componentA> <componentB>` and `<domain> opened <componentA> <componentB>` | Integer | `0` | Pairwise preference counters in -3..3 that define the "usual app" order. They embed component names and (with per-domain on) domains: **personal data**, count them but never print or publish them |
| Open (companion `RejectionDetector`) | `reject_last` | String | empty list | Records `[openedTimeMillis, component, url]` (newline-separated list, max 3) used for the 5 s bounce check. **Runtime state containing a URL; not a setting, do not restore it** |
| ClearURLs | `clearurl_auto` | Boolean | `false` | Apply cleaning automatically |
| ClearURLs | `clearurl_referral` | Boolean | `false` | **Allow** referral-marketing parameters: false (default) removes them too (`ClearUrlModule ~l.269`), true keeps them |
| ClearURLs | `clearurl_verbose` | Boolean | `false` | Add info lines about which providers/exceptions matched |
| ClearURLs | `clearurl_autoUpdate` | Boolean | `false` | Update the catalog periodically (contacts the catalog host) |
| ClearURLs | `clearurl_catalogURL` | String | upstream ClearURLs `data.minify.json` URL | Where the catalog is downloaded from |
| ClearURLs | `clearurl_hashURL` | String | upstream ClearURLs hash URL | Hash used to detect catalog changes |
| ClearURLs | `clearurl_lastUpdate` | Long | timestamp of the bundled catalog | When the catalog was last updated |
| ClearURLs | `clearurl_lastCheck` | Long | `-1` | Last update check (bookkeeping) |
| ClearURLs | `clearurl_lastAuto` | Boolean | `false` | Whether the last update was automatic (bookkeeping) |
| Redirect skipper | `skipRedirect_auto` | Boolean | `false` | Follow an embedded redirect target automatically (can break sign-in/OAuth links whose parameters look like redirects) |
| Status code | `statusCode_autoRedir` | Boolean | `false` | Replace the URL by the redirect target found by the status check |
| Pattern | `pttrn_regexfix` | Boolean | `true` | Fix optional regex groups (disable if a regex misbehaves) |
| Log | `log_limit` | Integer | `0` | Maximum entries; 0 is unlimited |
| Log | `log_data` (secret) | String | `""` | The log itself |
| VirusTotal | `api_key` (secret) | String | `""` | API key |
| Webhook | `webhook_url` (secret) | String | `""` | Destination |
| Webhook | `webhook_body` | String | JSON template with `$URL$` placeholder | Request body template |
| Unshortener | `unshorten_token` (secret) | String | `""` | Token for the authenticated endpoint (changes which response field is read) |
| Input text | `text_monospace` | Boolean | `true` | Monospace font in the editable URL (new after 3.5) |
| Debug | `debug_errortoast` | Boolean | `false` | Toast on recoverable internal errors, even if the module is off (new after 3.5) |
| Custom tabs service | `ctabs_toast` | Boolean | `false` | Toast when an app binds URLCheck's fake custom-tabs service (diagnostics) |

Retired/migrated: `statusCode_autoCheck` (old status auto-check regex) is converted to an automation by `VersionManager` on first start and the key removed; `redirect_rules` existed only in unreleased intermediate builds of the Redirector (before it moved to the `redirect` catalog file) and is ignored by 4.0.0.

## 4. Practical findings about the format

- **`value`/`type` pairs are the whole contract.** There is no checksum, signature, encryption or obfuscation. Everything can be read with a ZIP library and a JSON parser.
- **A backup is a diff against defaults.** A fresh install exports few keys. Things not in the backup keep their code default, so "add feature X" often means "write nothing" (see 6.1).
- **Import replaces; it does not merge.** With the default "delete before restoring", everything not in the file is removed first. Always take a fresh export from the device before importing an updated file.
- **Not part of any backup** (phone/system state): the Android default-browser role, per-app "Open supported links", the "Open in URLCheck" text-selection entry (a component enabled-state toggled via `PackageManager`), quick-settings tile placement, and the Hosts database cache.
- **Dynamic keys are most of a long-used backup.** The `opened ...` counters and `<id>_en` flags can be an order of magnitude more entries than the named keys; validate them by pattern, not by an allow-list.

## 5. Version-to-version migration notes

Compared with a git diff of every preference declaration (`*Pref("...")`, `PREF =`, `SUFFIX =`) between `v3.5`, `v4.0.0` and master:

| Change | 3.5 | 4.0.0 (`v4.0.0`) | master (`5bdfbc88`) |
|---|---|---|---|
| Removed or renamed keys | - | **none** | none |
| New named keys | - | `text_monospace`, `debug_errortoast`, `vertical_gravity`, `horizontal_gravity`, `<id>_pinned` (suffix) | + `open_denylist`, `skipRedirect_auto` |
| New modules | - | Redirector (`redirect`, off by default; catalog file `files/redirect`) | + Redirect skipper (`skipRedirect`, on by default) |
| New catalog fields | - | pattern `button`, `applied` labels | - |
| Backup format | `version`, `readme.txt`, `preferences`, `secrets`, `files/*`, `cache/*` | identical (`BackupActivity`, `ZipWriter`, `ZipReader`, `VersionManager` unchanged except a string-resource rename for the dialog labels) | identical |
| Runtime migration | - | `VersionManager`: `statusCode_autoCheck` becomes an automation | same |

Consequences for an update from 3.5:

1. Change `version` to the target version string. No key needs renaming.
2. `order`: new modules are not in an old `order`, so the app inserts them at the `debug` position. If the owner moved `debug` into the drawer (a common marker use), new modules appear in the drawer. To place them deliberately, insert their ids into `order` (remember the list is reverse of the display: ids later in the string are shown first/higher). Do not reorder anything else.
3. Do not carry transient state (`reject_last`) over, and do not "improve" values the owner set explicitly without saying so.
4. `open_denylist` and `skipRedirect_auto` exist only on master builds; a backup containing them imports into a `v4.0.0` build but the keys are ignored there (and `skipRedirect` is absent from its module list).
5. A fresh `app/build.gradle` version bump (e.g. 4.1) means: re-run the preference diff (section 9) before trusting this table.

## 6. Recommended settings (with rationale)

Rule of thumb used: apply only things that stay on the device or are reversible; keep every explicit owner choice; list anything that contacts a third party as "available, left off".

### 6.1 Privacy-safe, applied

| Setting | Rationale |
|---|---|
| Keep already-enabled local modules (`clearUrl`, `pattern`, `hosts`, `uriparts`, `skipRedirect` module on) | They parse locally; no network use |
| `clearurl_auto` on (only if the owner already uses ClearURLs) | Cleaning is local; removes tracking parameters before the URL leaves the device |
| `debug_errortoast` on (only if the owner already uses `debug` / `auto_error_toast`) | Makes silent internal errors visible; no data leaves the device |
| `clearUrl_pinned` on | Keeps the ClearURLs result visible so users see that a link was checked (UI only) |
| Pattern labels (`button`, `applied`) on existing patterns | Pure UI text; nothing else in a pattern changes |
| A few generic, **local** patterns (e.g. unwrap the Google AMP viewer to the original page, one-tap strip of AMP-cache hosts, warn on raw IP-address hosts) | Remove a middleman or surface a risk without any network call. Do not add patterns for things the existing ClearURLs/patterns already cover |
| `open_rejected` on | The only protection against two apps bouncing a link between each other. It is the code default; make it explicit if the old backup stored `false` and say so (it changes an owner choice) |
| Automations: an explicit hand-off to a real browser plus a no-args `open` rule, both with `stop:true` and an `excludeRegex` for flagged links | See 6.3; keeps the dialog for anything suspicious |
| Drop `reject_last` from the backup | Transient state with a URL and a timestamp, valid for 5 s; not a setting |

### 6.2 Available but left off (third-party contact or risk)

| Setting | Why it stays off |
|---|---|
| VirusTotal module (`virustotal_en`, `api_key`) | Sends URLs to a third party |
| Webhook module (`webhook_en`, `webhook_url`, `webhook_body`) and any rule using the `webhook` action | Sends links to a destination chosen by the owner |
| Redirector module (`redirect_en`) | Redundant with pattern redirects; rewrites hosts |
| `skipRedirect_auto` | Auto-skipping embedded redirect targets can break login/OAuth links (parameters like `redirect_uri`, `url`, `u`) |
| `lastOpen_perDomain` | Changes how the learned order works; keep the owner's choice, because per-domain counters may already exist in the backup |
| `open_noReferrer` | Not needed once `open_rejected` and explicit targets exist; keep owner choice |
| `clearurl_autoUpdate` | Contacts the catalog host periodically; only keep if the owner already had it |
| Unshortener, Status code | These contact the link's host (or the unshortener service) when they run; keep as the owner had them, and exclude shorteners from auto-continue so unshortening can happen first |
| Hosts lists | Downloaded from third-party sites when the database is rebuilt; the lists are cache, not part of a normal backup |
| `open_ctabs` auto | Would copy an incoming custom-tab request to the hand-off browser; unverified without a device |

### 6.3 Automation design for "continue to the real browser, but not for suspicious links"

Two rules, evaluated in order (after the owner's own rules), both `stop:true`:

1. `open` with `args.package = <browser package>` for plain web links.
2. `open` with no `args` (honors the learned order and the `open_rejected` guard) for links that have a dedicated app.

Both use `excludeRegex` entries so these links stay in the dialog: suspicious TLDs, punycode hosts, non-ASCII hosts, `user@host` links, IP-address hosts, known shorteners (so an unshorten rule can resolve them first and the resolved link then continues), and links that still carry an embedded redirect target (`?url=https://...`, `redirect_uri=`, base64 `aHR0...`). Rule 1 additionally excludes domains better opened in their app so those fall to rule 2.

Loop safety: the Open module never offers URLCheck as a target (`IntentApp.getOtherPackages`) and resolves explicit components rather than the system default. Rules with an explicit `package`/`component` have **no** bounce check, so reserve them for targets that cannot bounce a link back (a normal browser).

Side effect to state in any change report: auto-continue also fires for links the user sends to URLCheck on purpose (share, clipboard shortcut, history), because rules cannot see the source of a link.

## 7. Phone steps to route all links through URLCheck

A backup cannot set Android's default-browser role, per-app link handling, component states or permissions. Menu names are Samsung One UI (Android 14/15) with stock Android in brackets; they are not verified on a device.

0. **Before importing**, export a fresh backup in URLCheck (Settings > Backup / Restore > Backup). Restore deletes existing preferences and files first.
1. **Import:** copy the `.ucbckp` to the phone, URLCheck > Settings > Backup / Restore > Restore, pick it, keep Data and Secrets ticked, Cache unticked, "Delete elements before restoring" ticked. (Tapping a `.ucbckp` in a file manager also opens the same flow.) Rebuild the Hosts database afterwards if the Hosts module is used.
2. **Default browser** (the key step): Settings > Apps > Choose default apps > Browser app > URLCheck. [Stock: Settings > Apps > Default apps > Browser app.] The in-app "Default web browser" buttons open the system dialog. Without this, most apps keep sending web links to Chrome. Only the default browser gets plain web links and only the default browser is used as the custom-tabs provider.
3. **Stop other apps from taking links:** for Chrome and for the Google app: Settings > Apps > `<app>` > Set as default (Open by default) > turn "Open supported links" off and, if shown, tap "Clear defaults". In the Google app, check its settings for the option that decides where web pages open (names change between versions).
4. **Let URLCheck win for the hosts it declares:** Settings > Apps > URLCheck > Set as default > Open supported links > allow, and enable every entry under "Supported links". URLCheck's third manifest `intent-filter` declares a curated host list (the libredirect-style hosts: social/video/wiki/blog sites). On Android 12+ a manual user selection beats a "verified" app, so enabling these makes URLCheck win over the dedicated apps for those hosts; URLCheck then hands the link back to the right app through the Open module.
5. **Hand-off browser:** the Open module excludes URLCheck itself and queries all VIEW handlers (`MATCH_ALL`), choosing by explicit package/component, never by the system default, so a hand-off cannot loop back. If the owner switches browsers, change `args.package` in the hand-off automation to the new browser's package (never `com.trianguloy.urlchecker`). If the target is missing, the dialog still closes and the link is lost (see limit 8).
6. **Entry points not controllable by backup:** the text-selection entry (Settings switch "Add an 'Open in URLCheck' entry on text selection menu") is a component state and is not stored; verify it after the restore. Android 10+ forbids background clipboard reading, so use the "Check URLs from the clipboard" launcher shortcut or quick-settings tile.
7. **Re-test:** open a link from the Google app (Search result > Visit) and from a messaging app. Expect URLCheck's dialog (or the hand-off browser directly, if auto-continue applies). For diagnosis turn on `ctabs_toast` and the Debug module.

Android 12+ verified links: apps with verified domains (video, maps, shopping, Google-owned) are chosen before any browser if their "Open supported links" is on. The default browser is not asked. Fix per app (turn that setting off) or add the host to URLCheck's list in step 4 for hosts URLCheck declares. This is a system limit.

## 8. Known limits needing an app change

Not fixable by a backup; candidates for code threads. File locations are in the 4.0.0 tree (line numbers approximate).

1. **Google app "Visit" opens Chrome.** The system sheet lists only Chrome, i.e. the intent is restricted to Chrome (explicit package or custom-tab provider), not a plain `VIEW https`. `services/CustomTabs.java` is a stub (`onBind` returns a bare `Binder`), so clients that create a custom-tab session fail and may fall back to Chrome. Suggestion: a real `CustomTabsService` (`warmup`, `newSession`, `mayLaunchUrl`, `validateRelationship`) that forwards launches to the dialog. Unverified without a device: use `adb logcat -s CUSTOMTABS` with `ctabs_toast` on. See also [link-interception.md](../link-interception.md).
2. **Explicit-package/component intents** (`setPackage("com.android.chrome")`, `intent:` URIs with a package, `android-app://`) never reach URLCheck. No manifest fix. Last resort on the phone: disable Chrome (breaks things that expect it).
3. **Verified app links (Android 12+) for hosts not in URLCheck's third `intent-filter`** (`AndroidManifest.xml ~l.79-134`) cannot be added to the "Supported links" list; host-less filters are not listed there. A code change could declare more hosts.
4. **Schemes not declared** (`AndroidManifest.xml ~l.41-67`): only `http`, `https` and odd-case variants. Not caught: `intent:`, `market:`, `geo:`, `mailto:`, `tel:`, `ftp:`, app schemes.
5. **`SEND` filters** accept only `text/plain`, `text/html`, `application/xhtml+xml` (`~l.136-145`). Not accepted: `SEND_MULTIPLE`, `text/*`, `text/x-uri`, URLs in `EXTRA_STREAM`.
6. **Automations cannot tell the source of a link** (`AutomationRules.check`, `~l.90-120`): a rule cannot exclude "shared on purpose" from auto-continue. Suggestion: an `intentAction`/`source` field and an exclude-referrer list.
7. **Automation `open` with `package`/`component` has no rejection guard** (`OpenModule` automation `open`, `~l.86-95`; `openUrl(ComponentName, String)`, `~l.261`). Suggestion: apply the bounce check to explicit targets.
8. **`openUrl` always `finish()`es** (`OpenModule ~l.289-293`), even if no app could handle the intent: the link is lost. Suggestion: finish only after a successful start.
9. **Component states and the default-browser role are not backup data**: `.processUrl` is toggled through `PackageManager` in `SettingsActivity.configureProcessUrl`. Suggestion: mirror it into a preference so backups can carry it.
10. **Clipboard** is not detected automatically (Android 10+); only `ShortcutsActivity` and its tile read it (`AndroidManifest.xml ~l.178-232`).
11. **Work profile:** links opened inside the work profile resolve inside that profile; URLCheck installed only in the personal profile never sees them.
12. **Limit 8 interacts with section 6.3:** if the hand-off browser is not installed, auto-continue closes the dialog and the link is lost.

## 9. Validation approach and tools

No emulator or JVM was available for the original run (the installed Java 8 crashed on start), so validation was a faithful port of the app's import logic plus structural checks, run with Python 3 (standard library only). The tools in [tools/](../tools/) are generic and contain no personal data:

| Tool | Purpose |
|---|---|
| [`inspect_backup.py`](../tools/inspect_backup.py) | `python inspect_backup.py <file.ucbckp>`. Prints entries (size, compression, flags, CR/LF counts), key names grouped by kind (counts only for `opened ...` counters, which embed domains), and catalog entry counts/fields. Never prints values |
| [`validate_backup.py`](../tools/validate_backup.py) | `python validate_backup.py <file.ucbckp> [<original.ucbckp>] [--src <repo root>]`. Exit code 0 only when all checks pass. Prints names/counts only |
| [`orgjson.py`](../tools/orgjson.py) | `dumps`/`loads`/`loads_strict` reproducing Android `org.json` serialisation (escapes `/` as `\/`), duplicate-key detection. Import it from your own build script |

`validate_backup.py` checks:

- ZIP: CRC test, no duplicate entries, order starts `version`, `readme.txt`, `version` equals `versionName` in `<src>/app/build.gradle`, all entries deflated, comment as the app writes it, UTF-8 name flag set;
- `preferences` / `secrets` (either may be absent: reported as INFO): single-line compact JSON, strict parse with duplicate-key detection, every entry exactly `{value,type}`, type in `String|Integer|Long|Boolean`, value matches type and range, secret keys only in `secrets` and nothing else there; a `Float` entry is reported as a WARN (restore cannot read it);
- every key is known to the source tree: either a string literal in `app/src/main/java`, a dynamic `<moduleId>_en|_decorate|_pinned` with a real module id (extracted from the code), or an `opened ...` counter in -3..3; selected keys have their type/enum range checked (`EXPECT` table, update it when keys change); `order` has no duplicates and only known module ids (modules missing from it are reported as INFO because the app inserts them);
- catalogs (those present): automations, patterns, hosts, redirect, clearUrl have only known fields; automation actions are known; every regex compiles (Python `re` as an approximation of `java.util.regex`, so exotic Java-only syntax can false-fail and Java-only differences can false-pass);
- safety: no enabled `open` automation targets URLCheck itself;
- with an original: same entry names and order, readme/secrets and untouched catalogs byte-identical, the original automations and patterns unchanged and in order (patterns may only gain `button`/`applied`), and a names-only diff of preferences (added/removed/changed keys).

What it cannot prove: the actual restore on a device, Android's lenient `org.json` parser behaviour, and Java regex semantics. Mitigation: keep the original export, and test the restore on a phone or emulator with the dialog's "delete before restoring" understood.

**Run it from the repo root so `--src` defaults to `.`:**

```powershell
python docs/fork-sync/tools/validate_backup.py "$HOME\Backups\urlcheck\updated.ucbckp" "$HOME\Backups\urlcheck\original.ucbckp"
```

Keep the real backups outside any git work tree (see section 10).

## 10. Risks and rollback

- **Import replaces state.** Everything not in the file is deleted first (non-secret preferences, non-cache files; secrets too if ticked). Export from the device first.
- **Never commit backups.** `log_data` contains visited URLs, `secrets` contains tokens, the learned-order counters contain app and domain history. Restrict the file ACL (`icacls <file> /inheritance:r /grant:r "$env:USERNAME:(R,W)"`), keep it out of repos and chat, and keep scratch extractions outside the work tree.
- **Auto-continue changes behaviour** the most: clean links skip the dialog. Flagged, shortened, IP-host and embedded-redirect links stay. A missing hand-off browser loses the link (limit 8).
- **An automation whose module is off** (e.g. `scan` with the VirusTotal module disabled) is "not found" and shows an error toast if `auto_error_toast` is on. Decide with the owner whether to drop the action or enable the module (which sends URLs to a third party).
- **Silent catalog fallback:** invalid JSON in a `files/*` catalog means the built-in catalog is used with no message.
- **Fork build vs upstream build:** same package id, different signature; they cannot update over each other, so backup/restore is the migration path ([release-and-signing.md](../release-and-signing.md)).
- **Rollback:** restore the original export the same way. Because the updated file is a superset of the original (apart from deliberately changed keys, listed by the validator's diff), the original brings back exactly what was exported. A 3.5-labelled backup is accepted by 4.0.0 (older version).

## 11. Repeating the update for a future app version

1. **Get the inputs.** Original export outside the repo; a checkout of the target source. Copy the original once, never edit it.
2. **Inspect:** `inspect_backup.py original.ucbckp`. Note version, entries, counts.
3. **Diff the code, not the memory.** For the old and new version (tag or commit):
   ```powershell
   git grep -h -E 'Pref(<[^>]*>)?\(\s*"[A-Za-z_]+"|PREF = "[A-Za-z_]+"|SUFFIX = "[A-Za-z_]+"' <rev> -- app/src/main/java
   ```
   Compare the key sets, the module ids (`getId()` in `modules/list`), the module default-enabled values, `BackupActivity` constants, `IS_PREF_SECRET`, and each catalog's fields. Update section 3/5 and the validator's `EXPECT` table if anything differs. Check `git diff <old> <new> -- activities/BackupActivity.java modules/companions/VersionManager.java utilities/wrappers/Zip*.java` to confirm the format is unchanged.
4. **Plan the edit as small, named changes**, each classified: kept / migrated / added (new in the target version) / added (recommended) / left off. Never rewrite something the owner chose explicitly without recording it.
5. **Build the new file with a script** (not by hand): read the original entries; edit only what you planned; serialise `preferences`/`secrets` as compact single-line JSON, `automations`/`clearUrlCatalog`/`redirect` with `orgjson.dumps`, `patterns`/`hosts` with plain compact JSON; set `version` to the target; reuse the original `readme.txt` bytes; write entries in the original order with deflate and, if you want a byte-style match, the UTF-8 flag. The script will contain owner-specific rules, so keep it outside the repo.
6. **Validate** with `validate_backup.py <new> <original> --src <target source>` and read every INFO line.
7. **Dry-run automation regexes** against sample URLs with *full-match* semantics (`re.fullmatch`) and include negative cases (flagged hosts, shorteners, IP hosts, embedded redirects).
8. **Write the change report** (kept / migrated / added / recommended / left off / phone steps / known limits / validation / risks and rollback / open questions) in the owner's private folder; describe personal items as "kept as-is" or by key name only.
9. **Hand over with the rollback file**, tell the owner to export from the device first, and re-test the "link from another app" path from [section 7](#7-phone-steps-to-route-all-links-through-urlcheck).
10. Update this file and [`tools/validate_backup.py`](../tools/validate_backup.py) in a docs PR when the format or key set changes.

## 12. Lessons

- Read the import code before touching the file: the permissive import (no allow-list) is why validation has to live in your own script.
- `GenericPref.set(default)` removing the key means a backup is a diff against defaults: "recommended" often equals "write nothing", and stale explicit defaults in old backups are harmless.
- Tag and master can differ even at the same `versionName`: pin the commit you analysed (this report: master `5bdfbc88`; tag `v4.0.0` lacks `skipRedirect` and `open_denylist`) and re-run the preference diff before trusting a table.
- Python's `zipfile` clears the UTF-8 flag on write; if you want a byte-style match with the app, set `flag_bits |= 0x800` when writing, and write to a non-seekable sink if you want data descriptors.
- Android `org.json` escapes `/`; Python `json` does not. Use `orgjson.py` for the catalogs that the app writes with `org.json`.
- On Windows/PowerShell, keep the real backups and scratch extractions outside the repo work tree, and prefer absolute paths (a shell cwd persists between calls).

## Open questions for the owner of a given backup

These depend on the person, not on the code: whether auto-continue may skip the dialog for deliberate checks; which browser is the hand-off target; whether a `scan`/`webhook` action should stay while its module is off; whether per-domain learning should be on; whether custom-tab mirroring (`open_ctabs` auto) should be tried.
