// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A file travelling on its own, shared like a note (the owner, 2026-10-04; docs/HOME.md, decision 92).
 *
 * <p>A file kept loose on Home or in a collection went nowhere by itself: it travelled only inside the collection that
 * kept it, to everybody that collection reached. This is what it goes in when it is given to somebody on its own: its
 * id, which is the same on every device, its name, kind and size, where its pieces are once it has gone up (the few
 * hundred bytes an {@link Enclosure.Listed} carries, never the file), what the far end may do with it, who has it, and
 * whether it is gone for everybody. The bytes are fetched as a note's files are.
 *
 * <p>Sealed and answered like a note, under the file's own sixteen bytes and its own revision. Only ever sent to a device
 * that has said {@link Receipt#LOOSE}: a build from before takes anything it does not know for a note written the oldest
 * way, and would write these bytes over somebody's words.
 *
 * <p>Holds no Android types: the wire format and its bounds are unit tested.
 */
final class Sleeve {
    /** The format, and the first four bytes. Not a {@link Parcel}, not a {@link Carton}, and not "MNS1", which is Sealed. */
    static final byte[] MAGIC={'M','N','F','1'};

    /** One file, as it travels. */
    static final class Sent {
        /** Its id, the same on every device; what it is called; its kind; and where its pieces are, or empty. */
        final String id,name,kind,manifest;
        /** How big it is, in bytes. */
        final long bytes;
        /** What the far end may do with it: true renames it and replaces it with a new version (decision 93), false reads it. */
        final boolean writes;
        /** Who has it and at what standing, as a parcel's list: which thing, at the sender's end, and the list. */
        final String scope,target; final java.util.List<Parcel.Member> members;
        /** Whether the sender wants to hear it arrived; always, from a build that sends these. */
        final boolean answer;
        /** Gone for everybody: its owner deleted it for good, and whoever has a copy lets it go. */
        final boolean gone;
        /**
         * Whose it is (decision 93): the owner's address as the sender knows it and the key it signs with, or empty from a
         * 0.2.026 sender. A device that first has it from an admin keeps the owner as where it came from, so the owner's
         * delete for everybody is obeyed there too.
         */
        final String owner,ownerKey;
        /**
         * When its name was last chosen and when its bytes were last replaced, by whoever did it, or 0 for never: the later
         * of two always stands, whichever arrives first, so two writers end with the same file (decision 93).
         */
        final long named,replaced;
        /**
         * When it is to be gone, said only to the sender's own other devices, 0 to anybody else (decision 95): a file let go
         * on Temp is temporary on each of them, for the same time, as Temp is a note to oneself.
         */
        final long until;
        Sent(String id,String name,String kind,long bytes,String manifest,boolean writes,String scope,String target,
             java.util.List<Parcel.Member> members,boolean answer,boolean gone) {
            this(id,name,kind,bytes,manifest,writes,scope,target,members,answer,gone,"","",0,0);
        }
        Sent(String id,String name,String kind,long bytes,String manifest,boolean writes,String scope,String target,
             java.util.List<Parcel.Member> members,boolean answer,boolean gone,String owner,String ownerKey,long named,long replaced) {
            this(id,name,kind,bytes,manifest,writes,scope,target,members,answer,gone,owner,ownerKey,named,replaced,0);
        }
        Sent(String id,String name,String kind,long bytes,String manifest,boolean writes,String scope,String target,
             java.util.List<Parcel.Member> members,boolean answer,boolean gone,String owner,String ownerKey,long named,long replaced,long until) {
            this.id=id==null?"":id;this.name=name==null?"":name;this.kind=kind==null?"":kind;this.bytes=bytes;
            this.manifest=manifest==null?"":manifest;this.writes=writes;this.scope=scope==null?"":scope;
            this.target=target==null?"":target;
            this.members=members==null?java.util.Collections.<Parcel.Member>emptyList():members;
            this.answer=answer;this.gone=gone;
            this.owner=owner==null?"":owner;this.ownerKey=ownerKey==null?"":ownerKey;
            this.named=Math.max(0,named);this.replaced=Math.max(0,replaced);this.until=Math.max(0,until);
        }
        /** The same file, named but not yet said where to find, as a list that does not fit says it. */
        Sent unsaid(){return new Sent(id,name,kind,bytes,"",writes,scope,target,members,answer,gone,owner,ownerKey,named,replaced,until);}
        /** The same, not asking to be answered. */
        Sent quiet(){return new Sent(id,name,kind,bytes,manifest,writes,scope,target,members,false,gone,owner,ownerKey,named,replaced,until);}
    }

    private Sleeve(){}

    static byte[] wrap(Sent sent) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        out.write(MAGIC);
        out.writeBoolean(sent.writes);
        Parcel.put(out,sent.id,Parcel.ID_MOST);
        Parcel.put(out,sent.name,Parcel.NAME_MOST);
        Parcel.put(out,sent.kind,Parcel.NAME_MOST);
        out.writeLong(sent.bytes);
        Parcel.put(out,sent.manifest,Parcel.MANIFEST_MOST);
        Parcel.put(out,sent.scope,Parcel.ID_MOST);
        Parcel.put(out,sent.target,Parcel.ID_MOST);
        // Who has it, written exactly as a carton writes its list.
        int many=Math.min(sent.members.size(),Parcel.MEMBERS_MOST);
        out.writeInt(many);
        for(int at=0;at<many;at++) {
            Parcel.Member one=sent.members.get(at);
            Parcel.put(out,one.key,Parcel.KEY_MOST);
            Parcel.put(out,one.address,Parcel.ADDRESS_MOST);
            Parcel.put(out,one.name,Parcel.NAME_MOST);
            out.writeInt(one.level);
            out.writeLong(one.changed);
            byte[] agreement=one.agreement.length<=Parcel.KEY_MOST?one.agreement:new byte[0];
            out.writeInt(agreement.length);out.write(agreement);
        }
        out.writeBoolean(sent.answer);
        out.writeBoolean(sent.gone);
        // Since 0.2.027 (decision 93), where a 0.2.026 build stops reading: whose it is, and when its name and its bytes
        // were last changed.
        Parcel.put(out,sent.owner,Parcel.ADDRESS_MOST);
        Parcel.put(out,sent.ownerKey,Parcel.KEY_MOST);
        out.writeLong(sent.named);
        out.writeLong(sent.replaced);
        // Since 0.2.029 (decision 95), where a 0.2.027 or 0.2.028 build stops reading: when it is to be gone, to one's own devices.
        out.writeLong(sent.until);
        // Anything a later build adds goes after this, where this one stops reading.
        out.flush();
        return bytes.toByteArray();
    }

    /** The same, made to fit in {@code most} bytes: where it will not, it stops saying where the pieces are. */
    static byte[] wrap(Sent sent,int most) throws IOException {
        byte[] whole=wrap(sent);
        return whole.length<=most||sent.manifest.isEmpty()?whole:wrap(sent.unsaid());
    }

    /** What arrived, or null when this is not one, or is not one whole: half a file's word is not a word. */
    static Sent open(byte[] said) {
        if(said==null||said.length<MAGIC.length)return null;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return null;
        try(DataInputStream in=new DataInputStream(new java.io.ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            boolean writes=in.readBoolean();
            String id=Parcel.get(in,Parcel.ID_MOST), name=Parcel.get(in,Parcel.NAME_MOST), kind=Parcel.get(in,Parcel.NAME_MOST);
            if(id.trim().isEmpty())return null;
            long size=in.readLong();
            if(size<0)return null;
            String manifest=Parcel.get(in,Parcel.MANIFEST_MOST);
            String scope=Parcel.get(in,Parcel.ID_MOST), target=Parcel.get(in,Parcel.ID_MOST);
            int many=in.readInt();
            if(many<0||many>Parcel.MEMBERS_MOST)return null;
            java.util.List<Parcel.Member> members=new java.util.ArrayList<>(many);
            for(int at=0;at<many;at++) {
                String key=Parcel.get(in,Parcel.KEY_MOST), address=Parcel.get(in,Parcel.ADDRESS_MOST), who=Parcel.get(in,Parcel.NAME_MOST);
                int level=in.readInt();long changed=in.readLong();
                int length=in.readInt();
                if(length<0||length>Parcel.KEY_MOST)return null;
                byte[] agreement=new byte[length];in.readFully(agreement);
                members.add(new Parcel.Member(key,address,who,level,changed,agreement));
            }
            boolean answer=in.readBoolean(), gone=in.readBoolean();
            // From a 0.2.026 build nothing follows: nobody named as its owner, and nothing changed in it.
            if(in.available()==0)return new Sent(id,name,kind,size,manifest,writes,scope,target,members,answer,gone);
            String owner=Parcel.get(in,Parcel.ADDRESS_MOST), ownerKey=Parcel.get(in,Parcel.KEY_MOST);
            long named=in.readLong(), replaced=in.readLong();
            if(named<0||replaced<0)return null;
            // From a 0.2.027 or 0.2.028 build nothing more: not temporary.
            // Present or not at all: half of it is a sleeve cut short, refused (readLong runs out).
            long until=in.available()>0?in.readLong():0;
            if(until<0)return null;
            return new Sent(id,name,kind,size,manifest,writes,scope,target,members,answer,gone,owner,ownerKey,named,replaced,until);
        } catch(IOException | IllegalArgumentException broken){return null;}
    }
}
