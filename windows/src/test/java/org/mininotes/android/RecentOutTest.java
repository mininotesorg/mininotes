package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import org.mininotes.desktop.platform.content.Context;

/** Taken out of Recent, and back in it once opened again (the owner, 2026-10-11; decision 154). A real notebook, no network. */
public class RecentOutTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @Before public void open() throws Exception {store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();}
    @After public void close(){store.close();}

    private boolean listed(String id){for(NoteStore.Branch one:store.recent(0))if(one.id.equals(id))return true;return false;}

    @Test public void takenOutAndBackOnceOpened() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.body="Lately";store.save(note);
        NoteStore.Shelf folder=store.addCollectionIn(Things.HOME,"Lately folder");
        store.touch(NoteStore.Branch.Kind.PAGE,note.id);store.touch(NoteStore.Branch.Kind.COLLECTION,folder.id);
        assertTrue(listed(note.id));assertTrue(listed(folder.id));
        Thread.sleep(5);
        store.outOfRecent(note.id);store.outOfRecent(folder.id);
        assertFalse("the note is out",listed(note.id));
        assertFalse("and the folder",listed(folder.id));
        Thread.sleep(5);
        store.touch(NoteStore.Branch.Kind.PAGE,note.id);
        assertTrue("opened again: back",listed(note.id));
        assertFalse("the folder not opened: still out",listed(folder.id));
    }
}
