// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Who a device belongs to, told to everybody paired with it (decision 163: "one person, several devices"; the owner,
 * 2026-10-17: "go ahead with the about me"): the person id its owner's devices share (see {@link Persons}) and when it was
 * made. A contact who has two of Ana's devices then sees one Ana, with two devices, on a note's line and in its lists.
 *
 * <p>Sealed in an envelope as any card is, and about the device that sends it only; its first four bytes say what it is,
 * so a build from before, which has never said it reads one ({@link Receipt#ABOUT_ME}), is never sent one. It says nothing
 * a contact could not already tell by putting two names side by side: how many devices a person has, and that they are one.
 *
 * <p>Its four bytes are a {@link Persons} card's too, and a reader that asked Persons first took every one of these for a
 * broken card of persons. The bytes stay as they are, so the devices already sending one are read; what tells the two
 * apart is reading it whole with {@link #open}, before Persons is asked (decision 252). Holds no Android types.
 */
final class AboutMe {
    static final byte[] MAGIC={'M','N','A','1'};
    static final int FORMAT=1;
    static final int KEY_MOST=200;

    private AboutMe(){}

    /** One device's owner, as its card says it: the key the device signs with, its owner's id, and when that id was made. */
    static final class Card {
        final String key,person; final long made;
        Card(String key,String person,long made){this.key=key==null?"":key;this.person=person==null?"":person;this.made=made;}
    }

    static byte[] wrap(Card card) {
        if(!Persons.isId(card.person))throw new IllegalArgumentException("Not a person's id.");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);
            byte[] key=Groups.wire(card.key);
            if(key.length>KEY_MOST)throw new IOException("Too long a key");
            out.writeShort(key.length);out.write(key);
            byte[] person=card.person.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.writeShort(person.length);out.write(person);
            out.writeLong(card.made);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /** What a card says, or null where these bytes are not one, or are but broken. */
    static Card open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            int keyLength=in.readUnsignedShort();if(keyLength>KEY_MOST)return null;
            byte[] key=new byte[keyLength];in.readFully(key);
            int personLength=in.readUnsignedShort();if(personLength>KEY_MOST)return null;
            byte[] person=new byte[personLength];in.readFully(person);
            long made=in.readLong();
            String k=Groups.kept(key),p=new String(person,java.nio.charset.StandardCharsets.UTF_8);
            if(k.isEmpty()||!Persons.isId(p)||made<0||in.available()!=0)return null;
            return new Card(k,p,made);
        } catch(IOException|IllegalArgumentException broken){return null;}
    }
}
