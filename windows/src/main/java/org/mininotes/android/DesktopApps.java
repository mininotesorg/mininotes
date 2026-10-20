// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mininotes.desktop.platform.content.Context;

/**
 * The program a kind of file opens in on this PC (the owner, 2026-10-15: "the app used to open it should be presented there
 * in the menu and in the icon type, and be linked with the open with, so that it can be changed, and it should go get what
 * is defined at the windows level for the default app, only if this needs to be overridden it should be saved locally in
 * Mininotes"). Windows' own choice is read each time, never copied; a program picked in Open with… is kept here, for that
 * kind, only when it is not Windows' choice, and picking Windows' choice again forgets it.
 */
final class DesktopApps {
    private DesktopApps(){}

    /** A program: its name as Windows says it, and where it is; null for Windows' own way of opening the kind. */
    record App(String name,Path exe) {}

    private interface Shlwapi extends StdCallLibrary {
        Shlwapi LIB=windows()?Native.load("shlwapi",Shlwapi.class,W32APIOptions.DEFAULT_OPTIONS):null;
        int AssocQueryString(int flags,int str,WString assoc,WString extra,char[] out,IntByReference length);
    }
    private static final int BY_EXE_NAME=0x2,EXECUTABLE=2,FRIENDLY_APP_NAME=4;
    private static boolean windows(){return System.getProperty("os.name","").startsWith("Windows");}

    /** The programs picked here, by kind (".ods"), as kept in this PC's settings. Read once the window is up. */
    private static final Map<String,String> picked=new ConcurrentHashMap<>();
    private static Context context;

    /** Kept as one setting, a line a kind: ".ods", a tab, the program. */
    static void load(Context c) {
        context=c;picked.clear();
        for(String line:c.getSharedPreferences("settings",0).getString("openWith","").split("\n")) {
            int tab=line.indexOf('\t');
            if(tab>0&&tab<line.length()-1)picked.put(line.substring(0,tab),line.substring(tab+1));
        }
        icons.clear();
    }
    private static void keep() {
        if(context==null)return;
        StringBuilder all=new StringBuilder();
        for(Map.Entry<String,String> one:picked.entrySet())all.append(one.getKey()).append('\t').append(one.getValue()).append('\n');
        context.getSharedPreferences("settings",0).edit().putString("openWith",all.toString()).apply();
    }

    /** A file's kind as Windows names it: ".ods"; "" for a name with none. */
    static String kind(String name){int dot=name==null?-1:name.lastIndexOf('.');return dot>0?name.substring(dot).toLowerCase(Locale.ROOT):"";}

    private static String ask(int flags,int what,String of) {
        if(Shlwapi.LIB==null||of==null||of.isEmpty())return null;
        try {
            char[] out=new char[1024];IntByReference length=new IntByReference(out.length);
            if(Shlwapi.LIB.AssocQueryString(flags,what,new WString(of),null,out,length)!=0)return null;
            String said=Native.toString(out).trim();
            return said.isEmpty()?null:said;
        } catch(Throwable unavailable){return null;}
    }

    /** What Windows opens this kind with, or null where it opens it with nothing. */
    static App windowsChoice(String kind) {
        String exe=ask(0,EXECUTABLE,kind);
        if(exe==null)return null;
        String name=ask(0,FRIENDLY_APP_NAME,kind);
        return new App(name==null?stem(exe):name,Path.of(exe));
    }

    /** A program known by where it is: its name as Windows says it, else its file's name. */
    static App program(Path exe) {
        String name=ask(BY_EXE_NAME,FRIENDLY_APP_NAME,exe.toString());
        return new App(name==null?stem(exe.toString()):name,exe);
    }
    private static String stem(String exe){String n=Path.of(exe).getFileName().toString();return n.toLowerCase(Locale.ROOT).endsWith(".exe")?n.substring(0,n.length()-4):n;}

    /** The program picked here for this kind, where one was and it is still there; else null. */
    static App pickedFor(String kind) {
        String exe=picked.get(kind);
        if(exe==null)return null;
        Path p=Path.of(exe);
        return Files.isRegularFile(p)?program(p):null;
    }

    /** What a file of this kind opens in from Mininotes: the program picked here, else Windows' choice; null for none. */
    static App opensIn(String kind){App mine=pickedFor(kind);return mine!=null?mine:windowsChoice(kind);}

    /** The menu's one Open: "Open in LibreOffice Calc", or Open where nothing is known. */
    static String openLine(String fileName) {
        App app=opensIn(kind(fileName));
        return app==null?Say.t("Open"):Say.t("Open in {0}",app.name());
    }

    /**
     * The other programs Windows offers for this kind, as its own Open with lists them, Windows' choice first and the one
     * picked here among them; each once.
     */
    static List<App> offered(String kind) {
        Map<String,App> all=new LinkedHashMap<>();
        App own=windowsChoice(kind);if(own!=null)all.put(own.exe().toString().toLowerCase(Locale.ROOT),own);
        App mine=pickedFor(kind);if(mine!=null)all.putIfAbsent(mine.exe().toString().toLowerCase(Locale.ROOT),mine);
        if(windows()&&!kind.isEmpty()) {
            List<String> ids=new ArrayList<>();
            try{ids.addAll(Advapi32Util.registryGetValues(WinReg.HKEY_CLASSES_ROOT,kind+"\\OpenWithProgids").keySet());}catch(RuntimeException none){/* none listed */}
            for(String id:ids){String exe=ask(0,EXECUTABLE,id);if(exe==null)continue;
                String name=ask(0,FRIENDLY_APP_NAME,id);all.putIfAbsent(exe.toLowerCase(Locale.ROOT),new App(name==null?stem(exe):name,Path.of(exe)));}
            try {
                Map<String,Object> list=Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER,"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\"+kind+"\\OpenWithList");
                for(Map.Entry<String,Object> one:list.entrySet()) {
                    if(one.getKey().equalsIgnoreCase("MRUList")||!(one.getValue() instanceof String exeName))continue;
                    String exe=ask(BY_EXE_NAME,EXECUTABLE,exeName);if(exe==null)continue;
                    String name=ask(BY_EXE_NAME,FRIENDLY_APP_NAME,exeName);
                    all.putIfAbsent(exe.toLowerCase(Locale.ROOT),new App(name==null?stem(exe):name,Path.of(exe)));
                }
            } catch(RuntimeException none){/* none listed */}
        }
        List<App> out=new ArrayList<>();
        for(App one:all.values())if(Files.isRegularFile(one.exe()))out.add(one);
        return out;
    }

    /** Picked in Open with…: kept for this kind, or forgotten where it is Windows' own choice. */
    static void pick(String kind,App app) {
        App own=windowsChoice(kind);
        boolean windowsOwn=app==null||own!=null&&own.exe().toString().equalsIgnoreCase(app.exe().toString());
        if(windowsOwn)picked.remove(kind);else picked.put(kind,app.exe().toString());
        keep();icons.remove(kind);
    }
    /** Whether a program other than Windows' choice is kept for this kind. */
    static boolean overridden(String kind){return pickedFor(kind)!=null;}

    /** A file opened in the program it opens in from Mininotes; false where none would. */
    static boolean open(Path file) {
        App mine=pickedFor(kind(file.getFileName().toString()));
        try {
            if(mine!=null){new ProcessBuilder(mine.exe().toString(),file.toString()).start();return true;}
            java.awt.Desktop.getDesktop().open(file.toFile());return true;
        } catch(IOException|RuntimeException none){return false;}
    }

    /** The icon Windows shows this kind with, or the program picked here's: for a file's tile. Null where there is none. */
    static BufferedImage icon(String kind) {
        if(!windows()||kind.isEmpty())return null;
        return icons.computeIfAbsent(kind,k->{
            try {
                Path of;
                App mine=pickedFor(k);
                if(mine!=null)of=mine.exe();
                else {
                    Path room=Path.of(System.getProperty("java.io.tmpdir"),"mininotes-kinds");Files.createDirectories(room);
                    of=room.resolve("kind"+k);if(!Files.exists(of))Files.createFile(of);
                }
                javax.swing.Icon got=javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(of.toFile(),64,64);
                if(got==null||got.getIconWidth()<=0)return NONE;
                BufferedImage img=new BufferedImage(got.getIconWidth(),got.getIconHeight(),BufferedImage.TYPE_INT_ARGB);
                var g=img.createGraphics();got.paintIcon(null,g,0,0);g.dispose();
                return img;
            } catch(IOException|RuntimeException none){return NONE;}
        })==NONE?null:icons.get(kind);
    }
    private static final BufferedImage NONE=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
    private static final Map<String,BufferedImage> icons=new ConcurrentHashMap<>();
}
