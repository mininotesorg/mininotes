"""The translation packs Firefox uses (docs/TRANSLATION.md): which files make each pack, and fetching them, checked.

Reads Mozilla's public list (Remote Settings, bucket main, collection translations-models), picks for each language and
direction the newest released files for the device asked about, and downloads them, each checked against the SHA-256 the
list gives. The packs are MPL-2.0 (mozilla/firefox-translations-models LICENSE). What is fetched here is what goes up to our
GitHub releases (the owner, 2026-10-13: packs come from a copy on our GitHub releases, checked against our own checksums).

  python tools/translation_packs.py list [--android]
  python tools/translation_packs.py fetch OUT_DIR fr es zh-Hans [--android]
  python tools/translation_packs.py release OUT_DIR fr es zh-Hans
      every file flat in OUT_DIR, as the release holds them, and translation-packs.txt listing them (pack, name, size,
      SHA-256, between tabs), which the apps read (Translation.manifest)
"""
import hashlib, json, os, shutil, sys, urllib.request

LIST = 'https://firefox.settings.services.mozilla.com/v1/buckets/main/collections/translations-models/records'
FILES = 'https://firefox-settings-attachments.cdn.mozilla.net/'
# The kinds of file a pack is made of, as the list names them.
KINDS = ('model', 'lex', 'vocab', 'srcvocab', 'trgvocab')


def released_for(record, android):
    """Whether Firefox's release channel on this kind of device takes this file: no condition, or one that lets it in."""
    said = (record.get('filter_expression') or '').strip()
    if not said:
        return True
    if said == "env.appinfo.OS == 'Android'":
        return android
    if said == "env.appinfo.OS != 'Android' || env.channel != 'release'":
        return not android
    return False  # nightly or beta only: still in testing


def version(record):
    """'2.1' > '2.0' > '2.0a2': numbers first, a test letter counting below the same number without one."""
    out = []
    for part in record['version'].split('.'):
        digits = ''.join(c for c in part if c.isdigit())
        out += [int(digits or 0), 0 if any(c.isalpha() for c in part) else 1]
    return out


def packs(android=False, testing=False):
    """{(from, to): {kind: record}} for the newest files of each pack Firefox would take."""
    with urllib.request.urlopen(LIST, timeout=60) as answer:
        records = json.load(answer)['data']
    best = {}
    for r in records:
        if r['fileType'] not in KINDS:
            continue
        if not released_for(r, android) and not testing:
            continue
        key = (r['fromLang'], r['toLang'])
        # One version per pack: every file of it from the same version.
        best.setdefault(key, {}).setdefault(r['version'], {})[r['fileType']] = r
    out = {}
    for key, by_version in best.items():
        newest = max(by_version, key=lambda v: version(next(iter(by_version[v].values()))))
        out[key] = by_version[newest]
    return out


def fetch(out_dir, languages, android=False):
    """Every file of each language's two packs (from and to English), checked, into OUT_DIR/<from><to>/."""
    found = packs(android) or {}
    if any(not released_for_any(found, l) for l in languages):
        found = {**packs(android, testing=True), **found}
    for language in languages:
        for pair in (('en', language), (language, 'en')):
            files = found.get(pair)
            if not files:
                print('no pack', pair)
                continue
            folder = os.path.join(out_dir, pair[0] + pair[1])
            os.makedirs(folder, exist_ok=True)
            for kind, r in files.items():
                path = os.path.join(folder, r['name'])
                if not (os.path.exists(path) and sha256(path) == r['attachment']['hash']):
                    urllib.request.urlretrieve(FILES + r['attachment']['location'], path)
                if sha256(path) != r['attachment']['hash']:
                    os.remove(path)
                    raise SystemExit('checksum wrong for ' + r['name'])
                print(pair, kind, r['name'], r['version'], r['attachment']['size'])


# The apps name a pack by their language codes joined (Translation.Pair.folder); Chinese is zh-Hans in both.
def release(out_dir, languages):
    """The packs as our release holds them: flat, and the list of them the apps read."""
    work = os.path.join(out_dir, 'by-pack')
    fetch(work, languages)
    os.makedirs(out_dir, exist_ok=True)
    lines = []
    for pack in sorted(os.listdir(work)):
        for name in sorted(os.listdir(os.path.join(work, pack))):
            if name.endswith('.yml'):
                continue
            src = os.path.join(work, pack, name)
            dst = os.path.join(out_dir, name)
            if os.path.exists(dst) and sha256(dst) != sha256(src):
                raise SystemExit('two packs have a file called ' + name)
            if not os.path.exists(dst):
                shutil.copyfile(src, dst)
            lines.append('\t'.join([pack, name, str(os.path.getsize(src)), sha256(src)]))
    with open(os.path.join(out_dir, 'translation-packs.txt'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('# Mininotes translation packs: Mozilla Firefox Translations models, MPL-2.0. pack, file, size, SHA-256.\n')
        f.write('\n'.join(lines) + '\n')
    print(len(lines), 'files listed')


def released_for_any(found, language):
    return ('en', language) in found and (language, 'en') in found


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for block in iter(lambda: f.read(1 << 20), b''):
            h.update(block)
    return h.hexdigest()


if __name__ == '__main__':
    android = '--android' in sys.argv
    args = [a for a in sys.argv[1:] if a != '--android']
    if args[:1] == ['list']:
        for (a, b), files in sorted(packs(android).items()):
            print(a, b, next(iter(files.values()))['version'], round(sum(r['attachment']['size'] for r in files.values()) / 1e6, 1), 'MB')
    elif args[:1] == ['release'] and len(args) >= 3:
        release(args[1], args[2:])
    elif args[:1] == ['fetch'] and len(args) >= 3:
        fetch(args[1], args[2:], android)
    else:
        print(__doc__)
