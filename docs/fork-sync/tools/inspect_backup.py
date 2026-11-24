"""Prints the STRUCTURE of a .ucbckp (zip entries, key names grouped by kind, formatting facts). Never prints values.
Usage: python inspect_backup.py <file.ucbckp>
Use it to learn what an unknown backup contains before editing it (see settings-backup-updater.md).
"""
import sys, re, json, zipfile, collections

z = zipfile.ZipFile(sys.argv[1])
print('zip comment :', z.comment.decode('utf-8', 'replace'))
for i in z.infolist():
    raw = z.read(i.filename)
    print('entry %-24s %7d bytes | method %d | flags %#x | CR=%d LF=%d BOM=%s' % (
        i.filename, len(raw), i.compress_type, i.flag_bits, raw.count(b'\r'), raw.count(b'\n'), raw[:3] == b'\xef\xbb\xbf'))
print('version     :', z.read('version').decode('utf-8'))

for name in ('preferences', 'secrets'):
    if name not in z.namelist():
        continue
    data = json.loads(z.read(name).decode('utf-8'))
    kinds = collections.OrderedDict()
    for key, ent in data.items():
        if re.fullmatch(r'(?:\S+ )?opened \S+ \S+', key):
            kind = 'LastOpened counter (domain-prefixed)' if not key.startswith('opened ') else 'LastOpened counter (global)'
        elif re.fullmatch(r'\w+_(en|decorate|pinned)', key):
            kind = 'module flag *_' + key.rsplit('_', 1)[1]
        else:
            kind = 'named key'
        kinds.setdefault(kind, []).append((key, ent['type']))
    print('\n[%s] %d entries' % (name, len(data)))
    for kind, items in kinds.items():
        if kind.startswith('LastOpened'):
            print('  %-38s %d' % (kind, len(items)))   # keys can contain domains: counted, never listed
        else:
            print('  %-38s %s' % (kind, ', '.join('%s:%s' % kv for kv in items)))

for name in z.namelist():
    if name.startswith('files/') and name != 'files/.empty':
        try:
            data = json.loads(z.read(name).decode('utf-8'))
            fields = ''
            if name in ('files/hosts', 'files/patterns', 'files/automations', 'files/redirect'):   # keys of the entries (never values)
                fields = '; entry fields used: %s' % sorted({f for v in data.values() if isinstance(v, dict) for f in v})
            print('\n[%s] json object with %d top-level entries%s' % (name, len(data), fields))
        except ValueError:
            print('\n[%s] not json (plain text file)' % name)
