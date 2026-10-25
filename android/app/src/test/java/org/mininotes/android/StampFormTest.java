// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

/** What a note's stamp covers (decision 230): its title, an empty line and its text; older stamps their text alone. Made-up notes. */
public class StampFormTest {
    private static NoteStore.StampRow row(String kind,String hash){
        return new NoteStore.StampRow(1,"id",kind,"Deed",hash,1,"r","","",NoteStore.STAMP_CONFIRMED,0,0,2374505,"","","",true,"","");
    }

    @Test public void titleThenAnEmptyLineThenTheText() {
        assertEquals("Contract with Ana\n\nAna pays 300 by Friday",Integritas.noteWords("Contract with Ana","Ana pays 300 by Friday"));
        assertEquals("no title: the text alone","Ana pays",Integritas.noteWords("","Ana pays"));
        assertEquals("a title of spaces is none","Ana pays",Integritas.noteWords("   ","Ana pays"));
        assertEquals("taken as written, as Copy the note","  Deed \n\nx",Integritas.noteWords("  Deed ","x"));
        assertEquals("",Integritas.noteWords(null,null));
    }

    @Test public void eachStampIsComparedInItsOwnForm() throws Exception {
        String title="Deed",body="Synthetic words";
        NoteStore.StampRow old=row(Integritas.NOTE,Integritas.hashOfText(body)),titled=row(Integritas.TITLED,Integritas.hashOfNote(title,body));
        assertTrue(old.note());assertTrue(titled.note());assertTrue(titled.titled());assertFalse(old.titled());
        assertEquals(old.hash(),Integritas.hashNow(old,title,body));
        assertEquals(titled.hash(),Integritas.hashNow(titled,title,body));
        // Renamed: the titled stamp no longer matches, the old text-only one still does, and says so by its kind.
        assertNotEquals(titled.hash(),Integritas.hashNow(titled,"Deed, signed",body));
        assertSame(old,Integritas.matching(List.of(titled,old),"Deed, signed",body));
        assertSame(titled,Integritas.matching(List.of(titled,old),title,body));
        assertNull(Integritas.matching(List.of(titled),"Deed, signed",body));
        assertTrue(row(Integritas.OUTSIDE,"h").outside());assertFalse(row(Integritas.OUTSIDE,"h").note());
    }
}
