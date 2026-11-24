# Pull request status

Snapshot refreshed 2026-11-24 22:45 (UTC+2) by the PR fixer/merger thread from `gh pr list -R Daniel-OS01/URLCheck --state all`. Refresh with that command; the reasons below are the durable part. Per-PR fixes and merge SHAs: [threads/pr-fixer-merger.md](threads/pr-fixer-merger.md).

## Merge policy (used throughout)
- Merge only if: not a draft, `Validate gradle build test` green, no conflicts, relevant.
- `gh pr merge N --merge` (keeps original commits and authors); small fixes first, then features.
- After each wave: re-check `mergeable` (it flips to UNKNOWN for ~20 s, then CONFLICTING for newly affected PRs), merge master into conflicting branches, push, wait for CI. Check CI of master after each wave.
- Fork PR numbers differ from upstream numbers: the `Upstream: TrianguloY/URLCheck#N` footer in each PR body gives the original. Mirror branches are named `upstream-pr/<upstream#>-<slug>`.

## All fork PRs
| PR | Title | State |
|---|---|---|
| [#24](https://github.com/Daniel-OS01/URLCheck/pull/24) | Add user-defined custom modules | open |
| [#25](https://github.com/Daniel-OS01/URLCheck/pull/25) | updated data.minify.json | merged |
| [#26](https://github.com/Daniel-OS01/URLCheck/pull/26) | Keep the dialog state when the theme, language or orientation changes | merged |
| [#27](https://github.com/Daniel-OS01/URLCheck/pull/27) | Make the uri parts names translatable | merged |
| [#28](https://github.com/Daniel-OS01/URLCheck/pull/28) | Only follow ClearURLs redirections that lead to a web url | merged |
| [#29](https://github.com/Daniel-OS01/URLCheck/pull/29) | Name the action buttons after what they do, and say what applying a pattern did | merged |
| [#30](https://github.com/Daniel-OS01/URLCheck/pull/30) | Keep the dialog in recents until it is closed | merged |
| [#31](https://github.com/Daniel-OS01/URLCheck/pull/31) | Only offer a query value as the new url when it is a link | merged |
| [#32](https://github.com/Daniel-OS01/URLCheck/pull/32) | Break module labels at hyphens instead of mid-word | merged |
| [#33](https://github.com/Daniel-OS01/URLCheck/pull/33) | Dim the history list button when there is nothing to list | merged |
| [#34](https://github.com/Daniel-OS01/URLCheck/pull/34) | Show the flags search as a collapsible title like the uri parts groups | merged |
| [#35](https://github.com/Daniel-OS01/URLCheck/pull/35) | Keep label text readable on its colored background | merged |
| [#36](https://github.com/Daniel-OS01/URLCheck/pull/36) | Replace the module titles with self-explaining buttons | open (draft) |
| [#37](https://github.com/Daniel-OS01/URLCheck/pull/37) | Allow pinning modules so they stay visible when unused | merged |
| [#38](https://github.com/Daniel-OS01/URLCheck/pull/38) | Size the browser icon in dp so it looks the same on every screen | merged |
| [#39](https://github.com/Daniel-OS01/URLCheck/pull/39) | Add missing content descriptions to buttons | merged |
| [#40](https://github.com/Daniel-OS01/URLCheck/pull/40) | Accept text/html shares and read styled shared text | merged |
| [#41](https://github.com/Daniel-OS01/URLCheck/pull/41) | Detect IPv6 links in shared and clipboard text | merged |
| [#42](https://github.com/Daniel-OS01/URLCheck/pull/42) | Skip redirect module and french translation update | merged |
| [#43](https://github.com/Daniel-OS01/URLCheck/pull/43) | Enhance redirect detection: add support for meta refresh and JavaScript support | merged |
| [#44](https://github.com/Daniel-OS01/URLCheck/pull/44) | Translations update from Hosted Weblate | merged |
| [#45](https://github.com/Daniel-OS01/URLCheck/pull/45) | Add deny browser to open/share functionality | merged |
| [#46](https://github.com/Daniel-OS01/URLCheck/pull/46) | Added new resource file system | open (draft) |
| [#47](https://github.com/Daniel-OS01/URLCheck/pull/47) | Share automation: optional package/component arguments (from 8099bb4X21 fork) | merged |
| [#48](https://github.com/Daniel-OS01/URLCheck/pull/48) | Shorten fr-FR and ta-IN fastlane full descriptions under 4000 chars | merged |
| [#49](https://github.com/Daniel-OS01/URLCheck/pull/49) | Clarify that Check status connects directly to the remote site | merged |
| [#50](https://github.com/Daniel-OS01/URLCheck/pull/50) | Add persistent TextView labels and labelFor to EditText fields | merged |
| [#51](https://github.com/Daniel-OS01/URLCheck/pull/51) | [link-interception] Never open or share to URLCheck itself from automations | merged |
| [#52](https://github.com/Daniel-OS01/URLCheck/pull/52) | [link-interception] Add URLBlocklist and Direct Open browser forwarding (oooppet/URLCheck) | merged |
| [#53](https://github.com/Daniel-OS01/URLCheck/pull/53) | docs: fork-sync knowledge base | open (docs, merged by the parent thread) |
| [#54](https://github.com/Daniel-OS01/URLCheck/pull/54) | docs: add issues-fixer thread report | open (docs, contains #53) |

## Final state and reasons (after the PR fixer/merger thread)
| PR | State | Reason |
|---|---|---|
| #26 | merged `7abeead` | draft was complete; marked ready |
| #42 | merged `be93032` | `strings.xml` conflict resolved; recursion capped with `MAX_DEPTH = 10` in `SkipRedirectUtility` |
| #43 | merged `bd30b6e` | hardened first: 64 KB body read, html Content-Type only, read timeout, http/https redirect targets only |
| #45 | merged `3d8c759` | rebuilt on top of #47's `ShareUtility`, compile errors fixed; on Android < 7 the denied app still shows in the share chooser |
| #48, #49, #50 | merged `60cf889`, `d81dda4`, `68737e7` | small fixes from the issue fixer thread, green |
| #51 | merged `5bdfbc8` | loop guard: automations can no longer open or share to URLCheck itself |
| #52 | merged `a2dc6e7` | minSdk 19 lint errors fixed by the fork-scan thread; reviewed: opt-in, never targets URLCheck itself |
| #24 | open | custom modules send URLs to user-configured servers; hardened (http/https, method allowlist, 64 KB request, 256 KB response, no redirects) and green, but needs Daniel's decision and a device test |
| #36 | open (draft) | removes module titles: design decision for Daniel; conflicts with master |
| #46 | open (draft) | large untested refactor, far behind master, conflicts |

## Why PRs were skipped or held in round 1
| Fork PR | Reason |
|---|---|
| #24 (custom modules, upstream #629) | ~950 lines, new, unreviewed upstream, adds outbound HTTP actions (URLs sent to arbitrary servers); disabled by default but left for an explicit decision / hardening |
| #43 (meta-refresh/JS redirect detection, upstream #581) | read the whole response body without a size limit and parsed JavaScript with regexes; later iterations may have added limits, check the PR |
| #45 (deny browser, upstream #523) | hard-coded single-browser denylist, conflicted with #47; later picked up by the PR fixer thread, see [threads/](threads/) |
| #42 (skip redirect, upstream #606) | conflicts in round 1; later fixed and merged with a recursion cap |
| #26, #36, #46 (drafts) | drafts stay drafts unless the work is complete; see the thread report for any that were completed |

## Lessons
- Mirroring 23 PRs queues ~70 workflow runs (Validate + CodeQL + Codacy); GitHub drains about one PR per minute. Merge in waves as results arrive.
- Weblate/translation PRs are safe to merge early (distinct files) but touch ~130 files.
- Overlapping UI PRs (flags search vs content descriptions) conflict in layout + `strings.xml`; resolve by taking the newer layout and the union of strings.