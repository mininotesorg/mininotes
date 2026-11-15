// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The phone's own log, kept in a file as the PC's is (the owner, 2026-11-04: "Will the user be able to provide their log if
 * ever they face such a situation"): the system's log lasts minutes in a flood, so what this app said is copied out of it
 * now and then into two files of a megabyte at most, the older one dropped as a new one starts. Only this app's own lines,
 * which say what happened in counts and revisions, never a note's words; nothing is sent anywhere but where the person
 * shares it themselves (Feedback, Share the log). Holds no Android types: what is kept, and how it rolls, is unit tested.
 */
final class LogKeep {
    private LogKeep(){}

    /** How big each of the two files grows before the newer becomes the older. */
    static final long MOST=1024L*1024;

    /** The file written to now, and the older one before it. */
    static File now(File folder){return new File(folder,"mininotes.log");}
    static File older(File folder){return new File(folder,"mininotes.1.log");}

    /**
     * Whether a line of the system's log (threadtime: "11-04 18:02:03.123  1234  1250 I Mininotes/Post: ...") is one to keep:
     * this app's own tags, and whatever went wrong badly enough to be an error or a crash in its process.
     */
    static boolean ours(String line) {
        if(line==null||line.length()<33)return false;
        int level=levelAt(line);if(level<0)return false;
        String rest=line.substring(level+2);
        return rest.startsWith("Mininotes")||rest.startsWith("AndroidRuntime")||line.charAt(level)=='E'||line.charAt(level)=='F';
    }

    /** Where the level letter is in a threadtime line, or -1. */
    private static int levelAt(String line) {
        // After the date, the time, the process and the thread, each followed by spaces.
        int at=0,fields=0;
        while(at<line.length()&&fields<4) {
            while(at<line.length()&&line.charAt(at)!=' ')at++;
            while(at<line.length()&&line.charAt(at)==' ')at++;
            fields++;
        }
        return at+1<line.length()&&line.charAt(at+1)==' '&&"VDIWEF".indexOf(line.charAt(at))>=0?at:-1;
    }

    /** When a threadtime line was written, as the system's log prints it ("11-04 18:02:03.123"); empty for anything else. */
    static String stamp(String line){return line!=null&&line.length()>=18&&line.charAt(2)=='-'&&line.charAt(5)==' '?line.substring(0,18):"";}

    /**
     * Of what the system's log holds now, the lines to add: this app's, after the last one kept (the system hands back the
     * second it was asked from again). Stamps compare as text, month first; a year's turn starts the file over, no more.
     */
    static List<String> after(List<String> lines,String last) {
        List<String> out=new ArrayList<>();
        for(String one:lines) {
            if(!ours(one))continue;
            String at=stamp(one);
            if(last!=null&&!last.isEmpty()&&at.compareTo(last)<=0)continue;
            out.add(one);
        }
        return out;
    }

    /** These lines added to the log in this folder; the file rolled first where they would take it past {@link #MOST}. */
    static void add(File folder,List<String> lines) throws IOException {
        if(lines.isEmpty())return;
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException(folder.getPath());
        StringBuilder text=new StringBuilder();for(String one:lines)text.append(one).append('\n');
        byte[] bytes=text.toString().getBytes(StandardCharsets.UTF_8);
        File now=now(folder);
        if(now.length()+bytes.length>MOST&&now.length()>0)Files.move(now.toPath(),older(folder).toPath(),StandardCopyOption.REPLACE_EXISTING);
        Files.write(now.toPath(),bytes,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }

    /** Both files, the older first, as one text: what Share the log hands over. */
    static byte[] whole(File folder) throws IOException {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        for(File one:new File[]{older(folder),now(folder)})if(one.isFile())out.write(Files.readAllBytes(one.toPath()));
        return out.toByteArray();
    }
}
