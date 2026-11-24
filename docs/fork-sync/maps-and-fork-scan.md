# Upstream -> fork maps and first fork scan (2026-11-24)

Upstream = TrianguloY/URLCheck, fork = Daniel-OS01/URLCheck. 'upstream #N' are original numbers; fork numbers are in the links.

## Execution log

### Issue map (upstream -> fork), 23 issues
- upstream #542 -> https://github.com/Daniel-OS01/URLCheck/issues/1
- upstream #548 -> https://github.com/Daniel-OS01/URLCheck/issues/2
- upstream #552 -> https://github.com/Daniel-OS01/URLCheck/issues/3
- upstream #553 -> https://github.com/Daniel-OS01/URLCheck/issues/4
- upstream #555 -> https://github.com/Daniel-OS01/URLCheck/issues/5
- upstream #556 -> https://github.com/Daniel-OS01/URLCheck/issues/6
- upstream #558 -> https://github.com/Daniel-OS01/URLCheck/issues/7
- upstream #559 -> https://github.com/Daniel-OS01/URLCheck/issues/8
- upstream #565 -> https://github.com/Daniel-OS01/URLCheck/issues/9
- upstream #576 -> https://github.com/Daniel-OS01/URLCheck/issues/10
- upstream #579 -> https://github.com/Daniel-OS01/URLCheck/issues/11
- upstream #583 -> https://github.com/Daniel-OS01/URLCheck/issues/12
- upstream #584 -> https://github.com/Daniel-OS01/URLCheck/issues/13
- upstream #586 -> https://github.com/Daniel-OS01/URLCheck/issues/14
- upstream #589 -> https://github.com/Daniel-OS01/URLCheck/issues/15
- upstream #590 -> https://github.com/Daniel-OS01/URLCheck/issues/16
- upstream #597 -> https://github.com/Daniel-OS01/URLCheck/issues/17
- upstream #598 -> https://github.com/Daniel-OS01/URLCheck/issues/18
- upstream #599 -> https://github.com/Daniel-OS01/URLCheck/issues/19
- upstream #607 -> https://github.com/Daniel-OS01/URLCheck/issues/20
- upstream #626 -> https://github.com/Daniel-OS01/URLCheck/issues/21
- upstream #628 -> https://github.com/Daniel-OS01/URLCheck/issues/22
- upstream #569 -> https://github.com/Daniel-OS01/URLCheck/issues/23

### PR map (upstream -> fork), 23 open upstream PRs mirrored (drafts kept as drafts)
- upstream #397 -> https://github.com/Daniel-OS01/URLCheck/pull/46
- upstream #523 -> https://github.com/Daniel-OS01/URLCheck/pull/45
- upstream #568 -> https://github.com/Daniel-OS01/URLCheck/pull/44
- upstream #581 -> https://github.com/Daniel-OS01/URLCheck/pull/43
- upstream #606 -> https://github.com/Daniel-OS01/URLCheck/pull/42
- upstream #608 -> https://github.com/Daniel-OS01/URLCheck/pull/41
- upstream #609 -> https://github.com/Daniel-OS01/URLCheck/pull/40
- upstream #610 -> https://github.com/Daniel-OS01/URLCheck/pull/39
- upstream #611 -> https://github.com/Daniel-OS01/URLCheck/pull/38
- upstream #612 -> https://github.com/Daniel-OS01/URLCheck/pull/37
- upstream #613 -> https://github.com/Daniel-OS01/URLCheck/pull/36
- upstream #614 -> https://github.com/Daniel-OS01/URLCheck/pull/35
- upstream #615 -> https://github.com/Daniel-OS01/URLCheck/pull/34
- upstream #616 -> https://github.com/Daniel-OS01/URLCheck/pull/33
- upstream #617 -> https://github.com/Daniel-OS01/URLCheck/pull/32
- upstream #618 -> https://github.com/Daniel-OS01/URLCheck/pull/31
- upstream #619 -> https://github.com/Daniel-OS01/URLCheck/pull/30
- upstream #620 -> https://github.com/Daniel-OS01/URLCheck/pull/29
- upstream #621 -> https://github.com/Daniel-OS01/URLCheck/pull/28
- upstream #622 -> https://github.com/Daniel-OS01/URLCheck/pull/27
- upstream #623 -> https://github.com/Daniel-OS01/URLCheck/pull/26
- upstream #627 -> https://github.com/Daniel-OS01/URLCheck/pull/25
- upstream #629 -> https://github.com/Daniel-OS01/URLCheck/pull/24

### Fork scan (42 forks pushed since 2026-06-01; compared default branch vs upstream master)
- Ahead of upstream: jivandabeast (3, = upstream PR #629), jprapstyle (4, personal branding), LeDimigit (7, = upstream PR #606), BDHACK638 (1, deletes README - junk), snigdhasjg (1, trust user CA - already in upstream master under another sha, nothing to copy), 3ekodomo (49, ZIP-replaced root, workflow churn, theme customization - junk/too large), hizbu996-art and PingouinFerreux (repos deleted, 404), ShiroiKuma0:custom (25, black-yellow branding, own package; its on-device resolver and automation v2 are entangled with that, skipped), 8099bb4X21:patched (13), vimal-gandhi-dev (4, rebrand 'Serva LinkCheck'), morocoeltopo (4, only touches alpha.yml - junk).
- Accepted: 8099bb4X21 share automation package/component args (2 commits, author kept) -> fork PR #47. Rejected from the same fork: URL length<7 filter (arbitrary), fork CI/signing churn.
- Limitation: only each fork's default branch (plus 8099bb4X21:patched, ShiroiKuma0:custom) was compared; non-default branches of other forks were not enumerated.

