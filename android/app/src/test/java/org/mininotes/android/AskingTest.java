package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * Who has not accepted yet what was shared with them for the first time (the connect review, 2026-10-14): the sender's tick
 * said "everybody has it" while it still waited, hidden, in their Shared with me.
 */
public class AskingTest {
    @Test public void waitingThenAcceptedAndALateWaitChangesNothing() {
        Set<String> kept=new HashSet<>();
        kept=Asking.heard(kept,"Mx-ana","note-1",false);
        assertEquals(Set.of(Asking.key("Mx-ana","note-1")),Asking.waiting(kept));
        assertNull("said twice, the same",Asking.heard(kept,"Mx-ana","note-1",false));
        kept=Asking.heard(kept,"Mx-ana","note-1",true);
        assertTrue(Asking.waiting(kept).isEmpty());
        // A "waits" that was slower than the "accepted" it came before: accepted stands.
        assertNull(Asking.heard(kept,"Mx-ana","note-1",false));
        assertTrue(Asking.waiting(kept).isEmpty());
    }

    @Test public void eachDeviceAndEachThingOnItsOwn() {
        Set<String> kept=new HashSet<>();
        kept=Asking.heard(kept,"Mx-ana","note-1",false);
        kept=Asking.heard(kept,"Mx-ben","note-1",false);
        kept=Asking.heard(kept,"Mx-ana","note-2",false);
        kept=Asking.heard(kept,"Mx-ana","note-1",true);
        assertEquals(Set.of(Asking.key("Mx-ben","note-1"),Asking.key("Mx-ana","note-2")),Asking.waiting(kept));
        // An id that only begins like another is another.
        assertNull(Asking.forget(kept,"Mx-ana","note"));
        kept=Asking.forget(kept,"Mx-ana","note-2");
        assertEquals(Set.of(Asking.key("Mx-ben","note-1")),Asking.waiting(kept));
    }

    @Test public void nothingWithoutAWhoOrAWhat() {
        assertNull(Asking.heard(new HashSet<>(),"","note-1",false));
        assertNull(Asking.heard(new HashSet<>(),"Mx-ana","",true));
    }

    @Test public void theReceiversLineReadsBack() {
        assertArrayEquals(new String[]{"Mx-ana","PAGE","note-1"},Asking.told(Asking.told("Mx-ana","PAGE","note-1")));
        assertNull(Asking.told("Mx-ana\tPAGE"));
        assertNull(Asking.told((String)null));
    }

    @Test public void theTwoWordsAreAnswersOfTheirOwnThatABuildFromBeforeLetsBy() {
        assertEquals(36,Receipt.WAITS);assertEquals(37,Receipt.ACCEPTED);
        assertEquals(Receipt.WAITS,Receipt.open(Receipt.wrap(Receipt.WAITS)));
        assertEquals(Receipt.ACCEPTED,Receipt.open(Receipt.wrap(Receipt.ACCEPTED)));
        // Five bytes, as every answer: never a refusal, never taken for a note.
        assertNull(Receipt.refused(Receipt.wrap(Receipt.ACCEPTED)));
    }

    @Test public void aDeviceWhereItWaitsToBeAcceptedIsSentNotEverybodyHasIt() {
        Waits.Where waits=new Waits.Where(0,0,0,true,false,false,0,1);
        assertEquals(SyncMark.SENT,waits.mark());
        assertEquals("Waiting for Ana to accept it.",Waits.person("Ana",waits,0));
        assertEquals("Waiting for Ana to accept it.",Waits.said(java.util.List.of(new Waits.Device("Ana",waits,0)),1));
        // Something still to go comes first: the writing is not with them yet, accepted or not.
        Waits.Where owed=new Waits.Where(1,0,0,false,false,false,0,1);
        assertEquals(SyncMark.WAITING,owed.mark());
        assertEquals(SyncMark.WAITING.person(),Waits.person("Ana",owed,0));
        // Added up over a folder's notes, one waiting is enough.
        Waits.Where both=new Waits.Where(0,0,0,true,false).and(waits);
        assertEquals(1,both.asking());assertEquals(SyncMark.SENT,both.mark());
        // And without it, as before: everybody has it.
        assertEquals(SyncMark.GONE,new Waits.Where(0,0,0,true,false).mark());
    }
}
