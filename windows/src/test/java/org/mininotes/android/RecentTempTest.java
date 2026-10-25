// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.List;
import org.mininotes.desktop.platform.content.Context;

/** Temp in Recent once it is used (the owner, 2026-10-21; decision 229): a made-up notebook, no network. */
public class RecentTempTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @Before public void open() throws Exception{store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();}
    @After public void close(){if(store!=null)store.close();}

    private boolean listed(){
        for(NoteStore.Branch one:store.recent(System.currentTimeMillis()-86_400_000L))if(one.kind==NoteStore.Branch.Kind.TEMP)return true;
        return false;
    }

    @Test public void tempIsInRecentOncePutInOrOpened() throws Exception {
        assertFalse("never used: not there",listed());
        NoteStore.Note n=new NoteStore.Note();n.title="Shopping";n.body="Synthetic list";n.book=Things.HOME;store.save(n);
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,n.id,System.currentTimeMillis()+86_400_000L);
        assertTrue("something put in it",listed());
        List<NoteStore.Branch> lately=store.recent(System.currentTimeMillis()-86_400_000L);
        NoteStore.Branch line=lately.stream().filter(b->b.kind==NoteStore.Branch.Kind.TEMP).findFirst().orElseThrow();
        assertEquals(NoteStore.TEMP,line.id);assertEquals("1 thing",line.detail);
        // Taken out of Recent, as anything is; back once it is opened again.
        store.outOfRecent(NoteStore.TEMP);
        assertFalse(listed());
        Thread.sleep(5);
        store.touch(NoteStore.Branch.Kind.TEMP,NoteStore.TEMP);
        assertTrue("opened again",listed());
        // Taken off Temp is not a use of it.
        store.outOfRecent(NoteStore.TEMP);Thread.sleep(5);
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,n.id,0);
        assertFalse(listed());
    }

    @Test public void notListedOnceItsDaysAreOut() throws Exception {
        store.touch(NoteStore.Branch.Kind.TEMP,NoteStore.TEMP);
        assertTrue(store.recent(System.currentTimeMillis()-1000).stream().anyMatch(b->b.kind==NoteStore.Branch.Kind.TEMP));
        assertFalse("used before the days Recent keeps",store.recent(System.currentTimeMillis()+1000).stream().anyMatch(b->b.kind==NoteStore.Branch.Kind.TEMP));
    }
}
