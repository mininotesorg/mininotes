// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A note carried by a third device, for when the two it is between are never on at the same time.
 *
 * <p>Nothing in Maxima waits. A relay passes a message to whoever is there and keeps nothing, so a note
 * written on the PC just before it was shut, for a phone that was out of signal then, reached the phone
 * only the next time both were on together. But there is usually a third device that is on - the other
 * phone, in a pocket, listening. So the note is also left with it, and it hands the note over when the
 * phone that was away is back.
 *
 * <p>What is left is the note exactly as it was sealed for the phone it is going to: the device carrying
 * it cannot open it, cannot change it without that being seen, and cannot make it look as if it came from
 * anybody else. It knows who it is from, who it is for, which note (by its id) and how big - no word of it.
 * The phone it is for opens it as if it had come straight, and answers the phone that wrote it; that
 * answer, which the writer may now be too far away to hear, is carried back the same way.
 *
 * <p>Three messages, all inside an ordinary {@link Envelope} between two paired devices:
 * <ul>
 * <li><b>Leave</b>, to the carrier: who it is for, by the fingerprint of the key they sign with, and the
 *     sealed note or answer;</li>
 * <li><b>Bring</b>, from the carrier to whoever it is for: the sealed thing itself;</li>
 * <li>and a {@link Receipt#COLLECTED} back to the carrier, so it can let go of what was brought.</li>
 * </ul>
 * A later note from the same device for the same note replaces an earlier one, so a carrier holds one
 * copy of each thing however often it was left, and nothing is kept longer than {@link #KEPT_FOR}.
 *
 * <p>Holds no Android types: the format, its bounds and the waiting are unit tested.
 */
final class Courier {
    /** The format, and the first four bytes. Never sent to a device that has not said it knows it. */
    static final byte[] MAGIC={'M','N','C','1'};

    static final int LEAVE=1, BRING=2;
    /** What was left, so that a newer one of the same kind replaces it and another kind does not. */
    static final int NOTE=1, ANSWER=2;

    /** How long a carrier keeps something nobody came for. A phone away for a month is starting again anyway. */
    static final long KEPT_FOR=30L*24*60*60*1000;
    /** The most a carrier keeps for others at once, together. Text is small; this is a cap on a fault. */
    static final int MOST_KEPT=500;
    static final long MOST_BYTES=16L*1024*1024;
    /** The most carriers one thing is left with. */
    static final int CARRIERS=3;
    /** A device heard from this recently is taken to be there: nothing sent to it is left with anybody. */
    static final long THERE=2L*60*1000;
    /**
     * How long the same revision of a thing is not left again for the same device: a carrier keeps it and brings it till it
     * is collected, so each try at sending it again need not leave another copy (decision 257).
     */
    static final long LEAVE_AGAIN=60L*60*1000;
    /** How long a carrier is not told again that the same revision of a thing was collected: see {@link Lately}. */
    static final long COLLECTED_AGAIN=10L*60*1000;

    /** What {@link NoteStore#carry} did with what was left: kept now, already held at that revision or newer, or no room. */
    static final int CARRYING=1, ALREADY=0, NO_ROOM=-1;

    /** Room left inside an envelope once this format's own few bytes are counted. */
    static final int INNER_MOST=Envelope.MAX_TEXT-64;
    private static final int FINGERPRINT=32;

    /** One message of this format, read. */
    static final class Said {
        final int kind, sort;
        /** Who it is for: the SHA-256 of the key they sign with. Empty in a Bring. */
        final byte[] forWhom;
        /** The sealed note or answer, as it was sealed for them. */
        final byte[] inner;
        Said(int kind,int sort,byte[] forWhom,byte[] inner){this.kind=kind;this.sort=sort;this.forWhom=forWhom;this.inner=inner;}
    }

    private Courier(){}

    /** Something for {@code forWhom}, left with a carrier. */
    static byte[] leave(int sort,byte[] forWhom,byte[] inner) {
        if(forWhom==null||forWhom.length!=FINGERPRINT)throw new IllegalArgumentException("A fingerprint is 32 bytes");
        if(sort!=NOTE&&sort!=ANSWER)throw new IllegalArgumentException("Nothing of that kind is carried");
        return write(LEAVE,sort,forWhom,inner);
    }

    /** What was left, brought to whoever it is for. */
    static byte[] bring(int sort,byte[] inner){return write(BRING,sort,new byte[0],inner);}

    /** Whether something this size can be carried at all. A note near the largest there is cannot. */
    static boolean fits(byte[] inner){return inner!=null&&inner.length>0&&inner.length<=INNER_MOST;}

    private static byte[] write(int kind,int sort,byte[] forWhom,byte[] inner) {
        if(!fits(inner))throw new IllegalArgumentException("Too large to carry");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(kind);out.writeByte(sort);
            out.writeByte(forWhom.length);out.write(forWhom);
            out.writeInt(inner.length);out.write(inner);
        } catch(IOException e){throw new IllegalStateException("Cannot write in memory",e);}
        return bytes.toByteArray();
    }

    /** What these bytes say, or null when they are not this format - or are, but broken. */
    static Said open(byte[] said) {
        if(said==null||said.length<MAGIC.length+7)return null;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return null;
        try(DataInputStream in=new DataInputStream(new java.io.ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            int kind=in.readUnsignedByte(),sort=in.readUnsignedByte();
            if(kind!=LEAVE&&kind!=BRING)return null;
            if(sort!=NOTE&&sort!=ANSWER)return null;
            int whom=in.readUnsignedByte();
            if(kind==LEAVE?whom!=FINGERPRINT:whom!=0)return null;
            byte[] forWhom=new byte[whom];in.readFully(forWhom);
            int length=in.readInt();
            if(length<=0||length>INNER_MOST||length!=in.available())return null;
            byte[] inner=new byte[length];in.readFully(inner);
            return new Said(kind,sort,forWhom,inner);
        } catch(IOException broken){return null;}
    }

    /**
     * Whether something held for somebody should be brought again now. Relays take a message for a phone
     * that is not there and say yes, so nothing short of their {@link Receipt#COLLECTED} ends it: after a
     * minute, two, four, eight, and then every ten for as long as it is kept.
     */
    static boolean due(long tried,int tries,long now) {
        if(tries<=0)return true;
        if(now<tried)return true;   // a clock put back: better once too often than never again
        long wait=tries>=5?10L*60*1000:(1L<<(tries-1))*60*1000;
        return now-tried>=wait;
    }

    /**
     * What was said lately, by a key, with the revision and when, so the same is not said again within a while (decision
     * 257): a copy left with the carriers for one device, or a carrier told it can let go. Taken as it is queued and given
     * back where it did not go, so ten copies arriving together are answered once. Holds no Android types: unit tested.
     */
    static final class Lately {
        private final long within;
        private final java.util.Map<String,long[]> said=new java.util.HashMap<>();
        Lately(long within){this.within=within;}

        /** Whether this revision is to be said now; where it is, it counts as said from now on. */
        synchronized boolean take(String key,long revision,long now) {
            long[] was=said.get(key);
            if(was!=null&&was[0]>=revision&&now>=was[1]&&now-was[1]<within)return false;
            said.put(key,new long[]{revision,now});
            return true;
        }

        /** It did not go after all: the next one says it. */
        synchronized void giveBack(String key,long revision) {
            long[] was=said.get(key);
            if(was!=null&&was[0]==revision)said.remove(key);
        }
    }

    /**
     * Work queued, one at a time for each key, the newest revision kept: a receipt for a carrier waits once however many
     * copies come before it goes, and goes for the newest revision brought meanwhile. Each copy used to queue its own,
     * holding a thread's queue that grew until the heap was full, each a proof of work (Graphene, 2026-10-25, decision 257).
     * Holds no Android types: unit tested.
     */
    static final class Queued {
        private final java.util.Map<String,Long> waiting=new java.util.HashMap<>();

        /** Whether work is to be queued for this key now: false where one waits already, which then goes for this revision if newer. */
        synchronized boolean queue(String key,long revision) {
            Long was=waiting.get(key);
            waiting.put(key,was==null?revision:Math.max(was,revision));
            return was==null;
        }

        /** The revision the queued work is for, now that it runs: the newest queued, or {@code otherwise} where none is. */
        synchronized long run(String key,long otherwise) {
            Long was=waiting.remove(key);
            return was==null?otherwise:was;
        }

        synchronized int size(){return waiting.size();}
    }

    /** Whether something kept since {@code kept} has been kept long enough. */
    static boolean expired(long kept,long now){return now-kept>KEPT_FOR;}

    /** The sixteen bytes an envelope names a note by, as a carrier files it. */
    static String hex(byte[] bytes) {
        StringBuilder out=new StringBuilder(bytes.length*2);
        for(byte one:bytes)out.append(Character.forDigit((one>>4)&15,16)).append(Character.forDigit(one&15,16));
        return out.toString();
    }
}
