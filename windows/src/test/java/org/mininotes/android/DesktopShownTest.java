package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Things put anywhere on this device while they stay where their sharing has them, and a look of this device's own
 * (docs/HOME.md, decisions 65 and 66; the owner, 2026-10-03). A synthetic notebook, no window and no network.
 */
public class DesktopShownTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    private String kitchen,recipes,soup;

    /** Kitchen › Recipes, with Soup in Kitchen. */
    @Before public void open() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();
        kitchen=store.addCollection("Kitchen").id;recipes=store.addBook(kitchen,"Recipes").id;
        NoteStore.Note one=new NoteStore.Note();one.book=kitchen;one.title="Soup";one.body="Synthetic soup";store.save(one);soup=one.id;
    }
    @After public void close(){store.close();}

    private static Set<String> ids(List<NoteStore.Branch> lines){Set<String> out=new HashSet<>();for(NoteStore.Branch one:lines)out.add(one.id);return out;}

    @Test public void aThingShownElsewhereIsDrawnThereAndStaysWhereItIs() {
        List<String> path=store.pathOf(soup);
        assertEquals("",store.showIn(NoteStore.Branch.Kind.PAGE,soup,Things.HOME));
        assertTrue("drawn on Home",ids(store.contents(Things.HOME)).contains(soup));
        assertFalse("not in Kitchen any more, as seen",ids(store.contents(kitchen)).contains(soup));
        // Where it really is, which is what sharing reads, is as it was.
        assertEquals(path,store.pathOf(soup));
        assertEquals(kitchen,store.get(soup).book);
        // Shown in Recipes instead, then back where it is: an empty place again.
        assertEquals(Things.HOME,store.showIn(NoteStore.Branch.Kind.PAGE,soup,recipes));
        assertTrue(ids(store.contents(recipes)).contains(soup));assertFalse(ids(store.contents(Things.HOME)).contains(soup));
        store.showIn(NoteStore.Branch.Kind.PAGE,soup,kitchen);
        assertEquals("",store.shownIn(NoteStore.Branch.Kind.PAGE,soup));
        assertTrue(ids(store.contents(kitchen)).contains(soup));
    }

    @Test public void aRealMoveForgetsWhereItWasShown() {
        store.showIn(NoteStore.Branch.Kind.PAGE,soup,Things.HOME);
        store.moveInto(NoteStore.Branch.Kind.PAGE,soup,recipes);
        assertEquals("",store.shownIn(NoteStore.Branch.Kind.PAGE,soup));
        assertTrue(ids(store.contents(recipes)).contains(soup));assertFalse(ids(store.contents(Things.HOME)).contains(soup));
    }

    @Test public void shownInSomethingPutAwayItComesBackToWhereItIs() {
        String box=store.addCollection("Box").id;
        store.showIn(NoteStore.Branch.Kind.PAGE,soup,box);
        assertTrue(ids(store.contents(box)).contains(soup));
        store.putAway(NoteStore.Branch.Kind.COLLECTION,box,true,true);
        assertTrue("back in Kitchen rather than nowhere",ids(store.contents(kitchen)).contains(soup));
    }

    @Test public void aCollectionIsNeverShownInsideItself() {
        try{store.showIn(NoteStore.Branch.Kind.COLLECTION,kitchen,recipes);fail("shown inside itself");}
        catch(IllegalArgumentException expected){}
        // Nor inside one that is shown inside it.
        String shelf=store.addCollection("Shelf").id;
        store.showIn(NoteStore.Branch.Kind.COLLECTION,shelf,kitchen);
        assertTrue(ids(store.contents(kitchen)).contains(shelf));
        try{store.showIn(NoteStore.Branch.Kind.COLLECTION,kitchen,shelf);fail("shown inside what is shown inside it");}
        catch(IllegalArgumentException expected){}
    }

    @Test public void aLookOfThisDevicesOwnIsDrawnAndNeverSent() {
        store.setIcon(NoteStore.Branch.Kind.PAGE,soup,"carrot");
        long revision=store.get(soup).revision;
        store.setOwnIcon(NoteStore.Branch.Kind.PAGE,soup,"apple");
        assertEquals("what travels is everybody's",  "carrot",store.iconOf(NoteStore.Branch.Kind.PAGE,soup));
        assertEquals("apple",store.wornIcon(NoteStore.Branch.Kind.PAGE,soup));
        assertEquals("nothing owed for it",revision,store.get(soup).revision);
        List<NoteStore.Branch> lines=store.contents(kitchen);store.dress(lines);
        for(NoteStore.Branch one:lines)if(one.id.equals(soup))assertEquals("apple",one.icon);
        store.dropOwnLook(NoteStore.Branch.Kind.PAGE,soup);
        assertFalse(store.hasOwnLook(NoteStore.Branch.Kind.PAGE,soup));
        assertEquals("carrot",store.wornIcon(NoteStore.Branch.Kind.PAGE,soup));
    }
}
