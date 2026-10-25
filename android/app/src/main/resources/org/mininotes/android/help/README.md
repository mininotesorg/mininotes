# Mininotes

Mininotes is a notebook for your phone and your PC that keeps your notes, folders and files the same on all your
devices and shares them with the people you choose. There is no server, no account and nobody in between: everything
travels sealed end to end over the Minima network, or straight from device to device on the same Wi-Fi.

[![Latest release](https://img.shields.io/github/v/release/mininotesorg/mininotes?label=latest%20build)](https://github.com/mininotesorg/mininotes/releases/latest)

<!-- not in the app -->

[中文](README.zh.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) ·
[Português](README.pt.md) · [Italiano](README.it.md) · [日本語](README.ja.md) · [Русский](README.ru.md) ·
[हिन्दी](README.hi.md) · [فارسی](README.fa.md)

## Contents

1. [What's new](#1-whats-new)
2. [Install it](#2-install-it)
3. [Write](#3-write)
4. [Organise: Home, folders, dock and favourites](#4-organise-home-folders-dock-and-favourites)
5. [Your own devices](#5-your-own-devices)
6. [Share with other people](#6-share-with-other-people)
7. [People and devices, and My profile](#7-people-and-devices-and-my-profile)
8. [Know where everything stands](#8-know-where-everything-stands)
9. [Files, photos and recordings](#9-files-photos-and-recordings)
10. [Files that stay on your PC](#10-files-that-stay-on-your-pc)
11. [Versions, archive, bin and backups](#11-versions-archive-bin-and-backups)
12. [The Agent: ask, and have it done](#12-the-agent-ask-and-have-it-done)
13. [Stamp on Minima with Integritas](#13-stamp-on-minima-with-integritas)
14. [Privacy, encryption and keeping it safe](#14-privacy-encryption-and-keeping-it-safe)
15. [Private spaces and the help request](#15-private-spaces-and-the-help-request)
16. [Languages and translation](#16-languages-and-translation)
17. [Every menu entry, in one line](#17-every-menu-entry-in-one-line)
18. [Updates](#18-updates)
19. [Say something about it](#19-say-something-about-it)
20. [Build it yourself](#20-build-it-yourself)
21. [What is in this repository](#21-what-is-in-this-repository)
22. [Licence](#22-licence)

---

## 1. What's new

Newest first. Each version is the same on the phone and the PC unless it says otherwise.

### 0.4.028

- A stamped note or file you share takes its stamp with it: the person you share it with sees it in *Stamp on
  Minima…*, as the stamp of what they received only if it is exactly the same, with the links to check it on the
  Minima Explorer. A stamp made after sharing reaches them too.
- When you share a file whose Integritas report is kept beside it, *Send its Integritas report too* sends the
  report as well.
- The language window at the first start is in English, with the languages in alphabetical order.

### 0.4.024 to 0.4.027

- **Stamp on Minima…** is one line in a note's or a file's menu, saying how far its stamp is, and opens one window
  with everything: stamping, looking for it on Minima, verifying again, the versions stamped before, and comparing a
  file with a fingerprint. The Integritas PDF report can be attached to the note, or kept beside the file.
- Stamps go to your other devices: something stamped on the phone is listed on the PC, and the other way round.
- A stamped file wears a small stamp on its icon.
- *Compare with a fingerprint…*: paste the SHA-256 you were given for a file and see at once whether it is the same
  file. Nothing leaves the device and no account is needed.
- What Integritas answers is said in your language, and when it does not answer at once it is asked again.
- On the PC, the words of every screen, button and tip are in your language; a new notebook's first folders are named
  in it too. In French, a note that is new wears *nouv.*
- The phone no longer stops responding when its screen is drawn again while it is still reaching the relays.

### 0.4.021 to 0.4.023

- A note's tabs are put in another order by dragging them on the PC, or holding and carrying them on the phone; their
  menu also has *Move left* and *Move right*.
- The language is asked at each start until one is confirmed; on the phone a tap on a language is enough.
- Each app presents the other: *Mininotes on your phone…* on the PC, *Mininotes on your PC…* on the phone, with a
  code to scan.
- The Windows installer shows the Mininotes logo and can start Mininotes when it finishes.
- A note started in Temp stays in Temp even if the screen is drawn again before its first word.
- Devices no longer tell each other again and again that they have a file, which kept them busy.

### 0.4.019 to 0.4.020

- A new install asks its language first, and its README and first note are written in it. README is in eleven
  languages.
- On the PC, Ctrl+V on Home, in a folder or in Temp keeps the picture or the files on the clipboard there.
- On the PC, an installer (MSI): no administrator rights, Mininotes in Settings, Apps, and *Uninstall Mininotes…*
  in the menu, which offers a backup and lets you keep your notes or not. After 30 days without a backup, Mininotes
  reminds you.
- On the PC, lines with bold, italic or links are no longer drawn over one another.
- On the phone, what arrives while Mininotes is closed is gathered in one notification.
- *Stamped on Minima…* lists everything stamped, with a + to stamp something more.

### 0.4.016 to 0.4.018

- A note's stamp covers its title as well as its text.
- Temp is listed in Recent when you use it.
- Text pasted from other apps keeps its line breaks.
- A picture put in Temp reaches your other devices sooner.

### 0.4.011 to 0.4.015

- On the phone, Settings, *Keyboard learns from my notes*: on, the keyboard's voice typing works in notes.
- The Integritas key goes to the devices you choose. A note's *This note* menu has *Stamp on Minima…*, a stamped
  file is marked, and a stamp's details link to the Minima Explorer.
- Less work at each start: devices no longer repeat everything to each other.
- The phone listens again by itself after it restarts or after an update.

### 0.4.010

- *Read me* is now **README**: on Home and on GitHub, one and the same text, this one. A README nobody changed is
  written again by itself when a new version brings a new one; one you wrote in stays as you made it.

### 0.4.008 to 0.4.009

- A **Photos** album folder on Home by default: your pictures in a grid to browse, each opened large with previous and
  next.
- A search result opens at the first match, with a find bar to step through every match in the note.
- The **Agent's** conversation uses the whole window, with a full-screen button, and offers to take you to the right
  place, for example the Integritas part of *My profile*. It keeps the conversation for a few hours.
- *Read me* is shown or hidden from *Show on Home*, with its own icon.
- *Read me* describes the emergency contact (the help request) and the limits of private spaces.

### 0.4.007

- A note moved out of a shared folder on one of your devices keeps syncing with your other devices.
- Less back and forth between devices: the same answer is not sent again and again, so a phone works less hard.
- On the PC, resting the pointer on a round beside a note's title says who it is, what they may do with the note, where
  they stand with its writing, and how many of its files are still on their way to them.

### 0.4.006

- A PDF's card shows the top of its first page; a Word, PowerPoint, Excel, OpenDocument, RTF, HTML or text file's card
  shows its first lines.
- On the phone, *Choose several…* on a held file ticks several files to send with another app at once or put in the bin.
- *My profile* takes the Integritas API key in place, with *Save*, and can hand it to the own devices you switch on.
  *Stamped on Minima…* moved to the menu.
- On the PC, *My profile*'s text size is the same A− and A+ ladder as a note's, and a right click in Recent offers
  *Remove from Recent*. The app's name sits on the left of the bar, beside the logo.
- The words sent with your link are in the language you chose.
- Long descriptions under the pointer are shown whole, on several lines.

### 0.4.005

- A note's file put in the bin is taken out of the note on every device and for everybody the note goes to. *Put back*
  brings it back everywhere.
- What waits to go counts only the files the note shows, and lists each of them.
- *My profile* has an Integritas part, with your account and the list of what you stamped.
- On the PC, every menu entry has a short description shown under the pointer, switched on or off in *My profile*.
- On the phone, *Copy* on a held file puts the file itself on the clipboard; *Send with another app…* wears the usual
  share mark.
- A group's settings are behind its ›; *My profile* has *Manage…* for People and devices; every entry that opens a box
  or a page ends with "…"; *Keep as a file* is now *Save on my device…*.
- The help request leaves no trace in any menu: it is set only by a code typed in a private note.
- Writing in a busy shared note on the PC no longer lags.

### 0.4.004

- *People and devices* and *My profile* (once *Profile*) are at the end of *Sync and sharing*.
- On the PC, *My profile* sets the text size of the whole app, in five steps, from the next start.
- *Save on my computer…* (on the phone *Save on my phone…*) replaces *Download* and *Save a copy…*. Every box that picks
  a file or a folder on the PC is Windows' own.
- The help request moved into private spaces only.

### 0.4.003

- **Stamp on Minima** and **Verify on Minima** through your own Integritas account.

### 0.4.002

- On the PC, *Copy* in a file's menu puts a picture on the clipboard as a picture, and any file as a file.
- On the phone, a row that drops down to a choice shows its value on the right, on one line.

### 0.4.001

- On the PC, the paperclip's menu ticks a note's files to act on them together, and has *Take a photo* and *Record
  audio*.
- *Sync a folder of this PC…* is back in *Sync and sharing*: a folder of the PC, its files kept in it, shown in
  Mininotes with its own mark.
- Every message an error can show is translated.

## 2. Install it

**Android** (9 or newer): download `Mininotes-<version>.apk` from
[the latest release](https://github.com/mininotesorg/mininotes/releases/latest), check it is the file that was
published, and open it on the phone:

```sh
sha256sum -c Mininotes-<version>.apk.sha256
```

Android asks whether to allow installing from wherever you downloaded it; that permission can be switched off again
afterwards.

**Check who signed it.** Every release APK is signed with the same Mininotes key. Its SHA-256 certificate fingerprint,
as [AppVerifier](https://github.com/soupslurpr/AppVerifier) takes it (copy the two lines, then *Verify from clipboard*):

```
org.mininotes.android
F7:91:C2:14:D9:ED:9B:E9:DE:EC:6F:15:BE:D0:25:38:35:22:DD:F9:A8:21:23:4A:73:90:C8:1B:60:1F:91:AB
```

Or with the Android SDK: `apksigner verify --print-certs Mininotes-<version>.apk`. Any other fingerprint is not a build
from this repository's releases.

**Windows** (10 or 11): download `Mininotes-Windows-<version>.msi` from the same release and open it. It needs no
administrator rights, and Mininotes is then in Windows' Settings, Apps, like any app. Uninstalling it there keeps your
notes, which a later install finds again; *Uninstall Mininotes…* in the app's menu can delete them too, after offering
a backup. Java comes inside it; nothing else has to be installed.

Or, for those who prefer it, `Mininotes-Windows-<version>.zip`: extract it anywhere and open `Mininotes.exe`.

Nothing else is needed on either: the Maxima node that carries your notes is inside the app.

<!-- in the app again -->

## 3. Write

- **A ruled page, ready.** A note opens with the cursor in it and is saved as you write. There is no save button.
- **Tabs.** A note can hold several pages, like index cards: *Add a tab*; drag to reorder.
- **Bold, italic and underline.** Type the marks around the words, `**bold**`, `*italic*` and `__underline__`, or press
  Ctrl+B, Ctrl+I or Ctrl+U.
- **Two people in one note.** When two people write in the same note before either has seen the other, the two are
  put together line by line and nothing either of them wrote is lost. What each person wrote can be shown in their
  own colour: tap a round under the title to change one.
- **From other apps.** On Android, Mininotes is in the share sheet: a screenshot, photos, files, text or a link go into
  a new note or one you used lately.

## 4. Organise: Home, folders, dock and favourites

- **Home** holds your notes and folders as icons, on as many pages as you like, each where you put it.
- **Make something.** Tap + to make a note or a folder, or to add files from this device or another one.
- **Its menu.** Hold an icon for its menu: colour, icon, move, share and the rest. On the PC, right-click; there the logo
  takes you Home, and you can drag the divider to resize or fold the side list.
- **Folders** hold notes and other folders, as deep as you like. Drag anything onto a folder to put it inside.
- **Move it.** Hold and drag an icon: into a folder, out of one, or to another page. While you drag, the strip at the top
  lets you drop it straight onto a favourite, Temp, the archive, the bin or share, with no menu.
- **Pages.** Swipe between pages. Pinch Home, or choose *All pages* in the menu, to see every page at once.
- **Colours and icons.** Each note and folder can have its own colour and its own icon or picture, from its menu. The
  colour's strength is its own, set by the slider under the colours. Home's colour colours the whole app.
- **The dock** is the row at the foot of Home: whatever you put there, one tap away. *Add to the dock* is in every note's
  and folder's menu, apart from favourites. It holds as many as you like and scrolls sideways when they do not fit. Each
  device has its own dock, which is never sent to your other devices.
- **Favourites** keep what you come back to in one place. A new favourite also goes into the dock; taking a favourite
  away leaves it in the dock.
- **Recent** lists what you opened lately: down the side on the PC, on Home on the phone. *Remove from Recent* (a right
  click on the PC) takes something off the list; nothing is deleted. On the phone, swipe up from the dock, or tap the
  handle just above it, to flip through Recent as cards: tap a card to open it, swipe it away or press Back to close.
- **Temp** is for what you only need for a while, and to pass things between your own devices and the people you choose:
  what is in it goes to them and is gone everywhere when its time is up.
- **Shared with me** holds what others shared with you, waiting for your answer (see
  [Share with other people](#6-share-with-other-people)).
- **Search** at the foot of Home finds notes, folders and files by their words. **Tree** shows everything at once.
- **Archive and Bin**: out of the way, or on the way out. Things can be put back from both (see
  [Versions, archive, bin and backups](#11-versions-archive-bin-and-backups)).
- **README** on Home is this text, the same as on GitHub, without what only matters there (the contents, what is new,
  installing, building and the list of every menu entry, which the PC says under the pointer).
- **Show on Home**, in *Settings*, chooses which places Home shows (Recent, Temp, Shared with me, the archive, the bin)
  and whether the README is there. One that is off is still in the menu.
- **Photos**: an album folder on Home by default, every picture in it in a grid, each
  opened large with the next and the previous a swipe or an arrow away. Add photos with its +, or take one. Any folder
  can be shown as an album from its menu.

## 5. Your own devices

Link your phone and your PC once (*People and devices*, then *Add my device*, and scan the code of one on the other).
From then on, what you share with your devices keeps itself up to date on all of them, both ways, a few seconds after
you stop writing. A device of yours can be set to *Can read* if it should only look. Each device keeps its own layout:
where you put a note on one does not move it on the other, and each has its own dock.

## 6. Share with other people

- **Add someone** by scanning their code or opening the link they send you; they do the same with yours.
- **Share a note, a folder or a file** from its menu, *Sync and sharing*, then *Share…*. First choose what they may do,
  then pick a person or a group, or show them your code. Each person gets a role: *Owner* (whoever made it), *Admin*,
  *Can write* or *Can read*, changed at any time from the drop-down beside them, or removed.
- **From another device…** on any + takes what someone shows you.
- **Groups**, like Friends or Colleagues, gather people: share with a group and everyone in it gets the same role,
  including people added later.
- **Accept or refuse.** When someone shares with you, a note tells you and it waits in *Shared with me*. You see what it
  is and who sent it, and choose where it goes, before it is anywhere else. *Accept* to keep it, or *Refuse*, quietly or
  with a word back to them.
- **Colours travel.** A colour you give a shared note travels with it, and each device can set its own strength.
- **Everything in a shared folder is shared.** Whatever you put in a folder you share goes to the people you shared it
  with.
- **The rounds** beside a note's title are the people and devices who have it, each with their picture or initial. A tap
  on one shows their picture big. On the PC, resting the pointer on a round says who it is, what they
  may do with the note, where they stand with its writing, and how many of its files are still going to them.

## 7. People and devices, and My profile

Both are at the end of *Sync and sharing* in the menu.

- **People and devices** lists the people you added, your own devices and your groups, each on its own page. A
  person's page shows what you share with them and lets you give them a picture of your choosing, seen only by you. A
  group's settings are behind its ›.
- **The + by People** connects with someone: scan their code, paste their link, or *Share my link* to send yours in a
  message or a chat. **The + by My devices** adds another device of your own.
- **My profile** holds what is yours:
  - your **name** and your **picture**, which go to the people you are paired with;
  - your **Parlons! address**, sent by itself to your contacts;
  - your code and your link, for others to add you;
  - the **Integritas** API key (see [Stamp on Minima with Integritas](#13-stamp-on-minima-with-integritas));
  - on the PC, **Text size**: an A− and A+ ladder of five steps, from Smaller to Largest, for the whole app (Home, Recent,
    menus, boxes), taken from the next start, with *Restart now*;
  - on the PC, **Describe what each thing does**, on by default, which turns on or off the short line shown when the
    pointer rests on a menu entry or a round;
  - *Manage…*, which opens People and devices.

## 8. Know where everything stands

- **The mark** on every note, folder and icon says where it stands: an empty grey ring is only on this device; an amber
  arrow is waiting to go (press it to send now, and see each note and file still waiting); three amber dots are sent and
  waiting for an answer; a green tick means everybody has this version; two grey bars mean paused; a red exclamation mark
  means a device has not been reached for more than three days.
- **New** lists what changed since you last looked: notes others wrote in, things shared with you, files that came, with
  who sent each, when and where it is.
- **The strip at the top** appears when something is wrong with a contact (a device not reached for days, locked, or with
  an older Mininotes). Mininotes keeps trying by itself, and *What to do* says what you can do.

## 9. Files, photos and recordings

- **Attach** any file to a note or a folder, several at once.
- **Take a photo** from a note: the phone's camera, or on the PC Windows' own Camera, whose photos come straight into
  the note.
- **Record audio** into a note.
- **Light to carry.** Photos are made smaller when you add them, so a note of fifty photos still travels light; turn that
  off in *Settings*, *Pictures and files*. Other files are packed to save room.
- **Previews.** A PDF's card shows its first page; a Word, PowerPoint, Excel, OpenDocument, RTF, HTML or text file's
  shows its first lines. They are drawn by Mininotes from the file itself, never by Windows or another app, so no picture
  of them is kept outside.
- **Open** a file in the program your computer uses for it (*Open in LibreOffice*, for example), or choose another with
  *Open with…*. Changes you save come back into Mininotes and go to your devices.
- **Copy** on a file (a right click on the PC, a hold on the phone) puts the file itself on the clipboard, a picture as a
  picture, to paste in another app without saving it first.
- **Save it outside.** *Save on my computer…* (on the phone *Save on my phone…*) saves a copy where you choose. *Save on my
  device…* keeps it as a file in a folder of this device too.
- **Send with another app…** on the phone opens the phone's own share, to send a file through WhatsApp, for example.
  *Choose several…* on a held file ticks several to send that way together or to put in the bin.
- **Several at once on the PC.** The paperclip's menu has *Attach a file*, *Take a photo* and *Record audio*, then the
  note's files, each with a box to tick: *Select all*, then *Save on my computer…* (several go into one folder),
  *Stamp on Minima…* or *Move to bin* for those ticked.
- **Send files** straight to another of your devices or to somebody you added, in no note, door to door on the same
  Wi-Fi. What arrives lands on Home, marked *new* until you open it.
- **Stamp on Minima…** (in a file's or a note's menu): see [Stamp on Minima with Integritas](#13-stamp-on-minima-with-integritas).

## 10. Files that stay on your PC

- **Share a file through Mininotes** (on the PC, in *Sync and sharing*): a spreadsheet or a document stays where it is on
  your PC and opens in its own program. Each time you save it, that version goes to your other devices, and a version
  saved elsewhere is written back into it. The whole file travels, the latest version: changes inside a spreadsheet are
  not merged. If it changed in two places before they met, Mininotes asks which version to keep and replaces nothing
  until you answer.
- **Sync a folder of this PC** (on the PC, in *Sync and sharing*): pick a folder, on C: or anywhere, and everything in it
  shows in Mininotes as a folder marked as living on your PC. Files you put in it, in Explorer or from any program,
  are taken in a few seconds later and go to your devices and to whoever you share the folder with; files that come
  from them are written into it. The files stay in that folder on your PC, outside the app. Deleting one on disk
  never deletes it in Mininotes, and the other way round; if a file changed in both places, Mininotes asks which
  version to keep. On a phone the folder's files are kept inside the app.
- **Notes as files.** A note or a folder can also be kept as Markdown files in a folder of your choice on the PC
  (*Save on my device…*). Mininotes never deletes those files, and a private note is never written out.
- Such a folder wears a folder with arrows on its tile. Its menu says where it is on the PC, then *Show in Explorer*,
  *Keep it inside only* and *Remove from the app*.

## 11. Versions, archive, bin and backups

- **Versions.** Every version of a note is kept; *Versions…* puts any of them back.
- **Archive** puts things away out of sight; the **bin** holds what you deleted, files included, until you empty it.
  Both are on Home, with a way back out. *Put back* puts a thing back where it was.
- **A note's file in the bin.** A file taken out of a note goes to the bin at once, with no question, and is taken out
  of the note on all your devices and for everybody the note goes to. *Put back* brings it back everywhere. It is gone
  for good only when the bin is emptied.
- **Backups.** *Export backup* writes one file with every folder, note and file. Without the lock (see
  [Privacy, encryption and keeping it safe](#14-privacy-encryption-and-keeping-it-safe)), it asks for a password for that
  backup. *Add from backup* brings one back and asks whether to add to what is there or replace it.

## 12. The Agent: ask, and have it done

The **Agent** (in the top bar on the PC, beside the menu on the phone, or Ctrl+K) answers questions about Mininotes from
the help kept inside the app ([help in English](android/app/src/main/resources/org/mininotes/android/help/en.txt),
[en français](android/app/src/main/resources/org/mininotes/android/help/fr.txt)), and does simple tasks said in English or
French: *what is new?*, *what is on my todo list?*, *add milk to Grocery*, *share Grocery with Carol*, *add a contact*,
*open*, *find*, *new note called…*, *put … in the dock*. It runs on your device with no model and no server, sends
nothing, shows anything that reaches other people or changes a note before doing it, and offers to take you to the
right place. A press outside its box closes it.

## 13. Stamp on Minima with Integritas

With an [Integritas](https://docs.integritas.technology/) account, a file or a note can be stamped on the Minima
blockchain from its menu. Only its fingerprint (SHA-256) leaves the device, never the file.

- **Stamp on Minima…** records now that the file exists exactly as it is: a proof, from that moment, that it existed
  and has not changed since. It uses a stamp of your Integritas account.
- **Verify on Minima…** checks a file, later, against what was recorded: whether it was stamped, unchanged, and in
  which block and on which date. It changes nothing and costs nothing.
- **The difference:** stamping writes the proof once; verifying reads it back, as often as you like.
- **A note's stamp** covers its title, an empty line, then its text; renaming it marks it changed. Stamps made before
  0.4.018 cover the text alone: *Note, text only*.
- **My profile, Stamping:** paste your API key and press *Save*; it is kept in your notebook, locked with it, and never
  put in a backup. Switch on any of your own devices under *Also on my devices* and the key goes to them too, sealed for
  each.
- **Stamped on Minima…**, last in *Find*, lists what was stamped here; open one for its fingerprint, block and
  transaction. Its **+** stamps a note or file in Mininotes, or a file on this device that is not put in Mininotes.

## 14. Privacy, encryption and keeping it safe

- **No server, no account, no tracking.** Notes are kept on your devices and go only to the people and devices you
  share them with.
- **Sealed end to end.** Every note and file is sealed for the device it goes to, then carried by
  [Maxima](https://minima.global) relays, which pass on messages they cannot read, or straight from device to device on
  the same network.
- **Lock Mininotes** (*Settings*, *Security*) encrypts the notebook, its files and your backups on the device. It opens
  with your fingerprint or screen lock on the phone and Windows Hello or your password on the PC, with 12 recovery words
  as a spare key: write them down. Without it, notes are not encrypted on the device.
- **Two things use the network:** the Maxima node that carries what you share, and a once-a-day look at this
  repository for a newer version, which you can switch off. [docs/SHARING.md](docs/SHARING.md) says what is sealed,
  what is signed, and what is not built yet.

## 15. Private spaces and the help request

**Private spaces**

- **Open or make one.** In any note, type the word private, then //: and your password, all together with nothing
  between them, and press Enter. The code leaves the note the moment the colon is typed, and the password goes into a
  box of its own, so neither stays in the note. The space that password opens comes up on a screen of its own, over the
  whole window. With a password never used before, type it again and a new, empty space is made.
- **One space for each password.** Every different password is its own separate space. There is no limit to how many
  you make, and nothing anywhere records how many you have.
- **Write each password down.** Because nothing records them, a mistyped password asks to be typed again as a new
  space: if that happens when you meant an existing one, close the box and try again. A new space can sit over an
  earlier one and lose part of it.
- **Where you are.** The private screen has its own colour and a lock beside *Private*, so you always know where you
  are. Make notes, folders and files with the + at the bottom. The eye by the password shows it while you hold it.
  Back closes the space.
- **Delete one.** Open it and choose *Delete this private space* in its menu. It is gone for good, and its password
  opens nothing after.
- **A long password.** Use a long phrase of several words as the password. Nobody can recover a forgotten one, not even
  with the 12 recovery words.
- **What it holds.** A space holds about 64 MB in all, enough for notes and about fifty photos. Its notes, folders,
  photos and files show nowhere else, stay on this device only for now, cannot be shared or sent, and close when you
  leave Mininotes or after five minutes without use.
- **A code is never kept in an ordinary note.** If one is left in a note, Mininotes takes it out, with the rest of its
  line, before the note is saved, sent or shared.
- **Files.** *From this device…* on the + adds a file into the space, where it is kept encrypted. The original stays
  where it was unless you choose to delete it, and the phone's file picker keeps its own list of files you opened
  recently.
- **Tip.** You can keep a believable space under one password and the real one under another, and use the believable
  one now and then so it looks lived in.
- **Their limits.** Private spaces are built so that nobody reading the app's files or its code can see them, or tell
  how many there are. That cannot be confirmed to be 100% safe, so keep only things there that would not harm you if
  they were found.

**Emergency contact**

- **What it is.** A help request sends your location, and the words you chose, to the people you chose, the moment a
  private note is opened. It is for when you may be in danger and someone might make you open Mininotes.
- **Set one.** Open a private space, open one of its notes, type the word help, then //:, all together, and press
  Enter. The code disappears at once and a box asks who to tell and what to say. The note you typed it in is the one
  that sends it. Only someone who knows the space's password can set one or take it away.
- **Never shown again.** Once set it is never shown again, anywhere: no menu says it, and the box always opens empty.
- **Take them away.** To take away every help request of a space, type the word erase, then //:, in one of its notes.
  That code disappears at once too.
- **Location.** Mininotes asks for the phone's location permission for this, and the system shows when it is used.

## 16. Languages and translation

Mininotes speaks eleven languages: English, French, German, Spanish, Italian, Portuguese, Russian, Chinese, Japanese,
Hindi and Persian. It asks which at its first start, the device's own picked already, and this README is in that
language too; *Settings, Language* changes both. *Translate…* in a note's menu puts the page into another language on
the device, without sending it to a translation service.

<!-- not in the app -->

## 17. Every menu entry, in one line

On the PC, resting the pointer on a menu entry says what it does. *My profile*, *Text size*, *Describe what each thing
does* turns these lines on or off. An entry ending with "…" opens a box or a page; one without does it at once.

| Entry | What it does |
|---|---|
| Rename | Give it another name. |
| Remove this tab | Take this tab out of the note. |
| Move left | Put this tab one place to the left. |
| Move right | Put this tab one place to the right. |
| Choose a picture | Show a picture of your own on it instead of its icon. |
| Remove the picture | Show its icon again instead of the picture. |
| Picture | Choose or remove a picture of your own for it. |
| Icon | Choose the icon it wears on Home and in lists. |
| Colour | Colour it, at the strength you choose. |
| Writing colour | The colour this person's writing is shown in. |
| Sharing | Who has it and what each may do. |
| Share | Share it with people or your devices, and choose what each may do. |
| Sync parameters | When and how its changes go to the others. |
| Sync now | Send what is waiting now, and ask the others for theirs. |
| Show my code | A code for another device or a person to scan, to receive it. |
| Share a file through Mininotes | Keep a file of this PC in step with your devices; it stays where it is. |
| Sync a folder of this PC | Keep a whole folder of this PC in step with your devices; its files stay in it. |
| People and devices | The people and devices you share with, and your groups. |
| My profile | Your name, your code, your connection and the app's settings that are yours. |
| From another device | Scan a code or paste a link from another device or a person. |
| Scan a code or paste their link | Add someone, or one of your devices, from their code or link. |
| Copy my link | Copy your link, to send to someone so they can add you. |
| Attach a file | Keep a file of this PC with this note. |
| Add a file | Keep a file of this PC in this folder. |
| Take a photo | Open the camera; the photo comes into this note. |
| Show as an album | Show its pictures in a grid, to browse them large. |
| Add photos | Add several pictures of this device at once. |
| Record audio | Record sound into this note. |
| Attachments | The files kept with this note. |
| Move to | Put it in another folder, or on Home. |
| Add to the dock | Put it in the row at the foot of Home, one click away. |
| Remove from the dock | Take it out of the dock. Nothing is deleted. |
| Add to favourites | Keep it among your favourites, and in the dock. |
| Copy the text | Copy the note's words, to paste them elsewhere. |
| Translate | Put this page into another language, on this device. |
| Versions | Every earlier version of this note, to look at or put back. |
| Stamp on Minima | Record now on the Minima blockchain that it exists as it is. Only its fingerprint leaves this device. |
| Verify on Minima | Check whether it is on Minima unchanged, and when it was recorded. |
| Temporary | Keep it only for a while: it goes when its time is up. |
| Archive | Put it away, out of sight, kept. |
| Move to bin | Put it in the bin. It can be put back until the bin is emptied. |
| Move to the bin | Put it in the bin. It can be put back until the bin is emptied. |
| New note | A new note, here. |
| New folder | A new folder, here. |
| New folder in | A new folder inside this one. |
| All pages | Every page of Home at once. |
| Back to the main page | Back to Home's first page. |
| Open link | Open this link in your browser. |
| Copy link | Copy this link. |
| Cut | Cut the words chosen. |
| Copy | Copy it, to paste elsewhere: a picture as a picture, a file as a file. |
| Paste | Paste what was copied. |
| Select all | Choose everything. |
| Undo | Take back the last change. |
| Writing lines | Show or hide the ruled lines of the page. |
| Recent | What you opened lately. |
| Temp | What is only kept for a while, and goes to your other devices. |
| Shared with me | What others shared with you, waiting for your answer. |
| Bin | What was put in the bin, to put back or delete for good. |
| Recovery writing | Writing that could not be saved into its note, kept aside. |
| Export backup | Write one file holding every folder, note and file. |
| Add from backup | Bring back what a backup file holds. |
| Share Mininotes | Tell someone about Mininotes. |
| Update to v | Install the newer version. |
| Feedback | Say something about Mininotes, publicly, under your name. |
| Settings | How Mininotes works on this device. |
| Mininotes on your phone | Mininotes on Android phones, with a code to scan to get it. |
| Mininotes on your PC | Mininotes on Windows, with the link to get it. |
| About | Which version this is, and who made Mininotes. |
| Uninstall Mininotes | Remove Mininotes from this PC, keeping your notes or not, after offering a backup. |
| Exit Mininotes | Close Mininotes. |
| Send files | Send files straight to one of your devices or a person, in no note. |
| Sent files | The files sent from here, and where each stands. |
| Open a Markdown file | Open a Markdown file of this PC as a note kept in that file. |
| Edit a file | Open any text file of this PC to edit it, without keeping it in Mininotes. |
| Search | Find notes, folders and files by their words. |
| Agent | Ask a question, or ask for something to be done. |
| Tree | Every folder and note at once, down the side. |
| Dock | Show or hide the dock on Home. |
| Menu | Everything Mininotes can do. |
| Things stay | How long what is in Temp is kept. |
| Language | The language of the app. |
| Send what is in Temp | Give what is in Temp to your other devices. |
| Who it goes to | Which of your devices and people receive what is in Temp. |
| Also show on Home | Show what is in Temp on Home as well. |
| Sync Temp now | Send what is in Temp now. |
| Save on my computer | Save a copy where you choose on this PC. |
| Save on my phone | Save a copy where you choose on this phone. |
| Save on my device | Keep it as a file in a folder of this device too. |
| Remove | Take it off. |
| Open | Open it. |
| Open with | Open it in another program. |
| Send to a device | Send it straight to one of your devices or a person. |
| From this device | Add files from this device. |
| Which version to keep | It changed in two places: choose which version stays. |
| Show in Explorer | Show where it is on this PC. |
| Synced with the file on this PC | Keep it in step with its file on this PC, or stop. |
| Replace with another file | Put another file in its place. |
| Put in a note | Keep it with a note. |
| Remove from Mininotes | Take it out of Mininotes. The file on this PC stays. |
| Empty the bin | Delete for good everything in the bin. |
| Hide from Home | Do not show it on Home; it stays in the menu. |
| Waiting | What waits to go, and why. |
| Remove from Recent | Take it off the list of what you opened lately. |
| Put back | Put it back where it was. |
| Delete for good | Delete it for good. It cannot be put back. |
| Text size | The size the words are shown at. |
| Same as other notes | Show this note at the usual size. |
| Find it | Tell Mininotes where its file is now. |
| Keep it inside only | Stop keeping it as a file on this device; it stays in Mininotes. |
| Remove from the app | Take it out of Mininotes; its file on this device stays. |
| Delete group | Delete this group. The people in it stay. |
| My device | Count it as one of your own devices. |
| Forget | Take this device off your list. Nothing is unshared. |
| Remove this person | Take this person off everything you shared with them, and out of your people. |
| Change password | A new password for your notebook. |
| Delete | Delete it. |
| This note | What can be done with the open note. |

<!-- in the app again -->

## 18. Updates

The app looks once a day for a newer version and says so in one line; *Update* then installs it. The
[latest release](https://github.com/mininotesorg/mininotes/releases/latest) always has the files.

## 19. Say something about it

*Feedback* in the app's menu opens a filled-in form here, under your own name; nothing is posted for you. Everything
anybody has said is in [the feedback issues](https://github.com/mininotesorg/mininotes/issues?q=is%3Aissue+label%3Afeedback).
A **security** problem goes privately to
[a security advisory](https://github.com/mininotesorg/mininotes/security/advisories/new), not to an issue.
[docs/FEEDBACK.md](docs/FEEDBACK.md) has the rest.

<!-- not in the app -->

## 20. Build it yourself

**Android** needs JDK 17 and the Android SDK (compileSdk 36):

```sh
cd android
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

The APK lands in `android/app/build/outputs/apk/release/`, unsigned: sign it with your own key, or use
`./gradlew installDebug`. `android/local.properties` points at your SDK and is not in the repository.

**Windows** needs JDK 17: run `./windows/build.ps1 -Package` and open the `Mininotes.exe` in
`dist/latest/Mininotes-Windows-<version>/`. See [windows/README.md](windows/README.md).

**Checks:** about 870 unit tests on Android and 1,360 on Windows, no device needed. They cover where being wrong loses
somebody's writing: the merge, arriving notes, who may do what, the schema migrations, pairing, attachments. What a real
device is needed for is checked on two phones and a PC before each release, in a log the maintainer keeps privately,
because it names their devices and quotes their notes. Ask in an issue for the entry for a build.

## 21. What is in this repository

| Path | What is in it |
| --- | --- |
| [android/](android/) | The Android app, and the code both apps share. No XML layouts: the views are built in Java. |
| [windows/](windows/README.md) | The Windows app and its packaging. |
| [docs/HOME.md](docs/HOME.md) | How the app is laid out, and every decision taken, numbered. |
| [docs/SHARING.md](docs/SHARING.md) | The sync design: keys, pairing, merge, conflicts. |
| [docs/PRODUCT.md](docs/PRODUCT.md) | What the app is for, and what it is not. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to help, and where to start. |

Few third-party runtime dependencies: ZXing for QR codes, SQLCipher with AndroidX SQLite for the encrypted notebook,
FlatLaf for the Windows look, PDFBox for a PDF's first page on the PC. It is built that way so the whole thing can be read.

<!-- in the app again -->

## 22. Licence

Mininotes is **free and open-source software** under the
[GNU General Public License, version 3 or later](LICENSE). You may use it, study it, change it and pass it on, and sell
it too, but whatever you pass on, changed or not, goes with its source code under the same licence. Third-party terms
are in [NOTICE](NOTICE).

The Maxima transport inside the app is the core of [eurobuddha/maxima](https://github.com/eurobuddha/maxima), vendored
unmodified with its author's permission and credited in NOTICE. It is theirs, and is not under the GPL; its own
repository does not yet carry a licence.

<!-- not in the app -->

Until 26 September 2026 Mininotes was source-available (Apache 2.0 with the Commons Clause and a paid-product
condition); copies taken before then keep those terms.

Contributions: [CONTRIBUTING.md](CONTRIBUTING.md). Security reports: [SECURITY.md](SECURITY.md).
