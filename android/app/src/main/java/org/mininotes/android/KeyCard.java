// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * One of the owner's keys for a service, handed to the owner's own devices they choose (the owner, 2026-10-19: "when we set
 * Integritas, we should have the option to share it on all our devices, choosing by device"; decision 210). Sealed in an
 * envelope for that one device, as every card is, and taken in only from a device marked as the owner's own and paired; a
 * key no other device asked for. Which keys can travel is a short list: today the Integritas API key alone. Holds no
 * Android types.
 */
final class KeyCard {
    static final byte[] MAGIC={'M','N','K','1'};
    static final int FORMAT=1;
    static final int NAME_MOST=40,VALUE_MOST=2000;
    /** The keys that may travel, by the name they are kept under (see NoteStore#secret). */
    static final Set<String> TRAVELS=Set.of("integritas");

    private KeyCard(){}

    /** A key, by its name, its value (empty: taken away), and when it was set. */
    static final class Card {
        final String name,value; final long decided;
        Card(String name,String value,long decided){this.name=name==null?"":name;this.value=value==null?"":value;this.decided=decided;}
    }

    static byte[] wrap(Card card) {
        if(!TRAVELS.contains(card.name))throw new IllegalArgumentException("Not a key that travels.");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);
            byte[] name=card.name.getBytes(StandardCharsets.UTF_8),value=card.value.getBytes(StandardCharsets.UTF_8);
            if(name.length>NAME_MOST||value.length>VALUE_MOST)throw new IOException("Too long");
            out.writeShort(name.length);out.write(name);out.writeShort(value.length);out.write(value);out.writeLong(card.decided);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /** What a card says, or null where these bytes are not one, are broken, or name a key that does not travel. */
    static Card open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            int n=in.readUnsignedShort();if(n>NAME_MOST)return null;byte[] name=new byte[n];in.readFully(name);
            int v=in.readUnsignedShort();if(v>VALUE_MOST)return null;byte[] value=new byte[v];in.readFully(value);
            long decided=in.readLong();
            String k=new String(name,StandardCharsets.UTF_8);
            if(!TRAVELS.contains(k)||decided<0||in.available()!=0)return null;
            return new Card(k,new String(value,StandardCharsets.UTF_8),decided);
        } catch(IOException|IllegalArgumentException broken){return null;}
    }
}
