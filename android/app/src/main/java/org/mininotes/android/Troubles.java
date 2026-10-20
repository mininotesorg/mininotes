// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Trouble with a contact, said at the top of the app (the owner, 2026-10-17: "in a situation where there is an issue with
 * one contact, this should be displayed directly at the top of the app, it should be rectified by the app directly and if
 * the user input is needed, clear indication in how to solve the issue should be provided"; decision 188).
 *
 * <p>Only what is trouble and not a moment's delay: something owed to a device for more than three days with nothing heard
 * from it (as the red mark says, {@link SyncMark#LONG}); a device that said it is locked while something waits for it; a
 * device whose Mininotes is too old for something that waits; somebody added who has not answered for an hour. Each with
 * what is wrong, what Mininotes does about it by itself, and what the owner can do. Holds no Android types: both apps say
 * the same words.
 */
final class Troubles {
    private Troubles(){}

    enum Kind { STUCK, LOCKED, UPDATE, LINKING }

    /** One device's trouble: who, what kind, how many things wait for it, and whether it is one of the owner's own. */
    record Trouble(String address,String name,Kind kind,int waiting,boolean mine) {
        /** What is wrong, in one sentence. */
        String wrong() {
            return switch(kind) {
                case STUCK -> Say.t("{0} has not been reached for more than three days. {1} waiting for it.",name,String.valueOf(waiting));
                case LOCKED -> Say.t("{0} is locked, so what waits for it cannot be taken in.",name);
                case UPDATE -> Say.t("{0} has an older Mininotes, which cannot take some of what waits for it.",name);
                case LINKING -> Say.t("{0} has not answered since being added.",name);
            };
        }
        /** What Mininotes does about it by itself. */
        String doing() {
            return switch(kind) {
                case STUCK,LINKING -> Say.t("Mininotes keeps trying by itself, and tried again just now.");
                case LOCKED -> Say.t("It goes by itself once Mininotes is opened there.");
                case UPDATE -> Say.t("It goes by itself once Mininotes is updated there.");
            };
        }
        /** What the owner can do. */
        String yours() {
            return switch(kind) {
                case STUCK -> mine?Say.t("Open Mininotes on {0}, connected to the internet. If {0} was reset or replaced, add it again from Add my device.",name)
                    :Say.t("Ask {0} to open Mininotes, connected to the internet. If they changed phone, add them again: scan their code.",name);
                case LOCKED -> mine?Say.t("Open Mininotes on {0} and unlock it.",name):Say.t("Ask {0} to open Mininotes and unlock it.",name);
                case UPDATE -> mine?Say.t("Update Mininotes on {0}.",name):Say.t("Ask {0} to update Mininotes.",name);
                case LINKING -> mine?Say.t("Open Mininotes on {0}. If it still does not answer, scan its code again.",name)
                    :Say.t("Ask {0} to open Mininotes. If they still do not answer, scan their code again.",name);
            };
        }
    }

    /** How long somebody added may stay silent before it is trouble. */
    static final long LINKING_LONG=60*60*1000L;

    /** Every device in trouble, the worst first: stuck, locked, too old, not answering. Blocking. */
    static List<Trouble> of(NoteStore store) {
        Map<String,int[]> by=new LinkedHashMap<>();   // address -> {stuck things, locked things, update things}
        var each=store.whereEach(store.pagesUnder(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING));
        long now=System.currentTimeMillis();
        Map<String,Boolean> heard=new java.util.HashMap<>();
        for(Map.Entry<String,Map<String,Waits.Where>> page:each.entrySet())
            for(Map.Entry<String,Waits.Where> at:page.getValue().entrySet()) {
                Waits.Where w=at.getValue();
                int[] n=by.computeIfAbsent(at.getKey(),k->new int[3]);
                if(w.stuck())n[0]++;
                if(w.locked()&&w.owed()>0)n[1]++;
                if(w.update()>0) {
                    // A device never heard from cannot be said to be old: what waits for it for days is not reached.
                    if(heard.computeIfAbsent(at.getKey(),store::everHeard))n[2]++;
                    else{NoteStore.Note note=store.get(page.getKey());if(note!=null&&now-note.updated>SyncMark.LONG)n[0]++;}
                }
            }
        List<Trouble> out=new ArrayList<>();
        Map<String,NoteStore.Contact> known=new LinkedHashMap<>();
        for(NoteStore.Contact c:store.addresses())known.put(c.address,c);
        for(Kind kind:new Kind[]{Kind.STUCK,Kind.LOCKED,Kind.UPDATE})
            for(Map.Entry<String,int[]> one:by.entrySet()) {
                int n=one.getValue()[kind.ordinal()];
                NoteStore.Contact c=known.get(one.getKey());
                if(n==0||c==null||!c.paired())continue;
                boolean already=false;for(Trouble t:out)if(t.address().equals(one.getKey()))already=true;
                if(!already)out.add(new Trouble(c.address,store.nameFor(c.address),kind,n,c.mine));
            }
        Set<String> linking=store.linkingNow();
        for(String address:linking) {
            NoteStore.Contact c=known.get(address);
            if(c==null)continue;
            long since=store.linkingSince(address);
            boolean already=false;for(Trouble t:out)if(t.address().equals(address))already=true;
            if(!already&&since>0&&now-since>LINKING_LONG)out.add(new Trouble(address,store.nameFor(address),Kind.LINKING,0,c.mine));
        }
        return out;
    }

    /** The strip's one line: the first trouble, and how many more. */
    static String line(List<Trouble> all) {
        if(all.isEmpty())return "";
        Trouble first=all.get(0);
        String one=switch(first.kind()) {
            case STUCK -> Say.t("{0} not reached for days",first.name());
            case LOCKED -> Say.t("{0} is locked",first.name());
            case UPDATE -> Say.t("{0} needs a newer Mininotes",first.name());
            case LINKING -> Say.t("{0} has not answered",first.name());
        };
        return all.size()==1?one:Say.t("{0}, and {1} more",one,String.valueOf(all.size()-1));
    }
}
