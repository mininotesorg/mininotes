// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A photo from the PC's camera, into the open note (the owner, 2026-10-18: "let's have the photo available even on desktop,
 * triggering the camera function"; decision 194). Nothing added to Mininotes for it: Windows' own Camera opens, and what it
 * saves in the Camera Roll while the note waits comes into the note as it would from Attach, a photo or a video each, until
 * Camera has been quiet for five minutes. The bar says so all along.
 */
final class DesktopCamera {
    private DesktopCamera(){}

    /** How long it waits for a photo after the last one, or after Camera opened. */
    static final long QUIET=5*60_000L;
    /** What Camera saves that is kept: its photos and its videos. */
    private static final List<String> KEPT=List.of(".jpg",".jpeg",".png",".heic",".mp4",".mov");

    /** The one watch going, so a second press does not watch twice. */
    private static javax.swing.Timer watching;

    /** Where Windows' Camera saves: the Camera Roll in Pictures, or in OneDrive's Pictures where Pictures is kept there. */
    static List<Path> rolls() {
        Path home=Path.of(System.getProperty("user.home"));
        List<Path> out=new ArrayList<>();
        for(String one:new String[]{"Pictures/Camera Roll","OneDrive/Pictures/Camera Roll","OneDrive/Images/Camera Roll","OneDrive/Bilder/Camera Roll"})
            out.add(home.resolve(one));
        return out;
    }

    /** Camera opened, and what it saves from now on watched for, for this note. */
    static void take(Desktop d,String note) {
        if(note==null)return;
        long since=System.currentTimeMillis()-2000;
        try{new ProcessBuilder("cmd","/c","start","","microsoft.windows.camera:").start();}
        catch(Exception noCamera){d.status.setText(Say.t("Could not open Camera on this PC."));return;}
        d.status.setToolTipText(null);
        d.status.setText(Say.t("Take the photo in Camera. It comes into this note as soon as it is saved."));
        watch(d,note,since,rolls());
    }

    /**
     * The same for a folder shown as an album (decision 217): what Camera saves is kept in the folder, as Add photos keeps it.
     */
    static void into(Desktop d,String folder,String called) {
        if(folder==null)return;
        long since=System.currentTimeMillis()-2000;
        try{new ProcessBuilder("cmd","/c","start","","microsoft.windows.camera:").start();}
        catch(Exception noCamera){d.status.setText(Say.t("Could not open Camera on this PC."));return;}
        d.status.setToolTipText(null);
        d.status.setText(Say.t("Take the photo in Camera. It comes into this folder as soon as it is saved."));
        watch(d,since,rolls(),found->d.keep(folder,called,found));
    }

    /** Every second, what is new in the rolls and has stopped growing is kept with the note; quiet for long enough, it stops. */
    static void watch(Desktop d,String note,long since,List<Path> rolls){watch(d,since,rolls,found->d.attach(note,null,found));}
    /** The same, what is found handed to {@code kept}: a note's attach, or a folder's keep. */
    static void watch(Desktop d,long since,List<Path> rolls,java.util.function.Consumer<List<Path>> kept) {
        if(watching!=null)watching.stop();
        Map<Path,Long> growing=new HashMap<>();List<Path> taken=new ArrayList<>();long[] last={System.currentTimeMillis()};
        watching=new javax.swing.Timer(1000,null);
        javax.swing.Timer me=watching;
        me.addActionListener(e->{
            if(d.closing||System.currentTimeMillis()-last[0]>QUIET){me.stop();return;}
            d.disk.submit(()->ready(rolls,since,growing,taken),found->{
                if(found.isEmpty())return;
                last[0]=System.currentTimeMillis();
                kept.accept(found);
            },x->{});
        });
        me.start();
    }

    /**
     * The files saved in the rolls since then, not taken yet, whose size was the same a second ago: Camera has finished
     * writing them. Blocking: the worker calls it.
     */
    static List<Path> ready(List<Path> rolls,long since,Map<Path,Long> growing,List<Path> taken) {
        List<Path> out=new ArrayList<>();
        for(Path roll:rolls) {
            if(!Files.isDirectory(roll))continue;
            try(Stream<Path> all=Files.walk(roll,3)) {
                for(Path one:(Iterable<Path>)all::iterator) {
                    String name=one.getFileName().toString().toLowerCase(Locale.ROOT);
                    if(!Files.isRegularFile(one)||KEPT.stream().noneMatch(name::endsWith)||taken.contains(one))continue;
                    if(Files.getLastModifiedTime(one).toMillis()<since)continue;
                    long size=Files.size(one);
                    Long was=growing.put(one,size);
                    if(size>0&&was!=null&&was==size){taken.add(one);out.add(one);}
                }
            } catch(Exception unreadable){/* the next second */}
        }
        return out;
    }

    /** Whether it is watching, for the tests. */
    static boolean watching(){return watching!=null&&watching.isRunning();}
    static void stop(){if(watching!=null)watching.stop();}
}
