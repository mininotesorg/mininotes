// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;

/**
 * README, one text on GitHub and in the app (the owner, 2026-10-19: "these should match: the note in the app and what is
 * available on GitHub"; decision 221): the copy inside the app the same as the root one, its hash listed, each markdown
 * form written as the note writes it, nothing in it a code, and an untouched Read me renamed and said again.
 */
public class ReadMeTextTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final Path ROOT=Path.of("..").toAbsolutePath().normalize();
    private static final Path COPY=ROOT.resolve("android/app/src/main/resources/org/mininotes/android/help/README.md");
    private static final Path HASHES=ROOT.resolve("android/app/src/main/resources/org/mininotes/android/help/README.sha256");

    private static String read(Path file) throws Exception {
        return Files.readString(file,StandardCharsets.UTF_8).replace("\r\n","\n");
    }

    @Test public void readMeSaysItAll() {
        String says=NoteStore.READ_ME_SAYS;
        assertEquals("README",NoteStore.READ_ME);
        assertEquals("made from the README inside the app",ReadMe.text(),says);
        assertFalse("private alone is the way in",says.contains("privatespace"));
        assertTrue(says.contains("type the word private, then //: and your password, all together"));
        assertTrue("its limits said plainly",says.contains("cannot be confirmed to be 100% safe"));
        assertTrue(says.contains("believable space")&&says.contains("about 64 MB"));
        assertTrue(says.contains("**Emergency contact**")&&says.contains("type the word help, then //:")&&says.contains("type the word erase, then //:"));
        assertTrue(says.contains("The Agent")&&says.contains("Stamp on Minima")&&says.contains("Photos")&&says.contains("12 recovery words"));
        assertEquals("nothing in it is taken for a code",says,PrivateCode.scrub(says));
        assertFalse(PrivateCode.holds(says));
        assertTrue("a note holds 24,000 characters: leave more of README.md out of the app ("+says.length()+")",says.length()<=ReadMe.MOST);
        assertFalse("GitHub's parts left out",says.contains("Every menu entry")||says.contains("./gradlew")||says.contains("Contents"));
        // The hand-written ones before are said again; the README is not one of them.
        assertTrue(Arrays.asList(NoteStore.READ_ME_BEFORE).containsAll(List.of(NoteStore.READ_ME_0_3_050,NoteStore.READ_ME_0_4_008,NoteStore.READ_ME_0_4_009)));
        assertFalse(Arrays.asList(NoteStore.READ_ME_BEFORE).contains(says));
        assertTrue(Icons.known(NoteStore.READ_ME_ICON));
    }

    /** The copy inside the app is the root README.md, word for word. */
    @Test public void theAppsCopyIsTheReadme() throws Exception {
        assertEquals("copy README.md to "+ROOT.relativize(COPY),read(ROOT.resolve("README.md")),read(COPY));
        assertEquals(ReadMe.note(read(ROOT.resolve("README.md"))),NoteStore.READ_ME_SAYS);
    }

    /** Every text a build writes is listed, so a later README still finds a note nobody changed. */
    @Test public void everyReadmeWrittenIsListed() throws Exception {
        String hash=ReadMe.sha256(NoteStore.READ_ME_SAYS);
        assertTrue("README.md changed: add this line to "+ROOT.relativize(HASHES)+": "+hash,read(HASHES).lines().anyMatch(hash::equals));
        assertTrue(ReadMe.shipped(NoteStore.READ_ME_SAYS));
        assertFalse(ReadMe.shipped(NoteStore.READ_ME_SAYS+"\nMy own line"));
        assertTrue(NoteStore.readMeUntouched(NoteStore.READ_ME_0_4_009));
        assertFalse(NoteStore.readMeUntouched(NoteStore.READ_ME_0_4_009+" "));
    }

    @Test public void eachMarkdownFormAsTheNoteWritesIt() {
        assertEquals("**Mininotes**\nA pad.",ReadMe.note("# Mininotes\n\nA pad."));
        // A heading's number is GitHub's contents', left out with them; a version is not a number.
        assertEquals("**What's new**\nNewest first.\n\n**0.4.010**\n- One.",ReadMe.note("## 1. What's new\n\nNewest first.\n\n### 0.4.010\n\n- One."));
        // What GitHub alone needs, between the two comments, is left out; another comment is nothing.
        assertEquals("Kept.\n\nAgain.",ReadMe.note("Kept.\n\n"+ReadMe.LEAVE_OUT+"\n\n## 20. Build it\n\nSteps.\n\n"+ReadMe.TAKE_IN+"\n\n<!-- a remark -->\nAgain."));
        // A paragraph's lines joined, an item's own next line too, a nested item kept under it.
        assertEquals("One line and its next.\n\n- An item and its next.\n  - nested and its next.",
            ReadMe.note("One line\nand its next.\n\n- An item\n  and its next.\n  - nested\n    and its next."));
        assertEquals("See What's new.",ReadMe.inline("See [What's new](#1-whats-new)."));
        assertEquals("See docs/SHARING.md.",ReadMe.inline("See [docs/SHARING.md](docs/SHARING.md)."));
        assertEquals("from the latest release (https://example.org/latest), then",ReadMe.inline("from [the latest release](https://example.org/latest), then"));
        assertEquals("open Mininotes.exe",ReadMe.inline("open `Mininotes.exe`"));
        assertEquals("**bold**, *italic* and __underline__",ReadMe.inline("`**bold**`, *italic* and `__underline__`"));
        // A badge, a picture inside a link, is nothing, and its line with it.
        assertEquals("Before.\n\nAfter.",ReadMe.note("Before.\n\n[![Latest](https://img.example/b.svg)](https://example.org/r)\n\nAfter."));
        assertEquals("A  picture.",ReadMe.inline("A ![logo](logo.png) picture."));
        // A table: the heading row and the dashes gone, each row a line.
        assertEquals("**Menu**\n- Rename: Give it another name.\n- android/: The Android app.",
            ReadMe.note("## Menu\n\n| Entry | What it does |\n|---|---|\n| Rename | Give it another name. |\n| [android/](android/) | The Android app. |"));
        // A code block's lines as they are, never joined.
        assertEquals("Run:\n\ncd android\n./gradlew build\n\nDone.",ReadMe.note("Run:\n\n```sh\ncd android\n./gradlew build\n```\n\nDone."));
        // A rule is a gap; a line all bold a heading of its own.
        assertEquals("Above.\n\n**Private spaces**\n- One.",ReadMe.note("Above.\n\n---\n\n**Private spaces**\n\n- One."));
        assertEquals("**A** (9 or newer): words.",ReadMe.note("**A** (9 or newer):\nwords."));
        assertEquals("CRLF too","**T**\nx",ReadMe.note("# T\r\n\r\nx\r\n"));
    }

    private NoteStore store() throws Exception {
        NoteStore store=new NoteStore(new Context(temp.newFolder()));store.getWritableDatabase();open.add(store);return store;
    }

    private static NoteStore.Note note(NoteStore store,String title,String body) {
        NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title=title;n.body=body;n.revision=1;store.save(n);return n;
    }

    /** An untouched Read me is README now, said again; one written in keeps its name and words; README not said twice. */
    @Test public void anUntouchedReadMeBecomesTheReadme() throws Exception {
        NoteStore store=store();
        NoteStore.Note old=note(store,"Read me",NoteStore.READ_ME_0_4_009),mine=note(store,"Read me",NoteStore.READ_ME_0_4_009+"\nMy own line");
        NoteStore.Note made=note(store,"Read me",NoteStore.READ_ME_SAYS);
        assertTrue(store.readMeAgain());
        assertEquals(NoteStore.READ_ME,store.get(old.id).title);assertEquals(NoteStore.READ_ME_SAYS,store.get(old.id).body);
        assertTrue("the old words kept as a version",store.versions(old.id).stream().anyMatch(v->NoteStore.READ_ME_0_4_009.equals(v.body)));
        assertEquals("one this build wrote, under the old name, renamed",NoteStore.READ_ME,store.get(made.id).title);
        assertEquals("Read me",store.get(mine.id).title);assertEquals(NoteStore.READ_ME_0_4_009+"\nMy own line",store.get(mine.id).body);
        assertEquals("both names wear its icon",NoteStore.READ_ME_ICON,store.iconOf(NoteStore.Branch.Kind.PAGE,mine.id));
        long revision=store.get(old.id).revision;
        assertFalse("nothing more to say",store.readMeAgain());
        assertEquals(revision,store.get(old.id).revision);
    }

    /** Show on Home hides the README under either name, and shows it again. */
    @Test public void showOnHomeHidesEitherName() throws Exception {
        NoteStore store=store();
        NoteStore.Note readme=note(store,NoteStore.READ_ME,NoteStore.READ_ME_SAYS),mine=note(store,"Read me","Mine"),other=note(store,"Groceries","Milk");
        store.setReadMeOnHome(false);
        List<String> shown=new ArrayList<>();for(NoteStore.Branch one:store.contents(Things.HOME))shown.add(one.id);
        assertFalse(shown.contains(readme.id));assertFalse(shown.contains(mine.id));assertTrue(shown.contains(other.id));
        store.setReadMeOnHome(true);
        shown.clear();for(NoteStore.Branch one:store.contents(Things.HOME))shown.add(one.id);
        assertTrue(shown.contains(readme.id)&&shown.contains(mine.id));
    }
}
