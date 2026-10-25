// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.mininotes.desktop.platform.AtomicFile;
import org.mininotes.desktop.platform.database.Cursor;

/**
 * Notes and folders kept as files too (the owner, 2026-10-13; docs/FILES.md, decision 162): a note written as Markdown
 * in a folder anywhere on the PC, an external drive included, and kept up to date both ways. Mininotes writes the file
 * when the note changes, here or from somebody it is shared with; a change made to the file outside (Notepad, VS Code,
 * Obsidian) is taken in as the owner's own edit and goes on as any edit goes. Changed on both sides since Mininotes last
 * wrote it, nothing is lost: the file's words are taken in, and Mininotes' own stay in Versions. A folder kept on disk is
 * a Windows folder of such files, its folders as folders, both ways.
 *
 * <p>Which file is which note, and what the file said when Mininotes last wrote or read it, is kept in the notebook (the
 * table ondisk, this PC's own). A file that is not where it was is looked for beside where it was, by the id in its
 * header, and followed; one that cannot be found is lost, and the note says so, and why: moved or deleted, or on a drive
 * that is not connected, which comes back by itself.
 *
 * <p>Never written out: a private space and anything in it, and anything while the notebook is locked (decision 114's
 * guard, at this door). The writing refuses them itself, whatever a menu offered. Blocking: run on the notebook's worker.
 */
final class DesktopOnDisk {
    static final String NOTE="note",FOLDER="folder";
    /**
     * Any other file in a folder kept on disk, a .docx, a picture, a PDF: one of the folder's files in Mininotes, the same
     * bytes on both sides, so it is changed in Word or LibreOffice and goes to the owner's other devices (the owner,
     * 2026-10-14: "could Mininotes sync any type of file?"; docs/FILES.md, own devices first).
     */
    static final String FILE="file";
    /** Why a file is lost: not where it was; on a drive that is not connected; holding another note; too long to take in. */
    static final String GONE="gone",DRIVE="drive",OTHER="other",LONG="long";
    /**
     * Changed in Mininotes and in its file since Mininotes last wrote it, and waiting for the owner to say which words both
     * keep (the owner, 2026-10-14: "let's ask first"). Until then neither is written over.
     */
    static final String ASK="ask";
    /**
     * A file of a folder kept on disk no longer in Mininotes, deleted or moved out of the folder there: the file on disk stays
     * as it is, never deleted, and never taken in again as a new one. Not a lost thing: nothing is drawn for it.
     */
    static final String OUT="out";
    /**
     * A file shared through Mininotes whose syncing the owner switched off: still linked, so it can be switched on again,
     * nothing taken in or written out meanwhile (the owner, 2026-10-16: "after clicking stop syncing, we should have the
     * option, synchronise"). Not a lost thing: nothing is drawn for it but the switch.
     */
    static final String OFF="off";
    /** How long a file on disk is left unchanged before it is taken in: one still being copied or saved is not half taken. */
    static final long SETTLED=3000;
    /** How deep a folder kept on disk is looked through: deeper than anybody nests, short of a loop. */
    private static final int DEEPEST=12;
    /**
     * The most a file is read at: well past what a note holds (24,000 characters, four bytes each at most), so a file
     * that big is too long without anybody reading a film into memory to find out.
     */
    private static final long LARGEST=200_000;
    private static boolean tooBig(Path p){try{return Files.size(p)>LARGEST;}catch(IOException|RuntimeException unreadable){return false;}}

    /** One note or folder kept on disk, as the notebook keeps it. */
    record Link(String id,String kind,Path path,String hash,long at,long revision,String head,String under,String lost) {
        boolean note(){return NOTE.equals(kind);}
        boolean folder(){return FOLDER.equals(kind);}
        boolean file(){return FILE.equals(kind);}
        boolean isLost(){return !lost.isEmpty();}
        Link at(Path where){return new Link(id,kind,where,hash,at,revision,head,under,lost);}
        Link lost(String why){return new Link(id,kind,path,hash,at,revision,head,under,why);}
        Link under(String folder){return new Link(id,kind,path,hash,at,revision,head,folder,lost);}
        Link named(String name){return new Link(id,kind,path,hash,at,revision,name,under,lost);}
        Link seen(String newHash,long newAt,long newRevision,String newHead){return new Link(id,kind,path,newHash,newAt,newRevision,newHead,under,"");}
    }

    /** What a look at one thing did, for what is said and drawn again. */
    /** MADE: a file written for something new in Mininotes; ADDED: a note made from a file put in a folder kept on disk. */
    /** FILE_ADDED, FILE_TAKEN: a folder's file put there, or changed there, taken in. */
    enum Did { NOTHING, WROTE, TOOK_IN, BOTH, READ_ONLY, LOST, FOUND, LEFT, MADE, ADDED, FILE_ADDED, FILE_TAKEN }
    record Change(String id,Did did,String name) {}

    private final NoteStore store;
    private final Predicate<String> privately;
    private final BooleanSupplier locked;
    /** Each thing kept on disk, by id: "" while all is well, else why it is lost. What the icons' marks are drawn from. */
    volatile Map<String,String> states=Map.of();
    /** Where each is, and those written into a folder kept on disk, which go with that folder: for the menus and the line. */
    private volatile Map<String,Path> paths=Map.of();
    private volatile Set<String> underFolders=Set.of();

    /**
     * @param privately whether an id is a private space's or in one: never written out (decision 111)
     * @param locked    whether the notebook is locked, or locking: nothing is written out then
     */
    DesktopOnDisk(NoteStore store,Predicate<String> privately,BooleanSupplier locked) {
        this.store=store;this.privately=privately==null?id->false:privately;this.locked=locked==null?()->false:locked;
    }

    // ---- the notebook's side ----------------------------------------------------------------------------------------

    List<Link> links() {
        List<Link> all=new ArrayList<>();
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT id,kind,path,hash,at,revision,head,under,lost FROM ondisk ORDER BY rowid",null)) {
            while(c.moveToNext())all.add(read(c));
        }
        return all;
    }
    Link link(String id) {
        if(id==null)return null;
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT id,kind,path,hash,at,revision,head,under,lost FROM ondisk WHERE id=?",new String[]{id})) {
            return c.moveToFirst()?read(c):null;
        }
    }
    private static Link read(Cursor c) {
        return new Link(c.getString(0),c.getString(1),Path.of(c.getString(2)),c.getString(3),c.getLong(4),c.getLong(5),c.getString(6),c.getString(7),c.getString(8));
    }
    private void put(Link l) {
        store.getWritableDatabase().execSQL("INSERT OR REPLACE INTO ondisk(id,kind,path,hash,at,revision,head,under,lost) VALUES(?,?,?,?,?,?,?,?,?)",
            new Object[]{l.id,l.kind,l.path.toString(),l.hash,l.at,l.revision,l.head,l.under,l.lost});
    }
    private void forget(String id){store.getWritableDatabase().execSQL("DELETE FROM ondisk WHERE id=? OR under=?",new Object[]{id,id});}

    /** Whether a thing is kept on disk, and how: null for not, "" for kept and well, else why it is lost. */
    String state(String id){return id==null?null:states.get(id);}

    void refreshStates() {
        Map<String,String> now=new HashMap<>();Map<String,Path> where=new HashMap<>();Set<String> under=new HashSet<>();
        Set<String> off=new HashSet<>();
        for(Link l:links()) {
            if(OUT.equals(l.lost))continue;
            if(OFF.equals(l.lost)){off.add(l.id);where.put(l.id,l.path);continue;}
            now.put(l.id,l.lost);where.put(l.id,l.path);if(!l.under.isEmpty())under.add(l.id);
        }
        states=Collections.unmodifiableMap(now);paths=Collections.unmodifiableMap(where);underFolders=Collections.unmodifiableSet(under);offFiles=Collections.unmodifiableSet(off);
    }
    private volatile Set<String> offFiles=Set.of();
    /** Whether a file shared through Mininotes is linked to its file on this PC with syncing switched off. */
    boolean syncOff(String id){return id!=null&&offFiles.contains(id);}
    /** Whether a file is linked to a file on this PC on its own (Share a file through Mininotes), syncing or not. */
    boolean linkedFile(String id){Link l=id==null?null:link(id);return l!=null&&l.file()&&l.under.isEmpty()&&!OUT.equals(l.lost);}

    /**
     * Syncing with the file on this PC switched off or on again. Off: nothing is taken in or written out, and both stay as
     * they are. On: the next look makes them agree, asking which version to keep where both changed meanwhile.
     */
    void syncing(String id,boolean on) {
        Link l=link(id);
        if(l==null||!l.file())throw new IllegalStateException("That file is not linked to a file on this PC.");
        if(on){if(OFF.equals(l.lost))put(new Link(l.id,l.kind,l.path,l.hash,0,l.revision,l.head,l.under,""));}
        else if(!OFF.equals(l.lost))put(l.lost(OFF));
        refreshStates();
    }

    /** Where a thing kept on disk is, as last read; null for one not kept. */
    Path pathOf(String id){return id==null?null:paths.get(id);}
    /** Whether it was written into a folder kept on disk, and so is kept, or kept inside only, with that folder. */
    boolean underFolder(String id){return id!=null&&underFolders.contains(id);}

    /** The refusal, said before anything is written: a private thing, or a locked notebook, is never written out. */
    private void mayWrite(String id) {
        if(locked.getAsBoolean())throw new IllegalStateException("Mininotes is locked, so nothing is written out.");
        if(id==null||privately.test(id))throw new IllegalStateException("What is private is never written out of Mininotes.");
    }

    // ---- keeping a note as a file -----------------------------------------------------------------------------------

    /** A note kept as a file in a folder chosen: written there as its name, .md, and kept up to date from now on. */
    Link keepNote(String id,Path folder) throws IOException{return keepNote(id,folder,"",null);}

    /**
     * A note kept as the file named in Windows' save box: its title, unless another name was typed there, which the note
     * then takes, as a file renamed in Explorer gives the note its name. A file of that name already there is never written
     * over: the note is kept beside it, as Name (2).md.
     */
    Link keepNoteAs(String id,Path file) throws IOException {
        mayWrite(id);
        NoteStore.Note n=store.get(id);
        if(n==null||n.deleted)throw new IllegalStateException("This note is not here.");
        if(link(id)!=null)throw new IllegalStateException("This note is already kept as a file.");
        String stem=NoteFile.fileName(NoteFile.nameOf(file.getFileName().toString()));
        if(!stem.equals(NoteFile.fileName(name(n)))) {
            var db=store.getWritableDatabase();db.beginTransaction();
            try {
                store.keepVersion(id,"");
                NoteStore.Note renamed=store.get(id).copy();renamed.title=stem;renamed.revision=n.revision+1;
                renamed.updated=System.currentTimeMillis();renamed.by=Writers.ME;store.save(renamed);
                db.setTransactionSuccessful();
            } finally{db.endTransaction();}
        }
        return keepNote(id,file.toAbsolutePath().getParent(),"",stem);
    }

    private Link keepNote(String id,Path folder,String under,String named) throws IOException {
        mayWrite(id);
        NoteStore.Note n=store.get(id);
        if(n==null||n.deleted)throw new IllegalStateException("This note is not here.");
        Link was=link(id);
        if(was!=null)throw new IllegalStateException("This note is already kept as a file.");
        Path file=free(folder,named!=null?named:NoteFile.fileName(name(n)),NoteFile.EXTENSION);
        byte[] bytes=NoteFile.write(id,"",n.body).getBytes(StandardCharsets.UTF_8);
        write(file,bytes);
        Link l=new Link(id,NOTE,file,hash(bytes),stamp(file),n.revision,"",under,"");
        put(l);files(l,n);refreshStates();
        return l;
    }

    /** What a note is called on its file: its title, else its first line, else Untitled. */
    static String name(NoteStore.Note n) {
        if(n.title!=null&&!n.title.isBlank())return n.title.strip();
        String first=Given.firstLine(n.body);
        return first.isEmpty()?"Untitled":first;
    }

    /** No longer kept on disk: the note stays as it is, and so does the file, where it is. Neither is deleted. */
    void keepInsideOnly(String id){forget(id);refreshStates();}

    /**
     * The owner's answer for a note changed in both places, set up so the next look does the rest the usual way: the file's
     * words, by taking the note's change as seen (so only the file has changed, and it is taken in, Mininotes' words to
     * Versions); Mininotes' words, by keeping the file's in Versions and taking the file as seen (so only the note has
     * changed, and the file is written from it).
     */
    void settled(String id,boolean theFiles) throws IOException {
        Link l=link(id);NoteStore.Note n=store.get(id);
        if(l==null||n==null)return;
        if(theFiles)put(l.seen(l.hash,l.at,n.revision,l.head));
        else {
            byte[] bytes=Files.readAllBytes(l.path);
            NoteFile.Read r=NoteFile.read(new String(bytes,StandardCharsets.UTF_8));
            store.keepVersion(id,n.revision,"",n.title,r.body());
            put(l.seen(hash(bytes),stamp(l.path),l.revision,l.head));
        }
        refreshStates();
    }

    /** Where a lost note's file is now, picked by its owner: followed only if its header says it is this note. */
    Change findIt(String id,Path picked) throws IOException {
        mayWrite(id);
        Link l=link(id);
        if(l==null)throw new IllegalStateException("This note is not kept as a file.");
        Path file=picked.toAbsolutePath().normalize();
        if(tooBig(file))throw new IllegalStateException("That file is not this note's: it has no Mininotes id.");
        NoteFile.Read r=NoteFile.read(Files.readString(file,StandardCharsets.UTF_8));
        if(!id.equals(r.id()))throw new IllegalStateException(r.id()==null?"That file is not this note's: it has no Mininotes id.":"That file is another note's.");
        for(Link other:links())if(!other.id.equals(id)&&other.path.equals(file))throw new IllegalStateException("That file is another note's.");
        l=l.at(file).lost("");put(l);
        Change c=check(l);refreshStates();
        return c.did()==Did.NOTHING?new Change(id,Did.FOUND,c.name()):c;
    }

    /**
     * A Markdown file opened, or dropped on Mininotes: a note kept as that file, where it is (docs/FILES.md, option B). A file
     * kept already is that note; one of a note no longer followed is followed again; any other becomes a new note, made in
     * {@code folder}, and the header that says which note it is is written into it.
     */
    NoteStore.Note open(Path picked,String folder) throws IOException {
        Path file=picked.toAbsolutePath().normalize();
        for(Link l:links())if(l.note()&&l.path.equals(file)){NoteStore.Note n=store.get(l.id);if(n!=null&&!n.deleted)return n;forget(l.id);}
        if(tooBig(file))throw new IllegalStateException("This file is too long for a note: 24,000 characters at most.");
        String text=Files.readString(file,StandardCharsets.UTF_8);
        NoteFile.Read r=NoteFile.read(text);
        String stem=NoteFile.nameOf(file.getFileName().toString());
        String over=DesktopEdits.limits(stem.length()>160?stem.substring(0,160):stem,r.body());
        if(over!=null)throw new IllegalStateException("This file is too long for a note: 24,000 characters at most.");
        if(r.id()!=null) {
            NoteStore.Note n=store.get(r.id());Link l=link(r.id());
            // A note that was kept as this file, or lost it: followed to it again, and whatever the file says now taken in.
            if(n!=null&&!n.deleted&&(l==null||l.isLost()||!Files.exists(l.path))) {
                mayWrite(n.id);
                put(new Link(n.id,NOTE,file,"",0,n.revision,r.head(),l==null?"":l.under,""));
                check(link(n.id));refreshStates();
                return store.get(n.id);
            }
        }
        NoteStore.Note n=new NoteStore.Note();
        n.book=NoteStore.home(folder)?Things.HOME:folder;n.title=stem.length()>160?stem.substring(0,160):stem;n.body=r.body();n.by=Writers.ME;
        mayWrite(n.id);
        store.save(n);
        // The file now says which note it is, so it is known wherever it is moved or renamed to.
        byte[] bytes=NoteFile.write(n.id,r.head(),n.body).getBytes(StandardCharsets.UTF_8);
        write(file,bytes);
        put(new Link(n.id,NOTE,file,hash(bytes),stamp(file),n.revision,r.head(),"",""));refreshStates();
        return n;
    }

    // ---- keeping a folder on disk -----------------------------------------------------------------------------------

    /** A folder kept on disk: a Windows folder of its name in the one chosen, everything in it written there, both ways. */
    Link keepFolder(String id,Path parent) throws IOException {
        mayWrite(id);
        String name=store.nameOf(id,true);
        if(name.isEmpty())throw new IllegalStateException("This folder is not here.");
        if(link(id)!=null)throw new IllegalStateException("This folder is already kept on disk.");
        Path dir=free(parent,NoteFile.fileName(name),"");
        Files.createDirectories(dir);
        Link root=new Link(id,FOLDER,dir,"",stamp(dir),0,name,"","");
        put(root);
        mirror(root);refreshStates();
        return root;
    }

    /**
     * Share a file through Mininotes…: one file anywhere on this PC, an .ods or a .docx, left where it is and opened by its
     * own program, kept in step with a file of the same bytes on Home, which goes to the owner's devices (the owner,
     * 2026-10-15: "we just reference the file, it stays outside the app, is opened by its associated app ... only the
     * synchronisation is done through Mininotes"). Whole versions travel, not edits merged inside: changed on two sides,
     * the owner is asked which version it keeps. A Markdown file is a note instead (Open a Markdown file…).
     */
    Link syncFile(Path picked) throws IOException {
        if(locked.getAsBoolean())throw new IllegalStateException("Mininotes is locked, so nothing is written out.");
        Path file=picked.toAbsolutePath().normalize();
        if(!Files.isRegularFile(file))throw new IllegalStateException("That file is not there.");
        String name=file.getFileName().toString();
        if(NoteFile.markdownFile(name))throw new IllegalStateException("A Markdown file becomes a note: Open a Markdown file… does that.");
        for(Link l:links())if(l.path.equals(file)||l.folder()&&file.startsWith(l.path))throw new IllegalStateException("That file is already kept in step.");
        if(Files.size(file)>Attachment.PLENTY)throw new IllegalStateException("That file is too big for Mininotes.");
        String fileHash=hashOf(file);
        NoteStore.Held made;
        try(java.io.InputStream in=Files.newInputStream(file)){made=store.addFileExact(Things.HOME,name,Files.probeContentType(file),in);}
        Link l=new Link(made.id,FILE,file,fileHash,stamp(file),store.keptFileRevision(made.id),made.name,"","");
        put(l);refreshStates();
        return l;
    }

    /**
     * Keep a Windows folder in sync…: a folder already on this PC, everything in it, made a folder of Mininotes of its name
     * on Home, and kept in step both ways from now on, as a folder kept on disk is (the owner, 2026-10-14: "could Mininotes
     * sync any type of file?"; docs/FILES.md). What Mininotes keeps goes to the owner's own devices, as everything does.
     * A drive itself, or a folder that is kept in step already or holds or is in one, is refused.
     */
    Link syncFolder(Path picked) throws IOException {
        if(locked.getAsBoolean())throw new IllegalStateException("Mininotes is locked, so nothing is written out.");
        Path dir=picked.toAbsolutePath().normalize();
        if(!Files.isDirectory(dir))throw new IllegalStateException("That folder is not there.");
        if(dir.getParent()==null||dir.getFileName()==null)throw new IllegalStateException("A whole drive cannot be kept in step. Pick a folder on it.");
        for(Link l:links())if(l.folder()&&(dir.startsWith(l.path)||l.path.startsWith(dir)))
            throw new IllegalStateException("That folder, or one in it, is already kept in step.");
        String name=dir.getFileName().toString();
        NoteStore.Shelf made=store.addCollectionIn(Things.HOME,name);
        Link root=new Link(made.id,FOLDER,dir,"",stamp(dir),0,name,"","");
        put(root);
        mirror(root);refreshStates();
        return root;
    }

    /**
     * A folder kept on disk made to agree both ways: what Mininotes has that the disk has not is written, and what the disk
     * has that Mininotes has not is taken in: a folder made in Explorer becomes a folder, a .md put there becomes a note,
     * and a note's file moved to another of its folders moves the note there. A file taken away is not taken as a note
     * deleted: the note is lost, and says so (docs/FILES.md).
     */
    List<Change> mirror(Link root) throws IOException {
        List<Change> changes=new ArrayList<>();
        if(!Files.isDirectory(root.path)) {
            String why=driveGone(root.path)?DRIVE:GONE;
            if(!why.equals(root.lost)){put(root.lost(why));changes.add(new Change(root.id,Did.LOST,root.path.getFileName().toString()));}
            return changes;
        }
        if(root.isLost()){put(root.lost(""));changes.add(new Change(root.id,Did.FOUND,root.path.getFileName().toString()));}
        if(store.nameOf(root.id,true).isEmpty()){forget(root.id);changes.add(new Change(root.id,Did.LEFT,root.path.getFileName().toString()));return changes;}
        Set<String> inside=new HashSet<>();
        fromMininotes(root,root.id,root.path,0,changes,inside);
        fromDisk(root,root.id,root.path,0,changes);
        // What the disk's side just took in is in the folder too, though Mininotes' side looked before it was.
        for(Change c:changes)if(c.did()==Did.FILE_ADDED||c.did()==Did.FOUND)inside.add(c.id());
        for(Link l:links())if(l.file()&&l.under.equals(root.id)) {
            if(!inside.contains(l.id)){if(!OUT.equals(l.lost))put(l.lost(OUT));continue;}
            Change c=checkFile(l);if(c.did()!=Did.NOTHING)changes.add(c);
        }
        // A note moved out of the folder in Mininotes keeps its file where it is, as a note kept as a file on its own.
        for(Link l:links())if(l.note()&&l.under.equals(root.id)&&!inside.contains(l.id)) {
            NoteStore.Note n=store.get(l.id);
            if(n!=null&&!n.deleted&&!n.archived&&!inside(root.id,n.book))put(l.under(""));
        }
        return changes;
    }

    /** Mininotes' side: every live note and folder in it given its file and its folder on disk. */
    private void fromMininotes(Link root,String folder,Path dir,int depth,List<Change> changes,Set<String> inside) throws IOException {
        if(depth>DEEPEST)return;
        for(String note:notesIn(folder)) {
            inside.add(note);
            Link l=link(note);
            if(privately.test(note))continue;
            if(l==null){Link made=keepNote(note,dir,root.id,null);changes.add(new Change(note,Did.MADE,made.path.getFileName().toString()));continue;}
            // Moved to another of the folder's folders in Mininotes: its file goes to that folder on disk, its files with it.
            if(l.under.equals(root.id)&&Files.isRegularFile(l.path)&&!dir.equals(l.path.getParent())) {
                String stem=NoteFile.nameOf(l.path.getFileName().toString());
                Path to=free(dir,stem,NoteFile.EXTENSION),files=l.path.resolveSibling(stem+NoteFile.FILES);
                Files.move(l.path,to);
                Path filesTo=to.resolveSibling(NoteFile.nameOf(to.getFileName().toString())+NoteFile.FILES);
                if(Files.isDirectory(files)&&!Files.exists(filesTo))Files.move(files,filesTo);
                put(l.at(to).seen(l.hash,stamp(to),l.revision,l.head));
            }
        }
        for(NoteStore.Held f:store.filesOf(NoteStore.Branch.Kind.COLLECTION,folder)) {
            inside.add(f.id);
            if(privately.test(f.id))continue;
            Link l=link(f.id);
            if(l==null){Path made=writeOut(f,dir,root.id);if(made!=null)changes.add(new Change(f.id,Did.MADE,made.getFileName().toString()));continue;}
            if(!l.under.equals(root.id)||!Files.isRegularFile(l.path))continue;
            // Moved to another of the folder's folders in Mininotes, or renamed there: its file goes there, under that name.
            boolean moved=!dir.equals(l.path.getParent()),renamed=!f.name.equals(l.head);
            if(moved||renamed) {
                String[] parts=stemAndExtension(renamed?DesktopFiles.onDisk(f.name):l.path.getFileName().toString());
                Path to=free(dir,parts[0],parts[1]);
                try{Files.move(l.path,to);put(l.at(to).seen(l.hash,stamp(to),l.revision,f.name));}
                catch(IOException busy){/* open somewhere: moved at the next look */}
            }
        }
        for(String[] sub:foldersIn(folder)) {
            Link l=link(sub[0]);
            if(l==null) {
                if(privately.test(sub[0]))continue;
                Path made=free(dir,NoteFile.fileName(sub[1]),"");Files.createDirectories(made);
                l=new Link(sub[0],FOLDER,made,"",stamp(made),0,sub[1],root.id,"");put(l);
                changes.add(new Change(sub[0],Did.MADE,made.getFileName().toString()));
            } else if(l.under.equals(root.id)&&!sub[1].equals(l.head)&&Files.isDirectory(l.path)) {
                // Renamed in Mininotes: its folder on disk takes the new name, where that name is free.
                Path to=l.path.resolveSibling(NoteFile.fileName(sub[1]));
                if(!to.equals(l.path)&&!Files.exists(to)){Files.move(l.path,to);moveUnder(l.path,to);l=link(sub[0]);}
                l=l.named(sub[1]);put(l);
            }
            // Moved into another of the folder's folders in Mininotes: its folder on disk goes there too.
            if(l.under.equals(root.id)&&Files.isDirectory(l.path)&&!dir.equals(l.path.getParent())) {
                Path to=free(dir,l.path.getFileName().toString(),"");Path was=l.path;
                Files.move(was,to);moveUnder(was,to);l=link(sub[0]);
            }
            if(l.under.equals(root.id)&&Files.isDirectory(l.path))fromMininotes(root,sub[0],l.path,depth+1,changes,inside);
        }
    }

    /** The disk's side: folders made there made here, .md files put there made notes, a note's file moved followed. */
    private void fromDisk(Link root,String folder,Path dir,int depth,List<Change> changes) throws IOException {
        if(depth>DEEPEST)return;
        List<Path> dirs=new ArrayList<>(),files=new ArrayList<>(),others=new ArrayList<>();
        try(DirectoryStream<Path> in=Files.newDirectoryStream(dir)) {
            for(Path p:in) {
                String name=p.getFileName().toString();
                if(name.startsWith("."))continue;
                if(Files.isDirectory(p)){if(!name.endsWith(NoteFile.FILES))dirs.add(p);}
                else if(NoteFile.markdownFile(name))files.add(p);
                else if(plainFile(name))others.add(p);
            }
        }
        dirs.sort(null);files.sort(null);others.sort(null);
        for(Path d:dirs) {
            Link l=folderAt(d);
            if(l==null)l=renamedFolder(root,d);
            if(l==null) {
                String name=d.getFileName().toString();
                NoteStore.Shelf made=store.addCollectionIn(folder,name);
                l=new Link(made.id,FOLDER,d,"",stamp(d),0,name,root.id,"");put(l);
                changes.add(new Change(made.id,Did.ADDED,name));
            }
            if(l.under.equals(root.id))fromDisk(root,l.id,d,depth+1,changes);
        }
        for(Path f:files) {
            Path file=f.toAbsolutePath().normalize();
            if(noteAt(file)!=null||tooBig(file))continue;
            String text;
            try{text=Files.readString(file,StandardCharsets.UTF_8);}catch(IOException|RuntimeException unreadable){continue;}
            NoteFile.Read r=NoteFile.read(text);
            Link l=r.id()==null?null:link(r.id());
            if(l!=null&&l.note()&&!Files.exists(l.path)) {
                // Moved here in Explorer: followed, and where it is kept under this folder, the note goes where its file went.
                l=l.at(file).lost("");put(l);
                if(l.under.equals(root.id)){NoteStore.Note n=store.get(l.id);if(n!=null&&!folder.equals(home(n.book)))store.moveInto(NoteStore.Branch.Kind.PAGE,l.id,folder);}
                changes.add(new Change(l.id,Did.FOUND,file.getFileName().toString()));
                continue;
            }
            NoteStore.Note known=r.id()==null?null:store.get(r.id());
            if(l==null&&known!=null&&!known.deleted&&folder.equals(home(known.book))) {
                // A note of this folder, no longer followed: followed again rather than made twice.
                put(new Link(known.id,NOTE,file,"",0,known.revision,r.head(),root.id,""));
                changes.add(new Change(known.id,Did.FOUND,file.getFileName().toString()));
                continue;
            }
            if(DesktopEdits.limits("",r.body())!=null)continue;
            // Put there in Explorer, or a copy of another note's file: a new note, and the file says so from now on.
            NoteStore.Note n=new NoteStore.Note();
            String stem=NoteFile.nameOf(file.getFileName().toString());
            n.book=folder;n.title=stem.length()>160?stem.substring(0,160):stem;n.body=r.body();n.by=Writers.ME;
            store.save(n);
            byte[] bytes=NoteFile.write(n.id,r.head(),n.body).getBytes(StandardCharsets.UTF_8);
            write(file,bytes);
            put(new Link(n.id,NOTE,file,hash(bytes),stamp(file),n.revision,r.head(),root.id,""));
            changes.add(new Change(n.id,Did.ADDED,file.getFileName().toString()));
        }
        for(Path f:others) {
            Path file=f.toAbsolutePath().normalize();
            if(fileAt(file)!=null||!settled(file))continue;
            Change c=takeIn(root,folder,file);
            if(c!=null)changes.add(c);
        }
    }

    // ---- any other file in a folder kept on disk --------------------------------------------------------------------

    /**
     * Whether a name in a folder kept on disk is a file kept both ways: anything but a note's .md, and but what programs
     * leave beside what they work on (Word's ~$ lock, LibreOffice's .~lock, a download not finished, Windows' own).
     */
    static boolean plainFile(String name) {
        String n=name.toLowerCase(Locale.ROOT);
        if(n.startsWith(".")||n.startsWith("~")||NoteFile.markdownFile(name))return false;
        for(String leftover:new String[]{".tmp",".part",".crdownload",".partial",".lnk",".bak"})if(n.endsWith(leftover))return false;
        return !n.equals("desktop.ini")&&!n.equals("thumbs.db");
    }

    /** Whether a file has been left alone long enough to be read whole. */
    private static boolean settled(Path p){long at=stamp(p);return at!=0&&System.currentTimeMillis()-at>=SETTLED;}

    /**
     * A file put in a folder kept on disk taken into that folder in Mininotes, exactly; or, where its bytes are a file of the
     * folder no longer where it was, that file followed (renamed or moved in Explorer) and given the name it has now.
     */
    private Change takeIn(Link root,String folder,Path file) {
        String name=file.getFileName().toString();
        long size;try{size=Files.size(file);}catch(IOException gone){return null;}
        if(size>Attachment.PLENTY)return null;
        String fileHash;try{fileHash=hashOf(file);}catch(IOException busy){return null;}
        for(Link l:links())if(l.file()&&l.under.equals(root.id)&&fileHash.equals(l.hash)&&!Files.exists(l.path)&&store.keptFileRevision(l.id)>=0) {
            if(!name.equals(l.head))try{store.renameFile(l.id,name);}catch(RuntimeException readOnly){/* the name here stays */}
            put(l.at(file).seen(l.hash,stamp(file),store.keptFileRevision(l.id),name));
            return new Change(l.id,Did.FOUND,name);
        }
        NoteStore.Held made;
        try(java.io.InputStream in=Files.newInputStream(file)){made=store.addFileExact(folder,name,Files.probeContentType(file),in);}
        catch(IOException|RuntimeException busy){return null;}
        put(new Link(made.id,FILE,file,fileHash,stamp(file),store.keptFileRevision(made.id),made.name,root.id,""));
        return new Change(made.id,Did.FILE_ADDED,name);
    }

    /** One of a folder's files written into its folder on disk, as it is, under a name nothing there has. Null for one not here yet. */
    private Path writeOut(NoteStore.Held f,Path dir,String under) throws IOException {
        byte[] bytes;
        try{bytes=store.bytesOf(f);}catch(IOException|RuntimeException notHere){return null;}
        String[] parts=stemAndExtension(DesktopFiles.onDisk(f.name));
        Path to=free(dir,parts[0],parts[1]);
        write(to,bytes);
        put(new Link(f.id,FILE,to,hash(bytes),stamp(to),store.keptFileRevision(f.id),f.name,under,""));
        return to;
    }

    /**
     * One file of a folder kept on disk and its file in Mininotes made to agree: changed on disk, taken in exactly; changed
     * in Mininotes, written out (later, while another program holds it); changed on both, both kept, Mininotes' beside it as
     * "(other version)", which the next look takes in as a file of its own, so neither is lost. A file taken away on disk is
     * lost, never deleted in Mininotes; one taken out of Mininotes stays on disk.
     */
    Change checkFile(Link l) throws IOException {
        String called=l.path.getFileName().toString();
        if(OUT.equals(l.lost)||ASK.equals(l.lost)||OFF.equals(l.lost)||locked.getAsBoolean()||privately.test(l.id))return new Change(l.id,Did.NOTHING,called);
        long revision=store.keptFileRevision(l.id);
        // Put away here, in the bin or the archive: nothing followed until it is put back (the owner, 2026-10-16: "in bin so
        // the user can relink it easily, but it has to be clear that it is not deleting").
        if(revision>=0&&store.fileAway(l.id))return new Change(l.id,Did.NOTHING,called);
        if(revision<0){put(l.lost(OUT));return new Change(l.id,Did.NOTHING,called);}
        if(!Files.isRegularFile(l.path)) {
            if(GONE.equals(l.lost))return new Change(l.id,Did.NOTHING,called);
            put(l.lost(GONE));return new Change(l.id,Did.LOST,called);
        }
        long time=stamp(l.path);boolean wasLost=l.isLost();
        if(!wasLost&&time==l.at&&revision==l.revision)return new Change(l.id,Did.NOTHING,called);
        if(!settled(l.path))return new Change(l.id,Did.NOTHING,called);
        String fileHash;try{fileHash=hashOf(l.path);}catch(IOException busy){return new Change(l.id,Did.NOTHING,called);}
        boolean onDisk=!fileHash.equals(l.hash),here=revision!=l.revision;
        if(!onDisk&&!here){put(l.seen(l.hash,time,l.revision,l.head));return new Change(l.id,wasLost?Did.FOUND:Did.NOTHING,called);}
        NoteStore.Held kept=store.file(l.id);
        if(!onDisk) {
            byte[] bytes;
            try{bytes=store.bytesOf(kept);}catch(IOException|RuntimeException notHere){return new Change(l.id,Did.NOTHING,called);}
            // A revision moved on by something other than its bytes (who has it, where it is): nothing to write.
            if(hash(bytes).equals(l.hash)){put(l.seen(l.hash,time,revision,l.head));return new Change(l.id,Did.NOTHING,called);}
            try{write(l.path,bytes);}catch(IOException held){return new Change(l.id,Did.NOTHING,called);}
            put(l.seen(hash(bytes),stamp(l.path),revision,l.head));
            return new Change(l.id,Did.WROTE,called);
        }
        if(!store.mayChangeFile(l.id)) {
            // Read only here: the file on disk is left as it was changed, and Mininotes' stays as it is.
            put(l.seen(fileHash,time,revision,l.head));
            return new Change(l.id,Did.READ_ONLY,called);
        }
        if(here) {
            byte[] theirs;
            try{theirs=store.bytesOf(kept);}catch(IOException|RuntimeException notHere){theirs=null;}
            // Both only where Mininotes' bytes are not what was last written out: a revision moved on by anything else is not
            // a change. Both: asked, as a note is (the owner, 2026-10-15: "let's start with asking which version to keep").
            if(theirs!=null&&!hash(theirs).equals(l.hash)){put(l.lost(ASK));return new Change(l.id,Did.BOTH,called);}
        }
        try(java.io.InputStream in=Files.newInputStream(l.path)){store.replaceFile(l.id,kept.kind,in,true);}
        catch(IOException|RuntimeException busy){return new Change(l.id,Did.NOTHING,called);}
        put(l.seen(fileHash,time,store.keptFileRevision(l.id),l.head));
        return new Change(l.id,Did.FILE_TAKEN,called);
    }

    /**
     * The owner's answer for a file changed on disk and in Mininotes: the one on this PC taken in, Mininotes' replaced; or
     * Mininotes' written over the one on disk. Either way the two agree again, and the question is gone.
     */
    Change settleFile(String id,boolean theDisk) throws IOException {
        Link l=link(id);NoteStore.Held kept=store.file(id);
        if(l==null||!l.file()||kept==null)throw new IllegalStateException("That file is not here any more.");
        String called=l.path.getFileName().toString();
        if(theDisk) {
            String fileHash=hashOf(l.path);
            try(java.io.InputStream in=Files.newInputStream(l.path)){store.replaceFile(id,kept.kind,in,true);}
            put(l.seen(fileHash,stamp(l.path),store.keptFileRevision(id),l.head));refreshStates();
            return new Change(id,Did.FILE_TAKEN,called);
        }
        byte[] bytes=store.bytesOf(kept);
        write(l.path,bytes);
        put(l.seen(hash(bytes),stamp(l.path),store.keptFileRevision(id),l.head));refreshStates();
        return new Change(id,Did.WROTE,called);
    }

    /** The files waiting for the owner to say which version they keep, by id, with their names. */
    Map<String,String> filesAsked() {
        Map<String,String> out=new LinkedHashMap<>();
        for(Link l:links())if(l.file()&&ASK.equals(l.lost))out.put(l.id,l.path.getFileName().toString());
        return out;
    }

    /** A file's name parted where its kind begins: Report and .docx; a name with no kind, all of it and nothing. */
    static String[] stemAndExtension(String name) {
        int dot=name.lastIndexOf('.');
        return dot>0?new String[]{name.substring(0,dot),name.substring(dot)}:new String[]{name,""};
    }

    private Link fileAt(Path f){for(Link l:links())if(l.file()&&l.path.equals(f))return l;return null;}

    /** A file's hash, read as it streams: a film is never held in memory to be compared. */
    static String hashOf(Path file) throws IOException {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(java.io.InputStream in=Files.newInputStream(file)){byte[] piece=new byte[65536];int n;while((n=in.read(piece))!=-1)digest.update(piece,0,n);}
            return Update.hex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException none){throw new IllegalStateException(none);}
    }

    /**
     * A folder on disk with no folder of ours, whose notes were a folder whose own is gone: that folder, renamed or moved in
     * Explorer, followed there and given the name it has now. Null for a folder that is new.
     */
    private Link renamedFolder(Link root,Path d) throws IOException {
        Set<String> ids=new HashSet<>();
        try(DirectoryStream<Path> in=Files.newDirectoryStream(d)) {
            for(Path p:in)if(NoteFile.markdownFile(p.getFileName().toString())&&Files.isRegularFile(p)&&!tooBig(p))
                try{String id=NoteFile.read(Files.readString(p,StandardCharsets.UTF_8)).id();if(id!=null)ids.add(id);}catch(IOException|RuntimeException unreadable){/* not a note */}
        }
        for(Link l:links()) {
            if(!l.folder()||!l.under.equals(root.id)||Files.exists(l.path))continue;
            for(String id:ids){NoteStore.Note n=store.get(id);
                if(n!=null&&l.id.equals(n.book)) {
                    String name=d.getFileName().toString();
                    moveUnder(l.path,d);store.renameCollection(l.id,name);
                    Link moved=link(l.id).at(d).named(name).lost("");put(moved);return moved;
                }}
        }
        return null;
    }

    /** Every link whose path was in a folder that moved, moved with it. */
    private void moveUnder(Path was,Path now) {
        for(Link l:links())if(l.path.startsWith(was)&&!l.path.equals(was))put(l.at(now.resolve(was.relativize(l.path))));
        Link self=null;for(Link l:links())if(l.path.equals(was))self=l;
        if(self!=null)put(self.at(now));
    }

    /** Whether a folder is the one kept on disk or anywhere inside it, in Mininotes. */
    private boolean inside(String root,String folder) {
        String at=home(folder);
        for(int i=0;i<=DEEPEST*4&&!Things.HOME.equals(at);i++) {
            if(root.equals(at))return true;
            String up=store.collectionOfBook(at);at=home(up);
        }
        return false;
    }

    private Link folderAt(Path d){for(Link l:links())if(l.folder()&&l.path.equals(d))return l;return null;}
    private Link noteAt(Path f){for(Link l:links())if(l.note()&&l.path.equals(f))return l;return null;}
    private static String home(String book){return NoteStore.home(book)?Things.HOME:book;}

    private List<String> notesIn(String folder) {
        List<String> out=new ArrayList<>();
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT id FROM notes WHERE book=? AND deleted=0 AND archived=0 AND accepted=1 ORDER BY place",new String[]{folder})) {
            while(c.moveToNext())out.add(c.getString(0));
        }
        return out;
    }
    private List<String[]> foldersIn(String folder) {
        List<String[]> out=new ArrayList<>();
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT id,name FROM things WHERE parent=? AND binned=0 AND archived=0 AND accepted=1 ORDER BY ordinal",new String[]{folder})) {
            while(c.moveToNext())out.add(new String[]{c.getString(0),c.getString(1)});
        }
        return out;
    }

    // ---- keeping them up to date ------------------------------------------------------------------------------------

    /**
     * Everything kept on disk looked at, as cheaply as it can be: a note whose revision and whose file's time are what they
     * were is left alone; every folder kept on disk is made to agree. At start, when Windows says a watched folder changed,
     * after a save, and when something arrives.
     */
    List<Change> pass() {
        List<Change> changes=new ArrayList<>();
        if(locked.getAsBoolean())return changes;
        for(Link l:links()) {
            if(!l.folder()||!l.under.isEmpty())continue;
            try{changes.addAll(mirror(l));}catch(IOException|RuntimeException failed){org.mininotes.desktop.platform.util.Log.i("Mininotes/OnDisk","a folder kept on disk was not looked at: "+failed.getClass().getSimpleName());}
        }
        // Each file shared on its own through Mininotes, where it is.
        for(Link l:links()) {
            if(!l.file()||!l.under.isEmpty())continue;
            try{Change c=checkFile(l);if(c.did()!=Did.NOTHING)changes.add(c);}
            catch(IOException|RuntimeException failed){org.mininotes.desktop.platform.util.Log.i("Mininotes/OnDisk","a file shared through Mininotes was not looked at: "+failed.getClass().getSimpleName());}
        }
        Map<String,Long> revisions=revisions();
        for(Link l:links()) {
            if(!l.note())continue;
            Long revision=revisions.get(l.id);
            if(!l.isLost()&&revision!=null&&revision==l.revision&&stamp(l.path)==l.at&&l.at!=0)continue;
            try{Change c=check(l);if(c.did()!=Did.NOTHING)changes.add(c);}
            catch(IOException|RuntimeException failed){org.mininotes.desktop.platform.util.Log.i("Mininotes/OnDisk","a note kept as a file was not looked at: "+failed.getClass().getSimpleName());}
        }
        refreshStates();
        return changes;
    }

    private Map<String,Long> revisions() {
        Map<String,Long> out=new HashMap<>();
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT notes.id,notes.revision FROM notes JOIN ondisk ON ondisk.id=notes.id",null)) {
            while(c.moveToNext())out.put(c.getString(0),c.getLong(1));
        }
        return out;
    }

    /** One note and its file made to agree (see the class). */
    Change check(Link l) throws IOException {
        String called=l.path.getFileName().toString();
        if(locked.getAsBoolean()||privately.test(l.id))return new Change(l.id,Did.NOTHING,called);
        NoteStore.Note n=store.get(l.id);
        // Deleted for good: no longer kept, and the file stays where it is, as it was.
        if(n==null){forget(l.id);return new Change(l.id,Did.LEFT,called);}
        // In the bin or put away: the file waits as it was, and is written again if it comes back.
        if(n.deleted||n.archived)return new Change(l.id,Did.NOTHING,called);
        String retitle=null;
        if(!Files.isRegularFile(l.path)) {
            Path found=lookFor(l);
            if(found==null) {
                String why=driveGone(l.path)?DRIVE:GONE;
                if(why.equals(l.lost))return new Change(l.id,Did.NOTHING,called);
                put(l.lost(why));return new Change(l.id,Did.LOST,called);
            }
            // Renamed or moved beside where it was: followed, and a new name in Explorer is the note's new name.
            String stem=NoteFile.nameOf(found.getFileName().toString());
            if(found.getParent().equals(l.path.getParent())&&!stem.equals(NoteFile.fileName(name(n))))retitle=stem;
            if(found.getParent().equals(l.path.getParent())){Path files=l.path.resolveSibling(NoteFile.nameOf(called)+NoteFile.FILES),to=found.resolveSibling(stem+NoteFile.FILES);
                if(Files.isDirectory(files)&&!Files.exists(to))try{Files.move(files,to);}catch(IOException kept){/* its files stay where they were */}}
            l=l.at(found);put(l);called=found.getFileName().toString();
        }
        if(tooBig(l.path)) {
            // Too long for a note: the file is left as it is, and the note says why, until the file changes again.
            if(LONG.equals(l.lost))return new Change(l.id,Did.NOTHING,called);
            put(l.lost(LONG));return new Change(l.id,Did.LOST,called);
        }
        byte[] bytes=Files.readAllBytes(l.path);
        String fileHash=hash(bytes);long time=stamp(l.path);
        boolean fileChanged=!fileHash.equals(l.hash),noteChanged=n.revision!=l.revision,outside=fileChanged||retitle!=null;
        boolean wasLost=l.isLost();
        if(!outside&&!noteChanged) {
            if(wasLost||time!=l.at){put(l.seen(l.hash,time,l.revision,l.head));return new Change(l.id,wasLost?Did.FOUND:Did.NOTHING,called);}
            return new Change(l.id,Did.NOTHING,called);
        }
        String text=new String(bytes,StandardCharsets.UTF_8);
        NoteFile.Read r=NoteFile.read(text);
        if(r.id()!=null&&!r.id().equals(l.id)) {
            // Another note's file stands where this one's was: never written over.
            if(OTHER.equals(l.lost))return new Change(l.id,Did.NOTHING,called);
            put(l.lost(OTHER));return new Change(l.id,Did.LOST,called);
        }
        if(!outside) {
            // Only the note changed: the file written from it, under its new name where the note was renamed.
            Path was=l.path;l=renamed(l,n);
            byte[] out=NoteFile.write(l.id,l.head,n.body).getBytes(StandardCharsets.UTF_8);
            if(!Arrays.equals(out,bytes)||!was.equals(l.path))write(l.path,out);
            l=l.seen(hash(out),stamp(l.path),n.revision,l.head);put(l);files(l,n);
            return new Change(l.id,wasLost?Did.FOUND:Did.WROTE,l.path.getFileName().toString());
        }
        // The file changed outside.
        if(r.body().equals(n.body)&&retitle==null) {
            l=l.seen(fileHash,time,n.revision,r.head());
            if(r.id()==null||PrivateCode.holds(text))l=rewrite(l,n.body);
            put(l);return new Change(l.id,wasLost?Did.FOUND:Did.NOTHING,called);
        }
        // Only renamed in Explorer, the file's words are what Mininotes last wrote: the note's own, newer ones, stay.
        String body=fileChanged?r.body():n.body,title=retitle!=null?retitle:n.title;
        String over=DesktopEdits.limits(title,body);
        if(over!=null) {
            // Too long for a note: the file is left as it is, and the note says why, until the file changes again.
            if(LONG.equals(l.lost))return new Change(l.id,Did.NOTHING,called);
            put(l.lost(LONG));return new Change(l.id,Did.LOST,called);
        }
        if(Boolean.TRUE.equals(store.readOnlyHere(l.id)[0])) {
            // Read only here: what the file says is kept in Versions, and the file says what the note says again.
            if(fileChanged)store.keepVersion(l.id,n.revision,"",n.title,body);
            l=rewrite(l.seen(fileHash,time,n.revision,r.head()),n.body);put(l);
            return new Change(l.id,Did.READ_ONLY,called);
        }
        // Changed on both sides: asked, not decided here (the owner, 2026-10-14: "let's ask first"). Nothing is written over
        // until the owner says which words both keep; the other words go to Versions then.
        if(fileChanged&&noteChanged) {
            if(ASK.equals(l.lost))return new Change(l.id,Did.NOTHING,called);
            put(l.lost(ASK));return new Change(l.id,Did.BOTH,called);
        }
        // Taken in as the owner's own edit: what Mininotes said is kept in Versions, and it goes on as any edit goes.
        NoteStore.Note saved;
        var db=store.getWritableDatabase();db.beginTransaction();
        try {
            NoteStore.Note current=store.get(l.id);
            store.keepVersion(l.id,"");
            saved=current.copy();saved.body=body;saved.title=title;saved.revision=current.revision+1;saved.updated=System.currentTimeMillis();saved.by=Writers.ME;
            store.save(saved);
            db.setTransactionSuccessful();
        } finally{db.endTransaction();}
        l=l.seen(fileHash,time,saved.revision,r.head());
        // A header taken away by an editor is written back, a private code typed into the file taken out of it, and a file
        // only renamed given the note's newer words.
        if(r.id()==null||PrivateCode.holds(text)||!fileChanged)l=rewrite(l,saved.body);
        put(l);
        // Both changed is asked above; what reaches here changed only outside, or was only renamed there.
        return new Change(l.id,Did.TOOK_IN,called);
    }

    /** The file written again from the note's words, as Mininotes writes it. */
    private Link rewrite(Link l,String body) throws IOException {
        byte[] out=NoteFile.write(l.id,l.head,body).getBytes(StandardCharsets.UTF_8);
        write(l.path,out);
        return l.seen(hash(out),stamp(l.path),l.revision,l.head);
    }

    /** A note given a new title in Mininotes: its file, and its files' folder, take the new name where it is free. */
    private Link renamed(Link l,NoteStore.Note n) {
        if(n.title==null||n.title.isBlank())return l;
        String want=NoteFile.fileName(n.title),stem=NoteFile.nameOf(l.path.getFileName().toString());
        // Already that name, or that name with a number because the name itself was taken: nothing to rename.
        if(want.equals(stem)||stem.matches(java.util.regex.Pattern.quote(want)+" \\(\\d+\\)")||!Files.isRegularFile(l.path))return l;
        Path to=free(l.path.getParent(),want,NoteFile.EXTENSION);
        try {
            Files.move(l.path,to);
            Path files=l.path.resolveSibling(stem+NoteFile.FILES),filesTo=to.resolveSibling(NoteFile.nameOf(to.getFileName().toString())+NoteFile.FILES);
            if(Files.isDirectory(files)&&!Files.exists(filesTo))Files.move(files,filesTo);
            return l.at(to);
        } catch(IOException busy){return l;}
    }

    /**
     * Where a file not where it was is now: beside where it was, renamed, or anywhere in the folder kept on disk it was
     * written into, moved; known by the id in its header. Null where it is not to be seen.
     */
    private Path lookFor(Link l) {
        Set<Path> taken=new HashSet<>();for(Link other:links())if(other.note())taken.add(other.path);
        Path dir=l.path.getParent();
        Path found=dir==null?null:findIn(dir,l.id,taken,0);
        if(found!=null)return found;
        Link root=l.under.isEmpty()?null:link(l.under);
        return root==null||!Files.isDirectory(root.path)?null:findIn(root.path,l.id,taken,-DEEPEST);
    }
    private static Path findIn(Path dir,String id,Set<Path> taken,int depth) {
        if(!Files.isDirectory(dir))return null;
        List<Path> deeper=new ArrayList<>();
        try(DirectoryStream<Path> in=Files.newDirectoryStream(dir)) {
            for(Path p:in) {
                Path f=p.toAbsolutePath().normalize();
                if(Files.isDirectory(f)){if(depth<0&&!f.getFileName().toString().endsWith(NoteFile.FILES))deeper.add(f);continue;}
                if(!NoteFile.markdownFile(f.getFileName().toString())||taken.contains(f)||tooBig(f))continue;
                try{if(id.equals(NoteFile.read(Files.readString(f,StandardCharsets.UTF_8)).id()))return f;}catch(IOException|RuntimeException unreadable){/* not ours */}
            }
        } catch(IOException unreadable){return null;}
        for(Path d:deeper){Path f=findIn(d,id,taken,depth+1);if(f!=null)return f;}
        return null;
    }

    /** Whether a path is on a drive, or a share, that is not there now: unplugged, not lost. */
    static boolean driveGone(Path p) {
        Path root=p.getRoot();
        return root!=null&&!Files.exists(root);
    }
    /** The drive a path is on, as Windows says it: E:, or the share. */
    static String drive(Path p) {
        Path root=p.getRoot();if(root==null)return "";
        String r=root.toString();
        return r.endsWith("\\")&&r.length()>1?r.substring(0,r.length()-1):r;
    }

    /** The files kept with a note, written into Title.files beside it: those not there yet. Never anything taken away. */
    private void files(Link l,NoteStore.Note n) {
        List<NoteStore.Held> held=store.filesOf(NoteStore.Branch.Kind.PAGE,n.id);
        if(held.isEmpty())return;
        Path dir=l.path.resolveSibling(NoteFile.nameOf(l.path.getFileName().toString())+NoteFile.FILES);
        Set<String> used=new HashSet<>();
        for(NoteStore.Held one:held) {
            String name=NoteFile.fileName(one.name);
            int dot=name.lastIndexOf('.');String stem=dot>0?name.substring(0,dot):name,ext=dot>0?name.substring(dot):"";
            for(int i=2;!used.add(name.toLowerCase(Locale.ROOT));i++)name=stem+" ("+i+")"+ext;
            Path to=dir.resolve(name);
            if(Files.exists(to))continue;
            try{write(to,store.bytesOf(one));}catch(IOException|RuntimeException notHere){/* still on its way here: written at the next look */}
        }
    }

    // ---- the disk ---------------------------------------------------------------------------------------------------

    /** A name in a folder that nothing has yet: the name itself, else the name (2), (3)… */
    static Path free(Path folder,String stem,String extension) {
        Path at=folder.resolve(stem+extension).toAbsolutePath().normalize();
        for(int i=2;Files.exists(at);i++)at=folder.resolve(stem+" ("+i+")"+extension).toAbsolutePath().normalize();
        return at;
    }
    private static void write(Path file,byte[] bytes) throws IOException{AtomicFile.write(file,bytes);}
    private static long stamp(Path p){try{return Files.getLastModifiedTime(p).toMillis();}catch(IOException|RuntimeException none){return 0;}}
    static String hash(byte[] bytes) {
        try{return Update.hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException none){throw new IllegalStateException(none);}
    }

    /** The folders Windows is asked to watch: each note's, and every folder in a folder kept on disk. */
    Set<Path> watched() {
        Set<Path> dirs=new HashSet<>();
        for(Link l:links()) {
            if(l.note()||l.file()&&l.under.isEmpty()){Path d=l.path.getParent();if(d!=null&&Files.isDirectory(d))dirs.add(d);}
            else if(l.folder()&&Files.isDirectory(l.path))dirs.add(l.path);
        }
        return dirs;
    }

    // ---- on the PC's screen -----------------------------------------------------------------------------------------

    /** Windows asked to say when anything changes in the folders watched: a look follows a moment later, once it settled. */
    static final class Watch implements AutoCloseable {
        private final WatchService service;private final Map<Path,WatchKey> keys=new HashMap<>();private final Runnable changed;
        Watch(Runnable changed) throws IOException {
            this.changed=changed;service=FileSystems.getDefault().newWatchService();
            Thread t=new Thread(this::listen,"mininotes-ondisk");t.setDaemon(true);t.start();
        }
        /** The folders to watch now: new ones added, ones no longer wanted let go. A folder that cannot be watched is looked at in the rounds. */
        synchronized void watch(Set<Path> dirs) {
            for(Iterator<Map.Entry<Path,WatchKey>> it=keys.entrySet().iterator();it.hasNext();){var one=it.next();if(!dirs.contains(one.getKey())||!one.getValue().isValid()){one.getValue().cancel();it.remove();}}
            for(Path d:dirs)if(!keys.containsKey(d))
                try{keys.put(d,d.register(service,StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_DELETE,StandardWatchEventKinds.ENTRY_MODIFY));}
                catch(IOException|RuntimeException unwatchable){/* the round every half minute still looks */}
        }
        private void listen() {
            while(true) {
                WatchKey key;
                try{key=service.take();}catch(InterruptedException|ClosedWatchServiceException closed){return;}
                key.pollEvents();key.reset();changed.run();
            }
        }
        @Override public void close(){try{service.close();}catch(IOException closed){/* closed */}}
    }

    private Watch watch;private javax.swing.Timer soonTimer,round;private boolean looking,again,firstLook=true;

    /**
     * Started once the window is up: every file kept looked at, the folders watched, and a look again a moment after
     * anything changes in them, after anything is saved or arrives, and every half minute for what Windows cannot watch
     * (a drive plugged in again, a folder on another computer).
     */
    void start(Desktop d) {
        soonTimer=new javax.swing.Timer(700,e->look(d));soonTimer.setRepeats(false);
        round=new javax.swing.Timer(30_000,e->look(d));round.start();
        try{watch=new Watch(()->javax.swing.SwingUtilities.invokeLater(this::soon));}catch(IOException|RuntimeException none){watch=null;}
        look(d);
    }
    /** A look a moment from now; more changes before then wait for the same one. */
    void soon(){if(soonTimer!=null)soonTimer.restart();}
    void stop() {
        if(soonTimer!=null)soonTimer.stop();if(round!=null)round.stop();
        if(watch!=null)watch.close();watch=null;soonTimer=null;round=null;
    }
    private void look(Desktop d) {
        if(soonTimer==null)return;
        if(looking){again=true;return;}
        looking=true;
        d.disk.submit(()->new Object[]{pass(),watched()},got->{
            looking=false;
            @SuppressWarnings("unchecked") List<Change> changes=(List<Change>)got[0];
            @SuppressWarnings("unchecked") Set<Path> dirs=(Set<Path>)got[1];
            if(watch!=null)watch.watch(dirs);
            said(d,changes);
            // Asked once at start for what was left waiting, as it is when it happens.
            if(firstLook){firstLook=false;d.disk.submit(this::filesAsked,asked->asked.forEach((id,name)->askWhichFile(d,id,name)),e->{});}
            if(again){again=false;soon();}
        },e->looking=false);
    }

    /**
     * What a look did, said: what was taken in, found or lost, on the bar; the note on the page follows what was taken in;
     * and what was taken in goes on as any edit goes. Writing a file is what is expected, and is not said each time.
     */
    private void said(Desktop d,List<Change> changes) {
        String words=null;boolean drawn=false;
        for(Change c:changes) {
            switch(c.did()) {
                case TOOK_IN->{d.sendSoon(c.id());d.follow(c.id());words=Say.t("Taken in from {0}",c.name());}
                case BOTH->{Link l=link(c.id());if(l!=null&&l.file())askWhichFile(d,c.id(),c.name());else words=lostWords(ASK,c.name(),pathOf(c.id()));}
                case READ_ONLY->words=Say.t("Read only here. What {0} said is kept in Versions",c.name());
                case ADDED->{d.sendSoon(c.id());words=Say.t("{0} added from the folder on disk",c.name());}
                case FILE_ADDED->{d.fileToMine(c.id());words=Say.t("{0} added from the folder on disk",c.name());}
                case FILE_TAKEN->{d.fileToMine(c.id());words=Say.t("Taken in from {0}",c.name());}
                case FOUND->{d.follow(c.id());words=Say.t("{0} found again",c.name());}
                case LOST->words=lostWords(state(c.id()),c.name(),pathOf(c.id()));
                default->{}
            }
            drawn|=c.did()!=Did.NOTHING&&c.did()!=Did.WROTE;
        }
        if(words!=null){d.status.setToolTipText(null);d.status.setText(words);}
        if(drawn)d.refresh();
        else if(d.home!=null)d.home.repaint();
        d.lostStrip.update(d);
    }

    /**
     * Which version a file changed on this PC and in Mininotes keeps, asked in a box of its own that waits for the owner;
     * nothing is written over until then. Closed without an answer, it is asked again from the file's menu.
     */
    static void askWhichFile(Desktop d,String id,String name) {
        if(asking.contains(id))return;
        asking.add(id);
        Object[] ways={Say.t("Keep the file on this PC"),Say.t("Keep the version from Mininotes"),Say.t("Decide later")};
        javax.swing.SwingUtilities.invokeLater(()->{
            int picked=javax.swing.JOptionPane.showOptionDialog(d.frame,
                Say.t("{0} was changed on this PC and in Mininotes. Which version should it keep? The other one is replaced.",name),
                Say.t("Changed in two places"),javax.swing.JOptionPane.DEFAULT_OPTION,javax.swing.JOptionPane.QUESTION_MESSAGE,null,ways,ways[0]);
            asking.remove(id);
            if(picked!=0&&picked!=1){d.status.setText(Say.t("{0} waits: which version to keep, from its menu",name));return;}
            boolean theDisk=picked==0;
            d.disk.submit(()->d.onDisk.settleFile(id,theDisk),c->{
                if(theDisk)d.fileToMine(id);
                d.status.setText(theDisk?Say.t("Taken in from {0}",name):Say.t("{0} on this PC is Mininotes' version now",name));
                d.refresh();
            },e->failed(d,e));
        });
    }
    private static final Set<String> asking=new HashSet<>();

    /** Why a file is lost, in a few words with its name. */
    static String lostWords(String why,String name,Path path) {
        if(DRIVE.equals(why))return Say.t("{0} is on {1}, not connected",name,path==null?"":drive(path));
        if(OTHER.equals(why))return Say.t("{0} now holds another note",name);
        if(LONG.equals(why))return Say.t("{0} is too long to take in",name);
        if(ASK.equals(why))return Say.t("{0} changed here and in its file. Which words should it keep?",name);
        Path in=path==null?null:path.getParent();
        return Say.t("{0} is no longer in {1}",name,in==null||in.getFileName()==null?String.valueOf(in):in.getFileName().toString());
    }

    /** The menu lines for a note or a folder, kept on disk or not. A private thing never has them (decision 111). */
    static void rows(Desktop d,javax.swing.JPopupMenu menu,NoteStore.Branch thing) {
        boolean note=thing.kind==NoteStore.Branch.Kind.PAGE,folder=DesktopMoving.shelf(thing.kind);
        if(!note&&!folder||d.onDisk==null)return;
        String state=d.onDisk.state(thing.id);
        if(state==null) {
            javax.swing.JMenuItem keep=DesktopMenus.item("Save on my device…");
            keep.addActionListener(a->{if(note)keepAsFile(d,thing);else keepAsFolder(d,thing);});menu.add(keep);
            return;
        }
        // A folder that lives on this PC says where, first (the owner, 2026-10-18: "this folder should be shown in the app as
        // external folder and the content of it saved of course out of the app"; decision 197).
        Path there=d.onDisk.pathOf(thing.id);
        if(folder&&there!=null&&!d.onDisk.underFolder(thing.id)){javax.swing.JMenuItem lives=DesktopMenus.plain(Say.t("On this PC: {0}",there.toString()));lives.setEnabled(false);menu.add(lives);}
        javax.swing.JMenuItem show=DesktopMenus.item("Show in Explorer");show.addActionListener(a->showInExplorer(d,thing.id));menu.add(show);
        if(!state.isEmpty()&&note){javax.swing.JMenuItem find=DesktopMenus.item("Find it…");find.addActionListener(a->findIt(d,thing.id));menu.add(find);}
        if(!d.onDisk.underFolder(thing.id)){javax.swing.JMenuItem inside=DesktopMenus.item("Keep it inside only");inside.addActionListener(a->insideOnly(d,thing.id));menu.add(inside);}
        // Out of Mininotes altogether, the file left where it is (the owner, 2026-10-14: "here we miss remove from the app").
        if(!d.onDisk.underFolder(thing.id)){javax.swing.JMenuItem out=DesktopMenus.item("Remove from the app…");out.addActionListener(a->outOfTheApp(d,thing));menu.add(out);}
    }

    /**
     * Remove from the app: Mininotes no longer keeps it, and the file or the folder on disk stays exactly as it is, where it
     * is. What was in Mininotes goes to the bin, so it can still be put back; nothing on the disk is touched.
     */
    static void outOfTheApp(Desktop d,NoteStore.Branch thing) {
        Path p=d.onDisk.pathOf(thing.id);
        d.disk.submit(()->{d.onDisk.keepInsideOnly(thing.id);return null;},done->{
            d.putAway(thing,true);
            d.status.setText(Say.t("Removed from Mininotes. {0} stays where it is",p==null?"":p.getFileName().toString()));
            d.lostStrip.update(d);
        },e->failed(d,e));
    }

    /** The folder last chosen, offered first next time; Documents the first time. */
    private static java.io.File lastFolder(Desktop d) {
        String kept=d.context.getSharedPreferences("settings",0).getString("onDiskFolder","");
        if(!kept.isEmpty()&&Files.isDirectory(Path.of(kept)))return new java.io.File(kept);
        java.io.File documents=new java.io.File(System.getProperty("user.home"),"Documents");
        return documents.isDirectory()?documents:new java.io.File(System.getProperty("user.home"));
    }
    private static Path chooseFolder(Desktop d,String title) {
        try {
            Path chosen=DesktopFileDialog.folder(d.frame,title,lastFolder(d).toPath(),"Keep here");
            if(chosen!=null)d.context.getSharedPreferences("settings",0).edit().putString("onDiskFolder",chosen.toString()).apply();
            return chosen;
        } catch(DesktopFileDialog.Unavailable old){/* Java's own box, then */}
        javax.swing.JFileChooser pick=new javax.swing.JFileChooser(lastFolder(d));
        pick.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);pick.setDialogTitle(Say.t(title));
        if(pick.showDialog(d.frame,Say.t("Keep here"))!=javax.swing.JFileChooser.APPROVE_OPTION||pick.getSelectedFile()==null)return null;
        Path chosen=pick.getSelectedFile().toPath().toAbsolutePath();
        d.context.getSharedPreferences("settings",0).edit().putString("onDiskFolder",chosen.toString()).apply();
        return chosen;
    }
    /** Said when the notebook is encrypted: what is written out is not (docs/FILES.md, accepted by the owner). */
    private static String plain(Desktop d){return DesktopLock.locked(d.context.getFilesDir().toPath())?" "+Say.t("The file is not encrypted."):"";}
    private static String folderName(Path p){return p.getFileName()==null?p.toString():p.getFileName().toString();}
    private static void failed(Desktop d,Exception e){d.status.setText(Say.t(e.getMessage()==null?"That did not finish. Please try again.":e.getMessage()));}

    /** Keep as a file…: the folder chosen, the note written there, and said. */
    static void keepAsFile(Desktop d,NoteStore.Branch note) {
        // Saved first, so the name the box offers is the note's title as it is now (the owner, 2026-10-13: "by default the
        // name when saving should be the title of the note").
        d.save(()->d.disk.submit(()->{NoteStore.Note n=d.store.get(note.id);return n==null?"Untitled":NoteFile.fileName(name(n));},named->{
            Path file;
            try{file=DesktopFileDialog.save(d.frame,"Keep as a file",lastFolder(d).toPath(),named+NoteFile.EXTENSION,"Keep here");}
            catch(DesktopFileDialog.Unavailable old){
                // Java's own box where Windows' cannot be had: the folder chosen, the note's title as the name.
                Path folder=chooseFolder(d,"Keep as a file");file=folder==null?null:folder.resolve(named+NoteFile.EXTENSION);
            }
            if(file==null)return;
            Path chosen=file.toAbsolutePath();
            d.context.getSharedPreferences("settings",0).edit().putString("onDiskFolder",chosen.getParent().toString()).apply();
            d.status.setText(Say.t("Writing the file…"));
            d.disk.submit(()->d.onDisk.keepNoteAs(note.id,chosen),l->{
                d.status.setText(Say.t("Kept as {0} in {1}",l.path().getFileName().toString(),folderName(chosen.getParent()))+plain(d));
                // Named otherwise in the box: the note took that name, which goes as any rename goes.
                d.sendSoon(note.id);d.follow(note.id);d.refresh();d.onDisk.soon();
            },e->failed(d,e));
        },e->failed(d,e)));
    }
    /** Keep as a folder on disk…: the folder and all in it written in the one chosen, and kept both ways. */
    static void keepAsFolder(Desktop d,NoteStore.Branch folder) {
        Path parent=chooseFolder(d,"Keep as a folder on disk");
        if(parent==null)return;
        d.save(()->{
            d.status.setText(Say.t("Writing the files…"));
            d.disk.submit(()->d.onDisk.keepFolder(folder.id,parent),l->{
                d.status.setText(Say.t("Kept as the folder {0} in {1}",l.path().getFileName().toString(),folderName(parent))+plain(d));
                d.refresh();d.onDisk.soon();
            },e->failed(d,e));
        });
    }

    /** Share a file through Mininotes…: the file chosen, taken in where it is, said, and shown on Home. */
    static void syncFile(Desktop d) {
        List<Path> files;
        try{files=DesktopFileDialog.openAny(d.frame,"Share a file through Mininotes",lastFolder(d).toPath(),false);}
        catch(DesktopFileDialog.Unavailable old) {
            javax.swing.JFileChooser pick=new javax.swing.JFileChooser(lastFolder(d));pick.setDialogTitle(Say.t("Share a file through Mininotes"));
            if(pick.showOpenDialog(d.frame)!=javax.swing.JFileChooser.APPROVE_OPTION||pick.getSelectedFile()==null)return;
            files=List.of(pick.getSelectedFile().toPath());
        }
        if(files==null||files.isEmpty())return;
        Path file=files.get(0);
        d.context.getSharedPreferences("settings",0).edit().putString("onDiskFolder",file.toAbsolutePath().getParent().toString()).apply();
        d.status.setToolTipText(null);d.status.setText(Say.t("Taking in {0}…",file.getFileName().toString()));
        d.disk.submit(()->d.onDisk.syncFile(file),l->{
            d.fileToMine(l.id());
            d.status.setText(Say.t("{0} stays where it is. Each version you save goes to your devices",l.path().getFileName().toString()));
            d.refresh();d.onDisk.soon();
        },e->failed(d,e));
    }

    /** Keep a Windows folder in sync…: the folder chosen, taken in, said while it is, and shown on Home. */
    static void syncFolder(Desktop d) {
        Path dir=chooseFolder(d,"Sync a folder of this PC");
        if(dir==null)return;
        d.save(()->{
            d.status.setToolTipText(null);d.status.setText(Say.t("Taking in {0}…",dir.getFileName()==null?dir.toString():dir.getFileName().toString()));
            d.disk.submit(()->d.onDisk.syncFolder(dir),l->{
                d.status.setText(Say.t("{0} is kept in step with Mininotes and your devices",l.path().getFileName().toString())+plain(d));
                d.refresh();d.onDisk.soon();
            },e->failed(d,e));
        });
    }

    /** Open a Markdown file…: each one chosen a note kept as that file, on Home; the last one opened. */
    static void openFile(Desktop d) {
        try {
            List<Path> files=DesktopFileDialog.open(d.frame,"Open a Markdown file",lastFolder(d).toPath(),true);
            if(files==null||files.isEmpty())return;
            d.context.getSharedPreferences("settings",0).edit().putString("onDiskFolder",files.get(0).toAbsolutePath().getParent().toString()).apply();
            open(d,files,Things.HOME,null);return;
        } catch(DesktopFileDialog.Unavailable old){/* Java's own box, then */}
        javax.swing.JFileChooser pick=new javax.swing.JFileChooser(lastFolder(d));
        pick.setMultiSelectionEnabled(true);pick.setDialogTitle(Say.t("Open a Markdown file"));
        pick.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Markdown (*.md)","md","markdown"));
        if(pick.showOpenDialog(d.frame)!=javax.swing.JFileChooser.APPROVE_OPTION)return;
        java.io.File[] chosen=pick.getSelectedFiles();if(chosen.length==0&&pick.getSelectedFile()!=null)chosen=new java.io.File[]{pick.getSelectedFile()};
        List<Path> files=new ArrayList<>();for(java.io.File f:chosen)files.add(f.toPath());
        if(files.isEmpty())return;
        d.context.getSharedPreferences("settings",0).edit().putString("onDiskFolder",files.get(0).toAbsolutePath().getParent().toString()).apply();
        open(d,files,Things.HOME,null);
    }

    /**
     * Markdown files made notes kept as themselves, where they are: from Open a Markdown file…, or dropped on Home or a
     * folder ({@code place}: the place a drop on Temp's icon puts them in, or null). One is opened on the page; many are said.
     */
    static void open(Desktop d,List<Path> files,String folder,String place) {
        d.save(()->{
            d.status.setText(Say.t("Opening the file…"));
            d.disk.submit(()->{
                List<NoteStore.Note> made=new ArrayList<>();List<String> refused=new ArrayList<>();
                for(Path f:files)try{NoteStore.Note n=d.onDisk.open(f,folder);made.add(n);if(place!=null)d.intoPlaceNow(NoteStore.Branch.Kind.PAGE,n.id,place);}
                catch(IOException|RuntimeException e){refused.add(f.getFileName()+": "+Say.t(e.getMessage()==null?"It could not be read.":e.getMessage()));}
                return new Object[]{made,refused};
            },got->{
                @SuppressWarnings("unchecked") List<NoteStore.Note> made=(List<NoteStore.Note>)got[0];
                @SuppressWarnings("unchecked") List<String> refused=(List<String>)got[1];
                d.refresh();d.onDisk.soon();
                for(NoteStore.Note n:made)d.sendSoon(n.id);
                if(!refused.isEmpty()){d.status.setText(Say.t(refused.get(0)));return;}
                if(made.size()==1){d.open(made.get(0).id);d.status.setText(Say.t("{0} opened as a note",files.get(0).getFileName().toString()));}
                else d.status.setText(Say.t("{0} Markdown files opened as notes",made.size()));
            },e->failed(d,e));
        });
    }

    /** Show in Explorer: the file chosen in its folder, or the folder kept on disk opened; where it is gone, what is left of the way to it. */
    static void showInExplorer(Desktop d,String id) {
        Path p=d.onDisk.pathOf(id);
        if(p==null)return;
        try {
            if(Files.isRegularFile(p))new ProcessBuilder("explorer.exe","/select,",p.toString()).start();
            else if(Files.isDirectory(p))new ProcessBuilder("explorer.exe",p.toString()).start();
            else if(p.getParent()!=null&&Files.isDirectory(p.getParent())){new ProcessBuilder("explorer.exe",p.getParent().toString()).start();d.status.setText(lostWords(GONE,p.getFileName().toString(),p));}
            else d.status.setText(lostWords(driveGone(p)?DRIVE:GONE,p.getFileName().toString(),p));
        } catch(IOException none){d.status.setText(Say.t("Explorer could not be opened"));}
    }

    /** Keep it inside only: no longer kept on disk. The note stays, and so does the file, where it is. */
    static void insideOnly(Desktop d,String id) {
        Path p=d.onDisk.pathOf(id);
        d.disk.submit(()->{d.onDisk.keepInsideOnly(id);return null;},done->{
            d.status.setText(Say.t("Kept inside Mininotes only. {0} stays where it is",p==null?"":p.getFileName().toString()));
            d.refresh();d.lostStrip.update(d);
        },e->failed(d,e));
    }

    /**
     * Which words a note changed in both places keeps: the file's, taken in as an edit made outside, Mininotes' own kept in
     * Versions; or Mininotes', written to the file, the file's kept in Versions. Nothing anybody wrote is lost either way.
     */
    static void settle(Desktop d,String id,boolean theFiles) {
        d.disk.submit(()->{d.onDisk.settled(id,theFiles);return null;},done->{
            d.status.setToolTipText(null);d.status.setText(Say.t(theFiles?"The file's words are kept. Mininotes' are in Versions":"Mininotes' words are kept. The file's are in Versions"));
            d.onDisk.soon();d.lostStrip.update(d);
        },e->failed(d,e));
    }

    /** Find it…: the file picked where it is now, taken only if it is this note's. */
    static void findIt(Desktop d,String id) {
        Path was=d.onDisk.pathOf(id);
        java.io.File from=was!=null&&was.getParent()!=null&&Files.isDirectory(was.getParent())?was.getParent().toFile():lastFolder(d);
        Path picked;
        try {
            List<Path> got=DesktopFileDialog.open(d.frame,"Find it",from.toPath(),false);
            if(got==null||got.isEmpty())return;
            picked=got.get(0);
        } catch(DesktopFileDialog.Unavailable old) {
            javax.swing.JFileChooser pick=new javax.swing.JFileChooser(from);
            pick.setDialogTitle(Say.t("Find it"));pick.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Markdown (*.md)","md","markdown"));
            if(pick.showOpenDialog(d.frame)!=javax.swing.JFileChooser.APPROVE_OPTION||pick.getSelectedFile()==null)return;
            picked=pick.getSelectedFile().toPath();
        }
        Path file=picked;
        d.status.setText(Say.t("Looking at the file…"));
        d.disk.submit(()->d.onDisk.findIt(id,file),c->{
            d.status.setText(Say.t("{0} found again",file.getFileName().toString()));
            d.follow(id);if(c.did()==Did.TOOK_IN||c.did()==Did.BOTH)d.sendSoon(id);
            d.refresh();d.lostStrip.update(d);
        },e->failed(d,e));
    }

    /** The line on the open note while its file is lost: what happened, then Keep it inside only and Find it…. */
    static final class Strip extends javax.swing.JPanel {
        private final javax.swing.JLabel words=new javax.swing.JLabel();
        private final javax.swing.JPanel buttons=new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,8,0));
        private String shownFor,shownWhy;
        Strip() {
            super(new java.awt.BorderLayout(12,0));
            setOpaque(false);setBorder(javax.swing.BorderFactory.createEmptyBorder(6,30,6,38));setVisible(false);
            words.setFont(DesktopUi.BODY.deriveFont(13.5f));words.setForeground(DesktopUi.INK);
            buttons.setOpaque(false);add(words,java.awt.BorderLayout.CENTER);add(buttons,java.awt.BorderLayout.EAST);
        }
        @Override protected void paintComponent(java.awt.Graphics g) {
            java.awt.Color warning=DesktopMark.ink(NoteLine.Tone.FAILED.mark);
            g.setColor(DesktopUi.mix(warning,DesktopUi.PAPER,0.88f));g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(DesktopUi.mix(warning,DesktopUi.PAPER,0.6f));g.fillRect(0,getHeight()-1,getWidth(),1);
        }
        /** What the line says now, or null while it is put away: what the tests read. */
        String said(){return isVisible()?words.getText():null;}
        /** Shown, said again, or put away, for the note now open. */
        void update(Desktop d) {
            String id=d.noteShown();
            String why=id==null||d.onDisk==null?null:d.onDisk.state(id);
            if(why==null||why.isEmpty()){if(isVisible()){setVisible(false);revalidate();}shownFor=null;return;}
            if(id.equals(shownFor)&&why.equals(shownWhy)&&isVisible())return;
            shownFor=id;shownWhy=why;
            Path p=d.onDisk.pathOf(id);
            String said=lostWords(why,p==null?"":p.getFileName().toString(),p);
            words.setText(said);words.getAccessibleContext().setAccessibleName(said);
            words.setIcon(new javax.swing.Icon(){
                public int getIconWidth(){return 18;}public int getIconHeight(){return 18;}
                public void paintIcon(java.awt.Component c,java.awt.Graphics g,int x,int y){DesktopIcons.draw((java.awt.Graphics2D)g,"file-exclamation-point",x,y,18,DesktopMark.ink(NoteLine.Tone.FAILED.mark));}
            });words.setIconTextGap(8);
            buttons.removeAll();
            // Changed in both places: the two ways, the file's words the filled one, since an edit outside is what was just done.
            if(ASK.equals(why)) {
                buttons.add(DesktopUi.button("Keep Mininotes' words",()->settle(d,id,false)));
                buttons.add(DesktopUi.primary("Keep the file's words",()->settle(d,id,true)));
                setVisible(true);revalidate();repaint();return;
            }
            // The next thing to do is to find it: the one filled button, last, where the eye ends. Keeping it inside only is the other way out.
            if(!d.onDisk.underFolder(id))buttons.add(DesktopUi.button("Keep it inside only",()->insideOnly(d,id)));
            buttons.add(DesktopUi.primary("Find it…",()->findIt(d,id)));
            setVisible(true);revalidate();repaint();
        }
    }
}
