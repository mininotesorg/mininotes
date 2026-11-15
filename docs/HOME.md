# Home: notes and collections laid out like a phone's desktop

Status: design, agreed 2026-09-30. Step 0 done 2026-09-30 (0.1 frozen, backed up, going back tested). Step 1
(0.2.001) built, installed on all three devices, seen on Graphene; step 2 (0.2.002) built, not yet on a
phone; step 3 (0.2.003) built; step 4 (0.2.004) built; step 5 (0.2.005) built (see each step); decision 41, the archive
and the bin on Home, built in 0.2.006 (see *After step 5*). It is the whole of the 0.2 line; 0.1 ends at 0.1.041, kept
whole and installable.

## Where it stands

Mininotes has three fixed levels: a collection holds books, a book holds notes, and only a note holds
files. Every screen, every sharing rule and every message names one of the three. That ladder is the only
thing left that treats the levels differently: the four roles, the marks, the writing colours, drag and
drop and the menus all already act on "a thing", whatever it is called.

The owner's ask (2026-09-30): make the app's home the same as an Android phone's desktop. Icons on a grid;
a plain **+**; drag a note onto a note to make a collection; drag a collection into a collection; a
collection that can hold files and needs no note; favourites in a dock at the bottom; search at the
bottom; open things flicked through like open apps, with *Close all*; every note with an icon of its own,
or a picture; the PC the same screen, with the tree as an option. The Drop box goes: every collection is
one.

## The goal

Two kinds of thing, **note** and **collection**, nested as deep as anybody likes. Files at any level. Home
is itself a collection. Sharing and roles unchanged in meaning: share any thing, everything under it goes
with it. Both apps show the same Home.

## The design

### Things

- A **note** is what it is today: a title, ruled paper, files, versions, who wrote what.
- A **collection** holds notes, collections and files, in an order the owner sets. It needs none of them.
  It has a name, a tint, an icon or a picture; with none of its own it shows a mini-grid of what is inside,
  as the phone's tiles already do.
- **Home** is the collection everything is in. It is not shown as a thing; it is the screen.
- **Favourites** is a flag on a thing plus a place in the dock.
- The words: *note*, *collection*, *Home*. "Book" and "Drop box" leave the wording; "Inbox", "Archive" and
  "Bin" stay as places, reached from the ⋮ menu as today.

### The Home screen (both apps)

- A grid of icons that scrolls down (decision 1: no side-swiped pages). Order is the owner's, by drag.
- An unlabelled **+** at the bottom right. It offers *Note* and *Collection*. A new note opens; a new
  collection opens into its name.
- Tap a collection: a pop-up over the grid with its contents (its own grid, its files among them, its own
  **+**). Tap a note: it opens. Back, or tapping outside, closes the pop-up.
- Drag a note onto a note: a new collection holding both, named at once ("Untitled" until typed over).
  Drag anything onto a collection: it goes in. Drag anything out of a pop-up onto the grid behind: it comes
  up a level. Long press or right-click: the thing's menu, as today.
- The **dock**: the favourites, at the bottom, 5 on the phone, as many as fit on the PC (decision 3). More
  favourites than that live in a *Favourites* collection that is always first on Home and cannot be
  dragged, deleted or renamed. The star in a thing's menu puts it in the dock when there is room, else in
  Favourites.
- The **search bar** sits above the dock. It finds notes, collections and files by name and words.
- The **overview**: swipe up from the dock (phone), or the button beside the search bar / Ctrl+Tab (PC).
  Cards of the open notes and collections, flicked through sideways, newest first. Swipe a card away to
  close it; **Close all** closes them all. It replaces the PC's tab bar (decision 2: on the PC a note fills
  the window; the tree is an optional panel beside it, off by default, remembered).
- Marks, badges and people rounds are where they are today: the mark on a tile's corner, the rounds under
  a note's title, the count on a collection with new arrivals.

### Icons and pictures

- The set is **Lucide** (ISC licence, about 1,500 icons, single stroke, 24-unit grid; decision 4). It
  travels as path data in a generated Java class, drawn with `android.graphics.Path` and
  `java.awt.geom.Path2D`, so neither app gains a library. Its licence goes into NOTICE and About.
- An icon is named by its Lucide name and drawn in the thing's tint on a paper-coloured round square; the
  default for a note is `sticky-note`, for a collection the mini-grid.
- A **picture**: the owner picks an image; it is cut square and kept as a thumbnail of at most 32 KB
  (WebP where the platform has it, else PNG, 192 px). It travels with the thing, as its name does.
- The icon picker: a searchable grid of the set with the tints along the top and *Choose a picture…* at
  the end. The same picker on both apps.

### Files at every level

- A file can be kept with any thing. It shows as an icon among the thing's contents, with its kind's
  glyph and name; tap opens it, its menu is the file menu of today (Open, Save a copy, Send to a device,
  Put in a note → now *Move to…*, Delete).
- Files travel with the thing's sharing, as a note's files do today, with the same 16 MB rule.
- **Received files** (what the Drop box held) land on Home with a *new* badge (decision 6). The sending
  side is the file menu's *Send to a device*, unchanged. The Drop box's offer/accept messages (receipts
  15–17) stay as they are; only where the files land changes.

### Sharing

- Share any note or collection. The audience of a thing is the union of the rules on it and on every
  collection above it up to Home. Home itself is shareable as "everything on this device", which is what
  the library rule already means.
- The four roles (docs/SHARING.md) apply at the shared thing, unchanged.
- Someone you share a collection with sees it on their Home, with everything under it, and it fills in as
  things arrive.

### The PC

The same screen. Home fills the window; a note opens over it, filling the window, with the overview to get
back; a collection opens as a pop-up. The tree is *View → Tree*, a panel on the left, off by default. The
toolbar keeps *Profile*, *Share* and the ⋯ menu; *New note* goes, since **+** does it. Right-click,
keyboard reach (Tab between icons, Enter opens, F2 renames, Delete bins) and Explorer drag-and-drop work
on every icon, tile and pop-up, using the handlers built in 0.1.039–0.1.041.

## What changes underneath

### The notebook (schema step 30, additive)

- New table `things(id, parent, kind, name, icon, image, tint, rung, ordinal, favourite, dock, made,
  updated, gone)`. Every note and every collection has a row; a note's `id` is its `notes.id`, and its
  writing stays in `notes`. `parent` is a thing id or `HOME`.
- `held.note` keeps its name and now holds any thing id. `shares.target` likewise; `shares.scope` keeps
  its old values for rows that came from 0.1 devices and gains `THING` for new ones.
- Migration from step 29: each collection → a thing under `HOME`; each book → a thing under its
  collection; each note → a thing under its book; favourites → `favourite=1` in their order, the first five
  into the dock; each Drop box file → a `held` row on `HOME`, marked new; tints and rungs carried over;
  binned and archived things keep their flags. `collections` and `books` are left in place and no longer
  read. Nothing is deleted.
- The migration runs, in a test, against copies of all three real notebooks before it runs on any device
  (see *Step 1*): counts of things, files, shares and favourites before and after must agree.

### Messages

- **Parcel** (`MNB1`): a note's `collection/collectionName/book/bookName` stay filled for older readers
  (the top two ancestors, or the nearest ones). A new optional tail `path` carries the full ancestry:
  for each ancestor from Home down, `id, name, icon, tint, ordinal`, and for the thing itself `icon,
  image`. Older builds skip a tail they do not know, as they skip `history` today.
- **Collections travel on their own.** Today only a note is sent; a book or collection exists on the far
  side only through the notes in it. A collection with files and no notes must travel by itself: a new
  message `MNC1` (a thing's row plus its `path`, sealed and receipted like a note, revisioned by the
  same clock). Files on a collection are listed and fetched exactly as a note's are, with the collection
  id where the note id goes.
- **Receipts**: `TREE=22` (this build knows about trees; said in the hello exchange as `PERSONS` is).
  A device that never says it is a 0.1 device.
- **0.1 devices** (decision 5): a 0.2 device takes in everything a 0.1 device sends. It sends a 0.1
  device only what fits in three levels: a thing at depth 1 goes as a collection, depth 2 as a book,
  a note under them as a note. Anything deeper, and any collection's own files, wait with the words
  "*Name* needs to update Mininotes to receive this" in the mark's box (a new `Unsent` line), and go the
  moment the device says `TREE`.

### The apps

- **Android.** `MainActivity` keeps the note page and everything about writing, files, recording, marks
  and people. The level screens (`draw`, tiles, cards, list, the Drop box screen, the tree view) are
  replaced by new classes: `HomeScreen` (grid, +, dock, search), `Folder` (pop-up), `Overview`,
  `IconPicker`, `Dock`. The tree view stays as a sheet from the ⋮ menu.
- **Windows.** `DesktopShelves`, `DesktopDrops`, `DesktopTabs` give way to `DesktopHome`,
  `DesktopFolder`, `DesktopOverview`, `DesktopIconPicker`; `DesktopTree` becomes the optional panel.
  `Desktop` keeps the page, the status bar, the lock, the update check and the node.
- **Shared, Android-free, tested:** `Things` (the tree: ancestry, audience, depth, moves, the three-level
  view for old devices, dock rules), `Icons` (the generated set and the name lookup), `Thumb` (the
  square thumbnail's size rules), `Overview` (the open list and its order). They join `portable` in
  windows/build.gradle.

## The steps

Each step is a version on all three devices before the next starts; each has a verification entry, in a
log the maintainer keeps privately, saying what was seen and what was not.
Versions are 0.2.NNN with versionCode 2000+NNN, both apps together, as the 0.1 rule had it.

### Step 0 - freeze 0.1

1. Commit the tree as it stands, push to the private archive, tag `v0.1.041-final` there.
2. Keep `dist/latest/Mininotes-Android-0.1.041-debug.apk` and `Mininotes-Windows-0.1.041.zip` (with
   checksums) in `dist/archive/0.1.041-final/`.
3. Back up both phones' app files, file by file with sizes checked, and copy the PC's data folder
   (`%LOCALAPPDATA%\Mininotes`, with the app closed) to a backup folder.
4. Write down, in this file, how to go back: install the archived 0.1.041 and put the backed-up files
   back. Test that on the emulator with a copy of a phone backup before any 0.2 build is installed.

#### What was done, 2026-09-30

- The tree as it stood committed, and tagged `v0.1.041-final` on the private archive; its two builds kept in
  `dist/archive/0.1.041-final/` with their checksums, checked against the files.
- Every device's app files backed up first, file by file with every size checked. Two of the three notebooks
  are locked, encrypted with a key that never leaves the device, so only an unlocked phone's copy can be
  opened elsewhere. See *Step 1* for what that means for testing the migration.
- **Going back, tested** on an emulator kept offline and read-only: the archived 0.1.041 installed, a phone's
  backup put back file by file, and the notebook it opened counted the same as the backup in every table.

### Step 1 - the model (0.2.001)

The tree, the migration, the messages, and the old screens running on the new model so that nothing
visible changes yet except that books show as collections inside collections. Files at every level in
the model only. All of `Things`, the schema step, the parcel tail, `MNC1`, `TREE`, the three-level view
and the migration, with tests. The migration proven on copies of the three real notebooks (counts only,
no note text printed or kept). Installed and verified: every note, file, share, favourite and mark is
where it was; sync between the three still works; a 0.1 device (the emulator on the archived APK) still
exchanges notes with a 0.2 device.

#### How it is built (written 2026-09-30, before building)

- **The notebook, schema 30.** `things` holds every collection, at any depth: `id, parent, kind, name,
  icon, image, tint, ordinal, favourite, dock, archived, binned, theirs, origin, pause, revision, made,
  updated, gone`. Collections become rows under `HOME`, books rows under their collection, with their ids,
  names, colours, places, favourites, bin and archive flags, origins and waits. A note's place in the tree
  stays in its own row - `notes.book` is its parent (a collection's id, or `HOME`), `notes.place` its order,
  `notes.pinned` its favourite - and notes gain `icon`, `image` and `dock` (decision 8). The first five
  favourites on the shelves go into the dock in the order the Favourites place listed them. Files kept
  with a book say `collection` from now on. `collections` and `books` stay, unread and unwritten.
- **The migration checks itself.** Inside the upgrade's transaction it counts, before and after:
  containers (collections + books = things), notes, files, shares, favourites, and what is binned and
  archived; if any differ it throws, the transaction is rolled back, and the notebook is left at 29 for
  0.1.041 to open (decision 11). The same counts are what the test on the real notebooks prints.
- **`Things`** (Android-free, tested): the ancestry of a thing from Home down, its depth, whether a move
  would put a collection inside itself, the three-level view (a collection at depth 1 is a 0.1
  collection, at depth 2 a 0.1 book; a note fits only at depth 3), the parcel's two old shelf fields,
  the dock's first fill, and the envelope id of a thing that is not a note.
- **Sharing on paths.** A rule reaches a thing when it is the library rule or its target is the thing or
  any collection above it. `Sharing.audience`, `standing` and `moving` take the path; the three-argument
  forms stay for what still calls them. New rules on a collection are `THING`, on a note `PAGE`; a thing
  keeps whichever scope its rows already have, so one person is never on it twice (decision 12).
- **The parcel's `path` tail** (`MNP1`, after the history, which is written - empty if need be - so an
  older reader stops before it): for each collection above the note from Home down, `id, name, icon,
  tint, ordinal`, then the note's own `icon` and `image`. A reader that has it builds what is missing
  from the top down and moves nothing already here; one that has not uses the old two fields, which
  carry the nearest two collections (decision 15).
- **`MNC1`** (`Carton`): a collection on its own - its id, name, icon, tint, image, path, who has it and
  its files list - sealed and answered like a note, under the envelope id `Things` gives it, and sent
  only to devices that have said `TREE`. Step 1 sends one for a shared collection with no note under it.
- **`TREE` (receipt 22)**, said once a run to every paired device that has shown it answers, as `PERSONS`
  is, and written down by key when heard. A device that has not said it is a 0.1 device: it is sent only
  notes that fit three levels (every note does, after the migration), each with the old fields; anything
  else waits with *"Name needs to update Mininotes to receive this"* and goes when `TREE` is heard.
- **The old screens.** `BOOK` is no longer a kind the notebook hands out: every collection is a
  `COLLECTION`, and a collection's inside is its collections and then its notes. A collection at depth
  1 is drawn as a collection was (+ makes a collection in it), a deeper one as a book was (+ makes a
  note), and either shows the other kind too if it holds it (decision 13). "Book" becomes "collection"
  in every word the apps say. Moving: a note into any collection; a collection into Home or into any
  collection that is not inside it (decision 14). The tree is walked to any depth.

#### What was built, 2026-09-30 (0.2.001)

- **Model**: `Things`, schema step 30 with its self-check (`NoteStore.onUpgrade`), `Sharing` on paths and
  `Scope.THING`, `Outbox.Page.above`, the parcel's `MNP1` path tail, `Carton` (`MNC1`), `Receipt.TREE`,
  `Unsent.Why.NEEDS_UPDATE`, and every collections/books query in `NoteStore` moved to `things` (collections
  of any depth; `inside`, `wholeTree`, `places`, moving with `Things.mayGoInto`, bin/archive/restore/erase
  through the whole subtree, favourites and the dock's first fill, backups that carry `things` and still
  read a 0.1 backup). `Post`: the path on every parcel, the three-level view for devices that have not said
  `TREE` (`waitsForTrees`, `scopeSaid`), `TREE` said and heard, cartons sent for shared collections with no
  note under them and taken in, leave/remove said by depth.
- **Phone** (`MainActivity`): the trail follows real ancestry (`trailTo`), + makes a collection at the top
  and one down and a note deeper (`addsCollections`), moving and merging by drag on paths, a new note goes
  into the deepest writable collection on the trail, the tree indented to any depth, and no "book" left in
  what it says. **PC** (`Desktop*`): the same rules for + (`DesktopShelves.makesNote`), Move to… and
  dragging on `Things.mayGoInto`, the share box finding rules along the path (`Desktop.reaching`), marks to
  any depth, the line over a note's title naming every collection above it, and no "book" left.
- **Tests**: 621 on Android (lint 0 errors, the same 27 warnings), 755 on the PC; new `ThingsTest`,
  `ParcelPathTest`, `CartonTest`, `SharingPathTest`, `DesktopThingsTest`, `DesktopDepthTest`, a rewritten
  `DesktopMovingTest`. `MigrationProbe` (test sources) runs the step on a copy of a real notebook and
  prints counts only.
- **The migration on the real notebooks, before any device**: Graphene's copy (step 0's and a fresh one):
  every count agreed (11 collections and books, 9 notes, 15 files, 11 shares, 1 favourite, 2 binned), 4
  collections on Home and 7 one down, nothing orphaned, the favourite docked, the one file kept with a book
  now kept with that collection. The Pro's and the PC's current notebooks are locked and cannot be opened
  off their devices (decision 11); the Pro's last plain copy (28 September, schema 27) went through the same
  step with its check passing. A dry run of the PC build on a copy of the PC's folder waited at the copy's
  Windows Hello question, which nobody was there to answer, and was closed.
- **Going back from a migrated notebook**, on the emulator (Pixel 6 AVD, read-only, offline): Graphene's
  backup under 0.1.041, then 0.2.001 installed over it and opened (it logged the move with every count
  intact), then 0.1.041 installed back with `-d` and the backup's files put back: the notebook it opened
  was, count for count, the backup at schema 29.

### Step 2 - Home on the phone (0.2.002)

`HomeScreen`, `Folder`, `Dock`, search at the bottom, **+**, drag to merge and nest, the overview with
*Close all*, received files on Home, the Drop box screen gone. Icons: the default glyphs and tints only.
Verified on both phones with the owner's real layout migrated: the grid shows what the tiles showed, in
the same order.

#### How it is built (written 2026-09-30, before building)

- **Home** replaces the *All collections* screen; the note page, its menus and every box stay as they are.
  Top to bottom: the bar as today (name and version, the mark, ⋮); the grid - Home's collections and notes
  in the owner's order, then received files, each an icon with its name under it, four across (decision
  18); **+** at the bottom right; the search bar; the dock. A collection's icon is the mini-grid of what is
  inside it, a note's the paper tile with the note glyph, a file's its kind's glyph; each in its tint, the
  mark on the corner, a *new* badge on a received file.
- **+** offers *Note* and *Collection* where you are (Home or the open pop-up). A note opens at once; a
  collection is made as *Untitled* and its pop-up opens with the name ready to type over.
- **Folder** (the pop-up): a rounded card over the dimmed grid - the collection's name (tap to rename), its
  mark and its ⋮, then its grid (collections, notes, its own files) and its own **+**. A collection inside
  it opens in the same card with ‹ to go back up (decision 21); back goes up one level, a tap outside closes
  the card.
- **Dragging**: a long press opens the thing's menu as today; moving while still holding picks it up. On a
  note: a new collection *Untitled* holding both, name ready to type over. On a collection: into it. Between
  icons: a new place in the order. Out of a pop-up onto the dimmed grid: one level up. Onto the dock: a
  favourite.
- **Dock**: up to five favourites (`dock` 1-5), icons only, each named for anybody who cannot see it; the
  rest of the favourites in a *Favourites* collection that is first on Home whenever there are more than
  five, and cannot be dragged, renamed or deleted. An empty dock says *Favourites* quietly. A short handle
  above it says it can be swiped up (decision 19).
- **Overview**: swiping up from the dock (or the handle) shows the open notes and collections as cards,
  newest first, sideways; swipe a card up to close it; **Close all** is the one button. `Overview` (pure,
  shared with the PC) keeps the list - up to twelve, newest first, kept across restarts (decision 22).
- **Search** at the foot finds notes, collections and files (received ones and those kept with anything)
  by name and words; tapping a result opens it where it lives.
- **Received files on Home** (schema 31, additive): files gain `fresh`; every received file that is here
  gets a `files` row on Home, and its `transferred` row is marked `moved` rather than deleted; from now on a
  file that arrives lands the same way, *new* until opened. The Drop box screen and tile go; the offer box
  for files from somebody else's device stays; what this device sent to others is listed under ⋮ → *Sent
  files* (decision 20).
- Classes: `HomeScreen`, `Folder`, `Dock`, `OverviewScreen` (Android), `Overview` (pure, tested); the
  helpers they need from `MainActivity` are opened to the package, never copied.

#### What was built, 2026-09-30 (0.2.002)

- **Model** (shared): schema 31 - `files.fresh`, `transferred.moved`, every received file here given a
  `files` row on Home under its own id (bytes not copied), self-checked like step 30; arriving files land
  on Home in the same transaction; `contents(id)` (collections, notes, then files, in one order);
  `Branch.Kind.FILE` and `Branch.fresh`; the dock (`dock`, `toDock`, `outOfDock`, `favouritesBeyondDock`;
  pure `Things.intoDock/outOfDock`); `lookingEverywhere` (notes, collections and files); `addCollectionIn`,
  `moveInto` (any kind, files too), `order(lines)`, `merge`; `sentFiles`, `sendingOf`; backups carry Home's
  files. `Overview` (pure, shared). The PC's Drop box view lists the files on Home until step 3.
- **Phone**: `HomeScreen` (bar, grid four across, **+** with Note / Collection, search bar, received files
  with *new*), `Folder` (the card, ‹ up, tap outside closes, its own **+**, files dropped in from other
  apps), `Dock` (five, handle, swipe up, drop to dock, *Remove from the dock*), `OverviewScreen` (cards, swipe
  up to close, **Close all**), `Grid` (pure rules: columns for a width, what a drop does, dock slots, swipe
  up). The Drop box screen and the cards/list switch are gone; ⋮ → *Sent files* lists what went and what is
  still coming. Back from a note returns to where it was opened; back on Home leaves the app.
- **Tests**: 640 on Android (lint 0 errors, the same 27 warnings), 785 on the PC.
- **Not seen on a screen**: neither phone could be reached (asleep off the network from about 09:00), and
  the emulator could not keep its system running in the memory this PC had free. See the verification log.

### Step 3 - Home on the PC (0.2.003)

`DesktopHome`, `DesktopFolder`, `DesktopOverview`, the optional tree, keyboard reach, right-click and
Explorer drops on every icon. The tab bar and the Drop box view go. Gallery pictures of Home, a pop-up,
the overview and the tree panel.

#### How it is built (written 2026-09-30, before building)

- **Home** (`DesktopHome`) fills the window under the toolbar: the same `contents(HOME)` as the phone, as
  icons in as many columns as fit (the icon a rounded square with the thing's glyph, mini-grid or file kind,
  its tint, its mark on the corner, *new* on a received file, the name under it), **+** at the bottom right
  (Note / Collection, as on the phone), then the search field with the overview button beside it, then the
  dock (as many favourites as fit, decision 3), then the status bar (decision 25).
- **A note** opens filling the window, with **←** *Home* at the left of its bar (decision 24); Esc does the
  same. **A collection** opens as `DesktopFolder`, a rounded card over the dimmed Home, as on the phone: its
  name (F2 or a click renames), mark, ⋯, grid and **+**; a collection inside it opens in the same card with ‹.
- **The overview** (`DesktopOverview`) replaces the tab bar: the overview button or Ctrl+Tab shows the open
  notes and collections as cards, newest first (`Overview`, shared); click to go, × or middle-click to close,
  **Close all**; Ctrl+Tab again moves along the cards, letting go of Ctrl opens the one chosen.
- **The tree** is an optional panel on the left: ⋯ → *Tree* (ticked while shown) and the same switch in
  Settings → Window, off by default, remembered (decision 26); selecting in it opens what is selected.
- **Reach**: Tab and the arrows move between icons, Enter opens, F2 renames, Delete bins, the Menu key or
  Shift+F10 opens the menu; Ctrl+N a note, Ctrl+F the search. Right-click on every icon, card and dock icon
  gives the thing's menu, in the phone's order. Dragging an icon does what it does on the phone (`Grid`);
  files from Explorer dropped on a note are kept with it, on a collection or its card with the collection,
  on Home's grid on Home.
- **Gone**: the tab bar (`DesktopTabs`), the Drop box view (received files are on Home; ⋯ → *Sent files*
  lists what went and what is coming, as on the phone), the *New note* button (**+** does it).
- Gallery pictures: Home, a pop-up, a pop-up inside a pop-up, the overview, the tree panel, the dock with
  more favourites than fit, a received file, a note with ← Home.

#### What was built, 2026-09-30 (0.2.003)

- **PC**: `DesktopHome` (the grid in as many columns as fit, icons with marks and *new*, **+**, the search
  field with the overview button, the dock, keyboard reach, right-click menus in the phone's order,
  dragging on `Grid`'s rules, Explorer drops kept where they land), `DesktopFolder` (the card, ‹ up, rename
  in place, its own **+**; Home's **+** hidden while a card is open), `DesktopOverview` (cards, × and
  middle-click, **Close all**, Ctrl+Tab along the cards; 0.1's tabs read once), the tree as an optional
  panel (⋯ → *Tree*, Ctrl+B, Settings → Window). `DesktopShelves` and `DesktopTabs` are gone, and with them
  the Drop box view and the New note button; ⋯ → *Sent files* as on the phone. A note has **←** *Home* and
  Esc; Ctrl+W closes back to Home.
- **Phone**: Home's **+** is hidden while a card is open, as on the PC (it drew above the card's dimming).
- **Found and fixed on the way**: showing the tree emptied Home's grid until something else repainted it
  (the dock's resize refilled the grid and left placing it to a layout pass that had not run);
  `DesktopHomeTreeTest` catches it.
- **Tests**: 792 on the PC, 640 on the phone (lint unchanged). Gallery pictures of Home, the card, a card in
  a card, the overview, the tree panel, the dock with more favourites than fit, received files, a note with
  ← Home and the menus, looked at by the agent that built them and by me (Home, the card in a card, the
  overview, the tree panel, the note).

### Step 4 - icons, pictures and files everywhere (0.2.004)

The Lucide set generated into `Icons`, the picker on both apps, pictures as thumbnails that travel,
files on collections travelling and fetched, the "needs to update" line. Verified with a picture set on
one phone appearing on the other two, and a file kept on a collection arriving on the PC.

#### How it is built (written 2026-09-30, before building)

- **The set**: `Icons` (pure, shared) reads Lucide 1.49.0 from `icons.txt` beside the class - one line an
  icon: its name, the words it is found by, and every shape as SVG path data on the 24-unit grid - and draws
  it into a `Pen` (moves, lines, cubics, closes; arcs cut into quarter turns) that each app replays onto its
  own path type, stroked 2 units wide with round ends. `tools/icons/make_icons.py` makes `icons.txt` from
  the npm package (decision 29). Lucide's licence goes into NOTICE and About.
- **A picture**: `Thumb` (pure) - the square from the middle, the tries (192 px down to 64, falling
  quality) until one is at most 32 KB, and the check that what arrives is a WebP, PNG or JPEG that fits. The
  phone encodes WebP, the PC PNG. Kept as Base64 in the thing's own row (decision 30).
- **Drawing**: a note's icon is its glyph (the note glyph until chosen) in its tint on a paper round square;
  a collection shows its icon if it has one, else the mini-grid; a picture fills the round square. Home, a
  card, the dock, the overview, the tree and search results all draw them, on both apps.
- **The picker** (both apps, the same): the thing's menu gains *Icon…* right after *Colour* (decision 33); a
  box with the tints along the top, a search field, *Default* first, then the set in Lucide's order (found by
  name and words), and *Choose a picture…* at the end (decision 32).
- **Travelling**: a note's icon and picture ride in its parcel's path tail and are taken with the note; a
  collection's icon rides in every path through it and is followed like its name; a collection's picture,
  icon and files ride in its carton, which now goes for every shared collection whose revision moved, not
  only one with no note in it (decision 31). Files kept with a collection are listed, published and fetched
  as a note's are, with the collection's id where the note's goes.
- **Needs to update**: what a 0.1 device cannot take (a note that does not fit three levels, a carton, a
  collection's files) is counted as waiting for it, and the mark's box says *"Name needs to update
  Mininotes to receive this"* on both apps until it says `TREE`.

#### What was built, 2026-09-30 (0.2.004)

- **Shared**: `Icons` and `icons.txt` (Lucide 1.49.0, 1,857 icons; `tools/icons/make_icons.py`), `Thumb`, and
  `Looks` (which face a thing shows, the ink, the picker's search and *Default*, the encode loop); the store's
  `iconOf/imageOf/setIcon/setImage` and `dress`, a note's look in its parcel, a collection's in its path and
  carton, collection files published, listed, fetched and answered like a note's, and "needs to update"
  counted for 0.1 devices (decisions 29-38). NOTICE and THIRD-PARTY.txt carry Lucide's licence (ISC, with the
  Feather MIT notice) and, on the PC, TwelveMonkeys' (BSD-3).
- **Phone**: `IconPen`, `IconFace`, `IconPicker`: faces on Home, cards, the dock, the overview, search, the
  tree and the note's bar; *Icon…* after Colour and *Remove the picture*; pictures from the phone's picker,
  cut square, EXIF-turned, WebP until it fits 32 KB, with the busy strip saying so; About names Lucide.
- **PC**: `DesktopIcons`, `DesktopIconPicker`: the same faces everywhere, the same picker (tints, search,
  *Default*, the set painted row by row, *Choose a picture…* or paste or drop), PNG pictures, WebP read.
- **Tests**: 666 on the phone (lint unchanged), 848 on the PC; gallery pictures of every face and the picker
  looked at by the agent that built them and by me (Home with icons and pictures, the picker searching).

### Step 5 - polish (0.2.005)

Whatever the four verification rounds turned up; docs/PARITY.md, docs/SHARING.md, README.md and the
website's words updated from "collections, books and notes" to "notes and collections"; the public
release only when the owner says so.

#### How it is built (written 2026-09-30)

- **Icons where you put them** (decision 39): `Layout` (pure, shared: `arrange`, `moveTo`, `under`), schema 32,
  `NoteStore.place(lines,cells)`, `Branch.cell` on every line `contents()` gives; the phone's Home and card and
  the PC's Home and card draw each icon in its cell with the empty cells between, show the empty cell under a
  carried icon as the place it will go, and write the cells with `place` when it is let go there.
- **From the step 4 rounds and the docs audit**: the phone's in-app scanner reachable from ⋮ (decision 40);
  *Connect my other device* reachable on the phone as on the PC; files dropped on the phone from another app
  aimed at the icon under the finger, as the PC does; the PC's *Remove from the dock* for every docked icon it
  shows, not only the first five; the icon set read off the UI thread at the phone's start.
- **Words**: SHARING.md, PARITY.md (a fresh audit, 0.2.004), README.md, android/README.md, windows/README.md;
  the website's one line about books, in Documents/mininotes-site, left uncommitted for the owner.

#### What was built, 2026-09-30 (0.2.005)

- **Icons stay where you put them** (decision 39, the owner's ask made during this step): `Layout` (shared, 7
  tests), schema 32 `cell` on notes, collections and files, `NoteStore.place`. Phone (`HomeScreen.lay`, `Laid`,
  `across`, `put`, `Folder`) and PC (`DesktopHome` `Icons.drawn`, `Carry.across`, `keepCells`, `nudge`): each
  icon in its cell with the gaps kept, a quiet outline where a carried icon will land, the empty row below the
  last icon; on the PC Ctrl+arrow moves the focused icon a cell, with Undo; the Favourites icon keeps a cell of
  its own too (in each device's settings). `Grid.letGo` decides what a drop does, for both apps.
- **From the rounds and the audit**: ⋮ → *Scan a code…* on the phone (decision 40); the phone's Profile gains
  *Connect my other device…*, *Scan a code…* and *People and devices*; files dropped on the phone from another
  app go to the note or collection under the finger, or into the empty cell; the PC's *Remove from the dock* on
  every icon its dock draws; the icon set read off the UI thread when the phone starts.
- **Words**: SHARING.md, PARITY.md (audit at 0.2.004, then the dock and placing rows), README.md,
  android/README.md, windows/README.md; the website's line about books changed in Documents/mininotes-site and
  left uncommitted for the owner.
- **Tests**: 677 on the phone (lint 0 errors, the same 27 warnings), 866 on the PC; pictures of a Home with a
  gap, a carried icon over its landing outline, a card with a gap, a carry out of a card onto Home, the dock with
  two taken out - looked at by the agent that built them and by me (the landing outline and the placed icon).
- **Not done, found on the way** (for a later round): leaving or being removed from a collection two or more
  deep tells nobody yet; the phone's tree view shows every mark as the empty ring; the PC's tree still reorders
  by `order`; Explorer drops on the PC's Home land in the first free cell, not the cell under the pointer.

### After step 5 - the archive and the bin on Home (0.2.006)

Decision 41, the owner's ask during step 5, built on 2026-10-01 at his go-ahead.

- **Both apps**: Home shows an *Archive* and a *Bin* icon (Lucide `archive` and `trash` on the outlined square the
  Favourites icon has), each with how many things wait in it on its corner - quiet, and nothing when empty. A tap or
  click opens a card of what is in it, listed one after another, with no **+**; a thing there offers *Put back* and, in
  the bin, *Delete for good*, in the archive *Move to the bin*; the bin's ⋮ or ⋯ has *Empty the bin*. A note or a
  collection carried onto the Archive is archived, onto the Bin it goes in the bin, by the same road as its menu (said,
  with Undo); a file onto the Bin gets its own *Delete* question. Settings → *Show the archive and the bin on Home*, on
  unless switched off; off, ⋮ → *Open the archive* / *Open the bin* on the phone and ⋯ → *Put away* on the PC come back,
  and while on they are not in those menus, so there is one way to each.
- **Shared rules** (`Grid`, `Layout`, tested on both): `Grid.place`, `Grid.carried`, `Onto.AWAY`, `LetGo.AWAY`;
  `Layout.home` arranges Home with its places. A place's cell is kept in each device's own settings
  (`favouritesCell`, `archiveCell`, `binCell`). A place with no cell yet goes **after the last icon**, never into a gap:
  the first build put them in the first free cells from the top, and on Graphene's real Home, whose first row was left
  empty on purpose, that put the archive and the bin at the top left - seen, fixed, and tested
  (`onAHomeWithGapsTheArchiveAndTheBinGoAfterTheLastIconNotIntoAGap`).
- **Decision 42**: the three places are carried to another empty cell of Home like every icon, and into nothing.
- **Also**: the archive's menu loses *Nothing to put away*, a row that only said there was nothing to do; a place's menu
  has no heading over nothing; *Put back* says "Put back" rather than "Back on the shelves".
- **Not done**: carrying a thing out of a collection's card onto the Archive or the Bin behind the dim (only things on
  Home's own grid can be let go on them); files dropped from another program onto the Bin are kept on Home, as onto the
  Favourites icon.

### After step 5 - one menu for a press and a right-click, and Home in pages (0.2.008)

Decisions 43-50, the owner's asks of 2026-10-01, built the same day.

- **Menus** (43, 44): a long press on the phone and a right-click on the PC give the thing's own menu, everything about
  it and nothing else; ⋮ and ⋯ give that and then the app's rows. Home's empty room, held or right-clicked: New note, New
  collection, the text size, Home's colour and its strength (new on the PC, where Home was plain paper), Undo, Sync now,
  Show favourites, Show search, All pages, Back to the main page. A card's empty room: New note, New collection, then the
  collection's menu. The PC's + lost its shadow, which its own square cut off (the owner's ask).
- **Pages** (45-50): `Layout.pages` (tested: a long Home becomes a column of pages, a page exists only where something
  stands, a smaller window moves icons on their own page and writes nothing, something new goes on the main page and the
  bin after its last icon, a move pins every icon on every page), schema 33 `page` beside `cell`, the places' pages in
  each device's settings (`favouritesPage`, `archivePage`, `binPage`). PC: `DesktopHome` turns by the wheel, Shift and
  the wheel, a drag of the empty room, Page Up / Page Down (Ctrl for sideways), a double-click or Ctrl+Home home; an icon
  held at an edge for two thirds of a second goes on to the page beyond, one page for each hold; dots at the foot;
  `DesktopPages` is every page zoomed out (Ctrl with the wheel, or the menu). Phone: `HomeScreen` takes a quick swipe
  as a turn - a finger held first is still the icon's - a double tap or back home, a pinch to every page (`HomePages`),
  the same edge hold, the same dots. On both, the favourites and the search show on the main page only, their room kept
  on the others so every page is one size, and what + makes on another page stands on that page.

### After step 5 - pages that slide, and pages carried (0.2.009)

Decisions 51-53, the owner's asks of 2026-10-01 after trying 0.2.008 on the Graphene ("not smooth ... a long lag between
the action and the effect"), built the same day.

- **Phone** (51): `HomePages` is gone. Every page is drawn in one world (`HomeScreen`: `pager`, `world`, `here`); a swipe
  moves it under the finger and a let-go finishes the slide, a pinch scales it live about the point between the fingers,
  and zoomed out each page is whole in its frame, the one in view outlined in the accent. Let go, a pinch settles on the
  page, on one page with the next ones beside it (0.6), or on every page at once (`allAt`: as big as fits, half a page's
  room round them for the places a carried page can go); *All pages* is the last. A tap goes into a page, back comes
  back. Nothing round the world clips it (`pager` and `world` both `setClipChildren(false)`): with the pager clipping, the
  world was cut to its own bounds and nothing beside the page in view was ever drawn - the lag the owner saw.
- **Pages carried** (52): `Layout.movePage` (tested: onto an empty place it moves, onto another page the two change
  places, the main page's place stays the main page). Phone: held still on a page zoomed out, it lifts and goes with the
  finger, the empty places round the pages dashed. PC: `DesktopPages` drags a page the same way; Esc cancels; Undo puts
  every icon back.
- **Dots** (53): `Layout.dotted` - the pages there are and the one in view, even empty, so a thing carried onto a new page
  shows that page at once. On the phone the filled dot slides with the pages.

### After step 5 - the pinch reaches Home, a carry turns the page up and down, the version's dot (0.2.010)

Decisions 54-55 and a fault, the owner's word on 0.2.009 the same night.

- **The pinch did nothing on the phone** ("pinching with 2 fingers doesn't work"): `MainActivity.dispatchTouchEvent`, which
  every touch passes first, took every two-finger gesture for the whole pad's zoom - which only ever makes things bigger -
  and passed nothing on. A gesture whose first finger lands on Home's pages, nothing open over them and the pad at its
  own size, is Home's now (`HomeScreen.pinchesAt`); anywhere else the pad's zoom is as it was.
- **Up and down at once** (54): `HomeScreen.edgeWatch`, `DesktopHome.Carry.atTheEdge`.
- **The dot** (55): `Update.standing` (tested), the phone's version line, the PC's version in the bar.

### After step 5 - From another device on every + and every menu (0.2.011)

Decision 56, the owner's word on the published 0.2.010. One menu builds every + (`HomeScreen.plus`, `DesktopHome.plusMenu`);
the rooms' menus (`MainActivity.menuFor`, `Desktop.roomMenu`), the app's rows (`MainActivity.appRows`, where *Scan a
code…* is now *From another device…*), and the PC page's right-click (`Desktop.paperMenu`) offer it too. The accepting
box says the thing will appear on Home (it still said "on your shelves" on the phone, and nothing on the PC); a few
other words left over from the shelves went with it. Decision 57, found the same night: a pasted code is accepted as a
scanned one is (`MainActivity.pairOrAccept`, `Desktop.receiveCode`), with the six digits under People and devices on
both apps; the share box opens at the top and says when they accept (`codeSays`).

### After step 5: the grocery list, out of a collection by hand, writing lines (0.2.013)

The owner's asks of 2026-10-03, first round (decisions 60 to 64). The rest of that day's asks follow in later rounds:
a note's icon for me or for everybody, things placed anywhere while they stay shared, people's colours, a share accepted
and waiting shown on Home, and Temp, Recent and a Tools collection beside the archive and the bin.

### After step 5: in and out at any depth, put anywhere, an icon for me (0.2.014)

The owner's asks of 2026-10-03, second round (decisions 65 to 67). Built and tried on the Graphene only, at his word; the
Pro and the laptop are updated once every round is done.

### After step 5: everybody's own colour (0.2.015)

The owner's asks of 2026-10-03, third round (decisions 68 and 69). Graphene only.

### After step 5: Tools, Temp, Recent, and what is on its way (0.2.016)

The owner's asks of 2026-10-03, fourth round (decisions 70 to 73). Graphene only; the Pro and the laptop are updated after.

### After step 5: places wear their icon, and every favourite in one order (0.2.017)

The owner's asks of 2026-10-03, fifth round (decisions 74 and 75). Graphene only. Next: text formatting (0.2.018), the
PC's list of open things (0.2.019); the roadmap to F-Droid is a draft for the owner to review.

### After step 5: bold, italic and underline (0.2.018)

The owner's asks of 2026-10-03, sixth round (decision 76). Graphene only.

### After step 5: what is open, listed on the PC (0.2.019)

The owner's asks of 2026-10-03, seventh round (decision 77). PC only; the phone's version moves with it.

### After step 5: each place on Home with its own switch, Tools gone, Temp's time (0.2.020)

The owner's asks of 2026-10-03, eighth round (decisions 78 and 79). Graphene only.

## Decisions

1. Home scrolls down; no side-swiped pages. (2026-09-30)
2. On the PC a note fills the window; the tree is an optional panel; the tab bar becomes the overview.
3. Dock: 5 on the phone, as many as fit on the PC; the rest in a *Favourites* collection.
4. Icons: Lucide, as path data, drawn by both apps; pictures as ≤32 KB square thumbnails that travel.
5. 0.1 devices keep receiving what fits in three levels and are told to update for the rest; no flattening.
6. Received files land on Home with a badge; there is no Received collection.
7. Books become collections inside their collection; nothing moves and nothing is deleted.
8. A note's place in the tree stays in its `notes` row (`book` is its parent, `place` its order, `pinned`
   its favourite); `things` holds the collections. So nothing about a note is written in two places, and
   every query that reads a note goes on reading it where it is. (2026-09-30, refines *The notebook*.)
9. The file rows are `files.note` / `files.held` (`held` is the PC's mailbox table, not the files'); they
   hold any thing's id, and rows that said `book` say `collection`. (2026-09-30)
10. The Drop box's files move to Home in step 2, when the Drop box screen goes, so no file is shown in two
    places while both exist. (2026-09-30)
11. The migration counts before and after inside its own transaction and rolls back on any difference.
    The Pro's and the PC's notebooks are locked with keys that never leave them, so they cannot be opened
    off the device; for them this check, on the device, is the test on the real notebook, after the same
    migration passed offline on Graphene's copy and on synthetic locked-shape notebooks. (2026-09-30)
12. New sharing rules: `THING` on a collection, `PAGE` on a note; a thing keeps the scope its rows already
    have. (2026-09-30)
13. In step 1 a collection at depth 1 looks and adds as a collection did, a deeper one as a book did; both
    show whatever they hold. (2026-09-30)
14. In step 1 a note moves into any collection, a collection into Home or any collection not inside it.
    (2026-09-30)
15. The parcel's old `collection` and `book` fields carry the nearest two collections above the note.
    (2026-09-30)
16. A collection's envelope id: its UUID's bytes, or the first 16 bytes of SHA-256 of any other id.
    (2026-09-30)
17. A collection's tint and order travel in the path and are used only when it is first made on the far
    side; its name is followed, as now. (2026-09-30)
18. Home is four icons across on a phone held upright, as a phone's own home screen is. (2026-09-30)
19. The dock has no labels, as a phone's does; each icon says its name to a screen reader, and a long press
    gives its menu. A short handle above the dock shows it can be swiped up. (2026-09-30)
20. With the Drop box gone, what this device sent is listed under ⋮ → *Sent files*, and a sending still
    speaks through the busy strip while it goes. (2026-09-30)
21. A collection opened inside a pop-up replaces the pop-up's contents, with ‹ to go back up; phones do not
    nest folders, and a stack of cards would hide the grid it belongs to. (2026-09-30)
22. The overview holds up to twelve open things, newest first, and keeps them across restarts. (2026-09-30)
23. Home and every pop-up are icon grids, as a phone's desktop is; the *cards or a list* switch leaves
    Settings, and ⋮ → *Tree view* is the list of everything. A collection's files are added from its ⋮
    (*Add a file…*) or by dropping files from another app on its pop-up, since the level screen's clip goes
    with the level screen. (2026-09-30)
24. On the PC a note has **←** *Home* at the left of its bar, as the phone's note page has its back arrow, and
    Esc does the same. (2026-09-30)
25. The PC's status bar stays at the very bottom, under the dock, where a window's status bar is.
    (2026-09-30; was *Not decided*.)
26. The PC's tree is ⋯ → *Tree*, ticked while it shows, and the same switch in Settings → Window; off by
    default and remembered. (2026-09-30)
27. Home is not offered in the Share box: sharing everything stays *Connect my other device* in Profile, as
    today; the Share box is for a note or a collection. (2026-09-30; was *Not decided*.)
28. The phone's pinch and a note's own text size are left as they are in 0.1.037 - a look closer that does
    not change the note's size. (2026-09-30; was *Not decided*.)
29. The set travels as a text resource beside `Icons` (`icons.txt`, 420 KB, 126 KB compressed), not as Java
    source: 1,857 icons of path data do not fit a class file's limits, and a resource is the same data, drawn
    the same way by both apps with no library. (2026-09-30, refines decision 4.)
30. A picture is kept as Base64 in its thing's own row, so it goes with the row everywhere the row goes -
    backups included. (2026-09-30)
31. A carton goes for a shared collection with no note in it (as in step 1), and for one with notes once it has
    something of its own to send - its revision moved (name, colour, icon, picture or files), or it keeps files
    or a picture - so its look and files reach everybody who has it without a carton, and a "needs to update"
    line, for every collection to every 0.1 device. (2026-09-30)
32. The picker: tints along the top, a search field, *Default* first, then the set, then *Choose a
    picture…*; one box, the same on both apps. (2026-09-30)
33. *Icon…* sits in a thing's menu right after *Colour*, on both apps. (2026-09-30)
34. Choosing an icon, or *Default*, takes a picture off; a picture keeps the icon under it for when it is
    removed. (2026-09-30)
35. A note's icon and picture that arrive are taken when the arriving revision is newer than the one here; at
    the same revision the one that sorts later wins, so two devices that changed it at once agree. A copy this
    device only reads takes them whenever the words are taken. Changing a note's look bumps its revision with
    the same words, so it goes. (2026-09-30)
36. A collection's name, icon and picture are followed from its owner, and - for the owner's own collections -
    from the owner's other devices too, so a picture set on the phone reaches the PC that made the collection.
    An older carton from the same device changes nothing. (2026-09-30)
37. Until every device is on 0.2.004, a device on 0.2.001-0.2.003 sends paths with no icons and cartons with no
    picture, which can clear a look on a 0.2.004 device; said here so it is not taken for a fault. (2026-09-30)
38. The PC reads the phone's WebP pictures with the TwelveMonkeys WebP reader (pure Java, BSD-3, about 580 KB,
    listed in THIRD-PARTY.txt), since Java cannot read WebP; the PC itself writes PNG. The one alternative -
    the phone writing JPEG - would drop the library but make every picture worse; say so if you prefer it.
    (2026-09-30)
39. **The owner's ask, 2026-09-30, during step 5: "make sure we can move the assets freely on the desktop, not that
    they have to be sorted one after the other."** Every icon on Home and in a card stays in the grid cell it is let
    go in, with empty cells left empty, as a phone's home screen keeps them (`Layout`, schema 32 `cell`, kept per
    device and never sent). A grid nobody has moved anything on yet is drawn as before, one after the other; at the
    first move every icon is pinned where it is drawn. Let go in an empty cell: it goes there. Let go on an icon: a
    note on a note makes a collection, anything on a collection goes in, as before. Something new, or moved in from
    another grid, takes the first free cell from the top. A grid narrower than the one a cell was chosen on moves that
    icon to the first free cell after its row.
40. On the phone, scanning somebody's code is ⋮ → *Scan a code…* (in the People section), since **+** makes only notes
    and collections; the camera still opens a code's link as before. (2026-09-30) *Replaced by decision 56.*
41. **The owner's ask, 2026-09-30, during step 5: "let's make the archives and the bin as collections on the desktop
    with an option to hide them if wished."** Home shows an *Archive* and a *Bin* icon (Lucide `archive` and
    `trash-2`), like the Favourites icon: in a cell of their own, which a move keeps; not renamed, deleted,
    shared or given another icon; each opens as a card of what is in it, where a tap offers *Put back* and, in
    the Bin, *Delete for good*, and the Bin card's ⋮ has *Empty the bin*. Letting go of a thing on the Archive
    archives it, on the Bin bins it - said in the status line or strip, with Undo, as the menu does. A count shows
    how many things are in each. Settings → *Show the archive and the bin on Home*, on unless switched off; with
    it off they are reached from ⋮ as before. Both apps. (0.2.006, built 2026-10-01: the bin wears Lucide `trash`, since
    the set this app carries, Lucide 1.49, has no `trash-2` - its `trash` is the bin with the two lines in it.)
42. **The places are carried like every other icon (0.2.006).** Favourites, Archive and Bin are picked up as any icon is
    and let go in another empty cell of Home, where each stays (decision 39's own words: *every* icon on Home stays in
    the cell it is let go in); let go on anything else, or on the dock, they go back. They go into nothing. Until now the
    Favourites icon could not be moved at all, which left it, and would have left the bin, wherever the grid first put
    it. Not asked for in so many words; the agent's call while building 41, so the owner may say otherwise. (2026-10-01)
43. **The owner's ask, 2026-10-01: "a long push on mobile should do the same as right click on the laptop, and right click
    should bring all the design settings for this element".** A long press on the phone and a right-click on the PC open
    the same menu: everything about the thing pressed and nothing else - its colour and their strength, its icon, its
    text size where it has one, Undo, and what is done to it. The app's own rows (Find, People, Backup, Settings,
    Profile, About…) are in ⋮ and ⋯ only, which hold the thing's menu and then them (the owner's choice).
44. **Home's own menu**, a long press or a right-click where there is no icon: *New note*, *New collection*; the text size
    of notes; Home's colour and its strength - the PC gains the colour Home has on the phone; *Undo*; *Sync now*; *Show
    favourites* and *Show search*, switches the same as in Settings. In a card's empty room, the same for that collection:
    *New note*, *New collection*, then its own menu.
45. **The owner's ask, 2026-10-01: Home is pages in every direction**, replacing decision 1. Home is a page the size of the
    screen; others lie up, down, left and right of it. Moved between by a swipe on the phone; on the PC by the mouse
    wheel (up and down), Shift with the wheel or a touchpad (sideways), dragging the empty room, or Page Up / Page Down.
    A double tap or double-click on empty room goes back to the centre page. Zoomed out - a pinch on the phone, Ctrl with
    the wheel or ⋯ → *All pages* on the PC - every page is seen at once, and a tap or click goes to one.
46. **A page other than the centre exists only while something stands on it** (the owner: "the other pages should be made
    active only by dragging an asset there"). An icon carried to the edge of the page and held there goes on to the page
    beyond it - a new one if there is none - and is let go there; a page left empty is gone. Nothing new is ever made on a
    page of its own accord: what + makes, and what arrives, takes the first free cell of the page in view, or of the
    centre page.
47. **Favourites and search are on the centre page only** (the owner's ask), and each can be hidden: Settings, and Home's
    own menu. Hidden, the favourites are still in the Favourites icon and search is ⋮ / ⋯ → *Search*.
48. **A page is what the screen holds.** On the phone, four across (decision 18) and as many rows as fit above the search
    and the dock; every page that size. On the PC, what the window holds (the owner's choice): a smaller window moves the
    icons that no longer fit to the next free cell on their page, without writing anything, so a bigger one puts them back.
49. **Pages on Home only** (the owner's choice): a collection's card keeps its one grid that scrolls down, as a phone's
    folders do.
50. **Where a thing stands** is its page and its cell on it, kept per device as its cell already is (schema 33, `page` on
    notes, collections and files; the places' in each device's settings). A cell kept before pages - one long grid -
    stands on the centre page while it fits there, and its rows below the page go to the pages below, so a long Home
    becomes a column of pages and nothing is lost.
51. **The owner's ask, 2026-10-01: pages move as a phone's own home screen's do** ("the page show up in full in a smaller
    frame so that we can clearly see the transitioning from one to the other"). On the phone the pages follow the finger
    and a pinch shrinks them as it goes, about the point between the fingers, each whole in its frame, with nothing
    between the hand and what is seen. Let go, it settles on the nearest of three: the page, one page with the next ones
    beside it, or every page at once (the owner: "I don't manage to get the whole view when I want to"). A swipe is taken
    when it goes about a fifth of the way or is quick; else the page goes back.
52. **The owner's ask, 2026-10-01: zoomed out, a whole page is carried** ("grab and move the pages, just as it is
    possible with the other assets"). Let go on an empty place round the pages, it moves there; on another page, the two
    change places. The main page's place is always the main page: a page let go on it becomes the main page, and the main
    page goes where that one was. Undo, as for any icon moved.
53. **The owner's ask, 2026-10-01: the dots go with you** ("should update as we move, not only where we drop the grabbed
    asset"). The page in view always has its dot, empty or not, and on the phone the filled dot moves as the pages slide.

54. **The owner's ask, 2026-10-01: a thing carried out of the rows turns the page up or down at once** ("the transition
    should occur as soon as we hit the level under the version number"; "as soon as the note is dragged past the zone where
    the notes are displayed"). Above the rows, under the app's name: the page above, at once. Below the rows: the page
    below, at once - but while the dock is shown, after a third of a second, so a thing on its way to the dock does not
    turn the page as it passes over the search. One page each time it goes out; back into the rows for the next. Left
    and right stay as they were: held at the edge for two thirds of a second.
55. **The owner's ask, 2026-10-01: a dot before the version** - green on the newest version there is, yellow while a newer
    one is out; none until the repository has answered, or while looking is switched off. Yellow, a tap on it fetches the
    build, checks it and hands it to the installer, as before (0.0.109 on the phone, 0.0.020 on the PC).

56. **The owner, 2026-10-02: "the + button doesn't include *From another device* anymore - we absolutely need this, this
    is key: to scan the asset's QR code and paste the asset's code"; then "make sure all +, at all levels, pages,
    collections and notes, and menu include it".** Every + - Home's on every page, every collection card's - offers
    *Note*, *Collection* and, under a line, *From another device…*: the camera on the code someone shows of a note or a
    collection (or of a device), with *Paste instead* for a code sent as a link. So do the menus: Home's and a card's own
    (a long press or right-click on the empty room), under *New collection*; every ⋮ on the phone and the ⋯ on the PC, in
    People (not twice where the menu has it already); and a right-click on a note's page on the PC. What is accepted
    arrives on Home, and the box that accepts it says so.

57. **The owner, 2026-10-02: a code pasted from the Pro on the Graphene stopped at "This line came from somewhere else"**,
    asking for six digits the Pro never showed - a paste could not be finished. A pasted code (or a link opened from
    elsewhere) is taken as a scanned one is: the same box, Accept or Pair, saying it was pasted and giving the six
    digits; each device then shows the digits for every other under People and devices ("Check with them: …"), the
    same on both if nothing came between them. Also seen: the share box opened scrolled to its roles, the top of its
    code under the title where no camera could read it - it opens at the top now; and its "Waiting for them to scan
    it…" never changed - it says when they accept, and that it is on its way to them.

58. **The owner, 2026-10-02, three more from the phones.** (a) *A collection shared again did not show*: an earlier copy of
    it was in the Graphene's bin, and the new arrival went into that copy. Accepting a share now brings an earlier copy
    out of the bin or the archive (`NoteStore.acceptedBack`, both apps; tested in `DesktopLinkTest`). (b) *"Too sensitive
    to the bottom ... we can't even drop an asset on the last row"*: a phone's page left an empty band a row and a half
    tall under its last row - room kept for the + when Home scrolled - which looked like a row and turned the page. The
    room is now the +'s and no more (76), so the Graphene's pages have four rows; and down is only at the page's lower
    edge, where the dots are, or past it, after a quarter of a second (more while the dock shows) - on the PC too.
    Decision 54 is changed so. (c) *"Each time I open the note … it opens in the middle"*: a note opened with the
    cursor at its end; it opens at the top now, the cursor at its start, on both apps (the same note drawn again on the
    PC keeps its place).

59. **The owner, 2026-10-02: "trying to add from another Wi-Fi failed ... please make sure we can share with
    everybody".** Both phones were set to *Only between my devices*, which has no relay: a code they show reaches only
    this Wi-Fi, and a code from another network ended in "is saved, but is not on this Wi-Fi". Sharing with anybody
    anywhere needs *Also through helpers when needed* on both ends. The setting is the owner's and stays his, but it is
    offered where it bites: a code from another network asks "… is on another network - Use helpers?" and, said yes,
    switches and finishes the pairing; a share box shown while only between the owner's devices says the code works only
    on this Wi-Fi, with *Use helpers, so anybody anywhere can use it*, which switches and draws the code again. Both apps.

60. **The owner, 2026-10-03: "the grocery list is not shared between Parisa and the laptop, and also between Graphene and
    Parisa; everybody must be linked through the shared assets, not everyone sharing individually with everyone".** A
    device's list now names the owner's other devices too (`NoteStore.ownDevicesFor`): they have the thing through
    Connect my other device and so had no row on it, and the person it was shared with had no list that named them, so
    their asking to link was refused. Tested in `DesktopLinkTest`. The other half was the setting: the phones sent only
    between the owner's devices, with no relay, so Parisa's phone was out of reach whenever it was not on the same Wi-Fi.
    At his word the Graphene now uses helpers when needed; the Pro and the PC are his to switch.
61. **"We have to be able to move out of a group by a drag and drop."** A thing carried out of a collection's card makes the
    card step aside, as a phone's own folder closes, and the whole of Home is where it can be let go. Before, only a thin
    dimmed strip round the card took it. Both apps (`Folder.stepAside`, `DesktopFolder.stepAside`). In 0.2.013 a hold on the
    card's edge did it too; decision 65 took that out.
62. **"When moving an asset on a page above Home, only one dot remains."** The phone's dots were drawn in a strip one row
    tall; with a page above and one below, the rows above and below the middle were cut off. The strip is now as tall as
    its rows of dots.
63. **"With a right click on a note we should be able to display the writing lines or not."** *Writing lines*, a switch in a
    note's menu beside its colour, on both apps; kept on the device like the note's colour and size, never sent (schema
    34, `notes.lines`).
64. **"Let's remove all AI markers in text, such as long dashes"; "when we click search, the cursor should be in the search
    bar and the keypad should pop up".** No long dash or spaced hyphen is left in either app's wording. The phone's search
    opens with the word field focused and the keyboard up; the PC's field already took the focus.

65. **The owner, 2026-10-03: "when we drag a note we have to be able to put it at any level of grouping we have, in both
    directions, in and out of a group; the group should not be shown full screen so that we have space to move the notes;
    we should even be able to drag and move groups the same way as we move notes."** A collection's card is a box in the
    middle of Home, two thirds as tall, with Home round it. While a thing is carried, held a moment on a collection, that
    collection opens (its card over Home, or one level in); held on the card's *‹* and the name of the collection above, the
    card goes up a level; out of the card is Home. Let go in an empty cell of whatever is open and it is there, in that cell.
    Notes and collections alike, on both apps. A card opened under the finger waits for it to have been over the card before
    stepping aside, and a hold on the card's edge no longer steps it aside: on the Graphene that hold, on the way into a
    card over its name, put the card aside, and the holds that followed opened what lay behind it on Home (it put a test
    note and one of the owner's notes together; put right by hand, see the verification log).
66. **"Even for shared notes that are part of a shared group, everyone has to be able to take all elements where they want;
    in the background they must stay linked, but visually the user needs to be able to put all its assets, shared or not,
    wherever he wants"; and, asked, "we should be asked if we want to share".** A note or a collection has a place it is
    shown in on this device beside the place it really is (schema 35, `shown`, never sent; `NoteStore.showIn`). Sharing
    reads only where it really is. Moving something somebody shares with you, where the move would change who has it, only
    shows it there: it stays linked as before. Moving one of your own where it would change who has it asks: *Share it* (or
    *Stop sharing*), or *Only put it here*. A move that changes nobody's access is a move, as before. A thing shown in a
    collection that is later put away is shown where it really is again; a collection is never shown inside itself.
67. **"We have to be able to set the asset icon of a note individually or for everybody when shared."** The icon box has
    *For: Everybody / Only me* on anything shared; *Only me* is a look kept on this device only (schema 35, `myicon`,
    `myimage`), drawn over the one everybody sees, never sent, and allowed on a thing this device only reads (which shows
    "Only on this phone: you can only read it"). *Default* under *Only me* is the look everybody sees again.

68. **The owner, 2026-10-03: "each participant in a file should be able to pick an individual writing colour and everyone
    should see all others' writing colour"; "we should be able to set the contact colours as we want, because now there is
    confusion between PixelPro and P, and also a P for Parisa"; asked, "theirs, I can change it".** One colour per person,
    for their round and their writing. Each person chooses their own (Profile, *Your colour*, and Settings); it rides with
    every note they send, after the path behind a mark of its own (`Parcel.Sent.ink`, "MNI1"), with when it was chosen, and
    the other side keeps it (schema 36, `said_inks`). A colour given to somebody on this device (tap their round) wins over
    theirs and is never sent; taken back, theirs shows again. The owner's own devices take the later choice from each other.
    A build from before reads the path and stops, so it is not told.
69. **Rounds always wear the person's colour** - in People, in a share box, and under a note's title - not only on a page
    drawn in its writers' colours, so two people with the same initial are told apart at a glance. Writing on a page is in
    colour, as before, where two or more people wrote in it.

70. **The owner, 2026-10-03: "beside the bin, archive and temp we also need a recent group ... all these four boxes could be
    put by default in a tools group on the desktop of the user."** Home has four places beside Favourites: Archive, Bin,
    Temp and Recent, and a fifth that holds them, Tools (Lucide `toolbox`; Temp `timer`, Recent `clock-3`). Settings: *Show
    Tools on Home* (it was *Show the archive and the bin on Home*; off, they are in ⋮ / ⋯ as before) and *Recent keeps*.
71. **"Let's make the assets optionally temporary, so that the content of each note in it is deleted automatically after a set
    time"; asked, "gone for everybody".** *Temporary…* in a note's or a collection's menu, or letting it go on Temp, asks for
    how long: an hour, a day, a week, a month. The time travels with the note (after the path, behind "MNT1"; schema 37,
    `until`), its revision moving on so it goes now; a collection gives its time to every note in it. Each device that has
    it deletes it for good when the time comes (`NoteStore.expire`, when Home is read), so nothing has to be sent then. It
    stays where it is until then, and Temp lists it, soonest first, saying how long it has. A device on a build before
    0.2.016 is not told, and keeps its copy. Only somebody who can write in it can make it temporary.
72. *Tools replaced by decision 78.* **Tools and Recent.** The four are in Tools until carried out onto Home (or, from the menu of one, *Show on Home*), and
    back in it when let go on Tools (or *Keep in Tools*); on the PC, by the right-click menu. Recent lists the notes and
    collections opened or written in within the time Settings gives it (a day, three days, a week, a month; a week unless
    changed; schema 37, `touched`, never sent), the latest first. What Temp and Recent list opens where it really is, and is
    not moved from there.
73. **"When someone shares something with me, I scan it and accept, but it is waiting for him to confirm, the asset should
    already be added as waiting on Home."** An accepted code's offer ("the note …", "the collection …") is kept with it, and
    Home shows it at once, with an hourglass, until it comes; tapped, it says who it is waited for from, with *Stop waiting*.
    Both apps.

74. **The owner, 2026-10-03: "the user can put as many favourites as he wants and these all show in the favourites section,
    so there should be a way to scroll there and drag and drop the icons in the desired order."** Favourites are one order,
    however many there are; the dock shows the first places of it (five on the phone, as many as fit on the PC). The
    Favourites card lists every favourite in that order and scrolls; carried onto another favourite there, one goes before
    it, let go in the room after them, it goes last. The Favourites icon stands on Home whenever there is a favourite. Taken
    out of the dock, a favourite leaves its place, the dock's places close up, and those past the dock keep theirs, so the
    next one is not pulled in. Both apps (`NoteStore.favouritesAll`, `orderFavourites`).
75. **"When we open the bin and archive or any group we should have their icon displayed at the top beside the name, just
    like other assets."** A place's card (Favourites, Tools, Archive, Bin, Temp, Recent) has its glyph before its name, as
    a collection's card has its face. Both apps.

76. **The owner, 2026-10-03: "could we have basic text formatting like underline, with keyboard shortcuts like Ctrl+U"; asked,
    "marks in the text".** `**bold**`, `*italic*` and `__underline__`, the marks chat apps use, kept in the words (`Marks`,
    shared, tested), so a note's style travels, merges and is versioned as its words are; a build from before shows the
    marks. A mark opens and closes on one line, around words that neither start nor end with a space. The page draws the
    words in their style and the marks small and faint. Ctrl+B, Ctrl+I and Ctrl+U put a style on what is chosen, or take it
    off where it is already on, on the PC and on a phone with a keyboard; on a phone without one, *Bold*, *Italic* and
    *Underline* are beside Copy and Paste when words are chosen. Only the marks are put in or taken out, so who wrote the
    words is kept. On the PC, Ctrl+B on a page is bold; elsewhere in the window it still shows the tree.

77. *Moved to the right by decision 78.* **The owner, 2026-10-03: "we need at the top of the desktop app tabs for all the notes and even groups we want open ... a set
    of open elements we can go from one to the other directly"; asked, "a side list on the left".** The PC shows what is
    open down the left of the page area, under "Open" with *Close all*: one line for each open note and collection, its
    face and its name, the one in front marked with the accent; a click goes to it, its × closes it (`DesktopOpenList`).
    The lines are the overview's things (decision 22, at most twelve), kept in the order they were first opened, so they
    stay where they are while one goes between them. Hidden with nothing open; Settings → *Show what is open* and ⋯ →
    *Open things* switch it, on unless switched off. Ctrl+Tab and the overview are as they were.

78. **The owner, 2026-10-03: "on both devices, laptop and mobile, in the Home section of the menu, let's have a toggle for
    showing favourites, bin, archive, open notes, temp groups, and put these on the desktop directly, not in a group as we
    have Tools now"; and "on laptop, where do we see the open notes, we agreed to have them on the side right".** Tools is
    gone: Favourites (while there is one), Recent, Temp, Archive and Bin stand on Home themselves, each with its own switch
    in Home's menu (a long press on the phone, a right-click on the PC, and ⋮ / ⋯ on Home) and in Settings, on unless
    switched off; one switched off is in ⋮ / ⋯ instead, and its own menu has *Hide from Home*. Open: on the phone a place on
    Home (Lucide `layers`) whose card lists what is open, newest first; on the PC the list of what is open, now on the right
    of the window, switched by *Show Open*. The dock's switch is *Show the dock*, so two switches are not both called
    favourites.
79. **"In the settings of the temp group we need to be able to set how long we want the files to stay before being completely
    deleted."** Temp's own menu, and Settings, have *Things stay*: an hour, a day (unless changed), a week, a month. A thing
    let go on Temp is made temporary for that long with no question, said, with Undo; *Temporary...* in a thing's menu still
    asks, starting from that time.
80. **"The Show Search should be like others with a capital letter, we should have a dedicated section in the menu for these
    and it should be lower in the menu since these will be set once probably, put this just before the section Mininotes."**
    The switches are *Show Dock*, *Show Search*, *Show Favourites*, *Show Open*, *Show Recent*, *Show Temp*, *Show Archive*
    and *Show Bin*, under their own heading *On Home*: in ⋮ / ⋯ while Home is in view, just before *Mininotes*; last in
    Home's held or right-clicked menu, which has no app rows; and as a group in Settings. Home's own section keeps its
    colour and *All pages*. The PC's *Open things* row under Find went: *Show Open* is the one way.
81. **"All groups including these technical ones like favourites, archives and so on should have a choice of colour too, like
    all groups."** Favourites, Open, Recent, Temp, Archive and Bin have *Colour* in their own menu (and in ⋮ / ⋯ while their
    card is up) as a collection has: the icon's square is washed and edged in it, its glyph inked in it, and its card is
    the colour's room. Kept on each device, as Home's own colour is: a place is nobody else's.
82. **"The backup that we make should be encrypted ... or it should at least be an option. And what would be the key?"** His
    choice: the lock, or a backup password. With Mininotes locked, a backup is sealed with the notebook's own lock, as
    before (its password or its 12 recovery words open it). Not locked, *Export backup* asks for a password for that
    backup, typed twice (at least 8 characters on the phone, the PC's own rule there), and seals it with a lock made for it
    alone; it opens with that password on any phone or PC. *Without a password* is still there, after "Anyone who gets
    the file can read every note and file in it." A sealed backup is named `mininotes-backup-locked.mnbackup`. Not the
    Minima seed phrase: Mininotes does not hold it, and the notes would hang on the wallet's secret.
83. **"The side menu on laptop where we have recent should be possible to change size and collapse with the divider being
    able to be grabbed."** Taken as the list of what is open on the right (the tree on the left already had this). The
    line before it is the tree's grip, mirrored: pulled, the list takes that width, kept for next time; its handle folds
    the list to the window's edge, the line staying, and a click opens it again at its width; let go narrower than
    120 px, it folds.
84. **"I see Temp as the tool to be used to transfer elements across my own devices, like the notes to self in Signal, so this
    group should have a direct option to sync with all my devices."** Found first: the Graphene has no bond to his other
    devices (no Home-wide rule), only things shared one by one, so nothing on Temp went anywhere by itself. Now a thing
    let go on Temp, or made temporary from its menu, is given to each of his other paired devices that does not have it
    yet, to read and write, and sent at once; it arrives on theirs as temporary, so it is in their Temp, and is gone from
    all of them when its time is up (decision 71). Temp's menu has *Send to my devices* (on unless switched off; also in
    Settings) and *Sync now with my devices*, which gives everything on Temp to them and sends. Where this device may not
    share a thing (somebody else's, read only here), nobody is added.
85. **"The temp times must be shorter: 15 min, 30 min, 1h, 2h, 5h, 10h and 24h."** *Things stay* and *Temporary...* offer
    15 minutes, 30 minutes, an hour, 2 hours, 5 hours, 10 hours and 24 hours (24 hours unless changed), in place of
    decision 79's hour, day, week and month. Kept as minutes under a new setting, so a place in the old list is not read
    as one in the new.
86. **"What is the difference between Open and Recent for a user? These seem the same ... we might need only Recent";
    then "let's accept the reordering of elements in Recent" and "I thought we would get rid of Open and keep only
    Recent".** Open is gone; nothing is closed any more, and what was opened falls out of Recent after as long as *Recent
    keeps* says. Phone: no Open icon on Home, and the swipe up from the dock shows Recent's cards, newest first, a tap
    going there (no Close all, no throwing a card away). PC: the list down the right is Recent, newest first, reordered
    as things are opened, with no × and no Close all; *Show Recent* switches it, and the PC's Home has no Recent icon,
    since the list is it (in ⋯ → Places while the list is off); the Ctrl+Tab overview is Recent's cards too. Replaces the
    closing in decisions 22 and 77 and the Open place of decision 78.
87. **"All groups including Temp, Archives, Favourites and Bin should have the + button to include an asset directly from
    there, and the + should include a From this device so that we can attach all types of file, photos and others"; then
    "yes, please build everything".** Every + (Home, a collection, and now the cards of Temp, the archive, Favourites and
    the bin) offers Note, Collection, *From this device…* (any files, several at once: the phone's picker, the PC's file
    box) and *From another device…*. What a place's + makes is made on Home and put in the place: temporary for as long
    as Temp keeps things (and given to my devices where that is on), archived, in the bin, or a favourite; a note so made
    goes there at its first written word (the phone), or at once (the PC). And a file can now be where a note can: a
    favourite (listed after the others in Favourites, not in the dock), temporary (its time is this device's; when it
    comes, the file is taken out as Delete takes it, which goes to whoever has it), archived or in the bin, from its menu
    or let go on the place; *Delete* on a file puts it in the bin, as on a note, where *Delete for good* takes it. Schema
    38: `files.away` (1 archive, 2 bin), `files.until`, `files.starred`, this device's own; what travels is unchanged.
88. **"When we add elements to a folder like pictures and so on, the icon should be a preview (see Temp on the Pro, I added a
    PNG there)."** A picture file (PNG, JPEG, GIF, WebP, BMP, and HEIC on the phone) shows its picture on its icon, filling
    the round square with a hairline round it, wherever files are drawn: Home, a card, Temp, Favourites, the archive, the
    bin. Read through the lock on the worker and kept in memory while the app runs (the last 80); until it is read, and
    for any other file, the file's kind in its own letters, as before.
89. **"In the menu, first it should be in this order: Home, New, People, Find, On Home, Backup; Send files and Sent files
    should be in New; in Mininotes, in this order: Share Mininotes, Feedback, Settings, Profile, About, Exit"; then "the
    same menu on desktop and mobile as much as possible" and "the colour chooser should be taken out of Home on desktop,
    and Home should be the one we have on mobile 0.2.023".** Both apps' ⋮ / ⋯: the text size and, on Home, Home's colour
    at the top; then the thing (*This note*, *This collection*, a place) or *Home*: New note, New collection, From another
    device…, Sync now, All pages, Back to the main page; then *New* (Send files, Sent files; and New note, New collection
    and New collection in “…” on the PC away from Home), *People* (From another device… there too away from Home),
    *Find*, *Places* (only while one is off Home), *On Home* (while Home is in view), *Backup*, and *Mininotes* in his
    order (*Update to v...* first while one is known, on both; *Exit Mininotes* last on the PC). Then "harmonize
    everything as much as possible": *Tree* on both (was *Tree view* on the phone); the PC's Places open the archive's and
    the bin's cards (were list boxes); a thing's rows say the same on both: *Move to…*, *Share…*, *Add to favourites* /
    *Remove from favourites*, *Temporary…*, *Archive*, *Move to bin* (notes, collections and files). Left to each device:
    the colours inline on the phone and as *Colour ▸* on the PC, "…" on the PC's rows that open a box, *Rename…* and
    *Record audio* on the PC, *Send to another app* on the phone, *Exit* on the PC.
90. **"When we drag and drop elements, we should be presented, like on our phone with an app, Archive or Bin. We have to lean
    towards a UI/UX that lets the user do pretty much everything with drag and drop."** While a note, a collection or a
    file is carried - on Home, in any card, at any depth - a strip comes across the top: Favourite (★), Temp, Archive,
    Bin, Share (Send, for a file). The target under the finger or pointer is lit, and the bar says what letting go does;
    let go there and it is done as its menu does it (Undo where the menu has one; Share opens the sharing box, Send the
    device chooser). Over the strip nothing under it is aimed at. It goes when the carry ends, Esc on the PC included.
    Everything else a carry does is as it was. Where: in the bar just above Home's rows, compact and centred - first put
    over the first row, which then could not be dropped on - so carried past it, above it or beside it on the bar, the
    page above comes as before ("the bin and archive should be presented at the top, but if we overpass them, we go to
    the page on top").
91. **"When we share an element, we should see pretty early the rights we want to grant."** The phone's *Share with* box
    asks *What may they do with it?* first, with the roles this device may give, and the person picked gets that role
    (it gave *can write* to whoever was picked, the role changed only afterwards); the code box asks it above the code.
    The PC asked first already (a role box before the code, and before giving to someone known).
92. **The owner, 2026-10-04: "a picture or any file kept loose in a collection or on Home should be shareable like a note",
    with its own people and roles, its share box and its mark; and "We could define where shared files with me fall. By
    default let's have a folder, Shared with me, but this setting could be changed by the user."** Phase 1, built on both
    apps: a file kept loose has *Share…* in its menu (after *Move to…*), and the strip's last target is *Share* for a file
    too, opening the same box a note's opens (*Send to a device…* stays in the menu as it was). Its roles are *Can read*
    and *Admin* (an admin adds and removes readers and hands it on); nobody is given *Can write* yet, so nothing is
    replaced or renamed for the others. It travels on its own in a sleeve (`Sleeve`, `MNF1`: its id, the same on every
    device, its name, kind and size, where its pieces are, who has it, and whether it is gone), sealed under the file's own
    sixteen bytes and its own revision (`files.revision`), answered as a note is; the bytes go up and are fetched as a
    note's files are. Rules are `FILE` rules (`Sharing.Scope.FILE`, last in the list). A sleeve is never sealed for a
    device that has not said `LOOSE` (`Receipt.LOOSE`, 23, said and heard as `TREE` is): a build from before would take it
    for a note. Until it says so, the file waits, and its box says that device needs to update. The owner deleting the
    file for good deletes it for everybody (kept hidden, `files.gone`, until everybody has answered); anybody else deleting
    it leaves it (`LEFT_FILE`, 24); taking somebody off tells them (`REMOVED_FILE`, 25) and their copy becomes their own.
    Moving it to the bin is this device's own, as before. A file that arrives this way is kept on Home, theirs, so it never
    goes on with a collection, and shown where Settings says (`files.shown`): *Shared with me goes to*, on both apps, is
    Home, the collection *Shared with me* (made the first time it is needed, never shared by itself; unless changed) or
    any collection on Home; where that collection is gone, Home. A shared file wears the sync mark on its icon; one only
    here wears none, as before. Schema 39: `files.theirs`, `files.revision`, `files.gone`, `files.shown`, `incoming.alone`.
    Not built yet: writing in a shared file (replace, rename), and a file shared with you being handed on to your own
    other devices. See SHARING.md, *A file on its own*.
93. **The owner, 2026-10-04: "please now take care of the ones not done yet."** What decision 92 left, built on both apps
    (0.2.027). *Writing in a shared file:* *Can write* is offered for a file as for a note, and the owner, an admin or a
    writer finds *Rename…* and *Replace with another file…* in its menu (also for a file only here; never for one shared
    to be read, which refuses both, as a note shared to be read refuses writing). It stays the same file, by the same id:
    a new name goes at once, a new version goes up first and then its sleeve says where. The sleeve now says when the name
    was chosen and when the bytes were replaced (`files.named`, `files.replaced`), and the later of each stands everywhere,
    whichever arrives first; a new version replaces the file's bytes only once it is fetched (`incoming.replaces`), and
    one older than what is here changes nothing. Taken from the owner, an admin, a writer or another of your devices,
    never from a reader. A writer sends only what it changed (`files.changed`) and the owner passes it on to the others.
    *Your own devices:* a file shared on its own goes to its sender's other devices (*Connect my other device*) as a note
    does, and a file shared with you goes on to your other devices, even to read; its owner's delete for everybody
    reaches them through the device that had it. *Whose it is:* the sleeve names the owner (address and key, after the
    fields 0.2.026 reads), so a device that first has a file from an admin keeps the owner as where it came from, and the
    owner's delete for everybody is obeyed there. *Polish:* moving a shared file is asked about along where it is kept,
    not where it shows, saying who starts or stops having it and that the people it is shared with on its own keep it; a
    shared file wears its mark in Favourites, Temp and the archive as on Home; a file's share code says *They need
    Mininotes 0.2.026 or later to take a file shared on its own*; and anything temporary, a note, a collection or a file,
    wears a timer (Lucide's *timer*, as Temp does) on the corner under where a favourite wears its star. Schema 40:
    `files.named`, `files.replaced`, `files.changed`, `incoming.owner`, `incoming.replaces`. See SHARING.md, *A file on its
    own*.
94. **The owner, 2026-10-04: "let's have an icon for this folder like for the other Home folders and let them all have
    something that differentiates them; Shared with me should also have its on/off switch in the menu's On Home section.
    The newly installed app should come with a note that says My first note and also one that is Read me with all the tips
    in it. Have all this for people updating their version from an older one. ... let's use the term folder instead of
    collection, people know what they are"; asked how places are told apart, he chose "their own frame".** Built on both
    apps (0.2.028). *Shared with me is a place:* no longer a collection made on demand but a place of its own, as Temp is
    (`Branch.Kind.SHARED`, id `shared-with-me`), drawn on Home with Lucide's *inbox* and named *Shared with me*; a file
    that comes on its own shows there (`files.shown` names the place) unless Settings says Home or a folder, and is still
    kept on Home. Its menu is a place's: Open on the PC, its colour, *Hide from Home*; it is never renamed, shared, moved
    or put away. *Show Shared with me* is in On Home, in the menu and in Settings, on unless switched off; off, it is in
    Places. The collection 0.2.026 and 0.2.027 made for it becomes the place the first time this build opens the notebook:
    what showed in it shows in the place, Settings naming it names the place, and the collection goes where nothing was put
    in it (one somebody put things in stays, a folder of theirs). *Places in their own frame:* every place's square
    (Favourites, Recent, Temp, Shared with me, Archive, Bin, and Tools where it is drawn) has a thin ring inside its edge,
    in the edge's colour, so a place is told from a folder at a glance; folders keep their one edge, and so does what is
    on its way. *Two notes for everybody:* *My first note* and *Read me*, on Home, written with the app's own marks, the
    first time this build opens a notebook, a new one or one an older build kept, and never again on that device (the
    setting `welcomed`, which keeps the version that wrote them), so one deleted stays deleted; never inside a backup put
    back; and not where a note of that name is already in the notebook, come from the owner's other device. *Folder, not
    collection:* every word the owner reads on both apps says folder (*New folder*, *This folder*, the + menu's *Folder*,
    "2 folders · 3 notes", the boxes, the empty cards, Settings, what a screen reader says). The notebook, the wire and the
    code keep "collection": an offer still travels as "the collection …", which every build reads, and is said "the
    folder …" where it is shown; a folder named *New collection* by an older build is still taken for an unnamed one.
    *Shared files, without silence* (the owner saw a picture take two and a half minutes with no sign): a file's sleeve is
    no longer sent before its bytes are up, when it was refused as a file that cannot be fetched and only a later round
    brought it; it waits, owed and not failed, and goes from the round that sends the bytes up (the `LOOSE` gate is as it
    was). Meanwhile the file says *uploading* on its icon and in its box, on both apps. And the device it is shared with
    shows it at once in Shared with me as coming (the hourglass, with its name, who it comes from and its size) until its
    bytes are kept, when it is the file itself. Not done: what is coming shows where Settings shows such files only on
    Home and in the place, not in a folder chosen there.

95. **"I put 2 screenshots in the Temp folder on my Pro but they are not coming to the laptop, Send to my devices is on, even
    after pushing Sync now with my devices."** Found: a file let go on Temp was made temporary on that device only (decision
    87 wrote "a file's time is this device's: nothing to send", before a file could travel on its own), and Sync now with my
    devices took a file for a folder. Now a file on Temp is given to this owner's devices as a file on its own (decision
    92: Send to my devices, Sync now with my devices, a file let go on Temp, one made from Temp's +), and its sleeve says when
    it is to be gone, to the owner's own devices only (a later field: older builds stop before it). It lands there
    temporary for the same time, on Temp and on Home, not in Shared with me. "My devices" is one thing: any device paired
    as this owner's, bonded or not (the sleeve's time first went only to bonded ones, which his are not). Schema 41:
    `incoming.until`. And "instead of repeating Show at each element, change the title of the section to Show on Home and
    use only the element name": the menu's and Settings' section is *Show on Home*, its rows Dock, Search, Favourites,
    Recent, Temp, Shared with me, Archive, Bin.
96. **The owner, 2026-10-04: a screenshot on Temp reached the laptop, slowly. "It would be good if it would be faster,
    even more now that I'm on the same Wi-Fi. Is there a setting or a visualisation that would tell me if I'm directly
    connected to a device, could we show that in People and devices? If direct connection is activated, for my device and
    the others, and if it is used at the moment or if a relay is used?"** Built on both apps (0.2.030). *People and
    devices* says, under this device, whether its door is open and on which port, how many devices are heard on this
    Wi-Fi, how notes travel, and on the PC whether its router proved a public door; under each paired device, *On this
    Wi-Fi · direct*, *Direct door known*, *Through relays* or *Not heard lately*, and *Last sent: direct, 2 min ago* (or
    *through a relay*, or *left at your PC*), the road kept by `Direct.send` for each device across restarts (`Routes`).
    *Files near first:* with helpers, a file about to go up whose note, folder or own sharing reaches a device heard on
    this network is first kept at this device's door, and that device is sent its list or sleeve at once and fetches it
    from the door; the upload to the relays follows for everybody else, who are told where it is only once it is up. The
    `LOOSE` gate and the sealing are as they were. See DIRECT.md, *Files* and *What People and devices shows*.
97. **The owner, 2026-10-04, on the laptop: "When a message is displayed at the bottom of the screen like 'Graphene deleted
    a file shared with you', display the message over the elements on the page so that they don't move for the message.
    Let's display better the message beside every user saying 'On Wi-Fi direct...', make sure the message uses the width
    of the window. Also generally speaking this People and devices could be better structured, now the divisions are not
    clear, the assets shared could be in a dropdown section with a show button and so on, please review it in general."**
    Built on both apps (0.2.031). *Messages float (PC):* the status no longer has a bar of the window's own; it lies on a
    rounded strip over the foot of the window, the window's width less a margin, on the frame's layered pane above Home,
    the page and the overview. Nothing under it moves when it comes or goes. Its timing is as it was: gone after a few
    seconds, except a message ending in "…", which stays until the work says how it ended. *People and devices, both
    apps:* three parts under headings, *This device* (its name, its door and how notes travel), *My devices* and
    *People*. Each device is a card (a framed block on the phone): its round and name, then how it is reached on one
    line the width of the card (*On this Wi-Fi · direct · last sent: direct, 2 min ago*, `Routes.oneLine`), then the
    one-sided and check-with-them lines. What is shared with it is folded under *Shared with them (n)* with *Show ▾*,
    closed each time the box opens; what can be done to it (the My device switch, Address on the PC, Forget) sits in one
    row at the card's foot, and a right-click on the device (a hold on the phone) offers the same. An empty part says
    what to do: *No other device yet. Connect my other device to keep the same notes on both.*, with *Connect my other
    device…* always under My devices; *Nobody else yet. Share a note or a folder to add somebody.* Add someone to a note
    or folder keeps its Share… beside each name. Tests: `DesktopPeopleBoxTest` (the three parts in order, the folded list
    opened by Show, the line as wide as the card; a message over Home moving nothing), `RoutesTest`.

98. **The owner, 2026-10-04, on the laptop: "On laptop, the Temp files are also shown on the desktop, where is the setting
    for that. And the Temp folder and all other folders should have the 3 dots menu for contextual settings, just as the
    right click."** Built on both apps (0.2.031). *In Temp, not on Home:* a file let go on Temp on another of the owner's
    devices (decision 95) is shown in Temp only: its `files.shown` names the place Temp (`NoteStore.TEMP`), as one shown in
    Shared with me names that place, and no grid draws it; Temp's card lists it with its time, as before. Made not
    temporary, it is on Home again. *Also show on Home*, off unless switched on, in Temp's own menu and in Settings under
    *Temp: send to my devices* (*Temp: also show on Home*): on, such a file is on Home as well as in Temp, and switching it
    moves those already here (`NoteStore.setTempOnHome`, kept in the settings as a word both apps read). A file of this
    device's own let go on Temp stays where it is, as it did. *Every card's ⋯ (PC):* the bar of every card, Temp, Shared
    with me, Archive, Bin, Favourites, Recent, Tools and every folder at any depth, has a ⋯ at its right (*Menu*) that
    opens the very menu a right-click on that place or folder opens: a place's menu on Home (the bin's with *Empty the
    bin…* in it, Temp's with its own rows), a folder's own. The phone's cards already had their ⋮, with what a hold on
    the place or folder offers. Tests: `DesktopFileShareTest.aFileOnTempGoesToMyOtherDevicesWithItsTime`,
    `DesktopCardMenuTest`.

99. **The owner, 2026-10-05, on the laptop: "I dont get why by clicking a received file in Temp, the mark changes from New
    to this orange arrow, it seems that something is not finalised while I received it. By right clicking it we should have
    a status where we will see who sent it and what is the status of every device receiving it."** Built on both apps
    (0.2.032). *The arrow:* opening a file only clears *New*; the arrow was under it. A file that came from another of the
    owner's devices was owed by the receiver to the owner's other devices too, though the device it came from sends it to
    each of them itself, so it waited there until that device had heard it from here as well. Now a file come from one of
    my own devices, and not changed here, is owed to none of mine from here (`NoteStore.sleevesOwed`). *The status (his
    choice of two, the line and the box):* a file that came from somewhere has, first in its menu, *From Pro · today
    14:14*, which opens its Share box; that box, for any file, starts with *From* and a *Devices* part, each device that
    is to have it with its state in a word or three: *Sent it*, *Has it*, *Waiting*, *Uploading*, *Sent, not confirmed*,
    or, for one of mine that has it from the device it came from, *Gets it from Pro*, since nothing of it passes through
    here (`NoteStore.standing`). Test: `DesktopFileShareTest.aFileFromMyOtherDeviceIsNotOwedBackToIt`.

100. **The owner, 2026-10-05: "In the share admin, we should have the option to share with all my devices, the people and
    devices should be put in groups, my devices should be one but we should also be able to create people groups and add
    people to it like colleagues or friends and so on." Then: "in Who? should be reflected the grouping ... My devices should
    only be one group like others so in People and devices we need to be able to create groups and assign people to these
    and then these groups need to be reflected everywhere they make sense."** Built on both apps (0.2.034). His three
    answers: *live* (a share follows the group: put Ana in Friends later and she has everything Friends has, take her out
    and it is taken from her unless she was also given it on her own, as My devices always behaved), *the same on all my
    devices*, and *one person in several groups*. *Kept:* schema 42 adds `groups` (a name, when it was decided, deleted or
    not), `members` (who is in each, by the key their device signs with, put in or taken out and when), `group_shares` (a
    thing given to a group at a level, GONE where it was taken away, never deleted) and `shares.grp`, the group a person's
    rule came from (empty for one given on its own). *My devices* is a group like the others, built in and never kept: its
    people are the devices marked My device, and Share with all my devices is My devices in Who?. *Live:* one method,
    `NoteStore.applyGroups`, idempotent and in one transaction, gives each person in a group what the group has, at the
    highest level any of their groups gives, and takes away what a group gave and no longer does; never over a rule given
    on its own, never over somebody taken off by hand since, and only where this device may give that level. It runs after
    every change to a group and at every round, and each thing whose people changed is sent with its list. *Travels:* a
    groups card (`Groups`, magic `MNG1`) goes to each of the owner's devices that has said it reads them (`Receipt.GROUPS`),
    only when it changed; row by row, the later decision stands. A build that has not said so is never sent one. *On the
    screens:* People and devices has a part for each group, by name, after My devices (a person in two groups is in both),
    then *Not in a group*; each group's heading has ⋮ (and right-click) with *Rename…* and *Delete group* (its people stay);
    *New group…* at the foot; a person's card has *Groups*, a switch for each. Who? lists the groups first, *My devices ·
    2 devices*, *Friends · 3 people*, each given the thing at the rights chosen above in one tap, then the people one by one.
    Who has access shows a group given the thing as one line, *Friends · Can write*, with who has it from the group quietly
    under it, dropping down to the rights and *Remove*, which takes the thing from the group; the people folded under it
    are not listed again. Tests: `GroupsTest`, `SchemaMigrationsTest.nobodyIsInAGroupYetAndEveryRuleWasGivenDirectly`.

101. **The owner, 2026-10-05: "In People and devices, we should be able to add people's Parlons! profile link, and from the
    note app we should be able to contact them directly in Parlons! ... in the people details here, let's have a contact
    button, and if the Parlons! details are not specified, let's link this to the People and devices, and in there when we
    set a contact, we should have the option to reflect that across all my devices, pick the devices to copy the contact
    details, or keep on the current device."** Built on both apps (0.2.035). *Parlons!* is a Maxima chat app
    (`com.eurobuddha.maxima.app`, from its releases page); its address is `Mx`, letters, `@` and its home,
    `MxG18HGG…@78.141.237.9:9501`. It opens on nothing but its own first screen, no link and no shared text, so *Contact
    on Parlons!* copies their address, opens Parlons! and says the address is copied, to paste there to add or find them;
    where Parlons! is not on the phone, it says so with *Get Parlons!*. On the PC, which has no Parlons!, it copies the
    address and says to paste it in Parlons! on the phone. *Kept:* schema 43 adds `addresses.parlons` and
    `addresses.parlonsDecided`, on every row a person's device is filed under, kept through a re-scan. An address is
    checked loosely (it starts with Mx and has an @ in it) and found inside a pasted line; anything else is refused with
    words that say what an address looks like (`Parlons.address`). *On the screens:* each person's card in People and
    devices has a line *Parlons!* with the address's first eight letters, or *Not set*, and a › that opens their box: the
    address, *Contact on Parlons!* and *Remove* where one is set, *Where it goes* dropping down to *Only this device*, *All
    my devices* (chosen first when I have other devices) and *Choose devices…* (a switch for each of mine, one with an
    older build saying it needs an update), and *Save*, the one primary button. A person's round under a note's title
    opens a box with *Contact on Parlons!* as its button, or, where their address is not known, *Add their Parlons!
    address…*, which opens their box; the same line is in the round's menu (a hold on the phone, a right-click on the PC)
    and in the menu of a person's line in the share box. *Travels:* a small card (`Parlons`, magic `MNP1`): whose it is,
    by the key their device signs with, the address (empty where it was removed) and when that was decided. It goes once,
    when saved, only to the devices chosen that are mine here, count this one as theirs and have said they read one
    (`Receipt.PARLONS`, `Post.parlonsGoTo`); a build from before and anybody else's device are never sent one. Where it
    arrives, only from a device of mine, the person is found by their key and the later decision stands; a card about
    somebody not known there is dropped, and nobody is made up from it. Remove is an empty address decided later, and
    travels the same way. Saving says so on the strip (the status line on the PC) from start to end: *Saved, and sent to
    Pro*, which could not be reached now, and which needs an update first. Tests: `ParlonsContactTest`,
    `SchemaMigrationsTest.nobodyHasAParlonsAddressYet`, `DesktopGalleryTest` (82c to 82g).

102. **The owner, 2026-10-05, on the laptop: "we should be able to set the color at the whole app level too, equivalent of
    the color set at the desktop level on mobile, so when we set it up on laptop, the whole app color should change not only
    the desktop of it"; "in the right click on an element we should have This note at the top of the menu"; "the underline
    should not add space after and before the underlined text."** Built on the PC (0.2.036). *The app's colour:* Home's
    colour, chosen where it was in ⋯, now washes the bar, the side list and its tree as well as Home, and the page of a
    note that has no colour of its own (`Desktop.paintApp`, `washPage`); a note or folder with its own colour keeps it.
    *This note first:* a right-click on a page starts with *This note ▸*, then the words' own lines (Paste, Select all),
    then what is done most from the page. *Underline:* its marks are underscores, which drawn faint read as the line
    running on past the words at both ends; they are not drawn now unless the cursor is in the stretch, where they are
    there to edit. The room the marks take stays: the page is one plain text area, and taking it away means a page that
    lays out styled text, which is a rebuild of the page (not done; said to the owner). Test:
    `DesktopUiTest.theWholeAppTakesHomesColourAndThisNoteIsFirst`.

103. **The owner, 2026-10-05: "We should also in people have the group view to see clearly who's part of a given group and be
    able to manage this, adding and removing people and changing their rights and seeing what is shared with this group. I
    think the whole people and devices page should be reworked to better present all these functionalities. In the share
    with, we should see first the people and devices and then for each of them the rights we give them; for the group the
    rights could be defined at the group level or at the individual group's members level." Then, the same day, after the
    mock-ups: "it should be clearly 2 sections, people and their assignation to a group, and then the group management, the
    other view, group creation and the people in each group. Regarding browsing the app, you have to make sure we can always
    come back to the previous level when digging an element."** Built on both apps (0.2.037). His two choices: *Short list,
    then a page each*, and *Rights beside each name*. No schema step and nothing new on the wire: what decisions 100 and
    101 keep is read in new ways, and one new decision is written with what was there. *Two views:* People and devices
    opens on two views switched at its top, the one in view marked. *People*: This device, My devices with *Connect my
    other device…* under them, then People, each a line with › to their page; under each person a chip for every group,
    filled with a tick where they are in it, outlined with a plus where they are not, and a tap puts them in or takes them
    out at once, said on the strip (the status line on the PC). *Groups*: *New group*, then *My devices · 2 devices* first
    and each group by name, *Friends · 3 people*, with who is in it quietly under it, › to its page and ⋮ for *Rename…*
    and *Delete group* (My devices has neither). The cards of decision 97 and the parts of decision 100 are gone: a
    person's card is their page. *A group's page:* *Members*, each a line that opens their page, with *Remove*, which asks
    first and says by name what they lose; *Add people ▾*, which drops down whoever is not in it, a tap adding one and the
    list staying for the next; and *Shared with this group*, each thing with the group's rights dropping down to the ones
    this device may give and *Remove*, its name opening the thing's Share box. My devices' page lists its devices, with
    *Connect my other device…*, and what was given to it as a group. *A person's page:* what their card held (how they are
    reached, the six digits, *Parlons!* with its ›, *Address* on the PC, *Groups* with a switch each, *My device*,
    *Forget*), and *Shared with Ana*, thing by thing: what came through a group says *through Friends*, and changed there
    it is hers. *A way back:* one box, its levels drawn in turn, not a box on a box. Every level under the first has ‹ at
    its top naming where it goes back to (*‹ People and devices*, *‹ Friends*), and goes back to that level in the view and
    at the place it was left; the phone's Back, and Escape and the window's ✕ on the PC, go back one level and close only
    from the first. A thing's Share box and a person's Parlons! box open over the page they were asked from, with the same
    ‹, and that page is drawn again when they close; so is every level when a card from another device changes what it
    shows. On the phone, what People and devices opens from its foot, and People and devices from Profile, open over the
    box they were asked from, which is still there afterwards. *Share with:* Add someone on a thing (the Share box's line,
    its *Share* button, the + after a note's rounds) opens *Share “Saturday market” with*, in place of Rights then Who? on
    the phone and of the cards with *Share…* on the PC: *Groups* first, My devices then each by name, then *People*, whoever
    is in no group, with the rights beside each name. A name not on the thing yet has *Add ▾*, dropping to the rights this
    device may give, and picking one shares at once; one on it has its rights, dropping to the others and *Remove*. A
    group's name folds open (▸) to its people, each *As the group ▾*, or with rights of their own, or *Removed ▾*: a
    role picked for one member is a rule of their own on that thing, which stands whatever the group is given, and *As
    the group* gives it up, also for somebody taken off by hand. The box stays open and is drawn again after each change.
    Opened from the Share box it has *‹ Saturday market*, and going back opens that box again as things stand then. Only
    the owner or an admin opens it, as before. *Kept:* `NoteStore.asTheGroup` marks their
    rule as the group's and lets `applyGroups` set it to the highest level any of their groups gives, refused where no
    group of theirs has the thing; `standsOn` says where each person stands (their own rights, a group's, removed by hand,
    owner, through what holds it, or not on it), `sharedWithGroup` and `sharedWith` what a group and a person have, with
    the group each came through. Tests: `GroupsTest.aMembersOwnRightsWinAndAsTheGroupPutsTheGroupsBack`,
    `GroupsTest.whatAGroupHasAndWhatAPersonHasAreListedWithTheGroupItCameThrough`, `DesktopPeopleBoxTest` (the two views,
    a page each, back one level at a time, Share with), `DesktopGalleryTest` (04b to 04f, 12, 12b, 82g).

104. **The owner, 2026-10-05, with a picture of a blue window and a cream band down its page: "let's use an invisible
    scrolling bar on laptop."** Built on the PC (0.3.001, the first of the 0.3 builds, at his word: "let's start from
    0.3.001 now"). Every scroll bar's strip is the colour of what scrolls beside it, and its handle shows only while the
    pointer is on the strip or it is dragged (`DesktopScroll`, the one scroll-bar look for the whole app); the wheel, the
    keys and a drag scroll as before. The divider between the page and the side list is in the app's colour too
    (decision 102), since it was the other half of the band in his picture.

105. **The owner, 2026-10-05, on the laptop's People and devices: "When we click People and devices, and follow This
    device name, it should bring to the profile page. What we have now as direct connections open on port 9601 and so on
    should show in the profile page, and from the profile page, we should be able to go back to people and devices. My
    devices and People sections should be structured the same way, with the buttons located at the same place, the text
    too. We need to make it intuitive to the user. The connect my device should be placed where the add someone button
    is, and since we say People and devices, people section should be first on the page. This is the type of consistency
    we need across the whole app, consistency in design and in logic." And: "a new downloaded windows app should open on
    the desktop not inside the My first note."** Built on both apps (0.3.002). *The order the page is called by:* People,
    then My devices, then This device. *The same skeleton:* each part is its name, its lines, and its one thing to do at
    the same place in both: on the PC beside the name, at the right (*Add someone…*, *Connect my other device…*); on the
    phone a + and its words under the lines (*Add someone*, *Connect my other device*). *This device is Profile:* its
    line opens Profile over the page, with *‹ People and devices* (and Esc, ✕, the phone's Back) coming back to it; there
    is no second page about this device. Its door and how notes travel are said in Profile, under *Connection*; in
    Profile opened this way, *People…* goes back instead of opening the page a second time. *A new notebook opens on
    Home* on the PC, where *My first note* and *Read me* are to be seen; a note is in front again from the next start,
    as whatever was open always is (the phone opened on Home already).

106. **The owner, 2026-10-06, on People and devices: "remove connect my other device at the bottom of the section and at the
    bottom of the window, remove both add a device and share with someone, or even better, just put a + on the right side
    of both My devices and People so that people will understand that this is to add elements."** Built on both apps
    (0.3.003), his "even better". Each part's name has a + at its right, the one sign for adding at the one place: by
    *People* it connects with someone (scan their code or paste their link), by *My devices* it adds a device of yours (the
    code that connects it). What it does is said under the pointer and to a screen reader (*Add someone*, *Add a device*).
    Gone: the phone's rows under each part and its two buttons at the foot of the box (*Add a device*, *Share with
    someone*); the PC's buttons with words and its foot (*Scan a code or paste a link…*). An empty part says to use the +.
    A group's page and Share with keep their own actions.

107. **The owner, 2026-10-06, on the laptop, with a picture of a folder's *Colour ▸* (nine colours, then *Colour strength*
    and its slider): "the color intensity applies to the whole app, it should be specific to the elements selected. Also,
    when I pick the color, the menu should stay open so that I can set other elements, now it closes and I have to reopen
    it."** Built on both apps (0.3.004). *Its own strength:* the slider under a thing's colours sets how strongly that
    thing's colour lands, and that thing's only: a note, a folder, each place that has a colour (Favourites, Recent, Temp,
    Shared with me, the archive, the bin) and Home, whose colour on the PC is the whole app's (decision 102). It goes
    wherever the thing's colour is washed: its tile and the little pages in a folder's, its card, its page and ruled
    lines, its line of the tree on the PC, the overview. A thing never given one is washed at *the usual*: the one setting
    the whole pad had ("tone"), read as it was left and never written again, so nothing changed its look on the day of the
    upgrade; no menu sets the usual any more. *Kept:* a note's and a folder's beside its colour in the notebook (`tone`
    on `notes` and on `things`, -1 for the usual; schema step 44, additive), read with what a thing wears
    (`NoteStore.dress`) so `Branch.tone` and `Note.tone` are there to draw with; Home's ("homeTone") and a place's
    ("tone_" and its id) beside their colours in the settings. *On this device only*, as a note's size is: no revision
    moves for it, a save carries it over, nothing is sent, and no message changed. A backup holds it for a restore of the
    same pad, as it holds a note's size; a backup added as copies takes none. A folder's colour does travel (in its
    carton, and in the path every note under it is sent with); carrying the strength with it is not done, and is the
    owner's to decide. *The menu stays:* on the PC a colour picked leaves the whole menu up, the one it dropped from
    too: the ring moves to the colour picked, the line it dropped from wears it, the strength under the colours is drawn
    in it, and the thing is painted behind. So do *Writing lines* and *Same as other notes*, which are set by looking
    too; the text-size ladder and the strength always did. It closes as menus do: a click outside, Esc, or a line that
    does something. On the phone the sheet stayed open already for all of these; what is new there is that a tile on
    Home is drawn again behind it at once, not when the sheet closes, and that the slider is the thing's own and is only
    there under a thing's colours. Tests: `SchemaMigrationsTest.everyNoteAndFolderStartsAtTheUsualStrength`,
    `TintTest.aThingWithNoStrengthOfItsOwnIsWashedAtTheUsual`,
    `DesktopStoreTest.aStrengthIsOneThingsOwnAndEverythingElseKeepsTheUsual` and
    `theUpgradeLeavesEveryThingAtTheStrengthThePadHad`,
    `DesktopUiTest.aColourPickedLeavesTheMenuUpAndTheStrengthMovesOneThingOnly`, `DesktopGalleryTest` (32b to 32f).

108. **The owner, 2026-10-06, told that a strength stays on the device it was set on and that a note's own colour does not
    travel at all: "when sharing everything should travel, then on the other device it can be set individually."** Built
    on both apps (0.3.005). *What travels:* a note's own colour and its strength, in its parcel; a folder's strength
    beside its colour, in its carton. Home's and the places' colours and strengths are settings, not shared things, and
    stay where they are set. *The later decision wins:* each colour and each strength keeps when it was last decided
    (`colourDecided` and `toneDecided` on `notes` and on `things`, 0 for never; schema step 45, additive), and what
    arrives is taken half by half where it was decided later than what is here. So a colour or a strength set on the
    other device afterwards stays, and is that device's own until somebody decides again; an older one there is replaced.
    Setting either is a new decision, now: the thing's count moves on, as a rename moves it, and it is owed to everybody
    who has it, words unchanged; a note this device may only read is the one exception, its look its own with no count
    moved, so the owner's next words are never taken for older ones. Taking a look that arrived moves no count, so
    nothing is owed straight back. A folder's colour now follows too where it is decided after the folder was made
    (decision 17 had it used only when the folder is first made); one never decided still is. A folder's look is taken
    from whoever its name and icon are followed from (step 4). *The usual travels as the usual:* a thing given back to
    the usual arrives so, and is washed at the receiver's usual; a thing never given a look arrives with none. *On the
    wire:* a note's look is the last thing after its path, behind a mark of its own (MNL1: colour, strength, and the two
    times), after the sender's colour and the time a note is to be gone, so a build from before reads both of those and
    stops at a mark it does not know. A folder's is after its files (the same mark: strength and the two times; the colour
    is where it always was), where a build from before stops reading. So no capability bit: an older build takes the note
    and the folder and ignores the look. Said only where something was ever decided. Both apps send at once when a
    colour or a strength is set, as an icon is sent. A backup keeps the two times beside the strength for a restore of
    the same pad. Tests: `ParcelPathTest.aNotesOwnColourAndStrengthRideLastAfterThePath`,
    `CartonTest.itsStrengthAndWhenItsLookWasDecidedRideAfterTheFiles`, `SchemaMigrationsTest.everyLookStartsNeverDecided`,
    `DesktopLookTravelsTest` (both ways, the receiver's later own setting kept, the usual as the usual, a build from
    before, a reader's own look, a folder in its carton); `DesktopStoreTest.aStrengthIsOneThingsOwnAndEverythingElseKeepsTheUsual`
    now has a folder's count move for its strength.

109. **The owner, 2026-10-06: "When someone shares something with me, I need an alert, a pop-up at first telling me, and it should
    go in Shared with me, and there I should have an accept or refuse, and refuse could be silent or inform the sender with
    the option to send a message back."** Built on both apps (0.3.006). His two choices: *Goes to Home* (accepted, a note or a
    folder leaves Shared with me for Home; files stay where files shared on their own go, decision 94) and *It arrives,
    hidden until I accept* (kept, so accepting is instant, and shown nowhere but its line in Shared with me). *What waits:* a
    note, a folder or a file that another person shares with me for the first time and that lands on Home. Not what comes
    from one of my own devices (bonded or paired as mine), which comes as it always did and, where the same thing waits here
    from the other person, accepts it here too; not what lands inside something already here (a new note in an accepted
    folder, a new version, a role changed); not what a code I took up brings, which I said yes to already; and not anything
    here at the upgrade. *Kept:* schema step 46 adds `accepted` to `notes`, `things`, `files` and `incoming` (1 for every row
    there is, 0 for what arrives new from somebody else on Home; a file still coming waits as the file will), and on the
    sender's side `refusals` (who refused what, their words, when). A folder that waits is away as an archived one is, so
    nothing in it is shown or owed to anybody, my own devices included, until it is accepted; every live list says
    `accepted=1` (Home and every folder's contents, Shared with me and what is coming to it, both searches, Recent, what was
    written lately, the tree, Favourites, the dock, Temp and its count, where a thing can be moved, what a new page is
    written in, what opens first, and what is owed anybody). The phone has no widgets. *The pop-up:* once for each thing,
    never for each revision (`NoteStore.waitingUnsaid`, kept by id in this device's settings): *Shared with you*, "Ana shared
    “Saturday market” with you", under it *A folder · Can write. It waits for you in Shared with me.*, and *Later* and *See
    it*, which opens Shared with me. Several are one box, "Ana and Ben shared 3 things with you", and what comes while it is
    up says it there, never a box over a box. It comes when something arrives while the app is in front, and whenever it
    comes to the front; away, the PC's tray says the line, and the phone says it in a notification (*Open the pad to accept
    or refuse it.*), each once for each thing. Shared with me's tile on Home wears how many wait, as Temp wears its count.
    *Shared with me:* its first part, *Waiting for you*, a line for each thing, its icon, its name and "from Ana · Can
    write", with *Refuse* and *Accept* side by side, Accept filled; past three lines and a half it scrolls, so the files under
    it stay in sight; nothing waiting, it is not there. *Accept* is said on the strip (the status line on the PC) while it
    works and once it is done, "“Saturday market” is on Home now". *Refuse* asks in a small box: *Refuse quietly* (they are
    not told) or *Refuse and tell them*, with a field, *Add a few words, if you like*; its one button is *Refuse*, in the
    colour of what cannot be taken back. Either way what this device holds of it is deleted, words and versions too, and
    nothing more of it is taken in from anybody who has it (`NoteStore.refuseShared`): told, it is left as Unfollow leaves
    it (`refused.gone` 1, given again later brings it back, and waiting again); quietly, at `NoteStore.QUIETLY` (2), said to
    nobody, never undone, and what still arrives is answered as had, so its sender does not try for ever. *On the wire:* a
    refusal is a new answer, `Receipt.REFUSED` (29): the five bytes, then the number a leaving of that thing is said by
    (`LEFT_PAGE` and the others), then the words, UTF-8, at most 280 characters; which thing rides in the envelope as for a
    leaving, and when where a revision does (`Post.refuse`). It is longer than an answer, and a build from before takes what
    it does not know for a note, so it is sealed only for a device that has said it reads one (`Receipt.REFUSALS`, 28, said
    and heard as `LOOSE` is). Everybody else who has the thing, and a sender whose build has not said so, hears what Unfollow
    says (`LEFT_PAGE` and the others) and sees "Parisa unfollowed a note". The sender that reads it takes them off as a
    leaving does, keeps the refusal and the words, and says once "Parisa refused “Saturday market”" with the words under it
    (the status line and the tray on the PC, a line on the phone); in the thing's Share box, after the people, their line
    reads *Refused*, the words under their name. The telling is tried once, at the refusal: with nothing left here to name
    it by, a note or a folder is not said again as a leaving is for a week (a file, which names itself, is, as a leaving),
    and the strip says when nobody could be reached. A folder with no note in it yet has nothing to be named by, as for
    Unfollow, and is refused here without a word to anybody. The words are in one place for both apps (`FirstShare`). Tests:
    `DesktopFirstShareTest` (hidden and waiting, from my own device shown, every list walked, a note new in an accepted
    folder, a file waiting while it comes and once here, refused quietly, refused with words and the sender's store, a folder
    refused whole, the upgrade), `ReceiptTest.aRefusalCarriesWhatWasRefusedAndAFewWords`,
    `ReceiptTest.whatIsSaidAboutSomethingSharedWithMeForTheFirstTime`,
    `SchemaMigrationsTest.everythingAlreadyHereCountsAsAcceptedAndNobodyRefusedAnythingYet`, `DesktopSharedWithMeTest`
    (the pop-up for one and for several, Waiting for you, the Refuse box, the sender's notice and the Refused line, each
    pictured).

110. **The owner, 2026-10-06: "In People + we should have there a share my profile link, the part from my profile, your
    address, copy, but we should also have a share my address that triggers the share function of the phone so the
    address can be shared by social medias and others, and on the profile page."** Built on both apps (0.3.007). *+ by
    People* offers the ways to connect, theirs and mine: *Scan their code*, *Paste their link*, then *Share my link…*
    and *Show my code* (the PC: *Scan a code or paste their link…*, *Copy my link*, *Show my code…*, which opens
    Profile over the page). *Share my link…* hands the phone's share sheet my link (the one my code holds) with a few words
    for somebody who has never seen the app: who it is from, to open it where Mininotes is installed or paste it in + by
    People, and where to get the app (`Pairing.invite`, one wording for both apps). Profile has it too, under *Copy
    address*. The PC has no share sheet: its *Copy my link* (in the + and in Profile, which said *Copy pairing link*)
    puts the same words on the clipboard. The link is `mininotes://pair/…`: it opens Mininotes on a phone that has it, and
    some social networks do not make it clickable, which is why the words say how to paste it.

111. **The owner, 2026-10-06: "The user should be able to make himself highly private notes and folders, by typing a code
    anywhere, and it should reveal them. To create them it should be like that: type in any note privatefolder//:[password]
    or privatenote//:[password]; then, after closing, to access them only the password should open them. ... Once closed
    they should not appear anywhere, and a hacker should not have any trace of their existence even by looking at the code
    of the app."** And: "So that people are aware of this functionality, we should include that with other elements in the
    Read me note." His choices: this device only, no sharing, the keyboard does not learn in every note; then 64 MiB for
    everybody ("maybe like 50 photos"), writes on a fixed rhythm ("how do you make sure it stays always the same"), the
    decoy tip, and compression with no option. Built on both apps (0.3.008). *What is promised:* the code is public, so
    the feature is not hidden; what nobody without a password can tell is whether a device's owner ever used it, how much,
    or for what. A forgotten password loses them for good: the 12 recovery words do not cover them, since if they did they
    would prove a space is used. *The file:* `pages.protected` in the app's data, on every install from its first start
    (made by the upgrade too), 1024 slots of 64 KiB, 64 MiB, never more or less, random from end to end without a password
    (`Slots`). A slot on disk is 16 random bytes and the rest in AES-256-CTR under this device's own key (SHA-256 of a label
    and the device's agreement key, which the Keystore or the Windows user guards), so every slot written is new bytes.
    Under that, the same on any device: a password's slot is a 12-byte nonce, a 16-byte mark (HMAC-SHA256 of the nonce
    under the password's mark key) and AES-256-GCM of a 36-byte head (which thing, which writing of it, which part of how
    many, how long) and up to 65,440 bytes of it, Deflated before; any other slot is random. Slot 0 holds the salt, slots 1
    to 17 the journal. A password's keys are PBKDF2-HMAC-SHA256 of it (UTF-8) and the salt, 200,000 rounds (0.65 s on this
    laptop), split by HMAC-SHA256 into one to seal with and one to mark with. Opening reads each slot's nonce and mark, and
    decrypts only a slot whose mark that key makes: nothing answering, nothing opens, nothing is written and nothing is said.
    *The rhythm:* a batch is 16 slots, 1 MiB, written at every start, every minute while the app is in front (the PC: while
    it runs) and when it is left (the phone's onStop; the PC minimised, hidden or closed), whatever is private: first the
    parts of a change that wait, then slots anywhere, written again as they were under new random bytes. It goes to the
    journal first (its head with a SHA-256 over all of it, and its 16 slots), then in place, so a batch cut short is put in
    place again at the next start. A change is never written outside a batch: one too big for a batch waits in memory,
    sealed, for the next ones; closing does not wait for it, the batches after write it, and the line at the foot of the
    screen says so while it waits ("Writing privately: 3 MB to go. Keep Mininotes open."). *What a password holds* (`PrivateSpace`):
    a list of its things and each thing by name: every note and folder with its colour, strength and place and every file's
    name in one, each note's words and versions in another, each file's bytes in its own. A change writes new slots beside
    the old ones and the list last; the old slots are filled with nothing under the same key once the new list is whole, so a
    write cut short leaves the last whole state. *Rooms* (the owner, 2026-10-06, the trade-offs explained to him: "Decoy first,
    real inside it", then "Two fixed halves"): the data slots are split in two fixed halves by a rule the same on every
    install, every other slot (`Slots.half`), so each half is spread over the whole file and the split needs no record. Each
    space writes only in its own half, its data and its own fillers alike; batches still pick their other slots across the
    whole file. A space made from outside always has the first half, about 31 MB (503 slots), whatever else exists, and
    its room never changes, so its password shows the same room on every device. *New private space* in a first-half space's
    menu (the password twice; the box says it gets about 31 MB, the other half) makes the second space, whose room is the
    second half, whole; it is not offered inside a second-half space, so there are two at most. The second space's index
    keeps the first one's keys, so opening it opens the first too, unseen, and never writes over it; the first keeps nothing
    of the second, so opened alone it can never touch it, its password reveals nothing of it, and a space with a second made
    inside it looks the same, slot for slot, as one alone. A half full: "Private storage is full", inside only, and nothing
    of that change is kept. So the owner's order: the decoy first, the real one made inside it. *The codes* (`PrivateCode`): privatenote//:, privatefolder//: and private//:, in any case and with a space
    allowed after "private" (a phone's keyboard puts one), in a note's page or title, in the search, or in a private note.
    The moment the colon is typed the code is taken out of the words, before anything can write them down, keep a version or
    an undo of them or send them, and a password field takes the keyboard ("Password, then Enter"; Enter, as a phrase of
    several words has spaces). Nothing answering, privatenote//: and privatefolder//: ask for the password once more ("The
    same again, then Enter"), and only the same twice makes a new space with a note, or a folder with a note in it, and
    opens it, so a typo makes nothing and writes nothing; private//: does nothing and says nothing. While the word at the caret could still
    become a code ("privaten", "private note/") the writing down that happens by itself waits for the next key; a save that
    cannot wait first takes out an unfinished code that has its slashes. *Inside* (`PrivateScreen`, `DesktopPrivate`): a screen
    of its own over the whole window, *Private*, with + (New note, New folder, and in a note Add a photo or file), ⋯ (in a
    note Colour, Versions, Add a photo or file, Move to, Delete; then Bin, Change password, Close) and Close; its tiles drawn
    as Home draws them; a note on ruled paper washed in its colour; its files under it, a picture shown in the app from
    memory, Save a copy for any (the user's own act, the one way anything leaves); a bin with Put back, Delete for good and
    Empty the bin; a version kept each time a note is left. Nothing to share, send or sync. A photograph is made about two
    megapixels (its longest side 1920 pixels), turned as its camera said, JPEG at 85, with nothing of the camera or the
    place kept; everything is Deflated before it is sealed, and space is counted after. *Closing:* Close, Back or Esc from
    its top, leaving the app, five minutes with no touch or key, and the notebook locking again: keys and contents let go,
    a file's bytes zeroed, and what was copied from a private note taken off the clipboard if it is still there (and marked
    sensitive on Android 13 and later, so the keyboard and the system neither show nor keep it). The phone's window is
    FLAG_SECURE while it is up; the PC asks Windows to leave the window and every box over it out of any capture
    (SetWindowDisplayAffinity). *Nowhere else:* never in the notebook (no schema step), on Home, in search, Recent,
    Favourites, the dock, Temp, the tree, the overview, the open list, a share box, a notification or a log; whatever fails
    inside is let go without a word; pictures are read and made in memory (the PC's picture cache files are off for
    everybody). *The keyboard does not learn, in every note:* the page and every field the phone makes for a note, a name or
    the search say IME_FLAG_NO_PERSONALIZED_LEARNING; a PC keyboard has nothing to be told. *Backups:* every backup carries
    the file (its entry `pages`, the file without this device's outer layer), whatever is private; Replace everything puts it
    in place once the notes are in, and it opens there with the same passwords; adding as copies leaves it. A backup made
    by this build is refused by an older PC build, which does not know the entry. *Read me:* a part, *Private notes*: how
    to make and to open, about 31 MB a space (about 60 photos and text), the first space with privatenote//: and a second,
    the other half, with New private space from inside the first, two at most, and never a space by a new code from outside
    once one exists (it would take the first half again and write over it), a long phrase, no recovery, nowhere else
    and this device only, and his tip in his words in this order: a believable first space, the real one made inside it,
    and the first used now and then so it looks lived in. A Read me that still says word for word what an
    earlier build wrote says this now (the old words kept as a version); one somebody changed is left as it is. *Not
    leak-free, and why:* somebody with two copies of the file taken apart in time and this device's own key (a rooted
    phone, the Windows user's login) can see which slots' inner parts changed between them, and two copies of a backup that
    is not locked show the same (its inner part travels without the device's layer, so it opens anywhere); a space reuses
    its own slots, so somebody watching every single batch could see the same places written again; a space made from
    outside cannot know of one that is closed and always takes the first half, so typing a new code from outside once a space
    exists (with the same password twice) writes over the first, which Read me warns of; the halves are public, so anybody
    may be asked for a second password whether or not there is one: the decoy protects by being believable, and nothing on
    the device says whether the second half holds a space; the recent apps keep a blank picture of the app until it is in front again after being left
    with the private screen up; keys and words are in the app's memory while it is open, and Java's strings cannot be
    zeroed; a save that cannot wait with "privatenote" at the caret and no slash yet writes that word; Windows' clipboard
    history, where it is on, keeps what was copied; and until a change of password is written, the old password still
    opens in the next start of the app. Tests: `SlotsTest` (the same size of random from the first start, nothing for a
    wrong password, written only by batches, as many bytes for everybody, a change too big for a batch, a space closed with
    writing waiting, two passwords, a space made inside never writing over the one it was made in, the decoy made first and
    the real one inside it with 28 MB, the decoy opened alone and filled to the end of its half and the real one opening
    byte for byte, nothing in the decoy's index or anything its password opens saying a word of the real one (not its keys,
    its ids or its slots) and its room exactly the first half, a lone space's room the same as a decoy's with a real one
    inside it, slot for slot, and no third space, a new password, the old one opening nothing
    even before it is all written, full, a batch cut short, a backup opening on another device, deleted things filled with
    nothing), `PrivateSpaceTest` (the codes, the holding of a save, notes, folders, files, versions, the bin, full),
    `DesktopPrivateTest` (two installs, one never used and one with a private note and 50 photographs, after the same two
    starts: the same files at the same sizes, the same batches and bytes written, none of the words, title, password,
    photographs' names or camera, or the codes in any byte of any file, the file noise in both, no picture cache file; made from
    outside, the password asked twice, and two that differ writing not a byte and opening nothing; a code
    typed slowly in a note shared with a phone, in its title and in the search: none of it in the note, its versions, its
    undo, what is owed or what the phone gets; a password nothing answers to: not a byte changed; a backup opening on
    another PC; and the screen pictured).

112. **The owner, 2026-10-06: "could the file or folder have a compression option so that attachments are reduced?" then "can
    we add the compression rule to all notes and folders?"** Built on both apps (0.3.009). *Pictures:* every picture added
    anywhere (to a note, a folder, Home, Temp or Shared with me; pasted, dropped, shared from another app, taken in the
    phone's picker, added on the PC, sent to a device on its own, or a new version of a file) is made smaller before its
    row is written (`NoteStore.settle`, the rules in `Shrink`). Bigger than 1920 pixels on its longest side, or turned by
    its camera, it is drawn again, about two megapixels, upright, JPEG at 85. A JPEG already that small and upright loses
    only its EXIF, XMP, IPTC and comment parts, byte for byte otherwise, its colour profile kept. A PNG is a PNG or a JPEG,
    whichever is the smaller, and a PNG where any of it is see-through; one already small loses its words, times and EXIF
    the same way. A GIF is kept as it is, since it may move. What is made is kept only where it is smaller, except a turned
    one, which can only stand without its camera's word by being drawn again. A picture that became a JPEG is named so
    ("IMG_2041.HEIC" is kept as "IMG_2041.jpg"). The phone draws with Android's decoder (`Pictures`, which reads a HEIC),
    the PC with ImageIO in memory (`DesktopPictures`; a HEIC it cannot read is kept as it is), both reading every n-th
    pixel of a picture far bigger than it needs to be, never under 1920. The private notes (decision 111) draw with the
    same and still always make a JPEG, as they did. A picture is never packed: its kind is packed already, and on the disk
    it stays a picture anything can read. *Other files:* kept Deflated on this device where that saves a tenth
    or more (a big one tried on its first 256 KB first), behind a mark of their own (MNZ1, inside the seal while the
    notebook is locked), and always a file that begins with that mark, so none is ever taken for a packed one. Everything
    that reads a kept file unpacks it on the way: what goes up, a file lent to another app or opened, a copy saved, a
    backup. A file fetched from another device is packed as it is kept, and never shrunk: it is the same file as
    everybody's. *The wire is unchanged, so no capability bit:* a list names a file with its own size, and what goes up is
    its own bytes, so a build from before fetches and reads exactly what it always did; a shrunk picture travels as the JPEG
    or PNG it now is. *Settings,* under *Pictures and files* on both apps: *Shrink pictures*, on unless switched off
    (`shrinkPictures` in this device's settings; off, a picture is kept as it came and other files are still packed), and
    *Shrink the pictures already here…*, which looks first, the strip (the status line on the PC) saying how far ("Looking
    at the pictures: 3 of 23"), each made beside the notebook, sealed as kept files are; then asks, *Shrink 23 pictures?*
    "They take 84 MB now and would take 9.1 MB: about 75 MB freed. Each replaces its original, which cannot be had back.",
    with *Shrink them* in the colour of what cannot be taken back; closed, nothing is replaced and what was made goes. It
    shrinks only a picture that is this device's alone: added here, never gone up, never reached anybody, not sent on its
    own. One that went to somebody or came from somebody is left as it is, and the box says how many, since every list
    names a file with its size and every other device has it as it was. No revision moves for it, and no schema step.
    Tests: `ShrinkTest` (the parts taken out of a JPEG and a PNG, the rule against a painter that only says what it is
    told, names, packing read back in pieces of every size, a file cut short, the lookalike, the words),
    `DesktopShrinkTest` (a 3200 by 2400 photograph of 1,955,187 bytes kept as 832,081, 1440 by 1920 and upright, with no
    EXIF, camera or place; a small one, 278,995 to 278,901, the same picture byte for byte; a screenshot kept a PNG without
    its words; a see-through one made 1920 wide and still see-through; a photograph saved as a PNG, 10.7 MB, kept as a
    0.8 MB JPEG; the switch off keeping the original; 213,890 bytes of words kept in 12,862 and read back, locked and not,
    and in a backup; already here, 3.3 MB to 1.6 MB, the one that went up left byte for byte; a new version; the Settings
    part, the box and the line pictured), `DesktopFilesTravelTest.aPackedFileTravelsAsItsOwnBytesAndIsPackedWhereItLands`.
    *Not done:* a backup restored puts its files back as they were in it, neither packed nor shrunk.

113. **The owner, 2026-10-06: "could we even make it that if one note is open, it sends a help request to a contact with
    the phone location, all this of course fully secretly?"** Built on both apps (0.3.010). *The switch:* "Send a help
    request when opened" in every note's and folder's menu, private ones included, on both apps (`Help.SWITCH`). Off by
    default, and turning it on asks first (the owner: "By default they are not, and request confirmation before being set"):
    a box that says what will happen, who receives it and the few words to send, and that, for an ordinary note or folder,
    the setting can be found by somebody who examines the phone's data, while for a private one nothing outside the vault
    shows it. Its one button turns it on; closing it leaves the switch off. *On opening* (the page or the card, once per
    opening, not each redraw): the request is sent at once, with nothing on screen changing. *Where it is kept:* for an
    ordinary thing in the notebook (schema step 47 adds `helpOnOpen`, `helpTo` and `helpWords` to `notes` and `things`, off
    for every row there is, this device's own, never sent, so a forensic look finds it); for a private thing inside the
    vault, in the things index beside its colour and place (`PrivateSpace.Thing.helpOnOpen`, decision 111), with no trace
    outside it. *Recipients and words:* one or more Mininotes contacts, chosen in the box, and a short message the owner
    wrote. *The place* (the phone, `HelpAlarm`): the last known at once, so the first cry carries whatever is already
    there, then the precise one when the GPS answers, then one every 5 minutes for an hour. No foreground service and no
    notification, because either would show: the sending rides on the main thread's own timer, which keeps going quietly
    for as long as the process lives. *What stops it:* an hour from when the thing opened, or the process ending. Nothing is
    logged: not the place, not who it went to, not the words. The location is asked for only when the first alarm is
    switched on, never before, with the reason in the app's words; refused, the request still goes, without a place. *The
    PC* has no GPS, so it sends one request with no place and the box says so (`Desktop.helpSendNow`). *The wire:* a card of
    its own, `MNH1` (`Help`), read before a note is as a Parlons! address is, sealed and signed like everything else so it
    looks like any other sealed message; it carries the opening's sixteen-byte incident, a number that climbs, the words,
    and a place or none, and every update of one opening shares the incident so the far end shows one alert and fills it in
    as the updates come. It is never sealed for a device that has not said it reads one (`Receipt.HELP`, 30, said and heard
    as `Receipt.REFUSALS` is), so a build from before is never sent it and never writes it over a note. *Receiving:* a loud
    alert, "HELP from <name>" (`Help.title`), the message, the place, the time and a map link (a `geo:` link and an
    OpenStreetMap `https` one, both with a full stop for the point whatever the language); on the phone a high-priority
    notification (a channel of its own, `IMPORTANCE_HIGH`) when the pad is away and a box in front, on the PC the tray
    message and a box, each filled in as the updates come. Tests: `HelpTest` (the card with a place and without, that it is
    a card and a card is not a note and a note is not it, the words cut to a few, a bad incident refused, broken bytes and a
    place out of the world refused, the map links with a full stop in France's language, the alert's words), `ReceiptTest`
    (the capability its own number, said and heard, and nothing taken for a leaving), `PrivateSpaceTest` (the setting kept
    in the vault and back whole, a thing never set staying off, turned off holding nobody). *Not run here:* the phone's
    menus, the location gathering and the high-priority notification, which need a device; and the PC alert box was built
    but not pictured.

114. **The owner, 2026-10-06: "in the private note, let's keep the same + at the bottom and we don't need the close, the back
    button should be enough, we should just have a design/icon signaling we are in private mode. Why it says 192 KB to go
    and then MB? We have to make sure that if we type privatenotes or private//: or anything related to the private elements
    in a shared element, this is not shared, how can we do that?"** Built on both apps (0.3.011). *The private screen like
    Home:* Home's own + at the foot on the right, the same round, colour, size and place (the PC's is Home's, `DesktopHome.round`),
    its menu in Home's words, *Note* and *Folder*, and in a note *From this device…*, which adds a file to it; hidden in the
    bin. No Close, on the screen or in ⋯: ‹ is always there and goes up a level, and at the top it closes, as the phone's
    Back and the PC's Esc do, and closing lets go and wipes as Close did. On the PC ‹ names where it goes, *‹ Mininotes* at
    the top. *The private look* (`PrivateSpace.GROUND`, `LOCK`): the whole screen on a quiet grey violet, dusk rather than
    paper, #ECE8F6 on a light paper and #201C2E on a dark one, a note with no colour on it too and a coloured one washed over
    it; and a lock, drawn rather than an emoji so it is one shape everywhere, #5B4B91 (#B9ACEB on dark), beside *Private*
    (the folder's name in a folder, *Private note* on the PC's note, *Bin* in the bin). The PC has the light paper only.
    *Saving words* (`PrivateSpace.saying`, one wording for both apps): never a byte count. "Saving privately… about 4
    minutes left. Keep Mininotes open.", from the batches still needed (`Slots.Space.batchesLeft`: every slot queued up to
    this space's last, fillers too, 16 to a batch, one batch a minute), "less than a minute" for the last, then "Saved";
    "Private storage is full" as before. The "used of" line is gone. *The guard at the door* (`PrivateCode.scrub`, shared):
    "private", a space or none, "note" or "folder" or neither, then //: touching, in any case, taken out with everything
    after it to the end of its line, where its password would be. It runs (1) in every save of an ordinary note
    (`NoteStore.save`, which every write of a note goes through, the note handed in left as kept), and before that in the
    page itself (`MainActivity.save`, `Desktop.scrubCodes`), so the page shows at once what is kept, the caret moved back by
    what went before it, the keyboard's copy of the word dropped on the phone and nothing for Ctrl+Z on the PC; (2) in every
    version kept (`NoteStore.keepVersion`, both); (3) in every parcel sealed (`Parcel.wrap`, title and body); (4) in every
    note that arrives (`Parcel.open`, and `NoteStore.landed` and `copied`, so a bare text from the oldest builds too); (5) in
    text handed to another app (the phone's *Send to another app* and a copy from an ordinary page, the PC's *Copy the
    text*). *The catching, closer:* a change that finishes a code with more after its colon (a short paste, a word a phone's
    keyboard hands over with its password) takes that rest with the code, to the end of its line, as the password, Enter
    and all (`PrivateCode.finished`; found on the phone, 2026-10-06: the password stayed in the note); and what comes while
    the password field comes up, from a keyboard that hands its whole word over again code and all, keeps only what follows
    the code (`PrivateCode.more`). The PC catches the same way. *Read me* never spells a code as one run: "type the word
    privatenote, then //: and your password, all together with nothing between them", which the guard leaves whole, and new
    lines on the look and on the guard; the 0.3.008 one, and the same as the guard leaves it once saved again, are said
    again with the new one, the old kept as a version without its codes; the PC now says Read me again too, as the phone
    did. Tests: `PrivateCodeTest` (the guard and the caret; a phone's page modelled as `PrivateScreen.watch` and
    `MainActivity.save` treat it, typed a key at a time, handed over as whole words both ways, pasted short with and without
    Enter and long, a save that cannot wait mid-code with keys and with a keyboard's word written back; a parcel sealed and
    one from a build before the guard), `PrivateSpaceTest.theSavingLineSaysMinutesAndThenSavedNeverBytes`, `DesktopGuardTest`
    (a PC note shared with a phone: typed, whole words, pasted short and long, a save mid-code, the title, a parcel from an
    older phone; the note, its versions, what is owed, seven sealed parcels, the phone's note and versions, the clipboard and
    every file of the notebook hold no code and no password; Read me teaches it after a save and is said again over both old
    ones; and no string in either app's sources, comments aside, is a run the guard takes, but the codes' own list and the
    old Read me kept to know it again), `DesktopPrivateTest` (the screen pictured with its + and its lock). *Not run here:*
    the phone's screen (the +, the lock, the ground in light and dark, Back), the phone's keyboard against the new catching,
    which need a device. *Left as it was:* a folder's name is not guarded (no code is caught there either), a selection copied
    on the PC with Ctrl+C is the page's own, and a save that cannot wait with "privatenote" at the caret and no slash yet
    still writes that word (decision 111).

115. **The owner, 2026-10-06: "In private, the + should also let us attach files From this device. Will they also be fully
    hidden and encrypted?" and "the alarm ... should be set in profile and all authorisation should be granted beforehand."**
    Built on both apps (0.3.012). *A note first, since the second part is a safety feature, not a hidden one:* the help request
    below is a duress alarm for the phone's own owner, like a panic button. It is set up and shown in the owner's Profile, uses
    a location permission the app declares honestly, and the owner can find it, change it and switch it off. It is discreet only
    at the moment it fires, so that someone coercing the owner then does not notice, which is the point of a panic alarm; it is
    not built to be undetectable to the owner or to a look at the phone.
    *Private files from the + at every level:* the private + now offers *From this device…* at the top of the space and in a
    private folder too, not only in a note (`PrivateScreen.plusMenu`, `DesktopPrivate.plusMenu`). The space keeps a file only
    with a note (decision 111), so a file added loose becomes its own private note named after it, in the folder it was added
    to; added in a note it is kept with that note, as before. The copy inside the vault is encrypted, compressed and shrunk if
    it is a picture, as decision 111. After a file is added from the phone's picker, Mininotes offers once *Delete the original
    from this phone?* (with the name), done with `DocumentsContract.deleteDocument` where the provider allows it, and says
    plainly where it cannot ("delete it in Files or Gallery"); the PC offers to move the original to the Recycle Bin
    (`java.awt.Desktop.moveToTrash`), and says so where it cannot. *Read me* gains one line: the original stays where it was
    unless you delete it, and the phone's picker keeps its own recent list. The old Read me is said again over the 0.3.011 one
    (`READ_ME_0_3_011` added to `READ_ME_BEFORE`); the line holds no code the guard takes.
    *The help request moved into Profile (reshaped from decision 113):* Profile gains a part *Help request*, one row *Help
    request…* opening a set-up page with a back button (`MainActivity.helpRequestSetup`, `DesktopProfile.helpRequest`): who
    receives it (the owner's paired contacts, switches), the message, a line saying what it does and that it is controlled here,
    and *Notes and folders that send it*, the ordinary ones chosen each with *Remove* and *Choose…* over Home's tree
    (`NoteStore.helpTriggers`, `wholeTree`). The switch *Send a help request when opened* is gone from every ordinary note's and
    folder's menu and lives only here, one place the owner controls; a private note or folder keeps its own switch inside the
    private space (decision 113), since Profile cannot see it while the space is closed. Who receives it and the message are
    this device's own, in its settings and on each chosen thing in the notebook, as decision 113 keeps an ordinary thing's, so
    a forensic look finds them. *The permissions, asked in set-up, not when it fires* (the owner: "all authorisation should be
    granted beforehand"), each with its reason and state and a way to grant it: the location, then background location ("Allow
    all the time"; Android 11 and later grants it only on the app's own settings page, so the owner is led there), and the
    battery exemption if the hour of updates needs it. *When it fires* (a chosen note or folder opens): the request goes to the
    chosen contacts at once, then every five minutes for an hour, discreet on the owner's phone (no popup, sound or vibration).
    The place is the phone's last known position only (`LocationManager.getLastKnownLocation`, the freshest across providers):
    no active GPS request, which is lighter and happens not to raise the location indicator, and the first cry goes even with no
    position known (`HelpAlarm`). *The updates survive the screen off* through `AlarmManager` waking a receiver
    (`HelpWaking`), set to fire even while the phone is idle, with the background-location permission, so they continue with the
    screen off. No foreground service is used (it would show a permanent notification), and that is a UX choice, not
    concealment. *What Android may still delay or stop:* an idle alarm may be delivered late, by up to about half again the five
    minutes, and Doze may batch it; and if the system reclaims the process between updates the live incident is gone and the
    beat stops until a chosen thing opens again (nothing of an incident is written to disk, so a private trigger's recipients
    never leave the vault). The PC, which has no GPS, sends one request with no place, as before. Tests: `DesktopHelpTest`
    (the store lists, adds and removes the ordinary triggers, by name; the box that sets a private thing's on, and the alert
    when one arrives, as decision 113). *Not run here:* the phone's Profile set-up page and its permission prompts, the private
    +'s new levels and the delete-original box, the AlarmManager updates with the screen off, and the PC's Help request set-up
    page, which were built but not pictured.

116. **The owner, 2026-10-06: "the password should be hidden with a trigger (eye or other) to see it when we access the
    private space. Could we rename it privatespace and have it a folder like already to which we can add everything, folders,
    notes, attachments. So is it that every time I type privatespace//: with a new password it will create a new independent
    space, and to access an existing one I type private//: with an existing password? When we display opening, there should
    not be the text cursor flashing. We have to make sure all private spaces open in the folder mode not in the note mode,
    even more for the new generated ones."** Built on both apps (0.3.013). *One trigger, renamed:* `privatespace//:password`
    replaces `privatenote//:` and `privatefolder//:` (`PrivateCode`, Kind SPACE and OPEN). If a space answers to the password
    it opens it; otherwise the password is asked a second time and a new space is made. `private//:password` only ever opens.
    The guard at the door (`PrivateCode.scrub`, decision 114) takes out privatespace//: and private//: and still the old
    privatenote//: and privatefolder//:, so a code left in a note by a build before the rename is caught just the same.
    *Any number of independent spaces (his choice over the safe two-halves model):* `Slots` no longer gives a space a fixed
    half with a stored room. Each password owns a deterministic pseudo-random order of all the data slots, seeded from its own
    key (`Slots.Space.order`, a Fisher-Yates shuffle driven by an HMAC-of-the-mark-key stream). It keeps its content in the
    slots it already owns, and when it needs more it takes the next slots in its order it does not already own, writing there
    even if another password's space happens to hold them (accepted overwrite, warned in Read me); it never writes over a
    space open at the same moment. Open at the same moment means both the slots a space already holds and the slots it has
    queued for the next batch but not yet written: a space being made or grown skips every such slot of every other open
    space, so two freshly made spaces, neither yet written, can never pick and then both come to own one slot (0.3.015; before
    this a space was skipped by its owned slots alone, which a space not yet written had none of, so two made while both open
    could share a slot and `erase` could then fill a slot the other still held). A space is found by trying slots and matching its per-slot mark, as before: no count, no
    room list, nothing that says how many spaces exist. The fixed-rhythm batch writing and the journal are unchanged; the
    two-halves, room, remembered-keys and New-private-space-from-inside code is gone, and a space is written as an index with
    no room or remembered keys (a space from a build before this reads back whole, its old trailing room left unread). *Opens
    as a folder, always:* a space opens on its grid, never a note, including a brand-new one, which is made empty
    (`PrivateSpace.empty`) and opens as an empty folder with the + ready (`show` no longer walks into a note). Notes, folders
    and loose-file notes all sit in the grid, as decision 115 keeps them. *The password has an eye:* the field where it is
    made, confirmed, opened and changed is masked with an eye at its right that shows the characters while it is held, on both
    apps (the phone a drawn `PrivateScreen.Eye`, the PC FlatLaf's own reveal button). *"Opening..." has no caret:* while it
    says Opening the field shows no blinking text cursor (the phone hides the caret, the PC is not editable and its caret is
    hidden), so it does not look as if it wants typing. *Read me* reworded: one word, privatespace; any number of spaces, each
    its own password; a brand-new one opens as an empty folder; and the plain warning that, because nothing records how many
    spaces exist, typing privatespace//: with a mistyped or forgotten password makes another space that can sit on an earlier
    one, so write the password down and open with private//:. It never spells a code as one unbroken run, so the guard leaves
    it whole, and the 0.3.012 one is said again over it (`READ_ME_0_3_012` kept in `READ_ME_BEFORE`). Tests: `SlotsTest`
    (three passwords, three independent spaces, each its own content, opened on its own, a wrong password changing nothing;
    rewriting one space a hundred and fifty times reusing its own slots and never growing without bound; three spaces writing
    the same batches, bytes and file size as a fresh install, with no plaintext; a space growing over a closed one accepted
    and the writer staying whole while the one sat upon is gone; a space's index and contents naming nothing of another),
    `PrivateSpaceTest` (a new space empty, notes, folders, files, versions and the bin come back whole), `PrivateCodeTest`
    and `DesktopGuardTest` (every way a code comes in, privatespace and the old two forms taken out of the note, its versions,
    the outbox, every sealed parcel, what the phone keeps, the clipboard and every file of the notebook; Read me still teaches
    it; no string the app writes is a run the guard takes), `DesktopPrivateTest` (two installs, one never used and one with
    three spaces and fifty photographs, after the same starts: the same files at the same sizes, the same batches and bytes,
    none of the words, passwords, photographs or codes in any byte; made from outside, the password asked twice; a wrong
    password not a byte changed; a backup opening on another PC; each space opening on its grid; the screen pictured).
    *Not run here:* the phone's screen (the eye held, the caret gone while Opening, a new space opening as an empty folder,
    Back), which needs a device.

117. **The owner, 2026-10-07: "there should be a way to delete the privatespace, in the menu there we should have a delete
    option. We need to make the menu there better, just bin and change password is not at the level of the general menu we
    have in the app. The bin must look better in there too; we have to understand that these are elements, they look like
    simple text."** Built on both apps (0.3.014). *Delete this private space:* the space's own ⋮ (the space's menu, not a
    note's) gains *Delete this private space…*, set apart at the foot in the warning colour. It asks first, plainly, that
    everything in the space is deleted for good, its password will open nothing after, and it cannot be undone (the 12
    recovery words do not cover it); on yes the space is erased and the screen closes to the ordinary app. *Erasing*
    (`Slots.Space.erase`): every slot the space owns is queued to become a fresh filler, sealed as the batch seals a freed
    slot (random under this device's own layer, so a filler is indistinguishable from a slot never used), its index slots
    FIRST so the newest index is gone before anything else and a cut-short erase can never be opened to anything whole again,
    the rest over the following batches at the same fixed rhythm. It does not burst-write the whole space at once, which
    would betray the timing: the fillers go out through the ordinary queue, sixteen to a batch. After it the keys are
    forgotten and the space is dropped from the open set once its fillers have drained; nothing records that a space was
    deleted, and it writes only over its own slots, never another space's open at the same moment. So a fresh install and one
    that made and erased a space leave the same file, the same size and the same write rhythm (decisions 111 and 116 kept
    whole). *The private menu at the app's level:* the space's menu and a private note's menu are drawn and grouped like the
    app's own ⋮ menu, not a plain text list. On the phone the ordinary Sheet draws itself onto Home, which is behind the
    secure overlay, so a menu panel of the same look is drawn inside the private screen itself (`PrivateScreen.Panel`): the
    rounded sheet over a faint scrim, small capital headings, grouped rows with hairline dividers, the colours drawn as the
    colours themselves with the one it has ringed, and the warning colour for Delete. On a note: its colours, then *This
    note* (Versions, Add a photo or file, Move to, the help switch, Delete); then *Private space* (Bin, Change password, and
    Delete this private space). On the PC the menus were already the app's own JPopupMenu; the space's rows are grouped and
    Delete this private space set apart with a separator. *The private bin at the app's level:* binned things are drawn as
    the same tiles Home and folders use (face or picture, colour, name), not text rows, each with Put back / Delete for good,
    and *Empty the bin…* as the one primary action at the top, styled as the app's own. Both apps, same words. *Tests:*
    `SlotsTest` (a space erased opens nothing after and leaves the file a fresh install's size with no plaintext of it, and
    another space untouched; erasing touches only its own slots, never another open space's). *Not run here:* the phone's
    private menu panel and the private bin's tiles (the +, the panel, the bin tiles, Delete this private space), and the PC's
    private menu and bin were pictured but the phone's screen needs a device.

118. **The owner, 2026-10-07: "Underline must not add space around the underlined run."** Built on both apps (0.3.017).
    An underlined word sits flush with the words around it. The marks stay in the words, so a note still says `__bread__`
    and Ctrl+U (and Underline in the phone's selection menu) still puts them in and takes them out, and they still travel
    with the note. They are not drawn, and they take no room: on the phone the span builder gives each underline mark a
    width of nothing (`Pad`, a span that measures and draws as nothing); on the PC the page lays its lines out so those
    two underscores have no width (`FlushMarks`), and the line is drawn under the words alone. Bold and italic are
    unchanged: their marks stay, small and faint, because they are not lines that run on past the word. Decision 102 left
    the room the marks take, because the page was one plain text area; this is that layout, for the underline only.
    *Tests:* `DesktopMarksTest` (the word's edges are the edges of the letters, the marks add nothing on either side, and
    one space remains before and after). *Not run here:* the phone's screen, which needs a device.

119. **The owner, 2026-10-07: "On the desktop, binning an element must not move the others."** Built on the PC (0.3.018).
    Dragging a thing onto the Bin, or onto the Archive, puts that one away and leaves every other icon in the cell it was
    drawn in, gap and all. The same rule as a carry (decision 39): nothing had a cell until something on its grid was
    moved, so taking one out let the rest close up. Putting one away now writes down where each icon stands first, the
    one leaving included, so the gap stays and putting it back puts it back there. A place's card only lists, and is
    left as it is. The phone's grid was not this path.
    *Tests:* `DesktopAwayTest` (Ideas onto the Bin, then Kitchen onto the Archive: each neighbour keeps the bounds it had).

120. **The owner, 2026-10-07: "Roles must be changeable after an element is shared."** Built (0.3.019). A role changed after the share was already kept here and already went with the list: Can read made Can write, the other side may write, and made Can read again it may only read. The same for a file shared on its own. What did not work was choosing it on the phone. The role list is a window dropped from the Share box, and a window with no paper of its own does not take a tap, so the tap fell through onto the box and the role stayed as it was. The list now has a paper, and it opens on the next beat, so the tap that asked for it does not also put it away. A role that is only shown, tapped to read what it lets them do, is the same window.
    *Tests:* `DesktopRolesTest` (a reader made a writer may write, then a reader again may not, on the list that really travels) and `DesktopFileShareTest` (the same for a file, on the sleeve that really goes). *Not run here:* a finger on the phone's Share box. The Android suite for 0.3.019 passed (`testReleaseUnitTest`, `lintRelease`, `assembleDebug`, `assembleDemo`).

121. **The owner, 2026-10-07: "One eye, not two, on the desktop private-space password."** Built on the PC (0.3.020). The private password box showed two eyes: FlatLaf's own reveal, which every password field has, and the one that shows the characters while it is held (decision 116). A private field turns FlatLaf's eye off, so the held one is the only one. The same for the two fields that change a space's password. The notebook's own lock is unchanged, and still has FlatLaf's eye, because it never had a second.
    *Tests:* `DesktopPrivateTest` (an ordinary password field still has FlatLaf's eye; a private one has none of it).

122. **The owner, 2026-10-07: "A shared photo must arrive in Shared with me, not loose on the desktop."** Built (0.3.021). A file shared through Share was already shown in Shared with me. A note or a folder from somebody else, and a file sent by somebody else, were still drawn on Home once they were here. Those now show in Shared with me. A note or a folder from somebody else still waits there to be accepted or refused. A file sent by somebody else does not wait a second time, because that sending was already asked about. A file or a note from one of this owner's own devices stays on Home, and Temp stays on Temp. Where Settings says Shared with me goes to Home, it does. Something that arrived inside a folder already here stays in that folder. Accept says where it really is: in Shared with me, or on Home.
    *Tests:* `DesktopFirstShareTest` (Ana's note and folder, once accepted, are in Shared with me and not on Home; one of the owner's own devices still lands on Home), `DesktopHomeFilesTest` (a file from one of the owner's devices stays on Home; one from somebody else is in Shared with me, already accepted, and search says so), `DesktopDropTest` (a stranger's files, once taken, are in Shared with me and not new on Home), `DesktopFileShareTest` (a file shared on its own is still in Shared with me), `DesktopLooksTest` and `DesktopLookTravelsTest` (a folder from somebody else, once accepted, is drawn in Shared with me), `DesktopSharedWithMeTest` (Accept says it is in Shared with me now).
    *Choice:* a file sent by somebody else (Send to a device, not Share) lands in Shared with me already accepted. A file sent by one of the owner's own devices stays on Home. Confirm this.

123. **The owner, 2026-10-07: "On mobile, in the main + let's add the camera so that a photo can be saved directly in a folder or a note."** Built on the phone (0.3.022). The + on Home and in a folder offers Take a photo. The picture is kept where that + is: in the folder, or on Home, and then in Temp, the Archive, the Bin or Favourites when the + was one of those, the same as a file from this device. On a note, the camera beside the paperclip keeps the picture with that note, and a folder's own file row does the same for the folder. The phone's camera writes the picture, it is copied in and made smaller as any picture is, and the copy left in the cache is deleted. Leaving the camera without a picture keeps nothing.
    *Tests:* the phone's + and a note's camera, which need a device.

124. **The owner, 2026-10-07: "In people and devices, we need to be able to rename a person or device, we should see the name they share, the name we pick locally or across my devices and we should be able to choose whether to display the one they share or the one we pick for them. the first setting should be the one on all devices and we should have an option for all me devices or just that one."** Built (0.3.023). People and devices, on the phone and on the PC, shows the name they share and the name we pick. Show chooses which of the two is the name everywhere else. On chooses where that is kept. All my devices is the first choice, and it is the one until somebody says Only this device. A name kept on this device only stays there. One for all of this owner's devices goes to the other devices, and the later choice stands. The name they share is still what they publish. Picking a name does not change that. This device's own shared name is the one your devices call it, changed on the same page. Typing a name shows that name. Clearing it shows the name they share again.
    *Tests:* `CalledTest` (the card, which name is shown, and the words), `CalledStoreTest` (a name set and read back, the later one stands, Only this device is left alone, a published name keeps the one we pick, a re-scan keeps it, somebody unknown is not made up, and a card about this device itself) and `SchemaMigrationsTest` (the columns arrive empty, and nothing already there is rewritten).

125. **The owner, 2026-10-07: "when sharing an photo diectly form my phone and opting to attache it to a new notes, the note is not saved, it is like all the work is lost."** Built on the phone (0.3.023). A photo shared in from another app and added to a new note is a note with a name and no words yet. That used to be treated as nothing written, so the note was never kept and the photo went with it. The name is enough. The note is written down, with the photo on it, and leaving it keeps it.
    *Tests:* the share sheet, which needs a phone.

126. **The owner, 2026-10-07: "instead of having the pages of the desktop compeltly seperated one from the other, the scroling should be progressive, up down as well as left and right, this will prevent having empty space, on some of the phone, it is not possible to move a noe or folder at the buttom of the screen because it seems it would take too much space with its name, let'S avoid that with a scroling instead of completly moving from one page to the other."** Built on both apps (0.3.024). Home is one surface. A scroll moves by pixels, left and right and up and down, and it can rest between pages, so a sparse page does not leave a blank screen. The page and cell a thing was put in stay as they were. The search and the favourites stay with the centre page, the one mostly in view. The last row is a place a thing can be let go, and one row more can scroll into view so its name clears the foot. On the phone that extra row can draw an icon that used to sit at the top of the next page. Carrying into the band at the edge of the room scrolls, and one empty page opens past the last while the thing is still carried. A key, or a double-click on empty room, still goes a whole page, or home.
    *Tests:* `LayoutTest` (the span of the pages that have something, and the cell under a point of the canvas) and `DesktopPagesTest` (a wheel notch rests short of the next page, further notches arrive there, and carrying to the edge scrolls onto the page beyond). *Not run here:* a finger on the phone.

127. **The owner, 2026-10-07: "In shared with me, the elements must be displayed as everywhere else, only by clicking on it we should have the fonctions to decide what we do with it, accept, refuse, see sender, than there if not in contact, block sender. Then in people we should have a list of the senders blocked and there we should be able to permenently delete them."** Built on both apps (0.3.024). What waits in Shared with me is an icon like every other icon there. A click offers Accept, Refuse, and See sender. See sender shows who sent it. Block sender is on that view, and only when they are not already in People and devices. People and devices lists the blocked senders, after This device, and Delete removes that record, after "They can share with you again." Notes already kept stay. A later message from a blocked address is dropped. The list is on this device only. A file still on its way, already accepted, still says it is coming.
    *Tests:* `DesktopSharedWithMeTest` (the waiting things are icons, the click is Accept, Refuse and See sender, and Block sender is there for somebody not in People), `BlockedStoreTest` (a later message is dropped, a note already kept stays, and deleting the record lets a later message in), `DesktopPeopleBoxTest` (Blocked follows This device) and `ReceiptTest` (the words). *Not run here:* the phone's Shared with me and its People list.

128. **The owner, 2026-10-08: "we have to avoid the gap between the elements line on the desktop, for that make the favorites band and the search bar floting above the elements underneath."** Built on the PC (0.3.025). The search and the favourites float over the icons. They no longer keep a band of empty room under the lines, so one line of icons follows the next at the same spacing on every page. What is under the bars is still there. A click beside the bars lands on the icon, and the last line can scroll up clear of them. The phone is unchanged.
    *Tests:* `DesktopPagesTest` (the icons fill the window under the bars, the lines stay one cell apart, and a point between the bars is not the bars).

129. **The owner, 2026-10-08: "make sure there is no empty gap, here between the 2 lines I cant place an elements."** Built on the phone (0.3.026). The band under the last line was padding, not a cell, so a thing let go between one page and the next had nowhere to land. A page's rows now fill its height. One line follows the next, and the room in a line is a cell. The + floats over the corner.
    *Tests:* `LayoutTest` (the rows fill the height, and what is left is shorter than a row).

130. **The owner, 2026-10-08: "it seems that I cant at all times drag and drop an icon, or even not at all, I cant manage to do it anymore, please fix that."** Built on the phone (0.3.027). After the rows were stretched to fill the page, Home kept laying the grid out again, because it compared a row's reading size with the taller row it had just drawn. Each time, the icon under the finger was taken away, so the hold never became a drag. A row stretched to fill the page is still that row. Home lays the grid out again only when the room changes, or the reading size changes, and not while the finger is down. A finger that goes down on an icon keeps it. A small move while the hold is starting does not turn into a scroll and does not give up the hold. A clear slide, before the hold has had its time, still moves the pages. Empty room still scrolls as it did. The rows still fill the page.
    *Tests:* `LayoutTest` (a stretched row is not a reason to draw the grid again; a short slide on an icon stays with the icon; a clear slide before the hold moves the pages; once the hold has had its time the icon keeps the finger; empty room still gives the page a shorter slide). *Not run here:* a finger on the phone.

131. **The owner, 2026-10-08: keep pages left, right, above and below, make them all scrollable, and combine the long-press menu with smoother dragging.** Phone implementation for Graphene testing in 0.3.028. Home has one scrolling surface. Rows keep the height needed by an icon and its two-line name; a page ends on a whole row, even when the last row is partly below the screen. The final row can scroll fully into view. At normal size each swipe chooses its main direction and stays with it. Android's momentum slows a flick naturally, and touching stops it. Pinched out, the surface still moves freely. Visible neighboring pages are real grids, so their icons can be opened or carried without changing the active page first. Blank room between occupied pages is usable too. Existing page and cell storage stays the same, and deliberate gaps are kept.
    A hold lifts an icon slightly and brings up its contextual menu. Releasing without moving leaves the menu open. Continuing to move closes the menu and begins the drag in the same press. The drag shadow follows the point where the finger picked it up. Edge scrolling runs on animation frames, starts gently and accelerates towards the edge; the destination cell follows the moving surface. A persisted placement settles with a short animation. The Pro is reserved for the Stables agent. This is a phone test build; the Windows interface and public download remain at their previous versions.
    *Tests:* `HomeMotionTest` checks row boundaries across viewport and text sizes, swipe direction selection, and graduated edge speed. On-device verification is recorded separately, in the verification log the maintainer keeps privately.

132. **The owner, 2026-10-08: landscape notes and keyboard need more room, double tap must return to the centre, and hidden search and favourites must leave all their room free.** Phone implementation in 0.3.029. Away from the centre, the search bar and whole dock, including its handle, leave the layout. The grid fills that space and can receive taps and drops there. Page rows use the full room below the header, so showing or removing the foot does not rearrange cells during a swipe. The overview also gets the free room. A double tap on empty Home space returns to page (0, 0), including from a partial scroll; the second touch no longer cancels the return animation.
    While the keyboard is visible in landscape, a note uses one top toolbar: back, title and save/error status, sync mark, Note tools, and the note menu. The separate sharing row and attachment strip give their room to the paper. Note tools contains Show files, Attach a file, Take a photo and Record audio; opening it closes the keyboard. Recording controls remain visible during a recording. Hiding the keyboard restores the ordinary rows. The editor retains its existing flags against the keyboard's fullscreen extraction, and keeps the cursor in view. Physical verification is recorded in the verification log the maintainer keeps privately.

133. **The owner, 2026-10-08: after a drop, the icon must not briefly return to its old cell before arriving at its new one.** Phone fix in 0.3.030. An accepted empty-cell Home drop moves the still-hidden source view into the destination grid and cell immediately, including across pages. Home updates its targeting maps at the same time. Drag completion reveals the tile where it landed while the storage worker persists the placement. The usual read-back reconciles the view; a write failure still reports the error and restores the persisted arrangement. Cancelled or rejected drops keep the existing return behavior. No storage or sharing format change.

134. **The owner, 2026-10-08: finish rotation and keyboard behavior, gesture edge cases, and drag feedback.** Phone implementation in 0.3.031. Home saves its page and fractional scroll position through activity recreation, and rescales that position when the grid dimensions change. A note saves its selection and the character at the top of its reading area, rather than a pixel offset tied to the old line width. Keyboard visibility is restored only for a note being written; resizing keeps the caret visible while writing and keeps the reading position otherwise. Saved view state contains offsets, not note text.
    Touching an icon during momentum stops scrolling and lets the same press open its contextual menu or become a drag. Back cancels an item in the hand before leaving the underlying screen. Pickup adds a short system haptic that respects the phone's feedback setting. Empty destinations have an accent outline and a light fill. Existing occupied-cell meanings and confirmation rules are retained. Verification and remaining limits are recorded in the verification log the maintainer keeps privately.

135. **The owner, 2026-10-09: a visible scrollbar handle, long-note typing, and Back from a folder to Recent.** Windows 0.3.029 keeps its scrollbar handle visible at rest and strengthens it on hover and drag, against both light and dark paper. On both apps, a folder opened from a listing keeps that listing in the browsing trail. Back returns to Recent, Temp or Shared with me and restores its scroll position; the folder's physical parent and move destinations remain unchanged. The desktop Recent sidebar also opens folders with Back to Recent. Android 0.3.032 limits ruled-paper drawing to the visible area, reparses formatting on edited lines, and removes obsolete hidden underline spans. Existing body limits remain 24,000 UTF-16 code units. Verification records distinguish local checks from anything observed on Graphene; no phone installation is implied.

136. **Measured on Graphene, 2026-10-10: typing in a long note with marks was slow because of the marks.** Each letter moved every style after it, and Android laid a paragraph out again for every moved bold, italic or mark span; a line's restyle also took off and put back every span on it. The phone editor now lays a note's marks out once when opening it, keeps moved-only spans from relaying the layout, changes only spans that differ, restyles only the lines a Bold/Italic/Underline toggle changed, and draws a mark as one small faint span. Writers' colours draw beneath marks. Nothing stored or shared changes. On a 24,000-character synthetic note with 320 marked words, a letter typed near the top went from about 124 ms to 18 ms (26 ms composed as a keyboard does). Not yet in a numbered version.

137. **The owner, 2026-10-10: send a file through any other app, download it on the phone, and open it.** Every kept file, in a note or on its own, has Open, *Send with another app…* (the phone's own share sheet, lent read-only as Open lends it), *Download* (a copy in the phone's Downloads with nothing asked; before Android 10 the picker asks where) and *Save a copy…*. Not called Share…, which is people and their rights (decision 92). Private spaces keep Save a copy as the only way out. The PC keeps Open and Save a copy; it has no share sheet.

138. **The owner, 2026-10-10: a file shared to Mininotes can go anywhere, not only to a short list.** The box that asks where it goes keeps New note first and the recent notes, and gains *Choose a place…*: Home, then any collection opened with a tap (›) and left with ↑, any note in it added to with a tap, and the place looked at taken with its one button: *New note here* for words, *Put it here* for files on their own, kept there as files. Read-only places and notes are not offered.

139. **The owner, 2026-10-10: "I don't see why I have to push it manually when it is set to go automatically."** The phone sent a note's changes only if the note was still open when its wait ended; leaving it sooner meant nothing went until Sync now. Now every note written is queued with its own time, open or not, and one that could not go is tried again a minute later. Coming back to the app, on the phone or the PC, sends what is owed and asks the others for what they have, not more than every two minutes.

140. **The owner, 2026-10-10: typing on the PC moved the rest of the text and seemed to lose it.** The PC's page view (decision 118) kept each line's row breaks as places in the whole note, and writing above a line moved it without telling it: lines below a keystroke were laid out from where they used to be. Each line now follows its own place. A test types, deletes and adds marks 200 times in a long marked note and checks that the lines below move only as far as the paragraph written in, and that every letter is found where it is drawn. The line saying Saving… keeps its height whether it says anything or not.

141. **The owner, 2026-10-10: a contact's letter does much more than set a colour, and Parlons! is one tap away.** Tapping a person's round over a note, or beside them in a list, opens their menu: their name, *Chat on Parlons!* where their address is known, or *Parlons! address missing* greyed with *Add their Parlons! address…*, then *Writing colour…* and, on a note, *Sharing…*. The button that was Contact on Parlons! is Chat on Parlons! everywhere. A chat inside Mininotes is for later.

142. **The owner, 2026-10-10: a note can have several tabs, kept as sections in its words.** A line that says `=== Ideas ===` begins the tab called Ideas; words before the first such line are a first tab, Page 1. A note is still one text, so saving, merging, sharing, backups and the 24,000-unit limit are unchanged, and an older build shows the tab lines as headings. Adding a tab to a plain note makes its words Page 1. The last tab is never removed. The rules are in `Sections`, shared by both apps and unit tested. The page holds the open tab's words only; saving, merging what arrives, who wrote what, sharing the text and the length cap all use the whole note, put back together around it, so only switching tabs never writes anything.

143. **The owner, 2026-10-10: the note's head is design C.** On a band a shade darker than the paper: the way back, then who the note reaches (each round a tap to their menu) and its menu. Under it on the same band, the tabs as index cards, the open one the paper itself, then + for another; a note with no tabs yet says *+ Add a tab*. On the paper: the note's icon and title, and one quiet line saying where it lives and what its writing is doing, as tall saying nothing as saying Saving…. A tab is held (phone) or right-clicked (PC) to rename or remove it; removing asks first. A short window or a landscape keyboard folds the tabs and that line away.

144. **The owner, 2026-10-10, on 0.3.033: the title above the tabs, bold without its stars, and a missing Parlons! address that can be added from where it is said.** The note's title and the line under it move up onto the band, above the tabs, so the open tab leads straight into the page. Bold and italic marks are hidden and take no room, as an underline's always were (decision 118): the words say `**bread**`, the page shows **bread**. Ctrl+B, Ctrl+I, Ctrl+U and the Bold, Italic, Underline menu still put them in and take them out. *Parlons! address missing: add it…* is one row in a person's menu, and a tap opens the box that takes the address.

145. **The owner, 2026-10-10: "we should even have an inbox in the app, so that people can send brief messages, like please send me your Parlons! address."** The *Inbox* (first called Messages; the owner asked for Inbox the same day) is a place on Home, beside Shared with me, wearing how many messages are not read. Like Temp, Recent and the others it has its switch under Show on Home; switched off Home it is in ⋮. It holds one conversation for each person, newest first, and *New message* chooses whom to write to among the people paired here, or *New person…*. A conversation shows both sides, each message saying Sending… until its far end says it has it, then Delivered; the field takes 280 characters. *Ask for their Parlons! address* sends the request in one tap; it arrives with *Send my Parlons! address*, which sends your own (asked for once and kept on the device) and arrives with *Save it to <name>'s card*: a contact's address is never changed without a yes. A person's round, their page in People and devices and their line in a list each have *Message…*. On the wire a message is its own card (`MNM1`), sealed and signed like everything else, sent only to a device that has said it reads one (receipt 32), and kept at its far end before it is answered (receipt 33, with the message's id); one not answered is handed on again after 1, 2, 4, 8 and then every 15 minutes. Blocked and unpaired senders are dropped as for anything else. The notebook gains `messages` and `mine` (schema 49). A message is not sent through Parlons! itself: Mininotes has its own Maxima identity, so a Parlons! contact would see a separate "Mininotes" contact; that bridge waits for its author.

146. **The owner, 2026-10-10: "for sending an attachment, we should have our contacts but also the possibility to send to a new person; in every path we should be able to do everything."** A first pass over every place somebody is chosen. Send files lists the devices paired here as before, then the people known and not paired yet, greyed, a tap to pair, then *New person…* (scan their code, paste their link, share my link, show my code); with nobody paired it is no longer a dead end but says so and offers New person…. The same rows end the share-in box's *Or send to*, now shown whenever files come, and *New message*. A person's page has *Send files…* (straight to them) and *Message…*. The PC's file menus gain *Download*, a copy in the Downloads folder with nothing asked, as on the phone. Still to do, from the same review: after pairing, coming back to the list with them chosen; *Share…*, *Move to…* and *Put in another note* on a note's attachments; sending to a group at once; the PC's Share-with box offering *Copy my link*.

147. **The owner, 2026-10-11: "we need a proper chat or nothing at all, better nothing at all, and link easily to Parlons!; remove the inbox and work the Parlons! address setup so that it is easy and intuitive."** The Inbox (decision 145) is gone from both apps: no place, no menu, no Message… rows. A 0.3.036 build that still sends a message has it kept and answered, never read as a note, and shown nowhere; this build no longer says it reads them, so nobody starts. The `messages` table stays, unused, as a shipped migration must. Instead: *Your Parlons! address* in Profile, set once (filled in by itself when an address was just copied, with *Open Parlons! to copy it*), and sent by itself to everybody paired, as a Parlons! card about the sender's own key, only to builds that said they take one (receipt 34, said until said back). On their side it lands on your card, the later decision standing, so *Chat on Parlons!* works with nothing pasted; on your other devices it is your address too. A contact's box says theirs arrives by itself once they set it, or takes one pasted, also filled in from what was copied. Address fields show the shape `Mx…@…`, nobody's real address.

148. **The owner, 2026-10-11: from a contact at the top of a shared note, their profile page.** A person's menu, from their round over a note or beside them in a list, begins with *<name>'s page…*: People and devices opened on them, where they are renamed, their colour and Parlons! address set, their groups and what they have changed, with ‹ back to its list.

149. **The owner, 2026-10-11: a note made with Temp's + on the PC and shared with the phones went onto their Home, not into Temp.** Saving a note writes its whole row, and its time to be gone was not among what a save carries over, so the first words written made it an ordinary note again. A save now keeps what is not the page's to say: when it is to be gone, how long it waits before it is sent, where it stands on Home, the icon and picture this device gave it, and when it was last opened.

150. **The owner, 2026-10-11: "the temp folder should also be shared at the top level, so that I can always drag and drop an element like a screenshot and have it shared directly with the selected devices."** Temp's *Send to my devices* gains *Choose devices…*: every device of mine, as before, or those ticked, kept on each device. On the PC, files dropped on Temp's icon on Home, or into its open card, are kept in Temp and so go to those devices at once (they used to land on Home). On the phone, the box that takes what another app shares (a screenshot) offers *Temp: to my devices*, which does the same; words shared there make a note in Temp.
151. **The owner, 2026-10-11: "we have to make sure we can share Temp not only with my devices but also with other people."** Temp's *Send what is in Temp* (it was *Send to my devices*) has *Who it goes to…*: my devices, all of them unless some are unticked, and below them the people I am paired with, none unless ticked. People get what is let go on Temp to read, with Temp's time, so it is gone from them when it is gone from me. *Sync now with my devices* is *Sync Temp now*. Temp's *Also show on Home* now covers notes and folders let go on Temp here too, not only files from my other devices: off, they are in Temp only. On the phone, back from a note opened in Temp, Recent or Shared with me is that card again, not an empty page with its name.
152. **The owner, 2026-10-11: "the setting at a level like the folder should apply at everything in it, notes and subfolders, as long as it is not overwritten in the elements; this should apply everywhere."** Already so: sharing and rights (a folder's people reach everything in it, at any depth; an element can add people or raise rights), the wait before sending, and pausing what comes in. Made so now: a note or a folder made inside a temporary folder is temporary with it. Colour, strength and icon: decision 153. An element taking away a person or lowering rights its folder gives: agreed by the owner to be planned on its own (below).
153. **Colour, strength and icon follow the folder (the owner, 2026-10-11, "yes").** A note or a folder made in a folder wears its colour, strength and icon. Changed later on the folder, here or arriving from another device, they are followed at any depth by whatever never had its own decided (colour and strength) or still wears the folder's old icon with no picture. One given its own keeps it, and what is in a folder given its own follows that folder. Nothing extra is sent: each device follows when the folder's look reaches it.

154. **The owner, 2026-10-11: "a right-click or long push on an element in Recent should give a full contextual menu, including the option to take the element out of Recent; make sure everything across the app has a contextual menu."** In Recent a thing's menu is its whole menu with *Remove from Recent*, kept on each device until it is opened or written in again. Rows that had none now have one, on both apps: the open things (the overview cards on the phone, the open list on the PC: Open and Close on the phone, the thing's menu on the PC), This device (Profile), blocked senders (Delete), My devices (Open), members and those who could be added to a group (Their page, Remove or Add), what is shared on a person's or group's page (Who has it…, Remove), and on the PC the rows of Share with. Not yet: the all-pages view and the line under a note's title saying where it lives.
155. **The owner, 2026-10-11: "the screenshot shared in Temp from the laptop says it is synced but it is not on the Graphene", then "the screenshot from the Graphene shows a green tick but the laptop an orange arrow".** A file of mine shared on its own stays amber until every device it went to has said it has its bytes, not only its notice. One that came from another device of mine owes nothing to my other devices, bonded or only paired as mine (the owner's are paired, not bonded, so the laptop stood amber, owing it to the Pro). A fetch that fails logs why; bytes left by a failed try are cleared. Sharing into Mininotes on the phone: *Choose a place…* offers, at every level, a new note there with the files in it and a new folder, named and then opened; and Temp at the top.
156. **The owner, 2026-10-11, on the phone's share box: "too confusing ... it must be really simple, it must be possible to attach the file to anything, it must be beautiful".** *Add to Mininotes*: what came in a card of its own (a picture's thumbnail, its name under it); one button, *New note with it*; then three ways, each its icon, its name and one quiet line: *Temp*, *Into a folder or a note*, *Send to someone*. The place chooser: *Put it here*, *New note here*, *New folder here*, and at Home Temp and Recent, then every folder (in) and note (added to) with its own face; Back goes up a level. A subject alone (a screenshot's "Screenshot (date)") keeps it a file.
157. **The owner, 2026-10-11: "this is great and shows our UI can be improved to this level; review the whole app ... minimize the use of text in our menus and use symbols, since we will soon come with the app in multiple languages."** The share box is the model: a row is its symbol, its name in a word or two, and at most one quiet line under it; one primary button a screen; a value goes under its name, never squeezed beside it. Every menu row wears the symbol of what it does, the same on both apps, from one list (Symbols.java), found from the row's words; a name (a person, a file, a folder) wears none. Done first: symbols in every menu on both apps (the phone's sheets and pop-ups, the PC's menus), and every row and drop-down on the phone with its value under its name. Next: explanations shortened or behind an ⓘ, Settings, Profile and People first; then the PC's rows the same way.
158. **The owner, 2026-10-12: "for the contacts we should be able to define a picture for them, or in the Profile set a picture, and when we click a contact, as at the top of a note, we should see their picture in big."** Profile has *Your picture*: chosen, made small as a thing's picture is, and sent by itself to everybody paired, as your Parlons! address is (decision 147): a Face card (MNY1) about the sender's own key, sent only to a build that has said it takes one (Receipt 35), the later decision standing; from one of your own devices it is yours too. A person's page has *Picture…*: one picked here is worn over theirs and never sent; taken off, theirs is worn again. Every round wears the picture where there is one, its initial where not: under a note's title, on People's lines, in Profile. A tap on a round shows the picture big first, and on the phone a tap on that shows it over the whole screen. Schema 50: addresses gain face, faceDecided and faceHere.
159. **The owner, 2026-10-12: "when we choose a picture we should be able to reframe it, and it should be displayed on the profile page."** A person's picture, chosen, opens first in a round window: moved with a finger, made bigger or smaller with two, turned a quarter at a time; Use keeps what the round shows. Profile shows your picture big at its top.
160. **The owner, 2026-10-12: "both" (the app in several languages, and a note translated on the device).** The sources stay in English; every word put on the screen - a label, a row, a menu line, a button, a box, a hint, a drop-down's choices - is looked up by its English in a flat list per language (words/en.json, words/fr.json: the shape Weblate keeps), and said in English where a list has nothing. A sentence built around a name ("Take Ana off Courses?") is matched against the list's templates ("Take {0} off {1}?") and said with the name kept. A symbol is still found from the English. Language is in the main menu (the owner: "put directly in the menu so people do not need to look for it") and at the top of Settings: the device's own by default; on the phone it changes at once; on the PC it asks, saves and opens Mininotes again in it (the owner, 2026-10-12: "switching in French on desktop does not work"), and the choice is also kept beside the notebook so the next start draws in a face with that script (Nirmala UI for Hindi, Microsoft YaHei UI for Chinese, Yu Gothic UI for Japanese; each language name in the menu in a face that has its letters). The app's own places (Temp, Favourites, Bin, Archive, Shared with me, Recent, Home) are named in the language chosen, while a folder, note or file keeps the name its owner gave it (the owner, 2026-10-12: "why are the system folders not translated in Chinese?"). Persian (the owner, 2026-10-12) is the first language written from right to left: with it chosen, the phone lays every screen out mirrored and the PC mirrors each window as it opens. tools/extract_words.py collects every phrase into en.json and adds new ones, untranslated, to the other lists. French: every phrase on screen. Next: a note translated on the device, with Bergamot (Firefox's own engine), nothing sent anywhere.
161. **The owner, 2026-10-12 and 13: a note, or what is chosen in it, translated on the device, with Bergamot; the packs from a copy on our GitHub release; the result in a box; French, Spanish and Chinese first.** Translate… is in a note's menu, under This note on its page, and on the page's right-click when words are chosen (then only those). The box says the language it guessed and the one it puts it into (the app's), both changed there; Copy, then Replace for what was chosen or Add as a tab, named for the language, for the whole tab. The engine (bergamot-translator, MPL-2.0, built from open-source parts only by tools/build-translation-engine.ps1) ships beside the PC app and is started for each translation, then closed, so its memory is given back; a pair with no English goes through English, two runs. Packs are downloaded the first time two languages are wanted, the size said first, each file checked against our list (translation-packs.txt on the release translation-packs-1, made by tools/translation_packs.py from Mozilla's); Settings, Translation lists them and removes them. Lines, list marks, checkboxes, empty lines and marks around a whole line come through; marks inside a line do not yet. The phone follows (docs/TRANSLATION.md, phase 3).
162. **The owner, 2026-10-13: notes stay inside by default, and a note or a folder can also be kept as Markdown files anywhere on the PC, an external drive included, kept up to date by Mininotes, shared notes too; a lost file is said clearly; the files are not encrypted, and that is accepted; a private space is never written out.** Keep as a file… on a note's menu writes it, in a folder chosen, as Title.md: a short header with the note's id (`mininotes: <id>`), then its words as typed; underline as `<u>…</u>`, a tab line as `<!-- tab: name -->`, everything else as it is, and read back to exactly the same words (NoteFile, shared by both apps). Its files go in Title.files beside it. Keep as a folder on disk… writes a folder and all in it as a Windows folder of such files, its folders as folders, both ways: a folder made in Explorer becomes a folder, a .md put there becomes a note, a note's file moved to another of its folders moves the note, a folder renamed there is renamed here, and the same done in Mininotes is done on the disk. Open a Markdown file… in the main menu, or a .md dropped anywhere but on a note, makes a note kept as that file, where it is. Mininotes writes the file whenever the note changes, typed here or arrived from somebody; Windows says when a watched folder changes, every file is looked at again when Mininotes starts and every half minute, and a file changed outside is taken in as the owner's own edit and sent on as any edit is. Changed on both sides since Mininotes last wrote it, the file's words are taken in and Mininotes' own stay in Versions; a note read only here keeps the file's words in Versions and the file says the note's again. A file renamed or moved beside where it was, or anywhere in its folder kept on disk, is followed by the id in its header (renamed, the note takes the new name; renamed in Mininotes, the file does). One that cannot be found is lost: a warning mark on its icon (a file mark while all is well) and a line over the note saying what happened, with Keep it inside only and Find it… (the file picked is taken only if its header says it is this note); on a drive that is not connected it says so ("on E:, not connected") and comes back by itself. Show in Explorer and Keep it inside only are in the menu of what is kept; keeping inside only, or deleting a note for good, never deletes a file. Where each is kept is this PC's own, in the notebook (table ondisk, schema 51), never sent. A private space and anything in it, and a notebook while it is locked, are never written out: there is no menu line for them and the writing refuses them, and the guard against codes (decision 114) stands at this door too. The phone follows (docs/FILES.md, step 5).
163. **The owner, 2026-10-14, answering the connect review (docs/CONNECT-REVIEW.md): connecting with somebody and sharing a thing are both an invitation, accepted or ignored; keep `mininotes://`, no website; my contacts may see which devices are mine; a device of mine may be one that only reads; "Add someone" and "Add my device"; a Remove this person; and the three bugs first.** Fixed first, in 0.3.057 (both apps): on the phone, a code scanned from a Share with box led to People and devices and the thing was forgotten; it comes back to that box once paired, as the PC already did. On the phone, any picture of a code that was not a Mininotes code (a web address, a ticket) was kept as a contact nothing could reach; it is refused: "That is not a Mininotes code. Nothing was kept." (the PC refused it already). And the sender's tick said everybody has it while what was shared still waited, hidden, in the other person's Shared with me: a device where something waits now says so beside its answer (Receipt.WAITS, 36), and says it was accepted once it is (Receipt.ACCEPTED, 37), by Accept or by one of its owner's devices accepting it, again on every round until it goes and only to a device heard answering; the sender keeps which it last heard, per device and per note or file, in its settings (Asking; nothing in the notebook, no schema step), and draws Sent, "Sent · waiting for them to accept" in the Share box, "Waiting for Ben to accept it." on their round, and "waiting for the others" under the note, until it is accepted. A refusal forgets the wait. A build from before neither says nor hears either number and its tick stays as it was. *Tests:* `AskingTest` (the lists, the two numbers, the mark and the words), `DesktopAcceptingTest` (through Post.arrived: waits, the mark and the words, accepted makes it green, a late wait changes nothing, a refusal forgets it, kept while the other device has not been heard answering). *Not seen:* two real devices.
164. **The owner, 2026-10-14, with a picture of Temp on the laptop: "the 1 in Temp is not well displayed", then "as we have the mention new on elements in Temp, let's have the same everywhere a change hasn't been viewed, on notes when an update is shared".** Built in 0.3.057, both apps. *The count:* a place's count (Temp, Shared with me, Archive, Bin) was a pale pill on the tile's rim with its number set low, lost on a coloured Home; it is ink with the number in paper, bold, centred by the letters' own height, and kept whole inside the tile (`DesktopHome.pill`; the phone's HomeScreen the same). *New:* a note somebody else changed (new to this device, newer, or put together with what was here; not what one of the owner's own devices wrote) wears "new" until it is opened here, and so does every folder it is in, however deep; a note open as the change arrives is seen at once. The files' "new" is the same pill, and all of them sit over the middle of the top edge, clear of the mark on the corner. Kept on this device only, in its settings (`NoteStore.unseen`, `seen`, `unseenNotes`), never sent, nothing in a note's row. *Tests:* `DesktopAcceptingTest` (a note from another person is new on Home until opened, its folder too, and a note from my own device is not). *Not yet:* Recent and the tree do not say new.
165. **The owner, 2026-10-14: "when we are in a note, we have to be able to access easily the sharing settings of it, like the update frequency ... put it in the section This note, and call it Sync parameters, also there put sync now as the first object and versions can be put down in the list. In the Mininotes section of the menu, put share mininotes at the top and version just above About."** Built in 0.3.057, both apps. *This note* (and *This folder* on the phone) begins with *Sync now* (on the phone only where it reaches somebody, as before) and *Sync parameters…*, which opens the thing's Share box, where its Syncing part is (how long it waits, receiving); *Versions* is lower, after Temporary on the phone and after Translate on the PC. *Mininotes* begins with *Share Mininotes*; *Version · v0.3.057* is just above *About* (the PC gains that line; clicked, it looks for a newer one, as the phone's does). *Not seen:* the menus on a screen.
166. **The owner, 2026-10-14: "on top of folders like Temp, we should have an arrow there to sync now, or even better, everything should be sync as soon as possible, at least when we are out of a note ... the user likes it as quick as possible"; then, offered three changes and a fourth to decide, "build all these".** Built in 0.3.058, both apps. *Leaving a note sends it:* going back to Home, opening another note or folder, or putting the app away (the PC: its window losing the front) sends what the note owes at once instead of after the rest of its wait, also when its last words are still being written down; a note set to go only when asked is left so (`sendOnLeaving`, `hurry` on the phone, `hurried` on the PC). *Heard from, sent to:* anything signed that arrives from a device says it is there, so everything it is owed goes then, through `Post.send` for that one address, instead of at its next turn, which after a few tries was a quarter of an hour (`Post.heardFrom`, once a minute a device at most, only while the node is already running, never something carried). *Temp's mark:* Temp's card wears the sync mark, the worst of what is in it that reaches anybody (`NoteStore.markOf` and `pagesUnder` for Temp; its files in what waits); amber, a tap says what waits and sends it; otherwise a tap is Sync now, sending each thing in Temp and asking the others for theirs. *Not built, waiting for the owner:* keeping the phone listening while locked, which needs a notification that stays. *Tests:* `DesktopSyncStatusTest` (Temp's mark: only here, waiting, everybody has it, a note not in Temp not counted; what it owes and who is in it). Leaving a note and hearing from a device are not exercised by a test; neither is seen between two devices.
167. **The owner, 2026-10-14, with a picture of a note made with Temp's + on the laptop, its bar saying ← Home and Home: "it saves it on home, please make sure it saves it in the temp folder".** Built on the PC in 0.3.059. A note made in Temp is kept on Home with its time running, as Temp's things are, and was in Temp; but the PC gave it a place on Home's grid all the same, and its bar said Home. Now nothing made from Temp's + is placed on Home's grid while Temp's things are not shown there (`Desktop.shownOnHome`), the line over a note in Temp begins with Temp (`shelfLine`), and its way back says Temp and opens Temp's card. The phone has no such line, and its way back already goes where the note was opened from. *Tests:* `DesktopTempNoteTest`. *Not seen:* on the laptop's screen.
168. **The owner, 2026-10-14, three from the laptop: "the menu is full screen on desktop" (a picture: an Undo naming a note whose first line was a paragraph); "let's make sure the elements on the home page don't move when we drag an element in the bin or archive, the spot left behind should stay empty"; and, of the strip a carry shows, "the top menu elements writing is not sharp".** Built in 0.3.059. *The menu:* Undo names a thing by its first 32 characters on one line, and every menu line on the PC is kept to one line of at most 60 characters (`DesktopMenus.shortly`), the whole of it under the pointer. *The spot left behind:* an icon never placed by hand had no cell of its own and took the first free one, so when one went to the bin, the archive or Temp the others moved up into its place; now, as it is let go there, everything else on that grid is written down where it is drawn (the PC's `holdOthers`, the phone's HomeScreen the same). *The strip:* its words are drawn with Windows' own text settings, ClearType included, as the icons' names are. *Tests:* `DesktopMenuLineTest`; a drop on the bin keeping the others in place is not exercised by a test. *Not seen:* on a screen. Also: `DesktopTempNoteTest`, overwritten by mistake that day, was put back from git; the new Temp test is `DesktopTempPlusTest`.
169. **The owner, 2026-10-14, testing 0.3.059 on the phones and the laptop: several files at once; Temp's tile without its mark while the top said 2 not synced; "new" at the folder level and at the top, and the top marks listing what they mean; the favourite's star centred in its round; the version in the PC's menu and an Update at the top as VS Code has it; "Remove from the app" on a note kept as a file; and a Live note whose deleted words came back.** Built in 0.3.060. *Several files:* the phone's picker already takes several, but only when the first is held; it now says so as it opens. *Temp's tile* wears Temp's sync mark (the worst of what is in it, decision 166), its count beside it, on both apps. *New* climbs: a folder holding a file that came and was not opened is new, as one holding such a note is; Temp's tile is new while anything in it is; and "new", with how many, stands at the top of Home on both apps, a tap or a click listing what is new, each opened from there (`NoteStore.whatIsNew`, `foldersWithNew`, `tempHasNew`). *What waits:* the box an amber mark opens lists the notes and files that wait, each opened from there (`SyncStatus.waitingThings`), on both apps. *The star* is drawn, centred by its outline, on both apps (the phone's was the font's ★). *The PC's bar* no longer shows the version, which is in the menu above About (decision 165); Update stands at its right, beside Share, only while there is one, and a check asked for says so on the status line. *Remove from the app*, beside Keep it inside only: no longer kept on disk, the note or folder to the bin, the file left exactly where it is. *The Live note:* the logs show a merge, not a typing fault: two devices had put each other's copies together rather than agreeing, so the text both last had was older than the words since deleted, and the PC kept them as its own and sent them back. Not changed yet: to be reproduced between two notebooks first. What was changed: a device heard from is sent what it is owed only when it had not been heard from for two minutes (decision 166 sent on every message, the same revision two or three times in seconds). *Favourites in Show on Home:* a test of the pinned menu found the tick, the switch and the icon agreeing over three clicks; the owner then said it is shown when unticked and taken off when ticked; every line of Show on Home now reads its tick from the setting each time it is drawn, so the tick is the setting itself (0.3.060). *Tests:* `DesktopSyncStatusTest` (what waits and what is new listed), `DesktopFavouritesSwitchTest`. *Not seen:* any of it on a screen.
170. **The owner's Live note, 2026-10-14: "on Graphene in Live note, I deleted what I typed but it came back multiple times, now it seems ok after I close the note and reopen".** Found and fixed in 0.3.061. A device writes down what it sends "as it went", to weigh the next thing the other writes against; it read the note again from the notebook to do so, and a note being typed in had moved on by then, so the text that went was missing at its own revision. What the other device then sent was weighed against an older text, one without the words since deleted, and they came back as if the other device had written them. What goes is now written down exactly as it went, at the revision it went at (`Post`, `keepVersion` with the parcel's own words). Each weighing also logs, in counts and never in words, how it went and whether the text agreed on was found at its own revision (`Mininotes/Weigh`), so another case can be traced. *Tests:* `DesktopDeletedStaysTest` (the deletion stands; the old way, the line comes back), with the arriving, three-device, merge and sync tests of both apps. *Not seen:* between two real devices.
171. **The owner, 2026-10-14, on a note kept as a file that changed in Mininotes and in its file since Mininotes last wrote it: "let's ask first" (docs/FILES.md).** Built on the PC in 0.3.061. It is no longer decided by itself (the file's words used to win, Mininotes' going to Versions): nothing is written over, the note's icon wears the warning mark, the status line says "Plan.md changed here and in its file. Which words should it keep?", and the open note's strip asks the same with *Keep Mininotes' words* and *Keep the file's words* (filled). Either way the other words are kept in Versions, and the next look does the rest the usual way (`DesktopOnDisk.ASK`, `settled`). Asked once, not on every look. *Tests:* `DesktopOnDiskTest` (asked, nothing written over, either answer, the other words in Versions). *Not seen:* on the screen.
172. **The owner, 2026-10-14 and 15: a file only opened, worked in and closed, not kept in Mininotes, "removed completely" once closed (docs/FILES.md).** Built on the PC in 0.3.062. *Edit a file…*, under Open a Markdown file… in the menu, opens each file picked in a window of its own, as Notepad does: its words, saved into it a moment after typing stops, on Ctrl+S and on closing, its own line ends and mark kept, no Mininotes header written into it, no note made and nothing synced; closed, nothing of it stays here. Changed outside while open, it asks which words to keep (Keep the file's words, Keep my words), as a note kept as a file does (decision 171). A save that fails keeps the window open unless the owner closes it anyway. Made a window of its own rather than a hidden note because a note, even hidden, would write a header into the file and go to the owner's other devices while open. *Tests:* `DesktopEditFileTest` (line ends, mark and no header). *Not seen:* the window on the screen.
173. **The owner, 2026-10-15: "in a shared note, the people icons at the top should indicate clearly their writing color".** In 0.3.063. Each person's round over a note is ringed in their writing colour at full strength, over their picture as over their initial, on both apps (it was a pale wash under the initial, and a picture hid it). Also found while looking: the PC's bar broke onto two rows once "new" showed (decision 169), its buttons laid out as a box's footer, which wraps by the width it had before; it is one row that never wraps. *Seen:* the PC's gallery picture of a shared note (81-writing-colours): the round ringed in the colour of the words, the bar on one row. *Not seen:* the phone's rounds.

174. **The owner, 2026-10-15: "yes start on that too" (a folder of any files kept in step through Mininotes, own devices first; docs/FILES.md), then "let's start with asking you which version to keep" and "make 0.3.063 with all elements still to build".** In 0.3.063. On the PC, in a folder kept on disk, every file that is not a note's .md (a .docx, a picture, a PDF) is one of that folder's files in Mininotes, the same bytes on both sides, never a picture made smaller. Put there in Explorer, it is taken in once left alone three seconds; changed in Word or LibreOffice, the new version is taken in and goes to the owner's devices; changed in Mininotes, it is written out, or at the next look while another program holds it; changed on both sides, a box asks which version it keeps (the file on this PC, or the version from Mininotes, or decide later from the file's menu, "Which version to keep…"), and nothing is replaced until then. Renamed or moved in Explorer, it is followed by its bytes; renamed in Mininotes, the file takes the name. Deleted on disk, it is lost and says so, never deleted in Mininotes; deleted in Mininotes, the file on disk stays and is not taken in again. Lock files, unfinished downloads and desktop.ini are left alone. "Keep a Windows folder in sync…" (Create) takes in a folder already on the PC, its folders and files, as a folder on Home kept in step; a whole drive, or a folder already kept or inside one, is refused; own devices have it as they have everything. Every file now has Open to look (a read-only copy, as Open was), Open to edit (on the PC the file itself in a folder kept on disk, else a watched working copy whose saves are taken in exactly and sent; on the phone a working copy lent writable to the editing app, taken in when Mininotes is in front again) and Open with… (Windows' own Open with box; the phone's chooser). A click opens to look, except a file in a folder kept on disk, which opens to edit. *Seen:* DesktopFolderFilesTest (four tests) and DesktopOnDiskTest pass. *Not seen:* the phone's Open to edit with a real editing app.

175. **The owner, 2026-10-15: "instead of showing the icons of every device on top of the note, we should have a better way to display this, ... I can imagine having 10 or more".** In 0.3.063. A note's line draws all its rounds while they are few (up to four on the PC, three on the phone, never a "+1"); past that, that many rounds, those that need a look first (not linked, or not all there yet), then one "+N" round. A click on it lists everybody, one line each: their round, their name and where they stand, the ones that need a look first; a line is that person's menu, as their round is. *Seen:* the PC's picture of twelve made-up people (DesktopManyPeopleTest). *Not seen:* the phone's. *Next, already decided (163):* one round per person rather than per device, once a person's devices are folded into one.

176. **The owner, 2026-10-15: "what we need actually is synchronise an external file, so that we just reference the file, it stays outside the app, is opened by its associated app, like an ODS file opened in LibreOffice, only the synchronisation is done through Mininotes ... if only the latest version can be shared, we can say share a file through Mininotes and explain in the readme".** In 0.3.064. "Share a file through Mininotes…" (Create) replaces "Keep a Windows folder in sync…" in the menu (the folder still works, from code, if wanted back). One file anywhere on the PC stays where it is and opens in its own program; a file of the same bytes is on Home and goes to the owner's devices; each version saved there is taken in, each version from another device written into it, and changed on both sides it asks which version it keeps (decision 174). Only whole versions travel: an .ods or a .docx is a packed archive, and Mininotes does not merge two people's changes inside one; notes' words are merged. The README says so. Its menu has Show in Explorer and Stop syncing with the file on this PC (the file stays). Not yet: following the file when it is renamed or moved in Explorer (it is lost then); .txt and .csv merged line by line. *Seen:* DesktopFolderFilesTest's one-file test.

177. **The owner, 2026-10-15, on 0.3.064 (the Share box of an .ods, its menu): "when we use Share a file through Mininotes the Windows explorer should open in all types of files, not md files"; "we should have our contacts first"; "we should have only one open, once open we can decide to modify or not. The app used to open it should be presented there in the menu and in the icon type, and be linked with the open with, so that it can be changed, and it should go get what is defined at the Windows level for the default app, only if this needs to be overridden it should be saved locally in Mininotes"; "in the second window we have to indicate clearly how to go back once set. And when we come back we should have the possibility to change the settings for each contact".** In 0.3.065. Share a file through Mininotes… and Edit a file… open Windows' box on All files. A file has one Open, named by the program it opens in ("Open in LibreOffice"; Windows' own choice for the kind, read each time), then Open with…, which lists the programs Windows offers for the kind, Windows' choice marked, and another program from the disk; the one picked is kept in Mininotes, on this PC, only when it is not Windows' choice, and picking Windows' choice again forgets it. Open is to work in where this device may change the file (on the PC the file itself where it is kept in step, else a watched working copy; on the phone a working copy lent writable), and what is saved comes back; nothing saved, nothing changes; a file only shared to be read opens read only. A file's tile wears the icon Windows shows its kind with, or the picked program's. On the phone, Open names the phone's own app for the kind, and Open with… is the phone's chooser, whose "Always" sets the phone's choice. Share with lists people first, then groups (the reverse of 2026-10-05), and its foot ends on Done, the one filled button, back to the Share box, where each person's role drops down to be changed as before. Its words that stayed English in French (the title, Add, "My devices · 2 devices", the Owner line's tip) now follow the language. *Seen:* the gallery pictures 91-file-menu ("Open in LibreOffice", Open with…) and 92-share-with (people, groups, Done). *Not seen:* the phone's Open naming its app.

178. **The owner, 2026-10-16, on a linked file's menu: "after clicking stop syncing, we should have the option, synchronise. Where do we simply remove the file from Mininotes ... what happens when we put in the bin to the file on the drive? We should have distinct functions for linked files like these."** Asked first; his answers: remove from Mininotes "only this PC"; "in bin so the user can relink it easily, but it has to be clear that it is not deleting"; switched off, the copy stays in Mininotes; the phone says it is linked: yes. Built for 0.3.066. A file shared through Mininotes has a menu of its own: first, said and not done, "Linked to" its folder and name (the whole path under the pointer); Open in its program, Open with…; Show in Explorer; one switch, "Synced with the file on this PC", off and on again (off keeps the link and the copy, nothing taken in or written out; on again, what changed meanwhile is taken in, or asked where both changed); Rename and the rest as any file; and last, in place of Archive and Move to bin, "Remove from Mininotes…", whose question says nothing is deleted, the file on this PC stays as it is, Mininotes' copy goes to the bin (this PC's own, which never travels), put back from there to link it again, and the other devices keep theirs. While it is in the bin or archived, nothing is followed. Its tile wears a link, broken while syncing is off. Its sleeve, to the owner's own devices only, now says on which device it is linked (a field added at the end, where a 0.3.065 build stops reading), and the phone's file menu begins "Linked to a file on" that device. Also fixed: "Add to favourites" and "From …" stayed English in a file's menu, and the Open line had no symbol.

179. **Decision 163 carried on, while the phones were in a Stables test (the owner, 2026-10-16: "do as much as possible while the phones are in use"): the words, and Remove this person.** For 0.3.066. "Connect with someone" is "Add someone"; "Connect my other device" and "Add a device" are "Add my device"; "Pair with it" is "Add it", and the boxes and empty lines that said paired say added, on both apps. Remove this person… takes the place of Forget… for a person (a device of mine keeps Forget, off the list only): after a question that says what happens, they are taken off every thing given to them on its own that this device may change (an owner is never taken off what is theirs, and what they have through a group stays the group's, said in the question), told now where they can be reached, then taken out of my people. What they already have stays with them. *Seen:* RemovePersonTest. *Still to come from 163:* connecting as an invitation, accepted or ignored; a device of mine that only reads; one round per person.

180. **Decision 163, a device of mine may only read, and Home right-clicked (the owner, 2026-10-16: "the right click on the desktop should also display the Create and Search section of the menu").** For 0.3.067. Add my device offers only Can write (first) or Can read, under "What it may do", in a box called Add my device on both apps; Admin, which says nothing between one owner's devices, is gone from it. A file sent to a device of mine now says it may be written only where that device's bond says Can write (it said so to every device of mine). Home right-clicked where there is no icon has Home, Create and Find, as ⋯ has them, then Show on Home; the two share one set of Create and Find rows. Undo follows the language. *Seen:* the gallery picture 93-home-right-click. *Not seen:* a device of mine added as Can read, end to end on two devices.

181. **The owner, 2026-10-16, on Share with an .xlsx and on the Pro: "let's make sure these Add here are also translated, and that the buttons are not touching each other, and that we can always change roles, here Propriétaire ... once shared I should get a notification on the Pro at the phone level that someone shared something with me, and on the Pro, in New when I see something shared, clicking on it should not open it ... there first I should be able to see what I do with it, accept, refuse and where I want to put it ... clicking it should provide all the needed information for the person to decide".** In 0.3.068. Share with: the Add beside each person in a group follows the language; Done stands apart from the other buttons by a gap, and the box opens at least as wide as its foot. The Owner line stays: it is the device the thing was made on, whose rights nobody changes (said to the owner; to be looked at again with one round per person). The phone: a file shared from another device, the owner's own included, now says so on the phone ("PC shared “…” with you"), once, on a channel of its own, Shared with you, at the phone's usual loudness (the arrivals channel stays quiet, as a note being written in sends every few seconds); what waits to be accepted is said there too. A new file not opened yet, tapped on its folder or in New, opens nothing: its card first, New and its size, who sent it and when, Keep it here, Move to…, Move to bin, then Open and the rest. Things that wait to be accepted keep Accept and Refuse in Shared with me (decision 109). *Not seen:* the notification and the card on the Pro.

182. **Decision 163, connecting is an invitation, accepted or ignored (the owner, 2026-10-14: "let's make that as an invitation, for contact and for elements, all this needs to be accepted or ignored"; 2026-10-17: "it seems you are good to go").** In 0.3.069. Somebody who scans a plain code (one offering nothing) is no longer added back by the device that showed it: "Ben wants to add you" is asked, with Accept, Ignore and Later, there and then if the device is in front, else said on the phone (the Shared with you channel) or the PC's tray and asked when it is next opened. Accept adds them and answers their hello, as before; Ignore tells nobody, and their hello, which they say again at each opening, falls from then on; scanning their code later forgets that. A device already added is answered as before, unasked, and a code that offers a thing (Add my device, a note, a folder) stays one step, since showing it was the decision. The one who scanned reads "Asking Ana to accept…", then "Waiting for Ana to accept". Nothing new on the wire and nothing in the notebook: the hello that waits is kept in the settings (Invites). *Seen:* InvitesTest, and the pairing tests unchanged. *Not seen:* two devices, one scanning the other's code.

183. **Decision 163, one person on several devices: the about-me card (the owner, 2026-10-17: "ok, go ahead with the about me").** In 0.3.070. A new card, AboutMe (MNA1), says who a device belongs to: the person id its owner's devices already share among themselves (Persons), and when it was made. It goes to every paired device whose build has said it reads one (a new receipt, 38, ABOUT_ME, said and said back as the picture card's is), once, and again whenever the id changes; a build from before is never sent one, and older devices go on showing a round per device. The receiving device keeps whose each contact's device is in the settings, beside the notebook (no column, no migration). A note's line then folds a person's devices into one round, the first of them, wearing the mark among them that most needs a look and saying how many devices it stands for ("Has this version. · 2 devices"). The owner's own devices stay a round each, since which of them has a note is what those rounds are for (folding them hid it, which three tests caught). What it tells a contact: that two of their devices are one person's, which they could guess from the names. *Seen:* AboutMeTest (the card there and back and refused when cut; the folding). *Not seen:* two people's devices folding on a real note. *Still to come:* People and devices listing one person with their devices under them.

184. **People and devices, one person with their devices under them (the owner, 2026-10-17: "ok, do that").** In 0.3.071. On both apps, a person whose devices' about-me cards say they are one (decision 183) is one line, their round and name with "2 devices" under it, and their devices indented under that line, each still its own line with its page, its › and its group chips; anybody else a line of their own, as before. The owner's own devices are under My devices as before. *Seen:* the gallery picture 94-person-devices (Ana, 2 devices, her two devices under her, then Ben). *Not yet:* a name for each of somebody else's devices (both read "Ana"; only the owner's own devices tell each other their device names).

185. **The owner, 2026-10-17, on the menus: "let's have unique icon for each function; colour should be at the top, the top should always stay the same and generally speaking the order of elements in each menu should stay the same; we don't need two times the Text size; Mininotes is all about synchronisation across devices and sharing with other people, these functions should be at the top of all menus in a dedicated section"; then "Share should be just below Home and Sync now should be in the Share section"; "remove this dropdown Share menu and integrate it as the top section of the main menu"; and on the Share box and people: "we need to be able to change the roles and avoid duplicates", "the colour doesn't change, please make sure it is reflected right away", "the colour should be shown beside the name at the top ... not optimal to have a menu there with only these 2 elements", "we don't need discuss on Parlons! here", "we should be able to know where the element is located and go directly there"; "instead of having Mininotes plainly written at the top, put it on the left with the icon ... clicking it should bring back to the main page and when clicked from the main page to My profile".** In 0.3.072. Every menu, on both apps, has the same top in the same order: the reading size once; the colour and icon of what is in view; then what is in view (Home or This note), then Sync and sharing (Sync now, Sync parameters…, Share…, Show my code…, People and devices, From another device…, Profile…), then Create, Find, Places, Show on Home, Backup, Mininotes; a note's or folder's own menu begins with its colour and icon and Sync and sharing, and ends with Temporary, Archive and Move to bin. The second Text size is gone from This note. Each line wears a symbol of its own (Sync parameters is a slider now); a test (DesktopMenuOrderTest) holds the order and the symbols. The PC bar's Share ▾ is gone; its lines are in Sync and sharing. A Share box lists each person or device once, their own rule before one through a folder; the owner's own devices each with what they may do with everything, Can write or Can read, changed there; a "Where it is" card at its top names the folders above the thing with Show it, which opens that folder, or the note a file is in. A colour chosen is worn at once by every round showing. A round of one of the owner's own devices says it writes in the owner's colour, set in Profile, rather than offering the dots that changed every own device at once; and its box has no Parlons! button. A person's page shows their colour under their name, with no ⋮; Forget (a device of mine) or Remove this person at its foot. The phone's Home bar has the app's icon and name at the left, a tap being Profile; the PC's logo goes Home, and on Home, to Profile.

186. **The owner, 2026-10-17: "make sure everything is translated ... can you please have a full review of the app to make sure everything is translated. Can you put a rule for that, a structure making sure we don't miss any element?"** In 0.3.072. A rule, WordsEverywhereTest, checked at every PC build, for both apps: words/en.json is the list of every phrase the apps say, and every language has every one of them; every phrase handed to a screen in the sources (to Say.t, a menu line, a label, a button, a box's title or words, the status line, a toast…), as words or as a sentence built around a name, a number or a file's name ("Shared with " + name read as "Shared with {0}"), is on that list. The review found 163 phrases on screens in no list and 119 kept in French only; all are in all eleven languages now (the French written here, the nine others translated with each language's own words for the app's terms). Say.t also keeps a button's arrow (› or ▾) after words it translates, which left "Not set ›" and "The name they share ▾" in English.

187. **The owner, 2026-10-17: "the New window must be better designed and more informative, now the user doesn't really have any input".** In 0.3.072, on the PC. New lists each thing with its icon, its name, what it is (a file with its size), whom it came from, when, and where it is; and Open, Show where it is, and Mark as seen for each, with Mark all as seen at its foot. *Next:* the same on the phone; and, asked at once after, a contact's trouble said at the top of the app, mended by the app where it can, and said plainly with what to do where it cannot.

188. **The owner, 2026-10-17: "in a situation where there is an issue with one contact, this should be displayed directly at the top of the app, it should be rectified by the app directly and if the user input is needed, clear indication in how to solve the issue should be provided"; then "take care of the strip for a contact who has a problem, and the improved New window on the phone"; and "colour of writing here doesn't display anything; let's remove the Mininotes and icon at the very top; let's put Share a file through Mininotes in the section Sync and sharing".** In 0.3.073. Trouble with a contact (Troubles, on both apps) is what is not a moment's delay: something owed to a device for more than three days with nothing heard from it, the red mark's rule; a device that said it is locked while something waits for it; a device heard from whose Mininotes is too old for something that waits; somebody added who has not answered for an hour. It is said in a strip under the bar (the PC) or under Home's bar (the phone), the first trouble and how many more, and "Mininotes keeps trying"; What to do lists, for each, what is wrong, what Mininotes does about it by itself, and what the owner can do, with Try now, Their page and, where it helps, Scan their code. Trouble not seen before is tried again at once, by itself. The strip says a device needs a newer Mininotes only where that device was heard from (one never heard from was said to; the marks keep their own rule, which DesktopNeedsUpdateTest holds). The network's "no reachable address" is said as not reached. New on the phone is as on the PC (decision 187). The PC's bar no longer shows Mininotes and its icon (the window's title does). Share a file through Mininotes… is in Sync and sharing. A round of one of the owner's own devices has no Writing colour… line. *Seen:* DesktopTroublesTest, pictures 98-trouble-strip and 98b-what-to-do. *Not seen:* the phone's strip and New.

189. **The owner, 2026-10-17, on a note's Share box: "in Who has access we should be able to change directly the rights of the people and devices there. Owner should be in a dropdown menu and we should even have the possibility to remove someone, just as adding someone. In the following screen, we should have adding someone from my contacts easily accessible. Let's have the groups and list extended there, not collapsed by default".** In 0.3.074, both apps. Each of the owner's own devices is a line of its own on Who has access, and under My devices in Share with, with what it may do with everything, Can write or Can read, in a dropdown that also removes it (its bond ends); one with no bond yet (which drew it as Owner, nothing to change) is given one at what is chosen. Everybody else whose rights this device may change has the dropdown with Remove as before; the thing's owner and you stay said, not changed. Share with lists every contact under People, in a group or not, one tap from Add; and its groups are open at first, each folding with a tap. *Seen:* pictures 99-who-has-access and 99b-share-with; DesktopPeopleBoxTest brought to the new rules.

190. **The owner, 2026-10-18: "we need a button, add to the dock and this should be independent of add the favourites even if at first everything added to the favourite should be added to the dock. We also have to make sure we can add multiple elements to the dock and that we can browse them horizontally"; then "removing a favourite dont take it off the dock" and "dock should be specific per device".** In 0.3.077, both apps. The dock has an order of its own on each device (`docked`, schema 52), never sent, filled at the upgrade with every favourite in their order. Add to the dock and Remove from the dock are in every note's and folder's menu, apart from Add to favourites; a new favourite goes to the end of the dock too, and taking a favourite away leaves it in the dock; carrying something onto the dock puts it there without making it a favourite. No limit: past what the screen shows, the dock scrolls across (a swipe on the phone; the wheel, a thin bar or the keys on the PC), its rounded shell staying put. The Favourites icon is on Home whenever there is a favourite. What the PC had kept out of its old dock is taken out of the new one once. On a person's page, a thing whose role cannot be changed here says why under it: its maker is its owner, or you are neither its owner nor an admin. *Seen:* pictures 95c-dock-taken-out and 95d-dock-scrolls; DockOnItsOwnTest, DesktopPlacesTest, DesktopHomeFilesTest.

191. **The owner, 2026-10-18: "What about adding an agent that would answer such question in the app ... Could it work without any external dependencies?", then "build the agent as much as we can, to answer question but also do tasks like, add a contact, share a note, tell me what is new, tell me what is on my todo list, add this to my todo list and so on".** In 0.3.077, both apps: Ask Mininotes, in the menu under Find (Ctrl+K on the PC). On the device and nothing else: no model, no server, nothing sent. A question is answered from the help kept in the app, help/en.txt and help/fr.txt (33 answers, other languages answered in English), by the words it shares with each answer's ways of asking. A task said in English or French is one of what the menus do: what is new; the todo list (a note called Todo, To do, Tasks, À faire…) read, or a line added to it or to any note named ("add milk to Grocery"); a note or folder shared with somebody, to write or to read; add someone; open, find, make a note; put in the dock or the favourites. Anything that reaches other people or changes a note is shown first and done on a tap, the way its menu does it. Suggestions are shown in the language in use and read in English. Not built: a language model (an on-device one is possible later, optional and downloaded, as translation is planned), updating the help from GitHub, tasks said in languages other than English and French. *Seen:* pictures 99c-ask-mininotes and 99d-ask-mininotes-more; AssistantTest, DesktopAssistantTest.

192. **The owner, 2026-10-18: "I dont like the Menu in the top bar, please bring it in the app over Recents. Put the agent visible at the top bar where the menu is currently. On mobile, also display the agent beside the menu 3 dots. Call that Agent directly instead of Ask Mininotes. On mobile the ask button is not well displayed. Make it clear at the top of the agent dialog box that it answers questions and does stuff, that everything is private in the app. We dont need a close button on the agent window, only clicking out closes it".** In 0.3.078. PC: ≡ Menu at the top of the Recent column; while Recent is not on the screen (switched off, folded, or not there yet) it is back at the end of the title bar, so it is always within reach. The Agent, its symbol and its name, where the menu was; Menu → Agent… and Ctrl+K as well. Phone: the Agent's symbol beside ⋮ on Home's bar, a folder's and a note's. Its box, on both: titled Agent, first what it does and, in the accent colour, that everything stays in Mininotes on this device and nothing typed is sent; no Done and no close button: a press outside it closes it (on the PC a press of the mouse anywhere else in the app, not focus moving, which closed it by itself). On the phone the field and Ask are side by side at one height. *Seen:* pictures 90-home-everything, 34-coloured-note, 99c and 99d; DesktopAssistantTest presses outside and sees it close.

193. **The owner, 2026-10-18: "I cant manage to delete the files ... when deleted they should go in the bin", then "no need for this confirmation, it goes in the bin".** In 0.3.080, both apps. A note's file taken out with its × (Move to bin) goes to the bin at once, with no question: out of the note's strip and count, listed in the bin as a file from that note, put back into the note from there, and gone for good only when the bin is emptied, which then tells whoever had it, as deleting did. Until then it travels as it did.

194. **The owner, 2026-10-18: "let's have the photo available even on desktop, triggering the camera function".** In 0.3.080, the PC. A camera beside the microphone at the foot of a note opens Windows' own Camera; what it saves in the Camera Roll (Pictures, or OneDrive's Pictures) from then on comes into that note as an attachment once it has finished writing, photos and videos, until Camera has been quiet five minutes. Nothing is added to Mininotes for it; the bar says what to do.

195. **The owner, 2026-10-18, on a note called Martin: made on one phone, moved twice, it could not be found on the other phone where search said it was, and sharing the folder it had been put in did not share it: "we need to make sure users dont face this issue", and "when I share a folder, everything in it is shared even if this would create a duplicate".** The cause: the other phone took the note, from the owner's own device, as somebody else's, so moving it there only showed it in the new place (decision 122), while search named where it really was; and a folder's sharing reaches what is really in it. In 0.3.080, both apps: what comes from the owner's own devices is the owner's to move, and is moved for real, with the usual question where that changes who has it; only another person's things are shown elsewhere. And a note only shown in a folder that is shared is put in it for real whenever Home is read: the owner's own moved there, another person's copied there as the owner's own with its files, the original back where it really is; then sent. Not done for folders shown elsewhere.

196. **The owner, 2026-10-18: "we need to be able to perform bulk action on all attachments, here I would like to save them all on my C drive. We should be able to see them, be able to select them and perform action on the selection we pick", and of the paperclip's menu "we should also have the camera here then. This menu is a good start for the bulk action".** In 0.3.081, the PC. The paperclip's menu: Attach a file, Take a photo, Record audio; then the note's files, each with a box to tick, the menu staying open while they are; then Select all, and for those ticked Save to a folder… (one folder for all, a name already there given a number), Download, and Move to bin, each saying how many. And every message an error can put on the screen is now held to the translation rule (WordsEverywhereTest reads exceptions' words too): the owner found "Windows Hello took too long" in English; 212 such messages were translated. Not built yet: the same on the phone.

197. **The owner, 2026-10-18: "Could we even share a folder from the C drive and sync it with other devices, as soon as we put files in it they are synced, this folder should be shown in the app as external folder and the content of it saved of course out of the app?"** In 0.4.001, the PC. Built in 174 and kept since, its entry taken away by 176: "Sync a folder of this PC…" in Sync and sharing again. Such a folder, and one kept on disk (162), wears a folder with arrows on its tile, and its menu first says where it is ("On this PC: …"), then Show in Explorer, Keep it inside only, Remove from the app. Everything in it is a folder of Mininotes like any other, so it is shared as any folder is (195). The phone keeps its files inside the app.

198. **The owner, 2026-10-18: "it would be nice to have the option to copy a file, here a picture, directly from a right click, rather than having to open it", and of the phone's "Things stay / An hour": "we have to better align ... can you propose a better presentation".** In 0.4.002. PC: Copy in a file's menu on Home and on a note's attachment card: a picture goes on the clipboard as a picture and as a file, any other file as a file (a plain copy in the PC's temporary folder). Phone: a row that drops down to a choice says its name on the left and what it is set to, with ▾, on the right, on one line and in the middle of its height, where a switch row has its switch: Things stay, Language, Send changes, Recent keeps, Lock again, roles. The attachment card's Delete reads Move to bin, as it does (193).

199. **The owner, 2026-10-18: "How could we integrate Integritas directly through the app so that people could easily stamp a file from the app, could they link their Integritas account in there?", then "your approach seems the good one, please build it".** In 0.4.003, both apps. Each person links their own Integritas account (Integritas account… in the menu: the API key from their profile at integritas.technology, kept in the notebook's new secrets table on that device, so locked with it and never in a backup, and sent only to Integritas). Stamp on Minima… in a file's menu, an attachment's, the attachments menu for those ticked, and a note's (its words as they are now): the SHA-256 is made on the device and only it goes, to POST timestamp/post; asked first, said on the bar; recorded here in the stamps table, and a stamped file's menu starts with when. Verify on Minima: the same fingerprint sent to verify/file with a PDF report asked for; the box says whether it is there unchanged, the block and the date, and opens the report. Taken by the owner's defaults (asked, not yet answered): the key on each device, not sent to the owner's others; notes stampable as well as files. Not built: the stamp travelling with a file to others (verifying needs only the file), a stamp mark on tiles, the Agent stamping. *Seen:* IntegritasTest against a stand-in server (the key header, the hash alone, the answers as the v2 documentation gives them, a refused key). *Not seen:* a real Integritas account.

200. **The owner, 2026-10-18: "Personnes et appareils should be just on top Profil, and rename this one Mon profil"; "the font size has no direct impact on the Home screen ... put it in My profile for the setting at the app level; for the note it seems to work good"; "Enregistrer dans un dossier is not clear to me ... let's combine these in Save on my computer and again make sure we use the latest explorer windows, not this legacy one".** In 0.4.004. Sync and sharing ends From another device, People and devices, My profile, on both apps; the box is called My profile. PC: the size ladder at the top of the menu is there only while a note is open, and is the note's; My profile has Text size, five steps from Smaller to Largest, for the whole app on this PC (Home, Recent, menus, boxes), drawn as Windows' scaling draws it (on top of the screen's own), so it shows from the next start, with Restart now (DesktopScale; the installed app only). Save on my computer… replaces Download and Save a copy… for a file and an attachment, and Save to a folder… and Download for those ticked: one file where Windows' own box says, several into one folder. Every box that picks a file or a folder on the PC is now Windows' own, as Explorer shows it (DesktopPick, through DesktopFileDialog), Java's only where Windows' cannot be shown. The phone: Save on my phone… in place of Download and Save a copy….

201. **The owner, 2026-10-18: "Demande d'aide must be secret, it should be integrated to the private//: function, it can't be set in plain sight like these; in the settings of a private space this should be set, we need to show it in the menu only there and we have to clearly explain what it does and that it will never show up again. A special code like erase//: should be typed to erase it but it should never be displayed".** In 0.4.004, both apps. The help request is set only from a private space's note or folder (Help request… in its menu); the box always opens empty, says plainly what it does, that once set it is never shown again (not even there, and nothing says which thing sends it), and how to take it away: erase//: typed in one of the space's notes, taken out of the text the moment it is typed, takes away every help request of that space. It is gone from My profile, and an ordinary note or folder never sends one any more; those set before are taken away with their trace in the notebook (schema 54). Only a private thing sends it, opened with the space's password: so only someone who knows that password can set it, set it off, or take it away, which is what a duress alarm needs, and it cannot be set to follow somebody else who opens an ordinary note. Decision 115's words stand for the rest: the location permission is the app's, declared plainly, and the system shows when it is used.

202. **The owner, 2026-10-18: "remove the 3 dots beside Family and have all the parameters available when we click the arrow beside the groups"; "make sure we don't have these boxes missing on the left side, don't put these back elements in a box, align this with what we have at the note level"; "in profile, People and devices, just have a Manage button there"; "everything that opens a window should have the ...; please check the whole app"; "change Keep as a file to Save on my device".** In 0.4.005. PC: a group's line on People and devices has only its ›, and the group's page ends with Rename… and Delete group…; the page has no ⋮. Every page's way back (← People and devices, ← Home) is the same plain link as a note's ← Home, in no box, and the page's tabs say their names in the language in use. My profile's People and devices part on both apps is one sentence and Manage…, which opens People and devices. Every entry that opens a box or a page ends with "…" on both apps (Verify on Minima…, Versions…, About…, Search… and the rest). Keep as a file is Save on my device… on both apps.

203. **The owner, 2026-10-18: "I don't see in My profile the Integritas section; we should have a section where everything related to Integritas can be managed including the API key and we should have an Integritas dedicated window where we list everything stamped; we keep the on-chain reference and all needed information"; "what is the difference between Stamp on Minima and Verify on Minima".** In 0.4.005, both apps. My profile has an Integritas part: Your Integritas account… (the API key, kept sealed on this device) and Stamped on Minima…, the list of everything stamped from this device, newest first: its name, when it was stamped, its fingerprint (SHA-256), the request Integritas gave, and once verified the Minima block, the date it was recorded and the transaction (txpow), with Copy the details and Verify on Minima…. A verification found on Minima is kept with the stamp (schema 55 adds block, txpow, recorded and name to stamps; nothing already there changes). The Integritas row is gone from the main menus; it is in My profile. The difference, as the README says: Stamp on Minima records now, on the blockchain, the fingerprint of the file as it is (a proof it existed, unchanged, from that moment); Verify on Minima checks a file, later, against what was recorded and says whether and when it was stamped, unchanged. Stamping costs a stamp of your Integritas account; verifying does not change anything.

204. **The owner, 2026-10-18: "choose Help request is not good; nothing should be displayed, only the secret key like erase//: should be typed directly in a note there; this needs to leave 0 trace behind".** In 0.4.005, both apps. No menu says help request anywhere. A code typed in a private note, taken out of the text the moment it is typed and out of the note's undo, opens the box that sets that note's help request; the box always opens empty, with no way to read back what is set; erase//: takes away every help request of that space as before. The codes are in the help kept inside the app for the person who reads it on purpose, and not in the README, the menus or the descriptions.

205. **The owner, 2026-10-18: "we should have a mode in the profile that turns on or off the mouse-over description of everything, and for all elements we should have a brief description. All this will need to be in the Read me".** In 0.4.005, PC. Every menu entry has one line saying what it does (help/describe.txt, translated like every other word), shown when the pointer rests on it. My profile, Text size part, has Describe what each thing does, on by default, which turns them all on or off on this PC. The README lists every entry with its line, from the same file.

206. **The owner, 2026-10-18: "the attachments were not synced: in the note Martin I deleted them all from desktop and added 2 new; on the Pro all of them stayed. I guess we would need a setting to sync that or leave delete specific per device", answering, of three ways, "gone from the note everywhere"; then "on the Pro now it says 4 files to sync but only 3 are listed, please make sure we can browse all files needed to be synced"; and "working on the note Martin on the desktop there is a lag".** In 0.4.005, both apps. A note's file put in the bin on one device is taken out of the note on every device and for everybody the note goes to, whichever device added it: the note's list names it as taken out (a size of nothing and no place to fetch it from, which a build before this one names and leaves alone), and each device lets it go and remembers who took it out (schema 56, incoming.taken), so another device still listing it does not bring it back. It stays in the bin of the device it was put in the bin on; Put back there lists it again and the others fetch it again. One in a device's own bin stays there. Before, the bin was each device's own (schema 38) and only Delete for good reached the others. What waits to go now counts only the files the note shows (not one in the bin or the archive), and the box an amber mark opens lists each of those files under the notes, to be opened. PC: while a note is in front, Home under it is read again only when it is shown, and a whole-tree read already asked for is not asked again, so the note's saves no longer wait behind them on the one disk thread.

207. **The owner, 2026-10-18: "in the attachment we should have a copy directly from right click and long press, not that we have to save it first on the device, and we should have a share directly there so that we can send it through an app like WhatsApp; actually we have Send with another app, only the icon should be the share we are used to, not the same as Send to a device".** In 0.4.005. The phone: a held file's menu has Copy, which puts the file itself on the clipboard to paste in another app. Send with another app… wears the share mark everybody knows (share-2); Share with people takes a person with a plus (user-plus), so each mark still means one thing (decision 185). PC: a file's card had Copy already; a right click on a file's line in the paperclip's list now copies it too, and its description says so.

208. **The owner, 2026-10-18: "we have to make sure the mouse-over info are fully displayed"; "could we have a preview of the doc instead of just displaying in the image the file type?"; "when wanting to share with another app multiple attachments, we should be able to select many at once on mobile".** In 0.4.006. PC: a description longer than a short line is kept to a width (260 px at the usual size), on as many lines as it takes, so Swing can keep it on the screen. Both apps: a PDF's card shows the top of its first page (PDFBox on the PC, added as a dependency, Apache-2.0; the phone's own PdfRenderer, from a copy in the app's cache deleted at once); a Word, PowerPoint, Excel, OpenDocument, RTF, HTML or text file's card shows its first lines on a small white page, read by Peek from the XML inside it. Never Windows' thumbnails, which would keep a picture of the document in Windows' cache outside the notebook. Files over 20 MB show their kind as before. The phone: a held file's menu has Choose several…, the note's files in a list to tick (Select all first, the held one ticked), then Send with another app… for all of them at once (one share, ACTION_SEND_MULTIPLE) or Move to bin.

209. **The owner, 2026-10-19, on My profile's Text size list beside the ladder at the top of a note's menu: "in the menu let's use the same font scale we have in the menu to set the font size at the app level".** In 0.4.006, PC. My profile's Text size is the same A− and A+ ladder, with the app's five steps (Smaller to Largest) for rungs and the name of the one chosen under it, beside Restart now while the new size waits for the next start.

210. **The owner, 2026-10-19: "align the Mininotes on the left beside the logo"; "in the Recent, on a right click, we need to have a remove from Recent option"; "when we set Integritas, we should have the option to share it on all our devices, choosing by device"; "in the profile we should just have the account API setting, we don't need the other button, Stamped on Minima"; "since the pop-up only lets us enter the API key, let's do that on the profile page directly as we do for the Parlons address"; "Copy my link in Your code must be in my set language, not in English".** In 0.4.006. PC: the window's name is on the left of the bar beside the logo (the frame's own centred title hidden; this one follows it); a right click on a line of Recent down the side ends with Remove from Recent. Both apps: My profile's Integritas part is the API key in place with Save, the link to integritas.technology, and, where there are paired devices marked as yours, a switch for each under Also on my devices: those switched on get the key when Save is pressed, sealed for each (KeyCard, "MNK1"), and take it only from a paired device marked as yours, and only where it was set later than theirs (kept beside it as integritas.at). Only the Integritas key can travel this way. Stamped on Minima… moved from My profile to the menu, after Sent files…. The words sent with my link (Copy my link, Share Mininotes) are in the language chosen. Also found while testing: the PC's Recent split asked itself again and again to be placed while it had no width, keeping the screen's thread busy (a closing test window spun for an hour); it now waits for its first width.

212. **The owner, 2026-10-19, on the rounds beside a note's title: "when we mouse over these here, we should have the basic details including their status on the note, sync and all".** In 0.4.007, PC. The pointer resting on a round says who it is, your device or a person, what they may do with the note (Admin, Can write, Can read), where they stand with its writing, and how many of its files are still going to them, or that all are with them. Shown while the descriptions are on (My profile), as every line under the pointer is.

211. **The owner, 2026-10-19: "the app on Graphene is not syncing well and is slow to respond, the home page loads slowly, in Martin the attachments are still not there after more than 2 minutes"; then "now if I move Martin in the Friends folder back, it is syncing. We have to make sure it stays in sync even if we move it out of a folder only on one device".** In 0.4.007, both apps. Seen: Graphene's Mininotes at about 360% CPU, three threads mining the proof of work every Maxima message carries; the PC sending revision 85 of a note every few seconds, then a revision 77 arriving at Graphene again and again, each held beside its own (HAVE, "put it with their own") and answered, each answer another proof of work; it stopped when the note was put back in Friends. Fixed: (1) one of the owner's paired devices that has had a note keeps it after the note leaves the shared folder that reached it there (NoteStore.withOwnHolders, used by what is owed, who has a note, files lacking and where each stands), unless taken off the note or a folder above it; proven on synthetic devices (MovedOutOnOneDeviceTest, 8 tests, the 4 without an Everything bond failing before). (2) The same answer that went out lately is not sent again for ten minutes, by the device's key (Post.answer), whatever brings the same revision back. (3) "I have this file" is said once a run by the device's key, not its address, which moves. Not proven: how the 77 kept coming back where the devices are bonded for everything; a stale copy carried or collected is the likeliest.

213. **The owner, 2026-10-19: "make the Agent conversation window fully scrolling, not a little answer box in it, so that we use as much as possible the whole screen to display the conversation; we even should have a way to expand full screen the dialog box"; "the Agent should propose: do you want me to bring you there, so for example in setting Integritas it will open directly the section in the profile page"; "we have to make sure the Agent keeps the conversation for a couple of hours at least".** In 0.4.008, both apps. The Agent's window is one conversation that scrolls, questions on the right, answers on the left, newest at the bottom, the field under it: on the PC most of the screen's height and up to 780 px wide, on the phone 58% of the screen; New conversation and Full screen in its header. An answer about a place in the app ends with Take me there: My profile opened at Integritas (the key's field in hand), Parlons!, Text size, the descriptions switch or Backup; Settings at Security, Language, Temp or Show on Home; the bin, the archive, Shared with me, Stamped on Minima, Sent files, People and devices (help entries say their places with @ lines). The conversation is kept on the device, in the notebook's secrets, locked with it and in no backup, for 3 hours after its last line, never while a private space is showing, and erased by New conversation.

214. **The owner, 2026-10-19: "we have to add the Read me in the list of items to show or not on the home page"; "Read me needs the emergency contact description; let's remove privatespace and only mention private as a way to activate"; "Read me needs a specific icon, not the generic file one"; "the app is built to make the private notes invisible from a code observer but we can't confirm it is 100% safe, keep inside non-critical elements".** In 0.4.008, both apps. Show on Home has Read me (off: not on Home, still in Recent and search). Read me wears book-open-text where nobody gave it another icon. Its text (READ_ME_0_4_008, said again where an earlier one is unchanged) names only private as the way in, says that private spaces are built to stay hidden from someone reading the app's files or code but cannot be confirmed 100% safe, so only things that would not harm you if found belong there, and describes the emergency contact: set from a private note with the word help and //:, never shown again, taken away with the word erase and //:. The codes are written as words, as private's always was, so nothing in Read me is taken for a code; the feature being known says nothing about whether anybody set one. Also a few lines on files in a note, the Agent and Stamp on Minima.

215. **The owner, 2026-10-19, a note full of the same sentence at each stage of being typed ("it says submitted but", "it says submitted but it leave", "it says submitted but it leaves the user guessing…"): "it seems I face situations where the text is duplicated".** In 0.4.008, both apps. Where two devices wrote over the same lines (Merge.both), a line that is only the start of another line there is its earlier state, caught on one device while it was typed on the other: only the longer stays, and the same long line twice is once (Merge.typedOn; lines under 12 characters are kept whatever). The same result whichever device works it out. The cause upstream: an echo, a device sending on as its own a half-typed text it took, merged against an older shared text when the answer saying it was taken had not arrived yet. Text already doubled stays as it is, to be tidied by hand.

216. **The owner, 2026-10-19: "when we open a file or folder from the search bar, the note should open where the first occurrence of what we were looking for is, and in the note we should have a search window open letting us browse through all occurrences of the searched term".** In 0.4.008, both apps. A note opened from Search's results (a click, Enter, or the first result on Enter in the field) opens with the words looked for found in it: the first place chosen and scrolled to, and a bar over the top right of the page saying "1 of n", with the next (Enter, F3, ▼) and the one before (Shift with them, ▲), and × (or Esc) to close it. PC: every place marked, the one in hand more strongly; the words can be changed in the bar; marks go when it closes (DesktopFindBar). The phone: the match in hand chosen, the bar floating over the page, closed when another note or Home is shown. Nothing is written into the note.

217. **The owner, 2026-10-19: "would be nice to have a type of folder specific for photos, that will display photos in a nice album setup and make browsing convenient. It should come by default on the Home page for new installs and updates".** In 0.4.009, both apps. Photos is on Home, a folder wearing images, made once on each device at a start, a new install's or an update's, under the same id on every device of the owner (made from the person's id, Album.photosId), so the devices that sync everything have one Photos and not one each; not while a device knows no person yet (then at a later start), never again on a device once made there, even deleted or renamed, and not where one with that id came from another device already. A device that becomes one person with another lets go of the Photos it made alone while it is still empty and so called, and takes the other's. Shown as an album is each device's own view of a folder, kept in its settings and never sent: Photos is one from the start, and any folder's menu has the switch Show as an album. An album shows its pictures (the image files kept with the folder) as squares cropped to fill, newest first, about 160 to 200 px on the PC and three or four across on the phone, and under them, under Notes and other files, whatever else the folder holds, drawn as the folder draws it. A square clicked or tapped shows its picture large, fitted to the window or the screen and decoded at about that size: the one before and the one after with ← and →, or the buttons at its sides, on the PC, a swipe on the phone; Esc, × or Back closes it; its name, where it is ("3 of 24") and ⋯ or ⋮, the picture's own menu (Copy, Save on my computer or Save on my phone, Send with another app, Stamp on Minima, Move to bin and the rest). The squares and the large pictures are made on a thread of their own, never on the one the note's saves wait on, read through the lock (DesktopAlbum, PhoneAlbum). An album's + offers Add photos… (several at once, in Windows' own box or the phone's picker of pictures) and Take a photo… (Windows' Camera, or the phone's), kept straight in the folder. Read me says so (READ_ME_0_4_009).

218. **The owner, 2026-10-19, after stamping a note on the PC, where only a small message "“Integritas test” est horodaté sur Minima" showed low on the screen: "The Integritas message is too low, it should be centred on the screen, same when stamping. And then we need much more confirmation: we need a full communication box, showing everything, the transaction of encryption on the blockchain, the hashes, the date, the block and so on, and there should be a mention at the top of the note for all this, and as soon as there is a change, this should be notified; all stamped versions of the notes should be easily retrievable".** In 0.4.010, both apps. Stamp on Minima… opens a box, centred, that shows each step as it happens: 1, the fingerprint made here, what exactly (a note's text as saved, without its title, its length in characters, in UTF-8, that exact text kept with the stamp; a file's bytes as kept) and its SHA-256 in full, with Copy; nothing goes until Stamp is pressed; 2, sent to Integritas: when, the request, what Integritas said, the Minima address its transaction went to; 3, waiting for Minima, how many looks, when the last was and the seconds to the next; then confirmed: the block, the date it was recorded, the transaction (txpow) and the transaction's id, each with Copy, and Copy the details, Open this version (a note), Open the PDF report and Verify again. The note's or file's earlier stamps are listed under it, each opening its own box. Closing the box stops nothing: the watch (Integritas.Watch, one thread) looks for the fingerprint after 15 s, then 20, 30, 30, 45, 60, 60, 90, 120, 180 and every 5 minutes, 30 looks, about two hours, by verify/file with no report (it changes nothing and costs nothing); stopped, the stamp says not found yet and Verify again starts it again; a start of the app goes on with those still waiting. When it lands, or gives up, it says so in the middle of the window or the screen. Every notice of stamping and of linking or unlinking Integritas is centred (PC: a card over the window, gone after 5 s or a click, DesktopUi.centred; phone: a card in the middle of the screen, PhoneStamp.notice). A stamped note has a line under its title: "Stamped on Minima, <date>, block <n>", "Stamping, waiting for Minima" or "Stamped <date>, not found on Minima yet", which opens the stamp's box; once its text differs from every text stamped (compared a moment after the typing stops, by fingerprint), "Changed since it was stamped on <date>", toned down, with Stamp this version…; a text put back to one stamped stands by that stamp again. A file's menu on the PC starts with its last stamp, which opens its box. Every stamp is kept, each version its own row, in the new table stamped (schema 57: how far it is, the looks, the block, the txpow and transaction id, the Minima address, and for a note its title and the exact text stamped, in the notebook and locked with it; the stamps table is copied into it and left as it was). Stamped on Minima… lists them all, each with its state and block, Open…, Open this version, Verify again and Copy the details; Open this version shows the stamped text read only, checked again against the fingerprint, with Copy and Put this version back, which keeps what the note says now as a version first, as Versions does. A file stamped keeps no copy of its bytes (the file is kept with its note).
    What Integritas gives, read in its documentation (docs.integritas.technology, API v2, 2026-10-19): POST timestamp/post {hash} has Integritas' own Minima node send a transaction carrying the fingerprint (its answer: data {command "send", params {amount, address, state {"99": the hash}}, status true}, and "Hash anchored successfully."); it gives no block, no transaction id and no request status. The fingerprint is on Minima once that transaction is in a block; the v1 API's timestamp/status (by uid, onchain false then true, with a Merkle root, proof and address) says to wait and retry, about 3 minutes at most. In v2, POST file/check answers "Hash found" or not, and POST verify/file answers with result (full, partial or no match) and blockchainData, each record its blockNumber, blockDate, txpowId, transactionId and matchedHash; with the header x-report-required it also makes a PDF report, a link good for about an hour. Integritas' pages describe SHA3-256 for their own uploads; the apps send the SHA-256 of the bytes as the v2 calls take any 64-character hex hash. So a stamp is "waiting" from its send until verify/file finds it, and the proof kept is the block, its date and the transaction; Integritas gives no Merkle proof in v2. *Seen:* DesktopStampTest against a stand-in Integritas (the watch looking until it lands, the block, txpow and transaction kept, giving up after 30 looks and Verify again; the PC's box step by step, centred, the note's line before and after a change, the stamped version opened read only and put back, the list), StampLineTest (the line, which stamp a note stands by, the waits, the record that stands, the details copied), SchemaMigrationsTest. *Not seen:* a real Integritas account, the phone's box on a phone.

219. **The owner, 2026-10-19, on My profile's Text size: "we need more ladders here in the app level font size, and please make it that we don't need to close the app if possible, as we have at the note level".** In 0.4.010, PC. Nine steps, said as a share of the usual size (80 % to 180 %), kept as that size so the five steps of before are read as the size they were. Windows' scaling is read once, as the app starts, so the whole app cannot be resized while it runs (a note's size is drawn by Mininotes itself, which is why it can): a restart it takes. In 0.4.010 Mininotes started again by itself when a step was chosen; the owner, the same day: "if it is not possible to reflect the change without a restart, leave it nevertheless manually": from 0.4.011, Restart now does it, and opens My profile at Text size again.

220. **The owner, 2026-10-19: "we have to display clearly the updated folder and note: when I push an update from the laptop, the updated notes on phones should present it in their icon; maybe we need one more icon in our list with the check mark to illustrate something freshly updated".** In 0.4.010, both apps. A note changed on another of the owner's own devices, and every folder it is in, wears ✓ in the place "new" is worn, until the note is opened here; "new" stays for what somebody else changed or sent, and wins where both are true. Kept in this device's settings beside "new", never sent.

221. **The owner, 2026-10-19: "Again, this needs a full description in the README file; name it this way too, README, as people are used to see it everywhere including on GitHub. And these should match: the note in the app and what is available on GitHub, so please make sure everything we have now in both places is combined in one global version".** In 0.4.010, both apps. README.md is the one text: everything Read me said that it lacked (Home's +, holding and right-click, dragging and the strip, pages and pinch, colours and icons, the places, People and devices, sharing, pictures, the writing marks, private spaces in full and the emergency contact, both with their codes written as words, photos, the Agent, Stamp on Minima, the tips and keeping it safe) is in its numbered sections. The note is made from it: help/README.md, a copy of the root README.md kept identical (ReadMeTextTest fails when they differ), is turned into the note's writing by ReadMe (headings and all-bold lines bold lines without their numbers, links their words with a web address in brackets, pictures and badges dropped, a table row "- first cell: second cell", code blocks plain lines, the lines of a paragraph joined, bold and italic marks kept). A note holds 24,000 characters and the whole README about 35,000, so what only GitHub needs (the contents, What's new, installing, the table of every menu entry, which the PC says under the pointer, building, the repository's files, the licence's history) sits between two HTML comments GitHub does not show, <!-- not in the app --> and <!-- in the app again -->, and is left out of the note; ReadMeTextTest fails past 21,000 characters, under the PC's warning at 21,600. The note is called README; an untouched Read me (one still saying exactly what a build wrote) is renamed README and said again, the old words kept as a version; one somebody wrote in keeps its name and words, and Show on Home and the icon know both names. Since the text is now made, help/README.sha256 lists the SHA-256 of every text a build wrote, appended whenever README.md changes (ReadMeTextTest fails until it is), so a later README still reaches a note nobody changed, on any device.

222. **The owner, 2026-10-19: "on my Pro phone the micro to dictate text in the keyboard now says it is unavailable in the note, and on Graphene it is not working; it was previously", then, asked whether to trade it for decision 111's keyboard that learns nothing, "a switch in Settings".** In 0.4.011, the phone. Keyboards turn their voice typing off where a field asks them not to learn (their incognito), which every note had asked since decision 111. Settings has Keyboard learns from my notes, on by default: on, notes let the keyboard learn and so dictate; off, incognito as before. A private space's page, and every password field, stay incognito whatever it says. Also, PC: the New box's faces drawn with room so their edge is never cut, and the box wider.

223. **The owner, 2026-10-19: "I filled Integritas on the laptop and specified that I wanted that reflected on both phones, but on the Pro it is not there".** In 0.4.012, both apps. Nothing had gone: the device switches under the key were read only by Save, and remembered nowhere, so a phone switched on after Save got nothing, and the switches showed off again next time. Now the devices chosen are remembered on this device (settings, keyTo.integritas); a device switched on gets the saved key at once; Save sends it to every device switched on, and so does linking from the stamping box; and a send that finds no device says so in the log. Taking it in is as before: only from a paired device marked as yours.

224. **The owner, 2026-10-19, after a real stamp on the PC (block 2374505, the PDF report fine): "I was expecting to find Stamp on Minima in the This note section of the menu; can you put it there?"; "The text is written a bit small in this window; make it the same size as the buttons' text"; "In the details we should link as much as possible to the Minima explorer, like we have in the PDF report"; "A right click on an attachment should also bring as many functionalities as possible, including stamping on Minima. And the file should be marked as stamped when done, and all the details should be accessible again from the right click".** In 0.4.012, both apps. The ⋯ menu's This note has Stamp on Minima… and Verify on Minima… after Versions…, and first, where the note is stamped, its line ("Stamped on Minima, <date>, block <n>", waiting, or not found yet), opening the stamp's box; the phone's This note had them, and has the line now too. The note's own menu (a right click on its icon, and This note on the page) keeps them as before. The stamping box, the verifying box and Stamped on Minima… write labels and values at the buttons' size (PC: the look's button font; phone: the reading size); a fingerprint, a request or a transaction is whole, in letters of one width, wrapped where the box ends (it was a field cut at the left), with Copy beside its label. Linked to the Minima Explorer, as the report does, where the explorer shows the thing (its own forms, read from explorer.minima.global on 2026-10-19: a transaction at /transactions/<txpow id>, and a block or an address by its search, /search?q=<number or address>, the way the explorer itself links an address): the block, the transaction (txpow), the Minima address, and the NFT's transaction; each value in the accent, clicked to open it, with Minima Explorer › beside Copy (PC: the browser; phone: ACTION_VIEW). The transaction's id and the NFT's coin id are copied, not linked: the explorer's search does not find them. The NFT is what Integritas mints for a verification with its report: verify/file then answers nftCoinId and nftTxnId (v2 response schema); they are kept with every stamp of that fingerprint (schema 58 adds nft and nfttx to stamped; nothing already there changes) and shown in the box and the details copied. The list said "not found yet" beside the box's block 2374505 because they were two rows: an older stamp of the same text, given up on (or copied in from before 0.4.010 as not seen), and the newer one the watch found; the watch keeps only the row it looks for. Now a stamp landing, and the list opening, make every stamp of a fingerprint found on Minima say so, at the oldest record kept (NoteStore.stampsAgree); and the list is read again whenever the watch, a verification or a report hears something, so it always shows what the notebook has. In the list, Open… is Details…, and a stamp not found yet offers Verify on Minima (its first look); Verify again only once it has been found. An attachment's menu (PC: its card's right click; phone: held) has what a file has on Home that an attachment can use: Open, Open with…, Show in Explorer (a linked file, PC), Rename… and Replace with another file… (where this device may), Copy, Send with another app… (phone), Stamp on Minima…, Verify on Minima…, Save on my computer… / my phone…, Send to a device…, Move to another note… (Put in a note's own box) and Move to bin, each the existing one. A stamped file wears a small stamp on its card, which opens its box, and its menu starts with its stamp line, opening the box with all the details; Home's file menu starts with the same line. Not done: the line in the paperclip's list. *Seen:* DesktopStampTest (the list showing the older row found, block 2374505, Verify on Minima on one not found and Verify again on one found, Details…, the explorer's links, a stamp landing while the list is open, the words at the buttons' size; a stamped file's mark and its menu starting with its line, the attachment's menu's rows; This note's rows), StampLineTest (the links' forms, the details), IntegritasTest (the NFT read), SchemaMigrationsTest, WordsEverywhereTest, DesktopGalleryTest, pictures 218-stamp-box, 224-stamped-list and 47-menu-attachment. *Not seen:* a real Integritas report's NFT, the explorer pages opened from the app, the phone's box, list and menus on a phone (compiled only).

225. **The owner, 2026-10-20: "Measure Mininotes' CPU on Graphene while it runs (top -H on its threads). It ran at about 360% before decisions 211 and 215. If it is still high, find the cause with logs or jdb stacks (it is a debug build) and fix it", and "close the known gaps, smallest first: Copy in the phone's photo viewer; the stamp line in the PC paperclip list; a deleted Photos folder coming back on a new device".** In 0.4.013, both apps. Measured on Graphene (0.4.012, three devices paired): about 4% of a core once settled, the node's upkeep about 0.17 s each half minute; but 431 s of CPU in its first nine minutes, 255 s of it on mininotes-each-beat, the upkeep's own thread. Its log said why: at every start of the app the upkeep said again, to every paired device, each thing its build knows (trees, files on their own, a refusal, a help request, your own Parlons! address and picture, who a device belongs to, groups, Parlons! addresses, the name we pick, persons), and handed your own address, picture and card again, about 30 messages, each a Maxima message with its proof of work; and each device said its own back. What hears them keeps them for good. Now what a device was told and took is kept on this device for a day ("said." in the post settings, see Said), and the cards with what they said: a start within the day says nothing already taken; once the day is out, or where a card changed, it goes as before, and a device's word about persons anew still has the card sent again. Copy is on Home's file menu, after Rename… and Replace…, as on the PC, so the photo viewer's ⋮ has it. The PC's paperclip list says a stamped file's stamp after its size ("Stamped on Minima, <date>, block <n>", or waiting), as its card's mark does. And a new device that becomes one person with the owner's other devices before it made a Photos of its own takes theirs as it is, deleted included, and makes none at a later start: it had made one under the shared id, which then went to every device where the owner had deleted it. Found on the way: an install (adb install -r, or the app's own update) stops the app, and the phone listens again only once Mininotes is opened, as it is after a restart of the phone; left as built, for the owner to decide.

226. **The owner, 2026-10-20, asked whether Mininotes should listen again by itself after an install or a restart of the phone, as Graphene had missed the Integritas key for want of being opened: "Start by itself".** In 0.4.014, the phone (Starting): after the phone starts and after the app is updated it listens again by itself, where it listens at all (Listening.settle, as at an opening), its line in the notifications as always (RECEIVE_BOOT_COMPLETED). Seen on Graphene: "Start proc for broadcast Starting", then the listening service up, a few seconds after the install. Android sends neither to an app that was force-stopped and not opened since, which is what the backup before an install by adb does: those installs still need it opened once; an update through the app itself does not stop it first. Found while measuring that start (the Pro's Mininotes stopped by its own install): Graphene at about 168% of a core, then 81%, for minutes; jdb samples on its upkeep's thread showed the proof of work of "who this device belongs to" and its card, tried again for the unreachable Pro every minute and at every half-minute beat, each try a proof of work for each of its doors, its home and the relays. From 0.4.015, both apps: a device a receipt or a card (persons, groups, your own address and picture, who a device belongs to) could not be handed to is left alone a minute, then twice as long each time to half an hour (Said.rest), and tried again at once when anything arrives from it. Notes go as before.

227. **The owner, 2026-10-21: "It seems a picture attached to the temp folder will be sync with other device, it seems waything for a note, can you fix that?", then "it is now there, just took really long but also I'm wondering if it went because I created a note beside it in the same temp folder".** In 0.4.016, both apps. Watched on the Pro with a picture put on Temp alone: it went up and its sleeve went within two seconds (02:55:05 to 02:55:07), so it did not wait for a note; but the PC and Graphene had tried to fetch it at once, found its pieces not there yet ("chunk unavailable"), and then waited the minute a failure waits plus the next round: it came at 02:56:46 and 02:57:25. Its sleeve, said again a moment later once it was up, changed nothing, as its list of pieces was the same. Now a sleeve said again for a file whose last try failed is news: it is tried again at once, at most once in a quarter minute (NoteStore.SLEEVE_RETRY). And the box a press on a file's amber mark opens says what it waits for, "Waiting for <device> to confirm 1 file", and lists the file, where it said only "Your latest words go as soon as they are written down", words about writing a file never has (NoteStore.toConfirm, as the mark counts). Tried again after 0.4.016, a picture reached the PC within a second and Graphene in 1 min 47 s: Graphene, opened at that moment, tried 25 seconds in while its node was still starting, failed, and had no later sleeve to wake it. From 0.4.017 a file being fetched is tried again a quarter minute after its first failure, not a minute (Enclosure.fetchDue), then as before; uploads and files sent on their own keep their times.

228. **The owner, 2026-10-21, passing on a user's feedback with pictures of the same Chinese text in Xiaomi's notes and in Mininotes: "When copying and pasting text into MiniNotes, the paragraphs disappear, whereas Xiaomi's built-in notes app retains them. This issue needs to be improved", and "Please have a look at the feedback received".** In 0.4.017, both apps. Android lays a line out only at '
'; text copied from some apps ends its paragraphs with the Unicode paragraph or line separator (U+2029, U+2028), NEXT LINE (U+0085), "
" or a lone '
', so the paragraphs ran on as one. What is pasted, dropped or sent by the keyboard now has each of those made a '
' ("
" one, not two) before it lands (LineEnds; the phone's page filter, the PC's DesktopPrivate.Catch), and nothing else in the words changes; a note opened is laid down as it was saved. Not changed: an HTML entity written out in the text, such as the "&nbsp;" in the user's picture, is the source's own text and stays as it is. Also asked: "How can we make sure our app can be verified in appverifier?" AppVerifier takes no new apps into its own list ("Contribution of new apps to the internal verification info database won't be accepted at this time"), and verifies from verification info the developer publishes as an official source: README's Install it now gives the package and the release key's SHA-256 fingerprint in its form, to copy and use with Verify from clipboard. The owner's phones run debug builds, whose key is not that one.

229. **The owner, 2026-10-21: "let's make sure Temp folder is listed in recent when used".** In 0.4.017, both apps. Temp, a place with no row of its own, is listed in Recent, by when it was last opened or given something (put in Temp; taken off it is not a use), kept on this device (settings, tempUsed) and never sent; a line "Temp" with how many things it holds, opening Temp; Remove from Recent takes it out until it is used again. The PC's Recent down the side opens it too; the cards of what is open (Overview) leave it out, being a place.

230. **The owner, 2026-10-21: "Why integritas/stamping doesnt include the title of the note? I think it should. Please lets discuss first"; then, asked: renaming a stamped note marks it as changed, "Yes, absolutely"; "yes, Old stamps stay text-only and labelled as such"; "When we click in the menu, stamped on Minima, we should just have the list of everything we stamped, not directly the details, this should come afterward when clicking one element"; "Move the Stamped on Minima in find section of the menu. And in there, with the list of elements stamped, we should have a + button, also we should mention that it uses integritas with a know more with the link to their website"; and of the +, "It should give the option, from the app or external from the drive".** In 0.4.018, both apps. A note's stamp covers its title, an empty line, then its text, as saved (Integritas.noteWords: the PC's Copy the text gives exactly that, so anybody can copy a note and make the same SHA-256; a note with no title, its text alone), kind "titled"; stamps made before keep kind "note", are compared by their text alone and say Note, text only. Each stamp is compared in its own form (Integritas.matching), so renaming a note stamped with its title marks it Changed since it was stamped, with Stamp this version…; Verify on Minima… looks for the title and text first, then the text alone, and says which it found. Stamped on Minima… is the last entry of Find, on both apps, and lists the stamps only, newest first: name, kind, date and how far (block, waiting, not found yet); a click or tap opens that stamp's box with all its details. At its top, a line saying stamping uses Integritas, with your own account, recording each fingerprint on the Minima blockchain, and Know more opening integritas.technology; and a + offering A note or file in Mininotes… (a chooser with search) or A file on this PC… / A file on this phone…: a file from the drive or the phone is fingerprinted and stamped as kind "outside", never put in Mininotes, and its Verify on Minima… asks for the file again and compares. Files in Mininotes are stamped by their bytes as before; their names are not in it.

231. **The owner, 2026-10-21, on the Stamped on Minima window: "Remove the done and put the plus there similar to the + on the home page"; "Make know more a clickable link and use the integritas.technology label"; "Your Integritas account needs a window with the full settings, where we have the option to set it across, actually we need to be brought on the dedicated section on the profile page and in there we have to label this section clearly, stamping"; "In the profile page, the sections need to be better delimited and labelled, for now it is difficult to find what we are looking for"; "make it clear that this is a section of stamped elements, label it and list the elements underneath".** In 0.4.019, both apps. Stamped on Minima has no Done (it closes as any window or box does); Home's own round + sits where Done was, offering what to stamp; the line about Integritas is followed by a link, integritas.technology; the stamps are under a heading, Stamped notes and files. Your Integritas account… opens My profile at its part now called Stamping, the one place for the key, Save, the link and Also on my devices (the small account box is gone; where a stamp or a verification needed a key, it opens there too). My profile's parts are each a card with its symbol, a heading and a line saying what it holds, more space between them, and a row of their names at the top going to each. The Agent's Take me there names Stamping. On the phone, the part's old line about sending your location, which belongs to the help request set in a private note (decision 201), was taken out of it.

232. **The owner, 2026-10-21: "When we install the app for the first time, there should be a step where we set the language. So the user gets everything in the language of its choice, including the README right away"; then, asked, the README translated here and kept in step with every change ("I translate, every change"), and the step at the first start of a new install on both apps.** In 0.4.019, both apps. A new install asks its language before anything is written (the phone: before README and My first note are made; the PC: before the notebook exists): the eleven languages, each in its own words, the device's own first and chosen; the device's own kept as "the device's language", so it follows the device. An install already in use is never asked. README.md is translated whole into the ten languages as README.<code>.md beside it (zh, es, fr, de, pt, it, ja, ru, hi, fa), linked at its top, each saying on its first line the SHA-256 of the README.md it was made from, so a test fails when README.md changes until every translation is brought up to it; the app keeps copies (help/README.<code>.md) and makes the note from the one in the language in use (ReadMe.text), My first note in it too. Changing the language (Settings, Language) says an untouched README again in the new language; at a start an untouched README is said again only where it is English or this device's own language and older, so two of the owner's devices in two languages never write it back and forth. The codes stay written as words, the code words in English.

233. **The owner, 2026-10-21: "Would be good if after a screen grab with greenshot, a ctrl+v would let us put the image directly in a folder, it works in a note already"; then "if we type nothing in a note in temp and only add a picture the way I described above, the note is not saved when we press the back arrow", and "actually it seems the note is saved on the home page but not in the Temp folder ... this should not happen".** In 0.4.019. PC: Ctrl+V where no field has the keys, on Home, in a folder or in Temp, keeps the picture on the clipboard there as a PNG named for when it came, as one pasted on a note is, or the files copied in Explorer as if let go there; a note's page and every field paste for themselves. The phone: a note made from a place's + was put in the place by its first written word (decision 87), so one given only a picture stayed on Home; now its first file puts it there too (filesChanged), and so does leaving it with a file and no words. The PC, which puts such a note in its place when it is made, was right already (DesktopTempPictureTest, the owner's steps: Temp's card, its +, a picture, nothing typed, back).

234. **The owner, 2026-10-22, with a picture of the French README on the PC: "Many characters issues in the README, see image".** In 0.4.020, the PC. Lines were drawn over one another where a note has bold, italic, links or writers' colours: those are drawn a second time over the plain letters, from a box one pixel wider than the one the page lays its lines out in (the caret's width), so at some window widths a line took one more word in that pass and every line under it rose a row there, its words drawn where the page's bold or italic was. The three passes now draw from the page's own box (Desktop.Paper.laidOut; DesktopPageOnceTest, failing before with 48 stray pixels). And README's one ***bold italic*** label, which no note styles, is bold in it and in its ten translations.

235. **The owner, 2026-10-22: "Why while install mininotes cant be found on the list of app in windows and how would a user uninstall it?"; then "I installed the app on a second laptop using Mininotes.exe and cant find it in app, also nothing to uninstall"; a real installer chosen; "at uninstall, there should be a warning that everything in the app will be deleted if not backed up. Generally speaking we should propose a backup system. It should be clear that any private element cant be backed up"; and, asked, uninstalling from Windows' Settings keeps the notes.** In 0.4.020, the PC. Besides the ZIP, each build makes an MSI (jpackage with WiX 3.14), installed for the person alone with no administrator rights in %LOCALAPPDATA%\Mininotes App (never the notebook's own folder), in the Start menu and in Settings, Apps; a newer MSI replaces an older one (one upgrade id, never to change). An MSI copy updates itself by the next MSI, an unzipped one by its ZIP as before. Uninstalling from Settings removes the program and keeps the notebook, found again by a later install; Uninstall Mininotes… in the menu says that uninstalling deletes everything kept in Mininotes on this PC unless it is backed up, and that private spaces are never in a backup, offers Back up first…, and keeps the notes unless Keep my notes on this PC is turned off and confirmed. A backup is proposed: My profile's Backup says private spaces are never in it, and once a start, where none was made in 30 days and there are notes, the bar says No backup in 30 days. Back up now….

236. **The owner, 2026-10-22: "On the phones we should have a notification at the phone level and on the app icon when something new arrives"; asked, one notification gathering everything new, a sound only for the first after a quiet spell.** In 0.4.020, the phone. What arrives while no screen of the app is open is gathered in one notification (ArrivalNews): the sender, or "New in Mininotes", and "3 notes updated, 1 file", from whom, never a note's text; its count on the app's icon where the launcher shows counts; on a channel of its own, What arrived, that may sound, alerting only for the first arrival after ten minutes without one or since the app was last opened, then updating quietly; in the language in use; cleared when the app is opened. Help requests keep their own loud line. The earlier channels are kept, so nobody's settings for them are lost.

237. **The owner, 2026-10-24, about the installer: "can we display our logo there, where is the one displayed coming from? The general licence there, is it coming from fsf directly is it a generic one, why do we need that? When we click finish we should have the option to launch the app."** In 0.4.021, the PC. The picture on the installer's pages was WiX's own, its red and white artwork, shown when given none; the licence page showed the generic text jpackage passes when asked for one, which Mininotes does not need: the app's licence is in README and in the app. The installer now has its own pages (windows/installer/main.wxs, from the JDK 17 template): the Mininotes logo in the banner and on the first and last pages (windows/installer/make-images.ps1 draws them from the logo's own geometry, as DesktopIcon does), no licence page, and on the last page Launch Mininotes, ticked, which starts the installed app when Finish is pressed. Checked by reading the built MSI's tables.

238. **The owner, 2026-10-24: "Again, adding a note to temp, without title saved it on the desktop not in temp".** In 0.4.021, the phone. A note made from a place's + (Temp, the archive, the bin, Favourites) is put there at its first written word (decision 87), and where it was to go lived only in the screen's memory: a screen made again before that word (the phone turned, dark mode, the notebook locked while away, Android closing the app behind the camera) came back without it, and the words went on Home. The note and its place are now kept with the screen and with where the app was left (WaitingPlace), so the page that comes back is the same note, still on its way to Temp. WaitingPlaceTest and DesktopTempUntitledTest.

239. **The owner, 2026-10-24: "We have to be able to drag and reorder the tabs in a note".** In 0.4.021, both apps. A tab is carried along the strip and let go where it should go: dragged on the PC, held then carried on the phone (a hold let go where it was still opens its menu). Its menu also has Move left and Move right. A move takes the tab's line and its words together, is an edit of the note like any other (it travels and can be undone), and the open tab stays open; a first tab with no name takes the name the strip shows for it, Page 1, once it moves or another goes before it (Sections.move and Sections.follow; SectionsTest, DesktopTabsTest).

240. **The owner, 2026-10-24: "When launching for the first time, or until the language hasnt been confirmed, a pop up window should ask the user to pic his language".** In 0.4.021, both apps. Until now only a new install was asked (decision 232). Every start now asks until a language has been confirmed once, in that box or in Settings: a new install before its notebook is made, so README and the first note are written in that language; an install already in use once it is open, the language in use first and chosen. The demo build never asks (LanguageAsk; LanguageAskTest).

241. **The owner, 2026-10-24: "In both platform we should present the other, in windows the android and reverse."** In 0.4.021, both apps. The PC's menu has Mininotes on your phone…, near About: that Mininotes is also on Android phones and keeps the same notes in step once paired, a code for the phone's camera, the link mininotes.org/android written out with Copy link, and Done. The phone's menu has Mininotes on your PC…, the same for Windows, and its About says so too (DesktopOtherApp; DesktopOtherAppTest).

242. **The owner, 2026-10-24, on the Graphene with 0.4.021: "I picked french but it is not reflected and the window keeps coming back at every opening of the app".** In 0.4.022, the phone. The language box of decision 240 ticked a language on a tap and kept it only with Continue, under the list; on a phone the list is taller than the screen, so Continue was out of sight: the tick moved, nothing was kept, and the language never being confirmed, the box came back at every opening. Now a tap on a language takes it and closes the box, as the list in Settings does, and there is no Continue. The PC's box, which fits its window with Continue in view, is unchanged.

243. **The owner, 2026-10-24, after 0.4.022 went on: "at first mininotes was empty on mobile, I needed to close it so that it was filled with my stuff on Pro"; then "we have the same on the desktop now, you can have a look, nothing is displayed on the home page".** In 0.4.023, both apps (shared code). Once the PC was unlocked, it was told "they have a file" more than a thousand times in ten minutes, and the Pro went on saying "7 file(s) ... told it of all" every two or three seconds. A device answers a list naming files it has with "I have this file", once a run for each file and device (decision 211); but each answer costs a proof of work, and a file was marked as said only once it had gone, so a list coming every two seconds found the last answers still on their way and queued them all again, without end, keeping both ends busy. Now each file is marked as it is queued, and given back only where it did not go (Post.reserve, Post.release; SaidHaveTest). Home waiting behind that work is the likely reason it stayed empty; not proven, as the owner's screen is not looked at.

244. **The owner's rule of 2026-10-17 (decision 186): "make sure everything is translated ... can you put a rule for that, a structure making sure we don't miss any element?"** In 0.4.024, the PC. A made-up notebook started in French on 0.4.023 showed English on screen: the search field, the empty Favourites strip, tooltips, the note's line (Saving…, Sending to …), the status bar's messages, dialogs and the tray. The words were on the lists, but the PC handed the English to Swing without Say.t, and WordsEverywhereTest only checked that a phrase was listed. About 270 places now say it in the language chosen (a sentence around a name as one phrase with {0}); 127 new phrases in the ten languages. The note's line keeps its English apart (noteWordsSaid) for the checks that read it; the connection dots still take English, as they choose their colour from it and translate themselves. The first start's language box draws each language's name in a face that has its letters (Hindi showed as boxes). New in WordsEverywhereTest: a PC screen call given bare English fails, naming the file and line. Still English: sentences the shared code builds from several pieces where no phrase matches, names read only by a screen reader, and menu descriptions (describe.txt).

245. **The owner, 2026-10-25: "yes a new notebook's first folder should take the chosen language".** In 0.4.024, both apps (shared code). The first migration writes My notes and the Notes inside it in English, before a language is known, so a notebook started in French had Ma première note beside My notes. At the first start (NoteStore.welcome), a notebook with no note yet has them renamed to the language chosen, only where the English name is still there: a backup put back or a device joining keeps its folders' names (NoteStore.nameFirstFolders; ReadMeTranslatedTest).

246. **The owner, 2026-10-25: "the pro says Integritas est injoignable, what is causing that how can we prevent that?"** In 0.4.024, both apps (shared code). Read that morning: both of the app's Integritas hosts, integritas.minima.global and integritas.technology, are one server (147.93.126.192), so the second was no help when that server or the phone's way out had a moment's trouble; both answered from the PC and from the Pro when looked at (401 without a key, under half a second). What failed was not kept, so the cause on the Pro cannot be told after the fact. Now: a round that failed quickly (no network yet, a refusal, a 502 or 503) is tried once more after three seconds, one that waited out its 30 seconds is not; the reason goes in the log as Mininotes/Integritas (a code or a kind of failure, never the key or what was stamped); and a stamp looked for in the background says Integritas cannot be reached only after three looks in a row without an answer, while Verify again still says it at once (Integritas.post, Watch; IntegritasTest).

247. **Seen on the Pro, 2026-10-25 10:45, after the owner opened Mininotes on 0.4.023: Android's "not responding" (an ANR), the app closed by the owner from its box.** In 0.4.024, both apps (shared code). Read from the phone's ANR record (stack frames only): the screen had just been drawn again, and its start (Listening.watch, Node.near) waited on the Node class's lock, held by the listening thread for the whole start of the node, which waits on relays for seconds. The node is now made under a lock of its own (Node.started), and what the others read is handed over under the class's lock only once the relays are behind it; media() and blobs() no longer hold it while they start the node; Listening.watch and unwatch have their own lock, as hear() holds the class's through the same start. The likely reason Home stayed empty after the language box (decision 243's open question): the screen waiting there. NodeMakingTest.

248. **The owner, 2026-10-25: "since nouveau could we write nouv. at the note level. apply that to other languages where it makes sense".** In 0.4.024, both apps. A note's icon wears its own line, "new (on a note)": nouv. in French; the others keep their word, already short (neu, nuevo, nuovo, novo, новое, تازه, नया, 新規, 新), and "new" at the top of Home is unchanged (Say.onNote; SayTest). The PC drew that mark, "uploading", "Main page" and the icon picker's "No icon is called that" in English in every language; translated, and WordsEverywhereTest now reads drawString too.

249. **The owner, 2026-10-25, after stamping on 0.4.024: "It worked good. only, it says hash anchored successfully, first this should be translated and then lower it says that it is still progressing, this is a bit confusing. Then the file should carry the stamped marked on its icon".** In 0.4.025, both apps. The box quoted Integritas' own English, "Hash anchored successfully.", which means it took the fingerprint and sent it to Minima, not that Minima has it, while step 3 below said it was still waiting: now step 2 says "Integritas took it and sent it on to Minima." in the language chosen, so waiting for Minima follows from it; anything else Integritas says is still quoted (Integritas.answered). A stamped file wears a stamp on its icon's lower right corner, on Home and in a folder, as an attachment's card did since decision 224: in the accent once Minima has it, quiet while it is on its way (NoteStore.stampMark, Branch.stamped); Home is drawn again as a file is sent and as Minima is found to have it. The PC's "uploading" pill is measured in the words it shows. IntegritasTest.

250. **The owner, 2026-10-25: "when I receive a file, that I can compare it to the hash that I have"; then "yes build the fingerprint comparison now for 0.4.025".** In 0.4.025, both apps. A file's menu, on Home and on an attachment's card, has Compare with a fingerprint… after Verify on Minima…: the box shows the file's own SHA-256, made here from its bytes, and a field for the one the person has, filled from the clipboard where one is there; it answers as it is typed: "The same: this is exactly that file.", "Not the same: this is not that file, or it changed since.", or that what was pasted is not a SHA-256. A fingerprint is found with or without 0x, in capitals, with spaces or colons between pairs, with words around it (Integritas.fingerprintIn). Nothing leaves the device and no account is needed. IntegritasTest, DesktopStampTest, pictures 250-compare-same, 250-compare-not-same and 250-home-stamped-file.

251. **The owner, 2026-10-25: "is there a way, when I received a file or send one that the stamp tavels with it, so in the right click, the person can have the onchain check or even that we could attached the integritas pdf report to it"; answered: "yes carry its stamp automatically when shared", "PDF report go as an option".** In 0.4.028, both apps (shared code); built by a helper agent and checked here. A stamped note or file shared with somebody else carries its stamps: the rows of the stamps card (decision 252), at most 32, newest first, behind a mark of their own (MNW1) at the very end of what goes, after the last field a sleeve has (a file on its own) and after a note's last tail mark, where every build from before stops reading, so it reads the note or the file as it always did; a note carries its own stamps and its attachments'. They are taken only with the thing itself, where the note is taken in or the file from one of mine or one who may write it, and only for that note and its files; merged as the stamps card's are. Shown in the one window (decision 253), which takes a stamp as this version's only where its fingerprint is the one of what came, so a different version lists under Stamped before; the Minima Explorer's links let anybody check it without an account. A stamp made, found on Minima or given its NFT after sharing sends the thing again to whoever has it, at the same revision (Post.stampsOwed), not to my own devices, which have the stamps card. A device with no Integritas key no longer looks for the stamps it was given until it gives up. The report: a file shared on its own whose report is kept beside it has Send its Integritas report too in its share, off until chosen; on, the report is given the same role as the file to everybody who has it and whoever is given it later (NoteStore.reportGoesWith); a note's report is its attachment and goes with it. StampsWithSharesTest.

252. **The owner, 2026-10-25: "I horodated [a spreadsheet] from the pro, this is not visible on the laptop, can you make sure it updates".** In 0.4.026, both apps (shared code). A stamp was kept only on the device that made it. Now every device of the owner's sends the others a stamps card (StampCard, MNQ1): every stamp it holds, the stamped words of a note with it, newest first and bounded to an envelope; sent only to a paired device marked as mine that has said it reads it (Receipt.STAMPS, 39), again only when the card changed, promptly after a stamp is made, found on Minima or given its NFT (Post.stampsChanged), and every round otherwise; taken in only from a device of mine, never when carried for another. Merged by the stamp's request (its id at Integritas), or by thing, fingerprint and time where an old one has none: a stamp not known is added, one known takes the block and transactions once the other device found it on Minima, never deleted, never taken back to waiting (NoteStore.stampsArrived). The icons, the note's line and the stamps list follow. Two faults found on the way: the Integritas key card went to any device of mine, whatever its build, and one from before 0.4.006 would have kept it as a note: it now goes only to a device that has said STAMPS, so a device on 0.4.006 to 0.4.023 gets it once updated; and the About me card shared its first bytes with the Persons card and was read as one, so it never arrived: it is now recognised first, strictly. StampCardTest, StampsTravelTest.

253. **The owner, 2026-10-25: "I think we should have a combined windows with everything relating to horodating and only one element in the contextual menu of the note, so combining the three menu elements we currently have and making sure I can atached the integritas pdf report and chooe to have it traveling with the device it is shared with".** In 0.4.026, both apps. A note's or a file's menu has one line about Minima, Stamp on Minima…, which once it is stamped says how far its newest stamp is; Verify on Minima…, Compare with a fingerprint… and the stamp's own line are gone from the menus. It opens one window: the fingerprint of what it is now, made here; where that version is stamped, its stamp with every detail, Open this version, Open the PDF report, Verify again, Attach the PDF report (a note) or Keep the PDF report beside it (a file), and Compare with a fingerprint… (a file); where it is not, Stamp, Look for it on Minima (somebody may have stamped the same), and Compare…; the versions stamped before; when it changed since its last stamp, said first. Seeing it asks for no account; Stamp without one opens My profile at Stamping. The report is fetched while Integritas' link lasts and kept as any file is, sealed when the notebook is locked: attached to the note, or beside the file (attached to the same note, or in the same folder), so it goes where they go. Choosing per share whether it travels is decision 251's, next. DesktopStampTest (the window unstamped and stamped, the report attached), pictures 253-one-window-unstamped and 253-one-window-stamped. The owner, after trying it on 0.4.026: "The combined Intergritas menu seems right. In french we should say Attacher le rapport PDF, not garder le rapport PDF à côté": in French both lines say Attacher le rapport PDF, and their notices attaché (in 0.4.027).

254. **The owner, 2026-10-25: "when we display the languages window first, put the message in englosh and order the language alphabetically".** In 0.4.028, both apps. The language box of decisions 232 and 240 is made with the app's words in English, whatever the device is set to (LanguageAsk.inEnglish), so whoever opens it first reads it in the language most people read; each language keeps its own name. The languages are listed in the alphabetical order of those names (Deutsch, English, Español, Français, Italiano, Português, Русский, then the other scripts), the one in use ticked where it falls rather than moved first (LanguageAsk.order). LanguageAskTest.

255. **The owner, 2026-10-25, after pairing a new laptop with a QR code: "from people and device, I wanted to only have the new device in my connected device, not necessary share everything. Just addidnt it and tehn choosing. make sure we can do so"; then "from my Pro menu personnes et appareils, prressing + beside nes appareils took a bit long... but finaly worked".** In 0.4.029, both apps; built by a helper agent and checked here. Add my device made a code for everything, so adding a device of mine gave it the whole notebook. Now its box asks first what it gets: Nothing yet, chosen, or everything to read or to write. Nothing yet adds it as one of mine (listed under My devices, given the cards a device of mine has, Temp to my devices, the Integritas key where chosen) with no library rule: it gets only what is then shared with it, and on its line in People and devices it says Nothing yet ▾, dropping down to the roles of everything. Its code is a library one (target mine), so a build from before still takes the other device as its owner's. The stamps card carries a stamped note's text, so a device of mine not given everything gets only the stamps of what reaches it (Post.stampsReaching). The + answered late: on the phone its box waited behind sync work on the network's one thread, its busy strip hidden behind People and devices; on the PC behind the disk and network threads, with only the status bar saying anything. Both open their box at once and make the code on a thread of its own (MainActivity.addMyDevice, Desktop.addMyDevice). Devices already paired keep their shares. AddMyDeviceTest.

256. **The owner, 2026-10-25: "When installing on the new latpop and sharing with a QR code, the message received on Pro is in englosh, now the error m,essage, RPo could not reach PixelPro is also in english even if I'm set in french on the new laptop".** In 0.4.029, both apps (shared code); built by a helper agent and checked here. Decision 244 put the PC's own screens in the language chosen; the sentences the shared classes build (Unsent's could not reach, the note's line, Waits, Given, Shrink, the first share, Drop, Linking, Parlons, Called, Persons, Sharing, the pairing boxes and others) were still English glued around names. Each is now one template per sentence (one and many, phone and PC, named device or not), 258 phrases in the eleven languages. What a code offers still travels in English, so a build from before reads it, and the device that receives it says it again in its own language (Sharing.shown). Checks that read a line back compare it in either language (NoteLine.isSending, Say.fits); a count's place takes digits only, so a note called Meeting notes stays as it is. New in WordsEverywhereTest: a shared class gluing English to a name fails; SharedWordsTest asks each in French. Still English: what travels, tab names, file names, sizes' units, log lines.

257. **The owner, 2026-10-25: "the graphene is really slow to react, please have a look".** In 0.4.030, both apps (shared code); built by a helper agent and checked here. Measured on the Graphene (0.4.029): from its start at 20:42 Mininotes used about 18 minutes of CPU in six, its two sending threads mining the proof of work of a send; copies of two notes were brought to it about 120 times and each was answered with its own receipt, each a task waiting on one thread with its bytes, until the heap (256 MB there) was full, every allocation waited on the collector, and at 21:01 the alarm's round overran Android's limit (not responding, on Waking). The phone itself was short of memory (7.3 of 7.6 GB, swap nearly full). Causes: the carrier took a copy of the revision it held as new and brought it again at once; a sender left a new copy with up to three carriers at every retry; a carrier brought everything again every 20 s while the device it was for kept talking; the receiver sealed a receipt for every copy. Now a carrier keeps one copy per device and note, the newest, from whoever left it, the same or an older revision being already held; a sender leaves a revision once an hour; a carrier brings at once only to a device come back from away, then on its turn; one collected receipt per carrier and note goes per ten minutes, one waiting at a time for the newest revision; the announcements a device accepted but never answered are said again after 10, 20, 40, 80 minutes, then two hours at most; the alarm's round runs on a thread of its own and the alarm lets go after 7 s (Courier.Lately, Courier.Queued, Said.unanswered, Waking). CarriedOnceTest.

258. **The owner, 2026-10-25: "In personnes et appareils, its good that I can show my QR code but the + should bring the camera so that I can scan one code. like what we have in personnes, let's have just one big + button similar to what we have on the home page for adding notes and others and then once scanned, we should be invited to define what it is, own devce or not, put this in a group, what to share and so on there must be a clear path as unified as possible always, not start with a branch Personnes or Appareils and so on".** In 0.4.030, both apps; built by a helper agent and checked here (the PC's pictures looked at). People and devices has one round +, Home's, at its foot, and Show my code… quiet beside it; the + beside People and beside My devices are gone, the two sections the same. The + opens the camera at once (Paste a link, Open a picture and Show my code instead quiet under it). Every code, scanned or pasted, from here or from a Share box, goes the same way, a step at a time, the one filled button the next thing: What is it? (One of my devices or Someone else, chosen from what the code says where it says it); for someone else, a group (No group, a group, New group…); What it gets (a device of mine: Nothing yet, Can write or Can read of everything, decision 255; someone else: Nothing yet, or notes and folders of Home chosen one by one with their rights); then Add (Accept where the code brings a share), said as it goes and when done (Joining, JoiningBox, DesktopJoining; no new protocol). A plain code taken as one of my devices is mine here only until the other device turns My device on too, as before; the step says so. JoiningTest, pictures connect-01 to connect-17.

259. **The owner, 2026-10-26: "we should have the option to create a desktop icon when installing on windows".** In 0.4.031, the PC; built by a helper agent and checked here. The MSI has a page Shortcuts between its first page and Ready to install: Create a desktop shortcut, ticked, as every MSI before made one without asking. The shortcut is the installer's own (windows/installer/main.wxs): for this person, on the desktop, to the installed Mininotes.exe, made only where the box is ticked, gone at uninstall; the choice is kept (HKCU Software\Mininotes\Installer) so an update, which runs without its pages, keeps it. jpackage's own desktop shortcut, made whatever was chosen, is no longer asked for (build.ps1 without --win-shortcut); the Start menu entry stays. Read from the built MSI's tables (Shortcut, Component, Control, ControlEvent), not run.

260. **The owner, 2026-10-26: "One user told me that the automatic update process didnt work when i tried it, could you check that, I didn't test that on my side".** In 0.4.031, the PC; found by a helper agent and checked here. The update's script started in the folder Mininotes itself was working in, which is its own folder whenever a shortcut (Start in) or a double click opened it, and Windows does not move a folder a program is working in: every try to put the new copy in place failed, the old one opened again without a word, and the next start downloaded the update again, for every unzipped copy since the first that updated itself. Each script now starts in its own folder (DesktopUpdate.shell), which also serves Uninstall Mininotes…. Checked against the real release: every address the updaters of 0.4.015 to 0.4.030 build answers, the checksums match; they fail only at the swap, which no change on the release side can mend, so a person on one of them installs the MSI by hand once. The phone's update was found sound. DesktopUpdateTest (a copy opened from its own folder, failing without the change).

261. **Measured on the Graphene after 0.4.030 started, the owner agreeing ("ok, with 1, 2 and 3"): its upkeep's thread used 600 s of CPU in 2 h 40 min, about ten times decision 225's.** In 0.4.031, both apps (shared code); built by a helper agent and checked here. Profiled on the PC with a made-up notebook (500 notes, 100 files, 50 stamps, 10 things shared, 5 devices): every round worked out again which shared things owed their stamps (72 ms) and each device's stamps card (27 ms), each reading shares, what was sent and the addresses once per stamp, and every loop hashed each device's key again. Both are now kept until the notebook changes (NoteStore.changes, SQLite's count of what was written) and cheaper when they must be made again; a round that writes nothing does that work in well under a millisecond. Not yet measured on a phone. UpkeepRoundTest.

262. **"said this device takes files: not sent", in the log every ten minutes on the phones and the PC.** In 0.4.031, both apps (shared code). With notes going only between the owner's devices, a device out of reach makes a send wait (IllegalStateException, Direct.WAITS); this one announcement, alone among them, never let such a device rest and never kept what was taken, so it was said again every ten minutes and at every start. It now rests as the others do and is remembered a day (Drop.tellNow). DropTest.

263. **Decision 256's leftovers: sentences on either app's own screens still glued English around a name** (the move box, helpersFor, take off, the updates box, Parlons!, relays, the bin, backup, People, Private, Profile). In 0.4.031, both apps. WordsEverywhereTest read a screen call only up to the first +, so name+" is here" was never caught, nor either side of a choice; it now reads both, and every sentence it found is one template, 157 phrases in the eleven languages. Left English: descriptions read only by a screen reader, logs, what travels, file names.

264. **Decision 260 made the PC's update take; one that still does not is now said.** In 0.4.031, the PC. A swap that fails, or an MSI that does not finish, leaves a mark beside the copy with the version it was going to; at the next start Mininotes says once, in the status line: "The update to v{0} did not finish, so this is still v{1}. Click to update again, or install the MSI from the releases page.", opening the updates box (DesktopUpdate.unfinished). DesktopUpdateTest (a real swap kept from working, the mark, said once).

265. **Decision 263's leftover on the PC: files added to Home, a folder or another note than the open one were said with " to Home" or " to “name”" glued after the English " added"**, so in English it was the gluing 263 forbids and in any other language the place was left out, " added" not being found. In 0.4.032, both apps (shared code). Given.added takes where they went and says it in one template, 6 phrases in the eleven languages ("{0} added to {1}", fr "{0} ajouté dans {1}"); a folder with no name says no place, as the phone does. GivenTest, SharedWordsTest, DesktopDropOnNoteTest.

266. **The owner, 2026-10-28: "We need to be able to drag and drop files from the windows explorer into the temp folder or any open notes and folders, for now they fall on the home page when the temps folder is open"; "even the notes added to temp directly from the + end up on the home page".** In 0.4.032, the PC. Both came from one change made when Temp, Recent and the other places became cards: a place's card says it is Home when asked where it is (what is carried out of it goes there), and two things asked it. Files let go from Explorer on Temp's open card were kept on Home, though decision 150 put them in Temp: Temp's card, and the dimmed Home round any open card, now keep what is let go on them for what is open (Temp, or the folder). The card's round + made its note and folder on Home: it now makes them in the place where that place takes new things (Temp, the archive, the bin, Favourites; DesktopFolder.madeIn). The tests before called Temp's menu directly, not through its button. DesktopTempDropTest: five points on Temp's card, as Windows hands a drop to the component under the pointer; the card's own + pressed, with Temp shown on Home and not.

267. **The owner, 2026-10-28: "Make sure we can click and type anywhere in a note, not that we have to use enter to 'open it' so that we can type, we should also be able to scroll down note directly."** In 0.4.032, both apps (shared code); built by a helper agent and checked here. On the PC a click on the ruled paper under the last line did nothing: Enter made the lines first. On the phone a tap there made the lines (reachLine), but only on the paper a short note showed, and a save already due took the untyped lines with it. Now the paper goes on about a screen past the last line, whatever the note's length, so a note scrolls on until its last line is near the top, and a click or tap on any empty line puts the cursor there, the lines down to it made for you (BlankLines, one count for both). Paper is not writing: on the PC the clicked line is only drawn until something is typed (letters, Enter, a paste, a style), then made in one edit that Ctrl+Z takes back whole; on the phone the made lines are left out of what the note says and go again when the cursor leaves them. A click and nothing typed leaves no revision, nothing written and nothing sent. A note only read gets neither. The private space's page is left as it was. BlankLinesTest, DesktopPaperBelowTest. Not seen: the phone on a device (drag and fling onto the paper, the tap, typing speed in a long note).

268. **The owner, 2026-10-28: "in temp I should be able to link my other laptop. Actually when I link one additional of my devices, it should be linked to all of my devices or at least it should be clearly proposed to do so in the process. and if chosen to not link with all at the start, the device should be listed on all and easily linkable afterward."** With a picture of the PC's Temp box, where the owner's other laptop was under People as "Windows PC"; the owner chose the step proposed with every device ticked. In 0.4.032, both apps (shared code); built by a helper agent and checked here. The cause: a laptop that never chose the name other people see calls itself "Windows PC"; paired by a plain code on which the laptop chose One of my devices (decision 258), or linked through a list from the Pro, it stays a person on this PC, and only the switch at the foot of its page said otherwise; no card ever made a device mine. Now, where a device of mine is added (Add my device, or One of my devices on the + path), Also link it with comes first on the step, my other devices each switched on; switched off, one is left out. The device it is added on tells both of each two to link with the other (Receipt.LINK_MINE, 41, naming half the fingerprint of the other's key), once both have said they read it (Receipt.LINKS_MINE, 40) and the one told counts it as mine, again every ten minutes until the told device's card lists the other, for at most 60 days; never to a build from before. A persons card from a device mine at both ends is kept as it came: what it lists and this device is not linked with is under My devices, Not linked here yet, with Link, which links with no code. The two say the LINK hello naming "mine", taken as mine only where a card kept here lists the key it is signed with and the key to seal for matches; a build from before reads it as any LINK hello. Somebody in People whom such a card lists, or who counts this device as theirs, reads One of your devices? with This is my device; the switch My device turned on does the same and tells the other device. Temp's box has both. Nothing is made mine without the owner's word on one of their devices, and nobody else's card or word counts. A device linked this way gets Nothing yet (decision 255). Introducing, LinkMyDevicesTest, IntroducingTest, DesktopAlsoLinkTest, pictures 268-*. Not seen: two real devices, the phone's screens. Known: Link or This is my device pressed in Temp's box opens it again, unsaved ticks lost; a device Forgotten is offered again while another of mine lists it.

269. **The owner, 2026-10-28: "we have to make it that we dont need to accept files shared with my contact or at least it should be optional, direct when I accept one we should have a message auto accept all incoming files from this contact and this should be a parameter at the contact level."** In 0.4.032, both apps (shared code); built by a helper agent and checked here. What somebody else sends waits in two ways that ask the same question: files sent on their own (Drop, Accept or Refuse) and a note, a folder or a file shared for the first time (decision 109, Shared with me). One switch on the person's page in People and devices answers both: Accept everything without asking, off unless turned on, with "Files, notes and folders from {0} come in at once." under it. On, files they send are fetched at once, as from one of my own devices (Drop.arriving); what they share arrives already accepted, shown in Shared with me where Accept puts it (decision 122); nothing is told it waits, so their tick says I have it. Rights are as they gave them. A blocked sender stays blocked, a person removed goes off the list, my own devices take everything as before. Kept in this device's settings, never sent, no schema step (WithoutAsking). Accepting one by hand from somebody whose switch is off offers it at once in a small box: "Accept everything from {0} without asking?", one button Accept without asking, and Keep asking, remembered so it is not offered again for them. 8 phrases in the eleven languages. Things already waiting when it is turned on stay waiting. WithoutAskingTest, DesktopWithoutAskingTest (two notebooks through Post.arrived; the switch and the offer drawn). Not seen: the phone's switch and box on a screen, two real devices.

270. **The owner, 2026-10-28, with a picture of the PC's tree, its longer names cut off at the right: "the tree should be aligned on the left so that it is displayed in full".** In 0.4.032, the PC. Home, always open, kept an empty column for an arrow before it, and each level stepped in about 22 px, so a note four levels down started a third of the way across the panel. Home's line now starts at the left edge (no arrow at the top level, Home being the only line there), and each level steps in 14 px, the open/close arrow still clear of the line above; kept whenever the look is put back (Desktop.tree, updateUI).

271. **The owner, 2026-10-28: "It seems to me that the resolution of our notes icon could be increased, the edges are not sharp, is there something you can do about that?"** In 0.4.032, the PC. The icons are drawn, not pictures, but their rounded square was outlined with a 1.1 px line on whole pixels: at the owner's 125% that is 1.4 screen pixels, half between two, so every edge was smoothed over two pixels and read as soft; the fill's edge fell between pixels too. The outline and the fill of a thing's face (Home's tiles, the tree, the bars), a place's inner frame and a folder's small squares are now drawn on the screen's own pixels: their edges rounded to whole screen pixels, the line a whole number of them wide and centred on them, at any scaling (DesktopHome.line, fill). The glyph inside and pictures are as they were.

272. **The owner, 2026-10-28: "When I create a folder in temp and added a note to it, the note was created in the root temp folder, not in the folder created on it, on can see on the graphene phone".** In 0.4.033, both apps (shared code). The note was made in the folder, and temporary with it (decision 152); but Temp's card listed every temporary thing at its top, those inside a temporary folder too, so it showed there as well and looked made there. Temp's card, its count and Recent's Temp line now leave out what is inside a folder that is in Temp itself, shown inside that folder (NoteStore.temporaryShown); Temp's whole list, by which things are sent and go when their time is up, is unchanged. DesktopTempDropTest.

273. **The owner, 2026-10-28: "In personnes et appareils, please remove 'Afficher mon code' and in Mon profil, put the code as the first element on the top. Also there, remove the links we have at the top to the sections below or make these look much better, for now it looks bad the way the links are presented".** In 0.4.033, both apps. People and devices has only its round + at the foot; Show my code instead stays under the +'s camera, and My profile opens on Your code, now its first part, before You. The row of links at the top of My profile (decision 231) is gone: each part keeps its card, symbol and name, and the places that open My profile at a part still scroll to it. DesktopProfileTest, DesktopPeopleBoxTest, DesktopConnectGalleryTest.

274. **The owner, 2026-10-28: "In Farsi, with the icon and Minima name on the right, please make sure there is an space between the Minima and the icon on its right".** In 0.4.033, the phone. The gap between the app's icon and its name at the top was the name's left padding: in a language read from the right the icon stands on the right, so the gap fell on the far side and the name touched the icon. It is now on the side the name starts from, whichever way the language reads; the same for the words beside a mark's ring, a count beside a mark, the note's line of words and a value beside its name.

275. **The owner, 2026-10-28: "I dont know why the arrow at the top of the app is never green, there is always somethng to sync and why dont we have the equivalent of this arrow on desktop?"; then "the sync was comming from Parisa phone, now its good".** The phone's arrow was right: read on a copy of the Graphene's notebook (05:13), every note had reached the owner's own devices and one note shared with Parisa was still to go to her phone, which kept the whole notebook's mark amber; it went once her phone was reached. In 0.4.033, the PC: the top bar has the same mark beside the window's name, the worst of everything shared with every device it reaches (DesktopMark.WHOLE, read with the tree's marks); its words in the tooltip; waiting, a press says what waits and sends it (Waits), otherwise a press is Sync now. Seen in DesktopTreeLookTest's picture.

276. **The owner, 2026-10-28, on 0.4.033: "move the sync mark beside 'Mininotes' on the right as what we have on the phones"; and of 0.4.032's Temp drop and +, the paper, linking devices and accepting without asking: "seems good from my side".** In 0.4.034, the PC. The top bar's mark (decision 275) is on the right, just before the Agent, as the phone's stands just before its Agent. DesktopTreeLookTest.

277. **Left over from decisions 263 and 268, the owner agreeing ("ok, take care of these").** In 0.4.035, both apps. What the marks mean, in Settings, was English in every language (six lines), as were the phone's "Sent · waiting for them to accept" and "…to confirm": 8 phrases in the eleven languages, SharedWordsTest asking each mark's line in French. A device of mine forgotten here was offered again with Link while another of mine still listed it: Forget now keeps its key in this device's settings, never sent, and it is not offered until it is paired again (NoteStore.forgotten, Post.notLinked; LinkMyDevicesTest). Link or This is my device pressed in Temp's box opened it again from what was saved, unsaved ticks lost: it now opens again with them as they were on the screen. The phone's "Temp goes to N devices" after Save was English glued around the count: the template every language has.

278. **Measured on the Graphene on 0.4.034, the owner agreeing ("yes, ok, go for it") to finding the night's CPU.** In 0.4.035, both apps (shared code). A read-only watch from 16:44 to 03:18 and two readings on the 29th: 2 h 11 min of CPU in 20 h 44 min, 32 min of it the upkeep's round (mininotes-each-beat), 28 of those after 03:18, when the phone was awake and rounds ran every half minute; nothing at all while it sat idle. The 52 "gone for everybody" lines were one backlog handed over at start, not a loop. Profiled on the PC with UpkeepRoundTest's notebook: a round took 105 ms, most of it three readings of the whole notebook that nothing in a quiet round changes: every note's revision (what to send again), the collections owed on their own and the files owed on their own. All three are now kept while NoteStore.changes stands, as decision 261 keeps the stamps, and handed out as copies: a round is 44 ms. Not yet measured on a phone. UpkeepRoundTest. Seen and left for later: the files thread's round (289 ms here, 8 min on the Graphene) and the telling done each time a paired device is heard again on the network (11 min).

279. **The owner, 2026-10-29, with a picture of the PC's Perso folder wearing a filled ✓ over the middle of its top edge beside its sync mark: "i dont get wht it means and it doesnt look good if it can be replaced by the 'new' mark it would be good".** In 0.4.035, both apps. That ✓ was decision 220's: changed on another of the owner's own devices and not opened here since. Beside the ring of the sync mark it read as a second one. It now says "new", as what somebody else changed does; what it is kept for, and when it goes, are as before.

280. **The owner, 2026-10-29: "When we have a file like an .md in Temp, we need to be able to copy it directly so that we can paste it in a chat like Parlons!, everywhere to we should have the option to directly send with Parlons!"; "yes Send with Parlons! row, next to Copy wherever a file appears"; and "when sharing a md file, it shares the content and not the file", "it was Parlons and Whatsapp".** In 0.4.035, the phone. Send with Parlons! sits under Copy in a file's menu on Home (Temp and folders included) and on a note's attachment: the file is copied and Parlons! opened, saying to paste it in a chat, or, where Parlons! is not on the phone, Get Parlons!. Parlons! 0.6.149 still takes nothing shared from another app (read from its package on the Graphene), so the file cannot be handed to it; a request went to its author. Not on the PC: Parlons! has no Windows app and a PC's clipboard does not reach the phone. Send with another app said a text file (.md, .txt) was text, and WhatsApp sent its words as a message: it is now said to be a file like any other (Attachment.sentAs), so a chat app sends the file. Pasted into Parlons!, a copied text file still gives its words, which is Parlons!' own reading of the clipboard. AttachmentTest.

281. **The owner, 2026-10-29: "I got a nouveau, when clicking Afficher où il est, nothing displayed".** In 0.4.036, both apps (shared code). Show where it is, in New and on the PC's Share box, opened the folder a thing is in, the note a file is kept in on the PC, and otherwise Home. What is only in Temp, and a file shared with you on its own, which shows in Shared with me, are in neither, so Home opened, where they are not drawn; on the phone a file kept in a note went to Home too. NoteStore.shownAt says where for both apps: the note a file is kept in, the folder it is in, Shared with me, Temp, or Home, the nearest winning; the line under it names Temp and Shared with me as such, not Home. ShowWhereItIsTest.

282. **The owner, 2026-10-29: "yes add the phone's warning line near the limit".** In 0.4.036, both apps (shared code). A note holds 24,000 characters and a title 160. The phone stopped taking letters at the limit and said nothing; from nine tenths of the way its note line now says "Note text: 21,600 of 24,000 characters.", as the PC's did, in the warning's colour. The PC said it in English with the numbers glued in; both say it from NoteLimit, one template per language with the count as the language writes it (24 000 in French). Whether to raise the limit is the owner's to decide. NoteLimitTest.

283. **The owner, 2026-10-29: "the app on desktop is not responsive enough when we type, there is a delay ... to have the characters fully displayed"; "Absolutely measure it and fix it".** In 0.4.036, the PC. Measured in the real window with a made-up note (a throwaway test typing 300 letters as key events, each timed to the page drawn again, and the window's thread sampled): 7 ms a letter in a short note, 11.6 ms in one of 20,000 characters, 20 ms one time in ten, up to 48. On every repaint the page found every bold, italic and underline in the whole note, and after every letter every web address; with an underline anywhere in the note, every move of the cursor drew the whole page again; and every letter made the whole note's text to measure it against the limit. Marks and addresses never cross a line, so they are found in the lines on screen only; the cursor draws the page again only where the line it left or reached has an underline; the limit is measured from lengths until the note is near it. After: 7.7 ms a letter in the long note, 12 ms one time in ten, at most 28, as in a short one.

284. **The owner, 2026-10-29: "When we get Partagé avec vous message, the button is not translated and says See it".** In 0.4.036, both apps. See it was kept as a constant (FirstShare.SEE_IT) and WordsEverywhereTest reads the screens' own strings, so it was never looked for; 21 more such words were in no list (Block sender, Refuse and tell them, Send now, Choose devices…, how a device is reached, a file's line, the pictures switch's words and others), now in the eleven languages. SharedWordsTest asks every word kept as a constant in the classes that keep them, in every list.

285. **The owner, 2026-10-29: "From graphene, I attached two screenshot in temp simultaneously, one came pretty quick to the laptop but the other took long, can you make sure sync works quickly for all elements?"** In 0.4.036, both apps (shared code). Read from the Graphene's own log lines (17:29): the first was offered from the phone's door to the laptop at 17:29:47, went up to the relays by 17:30:03, and only then, at 17:30:23, was the second offered: files went one at a time, each offered and then sent up before the next began, so a device on the same Wi-Fi waited for uploads it did not need. A round now offers every file at the door first, with its list or sleeve to the devices on this network, and sends them up one by one after; one let go while the round is under way is offered at the door between two uploads (Post.offerNear). Compiled and tested in the suites; not yet seen between two devices.

286. **The owner, 2026-10-29: "the new tag, should be removed as soon as I open the temp folder, not that we have to click the elements individually".** In 0.4.036, both apps (shared code). Opening Temp marks every note and file it shows seen (NoteStore.seenInTemp), so none says "new" and Temp's tile no longer does. A folder in Temp still says it while something new is inside it, as a folder anywhere does.

287. **A person writing in Chinese, through the owner, 2026-10-29, with two pictures of the phone's search: 喷 found nothing, 喷嘴 found the note: "When search one character, no result. At least two characters?"** In 0.4.036, both apps (shared code). Every search box waited for two characters. One is now enough where one character is a word, Chinese, Japanese and Korean; elsewhere two, so one letter does not list every note (SearchTerm). The PC's "Nothing found for" was English glued around the word: the template every language has. SearchTermTest.

288. **The owner, 2026-10-29, on 0.4.036: "On desktop, the text is still delayed by a fraction of second where we type and it requires to push the text already in the note forward, can we make it even better? On Mobile it is just fine"; then, typing for it: "you see that it first writes over and than move the text".** In 0.4.037, the PC. Windows' "Show text suggestions when typing on the physical keyboard" is on on the owner's laptop (EnableHwkbTextPrediction): Windows then composes every word, drawing its letters in a window over the page at the cursor, and hands them to the page once the word is done, which moves the rest of the line along. The note's page and its title now take Windows' composition only while the keyboard in use composes, Chinese, Japanese or Korean, looked at again on focus and before each key (DesktopInput); a French, English or Farsi keyboard's letters go onto the page as they are typed. Found on the way: inside a long paragraph each bold word and address drew every row of its paragraph again, up to 59 ms a letter in a throwaway test; a paragraph now draws only the rows inside what is painted (FlushMarks). The collector was not it (179 pauses, 3 ms each). DesktopInputTest. Then, with pictures (a new note, no colours, "y<vv<xcv…" cut into slivers and doubled where the cursor was): Windows' text suggestions off changed nothing; a test window and the real 0.4.036 program on a made-up notebook (offline), typed into fast with real keys and photographed, drew every letter whole. In 0.4.038 every redraw of the page is of whole lines and the whole width (Desktop.Paper.repaint): the cursor's blink and a typed letter redrew a strip a few pixels wide, which at Windows' 125% can be laid half a pixel off the letters beside it. Not yet seen on the owner's screen.

289. **The owner, 2026-10-29: "There are issues in syncing the Temp folder elements, it works pretty good between the spare laptop and Pro, but elements are not reaching this laptop here and the graphene, at least not all elements. please make sure we have always a perfect syncing".** In 0.4.037, both apps (shared code). Read from the laptop's and both phones' own log lines and the Graphene's file rows (manifests only): one picture on the Pro's Temp, from 22:00, failed on the laptop and on the Graphene alike, "chunk unavailable", four tries each; both held the manifest the Pro had given at its door before the picture went up (no source), whose pieces the Pro lets go once it is up; asked, the Pro said "it went up a moment ago, so it is left as it is" and said nothing more. Now a device that cannot get a file is always told again where it is, whole: the note's list, the collection's card or the file's own sleeve (Post.sayWhereFileIs), as well as being sent up again where that is due; and one that was given only the sender's door asks at its first failure, not its fourth. The Graphene's 46 other waiting rows were all declined, as they should be. Compiled and in the suites; not yet seen between devices.

290. **The owner, 2026-10-30: "I dont get why on graphene Desktop PGC JSM4 is not listed as my device and it is on the Pro, all this should be uniformalized accross all device. Also the temp folder on the Pro is full of 5 files and graphene has none, also Temp is empty on the laptop here. We need to find a way that all device stay in sync".** In 0.4.039, both apps (shared code). Read on the Graphene (its device rows and the cards it keeps, no notes): the spare laptop paired, as somebody else's, while the Pro's card kept there listed it as the owner's; decision 268 only offered This is my device on its line. Now a device paired as somebody else's that a card from a device mine at both ends lists, active, as the owner's is taken as mine without asking, when the card comes and at every round (Post.adoptVouched, Introducing.adopted): the owner said so on that device. And a file that came from a device of mine went on to the rest of mine only from that device, which the spare laptop, on 0.4.030 and not counting the Graphene or the laptop as its owner's, never did; a device now passes it on where that device's own card here leaves the other out of its owner's devices (NoteStore.originCounts); with no card from it, as before, it is believed to send it itself. The test that the switch was the owner's to turn on each device (DesktopPersonsTest) now says the card's word is taken. IntroducingTest. The spare laptop's own side waits for a release it updates itself from.

291. **The owner, 2026-10-30, with a picture of the PC's Bin open in French, its title "Bin": "Bin should be translated here to inside it, make sure all app folders are translated"; "switch to a new version only when you install it"; and of the spare laptop: "I'm using it as a true users UI review so I will update it only when we release the updates".** In 0.4.040, the PC, and the words of both. An open place's title on the PC was its English name (DesktopFolder): now the language's, as its tile is; a folder's own name is never looked up. "Tools" was in no list: in the eleven now; SharedWordsTest asks every place's name in every list. build.ps1 no longer moves dist/latest/Mininotes.lnk: windows/use.ps1 does, at install (HANDSHAKE, Each round). The spare laptop stays on published releases, as a user would.

292. **The owner, 2026-10-30, with Temp's card open on the PC, seven pictures in it: "here I want to save 2 file on my pc, we should have a way to select multiple file at the time, also a drag and drop directly into the windows explorer should be able, both way, for a copy and for and move, so that it will delete it from the app".** The PC. Several chosen at once, on Home's grid and in any folder's or place's card (Temp, Shared with me...): Ctrl+click chooses an icon or lets go of it, Shift+click chooses the run from the icon last clicked to this one along the grid's rows (the page in view on Home), and a plain click opens as it always did, choosing nothing but marking where a run starts. Chosen icons wear the ground and ring the icon with the keyboard wears, a shade deeper; Esc, or a press on the empty room, lets go of them, and so does the menu of an icon not chosen. A note, a file or a folder can be chosen, never a place. The menu on one of those chosen acts on them all, each line with how many: for files Save on my computer… (one folder for all, as the paperclip's several, decision 196), Copy (all on the clipboard as files, to paste in a folder), Send to a device… (one Send box) and Move to bin; with a note or a folder among them only Move to bin, the others on the grid staying put (decision 119) and one Undo for all. Delete on one of them bins them all. Carried out of the window, a file (or the files chosen with it) is Windows' own drag from the moment the pointer leaves the window, so carrying icons about inside it is unchanged: let go on Explorer or the desktop it is copied there; with Shift, moved, and once Windows says so (or the copies have left), Mininotes' own is put in the bin, never erased. The copies are plain ones written as the icon is picked up, read as a save reads them, into %LOCALAPPDATA%\Mininotes-dragged beside the notebook, never in it; an hour later the next drag sweeps them. Files from Explorer stay a copy: a Shift-drop is now taken as a copy, since Explorer can take a move as done and its files away while they are still being read in, and dropped files go several ways (a note's, a folder's, the Send box, a Markdown file kept where it is) of which some keep the original. DesktopChoosing (the rules), DesktopDragOut (the drag out), DesktopHome (choosing, the menu, binAll), Desktop.copyFiles, DesktopFileCards.send for several, DesktopDrops (copy only, and not our own files back). DesktopChoosingTest, DesktopChoosingWindowTest. Not seen: the drag onto Explorer and the desktop, copy and Shift-move, and what Explorer answers a move with (compiled only; a test cannot drive Windows' drag); the chosen icons on a screen beyond the test's picture.

293. **The owner, 2026-10-30, with a picture of the PC's box "Partager « Canada » avec" (Mes appareils · 3 appareils, Administrateur; under it PixelPro with Peut écrire ▾, which dropped to Peut écrire, Peut lire and Retirer, and Graphene and Windows PC with nothing; Family · 1 personne, Parisa): "It seems Retirez also doesnt work"; "if Every device under 'Mes appareils' is you, so it already has full owner rights than why they would have the options, right and read, no it's better to keep them with the 3 levels, admin, write, read"; "this button retirer here, it should also be there for every contact including own devices"; and of Nothing yet's line: "I dont get why we say 'et Temp' here, what does it mean". The design put to the owner was approved ("ok").** Both apps (shared code); built by a helper agent, no version given here. The cause: the dropdown beside a device of mine in a note's or folder's Share box was not about that thing but its bond, what it may do with everything (decision 189); its Retirer ended the bond, while the device still had the thing through My devices, so nothing changed on the note, and the device would have lost the whole notebook. Now: in People and devices a device of mine's bond offers Admin, Can write and Can read (decision 180's two undone, in Add my device and One of my devices on the + path too), and Remove, which first asks "Remove access to everything?" with "{0} will no longer get everything of yours, only Temp and what you share with it. Its copy stays on it."; Nothing yet says "One of your devices. It gets only Temp and what you share with it. Choose here what it may do with everything else." In a note's or folder's Share box (the PC's Who has access and Share with, the phone's Share box and Share with), every person and every device, a device of mine and a member under a group included, has a dropdown for that thing alone: the levels this device may give and Remove, asked first ("They stop getting this one, and keep everything else they have. Their copy stays on their device, and they are told."); a bond is never changed there, and a device of mine is no longer Owner there (NoteStore.decideHere, standsOn). And the plan of 2026-10-11 below is built: where a folder above or everything gives somebody more than what is chosen, Remove included, their row on the thing is written "not here" (Sharing.Rule.here, a column of `shares`, schema 59, additive; a flag rather than a level, since a build from before reads any level past Admin as Admin), which for that device stands in place of every rule above, for the thing and everything inside it (Sharing.standingOn, under audience and standing, so sending, what is owed, moving and the weighing of what arrives all follow it: read-only at both ends). Choosing a level as high as what is above writes a plain rule, which lifts it; the Share box says Removed here, dropping down to the levels. Less than a group or a folder gives, for one member on one thing, is the same row at a level. Whoever is taken away is told by the road Remove always took (Receipt.REMOVED_*, again for a week), and their copy becomes their own, a device of mine's too; a folder deeper than a book is still not told, as before. The row travels in the thing's own list, to the owner's other devices and to admins, as any decision does (Parcel and Carton, behind a new mark, MNX1, after the look), held to the same rules (Sharing.mayCarry, counting what they have from above, so an admin through a folder is not taken away by another admin); only to a build that has said it reads it (Receipt.NOT_HERE, 42, said to every paired device as ABOUT_ME is, and the first time heard every thing with such a row sent again): to any other the entry is left off the list, so a build from before goes on sending that thing to that person, and what that person sends of it is refused here. A thing whose one row of its own is "not here" now carries its own list, as one with a person of its own always did. A file's sleeve cannot say it, so a file in a folder is still changed in its folder (said, not offered). *Tests:* NotHereTest (who reaches what by address and by key, a folder taken away, a rule deeper still giving, a bond, a move, when a choice is "not here", the parcel and the carton, byte for byte as before without it, a count that does not read), NotHereStoreTest (five notebooks, no network: taken away from one note in a shared folder, nothing more of it goes to Ana from the owner or from Ben, what she writes in it is refused, told, her copy is her own, a level gives it back; Ben given less writes nothing into it and still into the note beside it; a device of mine taken away from one note keeps its bond and its copy becomes its own; an admin takes a writer away, never another admin or the owner; a folder taken away takes what is inside; a build from before is never sent the entry), DesktopNotHereBoxTest (Share with on the PC: every line a dropdown, the laptop's three levels and Remove, Remove asked, Removed here, a level giving it back, the bond untouched; pictures 293-*), SchemaMigrationsTest. *Not seen:* the phone's screens, the PC's Who has access box on a screen, two real devices, a build from before given such a list, a device of mine taken away from a note that still sends its own copy back (refused and answered, as any refused writing).

294. **The owner, 2026-10-30: "The temp folder should sync faster, everything I go push the button Send now, can we make it and probably all folder and file sync instantly?"** In the next version, both apps (shared code). What was there: a new thing goes at once to whoever is reachable; what was handed and not answered waited a minute, two, four, eight, then a quarter of an hour between tries; a folder's card and a file on its own kept that wait even on Send now (sent as not whole); the phone's Send now went whole only for a note; and a phone's notes whose send had failed went again only when the app was opened or written in. Now: a try waits a minute, then two, never more (Outbox.againAfter); Send now goes whole for folders and files too, and on the phone for anything; a phone's round sends what it owes and never handed over, once a minute. A device heard again already had everything owed sent to it at once. OutboxTest. Not seen between devices.

295. **The owner, 2026-10-30, with a picture of the PC's "Waiting to go" box on 0.4.041, thirteen notes and a file waiting for the Graphene: "Why is there so many unsynced elements now on the desktop app? We also have to display better the list of elements in the windows, now it is really difficult to see the elements".** Why: Mininotes was not running on the Graphene. An update stops the app, and decision 226's receiver starts it again; but the backup before each install here force-stops it first, and Android then skips the update's broadcast ("Skipping broadcast of MY_PACKAGE_REPLACED ... stopped", the Graphene's log), so the phone stayed silent until opened, the PC trying again every two minutes. HANDSHAKE's install step now opens Mininotes once after install -r and goes back to the home screen, never photographed. In the next version, the PC: what waits is listed as New lists things, each with its face, its name and what it is under it, in a card that scrolls, a click opening it (Desktop.waitingRow). Not yet seen on the screen.

296. **The owner, 2026-10-30: "We have to make sure message displayed at the top of the app like 'Parisa n'a pas répondu...' with the button Que faire, can be removed with a X to close".** In the next version, both apps. The strip under the bar (decision 188) has a cross beside What to do: closed, the troubles it said are kept as closed in this device's settings, by who and what, and not said again; a new trouble opens it again, and one put right and back later is said again (Troubles.open, stillClosed). Mininotes goes on trying by itself as before. TroublesClosedTest, DesktopTroublesTest. Not yet seen on a screen.

297. **The owner, 2026-10-30: "when clicking personnes et appareils on graphene, it opens with a long delay, I would say 30 secs, can you make sure it opens instantly".** In the next version, the phone. Its page was read on the phone's one worker, behind every save, arrival and redraw; on the Graphene, just updated and catching up with what it had missed, that queue was long. It is read on a worker of its own now (MainActivity.reading), and an opening slower than a second and a half says in the log how long it waited and how long it read (Mininotes/People). Not yet seen on the Graphene.

298. **The owner, 2026-10-30: "When we click new at the top of the app, the elements need to be better presented, for now the structure is not good enough we can't understand what the new elements are, they should be simply and clearly listed, then a click on one should lead to it".** In the next version, both apps. New listed each thing as a card of three lines and three buttons (Open, Show where it is, Mark as seen). Now one line each, as a search lists what it found: its face, its name, and under it what it is, from whom, when and where; a tap or click opens it, a long press or right click offers Show where it is and Mark as seen; Mark all as seen stays at the foot. The PC's waiting list rows (295) take a click anywhere on them the same way. Not yet seen on a screen.

299. **The owner, 2026-10-30: "It would be good if we could organise the attachment in a note by Tab, so that when we attached a file it is specified to the open tab and when browsing the attachments they are ordered by the sequence of tab at the top and centered on the tab open, could you put this in place?"** Both apps (shared code); built by a helper agent, no version given here. A file kept with a note is under one of its tabs, by the tab's name as its `=== Name ===` line says it (decision 142), empty for a first tab with no name of its own: the tab open on the page when it was added, however it came (the paperclip, a drop, a paste, the camera, a recording, Explorer: every one goes through NoteStore.keep, which asks the note and tab open on this device's page; a note not open takes the tab it was last open on in this session). A name, not a place, so a file goes with its tab when tabs are carried (decision 239). A file kept before, or under a tab since removed or renamed elsewhere, is under the first tab: no "Other" group, which would need a word of its own for what is only a file that lost its tab. Kept in a table of its own by the file's id (`filetab`: tab and when it was decided; schema 60, additive), not columns of `files`, since a file kept again (a recording, a stamp's report) rewrites its row whole and a file still to be fetched has no row yet. The tab travels in the note's list of files, to the owner's other devices and to whoever has the note, behind a new mark (MNU1) after the stamps, the last thing in a parcel: a build from before reads everything it read and stops there; a list with no tab decided is byte for byte as before; the newest decision wins, by its time (Enclosure.Listed.tab, Parcel.tabs, NoteStore.enclosed and listed); backups carry it too. Browsing: on the PC, the cards under the page and the paperclip's list, on the phone the strip and Choose several…, are in the order of the tabs at the top, each tab's files after its name, quiet, where the note has more than one tab, and open with the open tab's files (or the nearest after them) first in view; switching tabs slides them there. A file's menu has Move to tab (PC, a submenu) and Move to tab… (phone) with the note's other tabs, where the device writes in the note and it has more than one tab. A tab renamed (or a first tab given a name: renamed, carried, or made Page 1 when a plain note gets a tab; or a named first tab carried away from the front) takes its files with it: TabFiles.carried says which names become which, NoteStore.retab changes them at once, and the list goes again. A file moved to another note leaves its tab behind. TabFiles (the rules), NoteStore (tabOpen, tabsOf, tabFile, retab), Parcel, Enclosure, SchemaMigrations, DesktopFileCards, Desktop, MainActivity. *Tests:* TabFilesTest (which tab, the groups and their order, where a list opens, renames and carrying, the parcel: the tab travels, byte for byte without one, a build from before reads the same bytes up to the mark, a count that does not match), DesktopTabFilesTest (a notebook, no network: added under the open tab, said on the list, another device's taken only when later, Move to tab and a rename, moved to another note), SchemaMigrationsTest. *Not seen:* any screen on either app (compiled only: the headings, the scrolling to the open tab, the menus), two real devices, a build from before given such a list.

300. **The owner, 2026-10-30, of Shared with me: "it is good that we have Files someone shares with you on their own and Anything someone shares with you for the first time, then we just have to make sure that thing in Temp and other already shared folder dont fall in there".** In the next version, both apps (shared code). A file on its own was shown in Shared with me as its bytes arrived, whoever's it was and whatever it was in, and Temp goes between the owner's devices as files on their own: every Temp picture from the owner's phone showed in Temp and in Shared with me. Now one is shown there only where it is somebody else's (whose it is, not which device brought it: somebody else's passed on by a device of mine is still shared with me) and not in Temp; a note or folder in Temp from somebody else waits in Temp, not there as well; what is inside a folder already here was never shown there. What builds before put there wrongly is taken out at each start (NoteStore.sharedWithMeTidied): only where it is shown changes, not where it lives. SharedWithMeTidiedTest; DesktopFileShareTest, DesktopSharedWithMeTest, TempTravelTest still pass. Not yet seen on a device.

301. **The Graphene's battery on 0.4.042 (the owner, 2026-10-30: "go with the Graphene's battery use").** In the next version, both apps (shared code). A read-only watch from 16:11 to 17:41: Mininotes used 17 minutes of a core in 90 (the battery 81% to 69%, with whatever else the phone did), spread over its sending threads; its log said "sent a file on its own" 54 times in two hours, a file owed to a device that was off, tried every two minutes since decision 294. A device not heard from lately is now tried a quarter of an hour apart again (Outbox.againAfter(tries,heard), Outbox.due with the quiet devices, Post.cartons and sleeves); one heard lately is still tried every minute or two, and one that comes back is sent everything the moment it is heard (Post.heardFrom), so nothing waits longer. OutboxTest. The watch goes on.

302. **The owner, 2026-10-31: "We should try to keep the text as minimal and make sure the app is self-explanatory"**, then the list below, approved as it stands. In the next version, both apps. *Gone:* the line under What is new (what arrived since you last looked); under a group's Shared with this group, that your devices keep the same notes already; the line about a group being the same on all your devices (on the PC, New group… now stands alone); "One of your devices. It writes in your colour…" on a device's round, its page and the colour box (nothing is said there now: the round wears your colour); under What the marks mean, the rounds after the mark; under Writing colours and Your colour, who sees your round and writing; the PC's line under Recent (the tree), under the dock and search (and the phone's dock and search line), under Text size of notes, under Temp: also show on Home, under Shared with me goes to, and the phone's under how long Temp keeps things; under the stamp key's devices, how the key travels; on Parlons!, the line under a contact's address field, the one under Your Parlons! address (on the PC its help line is blank until a save says how it went), and the one in the box setting it. *Shorter:* "No other device yet.", "Nobody in this group yet.", "Nobody else yet.", "No group of your own yet.", "Nothing opened lately.", "Nothing sent yet.", "Off: a note goes when you tap its mark." (PC: click), "Updated at the next restart." (in both of the PC's update boxes; Settings keeps the version, which ended the old sentence, as its own line, Mininotes v{0}), "On its way to Minima, usually within a few minutes." (a stamp waiting), "Optional." (the Minima address in Feedback). Nine new sentences in the eleven lists; the old ones are left in the lists, unused. MainActivity, PeopleBox, HomeScreen, OverviewScreen, PhoneStamp; Desktop, DesktopPeople, DesktopSettings, DesktopProfile, DesktopOverview, DesktopDrops, DesktopStamp. *Tests:* no test asserted any of these sentences. On the PC SharedWordsTest, WordsEverywhereTest, SymbolsEverywhereTest, DesktopPeopleBoxTest, DesktopProfileTest, OwnParlonsTest, DesktopStampTest, StampCardTest, AddMyDeviceTest, DesktopSyncStatusTest, DesktopMenuSymbolsTest, DesktopShrinkTest, RecentTempTest, DesktopTempNoteTest, DesktopTempDropTest, DesktopHomeRulesTest (89 tests, all pass); Android compileReleaseJavaWithJavac and SharedWordsTest (10, pass). *Not seen:* any screen on either app.

303. **The Graphene's battery, 2026-10-31 (the owner, 2026-10-30: "go with the Graphene's battery use")**: overnight on 0.4.043 its upkeep thread spent 2 h 42 min of CPU and the battery went from 63% to 18% in six hours, unplugged, with almost nothing in the log. The read-only watch put the spin between 04:30 and 11:00: it started a few rounds after the Graphene took something to carry for the PC, which was asleep (Windows: 02:54 to 11:01), and stopped the minute the PC woke and the thing was let go. A carried thing is brought 1, 2, 4, 8 minutes apart, then every 10; the log shows those five tries and none after, because a try that threw (no road to the sleeping PC) was not counted, so the thing was due again at the very next round: sealed and mined for every road twice a minute, about 13 s of CPU each time. Now a try counts whether it went or threw (Post.bringOne), and one that throws says so in the log, once a turn. The PC sleeping alone costs nothing (00:08 to 02:48 that night, nothing carried, no spin). Post. *Tests:* on the PC CarriedOnceTest (7, with aTryThatThrowsWaitsItsTurnLikeAnyOther) and DesktopCourierTest (3), all pass. *Not seen:* on the Graphene.

304. **The owner, 2026-10-31: "I added a screenshot to temp on Graphene, after a minute it is still not on the other device, it should go right way", then "all sync should be as fast as possible", and "yes, lets go with your Proposed fix decision 304".** Not versioned yet, both apps (shared code). The logs: the screenshot was shared into Mininotes at 16:12:42, just after 0.4.045 was installed and the app left stopped (decision 295), so the app started cold; saved at 16:12:49; the node up at 16:13:14; offered to the devices at 16:14:15; the PC had it at 16:14:17, the Pro at 16:14:48. The save asked for a round of file work at once (Post.filesChanged), which found the node not running and returned; nothing asked again when the node came up, so the file waited for the upkeep round, where file work came last, behind a minute of other things sent again. Now what is to be done when the node is up is done then (Node.whenUp, set beside everyBeat on both apps: a round of file work), and each upkeep round starts its file work first, on its own thread, as well as at its end (Post.again). Left: why the Pro took the file from the relays half a minute after the PC took it from the Graphene's door (the offer at the door went to 2 devices on the network, not the Pro). *Tests:* on the PC NodeWhenUpTest (2, new), CarriedOnceTest, DesktopCollectionFilesTest, DesktopFileShareTest, all pass; Android lintRelease, assembleDebug. *Not seen:* a file shared into a stopped app on a phone.

305. **The owner, 2026-10-31: "do we have a full log of all actions? This should be made available to the user but of course not including anything related to the private space, is this doable?", then "please go with the The history page".** Not versioned yet, both apps (shared code). *History…* in Sync and sharing, after People and devices (both apps; the PC's Share right-click too), with its own symbol (scroll-text; "history" had been Versions' clock, unused). It lists what Mininotes did, newest first, under Today, Yesterday or the date in the language in use, each line one sentence and its time: "{0} reached {1}." (a note, when the other device's own answer is kept, never a relay's; a file added here, when they say they have its bytes), "{0} came from {1}." (a note somebody wrote taken in, not one waiting to be accepted; a file fetched and kept), "{0} can be reached again." (a device heard after 15 minutes without a word, never at a start). A note's or a file's line opens it. *What is kept* (`History`, `NoteStore.happened`, `history`; schema 60 to 61, a new table): ids and addresses only, never a title or a word; the name of the thing and of the device are read when the page is drawn, so a renamed thing shows its name now. The same thing with the same device within ten minutes of its line is that line, its time moved on; thirty days, two thousand lines at most. Writing a line never throws into a send. *Private:* the private space is never in the notebook and never travels, so it cannot be a line; a line whose thing is not in the notebook says nothing and is let go; a note in the bin is hidden and keeps its lines; help requests, sent or heard, are never lines (decisions 201 and 204). Seven new sentences in the eleven lists. *Tests:* HistoryTest (5), DesktopHistoryTest (5: a note from my tablet and my note answered by it, through Post.arrived; the same again soon one line; thirty days; the bin and for good; never in the notebook or nobody known), SchemaMigrationsTest, WordsEverywhereTest, SharedWordsTest, SymbolsEverywhereTest, DesktopMenuSymbolsTest, DesktopMenuOrderTest, DesktopFirstShareTest, DesktopOwnDevicesFilesTest, all pass; Android lintRelease 0 errors, assembleDebug. *Seen:* the PC's window in the gallery (48c-history, synthetic notebook). *Not seen:* the phone's box; lines from real devices.

306. **The owner, 2026-10-31: "Beside each elements in nouveau we should have a X or something to remove them individualy".** Not versioned yet, both apps. Each line of New ends in a cross: marked seen (a file as opened, a note as seen) and taken off the list at once; the last one gone, the list says "Nothing is new.". The long press (phone) or right click (PC) Mark as seen does the same now, where it only greyed the line. MainActivity.whatIsNew, Desktop.newRow. *Not seen:* either on a screen.

307. **The owner, 2026-10-31: "In temps, we should have the possibility to put there a permanent note thats stays but it content is delete, put it by default and make it in its menu that it can be turned off".** Not versioned yet, both apps (shared code). A note's time in Temp can be *never* (`NoteStore.STAYS`): it stays in Temp, said "Stays in Temp", and once Temp's time (Things stay) has passed since it was last written its words go, a version kept first, the title and the note staying; every device does it at the same time, as Temp's ends always were, so nothing is sent. The time travels as any Temp time does. *The notepad:* made once on each device, in Temp and staying, titled Notepad in the language in use, its id made from the owner's person id so all their devices make the same note, not one each (`notepadOnce`, `notepadId`); never made again by itself. *Switches:* "Stays in Temp" in a note's menu beside Temporary…, for a note in Temp (on: stays; off: an ordinary Temp note, gone in Temp's time); and "Temp: a notepad that stays" in Settings on both apps (on: made, or back from the bin, and staying; off: an ordinary Temp note). A build before this lists such a note in Temp and never takes it. *Tests:* TempNotepadTest (2). *Not seen:* on a screen.

308. **The owner, 2026-10-31: "When we long press a link, we should get a menu with the basic function like copy and not opeining the link".** Not versioned yet, the phone (the PC's right click on a link already has Open link and Copy link). A link held in a note: Copy link, Open link, Share link, and nothing opened, nor the words selected; lifting the finger after it opens nothing. MainActivity (page's long click, linkAt). *Not seen:* on a phone.

309. **The owner, 2026-10-31, on the laptop: "When we create a new app, the set font size should be the one set at the app level, for now my app is set at the 4th level from the left but a new note opens at 6. make sure also a new installed app have it set at 4th", then "I want new notes to be at the 4th bar by default when opening".** Not versioned yet, the PC. The laptop's notes were set at the 6th rung (Text size of notes, "rung" 5), which every note without its own size follows; nothing else was wrong. A new PC now starts its notes at the 4th rung (DesktopLook.FIRST 3); and one that never had the older four sizes no longer read that missing setting as the 5th rung (DesktopLook.rung), which is why FIRST was never used. The phone keeps the 5th (the owner: "On PRo a new note is set at 5th and it is ok"). The laptop itself: set to the 4th rung at the next install, the app closed. DesktopStoreTest. *Found building 0.4.047:* at the 4th rung a click one pixel inside the letter after a closing mark found the letter four before it (FlushMarks.viewToModel summed letters one by one; drawing measures a run whole); it now measures from the row's start as drawing does (DesktopTypingStaysTest). And the notepad (307) is made with no time of its own, so it is never the note last written, which a start opens (DesktopAdaptiveTest, TempNotepadTest).

310. **The owner, 2026-10-31: "When we move an item in the bin, we should leave the space left empty, not replace the items ordering on the home page".** Not versioned yet, the phone (the PC already held Home's icons whichever way a thing was put away, decisions 119 and 168). Only a drop on the bin, the archive or Temp held the others on the phone; a menu's Move to bin or Archive, the strip and a file's menu let the icons never placed by hand close up. Now every way through putAway, and into Temp, holds them first (HomeScreen.holdOthers). *Not seen:* on a phone.

311. **The owner, 2026-11-01: "Yes, we should try to make the app as fast as possible", then "ok, lets try your fix".** The Graphene after the 0.4.047 install: 26 min of CPU in 11 min, frames skipped, taps late. GrapheneOS runs with no JIT (dalvik.vm.usejit false) and compiles every app ahead of time, but never a debuggable one: Mininotes was "run-from-apk", and `cmd package compile -m speed` gave only "verify" (undone); the profile showed only ART's interpreter. The Pro has JIT. The App info page has no JIT switch (the owner's screenshot: only WebView JIT). So a *fast* build type: the release build signed with the debug key, not debuggable (android/app/build.gradle `fast`; `assembleFast`), installed over the debug build with its notebook and back again (same signer, checked with apksigner). Run-as cannot reach a build that is not debuggable, so its backup is `ShellBackup` (src/fast only, never in a published build): a receiver only the shell can send to (android.permission.DUMP), which, the app stopped first, copies databases, files and shared_prefs into the app's own folder on shared storage and writes "done"; the PC pulls it into the usual Mininotes-phone-backups folder, checks every SHA-256 against the phone's copy and takes the phone's copy away. *Seen* on the MininotesDemo emulator: the debug build's notebook, the fast build over it, run-as refused, ShellBackup's 4 files (67 MB) matching run-as's SHA-256 on the phone and pulled, the phone's copy gone, the fast build opened, the debug build back over it with the notebook unchanged (the private file rewritten at its start, as it always is). lintFast 0 errors. The Pro stays on the debug build.

312. **The owner, 2026-11-01, of the box an amber mark opens ("En attente d'envoi"): "we should see clearly and simply what are the devices waiting for it, a bit structured like the Nouveau window".** 0.4.048, both apps. It was one paragraph, a sentence a device (Waits.lines), to be read to the end to know who lacked what. Now a Devices card above What waits: a row a device, as New lists things, its round initial, its name, under it what it lacks in two to four words joined by a dot (Your changes, Changes to 3 notes, 2 files to confirm, 1 answer waiting, Needs to update Mininotes, Not heard from for more than three days, Locked · takes it in when opened, Waiting for them to accept, Sent · waiting for them to confirm), and its own mark at the end. Same devices and order as the sentences (Waits.rows, SyncStatus.waiting); the sentences stay where nothing is known to wait and for telling a box with nothing to press (Looks.onlyUpdates). Eleven new phrases in the eleven languages. *Not seen:* on a screen.

313. **The owner, 2026-11-01, of decision 308: "It is good but the links are shown in the lower right corner of the app, could we make it close to where we type".** 0.4.048, the phone. The menu of a held link was a Sheet shown from the whole page, so it stood down the right-hand side under the page. Now Sheet.show(anchor,x,y): just under the line held, centred on the finger within the screen; where there is no room under it (the keyboard up, a link low on the page) above the line, so it never hides the link. Every other menu stays down the right-hand side. *Not seen:* on a phone.

314. **The owner, 2026-11-02: "on mobile the menu Réglage should also be better strcutured, the section are not as well delimited as on the laptop".** 0.4.049, the phone. Settings' parts were a quiet grey heading each, one running into the next. Now each part (Security, Sync, What the marks mean, View, Show on Home, Pictures and files, Writing colours, While the pad is closed, How notes travel, Updates) starts under a line across the whole box, its name in bold (MainActivity.section). Cards came first, in 0.4.049; the owner, 2026-11-03: "make sure the card use the full width of the Settings windows so that there are only horizontal dividers between the sections", so the line runs through the box's margins to both edges and nothing frames the sides. The language stays above them. *Not seen:* on a screen.

315. **The owner, 2026-11-02: "the text in Relais is not transalated".** 0.4.049, both apps. The words test reads only what is handed to a call that puts words on a screen, so it never saw sentences a method returns or a list kept as a constant: the two lines under the ways notes travel (Relays.travelLine), every answer to an address that is not one (Relays.problem), and, seen on the owner's screenshot of a thing's Temporaire box on the PC, Temp's times and the box's sentence. All 30 phrases added in the eleven languages. The phone glued English around an address ("… is already one of your relays", "Checking …") and showed a problem and the line under the switch without translating them; now templates and Say.t. RelaysEverywhereTest asks each of them in French.

316. **The owner, 2026-11-02: "we should have a option to relfect that on all my devices" (the relays).** 0.4.049, both apps (shared code). Settings → Relays ends with *The same on all my devices*, on by default: the owner's relays and the public switch, with when they were last chosen (Node.relaysAt, set by Add, Remove and the switch), go to every device of the owner's, paired and marked My device, at each round until it took them (Post.sendRelays), and one of those takes a newer choice as it is (Node.relaysFromMine: kept, then applied on a thread of its own, not dialled again: it was checked where it was added); off at either end, nothing goes or is taken. They ride on the key card (KeyCard, name `relays`, Relays.toCard: `public on|off`, then one relay a line), which every build since 0.4.006 reads and, for a name it does not know, takes nothing from; a card of their own would have been kept as a note by such a build. Taken only from a device marked the owner's and paired, like the Integritas key. *Tests:* RelaysEverywhereTest. *Not seen:* between two devices.

317. **The owner, 2026-11-02, with a screenshot of a thing's Temporaire box: "we should have the option to keep the element permanently ... an option 'until I delete it'".** 0.4.049, both apps (shared code). A thing's Temporary… box lists Temp's times and then *Until I delete it*: the thing stays in Temp, words and all, until somebody deletes it by hand (NoteStore.KEEPS, Long.MAX_VALUE-1: never reached, so expire never takes it, and not STAYS, so a note's words are not emptied as the notepad's are). Said "Stays until you delete it". It travels as any Temp time does, so it is the same on every device; a build before this one shows a time thousands of years away and never takes it either. *Tests:* RelaysEverywhereTest. *Not seen:* on a screen.

318. **The owner, 2026-11-03, of Parlons! now taking files shared from other apps: "ok" (to handing them to it).** Not versioned yet, the phone. Parlons! 0.6.152 (the Pro, 1 Nov) lists SEND and SEND_MULTIPLE for any type, which the request of decision 280 asked its author for. Send with Parlons! now hands the file to Parlons! itself (ACTION_SEND to its package, the file lent as for any app, a text file said to be a file), and Parlons! asks which chat; a Parlons! from before, which refuses it, gets the copy-and-open of decision 280 as before. *Not seen:* on a phone.

319. **The owner, 2026-11-03, with a screenshot of the PC's line "Texte de la note : 21,727 sur 24,000 caractères." on a note of eight tabs: "I dont have the impression there is so much text, first can you make sure the count doesnt take care of the deleted characters and coul we set the limit per tabs?", then "yes, build that".** 0.4.050, both apps (shared code). Nothing deleted was counted: the count was the note's text as kept, all its tabs together, with their tab lines, the marks of bold, slant and underline (Marks), and the line ends. Now each tab holds 24,000 (NoteLimit.TAB) and the line counts the tab open ("Cet onglet : … sur 24 000"), or the note where it has one tab; the whole note holds 100,000 (NoteLimit.NOTE, inside Parcel.TEXT_MOST and Envelope.MAX_TEXT, 200,000, accents and all), said only from nine tenths ("Tous les onglets : …"). The phone's page takes a tab's worth, or what the note still may; the PC refuses to keep a note with a tab or the whole over its limit, naming the tab. A backup's notes are taken up to 100,000 (NoteStore import); a build before this one refuses a note over 24,000 from a backup, though it shows and syncs it. And the PC counted in English in every language (21,727 for 21 727): its bundled Java had no locale data; jdk.localedata is now packed, cut to the eleven languages (windows/build.ps1). *Tests:* NoteLimitTest (5).

320. **The owner, 2026-11-03: "send only what changed" for notes.** 0.4.051, both apps (shared code). A note went whole at every revision: a line typed into a note of 90,000 characters sent all of it again, to every device, through relays and carriers. Now, to a build that has said it reads changes (Receipt.CHANGES, 43, said and remembered as "not here" is), a note goes as the stretches of lines replaced since the revision that device last confirmed it took (NoteStore.agreedAt), made against this device's exact copy of that revision (NoteStore.keptAt: a version at exactly that revision, never the nearest before). The stretches are Merge's own (Merge.changes), the lines both ends share left out first. They ride in the parcel after the path behind "MND1" (Delta), the body saying only that the change is in the tail (a NUL no note holds), with the SHA-256 of the text it was made against and of the text it makes. The far end finds its copy by that SHA-256 (NoteStore.keptAs: the versions at that revision first, then the page, then every version), rebuilds the note, checks the result, and goes on exactly as with a whole note: weighed, merged, kept, answered, in History. A copy not found, a result that does not match, or a change that does not read takes nothing, answers nothing, and asks for the note whole (Receipt.ASK naming it, which every build answers with the whole note). The whole note goes the first time, on Sync, under 2 KB, where the change would not be at most half, where it would not fit with its path, and always to a build that has not said it reads changes (the spare laptop and Parisa's phone on 0.4.030). The log says on both ends whole or change and how many bytes. No schema step. *Tests:* DeltaTest (8, both suites), DeltaStoreTest (4, PC: two notebooks, a 90,000-character note's one-line change 431 to 489 bytes against 90,330 whole). *Not seen:* between real devices.

321. **The owner, 2026-11-03: "Envoyer avec Parlons! for a note sent as .md file".** 0.4.052, the phone. A note's menu has Send with Parlons! under Move to…, as a file's has it beside Copy (decision 318). The note goes as the Markdown file it is kept as anywhere (NoteFile; decision 162): its id in the header, its words with tabs and marks as other editors read them, a private code never in it; named after its title. Lending hands it over at an address of its own (Lending.ofNote) and writes it as Parlons! reads it, never as a copy on the phone; the open note is written down first, so what goes is what is on the screen. Where Parlons! is not on the phone it is copied as before. *Not seen:* in Parlons! on a phone.

322. **The owner, 2026-11-03: "could we have a list of transfers and indicate how they went through? ... could it also keep the IP of the public relay? Only if that doesn't slow down the syncing".** 0.4.052, both apps (shared code). History's line for a note or a file that reached somebody says how the last thing sent to that device went: "Plan reached Pro, direct.", "left at your PC", "through your relay 70.34.244.170", "through a public relay 203.0.113.9", or both where both took it. A relay is the owner's where it is among the relays in Settings, public otherwise, shown by its address without the port. Direct.send, where no door or home took it, now walks the device's addresses itself instead of the transport's sendToContact: the same copy to every address, one after another with the transport's own five seconds each, then the device's directory asked once and every address tried again, exactly as the transport did; the doors that just failed are not dialled a second time, so it is never slower. An address with no host of its own (the transport places it) still goes through sendToContact, said as "through a relay". The road is kept by device in Routes while the app runs and written on the History line when the device's own answer comes (NoteStore.happened; schema 62, history.road, empty for lines kept before, which say what they said). What came from somebody says no road. A file's pieces are fetched from wherever they were put up; its line says how its announcement went. Turning off "Use the public relays" on one device, with "The same on all my devices", leaves only doors, the PC and the owner's relay between the owner's devices; a friend's device is reached through the relays it says it is on. *Tests:* HistoryTest (2), RoutesTest (1), SchemaMigrationsTest (1), RelaysEverywhereTest (every form in French). *Not seen:* between real devices.

323. **The owner, 2026-11-04: "I just launch the app on the Pro but it is stuck, a new note it seems is open but I cant go back ... make sure the users never face this situation"; "the Pro is updating note that have consequences to revert the latest changes made here on the laptop"; "we have to make sure this never happen, there is a risk of losing information for the user".** 0.4.053, both apps (shared code). The Pro, closed since the night before (notebook locked, so everything arriving waited in its inbox), was opened at 12:34 while the owner wrote in a shared note on the PC only. What happened: (1) the inbox was taken in as one piece of work on the storage thread, so the note being opened stayed a blank stand-in and Back, which waits for the page's save, waited ten minutes behind it; (2) every copy of the note the PC had sent meanwhile (revisions 4581 to 5078, each made against 4577, the last text the PC knew the Pro had) was weighed against that old base and merged, bringing back rows changed since, mostly the first of each tab where the owner writes; each merge counted one higher and went to the PC, which took it as newer (it had not written since agreeing) while the owner was typing there; (3) a change the Pro could not rebuild asked for the whole note each time, 129 times in two minutes; (4) a small note swapped between two texts at revision 20, 246 times, each device having held the other's text and taking it as newer; (5) the Pro sent the note's 37 KB twice every 30 s to a contact holding its own keys and heard it straight back; (6) the PC kept what arrived as a version but not its own text that it replaced, and the Pro's hundreds of arrivals pushed the notes' own history out of the hundred versions kept. Now: what the same sender wrote below the revision both last had is old news, never merged, never kept as a version, and a change below it is answered and not asked for whole (Arriving.stale, NoteStore.landed, Post); where each device held the other's text the higher count wins, at the same count the same text on both by its trace; a device's own text is kept as a version before anything replaces it, and its newest 30 own versions stay whatever arrives (NoteStore.prune); nothing goes to a contact holding this device's own keys, and anything signed by this device is dropped on arrival (Post.isSelf); the storage worker has a second line for work that gives way (Background.submitLater), and the inbox is taken in one message at a time on it, on the phone and the PC, so the page's save and Back run between two messages. Not done: why the Pro has a contact with its own keys; a log file on the phone for a report (asked, see NOW.md).

324. **The owner, 2026-11-04: "yes" (find why the Pro is its own contact); "In a multi tabs note, when we move to one tab and come back, we need to come back where we were, not at the top"; "We need a search in note function. that will be in the menu and from a right click but also from ctrl-f on desktop"; a .jar in Temp "doesnt have the java logo"; "ok" to a log on the phone for reports.** 0.4.054, both apps. (1) A device never its own contact: a note arriving carries the list of who has it, which names this device too, and the phone took in what waited while it was locked before it had been told its own key, so its own entry looked like a stranger and was linked, a contact of itself with a share to itself (the Pro sent a note to itself every 30 s). Now every arrival tells the notebook its key first (NoteStore.knowMyself), Linking.toLink links nobody while the key is unknown, pairedWith refuses this device's own key ("That is this device's own code.") and linkThrough skips it, the membership check compares keys in one form (isMine), and any contact holding the device's own key is forgotten with what was shared with it at every start (forgetSelf; phone in Listening.hear, PC in startNode). (2) A tab is back where it was left: the caret and how far down, per note and per tab by its name, while the window or screen lives; a tab never left opens at its top. (3) Find in this note: on the PC Ctrl+F over a note opens the find bar (Ctrl+Shift+F, and Ctrl+F on Home, is still the search of everything), also in the note's ⋮ and on right-click; on the phone in the note's ⋮, a box to type in over the page. On both, past the last match of a tab the next tab holding the words opens, and before the first the one before; marks only, nothing written. (4) A file's tile on the PC: where Windows has no icon for the kind itself (.jar with no DefaultIcon), the icon of the program it opens in, as Explorer shows; with no program, the kind in letters alone, never a blank page. (5) The phone's own log in a file, as the PC's: what this app wrote to the system's log, copied every two minutes into two files of 1 MB that roll (LogKeep, PhoneLog), only its own lines and errors, never sent: Feedback has Share the log (the phone) and Show the log (the PC). A too-big file dropped on Temp says "Nothing was added" as the + does (DesktopDropSaysTest); see decision 325.


325. **The owner, 2026-11-04: the 169 MB file was let go "inside Temp once it was open" and "nothing happen"; "these messages should also be displayed when using the drag and drop"; "ok big files only travel when the two devices can reach each other directly".** 0.4.054, the PC. Every part of an open Temp takes files (DesktopDropTargetsTest), and each route said why in the bar; but the bar is easily missed with the eyes on where the file went, so files not taken are now also said in a box, which and why, whether dropped or added with the + (Desktop.notTaken). Temp takes a drop from the moment it opens: before its things were read, about a second, a drop there was kept on Home. A drop that cannot be read says so ("Those files could not be read. Nothing was added.") instead of nothing, and every drop is written in the log, counts and where only, never names. Big files sent straight to a device, with a notification and a per-person setting to save them on the device, are next: agreed to travel only directly between two devices that reach each other, never through relays.

326. **The owner, 2026-11-05: "On the pro there is a 1 on top of the temp folder but nothing in it, please make sure the 1 is not displayed in such case, it even stay after the folder has been open"; "once the recent are displayed after a swipe up, again in a list individualy we could swipe them up to remove them from the list".** 0.4.055, both apps. (1) Temp's count leaves out the notepad, the note that stays in Temp (decision 307): it is always there, so it made a 1 over a Temp with nothing else in it, open or not; Recent's line for Temp says the same count. It is still listed in Temp. (2) The overview's cards (a swipe up from Home) are thrown up to be closed, as a phone's open apps: the card follows the finger up and, let go past a quarter of its height or flicked, flies off; sideways still scrolls the row. Closed, by the throw or the card's Close, it is out of Recent for good (NoteStore.outOfRecent), not only off the screen, where before it came back the next time; back once opened again. Not changed: the people over a note on the phone are three rounds at most, then +N listing everybody, then +, so nothing is covered (asked to check only).

327. **The owner, 2026-11-04: "Could we have a way to send file directly to the other device without putting it in the app, so the person will get an notification and we should have a setting allowing file from contact to be saved on device directly"; "ok big files only travel when the two devices can reach each other directly"; 2026-11-05: saved in "Downloads directly", "Up to 2 GB".** 0.4.055, both apps (Big, PhoneBig, DesktopBig). A file over 16 MB and up to 2 GB sent to a device is no longer refused: it is offered straight from where it is, never copied into the app, and only to a device this one reaches directly now, at a door of its own (Direct.firstTries: on this network, or a public door it proved), never a relay; otherwise it says so ("… cannot be reached directly just now. Files over 16 MB go only when both devices are on the same network."). The offer goes the usual way as a message of its own kind (Message.BIG), sealed end to end, with a token and a key made for it; a build from before reads that kind as nothing, so it never becomes a note, and is told it needs updating where it has not said it reads messages. The receiver is asked: on the phone a notification with Refuse and Save, which work with the app closed; on the PC a box. Save fetches it at the sender's door with the token (PrivateStreams), 256 KB pieces each sealed with AES-GCM under the key, numbered in the nonce, so a piece changed, missing, out of order or cut short is found and nothing is kept; written straight into Downloads (the phone through MediaStore, the PC as name.part then renamed, a number added where the name is taken), the notification or bar saying how far, then that it is there; the sender is told it was saved, or refused. From somebody whose files are taken without asking (decision 269's switch), saved at once. The sender keeps only the token, in memory, for a day: a restart there ends the offer ("That file is no longer offered."). On the PC, the box saying a file over 25 MB was not added (decision 325) offers "Send to a device…". Not yet: a big file let go on the phone's Temp is still only refused there; sending to several devices at once; resuming a transfer cut halfway (it starts again).
328. **The owner, 2026-11-05, of "1 brouillons de récupération": "what do you mean by drafts? For me everything is saved directly, there is no difference between draft and other notes"; then "ok with your proposal".** 0.4.056, the PC (NoteSplit shared). The PC kept writing a note could not take (the note deleted elsewhere while it was open, made read only, or over a limit) aside as a "recovery draft", outside notes and sync, with its own box, backup and warning. Now there are none: too long, what fits goes into the note, cut at a line end, and the rest into "Plan (continued)" notes beside it; the note gone, read only, or what arrived with it too long, the writing becomes "Plan (copy)" on Home, the rest after it. A title over 160 letters is cut, the whole of it the first line of the words. The bar and a notice say where it went. Drafts an older PC kept become notes on Home at the next start (DesktopRecovery.adopt). Recovery writing… left the menu, the export's warning and DesktopWritingBackup went with it. *Seen:* NoteSplitTest, DesktopRecoveryTest, the adaptive and page-follows tests, the package probe. *Not seen:* on the owner's PC, where one draft waits.
329. **The owner, 2026-11-05: "we should have a full backup page where the user can set its backup parameters, what is backup, where, when and at what frequency, like daily, weekly, monthly, on demand, when he closes the app. The backup should be encrypted ... available from the general setting"; then "having both choice another of my devices ... and having a folder we specify ... pick the when and frequency for each individually"; the lock, a password and the 12 words first, the key file and "this device only" after.** 0.4.056, both apps; step 1 of 3 (folders). A Backup page, from Settings (its own section), My profile's Backup and the menu's Backups…: each folder a card with its times, what goes in, what opens it, how many are kept, its last backup or why it failed, and the next; Back up now, Change, Remove (the backups there stay); Add a folder…; Add from backup. Per folder: Everything or Everything but the files; Only when I ask, each day, week (a day) or month (a date, 1 to 28) at an hour, or each time Mininotes closes (the phone: is left, at most once in 10 minutes); keep the newest 1 to 50, older ones of this device's own name removed once a new one is whole, nothing else in the folder touched; opened with the notebook's lock (while it is locked), a password for the backups, or 12 recovery words made for them (shown, three asked back). Always sealed, in the format Export writes (LOCKED head, the lock, the sealed zip), so Add from backup opens it anywhere. The key of a password or words lock is kept in the notebook's secrets, never in a backup; the place itself in the device's settings. Nothing changed since the last one made by itself: none made (a fingerprint of the text and the files). A failure is tried again after half an hour. The PC looks each minute and at closing; the phone by a job Android runs every half hour or so (PhoneBackupJob, JobScheduler), and one soon after it is left; a locked phone notebook not open says "unlock Mininotes first". Names: mininotes-backup-<device>-<yyyyMMdd-HHmmss>.mnbackup. Rules in Backups (shared, BackupsTest); DesktopBackups, PhoneBackups. *Seen:* BackupsTest, DesktopBackupsTest (sealed, opened with its password and its words on another notebook, unchanged skipped, the newest kept, another device's and other files left, no folder, no key). *Not seen:* the page on either device; the phone's folder picker, job and writing into a real folder. *Next:* step 2, another of my devices as a destination, and a small Linux receiver for a server (asked 2026-11-05); step 3, the key file and this device only (335).
330. **The owner, 2026-11-05: "could we have a cli script of mininote for server just to receive the backup, mainly Linux servers?"; "from the app we should made available the linux script to be installed, it should be on github, with all the documentation how to install it and link it" (step 2 of 329).** A backup place can be **another of my devices** instead of a folder (Backups.Place.device): the sealed backup is written beside the notebook, then **pushed** to that device (Big.Offer "2", Big.push / Big.take, Post.pushBig): the sender brings it to the other device's door, which a phone on mobile data can do where it can never be fetched from; the receiver says where to go on from, so a broken try resumes; not reached, it waits and is tried again each half hour. Taken only from one of mine, only a backup's own name (Backups.isBackupName), each device's in its own folder, its oldest beyond what it asked for removed (Keeping). The **PC** keeps those my other devices push to it (Documents\Mininotes backups, Change… on the Backup page). The **backup server** (server/, Server.java): Mininotes with no screen for a Linux server, built from the same shared sources (shared-sources.gradle), its keys and settings in files only its user reads; `pair` shows a code (Add my device, nothing yet, taken within 15 minutes), `status` lists devices and backups; its public address is said as its door (Node.publicHost). It says **Receipt.KEEPS_BACKUPS** (44) to each of mine: from then on it is sent nothing but backups (Post.sendsTo, the outbox, Temp's devices) and what was given to it is taken off. install.sh and its guide live in github.com/mininotesorg/backup-server, the jar a release there checked by its SHA-256; the Backup page's **Your own server…** shows the line that installs it, the one that pairs it, and the guide. Checked: Debian 12 in a container, install.sh, pair, a 40 MB push from another container, identical on arrival. Step 3 (a key file, "this device only") is next.
331. **The owner, 2026-11-07, of the phone's "Add a place for backups" (a screenshot): "the window here is not clear, the elements shold be better dispplayed and the folder I picked should be shown here. Also we should have a button to turn on and off the backup"; and an OK to "Your own relay…" in Settings, Relays.** 0.4.058, both apps. Each place has a **Back up here** switch, first on its card (Backups.Place.on, kept as set; places from before it are on): off, none is made at its time or on closing and none waiting is pushed again (Backups.due, dueOnClose, pushAgain, timed; the phone's job is cancelled when nothing on has a time); Back up now still makes one, and the card shows its times muted, without "Next:". The phone's box: the folder is a row of its own, **Folder** with its name under it and a › that opens the phone's picker, drawn again once one is picked; on the phone's own storage the name is its path from the top ("Documents/Mininotes backup"; PhoneBackups.shown); **Your own server…** a row with ›, under Where, not a centred link. Every dropdown row on the phone (dropRow) keeps what it is set to to half the screen at most, so a long choice wraps beside its name instead of pushing the name out (the screenshot: "Ouvert avec" gone, its ▾ alone on a second line, a tall gap). **Your own relay…**, under the field that adds a relay on both apps: what a relay of your own is, the line that installs it (mininotesorg/server's install.sh, which installs the relay and the backup server), where its address goes, and the guide. Six new sentences, 11 languages. *Seen:* BackupsTest, DesktopBackupsTest. *Not seen:* the box and the switch on a phone.
332. **The owner, 2026-11-07: "we should be able to select multiple elements like images in Temp to be able to copy them all at once" (then "ok with Choisir plusieurs", "yes write both"); "the full screen mode doesn't work, the dialog box doesnt show, there is no way to go back"; "when we type or remove around one found we then jump to the next one, we have to make sure the search bar doesnt impact what we type"; of a picture pasted in Temp, "it shouldn't say added to accueil".** 0.4.058, both apps. **Choose several** (fr "Choisir plusieurs"). The PC, on any grid, Temp among them, beside Ctrl+click and Shift+click (292): Choose several… in a file's right-click menu, after which a plain click on that grid chooses or lets go until Esc or a press on the empty room; Ctrl+A chooses everything there, Ctrl+C copies the chosen files; a box dragged over the empty room chooses what it covers (with Ctrl, added); Select all in the chosen ones' menu (DesktopChoosing.all, covered). The phone, on Home, in Temp and in a folder: Choose several… after Copy in a file's menu opens Choose files, that place's files with Select all, the one pressed ticked; Copy the primary (one clipboard entry holding every file), More… for Send with another app…, Save on my phone… (one folder from the phone's picker, each under its name), Send to a device…, Move to bin (Several, shared). **The Agent's full screen**. The phone: the conversation's height is worked out from the parts round it, not from the box, which the screen had cut short so it was asked to grow each time and the field went off the bottom; over the whole screen the box loses its margins, where a tap closed it; Back leaves the whole screen first. The PC: the Agent goes before it opens a note, a search or New in the main window, where it hid them; Esc leaves the whole screen first; only a press on the main window is outside it, so People or a code to scan opened from it no longer close it; the whole screen is the one the Agent is on. **Find in this note** (PC): the note written in while the bar is up is marked again, and the caret and the view stay where the writing is; only the arrows, Enter and F3 move them (DesktopFindBar.again). **Where it went**: a paste, a drop or a + in Temp, the archive, the bin or Favourites says that place, not Home (Desktop.keep, placeCalled). **Building**: only on the owner's go (HANDSHAKE, Each round): code is compiled and its focused tests run, then built once for the batch. *Seen:* DesktopChoosingTest, DesktopFindInNoteTest (typing with the bar up), SeveralTest, the Agent tests, WordsEverywhereTest, SharedWordsTest. *Not seen:* any of it on a screen.
333. **The owner, 2026-11-07: "We display « Mininotes-Windows-0.4.055.msi » n’a pas été envoyé but it seems it is because the app is not running on the receiving laptop ... Instead of the message rien n'a ete envoye, we should say this is a large file that can only be sent directly and then present the way to do it, all accross the app we should present the solution instead of presenting the problem"; then "yes" to doing the big file first.** 0.4.058, both apps. A big file not reached directly is no longer "was not sent": a box titled **Send “X” straight to Y** gives the steps (open Mininotes on Y, both on the same Wi-Fi, wake Y if it is asleep) and says it is tried again by itself; **Try again** the one primary, Stop trying, Close. It is offered again every two minutes for a day while Mininotes stays open here (Big.AGAIN_EVERY, AGAIN_FOR, tryAgain), the bar saying "◷ “X” waits for Y", and said when it goes; after a day, what to do (Big.gaveUp). Post.offerBig says the not-reached case as Big.NOT_NEAR itself so each app shows the steps (Big.notReached). The phone counts a file whose app does not say its size, so a big one goes the big way instead of "Nothing was sent". A file refused is said "name: why", no spaced dash (Given.refusal). 16 MB and 25 MB are two limits, not a mismatch: up to 16 MB travels with a note through relays (Enclosure.MOST), up to 25 MB is kept in a note or Temp (Attachment.LIMIT); over 16 MB a file sent to a device goes directly. The rest of the audit is decision 334. *Seen:* BigTest, GivenTest, DropTest. *Not seen:* the box on a screen, a retry that goes.
334. **The owner, 2026-11-07: "go with the rest of the solution-first list" (the audit after 333: "all accross the app we should present the solution instead of presenting the problem").** Not built yet, both apps. Each message that said only what went wrong now says what to do first and why after. **Backups:** a failed try says the fix ("Plug in the drive it is on, or choose another folder: …", "Open Mininotes on X, on the same Wi-Fi: the backup waiting for it goes by itself", "Free some room there…", "Unlock Mininotes, then press Back up now…"), and a folder gone or full shows **Choose another folder…** first (Backups.lastSaid, wantsFolder). **The note's line:** what waits and goes by itself (not reached, not in reach, not connected, an update to come) says "◷ Waits for X. See how", amber, not "Could not go", red (Unsent.waits, NoteLine.couldNot); the box gives what to do before why; a send that threw goes through the same box, never the exception's words alone. **Queued, not "save again later":** a Parlons! address, the name we pick, or a key that did not reach one of my devices is kept and sent again by itself every two minutes, a month at most (Later, Post.sendLater, the post settings' "later"), and said "It goes to X by itself once Mininotes is open there". **Big files:** one that did not come whole says how to get it, with **Save again** (the offer still stands); Android 9 says what does; the phone's file over 25 MB opens **Send it straight to a device** with Send to a device…, as the PC does. **Network:** the phone's profile says "Allow Mininotes on the network…" with **Open the app's settings**, or "Join the Wi-Fi your other devices are on…" with **Open Wi-Fi settings**; the PC says "Check that this PC is online, then press Reconnect". **Pairing:** "Open Mininotes on X, on the same Wi-Fi as this phone, then scan the code again…"; a wrong code says which to scan. **Integritas:** a stamp it did not answer for is sent again by itself each minute, five times, while its box is open (Integritas.retries). Smaller: a zip that is not a backup, a pad with no room, a file too big to share, each with what to do. *Seen:* NoteLineTest, UnsentTest, SharedWordsTest, LaterTest, CalledTest, BackupsTest, BigTest; PC WordsEverywhereTest, ParlonsContactTest, DesktopBackupsTest. *Not seen:* any of it on a screen.
335. **Step 3 of 329 (the owner, 2026-11-05: "the key file and “this device only” after"; 2026-11-07: "then backups step 3").** Not built yet, both apps. Two more ways to open a place's backups, after the others so kept places read the same (Backups.Opens KEY_FILE, DEVICE). **A key file:** a 32-byte random secret, written to a file the owner saves where they choose (mininotes-backup-key-<device>.mnkey, "keep it apart from the backups, on a USB key for instance"); the lock is made with it as with a password. **This device only:** the same kind of secret, kept in this device's settings (backupKeysHere), never in the notebook nor a backup; said first: "Only this PC (phone) can open these backups. If it is lost, or Mininotes is removed from it, so are they." Putting a backup back tries this device's secrets first and asks nothing when one opens it; the box asks for the password or the words, or **Use a key file…** (Backups.secretIn, openWith). *Seen:* BackupsTest (a key file read back, CRLF too, nothing else taken for one; only its secret opens the lock; the order of the choices). *Not seen:* the boxes, a key file saved and used on another device.
336. **The owner, 2026-11-09: "In temp and any other folder make sure the latest added element is positioned last and we should have the option in the right click menu to order the lements by date added, name, size and so on at the folder level".** 0.4.060, both apps, schema 63. A new date, `arrived`, on notes, folders and files (additive, with triggers): a row new here, a move to another folder and a move into Temp each say now; a save keeps it. What was already there has only its last writing, or the date a file or folder was made. In a folder and in Temp, what has no cell stands after the last icon, never in a gap (Layout.folder), the latest last; Home keeps its first free cell. Order by (right-click on a folder's or Temp's empty room; held down on the phone): Date added, Name, Size, Date changed, Type (FolderOrder), kept per folder on the device, dates oldest first, sizes biggest first. A chosen order ignores the cells without writing over them; an icon put somewhere by hand ends the chosen order.
337. **The owner, 2026-11-09: "on desktop when the cursor is on the left of a line it becomes an arrow and let's us select the whole line with one click".** 0.4.060, the PC. In the page's left margin, beside a line of writing, the pointer is an arrow and a click chooses that line as it is drawn (without its return since 339).
338. **The owner, 2026-11-09, with a screenshot of Translate saying every language "not available yet".** 0.4.060, both apps. Those words were said for three different states. Now: while the list of packs is unknown a language says nothing; when GitHub cannot be read the box says so with Try again (and the PC logs why); "not available yet" is only for a language that is not on the list.
339. **The owner, 2026-11-10, of the margin arrow, with a screenshot: "the point should point toward the right, like in the block note here".** 0.4.061, the PC. The arrow in the page's margin is the usual one mirrored, its tip at the top right, as Notepad's. And the owner: "it only selects the line not adding the return to the following line in it": the click chooses the words of the line, without the return.
340. **The owner, 2026-11-15: "We have to make sure we detect all hyperlink form every extension like [a .taxi address] should be made an hyperlink".** 0.4.061, both apps (Links). A bare name is an address when it ends in any of the web's endings, read from IANA's list (web-endings.txt, 1,285 without the xn-- names), not some thirty-five. Endings that are mostly files (zip, mov, py, md, sh, rs, java, ps, so, pm, mp) stay words unless written with www. or a scheme.
341. **The owner, 2026-11-15: "browse the tab in a note by scroling from right to left, making sure the + is always reachable".** 0.4.061, both apps. The tabs slide sideways (a swipe on the phone, the wheel or a touchpad on the PC); the + is no longer the row's last card but beside the row, right after the last tab while they fit and held at the edge once they do not. The open tab is brought into view. On the PC the row used to be cut off at the window's edge.
342. **The owner, 2026-11-15, with a screenshot of a shared folder: "the image should have the green check too".** 0.4.061, both apps (NoteStore). A file in a folder wears the mark its notes wear, from the people the folder reaches, amber until each has said it has it; decision 92's "only once shared on its own" stays for files on Home.
343. **The owner, 2026-11-15: "In the Menu, we should use a shade of green to highligh the selected element not a white", with a screenshot of the PC's Recent.** 0.4.061, the PC. The one in front is a deeper shade of the list's own colour, with its bar, not a white card.
344. **The owner, 2026-11-15, with a screenshot of a file's card: "the X to put in the bin doesnt work".** 0.4.061, the PC. A note opens locked and is unlocked a moment later; its cards were drawn in between and their crosses stayed switched off. They now follow the page.
345. **The owner, 2026-11-15: "On The Agent page, we should already have a conversation box available on the first window, only on it's corner we should have the simbol to put it full screen and again bring it smaller, we dont need the buttons we currently have".** 0.4.061, both apps. The conversation is a framed box from the start, with arrows out (and in, to come back) in its top right corner; New conversation and Full screen are gone (the owner chose to drop New conversation; a conversation still ends three hours after its last message).
346. **The owner, 2026-11-15: "The write displayed in an empty note should also be translated" and "review the full app to make sure everything is translated".** 0.4.061, both apps. WordsEverywhereTest read only a call's first words and no hints, so a heading given second ("Put a backup back") and an empty note's "Write." were never checked; it now reads them, and what it found is in all ten languages.
347. **The owner, 2026-11-15, with screenshots of 0.4.061's Agent: "the ⤢ symbol should be for the whole box, it is the agent whole window that needs to be extendable full screen and before being extended, the input box is not visible"; the mock-up agreed.** 0.4.062, both apps. The symbol is beside the Agent's name at the top right of its window (its title line on the PC), arrows out for the whole screen and in to come back; nothing lies over the conversation. On the phone the conversation is only as tall as leaves the field and Ask on the screen, worked out from what can be seen and the box's other parts, not from where the box sits.
Parked, at the owner's word (2026-11-15): **click anywhere and write**. Not spaces ("we have to keep the note light") nor tab stops: a blank sheet, as OneNote's page of boxes placed anywhere, to be drafted with the owner as its own round before any code.
Planned, not built (the owner agreed, 2026-10-11): **taking a person away below a folder**, built since in decision 293 (designed with the owner on 2026-10-30). Until then a folder's people reached everything in it and an element could only add people or raise rights. To let a note or a subfolder leave out somebody the folder includes, or lower their rights, the rules needed a "not here" entry that wins over a folder's rule above it; the sender had to stop sending that element to them and tell their device to put it away; and both apps' Sharing screens had to show where it was overridden.

### Desktop adaptation, 9 October 2026

Windows 0.3.028 applies decisions 126 through 134 to the existing Swing canvas.
Home counts complete row boundaries from the full viewport, keeps page/cell
identities and gaps, and preserves fractional offsets through resizing, including
negative page origins. Resizing never writes new cell placements. The entire foot
is hidden away from the centre and in All pages, with its hit area and layout band
released. Extra scroll room keeps the last row clear of centre controls.

Empty-space dragging locks a dominant axis after six pixels. Precise wheel deltas
accumulate without synthetic inertia; notched-wheel and return-to-centre motion
use one elapsed-time EDT timer. Press stops motion without consuming the press.
Shift-wheel, keyboard page movement, Ctrl+Home and empty-space double-click remain.
Visible neighboring tiles can take mouse focus without snapping the view.

Dragging captures the actual tile once at display scale, including its name and
preview, anchored at pickup. The source keeps its layout while hidden. Edge speed
uses HomeMotion with elapsed time and recomputes stationary-pointer destinations.
One extra blank page per edge excursion is bounded. Accepted empty-cell moves
place the tile before revealing it, retain transactional persistence and Undo,
and restore persisted placement on failure. Escape cancels before navigation;
deactivation and disposal clean up the drag and controller.

Notes retain their reading character/within-line offset and selection across
rewrap, or keep the caret visible during active writing. Opening a different note
still starts at the beginning. Short windows move auxiliary controls to Note tools,
retaining Home/title, save/error state and recording controls.

Invalid edits retain the portable title/body limits but first save full desktop
recovery writing atomically in the notebook, outside sharing and ordinary note
queries. Navigation waits for that commit; storage failure blocks it. Recovery
writing can be reopened, intentionally shortened, exported in full, saved as a
new valid note or explicitly discarded. Independent snapshots have separate
identities. A password-protected desktop writing backup covers these drafts;
portable backup warns that it covers saved notes only. See windows/README.md for
the recovery and safe-switch procedure and, for evidence, the log the maintainer keeps privately.
No native touch/haptic or Android keyboard parity is claimed from Swing tests.

## Not decided, taken as written unless the owner says otherwise


All three were settled on 2026-09-30 as decisions 25, 27 and 28 above; the owner may say otherwise.

## Going back

Install `dist/archive/0.1.041-final/` on the device, then put the device's backed-up app files back
file by file (phones: `run-as` copy into `databases/`, `files/`, `shared_prefs/`; PC: the copied
`%LOCALAPPDATA%\Mininotes` folder, app closed). On a phone: `adb install -r -d` the archived APK (`-d`
because it is older; a debug build allows it), `am force-stop org.mininotes.android`, empty and refill the
three folders with `adb exec-in run-as org.mininotes.android sh -c 'cat > <path>' < <file>` for each file,
list them again to check every size, then open the app. Tested on the emulator on 2026-09-30 (see *Step 0*). A 0.2 notebook is refused by 0.1.041 ("newer than this
app"), which is why the files, not the notebook alone, are what go back. The 0.2 line also imports a
0.1 backup file through *Add from backup* and *Replace*, so the older export stays usable.
