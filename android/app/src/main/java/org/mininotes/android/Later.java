// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What one of my devices did not get when it was sent, kept to be sent again by itself: a person's Parlons! address, the
 * name we pick for someone, a key (the owner, 2026-11-07: "all accross the app we should present the solution instead of
 * presenting the problem"). It said "could not be reached now: save again later"; now it says it goes by itself, and does.
 *
 * <p>Each one is the kind, what it is about, the device's address and when it first missed, in one line; the same kind about
 * the same thing to the same device is kept once, since the thing as it is then is what goes. Kept a month at most. Holds
 * no Android types: Post keeps the lines in its settings and sends them.
 */
final class Later {
    private Later(){}

    /** What it is: a Parlons! address, a name we pick, a key. */
    enum Kind{PARLONS,CALLED,KEY}

    /** How long one is tried, and how often at most. */
    static final long KEPT=30L*24*60*60_000L, EVERY=2*60_000L;

    /** One thing owed to one device. {@code about} is the person's address, the key's name, or empty for this device itself. */
    static final class Owed {
        final Kind kind;final String about,device;final long since;
        Owed(Kind kind,String about,String device,long since){this.kind=kind;this.about=about==null?"":about;this.device=device==null?"":device;this.since=since;}
        String line(){return kind.name()+"\u0001"+about+"\u0001"+device+"\u0001"+since;}
        boolean same(Owed o){return kind==o.kind&&about.equals(o.about)&&device.equals(o.device);}
        static Owed of(String line) {
            if(line==null)return null;
            String[] p=line.split("\u0001",-1);
            if(p.length!=4)return null;
            try{return new Owed(Kind.valueOf(p[0]),p[1],p[2],Long.parseLong(p[3]));}catch(RuntimeException unreadable){return null;}
        }
    }

    /** The lines with this one owed: kept where it was already, with when it first missed. */
    static Set<String> add(Collection<String> lines,Owed one) {
        Set<String> out=new LinkedHashSet<>(lines);
        for(String l:lines){Owed o=Owed.of(l);if(o!=null&&o.same(one))return out;}
        out.add(one.line());
        return out;
    }
    /** The lines without that one: it went. */
    static Set<String> remove(Collection<String> lines,Kind kind,String about,String device) {
        Owed gone=new Owed(kind,about,device,0);
        Set<String> out=new LinkedHashSet<>();
        for(String l:lines){Owed o=Owed.of(l);if(o!=null&&!o.same(gone))out.add(l);}
        return out;
    }
    /** What is still owed at {@code now}: unreadable lines and those older than {@link #KEPT} left out. */
    static List<Owed> owed(Collection<String> lines,long now) {
        List<Owed> out=new ArrayList<>();
        for(String l:lines){Owed o=Owed.of(l);if(o!=null&&now-o.since<KEPT)out.add(o);}
        return out;
    }
    /** The lines still kept at {@code now}. */
    static Set<String> kept(Collection<String> lines,long now) {
        Set<String> out=new LinkedHashSet<>();
        for(Owed o:owed(lines,now))out.add(o.line());
        return out;
    }
}
