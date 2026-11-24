# Making links from other apps open in URLCheck

Requirement: almost every link, from any app, should pass through URLCheck. Observed problem: in the Google app (Search result -> Visit) the link opens in Chrome (Samsung "Open with" dialog listing only Chrome, Personal/Work tabs).

## What the code already provides (master, `app/src/main/AndroidManifest.xml`)
- `dialogs.MainDialog`: `VIEW` + `DEFAULT` + `BROWSABLE` for `http`/`https` (plus odd-case schemes `Http`, `Https`, `HTTP`, `HTTPS`), a second VIEW filter with MIME `text/html`, `text/plain`, `application/xhtml+xml`, hosts used by libredirect-style redirects (instagram, twitter/x, youtube, reddit, tiktok, medium, wikipedia), `SEND` for text/html/xhtml, `MAIN` + `APP_BROWSER`.
- `activity-alias` for `PROCESS_TEXT` (text selection menu).
- `services.CustomTabs`: a minimal custom tabs service (protocol stub since v4.0.2, see below) with action `android.support.customtabs.action.CustomTabsService`, so apps that bind to the DEFAULT browser's custom-tabs provider can bind to URLCheck instead of Chrome.
- `BackupActivity` handles `.ucbckp` files. `ShortcutsActivity` provides shortcuts/tile.

## It is mostly a phone setting
1. Make URLCheck the default browser: Android Settings -> Apps -> Default apps -> Browser app -> URLCheck (Samsung One UI: Settings -> Apps -> Choose default apps -> Browser app).
2. In URLCheck, choose a REAL browser as the hand-off target so links do not loop back (the open/share modules must never choose URLCheck itself; see the self-exclusion fix in [pull-request-status.md](pull-request-status.md)).
3. Optional: for each app that grabs links itself (Chrome, Google), Settings -> Apps -> <app> -> Open by default -> turn off "Open supported links" / clear defaults.
4. In the app: keep link detection on for shared text and clipboard.

## Limits no setting can fix
- Links verified for another app (Android 12+ domain verification, e.g. YouTube, Google Maps, Google-owned domains) open in that app.
- Intents with an explicit target package (e.g. an app hard-coding Chrome or a Chrome Custom Tab) bypass the intent resolver.
- Schemes not declared in the manifest (`mailto:`, `tel:`, custom schemes) and intents without `BROWSABLE`.
- Android's own "Open with" dialog only lists apps matching the intent; URLCheck must match the exact intent (action, category, scheme, MIME) to appear.

## Done: custom tabs service answers clients (v4.0.2, fork PR #60)
Until v4.0.1 `services.CustomTabs` returned a bare `Binder`. Apps like the Google app bind to the default browser's custom tabs service and call `warmup`/`newSession`; on a bare Binder those calls fail, so the app gives up on URLCheck and opens Chrome (the "Open with" dialog listing only Chrome). The service now speaks the `android.support.customtabs.ICustomTabsService` protocol (transaction code = `FIRST_CALL_TRANSACTION` + id from the AIDL): `warmup` (1), `newSession` (2), `mayLaunchUrl` (3), `updateVisuals` (5), `newSessionWithExtras` (9) return true; every other call returns false / null Bundle / 0. No fork had this (scan of 142 forks, see [threads/link-interception-findings.md](threads/link-interception-findings.md)). Device test (see [threads/device-test-v4.0.2.md](threads/device-test-v4.0.2.md)): the Google app never binds to this service. It first sends an implicit `VIEW` with `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` (rejected for any browser, including URLCheck) and then starts Chrome by package name with a custom tab session, so the protocol stub does not change what the Google app does. URLCheck must still be the default browser for other apps that choose the default browser's custom tabs provider.

## Where code changes could help (candidates, not done unless a thread report says so)
- `validateRelationship`, `postMessage` and engagement signals in the custom tabs service (currently reported as unsupported; see upstream issues #527, #510).
- More VIEW filters (generic schemes upstream #320, MIME types upstream #494), `autoVerify` is not applicable to a browser.
- Auto-open/auto-continue to the real browser (upstream #18, #519), chooser improvements (upstream #405, #47, #353, #583), share targets (upstream #532).
- Fork work worth checking: `oooppet/URLCheck` (URLBlocklist + Direct Open browser forwarding, fork PR #52).

Threads append their concrete findings in [threads/](threads/).
