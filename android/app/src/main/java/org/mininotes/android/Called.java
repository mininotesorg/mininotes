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

/**
 * The name we pick for a person or a device, beside the name they share (the owner, 2026-10-07: "we should see the name
 * they share, the name we pick locally or across my devices and we should be able to choose whether to display the one
 * they share or the one we pick for them").
 *
 * <p>What they share stays theirs: a device's own name, or the name a pairing line carried. What we pick is ours. People
 * and devices shows both, and which of the two is the name everywhere else. The name we pick, and which one is shown,
 * goes to all of this owner's devices unless it is kept on this one only. All my devices is the first choice. The later
 * decision stands, so a card that arrives late changes nothing. A choice kept on this device only is never sent, and a
 * card does not overwrite it.
 *
 * <p>What goes to another device of the owner's is a small card: whose it is, by the key their device signs with, the
 * name we pick (empty where it was cleared), whether that name is the one shown, and when that was decided. A card goes
 * only to a device yours at both ends whose build has said it reads them ({@link Receipt#CALLED}): a build from before
 * would read these bytes as a note. A card about somebody the device does not know is dropped, apart from one about
 * this device itself, which is kept in its own settings.
 *
 * <p>Holds no Android types: the words, the format and every decision are unit tested.
 */
final class Called {
    /** The format, and the first four bytes. Not a {@link Carton}, a {@link Courier} or anything else. */
    static final byte[] MAGIC={'M','N','N','1'};
    /** One byte after the magic, so a later build can say more without being read as this one. */
    static final int FORMAT=1;

    static final int NAME_MOST=Persons.NAME_MOST, KEY_MOST=200;

    /** Which name is shown, as the line offers them. Theirs first: it is what stood before a name was picked. */
    static final String SHOW_THEIRS="The name they share", SHOW_OURS="The name we pick";
    /** Where the name we pick is kept. All my devices is first, and it is the choice until somebody says otherwise. */
    static final String ALL_MINE="All my devices", ONLY_HERE="Only this device";

    /** This device's own picked name, in settings: not a row, because this device is not one of its contacts. */
    static final String PREF_PICKED="devicePicked", PREF_SHOW="deviceShowPicked", PREF_HERE="devicePickedHere", PREF_AT="devicePickedAt";

    /** This device as People and devices shows it: the name it shares, the name we pick, which is shown, and whether that stays here. */
    static final class Choice {
        final String shared,picked; final boolean showOurs,onlyHere;
        Choice(String shared,String picked,boolean showOurs,boolean onlyHere) {
            this.shared=shared==null?"":shared;this.picked=picked==null?"":picked;this.showOurs=showOurs;this.onlyHere=onlyHere;
        }
    }

    private Called(){}

    /** One name we pick, as a card says it. Empty where it was cleared. */
    static final class Card {
        final String key,picked; final boolean showPicked; final long decided;
        Card(String key,String picked,boolean showPicked,long decided) {
            this.key=key==null?"":key;this.picked=picked==null?"":picked;this.showPicked=showPicked;this.decided=decided;
        }
    }

    /** A name short enough to keep, or empty. */
    static String clip(String name) {
        String said=name==null?"":name.trim();
        if(said.length()>NAME_MOST)said=said.substring(0,NAME_MOST).trim();
        return said;
    }

    /** The name to show: the one we pick where that was chosen and there is one, otherwise the one they share. */
    static String display(String shared,String picked,boolean showPicked) {
        String ours=clip(picked);
        if(showPicked&&!ours.isEmpty())return ours;
        return shared==null?"":shared.trim();
    }

    /** Whether a decision made at {@code decided} replaces one made at {@code had}: the later stands, and the same is no news. */
    static boolean later(long decided,long had){return decided>had;}

    /**
     * Whether a card goes to a device: one the owner chose, marked as theirs here, whose build has said it reads them,
     * and that has said this device is its owner's too.
     */
    static boolean goesTo(boolean chosen,boolean mineHere,boolean readsThem,boolean mineThere){return chosen&&mineHere&&readsThem&&mineThere;}

    /** What the owner is told when a name is saved: here, and where else it went. */
    static String said(String here,List<String> sent,List<String> notReached,List<String> older) {
        StringBuilder words=new StringBuilder("Saved");
        if(sent.isEmpty()&&notReached.isEmpty()&&older.isEmpty())return words.append(" on ").append(here).toString();
        if(!sent.isEmpty())words.append(", and sent to ").append(Parlons.names(sent));
        else words.append(" on ").append(here);
        if(!notReached.isEmpty())words.append(". ").append(Parlons.names(notReached)).append(" could not be reached now: save again later");
        if(!older.isEmpty())words.append(". ").append(Parlons.names(older)).append(older.size()==1?" needs":" need").append(" an update of Mininotes first");
        return words.toString();
    }

    /** The inside of a card. The key is said in the thirty-three bytes a pairing code carries. */
    static byte[] wrap(Card card) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);
            put(out,Groups.wire(card.key),KEY_MOST);
            put(out,clip(card.picked).getBytes(StandardCharsets.UTF_8),NAME_MOST);
            out.writeByte(card.showPicked?1:0);out.writeLong(card.decided);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    /** Whether these bytes say they are one of these cards, before anything else is asked of them. */
    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /** What a card says, or null where these bytes are not one, or are but broken, or with anything after it. */
    static Card open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            String key=Groups.kept(get(in,KEY_MOST));
            String picked=clip(new String(get(in,NAME_MOST),StandardCharsets.UTF_8));
            int show=in.readUnsignedByte();
            long decided=in.readLong();
            if(key.isEmpty()||decided<=0||show>1||in.available()!=0)return null;
            return new Card(key,picked,show==1,decided);
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
