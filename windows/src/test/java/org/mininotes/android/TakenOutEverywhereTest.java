// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.nio.file.Files;
import org.mininotes.desktop.platform.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * A note's file put in the bin on one device is taken out of the note everywhere, whichever device added it, and comes back
 * everywhere when that device puts it back (the owner, 2026-10-18: "I deleted them all from desktop and added 2 new, on the
 * Pro all of them stayed"; decision 206). And what waits to go counts only what the note shows ("it says 4 files to sync
 * but only 3 are listed").
 */
public class TakenOutEverywhereTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @After public void close(){if(store!=null)store.close();}

    private static final String PC="MxThePc@127.0.0.1:9601",PHONE="MxThePhone@127.0.0.1:9602";

    private NoteStore.Note note() {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Trip";note.body="Tickets";store.save(note);return note;
    }
    private String keep(String note,String name) throws Exception {
        String id=UUID.randomUUID().toString();
        Files.write(store.fileFor(id).toPath(),name.getBytes());
        store.keep(new NoteStore.Held(id,note,name,"text/plain",name.length(),System.currentTimeMillis()));
        return id;
    }
    private static Enclosure.Listed up(String id){return new Enclosure.Listed(id,"ticket.txt","text/plain",10,"{\"v\":\"1\"}");}
    private static Enclosure.Listed out(String id){return new Enclosure.Listed(id,"ticket.txt","text/plain",0,Enclosure.TAKEN_OUT);}
    private boolean kept(String id){return store.file(id)!=null;}

    @Test public void inTheBinHereItIsListedAsTakenOutAndTheListGoesOutAgain() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("pc")));store.getWritableDatabase();
        NoteStore.Note note=note();String a=keep(note.id,"a.txt"),b=keep(note.id,"b.txt");
        List<String> told=new ArrayList<>();store.filesOut=told::add;
        store.putAway(NoteStore.Branch.Kind.FILE,a,true,true);
        assertEquals("its note's list goes out again",List.of(note.id),told);
        List<Enclosure.Listed> listed=store.enclosed(note.id);
        assertEquals(2,listed.size());
        for(Enclosure.Listed one:listed)assertEquals(one.id,one.id.equals(a),one.takenOut());
        for(Enclosure.Listed one:listed)if(one.takenOut())assertFalse("nothing to fetch",one.fetchable());
        store.restore(NoteStore.Branch.Kind.FILE,a);
        assertEquals(2,told.size());
        for(Enclosure.Listed one:store.enclosed(note.id))assertFalse(one.takenOut());
        assertTrue(kept(b));
    }

    @Test public void takenOutThereGoesHereWhoeverAddedItAndOnlyTheyBringItBack() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("pro")));store.getWritableDatabase();
        NoteStore.Note note=note();String mine=keep(note.id,"mine.txt");
        // Added here, taken out on the PC: gone from the note here too.
        store.listed(note.id,PC,1,List.of(out(mine)));
        assertFalse(kept(mine));
        // Another device still listing it does not bring it back.
        assertFalse(store.listed(note.id,PHONE,1,List.of(up(mine))));
        assertFalse(kept(mine));
        // The PC putting it back does: it is fetched again.
        assertTrue("to be fetched",store.listed(note.id,PC,2,List.of(up(mine))));
    }

    @Test public void oneInThisDevicesOwnBinIsLeftThere() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("own")));store.getWritableDatabase();
        NoteStore.Note note=note();String a=keep(note.id,"a.txt");
        store.putAway(NoteStore.Branch.Kind.FILE,a,true,true);
        store.listed(note.id,PC,1,List.of(out(a)));
        assertTrue("still in this device's bin, to put back",kept(a));
    }

    @Test public void aListThatSaysTakenOutMeansNothingWhereTheFileIsNot() {
        String a=UUID.randomUUID().toString();
        Enclosure.Plan plan=Enclosure.plan(List.of(out(a)),java.util.Set.of(),java.util.Set.of(),java.util.Set.of(),java.util.Set.of());
        assertTrue(plan.fetch.isEmpty()&&plan.takeOut.isEmpty()&&plan.drop.isEmpty());
    }
}
