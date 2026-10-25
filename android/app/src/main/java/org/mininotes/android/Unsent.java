// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why something could not go, said so its owner knows what is wrong and what to do about it.
 *
 * <p>"Somebody has not been paired yet, so nothing can be sealed for them" was true and of no use: which somebody,
 * which thing, and then what? Every reason a send stops is one of these, with the device by its name, the thing by
 * its name, and - where the pad cannot put it right by itself - the one thing the owner can do, and a button that
 * goes there. Where it does put it right by itself, by trying again, it says that instead.
 *
 * <p>Holds no Android types: the phone and the PC say the same words, and they are unit tested.
 */
final class Unsent {
    private Unsent(){}

    enum Why {
        /** Paired halfway: the device is known here, but its keys never came, so nothing can be sealed for it. */
        NOT_PAIRED,
        /** Named in the list of people for a thing - it came in that list from somebody else - and never paired here. */
        ONLY_LISTED,
        /** Linked through that list, and asked to answer; nothing goes to it until it does. See {@link Linking}. */
        LINKING,
        /** Paired, and nothing of it could be reached just now: not on, not on this network, or no relay took it. */
        NOT_REACHED,
        /** Notes go only between the owner's devices, directly, and that device is not in reach of this one. */
        NOT_IN_REACH,
        /** This device's own connection is not up yet. */
        NOT_CONNECTED,
        /** A note from before sharing, whose id cannot travel. */
        TOO_OLD,
        /**
         * The device's build is from before collections nested (see {@link Things}): it knows three levels, and this does
         * not fit them. It waits, and goes the moment the device says it knows about trees.
         */
        NEEDS_UPDATE,
        /** Anything else, said in the words it came with. */
        OTHER
    }

    /** What the one button under the words does; NONE where waiting is the whole of it. */
    enum Fix { PAIR, TRAVEL, NONE }

    /** One reason, about one device and one thing. */
    static final class Problem {
        final Why why; final String who,thing,listedIn,detail;
        /** For {@link Why#ONLY_LISTED}, the address the list names, so the box can take them off it; empty otherwise. */
        final String address;
        /**
         * @param who      the device's name, or empty where it is not known
         * @param thing    what was being sent: a collection's or a book's name, a note's title; empty for everything
         * @param listedIn for {@link Why#ONLY_LISTED}, the thing whose list of people names them; empty otherwise
         * @param detail   for {@link Why#OTHER}, the words it failed with
         */
        Problem(Why why,String who,String thing,String listedIn,String detail){this(why,who,thing,listedIn,detail,"");}
        Problem(Why why,String who,String thing,String listedIn,String detail,String address) {
            this.why=why;this.who=who==null?"":who.trim();this.thing=thing==null?"":thing.trim();
            this.listedIn=listedIn==null?"":listedIn.trim();this.detail=detail==null?"":detail.trim();
            this.address=address==null?"":address.trim();
        }
        private String key(){return why+"\u0000"+who+"\u0000"+listedIn;}
    }

    /** Which reason an error is, from the words it was thrown with; the words kept for anything not known. */
    static Why of(String message) {
        String said=message==null?"":message;
        if(said.equals(Direct.WAITS))return Why.NOT_IN_REACH;
        if(said.contains("node is not running"))return Why.NOT_CONNECTED;
        if(said.contains("older than sharing"))return Why.TOO_OLD;
        // The network's own words for a device it has no way to, said in the app's words instead (the owner, 2026-10-17:
        // "no reachable address for …" in a box).
        if(said.contains("could not be looked up")||said.contains("could not be reached")||said.contains("no reachable address"))return Why.NOT_REACHED;
        return Why.OTHER;
    }

    /**
     * Which kind of reason a failure was, for a log line: the kind and never the words, which can name a device or a
     * note - and never the exception's name alone either, which says nothing to whoever reads the log.
     */
    static String reason(Throwable failed) {
        Why why=of(failed==null?null:failed.getMessage());
        return why==Why.OTHER?"something else ("+(failed==null?"nothing said":failed.getClass().getSimpleName())+")"
            :why.name().toLowerCase(java.util.Locale.ROOT).replace('_',' ');
    }

    /*
     * Every sentence below is one phrase of the word lists, said in the language in use (decision 256: "Pro could not reach
     * PixelPro" stayed English on a PC set to French): a name or a thing's name is what goes in its {0}, and where there
     * is no name, or the device is a phone or a PC, the sentence is a phrase of its own, never words glued on.
     */

    /** Whether "here" is a PC: its sentences say "this PC", a phone's "this phone". */
    private static boolean pc(String here){return "this PC".equals(here);}

    /**
     * What is wrong, in one or two sentences: the device and the thing by their names.
     *
     * @param here what this device is called in a sentence: "this phone", "this PC"
     */
    static String said(Problem p,String here) {
        boolean pc=pc(here),named=!p.who.isEmpty(),thing=!p.thing.isEmpty();
        switch(p.why) {
            case NOT_PAIRED:
                if(thing)return named
                    ?Say.t(pc?"{0} has not been paired with this PC yet, so “{1}” cannot be sealed for it.":"{0} has not been paired with this phone yet, so “{1}” cannot be sealed for it.",p.who,p.thing)
                    :Say.t(pc?"A device has not been paired with this PC yet, so “{0}” cannot be sealed for it.":"A device has not been paired with this phone yet, so “{0}” cannot be sealed for it.",p.thing);
                return named
                    ?Say.t(pc?"{0} has not been paired with this PC yet, so it cannot be sealed for it.":"{0} has not been paired with this phone yet, so it cannot be sealed for it.",p.who)
                    :Say.t(pc?"A device has not been paired with this PC yet, so it cannot be sealed for it.":"A device has not been paired with this phone yet, so it cannot be sealed for it.");
            case ONLY_LISTED: return notLinked(p.who,p.listedIn.isEmpty()?p.thing:p.listedIn,here);
            case LINKING: return linking(p.who);
            case NOT_REACHED:
                if(!thing)return named?Say.t("{0} could not be reached just now.",p.who):Say.t("That device could not be reached just now.");
                return named?Say.t("“{0}” could not reach {1} just now.",p.thing,p.who):Say.t("“{0}” could not reach that device just now.",p.thing);
            case NOT_IN_REACH:
                if(thing)return named
                    ?Say.t("{0} is not in reach, so “{1}” waits: notes go only between your devices, straight from one to the other, and it is not on this network.",p.who,p.thing)
                    :Say.t("A device is not in reach, so “{0}” waits: notes go only between your devices, straight from one to the other, and it is not on this network.",p.thing);
                return named
                    ?Say.t("{0} is not in reach, so it waits: notes go only between your devices, straight from one to the other, and it is not on this network.",p.who)
                    :Say.t("A device is not in reach, so it waits: notes go only between your devices, straight from one to the other, and it is not on this network.");
            case NOT_CONNECTED: return Say.t(pc?"Mininotes is not connected yet on this PC.":"Mininotes is not connected yet on this phone.");
            case TOO_OLD: return thing?Say.t("“{0}” was written before sharing existed here, and cannot travel.",p.thing)
                :Say.t("A note was written before sharing existed here, and cannot travel.");
            case NEEDS_UPDATE: return named?Say.t("{0} needs to update Mininotes to receive this.",p.who):Say.t("A device needs to update Mininotes to receive this.");
            default:
                // The words it failed with, said as they came: the network's or the system's, looked up where they are listed.
                if(!p.detail.isEmpty())return Say.t(p.detail);
                return thing?Say.t("Something went wrong sending “{0}”.",p.thing):Say.t("Something went wrong sending it.");
        }
    }

    /** What the owner can do about it; or, where the pad puts it right by itself, that it will. */
    static String remedy(Problem p,String here) {
        boolean pc=pc(here),named=!p.who.isEmpty();
        switch(p.why) {
            case NOT_PAIRED:
                return named?Say.t("Add it: open People and devices, show your code, and scan it on {0}.",p.who)
                    :Say.t("Add it: open People and devices, show your code, and scan it on that device.");
            case ONLY_LISTED: return Say.t("Link with them to send it, or take them off if they should not have it.");
            case LINKING: return named?Say.t("If they do not, open Mininotes on {0}, or link with them by hand.",p.who)
                :Say.t("If they do not, open Mininotes on that device, or link with them by hand.");
            case NOT_REACHED:
                if(named)return Say.t(pc?"Open Mininotes on {0}, on the same Wi-Fi as this PC if you can. It stays waiting and goes by itself."
                    :"Open Mininotes on {0}, on the same Wi-Fi as this phone if you can. It stays waiting and goes by itself.",p.who);
                return Say.t(pc?"Open Mininotes on that device, on the same Wi-Fi as this PC if you can. It stays waiting and goes by itself."
                    :"Open Mininotes on that device, on the same Wi-Fi as this phone if you can. It stays waiting and goes by itself.");
            case NOT_IN_REACH: return Say.t("Put both on the same Wi-Fi, or let helpers carry it in How notes travel. Until then it stays waiting.");
            case NOT_CONNECTED: return Say.t(pc?"Check that this PC is online. It goes by itself once it is connected."
                :"Check that this phone is online. It goes by itself once it is connected.");
            case TOO_OLD: return Say.t("Copy its words into a new note, and share that one.");
            case NEEDS_UPDATE: return named?Say.t("It stays waiting, and goes by itself once Mininotes is updated on {0}.",p.who)
                :Say.t("It stays waiting, and goes by itself once Mininotes is updated on that device.");
            default: return Say.t("It stays waiting and is tried again by itself.");
        }
    }

    static Fix fix(Problem p) {
        switch(p.why) {
            case NOT_PAIRED: case ONLY_LISTED: case LINKING: return Fix.PAIR;
            case NOT_IN_REACH: return Fix.TRAVEL;
            default: return Fix.NONE;
        }
    }

    /** The button's words, or null for none. */
    static String button(Fix fix){return fix==Fix.PAIR?Say.t("Add it"):fix==Fix.TRAVEL?Say.t("How notes travel"):null;}

    /** The button's words for this reason: somebody only listed is linked with by their name. */
    static String button(Problem p) {
        return listed(p)?link(p.who):button(p==null?Fix.NONE:fix(p));
    }

    /** Whether it is about somebody a list names - not linked, or being linked - whose fix is linking by hand. */
    static boolean listed(Problem p){return p!=null&&(p.why==Why.ONLY_LISTED||p.why==Why.LINKING);}

    /**
     * Somebody a list names that this device has linked with through it and asked to answer: nothing goes until they do.
     * Said the same in the could-not-go box and in the box their round opens.
     */
    static String linking(String who) {
        return who==null||who.isBlank()?Say.t("Linking with that device… it goes once they answer.")
            :Say.t("Linking with {0}… it goes once they answer.",who.trim());
    }

    /**
     * Somebody on a thing's list of people this device was never linked with: who, and on what. Said the same in
     * the could-not-go box and in the box their round on a note opens.
     */
    static String notLinked(String who,String listedIn,String here) {
        boolean pc=pc(here),named=who!=null&&!who.isBlank(),on=listedIn!=null&&!listedIn.isBlank();
        if(on)return named
            ?Say.t(pc?"{0} is listed for “{1}”, but this PC is not linked to them.":"{0} is listed for “{1}”, but this phone is not linked to them.",who.trim(),listedIn.trim())
            :Say.t(pc?"A device is listed for “{0}”, but this PC is not linked to them.":"A device is listed for “{0}”, but this phone is not linked to them.",listedIn.trim());
        return named
            ?Say.t(pc?"{0} is listed for this, but this PC is not linked to them.":"{0} is listed for this, but this phone is not linked to them.",who.trim())
            :Say.t(pc?"A device is listed for this, but this PC is not linked to them.":"A device is listed for this, but this phone is not linked to them.");
    }

    /**
     * What waits for a device whose build is from before collections nested (see {@link Why#NEEDS_UPDATE}): "Ana's phone
     * needs to update Mininotes to receive this", with no full stop, so the words about an amber mark (see {@link Waits})
     * list it the same way.
     */
    static String needsUpdate(String who) {
        return who==null||who.isBlank()?Say.t("A device needs to update Mininotes to receive this")
            :Say.t("{0} needs to update Mininotes to receive this",who.trim());
    }

    /** The one primary action for somebody only listed. */
    static String link(String who){return who==null||who.isBlank()?Say.t("Link with them"):Say.t("Link with {0}",who.trim());}

    /** The quiet second action for somebody only listed, or null where nothing lists them. */
    static String takeOff(Problem p) {
        return !listed(p)||p.address.isEmpty()?null:takeOff(p.listedIn);
    }
    static String takeOff(String listedIn) {
        return listedIn==null||listedIn.isBlank()?Say.t("Take them off the list"):Say.t("Take them off “{0}”",listedIn.trim());
    }

    /** The same reason about the same device said once, however many notes it stopped. In the order they came. */
    static List<Problem> distinct(List<Problem> all) {
        Map<String,Problem> once=new LinkedHashMap<>();
        if(all!=null)for(Problem p:all)once.putIfAbsent(p.key(),p);
        return new ArrayList<>(once.values());
    }

    /** The reason whose fix is the box's one button: a pairing first, since nothing else puts that right. */
    static Problem first(List<Problem> all) {
        List<Problem> once=distinct(all);
        for(Problem p:once)if(fix(p)==Fix.PAIR)return p;
        for(Problem p:once)if(fix(p)!=Fix.NONE)return p;
        return once.isEmpty()?null:once.get(0);
    }

    /** The box's title: whether any of it went. */
    static String title(int sent,int failed) {
        if(sent<=0)return Say.t("It could not go");
        return failed==1?Say.t("Some of it went, and one note could not"):Say.t("Some of it went, and {0} notes could not",failed);
    }

    /** The box's title, naming the device where one device is the whole of it: "“Transfer” could not reach Ana's phone". */
    static String title(List<Problem> all,int sent,int failed) {
        List<Problem> once=distinct(all);
        if(once.size()!=1||once.get(0).who.isEmpty())return title(sent,failed);
        Problem p=once.get(0);
        if(sent>0)return Say.t("Some of it went, but not to {0}",p.who);
        if(p.why==Why.LINKING)return Say.t("Linking with {0}",p.who);
        String what=!p.thing.isEmpty()?p.thing:p.listedIn;
        return what.isEmpty()?Say.t("It could not reach {0}",p.who):Say.t("“{0}” could not reach {1}",what,p.who);
    }

    /** The box's words: each reason and what to do about it, a paragraph each; at most three, then how many more. */
    static String words(List<Problem> all,String here) {
        List<Problem> once=distinct(all);
        StringBuilder out=new StringBuilder();
        for(int at=0;at<once.size()&&at<3;at++) {
            if(out.length()>0)out.append("\n\n");
            out.append(said(once.get(at),here)).append(' ').append(remedy(once.get(at),here));
        }
        int more=once.size()-3;
        if(more>0)out.append("\n\n").append(more==1?Say.t("1 more reason, the same way."):Say.t("{0} more reasons, the same way.",more));
        return out.length()==0?Say.t("It stays waiting and is tried again by itself."):out.toString();
    }
}
