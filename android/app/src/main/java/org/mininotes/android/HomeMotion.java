// SPDX-License-Identifier: GPL-3.0-or-later
package org.mininotes.android;

/** Measurements shared by Home's scrolling and dragging, without Android gesture state. */
final class HomeMotion {
    private HomeMotion(){}
    static final int NONE=0,HORIZONTAL=1,VERTICAL=2;

    /** A page ends on a row boundary, even when the last row is partly below the viewport. */
    static int rows(int viewport,int row){return Math.max(1,(int)Math.ceil(Math.max(1,viewport)/(double)Math.max(1,row)));}

    /** Pick once per swipe. Finger jitter never changes the direction midway through it. */
    static int axis(float dx,float dy){return Math.abs(dx)>Math.abs(dy)?HORIZONTAL:VERTICAL;}

    /** Gentle at the start of the edge band, faster towards its outer edge. Pixels per second. */
    static float edgeSpeed(float point,float start,float end,float band,float maximum) {
        if(band<=0||end<=start)return 0;
        float before=Math.max(0,Math.min(1,(start+band-point)/band));
        float after=Math.max(0,Math.min(1,(point-end+band)/band));
        return maximum*(after*after-before*before);
    }
}
