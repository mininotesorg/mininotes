package org.mininotes.android;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/** Bold, italic and underline as marks in the words (docs/HOME.md, decision 76). */
public class MarksTest {
    private static String words(String text,Marks.Run run){return text.substring(run.start(),run.end());}

    @Test public void theThreeMarksAreReadOnALine() {
        String text="Buy **milk** and *eggs* and __bread__ today";
        List<Marks.Run> runs=Marks.of(text);
        assertEquals(3,runs.size());
        assertEquals(Marks.Style.BOLD,runs.get(0).style());assertEquals("milk",words(text,runs.get(0)));
        assertEquals(Marks.Style.ITALIC,runs.get(1).style());assertEquals("eggs",words(text,runs.get(1)));
        assertEquals(Marks.Style.UNDERLINE,runs.get(2).style());assertEquals("bread",words(text,runs.get(2)));
        assertEquals(text.indexOf("**milk**"),runs.get(0).open());
        assertEquals(text.indexOf("**milk**")+8,runs.get(0).close());
    }

    @Test public void whatIsNotAMarkIsLeftAlone() {
        assertTrue("2 * 3 * 4",Marks.of("2 * 3 * 4").isEmpty());
        assertTrue("a snake_case_name",Marks.of("a snake_case_name").isEmpty());
        assertTrue("across lines",Marks.of("**one\ntwo**").isEmpty());
        assertTrue("never closed",Marks.of("**open").isEmpty());
        assertTrue("a bullet",Marks.of("* item\n* item").isEmpty());
        assertTrue("three stars",Marks.of("***").isEmpty());
    }

    @Test public void stylesNestInsideEachOther() {
        String text="__underlined **and bold**__";
        List<Marks.Run> runs=Marks.of(text);
        assertEquals(2,runs.size());
        assertEquals(Marks.Style.UNDERLINE,runs.get(0).style());
        assertEquals(Marks.Style.BOLD,runs.get(1).style());assertEquals("and bold",words(text,runs.get(1)));
    }

    @Test public void aStyleIsPutOnAndTakenOff() {
        Marks.Changed on=Marks.toggle("Buy milk today",4,8,Marks.Style.UNDERLINE);
        assertEquals("Buy __milk__ today",on.text());
        assertEquals("milk",on.text().substring(on.start(),on.end()));
        Marks.Changed off=Marks.toggle(on.text(),on.start(),on.end(),Marks.Style.UNDERLINE);
        assertEquals("Buy milk today",off.text());
        assertEquals("milk",off.text().substring(off.start(),off.end()));
        // Spaces chosen with the words stay outside the marks.
        assertEquals("Buy **milk** today",Marks.toggle("Buy milk today",3,9,Marks.Style.BOLD).text());
        // Nothing chosen: a pair to type into, the cursor between.
        Marks.Changed empty=Marks.toggle("Hi ",3,3,Marks.Style.ITALIC);
        assertEquals("Hi **",empty.text());assertEquals(4,empty.start());
        // Italic taken off a bold's word only where it is an italic, not the bold's inner stars.
        assertTrue("the bold's stars stay",Marks.toggle("**milk**",2,6,Marks.Style.ITALIC).text().startsWith("**"));
    }
}
