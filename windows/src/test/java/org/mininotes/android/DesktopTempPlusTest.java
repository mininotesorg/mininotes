package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A note made with Temp's + is in Temp, and says so (the owner, 2026-10-14: "when I push + note in Temp, it saves it on home,
 * please make sure it saves it in the temp folder"). It is kept on Home with its time running, as Temp's things are; it must
 * not be given a place on Home's grid while Temp's things are not shown there, and its line and its way back say Temp.
 */
public class DesktopTempPlusTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @Before public void open() throws Exception{store=new NoteStore(new Context(temp.newFolder("temp-plus")));}
    @After public void close(){store.close();}

    @Test public void aNoteMadeInTempIsInTempNotOnHome() {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.body="Synthetic";note.revision=1;store.save(note);
        assertEquals("Home",Desktop.shelfLine(store,note.id));
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,note.id,System.currentTimeMillis()+3_600_000L);
        // Written in again, it stays in Temp (decision 149).
        NoteStore.Note again=store.get(note.id);again.body="Synthetic, written in";again.revision++;store.save(again);
        assertTrue(store.untilOf(NoteStore.Branch.Kind.PAGE,note.id)>0);
        assertEquals("Temp",Desktop.shelfLine(store,note.id));
        assertTrue("in Temp",store.temporary(System.currentTimeMillis()).stream().anyMatch(b->b.id.equals(note.id)));
        assertFalse("not on Home while Temp is not shown there",store.contents(Things.HOME).stream().anyMatch(b->b.id.equals(note.id)));
        // Not given a place on Home's grid either, unless Temp's things are shown on Home.
        assertFalse(Desktop.shownOnHome(NoteStore.TEMP,false));
        assertTrue(Desktop.shownOnHome(NoteStore.TEMP,true));
        assertTrue(Desktop.shownOnHome(NoteStore.FAVOURITES,false));
        assertFalse(Desktop.shownOnHome(NoteStore.BIN,true));
    }
}
