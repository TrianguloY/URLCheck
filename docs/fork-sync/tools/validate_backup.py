"""Validates a .ucbckp against the URLCheck source tree the way BackupActivity.restore() reads it.
Usage: python validate_backup.py <file.ucbckp> [<original.ucbckp>] [--src <repo root, default: .>]
Prints key NAMES and counts only, never values (rule/pattern names may appear in failure messages).
Pure standard library. See docs/fork-sync/threads/settings-backup-updater.md for what is checked and why.
"""
import sys, re, json, zipfile, os, argparse

ap = argparse.ArgumentParser()
ap.add_argument('backup')
ap.add_argument('original', nargs='?')
ap.add_argument('--src', default='.', help='repository root (contains app/build.gradle)')
a = ap.parse_args()
fails, notes = [], []


def check(cond, msg):
    (notes if cond else fails).append(('OK   ' if cond else 'FAIL ') + msg)


def strict(text):
    def hook(pairs):
        ks = [k for k, _ in pairs]
        if len(ks) != len(set(ks)):
            raise ValueError('duplicate keys')
        return dict(pairs)
    return json.loads(text, object_pairs_hook=hook)


# ---- source facts (from the 4.0.0 code)
java = os.path.join(a.src, 'app', 'src', 'main', 'java')
literals, mod_ids = set(), set()
for root, _, files in os.walk(java):
    for fn in files:
        if fn.endswith('.java'):
            t = open(os.path.join(root, fn), encoding='utf-8').read()
            literals |= set(re.findall(r'"([^"\\\n]+)"', t))
            mod_ids |= set(re.findall(r'public String getId\(\)\s*\{\s*return "(\w+)";', t))
mod_ids.add('debug')  # DebugModule.ID constant
bv = open(os.path.join(a.src, 'app', 'build.gradle'), encoding='utf-8').read()
app_version = re.search(r'versionName "([^"]+)"', bv).group(1)
SECRET_PREFS = {'api_key', 'log_data', 'webhook_url', 'unshorten_token'}   # BackupActivity.IS_PREF_SECRET
EXPECT = {  # key -> (type, allowed values or None)
    'order': ('String', None), 'open_ctabs': ('Integer', {0, 1, 2, 3, 4, 5}), 'open_incognito': ('Integer', {0, 1, 2, 3, 4, 5}),
    'open_iconsize': ('Integer', None), 'open_rejected': ('Boolean', None), 'open_closeopen': ('Boolean', None),
    'open_noReferrer': ('Boolean', None), 'lastOpen_perDomain': ('Boolean', None), 'reject_last': ('String', None),
    'clearurl_auto': ('Boolean', None), 'clearurl_autoUpdate': ('Boolean', None), 'clearurl_referral': ('Boolean', None),
    'clearurl_verbose': ('Boolean', None), 'clearurl_lastCheck': ('Long', None), 'clearurl_lastUpdate': ('Long', None),
    'clearurl_lastAuto': ('Boolean', None), 'clearurl_catalogURL': ('String', None), 'clearurl_hashURL': ('String', None),
    'changelog_lastVersion': ('String', None), 'ctabs_toast': ('Boolean', None), 'auto_enabled': ('Boolean', None),
    'auto_error_toast': ('Boolean', None), 'tutorial_done': ('Boolean', None), 'animations': ('Boolean', None),
    'log_limit': ('Integer', None), 'debug_errortoast': ('Boolean', None), 'skipRedirect_auto': ('Boolean', None),
    'text_monospace': ('Boolean', None), 'vertical_gravity': ('Integer', {0, 1, 2, 3}), 'horizontal_gravity': ('Integer', None),
    'width': ('Integer', None), 'syncProcessText': ('Boolean', None), 'dayNight': ('Integer', None), 'locale': ('String', None),
    'statusCode_autoRedir': ('Boolean', None), 'pttrn_regexfix': ('Boolean', None), 'open_closeshare': ('Boolean', None),
    'open_closecopy': ('Boolean', None), 'open_mergeCopy': ('Boolean', None), 'webhook_body': ('String', None),
    'jsonPref_size': ('Float', None), 'open_denylist': ('Integer', {0, 1, 2, 3, 4, 5}),
}
PY_TYPES = {'String': str, 'Integer': int, 'Long': int, 'Boolean': bool}

# ---- zip (ZipReader: java.util.zip.ZipFile by entry name)
z = zipfile.ZipFile(a.backup)
check(z.testzip() is None, 'zip CRC test passed')
names = [i.filename for i in z.infolist()]
check(len(names) == len(set(names)), 'no duplicate zip entries')
check(names[:2] == ['version', 'readme.txt'], 'entry order starts with version, readme.txt: %s' % names)
ver = z.read('version').decode('utf-8')
check(ver == app_version, "version file = %r (app %r)" % (ver, app_version))
check(all(i.compress_type == zipfile.ZIP_DEFLATED for i in z.infolist()), 'all entries DEFLATED')
check(z.comment == b'URLCheck (com.trianguloy.urlchecker) Backup', 'zip comment as the app writes it')
check(all(i.flag_bits & 0x800 for i in z.infolist()), 'UTF-8 name flag set like java.util.zip')
if a.original:
    zo = zipfile.ZipFile(a.original)
    check([i.filename for i in zo.infolist()] == names, 'same entries and order as the original')
    check(zo.read('readme.txt') == z.read('readme.txt'), 'readme.txt byte-identical to the original')
    check(zo.read('secrets') == z.read('secrets'), 'secrets byte-identical to the original')
    for fn in ('files/clearUrlCatalog', 'files/hosts'):   # catalogs a migration normally leaves alone
        if fn in zo.namelist() and fn in names:
            check(zo.read(fn) == z.read(fn), fn + ' byte-identical to the original')

# ---- restorePreferencesMatching() port, for both files
valid_types = {'String', 'Integer', 'Long', 'Boolean'}
total, unknown, dyn = 0, [], 0
for fname, want_secret in (('preferences', False), ('secrets', True)):
    if fname not in names:   # the export boxes can be unticked: either entry may be absent
        notes.append('INFO no %r entry in this backup' % fname)
        continue
    raw = z.read(fname).decode('utf-8')
    check('\n' not in raw and '\r' not in raw, '%s is single-line compact json like the app writes' % fname)
    prefs = strict(raw)
    check(isinstance(prefs, dict), '%s parses, no duplicate keys' % fname)
    for key, ent in prefs.items():
        total += 1
        if isinstance(ent, dict) and set(ent) == {'value', 'type'} and ent['type'] == 'Float':
            # the app EXPORTS Float prefs but BackupActivity.restorePreferencesMatching has no Float case
            # (assertError("Unknown type")): the key is deleted by "delete before restoring" and not put back
            notes.append('WARN %s: key %r is type Float, which restore does not support (it will be lost)' % (fname, key))
            continue
        ok_shape = isinstance(ent, dict) and set(ent) == {'value', 'type'} and ent['type'] in valid_types
        if not ok_shape:
            fails.append('FAIL %s: bad entry shape for key %r' % (fname, key)); continue
        t, v = ent['type'], ent['value']
        if not isinstance(v, PY_TYPES[t]) or (t != 'Boolean' and isinstance(v, bool)):
            fails.append('FAIL %s: value type mismatch for %r' % (fname, key))
        if t == 'Integer' and not -2**31 <= v < 2**31:
            fails.append('FAIL %s: int overflow for %r' % (fname, key))
        if (key in SECRET_PREFS) != want_secret:
            fails.append('FAIL %s: key %r is in the wrong file (secret split)' % (fname, key))
        # is the key known to the code?
        m_dyn = re.fullmatch(r'(\w+?)_(en|decorate|pinned)', key)
        if key.startswith('opened ') or re.match(r'^\S+ opened \S+ \S+$', key):
            dyn += 1
            if not re.fullmatch(r'(?:\S+ )?opened \S+ \S+', key) or t != 'Integer' or not -3 <= v <= 3:
                fails.append('FAIL %s: bad LastOpened entry' % fname)
        elif m_dyn and m_dyn.group(1) in mod_ids:
            dyn += 1
            if t != 'Boolean':
                fails.append('FAIL module flag %r not Boolean' % key)
            if m_dyn.group(2) == 'pinned' and m_dyn.group(1) == 'drawer':
                fails.append('FAIL drawer cannot be pinned')
        elif key in literals:
            exp = EXPECT.get(key)
            if exp and exp[0] != t:
                fails.append('FAIL type of %r is %s, code expects %s' % (key, t, exp[0]))
            if exp and exp[1] is not None and v not in exp[1]:
                fails.append('FAIL value of %r outside its enum' % key)
        else:
            unknown.append(key)
    if fname == 'preferences' and 'order' in prefs:
        order = prefs['order']['value'].split(';')
        check(len(order) == len(set(order)), 'order: no duplicate module ids')
        check(set(order) <= mod_ids, 'order: all ids are modules of this source tree (unknown: %s)' % sorted(set(order) - mod_ids))
        missing = sorted(mod_ids - set(order))
        notes.append('INFO order lacks %s (the app inserts them where "debug" is)' % missing)
check(not unknown, 'all %d keys known to this source tree (%d dynamic) | unknown: %s' % (total, dyn, unknown))

# ---- catalogs
def load(zf, name):
    """Parsed json of files/<name>, or {} when the backup has no such file (a backup may omit any catalog)."""
    return strict(zf.read('files/' + name).decode('utf-8')) if 'files/' + name in zf.namelist() else {}


auto, pats, hosts, clear, redir = (load(z, n) for n in ('automations', 'patterns', 'hosts', 'clearUrlCatalog', 'redirect'))
ACTIONS = {'open', 'share', 'copy', 'ctabs', 'incognito', 'close', 'checkStatus', 'unshort', 'scan', 'drawer', 'webhook', 'toast'}
AUTO_KEYS = {'regex', 'excludeRegex', 'referrer', 'action', 'args', 'stop', 'enabled'}
PAT_KEYS = {'regex', 'excludeRegex', 'replacement', 'automatic', 'enabled', 'decode', 'encode', 'button', 'applied', 'description'}  # description: pass-through
HOST_KEYS = {'file', 'hosts', 'color', 'replace', 'enabled'}


def arr(x):
    return x if isinstance(x, list) else [] if x is None else [x]


def java_regex_ok(rx):
    """Python approximation of java.util.regex compile (no JVM available here)."""
    try:
        re.compile(rx.replace('(?i:', '(?:').replace('\\p{ASCII}', '[\\x00-\\x7f]'))
        return True
    except re.error:
        return False


bad = []
for name, e in auto.items():
    if not isinstance(e, dict) or not set(e) <= AUTO_KEYS: bad.append('automation fields: ' + name)
    for act in arr(e.get('action')):
        if act not in ACTIONS: bad.append('unknown action %r in %s' % (act, name))
    for rx in arr(e.get('regex')) + arr(e.get('excludeRegex')):
        if not java_regex_ok(rx): bad.append('regex does not compile in automation ' + name)
    if 'enabled' in e and not isinstance(e['enabled'], bool): bad.append('enabled type ' + name)
check(not bad, 'automations: %d rules, known actions/fields, regexes compile | %s' % (len(auto), bad))
bad = []
for name, e in pats.items():
    if not isinstance(e, dict) or not set(e) <= PAT_KEYS: bad.append('pattern fields: ' + name)
    for rx in arr(e.get('regex')) + arr(e.get('excludeRegex')):
        if not java_regex_ok(rx): bad.append('regex does not compile in pattern ' + name)
check(not bad, 'patterns: %d entries, known fields, regexes compile | %s' % (len(pats), bad))
bad = [n for n, e in hosts.items() if not set(e) <= HOST_KEYS]
check(not bad, 'hosts: %d entries, known fields' % len(hosts))
if clear:
    check(set(clear) == {'providers', 'personal'} and all(isinstance(v, dict) for v in clear.values()),
          'clearUrlCatalog: providers(%d) + personal(%d), each an object of objects' % (len(clear['providers']), len(clear['personal'])))
bad = [n for n, e in redir.items() if not isinstance(e, dict) or not set(e) <= {'from', 'to', 'auto'}]
check(not bad, 'redirect: %d entries, known fields (from/to/auto, see RedirectCatalog.java)' % len(redir))

# ---- automation sanity for the loop/auto-open design
for name, e in auto.items():
    if 'open' in arr(e.get('action')) and e.get('enabled', True):
        args = e.get('args', {})
        check(args.get('package') != 'com.trianguloy.urlchecker' and 'trianguloy' not in args.get('component', ''),
              'auto-open rule %r never targets URLCheck itself' % name[:50])

if a.original:  # diff summary vs original (names only)
    po = strict(zo.read('preferences').decode('utf-8')); pn = strict(z.read('preferences').decode('utf-8'))
    notes.append('INFO prefs: -%s +%s changed %s' % (sorted(set(po) - set(pn)), sorted(set(pn) - set(po)),
                                                    sorted(k for k in po if k in pn and po[k] != pn[k])))
    ao = load(zo, 'automations')
    check(all(k in auto and auto[k] == ao[k] for k in ao), 'all %d original automations unchanged (same key, same content)' % len(ao))
    check(list(ao) == [k for k in auto if k in ao], 'original automations keep their relative order')
    po_ = load(zo, 'patterns')
    changed_pats = [k for k in po_ if pats.get(k) != po_[k]]
    check(all(k in pats for k in po_), 'all %d original patterns still present' % len(po_))
    check(all({kk: vv for kk, vv in pats[k].items() if kk not in ('button', 'applied')} == po_[k] for k in po_),
          'original patterns only gained button/applied labels (%d touched)' % len(changed_pats))
    check(list(po_) == [k for k in pats if k in po_], 'original patterns keep their relative order')
    check(load(zo, 'hosts') == hosts, 'hosts catalog unchanged')

print('\n'.join(notes))
print('\n'.join(fails) if fails else '\nALL CHECKS PASSED')
sys.exit(1 if fails else 0)
