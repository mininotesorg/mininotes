// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import org.junit.Test;

/** What a device was told lately, kept across restarts so a start does not say it all again (decision 225). */
public class SaidTest {
    @Test public void keptAndReadBack() {
        String kept=Said.keep("1",1_700_000_000_000L);
        assertEquals(1_700_000_000_000L,Said.at(kept));
        assertEquals("1",Said.value(kept));
        // A card's value may itself hold an @: only the last one divides it.
        assertEquals("a@b",Said.value(Said.keep("a@b",5)));
    }

    @Test public void recentForADayOnly() {
        long at=1_700_000_000_000L;String kept=Said.keep("1",at);
        assertTrue(Said.recent(kept,at));
        assertTrue(Said.recent(kept,at+Said.KEEP-1));
        assertFalse("said again once a day",Said.recent(kept,at+Said.KEEP));
        assertFalse("a clock gone back says it again",Said.recent(kept,at-1));
    }

    @Test public void aDeviceOutOfReachIsTriedLessAndLess() {
        assertEquals(0,Said.rest(0));
        assertEquals(60_000L,Said.rest(1));
        assertEquals(120_000L,Said.rest(2));
        assertEquals(16*60_000L,Said.rest(5));
        assertEquals("half an hour at most",30*60_000L,Said.rest(6));
        assertEquals(30*60_000L,Said.rest(40));
        long at=1_700_000_000_000L;
        assertFalse("never missed",Said.resting(at,0,at));
        assertTrue(Said.resting(at,1,at+59_999));
        assertFalse(Said.resting(at,1,at+60_000));
        assertTrue(Said.resting(at,3,at+3*60_000));
        assertFalse("a clock gone back tries again",Said.resting(at,3,at-1));
    }

    @Test public void nothingReadableIsNothingKept() {
        assertFalse(Said.recent(null,5));
        assertFalse(Said.recent("1",5));
        assertFalse(Said.recent("1@soon",5));
        assertNull(Said.value("1@"));
        assertNull(Said.value(null));
        assertEquals(0,Said.at("@0"));
    }
}
