// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What one password opens, while it is open: private notes and folders, with their colours, their files and pictures,
 * their versions and a bin of their own (the owner, 2026-10-06: "The note and folder should then have all the options
 * regular assets have, but also the option to change the password"; decision 111). Lives in memory only, never in the
 * notebook: kept in its slots by {@link Slots}, as three kinds of thing by name, {@link #THINGS} (every note and folder,
 * and every file's name), a note's words and versions, and a file's bytes, so changing a note's words writes that note and
 * the list, not everything.
 *
 * <p>Nothing here is ever shared or sent: there is no way out of it but the user's own Save a copy (the owner's choices,
 * "This device only" and "No sharing"). Holds no Android types; logs nothing.
 */
final class PrivateSpace {
    static final String THINGS="things", NOTE="note:", FILE="file:";
    /** Versions kept of each note, as many as the notebook keeps. */
    static final int VERSIONS=20;

    static final class Thing {
        String id=UUID.randomUUID().toString(),parent="",title="",body="",binnedFrom="";
        boolean folder,binned;int colour=Tint.NONE,tone=Tint.USUAL;long made,changed,binnedAt;
        /**
         * Whether this thing sends a help request the moment it opens, to whom, and with what words (decision 113). For a
         * private thing it lives here, inside the vault, with no trace outside it (decision 111): never in the notebook, so
         * a forensic look at the phone cannot tell it is set. Off by default.
         */
        boolean helpOnOpen; List<String> helpTo=new ArrayList<>(); String helpMessage="";
        /** What it is called on its tile: a folder's name; a note's title, else its first line. */
        String name() {
            if(folder||!title.trim().isEmpty())return title.trim().isEmpty()?"Untitled":title.trim();
            String first=Given.firstLine(body);
            return first.isEmpty()?"Untitled":first;
        }
    }
    static final class Kept {
        String id=UUID.randomUUID().toString(),note="",name="",type="";long bytes,added;boolean picture;
    }
    static final class Version {
        final long at;final String title,body;
        Version(long at,String title,String body){this.at=at;this.title=title;this.body=body;}
    }

    final Slots.Space space;
    final LinkedHashMap<String,Thing> things=new LinkedHashMap<>();
    final LinkedHashMap<String,Kept> files=new LinkedHashMap<>();
    private final Map<String,List<Version>> versions=new LinkedHashMap<>();
    private final Map<String,byte[]> bytes=new LinkedHashMap<>();
    /** What changed since the last writing, by name, and what is gone. */
    private final Set<String> changed=new HashSet<>(),gone=new HashSet<>();
    /** The state as last written, to go back to whole when a change does not fit (Private storage is full). */
    private Map<String,byte[]> saved=new LinkedHashMap<>();

    private PrivateSpace(Slots.Space space){this.space=space;}

    /**
     * The private screen's own look, the same idea on both apps (the owner, 2026-10-06: "we should just have a design/icon
     * signaling we are in private mode"; decision 114): its whole ground a quiet grey violet, dusk rather than paper, so it
     * is never taken for Home or for a note washed in a colour; and a lock, deeper of the same, beside Private. A light paper
     * and a dark one each have theirs.
     */
    static final int GROUND=0xFFECE8F6,GROUND_DARK=0xFF201C2E,LOCK=0xFF5B4B91,LOCK_DARK=0xFFB9ACEB;

    /**
     * The line at the foot of the private screen, the same words on both apps (the owner, 2026-10-06: "Why it says 192 KB to
     * go and then MB?"; decision 114): never a byte count. Full; or how long until it is all written, from the batches still
     * needed at one a minute; or Saved.
     */
    static String saying(boolean full,int batchesLeft) {
        if(full)return "Private storage is full";
        if(batchesLeft<=0)return "Saved";
        return "Saving privately… "+(batchesLeft==1?"less than a minute":"about "+batchesLeft+" minutes")+" left. Keep Mininotes open.";
    }
    /** What this space's line says now. */
    String saying(boolean full){return saying(full,space.batchesLeft());}

    /** What a space opened holds. */
    static PrivateSpace of(Slots.Space space) throws IOException {
        PrivateSpace made=new PrivateSpace(space);
        Map<String,byte[]> held=space.take();
        made.read(held);made.saved=held;
        return made;
    }

    /**
     * What a space made a moment ago holds: nothing (the owner, 2026-10-06: "all private spaces open in the folder mode not
     * in the note mode, even more for the new generated ones"; decision 116). A new space opens as an empty folder with the +
     * ready, never into a note, so it is made empty and the only thing written is its index.
     */
    static Map<String,byte[]> empty() {
        return new LinkedHashMap<>();
    }

    // ---- reading and writing ------------------------------------------------------------------------------------------

    private void read(Map<String,byte[]> held) throws IOException {
        byte[] list=held.get(THINGS);
        if(list!=null)try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(list))) {
            if(in.readInt()!=1)throw new IOException("Unknown");
            int n=in.readInt();
            for(int i=0;i<n;i++) {
                Thing t=new Thing();t.id=in.readUTF();t.folder=in.readBoolean();t.parent=in.readUTF();t.title=in.readUTF();
                t.colour=in.readInt();t.tone=in.readInt();t.made=in.readLong();t.changed=in.readLong();
                t.binned=in.readBoolean();t.binnedFrom=in.readUTF();t.binnedAt=in.readLong();
                things.put(t.id,t);
            }
            n=in.readInt();
            for(int i=0;i<n;i++) {
                Kept k=new Kept();k.id=in.readUTF();k.note=in.readUTF();k.name=in.readUTF();k.type=in.readUTF();
                k.bytes=in.readLong();k.added=in.readLong();k.picture=in.readBoolean();
                files.put(k.id,k);
            }
            // Which things send a help request when they open, and to whom (decision 113). Written after the files, so a space
            // from before this reads back whole with these off; only the things that have it set are named.
            if(in.available()>0) {
                int withHelp=in.readInt();
                for(int i=0;i<withHelp;i++) {
                    String id=in.readUTF();int to=in.readInt();List<String> addresses=new ArrayList<>();
                    for(int j=0;j<to;j++)addresses.add(in.readUTF());
                    String message=readLong(in);
                    Thing t=things.get(id);if(t!=null){t.helpOnOpen=true;t.helpTo=addresses;t.helpMessage=message;}
                }
            }
        }
        for(Thing t:things.values()) {
            List<Version> kept=new ArrayList<>();versions.put(t.id,kept);
            byte[] said=held.get(NOTE+t.id);
            if(t.folder||said==null)continue;
            try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said))) {
                in.readInt();t.title=in.readUTF();t.body=readLong(in);
                int n=in.readInt();for(int i=0;i<n;i++)kept.add(new Version(in.readLong(),in.readUTF(),readLong(in)));
            }
        }
        for(Kept k:files.values()){byte[] b=held.get(FILE+k.id);if(b!=null)bytes.put(k.id,b);}
    }
    private byte[] writeThings() throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);
        out.writeInt(1);out.writeInt(things.size());
        for(Thing t:things.values()) {
            out.writeUTF(t.id);out.writeBoolean(t.folder);out.writeUTF(t.parent);out.writeUTF(t.folder?t.title:"");
            out.writeInt(t.colour);out.writeInt(t.tone);out.writeLong(t.made);out.writeLong(t.changed);
            out.writeBoolean(t.binned);out.writeUTF(t.binnedFrom);out.writeLong(t.binnedAt);
        }
        out.writeInt(files.size());
        for(Kept k:files.values()){out.writeUTF(k.id);out.writeUTF(k.note);out.writeUTF(k.name);out.writeUTF(k.type);out.writeLong(k.bytes);out.writeLong(k.added);out.writeBoolean(k.picture);}
        // The help-when-opened setting of each thing that has it on (decision 113): its id, its recipients and its words.
        int withHelp=0;for(Thing t:things.values())if(t.helpOnOpen)withHelp++;
        out.writeInt(withHelp);
        for(Thing t:things.values())if(t.helpOnOpen) {
            out.writeUTF(t.id);out.writeInt(t.helpTo.size());for(String a:t.helpTo)out.writeUTF(a);writeLong(out,t.helpMessage);
        }
        return b.toByteArray();
    }
    private byte[] writeNote(String id) throws IOException {
        Thing t=things.get(id);
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);
        out.writeInt(1);out.writeUTF(t.title);writeLong(out,t.body);
        List<Version> kept=versions.getOrDefault(id,List.of());out.writeInt(kept.size());
        for(Version v:kept){out.writeLong(v.at);out.writeUTF(v.title);writeLong(out,v.body);}
        return b.toByteArray();
    }
    /** Words longer than writeUTF takes, as a note's can be. */
    private static void writeLong(DataOutputStream out,String s) throws IOException {
        byte[] b=s.getBytes(java.nio.charset.StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);
    }
    private static String readLong(DataInputStream in) throws IOException {
        int n=in.readInt();if(n<0||n>(64<<20))throw new IOException("Damaged");
        byte[] b=new byte[n];in.readFully(b);return new String(b,java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * What changed, handed to the slots to be written by the next batches. Where it does not fit, nothing of it is kept
     * and everything is as it was last written: the caller says Private storage is full, inside.
     */
    void write() throws Slots.Full, IOException, java.security.GeneralSecurityException {
        if(changed.isEmpty()&&gone.isEmpty())return;
        Map<String,byte[]> out=new LinkedHashMap<>();
        out.put(THINGS,writeThings());
        for(String name:changed) {
            if(name.startsWith(NOTE)&&things.containsKey(name.substring(NOTE.length())))out.put(name,writeNote(name.substring(NOTE.length())));
            else if(name.startsWith(FILE)&&bytes.containsKey(name.substring(FILE.length())))out.put(name,bytes.get(name.substring(FILE.length())));
        }
        Set<String> removed=new HashSet<>(gone);removed.removeAll(out.keySet());
        try{space.put(out,removed);}
        catch(Slots.Full full){back();throw full;}
        Map<String,byte[]> now=new LinkedHashMap<>(saved);
        for(String name:removed)now.remove(name);
        now.putAll(out);saved=now;
        changed.clear();gone.clear();
    }
    /** Everything as it was last written. */
    private void back() throws IOException {
        things.clear();files.clear();versions.clear();bytes.clear();changed.clear();gone.clear();
        read(saved);
    }

    // ---- what can be done ---------------------------------------------------------------------------------------------

    /** What is directly in a folder ({@code ""} for the top), not binned, folders first, then the newest. */
    List<Thing> in(String parent) {
        List<Thing> out=new ArrayList<>();
        for(Thing t:things.values())if(!t.binned&&t.parent.equals(parent==null?"":parent)&&!inBin(t))out.add(t);
        out.sort((a,b)->a.folder!=b.folder?(a.folder?-1:1):Long.compare(b.changed,a.changed));
        return out;
    }
    /** What was put in the bin, newest first: a folder binned holds what it held. */
    List<Thing> binned() {
        List<Thing> out=new ArrayList<>();
        for(Thing t:things.values())if(t.binned)out.add(t);
        out.sort((a,b)->Long.compare(b.binnedAt,a.binnedAt));
        return out;
    }
    private boolean inBin(Thing t){for(Thing up=things.get(t.parent);up!=null;up=things.get(up.parent))if(up.binned)return true;return false;}

    Thing thing(String id){return things.get(id);}

    Thing newNote(String parent,long now) {
        Thing t=new Thing();t.parent=parent==null?"":parent;t.made=t.changed=now;
        things.put(t.id,t);versions.put(t.id,new ArrayList<>());changed.add(NOTE+t.id);return t;
    }
    Thing newFolder(String parent,String name,long now) {
        Thing t=new Thing();t.folder=true;t.parent=parent==null?"":parent;t.title=name==null||name.trim().isEmpty()?"Untitled":name.trim();t.made=t.changed=now;
        // Marked, so a folder made in a brand-new empty space is written even when nothing else changed (decision 116).
        things.put(t.id,t);versions.put(t.id,new ArrayList<>());mark();return t;
    }
    /** A note's words, as typed. Returns whether they changed. */
    boolean text(String id,String title,String body,long now) {
        Thing t=things.get(id);if(t==null||t.folder)return false;
        title=title==null?"":title;body=body==null?"":body;
        if(t.title.equals(title)&&t.body.equals(body))return false;
        t.title=title;t.body=body;t.changed=now;changed.add(NOTE+id);return true;
    }
    void rename(String id,String name) {
        Thing t=things.get(id);if(t==null)return;
        t.title=name==null?"":name.trim();if(!t.folder)changed.add(NOTE+id);mark();
    }
    void colour(String id,int colour){Thing t=things.get(id);if(t!=null){t.colour=colour;mark();}}
    void tone(String id,int tone){Thing t=things.get(id);if(t!=null){t.tone=tone;mark();}}
    /** Set, or clear, whether a private thing sends a help request when it opens, to whom, and with what words (decision 113). */
    void help(String id,boolean on,List<String> to,String message) {
        Thing t=things.get(id);if(t==null)return;
        t.helpOnOpen=on;t.helpTo=on&&to!=null?new ArrayList<>(to):new ArrayList<>();t.helpMessage=on&&message!=null?message:"";mark();
    }
    /** Into a folder, or to the top; never a folder into itself or into something inside it. */
    boolean move(String id,String parent) {
        Thing t=things.get(id);parent=parent==null?"":parent;
        if(t==null||t.parent.equals(parent))return false;
        for(Thing up=things.get(parent);up!=null;up=things.get(up.parent))if(up.id.equals(id))return false;
        if(!parent.isEmpty()&&(things.get(parent)==null||!things.get(parent).folder))return false;
        t.parent=parent;mark();return true;
    }
    /** Folders it could go to: every folder not binned, not itself, not inside it. */
    List<Thing> placesFor(String id) {
        List<Thing> out=new ArrayList<>();
        for(Thing f:things.values()) {
            if(!f.folder||f.binned||inBin(f)||f.id.equals(id))continue;
            boolean inside=false;for(Thing up=things.get(f.parent);up!=null;up=things.get(up.parent))if(up.id.equals(id))inside=true;
            if(!inside)out.add(f);
        }
        out.sort((a,b)->a.title.compareToIgnoreCase(b.title));
        return out;
    }
    void bin(String id,long now){Thing t=things.get(id);if(t==null||t.binned)return;t.binned=true;t.binnedAt=now;t.binnedFrom=t.parent;mark();}
    void putBack(String id) {
        Thing t=things.get(id);if(t==null||!t.binned)return;
        t.binned=false;Thing was=things.get(t.binnedFrom);
        t.parent=was!=null&&!was.binned&&!inBin(was)?t.binnedFrom:"";mark();
    }
    /** Gone for good, with everything in it and every file and version of it: nothing of it is written again. */
    void deleteForGood(String id) {
        Thing t=things.get(id);if(t==null)return;
        for(Thing child:new ArrayList<>(things.values()))if(child.parent.equals(id))deleteForGood(child.id);
        things.remove(id);versions.remove(id);gone.add(NOTE+id);changed.remove(NOTE+id);
        for(Kept k:new ArrayList<>(files.values()))if(k.note.equals(id))removeFile(k.id);
        mark();
    }
    void emptyBin(){for(Thing t:binned())deleteForGood(t.id);}

    /** A file kept with a note: a picture already made small, or any other file as it is. */
    Kept addFile(String note,String name,String type,byte[] data,boolean picture,long now) {
        Kept k=new Kept();k.note=note;k.name=name==null||name.isEmpty()?"File":name;k.type=type==null?"":type;
        k.bytes=data.length;k.picture=picture;k.added=now;
        files.put(k.id,k);bytes.put(k.id,data);changed.add(FILE+k.id);gone.remove(FILE+k.id);mark();
        return k;
    }
    void removeFile(String id) {
        if(files.remove(id)==null)return;
        // Not zeroed here: the state last written still holds it, to go back to (see back); wipe zeroes everything.
        bytes.remove(id);
        gone.add(FILE+id);changed.remove(FILE+id);mark();
    }
    List<Kept> filesOf(String note){List<Kept> out=new ArrayList<>();for(Kept k:files.values())if(k.note.equals(note))out.add(k);return out;}
    byte[] bytesOf(String id){return bytes.get(id);}

    /** What a note said, kept when its editing ends, unless that is what was kept last. */
    void keepVersion(String note,long now) {
        Thing t=things.get(note);if(t==null||t.folder)return;
        List<Version> kept=versions.computeIfAbsent(note,k->new ArrayList<>());
        Version last=kept.isEmpty()?null:kept.get(0);
        if(last!=null&&last.title.equals(t.title)&&last.body.equals(t.body))return;
        if(last==null&&t.title.isEmpty()&&t.body.isEmpty())return;
        kept.add(0,new Version(now,t.title,t.body));
        while(kept.size()>VERSIONS)kept.remove(kept.size()-1);
        changed.add(NOTE+note);
    }
    List<Version> versionsOf(String note){return new ArrayList<>(versions.getOrDefault(note,List.of()));}
    /** An older version put back: what it says now kept first, so nothing is lost by it. */
    void putBackVersion(String note,Version v,long now) {
        keepVersion(note,now);
        text(note,v.title,v.body,now);
    }

    /** How many slots this space uses now, for what is said inside. */
    int used(){return space.used();}

    /** Everything let go: contents, the bytes of every file (zeroed), what was written last. */
    void wipe() {
        for(byte[] b:bytes.values())Arrays.fill(b,(byte)0);
        for(byte[] b:saved.values())Arrays.fill(b,(byte)0);
        things.clear();files.clear();versions.clear();bytes.clear();saved.clear();changed.clear();gone.clear();
    }

    private void mark(){changed.add(THINGS);}
}
