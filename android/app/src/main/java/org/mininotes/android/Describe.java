// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What each menu entry does, in one line (the owner, 2026-10-18: "we should have a mode in the profile that turns on or off
 * the mouse-over description of everything, and for all elements we should have a brief description. All this will need
 * to be in the Read me"; decision 205). Read from help/describe.txt, the same list the README gives, and said in the
 * language in use. Holds no Android types.
 */
final class Describe {
    private Describe(){}

    private static Map<String,String> all;

    /** Every entry and its line, in the file's order. */
    static synchronized Map<String,String> all() {
        if(all!=null)return all;
        Map<String,String> read=new LinkedHashMap<>();
        try(InputStream in=Describe.class.getResourceAsStream("/org/mininotes/android/help/describe.txt")) {
            if(in!=null)try(BufferedReader lines=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))) {
                for(String line;(line=lines.readLine())!=null;) {
                    String t=line.trim();if(t.isEmpty()||t.startsWith("#"))continue;
                    int eq=t.indexOf(" = ");if(eq<=0)continue;
                    read.put(t.substring(0,eq).trim(),t.substring(eq+3).trim());
                }
            }
        } catch(Exception unreadable){/* none, then */}
        all=Collections.unmodifiableMap(read);
        return all;
    }

    /**
     * The line for a menu entry's words, in the language in use, or null where it has none: its dots, a count after it
     * ("Move to bin  (2)") and a version after Update to v are not part of its name.
     */
    static String of(String entry) {
        if(entry==null)return null;
        String name=entry.replaceAll("\\s*\\(\\d+\\)$","").trim();
        if(name.endsWith("…"))name=name.substring(0,name.length()-1).trim();
        if(name.startsWith("Update to v"))name="Update to v";
        String said=all().get(name);
        return said==null?null:Say.t(said);
    }
}
