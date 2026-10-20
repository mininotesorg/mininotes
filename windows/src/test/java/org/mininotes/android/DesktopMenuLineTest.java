package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * A menu line kept to one line of a reasonable length (the owner, 2026-10-14: "the menu is full screen on desktop": an Undo
 * naming a note whose first line was a whole paragraph made the menu as wide as the screen).
 */
public class DesktopMenuLineTest {
    @Test public void aLongNameIsShortenedAndOnOneLine() {
        String paragraph="Undo “We kept Maxima and used parts of your core that were already there. Each device opens a door.”";
        String said=DesktopMenus.shortly(paragraph);
        assertTrue(said.length()<=DesktopMenus.MOST);
        assertTrue(said.endsWith("…"));
        assertEquals("Two lines one line",DesktopMenus.shortly("Two lines\n  one line"));
        assertEquals("Sync now",DesktopMenus.shortly("Sync now"));
        assertTrue(DesktopMenus.item(paragraph).getText().length()<=DesktopMenus.MOST);
    }
}
