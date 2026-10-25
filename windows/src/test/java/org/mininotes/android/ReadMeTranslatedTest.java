// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;

/**
 * README in every language (the owner, 2026-10-21: "the user gets everything in the language of its choice, including the
 * README right away", and "I translate, every change"; decision 232): each translation made from today's README.md, the
 * app's copy the same, the same parts left out of the note, a note that fits, nothing in it a code, its text listed; and
 * the note written and said again in the language in use. Made-up notebooks.
 */
public class ReadMeTranslatedTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();Say.use("en");}

    private static final Path ROOT=Path.of("..").toAbsolutePath().normalize();
    private static final Path HELP=ROOT.resolve("android/app/src/main/resources/org/mininotes/android/help");
    private static String read(Path file) throws Exception {return Files.readString(file,StandardCharsets.UTF_8).replace("\r\n","\n");}
    private static List<String> markers(String md) {
        List<String> out=new ArrayList<>();
        for(String line:md.split("\n",-1))if(line.trim().equals(ReadMe.LEAVE_OUT)||line.trim().equals(ReadMe.TAKE_IN))out.add(line.trim());
        return out;
    }

    @Test public void everyTranslationIsOfTodaysReadme() throws Exception {
        String english=read(ROOT.resolve("README.md"));
        String made="<!-- Translated from README.md, SHA-256 "+ReadMe.sha256(english)+" -->";
        String hashes=read(HELP.resolve("README.sha256"));
        for(String code:Say.LANGUAGES.keySet()) {
            if("en".equals(code))continue;
            Path root=ROOT.resolve("README."+code+".md");
            assertTrue("README."+code+".md is missing",Files.exists(root));
            String md=read(root);
            assertEquals("README.md changed: bring README."+code+".md up to it, then its first line",made,md.split("\n",2)[0]);
            assertEquals("copy README."+code+".md to "+ROOT.relativize(HELP),md,read(HELP.resolve("README."+code+".md")));
            assertEquals(code+": the same parts left out of the app",markers(english),markers(md));
            String says=ReadMe.text(code);
            assertNotEquals(code+": its own text, not English's",ReadMe.text(),says);
            assertTrue(code+": a note holds 24,000 characters ("+says.length()+")",says.length()<=23500);
            assertEquals(code+": nothing in it is taken for a code",says,PrivateCode.scrub(says));
            assertFalse(PrivateCode.holds(says));
            assertFalse(code+": no long dashes",says.contains("—"));
            String line=code+" "+ReadMe.sha256(says);
            if(hashes.lines().noneMatch(line::equals)){missing.append(line).append('\n');continue;}
            assertTrue(ReadMe.shipped(says));assertEquals(code,ReadMe.language(says));
        }
        assertEquals("README translations changed: add these lines to help/README.sha256","",missing.toString());
    }
    private final StringBuilder missing=new StringBuilder();
    /** Where two texts first differ, for a message that shows it. */
    static String differs(String a,String b) {
        if(a==null||b==null)return a+" / "+b;
        int i=0;while(i<a.length()&&i<b.length()&&a.charAt(i)==b.charAt(i))i++;
        return "at "+i+": «"+a.substring(Math.max(0,i-30),Math.min(a.length(),i+60))+"» / «"+b.substring(Math.max(0,i-30),Math.min(b.length(),i+60))+"»";
    }

    private NoteStore store(String name) throws Exception {
        NoteStore one=new NoteStore(new Context(temp.newFolder(name)));one.getWritableDatabase();open.add(one);return one;
    }
    private static String readMe(NoteStore store) {
        for(NoteStore.Note n:store.pages(Things.HOME))if(NoteStore.READ_ME.equals(n.title))return store.get(n.id).body;
        return null;
    }

    @Test public void theFirstStartWritesItInTheLanguageChosen() throws Exception {
        Say.use("fr");
        NoteStore fr=store("fr");
        assertTrue(fr.welcome("0.4.019"));
        assertEquals(differs(ReadMe.text("fr"),readMe(fr)),ReadMe.text("fr"),readMe(fr));
        assertTrue("My first note in French too",fr.pages(Things.HOME).stream().anyMatch(n->Say.t(NoteStore.FIRST_NOTE).equals(n.title)));
    }

    @Test public void aNewNotebooksFirstFoldersTakeTheLanguageChosen() throws Exception {
        Say.use("fr");
        NoteStore fr=store("fr");
        assertTrue(fr.welcome("0.4.024"));
        assertEquals("Mes notes",fr.collectionName(SchemaMigrations.FIRST_COLLECTION));
        assertEquals(Say.t("Notes"),fr.bookName(SchemaMigrations.FIRST_BOOK));
        // A notebook that already holds a note (a backup put back, a device joining) keeps its folders' names.
        Say.use("en");NoteStore used=store("used");
        NoteStore.Note n=new NoteStore.Note();n.title="Kept";n.body="";n.book=SchemaMigrations.FIRST_BOOK;n.updated=1;n.revision=1;used.save(n);
        Say.use("fr");used.welcome("0.4.024");
        assertEquals("My notes",used.collectionName(SchemaMigrations.FIRST_COLLECTION));
        Say.use("en");NoteStore en=store("en");en.welcome("0.4.024");
        assertEquals("My notes",en.collectionName(SchemaMigrations.FIRST_COLLECTION));
    }

    @Test public void saidAgainInTheLanguageInUseButNeverBackAndForth() throws Exception {
        // An English README on a device now in French: said in French at the next start.
        Say.use("en");NoteStore one=store("one");one.welcome("0.4.018");
        Say.use("fr");one.readMeAgain();
        assertEquals(ReadMe.text("fr"),readMe(one));
        // A French README on a device in English: left as the other device wrote it, at a start.
        Say.use("en");one.readMeAgain();
        assertEquals(ReadMe.text("fr"),readMe(one));
        // Unless the language was just changed here: then in it.
        one.readMeAgain(true);
        assertEquals(ReadMe.text(),readMe(one));
    }
}
