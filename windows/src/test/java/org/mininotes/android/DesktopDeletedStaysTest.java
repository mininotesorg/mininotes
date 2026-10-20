package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Words deleted on one device do not come back from another (the owner's Live note, 2026-10-14: "I deleted what I typed but
 * it came back multiple times"). Two notebooks, no network: the phone sends a revision, the PC takes it, the phone writes on
 * and deletes a line, and the PC, which never got the deletion, sends a change of its own. Weighed against the text the two
 * last agreed on, the deletion stands; weighed against an older text, because the one that went was not kept as it went, the
 * line comes back.
 */
public class DesktopDeletedStaysTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore phone,pc;
    private static final String PHONE="MxDeletedPhoneFixture@127.0.0.1:9831",PC="MxDeletedPcFixture@127.0.0.1:9832";
    @Before public void open() throws Exception {
        phone=new NoteStore(new Context(temp.newFolder("phone")));pc=new NoteStore(new Context(temp.newFolder("pc")));
    }
    @After public void close(){phone.close();pc.close();}

    /** What the phone has said, at a revision: its note written, as typing writes it. */
    private NoteStore.Note write(NoteStore.Note n,String body,long revision){n.body=body;n.revision=revision;phone.save(n);return phone.get(n.id);}

    private String run(boolean keptAsItWent) {
        NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="Live";n.body="a\nb";n.revision=1;phone.save(n);
        String id=n.id;phone.keepVersion(id,"");
        // The phone sends revision 2; the PC takes it, and says it now says the same.
        NoteStore.Note sent=write(phone.get(id),"a\nb\nX",2);
        // Typed on while it went: the note is at revision 3 by the time what went is written down.
        write(phone.get(id),"a\nb\nX\nY",3);
        if(keptAsItWent)phone.keepVersion(id,sent.revision,"",sent.title,sent.body);else phone.keepVersion(id,"");
        pc.landed(id,PHONE,2,"Live","a\nb\nX",pc.someBook(),0,phone.history(id));
        phone.acknowledged(PC,id,2,true);
        // The phone takes X out again.
        write(phone.get(id),"a\nb\nY",4);
        // The PC, which never got that, writes Z, and sends it based on revision 2, which both had.
        NoteStore.Note there=pc.get(id);there.body="a\nb\nX\nZ";there.revision=5;pc.save(there);
        phone.landed(id,PC,5,"Live","a\nb\nX\nZ",phone.someBook(),2,pc.history(id));
        return phone.get(id).body;
    }

    @Test public void aDeletedLineStaysDeleted() {
        String now=run(true);
        assertFalse("X was deleted on the phone and stays so: "+now.replace('\n','|'),now.contains("X"));
        assertTrue("what the PC wrote is kept",now.contains("Z"));
        assertTrue("and what the phone wrote",now.contains("Y"));
    }

    /** The way it was: what went read again from the notebook after the note had moved on. The line came back. */
    @Test public void keptAfterItMovedOnTheLineCameBack() {
        assertTrue(run(false).contains("X"));
    }
}
