package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * Whose things are accepted without asking (the owner, 2026-10-28: "we have to make it that we dont need to accept files
 * shared with my contact or at least it should be optional"; decision 269): the list, the blocked rule, when the offer is
 * made after Accept, a sending from them fetched at once, and the words.
 */
public class WithoutAskingTest {
    @Test public void offUnlessTurnedOnAndEachAddressOnItsOwn() {
        Set<String> kept=new HashSet<>();
        assertFalse("off by default",WithoutAsking.takes(kept,"Mx-ana",false));
        kept=WithoutAsking.set(kept,"Mx-ana",true);
        assertTrue(WithoutAsking.takes(kept,"Mx-ana",false));
        assertFalse("another person is still asked",WithoutAsking.takes(kept,"Mx-ben",false));
        assertNull("turned on twice, nothing changes",WithoutAsking.set(kept,"Mx-ana",true));
        kept=WithoutAsking.set(kept,"Mx-ana",false);
        assertFalse(WithoutAsking.takes(kept,"Mx-ana",false));
        assertNull("off twice, nothing changes",WithoutAsking.set(kept,"Mx-ana",false));
        assertNull("no address, nothing",WithoutAsking.set(kept,"",true));
        assertNull(WithoutAsking.set(null,null,true));
        assertFalse(WithoutAsking.takes(null,"Mx-ana",false));
    }

    @Test public void aBlockedSenderStaysBlocked() {
        Set<String> kept=WithoutAsking.set(new HashSet<>(),"Mx-ana",true);
        assertFalse(WithoutAsking.takes(kept,"Mx-ana",true));
        assertFalse("and is not offered it",WithoutAsking.offer(new HashSet<>(),new HashSet<>(),"Mx-ana",true,false,true));
    }

    @Test public void offeredAfterAcceptOnlyToSomebodyPairedNotMineNotOnAndNotToldKeepAsking() {
        Set<String> none=new HashSet<>();
        assertTrue(WithoutAsking.offer(none,none,"Mx-ana",true,false,false));
        assertFalse("not paired",WithoutAsking.offer(none,none,"Mx-ana",false,false,false));
        assertFalse("one of my devices never asks",WithoutAsking.offer(none,none,"Mx-tablet",true,true,false));
        assertFalse("already on",WithoutAsking.offer(Set.of("Mx-ana"),none,"Mx-ana",true,false,false));
        assertFalse("Keep asking was said",WithoutAsking.offer(none,Set.of("Mx-ana"),"Mx-ana",true,false,false));
        assertTrue("for her only",WithoutAsking.offer(none,Set.of("Mx-ana"),"Mx-ben",true,false,false));
        assertFalse(WithoutAsking.offer(none,none,"",true,false,false));
    }

    @Test public void aSendingFromThemIsFetchedAtOnceAsFromMyOwnDevices() {
        assertEquals(Drop.ASKING,Drop.arriving(true,false,false));
        assertEquals(Drop.FETCHING,Drop.arriving(true,false,true));
        assertEquals(Drop.FETCHING,Drop.arriving(true,true,false));
        assertEquals("never from a device not paired here",0,Drop.arriving(false,false,true));
        // The old two-argument form is unchanged.
        assertEquals(Drop.ASKING,Drop.arriving(true,false));
        assertEquals(Drop.FETCHING,Drop.arriving(true,true));
    }

    @Test public void theWordsAreOneTemplateEachWithTheName() {
        assertEquals("Accept everything from Parisa without asking?",WithoutAsking.question("Parisa"));
        assertEquals("Files, notes and folders from Parisa come in at once.",WithoutAsking.under("Parisa"));
        assertEquals("Everything from Parisa is accepted without asking.",WithoutAsking.done("Parisa",true));
        assertEquals("Everything from Parisa waits for you to accept it.",WithoutAsking.done("Parisa",false));
        for(String said:new String[]{WithoutAsking.SWITCH,WithoutAsking.YES,WithoutAsking.KEEP,WithoutAsking.WHERE,
                WithoutAsking.question("x"),WithoutAsking.under("x"),WithoutAsking.done("x",true),WithoutAsking.done("x",false)}) {
            assertFalse("no long dash: "+said,said.contains("—")||said.contains(" - "));
        }
    }
}
