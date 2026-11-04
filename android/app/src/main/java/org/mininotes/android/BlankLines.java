// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

/**
 * The ruled paper under the writing, as a paper pad has it (decision 267): it goes on past the last line, and a click or a
 * tap anywhere on it puts the cursor on that line, the empty lines down to it made for you.
 *
 * <p>Lines made by a click are paper, not writing. Until something is typed on them nothing is written down, nothing gets a
 * new revision and nothing goes to anybody; and if the cursor leaves them unwritten, they go again. Both apps ask the same
 * three questions here: how many lines a click below the writing makes, how much paper to give below the last line, and
 * how much of a made stretch the cursor still needs.
 */
final class BlankLines {
    private BlankLines(){}

    /** A click a long way down never makes more lines than this: about two screens of a large monitor. */
    static final int MOST=100;

    /**
     * How many lines a click at {@code at} makes, measured down the page as the writing is: none above {@code bottom}, the
     * foot of the last line written; one on the first empty rule under it, two on the next, and so on.
     */
    static int below(double at,double bottom,double lineHeight) {
        if(!(lineHeight>0)||!(at>bottom))return 0;
        return (int)Math.min(MOST,Math.floor((at-bottom)/lineHeight)+1);
    }

    /** That many line ends, the lines made. */
    static String lines(int count) {
        return count<=0?"":"\n".repeat(count);
    }

    /**
     * The empty paper under the last line, for a page that shows {@code shown} of itself: all of a screen but two lines, so
     * a note scrolled to its end still shows the last of its writing at the top, and below it paper to write on.
     */
    static int paperBelow(int shown,int lineHeight) {
        return Math.max(0,shown-2*Math.max(0,lineHeight));
    }

    /**
     * Lines made by a tap from {@code madeFrom} to the end, not yet written on, with the cursor now at
     * {@code start}..{@code end}: how long the words should be, keeping the made lines down to the cursor and no further.
     * The whole text's length when the made lines are no longer only line ends (something was written there), so nothing
     * is taken; {@code madeFrom} when the cursor went back up into the writing, or chose words.
     */
    static int keep(CharSequence text,int madeFrom,int start,int end) {
        int length=text.length();
        if(madeFrom<0||madeFrom>length)return length;
        for(int at=madeFrom;at<length;at++)if(text.charAt(at)!='\n')return length;
        if(start!=end||end<madeFrom)return madeFrom;
        return Math.min(end,length);
    }

    /** Whether the made lines from {@code madeFrom} are still only line ends: paper, not writing. */
    static boolean unwritten(CharSequence text,int madeFrom) {
        if(madeFrom<0||madeFrom>text.length())return false;
        for(int at=madeFrom;at<text.length();at++)if(text.charAt(at)!='\n')return false;
        return true;
    }
}
