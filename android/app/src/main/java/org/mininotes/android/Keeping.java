// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

/**
 * Backups pushed to this device by the owner's others, kept (decision 330; the owner, 2026-11-05: "having both choice another
 * of my devices, it could be a server right?"; "could we have a cli script of mininote for server just to receive the backup,
 * mainly Linux servers?"). A backup server, a Mininotes with no screen on the owner's own server, and the PC both keep them:
 * each device's in a folder of its own under one folder, as the sender sealed them, never opened here, which this device
 * cannot do anyway. Only a backup's own name is taken; a try that broke off goes on where it stopped; once one came whole,
 * that device's oldest beyond the number it asked for go. Holds no Android types.
 */
final class Keeping {
    private Keeping(){}

    /** Room left beside a backup: one that would leave less than this free is not taken. */
    static final long SPARE=64L*1024*1024;
    /** What a backup is written as until it came whole. */
    static final String PART=".part";

    /**
     * This pushed backup taken into {@code root}, in its sender's folder: null where it is taken, or why not (for the log, never
     * shown). {@code kept} told of each once whole, with its path.
     */
    static String take(Path root,Big.Waiting one,LongConsumer progress,java.util.function.Consumer<Path> kept) {
        Big.Offer offer=one.offer;
        if(!Backups.isBackupName(offer.name))return "not a backup's name";
        if(!offer.kind.startsWith(Backups.KIND))return "not a backup";
        try {
            // The device's name as its backups carry it, so a device renamed in a list does not split its backups in two, and the
            // oldest are counted in the folder they are in.
            Path folder=root.resolve(Backups.deviceIn(offer.name));
            Files.createDirectories(folder);
            if(Files.getFileStore(folder).getUsableSpace()<offer.bytes+SPARE)return "no room";
            Path file=folder.resolve(offer.name),part=folder.resolve(offer.name+PART);
            if(Files.exists(file))return "kept already";
            int keep=Backups.keepIn(offer.kind);
            Big.take(offer,new Big.Sink(){
                @Override public long have(){try{return Files.exists(part)?Files.size(part):0;}catch(IOException none){return 0;}}
                @Override public OutputStream from(long at) throws IOException {
                    RandomAccessFile out=new RandomAccessFile(part.toFile(),"rw");
                    out.setLength(at);out.seek(at);
                    return new OutputStream(){
                        @Override public void write(int b) throws IOException{out.write(b);}
                        @Override public void write(byte[] b,int off,int len) throws IOException{out.write(b,off,len);}
                        @Override public void flush() throws IOException{out.getFD().sync();}
                        @Override public void close() throws IOException{try{out.getFD().sync();}finally{out.close();}}
                    };
                }
                @Override public void done() throws IOException {
                    Files.move(part,file,StandardCopyOption.ATOMIC_MOVE);
                    for(String old:Backups.beyond(names(folder),Backups.deviceIn(offer.name),keep))Files.deleteIfExists(folder.resolve(old));
                    if(kept!=null)kept.accept(file);
                }
            },progress);
            return null;
        } catch(IOException|RuntimeException failed){return "could not be written: "+failed.getClass().getSimpleName();}
    }

    /** The names in a folder. */
    static List<String> names(Path folder) throws IOException {
        List<String> out=new ArrayList<>();
        try(Stream<Path> all=Files.list(folder)){all.forEach(p->out.add(p.getFileName().toString()));}
        return out;
    }

    /** Every backup kept, by device folder: for the server's list and the PC's page. */
    static List<Path> all(Path root) throws IOException {
        List<Path> out=new ArrayList<>();
        if(!Files.isDirectory(root))return out;
        try(Stream<Path> devices=Files.list(root)) {
            for(Path device:(Iterable<Path>)devices::iterator) {
                if(!Files.isDirectory(device))continue;
                for(String name:names(device))if(Backups.isBackupName(name))out.add(device.resolve(name));
            }
        }
        out.sort(null);
        return out;
    }
}
