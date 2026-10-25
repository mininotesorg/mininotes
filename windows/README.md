# Mininotes for Windows

The Windows desktop app uses the existing Android app's Java notebook
and sharing logic. The interface is Swing; SQLite, preferences and protected
keys have Windows adapters. Android source files are not modified by this build.
The launcher, window, header and tray use the mobile notebook logo. Its exact
geometry and colours from Android's `res/drawable/ic_note.xml` are rendered by
`DesktopIcon.java`; packaging generates a multi-resolution Windows ICO.

## Build and run

Windows x64, JDK 17, and internet access for the first dependency download:

```powershell
./windows/build.ps1 -Package
```

The script runs the shared and Windows tests and makes
`dist/latest/Mininotes-Windows-0.4.027.zip` plus its SHA-256 checksum. Extract the whole
ZIP and run `Mininotes/Mininotes.exe`. Keep the runtime and app folders beside
the executable. No Java installation is required to run this bundle.

It also makes the installer, `dist/latest/Mininotes-Windows-<version>.msi` and its checksum
(decision 235): per user, with no administrator rights, under `%LOCALAPPDATA%\Mininotes App`,
and in Settings, Apps afterwards, where uninstalling keeps the notebook. It needs WiX Toolset 3.14:
`WIX` set to its folder, or `tools\wix314` in your user folder. `-NoMsi` leaves it out.
Its pages come from `windows/installer` (decision 237): the logo in place of WiX's artwork
(`make-images.ps1` makes the two BMPs), no licence page, and Launch Mininotes, ticked, on the last
page. `main.wxs` there is jpackage's own with those pages in it; its comments say what changed.

Android and Windows use the same version number, currently 0.4.027. Installation
and verification are recorded separately for each platform.

This is an unsigned preview, not a published release.
The bundled runtime makes it substantially larger than the Android APK.

In 0.3.029 the scrollbar handle remains visible without hover, on light or dark
paper. Opening a folder from Recent keeps a Back to Recent action, including
from the right-hand Recent list. Esc goes back through the browsing trail;
returning to a list restores its scroll position. Temp and Shared with me use
the same browsing behavior. This does not move the folder or change where a
file dragged out of it belongs.

For development:

```powershell
$env:JAVA_HOME = 'path-to-your-JDK-17'
./android/gradlew.bat -p windows test run
```

Local data lives in `%LOCALAPPDATA%/Mininotes`. Only one process may open that
notebook at a time. `--data-dir <folder>` selects a separate notebook for tests;
`--offline` prevents the desktop session from starting a node. A normal launch
starts its own Maxima node. **Settings → Keep listening** (*While the window is
closed*) keeps the node running in the system tray when the window closes.
Double-click the tray icon to reopen, or use its Exit command to stop Mininotes.
This does not start the app automatically when Windows starts.

**Troubleshooting.** What the PC does with sharing is written to a plain-text log,
`%LOCALAPPDATA%/Mininotes/logs/mininotes.log` (in the `--data-dir` folder when one
is given): one line per event with the time, the same tags as the phone's log
(`Mininotes/Post`, `Mininotes/Persons`, `Mininotes/Link`, `Mininotes/Drop`…) and
counts and states only - never a name, an address, a key or a word of a note, so
it can be sent to whoever helps. It grows to about 1 MB, is then kept as
`mininotes.1.log` and a new one starts: two files at most. Tests write no log
unless started with `-Dmininotes.log=<folder>`.

## Recovery writing and a safe switch

Windows 0.3.028 keeps invalid local edits in a desktop-only table in the notebook.
The complete title, text, original revision/text and available writer information
commit on the storage worker before Home, another note, New note or Exit proceeds.
A write failure still blocks leaving. Recovery writing is separate from saved
notes, Home/search and sharing; it never queues invalid text for delivery.
When the notebook is locked the table uses the same database encryption, with no
plaintext journal. An unlocked notebook remains unencrypted.

Use **⋯ → Recovery writing…** after leaving or restarting. **Reopen** displays
complete writing for shortening and an ordinary save; that save merges against
the current note. **Save as new note** makes a separate note when within the same
limits. **Export text…** writes the entire title and body as UTF-8 text, with a
plaintext warning for a locked notebook. **Discard…** requires explicit confirmation.
Independent snapshots stay until that exact snapshot is saved or discarded.
If the original note is gone or read only, reopen and shorten the writing, return
to Home to keep that shortened draft, then use Save as new note. The shared note
is never written through a read-only restriction.

**Back up recovery writing…** creates a password-protected `.mnwriting` file.
Keep that password separately. **Restore recovery writing…** authenticates the
file and adds drafts alongside those here, without replacing saved notes or other
drafts. This desktop-only backup is separate from the ordinary portable notebook
backup. The portable export explicitly warns when recovery writing is pending
and covers saved notes only. For a complete local backup, export both the portable
notebook backup and the locked recovery-writing backup. Private-space exports
remain separate. Test a restore in a separate `--data-dir` notebook before relying
on a backup; this session's restore tests used synthetic writing only.

The new build cannot retrieve unsaved text from an older process's memory. To
switch a currently blocked older session safely:

1. Leave the old app running. Manually copy its **complete title and complete body**
   into a UTF-8 text file in storage you trust. Save it, reopen it, and check both
   beginning and end, including the last unsaved words. Preserve any other open
   unsaved writing the same way. A plaintext file needs appropriate protection.
2. Only after verifying those copies, shorten the old editor to the existing
   limits and save with Ctrl+S. Export a notebook backup and verify its restore in
   an isolated notebook. Keep the complete text files and original backup.
3. Exit the old app normally through **⋯ → Exit Mininotes** (or the tray's Exit).
   Closing its window may keep it running. Do not kill it or launch the new build
   to force a handover while writing is unpreserved.
4. Open `dist/latest/Mininotes-Windows-0.4.027/Mininotes.exe` or the local
   `dist/latest/Mininotes.lnk`. Paste a preserved oversized edit back into its
   intended note if needed. Wait for **Writing kept on this PC**, then use Recovery
   writing to reopen, export or shorten it. Retain the verified external copy.

Packaging retains older runnable app folders and timestamped images, archives
only older ZIP/checksum files, and creates a new folder on a same-version rebuild.
It never closes or launches your existing session. `-SkipTests` is only for an
unchanged build whose full test suite has just passed separately.

## Using the pad

Notes, profile names and settings save automatically. Profile connection refreshes
preserve the scroll position and leave address selections alone when unchanged.

Over a note's title, **← Home** and the collections it is in; under it, where it
stands, in marks rather than words: its sync mark, then a round for each person it
reaches, tinted by where they stand, and while it is being saved or sent a few
words beside them. Click the mark to send what is waiting or see who has it, and a
round to see who and where. A file still going keeps the mark amber; one too big to
travel never holds it back. Only the other device's answer clears the mark, never a
relay taking the note, and it is the last known shared state, not a guarantee that
an offline device has no newer edits.

**Home** fills the window, as a phone's home screen fills the phone (see
`docs/HOME.md`): your collections and notes as icons, in your order, then the
files other devices sent you, marked *new* until opened. **+** at the bottom
right makes a note or a collection, or takes one from another device (scan the code
they show, or paste the code they sent - also in every menu); the search field under the icons finds
notes, collections and files; the button beside it (or Ctrl+Tab) shows the
notes and collections you have open, with **Close all**; the dock under it holds
your favourites. A collection opens as a card over Home - collections go inside
collections, as deep as you like - and a note fills the window, with **← Home**
(or Esc) to go back. Drag an icon onto a note to put the two in a new collection,
onto a collection to put it in, into an empty cell to put it there (icons stay where you put them, gaps and all), onto the dock to make
it a favourite, onto the Archive or the Bin to put it away there. Home is pages in every
direction, each as big as the window: the mouse wheel turns them up and down, Shift with
it (or a touchpad) sideways, and Page Up / Page Down; a double-click on empty room goes
back to the main page, where the favourites and the search are; Ctrl with the wheel shows
every page at once, where a page can be dragged to another place, or onto another page to
change places with it. Carry an icon to the left or right edge and hold it there to take it
to the page beyond - that is how a new page comes to be; carried above the rows it goes to the
page above with continuous scrolling, and down to the page's lower edge, to the page below. The dot before the version is green on
the newest version, yellow when a newer one is out. Right-click is everything about what you
click: on Home, its colour, the text size, and whether the favourites and the search show. The archive and the bin are icons on Home,
each with how many things wait in it, opening as a card of what is in it (Settings → Window can put them
back in the ⋯ menu). Right-click, or the Menu key, gives any icon's menu; Tab and the
arrows move between icons, Enter opens, F2 renames, Delete bins. **⋯ → Tree**
(or Ctrl+B) shows everything as a tree beside Home.

Write on the ruled page; edits save automatically. Ctrl+N makes a note, Ctrl+F
focuses search, Ctrl+S saves immediately, and Ctrl+Z/Ctrl+Y undo/redo text edits.
The menu opens versions, manages attachments, archives or bins, restores, and
exports or adds backups. Import adds copies, leaving current notes in place. ZIP
backups carry local attachments and use Android's existing format. **Send
files…** in the menu, or files dropped from Explorer on the toolbar, send files
straight to a paired device in no note; files dropped on a note are kept with
it, on a collection with the collection, on Home on Home. Ctrl+V on a note keeps
a picture with it, as a messaging app does - a screenshot from Greenshot or the
Snipping Tool, a picture copied in a browser - and files copied in Explorer too;
words pasted stay words. **⋯ → Sent files**
lists what went and what is still coming (see `docs/SHARING.md`, *Sending
files*).

Select a note or collection and choose **Share**. Show the code to the
phone's Mininotes scanner, then approve the phone's request on this PC. To accept
a phone's offer, use **From another device**: start a webcam, open a QR image,
or paste an image or sharing link from the clipboard. Camera access starts only
when you press Start camera; frames are not recorded. Windows camera permissions
must allow desktop apps. A pasted link or a saved image is accepted as a scan is,
and says so; afterwards People shows six digits for each device, the same on
both if nothing changed the code on the way, as on the phone.

**Profile**, also in the top bar, contains the editable device name, personal
pairing QR, copyable live Maxima address, address-only QR, permanent address,
relay connection status, Reconnect, People, the lock and backup actions.
**Connect my other device** offers your whole pad. Per-item sync timing is set on collections
or notes.
It refreshes the address and QR as the node changes relays. An offline session
still lets you change your name and manage local data.

**Share → Add someone** chooses an already-paired person or opens the scanner.
Choose Can read, Can write or Admin. People also shows addresses and whether a
device is yours. Share a collection to include future collections and notes. Roles,
pause, sync delay and manual sync use the existing protocol. Windows does not
yet register `mininotes://` as a system protocol handler; paste links in the app.

## Boundaries

- What was checked, on which build, is recorded before each release in a log the
  maintainer keeps privately. Matching formats alone do not prove live delivery.
- Notes are unencrypted on disk until the notebook is locked (the lock in the top
  bar): then the notebook and its files are encrypted, and open with the password,
  Windows Hello or the twelve recovery words. Device signing/agreement keys and
  preferences (including the node seed) are protected with current-user Windows
  DPAPI. They cannot be restored by copying those files to another Windows account.
- Files kept with a note or a collection are included in backups, and go with it
  to whoever it is shared with, up to 16 MB each (see `docs/SHARING.md`). This
  build has no dark paper, and differs from the phone where `docs/PARITY.md` says.
- Saved notes retain portable limits: title 160 and body 24,000 Java string
  characters (UTF-16 code units). Transport limits count UTF-8 bytes separately.
  Oversized stored text is displayed in full. Invalid local writing is kept as
  desktop recovery writing before navigation continues.
  A save failure leaves the writing on screen and blocks navigation/exit; copy
  that text somewhere safe if a storage problem cannot be corrected.

Home uses whole row boundaries and locks empty-space panning to its dominant axis.
Precise wheel deltas accumulate directly; a notched wheel has restrained smoothing,
Shift-wheel moves sideways, and Ctrl-wheel opens All pages. The entire centre foot
is absent on other pages and in All pages. Double-click empty space or Ctrl+Home
returns smoothly to the centre. Resize keeps the fractional Home position and the
note's selection and reading anchor; active writing keeps its caret visible.
Short note windows offer Note tools and retain save/error and recording access.
The minimum window remains 820×520. Physical touchpad two-axis delivery, touch hold
and on-screen-keyboard overlap have not been verified on hardware.

## Maintenance

`build.gradle` generates shared sources into `build/generated`: pure Java classes
are copied verbatim; `NoteStore`, `Post` and `Node` receive platform-name
substitutions. The small API in `desktop/platform` implements only the operations
these classes need. Compilation fails when new platform APIs require attention.
Do not edit generated files or fork the wire formats. JDBC transactions hold a
reentrant connection lock through their entire lifetime; cursors are detached
so a query inside a cursor loop is safe. Failed and nested transactions roll back.

Keep dependency versions pinned and preserve the bundled license notices.
Packaging uses JDK `jpackage --type app-image` and ships its runtime/legal tree.
Open an issue before changing protocol, storage or release behavior (see ../CONTRIBUTING.md).
