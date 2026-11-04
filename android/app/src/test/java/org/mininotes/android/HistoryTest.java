package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Decision 305: the History page's rules that need no device. The same thing again within ten minutes is one line; a device is
 * back only after a quarter of an hour without a word, never at a start; the codes the notebook keeps never change; the
 * sentences, an untitled thing included; and the day a line is under.
 */
public class HistoryTest {
    @Before public void english(){Say.use("en");}

    @Test public void theSameAgainWithinTenMinutesIsOneLine() {
        assertTrue(History.together(1_000,1_000));
        assertTrue(History.together(0,History.TOGETHER-1));
        assertFalse(History.together(0,History.TOGETHER));
        assertFalse("a clock put back is a new line",History.together(5_000,1_000));
    }

    @Test public void aDeviceIsBackOnlyAfterAQuietStretchNeverAtAStart() {
        assertFalse("nobody heard yet since this start",History.back(0,History.QUIET*4));
        assertFalse(History.back(1_000,1_000+History.QUIET-1));
        assertTrue(History.back(1_000,1_000+History.QUIET));
    }

    @Test public void theCodesKeptInTheNotebookNeverChange() {
        assertEquals("note-reached",History.Kind.NOTE_REACHED.code);
        assertEquals("note-came",History.Kind.NOTE_CAME.code);
        assertEquals("file-reached",History.Kind.FILE_REACHED.code);
        assertEquals("file-came",History.Kind.FILE_CAME.code);
        assertEquals("back",History.Kind.BACK.code);
        for(History.Kind one:History.Kind.values())assertSame(one,History.Kind.of(one.code));
        assertNull("a later build's line is not read as one of these",History.Kind.of("something-new"));
    }

    @Test public void eachLineIsOneSentence() {
        assertEquals("Plan reached Pro.",History.said(History.Kind.NOTE_REACHED,"Plan","Pro"));
        assertEquals("Screenshot.png reached Laptop.",History.said(History.Kind.FILE_REACHED,"Screenshot.png","Laptop"));
        assertEquals("Plan came from Ana.",History.said(History.Kind.NOTE_CAME,"Plan","Ana"));
        assertEquals("Untitled came from Ana.",History.said(History.Kind.NOTE_CAME,"  ","Ana"));
        assertEquals("Pro can be reached again.",History.said(History.Kind.BACK,"","Pro"));
    }

    @Test public void whatReachedSomebodySaysHowItWent() {
        // Decision 322: direct, left at the PC, or the relays that took it, the owner's named as theirs, each by its address.
        java.util.List<String> own=java.util.List.of("70.34.244.170:9501");
        assertEquals("Plan reached Pro, direct.",History.said(History.Kind.NOTE_REACHED,"Plan","Pro","door",own));
        assertEquals("Plan reached Pro, left at your PC.",History.said(History.Kind.NOTE_REACHED,"Plan","Pro","home",own));
        assertEquals("Plan reached Pro, through your relay 70.34.244.170.",
            History.said(History.Kind.NOTE_REACHED,"Plan","Pro","relay 70.34.244.170:9501",own));
        assertEquals("Plan reached Pro, through a public relay 203.0.113.9.",
            History.said(History.Kind.NOTE_REACHED,"Plan","Pro","relay 203.0.113.9:9001",own));
        assertEquals("Shot.png reached Pro, through your relay 70.34.244.170 and through public relays 203.0.113.9, 2001:db8::1.",
            History.said(History.Kind.FILE_REACHED,"Shot.png","Pro","relay 70.34.244.170:9501 203.0.113.9:9001 [2001:db8::1]:9001",own));
        assertEquals("the transport placed it, the relays not named","Plan reached Pro, through a relay.",
            History.said(History.Kind.NOTE_REACHED,"Plan","Pro","relay",own));
        assertEquals("a line kept before 322 says what it always said","Plan reached Pro.",History.said(History.Kind.NOTE_REACHED,"Plan","Pro","",own));
        assertEquals("what came is not said to have gone","Plan came from Ana.",History.said(History.Kind.NOTE_CAME,"Plan","Ana","door",own));
        assertEquals("a later build's road is not guessed at","Plan reached Pro.",History.said(History.Kind.NOTE_REACHED,"Plan","Pro","pigeon",own));
    }

    @Test public void aRelayIsShownByItsAddressAlone() {
        assertEquals("70.34.244.170",History.ip("70.34.244.170:9501"));
        assertEquals("relay.example.org",History.ip("relay.example.org:9001"));
        assertEquals("2001:db8::1",History.ip("[2001:db8::1]:9001"));
        assertEquals("70.34.244.170",History.ip("70.34.244.170"));
    }

    @Test public void aLineIsUnderTodayYesterdayOrItsDate() {
        ZoneId zone=ZoneId.of("Europe/Istanbul");
        long now=ZonedDateTime.of(2026,10,31,16,0,0,0,zone).toInstant().toEpochMilli();
        assertEquals("Today",History.day(now-3_600_000L,now,zone));
        assertEquals("Yesterday",History.day(ZonedDateTime.of(2026,10,30,23,59,0,0,zone).toInstant().toEpochMilli(),now,zone));
        String older=History.day(ZonedDateTime.of(2026,10,20,9,0,0,0,zone).toInstant().toEpochMilli(),now,zone);
        assertTrue(older,older.contains("20")&&older.contains("2026"));
    }
}
