"""Every phrase the app shows, by its English, into android/app/src/main/resources/org/mininotes/android/words/en.json.

The app's words are looked up by their English (Say.java, decision 160): this collects the phrases the sources hand to the
places words are put on the screen - labels, rows, menu lines, buttons, boxes, hints - and the menu lines Symbols knows,
and writes them as the list a translation starts from, in the flat JSON shape Weblate keeps. Each other language's list is
kept in step: a phrase new here is added to it untranslated (empty), one no longer used is left for its translator to see.

Run from the project root:  python tools/extract_words.py
"""
import glob, json, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORDS = os.path.join(ROOT, 'android', 'app', 'src', 'main', 'resources', 'org', 'mininotes', 'android', 'words')
SOURCES = glob.glob(os.path.join(ROOT, 'android', 'app', 'src', 'main', 'java', 'org', 'mininotes', 'android', '*.java')) + \
          glob.glob(os.path.join(ROOT, 'windows', 'src', 'main', 'java', 'org', 'mininotes', 'android', '*.java'))

CALLS = (r'(?:label|line|under|part|row|tapRow|toDo|primary|heading|toggle|dimRow|alert|toast|setTitle|setMessage|'
         r'setPositiveButton|setNegativeButton|setNeutralButton|switchRow|dropRow|body|quiet|note|button|item|spread|tell|'
         r'confirm|danger|field|choice|heldMenu|sheet|card|ask|busy|t)\(')
LITERAL = r'"((?:[^"\\]|\\.)+)"'
AT_CALL = re.compile(CALLS + r'(?:[^;(){}]*?,\s*)?' + LITERAL)
HELD_ROW = re.compile(LITERAL + r'\s*,\s*\(Runnable\)')
CONSTANT = re.compile(r'static final String [A-Z_][A-Z0-9_]*\s*=\s*' + LITERAL + r'\s*[;,]')
# Words a method gives back (a role's name, what it does) and either side of a choice made in place.
RETURNED = re.compile(r'return\s+' + LITERAL + r'\s*;')
CHOICE = re.compile(r'\?\s*' + LITERAL + r'\s*:\s*' + LITERAL)


def unescape(s):
    """A Java string literal's escapes undone: \\uXXXX, \\n, \\t, \\" and \\\\."""
    out, i = [], 0
    while i < len(s):
        c = s[i]
        if c == '\\' and i + 1 < len(s):
            e = s[i + 1]
            if e == 'u' and i + 5 < len(s):
                out.append(chr(int(s[i + 2:i + 6], 16)))
                i += 6
                continue
            out.append({'n': '\n', 't': '\t', 'r': '\r'}.get(e, e))
            i += 2
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def wording(s):
    """Words a person reads: letters and a space, or one capitalised word; not a key, a path, a format or a log line."""
    if len(s) < 2 or not re.search(r'[A-Za-z]{2}', s):
        return False
    if re.search(r'[/\\]|^[a-z_.]+$|^[A-Z_]+$|%[sd]|\{[a-z]|https?:|\.java|SELECT |INSERT |CREATE |UPDATE ', s):
        return False
    # A key of the look (ComboBox.padding), a class or a provider's name (AndroidKeyStore): one word, a dot or a hump inside.
    if ' ' not in s and (re.search(r'[a-z][.][A-Za-z]', s) or re.search(r'[a-z][A-Z]', s)):
        return False
    return bool(re.match(r'^[A-Z←-⇿›"“(+]', s) or ' ' in s)


def chains(text):
    """Every sentence built from pieces, "Take "+who+" off "+what+"?", as the template Say matches: Take {0} off {1}?

    Read from each string literal on: literal, then + something + literal, and so on, the somethings being anything with
    balanced brackets and no string in it; and a something just before the first literal (who+" is off "+what).
    """
    out = []
    lit = re.compile(LITERAL)
    for m in lit.finditer(text):
        pieces, at = [unescape(m.group(1))], m.end()
        # A something before it, joined with +.
        before = text[:m.start()].rstrip()
        lead = None
        if before.endswith('+'):
            e = len(before) - 1
            k = e - 1
            while k >= 0 and text[k] == ' ':
                k -= 1
            depth, j = 0, k
            while j >= 0:
                c = text[j]
                if c in ')]':
                    depth += 1
                elif c in '([':
                    if depth == 0:
                        break
                    depth -= 1
                elif depth == 0 and c in ',;=?:{}+!&|<>' or c == '"':
                    break
                j -= 1
            lead = text[j + 1:k + 1].strip()
        slots = 0
        while True:
            rest = text[at:]
            mm = re.match(r'\s*\+\s*', rest)
            if not mm:
                break
            p = at + mm.end()
            if p < len(text) and text[p] == '"':
                m2 = lit.match(text, p)
                pieces[-1] += unescape(m2.group(1))
                at = m2.end()
                continue
            depth, q = 0, p
            while q < len(text):
                c = text[q]
                if c == '"':
                    break
                if c in '([':
                    depth += 1
                elif c in ')]':
                    if depth == 0:
                        break
                    depth -= 1
                elif depth == 0 and c in ',;+?:':
                    break
                q += 1
            if q >= len(text) or text[q] == '"' or q == p:
                break
            pieces.append(None)
            m3 = re.match(r'\s*\+\s*' + LITERAL, text[q:])
            if not m3:
                break
            pieces.append(unescape(m3.group(1)))
            at = q + m3.end()
        if None not in pieces and lead is None:
            continue
        n, template = 0, ''
        if lead:
            template, n = '{0}', 1
        for piece in pieces:
            if piece is None:
                template += '{%d}' % n
                n += 1
            else:
                template += piece
        out.append(template)
    return out


def main():
    found = set()
    for path in SOURCES:
        text = open(path, encoding='utf-8').read()
        for pattern in (AT_CALL, HELD_ROW, CONSTANT, RETURNED, CHOICE):
            for m in pattern.finditer(text):
                for group in m.groups():
                    phrase = unescape(group)
                    if wording(phrase):
                        found.add(phrase)
        for template in chains(text):
            plain = re.sub(r'\{\d\}', '', template)
            if '{0}' in template and wording(plain.strip() or 'x') and re.search(r'[A-Za-z]{3}', plain):
                found.add(template)
    os.makedirs(WORDS, exist_ok=True)
    english = {p: p for p in sorted(found, key=str.lower)}
    with open(os.path.join(WORDS, 'en.json'), 'w', encoding='utf-8') as out:
        json.dump(english, out, ensure_ascii=False, indent=1)
    for other in glob.glob(os.path.join(WORDS, '*.json')):
        if other.endswith('en.json'):
            continue
        kept = json.load(open(other, encoding='utf-8'))
        for p in english:
            kept.setdefault(p, '')
        with open(other, 'w', encoding='utf-8') as out:
            json.dump(dict(sorted(kept.items(), key=lambda kv: kv[0].lower())), out, ensure_ascii=False, indent=1)
    print(len(english), 'phrases')


if __name__ == '__main__':
    main()
