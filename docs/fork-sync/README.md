# Fork sync knowledge base (Daniel-OS01/URLCheck)

Everything learned and done while syncing this fork with `TrianguloY/URLCheck` on 2026-11-24 and in the follow-up work threads. Start here; each file is self-contained and written for whoever (human or agent) does the next update.

> No secrets or personal data live in this folder: no keystore, passwords, tokens, webhook URLs or settings-backup contents. Where such things exist they are only referred to by name.

## Index

| File | What it covers |
|---|---|
| [process-and-decisions.md](process-and-decisions.md) | The step-by-step run that was executed (phases, decisions D1-D3, order of operations, results, errors) |
| [playbook.md](playbook.md) | Reusable recommendations for repeating a fork sync on any repo, including the Windows/PowerShell and GitHub Actions pitfalls |
| [maps-and-fork-scan.md](maps-and-fork-scan.md) | Upstream issue/PR -> fork issue/PR maps and the first fork scan verdicts |
| [pull-request-status.md](pull-request-status.md) | What happened to every mirrored/new PR (merged, skipped, why) and merge policy |
| [release-and-signing.md](release-and-signing.md) | How the signed release is produced, the signing setup (no secrets), verification, how to cut the next release |
| [link-interception.md](link-interception.md) | How to make links from other apps (e.g. Google app) open in URLCheck: what is a phone setting, what is code, known limits |
| [tools/](tools/) | Python helpers to inspect and validate a settings backup (`.ucbckp`) without the app; usage in [threads/settings-backup-updater.md](threads/settings-backup-updater.md) |
| [threads/](threads/) | One report per work thread (issue fixer, PR fixer/merger, fork scanner, settings-backup updater, link interception) with findings, decisions and open questions |

## Quick facts

- Fork `master` started even with upstream at `ca3bb00`; `.github/` was already identical.
- Release `v4.0.0` (versionCode 48) is signed with a **new** fork-specific certificate, so it cannot update over the upstream/Play/F-Droid builds (same package id `com.trianguloy.urlchecker`, different signature): uninstall first, use the app's backup/restore to migrate settings.
- CI gate for merging: workflow `Validate gradle build test` (`./gradlew build test`).
- Mirrored PRs keep an `Upstream: TrianguloY/URLCheck#N by <author>, <date>` footer; copied issues use the same marker. Use it to stay idempotent and to find the original.

## How to keep this folder up to date

1. After any sync/merge/release round, append to [process-and-decisions.md](process-and-decisions.md) (what, when, why) and update [pull-request-status.md](pull-request-status.md).
2. New lessons (tooling, OS quirks, GitHub behaviour) go into [playbook.md](playbook.md).
3. Each work thread adds or updates its own file under [threads/](threads/) rather than editing shared files, to avoid merge conflicts.
