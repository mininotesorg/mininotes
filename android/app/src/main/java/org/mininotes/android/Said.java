// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

/**
 * What a device was told lately and took, kept across restarts: a receipt ("this build knows about trees") or a card (your
 * own Parlons! address), with when (decision 225).
 *
 * <p>Each start of the app used to say every one of them again to every paired device, and they said theirs back, each a
 * Maxima message with its proof of work: on Graphene, with three devices, about four minutes of a whole core at every start
 * (the owner, 2026-10-20). What hears them keeps them for good, so once a day is enough to mend one a relay took and lost.
 * Kept as "value@when".
 */
final class Said {
    private Said(){}

    /** How long something said and taken is not said again after a restart. */
    static final long KEEP=24L*60*60*1000;

    static String keep(String value,long at){return value+"@"+at;}

    /** When it was said, or 0 where nothing readable is kept. */
    static long at(String kept) {
        if(kept==null)return 0;
        int cut=kept.lastIndexOf('@');
        try{return cut<0?0:Long.parseLong(kept.substring(cut+1));}catch(NumberFormatException notOne){return 0;}
    }

    /** What was said, or null where nothing readable is kept. */
    static String value(String kept){return at(kept)<=0?null:kept.substring(0,kept.lastIndexOf('@'));}

    /** Whether it was said within the day: never from a clock that has since gone back. */
    static boolean recent(String kept,long now){long at=at(kept);return at>0&&at<=now&&now-at<KEEP;}

    /**
     * How long a device that could not be handed a receipt or a card is left before the next try, after {@code misses} in a
     * row: a minute, then twice as long each time, to half an hour (decision 226). Each try is a proof of work for every way
     * there is to it, its doors, its home and the relays; seen on Graphene with the Pro's Mininotes stopped by an install,
     * about 80% of a core kept trying it every minute and every half minute.
     */
    static long rest(int misses){return misses<=0?0:Math.min(30L*60_000L,60_000L<<Math.min(misses-1,5));}

    /** Whether a device missed {@code misses} times in a row, the last at {@code at}, is still left alone at {@code now}. */
    static boolean resting(long at,int misses,long now){return misses>0&&at<=now&&now-at<rest(misses);}
}
