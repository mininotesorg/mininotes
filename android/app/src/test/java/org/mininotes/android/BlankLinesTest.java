package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;

/** The paper under the writing, as both apps measure it (decision 267). */
public class BlankLinesTest {
    @Test public void aClickOnTheWritingMakesNoLines() {
        assertEquals(0,BlankLines.below(100,120,24));
        assertEquals("the foot of the last line is still the writing",0,BlankLines.below(120,120,24));
    }

    @Test public void eachRuleDownMakesOneLineMore() {
        assertEquals("the first empty rule",1,BlankLines.below(121,120,24));
        assertEquals(1,BlankLines.below(143,120,24));
        assertEquals("the second",2,BlankLines.below(144,120,24));
        assertEquals("the fourth",4,BlankLines.below(120+3*24+5,120,24));
    }

    @Test public void farDownIsHeldToAFewScreens() {
        assertEquals(BlankLines.MOST,BlankLines.below(1e6,0,24));
        assertEquals("no line height, no lines",0,BlankLines.below(500,0,0));
    }

    @Test public void theLinesAreLineEnds() {
        assertEquals("",BlankLines.lines(0));
        assertEquals("\n\n\n",BlankLines.lines(3));
    }

    @Test public void aboutAScreenOfPaperBelow() {
        assertEquals("all of the view but two lines",600-48,BlankLines.paperBelow(600,24));
        assertEquals("a view smaller than two lines gets none",0,BlankLines.paperBelow(30,24));
    }

    @Test public void madeLinesAreKeptDownToTheCursorOnly() {
        String text="abc\n\n\n\n";  // three letters written, four lines made from 3
        assertEquals("the cursor on the last made line: all kept",7,BlankLines.keep(text,3,7,7));
        assertEquals("on the second made line: the two under it go",5,BlankLines.keep(text,3,5,5));
        assertEquals("back up in the writing: all of them go",3,BlankLines.keep(text,3,1,1));
        assertEquals("words chosen: all of them go",3,BlankLines.keep(text,3,4,6));
    }

    @Test public void madeLinesWrittenOnAreWriting() {
        assertEquals("a letter typed there: nothing taken",8,BlankLines.keep("abc\n\nxy\n",3,1,1));
        assertFalse(BlankLines.unwritten("abc\n\nx",3));
        assertTrue(BlankLines.unwritten("abc\n\n",3));
        assertTrue("nothing made yet is nothing written",BlankLines.unwritten("abc",3));
        assertFalse("none made",BlankLines.unwritten("abc",-1));
        assertEquals("text shorter than where they were made: not ours",2,BlankLines.keep("ab",3,2,2));
    }
}
