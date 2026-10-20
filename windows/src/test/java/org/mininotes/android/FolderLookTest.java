package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A folder's colour, strength and icon are what is in it wears, notes and folders at any depth, until one is given its own
 * (the owner, 2026-10-11: "the setting at a level like the folder should apply at everything in it, as long as it is not
 * overwritten in the elements"; decision 153). A real notebook, no network.
 */
public class FolderLookTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @Before public void open() throws Exception {store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();}
    @After public void close(){store.close();}

    @Test public void whatIsMadeInAFolderWearsItsLook() {
        NoteStore.Shelf folder=store.addCollectionIn(Things.HOME,"Work");
        store.paint(NoteStore.Branch.Kind.COLLECTION,folder.id,3);
        store.tone(NoteStore.Branch.Kind.COLLECTION,folder.id,2);
        store.setIcon(NoteStore.Branch.Kind.COLLECTION,folder.id,"heart");
        NoteStore.Note note=new NoteStore.Note();note.book=folder.id;note.body="In it";store.save(note);
        NoteStore.Shelf inner=store.addCollectionIn(folder.id,"Inner");
        assertEquals(3,store.colourOf(NoteStore.Branch.Kind.PAGE,note.id));
        assertEquals(2,store.toneOf(NoteStore.Branch.Kind.PAGE,note.id));
        assertEquals("heart",store.iconOf(NoteStore.Branch.Kind.PAGE,note.id));
        assertEquals(3,store.colourOf(NoteStore.Branch.Kind.COLLECTION,inner.id));
        assertEquals("heart",store.iconOf(NoteStore.Branch.Kind.COLLECTION,inner.id));
        NoteStore.Note plain=new NoteStore.Note();plain.book=Things.HOME;store.save(plain);
        assertEquals("on Home: none",Tint.NONE,store.colourOf(NoteStore.Branch.Kind.PAGE,plain.id));
    }

    @Test public void aFolderChangedIsFollowedExceptWhereItWasOverwritten() {
        NoteStore.Shelf folder=store.addCollectionIn(Things.HOME,"Work");
        NoteStore.Shelf inner=store.addCollectionIn(folder.id,"Inner");
        NoteStore.Note deep=new NoteStore.Note();deep.book=inner.id;store.save(deep);
        NoteStore.Note own=new NoteStore.Note();own.book=folder.id;store.save(own);
        store.paint(NoteStore.Branch.Kind.PAGE,own.id,5);
        store.setIcon(NoteStore.Branch.Kind.PAGE,own.id,"star");
        store.paint(NoteStore.Branch.Kind.COLLECTION,folder.id,3);
        store.setIcon(NoteStore.Branch.Kind.COLLECTION,folder.id,"heart");
        assertEquals("the folder inside follows",3,store.colourOf(NoteStore.Branch.Kind.COLLECTION,inner.id));
        assertEquals("and the note deep inside",3,store.colourOf(NoteStore.Branch.Kind.PAGE,deep.id));
        assertEquals("heart",store.iconOf(NoteStore.Branch.Kind.PAGE,deep.id));
        assertEquals("a note given its own colour keeps it",5,store.colourOf(NoteStore.Branch.Kind.PAGE,own.id));
        assertEquals("and its own icon","star",store.iconOf(NoteStore.Branch.Kind.PAGE,own.id));
        // An inner folder given its own: what is in it follows that, not the folder above.
        store.paint(NoteStore.Branch.Kind.COLLECTION,inner.id,7);
        store.paint(NoteStore.Branch.Kind.COLLECTION,folder.id,2);
        assertEquals(7,store.colourOf(NoteStore.Branch.Kind.COLLECTION,inner.id));
        assertEquals(7,store.colourOf(NoteStore.Branch.Kind.PAGE,deep.id));
    }
}
