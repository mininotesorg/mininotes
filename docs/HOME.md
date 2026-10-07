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
