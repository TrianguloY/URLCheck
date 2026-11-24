# Device test of v4.0.2 (custom tabs service) and settings backup

Status: **interim**. Tests 1-5 done on the test phone, test 6 (custom tabs) shows the fix does not change what the Google app does, root cause found, test 7 and the daily-phone checks are partly done. Open items are listed at the end.

## Devices

| Phone | Model | OS | Used for |
|---|---|---|---|
| poco-f5 (`d9b3bef4`) | 23049PCD8G (`marble`) | Android 15 (SDK 35), HyperOS 3.0.4.0 | install, restore, tests 1-6 |
| poco-x8 (`HEW8EMAYWKIVRWPN`) | 2602BPC18G (`dash`) | Android 16 (SDK 36), HyperOS 3, users: 0, 11 (work profile), 999 (XSpace) | read-only checks only |

Screenshots that show personal data (Google app, account names) were kept out of git in `C:\Users\Daniel\Backups\urlcheck-settings\device-test\`.

## What was installed and changed on poco-f5

| # | Change | Revert |
|---|---|---|
| 1 | Play Store build 3.5 (versionCode 47, installer `com.android.vending`) uninstalled, fork build 4.0.2 (versionCode 50, sideloaded, APK SHA-256 `80BBE9E4...28FBA` verified) installed | the user's own export taken first: `Backups\urlcheck-settings\URLCheck_20261124232605_phone-play-3.5.ucbckp` (data + secrets). To go back: uninstall the fork build, install from Play, restore that file |
| 2 | Settings restored from the **safe** backup `URLCheck_20261124230500_safe.ucbckp`, not from the phone's own export | restore the phone's own export above |
| 3 | Browser role: URLCheck (Chrome took it back on uninstall) | `cmd role add-role-holder android.app.role.BROWSER <package> 0` |
| 4 | Airplane mode turned off (it was on) to get network | `cmd connectivity airplane-mode enable` |
| 5 | App-link selection for URLCheck enabled for `medium.com`, `*.wikipedia.org`, `www.reddit.com` (were all disabled) | `pm set-app-links-user-selection --user 0 --package com.trianguloy.urlchecker false medium.com '*.wikipedia.org' www.reddit.com` |
| 6 | Clipboard replaced by the text `hello world` (to test "No links detected") | none needed |
| 7 | Files left in `/sdcard/Download`: `URLCheck_safe.ucbckp`, `URLCheck_20261124232605.ucbckp` | delete |

The Google app was only force-stopped and opened. Nothing else was changed on poco-f5.

## Tests on poco-f5

| # | Test | Expected | Actual | Result |
|---|---|---|---|---|
| 1 | Install v4.0.2 | versionName 4.0.2, fork signature | versionCode 50, versionName 4.0.2, signature differs from the Play build (so uninstall was required) | pass |
| 2 | Restore safe backup | no error, modules, patterns, automations present | toast "Backup restored" ([screenshot](device-test-v4.0.2/t2-3-after-restore-pick.png)); all modules listed; automations editor holds 7 rules (3 with `"enabled": false`); the backup holds 57 patterns including `Clean: Google AMP viewer to original` and `Warn: IP address host (IPv4 or IPv6)` (checked in the backup file, the pattern editor text is truncated in the UI tree) | pass |
| 3a | `VIEW` + `BROWSABLE` `https://example.com/?utm_source=x&fbclid=abc&id=5` | URLCheck dialog, tracking removed, hand-off to a real browser | dialog shows `https://example.com/?id=5`, hand-off target Brave, Brave opened ([dialog](device-test-v4.0.2/t3-1-view-browsable.png), [Brave](device-test-v4.0.2/t3-3-after-open-brave.png)); URLCheck did not reopen itself | pass |
| 3b | same without `BROWSABLE` | same | same dialog, cleaned URL. One earlier run right after Home showed no dialog and was not reproduced in three retries | pass (one unexplained miss) |
| 4a | `SEND` `text/plain` "see https://example.com/?utm_medium=y" | URLCheck offered, link extracted | URLCheck listed under "More" in the share sheet, dialog shows `https://example.com/` | pass |
| 4b | clipboard check | "No links detected" only when no link | link in clipboard: dialog opened; plain text: toast "No links detected" ([screenshot](device-test-v4.0.2/t4-6-clipboard-nolink.png)). The quick-settings tile was not tested separately (same code path as the toolbar button) | pass |
| 5 | URLCheck as default browser | role holder is URLCheck | `cmd role add-role-holder` worked; `resolve-activity` for `http` returns `MainDialog` | pass |
| 6 | Custom tabs from the Google app | `onBind` and `transaction N` in the `CUSTOMTABS` log, link opens in URLCheck | **no `CUSTOMTABS` line at all**, link opened in a Chrome custom tab | **fail (not caused by the service)** |
| 6b | another custom tabs app | opens via URLCheck | not testable: Telegram, Reddit and Infinity are disabled on the phone, Gmail would need mail content | not tested |
| 7 | links from other apps | lands in URLCheck | see "Root cause", neutral sources only on the daily phone | partly |

## Root cause: why the Google app opens Chrome

Captured from the system log and `dumpsys` (poco-f5, Google app search result tap):

1. `START u0 {act=VIEW cat=[BROWSABLE] dat=https://www.iana.org/... flg=0x10000400 (has extras)}` from the Google app, result code `-91`. Flag `0x400` is `FLAG_ACTIVITY_REQUIRE_NON_BROWSER`: the Google app first asks Android for an app that is **not a browser**; if none exists the start fails.
2. `START u0 {act=VIEW dat=https://www.iana.org/... pkg=com.android.chrome cmp=com.android.chrome/...IntentDispatcher (has extras)}` from the Google app, then `cr_ChromeConnection: New Custom Tab session created by package: com.google.android.googlequicksearchbox`. The Google app targets Chrome by package name. It never bound URLCheck's service (no `CUSTOMTABS` log, `onBind` never ran).

Captured on poco-x8 from the "Open with" dialog that was on screen (`dumpsys activity activities`):

`Intent { act=android.intent.action.VIEW dat=https://x.com/<account>/status/<id> flg=0x800000 xflg=0x4 pkg=com.android.chrome cmp=com.android.intentresolver/.ResolverActivity (has extras) }`, `launchedFromPackage=com.google.android.googlequicksearchbox`, `resultTo=...googlequicksearchbox/.SearchActivity`.

So on both phones the Google app sends `VIEW` with an explicit `package=com.android.chrome`. A package-targeted intent is delivered only to that package, so URLCheck's intent filters are never consulted. The "Open with: Personal/Work, Chrome only" dialog is the system resolver choosing between the personal and the work copy of Chrome (poco-x8 has a work profile); it is not a list of browsers. Checks on poco-x8: with `x.com` already approved for URLCheck, a plain `VIEW` + `BROWSABLE` `x.com` URL resolves to `com.trianguloy.urlchecker/.dialogs.MainDialog`; the same URL with `-p com.android.chrome` resolves only to Chrome's `IntentDispatcher`.

### Supported links (adding hosts)

On poco-f5, with `medium.com`, `*.wikipedia.org` and `www.reddit.com` selected in "Open by default":

| Intent | Result |
|---|---|
| `VIEW` `BROWSABLE` `https://medium.com/...` flags `NEW_TASK` | URLCheck dialog |
| same with `REQUIRE_NON_BROWSER` (what the Google app sends first) | not resolved |
| same for `en.wikipedia.org` and a host URLCheck does not declare | not resolved |

Approving hosts changes nothing for the observed cases. Adding more hosts to the filter would not help either: the Google app's decisive intent is package-targeted (Chrome), and its first attempt (`REQUIRE_NON_BROWSER`) is rejected for URLCheck even on approved hosts, because Android keeps only the **first** matching filter per activity and for `MainDialog` that is the generic http/https filter, which marks the activity as a browser. A host-only filter in a **separate** activity (alias) would be classified as a non-browser; whether that makes the Google app hand an approved host to URLCheck has not been tested yet (see open items).

## Open items

- Google app setting "Open web pages in the Google app" (Settings > General): not read yet. poco-x8 is in use by the user (the foreground app changed on its own during checks), so no input was sent to it.
- Evaluation of a host-only `activity-alias` (non-browser) so that `REQUIRE_NON_BROWSER` callers can reach URLCheck for approved hosts. Needs a CI evaluation APK (installs side by side as `com.trianguloy.urlchecker.test`).
- Test 7 with a neutral source on the daily phone.
- Revert the poco-f5 changes listed above when the phone is no longer needed.

## Changes made on poco-x8

None to settings or data. Read-only `dumpsys`, `pm get-app-links`, `cmd role get-role-holders`, `cmd package query-activities`. Side effects to know about: one Back key press (closed the "Open with" dialog the user had left open), the Google app was brought to the foreground twice, and the log buffer was cleared once.
