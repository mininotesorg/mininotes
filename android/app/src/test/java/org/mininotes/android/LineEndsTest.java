// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import org.junit.Test;

/** Pasted words end their lines as the page does (decision 228): made-up text, written as the apps that send it write it. */
public class LineEndsTest {
    @Test public void paragraphSeparatorsBecomeLines() {
        String pasted="第一段。  第二段。 第三段。";
        assertTrue(LineEnds.other(pasted));
        assertEquals("第一段。\n\n第二段。\n第三段。",LineEnds.plain(pasted));
    }

    @Test public void everyOtherEndIsOneLine() {
        assertEquals("a\nb\nc\nd\ne",LineEnds.plain("a\r\nb\rc d\u0085e"));
        assertEquals("two lines, not three","a\n\nb",LineEnds.plain("a\r\n\r\nb"));
        assertEquals("a\n",LineEnds.plain("a\r"));
    }

    @Test public void plainWordsAreLeftAsTheyAre() {
        assertFalse(LineEnds.other("one\ntwo\n\n\tthree &nbsp;"));
        assertFalse(LineEnds.other(""));assertFalse(LineEnds.other(null));
        assertEquals("",LineEnds.plain(null));
    }
}
