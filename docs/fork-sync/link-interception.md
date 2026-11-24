# Making links from other apps open in URLCheck

Requirement: almost every link, from any app, should pass through URLCheck. Observed problem: in the Google app (Search result -> Visit) the link opens in Chrome (Samsung "Open with" dialog listing only Chrome, Personal/Work tabs).

## What the code already provides (master, `app/src/main/AndroidManifest.xml`)
- `dialogs.MainDialog`: `VIEW` + `DEFAULT` + `BROWSABLE` for `http`/`https` (plus odd-case schemes `Http`, `Https`, `HTTP`, `HTTPS`), a second VIEW filter with MIME `text/html`, `text/plain`, `application/xhtml+xml`, hosts used by libredirect-style redirects (instagram, twitter/x, youtube, reddit, tiktok, medium, wikipedia), `SEND` for text/html/xhtml, `MAIN` + `APP_BROWSER`.
- `activity-alias` for `PROCESS_TEXT` (text selection menu).
- `services.CustomTabs`: an empty "fake custom tabs" service with action `android.support.customtabs.action.CustomTabsService`, so apps that bind to the DEFAULT browser's custom-tabs provider can bind to URLCheck instead of Chrome.
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

## Where code changes could help (candidates, not done unless a thread report says so)
- A real `CustomTabsService` (newSession, warmup, mayLaunchUrl, validateRelationship) instead of the empty one (see upstream issues #527, #510).
- More VIEW filters (generic schemes upstream #320, MIME types upstream #494), `autoVerify` is not applicable to a browser.
- Auto-open/auto-continue to the real browser (upstream #18, #519), chooser improvements (upstream #405, #47, #353, #583), share targets (upstream #532).
- Fork work worth checking: `oooppet/URLCheck` (URLBlocklist + Direct Open browser forwarding, fork PR #52).

Threads append their concrete findings in [threads/](threads/).
