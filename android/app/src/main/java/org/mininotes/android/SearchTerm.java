// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

/**
 * When what is typed in a search box is enough to look for (decision 287). Two letters, so one letter of a word does not
 * list every note; but in Chinese, Japanese and Korean one character is often a whole word, and 喷 found nothing until a
 * second was typed (a person writing in Chinese, 2026-10-29: "When search one character, no result"). Counted in
 * characters as a person sees them, so an emoji is one. Holds no Android types.
 */
final class SearchTerm {
    private SearchTerm(){}

    static boolean enough(String typed) {
        if(typed==null)return false;
        String term=typed.trim();
        int count=term.codePointCount(0,term.length());
        if(count>=2)return true;
        if(count==0)return false;
        return wordAlone(term.codePointAt(0));
    }

    /** A character that can be a word by itself: one of the Han characters, or of Japanese's or Korean's own scripts. */
    static boolean wordAlone(int c) {
        Character.UnicodeScript script;
        try{script=Character.UnicodeScript.of(c);}catch(IllegalArgumentException none){return false;}
        return script==Character.UnicodeScript.HAN||script==Character.UnicodeScript.HIRAGANA
            ||script==Character.UnicodeScript.KATAKANA||script==Character.UnicodeScript.HANGUL;
    }
}
