// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * People in groups (the owner, 2026-10-05: "the people and devices should be put in groups, my devices should be one but
 * we should also be able to create people groups and add people to it like colleagues or friends"; docs/HOME.md, decision
 * 100).
 *
 * <p>A group is a name and the people in it, and one person can be in any number of them. A thing shared with a group is
 * shared with everybody in it for as long as they are in it: put Ana in Friends later and she has everything Friends has;
 * take her out and it is taken from her, unless she was also given it on her own. That is how My devices always behaved,
 * and My devices is a group like the others now, built in: never kept, its people the devices marked My device here.
 *
 * <p>Groups are the owner's, so they are the same on every device of theirs: what travels between them is the card, every
 * group with when it was named or deleted, everybody in each with when they were put in or taken out, and every thing
 * given to a group with its level and when. Row by row, the later decision stands, so a card that arrives late changes
 * nothing. A card goes only to a device yours at both ends whose build has said it reads them ({@link Receipt#GROUPS}): a
 * build from before would read these bytes as a note, and nobody else has any business knowing who is in your groups.
 *
 * <p>Holds no Android types: the format, its bounds and every decision are unit tested.
 */
final class Groups {
    /** The format, and the first four bytes. Not a {@link Persons} card, a {@link Parcel} or anything else. */
    static final byte[] MAGIC={'M','N','G','1'};
    /** One byte after the magic, so a later build can say more without being read as this one. */
    static final int FORMAT=1;

    /** My devices, the group every owner has: never kept, never renamed, never deleted, and never on a card as a group. */
    static final String MINE="mine";
    static final String MY_DEVICES="My devices";
    /**
     * What Share with says beside somebody who has a thing as their group has it, who was taken off it by hand, and who is
     * not on it yet (decision 103).
     */
    static final String AS_GROUP="As the group",AS_GROUP_DOES="They may do what the group may, and follow it when it changes.",REMOVED="Removed",ADD="Add";

    /** More than anybody keeps, and few enough that a card always fits one envelope. */
    static final int GROUPS_MOST=64, MEMBERS_MOST=500, GIVEN_MOST=1000;
    static final int ID_MOST=64, NAME_MOST=Hello.NAME_MOST, KEY_MOST=200, TARGET_MOST=128;

    private Groups(){}

    /** One group, as kept and as a card says it: deleted ({@code gone}) is a decision like a name, and travels as one. */
    static final class Group {
        final String id,name; final long decided; final boolean gone;
        Group(String id,String name,long decided,boolean gone){this.id=id==null?"":id;this.name=name==null?"":name;this.decided=decided;this.gone=gone;}
    }

    /** Somebody in a group, or taken out of it, by the key their device signs with: an address moves, a key does not. */
    static final class Member {
        final String group,key; final long decided; final boolean gone;
        Member(String group,String key,long decided,boolean gone){this.group=group==null?"":group;this.key=key==null?"":key;this.decided=decided;this.gone=gone;}
    }

    /** A thing given to a group at a level, or taken from it ({@link Sharing.Level#GONE}), and when that was decided. */
    static final class Given {
        final String group; final Sharing.Scope scope; final String target; final Sharing.Level level; final long decided;
        Given(String group,Sharing.Scope scope,String target,Sharing.Level level,long decided) {
            this.group=group==null?"":group;this.scope=scope;this.target=target==null?"":target;
            this.level=level==null?Sharing.Level.GONE:level;this.decided=decided;
        }
    }

    /** Everything one device of yours says to another about your groups. */
    static final class Card {
        final List<Group> groups; final List<Member> members; final List<Given> given;
        Card(List<Group> groups,List<Member> members,List<Given> given) {
            this.groups=groups==null?new ArrayList<>():groups;this.members=members==null?new ArrayList<>():members;
            this.given=given==null?new ArrayList<>():given;
        }
        /** The newest decision on it, which rides where a revision goes. */
        long newest() {
            long newest=0;
            for(Group one:groups)newest=Math.max(newest,one.decided);
            for(Member one:members)newest=Math.max(newest,one.decided);
            for(Given one:given)newest=Math.max(newest,one.decided);
            return newest;
        }
    }

    /** A thing whose people a group changed, for the caller to send: its list travels with it, and with nothing else. */
    static final class Changed {
        final Sharing.Scope scope; final String target;
        Changed(Sharing.Scope scope,String target){this.scope=scope;this.target=target;}
    }

    // ---- names and words ------------------------------------------------------------------------------------

    /** A new group's id, made once, on whichever device made it. */
    static String newId(SecureRandom random){return Persons.newId(random);}

    /** A group's name as it is kept: trimmed, and never empty or longer than a card carries. */
    static String name(String said) {
        String name=said==null?"":said.trim();
        if(name.isEmpty())throw new IllegalArgumentException("A group needs a name.");
        if(name.getBytes(StandardCharsets.UTF_8).length>NAME_MOST)throw new IllegalArgumentException("That name is too long for a group.");
        return name;
    }

    /** A group as Who? lists it: "Friends · 3 people", "My devices · 1 device". */
    static String counted(String name,int many,boolean devices) {
        return name+" · "+many+" "+(devices?(many==1?"device":"devices"):(many==1?"person":"people"));
    }

    /** A group as Who has access lists it: "Friends · Can write". */
    static String given(String name,Sharing.Level level){return name+" · "+level.words();}

    /** Whether a decision made at {@code decided} replaces one made at {@code had}: the later stands, and the same is no news. */
    static boolean later(long decided,long had){return decided>had;}

    /**
     * Whether a card goes to a device: one marked as yours here, whose build has said it reads groups, and that has said
     * this device is its owner's too. Anybody else's device never learns who is in your groups.
     */
    static boolean goesTo(boolean mineHere,boolean readsGroups,boolean mineThere){return mineHere&&readsGroups&&mineThere;}

    // ---- keys -----------------------------------------------------------------------------------------------

    /** A key as a card carries it: the thirty-three bytes a pairing code does, whichever form it is kept in. */
    static byte[] wire(String kept) {
        try{return Point.shorten(Point.read(Base64.getDecoder().decode(kept)));}
        catch(Exception notAKey){throw new IllegalArgumentException("Not a key");}
    }

    /** And back, to the one form a notebook keeps keys in; empty where it is not a key. */
    static String kept(byte[] wire){return Persons.key(wire);}

    // ---- the card -------------------------------------------------------------------------------------------

    /** The inside of a card. */
    static byte[] wrap(Card card) {
        if(card.groups.size()>GROUPS_MOST||card.members.size()>MEMBERS_MOST||card.given.size()>GIVEN_MOST)
            throw new IllegalArgumentException("More groups than a card carries");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);
            out.writeShort(card.groups.size());
            for(Group one:card.groups) {
                put(out,text(one.id),ID_MOST);put(out,text(one.name),NAME_MOST);
                out.writeLong(one.decided);out.writeByte(one.gone?1:0);
            }
            out.writeShort(card.members.size());
            for(Member one:card.members) {
                put(out,text(one.group),ID_MOST);put(out,wire(one.key),KEY_MOST);
                out.writeLong(one.decided);out.writeByte(one.gone?1:0);
            }
            out.writeShort(card.given.size());
            for(Given one:card.given) {
                put(out,text(one.group),ID_MOST);put(out,text(one.scope.name()),ID_MOST);put(out,text(one.target),TARGET_MOST);
                out.writeByte(one.level.said());out.writeLong(one.decided);
            }
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    /** Whether these bytes say they are a groups card, before anything else is asked of them. */
    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /**
     * What a card says, or null where these bytes are not one, or are but broken, out of bounds, naming a level or a kind of
     * thing this build does not know, or with anything after the last row. Half a card is not a card.
     */
    static Card open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            int count=in.readUnsignedShort();
            if(count>GROUPS_MOST)return null;
            List<Group> groups=new ArrayList<>(count);
            for(int at=0;at<count;at++) {
                String id=string(in,ID_MOST),name=string(in,NAME_MOST);long decided=in.readLong();int gone=in.readUnsignedByte();
                if(id.isEmpty()||MINE.equals(id)||gone>1)return null;
                groups.add(new Group(id,name,decided,gone==1));
            }
            count=in.readUnsignedShort();
            if(count>MEMBERS_MOST)return null;
            List<Member> members=new ArrayList<>(count);
            for(int at=0;at<count;at++) {
                String group=string(in,ID_MOST);String key=kept(get(in,KEY_MOST));long decided=in.readLong();int gone=in.readUnsignedByte();
                if(group.isEmpty()||key.isEmpty()||gone>1)return null;
                members.add(new Member(group,key,decided,gone==1));
            }
            count=in.readUnsignedShort();
            if(count>GIVEN_MOST)return null;
            List<Given> given=new ArrayList<>(count);
            for(int at=0;at<count;at++) {
                String group=string(in,ID_MOST),scope=string(in,ID_MOST),target=string(in,TARGET_MOST);
                int level=in.readUnsignedByte();long decided=in.readLong();
                if(group.isEmpty()||target.isEmpty()||level>Sharing.Level.ADMIN.said())return null;
                given.add(new Given(group,Sharing.Scope.valueOf(scope),target,Sharing.Level.of(level),decided));
            }
            if(in.available()!=0)return null;
            return new Card(groups,members,given);
        } catch(IOException|IllegalArgumentException broken){return null;}
    }

    private static byte[] text(String said){return said.getBytes(StandardCharsets.UTF_8);}

    private static String string(DataInputStream in,int most) throws IOException {
        return new String(get(in,most),StandardCharsets.UTF_8).trim();
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
