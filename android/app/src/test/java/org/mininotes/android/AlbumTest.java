// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

/** What both apps' albums are built on (decision 217): Photos' one id, which files are pictures, and browsing them. */
public class AlbumTest {
    @Test public void photosHasOneIdForOnePerson() {
        String one=Album.photosId("0123456789abcdef0123456789abcdef");
        assertEquals("the same on every device of the person",one,Album.photosId("0123456789abcdef0123456789abcdef"));
        assertNotEquals(one,Album.photosId("fedcba9876543210fedcba9876543210"));
        assertEquals("a folder id like any other",one,UUID.fromString(one).toString());
        assertTrue(Icons.known(Album.ICON));
    }

    @Test public void picturesAreKnownByKindOrName() {
        assertTrue(Album.picture("IMG_0001.JPG",null));assertTrue(Album.picture("scan","image/png"));assertTrue(Album.picture("shot.heic",""));
        assertFalse(Album.picture("tickets.pdf","application/pdf"));assertFalse(Album.picture(null,null));
    }

    @Test public void browsingStopsAtEitherEnd() {
        assertEquals(1,Album.next(0,3));assertEquals(2,Album.next(2,3));assertEquals(0,Album.previous(0,3));assertEquals(1,Album.previous(2,3));
        assertEquals(-1,Album.next(0,0));
        assertEquals("3 of 24",Album.position(2,24));
    }

    @Test public void squaresFillTheWidth() {
        int across=Album.across(296,88,4);assertEquals(3,across);
        assertEquals(96,Album.side(296,3,4));
        assertTrue(Arrays.equals(new int[]{50,0,100},Album.square(200,100)));
        assertTrue(Arrays.equals(new int[]{0,0,100,50},Album.fit(200,100,100,100))||Album.fit(200,100,100,100)[2]==100);
        assertEquals(4,Album.step(4000,3000,700));assertEquals(1,Album.step(300,200,256));
        List<NoteStore.Held> files=List.of(new NoteStore.Held("a","f","a.png","image/png",1,5),new NoteStore.Held("b","f","b.txt","text/plain",1,9),new NoteStore.Held("c","f","c.jpg",null,1,7));
        assertEquals("c",Album.pictures(files).get(0).id);assertEquals(2,Album.pictures(files).size());
    }
}
