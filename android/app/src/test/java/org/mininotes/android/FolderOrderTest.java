package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** The order of a folder (decision 336): the latest last, and each order that can be chosen. */
public class FolderOrderTest {
    private static final Map<String,FolderOrder.Facts> FACTS=Map.of(
        "b.txt",new FolderOrder.Facts(30,10,5,FolderOrder.type(FolderOrder.FILE,"b.txt")),
        "Apple",new FolderOrder.Facts(20,40,900,FolderOrder.NOTE),
        "zoo",new FolderOrder.Facts(10,20,100,FolderOrder.FOLDER),
        "a.PDF",new FolderOrder.Facts(40,30,50,FolderOrder.type(FolderOrder.FILE,"a.PDF")));
    private static final List<String> GIVEN=List.of("zoo","b.txt","a.PDF","Apple");

    private static List<String> by(String how){return FolderOrder.sorted(GIVEN,how,one->one,FACTS::get);}

    @Test public void withNothingChosenTheLatestIsLast(){assertEquals(List.of("zoo","Apple","b.txt","a.PDF"),by(""));}
    @Test public void dateAddedPutsTheLatestLast(){assertEquals(List.of("zoo","Apple","b.txt","a.PDF"),by(FolderOrder.ADDED));}
    @Test public void nameIgnoresCapitals(){assertEquals(List.of("a.PDF","Apple","b.txt","zoo"),by(FolderOrder.NAME));}
    @Test public void sizeIsBiggestFirst(){assertEquals(List.of("Apple","zoo","a.PDF","b.txt"),by(FolderOrder.SIZE));}
    @Test public void dateChangedPutsTheLatestLast(){assertEquals(List.of("b.txt","zoo","a.PDF","Apple"),by(FolderOrder.CHANGED));}
    @Test public void typeIsFoldersThenNotesThenFilesByEnding(){assertEquals(List.of("zoo","Apple","a.PDF","b.txt"),by(FolderOrder.TYPE));}

    @Test public void aThingNotFoundIsKeptAndNothingIsLost() {
        List<String> out=FolderOrder.sorted(Arrays.asList("x","zoo"),FolderOrder.ADDED,one->one,FACTS::get);
        assertEquals(List.of("x","zoo"),out);
    }

    @Test public void aFolderPutsNewThingsAfterTheLastIconNeverInAGap() {
        int n=Layout.NONE;
        Map<String,int[]> at=Layout.folder(List.of("kept","new"),Arrays.asList(Layout.cell(1,2),n),4);
        assertArrayEquals(new int[]{1,2},at.get("kept"));
        assertArrayEquals("the gaps before it stay empty",new int[]{1,3},at.get("new"));
        Map<String,int[]> wrapped=Layout.folder(List.of("kept","a","b"),Arrays.asList(Layout.cell(0,3),n,n),4);
        assertArrayEquals(new int[]{1,0},wrapped.get("a"));
        assertArrayEquals(new int[]{1,1},wrapped.get("b"));
        assertEquals("in the order given",List.of("kept","a","b"),List.copyOf(wrapped.keySet()));
    }

    @Test public void aFolderWithNothingPlacedStandsInTheOrderGiven() {
        Map<String,int[]> at=Layout.folder(List.of("a","b"),Arrays.asList(Layout.NONE,Layout.NONE),4);
        assertArrayEquals(new int[]{0,0},at.get("a"));
        assertArrayEquals(new int[]{0,1},at.get("b"));
    }

    @Test public void everyOrderHasItsWords() {
        for(String how:FolderOrder.CHOICES)assertFalse(FolderOrder.said(how).isBlank());
        assertEquals("Date added",FolderOrder.said(FolderOrder.ADDED));
    }
}
