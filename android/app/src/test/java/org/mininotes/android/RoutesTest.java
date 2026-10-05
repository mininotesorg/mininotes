// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import com.eurobuddha.maxima.core.media.MediaManifest;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Which road things last went by to each device, and what People and devices says about it; and the near-first decision for
 * a file (decision 96, the owner: "tell me if I'm directly connected to a device ... and if it is used at the moment or if a
 * relay is used").
 */
public class RoutesTest {
    private static final String KEY="0xabc123", OTHER="0xdef456";

    @After public void clean(){Routes.forgetAll();Routes.keep=null;Routes.nearNow=any->false;}

    @Test public void theLastRoadIsKeptByKeyWhicheverWayTheKeyIsWritten() {
        assertNull(Routes.last(KEY));
        Routes.went(KEY,Routes.Way.RELAY,1_000);
        Routes.went("0XABC123 ",Routes.Way.DOOR,2_000);
        Routes.Last last=Routes.last("0xABC123");
        assertEquals(Routes.Way.DOOR,last.way());
        assertEquals(2_000,last.at());
        assertNull("another device has its own",Routes.last(OTHER));
        Routes.went("",Routes.Way.DOOR,3_000);Routes.went(null,Routes.Way.DOOR,3_000);Routes.went(OTHER,null,3_000);
        assertNull(Routes.last(OTHER));
    }

    @Test public void keptAndTakenBackAfterARestart() {
        Routes.went(KEY,Routes.Way.HOME,5_000);Routes.went(OTHER,Routes.Way.RELAY,6_000);
        String kept=Routes.kept();
        Routes.forgetAll();
        assertNull(Routes.last(KEY));
        Routes.keptWas(kept+"not a line\n0x1 SOMEWHERE 5\n0x2 DOOR notatime\n");
        assertEquals(new Routes.Last(Routes.Way.HOME,5_000),Routes.last(KEY));
        assertEquals(new Routes.Last(Routes.Way.RELAY,6_000),Routes.last(OTHER));
        assertNull("a line that does not read is left out",Routes.last("0x1"));
        assertNull(Routes.last("0x2"));
        Routes.keptWas(null);
        assertNull(Routes.last(KEY));
    }

    @Test public void keptWhenTheRoadChangesAndOtherwiseOnlyNowAndThen() {
        List<String> writes=new ArrayList<>();
        Routes.keep=writes::add;
        Routes.went(KEY,Routes.Way.DOOR,100_000);
        assertEquals(1,writes.size());
        Routes.went(KEY,Routes.Way.DOOR,110_000);
        assertEquals("the same road a moment later is not written again",1,writes.size());
        Routes.went(KEY,Routes.Way.RELAY,120_000);
        assertEquals("another road is",2,writes.size());
        Routes.went(KEY,Routes.Way.RELAY,120_000+Routes.KEEP_EVERY);
        assertEquals("and a minute on, the time is",3,writes.size());
        assertTrue(writes.get(2).contains("RELAY 180000"));
    }

    @Test public void neverMoreDevicesThanItHolds() {
        for(int one=0;one<Routes.MOST+5;one++)Routes.went("0x"+Integer.toHexString(one+4096),Routes.Way.DOOR,one);
        assertTrue(Routes.kept().split("\n").length<=Routes.MOST);
    }

    @Test public void whatIsSaidUnderADevice() {
        assertEquals("Not connected yet",Routes.state(false,true,true,true,true));
        assertEquals("On this Wi-Fi · direct",Routes.state(true,true,true,false,true));
        assertEquals("Direct door known",Routes.state(true,false,true,false,true));
        assertEquals("Through relays",Routes.state(true,false,false,true,true));
        assertEquals("Waits until you are on the same Wi-Fi",Routes.state(true,false,false,true,false));
        assertEquals("Not heard lately",Routes.state(true,false,false,false,true));
        long now=10L*24*3600_000;
        assertEquals("",Routes.lastSent(null,now));
        assertEquals("Last sent: direct, just now",Routes.lastSent(new Routes.Last(Routes.Way.DOOR,now-30_000),now));
        assertEquals("Last sent: through a relay, 2 min ago",Routes.lastSent(new Routes.Last(Routes.Way.RELAY,now-150_000),now));
        assertEquals("Last sent: left at your PC, 3 h ago",Routes.lastSent(new Routes.Last(Routes.Way.HOME,now-3*3600_000-1),now));
        // One line across the card (decision 97).
        assertEquals("On this Wi-Fi · direct · last sent: direct, 2 min ago",Routes.oneLine(new String[]{Routes.NEAR,"Last sent: direct, 2 min ago"}));
        assertEquals("Through relays",Routes.oneLine(new String[]{Routes.RELAYS,""}));
        assertEquals("",Routes.oneLine(null));
        assertEquals("yesterday",Routes.ago(30L*3600_000));
        assertEquals("4 days ago",Routes.ago(4L*24*3600_000+5));
        assertEquals("a clock put back says just now","just now",Routes.ago(-5_000));
    }

    @Test public void whatIsSaidAboutThisDevice() {
        assertEquals("Direct connections: open on port 9601",Routes.doorLine(true,9601,0));
        assertEquals("Direct connections: open on port 9602 · 1 device on this Wi-Fi",Routes.doorLine(true,9602,1));
        assertEquals("Direct connections: open on port 9601 · 3 devices on this Wi-Fi",Routes.doorLine(true,9601,3));
        assertEquals("Direct connections: closed, this device could not open its door",Routes.doorLine(true,-1,2));
        assertEquals("Direct connections: not connected yet",Routes.doorLine(false,9601,0));
        assertTrue(Routes.travelLine(true).startsWith("Notes travel with helpers"));
        assertTrue(Routes.travelLine(false).startsWith("Notes travel only between my devices"));
        // No long dashes and no spaced hyphens in what the owner reads.
        for(String said:new String[]{Routes.travelLine(true),Routes.travelLine(false),Routes.doorLine(true,9601,2),Routes.NEAR,
                Routes.DOOR_KNOWN,Routes.RELAYS,Routes.WAITS,Routes.QUIET})
            assertFalse(said,said.contains("—")||said.contains(" - ")||said.contains("–"));
    }

    @Test public void aFileGoesToTheDoorFirstOnlyWithHelpersOnceAndForSomebodyNear() {
        assertTrue(Routes.doorFirst(true,false,true,1));
        assertFalse("only between my devices it is always at the door, and goes nowhere else",Routes.doorFirst(false,false,true,1));
        assertFalse("offered or up already",Routes.doorFirst(true,true,true,1));
        assertFalse("too big or empty",Routes.doorFirst(true,false,false,1));
        assertFalse("nobody near",Routes.doorFirst(true,false,true,0));
    }

    private static String manifest(String... sources) {
        return new MediaManifest("image/png",10,"0x01","0x02","0x03",List.of("0x"+"a".repeat(64)),List.of(sources)).encode();
    }

    @Test public void aManifestForTheDoorAloneIsSaidOnlyToADeviceNear() {
        String door=manifest(), up=manifest("MxRelay@relay.example:9001");
        assertTrue(Routes.doorOnly(door,0));
        assertFalse("gone up",Routes.doorOnly(door,1234));
        assertFalse("asked for again, still up somewhere",Routes.doorOnly(up,0));
        assertFalse(Routes.doorOnly("",0));assertFalse(Routes.doorOnly(null,0));assertFalse(Routes.doorOnly("not one",0));
        assertEquals(door,Routes.manifestFor(door,0,true));
        assertEquals("away from home it is not up yet","",Routes.manifestFor(door,0,false));
        assertEquals(up,Routes.manifestFor(up,0,false));
        assertEquals(door,Routes.manifestFor(door,99,false));
        assertEquals("",Routes.manifestFor(null,0,true));
    }

    @Test public void whoIsNearIsAskedSafely() {
        assertFalse(Routes.near(null));
        Routes.nearNow=them->{throw new IllegalStateException("node going");};
        assertFalse(Routes.near(new NoteStore.Contact("Mx@x",  "x",false,"",new byte[0],new byte[0])));
    }
}
