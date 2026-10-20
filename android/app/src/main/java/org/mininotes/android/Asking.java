// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.HashSet;
import java.util.Set;

/**
 * Who has not accepted yet what was shared with them for the first time, and who was told that something waits here.
 *
 * <p>Something shared with somebody for the first time waits in their Shared with me until they accept it (decision 109),
 * and their device answers "I have it" all the same, so that it is not sent again every quarter of an hour. On the sender's
 * side that answer drew the green tick, "everybody has it", while the other person had not even seen it (the connect
 * review, 2026-10-14). So the receiving device also says {@link Receipt#WAITS}, and {@link Receipt#ACCEPTED} once it is
 * accepted, and the sender keeps, per device and per note or file, which of the two it last heard. Two lists, kept in the
 * device's settings as the locked devices are (see {@link NoteStore#lockedThere}): small, and nothing in the notebook.
 *
 * <p>The sender's list: {@code address TAB id TAB w} for "waits for them to accept", {@code a} for "accepted". Accepted
 * stands: a "waits" that arrives after it, from a message that was slower, changes nothing.
 *
 * <p>The receiver's list: {@code address TAB kind TAB id}, each a device told that something waits here, until it is told
 * that it was accepted, or the thing is gone.
 *
 * <p>Holds no Android or Swing types, so both apps use it and it is unit tested.
 */
final class Asking {
    private Asking(){}

    static final String WAITS="w", ACCEPTED="a";

    /** One key, the address and the id, for the sender's list. */
    static String key(String address,String id){return address+"\t"+id;}

    /**
     * The sender's list after one device said a thing waits for it, or that it was accepted; null where nothing changes.
     * A "waits" after an "accepted" is from a message that was slower, and changes nothing.
     */
    static Set<String> heard(Set<String> kept,String address,String id,boolean accepted) {
        if(address==null||address.isEmpty()||id==null||id.isEmpty())return null;
        String key=key(address,id),was=null;
        for(String one:kept)if(one.startsWith(key+"\t")&&one.length()==key.length()+2)was=one.substring(key.length()+1);
        String now=accepted?ACCEPTED:WAITS;
        if(now.equals(was)||(!accepted&&ACCEPTED.equals(was)))return null;
        Set<String> out=new HashSet<>(kept);
        if(was!=null)out.remove(key+"\t"+was);
        out.add(key+"\t"+now);
        return out;
    }

    /** What still waits to be accepted, as {@link #key}s. */
    static Set<String> waiting(Set<String> kept) {
        Set<String> out=new HashSet<>();
        for(String one:kept)if(one.endsWith("\t"+WAITS))out.add(one.substring(0,one.length()-2));
        return out;
    }

    /** The sender's list without one device's word about one thing: it refused it, or it was taken off. Null where unchanged. */
    static Set<String> forget(Set<String> kept,String address,String id) {
        String key=key(address,id);
        Set<String> out=new HashSet<>();
        for(String one:kept)if(!one.startsWith(key+"\t"))out.add(one);
        return out.size()==kept.size()?null:out;
    }

    /** One line of the receiver's list. */
    static String told(String address,String kind,String id){return address+"\t"+kind+"\t"+id;}

    /** A line of the receiver's list read back: {address, kind, id}, or null for one that is not whole. */
    static String[] told(String line) {
        String[] parts=line==null?new String[0]:line.split("\t",-1);
        if(parts.length!=3||parts[0].isEmpty()||parts[1].isEmpty()||parts[2].isEmpty())return null;
        return parts;
    }
}
