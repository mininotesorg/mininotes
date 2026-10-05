// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.List;

/**
 * Bold, italic and underline, kept as marks in the words themselves (the owner, 2026-10-03: "basic text formatting like
 * underline, with keyboard shortcuts like Ctrl+U"; asked, "marks in the text"): <b>**bold**</b>, <i>*italic*</i> and
 * <u>__underline__</u>, the marks chat apps use. Kept in the text, a note's style travels, merges and is versioned as its
 * words are, and a build from before shows the marks themselves. A mark opens and closes on the same line, around words
 * that neither start nor end with a space. Pure, so both apps draw the same runs.
 */
final class Marks {
    enum Style {
        BOLD("**"),ITALIC("*"),UNDERLINE("__");
        final String mark;
        Style(String mark){this.mark=mark;}
    }

    /** One styled stretch: its opening mark at {@code open}, its words from {@code start} to {@code end}, its closing mark ending at {@code close}. */
    record Run(Style style,int open,int start,int end,int close) {}

    /** Every styled stretch in the words, in the order they begin. */
    static List<Run> of(CharSequence text) {
        List<Run> runs=new ArrayList<>();
        if(text==null)return runs;
        int length=text.length();
        boolean[] used=new boolean[length];
        // The two-letter marks first, so a bold's stars are never read as two italics.
        find(text,Style.BOLD,used,runs);
        find(text,Style.UNDERLINE,used,runs);
        find(text,Style.ITALIC,used,runs);
        runs.sort((one,other)->Integer.compare(one.open(),other.open()));
        return runs;
    }

    private static void find(CharSequence text,Style style,boolean[] used,List<Run> runs) {
        String mark=style.mark;int width=mark.length(),length=text.length();
        int at=0;
        while(at+width<=length) {
            if(!opensAt(text,mark,at,used)){at++;continue;}
            int start=at+width;
            if(start>=length||text.charAt(start)==' '||text.charAt(start)=='\n'){at++;continue;}
            int close=-1;
            for(int look=start+1;look+width<=length;look++) {
                if(text.charAt(look)=='\n')break;
                if(opensAt(text,mark,look,used)&&text.charAt(look-1)!=' '){close=look;break;}
            }
            if(close<0){at++;continue;}
            for(int i=at;i<start;i++)used[i]=true;
            for(int i=close;i<close+width;i++)used[i]=true;
            runs.add(new Run(style,at,start,close,close+width));
            at=close+width;
        }
    }

    /** Whether the mark stands at {@code at}, unused, and is not part of a longer run of the same letter. */
    private static boolean opensAt(CharSequence text,String mark,int at,boolean[] used) {
        int width=mark.length();
        if(at<0||at+width>text.length())return false;
        for(int i=0;i<width;i++)if(used[at+i]||text.charAt(at+i)!=mark.charAt(i))return false;
        char letter=mark.charAt(0);
        // "***" is not a bold beside an italic, nor "___" an underline: a single star is not taken from a pair either.
        if(at>0&&text.charAt(at-1)==letter&&!used[at-1])return false;
        if(at+width<text.length()&&text.charAt(at+width)==letter&&!used[at+width])return false;
        return true;
    }

    /** The words after a style is put on, or taken off, what is chosen: the new words and where the choice now is. */
    record Changed(String text,int start,int end) {}

    /**
     * A style switched on what is chosen: taken off where the choice already wears it - its marks right outside it - and put
     * on otherwise. With nothing chosen, an empty pair with the cursor between, to type into.
     */
    static Changed toggle(String text,int from,int to,Style style) {
        String words=text==null?"":text;
        int start=Math.max(0,Math.min(Math.min(from,to),words.length())),end=Math.max(start,Math.min(Math.max(from,to),words.length()));
        String mark=style.mark;int width=mark.length();
        if(start-width>=0&&end+width<=words.length()&&words.startsWith(mark,start-width)&&words.startsWith(mark,end)
                &&!(width==1&&(start-2>=0&&words.charAt(start-2)=='*'||end+1<words.length()&&words.charAt(end+1)=='*')))
            return new Changed(words.substring(0,start-width)+words.substring(start,end)+words.substring(end+width),start-width,end-width);
        // Spaces round what is chosen stay outside the marks, or the marks would not read as marks.
        while(start<end&&words.charAt(start)==' ')start++;
        while(end>start&&words.charAt(end-1)==' ')end--;
        return new Changed(words.substring(0,start)+mark+words.substring(start,end)+mark+words.substring(end),start+width,end+width);
    }

    private Marks(){}
}
