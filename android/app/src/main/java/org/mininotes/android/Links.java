// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The web and mail addresses in a note, found the same way on the phone and the PC, so an address that opens on
 * one opens on the other. The phone used the system's own finder, which the PC does not have; the PC found none.
 *
 * <p>What counts: anything starting {@code http://}, {@code https://} or {@code www.}; {@code mailto:}; an e-mail
 * address; and a bare name ending in any of the web's endings ({@code example.com/page}, {@code example.taxi}), read
 * from IANA's list in web-endings.txt (the owner, 2026-11-15: "detect all hyperlink form every extension like" a .taxi address;
 * it knew some thirty-five). A file name such as {@code notes.txt} is left as words, and so is one whose ending is also a
 * web ending but mostly a file's ({@link #FILE_TYPES}: {@code Notes.md}, {@code setup.py}) unless written with www. or a
 * scheme. Stops at a space, and a full stop or bracket closing the sentence is not taken as part of the address.
 *
 * <p>Holds no Android or Swing types: unit tested.
 */
final class Links {
    private Links(){}

    /** One address: where it is in the text, and what opening it opens. */
    record Link(int start,int end,String target) {
        boolean covers(int at){return at>=start&&at<end;}
    }

    /** Every web ending, lower case; the common ones written in, should the list not be read. */
    static final Set<String> ENDINGS=endings();
    /**
     * Web endings that are mostly a kind of file on a page: {@code script.sh}, {@code clip.mov}, {@code Notes.md}. A bare
     * name ending in one stays words; with www. or https:// in front it is an address as any other.
     */
    static final Set<String> FILE_TYPES=Set.of("zip","mov","py","md","sh","rs","java","ps","so","pm","mp");

    private static Set<String> endings() {
        Set<String> out=new HashSet<>(List.of("com","org","net","io","dev","app","co","info","me","edu","gov","eu","uk","de",
            "fr","nl","es","it","tr","ch","at","be","se","no","dk","fi","pl","ie","pt","ca","us","au","nz","jp","in","br"));
        // Read a line at a time: InputStream.readAllBytes is Android 13's, and the app runs from Android 9.
        try(InputStream in=Links.class.getResourceAsStream("/org/mininotes/android/web-endings.txt")) {
            if(in!=null)try(java.io.BufferedReader lines=new java.io.BufferedReader(new java.io.InputStreamReader(in,StandardCharsets.UTF_8))) {
                for(String line=lines.readLine();line!=null;line=lines.readLine()) {
                    line=line.trim().toLowerCase(Locale.ROOT);
                    if(!line.isEmpty()&&!line.startsWith("#"))out.add(line);
                }
            }
        } catch(IOException|RuntimeException unread){/* the common ones, as before */}
        return Collections.unmodifiableSet(out);
    }

    private static final Pattern FIND=Pattern.compile(
        "(?i)(?<![\\w@.-])(?:"
        // An e-mail address, first, so the name before the @ is not left behind as words.
        +"(?<mail>[a-z0-9._%+-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.[a-z]{2,})"
        +"|(?<web>(?:https?://|www\\.|mailto:)[^\\s<>\"]+)"
        // A name and its ending, the ending looked up in ENDINGS once found: one pattern of 1,400 endings would be tried
        // ending by ending at every place in a note.
        +"|(?<bare>(?:[a-z0-9-]+\\.)+(?<end>[a-z]{2,63})(?:/[^\\s<>\"]*)?)(?![\\w@-])"
        +")");

    /** Whether a bare name ending so is an address: one of the web's endings, and not mostly a kind of file. */
    static boolean webEnding(String ending) {
        String e=ending.toLowerCase(Locale.ROOT);
        return ENDINGS.contains(e)&&!FILE_TYPES.contains(e);
    }

    /** Every address in the text, in order. */
    static List<Link> find(CharSequence text) {
        List<Link> out=new ArrayList<>();
        if(text==null||text.length()==0)return out;
        Matcher m=FIND.matcher(text);
        while(m.find()) {
            if(m.group("bare")!=null&&!webEnding(m.group("end")))continue;
            int start=m.start(),end=trimmed(text,start,m.end());
            if(end<=start)continue;
            String found=text.subSequence(start,end).toString();
            String target=m.group("mail")!=null?"mailto:"+found
                :m.group("bare")!=null||found.regionMatches(true,0,"www.",0,4)?"https://"+found:found;
            // Nothing after the scheme is not an address.
            if(target.matches("(?i)(https?://|mailto:)(www\\.?)?"))continue;
            out.add(new Link(start,end,target));
        }
        return out;
    }

    /** The address at a place in the text, or null. */
    static Link at(List<Link> links,int at) {
        if(links!=null)for(Link one:links)if(one.covers(at))return one;
        return null;
    }

    /**
     * Where an address really ends: without the full stop, comma or quote that ends the sentence around it, and
     * without a closing bracket it did not open - "(see example.com)" is the address without the bracket.
     */
    private static int trimmed(CharSequence text,int start,int end) {
        while(end>start) {
            char last=text.charAt(end-1);
            if(".,;:!?'\"".indexOf(last)>=0){end--;continue;}
            if(last==')'||last==']') {
                char open=last==')'?'(':'[';int opened=0,closed=0;
                for(int i=start;i<end;i++){char c=text.charAt(i);if(c==open)opened++;else if(c==last)closed++;}
                if(closed>opened){end--;continue;}
            }
            break;
        }
        return end;
    }
}
