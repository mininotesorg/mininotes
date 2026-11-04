// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/**
 * The relays on all the owner's devices (decision 316), and what Relays and Temp say where no words test looks: sentences a method
 * returns, and lists of choices kept as constants (the owner, 2026-11-02: "the text in Relais is not translated", and the times of a
 * thing's Temporary… box in English on the PC; decisions 315 and 317).
 */
public class RelaysEverywhereTest {
    @After public void english(){Say.use("en");}

    @Test public void theRelaysTravelOnAKeyCardWholeAndOnlyAsAddresses() {
        String card=Relays.toCard(false,List.of("relay.example.org:9501","[2001:db8::1]:9501"));
        byte[] wire=KeyCard.wrap(new KeyCard.Card(Relays.CARD,card,1_800_000_000_000L));
        KeyCard.Card back=KeyCard.open(wire);
        assertEquals(Relays.CARD,back.name);assertEquals(1_800_000_000_000L,back.decided);
        assertFalse(Relays.cardPublic(back.value));
        assertEquals(List.of("relay.example.org:9501","[2001:db8::1]:9501"),Relays.cardRelays(back.value));
        // None of the owner's relays, the public ones on: still a card, and it says so.
        assertTrue(Relays.cardPublic(Relays.toCard(true,List.of())));
        assertEquals(List.of(),Relays.cardRelays(Relays.toCard(true,List.of())));
        // What is not an address is left out, and never more than the most there can be.
        assertEquals(List.of("relay.example.org:9501"),Relays.cardRelays("public on\nnot an address\nrelay.example.org:9501"));
        List<String> many=new ArrayList<>();for(int at=1;at<=12;at++)many.add("relay"+at+".example.org:9501");
        assertEquals(Relays.MOST,Relays.cardRelays(Relays.toCard(true,many)).size());
    }

    @Test public void untilDeletedIsNeverReachedAndKeepsItsWords() {
        assertTrue(NoteStore.keeps(NoteStore.KEEPS));assertFalse(NoteStore.stays(NoteStore.KEEPS));
        assertTrue(NoteStore.KEEPS>System.currentTimeMillis());
        assertEquals("Stays until you delete it",NoteStore.goneIn(NoteStore.KEEPS,System.currentTimeMillis()));
    }

    @Test public void everyRelayAndTempSentenceIsSaidInFrench() throws Exception {
        List<String> said=new ArrayList<>(List.of(Relays.travelLine(true),Relays.travelLine(false),Relays.SWITCH,Relays.ONLY_MINE,Relays.HELPERS,
            Relays.SAME,Relays.SAME_MEANS,Relays.SAME_ON,Relays.SAME_OFF,Desktop.UNTIL_DELETED,
            NoteStore.goneIn(NoteStore.KEEPS,0),"Gone in, and then deleted for good, for everybody who has it on Mininotes 0.2.016 or later."));
        for(boolean on:new boolean[]{true,false})for(int own=0;own<2;own++)for(int connected=0;connected<2;connected++)
            for(boolean pc:new boolean[]{true,false})said.add(Relays.line(on,own,connected,pc));
        for(String typed:new String[]{"","a,b","a b","[2001:db8::1","[2001:db8::1]9001","[zz]:1","relay.example.org","2001:db8::1:9001",":9001",
                "not_an_address:9001","relay.example.org:0","192.168.1.2:9001","100.64.1.2:9001"}) {
            String problem=Relays.problem(typed);
            assertNotNull("a problem for "+typed,problem);
            said.add(problem);
        }
        // The phone's list is the same words as the PC's (MainActivity is not built here): see TEMP_SPANS on both.
        said.addAll(List.of(Desktop.TEMP_SPANS));
        // How what reached somebody went (decision 322), every form History says.
        java.util.List<String> own=List.of("70.34.244.170:9501");
        for(String road:new String[]{"door","home","relay","relay 70.34.244.170:9501","relay 1.2.3.4:9001","relay 1.2.3.4:9001 5.6.7.8:9001",
                "relay 70.34.244.170:9501 1.2.3.4:9001"})
            said.add(History.said(History.Kind.NOTE_REACHED,"","",road,own));
        java.util.Map<String,String> french=WordsEverywhereTest.list("fr");
        Say.use("fr");
        // Translated, or listed in French as the same words ("15 minutes").
        for(String one:said)assertTrue("not in French: "+one,!Say.t(one).equals(one)||french.containsKey(one));
    }
}
