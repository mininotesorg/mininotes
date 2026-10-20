# Translating notes on the device

Plan agreed with the owner on 2026-10-12 ("we should go with Bergamot"). Nothing here is built yet.

## What the person sees

- Select some text in a note, then **Translate** (right-click on the PC, the selection bar on the phone). Or **Translate** in a note's menu for the whole note.
- A box shows the translation, with **Copy**, **Replace** (the selection) and **Add as a tab** (the whole note, as a tab named for the language, for example "FR → EN").
- The language it is translated into is the app's language unless changed in the box. The language it comes from is guessed and can be changed.
- Each language is a pack downloaded the first time it is needed, with its size said first and the busy strip while it comes. Settings, Translation lists the packs on this device and removes them.
- Nothing is ever sent anywhere: the text is translated on the device.

## The pieces

- **Engine: bergamot-translator** (github.com/browsermt/bergamot-translator), MPL-2.0, the engine Firefox translates with. MPL-2.0 can be combined with our GPL-3.0-or-later. Not **slimt**, which is lighter but GPL-2.0 only, and that cannot be combined with GPL-3.0.
- **Models: Mozilla's Firefox Translations models.** The old models repository says MPL-2.0; it is archived and the models now live in Mozilla's registry. The licence is to be checked there before anything ships.
- **Pairs go through English** where there is no direct pair: French to Chinese is French to English, then English to Chinese, so each language is one pack each way with English.
- Sizes are estimates until measured: roughly 15 to 40 MB per language and direction for the small models, and a few MB of engine in each app.

## Phases

**0. Checks (about a day).** Write down the engine's and the models' licences in NOTICE. List which of our eleven languages have a released pack (en, fr, es, zh, de, pt, it, ja, ru, hi, fa); Persian and Hindi may be in development only. Measure each pack's size.

**1. PC trial (two or three days).** Build bergamot-translator on Windows as a small helper program that reads text and writes the translation, started by Mininotes when needed: a crash in it cannot take the notebook down, and nothing native runs inside the app. Translate a paragraph French to English and English to Chinese. Measure the time it takes and the memory it uses.

**2. On the PC (three to five days).** The Translate lines in the note's menus, the box, Add as a tab, the packs in Settings, the busy strip while a pack downloads or a long note is translated, and a way to stop.

**3. On the phone (five to eight days).** The same engine built for Android (arm64, plus x86_64 for the emulator) and called from the app. The same Translate line, box and packs. The app grows by a few MB; the packs stay out of it.

**4. Finishing.** Bold, italic, underline and line breaks kept through a translation. Long notes translated in pieces in the background. The packs' checksums checked before use. F-Droid notes in docs/FDROID.md.

## Phase 0, found (2026-10-13)

- **Licences**: bergamot-translator, mozilla/translations and mozilla/firefox-translations-models each carry the MPL-2.0 LICENSE file. MPL-2.0 files can be shipped beside GPL-3.0-or-later code; the packs we pass on stay under MPL-2.0, with where they came from said in NOTICE.
- **Where Firefox gets them**: the public list at firefox.settings.services.mozilla.com (bucket main, collection translations-models), each file with its SHA-256, downloaded from firefox-settings-attachments.cdn.mozilla.net. Tried: a file came down with the size and checksum the list gives.
- **Our languages**: every one has a pack each way with English, 60 languages in all. Each pack is a model, a word list (lex) and one or two vocabularies.

| Language | From English | To English | Note |
|---|---|---|---|
| French, Spanish, German, Portuguese, Italian | about 37 MB | about 37 MB | released |
| Chinese (Simplified, zh-Hans) | 52 MB | 70 MB on PCs, 54 MB on Android | released |
| Japanese | 50 MB | 55 MB | released |
| Russian | 35 MB | 37 MB | from English still in testing at Mozilla |
| Hindi | 22 MB | 23 MB | released, first version |
| Persian | 22 MB | 24 MB | to English still in testing at Mozilla |

So French both ways is about 74 MB, Chinese both ways about 120 MB. A pair not through English (French to Chinese) needs both packs and takes two steps.

- **Tools on this PC**: Visual Studio 2022 Build Tools (MSVC 14.44, its CMake and Ninja, clang) and the Windows 10 SDK, enough to build the engine for phase 1. The Android NDK is not installed yet; phase 3 needs it.

## Phase 1, found (2026-10-13)

- **It builds on Windows from open-source parts only**: bergamot-translator at 9271618 (2024-05-12) with its WebAssembly-compatible sources (intgemm, sentencepiece, onnx sgemm; no Intel MKL), PCRE2 built in, Visual Studio 2022 Build Tools and Ninja. Two fixes to the sentence splitter's build for MSVC are in tools/translation-engine-windows.patch; tools/build-translation-engine.ps1 does the whole build into native/build-pc/app/bergamot.exe (10.6 MB, needs nothing else). native/ is never in the tree.
- **The packs**: tools/translation_packs.py lists Mozilla's packs and fetches one language's two packs, each file checked against its SHA-256.
- **Tried on this PC** (4 threads, the engine started fresh each time, so the times include loading the pack):

| What | Time | Memory at most |
|---|---|---|
| French to English, 2 sentences | 0.7 s | 250 MB |
| English to Chinese, 2 sentences | 1.1 s | 410 MB |
| Chinese to English, 2 sentences | 1.9 s | 540 MB |
| English to Spanish, 2 sentences | 0.8 s | 250 MB |
| French to English, 6,500 characters | 2.3 s | 440 MB |

French "Bonjour à tous. Nous allons au marché demain matin pour acheter des légumes, du pain et du fromage. La réunion de jeudi est déplacée à vendredi, à quinze heures." came out as "Hello everyone. We're going to the market tomorrow morning to buy vegetables, bread and cheese. Thursday's meeting is moved to Friday, at fifteen o'clock." and, on through English, Chinese "大家好。 明天早上我们去市场买蔬菜、面包和奶酪。 周四的会议将移至周五十五点。". Good enough to build on; hours are taken literally ("fifteen o'clock").

- **What follows from it**: the helper is started when Translate is pressed and closed when done, so its memory is taken back; a pair through English is two runs (about 2 s for French to Chinese). Kept running between translations, the second one would be quicker; that is for phase 2 to decide.

## Phase 2, done on the PC (0.3.051, 2026-10-13)

- Translate… in a note's menu, under This note on its page, and on the page's right-click when words are chosen. The box: the language guessed and the app's, both changeable; Copy; Replace (what was chosen) or Add as a tab (the whole tab, named for the language); Stop; the size of missing packs said before Download, then a percentage. Settings, Translation lists the packs here and removes them.
- The engine is in the app (app/translate/bergamot.exe) with the licences of every part (THIRD-PARTY.txt, licenses/). Packs live beside the notebook in translation/<pack>/.
- Lines, list marks, checkboxes, empty lines and marks around a whole line come through (Translation.prepare and restore); marks inside a line do not yet.
- Tests: TranslationTest (packs, list, settings, the guess, lines kept), DesktopTranslateTest (fetch checked against the list, a changed file refused, removal, the box, Add as a tab; the real engine French to Chinese when MININOTES_REAL_PACKS is set: right, 5 to 9 s with the PC busy, about 2 s of it loading each pack).
- Not yet: our release translation-packs-1 is not published, so on another PC Download says the packs are not on GitHub yet. On the owner's PC the French, Spanish and Chinese packs were put in place by hand (258 MB) from tools/translation_packs.py release. Keeping the engine loaded between translations, to make the second one quick, is for phase 4.

## Packs published (2026-10-13)

The owner said yes: release translation-packs-1 on mininotesorg/mininotes, not marked Latest (the app's updater reads Latest, which stays v0.3.016), 63 files: translation-packs.txt and every pack of the ten languages both ways with English, 782 MB, made by tools/translation_packs.py release. English to Russian and Persian to English are still in testing at Mozilla. Every language in the lists can now be downloaded from the app.

Turkish added (the owner, 2026-10-13: "yes, please add the Turkish file"): Mozilla's en-tr and tr-en packs (version 1.0, six files, 43 MB) fetched with tools/translation_packs.py, checked against Mozilla's checksums, uploaded to translation-packs-1, and their six lines added to translation-packs.txt there (69 files on the release). Downloaded back from GitHub, every file matched its line. Turkish is a language to translate, not one the app is written in (Translation.name gives it its own name, Türkçe). Tried by hand with the engine on the published files: "Hello everyone. We are going to the market tomorrow morning to buy bread and cheese." came out "Herkese merhaba. Yarın sabah ekmek ve peynir almak için pazara gidiyoruz." and back to English unchanged in sense.

## Decisions (the owner, 2026-10-13)

1. **Where the packs come from**: a copy on our GitHub releases, checked against our own checksums.
2. **What Translate gives**: the box, with Copy, Replace and Add as a tab.
3. **First languages**: French, Spanish and Chinese, each with English.

## Risks

- Building the engine on Windows is the hardest step; phase 1 is there to find out early.
- Quality is lower for some pairs, Chinese included, and Persian and Hindi may not be released yet.
- The packs are big, so they are only ever downloaded when asked for, never with the app.
