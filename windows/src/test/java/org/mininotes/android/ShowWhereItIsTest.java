// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.UUID;
import org.mininotes.desktop.platform.content.Context;

/**
 * Where Show where it is takes a thing (decision 281). Something new only in Temp, or a file shared with you on its own, went to
 * Home, where neither is drawn, and nothing was seen to happen (the owner, 2026-10-29).
 */
public class ShowWhereItIsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @After public void close(){if(store!=null)store.close();}
    @Before public void open(){store=new NoteStore(new Context(temp.getRoot()));store.getWritableDatabase();}

    private String note(String book){NoteStore.Note n=new NoteStore.Note();n.book=book;n.title="Synthetic";n.body="words";store.save(n);return n.id;}
    private String file(String in,NoteStore.Branch.Kind holder) {
        String id=UUID.randomUUID().toString();
        store.keep(new NoteStore.Held(id,in,"synthetic.md","text/markdown",10,1_760_000_000_000L,holder));
        return id;
    }
    private NoteStore.Shown at(NoteStore.Branch.Kind kind,String id){return store.shownAt(kind,id);}

    @Test public void aNoteOnHomeIsShownOnHome() {
        assertEquals(new NoteStore.Shown(NoteStore.Branch.Kind.LIBRARY,Things.HOME),at(NoteStore.Branch.Kind.PAGE,note(Things.HOME)));
    }

    @Test public void aNoteInAFolderIsShownInItsFolder() {
        String folder=store.addCollection("Synthetic folder").id;
        assertEquals(new NoteStore.Shown(NoteStore.Branch.Kind.COLLECTION,folder),at(NoteStore.Branch.Kind.PAGE,note(folder)));
    }

    @Test public void aFileKeptInANoteIsShownInThatNote() {
        String in=note(Things.HOME);
        assertEquals(new NoteStore.Shown(NoteStore.Branch.Kind.PAGE,in),at(NoteStore.Branch.Kind.FILE,file(in,NoteStore.Branch.Kind.PAGE)));
    }

    @Test public void whatIsOnlyInTempIsShownInTemp() {
        String n=note(Things.HOME);store.makeTemporary(NoteStore.Branch.Kind.PAGE,n,System.currentTimeMillis()+86_400_000L);
        assertEquals(new NoteStore.Shown(NoteStore.Branch.Kind.TEMP,NoteStore.TEMP),at(NoteStore.Branch.Kind.PAGE,n));
        String f=file(Things.HOME,NoteStore.Branch.Kind.COLLECTION);store.makeTemporary(NoteStore.Branch.Kind.FILE,f,System.currentTimeMillis()+86_400_000L);
        assertEquals(new NoteStore.Shown(NoteStore.Branch.Kind.TEMP,NoteStore.TEMP),at(NoteStore.Branch.Kind.FILE,f));
        // A note in Temp that sits in a folder is drawn there: the nearer place wins.
        String folder=store.addCollection("Synthetic folder").id;String m=note(folder);
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,m,System.currentTimeMillis()+86_400_000L);
        assertEquals(new NoteStore.Shown(NoteStore.Branch.Kind.COLLECTION,folder),at(NoteStore.Branch.Kind.PAGE,m));
    }

    @Test public void aFileSharedWithYouOnItsOwnIsShownInSharedWithMe() {
        assertEquals(new NoteStore.Shown(NoteStore.Branch.Kind.SHARED,NoteStore.SHARED),
            at(NoteStore.Branch.Kind.FILE,file(NoteStore.SHARED,NoteStore.Branch.Kind.COLLECTION)));
    }
}
