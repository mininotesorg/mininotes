// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The stamps on Minima, the same on every device of the owner's (the owner, 2026-10-25: "I horodated [a spreadsheet] from the pro, this is not visible on the laptop, can you make sure it updates"; decision 252). A stamp was
 * kept only on the device that made it; what travels now is this card, every stamp kept on the device that sends it, with
 * all that is needed to list it and verify it again: what was stamped (its id, kind and name), its fingerprint and when,
 * the request Integritas was sent, what it said and the Minima address, how far it is, where on Minima it landed, the NFT
 * of a verification, and for a note the exact title and text that were fingerprinted. How often and when it was last looked
 * for stay on each device.
 *
 * <p>A stamp is known on both ends by its request, a random id made for each; one copied from before 0.4.010 may have none,
 * and is known by what was stamped, its fingerprint and when (see NoteStore#stampsArrived). Nothing is ever taken away, and
 * nothing found on Minima is said to be waiting again.
 *
 * <p>Sealed in an envelope as every card is, only for a device marked as yours at both ends whose build has said it reads one
 * ({@link Receipt#STAMPS}): a build from before would read these bytes as a note, and nobody else has any business knowing
 * what you stamped. Where every stamp does not fit one envelope, the newest go, a note's text left off one that would not fit
 * with it. Holds no Android types: the format and its bounds are unit tested.
 */
final class StampCard {
    /** The format, and the first four bytes. Not a {@link Groups} card, a {@link KeyCard} or anything else. */
    static final byte[] MAGIC={'M','N','Q','1'};
    /** One byte after the magic, so a later build can say more without being read as this one. */
    static final int FORMAT=1;

    /** More stamps than anybody makes, and a bound a stranger's count cannot pass. */
    static final int ROWS_MOST=20000;
    static final int ID_MOST=200, KIND_MOST=40, NAME_MOST=2000, HASH_MOST=200, REQUEST_MOST=200, SAID_MOST=4000,
        ADDRESS_MOST=400, STATE_MOST=40, CHAIN_MOST=400, TITLE_MOST=4000;
    /** How large a card may be: what one envelope takes. */
    static final int CARD_MOST=Envelope.MAX_TEXT;
    /** How far a stamp can be, as NoteStore keeps it; a card saying anything else is not read. */
    static final Set<String> STATES=java.util.Collections.unmodifiableSet(new java.util.HashSet<>(java.util.Arrays.asList(
        NoteStore.STAMP_WAITING,NoteStore.STAMP_CONFIRMED,NoteStore.STAMP_UNSEEN)));

    private StampCard(){}

    /**
     * One stamp as a card carries it. {@code title} and {@code words} are a note's stamped version, null where none was kept
     * or it did not fit; {@code nft} and {@code nftTx} are empty until a verification minted one.
     */
    static final class Row {
        final String id,kind,name,hash,request,said,address,state,txpow,tx,recorded,title,words,nft,nftTx;
        final long at,block;
        Row(String id,String kind,String name,String hash,long at,String request,String said,String address,String state,
            long block,String txpow,String tx,String recorded,String title,String words,String nft,String nftTx) {
            this.id=text(id);this.kind=text(kind);this.name=text(name);this.hash=text(hash);this.at=at;this.request=text(request);
            this.said=text(said);this.address=text(address);this.state=text(state);this.block=block;this.txpow=text(txpow);
            this.tx=text(tx);this.recorded=text(recorded);this.words=words;this.title=words==null?null:text(title);
            this.nft=text(nft);this.nftTx=text(nftTx);
        }
        /** Found on Minima, as NoteStore.StampRow#confirmed says it. */
        boolean confirmed(){return block>0||NoteStore.STAMP_CONFIRMED.equals(state);}
        /** The same stamp without its note's text, for a card it would not fit with it. */
        Row bare(){return new Row(id,kind,name,hash,at,request,said,address,state,block,txpow,tx,recorded,null,null,nft,nftTx);}
        private static String text(String said){return said==null?"":said;}
    }

    /**
     * Whether a card goes to a device: one marked as yours here, whose build has said it reads stamps, and that has said this
     * device is its owner's too, as a groups card goes (see {@link Groups#goesTo}).
     */
    static boolean goesTo(boolean mineHere,boolean readsStamps,boolean mineThere){return mineHere&&readsStamps&&mineThere;}

    /** The newest stamp on it, which rides where a revision goes. */
    static long newest(List<Row> rows) {
        long newest=0;
        for(Row one:rows)newest=Math.max(newest,Math.max(0,one.at));
        return newest;
    }

    /**
     * The inside of a card: the rows in the order given (the newest first), as many as fit {@link #CARD_MOST}; one that would
     * not fit with its note's text goes without it, and the first that does not fit even so ends the card. A row this format
     * cannot say (a field longer than its bound, or no id, kind or fingerprint) is left off.
     */
    static byte[] wrap(List<Row> rows) {
        ByteArrayOutputStream body=new ByteArrayOutputStream();
        int count=0,room=CARD_MOST-MAGIC.length-1-4;
        for(Row one:rows) {
            if(count>=ROWS_MOST)break;
            byte[] row=row(one);
            if((row==null||row.length>room)&&one.words!=null)row=row(one.bare());
            if(row==null)continue;
            if(row.length>room)break;
            body.write(row,0,row.length);room-=row.length;count++;
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);out.writeInt(count);body.writeTo(out);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    /** How many stamps a card holds, without reading them; 0 where these bytes are not one. */
    static int count(byte[] said) {
        if(!isCard(said)||said.length<MAGIC.length+5)return 0;
        int at=MAGIC.length+1;
        return (said[at]&0xff)<<24|(said[at+1]&0xff)<<16|(said[at+2]&0xff)<<8|(said[at+3]&0xff);
    }

    /** One row's bytes, or null where this format cannot say it. */
    private static byte[] row(Row one) {
        if(one.id.isEmpty()||one.kind.isEmpty()||one.hash.isEmpty()||one.at<0||one.block<0)return null;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            put(out,one.id,ID_MOST);put(out,one.kind,KIND_MOST);put(out,one.name,NAME_MOST);put(out,one.hash,HASH_MOST);
            out.writeLong(one.at);
            put(out,one.request,REQUEST_MOST);put(out,one.said,SAID_MOST);put(out,one.address,ADDRESS_MOST);put(out,one.state,STATE_MOST);
            out.writeLong(one.block);
            put(out,one.txpow,CHAIN_MOST);put(out,one.tx,CHAIN_MOST);put(out,one.recorded,CHAIN_MOST);
            put(out,one.nft,CHAIN_MOST);put(out,one.nftTx,CHAIN_MOST);
            if(one.words==null)out.writeByte(0);
            else {
                out.writeByte(1);put(out,one.title,TITLE_MOST);
                byte[] words=one.words.getBytes(StandardCharsets.UTF_8);
                if(words.length>CARD_MOST)return null;
                out.writeInt(words.length);out.write(words);
            }
        } catch(IOException tooLong){return null;}
        return bytes.toByteArray();
    }

    /** Whether these bytes say they are a stamps card, before anything else is asked of them. */
    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /**
     * What a card says, or null where these bytes are not one, or are but broken, out of bounds, with a stamp missing what
     * every stamp has or saying a state this build does not know, or with anything after the last row. Half a card is not one.
     */
    static List<Row> open(byte[] said) {
        if(!isCard(said)||said.length>CARD_MOST)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            int count=in.readInt();
            if(count<0||count>ROWS_MOST)return null;
            List<Row> rows=new ArrayList<>(Math.min(count,1000));
            for(int at=0;at<count;at++) {
                String id=get(in,ID_MOST),kind=get(in,KIND_MOST),name=get(in,NAME_MOST),hash=get(in,HASH_MOST);
                long when=in.readLong();
                String request=get(in,REQUEST_MOST),says=get(in,SAID_MOST),address=get(in,ADDRESS_MOST),state=get(in,STATE_MOST);
                long block=in.readLong();
                String txpow=get(in,CHAIN_MOST),tx=get(in,CHAIN_MOST),recorded=get(in,CHAIN_MOST),nft=get(in,CHAIN_MOST),nftTx=get(in,CHAIN_MOST);
                int kept=in.readUnsignedByte();
                String title=null,words=null;
                if(kept>1)return null;
                if(kept==1) {
                    title=get(in,TITLE_MOST);
                    int length=in.readInt();
                    if(length<0||length>in.available())return null;
                    byte[] text=new byte[length];in.readFully(text);words=new String(text,StandardCharsets.UTF_8);
                }
                if(id.isEmpty()||kind.isEmpty()||hash.isEmpty()||when<0||block<0||!STATES.contains(state))return null;
                rows.add(new Row(id,kind,name,hash,when,request,says,address,state,block,txpow,tx,recorded,title,words,nft,nftTx));
            }
            if(in.available()!=0)return null;
            return rows;
        } catch(IOException|IllegalArgumentException broken){return null;}
    }

    private static void put(DataOutputStream out,String said,int most) throws IOException {
        byte[] bytes=said.getBytes(StandardCharsets.UTF_8);
        if(bytes.length>most)throw new IOException("Too long to send: "+bytes.length+" of "+most);
        out.writeShort(bytes.length);out.write(bytes);
    }

    private static String get(DataInputStream in,int most) throws IOException {
        int length=in.readUnsignedShort();
        if(length>most)throw new IllegalArgumentException("A field said it was "+length+" long");
        byte[] bytes=new byte[length];in.readFully(bytes);
        return new String(bytes,StandardCharsets.UTF_8);
    }
}
