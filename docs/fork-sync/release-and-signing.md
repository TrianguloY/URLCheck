# Release and signing

## How a release is produced
- Workflow: `.github/workflows/release.yml` (`Create release`), triggered by pushing a tag matching `v*` (or manually via `workflow_dispatch`, which only uploads an artifact).
- Steps: checkout -> JDK 17 (Temurin) -> Gradle -> decode secret `RELEASE_STORE_FILE` (base64 keystore) to a file -> `./gradlew build assembleRelease` with `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` -> `gh release create <tag>` with the APK and notes.
- `app/build.gradle` skips signing when the secrets are missing (unsigned, not installable). Read the `secret(...)` helper there before changing anything.
- Release notes = the newest file in `fastlane/metadata/android/en-US/changelogs/` (highest number) + a fixed footer with package id and the signing certificate SHA-256. F-Droid limits changelogs to 500 bytes.

## Cutting the next release
1. Decide the version: bump `versionCode` (+1) and `versionName` in `app/build.gradle` (lines 14-15).
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (`V x.y.z`, bullets: New / Improve / Fix / Internal).
3. Make sure master CI (`Validate gradle build test`) is green on that commit.
4. `git tag -a vX.Y.Z -m "URLCheck X.Y.Z" <sha>`; `git push origin vX.Y.Z`.
5. Wait for `Create release` (~1.5 min), then verify: `gh release view vX.Y.Z --json assets`, download the APK, check the signature.

## Signing setup of this fork (no secrets here)
- A new PKCS12 keystore (RSA 4096, alias `urlcheck-release`, validity 36500 days) generated with JDK 17 `keytool`; passwords were random and generated in memory.
- Stored only as the four repo secrets above and as an offline backup (keystore + credentials) in a user-only-ACL folder outside the repo. **If the backup and the secrets are both lost, no update can ever be signed with the same identity.**
- Certificate SHA-256: `D4:CF:0A:14:46:26:2A:1B:76:57:F5:31:AD:94:26:C7:32:43:E4:E3:D4:EF:83:D8:01:3F:AD:1C:CC:01:20:C9` (also shown in each release's notes).
- Consequence: same package id as the upstream/Play/F-Droid builds but a different certificate, so the fork APK cannot be installed over them. Uninstall first and migrate with the app's own backup/restore (`.ucbckp`).

## Verification used for v4.0.0
- Asset `URLCheck-4.0.0.apk` (1,349,376 bytes), SHA-256 `B8F225C049207BA09240A20AED7598C723A49498BE67E44B44CA2F5ACB4E5C9D`.
- `keytool -printcert -jarfile URLCheck-4.0.0.apk` showed the expected certificate; `jarsigner -verify` -> `jar verified.` (v1 signature only; `apksigner verify --print-certs` was not available because no Android SDK is installed locally. Prefer `apksigner` when it is.)

## Gotchas
- `alpha.yml` (`Build alpha apk`, manual) expects an existing `alpha` release; on the fork it fails at the upload step and leaves an `alpha` tag.
- Pushing a tag before the workflows are registered/enabled on a fresh fork does nothing; see [playbook.md](playbook.md).
- The `assets-updater.yml` cron (weekly) will keep opening `updated data.minify.json` PRs on the fork; merge or disable as wanted.

## Releases
| Version | Date | APK SHA-256 | Notes |
|---|---|---|---|
| v4.0.0 (versionCode 48) | 2026-11-24 | `B8F225C049207BA09240A20AED7598C723A49498BE67E44B44CA2F5ACB4E5C9D` | first fork release: upstream PR merges, new signing key |
| v4.0.1 (versionCode 49) | 2026-11-24 | `FEAFA5A8A005E1D19238271CF5B4D595EDEE02D209FA5C34E813742C7A08C7A2` | skip redirect module, deny browser, meta-refresh/JS redirect detection, dialog state restore, never open URLCheck from itself, status-check warning, text-field labels; signer certificate unchanged |
| v4.0.2 (versionCode 50) | 2026-11-24 | `80BBE9E478F87B034208445B25DD18E1EAE7D2FA3E171A09E24A6BF0D6D28FBA` | custom tabs service answers clients (Google app no longer falls back to Chrome), fork PR #60; signer certificate unchanged |
