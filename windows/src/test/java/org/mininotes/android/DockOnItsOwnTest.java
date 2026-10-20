package org.mininotes.android;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;

/**
 * The dock on its own (the owner, 2026-10-18; decision 190): Add to the dock apart from Add to favourites, a new favourite
 * in the dock too, a favourite taken away still in it, as many as were put there, and what this PC kept out of its dock
 * before taken out of the notebook's own once. Synthetic notes only.
 */
public class DockOnItsOwnTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    private static String note(NoteStore store,String title) {
        NoteStore.Note n=new NoteStore.Note();n.book=store.someBook();n.title=title;n.body="Synthetic "+title;n.revision=1;store.save(n);return n.id;
    }
    private static List<String> names(List<NoteStore.Branch> lines){List<String> out=new ArrayList<>();for(NoteStore.Branch one:lines)out.add(one.name);return out;}

    @Test public void theDockIsItsOwnAndHoldsAsManyAsArePutThere() throws Exception {
        Context context=new Context(temp.newFolder("pc"));
        try(NoteStore store=new NoteStore(context)) {
            String a=note(store,"Apples"),b=note(store,"Bread");
            String c=store.addCollection("Cellar").id;
            final NoteStore.Branch.Kind PAGE=NoteStore.Branch.Kind.PAGE,FOLDER=NoteStore.Branch.Kind.COLLECTION;
            assertTrue(store.dock().isEmpty());
            // Made a favourite: in the dock too.
            store.keepToHand(PAGE,a,true);
            assertEquals(List.of("Apples"),names(store.dock()));assertTrue(store.inDock(PAGE,a));
            // Added to the dock: not made a favourite.
            store.toDockEnd(PAGE,b);
            assertEquals(List.of("Apples","Bread"),names(store.dock()));assertFalse(store.favourite(PAGE,b));
            // Written in: still in the dock, where it was (a note's row is written whole).
            NoteStore.Note bread=store.get(b);bread.body+="\nmore";bread.revision++;store.save(bread);
            assertEquals(List.of("Apples","Bread"),names(store.dock()));
            // No longer a favourite: still in the dock.
            store.keepToHand(PAGE,a,false);
            assertEquals(List.of("Apples","Bread"),names(store.dock()));assertFalse(store.favourite(PAGE,a));
            // A folder put first; taken out of the dock, a favourite stays one.
            store.keepToHand(FOLDER,c,true);store.toDock(FOLDER,c,1);
            assertEquals(List.of("Cellar","Apples","Bread"),names(store.dock()));
            store.outOfDock(FOLDER,c);
            assertEquals(List.of("Apples","Bread"),names(store.dock()));assertTrue(store.favourite(FOLDER,c));
            assertEquals("the favourite not in the dock",List.of("Cellar"),names(store.favouritesBeyondDock()));
            // As many as are put there, far past the phone's five.
            for(int i=0;i<12;i++)store.toDockEnd(PAGE,note(store,"Extra "+i));
            assertEquals(14,store.dock().size());assertEquals("Extra 11",store.dock().get(13).name);
            // Moved within it: the same thing once, at its new place.
            store.toDock(PAGE,b,1);
            assertEquals("Bread",store.dock().get(0).name);assertEquals(14,store.dock().size());
            // What this PC had kept out of its dock before: taken out of the notebook's own, once.
            context.getSharedPreferences("settings",0).edit().putStringSet(DesktopHome.KEPT_OUT,Set.of(a)).apply();
            DesktopHome.keptOutOnce(context,store);
            assertFalse(store.inDock(PAGE,a));
            assertTrue(context.getSharedPreferences("settings",0).getStringSet(DesktopHome.KEPT_OUT,Set.of()).isEmpty());
        }
    }
}
