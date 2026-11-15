package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Choose several… where a file is held: which place's files it lists, and which are ticked (the owner, 2026-11-07). */
public class SeveralTest {
    /** A line as a place shows it: "f:" a file, "n:" a note, "c:" a folder. */
    private static boolean file(String line){return line.startsWith("f:");}
    private static String id(String line){return line;}

    @Test public void thePlaceIsHomeTempOrTheFolderShownAndNothingElsewhere() {
        assertEquals(Several.Place.HOME,Several.place(true,null));
        assertEquals(Several.Place.TEMP,Several.place(true,NoteStore.Branch.Kind.TEMP));
        assertEquals(Several.Place.FOLDER,Several.place(true,NoteStore.Branch.Kind.COLLECTION));
        assertEquals(Several.Place.NONE,Several.place(true,NoteStore.Branch.Kind.FAVOURITES));
        assertEquals(Several.Place.NONE,Several.place(true,NoteStore.Branch.Kind.BIN));
        assertEquals(Several.Place.NONE,Several.place(true,NoteStore.Branch.Kind.RECENT));
        // A note on the screen, not Home: no place to list.
        assertEquals(Several.Place.NONE,Several.place(false,null));
        assertEquals(Several.Place.NONE,Several.place(false,NoteStore.Branch.Kind.TEMP));
    }

    @Test public void onlyThePlacesFilesAreListedInItsOrderEachOnce() {
        List<String> shown=Arrays.asList("n:list","f:b.jpg","c:trip","f:a.jpg","f:b.jpg","f:c.png");
        assertEquals(Arrays.asList("f:b.jpg","f:a.jpg","f:c.png"),Several.listed(shown,SeveralTest::file,SeveralTest::id,"f:a.jpg"));
    }

    @Test public void theFileHeldIsListedFirstWhereThePlaceDoesNotShowIt() {
        List<String> shown=Arrays.asList("f:a.jpg","n:list");
        assertEquals(Arrays.asList("f:docked.pdf","f:a.jpg"),Several.listed(shown,SeveralTest::file,SeveralTest::id,"f:docked.pdf"));
        assertEquals(Collections.singletonList("f:x"),Several.listed(null,SeveralTest::file,SeveralTest::id,"f:x"));
    }

    @Test public void theBoxOpensWithTheHeldFileTickedAndSelectAllFirst() {
        List<String> files=Arrays.asList("f:a","f:b","f:c");
        boolean[] ticked=Several.ticks(files,SeveralTest::id,"f:b");
        assertArrayEquals(new boolean[]{false,false,true,false},ticked);
        // Alone in its place: it is all of them.
        assertArrayEquals(new boolean[]{true,true},Several.ticks(Collections.singletonList("f:a"),SeveralTest::id,"f:a"));
    }

    @Test public void selectAllTicksEveryFileAndAnyFileKeepsItTrue() {
        List<String> files=Arrays.asList("f:a","f:b","f:c");
        boolean[] ticked=Several.ticks(files,SeveralTest::id,"f:b");
        Several.tick(ticked,0,true);
        assertArrayEquals(new boolean[]{true,true,true,true},ticked);
        Several.tick(ticked,1,false);
        assertArrayEquals(new boolean[]{false,false,true,true},ticked);
        Several.tick(ticked,1,true);
        assertTrue(ticked[0]);
        Several.tick(ticked,0,false);
        assertArrayEquals(new boolean[]{false,false,false,false},ticked);
    }

    @Test public void whatIsChosenKeepsTheListsOrderForTheClipboard() {
        List<String> files=Arrays.asList("f:a","f:b","f:c");
        boolean[] ticked={false,true,false,true};
        assertEquals(Arrays.asList("f:a","f:c"),Several.chosen(files,ticked));
        assertTrue(Several.chosen(files,new boolean[]{false,false,false,false}).isEmpty());
    }
}
