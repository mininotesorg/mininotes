// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

/** A note's length near its limits, said on both apps in the language in use (decisions 282 and 319). Made-up lengths only. */
public class NoteLimitTest {
    @After public void english(){Say.use("en");}

    private static String letters(int n){return "a".repeat(n);}

    @Test public void nothingIsSaidFarFromTheLimit() {
        assertNull(NoteLimit.near(10,0));
        assertNull(NoteLimit.near(NoteLimit.TITLE_NEAR-1,NoteLimit.TAB_NEAR-1));
        assertNull(NoteLimit.over(NoteLimit.TITLE,letters(NoteLimit.TAB)));
    }

    @Test public void fromNineTenthsTheLineSaysHowFar() {
        assertEquals("Note text: 21,600 of 24,000 characters.",NoteLimit.near(0,21600));
        assertEquals("Note text: 24,000 of 24,000 characters.",NoteLimit.near(0,24000));
        assertEquals("Title: 144 of 160 characters.",NoteLimit.near(144,0));
    }

    @Test public void overTheLimitItSaysWhy() {
        assertEquals("Note text has 24,001 characters; the limit is 24,000.",NoteLimit.near(0,24001));
        assertEquals("Title has 161 characters; the limit is 160.",NoteLimit.over(161,""));
        assertEquals("Note text has 24,001 characters; the limit is 24,000.",NoteLimit.over(0,letters(24001)));
    }

    /** The owner's note of eight tabs, 21,727 letters in all, was said nearly full: each tab now has its own 24,000 (decision 319). */
    @Test public void eachTabHasItsOwnLimitAndTheLineCountsTheTabOpen() {
        // Eight tabs of 3,000 letters: 24,000 and more in all, and nothing to say.
        StringBuilder note=new StringBuilder();
        for(int at=1;at<=8;at++)note.append("=== Tab ").append(at).append(" ===\n").append(letters(3000)).append('\n');
        assertTrue(note.length()>NoteLimit.TAB);
        assertNull(NoteLimit.over(0,note.toString()));
        assertNull(NoteLimit.near(0,note.length(),3000,8));
        // The tab open near its own limit says so as a tab.
        assertEquals("This tab: 22,000 of 24,000 characters.",NoteLimit.near(0,40000,22000,3));
        assertEquals("This tab has 24,001 characters; the limit is 24,000.",NoteLimit.near(0,40000,24001,3));
        // One tab over its limit is named, wherever it is.
        String over="=== Short ===\nab\n=== Long ===\n"+letters(24001)+"\n";
        String said=NoteLimit.over(0,over);
        assertTrue(said,said.startsWith("The tab Long has 24,00")&&said.endsWith(" characters; the limit is 24,000."));
        // The whole note has its own, larger limit.
        assertEquals("All the tabs: 90,000 of 100,000 characters.",NoteLimit.near(0,90000,100,5));
        assertEquals("Note text has 100,001 characters; the limit is 100,000.",NoteLimit.near(0,100001,100,5));
    }

    @Test public void inFrenchWithItsOwnNumbers() {
        Say.use("fr");
        String said=NoteLimit.near(0,21600);
        assertTrue(said,said.startsWith("Texte de la note : 21"));
        assertTrue(said,said.endsWith("000 caractères."));
        assertFalse("a French count has no comma: "+said,said.contains(","));
        said=NoteLimit.near(0,40000,22000,3);
        assertFalse("not in English: "+said,said.startsWith("This tab"));
    }
}
