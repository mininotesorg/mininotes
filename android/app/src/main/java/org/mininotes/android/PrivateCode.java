// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The codes typed anywhere that open a private space, or make one (the owner, 2026-10-06: "Could we rename it privatespace
 * ... every time I type privatespace//: with a new password it will create a new independent space, and to access an
 * existing one I type private//: with an existing password?"; decision 116). The same on both apps. One word now,
 * privatespace, where there were two (privatenote, privatefolder) before (decision 111).
 *
 * <p>{@code privatespace//:} and {@code private//:}, in any case, with a space allowed after "private" because a phone's
 * keyboard puts one there. The moment the colon is typed the code is taken out of the text, at once, before anything can
 * write it down, keep it as a version or an undo, or send it; what is typed next goes into a password field of its own (the
 * keyboard told it is a password), and Enter opens. {@code private//:} only ever opens: typed with a password nothing
 * answers to, nothing happens and nothing is said. {@code privatespace//:} opens where a space answers, and otherwise asks
 * the password a second time and makes a new, independent space (the owner, 2026-10-06: every new password a new space, any
 * number of them; decision 116).
 *
 * <p>The beginning of a code is in the text for a moment while it is typed: while the word at the caret could still become
 * one ({@link #typing}), the writing down that happens by itself waits for the next key; a save that cannot wait takes the
 * unfinished code out first ({@link #unfinished}). Holds no Android types.
 *
 * <p>And a guard at the door, {@link #scrub} (the owner, 2026-10-06: "We have to make sure that if we type privatenotes or
 * private//: or anything related to the private elements in a shared element, this is not shared, how can we do that?";
 * decision 114): whatever got past the catching, a code and the rest of its line are taken out of every ordinary note's
 * save, every version kept, every parcel sealed for another device, every note that arrives from one, and every text
 * handed to another app. The guard still catches the old privatenote//: and privatefolder//: as well (the owner,
 * 2026-10-06; decision 116), so a code left in a note by a build before the rename is taken out just the same.
 */
final class PrivateCode {
    enum Kind { SPACE, OPEN }

    /** The codes typed to open or make: one word, privatespace (a phone's keyboard may put a space after "private"). */
    private static final String[] CODES={"privatespace//:","private space//:","private//:"};
    private static final int LONGEST=16;

    private PrivateCode(){}

    /** A code the text before the caret ends with, just completed by its colon: its kind and where it starts, or null. */
    static Object[] ending(CharSequence text,int caret) {
        if(text==null||caret<=0||caret>text.length()||text.charAt(caret-1)!=':')return null;
        String tail=text.subSequence(Math.max(0,caret-LONGEST),caret).toString().toLowerCase(Locale.ROOT);
        for(String code:CODES)if(tail.endsWith(code))
            return new Object[]{code.contains("space")?Kind.SPACE:Kind.OPEN,caret-code.length()};
        return null;
    }

    /**
     * Whether the word at the caret could still become a code: "privaten", "private note/", "private/" and so on, but not
     * "private" alone, nor "private " with nothing after it, which are words people write.
     */
    static boolean typing(CharSequence text,int caret){return unfinishedFrom(text,caret)>=0;}

    /** Where the unfinished code at the caret starts, or -1. */
    static int unfinishedFrom(CharSequence text,int caret) {
        if(text==null||caret<=0||caret>text.length())return -1;
        String tail=text.subSequence(Math.max(0,caret-LONGEST),caret).toString().toLowerCase(Locale.ROOT);
        for(int k=Math.min(tail.length(),LONGEST);k>"private".length();k--) {
            String part=tail.substring(tail.length()-k);
            if(!part.startsWith("private")||part.equals("private "))continue;
            for(String code:CODES)if(code.startsWith(part)&&!code.equals(part))return caret-k;
        }
        return -1;
    }

    /**
     * The unfinished code at the caret, for a save that cannot wait: taken out only where it has its slashes, which no word
     * people write has ({@code {start,end}}), else null and it is written as typed.
     */
    static int[] unfinished(CharSequence text,int caret) {
        int from=unfinishedFrom(text,caret);
        if(from<0)return null;
        String part=text.subSequence(from,caret).toString().toLowerCase(Locale.ROOT);
        // "private/" alone can be a path being written; "private//" and "privatespace/" are nobody's words (decision 116).
        return part.contains("//")||part.indexOf('/')>=0&&part.contains("space")?new int[]{from,caret}:null;
    }

    // ---- the guard at the door (decision 114) -----------------------------------------------------------------------

    /**
     * A code as the guard sees it: "private", a space or none, "space" or the old "note" or "folder" or neither, then //:
     * touching, and the rest of that line, which is where its password would be (decision 116 keeps the two old words so a
     * code from a build before the rename is still caught). The word and //: must touch: "the word privatespace, then //:",
     * as Read me teaches it, is left alone.
     */
    private static final Pattern GUARD=Pattern.compile("private ?(?:space|note|folder)?//:[^\r\n]*",Pattern.CASE_INSENSITIVE);

    /** The text with every code and the rest of its line taken out; the same text, the same object, where there is none. */
    static String scrub(String text) {
        if(text==null||text.indexOf("//:")<0)return text;
        Matcher m=GUARD.matcher(text);
        return m.find()?m.replaceAll(""):text;
    }
    /** Whether the guard would take anything out of it. */
    static boolean holds(CharSequence text){return text!=null&&text.toString().contains("//:")&&GUARD.matcher(text).find();}
    /** Each run the guard takes out, {start,end}, in order: for an editor, which takes them out of its own words. */
    static List<int[]> runs(CharSequence text) {
        List<int[]> out=new ArrayList<>();
        if(text==null||!text.toString().contains("//:"))return out;
        Matcher m=GUARD.matcher(text);
        while(m.find())out.add(new int[]{m.start(),m.end()});
        return out;
    }
    /** Where a caret stands once those runs are out: moved back by what went before it, at a run's start if it was in one. */
    static int caretAfter(List<int[]> runs,int caret) {
        int moved=caret;
        for(int[] r:runs){if(r[0]>=caret)break;moved-=Math.min(caret,r[1])-r[0];}
        return Math.max(0,moved);
    }

    // ---- a code finished by one change ------------------------------------------------------------------------------

    /** A code a change of text finished: its kind, where it starts, where what to take out ends, what came after it. */
    static final class Caught {
        final Kind kind;final int start,stop;final String rest;final boolean entered;
        Caught(Kind kind,int start,int stop,String rest,boolean entered){this.kind=kind;this.start=start;this.stop=stop;this.rest=rest;this.entered=entered;}
    }

    /**
     * The code that a change of the text between {@code from} and {@code to} finished, or null: a key, a word a phone's
     * keyboard hands over again whole at every letter, a short paste. What came after its colon in the same change is
     * the password, to the end of its line, taken with the code so none of it stays in the note ({@link Caught#rest});
     * {@link Caught#entered} where that line ended, as Enter would end it. Found on the phone, 2026-10-06: a code pasted
     * or handed over with its password left the password in the note.
     */
    static Caught finished(CharSequence text,int from,int to) {
        if(text==null)return null;
        to=Math.min(to,text.length());
        for(int k=to;k>Math.max(0,from);k--) {
            Object[] found=ending(text,k);
            if(found==null)continue;
            int stop=to;boolean entered=false;
            for(int i=k;i<to;i++){char c=text.charAt(i);if(c=='\n'||c=='\r'){stop=i;entered=true;break;}}
            return new Caught((Kind)found[0],(Integer)found[1],stop,text.subSequence(k,stop).toString(),entered);
        }
        return null;
    }

    /**
     * What is typed while the password field comes up, kept for it. A keyboard that writes a word while it is typed hands
     * the whole word over again, code and all: only what follows the code is the password (the owner, 2026-10-06).
     */
    static void more(StringBuilder caught,CharSequence typed) {
        if(typed==null||typed.length()==0)return;
        for(int k=typed.length();k>0;k--)if(ending(typed,k)!=null){caught.setLength(0);caught.append(typed,k,typed.length());return;}
        caught.append(typed);
    }
}
