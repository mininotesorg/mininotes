package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class SectionsTest {
    @Test public void aNoteWithNoTabLineIsOneUnnamedTab() {
        List<Sections.Section> all=Sections.of("Bread\nTomatoes");
        assertEquals(1,all.size());assertNull(all.get(0).name);assertEquals("Page 1",all.get(0).called(0));
        assertEquals("Bread\nTomatoes",Sections.words("Bread\nTomatoes",0));
        assertFalse(Sections.tabbed("Bread\nTomatoes"));assertFalse(Sections.tabbed(""));
    }

    @Test public void tabLinesSplitTheWordsAndWordsBeforeThemAreAFirstTab() {
        String body="Intro\n=== Ideas ===\none\ntwo\n=== Receipts ===\nthree";
        List<Sections.Section> all=Sections.of(body);
        assertEquals(3,all.size());
        assertNull(all.get(0).name);assertEquals("Ideas",all.get(1).name);assertEquals("Receipts",all.get(2).name);
        assertEquals("Intro",Sections.words(body,0));assertEquals("one\ntwo",Sections.words(body,1));assertEquals("three",Sections.words(body,2));
        assertTrue(Sections.tabbed(body));
        assertTrue(Sections.tabbed("=== Only ===\nwords"));
    }

    @Test public void onlyAWholeTidyLineIsATabLine() {
        assertTrue(Sections.isLine("=== Ideas ==="));assertTrue(Sections.isLine("=== Ideas ===\r"));
        for(String not:new String[]{"===  ===","=== ===","== Ideas ==","=== Ideas ==="+" more","x === Ideas ===","=== a === b ===","===  Ideas ===","=== "+"x".repeat(41)+" ==="})
            assertFalse(not,Sections.isLine(not));
        assertEquals(1,Sections.of("Text with === Ideas === inside").size());
    }

    @Test public void changingOneTabKeepsEveryOtherLetter() {
        String body="Intro\n=== Ideas ===\none\n=== Receipts ===\nthree";
        String now=Sections.put(body,1,"one\nand more");
        assertEquals("Intro\n=== Ideas ===\none\nand more\n=== Receipts ===\nthree",now);
        assertEquals("Intro",Sections.words(now,0));assertEquals("three",Sections.words(now,2));
        assertEquals("Intro changed\n=== Ideas ===\none\n=== Receipts ===\nthree",Sections.put(body,0,"Intro changed"));
        assertEquals("Intro\n=== Ideas ===\none\n=== Receipts ===\n",Sections.put(body,2,""));
        // Every tab read back and put back unchanged leaves the note exactly as it was.
        for(int i=0;i<3;i++)assertEquals(body,Sections.put(body,i,Sections.words(body,i)));
    }

    @Test public void wordsGoIntoAnEmptyTabWithoutJoiningTheNextTabLine() {
        String body="=== A ===\n=== B ===\nb";
        assertEquals("",Sections.words(body,0));
        String now=Sections.put(body,0,"a");
        assertEquals("=== A ===\na\n=== B ===\nb",now);
        assertEquals(2,Sections.of(now).size());assertEquals("a",Sections.words(now,0));assertEquals("b",Sections.words(now,1));
        String last=Sections.put("=== A ===\na\n=== B ===",1,"b");
        assertEquals("=== A ===\na\n=== B ===\nb",last);assertEquals("b",Sections.words(last,1));
    }

    @Test public void addingATabToAPlainNoteMakesItsWordsPageOne() {
        String now=Sections.add("Bread\nTomatoes","Ideas");
        assertEquals("=== Page 1 ===\nBread\nTomatoes\n=== Ideas ===",now);
        List<Sections.Section> all=Sections.of(now);
        assertEquals(2,all.size());assertEquals("Bread\nTomatoes",Sections.words(now,0));assertEquals("",Sections.words(now,1));
        assertEquals("=== Page 1 ===\n=== Page 2 ===",Sections.add("",""));
        String third=Sections.add(now,null);
        assertEquals(3,Sections.of(third).size());assertEquals("Page 3",Sections.of(third).get(2).name);
        assertEquals("Bread\nTomatoes",Sections.words(third,0));
    }

    @Test public void renamingKeepsTheWordsAndNamesAFirstUnnamedTab() {
        String body="Intro\n=== Ideas ===\none";
        assertEquals("Intro\n=== Plans ===\none",Sections.rename(body,1,"Plans"));
        String named=Sections.rename(body,0,"Start");
        assertEquals("=== Start ===\nIntro\n=== Ideas ===\none",named);
        assertEquals("Intro",Sections.words(named,0));assertEquals("one",Sections.words(named,1));
        assertEquals("Intro\n=== Two words ===\none",Sections.rename(body,1,"  Two \n words === "));
        assertEquals("Intro\n=== Page 2 ===\none",Sections.rename(body,1,"   "));
    }

    @Test public void removingATabTakesItsLineAndWordsAndNeverTheLastOne() {
        String body="Intro\n=== Ideas ===\none\n=== Receipts ===\nthree";
        assertEquals("=== Ideas ===\none\n=== Receipts ===\nthree",Sections.remove(body,0));
        assertEquals("Intro\n=== Receipts ===\nthree",Sections.remove(body,1));
        assertEquals("Intro\n=== Ideas ===\none",Sections.remove(body,2));
        assertEquals("=== Only ===\nwords",Sections.remove("=== Only ===\nwords",0));
        assertEquals("plain",Sections.remove("plain",0));
    }

    @Test public void readingIsStableForWindowsLineEndsAndEmptyNotes() {
        String body="=== A ===\r\nx\r\n=== B ===\r\ny";
        assertEquals(2,Sections.of(body).size());assertEquals("A",Sections.of(body).get(0).name);
        assertEquals(1,Sections.of(null).size());assertEquals("",Sections.words(null,0));
    }
}
