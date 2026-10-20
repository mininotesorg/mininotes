// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The size of the whole app on this PC, as Windows' own scaling makes everything bigger or smaller at once (the owner,
 * 2026-10-18: "the font size has no direct impact on the Home screen ... let's fix that and put it in My profile for the
 * setting at the app level"; decision 200): Home, its names, Recent, the menus and the boxes alike. Read once, before
 * anything is drawn, on top of the scale Windows gives the screen; so a change takes a restart, which My profile offers
 * at once. A note's own size stays in its menu. Only for the installed app: a copy run from the build or by the tests is
 * drawn as it is.
 */
final class DesktopScale {
    private DesktopScale(){}

    /**
     * The sizes offered, as a share of Windows' own: nine, as many as a note has nearly (the owner, 2026-10-19: "we need
     * more ladders here in the app level font size"; decision 219). Said as a share, which needs no words.
     */
    static final double[] STEPS={0.8,0.9,1.0,1.1,1.2,1.3,1.45,1.6,1.8};
    static final int USUAL=2;
    /** What a step is called: its share of the usual size. */
    static String named(int step){return Math.round(STEPS[Math.max(0,Math.min(STEPS.length-1,step))]*100)+" %";}
    /** The five steps of 0.4.004 to 0.4.009, kept as their place in this list: read as the size they were. */
    private static final double[] BEFORE={0.9,1.0,1.1,1.25,1.4};
    /** The step nearest a size. */
    static int nearest(double size){int best=USUAL;for(int i=0;i<STEPS.length;i++)if(Math.abs(STEPS[i]-size)<Math.abs(STEPS[best]-size))best=i;return best;}

    private static Path file(){String local=System.getenv("LOCALAPPDATA");return local==null?null:Path.of(local,"Mininotes","app-size");}

    /** The step chosen, {@link #USUAL} where none was. */
    static int chosen() {
        try{Path f=file();if(f==null||!Files.exists(f))return USUAL;return read(Files.readString(f,StandardCharsets.UTF_8));}
        catch(Exception unreadable){return USUAL;}
    }
    /** What the file says: a size ("1.25"), kept since 0.4.010, or the place in the five steps before it. */
    static int read(String said) {
        String t=said==null?"":said.trim();
        try{if(t.contains("."))return nearest(Double.parseDouble(t));int n=Integer.parseInt(t);return n>=0&&n<BEFORE.length?nearest(BEFORE[n]):USUAL;}
        catch(NumberFormatException none){return USUAL;}
    }
    /** Kept for the next start. */
    static void choose(int step) throws java.io.IOException {
        Path f=file();if(f==null)return;Files.createDirectories(f.getParent());
        Files.writeString(f,String.format(java.util.Locale.ROOT,"%.2f",STEPS[Math.max(0,Math.min(STEPS.length-1,step))]),StandardCharsets.UTF_8);
    }
    /** The step the app was started with, so My profile can say a restart is waiting. */
    static int started=USUAL;

    private interface Dpi extends StdCallLibrary{Dpi USER=Native.load("user32",Dpi.class);int GetDpiForSystem();}

    /** Before anything is drawn: the chosen size set on top of Windows' scale, for the installed app only. */
    static void apply() {
        started=chosen();
        if(started==USUAL||System.getProperty("jpackage.app-path")==null||System.getProperty("sun.java2d.uiScale")!=null)return;
        double windows=1.0;
        try{windows=Math.max(1.0,Dpi.USER.GetDpiForSystem()/96.0);}catch(Throwable older){/* Windows' scale unknown: 100 % */}
        System.setProperty("sun.java2d.uiScale.enabled","true");
        System.setProperty("sun.java2d.uiScale",String.format(java.util.Locale.ROOT,"%.2f",windows*STEPS[started]));
    }

    /** Said before a restart for the size: My profile opens again at Text size when it is back (decision 219). */
    private static Path reopen(){String local=System.getenv("LOCALAPPDATA");return local==null?null:Path.of(local,"Mininotes","reopen-text-size");}
    /** Whether this start follows a restart for the size, the mark taken away as it is read. */
    static boolean reopening(){try{Path f=reopen();return f!=null&&Files.deleteIfExists(f);}catch(Exception none){return false;}}

    /** Mininotes started again, the installed one, a moment after this one has closed. */
    static boolean restart(Desktop d) {
        String exe=System.getProperty("jpackage.app-path");
        if(exe==null)return false;
        try{Path f=reopen();if(f!=null)Files.writeString(f,"1",StandardCharsets.UTF_8);}catch(Exception none){/* back at Home, then */}
        try{new ProcessBuilder("cmd","/c","ping -n 3 127.0.0.1 >nul & start \"\" \""+exe+"\"").start();}catch(Exception none){return false;}
        d.save(()->d.shutdown(true));
        return true;
    }
}
