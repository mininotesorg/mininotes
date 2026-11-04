// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Lending one kept file to whatever app can open it. A file in the app's own folder cannot be handed over
 * as a path — the phone refuses that, and rightly — so it is handed over as an address that grants the
 * reader one look at one file, for as long as that app is open.
 *
 * <p>Read only, but for a working copy lent by Open to edit (see Editing); never exported, and it will only open a file the notebook has a row for: an address made
 * up by something else opens nothing.
 */
public final class Lending extends ContentProvider {
    /** One per app id, so the demo build (".demo") installs beside the real app without the two claiming one address. */
    static final String AUTHORITY=BuildConfig.APPLICATION_ID+".files";

    /** The address for one kept file. */
    static Uri of(String id){return Uri.parse("content://"+AUTHORITY+"/"+id);}

    /**
     * The address for one note as a Markdown file, as a note is kept as a file anywhere (NoteFile; decision 162): what Send with
     * Parlons! hands over for a note (the owner, 2026-11-03; decision 321). Written as it is read, never as a copy on the phone.
     */
    static Uri ofNote(String id){return Uri.parse("content://"+AUTHORITY+"/"+NOTE+"/"+Uri.encode(id));}
    private static final String NOTE="note";

    /** The note an address names, or null where it names none: an address for a file, or a note not in the notebook. */
    private NoteStore.Note note(Uri uri) throws FileNotFoundException {
        java.util.List<String> path=uri==null?java.util.List.of():uri.getPathSegments();
        if(path.size()!=2||!NOTE.equals(path.get(0)))return null;
        NoteStore.Note note=store().get(path.get(1));
        if(note==null||note.deleted)throw new FileNotFoundException("No such note");
        return note;
    }

    private static byte[] markdown(NoteStore.Note note) {
        return NoteFile.write(note.id,"",note.body).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String fileName(NoteStore.Note note){return NoteFile.fileName(note.title)+NoteFile.EXTENSION;}


    @Override public boolean onCreate(){return true;}

    /** Opened lazily and left open: the provider lives as long as the app does. */
    /** The process's own notebook, so a locked one is lent only while it is open. */
    private NoteStore store() throws FileNotFoundException {
        try{return NoteStore.of(getContext());}catch(IllegalStateException locked){throw new FileNotFoundException("The notebook is locked");}
    }

    private NoteStore.Held asked(Uri uri) throws FileNotFoundException {
        String id=uri==null?null:uri.getLastPathSegment();
        if(id==null||Attachment.idOf(Attachment.entry(id))==null)throw new FileNotFoundException("Not a file of ours");
        NoteStore.Held held=store().keptFile(id);
        if(held==null)throw new FileNotFoundException("No such file");
        return held;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        NoteStore.Note note=note(uri);
        if(note!=null) {
            if(mode!=null&&!mode.equals("r"))throw new FileNotFoundException("Read only");
            byte[] words=markdown(note);
            try {
                ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createPipe();
                new Thread(()->{try(java.io.OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){out.write(words);}catch(java.io.IOException gone){/* they stopped reading */}},"mininotes-lending").start();
                return pipe[0];
            } catch(java.io.IOException e){throw new FileNotFoundException("Could not open the note");}
        }
        NoteStore.Held held=asked(uri);
        // Open to edit (see Editing): the working copy made for it, which the app it was lent to may write.
        if(Editing.editing(uri)) {
            File copy=Editing.copyOf(getContext(),held.id);
            if(!copy.isFile())throw new FileNotFoundException("Not lent to edit");
            return ParcelFileDescriptor.open(copy,ParcelFileDescriptor.parseMode(mode==null?"r":mode));
        }
        if(mode!=null&&!mode.equals("r"))throw new FileNotFoundException("Read only");
        File file=store().fileFor(held.id);
        if(!file.isFile())throw new FileNotFoundException("The file is gone");
        if(!PhoneLock.sealed(file)&&!PhoneLock.packed(file))return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
        // Sealed or packed (decision 112): opened into a pipe as the other app reads it, so no plain copy is ever written down
        // and the other app reads the file's own bytes.
        try {
            ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createPipe();
            new Thread(()->{try(java.io.OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){PhoneLock.copyOut(file,out);}catch(java.io.IOException gone){/* they stopped reading */}},"mininotes-lending").start();
            return pipe[0];
        } catch(java.io.IOException e){throw new FileNotFoundException("Could not open the file");}
    }

    @Override public String getType(Uri uri) {
        try{return note(uri)!=null?"text/markdown":asked(uri).kind;}catch(FileNotFoundException e){return null;}
    }

    /** What every app asks before opening something: what it is called and how big it is. */
    @Override public Cursor query(Uri uri,String[] wanted,String where,String[] args,String order) {
        String[] columns=wanted==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:wanted;
        try {
            NoteStore.Note note=note(uri);
            if(note!=null) {
                MatrixCursor answer=new MatrixCursor(columns,1);Object[] row=new Object[columns.length];
                for(int at=0;at<columns.length;at++)
                    row[at]=OpenableColumns.DISPLAY_NAME.equals(columns[at])?fileName(note):OpenableColumns.SIZE.equals(columns[at])?(Object)(long)markdown(note).length:null;
                answer.addRow(row);
                return answer;
            }
        } catch(FileNotFoundException e){return null;}
        NoteStore.Held held;
        try{held=asked(uri);}catch(FileNotFoundException e){return null;}
        MatrixCursor answer=new MatrixCursor(columns,1);
        Object[] row=new Object[columns.length];
        for(int at=0;at<columns.length;at++) {
            if(OpenableColumns.DISPLAY_NAME.equals(columns[at]))row[at]=held.name;
            else if(OpenableColumns.SIZE.equals(columns[at]))row[at]=Editing.editing(uri)?Editing.copyOf(getContext(),held.id).length():held.bytes;
        }
        answer.addRow(row);
        return answer;
    }

    // Nothing is written, deleted or renamed through here: this is a way out of the pad, not a way in.
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}
}
