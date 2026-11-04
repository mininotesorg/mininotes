package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Which tab each of a note's files goes under, how they are grouped, and how the tab travels with the list (decision 299). */
public class TabFilesTest {
    private static final String NOTE="Intro\n=== Ideas ===\none\n=== Receipts ===\nthree";
    private static final List<Sections.Section> TABS=Sections.of(NOTE);

    @Test public void aFileGoesUnderItsTabByNameAndOtherwiseUnderTheFirst() {
        assertEquals(0,TabFiles.under(null,TABS));
        assertEquals(0,TabFiles.under("",TABS));
        assertEquals(1,TabFiles.under("Ideas",TABS));
        assertEquals(2,TabFiles.under("Receipts",TABS));
        // A tab renamed or removed elsewhere: under the first tab, never lost.
        assertEquals(0,TabFiles.under("Gone",TABS));
        assertEquals("",TabFiles.keyOf(TABS,0));assertEquals("Ideas",TabFiles.keyOf(TABS,1));assertEquals("",TabFiles.keyOf(TABS,9));
    }

    @Test public void filesAreGroupedInTheOrderOfTheTabsAndKeepTheirOwnOrderWithin() {
        Map<String,String> tab=new HashMap<>();
        tab.put("a","Receipts");tab.put("b","");tab.put("c","Ideas");tab.put("d","Receipts");tab.put("e","Gone");
        List<String> files=Arrays.asList("a","b","c","d","e");
        List<TabFiles.Group<String>> groups=TabFiles.groups(NOTE,files,tab::get);
        assertEquals(3,groups.size());
        assertEquals(0,groups.get(0).tab);assertEquals("Page 1",groups.get(0).called);assertEquals(Arrays.asList("b","e"),groups.get(0).files);
        assertEquals("Ideas",groups.get(1).called);assertEquals(Arrays.asList("c"),groups.get(1).files);
        assertEquals("Receipts",groups.get(2).called);assertEquals(Arrays.asList("a","d"),groups.get(2).files);
        assertEquals(Arrays.asList("b","e","c","a","d"),TabFiles.ordered(groups));
        assertTrue(TabFiles.headed(NOTE));
        // One tab: one group, no heading.
        assertFalse(TabFiles.headed("Just words"));
        assertEquals(1,TabFiles.groups("Just words",files,tab::get).size());
        assertTrue(TabFiles.groups(NOTE,new ArrayList<String>(),tab::get).isEmpty());
    }

    @Test public void theListOpensOnTheOpenTabOrTheNearestAfterIt() {
        Map<String,String> tab=new HashMap<>();tab.put("a","");tab.put("c","Receipts");
        List<TabFiles.Group<String>> groups=TabFiles.groups(NOTE,Arrays.asList("a","c"),tab::get);
        assertEquals(0,TabFiles.opensAt(groups,0));
        assertEquals(1,TabFiles.opensAt(groups,1));   // Ideas has none: Receipts', the next
        assertEquals(1,TabFiles.opensAt(groups,2));
        assertEquals(1,TabFiles.opensAt(groups,7));   // past the end: the last
        assertEquals(-1,TabFiles.opensAt(new ArrayList<TabFiles.Group<String>>(),0));
    }

    @Test public void aRenamedTabTakesItsFiles() {
        assertEquals(Map.of("Ideas","Thoughts"),TabFiles.carried(NOTE,Sections.rename(NOTE,1,"Thoughts")));
        // The first tab, with no name of its own, given one.
        assertEquals(Map.of("","Notes"),TabFiles.carried(NOTE,Sections.rename(NOTE,0,"Notes")));
        // A named first tab renamed: its files by name follow; the old ones kept as empty are still under the first.
        String named="=== A ===\nx\n=== B ===\ny";
        assertEquals(Map.of("A","Z"),TabFiles.carried(named,Sections.rename(named,0,"Z")));
    }

    @Test public void carryingTabsKeepsFilesWithTheTabTheyWereShownUnder() {
        // A first tab with no name, carried: it becomes Page 1, and what was kept as empty with it.
        String two="Intro\n=== Ideas ===\none";
        assertEquals(Map.of("","Page 1"),TabFiles.carried(two,Sections.move(two,0,1)));
        // A named first tab carried away from the front: what was kept as empty stays with it.
        String named="=== A ===\nx\n=== B ===\ny";
        assertEquals(Map.of("","A"),TabFiles.carried(named,Sections.move(named,0,1)));
        // Two tabs after the first swapped: nothing moves.
        assertTrue(TabFiles.carried(NOTE,Sections.move(NOTE,1,2)).isEmpty());
    }

    @Test public void addingOrRemovingATabMovesNothingButAPlainNoteGivenTabs() {
        assertEquals(Map.of("","Page 1"),TabFiles.carried("Bread",Sections.add("Bread",null)));
        assertTrue(TabFiles.carried(NOTE,Sections.add(NOTE,"More")).isEmpty());
        assertTrue(TabFiles.carried(NOTE,Sections.remove(NOTE,1)).isEmpty());
        assertTrue(TabFiles.carried(NOTE,Sections.put(NOTE,1,"other words")).isEmpty());
    }

    // ---- the list's tabs as they travel ---------------------------------------------------------------------------------

    private static final String A="0f8c1d52-5b0e-4a51-9f6a-0d3c2b1a0e01",B="0f8c1d52-5b0e-4a51-9f6a-0d3c2b1a0e02";

    private static Parcel.Sent sent(List<Enclosure.Listed> files) {
        return new Parcel.Sent("c","Home","b","Lists","Groceries",NOTE,true,List.of(),"PAGE","n-1",true,4L,true,files,123L,
            List.of(),List.of(new Parcel.Step("c","Home","",0,0L)),"",null);
    }

    @Test public void theTabOfEachFileTravelsWithTheList() throws Exception {
        List<Enclosure.Listed> files=Arrays.asList(new Enclosure.Listed(A,"a.pdf","application/pdf",5,"m").under("Ideas",77L),
            new Enclosure.Listed(B,"b.png","image/png",6,""));
        Parcel.Sent in=Parcel.open(Parcel.wrap(sent(files)));
        assertNotNull(in);
        assertEquals("Ideas",in.files.get(0).tab);assertEquals(77L,in.files.get(0).tabAt);
        // Not decided: nothing said, as from a build before.
        assertNull(in.files.get(1).tab);assertEquals(0L,in.files.get(1).tabAt);
        // A file named but not said where survives being made lighter with its tab.
        assertEquals("Ideas",files.get(0).unsaid().tab);
    }

    @Test public void aListWithNoTabIsSaidByteForByteAsBefore() throws Exception {
        List<Enclosure.Listed> plain=Arrays.asList(new Enclosure.Listed(A,"a.pdf","application/pdf",5,"m"));
        List<Enclosure.Listed> undecided=Arrays.asList(new Enclosure.Listed(A,"a.pdf","application/pdf",5,"m","",0L));
        assertArrayEquals(Parcel.wrap(sent(plain)),Parcel.wrap(sent(undecided)));
        assertEquals(0,Parcel.tabs(sent(plain)).length);
    }

    @Test public void aBuildFromBeforeReadsEverythingUpToTheTabsAndStopsThere() throws Exception {
        List<Enclosure.Listed> plain=Arrays.asList(new Enclosure.Listed(A,"a.pdf","application/pdf",5,"m"));
        List<Enclosure.Listed> tabbed=Arrays.asList(plain.get(0).under("Receipts",9L));
        byte[] before=Parcel.wrap(sent(plain)),now=Parcel.wrap(sent(tabbed));
        // The same bytes, then the mark: a reader that does not know it stops where it always stopped.
        assertEquals(before.length+Parcel.tabs(sent(tabbed)).length,now.length);
        assertArrayEquals(before,Arrays.copyOf(now,before.length));
        assertEquals(Parcel.TABS_MARK,java.nio.ByteBuffer.wrap(now,before.length,4).getInt());
    }

    @Test public void tabsThatDoNotMatchTheListAreLeftUnsaid() throws Exception {
        List<Enclosure.Listed> tabbed=Arrays.asList(new Enclosure.Listed(A,"a.pdf","application/pdf",5,"m").under("Ideas",3L));
        byte[] whole=Parcel.wrap(sent(tabbed));
        // The count says two files where the list has one: no tab taken, the parcel still read.
        byte[] wrong=whole.clone();
        int at=whole.length-Parcel.tabs(sent(tabbed)).length+4;
        java.nio.ByteBuffer.wrap(wrong,at,4).putInt(2);
        Parcel.Sent in=Parcel.open(wrong);
        assertNotNull(in);assertNull(in.files.get(0).tab);
        // Cut short in the middle: the same.
        Parcel.Sent cut=Parcel.open(Arrays.copyOf(whole,whole.length-3));
        assertNotNull(cut);assertNull(cut.files.get(0).tab);
    }
}
