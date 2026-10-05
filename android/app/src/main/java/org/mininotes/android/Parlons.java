// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A person's Parlons! address (the owner, 2026-10-05: "In People and devices, we should be able to add people's Parlons!
 * profile link, and from the note app we should be able to contact them directly in Parlons!"; docs/HOME.md, decision 101).
 *
 * <p>Parlons! is a Maxima chat app. Its address is {@code Mx} and letters, an {@code @}, and where its home is:
 * {@code MxG18HGG…@78.141.237.9:9501}. The app opens on nothing but its own launcher, no link and no shared text, so
 * contacting somebody in it is this: their address copied, Parlons! opened, and a word saying to paste it there.
 *
 * <p>An address is kept on the person's card here, with when it was decided. When it is set the owner chooses where it
 * goes: only this device, all of theirs, or the ones they pick. What goes to another device of the owner's is a small
 * card: whose it is, by the key their device signs with (an address moves, a key does not), the Parlons! address, empty
 * where it was removed, and when that was decided. The later decision stands, so a card that arrives late changes
 * nothing. A card goes only to a device yours at both ends whose build has said it reads them ({@link Receipt#PARLONS}):
 * a build from before would read these bytes as a note. A card about somebody the device does not know is dropped.
 *
 * <p>Holds no Android types: the address, the format and every decision are unit tested.
 */
final class Parlons {
    /** The format, and the first four bytes. Not a {@link Groups} card, a {@link Parcel} or anything else. */
    static final byte[] MAGIC={'M','N','P','1'};
    /** One byte after the magic, so a later build can say more without being read as this one. */
    static final int FORMAT=1;

    /** The app, as Android knows it, and where it is had from where it is not installed. */
    static final String PACKAGE="com.eurobuddha.maxima.app";
    static final String RELEASES="https://github.com/eurobuddha/maxima/releases/latest";

    /** Longer than any address, and short enough that a card is always small. */
    static final int ADDRESS_MOST=400, KEY_MOST=200;

    /** The three places an address can go, as the box offers them. */
    static final String ONLY_HERE="Only this device", ALL_MINE="All my devices", CHOOSE="Choose devices…";

    /** The button that contacts them, and the one that asks for their address where there is none yet. */
    static final String CONTACT="Contact on Parlons!", ADD="Add their Parlons! address…";

    private Parlons(){}

    /** One person's Parlons! address, as a card says it: empty where it was removed. */
    static final class Card {
        final String key,address; final long decided;
        Card(String key,String address,long decided){this.key=key==null?"":key;this.address=address==null?"":address;this.decided=decided;}
    }

    // ---- the address ----------------------------------------------------------------------------------------

    /** An address inside whatever was pasted: Mx, then anything but a space up to an @, then anything but a space. */
    private static final Pattern INSIDE=Pattern.compile("Mx[^\\s@]+@[^\\s]+");

    /**
     * An address as it is kept, from what was typed or pasted: trimmed, and found inside a line that carries more than
     * the address (a message from Parlons! with it in, say). Empty for nothing, which is no address. Loosely checked, as
     * the app itself may change its addresses: it starts with Mx and has an @ in it.
     */
    static String address(String said) {
        String text=said==null?"":said.trim();
        if(text.isEmpty())return "";
        String found=text;
        if(!(text.startsWith("Mx")&&text.indexOf('@')>2&&!text.matches(".*\\s.*"))) {
            Matcher inside=INSIDE.matcher(text);
            if(!inside.find())throw new IllegalArgumentException("That is not a Parlons! address. One starts with Mx and has an @ in it, like MxG18HGG…@78.141.237.9:9501.");
            found=inside.group();
        }
        // A sentence's full stop, or the bracket or quote round it, is not part of it.
        while(!found.isEmpty()&&".,;:)]}>\"'”’".indexOf(found.charAt(found.length()-1))>=0)found=found.substring(0,found.length()-1);
        if(found.endsWith("@")||found.indexOf('@')<=2)throw new IllegalArgumentException("That Parlons! address is cut short. Copy the whole of it from Parlons!.");
        if(found.getBytes(StandardCharsets.UTF_8).length>ADDRESS_MOST)throw new IllegalArgumentException("That is too long for a Parlons! address.");
        return found;
    }

    /** An address as a card's line shows it: its first eight letters, or Not set. */
    static String shortened(String address) {
        if(address==null||address.isEmpty())return "Not set";
        return address.length()<=8?address:address.substring(0,8)+"…";
    }

    /** Whether a decision made at {@code decided} replaces one made at {@code had}: the later stands, and the same is no news. */
    static boolean later(long decided,long had){return decided>had;}

    /**
     * Whether a card goes to a device: one the owner chose, marked as theirs here, whose build has said it reads them,
     * and that has said this device is its owner's too. Anybody else's device never learns who you talk to.
     */
    static boolean goesTo(boolean chosen,boolean mineHere,boolean readsThem,boolean mineThere){return chosen&&mineHere&&readsThem&&mineThere;}

    /**
     * What the owner is told when an address is saved or removed: here, and where else it went, by the names of their
     * devices, and why any did not have it: out of reach now, or a Mininotes from before.
     */
    static String said(boolean removed,String here,List<String> sent,List<String> notReached,List<String> older) {
        StringBuilder words=new StringBuilder(removed?"Removed":"Saved");
        if(sent.isEmpty()&&notReached.isEmpty()&&older.isEmpty())return words.append(" on ").append(here).toString();
        if(!sent.isEmpty())words.append(removed?" here, and on ":", and sent to ").append(names(sent));
        else words.append(" on ").append(here);
        if(!notReached.isEmpty())words.append(". ").append(names(notReached)).append(" could not be reached now: save again later");
        if(!older.isEmpty())words.append(". ").append(names(older)).append(older.size()==1?" needs":" need").append(" an update of Mininotes first");
        return words.toString();
    }

    /** "Pro", "Pro and Laptop", "Pro, Tablet and Laptop". */
    static String names(List<String> all) {
        if(all.size()==1)return all.get(0);
        return String.join(", ",all.subList(0,all.size()-1))+" and "+all.get(all.size()-1);
    }

    // ---- the card -------------------------------------------------------------------------------------------

    /** The inside of a card. The key is said in the thirty-three bytes a pairing code carries, as a groups card says it. */
    static byte[] wrap(Card card) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);
            put(out,Groups.wire(card.key),KEY_MOST);put(out,card.address.getBytes(StandardCharsets.UTF_8),ADDRESS_MOST);
            out.writeLong(card.decided);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    /** Whether these bytes say they are a Parlons! card, before anything else is asked of them. */
    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /**
     * What a card says, or null where these bytes are not one, or are but broken, out of bounds, carrying something that
     * is not an address, or with anything after it. Half a card is not a card.
     */
    static Card open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            String key=Groups.kept(get(in,KEY_MOST));
            String written=new String(get(in,ADDRESS_MOST),StandardCharsets.UTF_8);
            long decided=in.readLong();
            if(key.isEmpty()||decided<=0||in.available()!=0)return null;
            String address=address(written);
            if(!address.equals(written))return null;
            return new Card(key,address,decided);
        } catch(IOException|IllegalArgumentException broken){return null;}
    }

    private static void put(DataOutputStream out,byte[] bytes,int most) throws IOException {
        if(bytes.length>most)throw new IOException("Too long to send: "+bytes.length+" of "+most);
        out.writeShort(bytes.length);out.write(bytes);
    }

    private static byte[] get(DataInputStream in,int most) throws IOException {
        int length=in.readUnsignedShort();
        if(length>most)throw new IllegalArgumentException("A field said it was "+length+" long");
        byte[] bytes=new byte[length];in.readFully(bytes);
        return bytes;
    }
}
