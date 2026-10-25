// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.List;

/**
 * Something another person shares with me for the first time, and the words both apps say about it (the owner, 2026-10-06:
 * "When someone shares something with me, I need an alert, a pop-up at first telling me, and it should go in Shared with me,
 * and there I should have an accept or refuse, and refuse could be silent or inform the sender with the option to send a
 * message back"; docs/HOME.md, decision 109). It arrives and is kept, hidden everywhere but its line in Shared with me, until
 * it is accepted or refused. One place for the words, so the phone and the PC say the same thing in the same place (decision
 * 105). Holds no Android types, so the words are unit tested.
 */
final class FirstShare {
    /** The part of Shared with me that holds what waits, first in it. */
    static final String WAITING="Waiting for you";
    static final String ACCEPT="Accept", REFUSE="Refuse", SEE_IT="See it", LATER="Later";
    /** On the thing itself, once it is clicked: who sent it, and, where they are not a contact, stopping them. */
    static final String SEE_SENDER="See sender", BLOCK="Block sender";
    static final String BLOCKED_SAID="Blocked. They are listed in People and devices.";
    /** The part of People and devices that lists them, and what an empty one says. */
    static final String BLOCKED="Blocked", NOBODY_BLOCKED="Nobody blocked.";
    static final String CAN_SHARE_AGAIN="They can share with you again.";
    static final String QUIETLY="Refuse quietly", TELL="Refuse and tell them", WORDS_HINT="Add a few words, if you like";
    static final String QUIETLY_DOES="They are not told.", TELL_DOES="They are told you refused it, with your words if you write any.";
    /** What a person's line in a Share box says once they refused the thing. */
    static final String REFUSED="Refused";

    private FirstShare(){}

    /**
     * The pop-up's one line: who shared what. One thing by its name; several counted, with everybody who shared them named.
     *
     * @param senders who shared them, each once, in the order they came
     */
    static String title(List<String> senders,int count,String onlyName) {
        String who=names(senders);
        if(count>1)return Say.t("{0} shared {1} things with you",who,count);
        return onlyName==null||onlyName.isBlank()?Say.t("{0} shared something with you",who):Say.t("{0} shared “{1}” with you",who,onlyName.trim());
    }

    /** "Ana", "Ana and Ben", "Ana, Ben and Cy": people as a sentence names them. Somebody where nobody is known. */
    static String names(List<String> who) {
        if(who==null||who.isEmpty())return Say.t("Somebody");
        if(who.size()==1)return who.get(0);
        return Parlons.names(who);
    }

    /** What it is and the rights given, under the pop-up's line: "A folder · Can write". */
    static String what(NoteStore.Branch.Kind kind,Sharing.Level level) {
        String it=Say.t(kind==NoteStore.Branch.Kind.PAGE?"A note":kind==NoteStore.Branch.Kind.FILE||kind==NoteStore.Branch.Kind.WAITING?"A file":"A folder");
        return level==null||level==Sharing.Level.GONE?it:it+" · "+Say.t(level.words());
    }

    /** A line in Waiting for you, under its name: "from Ana · Can write". */
    static String from(String who,Sharing.Level level) {
        String said=who==null||who.isBlank()?Say.t("from somebody"):Say.t("from {0}",who.trim());
        return level==null||level==Sharing.Level.GONE?said:said+" · "+Say.t(level.words());
    }

    /** The Refuse box's title. */
    static String refuseTitle(String name){return name==null||name.isBlank()?Say.t("Refuse it?"):Say.t("Refuse “{0}”?",name.trim());}

    /** The blocked-sender row's question, before the record is deleted. */
    static String deleteBlocked(String name) {
        return name==null||name.isBlank()?Say.t("Delete this sender?"):Say.t("Delete “{0}”?",name.trim());
    }

    /** Said once it is accepted, where it is now: Home for a note or a folder, where files shared with you go for a file. */
    static String accepted(String name,String where) {
        boolean home=where==null||where.isBlank()||"Home".equals(where);
        if(name==null||name.isBlank())return home?Say.t("It is on Home now"):Say.t("It is in {0} now",where);
        return home?Say.t("“{0}” is on Home now",name.trim()):Say.t("“{0}” is in {1} now",name.trim(),where);
    }

    /** Said once it is refused. */
    static String refusedSaid(boolean told,int reached) {
        if(!told)return Say.t("Refused quietly. It is deleted here");
        return Say.t(reached>0?"Refused, and they were told. It is deleted here":"Refused. They could not be reached now, so they were not told. It is deleted here");
    }

    /** The sender's notice: "Parisa refused “Saturday market”", and their words under it where they wrote any. */
    static String notice(String who,String name,String words) {
        boolean somebody=who==null||who.isBlank(),something=name==null||name.isBlank();
        String said=somebody?(something?Say.t("Somebody refused something"):Say.t("Somebody refused “{0}”",name.trim()))
            :(something?Say.t("{0} refused something",who.trim()):Say.t("{0} refused “{1}”",who.trim(),name.trim()));
        // Their own words, as they wrote them.
        return words==null||words.isBlank()?said:said+"\n“"+words.trim()+"”";
    }

}
