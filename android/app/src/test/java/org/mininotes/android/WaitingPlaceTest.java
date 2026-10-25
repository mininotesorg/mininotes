package org.mininotes.android;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Where a note made from Temp's + is going, kept past the screen it was made on (the owner, 2026-10-24: "Again, adding a note to
 * temp, without title saved it on the desktop not in temp"; decision 238): the same note comes back still going there, and no
 * other note is sent there by it.
 */
public class WaitingPlaceTest {
    @Test public void theSameNoteComesBackGoingToItsPlace() {
        String kept=WaitingPlace.say("note-1",NoteStore.TEMP);
        assertEquals(NoteStore.TEMP,WaitingPlace.forNote(kept,"note-1"));
        assertEquals(NoteStore.ARCHIVE,WaitingPlace.forNote(WaitingPlace.say("n",NoteStore.ARCHIVE),"n"));
        assertEquals(NoteStore.FAVOURITES,WaitingPlace.forNote(WaitingPlace.say("n",NoteStore.FAVOURITES),"n"));
    }
    @Test public void anotherNoteIsNotSentThere() {
        assertNull(WaitingPlace.forNote(WaitingPlace.say("note-1",NoteStore.TEMP),"note-2"));
        assertNull(WaitingPlace.forNote(WaitingPlace.say("note-1",NoteStore.TEMP),null));
        assertNull("a prefix is not the note",WaitingPlace.forNote(WaitingPlace.say("note-1",NoteStore.TEMP),"note-"));
    }
    @Test public void nothingWaitsWithoutAPlaceAPlusPutsThingsIn() {
        assertNull(WaitingPlace.say("note-1",null));
        assertNull(WaitingPlace.say(null,NoteStore.TEMP));
        assertNull("a collection is where a note is made, not where it is put afterwards",WaitingPlace.say("note-1","some-folder"));
        assertNull(WaitingPlace.forNote(null,"note-1"));
        assertNull(WaitingPlace.forNote("note-1\tsome-folder","note-1"));
        assertNull(WaitingPlace.forNote("note-1","note-1"));
        assertNull(WaitingPlace.forNote("note-1\ttemp\tmore","note-1"));
    }
}
