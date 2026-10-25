// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.List;

/**
 * What the open note's syncing is doing, said in a few quiet words on the line under its title, after its mark
 * and its people.
 *
 * <p>It was said at the foot of the window, on the PC's bar and the phone's strip, while the mark it was about sat
 * at the top: the eye went up to see whether the note had gone and the answer was at the bottom. Now the words are
 * beside the mark, in its colour - amber while it goes, green when it has, red when it could not - and a tap on them
 * does what a tap on the mark does. What is not about the open note stays where it was.
 *
 * <p>Holds no Android or Swing types: the phone and the PC say the same words, and they are unit tested.
 */
final class NoteLine {
    private NoteLine(){}

    /** Which colour the words wear: the mark's own for the same thing. */
    enum Tone {
        GOING(SyncMark.WAITING), DONE(SyncMark.GONE), FAILED(SyncMark.STUCK);
        final SyncMark mark;
        Tone(SyncMark mark){this.mark=mark;}
    }

    static final String SAVING="Saving…",SENDING="Sending…",SYNCING="Syncing…";

    /** Words that stay up until the work that said them says how it ended: anything still going on. */
    static boolean stays(String words){return words!=null&&words.trim().endsWith("…");}

    /*
     * The line's own sentences come out in the language in use (decision 256); the three words above stay English, and are
     * said in it where they are put on the line, which compares them as they are.
     */

    /** "Sending to Ana…": who, where the note reaches few enough to name. */
    static String sending(List<String> names) {
        if(names==null||names.isEmpty())return Say.t(SENDING);
        if(names.size()==1)return Say.t("Sending to {0}…",names.get(0));
        if(names.size()==2)return Say.t("Sending to {0} and {1}…",names.get(0),names.get(1));
        return Say.t("Sending to {0} and {1} others…",names.get(0),names.size()-1);
    }

    /** Whether the line says a sending is going on: "Sending…" or "Sending to Ana…", in English or the language in use. */
    static boolean isSending(String words) {
        if(words==null)return false;
        for(String one:new String[]{SENDING,"Sending to {0}…","Sending to {0} and {1}…","Sending to {0} and {1} others…"})
            if(one.equals(words)||Say.fits(one,words))return true;
        return false;
    }

    /** Gone, said by name; that they have it is theirs to say, and the mark turns to a tick when they do. */
    static String sent(List<String> names) {
        if(names==null||names.isEmpty())return Say.t("Sent, waiting for them to confirm");
        if(names.size()==1)return Say.t("Sent to {0}, waiting for them to confirm",names.get(0));
        if(names.size()==2)return Say.t("Sent to {0} and {1}, waiting for them all to confirm",names.get(0),names.get(1));
        return Say.t("Sent to {0} and {1} others, waiting for them all to confirm",names.get(0),names.size()-1);
    }

    /** Nothing was waiting, and the others were asked for what they have. */
    static final String NOTHING="Nothing new to send. Asked the others for theirs.";

    /**
     * What did not go, short enough for the line, with the reasons behind a tap (see Unsent). Somebody still being
     * linked with is not a failure: it goes by itself once they answer.
     */
    static String couldNot(List<Unsent.Problem> problems,int sent) {
        Unsent.Problem first=Unsent.first(problems);
        if(first!=null&&first.why==Unsent.Why.LINKING)return Unsent.linking(first.who);
        if(sent>0)return Say.t("Some of it went. See why");
        if(first!=null&&!first.who.isEmpty()&&Unsent.distinct(problems).size()==1)return Say.t("Could not reach {0}. See why",first.who);
        return Say.t("Could not go. See why");
    }

    /** The colour of what couldNot says: amber for a link that is on its way, red for the rest. */
    static Tone couldNotTone(List<Unsent.Problem> problems) {
        Unsent.Problem first=Unsent.first(problems);
        return first!=null&&first.why==Unsent.Why.LINKING?Tone.GOING:Tone.FAILED;
    }
}
