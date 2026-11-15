// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.List;

/**
 * Writing a note cannot take, made into ordinary notes (decision 328; the owner, 2026-11-05: "For me everything is saved
 * directly, there is no difference between draft and other notes"). The PC kept such writing aside as a "recovery draft"
 * when the note was too long, gone, or read only; now it is notes like any other: the words cut at line ends into pieces
 * each within {@link NoteLimit}, and a title too long for one kept whole at the top of the words. Holds no Android types.
 */
final class NoteSplit {
    private NoteSplit(){}

    /**
     * A title and words that fit one note: the words are the given words from {@code from} to {@code to}, after {@code lead}
     * letters that are not from them (a title too long, put first).
     */
    static final class Piece {
        final String title,body;final int lead,from,to;
        Piece(String title,String body,int lead,int from,int to){this.title=title;this.body=body;this.lead=lead;this.from=from;this.to=to;}
        /** Who wrote each of its letters, given who wrote the words; null where that is not known. */
        Writers writers(Writers all) {
            if(all==null)return null;
            Writers own=all.slice(from,to);
            return lead==0?own:Writers.join(Writers.of(Writers.ME,lead),own);
        }
    }

    /**
     * The writing as pieces each within the limits, in order, none lost: the first carries the title (cut to the limit,
     * the whole of it then the first line of its words), the next ones nothing but their words. Always at least one.
     */
    static List<Piece> pieces(String title,String body) {
        String name=title==null?"":title,words=body==null?"":body;int lead=0;
        if(name.length()>NoteLimit.TITLE){words=name+"\n"+words;lead=name.length()+1;name=cut(name,NoteLimit.TITLE);}
        List<Piece> out=new ArrayList<>();
        int at=0;
        do {
            int end=longest(out.isEmpty()?name.length():0,words,at);
            out.add(new Piece(out.isEmpty()?name:"",words.substring(at,end),Math.max(0,Math.min(end,lead)-at),Math.max(0,at-lead),Math.max(0,end-lead)));
            at=end;
        } while(at<words.length());
        return out;
    }

    /**
     * Where the longest piece from {@code at} ends: at a line end where one fits (found by halving, as a shorter piece of
     * a piece that fits always fits), else as many letters as one tab holds, never between the two halves of an emoji.
     */
    private static int longest(int title,String words,int at) {
        if(NoteLimit.over(title,words.substring(at))==null)return words.length();
        List<Integer> ends=new ArrayList<>();
        for(int i=words.indexOf('\n',at);i>=0;i=words.indexOf('\n',i+1))ends.add(i+1);
        int low=0,high=ends.size()-1,best=-1;
        while(low<=high){int mid=(low+high)>>>1;if(NoteLimit.over(title,words.substring(at,ends.get(mid)))==null){best=mid;low=mid+1;}else high=mid-1;}
        if(best>=0)return ends.get(best);
        int end=Math.min(words.length(),at+NoteLimit.TAB);
        if(end<words.length()&&Character.isHighSurrogate(words.charAt(end-1)))end--;
        return Math.max(at+1,end);
    }

    /** At most {@code most} letters, never half an emoji. */
    static String cut(String s,int most) {
        if(s.length()<=most)return s;
        int end=most;if(Character.isHighSurrogate(s.charAt(end-1)))end--;
        return s.substring(0,end);
    }

    /** "Plan (copy)", "Plan (continued)", "Plan (continued 3)": within the title's limit, the name cut where it must be. */
    static String copyTitle(String title,int nth,boolean copy) {
        String name=title==null||title.isBlank()?Say.t("Untitled"):title;
        String made=named(name,nth,copy);
        if(made.length()<=NoteLimit.TITLE)return made;
        return named(cut(name,Math.max(1,NoteLimit.TITLE-(made.length()-name.length()))),nth,copy);
    }
    private static String named(String name,int nth,boolean copy) {
        if(copy)return nth<=1?Say.t("{0} (copy)",name):Say.t("{0} (copy {1})",name,String.valueOf(nth));
        return nth<=1?Say.t("{0} (continued)",name):Say.t("{0} (continued {1})",name,String.valueOf(nth));
    }
}
