// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import org.junit.Test;

/** The README is inside each app, made into the note, and listed as written (decision 221); run by both builds. */
public class ReadMeResourceTest {
    @Test public void theReadmeIsInsideTheApp() {
        String says=ReadMe.text();
        assertNotNull("help/README.md is packed with the app",says);
        assertTrue(says.startsWith("**Mininotes**\n"));
        assertTrue(says.contains("**Emergency contact**")&&says.contains("type the word private, then //: and your password"));
        assertTrue(says.length()<=ReadMe.MOST);
        assertEquals(says,PrivateCode.scrub(says));
        assertTrue("its SHA-256 is in help/README.sha256",ReadMe.shipped(says));
    }
}
