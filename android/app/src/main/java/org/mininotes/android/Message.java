// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A brief message to somebody paired here, for the Inbox (the owner, 2026-10-10: "we should even have an inbox in the app,
 * so that people can send brief messages, like please send me your Parlons! address"; decision 145).
 *
 * <p>It rides as a card of its own, like a {@link Help} request: four bytes that say what it is, read before a note is, so
 * it is never taken for somebody's words. It is sealed and signed like everything else, and sealed only for a device that
 * has said it reads one ({@link Receipt#MESSAGES}): a build from before would write it over a note. The envelope's page is
 * the message's own sixteen-byte id, so the answer that it was kept ({@link Receipt#MESSAGE_KEPT}) can say which one.
 *
 * <p>Three kinds: words; a request for their Parlons! address; and the answer to one, which carries it. An address that
 * arrives is offered to be kept on their card, never kept without a yes.
 *
 * <p>Holds no Android types: the format is unit tested.
 */
final class Message {
    /** The format, and the first four bytes. Not a {@link Help} card, a {@link Parlons} card or anything else. */
    static final byte[] MAGIC={'M','N','M','1'};
    /** One byte after the magic, so a later build can say more without being read as this one. */
    static final int FORMAT=1;
    /** Words; "please send me your Parlons! address"; and the answer, with the address. */
    static final int TEXT=1, ASK_PARLONS=2, PARLONS=3;
    /** A brief message, not a letter: as long as a help request's words. */
    static final int MOST_WORDS=280;
    /** Sixteen bytes name a message, so its answer and a second arrival of it are known for the same one. */
    static final int ID=16;

    /** What the place is called, on Home and in menus, on both apps. */
    static final String PLACE="Inbox";

    /** The words said for the two kinds that have none of their own, the same on both apps. */
    static final String ASKS="Please send me your Parlons! address.", SENDS="My Parlons! address";

    /** What a conversation says while their Mininotes has not answered that it reads messages, the same on both apps. */
    static String waiting(String name){return "Waiting for "+name+"'s Mininotes to answer. Anything you send waits here and goes as soon as it does. A Mininotes older than 0.3.035 needs updating.";}

    private Message(){}

    /** One message as a card says it. */
    static final class Card {
        final byte[] id; final int kind; final String words,parlons; final long at;
        Card(byte[] id,int kind,String words,String parlons,long at) {
            this.id=id;this.kind=kind;this.words=words==null?"":words;this.parlons=parlons==null?"":parlons;this.at=at;
        }
    }

    /** What a message says on a line: its words, or what its kind says. */
    static String said(int kind,String words) {
        if(words!=null&&!words.trim().isEmpty())return words.trim();
        return kind==ASK_PARLONS?ASKS:kind==PARLONS?SENDS:"";
    }

    /** The inside of a card. */
    static byte[] wrap(Card c) {
        if(c.id==null||c.id.length!=ID)throw new IllegalArgumentException("A message needs a sixteen-byte id.");
        if(c.kind!=TEXT&&c.kind!=ASK_PARLONS&&c.kind!=PARLONS)throw new IllegalArgumentException("No such kind of message.");
        String words=c.words.trim();if(words.length()>MOST_WORDS)words=words.substring(0,MOST_WORDS);
        String parlons=c.kind==PARLONS?c.parlons.trim():"";
        if(c.kind==TEXT&&words.isEmpty())throw new IllegalArgumentException("A message needs words.");
        if(c.kind==PARLONS&&parlons.isEmpty())throw new IllegalArgumentException("An address needs an address.");
        if(parlons.length()>Parlons.ADDRESS_MOST)throw new IllegalArgumentException("That address is too long.");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);out.write(c.id);out.writeByte(c.kind);
            byte[] text=words.getBytes(StandardCharsets.UTF_8);out.writeShort(text.length);out.write(text);
            byte[] address=parlons.getBytes(StandardCharsets.UTF_8);out.writeShort(address.length);out.write(address);
            out.writeLong(c.at);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    /** Whether these bytes say they are a message, before anything else is asked of them. */
    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /** What a card says, or null where these bytes are not one, or are but broken, out of bounds, or with anything after. */
    static Card open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            byte[] id=new byte[ID];in.readFully(id);
            int kind=in.readUnsignedByte();
            if(kind!=TEXT&&kind!=ASK_PARLONS&&kind!=PARLONS)return null;
            int length=in.readUnsignedShort();if(length>MOST_WORDS*4)return null;
            byte[] text=new byte[length];in.readFully(text);
            int place=in.readUnsignedShort();if(place>Parlons.ADDRESS_MOST*4)return null;
            byte[] address=new byte[place];in.readFully(address);
            long at=in.readLong();
            if(at<=0||in.available()!=0)return null;
            String words=new String(text,StandardCharsets.UTF_8),parlons=new String(address,StandardCharsets.UTF_8);
            if(words.length()>MOST_WORDS)words=words.substring(0,MOST_WORDS);
            if(kind==TEXT&&words.trim().isEmpty())return null;
            if(kind==PARLONS&&parlons.trim().isEmpty())return null;
            return new Card(id,kind,words,kind==PARLONS?parlons:"",at);
        } catch(IOException broken){return null;}
    }

    /** A message id as the notebook keeps it. */
    static String hex(byte[] id) {
        StringBuilder out=new StringBuilder();for(byte b:id)out.append(String.format("%02x",b&0xff));return out.toString();
    }

    /** And back, or null where it is not one. */
    static byte[] unhex(String id) {
        if(id==null||id.length()!=ID*2)return null;
        byte[] out=new byte[ID];
        try{for(int i=0;i<ID;i++)out[i]=(byte)Integer.parseInt(id.substring(i*2,i*2+2),16);}catch(NumberFormatException no){return null;}
        return out;
    }

    /** A new message id. */
    static byte[] newId(){byte[] id=new byte[ID];new java.security.SecureRandom().nextBytes(id);return id;}
}
