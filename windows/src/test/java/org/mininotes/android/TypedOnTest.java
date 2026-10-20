// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

/**
 * A line caught on one device while it was being typed, and the same line typed on further on the other, merged as one
 * line, the longer; not kept twice as if two people had written two things (decision 215). Made-up words only.
 */
public class TypedOnTest {
    @Test public void theLineTypedOnStaysAndItsEarlierStateGoesWhicheverSideWorksItOut() {
        String base="Shopping",
            here="Shopping\nthe shop opens at nine and closes",
            there="Shopping\nthe shop opens at nine and closes at six on weekdays";
        Merge.Result one=Merge.merge(base,here,there),two=Merge.merge(base,there,here);
        assertEquals("Shopping\nthe shop opens at nine and closes at six on weekdays",one.text);
        assertEquals("the same on both devices",one.text,two.text);
    }

    @Test public void twoDifferentLinesStayBothAndShortOnesAreNeverTouched() {
        // Two people writing two different things over the same line: both stay, as they always have.
        Merge.Result both=Merge.merge("Plan","Plan\nmeet at the station at noon","Plan\nmeet at the museum at ten");
        assertTrue(both.text.contains("meet at the station at noon")&&both.text.contains("meet at the museum at ten"));
        // A short line that starts a longer one ("- eggs" and "- eggs and flour") can be meant: kept.
        assertEquals(List.of("- eggs","- eggs and flour"),Merge.typedOn(List.of("- eggs","- eggs and flour")));
        // The same long line twice is one line.
        assertEquals(List.of("the meeting moved to Thursday"),Merge.typedOn(List.of("the meeting moved to Thursday","the meeting moved to Thursday")));
    }
}
