# Mininotes on F-Droid

Status: plan, drafted 2026-10-09 for the owner to review. Nothing here is built and nothing has been
sent to F-Droid. The website's roadmap already promises it: "Mininotes listed on F-Droid, built from
its public source, so anybody can install and update it without us in between."

## Where it stands

F-Droid lists an app only if it can build the app itself, from public source, with free tools, and
find nothing in it that is not free software. Most of that is already true of Mininotes (checked
2026-10-09 against the inclusion policy and the submission guide, see Sources at the end):

- The project is GPL-3.0-or-later, with LICENSE and NOTICE at the root.
- The public repository (github.com/mininotesorg/mininotes) carries the whole `android/` tree, and every
  release is a tag `vX.Y.Z` on it that equals the build's `versionName`. The version lives in the usual
  place, `android/app/build.gradle`, so F-Droid's update checker can read it without running Gradle.
- Gradle 8.11.1, Android Gradle Plugin 8.10.1 and JDK 17 are all free software and all on F-Droid's
  build servers. Dependencies come only from Maven Central and Google's Maven: ZXing core (Apache-2.0),
  sqlcipher-android (BSD-3, a prebuilt AAR from Maven Central, which the policy allows), androidx.sqlite
  (Apache-2.0). JUnit is test-only.
- No Play Services, no Firebase, no advertising, no analytics, no tracker of any kind.
- The application ID `org.mininotes.android` is under a domain the owner holds, which is what F-Droid asks.
- The release is built in CI from a clean tagged commit, without minification, and signed with
  `apksigner`. That is the shape a reproducible build needs (Phase 4).
- The only binary in the tree is `gradle-wrapper.jar`, which F-Droid's scanner recognises and allows.

Four things stand in the way, one of them outside our hands:

1. **The Maxima core has no licence.** `android/maxima-core/` is 88 files vendored from
   github.com/eurobuddha/maxima, which declares no licence (checked again 2026-10-09). Its author's
   "go for it, with my blessing" is a permission to Mininotes, recorded in NOTICE and
   docs/MAXIMA-TRANSPORT.md; it is not a licence, and NOTICE says so in as many words. F-Droid judges
   licences by the OSI, FSF and SPDX lists and reads the source; a reviewer will open that directory,
   read our own NOTICE, and stop there. IzzyOnDroid, the looser repository people go to when F-Droid
   says no, has the same rule, so there is no side door. This is the one blocker, and it is the author's
   decision, not ours.
2. **The app fetches and installs its own updates.** Tap *Update to v…* and the pad downloads the APK
   from the GitHub release, checks it and hands it to Android's installer, under
   `REQUEST_INSTALL_PACKAGES`. The policy allows an app to download executables only with explicit
   opt-in, where declining is no harder than accepting and the person is told they are going round
   F-Droid's checks. Today the row is there for everybody and nothing is said.
3. **F-Droid has nothing to show.** It takes its listing from `fastlane/metadata/android/en-US/` in
   the repository: a one-line summary, a description, a 512 px icon, screenshots, and a changelog per
   `versionCode`. None of that exists yet.
4. **Small untruths to fix.** `android/README.md` says SDK platform 36; `build.gradle` says
   `compileSdk 37`. The README's "This initial ID must be finalized before public distribution" is
   stale: the ID shipped in 0.0.x and cannot change now.

## The goal

Mininotes on f-droid.org, built by F-Droid from the public tag and updated by F-Droid itself whenever a
new tag appears, with nothing for us to do per release beyond what the release already does. Best of
all, the very same signed file on F-Droid as on GitHub, so a person can install from either and move
between them without losing a notebook.

## Phase 0: the licence (the author's decision)

1. Ask the Maxima author to put a licence file on their repository. Three lines, as every message to
   somebody else is:

   > Mininotes is going to F-Droid, which lists only code under a licence it can read, and the Maxima
   > core inside it has none yet. Would you add a LICENSE file to github.com/eurobuddha/maxima, whichever
   > free licence you prefer (Apache-2.0 is what Minima's own MDS library uses)? Nothing else changes:
   > your code stays yours and stays credited to you, exactly as now.

2. When it is there: re-vendor `core/src/main` at the first commit that carries the licence, byte for
   byte as before, and put a copy of the LICENSE inside `android/maxima-core/`. Record the new commit
   hash and file count in NOTICE, `android/maxima-core/NOTICE` and docs/MAXIMA-TRANSPORT.md, which
   already says that when a licence appears "that licence governs this tree from then on, and this page
   should say so". NOTICE then names the licence and keeps the permission story as history.
3. If the answer is no, or does not come: F-Droid and IzzyOnDroid are both closed. The remaining way to
   let people install and update from an F-Droid client is our own repository (an `fdroid`-format repo
   on mininotes.org, built by our CI and signed with our key). That is a real option, and the client
   treats it like any other repo once added, but it is not "on F-Droid" and nobody finds it by
   searching. It is listed here so the fallback is known, not as a recommendation.

Everything below can be built and tested while waiting for the answer; only the submission waits on it.

## Phase 1: the update, made to the rule

The in-app install was the owner's ask and stays, as an opt-in. One build for everybody, no flavour,
because Phase 4 needs the F-Droid build and the GitHub build to be the same file.

1. **About gets a switch, off by default:** *Fetch updates from GitHub and install them here*, with its
   quiet line: "Off, the pad only says when a newer version is out. On, it fetches the file from the
   release, checks it, and asks Android to install it. A pad installed from F-Droid is updated by F-Droid;
   switching this on goes round F-Droid's checks." That is the policy's three conditions, each in a
   sentence: opt-in, declining no harder than accepting, told what it bypasses.
2. **With the switch off,** *Update to v…* opens the release page, nothing more. `REQUEST_INSTALL_PACKAGES`
   stays declared, because the switch exists; the policy accepts that when the install is opt-in.
3. **A pad installed from F-Droid** (Android says who installed a package: `InstallSourceInfo`,
   `org.fdroid.fdroid` or one of its forks) shows the same row with the same switch, and its About says
   "Installed from F-Droid" so nobody wonders why the row reads differently. The existing "signed with a
   different key" message already covers the case where F-Droid's build and ours are not the same file.
4. **The daily look stays as it is** (one line of text, nothing sent, switch in About). It is not an
   executable and the policy has nothing against it. See Counting below for a change to where it looks.
5. Tests: `Update` stays pure and tested; a unit test for the three states of the row (off, on, F-Droid);
   seen on the emulator with a synthetic notebook: switch off, tap Update, the browser opens; switch on,
   the fetch runs as today. A verification entry in the log the maintainer keeps privately. README and the manifest
   comment rewritten to say what is now true.

The alternative, two builds (`github` with the installer, `fdroid` without, by product flavour), is
simpler to explain to a reviewer but costs the one-key goal: F-Droid compares its build with ours, so
ours would have to be the F-Droid flavour, and the GitHub release would lose the installer anyway. Not
recommended.

## Phase 2: what F-Droid reads from the repository

Under `fastlane/metadata/android/en-US/` in the public repository:

- `short_description.txt`, under 80 characters, no full stop: "A paper pad that shares a note phone to
  phone, with nobody in between".
- `full_description.txt`: the README's first two paragraphs, nothing about Windows.
- `images/icon.png`, 512 by 512: the launcher icon, rendered from the adaptive icon;
  `dist/mininotes-avatar-512.png` exists untracked and should be checked against the icon on the phone.
- `images/phoneScreenshots/1.png …`: four to six, from the **demo build** (made-up notes, light theme,
  the same one the website animation came from), never from the owner's phones. Home, an open note,
  the sharing box, People and devices.
- `changelogs/<versionCode>.txt`, 500 characters at most, one per release from the first one listed.
  The release workflow should refuse a tag whose changelog file is missing, the way it already refuses a
  tag that disagrees with `latest.txt`.
- Fix the README's SDK line and the stale ID sentence (point 4 above).

## Phase 3: the recipe, and the submission

Everything F-Droid needs to build is one YAML file in its `fdroiddata` repository, which we write and
send as a merge request on GitLab.

1. Fork `gitlab.com/fdroid/fdroiddata`, branch `org.mininotes.android`, file
   `metadata/org.mininotes.android.yml`:

   ```yaml
   Categories: [Writing]
   License: GPL-3.0-or-later
   AuthorName: Mininotes contributors
   WebSite: https://mininotes.org
   SourceCode: https://github.com/mininotesorg/mininotes
   IssueTracker: https://github.com/mininotesorg/mininotes/issues
   Changelog: https://github.com/mininotesorg/mininotes/releases

   RepoType: git
   Repo: https://github.com/mininotesorg/mininotes

   Builds:
     - versionName: 0.3.0xx
       versionCode: 30xx
       commit: v0.3.0xx
       subdir: android/app
       gradle:
         - yes

   AutoUpdateMode: Version
   UpdateCheckMode: Tags ^v[0-9.]+$
   CurrentVersion: 0.3.0xx
   CurrentVersionCode: 30xx
   ```

   Summary and Description are taken from fastlane, so they are not repeated here. `AutoUpdateMode:
   Version` with `UpdateCheckMode: Tags` means that from then on a tag is the release: F-Droid sees it,
   adds the build block itself, builds it in the next cycle, and publishes 24 to 48 hours after (signing
   is by hand at their end).
2. Test the recipe before sending it. Two ways: push the branch to the fork and read its GitLab CI
   pipeline, or run `fdroidserver` locally in its Docker image (`fdroid readmeta`, `fdroid lint`,
   `fdroid checkupdates --allow-dirty`, `fdroid build org.mininotes.android`; about 5 GB of disk and
   2 GB of traffic the first time). The build must pass with no network beyond Maven, no SDK component
   missing (`compileSdk 37` has to be installable by their `sdkmanager`), and the scanner must find
   nothing it dislikes.
3. Open the merge request, titled `New App: org.mininotes.android`. Reviewers read the licence, open
   `android/maxima-core/` (Phase 0 has to be done by now), may ask about the location permission (the
   help request, opt-in, decision 113) and the install permission (Phase 1's switch), and may patch the
   recipe themselves. Answer within a day; a quiet submission is closed.
4. No anti-feature expected. The relays are public Minima nodes anybody can run, so NonFreeNet does not
   apply; the update look reads a text file and is switchable; there is nothing proprietary.
5. Timeline, from the guide: review in days to weeks depending on the queue, then a build cycle, then
   one to two days to publish. The listing appears on f-droid.org a little after the client has it.

## Phase 4: one key everywhere (reproducible builds)

F-Droid normally signs what it builds with its own key. Android will not install a file signed with one
key over an app signed with another, and uninstalling takes the notebook with it. So a person who
installed from GitHub could not switch to F-Droid, nor the other way round, without a backup and a
fresh start, and a person who flipped Phase 1's switch on an F-Droid pad would get the "different key"
message every time. F-Droid's way out is reproducible builds: it builds from the recipe, downloads our
signed APK from the release, and if the two are the same bytes apart from the signature, it publishes
ours. One file, one key, both doors.

The guide says this is "not required but best practice" and that "switching later is hard", because the
first file published on F-Droid fixes which key F-Droid users have. So the choice is made before Phase 3
is merged, not after.

Mininotes is a good candidate: plain Java, no R8, no native code of our own, no PNGs to crunch (the
resources are XML and vector), built in CI from a clean tag, signed with `apksigner`. What has to be
checked, in order:

1. Build the same tag twice on two machines (the laptop and GitHub's runner) and compare the unsigned
   APKs with `diffoscope`. Known sources of difference from F-Droid's list: the JDK (Temurin 17 in CI,
   Debian's OpenJDK 17 at F-Droid; the same javac output in practice, but it is checked, not assumed);
   AGP's `vcsInfo` (the same from a clean tag); `platformBuildVersionName`, which follows the exact
   platform revision of `compileSdk`; and the build-tools version. `release.yml` signs with whatever
   build-tools is newest on the runner (`sort -V | tail -1`); pin it, and note F-Droid's warning that
   `apksigner` from build-tools 35.0.0-rc1 on has broken signature copying for some apps, so the pin may
   have to be 34.0.0 for the signing step.
2. Run `fdroid build` and then `fdroid verify` against the GitHub release locally. Only a passing
   `verify` counts.
3. Add to the recipe: `Binaries: https://github.com/mininotesorg/mininotes/releases/download/v%v/Mininotes-%v.apk`
   and `AllowedAPKSigningKeys: <sha256 of our signing certificate>`. The certificate fingerprint is
   public (it is in every APK); the key is not, and stays where it is.
4. If verification cannot be made to pass in a bounded time, publish F-Droid-signed, say in the README
   and in About which channel a pad came from, and keep the backup-then-reinstall path documented.
   That is a one-way door, so it is the owner's call, made knowingly.

## Phase 5: after it is listed

- README and mininotes.org: the F-Droid badge beside the GitHub one; the website's "On F-Droid" line
  moves from roadmap to done.
- Each release adds `changelogs/<versionCode>.txt`; the iteration loop gains that line. Nothing else
  changes: tag, CI builds and signs, F-Droid follows.
- Watch F-Droid's build status (monitor.f-droid.org, and the `fdroiddata` issue tracker) after each tag:
  a failed build is reported there and nowhere else.
- Keep `versionCode` rising as it does (0.3.031 is 3031). F-Droid refuses a release whose code does not
  rise.

## Counting installs

F-Droid gives no numbers, on purpose. The client reports nothing about what it installs, F-Droid dropped
its own download counts years ago because mirrors made them wrong, and the counter and the opt-in
"popularity contest" its team has talked about on the forum were not confirmed shipped when checked on
2026-10-09. Two outside pages estimate from f-droid.org's own download logs (divested.dev's F-Droid stats
page, the fdroid-metrics dashboard); they miss mirrors and lag, so they say "some dozens" rather than a
number.

What we have today, and what we could have:

1. **GitHub counts every download of a release file** and the API says so per file. As of 2026-10-09
   the Android files have been downloaded 73 times across all releases and the Windows zips 24 times
   (the versioned file and the fixed-name copy added together; a read of the `.sha256` beside it does
   not count). A ten-line script under `tools/` can print that table any day. This counts downloads, not
   installs, and nothing from F-Droid.
2. **The pad already asks once a day whether it is behind.** Every pad with the look switched on reads
   `dist/latest.txt` from `raw.githubusercontent.com`, which keeps no log we can see. If that one line
   were served from the owner's own server instead (the one that already takes the nightly backup), its
   access log would count daily looks: the number of distinct pads asking in a day is, give or take,
   the number of pads in use. Nothing new is sent (the request already carries an address and a user
   agent to GitHub today); what changes is who sees it. To stay honest with the README's "no analytics":
   keep only a date, a count and the channel in the log, never the address; and say in README and in
   About that the look goes to mininotes.org and is counted. The channel is one word Android already
   knows (who installed the package, Phase 1's point 3), so GitHub pads and F-Droid pads are counted
   apart. The switch that turns the look off is already in About. If the server is down the pad says
   nothing and tries tomorrow, as it does now when GitHub is unreachable.
3. **An opt-in counter in the app** ("tell mininotes.org this pad exists", asked once, default no) is
   what the F-Droid forum suggests; it is allowed, and it is heavier than option 2 for the same number.
   Not recommended while option 2 is available.

Recommendation: do 1 now, since it costs nothing; do 2 together with Phase 1, since both touch the same
About box; and read F-Droid's third-party pages as a rough second opinion, not a figure.

## Decisions for the owner

1. Send the three lines to the Maxima author, or not. Without a licence, Phases 3 and 4 cannot happen.
2. The in-app installer: an opt-in switch off by default (recommended, keeps the feature), or taken
   out and replaced by the release page.
3. One key everywhere before the first F-Droid build (recommended, Phase 4), or F-Droid's key and two
   channels that cannot be crossed without a backup.
4. Counting: the daily look served and counted from the owner's server (option 2), or GitHub's download
   counts only.
5. Screenshots from the demo build: yes, or the owner picks the screens.

## Sources

- F-Droid inclusion policy: https://f-droid.org/docs/Inclusion_Policy/
- Submitting to F-Droid, quick start: https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- Reproducible builds: https://f-droid.org/docs/Reproducible_Builds/
- F-Droid forum, "Does F-Droid provide any sort of stats?": https://forum.f-droid.org/t/does-f-droid-provide-any-sort-of-stats/4346
- Third-party estimates: https://divested.dev/pages/fdroid_stats and the fdroid-metrics dashboard
- IzzyOnDroid inclusion policy (same licence rule): https://izzyondroid.org/docs/general/AppInclusionPolicy/
- The Maxima repository, no licence as of 2026-10-09: https://github.com/eurobuddha/maxima
