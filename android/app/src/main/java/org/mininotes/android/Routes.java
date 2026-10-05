// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which way things go to each device, said where the owner can see it, and files offered from this device's door to a
 * device on the same Wi-Fi before they go up to a relay (docs/HOME.md, decision 96; docs/DIRECT.md, <i>What People and
 * devices shows</i>).
 *
 * <p>The owner, 2026-10-04: a screenshot on Temp reached the laptop, slowly, though both were on one Wi-Fi: "Is there a
 * setting or a visualisation that would tell me if I'm directly connected to a device ... and if it is used at the moment
 * or if a relay is used?" Notes already went door to door at home; nothing said so. Files went up to two relays first and
 * were fetched back, even for a device in the same room.
 *
 * <p>What is kept: for each device, by its identity key, the road the last send that was taken went by - a door, its home
 * (the owner's PC), or the relays - and when. A door or a relay taking something is not it arriving (only the far device's
 * answer is), but the road is what the owner asked about. Kept in the node's own settings, so it says the same after a
 * restart; a few hundred bytes a device.
 *
 * <p>Holds no Android types: the words and the decision are unit tested.
 */
final class Routes {
    private Routes(){}

    /** The road a send took: see {@link Direct#send}. */
    enum Way {
        DOOR("direct"),HOME("left at your PC"),RELAY("through a relay");
        final String said;
        Way(String said){this.said=said;}
    }

    /** The last send to one device that was taken: which way, and when. */
    record Last(Way way,long at) {}

    /** By identity key, as {@link Direct#key} writes it. At most this many devices, which is more than anybody pairs. */
    private static final Map<String,Last> LAST=new ConcurrentHashMap<>();
    static final int MOST=64;
    /** Kept again at most this often, unless the road changed: a note goes every few seconds while somebody types. */
    static final long KEEP_EVERY=60L*1000;
    private static volatile long keptAt;
    /** Where the record is kept, set by whoever runs the node; nowhere in a test. */
    static volatile java.util.function.Consumer<String> keep;

    /**
     * Whether a paired device is heard on this network now, set by whoever runs the node ({@link Node}); nobody where
     * none runs. Asked when a file's list or sleeve goes, so a device in the same house is given where it can fetch the
     * pieces at this device's door before they are up anywhere else.
     */
    static volatile java.util.function.Predicate<NoteStore.Contact> nearNow=any->false;

    static boolean near(NoteStore.Contact them) {
        try{return them!=null&&nearNow.test(them);}catch(RuntimeException notNow){return false;}
    }

    /** A send taken by the road it went. */
    static void went(String identity,Way way,long at) {
        if(identity==null||identity.trim().isEmpty()||way==null)return;
        String key=keyOf(identity);
        if(LAST.size()>=MOST&&!LAST.containsKey(key))LAST.clear();
        Last was=LAST.put(key,new Last(way,at));
        java.util.function.Consumer<String> to=keep;
        if(to!=null&&(was==null||was.way()!=way||at-keptAt>=KEEP_EVERY||at<keptAt)) {
            keptAt=at;
            try{to.accept(kept());}catch(RuntimeException notNow){/* kept at the next */}
        }
    }

    /** The last send to a device that was taken, or null where none has been since the record began. */
    static Last last(String identity) {
        return identity==null||identity.trim().isEmpty()?null:LAST.get(keyOf(identity));
    }

    private static String keyOf(String identity) {
        try{return Direct.key(identity);}catch(RuntimeException notAKey){return identity.trim();}
    }

    /** The record, one device a line: its key, the way, the time. */
    static String kept() {
        StringBuilder out=new StringBuilder();
        for(Map.Entry<String,Last> one:LAST.entrySet())
            out.append(one.getKey()).append(' ').append(one.getValue().way().name()).append(' ').append(one.getValue().at()).append('\n');
        return out.toString();
    }

    /** The record as {@link #kept} wrote it, taken back; a line that does not read is left out. */
    static void keptWas(String said) {
        LAST.clear();
        if(said==null)return;
        for(String line:said.split("\n")) {
            String[] part=line.trim().split(" ");
            if(part.length!=3||LAST.size()>=MOST)continue;
            try{LAST.put(part[0],new Last(Way.valueOf(part[1]),Long.parseLong(part[2])));}catch(RuntimeException notOne){/* left out */}
        }
    }

    /** Everything let go of: a test, or a notebook that is not this one any more. */
    static void forgetAll(){LAST.clear();keptAt=0;}

    // ---- what People and devices says ------------------------------------------------------------------------

    static final String NEAR="On this Wi-Fi · direct", DOOR_KNOWN="Direct door known", RELAYS="Through relays",
        WAITS="Waits until you are on the same Wi-Fi", QUIET="Not heard lately", NOT_STARTED="Not connected yet";

    /**
     * Where a device is, in a few words: heard on this network, so everything goes to its door; a door it proved in public;
     * heard from lately but only through relays (or, with no helpers, waiting to meet); or not heard lately.
     *
     * @param running whether this device's node runs at all
     * @param near    heard on this network in the last few minutes
     * @param door    a public door it proved, which this device can dial from anywhere
     * @param heard   anything heard from it lately
     * @param helpers whether relays may carry what no door takes
     */
    static String state(boolean running,boolean near,boolean door,boolean heard,boolean helpers) {
        if(!running)return NOT_STARTED;
        if(near)return NEAR;
        if(door)return DOOR_KNOWN;
        if(!heard)return QUIET;
        return helpers?RELAYS:WAITS;
    }

    /** "Last sent: direct, 2 min ago", or empty where nothing has gone to it since the record began. */
    static String lastSent(Last last,long now) {
        return last==null?"":"Last sent: "+last.way().said+", "+ago(now-last.at());
    }

    /**
     * The two said as one line under a device's name, read across its card rather than stacked in a narrow column (decision
     * 97, the owner: "make sure the message uses the width of the window"): "On this Wi-Fi · direct · last sent: direct, 2
     * min ago". Only the first where nothing has gone to it yet.
     */
    static String oneLine(String[] seen) {
        if(seen==null||seen.length==0)return "";
        String first=seen[0]==null?"":seen[0],then=seen.length<2||seen[1]==null?"":seen[1];
        if(then.isEmpty())return first;
        then=then.substring(0,1).toLowerCase(java.util.Locale.ROOT)+then.substring(1);
        return first.isEmpty()?then:first+" · "+then;
    }

    static String ago(long ms) {
        if(ms<60_000L)return "just now";
        long minutes=ms/60_000L;
        if(minutes<60)return minutes+" min ago";
        long hours=minutes/60;
        if(hours<24)return hours+" h ago";
        long days=hours/24;
        return days==1?"yesterday":days+" days ago";
    }

    /**
     * What is said about this device at the top: its door, and how notes travel. On the PC a third line, whether its router
     * proved a public door, comes from {@link Direct#reachability}.
     *
     * @param door  the door's port, or 0 or less where it is not open
     * @param near  how many paired devices are heard on this network now
     */
    static String doorLine(boolean running,int door,int near) {
        if(!running)return "Direct connections: "+NOT_STARTED.toLowerCase(java.util.Locale.ROOT);
        if(door<=0)return "Direct connections: closed, this device could not open its door";
        return "Direct connections: open on port "+door+(near<=0?"":near==1?" · 1 device on this Wi-Fi":" · "+near+" devices on this Wi-Fi");
    }

    static String travelLine(boolean helpers) {
        return helpers?"Notes travel with helpers: direct when they can, through a relay when not"
            :"Notes travel only between my devices: direct, never through a relay";
    }

    // ---- files near first ------------------------------------------------------------------------------------

    /**
     * Whether a file about to go up is first offered from this device's door: with helpers (only between the owner's
     * devices it always is, and nothing goes up), when it has not been offered or gone up yet, when it can travel at all,
     * and when somebody it goes to is on this network now. Then its list or sleeve goes to them at once, saying where its
     * pieces are here, and the upload to the relays follows for everybody else.
     */
    static boolean doorFirst(boolean helpers,boolean offeredOrUp,boolean travels,int nearNow) {
        return helpers&&!offeredOrUp&&travels&&nearNow>0;
    }

    /**
     * Whether a file's manifest, as this device keeps it, says only this device's door: offered to devices on this network
     * and not up anywhere yet. Such a manifest names no source at all, and is kept as not gone up ({@code at} 0), so the
     * round of file work still sends it up for everybody else.
     *
     * @param upAt when it went up, 0 where it is still to
     */
    static boolean doorOnly(String manifest,long upAt) {
        if(upAt!=0||manifest==null||manifest.isEmpty())return false;
        com.eurobuddha.maxima.core.media.MediaManifest said=com.eurobuddha.maxima.core.media.MediaManifest.decode(manifest);
        return said!=null&&said.sources.isEmpty();
    }

    /**
     * What a device is told about where a file is: the manifest, unless it says only this device's door and that device is
     * not on this network, when nothing yet, exactly as before the file went up. A device away from home given a door it
     * cannot reach would fail its fetch and wait out the clock.
     */
    static String manifestFor(String manifest,long upAt,boolean near) {
        if(manifest==null)return "";
        return doorOnly(manifest,upAt)&&!near?"":manifest;
    }
}
