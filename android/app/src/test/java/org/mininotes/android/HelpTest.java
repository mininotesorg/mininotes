// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;

/** The help request's wire format, map links and words (decision 113). No Android types, so it is unit tested. */
public class HelpTest {

    private static byte[] incident() {
        byte[] b=new byte[Help.INCIDENT];for(int i=0;i<b.length;i++)b[i]=(byte)(i+1);return b;
    }

    @Test public void aRequestWithAPlaceReadsBackAsItself() {
        Help.Request r=new Help.Request(incident(),2,"Help me",true,48.8566,2.3522,12.5f,1000L,2000L);
        Help.Request back=Help.open(Help.wrap(r));
        assertNotNull(back);
        assertArrayEquals(incident(),back.incident);
        assertEquals(2,back.number);
        assertEquals("Help me",back.message);
        assertTrue(back.located);
        assertEquals(48.8566,back.lat,1e-9);
        assertEquals(2.3522,back.lon,1e-9);
        assertEquals(12.5f,back.metres,1e-6);
        assertEquals(1000L,back.placeAt);
        assertEquals(2000L,back.at);
    }

    @Test public void aRequestWithNoPlaceReadsBackAsItself() {
        Help.Request r=new Help.Request(incident(),0,"",false,0,0,0,0,5000L);
        Help.Request back=Help.open(Help.wrap(r));
        assertNotNull(back);
        assertFalse(back.located);
        assertEquals(0,back.number);
        assertEquals(5000L,back.at);
    }

    @Test public void itIsACardAndACardIsNotANote() throws Exception {
        Help.Request r=new Help.Request(incident(),0,"x",false,0,0,0,0,1L);
        byte[] card=Help.wrap(r);
        assertTrue(Help.isCard(card));
        // A build that read a help request as a note would write a cry for help over somebody's words: it must not.
        assertNull(Parcel.open(card));
        // And a note is not a help request.
        byte[] note=Parcel.wrap(new Parcel.Sent("c","C","b","B","","Milk",true));
        assertFalse(Help.isCard(note));
        assertNull(Help.open(note));
        assertNull(Help.open(Receipt.wrap(Receipt.HELP)));
    }

    @Test public void theWordsAreCutToAFewNotALetter() {
        StringBuilder many=new StringBuilder();for(int i=0;i<Help.MOST_WORDS+50;i++)many.append('a');
        Help.Request back=Help.open(Help.wrap(new Help.Request(incident(),0,many.toString(),false,0,0,0,0,1L)));
        assertNotNull(back);
        assertEquals(Help.MOST_WORDS,back.message.length());
    }

    @Test public void aBadIncidentIsRefused() {
        try{Help.wrap(new Help.Request(new byte[5],0,"x",false,0,0,0,0,1L));fail("a short incident should be refused");}
        catch(IllegalArgumentException expected){/* as it should */}
    }

    @Test public void brokenBytesAreNotACard() {
        assertNull(Help.open(null));
        assertNull(Help.open(new byte[]{'M','N','H'}));
        assertNull(Help.open(new byte[]{'M','N','H','1'}));
        // Trailing rubbish after a whole card is not a card: half a card is not a card.
        byte[] card=Help.wrap(new Help.Request(incident(),0,"x",false,0,0,0,0,1L));
        byte[] more=java.util.Arrays.copyOf(card,card.length+1);
        assertNull(Help.open(more));
    }

    @Test public void aPlaceOutOfTheWorldIsRefused() {
        Help.Request r=new Help.Request(incident(),0,"x",true,200,500,0,0,1L);
        assertNull(Help.open(Help.wrap(r)));
    }

    @Test public void theMapLinksUseAFullStopWhateverTheLanguage() {
        java.util.Locale was=java.util.Locale.getDefault();
        try {
            // France writes 48,8566; a geo: link must still say 48.856600, or a map opens on the wrong place.
            java.util.Locale.setDefault(java.util.Locale.FRANCE);
            String geo=Help.geo(48.8566,2.3522);
            assertTrue(geo,geo.startsWith("geo:48.856600,2.352200"));
            String web=Help.web(-0.5,-0.25);
            assertTrue(web,web.contains("mlat=-0.500000")&&web.contains("mlon=-0.250000"));
        } finally { java.util.Locale.setDefault(was); }
    }

    @Test public void theAlertSaysWhoAndWhat() {
        assertEquals("HELP from Ana",Help.title("Ana"));
        assertEquals("HELP from a contact",Help.title(""));
        String body=Help.body("By the river",true,48.8566,2.3522,10f,"14:05");
        assertTrue(body,body.contains("By the river"));
        assertTrue(body,body.contains("48.856600, 2.352200"));
        assertTrue(body,body.contains("14:05"));
        assertTrue(body,body.contains("openstreetmap.org"));
        assertTrue(Help.body("",false,0,0,0,"").contains("No location"));
    }
}
