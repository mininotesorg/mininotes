package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;
import static org.junit.Assert.*;

/** Connecting is an invitation (decision 163): kept until Accept or Ignore; ignored, it falls; scanning them forgets that. Made up, offline. */
public class InvitesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void keptUntilAcceptedOrIgnored() throws Exception {
        Context c=new Context(temp.newFolder("pad"));
        Hello.Said ben=new Hello.Said("Ben","MxBenInvite@127.0.0.1:9921",new byte[]{1,2,3},new byte[]{4,5,6},"","",false,true);
        Hello.Said cy=new Hello.Said("Cy","MxCyInvite@127.0.0.1:9922",new byte[]{7},new byte[]{8},"","",false,true);
        assertTrue("new",Invites.add(c,ben));assertFalse("said once",Invites.add(c,ben));
        Invites.add(c,cy);
        assertEquals(2,Invites.waiting(c).size());
        Hello.Said back=Invites.waiting(c).get(0);
        assertEquals("Ben",back.name);assertEquals(ben.address,back.address);assertArrayEquals(ben.signing,back.signing);assertArrayEquals(ben.agreement,back.agreement);
        // Accepted: no longer waiting. Ignored: no longer waiting, and its hello falls from now on.
        Invites.done(c,ben,false);Invites.done(c,cy,true);
        assertTrue(Invites.waiting(c).isEmpty());
        assertFalse(Invites.ignored(c,ben));assertTrue(Invites.ignored(c,cy));
        assertFalse("an ignored hello is not kept again",Invites.add(c,cy));assertTrue(Invites.waiting(c).isEmpty());
        // Scanning their code here: ignoring them is forgotten.
        Invites.unignore(c,cy.signing);assertFalse(Invites.ignored(c,cy));
    }
}
