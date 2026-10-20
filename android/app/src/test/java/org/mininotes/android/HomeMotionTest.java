// SPDX-License-Identifier: GPL-3.0-or-later
package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeMotionTest {
    @Test public void pagesJoinOnWholeRowsAcrossScreenSizesAndTextSizes() {
        for(int viewport:new int[]{1,50,399,400,788,1200})for(int row:new int[]{100,200,270}) {
            int rows=HomeMotion.rows(viewport,row);
            assertTrue(rows*row>=viewport);
            assertTrue(rows*row-viewport<row);
            // The final visible strip is a usable cell; the next page starts on the next regular row.
            assertArrayEquals(new int[]{rows-1,0},Layout.under(1,rows*row-1,100,row,4));
            assertArrayEquals(new int[]{0,1,0,0},Layout.underCanvas(1,rows*row,400,rows*row,0,0,100,row,4,rows,0,0,0,1));
        }
    }
    @Test public void aMostlyVerticalOrHorizontalSwipeChoosesOnlyThatAxis() {
        assertEquals(HomeMotion.VERTICAL,HomeMotion.axis(12,-60));
        assertEquals(HomeMotion.HORIZONTAL,HomeMotion.axis(-60,12));
    }
    @Test public void edgeScrollHasNoStepAtTheBandAndAcceleratesTowardsTheEdge() {
        assertEquals(0,HomeMotion.edgeSpeed(50,0,500,50,400),0);
        assertEquals(0,HomeMotion.edgeSpeed(250,0,500,50,400),0);
        assertEquals(-100,HomeMotion.edgeSpeed(25,0,500,50,400),0);
        assertEquals(-400,HomeMotion.edgeSpeed(0,0,500,50,400),0);
        assertEquals(100,HomeMotion.edgeSpeed(475,0,500,50,400),0);
        assertEquals(400,HomeMotion.edgeSpeed(500,0,500,50,400),0);
        assertEquals(400,HomeMotion.edgeSpeed(600,0,500,50,400),0);
    }
}
