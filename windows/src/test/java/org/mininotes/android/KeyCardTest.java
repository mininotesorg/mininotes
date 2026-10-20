// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import org.junit.Test;

/** The owner's Integritas key, handed to the owner's own devices chosen (decision 210): what travels, and what does not. */
public class KeyCardTest {
    @Test public void theIntegritasKeyGoesThereAndBackWhole() {
        byte[] wire=KeyCard.wrap(new KeyCard.Card("integritas","made-up-key-1234",1_700_000_000_000L));
        assertTrue(KeyCard.isCard(wire));
        KeyCard.Card back=KeyCard.open(wire);
        assertEquals("integritas",back.name);assertEquals("made-up-key-1234",back.value);assertEquals(1_700_000_000_000L,back.decided);
        // Taken away is said as an empty key.
        assertEquals("",KeyCard.open(KeyCard.wrap(new KeyCard.Card("integritas","",5))).value);
    }

    @Test public void onlyTheKeysThatTravelAndNothingBroken() {
        try{KeyCard.wrap(new KeyCard.Card("password","x",1));fail("a key that does not travel was wrapped");}catch(IllegalArgumentException expected){}
        byte[] wire=KeyCard.wrap(new KeyCard.Card("integritas","k",1));
        byte[] cut=java.util.Arrays.copyOf(wire,wire.length-3);
        assertNull(KeyCard.open(cut));
        byte[] longer=java.util.Arrays.copyOf(wire,wire.length+1);
        assertNull("nothing after it",KeyCard.open(longer));
        assertFalse(KeyCard.isCard(AboutMe.MAGIC));
    }
}
