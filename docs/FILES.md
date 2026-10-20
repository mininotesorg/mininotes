# Notes kept as files

Plan from the owner's answers of 2026-10-13 (decision 162 in docs/HOME.md). Steps 1 to 4 are built on the PC, in 0.3.053 (2026-10-13); step 5, the phone, is planned at the end and not built.

## What the owner asked

- Notes stay inside Mininotes by default.
- A note, or a folder of notes, can also be kept as Markdown files in a folder anywhere on the PC, an external drive included, to see and organise them in Windows Explorer.
- Mininotes keeps those files up to date, shared notes included.
- When Mininotes loses track of a file (moved, renamed, deleted, or its drive unplugged), the note says so clearly.
- Notes on disk are not encrypted by Mininotes, and that is accepted.
- A private space is never written out of the app.

## What the person sees

- **Keep as a file…** in a note's menu: choose a folder; the note is written there as `Title.md`. Its icon wears a small file mark from then on.
- **Keep as a folder on disk…** in a folder's menu: the folder and everything in it is written as a Windows folder of `.md` files, sub-folders as sub-folders. A `.md` file put in that folder in Explorer appears in Mininotes.
- **Open a Markdown file…** in the menu, or a `.md` file dropped on Mininotes: it becomes a note kept as that file, where it is (Option B).
- **Show in Explorer** and **Keep it inside only** in the menu of a note kept as a file.
- **Lost**: the icon shows a warning mark, and the note opens on a line saying what happened, with:
  - **Find it…**: pick the file where it is now; Mininotes checks it is the same note (by its id) and follows it.
  - **Keep it inside only**: the last copy Mininotes has stays, as an ordinary note.
  - A drive that is unplugged is said as such ("on E:, not connected"), and the note comes back by itself when the drive does.

## The file

```
---
mininotes: 7f3c9a2e-…
---
Shopping
- Bread
- <u>Milk</u>
<!-- tab: Page 2 -->
…
```

- Markdown, UTF-8. Bold and italic as Mininotes already writes them; underline as `<u>…</u>`, since Markdown has none.
- Tabs as `<!-- tab: name -->` lines: other editors hide them, Mininotes reads them back as tabs.
- The short header carries the note's id, so a file moved or renamed is recognised as the same note.
- Files kept with the note go in a folder beside it, `Title.files\`.
- Who wrote what (the colours), the versions and who it is shared with stay in Mininotes' encrypted notebook, joined to the file by its id.

## Keeping them up to date

- Written by Mininotes on every save of the note, and when a change arrives from somebody it is shared with.
- Edits made outside (Notepad, VS Code, Obsidian) are noticed (Windows tells Mininotes when a file in a watched folder changes, and every file is checked again when Mininotes starts) and taken in as the owner's edit, then sent to the people the note is shared with as any edit is.
- Changed on both sides since Mininotes last wrote it: nothing is lost. The outside version is taken in, and Mininotes' own is kept in Versions.

## Never written out

- A private space, anything in it, and a notebook while it is locked. The menu lines are not there, and the writing refuses them, as the guard against codes in ordinary notes does (decision 114).

## Order of work

1. The file format both ways (Markdown in and out, tabs, underline, header), with tests.
2. Keep as a file…, Open a Markdown file…, Show in Explorer, Keep it inside only, for a single note on the PC.
3. Watching for changes outside, and the lost state with Find it…, the drive not connected.
4. Folders on disk, both ways.
5. The phone, with Android's folder picker.

## Built on the PC (0.3.053, 2026-10-13)

- **The file format** is `NoteFile` (android/app/src/main/java/org/mininotes/android/NoteFile.java), shared by both apps and in the PC's list of shared sources. Read back, a file Mininotes wrote gives exactly the words it was written from (NoteFileTest). Where a note itself says `<u>` or begins a line with `<!-- tab:`, a backslash is put before it, as Markdown escapes, and backslashes already there are doubled before such a place only. A file saved by Notepad with Windows line ends, or with a byte-order mark, is read as the same words. Another editor's own header lines (Obsidian's `tags:`, `aliases:`) are kept and written back. A file that only opens on a `---` rule is not taken for a header. The guard against private codes (decision 114) runs on what is written and on what is read.
- **Where each is kept** is the table `ondisk` in the notebook (schema 51: one new table, nothing else changed): the note's or folder's id, the path, the hash and time of the file as last written or read, the note's revision then, another editor's header lines, the folder kept on disk it was written into, and why it is lost. This PC's own: never sent, never shared.
- **The menus**: Keep as a file… on a note; Keep as a folder on disk… on a folder; Show in Explorer and Keep it inside only once kept; Find it… while lost; Open a Markdown file… in the main menu, under Create. Each with its own symbol, in all eleven languages.
- **Keeping them up to date** (DesktopOnDisk): a look after every save and every arrival, when Windows says a watched folder changed, at start, and every 30 seconds for what Windows cannot watch (a drive plugged in again, a folder on another computer). A look costs a query and one file time per note kept, and reads a file only when its time or the note's revision moved.
- **Lost**: a warning file mark on the icon (a file mark with a tick while all is well), and a line over the open note saying what happened, with Keep it inside only and Find it… (the one filled button). The words: "Shopping.md is no longer in Notes", "Shopping.md is on E:, not connected", "Shopping.md now holds another note", "Shopping.md is too long to take in".
- **Never written out**: the writing refuses an id that is in a private space open now, and anything while the window is locking or closed for the lock; private notes are never in the notebook in the first place, and the private screen has menus of its own, without these lines (DesktopOnDiskTest, aPrivateThingOrALockedNotebookIsNeverWrittenOut).

## Choices made while building (not settled by the answers above)

- **Renamed in Explorer, in the same folder, is followed, not lost**: the id in the header says which note it is, and the note takes the file's new name. Moved to another of the folders of a folder kept on disk, the note moves there too. Anywhere else, it is lost until Find it….
- **Renamed in Mininotes**: the file takes the new title. A note with no title keeps the file name it was given (its first line then), so typing its first line does not rename the file at every key.
- **A Markdown file dropped** anywhere but on a note (Home, a folder's icon or card, Temp, the rest of the window) becomes a note kept as that file. Dropped on a note, it is attached to it, as any file is.
- **Opening a Markdown file writes the header into it**, so it is known wherever it is moved or renamed to. Its words are not changed. A file of a note that was kept and lost is followed again rather than made a second note; a copy of a kept file (the original still there) becomes a note of its own, and its header gets the new note's id.
- **Nothing on the disk is ever deleted by Mininotes.** Keep it inside only, deleting a note for good, or binning a folder leaves the files where they are. A file deleted in Explorer makes the note lost, not deleted. A note in the bin or the archive leaves its file as it was, and is written again if it comes back.
- **Too long**: a file over 24,000 characters (the note limit) is not taken in and not written over; the note says so until the file changes again. Open a Markdown file… refuses it.
- **Read only here** (shared by somebody else, Can read): a change made to the file is kept in Versions, and the file says the note's words again.
- **Files kept with a note** go one way, into Title.files beside it: those not there yet are written, nothing is taken away. Two of the same name are "map.txt" and "map (2).txt".
- **A note moved out of a folder kept on disk**, in Mininotes, stays kept as a file of its own, where it is.
- **Line ends**: written with Unix line ends, which Notepad reads; a file saved with Windows line ends is read as the same words.
- **On an encrypted notebook**, the bar says after keeping: "The file is not encrypted."
- **Windows' own file box** (0.3.054, the owner: "the modern Windows Explorer when saving and opening a MD file"): Keep as a file… is a Save box with the note's title as the name (the owner: "by default the name when saving should be the title of the note"), Keep as a folder on disk… picks a folder, Open a Markdown file… and Find it… open files, all four in the box Explorer and Notepad use (DesktopFileDialog). A different name typed in the Save box becomes the note's title, as a file renamed in Explorer does; a file of that name already there is never written over (the note is kept beside it as Name (2).md). Java's old box is used only where Windows' cannot be had.

## What was found

- Nothing in how notes are stored had to change: a note is one text with its tabs and marks in it (decisions 76 and 142), so the file is that text with two marks spelled the Markdown way and an id on top.
- The page already follows a note changed underneath it (changedUnderneath), typed words kept, so a change taken in from a file shows on the open page the same way a change from another device does.

## Left for later

- Files added to a Title.files folder in Explorer are not taken in, and a file replaced or renamed in Mininotes is not written again there (only missing ones are).
- A folder deleted in Explorer leaves its notes lost, one by one; it is not said for the folder as a whole.
- The tree's lines do not wear the file mark; Home's icons and the note's line do.
- Show in Explorer was not opened in a test (it would open Explorer on the desktop); it uses `explorer.exe /select,`.

## The phone (step 5): the plan, not built

- **Where**: Android's folder picker (`ACTION_OPEN_DOCUMENT_TREE`), with the permission kept (`takePersistableUriPermission`), so a folder on the phone, its SD card or a USB drive is chosen once and kept. Files are written and read through `DocumentsContract`, never by file path, which Android no longer gives apps. The same `NoteFile` writes and reads them, so a file kept by the phone and one kept by the PC are the same format.
- **The link** is the same table, `ondisk`: its path holds the tree's address and the document's id instead of a Windows path. A link is the device's own, as on the PC: each device keeps its own files, and the note is shared between them as notes always are.
- **Keeping up to date**: written on every save and arrival, as on the PC. Android cannot watch a folder chosen this way, so the files are looked at when Mininotes comes to the front and every few minutes while it is open, by their last-modified time; a change is taken in as the owner's edit, and changed on both sides the file's words are taken in and the phone's own kept in Versions.
- **Lost**: a folder whose permission was taken away, or a card or USB drive taken out, is said as on the PC ("on the SD card, not connected") and comes back by itself; Find it… opens the picker on one file (`ACTION_OPEN_DOCUMENT`) and takes it only if its header says it is this note.
- **Menus**: the same lines in a note's and a folder's long-press menus, with the same symbols; Show in Explorer becomes Show in Files (opens the folder in the Files app where it can).
- **Never written out**: the same refusal for a private space and while locked.
- **To check on a phone first**: GrapheneOS's storage scopes, and how slow `DocumentsContract` is for a folder of a few hundred notes; a look by modified time should keep it to one query per folder.

## Files of any type, and files only opened: discussed with the owner, 2026-10-14, not built

What came out of the owner's questions ("for an external file I just want to open, work in and close, how should we
present it when I don't want it in the app?", "could Mininotes become an app we could use to sync and collaborate on any
type of file like docx?", "there should be a default app set to open them", "having the option Open to look and Open to
edit would be good"):

- **Found:** the owner's release notes were kept as a file in the Stables project, which another session also writes. When
  a note and its file have both changed since Mininotes last wrote it, the file's words are taken in and Mininotes' own go
  to Versions (see *Keeping them up to date*): to the owner this looked like "my changes are not reflected in the file".
  Not confirmed (the PC notebook is encrypted); the note's Versions would show it.
- **Edit a file…** beside Open a Markdown file…: the file open as Notepad opens it, saved straight to it, in Recent while
  open with the file mark, no tile on Home, nothing kept once closed. *Keep in Mininotes* in its menu for the other kind.
  Closed, its note is removed completely, not put in the bin: the file on disk holds the words (the owner, 2026-10-14: "I
  prefer removed completely"). Built on the PC as a window of its own, no note made (HOME.md, decision 172).
- **Both changed, for Markdown:** asked each time (the owner, 2026-10-14: "let's ask first"), instead of the file silently
  winning. Built on the PC (HOME.md, decision 171).
- **A synced folder on the PC** for any file type, built in 0.3.063 (HOME.md, decision 174): a folder kept on disk, or a
  Windows folder already there taken in with *Keep a Windows folder in sync…*; a file saved there by Word or LibreOffice is
  taken in, the same bytes, once saved and let alone, and goes to the owner's own devices; never written over while
  another program holds it. Changed on this PC and in Mininotes both: asked which version it keeps (the owner, 2026-10-15:
  "let's start with asking you which version to keep"), nothing replaced until then. People after own devices.
- **Opening a file,** reworked in 0.3.065 (HOME.md, decision 177): one **Open**, named by the program the kind opens in
  (Windows' own choice, read each time), to work in where this device may change it, its saves coming back; read only
  where it was shared to be read. **Open with…** changes the program for that kind, kept in Mininotes only where it is
  not Windows' choice. The tile wears the kind's icon.
- **The phone:** Open saving back depends on the editing app accepting a writable address; not yet seen with Word or Docs.
- **Not possible this way:** two people typing in one Word document at once. One at a time, with a copy where they cross.
- **Order:** both-changed for Markdown and Edit a file… (small); the synced folder and Open to look / Open to edit on the
  PC (about a week); sharing a synced folder with people (small after it); saving back on the phone (large).
