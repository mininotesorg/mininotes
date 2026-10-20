package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A person's own picture, as everybody paired with them sees it on their round (the owner, 2026-10-12: "for the contacts we
 * should be able to define a picture for them, or in the Profile set a picture, and when we click a contact we should see
 * their picture in big"; decision 158). Set in Profile, it goes to every device paired, as their own Parlons! address does
 * (decision 147): one card, about the device that sends it, the later decision standing. A picture somebody picks for a
 * contact on their own device is theirs alone, and is worn over the one the contact gave.
 *
 * <p>Sealed in an envelope as any card is; its first four bytes say what it is, so a build from before, which has never
 * said it takes one, is never sent one. A picture is a {@link Thumb}: small, square, and no more than {@link Thumb#MOST}.
 * Holds no Android types.
 */
final class Face {
    static final byte[] MAGIC={'M','N','Y','1'};
    static final int FORMAT=1;
    static final int KEY_MOST=200;

    private Face(){}

    /** One person's own picture, as a card says it: empty where it was taken off. */
    static final class Card {
        final String key; final byte[] picture; final long decided;
        Card(String key,byte[] picture,long decided){this.key=key==null?"":key;this.picture=picture==null?new byte[0]:picture;this.decided=decided;}
        boolean removed(){return picture.length==0;}
    }

    static byte[] wrap(Card card) {
        if(!card.removed()&&!Thumb.takes(card.picture))throw new IllegalArgumentException("Not a picture that can travel.");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);
            byte[] key=Groups.wire(card.key);
            if(key.length>KEY_MOST)throw new IOException("Too long a key");
            out.writeShort(key.length);out.write(key);
            out.writeInt(card.picture.length);out.write(card.picture);
            out.writeLong(card.decided);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /** What a card says, or null where these bytes are not one, or are but broken, too big, or not a picture. */
    static Card open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            int keyLength=in.readUnsignedShort();if(keyLength>KEY_MOST)return null;
            byte[] key=new byte[keyLength];in.readFully(key);
            int length=in.readInt();if(length<0||length>Thumb.MOST)return null;
            byte[] picture=new byte[length];in.readFully(picture);
            long decided=in.readLong();
            String said2=Groups.kept(key);
            if(said2.isEmpty()||decided<=0||in.available()!=0)return null;
            if(length>0&&!Thumb.takes(picture))return null;
            return new Card(said2,picture,decided);
        } catch(IOException|IllegalArgumentException broken){return null;}
    }
}
