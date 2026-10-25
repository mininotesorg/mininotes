// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

/**
 * The ends of lines in words put on a page, as the page knows them: one character, '\n' (decision 228).
 *
 * <p>A text copied elsewhere and pasted in can end its lines otherwise: "\r\n" or a lone '\r' from Windows or an old Mac, and
 * the Unicode paragraph and line separators (U+2029, U+2028) or NEXT LINE (U+0085) that some apps write between paragraphs.
 * Android lays out a line only at '\n', so those paragraphs ran on as one (feedback to the owner, 2026-10-21: "When copying
 * and pasting text into MiniNotes, the paragraphs disappear, whereas Xiaomi's built-in notes app retains them"). Nothing
 * else in the words is changed.
 */
final class LineEnds {
    private LineEnds(){}

    /** Whether any of these words ends a line some other way than '\n'. */
    static boolean other(CharSequence words) {
        if(words==null)return false;
        for(int i=0;i<words.length();i++){char c=words.charAt(i);if(c=='\r'||c==' '||c==' '||c=='\u0085')return true;}
        return false;
    }

    /** The same words, every end of line a '\n': "\r\n" one of them, not two. */
    static String plain(CharSequence words) {
        if(words==null)return "";
        StringBuilder out=new StringBuilder(words.length());
        for(int i=0;i<words.length();i++) {
            char c=words.charAt(i);
            if(c=='\r'){out.append('\n');if(i+1<words.length()&&words.charAt(i+1)=='\n')i++;}
            else if(c==' '||c==' '||c=='\u0085')out.append('\n');
            else out.append(c);
        }
        return out.toString();
    }
}
