// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Open and Open with… on the PC (the owner, 2026-10-15: "we should have only one open, once open we can decide to modify
 * or not. The app used to open it should be presented there in the menu ... and be linked with the open with, so that it
 * can be changed"; docs/FILES.md). Open hands the file to the program it opens in (see DesktopApps): the file itself where
 * a folder kept on disk or Share a file through Mininotes keeps it, which is kept in step already; anywhere else a working
 * copy, watched, whose saves are taken in as the file's new version, exactly, and go to whoever has it. Nothing saved,
 * nothing changes. A file this PC may only read opens as a read-only copy, and says so. Open with… picks the program, for
 * that kind, kept here only where it is not Windows' own choice.
 */
final class DesktopOpening {
    private DesktopOpening(){}

    /** How long a saved working copy is left alone before it is taken in: a program still saving is not half taken. */
    private static final long SETTLED=1500;

    /** The working copies being edited, by file id: where each is and what it said when it was last taken in or written. */
    private static final Map<String,Path> copies=new HashMap<>();
    private static final Map<String,String> sums=new HashMap<>();
    private static javax.swing.Timer watching;

    /** Open: to work in where this PC may change it, else to look at, read only. */
    static void open(Desktop d,NoteStore.Held file) {
        d.disk.submit(()->d.store.mayChangeFile(file.id),may->{if(may)edit(d,file);else look(d,file);},d::failed);
    }

    /**
     * Open with…: the programs Windows offers for this kind, Windows' own choice first, and another program picked from
     * the disk; the one picked kept for every file of this kind opened from Mininotes, then this one opened in it.
     */
    static void openWith(Desktop d,NoteStore.Held file) {
        String kind=DesktopApps.kind(file.name);
        d.disk.submit(()->new Object[]{DesktopApps.offered(kind),DesktopApps.windowsChoice(kind),DesktopApps.opensIn(kind)},got->{
            @SuppressWarnings("unchecked") List<DesktopApps.App> offered=new ArrayList<>((List<DesktopApps.App>)got[0]);
            DesktopApps.App own=(DesktopApps.App)got[1],now=(DesktopApps.App)got[2];
            DesktopApps.App another=new DesktopApps.App(Say.t("Another program…"),null);offered.add(another);
            String message=own==null?Say.t("Windows opens no program for {0} files. The one picked is kept in Mininotes, on this PC.",kind)
                :Say.t("Windows opens {0} files with {1}. Another one picked is kept in Mininotes only, on this PC; Windows' own choice stays as it is.",kind,own.name());
            DesktopApps.App chosen=DesktopUi.pick(d.frame,Say.t("Open {0} with",file.name),message,offered,
                a->a==another?a.name():a.name()+(own!=null&&a.exe().equals(own.exe())?"  ·  "+Say.t("Windows' choice"):"")+(now!=null&&a.exe().equals(now.exe())?"  ✓":""),Say.t("Open"));
            if(chosen==null)return;
            if(chosen==another) {
                java.nio.file.Path program=DesktopPick.openOne(d.frame,Say.t("Open {0} with",file.name));if(program==null)return;
                chosen=DesktopApps.program(program);
            }
            DesktopApps.App use=chosen;
            d.disk.submit(()->{DesktopApps.pick(kind,use);return null;},done->{d.refresh();open(d,file);},d::failed);
        },d::failed);
    }

    /** A read-only copy, opened in the program it opens in: for a file this PC may only read. */
    static void look(Desktop d,NoteStore.Held file) {
        d.status.setToolTipText(null);d.status.setText(Say.t("Opening {0}…",file.name));
        d.disk.submit(()->{
            if(file.fresh)d.store.opened(file.id);
            Path room=Files.createTempDirectory("mininotes-look-");room.toFile().deleteOnExit();
            Path copy=room.resolve(DesktopFiles.onDisk(file.name));
            try(OutputStream out=Files.newOutputStream(copy)){DesktopFiles.copyOut(d.context,d.store.fileFor(file.id).toPath(),out);}
            copy.toFile().setReadOnly();copy.toFile().deleteOnExit();
            return copy;
        },copy->{
            if(DesktopApps.open(copy))d.status.setText(Say.t("{0} is open to read only: it was shared with you to read",file.name));
            else d.status.setText(Say.t("Nothing on this PC opens {0}. Save a copy to keep it somewhere",file.name));
            if(file.fresh)d.refresh();
        },d::failed);
    }

    /** To work in: the file itself where it is kept in step on disk, else a working copy whose saves come back. */
    private static void edit(Desktop d,NoteStore.Held file) {
        d.status.setToolTipText(null);d.status.setText(Say.t("Opening {0}…",file.name));
        d.disk.submit(()->{
            if(!d.store.mayChangeFile(file.id))throw new IllegalStateException(Say.t("Only to look: {0} was shared with you to read.",file.name));
            if(file.fresh)d.store.opened(file.id);
            // In a folder kept on disk: that file, which is kept in step both ways already.
            Path there=d.onDisk==null?null:d.onDisk.pathOf(file.id);
            if(there!=null&&"".equals(d.onDisk.state(file.id))&&Files.isRegularFile(there))return there;
            Path room=Path.of(System.getenv().getOrDefault("LOCALAPPDATA",System.getProperty("java.io.tmpdir")),"Mininotes-editing",file.id);
            Files.createDirectories(room);
            Path copy=room.resolve(DesktopFiles.onDisk(file.name));
            try(OutputStream out=Files.newOutputStream(copy)){DesktopFiles.copyOut(d.context,d.store.fileFor(file.id).toPath(),out);}
            synchronized(copies){copies.put(file.id,copy);sums.put(file.id,DesktopOnDisk.hashOf(copy));}
            return copy;
        },path->{
            watch(d);
            if(DesktopApps.open(path))d.status.setText(Say.t("{0} is open. What you save there is kept in Mininotes",file.name));
            else d.status.setText(Say.t("Nothing on this PC opens {0}. Save a copy to keep it somewhere",file.name));
            if(file.fresh)d.refresh();
        },d::failed);
    }

    /** Every few seconds while a working copy is out: what was saved into it taken in, sent, and said. */
    private static void watch(Desktop d) {
        if(watching!=null)return;
        watching=new javax.swing.Timer(3000,e->d.disk.submit(()->takeIn(d.store),taken->{
            if(taken.isEmpty())return;
            for(String id:taken.keySet())d.fileToMine(id);
            d.status.setToolTipText(null);
            d.status.setText(taken.size()==1?Say.t("Taken in from {0}",taken.values().iterator().next()):Say.t("{0} files taken in",String.valueOf(taken.size())));
            d.refresh();
        },x->{}));
        watching.start();
    }

    /**
     * Each working copy saved since it was last taken in: taken in as its file's new version, exactly. A copy whose file is
     * gone is let go; the copy itself is left where it is. Blocking.
     *
     * @return the ids taken in, with their names
     */
    static Map<String,String> takeIn(NoteStore store) {
        Map<String,String> taken=new LinkedHashMap<>();
        Map<String,Path> now;synchronized(copies){now=new HashMap<>(copies);}
        for(Map.Entry<String,Path> one:now.entrySet()) {
            String id=one.getKey();Path copy=one.getValue();
            NoteStore.Held held=store.file(id);
            if(held==null||!Files.isRegularFile(copy)){synchronized(copies){copies.remove(id);sums.remove(id);}continue;}
            try {
                if(System.currentTimeMillis()-Files.getLastModifiedTime(copy).toMillis()<SETTLED)continue;
                String sum=DesktopOnDisk.hashOf(copy);
                String was;synchronized(copies){was=sums.get(id);}
                if(sum.equals(was))continue;
                try(InputStream in=Files.newInputStream(copy)){store.replaceFile(id,held.kind,in,true);}
                synchronized(copies){sums.put(id,sum);}
                taken.put(id,held.name);
            } catch(IOException|RuntimeException busy){/* still held by its program: the next look */}
        }
        return taken;
    }

}
