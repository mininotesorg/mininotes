// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What arrived since the person last opened the app, gathered into one notification (the owner, 2026-10-22: "On the phones
 * we should have a notification at the phone level and on the app icon on the desktop when something new arrives"; decision
 * 236): one line with its count, in the person's language, a sound only for the first arrival after a quiet spell, cleared
 * when the app is opened.
 *
 * <p>It says who and what kind of thing, and how many: never a word of what a note says, and a note's title nowhere, as
 * the lines before it did not (a notification is read by whoever holds the phone, locked or not). Something shared for
 * the first time is named by its name, as its own line already was ({@link FirstShare#title}), only when it is the one
 * thing there is to say.
 *
 * <p>Plain Java, so it is tested on its own and both apps can read it. Not thread-safe by itself: its one keeper locks.
 */
final class ArrivalNews {
    /** What kind of thing came. In the order they are said. */
    enum Kind { SHARED, UPDATED, WAITING, FILES, OTHER }

    /**
     * A quiet spell: after this long with nothing arriving, the next arrival sounds again. A shared list being written in
     * sends every few seconds, and a pad that chimed each time would be switched off within the hour; one chime for each
     * time somebody starts is what is worth hearing.
     */
    static final long QUIET=10*60_000L;

    /** One thing that came, under the key it is counted by (a note's id, a shared thing's id), so a note written in twenty times is one note. */
    private static final class One {
        final Kind kind; final String from; final String line; int many;
        One(Kind kind,String from,String line,int many){this.kind=kind;this.from=from;this.line=line;this.many=many;}
    }

    private final Map<String,One> came=new LinkedHashMap<>();
    /** When the last thing came, and whether anything has since the app was opened. */
    private long last; private boolean heard;
    private int unkeyed;

    /**
     * Something came at {@code now}.
     *
     * @param kind what kind of thing
     * @param from who it is from, by the name they go by here; empty or null where nobody is known
     * @param key what it is counted by, so it counts once however often it comes (a note's id); null or empty where each
     *            arrival is one of its own (files sent on their own)
     * @param many how many files, for {@link Kind#FILES}; one otherwise
     * @param line its own line, said only where it is the one thing there is and nothing better is known of it
     *             ({@link Kind#WAITING}, {@link Kind#OTHER})
     * @return whether this arrival is to sound: the first since the app was last opened, or after nothing came for {@link #QUIET}
     */
    boolean arrived(Kind kind,String from,String key,int many,String line,long now) {
        boolean sound=!heard||now-last>=QUIET;
        last=now;heard=true;
        String who=from==null?"":from.trim();
        String k=key==null||key.isEmpty()?"#"+(unkeyed++):key;
        One was=came.get(k);
        // The same thing again: still the one thing, the kind it first came as (a note shared, then written in, is a note shared).
        if(was==null)came.put(k,new One(kind,who,line==null?"":line,Math.max(1,many)));
        else if(kind==Kind.FILES&&was.kind==Kind.FILES)was.many=Math.max(was.many,many);
        return sound;
    }

    /** The app was opened: everything was seen, and the next arrival sounds. */
    void clear(){came.clear();last=0;heard=false;unkeyed=0;}

    /** Whether there is nothing to say. */
    boolean isEmpty(){return came.isEmpty();}

    /** How many things came, the count a launcher shows on the icon. Files count as files. */
    int total() {
        int all=0;
        for(One one:came.values())all+=one.kind==Kind.FILES?one.many:1;
        return all;
    }

    /** Everybody something came from, in the order they first did. */
    List<String> senders() {
        Set<String> who=new LinkedHashSet<>();
        for(One one:came.values())if(!one.from.isEmpty())who.add(one.from);
        return new ArrayList<>(who);
    }

    /** The note to open from the notification: where one note is all that came, that note; else empty. */
    String onlyNote() {
        if(came.size()!=1)return "";
        Map.Entry<String,One> one=came.entrySet().iterator().next();
        Kind kind=one.getValue().kind;
        return (kind==Kind.SHARED||kind==Kind.UPDATED)&&!one.getKey().startsWith("#")?one.getKey():"";
    }

    /** Whether files are all that came, so the notification opens Received files. */
    boolean onlyFiles() {
        if(came.isEmpty())return false;
        for(One one:came.values())if(one.kind!=Kind.FILES)return false;
        return true;
    }

    /** The title: the one person it is all from, or the app's name for news where it is from several or nobody known. */
    String title() {
        List<String> who=senders();
        return who.size()==1?who.get(0):Say.t("New in Mininotes");
    }

    /**
     * The line under it. One thing: what it is, said as a sentence ("Ana updated a note."). Several: how many of each kind,
     * and who from where the title does not say it already ("3 notes updated, 1 file, from Ana, Ben").
     */
    String text() {
        if(came.isEmpty())return "";
        List<String> who=senders();
        if(came.size()==1)return sentence(came.values().iterator().next());
        Map<Kind,Integer> counts=new EnumMap<>(Kind.class);
        for(One one:came.values())counts.merge(one.kind,one.kind==Kind.FILES?one.many:1,Integer::sum);
        List<String> parts=new ArrayList<>();
        for(Map.Entry<Kind,Integer> each:counts.entrySet())parts.add(counted(each.getKey(),each.getValue()));
        String said=String.join(", ",parts);
        if(who.size()<2)return said;
        return Say.t("{0}, from {1}",said,names(who));
    }

    /** One thing, as a sentence in the person's language. */
    private static String sentence(One one) {
        String who=one.from.isEmpty()?Say.t("Somebody"):one.from;
        switch(one.kind) {
            case SHARED: return Say.t("{0} shared a note with you.",who);
            case UPDATED: return Say.t("{0} updated a note.",who);
            // How many, and no verb: it may be files that came or files somebody asks to send, and the title says who.
            case FILES: return counted(one.kind,one.many);
            // Its own line, which already names it the way it was said before, looked up in case the list has it.
            default: return one.line.isEmpty()?counted(one.kind,1):Say.t(one.line);
        }
    }

    /**
     * How many of a kind: a phrase for one and a phrase for more, which is what English needs; a language with more plural
     * forms than two has its phrase for more written so the number does not have to agree ("Обновлено заметок: {0}").
     */
    static String counted(Kind kind,int n) {
        switch(kind) {
            case SHARED: return n==1?Say.t("1 note shared with you"):Say.t("{0} notes shared with you",n);
            case UPDATED: return n==1?Say.t("1 note updated"):Say.t("{0} notes updated",n);
            case WAITING: return n==1?Say.t("1 thing to accept"):Say.t("{0} things to accept",n);
            case FILES: return n==1?Say.t("1 file"):Say.t("{0} files",n);
            default: return n==1?Say.t("1 other change"):Say.t("{0} other changes",n);
        }
    }

    /**
     * "Ana", "Ana, Ben, Cy", and past three "Ana, Ben, Cy +2": as many as a line has room for. A comma and a number rather
     * than "and", because a phrase as wide as "{0} and {1}" would be found in any English words with an "and" in them
     * (Say looks a template up by its shape) and translate a note's title the person never asked to be.
     */
    static String names(List<String> who) {
        if(who.size()<=3)return String.join(", ",who);
        return String.join(", ",who.subList(0,3))+" +"+(who.size()-3);
    }
}
