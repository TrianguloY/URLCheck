r"""Minimal emulation of Android's org.json serialisation (JSONObject.toString()).

Android JSONStringer.string():
  " -> \"   \\ -> \\\\   / -> \/   \b \f \n \r \t escaped, other < 0x20 -> \\u00XX,
  U+2028 / U+2029 -> \\u2028 / \\u2029. Everything else (incl. non-ASCII) is written as-is.
Object key order is insertion order (LinkedHashMap), no whitespace.
"""
import json


def _s(text: str) -> str:
    out = ['"']
    for c in text:
        if c in ('"', '\\'):
            out.append('\\' + c)
        elif c == '/':
            out.append('\\/')
        elif c == '\t':
            out.append('\\t')
        elif c == '\b':
            out.append('\\b')
        elif c == '\n':
            out.append('\\n')
        elif c == '\r':
            out.append('\\r')
        elif c == '\f':
            out.append('\\f')
        elif c in ('\u2028', '\u2029') or ord(c) < 0x20:
            out.append('\\u%04x' % ord(c))
        else:
            out.append(c)
    out.append('"')
    return ''.join(out)


def dumps(o) -> str:
    if o is None:
        return 'null'
    if o is True:
        return 'true'
    if o is False:
        return 'false'
    if isinstance(o, int):
        return str(o)
    if isinstance(o, float):
        return repr(o)
    if isinstance(o, str):
        return _s(o)
    if isinstance(o, (list, tuple)):
        return '[' + ','.join(dumps(v) for v in o) + ']'
    if isinstance(o, dict):
        return '{' + ','.join(_s(k) + ':' + dumps(v) for k, v in o.items()) + '}'
    raise TypeError(type(o))


def loads(text: str):
    # json.loads keeps insertion order of keys; duplicate keys are detected separately
    return json.loads(text)


def loads_strict(text: str):
    """json.loads that fails on duplicate keys."""
    def hook(pairs):
        keys = [k for k, _ in pairs]
        if len(keys) != len(set(keys)):
            dup = sorted({k for k in keys if keys.count(k) > 1})
            raise ValueError('duplicate keys: %r' % dup[:3])
        return dict(pairs)
    return json.loads(text, object_pairs_hook=hook)
