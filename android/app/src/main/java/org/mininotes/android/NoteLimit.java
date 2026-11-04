// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.List;

/**
 * How long a note may be, and what its line says near that (decision 282). The phone stopped taking letters at the limit
 * and said nothing; the PC said it in English with the numbers glued in. Both now say it in one template per language,
 * from nine tenths of the way. Lengths are counted as the editors count them, so an emoji may count as two. Holds no
 * Android types.
 *
 * <p>Per tab since decision 319 (the owner, 2026-11-03: "The app shows that we are getting close the the length limit, I dont
 * have the impression there is so much text ... could we set the limit per tabs?"): a note's tabs are one text (Sections), and
 * the cap was on all of them together, their tab lines and the marks of bold and slant included, so eight tabs of a few
 * thousand letters each read as a note nearly full. Each tab now holds {@link #TAB}, and the line counts the tab open; the
 * whole note holds {@link #NOTE}, well inside what one sealed envelope carries (Envelope.MAX_TEXT), accents and all.
 */
final class NoteLimit {
    /** A tab's words, a note's words with all its tabs, and its title, at most. A note of one tab is held to TAB. */
    static final int TAB=24000, NOTE=100000, TITLE=160;
    /** The limit before decision 319, on the whole note: what a build before it takes from a backup or an import. */
    static final int BODY=TAB;
    /** From here the note's line says how far it has come. */
    static final int TAB_NEAR=TAB*9/10, NOTE_NEAR=NOTE*9/10, TITLE_NEAR=TITLE*9/10;
    /** Kept for the callers that ask by the old name: the near mark of one tab, which a note without tabs is. */
    static final int BODY_NEAR=TAB_NEAR;

    private NoteLimit(){}

    /** What is over a limit, said as a reason a note cannot be kept so; null where nothing is. Every tab is measured. */
    static String over(int title,String body) {
        String text=body==null?"":body;
        if(title>TITLE)return Say.t("Title has {0} characters; the limit is {1}.",count(title),count(TITLE));
        if(text.length()>NOTE)return Say.t("Note text has {0} characters; the limit is {1}.",count(text.length()),count(NOTE));
        List<Sections.Section> tabs=Sections.of(text);
        for(Sections.Section one:tabs) {
            int length=one.end-one.start;
            if(length<=TAB)continue;
            return tabs.size()<2?Say.t("Note text has {0} characters; the limit is {1}.",count(length),count(TAB))
                :Say.t("The tab {0} has {1} characters; the limit is {2}.",one.called(tabs.indexOf(one)),count(length),count(TAB));
        }
        return null;
    }

    /**
     * The note's line near a limit or over it; null while it is far from all of them. Measured from lengths alone, so the editors
     * can ask on every key without making the whole note's text: the title, the whole note, and the tab open.
     *
     * @param tabs how many tabs the note has: with one, its tab is the note, and the line says so
     */
    static String near(int title,int note,int tab,int tabs) {
        if(title>TITLE)return Say.t("Title has {0} characters; the limit is {1}.",count(title),count(TITLE));
        if(note>NOTE)return Say.t("Note text has {0} characters; the limit is {1}.",count(note),count(NOTE));
        if(tab>TAB)return tabs<2?Say.t("Note text has {0} characters; the limit is {1}.",count(tab),count(TAB))
            :Say.t("This tab has {0} characters; the limit is {1}.",count(tab),count(TAB));
        if(title>=TITLE_NEAR)return Say.t("Title: {0} of {1} characters.",count(title),count(TITLE));
        if(tab>=TAB_NEAR)return tabs<2?Say.t("Note text: {0} of {1} characters.",count(tab),count(TAB))
            :Say.t("This tab: {0} of {1} characters.",count(tab),count(TAB));
        if(note>=NOTE_NEAR)return Say.t("All the tabs: {0} of {1} characters.",count(note),count(NOTE));
        return null;
    }

    /** A note of one tab, by lengths alone. */
    static String near(int title,int body){return near(title,body,body,1);}

    /** A count as the language in use writes one: 24,000 in English, 24 000 in French. */
    static String count(int n) {
        return java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag(Say.language())).format(n);
    }
}
