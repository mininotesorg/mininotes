// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.HashSet;
import java.util.Set;

/**
 * Whose files, notes and folders are accepted here without asking (the owner, 2026-10-28, decision 269: "we have to make it
 * that we dont need to accept files shared with my contact or at least it should be optional").
 *
 * <p>Somebody else's things wait to be accepted: a file sent on its own (see {@link Drop}, {@link Drop#ASKING}) and a note, a
 * folder or a file shared for the first time (decision 109). Both ask the same question, "do you want this from them?", so one
 * switch on the person's page answers both, off unless it is turned on. On, what they send comes in as if Accept had been
 * pressed the moment it arrived, as from one of my own devices: it goes where Accept would put it, and nothing is told "it
 * waits", so their tick says they have it. Rights are as they gave them; Accept never changed those either.
 *
 * <p>Kept per device, in its settings, as the blocked senders are: a list of addresses, never sent to anybody. A blocked
 * sender is never accepted, whatever the list says, and a device not paired here is asked nothing at all.
 *
 * <p>Two lists: who is accepted without asking, and who the owner said Keep asking to when it was offered after an Accept, so
 * it is not offered again for them (their page still has the switch).
 *
 * <p>Holds no Android or Swing types, so both apps use it and it is unit tested.
 */
final class WithoutAsking {
    private WithoutAsking(){}

    /** The settings keys: the addresses accepted without asking, and those not to be offered it again. */
    static final String KEY="acceptsWithoutAsking", KEEP_ASKING="keepAsking";

    /** The list with one address put in or taken out; null where nothing changes. */
    static Set<String> set(Set<String> kept,String address,boolean on) {
        if(address==null||address.isEmpty())return null;
        Set<String> out=kept==null?new HashSet<>():new HashSet<>(kept);
        boolean changed=on?out.add(address):out.remove(address);
        return changed?out:null;
    }

    /** Whether what this device sends is taken in at once: on its list and not blocked. One of my own devices never asks anyway. */
    static boolean takes(Set<String> kept,String address,boolean blocked) {
        return address!=null&&!address.isEmpty()&&!blocked&&kept!=null&&kept.contains(address);
    }

    /**
     * Whether to offer the switch right after Accept was pressed by hand: somebody paired here who is not one of my devices,
     * not blocked, not accepted without asking already, and not told Keep asking before.
     */
    static boolean offer(Set<String> kept,Set<String> keepAsking,String address,boolean paired,boolean mine,boolean blocked) {
        if(address==null||address.isEmpty()||!paired||mine||blocked)return false;
        return !(kept!=null&&kept.contains(address))&&!(keepAsking!=null&&keepAsking.contains(address));
    }

    // ---- words, each one template in every language (decision 256) ------------------------------------------

    /** The switch on a person's page. */
    static final String SWITCH="Accept everything without asking";
    /** The box's one button, and the quiet one beside it. */
    static final String YES="Accept without asking", KEEP="Keep asking";
    /** Where it is changed later, under the question. */
    static final String WHERE="You can change this on their page in People and devices.";

    /** The quiet line under the switch. */
    static String under(String who){return Say.t("Files, notes and folders from {0} come in at once.",who);}

    /** The question put right after Accept. */
    static String question(String who){return Say.t("Accept everything from {0} without asking?",who);}

    /** Said once the switch has been changed, from their page or from the question. */
    static String done(String who,boolean on) {
        return on?Say.t("Everything from {0} is accepted without asking.",who):Say.t("Everything from {0} waits for you to accept it.",who);
    }
}
