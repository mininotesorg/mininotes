// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A device of mine linked with every other device of mine (the owner, 2026-10-28: "when I link one additional of my devices, it
 * should be linked to all of my devices or at least it should be clearly proposed to do so in the process. and if chosen to not
 * link with all at the start, the device should be listed on all and easily linkable afterward"; docs/HOME.md, decision 268).
 *
 * <p>Nothing new is learnt here that the cards between my devices did not carry already: a {@link Persons} card lists every
 * device of mine its sender counts as mine, each with both its keys and where it is. A card is kept as it arrives from a device
 * that is mine at both ends, and what it lists and this device is not linked with is shown under My devices, with Link.
 *
 * <p>Linking is the hello devices linking through a list already say ({@link Linking}), naming {@link #TARGET} instead of a
 * list. The other device takes it from a device it is not linked with only where a card it keeps, from a device that is mine at
 * both ends there, lists the key the hello is signed with as mine ({@link #vouched}): the owner said so on that device. Nobody
 * else's word makes a device mine, and nothing becomes mine without the owner having said so on one of his devices: Link
 * pressed here, a tick when it was added there, or Add my device there.
 *
 * <p>The tick: the device a new one was added on tells each of the two, with {@link Receipt#LINK_MINE}, to link with the other.
 * Said only to a device that has said it reads it ({@link Receipt#LINKS_MINE}), only about a device both have on their cards,
 * and again in a while until the card of the device told lists the other as its own. A build from before never says
 * LINKS_MINE and is never told; it reads a hello naming this target as it reads any LINK hello: as before.
 *
 * <p>Holds no Android types: the rules are unit tested.
 */
final class Introducing {
    private Introducing(){}

    /** What a LINK hello names when it is a device of mine linking with another of mine: never a share's target, never empty. */
    static final String TARGET="mine";

    /** How long an introduction is tried for, a device that never updates being told nothing after it. */
    static final long KEEP=60L*24*60*60*1000;
    /** How long after an introduction is said it is said again, until it is done. */
    static final long AGAIN=10L*60*1000;

    /** Whether a hello is a device of mine linking with this one. */
    static boolean isMine(Hello.Said said){return Linking.isLink(said)&&TARGET.equals(said.target);}

    /** A hello of this kind, from this device: asking, or answering. */
    static Hello.Said hello(String name,String address,byte[] agreement,byte[] signing,boolean asking) {
        return new Hello.Said(name,address,agreement,signing,Linking.SCOPE,TARGET,asking);
    }

    /** The SHA-256 of a key in its long form, as an envelope names its sender: empty where it is not a key. */
    static byte[] fingerprint(byte[] signing) {
        if(signing==null||signing.length==0)return new byte[0];
        try{return MessageDigest.getInstance("SHA-256").digest(Point.read(signing).getEncoded());}
        catch(Exception notAKey){return new byte[0];}
    }

    /** The same, as the hex the notebook's settings keep devices by. */
    static String who(byte[] signing){byte[] made=fingerprint(signing);return made.length==0?"":Courier.hex(made);}

    /**
     * Which device an introduction is about, in the sixteen bytes an envelope's page has: the first half of its fingerprint.
     * Unique among the thirty-two devices a card can list.
     */
    static byte[] which(byte[] signing) {
        byte[] made=fingerprint(signing);
        return made.length==0?new byte[0]:Arrays.copyOf(made,16);
    }

    /** One device of mine a card lists, and how it stands here. */
    static final class Offered {
        final Persons.Device device;
        /** Its key, in the one form {@link Persons#key} makes. */
        final String key;
        /** Whether it is paired here already, but not as mine: a person who is in fact a device of mine. */
        final boolean pairedHere;
        Offered(Persons.Device device,String key,boolean pairedHere){this.device=device;this.key=key;this.pairedHere=pairedHere;}
    }

    /**
     * The devices of mine the cards list, the newest word on each: active, with both keys and an address. One a card says is
     * mine no longer is not offered where that word is the later.
     */
    private static Map<String,Persons.Device> listed(List<Persons.Card> cards) {
        Map<String,Persons.Device> newest=new HashMap<>();
        if(cards==null)return newest;
        for(Persons.Card card:cards) {
            if(card==null)continue;
            for(Persons.Device one:card.devices) {
                String key=Persons.key(one.signing);
                if(key.isEmpty())continue;
                Persons.Device had=newest.get(key);
                if(had==null||one.decided>had.decided||one.decided==had.decided&&had.state==Persons.ACTIVE&&one.state==Persons.GONE)newest.put(key,one);
            }
        }
        newest.values().removeIf(one->one.state!=Persons.ACTIVE||Persons.key(one.agreement).isEmpty()||one.address.trim().isEmpty());
        return newest;
    }

    /**
     * What My devices offers to link: every device of mine the cards list that is not this one and not linked here as mine,
     * in the order of their names.
     *
     * @param cards  the cards kept from devices that are mine at both ends
     * @param self   this device's signing key
     * @param here   every device paired here, by key in one form, and whether it is mine here
     */
    /**
     * The devices to take as mine without asking (decision 290): paired here already as somebody else's, and listed, active, as
     * mine on a card from a device mine at both ends. The owner said so on that device; asking again on each of the others left
     * them disagreeing (the owner, 2026-10-30: the spare laptop "is not listed as my device" on the Graphene "and it is on the
     * Pro, all this should be uniformalized accross all device").
     */
    static List<Offered> adopted(List<Offered> offered) {
        List<Offered> out=new ArrayList<>();
        if(offered!=null)for(Offered one:offered)if(one.pairedHere&&one.device.state==Persons.ACTIVE)out.add(one);
        return out;
    }

    /** Whether a card lists this key as an active device of its sender's owner (decision 290). */
    static boolean lists(Persons.Card card,byte[] signing) {
        if(card==null)return false;
        String key=Persons.key(signing);
        Persons.Device one=key.isEmpty()?null:listed(java.util.Collections.singletonList(card)).get(key);
        return one!=null&&one.state==Persons.ACTIVE;
    }

    static List<Offered> notLinked(List<Persons.Card> cards,byte[] self,Map<String,Boolean> here) {
        String me=Persons.key(self);
        List<Offered> out=new ArrayList<>();
        for(Map.Entry<String,Persons.Device> one:listed(cards).entrySet()) {
            String key=one.getKey();
            if(key.equals(me))continue;
            Boolean mine=here==null?null:here.get(key);
            if(Boolean.TRUE.equals(mine))continue;
            out.add(new Offered(one.getValue(),key,mine!=null));
        }
        out.sort((a,b)->{int by=a.device.name.compareToIgnoreCase(b.device.name);return by!=0?by:a.key.compareTo(b.key);});
        return out;
    }

    /**
     * The one question asked of a hello naming {@link #TARGET}: whether a card kept here, from a device mine at both ends, lists
     * the key it is signed with as mine. The device the card lists, or null where none does, and then it is not taken as mine.
     */
    static Persons.Device vouched(List<Persons.Card> cards,byte[] signing) {
        String key=Persons.key(signing);
        return key.isEmpty()?null:listed(cards).get(key);
    }

    /**
     * The device an introduction from another device of mine is about: on that device's card, active, named by {@code which},
     * and not this one. Null where its card does not list it yet; it is said again, and the card comes meanwhile.
     */
    static Persons.Device introduced(Persons.Card card,byte[] which,byte[] self) {
        if(card==null||which==null||which.length!=16)return null;
        String me=Persons.key(self);
        for(Persons.Device listed:listed(java.util.Collections.singletonList(card)).values())
            if(Arrays.equals(which(listed.signing),which)&&!Persons.key(listed.signing).equals(me))return listed;
        return null;
    }

    /** Whether the card of a device told about another lists that other as its own: the introduction is done. */
    static boolean done(Persons.Card toldCard,byte[] about) {
        String key=Persons.key(about);
        if(toldCard==null||key.isEmpty())return false;
        for(Persons.Device one:toldCard.devices)if(one.state==Persons.ACTIVE&&key.equals(Persons.key(one.signing)))return true;
        return false;
    }

    // ---- what waits to be said: kept as a line each, "told:about:since" by fingerprint -------------------------------

    /** One introduction to say: to the device told, about the other, since when. */
    static final class Pending {
        final String told,about; final long since;
        Pending(String told,String about,long since){this.told=told;this.about=about;this.since=since;}
        String kept(){return told+":"+about+":"+since;}
    }

    /** A line kept, read back; null where it is not one. */
    static Pending pending(String kept) {
        if(kept==null)return null;
        String[] parts=kept.split(":");
        if(parts.length!=3||parts[0].isEmpty()||parts[1].isEmpty()||parts[0].equals(parts[1]))return null;
        try{return new Pending(parts[0],parts[1],Long.parseLong(parts[2]));}catch(NumberFormatException broken){return null;}
    }

    /**
     * Both ways for each device ticked: the new one told about it, and it told about the new one, so whichever is reachable
     * first links. Added to what waits already; the same two again are kept once, with the later time.
     */
    static Set<String> introduce(Set<String> kept,String added,List<String> others,long now) {
        Set<String> all=new java.util.LinkedHashSet<>();
        Map<String,Pending> by=new java.util.LinkedHashMap<>();
        if(kept!=null)for(String line:kept){Pending one=pending(line);if(one!=null)by.put(one.told+":"+one.about,one);}
        if(added!=null&&!added.isEmpty()&&others!=null)for(String other:others) {
            if(other==null||other.isEmpty()||other.equals(added))continue;
            by.put(added+":"+other,new Pending(added,other,now));
            by.put(other+":"+added,new Pending(other,added,now));
        }
        for(Pending one:by.values())all.add(one.kept());
        return all;
    }

    /** Whether an introduction is past trying: a device told nothing for two months is not going to update for this. */
    static boolean expired(Pending one,long now){return one==null||now>=one.since&&now-one.since>KEEP;}

    /** What is done with an introduction waiting, at a round: said, left to wait, done, or dropped. */
    enum Step{SAY,WAIT,DONE,DROP}

    /**
     * One introduction weighed. Dropped where it does not read, is past trying, or either device is forgotten or no longer mine
     * here: nothing is said about a device that is not mine. Done where the card of the one told lists the other as its own.
     * Otherwise said only once both have said they read it and the one told counts this device as mine; until then it waits.
     *
     * @param toldMine   whether the one told is paired here and marked mine; false for one no longer here
     * @param aboutMine  the same, of the one it is about
     * @param done       whether the card kept from the one told lists the other as its own
     * @param toldReads  whether the one told has said {@link Receipt#LINKS_MINE}
     * @param aboutReads the same, of the one it is about
     * @param countsThis whether the one told counts this device as its owner's
     */
    static Step weigh(Pending one,long now,boolean toldMine,boolean aboutMine,boolean done,boolean toldReads,boolean aboutReads,boolean countsThis) {
        if(one==null||expired(one,now)||!toldMine||!aboutMine)return Step.DROP;
        if(done)return Step.DONE;
        return toldReads&&aboutReads&&countsThis?Step.SAY:Step.WAIT;
    }

    /** Whether it is time to say it again: once {@link #AGAIN} has gone since it was last said this run. */
    static boolean due(long last,long now){return last<=0||now<last||now-last>=AGAIN;}

    // ---- words ----------------------------------------------------------------------------------------------------------

    /** The step's heading where a device of mine is added, over a tick for each of my other devices. */
    static final String ALSO_LINK="Also link it with";
    /** Under it: what the tick does. */
    static final String ALSO_DOES="Each one switched on counts it as one of yours too, with no code to scan.";
    /** Under a device of mine a card lists and this one is not linked with. */
    static final String NOT_LINKED="Not linked here yet";
    /** Under somebody paired as a person whom another device of mine lists as mine, or who counts this device as theirs. */
    static final String IS_IT_YOURS="One of your devices?";
    /** The one button on that line. */
    static final String THIS_IS_MINE="This is my device";
}
