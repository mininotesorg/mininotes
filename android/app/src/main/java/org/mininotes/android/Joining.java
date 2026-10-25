// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * One path after a code is scanned or pasted, the same whatever the code was (the owner, 2026-10-25: "the + should bring the
 * camera so that I can scan one code ... once scanned, we should be invited to define what it is, own device or not, put this
 * in a group, what to share and so on there must be a clear path as unified as possible always, not start with a branch
 * Personnes or Appareils"; docs/HOME.md, decision 258).
 *
 * <p>A step at a time, one button each: what it is (one of my devices, or someone else), a group for someone else, what to
 * share (Nothing yet first), and done. Both apps draw the steps; what is kept and sent is here, so the two cannot differ.
 * Nothing new travels: the pairing is the one a code always made (Pairing, Post.accept), a group is put as People and devices
 * puts one, and a share is given as the Share box gives it.
 */
final class Joining {
    private Joining(){}

    /** Whether the code says the device that showed it is one of mine: a code of the library's, as Add my device makes. */
    static boolean saysMine(Pairing.Said code) {
        return code!=null&&Sharing.Scope.LIBRARY.name().equals(Pairing.scopeIn(code.scope));
    }

    /** Whether the code brings something they share with me, so the last step accepts it: not Add my device's nothing yet. */
    static boolean offers(Pairing.Said code) {
        return code!=null&&!code.offer.isEmpty()&&!Sharing.mineOnly(code.scope,code.target);
    }

    /** What was chosen on the way: what it is, its group, what it gets. */
    static final class Choice {
        /** One of my devices; else someone else. */
        boolean mine;
        /** Someone else's group: an existing group's id, or empty for none. */
        String group="";
        /** A group made on the way, by its name, which they are put in; empty for none. */
        String newGroup="";
        /** For a device of mine: what it may do with everything, or null for nothing yet (decision 255). */
        Sharing.Level everything;
        /** For someone else: the notes, folders and files chosen for them, each at {@link #level}. Empty for nothing yet. */
        final List<NoteStore.Branch> things=new ArrayList<>();
        Sharing.Level level=Sharing.Level.WRITE;

        /** Where the code said what it is, that; otherwise someone else, the more common. */
        static Choice from(Pairing.Said code){Choice c=new Choice();c.mine=saysMine(code);return c;}
        boolean sharesNothing(){return mine?everything==null:things.isEmpty();}
    }

    /** What the choices changed here, for {@link #send} to send. */
    static final class Kept {
        final List<Groups.Changed> groups=new ArrayList<>();
        /** A group was made or somebody put in one: the groups card goes to my other devices. */
        boolean grouped;
        /** Everything given to a device of mine. */
        boolean everything;
        /** Each thing given to someone else. */
        final List<NoteStore.Branch> given=new ArrayList<>();
        /** Things this device may not share at all, and so not given. */
        int refused;
    }

    /** The things on Home that may be shared from here: notes, folders and files, the kinds a Share box shares. */
    static List<NoteStore.Branch> shareable(NoteStore store) {
        List<NoteStore.Branch> all=new ArrayList<>();
        for(NoteStore.Branch one:store.contents(Things.HOME)) {
            Sharing.Scope scope=scopeOf(one);
            if(scope!=null&&!store.mayGive(scope,one.id).isEmpty())all.add(one);
        }
        return all;
    }

    /** Home's things to choose from, with anything chosen already that is not on Home (a note in a folder) first. */
    static List<NoteStore.Branch> withChosen(List<NoteStore.Branch> home,List<NoteStore.Branch> chosen) {
        List<NoteStore.Branch> all=new ArrayList<>();
        for(NoteStore.Branch one:chosen){boolean there=false;for(NoteStore.Branch h:home)there|=h.id.equals(one.id);if(!there)all.add(one);}
        all.addAll(home);
        return all;
    }

    /** The scope a thing is shared at, or null where it is not shared on its own. */
    static Sharing.Scope scopeOf(NoteStore.Branch one) {
        if(one==null)return null;
        switch(one.kind){case PAGE:return Sharing.Scope.PAGE;case COLLECTION:return Sharing.Scope.COLLECTION;case FILE:return Sharing.Scope.FILE;default:return null;}
    }

    /**
     * The device that showed the code, kept as the code and the first step say: one of mine or not. Scanning their code is
     * wanting them, so ignoring them before is forgotten. Blocking.
     */
    static void paired(Context where,NoteStore store,Pairing.Said code,boolean mine) {
        store.pairedWith(code.address,code.name,mine,code.agreement,code.signing);
        Invites.unignore(where,code.signing);
        if(mine)store.setMine(code.address,true);
    }

    /**
     * What is said back to the device that showed the code, kept before it is sent, since it may be sent to a device nobody is
     * holding; and what it brings, so it stands on Home as waiting (decision 73). Blocking.
     */
    static void accepting(NoteStore store,Pairing.Said code) {
        store.accepting(code.address,code.name,code.scope,code.target,code.level);
        store.acceptingOffer(code.address,code.offer);
        // An earlier copy of it put in the bin or the archive here comes back out: accepting is wanting it.
        if(!code.target.isEmpty())store.acceptedBack(code.address,code.target);
    }

    /**
     * The rest of the path, kept once the device is paired: its group, made where it was named on the way, and what it gets,
     * given as a Share box gives it, refused for anything this device may not share at that level. Blocking.
     */
    static Kept chosen(NoteStore store,Pairing.Said code,Choice choice) {
        Kept kept=new Kept();
        if(choice.mine) {
            if(choice.everything!=null&&choice.everything!=Sharing.Level.GONE) {
                store.give(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,code.address,choice.everything,code.name);
                kept.everything=true;
            }
            return kept;
        }
        String group=choice.group==null?"":choice.group;
        if(choice.newGroup!=null&&!choice.newGroup.trim().isEmpty()){group=store.makeGroup(choice.newGroup);kept.grouped=true;}
        if(!group.isEmpty()&&!Groups.MINE.equals(group)){kept.groups.addAll(store.putInGroup(group,code.address,true));kept.grouped=true;}
        Sharing.Level level=choice.level==null?Sharing.Level.WRITE:choice.level;
        for(NoteStore.Branch one:choice.things) {
            Sharing.Scope scope=scopeOf(one);
            List<Sharing.Level> may=scope==null?new ArrayList<Sharing.Level>():store.mayGive(scope,one.id);
            // Can write where this device may give it; Can read where it may give only that, as a Share box would offer.
            Sharing.Level given=may.contains(level)?level:may.contains(Sharing.Level.READ)?Sharing.Level.READ:null;
            if(given==null){kept.refused++;continue;}
            store.give(scope,one.id,code.address,given,code.name);
            kept.given.add(one);
        }
        return kept;
    }

    /** What the choices changed, sent: the groups and what they give, everything, each thing given. How many could not go yet. Blocking. */
    static int send(Context where,NoteStore store,Keys keys,Kept kept) throws Exception {
        int failed=0;
        if(kept.grouped)failed+=Post.groupsChanged(where,store,keys,kept.groups).failed;
        if(kept.everything)failed+=Post.send(where,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING).failed;
        for(NoteStore.Branch one:kept.given)failed+=Post.send(where,store,keys,one.kind,one.id).failed;
        return failed;
    }

    /** Where the device now stands, said at the end of the path: its section, in two or three words. */
    static String done(String name,boolean mine){return mine?Say.t("{0} is in My devices",name):Say.t("{0} is in People",name);}
}
