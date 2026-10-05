// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/** The path a note stands on, from Home down, as it travels after everything a build from before reads. */
public class ParcelPathTest {
    private static final List<Enclosure.Listed> NONE=List.of();
    private static final List<Parcel.Step> PATH=List.of(
        new Parcel.Step("c-1","Work","briefcase",3,-10L),
        new Parcel.Step("b-7","Meetings","",0,5L),
        new Parcel.Step("x-9","Mondays","calendar",7,1L));

    private static Parcel.Sent sent(List<String> history,List<Parcel.Step> path) {
        return new Parcel.Sent("b-7","Meetings","x-9","Mondays","Monday","Two things.",true,List.of(),
            "PAGE","n-1",true,4L,true,NONE,123L,history,path,"sticky-note",new byte[]{1,2,3});
    }

    @Test public void thePathComesBackWholeWithTheNotesOwnIconAndPicture() throws IOException {
        Parcel.Sent in=Parcel.open(Parcel.wrap(sent(List.of(),PATH)));
        assertNotNull(in.path);
        assertEquals(3,in.path.size());
        assertEquals("c-1",in.path.get(0).id);
        assertEquals("Work",in.path.get(0).name);
        assertEquals("briefcase",in.path.get(0).icon);
        assertEquals(3,in.path.get(0).tint);
        assertEquals(-10L,in.path.get(0).ordinal);
        assertEquals("Mondays",in.path.get(2).name);
        assertEquals("sticky-note",in.icon);
        assertArrayEquals(new byte[]{1,2,3},in.image);
        assertEquals("Two things.",in.body);
        assertEquals(4L,in.basedOn);
    }

    @Test public void aNoteOnHomeHasAnEmptyPathNotNone() throws IOException {
        Parcel.Sent in=Parcel.open(Parcel.wrap(sent(List.of(),List.of())));
        assertNotNull(in.path);
        assertTrue(in.path.isEmpty());
    }

    /**
     * What a build from before reads is the same with the path as without it, byte for byte: the path is only ever
     * after the texts held before, where such a build stops.
     */
    @Test public void aBuildFromBeforeReadsExactlyWhatItReadBefore() throws IOException {
        List<String> history=List.of(Arriving.trace("one"),Arriving.trace("two"));
        byte[] without=Parcel.wrap(sent(history,null));
        byte[] with=Parcel.wrap(sent(history,PATH));
        assertTrue(with.length>without.length);
        assertArrayEquals("everything before the path is what was always sent",without,Arrays.copyOf(with,without.length));
        assertNull(Parcel.open(without).path);
    }

    /**
     * The sender's own colour rides after the path (the owner, 2026-10-03: everybody sees each person's colour): read back
     * whole, and everything before it is what a build from before reads; never said where the sender never chose one.
     */
    @Test public void theSendersColourRidesAfterThePath() throws IOException {
        Parcel.Sent going=sent(List.of(),PATH);
        byte[] plain=Parcel.wrap(going);
        assertEquals("never chosen, never said",0L,Parcel.open(plain).inkAt);
        going.ink=Tint.count()-1;going.inkAt=1_700_000_000_000L;
        byte[] inked=Parcel.wrap(going);
        assertArrayEquals("what came before is unchanged",plain,Arrays.copyOf(inked,plain.length));
        Parcel.Sent in=Parcel.open(inked);
        assertEquals(Tint.count()-1,in.ink);assertEquals(1_700_000_000_000L,in.inkAt);
        assertEquals("the path still reads",3,in.path.size());
        // Without a path there is nowhere for it: a build that knows no trees is not told.
        Parcel.Sent old=sent(List.of(),null);old.ink=2;old.inkAt=5L;
        assertEquals(0L,Parcel.open(Parcel.wrap(old)).inkAt);
        // And a parcel made to fit keeps it.
        Parcel.Sent fitted=Parcel.open(Parcel.wrap(going,Envelope.MAX_TEXT));
        assertEquals(Tint.count()-1,fitted.ink);
    }

    /** When a note is to be gone rides after the path too, with the colour or without it, and is read back whole. */
    @Test public void whenItIsToBeGoneRidesAfterThePath() throws IOException {
        Parcel.Sent going=sent(List.of(),PATH);
        assertEquals("unsaid",-1L,Parcel.open(Parcel.wrap(going)).until);
        going.until=0L;
        assertEquals("not temporary, said",0L,Parcel.open(Parcel.wrap(going)).until);
        going.until=1_800_000_000_000L;going.ink=2;going.inkAt=7L;
        Parcel.Sent in=Parcel.open(Parcel.wrap(going));
        assertEquals(1_800_000_000_000L,in.until);assertEquals(2,in.ink);assertEquals(7L,in.inkAt);
        going.inkAt=0L;
        assertEquals("without the colour before it",1_800_000_000_000L,Parcel.open(Parcel.wrap(going)).until);
    }

    /** Where there were no texts to say, none are said, so the path still stands behind them. */
    @Test public void withNoTextsHeldBeforeTheyAreSaidAsNone() throws IOException {
        Parcel.Sent in=Parcel.open(Parcel.wrap(sent(null,PATH)));
        assertNotNull(in.history);
        assertTrue(in.history.isEmpty());
        assertEquals(3,in.path.size());
        // And a parcel with neither ends where it always ended.
        Parcel.Sent plain=Parcel.open(Parcel.wrap(sent(null,null)));
        assertNull(plain.history);
        assertNull(plain.path);
    }

    /** A parcel made to fit that drops its list of files drops the path too: it could not be said without it. */
    @Test public void aParcelWithNoListOfFilesSaysNoPath() throws IOException {
        Parcel.Sent noFiles=new Parcel.Sent("b-7","Meetings","x-9","Mondays","Monday","Two things.",true,List.of(),
            "PAGE","n-1",true,4L,true,null,0L,null,PATH,"",null);
        Parcel.Sent in=Parcel.open(Parcel.wrap(noFiles));
        assertNull(in.files);
        assertNull(in.path);
        assertEquals("Mondays",in.bookName);
    }

    /** A path cut short is no path; the note is still the note, landing where its two old fields say. */
    @Test public void aDamagedPathIsNoPathAndTheNoteStillReads() throws IOException {
        byte[] whole=Parcel.wrap(sent(List.of(),PATH));
        byte[] cut=Arrays.copyOf(whole,whole.length-5);
        Parcel.Sent in=Parcel.open(cut);
        assertNotNull(in);
        assertNull(in.path);
        assertEquals("Two things.",in.body);
        assertEquals("x-9",in.book);
    }

    @Test public void aPictureTooBigToCarryIsNotCarried() throws IOException {
        Parcel.Sent big=new Parcel.Sent("b-7","Meetings","x-9","Mondays","Monday","Two things.",true,List.of(),
            "PAGE","n-1",true,4L,true,NONE,123L,null,PATH,"",new byte[Parcel.IMAGE_MOST+1]);
        Parcel.Sent in=Parcel.open(Parcel.wrap(big));
        assertEquals(3,in.path.size());
        assertEquals(0,in.image.length);
    }
}
